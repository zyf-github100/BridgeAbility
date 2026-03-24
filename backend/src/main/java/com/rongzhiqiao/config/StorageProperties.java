package com.rongzhiqiao.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String provider = "auto";
    private String localRoot = "uploads";
    private final R2 r2 = new R2();

    @Data
    public static class R2 {

        private String endpoint;
        private String bucket;
        private String accessKeyId;
        private String secretAccessKey;
        private String region = "auto";

        public boolean isConfigured() {
            return hasText(endpoint)
                    && hasText(bucket)
                    && hasText(accessKeyId)
                    && hasText(secretAccessKey);
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
