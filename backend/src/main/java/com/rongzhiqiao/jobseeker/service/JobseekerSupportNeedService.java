package com.rongzhiqiao.jobseeker.service;

import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.jobseeker.dto.JobseekerSupportNeedUpsertRequest;
import com.rongzhiqiao.jobseeker.repository.JobseekerSupportNeedRepository;
import com.rongzhiqiao.jobseeker.repository.JobseekerSupportNeedRepository.JobseekerSupportNeedRecord;
import com.rongzhiqiao.jobseeker.repository.JobseekerSupportNeedRepository.SaveCommand;
import com.rongzhiqiao.jobseeker.vo.InterviewCommunicationCardResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerSupportNeedResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobseekerSupportNeedService {

    private static final Set<String> ALLOWED_SUPPORT_VISIBILITIES = Set.of("SUMMARY", "HIDDEN");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JobseekerSupportNeedRepository jobseekerSupportNeedRepository;

    public JobseekerSupportNeedResponse getCurrentSupportNeeds() {
        Long userId = SecurityUtils.getCurrentUserId();
        return toResponse(getCurrentSupportNeedSnapshot(userId));
    }

    public JobseekerSupportNeedResponse getSupportNeedsForUser(Long userId) {
        if (userId == null) {
            return toResponse(buildSnapshot(null));
        }
        return toResponse(getCurrentSupportNeedSnapshot(userId));
    }

    public JobseekerSupportNeedResponse saveCurrentSupportNeeds(JobseekerSupportNeedUpsertRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String supportVisibility = normalizeRequiredEnum(request.getSupportVisibility(), "supportVisibility");
        JobseekerSupportNeedRecord saved = jobseekerSupportNeedRepository.upsert(new SaveCommand(
                userId,
                supportVisibility,
                request.isTextCommunicationPreferred(),
                request.isSubtitleNeeded(),
                request.isRemoteInterviewPreferred(),
                request.isKeyboardOnlyMode(),
                request.isHighContrastNeeded(),
                request.isLargeFontNeeded(),
                request.isFlexibleScheduleNeeded(),
                request.isAccessibleWorkspaceNeeded(),
                request.isAssistiveSoftwareNeeded(),
                normalizeOptional(request.getRemark())
        ));
        return toResponse(buildSnapshot(saved));
    }

    public SupportNeedSnapshot getCurrentSupportNeedSnapshot(Long userId) {
        return buildSnapshot(jobseekerSupportNeedRepository.findByUserId(userId));
    }

    private JobseekerSupportNeedResponse toResponse(SupportNeedSnapshot snapshot) {
        return new JobseekerSupportNeedResponse(
                snapshot.supportVisibility(),
                snapshot.consentToShareSupportNeed(),
                snapshot.hasAnyNeed(),
                snapshot.textCommunicationPreferred(),
                snapshot.subtitleNeeded(),
                snapshot.remoteInterviewPreferred(),
                snapshot.keyboardOnlyMode(),
                snapshot.highContrastNeeded(),
                snapshot.largeFontNeeded(),
                snapshot.flexibleScheduleNeeded(),
                snapshot.accessibleWorkspaceNeeded(),
                snapshot.assistiveSoftwareNeeded(),
                snapshot.remark(),
                snapshot.supportSummary(),
                snapshot.summaryText(),
                snapshot.interviewCommunicationCard(),
                snapshot.updatedAt()
        );
    }

    private SupportNeedSnapshot buildSnapshot(JobseekerSupportNeedRecord record) {
        if (record == null) {
            List<String> emptySummary = List.of();
            return new SupportNeedSnapshot(
                    "HIDDEN",
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    null,
                    emptySummary,
                    null,
                    buildInterviewCommunicationCard(emptySummary, "HIDDEN", false),
                    ""
            );
        }

        List<String> supportSummary = buildSupportSummary(record);
        String summaryText = supportSummary.isEmpty() ? null : String.join("；", supportSummary);
        String supportVisibility = normalizeRequiredEnum(record.supportVisibility(), "supportVisibility");
        boolean consent = "SUMMARY".equals(supportVisibility);

        return new SupportNeedSnapshot(
                supportVisibility,
                consent,
                !supportSummary.isEmpty(),
                record.textCommunicationPreferred(),
                record.subtitleNeeded(),
                record.remoteInterviewPreferred(),
                record.keyboardOnlyMode(),
                record.highContrastNeeded(),
                record.largeFontNeeded(),
                record.flexibleScheduleNeeded(),
                record.accessibleWorkspaceNeeded(),
                record.assistiveSoftwareNeeded(),
                normalizeOptional(record.remark()),
                supportSummary,
                summaryText,
                buildInterviewCommunicationCard(supportSummary, supportVisibility, !supportSummary.isEmpty()),
                formatDateTime(record.updatedAt())
        );
    }

    private List<String> buildSupportSummary(JobseekerSupportNeedRecord record) {
        List<String> values = new ArrayList<>();
        if (record.textCommunicationPreferred()) {
            values.add("优先采用文字或书面沟通");
        }
        if (record.subtitleNeeded()) {
            values.add("需要字幕或同步文字支持");
        }
        if (record.remoteInterviewPreferred()) {
            values.add("更适合线上或远程面试");
        }
        if (record.keyboardOnlyMode()) {
            values.add("流程与系统需兼容纯键盘操作");
        }
        if (record.highContrastNeeded()) {
            values.add("材料请提供高对比度版本");
        }
        if (record.largeFontNeeded()) {
            values.add("材料请提供较大字号版本");
        }
        if (record.flexibleScheduleNeeded()) {
            values.add("希望预留更灵活的签到或测试时间");
        }
        if (record.accessibleWorkspaceNeeded()) {
            values.add("线下场地需确认无障碍通行与办公条件");
        }
        if (record.assistiveSoftwareNeeded()) {
            values.add("可能需要辅助软件或设备支持");
        }
        String remark = normalizeOptional(record.remark());
        if (remark != null) {
            values.add("其他说明：" + remark);
        }
        return List.copyOf(values);
    }

    private InterviewCommunicationCardResponse buildInterviewCommunicationCard(List<String> supportSummary,
                                                                               String supportVisibility,
                                                                               boolean hasAnyNeed) {
        List<String> lines = supportSummary.isEmpty()
                ? List.of("当前未填写具体便利需求，可按常规流程沟通。")
                : supportSummary;
        String subtitle = hasAnyNeed
                ? "以下安排能帮助我更稳定地完成面试与沟通。"
                : "如果后续有新的安排偏好，我会提前补充说明。";
        String scopeText = "SUMMARY".equals(supportVisibility)
                ? "授权范围：向企业展示支持需求摘要"
                : "授权范围：暂不向企业展示支持需求摘要";

        List<String> copyLines = new ArrayList<>();
        copyLines.add("面试沟通卡");
        copyLines.add(subtitle);
        copyLines.addAll(lines);
        copyLines.add(scopeText);

        return new InterviewCommunicationCardResponse(
                "面试沟通卡",
                subtitle,
                lines,
                String.join(System.lineSeparator(), copyLines)
        );
    }

    private String normalizeRequiredEnum(String rawValue, String fieldName) {
        String normalized = rawValue == null ? "" : rawValue.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_SUPPORT_VISIBILITIES.contains(normalized)) {
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

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    public record SupportNeedSnapshot(
            String supportVisibility,
            boolean consentToShareSupportNeed,
            boolean hasAnyNeed,
            boolean textCommunicationPreferred,
            boolean subtitleNeeded,
            boolean remoteInterviewPreferred,
            boolean keyboardOnlyMode,
            boolean highContrastNeeded,
            boolean largeFontNeeded,
            boolean flexibleScheduleNeeded,
            boolean accessibleWorkspaceNeeded,
            boolean assistiveSoftwareNeeded,
            String remark,
            List<String> supportSummary,
            String summaryText,
            InterviewCommunicationCardResponse interviewCommunicationCard,
            String updatedAt
    ) {
    }
}
