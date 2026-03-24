package com.rongzhiqiao.jobseeker.controller;

import com.rongzhiqiao.catalog.vo.CatalogResponses.JobResponse;
import com.rongzhiqiao.common.api.ApiResponse;
import com.rongzhiqiao.common.api.PageResponse;
import com.rongzhiqiao.jobseeker.dto.ApplyJobRequest;
import com.rongzhiqiao.jobseeker.dto.JobseekerEmploymentFollowupCreateRequest;
import com.rongzhiqiao.jobseeker.dto.JobseekerInterviewSupportRequestCreateRequest;
import com.rongzhiqiao.jobseeker.dto.JobseekerProfileUpsertRequest;
import com.rongzhiqiao.jobseeker.dto.JobseekerSensitiveInfoUpsertRequest;
import com.rongzhiqiao.jobseeker.dto.JobseekerServiceAuthorizationUpsertRequest;
import com.rongzhiqiao.jobseeker.dto.JobseekerSupportNeedUpsertRequest;
import com.rongzhiqiao.jobseeker.service.JobApplicationService;
import com.rongzhiqiao.jobseeker.service.JobseekerEmploymentFollowupService;
import com.rongzhiqiao.jobseeker.service.JobseekerInterviewSupportRequestService;
import com.rongzhiqiao.jobseeker.service.JobseekerPrivacyService;
import com.rongzhiqiao.jobseeker.service.JobseekerProfileService;
import com.rongzhiqiao.jobseeker.service.JobseekerResumeExporter;
import com.rongzhiqiao.jobseeker.service.JobseekerResumeService;
import com.rongzhiqiao.jobseeker.service.JobseekerServiceRecordService;
import com.rongzhiqiao.jobseeker.service.JobseekerSupportNeedService;
import com.rongzhiqiao.matching.service.JobRecommendationService;
import com.rongzhiqiao.jobseeker.vo.JobApplicationResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerEmploymentFollowupResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerInterviewSupportRequestResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerProfileResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerResumePreviewResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerSensitiveInfoResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerServiceAuthorizationResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerSupportNeedResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceCaseDetailResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceCaseSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobseeker")
@RequiredArgsConstructor
public class JobseekerController {

    private final JobseekerProfileService jobseekerProfileService;
    private final JobseekerSupportNeedService jobseekerSupportNeedService;
    private final JobRecommendationService jobRecommendationService;
    private final JobApplicationService jobApplicationService;
    private final JobseekerResumeService jobseekerResumeService;
    private final JobseekerInterviewSupportRequestService jobseekerInterviewSupportRequestService;
    private final JobseekerEmploymentFollowupService jobseekerEmploymentFollowupService;
    private final JobseekerServiceRecordService jobseekerServiceRecordService;
    private final JobseekerPrivacyService jobseekerPrivacyService;

    @GetMapping("/profile")
    public ApiResponse<JobseekerProfileResponse> profile() {
        return ApiResponse.success(jobseekerProfileService.getCurrentProfile());
    }

    @PutMapping("/profile")
    public ApiResponse<JobseekerProfileResponse> updateProfile(@Valid @RequestBody JobseekerProfileUpsertRequest request) {
        return ApiResponse.success(jobseekerProfileService.saveCurrentProfile(request));
    }

    @GetMapping("/support-needs")
    public ApiResponse<JobseekerSupportNeedResponse> supportNeeds() {
        return ApiResponse.success(jobseekerSupportNeedService.getCurrentSupportNeeds());
    }

    @PutMapping("/support-needs")
    public ApiResponse<JobseekerSupportNeedResponse> updateSupportNeeds(@Valid @RequestBody JobseekerSupportNeedUpsertRequest request) {
        return ApiResponse.success(jobseekerSupportNeedService.saveCurrentSupportNeeds(request));
    }

    @GetMapping("/sensitive-info")
    public ApiResponse<JobseekerSensitiveInfoResponse> sensitiveInfo() {
        return ApiResponse.success(jobseekerPrivacyService.getCurrentSensitiveInfo());
    }

    @PutMapping("/sensitive-info")
    public ApiResponse<JobseekerSensitiveInfoResponse> updateSensitiveInfo(
            @Valid @RequestBody JobseekerSensitiveInfoUpsertRequest request) {
        return ApiResponse.success(jobseekerPrivacyService.saveCurrentSensitiveInfo(request));
    }

