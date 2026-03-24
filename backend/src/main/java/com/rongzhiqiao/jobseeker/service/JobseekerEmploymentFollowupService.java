package com.rongzhiqiao.jobseeker.service;

import com.rongzhiqiao.auth.entity.SysUser;
import com.rongzhiqiao.auth.mapper.SysUserMapper;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.jobseeker.dto.JobseekerEmploymentFollowupCreateRequest;
import com.rongzhiqiao.jobseeker.entity.JobseekerProfile;
import com.rongzhiqiao.jobseeker.mapper.JobseekerProfileMapper;
import com.rongzhiqiao.jobseeker.repository.JobApplicationRepository;
import com.rongzhiqiao.jobseeker.repository.JobApplicationRepository.ApplicationRecord;
import com.rongzhiqiao.jobseeker.repository.JobseekerEmploymentFollowupRepository;
import com.rongzhiqiao.jobseeker.repository.JobseekerEmploymentFollowupRepository.EmploymentFollowupRecord;
import com.rongzhiqiao.jobseeker.vo.JobseekerEmploymentFollowupResponse;
import com.rongzhiqiao.serviceorg.service.EmploymentFollowupWorkflowService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobseekerEmploymentFollowupService {

    private static final Set<String> ALLOWED_FOLLOWUP_STAGES = Set.of("DAY_7", "DAY_30");

    private final JobApplicationRepository jobApplicationRepository;
    private final JobseekerEmploymentFollowupRepository jobseekerEmploymentFollowupRepository;
    private final EmploymentFollowupWorkflowService employmentFollowupWorkflowService;
    private final JobseekerProfileMapper jobseekerProfileMapper;
    private final SysUserMapper sysUserMapper;

    @Transactional
    public JobseekerEmploymentFollowupResponse createCurrentUserFollowup(
            JobseekerEmploymentFollowupCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ApplicationRecord application = jobApplicationRepository.findByUserIdAndJobId(userId, request.getJobId());
        if (application == null) {
            throw new BusinessException(4004, "candidate application not found");
        }
        if (!"HIRED".equalsIgnoreCase(application.status())) {
            throw new BusinessException(4001, "only hired applications can submit followup");
        }

        String followupStage = normalizeFollowupStage(request.getFollowupStage());
        EmploymentFollowupRecord saved = jobseekerEmploymentFollowupRepository.upsert(
                userId,
                application.jobId(),
                followupStage,
                request.getAdaptationScore(),
                normalizeOptional(request.getEnvironmentIssue()),
                normalizeOptional(request.getCommunicationIssue()),
                Boolean.TRUE.equals(request.getSupportImplemented()),
                Boolean.TRUE.equals(request.getLeaveRisk()),
                Boolean.TRUE.equals(request.getNeedHelp()),
                normalizeOptional(request.getRemark()),
                LocalDateTime.now()
        );

        String displayName = resolveDisplayName(userId);
        employmentFollowupWorkflowService.recordJobseekerFollowup(
                userId,
                displayName,
                application.jobId(),
                application.jobTitle(),
                followupStage,
                request.getAdaptationScore(),
                normalizeOptional(request.getEnvironmentIssue()),
                normalizeOptional(request.getCommunicationIssue()),
                Boolean.TRUE.equals(request.getSupportImplemented()),
                Boolean.TRUE.equals(request.getLeaveRisk()),
                Boolean.TRUE.equals(request.getNeedHelp()),
                displayName
        );
        return toResponse(saved);
    }

    public List<JobseekerEmploymentFollowupResponse> listCurrentUserFollowups() {
        Long userId = SecurityUtils.getCurrentUserId();
        return jobseekerEmploymentFollowupRepository.listByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private JobseekerEmploymentFollowupResponse toResponse(EmploymentFollowupRecord record) {
        return new JobseekerEmploymentFollowupResponse(
                record.id(),
                record.jobId(),
                record.followupStage(),
                record.adaptationScore(),
                record.environmentIssue(),
                record.communicationIssue(),
                record.supportImplemented(),
                record.leaveRisk(),
                record.needHelp(),
                record.remark(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private String resolveDisplayName(Long userId) {
        JobseekerProfile profile = jobseekerProfileMapper.selectByUserId(userId);
        if (profile != null && profile.getRealName() != null && !profile.getRealName().isBlank()) {
            return profile.getRealName().trim();
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null && user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname().trim();
        }
        if (user != null && user.getAccount() != null && !user.getAccount().isBlank()) {
            return user.getAccount().trim();
        }
        return "求职者";
    }

    private String normalizeFollowupStage(String rawValue) {
        String normalized = normalizeRequiredText(rawValue, "followupStage").toUpperCase(Locale.ROOT);
        if (!ALLOWED_FOLLOWUP_STAGES.contains(normalized)) {
            throw new BusinessException(4001, "followupStage is invalid");
        }
        return normalized;
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

    private String normalizeOptional(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String trimmed = rawValue.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
