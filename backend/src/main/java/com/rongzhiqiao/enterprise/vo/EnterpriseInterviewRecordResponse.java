package com.rongzhiqiao.enterprise.vo;

public record EnterpriseInterviewRecordResponse(
        long interviewId,
        String interviewTime,
        String interviewMode,
        String interviewerName,
        String inviteNote,
        String resultStatus,
        String feedbackNote,
        String rejectReason,
        String createdAt,
        String updatedAt
) {
}
