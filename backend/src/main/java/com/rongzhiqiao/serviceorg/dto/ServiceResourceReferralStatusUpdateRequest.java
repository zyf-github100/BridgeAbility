package com.rongzhiqiao.serviceorg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ServiceResourceReferralStatusUpdateRequest {

    @NotBlank
    @Size(max = 32)
    private String targetStatus;

    @Size(max = 255)
    private String statusNote;

    @NotBlank
    @Size(max = 64)
    private String operatorName;
}
