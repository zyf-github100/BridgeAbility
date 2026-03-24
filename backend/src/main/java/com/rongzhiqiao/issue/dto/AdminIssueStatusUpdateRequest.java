package com.rongzhiqiao.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminIssueStatusUpdateRequest {

    @NotBlank
    @Size(max = 32)
    private String targetStatus;

    @NotBlank
    @Size(max = 500)
    private String resolutionNote;

    @NotBlank
    @Size(max = 64)
    private String handlerName;
}
