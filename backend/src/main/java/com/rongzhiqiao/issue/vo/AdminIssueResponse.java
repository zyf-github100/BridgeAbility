package com.rongzhiqiao.issue.vo;

public record AdminIssueResponse(
        String id,
        String issueType,
        String sourceRole,
        Long sourceUserId,
        String sourceName,
        String title,
        String content,
        String relatedType,
        String relatedId,
        int severityLevel,
        String ticketStatus,
        String resolutionNote,
        String handledBy,
        String handledAt,
        String createdAt
) {
}
