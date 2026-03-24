package com.rongzhiqiao.serviceorg.vo;

public record ServiceFollowupResponse(
        Long id,
        String caseId,
        String jobId,
        String followupStage,
        int adaptationScore,
        String environmentIssue,
        String communicationIssue,
        boolean supportImplemented,
        boolean leaveRisk,
        boolean needHelp,
        String recordStatus,
        String operatorName,
        String dueAt,
        String createdAt,
        String completedAt
) {
}
