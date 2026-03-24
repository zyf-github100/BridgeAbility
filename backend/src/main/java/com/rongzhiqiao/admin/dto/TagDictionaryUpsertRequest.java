package com.rongzhiqiao.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagDictionaryUpsertRequest {

    @NotBlank
    @Size(max = 64)
    private String tagCode;

    @NotBlank
    @Size(max = 64)
    private String tagName;

    @NotBlank
    @Size(max = 32)
    private String tagCategory;

    @NotBlank
    @Size(max = 16)
    private String tagStatus;

    @Size(max = 255)
    private String description;
}
