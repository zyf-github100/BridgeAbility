package com.rongzhiqiao.notification.repository;

import java.time.LocalDateTime;

public record NotificationMessageRecord(
        String id,
        String type,
        String title,
        String content,
        String targetRole,
        LocalDateTime createdAt,
        Long createdByUserId,
        boolean defaultRead,
        int sortNo
) {
}
