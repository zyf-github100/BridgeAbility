package com.rongzhiqiao.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String issuer;
    private String secret;
    private long accessTokenExpireMinutes;
}
