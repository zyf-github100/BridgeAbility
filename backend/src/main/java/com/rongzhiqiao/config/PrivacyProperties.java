package com.rongzhiqiao.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.privacy")
public class PrivacyProperties {

    private String encryptionSecret = "change-this-privacy-encryption-secret";
}
