package com.rongzhiqiao.serviceorg.vo;

public record ServiceInterventionResponse(
        Long id,
        String caseId,
        String interventionType,
        String content,
        String attachmentNote,
        String operatorName,
        String createdAt
) {
}
