package com.rongzhiqiao.jobseeker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplyJobRequest {

    @NotBlank(message = "coverNote is required")
    @Size(min = 24, max = 2000, message = "coverNote must be between 24 and 2000 characters")
    private String coverNote;

    @NotBlank(message = "preferredInterviewMode is required")
    private String preferredInterviewMode;

    private String supportVisibility;

    @Size(max = 1000, message = "additionalSupport must be less than 1000 characters")
    private String additionalSupport;
}
