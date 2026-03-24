package com.rongzhiqiao.jobseeker.vo;

public record JobseekerProjectExperienceResponse(
        Long id,
        String projectName,
        String roleName,
        String description,
        String startDate,
        String endDate,
        String periodLabel
) {
}
