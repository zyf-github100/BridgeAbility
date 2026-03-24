package com.rongzhiqiao.enterprise.controller;

import com.rongzhiqiao.common.api.ApiResponse;
import com.rongzhiqiao.common.api.PageResponse;
import com.rongzhiqiao.enterprise.dto.EnterpriseApplicationStatusUpdateRequest;
import com.rongzhiqiao.enterprise.dto.EnterpriseInterviewInviteRequest;
import com.rongzhiqiao.enterprise.dto.EnterpriseInterviewResultRequest;
import com.rongzhiqiao.enterprise.dto.EnterpriseJobUpsertRequest;
import com.rongzhiqiao.enterprise.dto.EnterpriseVerificationProfileUpsertRequest;
import com.rongzhiqiao.enterprise.service.EnterpriseCandidateService;
import com.rongzhiqiao.enterprise.service.EnterpriseInterviewService;
import com.rongzhiqiao.enterprise.service.EnterpriseJobService;
import com.rongzhiqiao.enterprise.service.EnterpriseRecruitmentStatsService;
import com.rongzhiqiao.enterprise.service.EnterpriseVerificationService;
import com.rongzhiqiao.enterprise.vo.EnterpriseCandidateApplicationResponse;
import com.rongzhiqiao.enterprise.vo.EnterpriseJobDetailResponse;
import com.rongzhiqiao.enterprise.vo.EnterpriseRecruitmentStatsResponse;
import com.rongzhiqiao.enterprise.vo.EnterpriseJobSummaryResponse;
import com.rongzhiqiao.enterprise.vo.EnterpriseVerificationMaterialResponse;
import com.rongzhiqiao.enterprise.vo.EnterpriseVerificationProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseCandidateService enterpriseCandidateService;
    private final EnterpriseInterviewService enterpriseInterviewService;
    private final EnterpriseJobService enterpriseJobService;
    private final EnterpriseRecruitmentStatsService enterpriseRecruitmentStatsService;
    private final EnterpriseVerificationService enterpriseVerificationService;

    @GetMapping("/profile")
    public ApiResponse<EnterpriseVerificationProfileResponse> profile() {
        return ApiResponse.success(enterpriseVerificationService.getCurrentProfile());
    }

    @PutMapping("/profile")
    public ApiResponse<EnterpriseVerificationProfileResponse> updateProfile(
            @Valid @RequestBody EnterpriseVerificationProfileUpsertRequest request) {
        return ApiResponse.success(enterpriseVerificationService.saveCurrentProfile(request));
    }

    @PostMapping(value = "/profile/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EnterpriseVerificationMaterialResponse> uploadMaterial(@RequestParam String materialType,
                                                                             @RequestParam(required = false) String note,
                                                                             @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(enterpriseVerificationService.uploadCurrentMaterial(materialType, note, file));
    }

    @DeleteMapping("/profile/materials/{materialId}")
    public ApiResponse<Boolean> deleteMaterial(@PathVariable Long materialId) {
        enterpriseVerificationService.deleteCurrentMaterial(materialId);
        return ApiResponse.success(Boolean.TRUE);
    }

    @GetMapping("/profile/materials/{materialId}/download")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long materialId) {
        EnterpriseVerificationService.StoredMaterial material = enterpriseVerificationService.loadCurrentMaterial(materialId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.originalFileName() + "\"")
                .contentType(resolveMediaType(material.contentType()))
                .body(material.resource());
    }

    @GetMapping("/jobs")
    public ApiResponse<PageResponse<EnterpriseJobSummaryResponse>> jobs(@RequestParam(required = false) Integer page,
                                                                        @RequestParam(required = false) Integer pageSize,
                                                                        @RequestParam(required = false) String city,
                                                                        @RequestParam(required = false) String workMode,
                                                                        @RequestParam(required = false) String publishStatus) {
        return ApiResponse.success(enterpriseJobService.listJobs(page, pageSize, city, workMode, publishStatus));
    }

    @GetMapping("/stats")
    public ApiResponse<EnterpriseRecruitmentStatsResponse> stats() {
        return ApiResponse.success(enterpriseRecruitmentStatsService.getCurrentStats());
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<EnterpriseJobDetailResponse> jobDetail(@PathVariable String jobId) {
        return ApiResponse.success(enterpriseJobService.getJob(jobId));
    }

    @PostMapping("/jobs")
    public ApiResponse<EnterpriseJobDetailResponse> createJob(@Valid @RequestBody EnterpriseJobUpsertRequest request) {
        return ApiResponse.success(enterpriseJobService.createJob(request));
    }

    @PutMapping("/jobs/{jobId}")
    public ApiResponse<EnterpriseJobDetailResponse> updateJob(@PathVariable String jobId,
                                                              @Valid @RequestBody EnterpriseJobUpsertRequest request) {
        return ApiResponse.success(enterpriseJobService.updateJob(jobId, request));
    }

    @PostMapping("/jobs/{jobId}/offline")
    public ApiResponse<EnterpriseJobDetailResponse> offlineJob(@PathVariable String jobId) {
        return ApiResponse.success(enterpriseJobService.offlineJob(jobId));
    }

    @GetMapping("/jobs/{jobId}/candidates")
    public ApiResponse<PageResponse<EnterpriseCandidateApplicationResponse>> candidates(@PathVariable String jobId,
                                                                                        @RequestParam(required = false) Integer page,
                                                                                        @RequestParam(required = false) Integer pageSize,
                                                                                        @RequestParam(required = false) Boolean consentGranted) {
        return ApiResponse.success(enterpriseCandidateService.listCandidates(jobId, consentGranted, page, pageSize));
    }

    @PostMapping("/applications/{applicationId}/status")
    public ApiResponse<EnterpriseCandidateApplicationResponse> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody EnterpriseApplicationStatusUpdateRequest request) {
        return ApiResponse.success(enterpriseCandidateService.updateApplicationStatus(applicationId, request));
    }

    @PostMapping("/interview/invite")
    public ApiResponse<EnterpriseCandidateApplicationResponse> inviteInterview(
            @Valid @RequestBody EnterpriseInterviewInviteRequest request) {
        return ApiResponse.success(enterpriseInterviewService.invite(request));
    }

    @PostMapping("/interview/result")
    public ApiResponse<EnterpriseCandidateApplicationResponse> submitInterviewResult(
            @Valid @RequestBody EnterpriseInterviewResultRequest request) {
        return ApiResponse.success(enterpriseInterviewService.submitResult(request));
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
