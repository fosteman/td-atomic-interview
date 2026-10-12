package com.atomicfx.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_published", columnList = "published"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class OutboxEvent {
    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "trade_id")
    private String tradeId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "published")
    private boolean published = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public OutboxEvent() {
    }

    public OutboxEvent(String tradeId, String eventType, String payload) {
        this.eventId = UUID.randomUUID().toString();
        this.tradeId = tradeId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.published = false;
    }

    // Getters & Setters
    public String getEventId() { return eventId; }
    public String getTradeId() { return tradeId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public Instant getCreatedAt() { return createdAt; }
}
