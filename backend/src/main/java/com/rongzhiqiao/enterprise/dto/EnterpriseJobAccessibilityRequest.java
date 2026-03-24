package com.rongzhiqiao.enterprise.dto;

import lombok.Data;

@Data
public class EnterpriseJobAccessibilityRequest {

    private Boolean onsiteRequired;

    private Boolean remoteSupported;

    private Boolean highFrequencyVoiceRequired;

    private Boolean noisyEnvironment;

    private Boolean longStandingRequired;

    private Boolean textMaterialSupported;

    private Boolean onlineInterviewSupported;

    private Boolean textInterviewSupported;

    private Boolean flexibleScheduleSupported;

    private Boolean accessibleWorkspace;

    private Boolean assistiveSoftwareSupported;
}
