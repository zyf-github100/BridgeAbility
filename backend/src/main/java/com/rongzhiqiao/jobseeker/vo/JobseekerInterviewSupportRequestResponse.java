package com.rongzhiqiao.jobseeker.vo;

public record JobseekerInterviewSupportRequestResponse(
        long id,
        long applicationId,
        String requestType,
        String requestTypeLabel,
        String requestContent,
        String requestStatus,
        String createdAt,
        String updatedAt
) {
}
