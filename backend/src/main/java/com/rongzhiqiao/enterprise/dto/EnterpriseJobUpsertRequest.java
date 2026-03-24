package com.rongzhiqiao.enterprise.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class EnterpriseJobUpsertRequest {

    @NotBlank
    @Size(max = 128)
    private String title;

    @NotBlank
    @Size(max = 128)
    private String department;

    @NotBlank
    @Size(max = 64)
    private String city;

    @NotNull
    @Min(1000)
    private Integer salaryMin;

    @NotNull
    @Min(1000)
    private Integer salaryMax;

    @NotNull
    @Min(1)
    @Max(999)
    private Integer headcount;

    @NotBlank
    private String description;

    @NotBlank
    private String requirementText;

    @NotBlank
    @Size(max = 32)
    private String workMode;

    @NotBlank
    @Size(max = 32)
    private String interviewMode;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    @NotBlank
    @Size(max = 32)
    private String publishStatus;

    private EnterpriseJobAccessibilityRequest accessibilityTag;
}
