package com.rongzhiqiao.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SystemAnnouncementRequest {

    @NotBlank
    @Size(max = 128)
    private String title;

    @NotBlank
    @Size(max = 500)
    private String content;

    @NotBlank
    @Size(max = 32)
    private String targetRole;
}
