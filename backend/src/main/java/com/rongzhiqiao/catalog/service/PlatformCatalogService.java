package com.rongzhiqiao.catalog.service;

import com.rongzhiqiao.catalog.repository.CatalogRepository;
import com.rongzhiqiao.catalog.vo.CatalogResponses.AdminDashboardResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.CandidateResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.EnterpriseProfileResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.EnterpriseReviewItemResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.JobResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.KnowledgeArticleResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.MatchingStatusResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.NotificationResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.ServiceAlertResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.ServiceCaseResponse;
import com.rongzhiqiao.common.api.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlatformCatalogService {

    private final CatalogRepository catalogRepository;

    public PageResponse<JobResponse> listRecommendedJobs(String city, String workMode, String keyword, Integer page, Integer pageSize) {
        return catalogRepository.listRecommendedJobs(city, workMode, keyword, page, pageSize);
    }

    public JobResponse getPublishedJob(String jobId) {
        return catalogRepository.getPublishedJob(jobId);
    }

    public JobResponse getJob(String jobId) {
        return catalogRepository.getJob(jobId);
    }

    public PageResponse<JobResponse> listEnterpriseJobs(Integer page, Integer pageSize, String city, String workMode) {
        return catalogRepository.listEnterpriseJobs(page, pageSize, city, workMode);
    }

    public PageResponse<CandidateResponse> listCandidates(String jobId, Boolean consentGranted, Integer page, Integer pageSize) {
        return catalogRepository.listCandidates(jobId, consentGranted, page, pageSize);
    }

    public EnterpriseProfileResponse getEnterpriseProfile() {
        return catalogRepository.getEnterpriseProfile();
    }

    public List<ServiceCaseResponse> listServiceCases() {
        return catalogRepository.listServiceCases();
    }

    public ServiceCaseResponse getServiceCase(String caseId) {
        return catalogRepository.getServiceCase(caseId);
    }

    public PageResponse<ServiceAlertResponse> listServiceAlerts(String status, Integer level, Integer page, Integer pageSize) {
        return catalogRepository.listServiceAlerts(status, level, page, pageSize);
    }

    public AdminDashboardResponse getAdminDashboard() {
        return catalogRepository.getAdminDashboard();
    }

    public List<EnterpriseReviewItemResponse> listPendingEnterpriseReviews() {
        return catalogRepository.listPendingEnterpriseReviews();
    }

    public List<String> listAuditLogs() {
        return catalogRepository.listAuditLogs();
    }

    public List<KnowledgeArticleResponse> listKnowledgeArticles() {
        return catalogRepository.listKnowledgeArticles();
    }

    public List<NotificationResponse> listNotifications(Boolean read) {
        return catalogRepository.listNotifications(read);
    }

    public MatchingStatusResponse getMatchingStatus() {
        return catalogRepository.getMatchingStatus();
    }
}
