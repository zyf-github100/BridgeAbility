package com.rongzhiqiao.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EnterpriseReviewDecisionRequest {

    @NotBlank(message = "decision is required")
    private String decision;

    @Size(max = 500, message = "note must be less than 500 characters")
    private String note;
}
