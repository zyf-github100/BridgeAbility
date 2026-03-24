package com.rongzhiqiao.enterprise.vo;

public record EnterpriseSupportRequestResponse(
        String requestType,
        String requestTypeLabel,
        String requestContent,
        String requestSource,
        String requestStatus
) {
}
