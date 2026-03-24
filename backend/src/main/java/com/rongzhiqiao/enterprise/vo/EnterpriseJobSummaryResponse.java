package com.rongzhiqiao.enterprise.vo;

public record EnterpriseJobSummaryResponse(
        String id,
        String title,
        String department,
        String city,
        String workMode,
        String salaryRange,
        int headcount,
        String deadline,
        String publishStatus,
        String stage,
        int matchScore,
        int accessibilityCompletionRate,
        boolean readyToPublish,
        long candidateCount
) {
}
