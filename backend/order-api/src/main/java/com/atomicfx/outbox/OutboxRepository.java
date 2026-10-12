package com.atomicfx.outbox;

import org.springframework.data.jpa.repository.JpaRepository; // java persistence api interface that gives me methods to find / update entities etc. good stuff.
// hyber

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findByPublishedFalseOrderByCreatedAt();
}
