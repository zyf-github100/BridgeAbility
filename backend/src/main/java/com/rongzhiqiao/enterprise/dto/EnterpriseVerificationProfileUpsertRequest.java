package com.rongzhiqiao.enterprise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EnterpriseVerificationProfileUpsertRequest {

    @NotBlank(message = "companyName is required")
    @Size(max = 128, message = "companyName must be less than 128 characters")
    private String companyName;

    @Size(max = 64, message = "industry must be less than 64 characters")
    private String industry;

    @Size(max = 64, message = "city must be less than 64 characters")
    private String city;

    @Size(max = 64, message = "unifiedSocialCreditCode must be less than 64 characters")
    private String unifiedSocialCreditCode;

    @Size(max = 64, message = "contactName must be less than 64 characters")
    private String contactName;

    @Size(max = 32, message = "contactPhone must be less than 32 characters")
    private String contactPhone;

    @Size(max = 255, message = "officeAddress must be less than 255 characters")
    private String officeAddress;

    @Size(max = 4000, message = "accessibilityCommitment must be less than 4000 characters")
    private String accessibilityCommitment;

    private boolean submitForReview;
}
