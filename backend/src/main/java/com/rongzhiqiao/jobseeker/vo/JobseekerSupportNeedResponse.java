package com.rongzhiqiao.jobseeker.vo;

import java.util.List;

public record JobseekerSupportNeedResponse(
        String supportVisibility,
        boolean consentToShareSupportNeed,
        boolean hasAnyNeed,
        boolean textCommunicationPreferred,
        boolean subtitleNeeded,
        boolean remoteInterviewPreferred,
        boolean keyboardOnlyMode,
        boolean highContrastNeeded,
        boolean largeFontNeeded,
        boolean flexibleScheduleNeeded,
        boolean accessibleWorkspaceNeeded,
        boolean assistiveSoftwareNeeded,
        String remark,
        List<String> supportSummary,
        String summaryText,
        InterviewCommunicationCardResponse interviewCommunicationCard,
        String updatedAt
) {
}