    @GetMapping("/service-authorizations")
    public ApiResponse<List<JobseekerServiceAuthorizationResponse>> serviceAuthorizations() {
        return ApiResponse.success(jobseekerPrivacyService.listCurrentServiceAuthorizations());
    }

    @PutMapping("/service-authorizations")
    public ApiResponse<JobseekerServiceAuthorizationResponse> updateServiceAuthorization(
            @Valid @RequestBody JobseekerServiceAuthorizationUpsertRequest request) {
        return ApiResponse.success(jobseekerPrivacyService.saveCurrentServiceAuthorization(request));
    }

    @GetMapping("/resume-preview")
    public ApiResponse<JobseekerResumePreviewResponse> resumePreview() {
        return ApiResponse.success(jobseekerResumeService.getCurrentResumePreview());
    }

    @GetMapping("/resume-export")
    public ResponseEntity<ByteArrayResource> resumeExport(@RequestParam(defaultValue = "pdf") String format) {
        JobseekerResumeExporter.ResumeExportPayload exportPayload = jobseekerResumeService.exportCurrentResume(format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportPayload.fileName() + "\"")
                .contentType(MediaType.parseMediaType(exportPayload.contentType()))
                .contentLength(exportPayload.content().length)
                .body(new ByteArrayResource(exportPayload.content()));
    }

    @GetMapping("/recommend-jobs")
    public ApiResponse<PageResponse<JobResponse>> recommendJobs(@RequestParam(required = false) Integer page,
                                                                @RequestParam(required = false) Integer pageSize,
                                                                @RequestParam(required = false) String city,
                                                                @RequestParam(required = false) String workMode,
                                                                @RequestParam(required = false) String keyword) {
        return ApiResponse.success(jobRecommendationService.listRecommendedJobs(page, pageSize, city, workMode, keyword));
    }

    @GetMapping("/recommend-jobs/{jobId}")
    public ApiResponse<JobResponse> recommendJobDetail(@PathVariable String jobId) {
        return ApiResponse.success(jobRecommendationService.getPublishedJobForCurrentUser(jobId));
    }

    @PostMapping("/apply/{jobId}")
    public ApiResponse<JobApplicationResponse> applyJob(@PathVariable String jobId,
                                                        @Valid @RequestBody ApplyJobRequest request) {
        return ApiResponse.success(jobApplicationService.applyCurrentUser(jobId, request));
    }

    @GetMapping("/applications")
    public ApiResponse<List<JobApplicationResponse>> applications() {
        return ApiResponse.success(jobApplicationService.listCurrentUserApplications());
    }

    @GetMapping("/interview-support-requests")
    public ApiResponse<List<JobseekerInterviewSupportRequestResponse>> interviewSupportRequests() {
        return ApiResponse.success(jobseekerInterviewSupportRequestService.listCurrentUserRequests());
    }

    @PostMapping("/interview-support-request")
    public ApiResponse<JobseekerInterviewSupportRequestResponse> createInterviewSupportRequest(
            @Valid @RequestBody JobseekerInterviewSupportRequestCreateRequest request) {
        return ApiResponse.success(jobseekerInterviewSupportRequestService.createCurrentUserRequest(request));
    }

    @GetMapping("/service-records")
    public ApiResponse<List<ServiceCaseSummaryResponse>> serviceRecords() {
        return ApiResponse.success(jobseekerServiceRecordService.listCurrentUserCases());
    }

    @GetMapping("/service-records/{caseId}")
    public ApiResponse<ServiceCaseDetailResponse> serviceRecordDetail(@PathVariable String caseId) {
        return ApiResponse.success(jobseekerServiceRecordService.getCurrentUserCaseDetail(caseId));
    }

    @GetMapping("/followups")
    public ApiResponse<List<JobseekerEmploymentFollowupResponse>> followups() {
        return ApiResponse.success(jobseekerEmploymentFollowupService.listCurrentUserFollowups());
    }

    @PostMapping("/followup")
    public ApiResponse<JobseekerEmploymentFollowupResponse> createEmploymentFollowup(
            @Valid @RequestBody JobseekerEmploymentFollowupCreateRequest request) {
        return ApiResponse.success(jobseekerEmploymentFollowupService.createCurrentUserFollowup(request));
    }
}
