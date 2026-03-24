package com.rongzhiqiao.auth.controller;

import com.rongzhiqiao.auth.dto.LoginRequest;
import com.rongzhiqiao.auth.dto.ResetPasswordRequest;
import com.rongzhiqiao.auth.dto.RegisterRequest;
import com.rongzhiqiao.auth.dto.SendPasswordResetCodeRequest;
import com.rongzhiqiao.auth.dto.SendEmailCodeRequest;
import com.rongzhiqiao.auth.service.AuthService;
import com.rongzhiqiao.auth.vo.AuthTokenResponse;
import com.rongzhiqiao.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/register/email-code")
    public ApiResponse<Void> sendRegisterEmailCode(@Valid @RequestBody SendEmailCodeRequest request) {
        authService.sendRegisterEmailCode(request.getEmail());
        return ApiResponse.success(null);
    }

    @PostMapping("/password-reset/email-code")
    public ApiResponse<Void> sendPasswordResetEmailCode(@Valid @RequestBody SendPasswordResetCodeRequest request) {
        authService.sendPasswordResetEmailCode(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/password-reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success(null);
    }
}
