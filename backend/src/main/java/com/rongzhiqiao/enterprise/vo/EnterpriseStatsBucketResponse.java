package com.rongzhiqiao.enterprise.vo;

public record EnterpriseStatsBucketResponse(
        String code,
        String label,
        long value,
        String hint
) {
}
