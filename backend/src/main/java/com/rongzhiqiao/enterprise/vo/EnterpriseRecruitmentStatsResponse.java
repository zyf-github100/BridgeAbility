package com.rongzhiqiao.enterprise.vo;

import java.util.List;

public record EnterpriseRecruitmentStatsResponse(
        long totalJobs,
        long publishedJobs,
        long draftJobs,
        long offlineJobs,
        long totalApplications,
        long appliedCount,
        long interviewingCount,
        long offeredCount,
        long hiredCount,
        long rejectedCount,
        long consentGrantedCount,
        int averageMatchScore,
        int averageAccessibilityCompletionRate,
        List<EnterpriseStatsBucketResponse> publishStatusBreakdown,
        List<EnterpriseStatsBucketResponse> applicationStatusBreakdown,
        List<EnterpriseStatsJobResponse> topJobs,
        List<String> insights
) {
}
