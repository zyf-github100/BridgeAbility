package com.rongzhiqiao.enterprise.vo;

import com.rongzhiqiao.catalog.vo.CatalogResponses.ScoreItem;
import java.util.List;

public record EnterpriseJobDetailResponse(
        String id,
        String title,
        String companyName,
        String department,
        String city,
        Integer salaryMin,
        Integer salaryMax,
        String salaryRange,
        Integer headcount,
        String description,
        String requirementText,
        String workMode,
        String deadline,
        String interviewMode,
        String publishStatus,
        String stage,
        int matchScore,
        int accessibilityCompletionRate,
        boolean readyToPublish,
        EnterpriseJobAccessibilityResponse accessibilityTag,
        List<ScoreItem> dimensionScores,
        List<String> reasons,
        List<String> risks,
        List<String> supports,
        List<String> environment,
        String applyHint
) {
}
