package com.rongzhiqiao.auth.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SysUser {

    private Long id;
    private String account;
    private String passwordHash;
    private String phone;
    private String email;
    private String nickname;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
