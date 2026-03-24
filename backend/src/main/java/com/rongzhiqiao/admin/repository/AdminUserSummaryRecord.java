package com.rongzhiqiao.admin.repository;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminUserSummaryRecord {

    private Long userId;
    private String account;
    private String nickname;
    private String email;
    private String phone;
    private Integer status;
    private String roleCodes;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
