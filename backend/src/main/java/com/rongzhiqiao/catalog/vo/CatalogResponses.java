package com.rongzhiqiao.catalog.vo;

import java.util.List;

public final class CatalogResponses {

    private CatalogResponses() {
    }

    public record ScoreItem(
            String label,
            int value
    ) {
    }

    public record JobResponse(
            String id,
            String title,
            String company,
            String city,
            String salaryRange,
            String workMode,
            String summary,
            String stage,
            int matchScore,
            List<ScoreItem> dimensionScores,
            List<String> reasons,
            List<String> risks,
            List<String> supports,
            List<String> description,
            List<String> requirements,
            List<String> environment,
            String applyHint
    ) {
    }

    public record CandidateResponse(
            String id,
            String name,
            String city,
            String expectedJob,
            String workMode,
            int matchScore,
            String stage,
            boolean consentGranted,
            List<String> skills,
            String summary,
            List<String> risks,
            List<String> supportSummary,
            List<String> suggestions
    ) {
    }

    public record EnterpriseProfileResponse(
            String companyName,
            String industry,
            String city,
            String verificationStatus,
            String accessibilityCommitment,
            int publishedJobCount,
            int openCandidateCount,
            int interviewCount
    ) {
    }

    public record ServiceCaseResponse(
            String id,
            String name,
            String stage,
            String owner,
            String nextAction,
            String alertLevel,
            List<String> timeline
    ) {
    }

    public record ServiceAlertResponse(
            String alertId,
            Long userId,
            String name,
            String alertType,
            int alertLevel,
            String triggerReason,
            String createdAt,
            String alertStatus
    ) {
    }

    public record AdminMetricResponse(
            String label,
            String value,
            String hint
    ) {
    }

    public record EnterpriseReviewItemResponse(
            String company,
            String industry,
            String city,
            String status,
            String note
    ) {
    }

    public record AdminDashboardResponse(
            int jobseekerCount,
            int enterpriseCount,
            int publishedJobCount,
            int applicationCount,
            int hiredCount,
            int openAlertCount,
            List<AdminMetricResponse> metrics,
            List<EnterpriseReviewItemResponse> reviewQueue,
            List<String> auditLogs
    ) {
    }

    public record KnowledgeArticleResponse(
            String id,
            String title,
            String category,
            String summary,
            List<String> tags,
            String publishDate
    ) {
    }

    public record NotificationResponse(
            String id,
            String type,
            String title,
            String content,
            String createdAt,
            boolean read
    ) {
    }

    public record MatchingStatusResponse(
            String mode,
            String version,
            List<String> dimensions,
            int ruleCount,
            int availableJobCount,
            int candidateCount
    ) {
    }
}
