package com.rongzhiqiao.jobseeker.vo;

public record JobseekerServiceAuthorizationResponse(
        Long serviceOrgUserId,
        String serviceOrgAccount,
        String serviceOrgNickname,
        boolean profileAccessGranted,
        boolean sensitiveAccessGranted,
        String grantedAt
) {
}
