package com.rongzhiqiao.enterprise.service;

import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.enterprise.dto.EnterpriseInterviewInviteRequest;
import com.rongzhiqiao.enterprise.dto.EnterpriseInterviewResultRequest;
import com.rongzhiqiao.enterprise.repository.EnterpriseInterviewRepository;
import com.rongzhiqiao.enterprise.vo.EnterpriseCandidateApplicationResponse;
import com.rongzhiqiao.enterprise.vo.EnterpriseInterviewRecordResponse;
import com.rongzhiqiao.serviceorg.service.EmploymentFollowupWorkflowService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnterpriseInterviewService {

    private static final Set<String> INVITABLE_APPLICATION_STATUSES = Set.of("APPLIED", "INTERVIEW", "INTERVIEWING");
    private static final Set<String> PASS_APPLICATION_STATUSES = Set.of("OFFERED", "HIRED");
    private static final Set<String> ALLOWED_INTERVIEW_MODES = Set.of("ONSITE", "ONLINE", "TEXT", "WRITTEN_TEST", "HYBRID");
    private static final Set<String> ALLOWED_RESULT_STATUSES = Set.of("PASS", "FAIL");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EnterpriseCandidateService enterpriseCandidateService;
    private final EnterpriseInterviewRepository enterpriseInterviewRepository;
    private final EmploymentFollowupWorkflowService employmentFollowupWorkflowService;

    @Transactional
    public EnterpriseCandidateApplicationResponse invite(EnterpriseInterviewInviteRequest request) {
        EnterpriseCandidateApplicationResponse candidate =
                enterpriseCandidateService.getCandidate(request.getApplicationId());
        validateInvitableStatus(candidate.status());

        LocalDateTime interviewTime = parseInterviewTime(request.getInterviewTime());
        String interviewMode = normalizeRequiredEnum(request.getInterviewMode(), "interviewMode", ALLOWED_INTERVIEW_MODES);
        String interviewerName = normalizeRequiredText(request.getInterviewerName(), "interviewerName");
        String inviteNote = normalizeOptional(request.getNote());

        EnterpriseInterviewRecordResponse latestInterview = candidate.latestInterview();
        if (latestInterview != null && "PENDING".equals(latestInterview.resultStatus())) {
            enterpriseInterviewRepository.updatePendingInterviewRecord(
                    latestInterview.interviewId(),
                    interviewTime,
                    interviewMode,
                    interviewerName,
                    inviteNote
            );
        } else {
            enterpriseInterviewRepository.createInterviewRecord(
                    candidate.applicationId(),
                    interviewTime,
                    interviewMode,
                    interviewerName,
                    inviteNote
            );
        }

        enterpriseInterviewRepository.updateApplicationStatus(candidate.applicationId(), "INTERVIEW");
        return enterpriseCandidateService.getCandidate(candidate.applicationId());
    }

    @Transactional
    public EnterpriseCandidateApplicationResponse submitResult(EnterpriseInterviewResultRequest request) {
        EnterpriseCandidateApplicationResponse candidate =
                enterpriseCandidateService.getCandidate(request.getApplicationId());
        EnterpriseInterviewRecordResponse latestInterview = candidate.latestInterview();
        if (latestInterview == null || !"PENDING".equals(latestInterview.resultStatus())) {
            throw new BusinessException(4001, "pending interview record not found");
        }

        String resultStatus = normalizeRequiredEnum(request.getResultStatus(), "resultStatus", ALLOWED_RESULT_STATUSES);
        String feedbackNote = normalizeOptional(request.getFeedbackNote());
        String rejectReason = normalizeOptional(request.getRejectReason());
        String applicationStatus = normalizeResultApplicationStatus(resultStatus, request.getApplicationStatus(), rejectReason);

        enterpriseInterviewRepository.updateInterviewResult(
                latestInterview.interviewId(),
                resultStatus,
                feedbackNote,
                rejectReason
        );
        enterpriseInterviewRepository.updateApplicationStatus(candidate.applicationId(), applicationStatus);
        if ("HIRED".equals(applicationStatus)) {
            employmentFollowupWorkflowService.scheduleFollowupsForHire(
                    candidate.userId(),
                    candidate.candidateName(),
                    candidate.jobId(),
                    candidate.jobTitle(),
                    LocalDateTime.now()
            );
        }
        return enterpriseCandidateService.getCandidate(candidate.applicationId());
    }

    private void validateInvitableStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!INVITABLE_APPLICATION_STATUSES.contains(normalized)) {
            throw new BusinessException(4001, "application status does not allow interview invite");
        }
    }

    private String normalizeResultApplicationStatus(String resultStatus, String rawApplicationStatus, String rejectReason) {
        String applicationStatus = normalizeOptionalUpper(rawApplicationStatus);
        if ("PASS".equals(resultStatus)) {
            if (applicationStatus == null || !PASS_APPLICATION_STATUSES.contains(applicationStatus)) {
                throw new BusinessException(4001, "applicationStatus must be OFFERED or HIRED when resultStatus is PASS");
            }
            return applicationStatus;
        }

        if (rejectReason == null) {
            throw new BusinessException(4001, "rejectReason is required when resultStatus is FAIL");
        }
        if (applicationStatus == null) {
            return "REJECTED";
        }
        if (!"REJECTED".equals(applicationStatus)) {
            throw new BusinessException(4001, "applicationStatus must be REJECTED when resultStatus is FAIL");
        }
        return applicationStatus;
    }

    private LocalDateTime parseInterviewTime(String rawValue) {
        String normalized = normalizeRequiredText(rawValue, "interviewTime");
        try {
            return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(4001, "interviewTime is invalid");
        }
    }

    private String normalizeRequiredEnum(String rawValue, String fieldName, Set<String> allowedValues) {
        String normalized = normalizeRequiredText(rawValue, fieldName).toUpperCase(Locale.ROOT);
        if (!allowedValues.contains(normalized)) {
            throw new BusinessException(4001, fieldName + " is invalid");
        }
        return normalized;
    }

    private String normalizeRequiredText(String rawValue, String fieldName) {
        String normalized = normalizeOptional(rawValue);
        if (normalized == null) {
            throw new BusinessException(4001, fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeOptionalUpper(String rawValue) {
        String normalized = normalizeOptional(rawValue);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String trimmed = rawValue.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
