package com.rongzhiqiao.enterprise.service;

import com.rongzhiqiao.common.api.PageResponse;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.enterprise.dto.EnterpriseApplicationStatusUpdateRequest;
import com.rongzhiqiao.enterprise.repository.EnterpriseCandidateRepository;
import com.rongzhiqiao.enterprise.repository.EnterpriseInterviewRepository;
import com.rongzhiqiao.enterprise.repository.EnterpriseJobRepository;
import com.rongzhiqiao.enterprise.vo.EnterpriseCandidateApplicationResponse;
import com.rongzhiqiao.serviceorg.service.EmploymentFollowupWorkflowService;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnterpriseCandidateService {

    private static final Set<String> MANUAL_STATUSES = Set.of("VIEWED", "INTERVIEWING", "OFFERED", "HIRED", "REJECTED");
    private static final Set<String> TERMINAL_STATUSES = Set.of("HIRED", "REJECTED");

    private final EnterpriseCandidateRepository enterpriseCandidateRepository;
    private final EnterpriseInterviewRepository enterpriseInterviewRepository;
    private final EnterpriseJobRepository enterpriseJobRepository;
    private final EmploymentFollowupWorkflowService employmentFollowupWorkflowService;

    public PageResponse<EnterpriseCandidateApplicationResponse> listCandidates(String jobId,
                                                                               Boolean consentGranted,
                                                                               Integer page,
                                                                               Integer pageSize) {
        requireOwnedJob(jobId);
        return enterpriseCandidateRepository.listByJobId(jobId, SecurityUtils.getCurrentUserId(), consentGranted, page, pageSize);
    }

    public EnterpriseCandidateApplicationResponse getCandidate(Long applicationId) {
        EnterpriseCandidateApplicationResponse response =
                enterpriseCandidateRepository.findByApplicationId(applicationId, SecurityUtils.getCurrentUserId());
        if (response == null) {
            throw new BusinessException(4004, "candidate application not found");
        }
        return response;
    }

    @Transactional
    public EnterpriseCandidateApplicationResponse updateApplicationStatus(Long applicationId,
                                                                         EnterpriseApplicationStatusUpdateRequest request) {
        EnterpriseCandidateApplicationResponse candidate = getCandidate(applicationId);
        String targetStatus = normalizeTargetStatus(request.getTargetStatus());
        validateStatusTransition(candidate.status(), targetStatus);

        enterpriseInterviewRepository.updateApplicationStatus(applicationId, targetStatus);
        if ("HIRED".equals(targetStatus) && !"HIRED".equalsIgnoreCase(candidate.status())) {
            employmentFollowupWorkflowService.scheduleFollowupsForHire(
                    candidate.userId(),
                    candidate.candidateName(),
                    candidate.jobId(),
                    candidate.jobTitle(),
                    LocalDateTime.now()
            );
        }
        return getCandidate(applicationId);
    }

    private String normalizeTargetStatus(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().toUpperCase(Locale.ROOT);
        if (!MANUAL_STATUSES.contains(normalized)) {
            throw new BusinessException(4001, "targetStatus is invalid");
        }
        return normalized;
    }

    private void validateStatusTransition(String currentStatus, String targetStatus) {
        String normalizedCurrent = currentStatus == null ? "" : currentStatus.trim().toUpperCase(Locale.ROOT);
        if (normalizedCurrent.equals(targetStatus)) {
            return;
        }
        if (TERMINAL_STATUSES.contains(normalizedCurrent)) {
            throw new BusinessException(4001, "terminal application status cannot be changed");
        }
    }

    private void requireOwnedJob(String jobId) {
        if (enterpriseJobRepository.findByJobId(jobId, SecurityUtils.getCurrentUserId()) == null) {
            throw new BusinessException(4004, "job not found");
        }
    }
}
