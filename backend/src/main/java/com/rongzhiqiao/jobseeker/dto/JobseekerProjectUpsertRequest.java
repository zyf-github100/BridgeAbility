package com.rongzhiqiao.jobseeker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class JobseekerProjectUpsertRequest {

    @NotBlank(message = "projectName is required")
    @Size(max = 128, message = "projectName must be less than 128 characters")
    private String projectName;

    @Size(max = 64, message = "roleName must be less than 64 characters")
    private String roleName;

    @Size(max = 2000, message = "description must be less than 2000 characters")
    private String description;

    private LocalDate startDate;

    private LocalDate endDate;
}
