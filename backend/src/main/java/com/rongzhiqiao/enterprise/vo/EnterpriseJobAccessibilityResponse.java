package com.rongzhiqiao.enterprise.vo;

public record EnterpriseJobAccessibilityResponse(
        Boolean onsiteRequired,
        Boolean remoteSupported,
        Boolean highFrequencyVoiceRequired,
        Boolean noisyEnvironment,
        Boolean longStandingRequired,
        Boolean textMaterialSupported,
        Boolean onlineInterviewSupported,
        Boolean textInterviewSupported,
        Boolean flexibleScheduleSupported,
        Boolean accessibleWorkspace,
        Boolean assistiveSoftwareSupported
) {
}
