package com.rongzhiqiao.jobseeker.vo;

public record JobseekerEmploymentFollowupResponse(
        long id,
        String jobId,
        String followupStage,
        int adaptationScore,
        String environmentIssue,
        String communicationIssue,
        boolean supportImplemented,
        boolean leaveRisk,
        boolean needHelp,
        String remark,
        String createdAt,
        String updatedAt
) {
}
