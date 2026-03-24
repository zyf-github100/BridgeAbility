package com.rongzhiqiao.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendPasswordResetCodeRequest {

    @NotBlank(message = "账号不能为空")
    private String account;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;
}
