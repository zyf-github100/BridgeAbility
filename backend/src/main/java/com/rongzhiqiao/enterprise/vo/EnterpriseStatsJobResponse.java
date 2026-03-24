package com.rongzhiqiao.enterprise.vo;

public record EnterpriseStatsJobResponse(
        String jobId,
        String title,
        String publishStatus,
        String stage,
        int accessibilityCompletionRate,
        long candidateCount,
        long interviewingCount,
        long hiredCount,
        int averageMatchScore
) {
}
