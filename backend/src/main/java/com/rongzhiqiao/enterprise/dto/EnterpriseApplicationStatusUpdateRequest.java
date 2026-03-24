package com.rongzhiqiao.enterprise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EnterpriseApplicationStatusUpdateRequest {

    @NotBlank(message = "targetStatus is required")
    private String targetStatus;

    @Size(max = 500, message = "note must be less than 500 characters")
    private String note;
}
