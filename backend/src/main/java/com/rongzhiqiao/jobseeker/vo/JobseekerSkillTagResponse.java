package com.rongzhiqiao.jobseeker.vo;

public record JobseekerSkillTagResponse(
        String skillCode,
        String skillName,
        int skillLevel,
        String skillLevelLabel
) {
}
