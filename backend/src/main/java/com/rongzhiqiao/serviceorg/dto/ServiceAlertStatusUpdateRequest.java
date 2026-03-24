package com.rongzhiqiao.serviceorg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ServiceAlertStatusUpdateRequest {

    @NotBlank
    @Size(max = 32)
    private String targetStatus;

    @NotBlank
    @Size(max = 255)
    private String resolutionNote;

    @NotBlank
    @Size(max = 64)
    private String operatorName;
}
