package com.rongzhiqiao.serviceorg.vo;

public record ServiceAlertResponse(
        String alertId,
        String caseId,
        Long userId,
        String name,
        String alertType,
        int alertLevel,
        String triggerReason,
        String createdAt,
        String alertStatus,
        String resolutionNote,
        String handledBy,
        String handledAt
) {
}
