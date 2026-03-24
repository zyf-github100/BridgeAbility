package com.rongzhiqiao.jobseeker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JobseekerEmploymentFollowupCreateRequest {

    @NotBlank(message = "jobId is required")
    @Size(max = 64, message = "jobId must be less than 64 characters")
    private String jobId;

    @NotBlank(message = "followupStage is required")
    @Size(max = 16, message = "followupStage must be less than 16 characters")
    private String followupStage;

    @NotNull(message = "adaptationScore is required")
    @Min(value = 0, message = "adaptationScore must be at least 0")
    @Max(value = 100, message = "adaptationScore must be at most 100")
    private Integer adaptationScore;

    @Size(max = 500, message = "environmentIssue must be less than 500 characters")
    private String environmentIssue;

    @Size(max = 500, message = "communicationIssue must be less than 500 characters")
    private String communicationIssue;

    @NotNull(message = "supportImplemented is required")
    private Boolean supportImplemented;

    @NotNull(message = "leaveRisk is required")
    private Boolean leaveRisk;

    @NotNull(message = "needHelp is required")
    private Boolean needHelp;

    @Size(max = 1000, message = "remark must be less than 1000 characters")
    private String remark;
}
