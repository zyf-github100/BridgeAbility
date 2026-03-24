package com.rongzhiqiao.auth.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SysRole {

    private Long id;
    private String roleCode;
    private String roleName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
