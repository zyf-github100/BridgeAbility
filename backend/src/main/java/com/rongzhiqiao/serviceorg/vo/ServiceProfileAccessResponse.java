package com.rongzhiqiao.serviceorg.vo;

import com.rongzhiqiao.jobseeker.vo.JobseekerProfileResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerSupportNeedResponse;

public record ServiceProfileAccessResponse(
        boolean linkedJobseeker,
        boolean profileAuthorized,
        String linkedAccount,
        String linkedDisplayName,
        String authorizationNote,
        String authorizationUpdatedBy,
        String authorizationUpdatedAt,
        JobseekerProfileResponse jobseekerProfile,
        JobseekerSupportNeedResponse supportNeeds
) {
}
