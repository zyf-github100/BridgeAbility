package com.rongzhiqiao.serviceorg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ServiceCaseCreateRequest {

    @NotBlank
    @Size(max = 64)
    private String name;

    @NotBlank
    @Size(max = 64)
    private String stage;

    @NotBlank
    @Size(max = 64)
    private String ownerName;

    @NotBlank
    @Size(max = 255)
    private String nextAction;

    @Size(max = 64)
    private String jobseekerAccount;

    @Size(max = 500)
    private String intakeNote;

    @NotNull
    private Boolean profileAuthorized;

    @Size(max = 255)
    private String authorizationNote;

    @NotBlank
    @Size(max = 64)
    private String operatorName;
}
