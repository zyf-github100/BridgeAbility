package com.rongzhiqiao.jobseeker.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobseekerProfileResponse {

    private Long id;
    private Long userId;
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
    private Integer profileCompletionRate;
    private List<JobseekerSkillTagResponse> skillTags;
    private List<JobseekerProjectExperienceResponse> projectExperiences;
    private List<JobseekerAbilityCardResponse> abilityCards;
}
