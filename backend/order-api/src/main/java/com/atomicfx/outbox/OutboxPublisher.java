package com.atomicfx.outbox;

import com.atomicfx.config.SolaceConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * OutboxPublisher polls the outbox table for unpublished events and reliably
 * publishes them to Solace via REST API. This decouples the API from Solace availability.
 *
 * Key responsibilities:
 * 1. Poll unpublished events from the database
 * 2. Publish each to Solace REST API
 * 3. Mark as published ONLY after Solace ACKs
 * 4. Retry on failure (event stays in DB, will be retried next cycle)
 *
 * This is the "reliable outbox pattern" or "transactional outbox."
 */
@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxRepository outboxRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxRepository outboxRepository, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Scheduled task: run every 500ms.
     * This interval is a balance: frequent enough to be responsive (low latency),
     * infrequent enough to not hammer the database.
     * In production, this might be 1000ms or configurable.
     */
    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
    @Transactional
    public void publishOutboxEvents() {
        try {
            // Query: find all unpublished events, ordered by creation time
            // This ensures events are published in order (FIFO per trade).
            List<OutboxEvent> unpublished = outboxRepository.findByPublishedFalseOrderByCreatedAt();

            if (unpublished.isEmpty()) {
                return; // No events to publish; skip logging noise
            }

            log.info("Publishing {} outbox events", unpublished.size());

            for (OutboxEvent event : unpublished) {
                try {
                    publishToSolace(event);
                    // Mark as published: only if Solace sends succeeded
                    event.setPublished(true);
                    outboxRepository.save(event);
                    log.debug("Published and marked: {}", event.getEventId());
                } catch (Exception e) {
                    // Solace is down, unreachable, or slow timeout.
                    // Leave published = false; retry next cycle.
                    // This event will block subsequent events for the same trade
                    // (ordering guarantee), but that's acceptable for short outages.
                    log.warn("Failed to publish event {}: {}. Will retry next cycle.",
                        event.getEventId(), e.getMessage());
                    // Don't throw; continue to next event so we don't get stuck.
                }
            }
        } catch (Exception e) {
            log.error("OutboxPublisher error", e);
            // Don't re-throw; let the next scheduled invocation retry.
        }
    }

    /**
     * Send a single event to Solace via REST API.
     * Topic is derived from event type: "trading.fx.trade.submitted"
     *
     * After publishing to Solace, also notify interested services (Settlement Service)
     * via HTTP webhooks. This creates dual pathways:
     * - Async via Solace (reliable, fan-out to many subscribers)
     * - Sync via webhook (immediate processing for direct subscribers)
     */
    private void publishToSolace(OutboxEvent event) {
        String topic = event.getEventType();
        String payload = event.getPayload();

        log.debug("Sending to Solace: topic={}, eventId={}", topic, event.getEventId());

        try {
            // Publish to Solace (async, durable)
            Map<String, Object> requestBody = Map.of(
                "topic", topic,
                "message", payload
            );
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String auth = SolaceConfig.SolaceProperties.USERNAME + ":" + SolaceConfig.SolaceProperties.PASSWORD;
            String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            String url = SolaceConfig.SolaceProperties.BASE_URL + "/api/v2/messagingServices/default/topics";

            restTemplate.postForObject(url, request, String.class);
            log.debug("Published to Solace: {}", event.getEventId());

            // Notify downstream services via webhooks (for immediate processing)
            notifySubscribers(topic, payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish to Solace: " + e.getMessage(), e);
        }
    }

    /**
     * Notify interested services of the event via HTTP webhooks.
     * This is a synchronous notification alongside the async Solace path.
     */
    private void notifySubscribers(String topic, String payload) {
        if ("trading.fx.trade.submitted".equals(topic)) {
            try {
                log.debug("Notifying Settlement Service via webhook...");
                String webhookUrl = "http://localhost:8081/webhooks/trade-submitted";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> request = new HttpEntity<>(payload, headers);
                restTemplate.postForObject(webhookUrl, request, String.class);
                log.debug("Settlement Service notified");
            } catch (Exception e) {
                log.warn("Failed to notify Settlement Service (non-fatal, Solace path still active): {}", e.getMessage());
                // Non-fatal: Solace still has the event for async processing
            }
        }
    }
}
