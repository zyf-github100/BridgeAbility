package com.rongzhiqiao.enterprise.entity;

import com.rongzhiqiao.catalog.vo.CatalogResponses.ScoreItem;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class EnterpriseJobPosting {

    private String jobId;

    private String title;

    private String companyName;

    private String department;

    private String city;

    private Integer salaryMin;

    private Integer salaryMax;

    private String salaryRange;

    private Integer headcount;

    private String descriptionText;

    private String requirementText;

    private String workMode;

    private LocalDate deadline;

    private String interviewMode;

    private String publishStatus;

    private String summary;

    private String stage;

    private Integer matchScore;

    private List<ScoreItem> dimensionScores;

    private List<String> reasons;

    private List<String> risks;

    private List<String> supports;

    private List<String> descriptionItems;

    private List<String> requirementItems;

    private List<String> environmentItems;

    private String applyHint;

    private Boolean onsiteRequired;

    private Boolean remoteSupported;

    private Boolean highFrequencyVoiceRequired;

    private Boolean noisyEnvironment;

    private Boolean longStandingRequired;

    private Boolean textMaterialSupported;

    private Boolean onlineInterviewSupported;

    private Boolean textInterviewSupported;

    private Boolean flexibleScheduleSupported;

    private Boolean accessibleWorkspace;

    private Boolean assistiveSoftwareSupported;

    private Long createdByUserId;

    private Integer sortNo;
}
