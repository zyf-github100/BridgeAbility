package com.rongzhiqiao.admin.service;

import com.rongzhiqiao.admin.repository.AdminUserManagementMapper;
import com.rongzhiqiao.admin.repository.AdminUserSummaryRecord;
import com.rongzhiqiao.admin.vo.AdminUserSummaryResponse;
import com.rongzhiqiao.common.security.SecurityUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserManagementService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AdminUserManagementMapper adminUserManagementMapper;

    public List<AdminUserSummaryResponse> listUsers() {
        SecurityUtils.requireAuthority("ROLE_ADMIN");
        return adminUserManagementMapper.selectAllUsers().stream()
                .map(this::toResponse)
                .toList();
    }

    private AdminUserSummaryResponse toResponse(AdminUserSummaryRecord record) {
        List<String> roles = Arrays.stream((record.getRoleCodes() == null ? "" : record.getRoleCodes()).split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        return new AdminUserSummaryResponse(
                record.getUserId(),
                emptyToNull(record.getAccount()),
                emptyToNull(record.getNickname()),
                emptyToNull(record.getEmail()),
                emptyToNull(record.getPhone()),
                record.getStatus(),
                getStatusLabel(record.getStatus()),
                roles,
                formatDateTime(record.getLastLoginAt()),
                formatDateTime(record.getCreatedAt())
        );
    }

    private String getStatusLabel(Integer status) {
        return status != null && status == 1 ? "ACTIVE" : "DISABLED";
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
