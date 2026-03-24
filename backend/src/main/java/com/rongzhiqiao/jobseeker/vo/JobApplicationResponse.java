package com.rongzhiqiao.jobseeker.vo;

import java.util.List;

public record JobApplicationResponse(
        long applicationId,
        String jobId,
        String jobTitle,
        String companyName,
        String status,
        boolean consentToShareSupportNeed,
        String supportVisibility,
        String preferredInterviewMode,
        String coverNote,
        String additionalSupport,
        int matchScoreSnapshot,
        List<String> explanationSnapshot,
        JobseekerInterviewRecordResponse latestInterview,
        List<JobseekerInterviewRecordResponse> interviewRecords,
        String submittedAt
) {
}
