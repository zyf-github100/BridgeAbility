package com.rongzhiqiao.jobseeker.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JobseekerSensitiveInfoUpsertRequest {

    @Size(max = 64)
    private String disabilityType;

    @Size(max = 64)
    private String disabilityLevel;

    @Size(max = 2000)
    private String supportNeedDetail;

    @Size(max = 2000)
    private String healthNote;

    @Size(max = 128)
    private String emergencyContactName;

    @Size(max = 32)
    private String emergencyContactPhone;
}
