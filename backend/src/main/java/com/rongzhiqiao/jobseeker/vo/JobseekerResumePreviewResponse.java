package com.rongzhiqiao.jobseeker.vo;

import java.util.List;

public record JobseekerResumePreviewResponse(
        String displayName,
        String headline,
        String summary,
        int profileCompletionRate,
        JobseekerProfileResponse profile,
        JobseekerSupportNeedResponse supportNeeds,
        List<JobApplicationResponse> recentApplications,
        List<String> strengths,
        List<String> suggestions,
        String exportFileName,
        String generatedAt
) {
}
