package com.rongzhiqiao.jobseeker.vo;

public record JobseekerInterviewRecordResponse(
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
