package com.rongzhiqiao.serviceorg.controller;

import com.rongzhiqiao.common.api.ApiResponse;
import com.rongzhiqiao.common.api.PageResponse;
import com.rongzhiqiao.jobseeker.service.JobseekerPrivacyService;
import com.rongzhiqiao.jobseeker.vo.JobseekerProfileResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerSensitiveInfoResponse;
import com.rongzhiqiao.serviceorg.dto.ServiceAlertStatusUpdateRequest;
import com.rongzhiqiao.serviceorg.dto.ServiceCaseCreateRequest;
import com.rongzhiqiao.serviceorg.dto.ServiceCaseProfileAccessUpdateRequest;
import com.rongzhiqiao.serviceorg.dto.ServiceFollowupCreateRequest;
import com.rongzhiqiao.serviceorg.dto.ServiceInterventionCreateRequest;
import com.rongzhiqiao.serviceorg.dto.ServiceResourceReferralCreateRequest;
import com.rongzhiqiao.serviceorg.dto.ServiceResourceReferralStatusUpdateRequest;
import com.rongzhiqiao.serviceorg.service.ServiceOrgService;
import com.rongzhiqiao.serviceorg.vo.ServiceAlertResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceCaseDetailResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceCaseSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service")
@RequiredArgsConstructor
public class ServiceOrgController {

    private final ServiceOrgService serviceOrgService;
    private final JobseekerPrivacyService jobseekerPrivacyService;

    @GetMapping("/cases")
    public ApiResponse<List<ServiceCaseSummaryResponse>> cases() {
        return ApiResponse.success(serviceOrgService.listCases());
    }

    @PostMapping("/cases")
    public ApiResponse<ServiceCaseDetailResponse> createCase(@Valid @RequestBody ServiceCaseCreateRequest request) {
        return ApiResponse.success(serviceOrgService.createCase(request));
    }

    @GetMapping("/cases/{caseId}")
    public ApiResponse<ServiceCaseDetailResponse> caseDetail(@PathVariable String caseId) {
        return ApiResponse.success(serviceOrgService.getCaseDetail(caseId));
    }

    @GetMapping("/jobseekers/{userId}/profile")
    public ApiResponse<JobseekerProfileResponse> jobseekerProfile(@PathVariable Long userId) {
        return ApiResponse.success(jobseekerPrivacyService.getAuthorizedProfile(userId));
    }

    @GetMapping("/jobseekers/{userId}/sensitive-info")
    public ApiResponse<JobseekerSensitiveInfoResponse> jobseekerSensitiveInfo(@PathVariable Long userId) {
        return ApiResponse.success(jobseekerPrivacyService.getAuthorizedSensitiveInfo(userId));
    }

    @PostMapping("/cases/{caseId}/profile-access")
    public ApiResponse<ServiceCaseDetailResponse> updateProfileAccess(@PathVariable String caseId,
                                                                      @Valid @RequestBody ServiceCaseProfileAccessUpdateRequest request) {
        return ApiResponse.success(serviceOrgService.updateProfileAccess(caseId, request));
    }

    @PostMapping("/cases/{caseId}/interventions")
    public ApiResponse<ServiceCaseDetailResponse> addIntervention(@PathVariable String caseId,
                                                                  @Valid @RequestBody ServiceInterventionCreateRequest request) {
        return ApiResponse.success(serviceOrgService.addIntervention(caseId, request));
    }

    @PostMapping("/cases/{caseId}/followups")
    public ApiResponse<ServiceCaseDetailResponse> addFollowup(@PathVariable String caseId,
                                                              @Valid @RequestBody ServiceFollowupCreateRequest request) {
        return ApiResponse.success(serviceOrgService.addFollowup(caseId, request));
    }

    @PostMapping("/cases/{caseId}/referrals")
    public ApiResponse<ServiceCaseDetailResponse> addReferral(@PathVariable String caseId,
                                                              @Valid @RequestBody ServiceResourceReferralCreateRequest request) {
        return ApiResponse.success(serviceOrgService.addReferral(caseId, request));
    }

    @GetMapping("/alerts")
    public ApiResponse<PageResponse<ServiceAlertResponse>> alerts(@RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) Integer level,
                                                                  @RequestParam(required = false) Integer page,
                                                                  @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(serviceOrgService.listAlerts(status, level, page, pageSize));
    }

    @PostMapping("/alerts/{alertId}/status")
    public ApiResponse<ServiceAlertResponse> updateAlertStatus(@PathVariable String alertId,
                                                               @Valid @RequestBody ServiceAlertStatusUpdateRequest request) {
        return ApiResponse.success(serviceOrgService.updateAlertStatus(alertId, request));
    }

    @PostMapping("/referrals/{referralId}/status")
    public ApiResponse<ServiceCaseDetailResponse> updateReferralStatus(@PathVariable Long referralId,
                                                                       @Valid @RequestBody ServiceResourceReferralStatusUpdateRequest request) {
        return ApiResponse.success(serviceOrgService.updateReferralStatus(referralId, request));
    }
}
