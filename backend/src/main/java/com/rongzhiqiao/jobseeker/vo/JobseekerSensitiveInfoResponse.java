package com.rongzhiqiao.jobseeker.vo;

public record JobseekerSensitiveInfoResponse(
        boolean hasSensitiveInfo,
        String disabilityType,
        String disabilityLevel,
        String supportNeedDetail,
        String healthNote,
        String emergencyContactName,
        String emergencyContactPhone,
        String updatedAt
) {
}
