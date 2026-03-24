package com.rongzhiqiao.serviceorg.vo;

import java.util.List;

public record ServiceCaseSummaryResponse(
        String id,
        String name,
        String stage,
        String owner,
        String nextAction,
        String alertLevel,
        List<String> timeline,
        boolean profileAuthorized,
        int pendingAlertCount,
        int followupCount,
        int referralCount
) {
}
