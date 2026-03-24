package com.rongzhiqiao.issue.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class IssueReportRequest {

    @NotBlank
    @Size(max = 32)
    private String issueType;

    @NotBlank
    @Size(max = 128)
    private String title;

    @NotBlank
    @Size(max = 1000)
    private String content;

    @Size(max = 32)
    private String relatedType;

    @Size(max = 64)
    private String relatedId;

    @Min(1)
    @Max(3)
    private Integer severityLevel;
}
