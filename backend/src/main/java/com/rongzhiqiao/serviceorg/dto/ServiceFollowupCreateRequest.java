package com.rongzhiqiao.serviceorg.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ServiceFollowupCreateRequest {

    @Size(max = 64)
    private String jobId;

    @NotBlank
    @Size(max = 32)
    private String followupStage;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer adaptationScore;

    @Size(max = 255)
    private String environmentIssue;

    @Size(max = 255)
    private String communicationIssue;

    @NotNull
    private Boolean supportImplemented;

    @NotNull
    private Boolean leaveRisk;

    @NotNull
    private Boolean needHelp;

    @NotBlank
    @Size(max = 64)
    private String operatorName;
}
