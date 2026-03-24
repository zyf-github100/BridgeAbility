package com.rongzhiqiao.jobseeker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JobseekerInterviewSupportRequestCreateRequest {

    @NotNull(message = "applicationId is required")
    private Long applicationId;

    @NotBlank(message = "requestType is required")
    @Size(max = 32, message = "requestType must be less than 32 characters")
    private String requestType;

    @NotBlank(message = "requestContent is required")
    @Size(max = 500, message = "requestContent must be less than 500 characters")
    private String requestContent;
}
