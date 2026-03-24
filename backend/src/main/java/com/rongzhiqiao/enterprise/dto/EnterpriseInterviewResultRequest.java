package com.rongzhiqiao.enterprise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EnterpriseInterviewResultRequest {

    @NotNull(message = "applicationId is required")
    private Long applicationId;

    @NotBlank(message = "resultStatus is required")
    private String resultStatus;

    private String applicationStatus;

    @Size(max = 1000, message = "feedbackNote must be less than 1000 characters")
    private String feedbackNote;

    @Size(max = 500, message = "rejectReason must be less than 500 characters")
    private String rejectReason;
}
