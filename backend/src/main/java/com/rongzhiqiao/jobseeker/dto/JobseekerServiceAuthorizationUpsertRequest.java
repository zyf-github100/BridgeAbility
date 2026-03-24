package com.rongzhiqiao.jobseeker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobseekerServiceAuthorizationUpsertRequest {

    @NotBlank
    private String serviceOrgAccount;

    private Boolean profileAccessGranted;

    private Boolean sensitiveAccessGranted;
}
