package com.paranietharan.byteblog.repository;

import com.paranietharan.byteblog.entity.NotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {
}
