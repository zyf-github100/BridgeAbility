package com.rongzhiqiao.jobseeker.service;

import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.jobseeker.dto.JobseekerInterviewSupportRequestCreateRequest;
import com.rongzhiqiao.jobseeker.repository.JobApplicationRepository;
import com.rongzhiqiao.jobseeker.repository.JobApplicationRepository.ApplicationRecord;
import com.rongzhiqiao.jobseeker.repository.JobseekerInterviewSupportRequestRepository;
import com.rongzhiqiao.jobseeker.repository.JobseekerInterviewSupportRequestRepository.InterviewSupportRequestRecord;
import com.rongzhiqiao.jobseeker.vo.JobseekerInterviewSupportRequestResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobseekerInterviewSupportRequestService {

    private static final Set<String> ALLOWED_APPLICATION_STATUSES = Set.of("APPLIED", "VIEWED", "INTERVIEW", "INTERVIEWING");

    private final JobApplicationRepository jobApplicationRepository;
    private final JobseekerInterviewSupportRequestRepository jobseekerInterviewSupportRequestRepository;

    public JobseekerInterviewSupportRequestResponse createCurrentUserRequest(
            JobseekerInterviewSupportRequestCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ApplicationRecord application = jobApplicationRepository.findByIdAndUserId(request.getApplicationId(), userId);
        if (application == null) {
            throw new BusinessException(4004, "candidate application not found");
        }

        String applicationStatus = normalizeUpper(application.status());
        if (!ALLOWED_APPLICATION_STATUSES.contains(applicationStatus)) {
            throw new BusinessException(4001, "application status does not allow interview support request");
        }

        InterviewSupportRequestRecord saved = jobseekerInterviewSupportRequestRepository.insert(
                application.applicationId(),
                normalizeRequestType(request.getRequestType()),
                normalizeRequiredText(request.getRequestContent(), "requestContent"),
                "PENDING",
                LocalDateTime.now()
        );
        return toResponse(saved);
    }

    public List<JobseekerInterviewSupportRequestResponse> listCurrentUserRequests() {
        Long userId = SecurityUtils.getCurrentUserId();
        return jobseekerInterviewSupportRequestRepository.listByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private JobseekerInterviewSupportRequestResponse toResponse(InterviewSupportRequestRecord record) {
        return new JobseekerInterviewSupportRequestResponse(
                record.id(),
                record.applicationId(),
                record.requestType(),
                toRequestTypeLabel(record.requestType()),
                record.requestContent(),
                record.requestStatus(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private String normalizeRequestType(String rawValue) {
        String normalized = normalizeRequiredText(rawValue, "requestType").toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TEXT", "TEXT_INTERVIEW" -> "TEXT_INTERVIEW";
            case "SUBTITLE" -> "SUBTITLE";
            case "REMOTE", "REMOTE_INTERVIEW" -> "REMOTE_INTERVIEW";
            case "FLEXIBLE_TIME" -> "FLEXIBLE_TIME";
            case "OTHER" -> "OTHER";
            default -> throw new BusinessException(4001, "requestType is invalid");
        };
    }

    private String toRequestTypeLabel(String requestType) {
        return switch (requestType) {
            case "TEXT_INTERVIEW" -> "文字面试";
            case "SUBTITLE" -> "字幕支持";
            case "REMOTE_INTERVIEW" -> "远程面试";
            case "FLEXIBLE_TIME" -> "弹性时间";
            default -> "其他说明";
        };
    }

    private String normalizeUpper(String rawValue) {
        return normalizeRequiredText(rawValue, "status").toUpperCase(Locale.ROOT);
    }

    private String normalizeRequiredText(String rawValue, String fieldName) {
        if (rawValue == null) {
            throw new BusinessException(4001, fieldName + " is required");
        }
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(4001, fieldName + " is required");
        }
        return trimmed;
    }
}
