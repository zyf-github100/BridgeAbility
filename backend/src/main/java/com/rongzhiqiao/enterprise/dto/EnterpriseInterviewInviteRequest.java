package com.rongzhiqiao.enterprise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EnterpriseInterviewInviteRequest {

    @NotNull(message = "applicationId is required")
    private Long applicationId;

    @NotBlank(message = "interviewTime is required")
    private String interviewTime;

    @NotBlank(message = "interviewMode is required")
    private String interviewMode;

    @NotBlank(message = "interviewerName is required")
    @Size(max = 64, message = "interviewerName must be less than 64 characters")
    private String interviewerName;

    @Size(max = 1000, message = "note must be less than 1000 characters")
    private String note;
}
