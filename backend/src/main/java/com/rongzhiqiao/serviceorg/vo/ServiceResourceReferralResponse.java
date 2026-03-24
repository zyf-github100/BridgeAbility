package com.rongzhiqiao.serviceorg.vo;

public record ServiceResourceReferralResponse(
        long id,
        String caseId,
        String referralType,
        String resourceName,
        String providerName,
        String contactName,
        String contactPhone,
        String scheduledAt,
        String referralStatus,
        String statusNote,
        String operatorName,
        String createdAt,
        String updatedAt
) {
}
