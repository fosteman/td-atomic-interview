package com.atomicfx.outbox;

import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * OutboxPublisher polls unpublished events and publishes to Solace via JCSMP.
 * Transactional outbox pattern: if API crashes, events survive in DB and are published on recovery.
 */
@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxRepository outboxRepository;
    private final JCSMPSession solaceSession;
    private final XMLMessageProducer producer;

    public OutboxPublisher(OutboxRepository outboxRepository, JCSMPSession solaceSession) throws JCSMPException {
        this.outboxRepository = outboxRepository;
        this.solaceSession = solaceSession;
        this.producer = solaceSession.getMessageProducer(new AsyncHandler());
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
    @Transactional
    public void publishOutboxEvents() {
        try {
            List<OutboxEvent> unpublished = outboxRepository.findByPublishedFalseOrderByCreatedAt();
            if (unpublished.isEmpty()) {
                return;
            }

            log.info("Publishing {} events to Solace", unpublished.size());

            for (OutboxEvent event : unpublished) {
                try {
                    publishToSolace(event);
                    event.setPublished(true);
                    outboxRepository.save(event);
                    log.debug("Published and marked: {}", event.getEventId());
                } catch (Exception e) {
                    log.warn("Publish failed (will retry): eventId={}, error={}",
                        event.getEventId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("OutboxPublisher error", e);
        }
    }

    private void publishToSolace(OutboxEvent event) throws JCSMPException {
        String queueName = event.getEventType();
        String payload = event.getPayload();

        log.debug("Publishing: queue={}, eventId={}", queueName, event.getEventId());

        Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
        TextMessage message = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
        message.setText(payload);

        producer.send(message, queue);
        log.info("Published: eventId={}", event.getEventId());
    }

    private static class AsyncHandler implements JCSMPStreamingPublishEventHandler {
        private static final Logger log = LoggerFactory.getLogger(AsyncHandler.class);

        @Override
        public void responseReceived(String messageID) {
            log.debug("ACK: {}", messageID);
        }

        @Override
        public void handleError(String messageID, JCSMPException e, long timestamp) {
            log.error("Publish error: {}", e.getMessage());
        }
    }
}
