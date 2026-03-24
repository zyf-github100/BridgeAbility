package com.rongzhiqiao.admin.controller;

import com.rongzhiqiao.admin.dto.EnterpriseReviewDecisionRequest;
import com.rongzhiqiao.admin.dto.MatchingConfigUpsertRequest;
import com.rongzhiqiao.admin.service.AdminUserManagementService;
import com.rongzhiqiao.admin.vo.AdminUserSummaryResponse;
import com.rongzhiqiao.admin.vo.EnterpriseReviewItemResponse;
import com.rongzhiqiao.catalog.service.PlatformCatalogService;
import com.rongzhiqiao.catalog.vo.CatalogResponses.AdminDashboardResponse;
import com.rongzhiqiao.common.api.ApiResponse;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.enterprise.service.EnterpriseVerificationService;
import com.rongzhiqiao.enterprise.vo.EnterpriseVerificationProfileResponse;
import com.rongzhiqiao.matching.service.MatchingConfigService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PlatformCatalogService platformCatalogService;
    private final EnterpriseVerificationService enterpriseVerificationService;
    private final AdminUserManagementService adminUserManagementService;
    private final MatchingConfigService matchingConfigService;

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> dashboard() {
        return ApiResponse.success(platformCatalogService.getAdminDashboard());
    }

    @GetMapping("/enterprises/pending")
    public ApiResponse<List<EnterpriseReviewItemResponse>> pendingEnterprises() {
        List<EnterpriseReviewItemResponse> pending = enterpriseVerificationService.listPendingReviews();
        if (!pending.isEmpty()) {
            return ApiResponse.success(pending);
        }
        return ApiResponse.success(platformCatalogService.listPendingEnterpriseReviews().stream()
                .map(item -> new EnterpriseReviewItemResponse(
                        null,
                        item.company(),
                        item.industry(),
                        item.city(),
                        item.status(),
                        item.note(),
                        "",
                        0
                ))
                .toList());
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<String>> auditLogs() {
        List<String> logs = enterpriseVerificationService.listAuditLogs();
        return ApiResponse.success(logs.isEmpty() ? platformCatalogService.listAuditLogs() : logs);
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserSummaryResponse>> users() {
        return ApiResponse.success(adminUserManagementService.listUsers());
    }

    @GetMapping("/matching-config")
    public ApiResponse<MatchingConfigService.Snapshot> matchingConfig() {
        return ApiResponse.success(matchingConfigService.getCurrentSnapshot());
    }

    @PutMapping("/matching-config")
    public ApiResponse<MatchingConfigService.Snapshot> updateMatchingConfig(@Valid @RequestBody MatchingConfigUpsertRequest request) {
        return ApiResponse.success(matchingConfigService.update(
                new MatchingConfigService.UpdateCommand(
                        new MatchingConfigService.ScoreWeights(
                                request.getScoreWeights().getSkill(),
                                request.getScoreWeights().getWorkMode(),
                                request.getScoreWeights().getCommunication(),
                                request.getScoreWeights().getEnvironment(),
                                request.getScoreWeights().getAccommodation()
                        ),
                        new MatchingConfigService.Risk(
                                request.getRisk().getPenaltyPerRisk(),
                                request.getRisk().getPenaltyPerBlockingRisk(),
                                request.getRisk().getMaxPenalty(),
                                request.getRisk().getHardFilteredMaxScore()
                        ),
                        new MatchingConfigService.CandidateStage(
                                request.getCandidateStage().getMatchScoreWeight(),
                                request.getCandidateStage().getProfileCompletionWeight(),
                                request.getCandidateStage().getPriorityThreshold(),
                                request.getCandidateStage().getFollowUpThreshold()
                        )
                ),
                SecurityUtils.getCurrentUserId()
        ));
    }

    @PostMapping("/matching-config/reset")
    public ApiResponse<MatchingConfigService.Snapshot> resetMatchingConfig() {
        return ApiResponse.success(matchingConfigService.resetToDefault());
    }

    @GetMapping("/enterprises/{userId}/review")
    public ApiResponse<EnterpriseVerificationProfileResponse> reviewDetail(@PathVariable Long userId) {
        return ApiResponse.success(enterpriseVerificationService.getReviewDetail(userId));
    }

    @PostMapping("/enterprises/{userId}/review")
    public ApiResponse<EnterpriseVerificationProfileResponse> reviewEnterprise(@PathVariable Long userId,
                                                                              @Valid @RequestBody EnterpriseReviewDecisionRequest request) {
        return ApiResponse.success(enterpriseVerificationService.reviewEnterprise(userId, request.getDecision(), request.getNote()));
    }

    @GetMapping("/enterprises/{userId}/materials/{materialId}/download")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long userId, @PathVariable Long materialId) {
        EnterpriseVerificationService.StoredMaterial material = enterpriseVerificationService.loadReviewMaterial(userId, materialId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.originalFileName() + "\"")
                .contentType(resolveMediaType(material.contentType()))
                .body(material.resource());
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
