package com.rongzhiqiao.admin.vo;

import java.util.List;

public record AdminUserSummaryResponse(
        Long userId,
        String account,
        String nickname,
        String email,
        String phone,
        Integer status,
        String statusLabel,
        List<String> roles,
        String lastLoginAt,
        String createdAt
) {
}
