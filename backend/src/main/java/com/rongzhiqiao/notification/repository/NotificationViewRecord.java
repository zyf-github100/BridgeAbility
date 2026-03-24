package com.rongzhiqiao.notification.repository;

import java.time.LocalDateTime;

public record NotificationViewRecord(
        String id,
        String type,
        String title,
        String content,
        String targetRole,
        LocalDateTime createdAt,
        boolean read
) {
}
