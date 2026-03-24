package com.rongzhiqiao.admin.vo;

public record EnterpriseReviewItemResponse(
        Long userId,
        String company,
        String industry,
        String city,
        String status,
        String note,
        String submittedAt,
        int materialCount
) {
}
