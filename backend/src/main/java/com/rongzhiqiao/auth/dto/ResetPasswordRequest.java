package com.rongzhiqiao.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "账号不能为空")
    private String account;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "邮箱验证码不能为空")
    private String emailVerificationCode;

    @Size(min = 8, max = 20, message = "密码长度需为 8-20 位")
    @NotBlank(message = "密码不能为空")
    private String password;
}
