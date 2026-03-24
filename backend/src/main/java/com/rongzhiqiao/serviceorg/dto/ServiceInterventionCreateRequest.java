package com.rongzhiqiao.serviceorg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ServiceInterventionCreateRequest {

    @NotBlank
    @Size(max = 32)
    private String interventionType;

    @NotBlank
    private String content;

    @Size(max = 255)
    private String attachmentNote;

    @NotBlank
    @Size(max = 64)
    private String operatorName;
}
