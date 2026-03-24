package com.rongzhiqiao.admin.vo;

public record TagDictionaryResponse(
        Long id,
        String tagCode,
        String tagName,
        String tagCategory,
        String tagStatus,
        String description,
        String createdAt,
        String updatedAt
) {
}
