package com.rongzhiqiao.jobseeker.vo;

import java.util.List;

public record JobseekerAbilityCardResponse(
        String code,
        String title,
        String summary,
        List<String> highlights
) {
}
