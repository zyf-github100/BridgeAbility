package com.rongzhiqiao.jobseeker.vo;

import java.util.List;

public record InterviewCommunicationCardResponse(
        String title,
        String subtitle,
        List<String> lines,
        String copyText
) {
}
