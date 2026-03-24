package com.rongzhiqiao.jobseeker.service;

import com.rongzhiqiao.catalog.vo.CatalogResponses.JobResponse;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.jobseeker.dto.ApplyJobRequest;
import com.rongzhiqiao.jobseeker.repository.JobApplicationRepository;
import com.rongzhiqiao.jobseeker.service.JobseekerSupportNeedService.SupportNeedSnapshot;
import com.rongzhiqiao.matching.service.JobRecommendationService;
import com.rongzhiqiao.jobseeker.vo.JobApplicationResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private static final Set<String> ALLOWED_INTERVIEW_MODES = Set.of("TEXT", "ONLINE", "HYBRID");
    private static final Set<String> ALLOWED_SUPPORT_VISIBILITIES = Set.of("SUMMARY", "HIDDEN");

    private final JobRecommendationService jobRecommendationService;
    private final JobApplicationRepository jobApplicationRepository;
    private final JobseekerSupportNeedService jobseekerSupportNeedService;

    public JobApplicationResponse applyCurrentUser(String jobId, ApplyJobRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        JobResponse job = jobRecommendationService.getPublishedJobForUser(userId, jobId);
        if (jobApplicationRepository.existsByUserIdAndJobId(userId, jobId)) {
            throw new BusinessException(4008, "job has already been applied");
        }

        String preferredInterviewMode = normalizeRequiredEnum(
                request.getPreferredInterviewMode(),
                "preferredInterviewMode",
                ALLOWED_INTERVIEW_MODES
        );
        String coverNote = request.getCoverNote().trim();
        SupportNeedSnapshot supportNeedSnapshot = jobseekerSupportNeedService.getCurrentSupportNeedSnapshot(userId);
        String supportVisibility = supportNeedSnapshot.supportVisibility();
        if (request.getSupportVisibility() != null && !request.getSupportVisibility().isBlank()) {
            supportVisibility = normalizeRequiredEnum(
                    request.getSupportVisibility(),
                    "supportVisibility",
                    ALLOWED_SUPPORT_VISIBILITIES
            );
        }
        String additionalSupport = normalizeOptional(request.getAdditionalSupport());
        if (additionalSupport == null) {
            additionalSupport = supportNeedSnapshot.summaryText();
        }

        return jobApplicationRepository.insert(
                userId,
                job.id(),
                job.title(),
                job.company(),
                "SUMMARY".equals(supportVisibility),
                coverNote,
                preferredInterviewMode,
                supportVisibility,
                additionalSupport,
                job.matchScore(),
                job.dimensionScores(),
                job.reasons(),
                LocalDateTime.now()
        );
    }

    public List<JobApplicationResponse> listCurrentUserApplications() {
        Long userId = SecurityUtils.getCurrentUserId();
        return jobApplicationRepository.listByUserId(userId);
    }

    private String normalizeRequiredEnum(String rawValue, String fieldName, Set<String> allowedValues) {
        String normalized = rawValue == null ? "" : rawValue.trim().toUpperCase(Locale.ROOT);
        if (!allowedValues.contains(normalized)) {
            throw new BusinessException(4001, fieldName + " is invalid");
        }
        return normalized;
    }

    private String normalizeOptional(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String trimmed = rawValue.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
