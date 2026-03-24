package com.rongzhiqiao.enterprise.vo;

import java.util.List;

public record EnterpriseVerificationProfileResponse(
        Long userId,
        String companyName,
        String industry,
        String city,
        String unifiedSocialCreditCode,
        String contactName,
        String contactPhone,
        String officeAddress,
        String accessibilityCommitment,
        String verificationStatus,
        String reviewNote,
        String submittedAt,
        String reviewedAt,
        boolean canPublishJobs,
        int publishedJobCount,
        List<EnterpriseVerificationMaterialResponse> materials
) {
}
