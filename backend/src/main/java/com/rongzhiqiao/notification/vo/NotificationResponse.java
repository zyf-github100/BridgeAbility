package com.rongzhiqiao.notification.vo;

public record NotificationResponse(
        String id,
        String type,
        String title,
        String content,
        String targetRole,
        String createdAt,
        boolean read
) {
}
