package com.rongzhiqiao.jobseeker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JobseekerSupportNeedUpsertRequest {

    @NotBlank(message = "supportVisibility is required")
    private String supportVisibility;

    private boolean textCommunicationPreferred;

    private boolean subtitleNeeded;

    private boolean remoteInterviewPreferred;

    private boolean keyboardOnlyMode;

    private boolean highContrastNeeded;

    private boolean largeFontNeeded;

    private boolean flexibleScheduleNeeded;

    private boolean accessibleWorkspaceNeeded;

    private boolean assistiveSoftwareNeeded;

    @Size(max = 500, message = "remark must be less than 500 characters")
    private String remark;
}
