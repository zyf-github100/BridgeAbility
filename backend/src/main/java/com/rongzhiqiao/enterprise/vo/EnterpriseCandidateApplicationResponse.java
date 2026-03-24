package com.rongzhiqiao.enterprise.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rongzhiqiao.catalog.vo.CatalogResponses.ScoreItem;
import java.util.List;

public record EnterpriseCandidateApplicationResponse(
        long applicationId,
        @JsonIgnore
        Long userId,
        String jobId,
        String jobTitle,
        String companyName,
        String candidateName,
        String city,
        String expectedJob,
        String workModePreference,
        String schoolName,
        String major,
        String intro,
        int profileCompletionRate,
        int matchScore,
        String recommendationStage,
        String recommendationSummary,
        List<ScoreItem> dimensionScores,
        List<String> explanationSnapshot,
        String status,
        boolean consentGranted,
        String supportVisibility,
        String preferredInterviewMode,
        String coverNote,
        String additionalSupport,
        List<String> supportSummary,
        String submittedAt,
        EnterpriseInterviewRecordResponse latestInterview,
        List<EnterpriseInterviewRecordResponse> interviewRecords,
        List<EnterpriseSupportRequestResponse> supportRequests
) {
}
