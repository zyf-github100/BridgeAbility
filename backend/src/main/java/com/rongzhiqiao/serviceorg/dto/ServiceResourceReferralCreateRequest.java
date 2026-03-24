package com.rongzhiqiao.serviceorg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ServiceResourceReferralCreateRequest {

    @NotBlank
    @Size(max = 32)
    private String referralType;

    @NotBlank
    @Size(max = 128)
    private String resourceName;

    @Size(max = 128)
    private String providerName;

    @Size(max = 64)
    private String contactName;

    @Size(max = 32)
    private String contactPhone;

    @Size(max = 32)
    private String scheduledAt;

    @Size(max = 255)
    private String statusNote;

    @NotBlank
    @Size(max = 64)
    private String operatorName;
}
