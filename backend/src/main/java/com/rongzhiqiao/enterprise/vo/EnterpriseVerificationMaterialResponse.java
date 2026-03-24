package com.rongzhiqiao.enterprise.vo;

public record EnterpriseVerificationMaterialResponse(
        Long id,
        String materialType,
        String materialTypeLabel,
        String originalFileName,
        String contentType,
        long fileSize,
        String note,
        String uploadedAt
) {
}
