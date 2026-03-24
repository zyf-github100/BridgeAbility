package com.rongzhiqiao.serviceorg.vo;

import java.util.List;

public record ServiceCaseDetailResponse(
        String id,
        String name,
        String stage,
        String owner,
        String nextAction,
        String alertLevel,
        String intakeNote,
        List<String> timeline,
        ServiceProfileAccessResponse profileAccess,
        List<ServiceInterventionResponse> interventions,
        List<ServiceFollowupResponse> followups,
        List<ServiceAlertResponse> alerts,
        List<ServiceResourceReferralResponse> referrals
) {
}
