package com.rongzhiqiao.serviceorg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ServiceCaseProfileAccessUpdateRequest {

    @NotNull
    private Boolean profileAuthorized;

    @Size(max = 255)
    private String authorizationNote;

    @NotBlank
    @Size(max = 64)
    private String operatorName;
}
