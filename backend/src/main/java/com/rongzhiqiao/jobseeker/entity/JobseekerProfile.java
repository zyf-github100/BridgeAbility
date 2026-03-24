package com.rongzhiqiao.jobseeker.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class JobseekerProfile {

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
