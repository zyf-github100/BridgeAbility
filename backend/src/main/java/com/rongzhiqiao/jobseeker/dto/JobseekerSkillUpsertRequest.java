package com.rongzhiqiao.jobseeker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JobseekerSkillUpsertRequest {

    @Size(max = 64, message = "skillCode must be less than 64 characters")
    private String skillCode;

    @NotBlank(message = "skillName is required")
    @Size(max = 64, message = "skillName must be less than 64 characters")
    private String skillName;

    @Min(value = 1, message = "skillLevel must be between 1 and 5")
    @Max(value = 5, message = "skillLevel must be between 1 and 5")
    private Integer skillLevel;
}
