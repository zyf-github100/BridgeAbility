package com.rongzhiqiao.jobseeker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class JobseekerProfileUpsertRequest {

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String gender;
    private Integer birthYear;
    private String schoolName;
    private String major;
    private String degree;
    private Integer graduationYear;
    private String currentCity;
    private String targetCity;
    private String expectedJob;
    private BigDecimal expectedSalaryMin;
    private BigDecimal expectedSalaryMax;
    private String workModePreference;
    private String intro;
    @Valid
    private List<JobseekerSkillUpsertRequest> skillTags;
    @Valid
    private List<JobseekerProjectUpsertRequest> projectExperiences;
}
