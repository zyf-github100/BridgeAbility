package com.rongzhiqiao.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.auth.verification")
public class AuthVerificationProperties {

    private boolean requireEmailCode = true;
    private int codeLength = 6;
    private int expireMinutes = 5;
    private int cooldownSeconds = 60;
    private int maxAttempts = 5;
    private String subject = "[BridgeAbility] Verification code";
    private String fromAddress;
    private String fromName = "BridgeAbility";
}
