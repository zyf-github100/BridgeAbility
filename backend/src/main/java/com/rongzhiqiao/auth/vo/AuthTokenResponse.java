package com.rongzhiqiao.auth.vo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthTokenResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresInSeconds;
    private Long userId;
    private String account;
    private String nickname;
    private List<String> roles;
}
