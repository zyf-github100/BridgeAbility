package com.rongzhiqiao.enterprise.service;

import com.rongzhiqiao.catalog.vo.CatalogResponses.ScoreItem;
import com.rongzhiqiao.common.api.PageResponse;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.enterprise.dto.EnterpriseJobAccessibilityRequest;
import com.rongzhiqiao.enterprise.dto.EnterpriseJobUpsertRequest;
import com.rongzhiqiao.enterprise.entity.EnterpriseJobPosting;
import com.rongzhiqiao.enterprise.repository.EnterpriseJobRepository;
import com.rongzhiqiao.enterprise.vo.EnterpriseJobAccessibilityResponse;
import com.rongzhiqiao.enterprise.vo.EnterpriseJobDetailResponse;
import com.rongzhiqiao.enterprise.vo.EnterpriseJobSummaryResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnterpriseJobService {

    private static final List<String> ALLOWED_WORK_MODES = List.of("FULL_TIME", "INTERNSHIP", "PART_TIME", "REMOTE", "HYBRID");
    private static final List<String> ALLOWED_INTERVIEW_MODES = List.of("TEXT", "ONLINE", "HYBRID");
    private static final List<String> ALLOWED_PUBLISH_STATUSES = List.of("DRAFT", "PUBLISHED", "OFFLINE");
    private static final Pattern QUESTION_MARK_CLUSTER = Pattern.compile("[?？]{2,}");

    private final EnterpriseJobRepository enterpriseJobRepository;
    private final EnterpriseVerificationService enterpriseVerificationService;

    public PageResponse<EnterpriseJobSummaryResponse> listJobs(Integer page,
                                                               Integer pageSize,
                                                               String city,
                                                               String workMode,
                                                               String publishStatus) {
        return enterpriseJobRepository.listJobs(SecurityUtils.getCurrentUserId(), page, pageSize, city, workMode, publishStatus);
    }

    public EnterpriseJobDetailResponse getJob(String jobId) {
        return toDetailResponse(requireOwnedJob(jobId));
    }

    @Transactional
    public EnterpriseJobDetailResponse createJob(EnterpriseJobUpsertRequest request) {
        String publishStatus = normalizeEnum(request.getPublishStatus(), "publishStatus", ALLOWED_PUBLISH_STATUSES);
        if ("OFFLINE".equals(publishStatus)) {
            throw new BusinessException(4001, "new job cannot start as offline");
        }

        EnterpriseJobPosting jobPosting = buildJobPosting(null, request, publishStatus);
        jobPosting.setJobId(generateJobId());
        jobPosting.setSortNo(enterpriseJobRepository.nextSortNo());
        jobPosting.setCreatedByUserId(SecurityUtils.getCurrentUserId());
        enterpriseJobRepository.upsert(jobPosting);
        return getJob(jobPosting.getJobId());
    }

    @Transactional
    public EnterpriseJobDetailResponse updateJob(String jobId, EnterpriseJobUpsertRequest request) {
        EnterpriseJobPosting existing = requireOwnedJob(jobId);

        String publishStatus = normalizeEnum(request.getPublishStatus(), "publishStatus", ALLOWED_PUBLISH_STATUSES);
        EnterpriseJobPosting jobPosting = buildJobPosting(existing, request, publishStatus);
        jobPosting.setJobId(jobId);
        jobPosting.setSortNo(existing.getSortNo());
        jobPosting.setCreatedByUserId(existing.getCreatedByUserId() == null ? SecurityUtils.getCurrentUserId() : existing.getCreatedByUserId());
        enterpriseJobRepository.upsert(jobPosting);
        return getJob(jobId);
    }

    @Transactional
    public EnterpriseJobDetailResponse offlineJob(String jobId) {
        requireOwnedJob(jobId);
        enterpriseJobRepository.updatePublishStatus(jobId, SecurityUtils.getCurrentUserId(), "OFFLINE");
        return getJob(jobId);
    }

    private EnterpriseJobPosting requireOwnedJob(String jobId) {
        EnterpriseJobPosting jobPosting = enterpriseJobRepository.findByJobId(jobId, SecurityUtils.getCurrentUserId());
        if (jobPosting == null) {
            throw new BusinessException(4004, "job not found");
        }
        return jobPosting;
    }

    private EnterpriseJobPosting buildJobPosting(EnterpriseJobPosting existing,
                                                 EnterpriseJobUpsertRequest request,
                                                 String publishStatus) {
        validateRequest(request, publishStatus);

        EnterpriseJobAccessibilityRequest accessibility = request.getAccessibilityTag();
        EnterpriseJobPosting jobPosting = new EnterpriseJobPosting();
        jobPosting.setTitle(trim(request.getTitle()));
        jobPosting.setCompanyName(existing == null ? enterpriseVerificationService.getCurrentCompanyName() : existing.getCompanyName());
        jobPosting.setDepartment(trim(request.getDepartment()));
        jobPosting.setCity(trim(request.getCity()));
        jobPosting.setSalaryMin(request.getSalaryMin());
        jobPosting.setSalaryMax(request.getSalaryMax());
        jobPosting.setSalaryRange(formatSalaryRange(request.getSalaryMin(), request.getSalaryMax()));
        jobPosting.setHeadcount(request.getHeadcount());
        jobPosting.setDescriptionText(trim(request.getDescription()));
        jobPosting.setRequirementText(trim(request.getRequirementText()));
        jobPosting.setWorkMode(normalizeEnum(request.getWorkMode(), "workMode", ALLOWED_WORK_MODES));
        jobPosting.setDeadline(request.getDeadline());
        jobPosting.setInterviewMode(normalizeEnum(request.getInterviewMode(), "interviewMode", ALLOWED_INTERVIEW_MODES));
        jobPosting.setPublishStatus(publishStatus);

        jobPosting.setOnsiteRequired(accessibility == null ? null : accessibility.getOnsiteRequired());
        jobPosting.setRemoteSupported(accessibility == null ? null : accessibility.getRemoteSupported());
        jobPosting.setHighFrequencyVoiceRequired(accessibility == null ? null : accessibility.getHighFrequencyVoiceRequired());
        jobPosting.setNoisyEnvironment(accessibility == null ? null : accessibility.getNoisyEnvironment());
        jobPosting.setLongStandingRequired(accessibility == null ? null : accessibility.getLongStandingRequired());
        jobPosting.setTextMaterialSupported(accessibility == null ? null : accessibility.getTextMaterialSupported());
        jobPosting.setOnlineInterviewSupported(accessibility == null ? null : accessibility.getOnlineInterviewSupported());
        jobPosting.setTextInterviewSupported(accessibility == null ? null : accessibility.getTextInterviewSupported());
        jobPosting.setFlexibleScheduleSupported(accessibility == null ? null : accessibility.getFlexibleScheduleSupported());
        jobPosting.setAccessibleWorkspace(accessibility == null ? null : accessibility.getAccessibleWorkspace());
        jobPosting.setAssistiveSoftwareSupported(accessibility == null ? null : accessibility.getAssistiveSoftwareSupported());

        List<String> descriptionItems = splitTextItems(jobPosting.getDescriptionText());
        List<String> requirementItems = splitTextItems(jobPosting.getRequirementText());
        List<String> reasons = buildReasons(jobPosting);
        List<String> risks = buildRisks(jobPosting);
        List<String> supports = buildSupports(jobPosting);
        List<String> environmentItems = buildEnvironmentItems(jobPosting);
        List<ScoreItem> dimensionScores = buildDimensionScores(jobPosting);
        int matchScore = Math.toIntExact(Math.round(dimensionScores.stream().mapToInt(ScoreItem::value).average().orElse(76)));

        jobPosting.setDescriptionItems(descriptionItems);
        jobPosting.setRequirementItems(requirementItems);
        jobPosting.setReasons(reasons);
        jobPosting.setRisks(risks);
        jobPosting.setSupports(supports);
        jobPosting.setEnvironmentItems(environmentItems);
        jobPosting.setDimensionScores(dimensionScores);
        jobPosting.setMatchScore(matchScore);
        jobPosting.setStage(determineStage(risks.size(), supports.size(), publishStatus));
        jobPosting.setSummary(buildSummary(descriptionItems));
        jobPosting.setApplyHint(buildApplyHint(jobPosting));
        return jobPosting;
    }

    private void validateRequest(EnterpriseJobUpsertRequest request, String publishStatus) {
        validateNoCorruptedText("岗位标题", request.getTitle());
        validateNoCorruptedText("部门", request.getDepartment());
        validateNoCorruptedText("城市", request.getCity());
        validateNoCorruptedText("岗位描述", request.getDescription());
        validateNoCorruptedText("岗位要求", request.getRequirementText());
        if (request.getSalaryMin() != null
                && request.getSalaryMax() != null
                && request.getSalaryMin() > request.getSalaryMax()) {
            throw new BusinessException(4001, "salaryMin cannot be greater than salaryMax");
        }
        if (request.getDeadline() != null && request.getDeadline().isBefore(LocalDate.now())) {
            throw new BusinessException(4001, "deadline cannot be in the past");
        }
        if ("PUBLISHED".equals(publishStatus)) {
            enterpriseVerificationService.ensureCurrentEnterpriseApprovedForPublishing();
        }
        if ("PUBLISHED".equals(publishStatus) && calculateAccessibilityCompletionRate(request.getAccessibilityTag()) < 100) {
            throw new BusinessException(4001, "accessibilityTag must be completed before publishing");
        }
    }

    private void validateNoCorruptedText(String fieldLabel, String value) {
        String normalized = trim(value);
        if (normalized.isEmpty()) {
            return;
        }

        if (normalized.indexOf('\uFFFD') >= 0
                || normalized.indexOf('�') >= 0
                || QUESTION_MARK_CLUSTER.matcher(normalized).find()) {
            throw new BusinessException(4001, fieldLabel + "包含异常占位字符，请检查输入编码后重试");
        }
    }

    private EnterpriseJobDetailResponse toDetailResponse(EnterpriseJobPosting jobPosting) {
        int completionRate = calculateAccessibilityCompletionRate(jobPosting);
        return new EnterpriseJobDetailResponse(
                jobPosting.getJobId(),
                jobPosting.getTitle(),
                jobPosting.getCompanyName(),
                jobPosting.getDepartment(),
                jobPosting.getCity(),
                jobPosting.getSalaryMin(),
                jobPosting.getSalaryMax(),
                jobPosting.getSalaryRange(),
                jobPosting.getHeadcount(),
                jobPosting.getDescriptionText(),
                jobPosting.getRequirementText(),
                jobPosting.getWorkMode(),
                jobPosting.getDeadline() == null ? "" : jobPosting.getDeadline().toString(),
                jobPosting.getInterviewMode(),
                jobPosting.getPublishStatus(),
                jobPosting.getStage(),
                jobPosting.getMatchScore(),
                completionRate,
                completionRate == 100,
                new EnterpriseJobAccessibilityResponse(
                        jobPosting.getOnsiteRequired(),
                        jobPosting.getRemoteSupported(),
                        jobPosting.getHighFrequencyVoiceRequired(),
                        jobPosting.getNoisyEnvironment(),
                        jobPosting.getLongStandingRequired(),
                        jobPosting.getTextMaterialSupported(),
                        jobPosting.getOnlineInterviewSupported(),
                        jobPosting.getTextInterviewSupported(),
                        jobPosting.getFlexibleScheduleSupported(),
                        jobPosting.getAccessibleWorkspace(),
                        jobPosting.getAssistiveSoftwareSupported()
                ),
                jobPosting.getDimensionScores(),
                jobPosting.getReasons(),
                jobPosting.getRisks(),
                jobPosting.getSupports(),
                jobPosting.getEnvironmentItems(),
                jobPosting.getApplyHint()
        );
    }

    private int calculateAccessibilityCompletionRate(EnterpriseJobAccessibilityRequest accessibility) {
        if (accessibility == null) {
            return 0;
        }
        int filled = 0;
        Boolean[] values = new Boolean[]{
                accessibility.getOnsiteRequired(),
                accessibility.getRemoteSupported(),
                accessibility.getHighFrequencyVoiceRequired(),
                accessibility.getNoisyEnvironment(),
                accessibility.getLongStandingRequired(),
                accessibility.getTextMaterialSupported(),
                accessibility.getOnlineInterviewSupported(),
                accessibility.getTextInterviewSupported(),
                accessibility.getFlexibleScheduleSupported(),
                accessibility.getAccessibleWorkspace(),
                accessibility.getAssistiveSoftwareSupported()
        };
        for (Boolean value : values) {
            if (value != null) {
                filled += 1;
            }
        }
        return Math.toIntExact(Math.round(filled * 100.0 / values.length));
    }

    private int calculateAccessibilityCompletionRate(EnterpriseJobPosting jobPosting) {
        int filled = 0;
        Boolean[] values = new Boolean[]{
                jobPosting.getOnsiteRequired(),
                jobPosting.getRemoteSupported(),
                jobPosting.getHighFrequencyVoiceRequired(),
                jobPosting.getNoisyEnvironment(),
                jobPosting.getLongStandingRequired(),
                jobPosting.getTextMaterialSupported(),
                jobPosting.getOnlineInterviewSupported(),
                jobPosting.getTextInterviewSupported(),
                jobPosting.getFlexibleScheduleSupported(),
                jobPosting.getAccessibleWorkspace(),
                jobPosting.getAssistiveSoftwareSupported()
        };
        for (Boolean value : values) {
            if (value != null) {
                filled += 1;
            }
        }
        return Math.toIntExact(Math.round(filled * 100.0 / values.length));
    }

    private List<ScoreItem> buildDimensionScores(EnterpriseJobPosting jobPosting) {
        int skillScore = 78;
        int workModeScore = Boolean.TRUE.equals(jobPosting.getRemoteSupported()) || "REMOTE".equals(jobPosting.getWorkMode())
                ? 92 : "HYBRID".equals(jobPosting.getWorkMode()) ? 86 : 74;
        int communicationScore = scoreFromFlags(
                Boolean.TRUE.equals(jobPosting.getTextInterviewSupported()),
                Boolean.TRUE.equals(jobPosting.getTextMaterialSupported()),
                !Boolean.TRUE.equals(jobPosting.getHighFrequencyVoiceRequired())
        );
        int environmentScore = scoreFromFlags(
                Boolean.TRUE.equals(jobPosting.getAccessibleWorkspace()),
                !Boolean.TRUE.equals(jobPosting.getNoisyEnvironment()),
                !Boolean.TRUE.equals(jobPosting.getLongStandingRequired())
        );
        int accommodationScore = scoreFromFlags(
                Boolean.TRUE.equals(jobPosting.getFlexibleScheduleSupported()),
                Boolean.TRUE.equals(jobPosting.getAssistiveSoftwareSupported()),
                Boolean.TRUE.equals(jobPosting.getOnlineInterviewSupported())
        );
        return List.of(
                new ScoreItem("skill", skillScore),
                new ScoreItem("workMode", workModeScore),
                new ScoreItem("communication", communicationScore),
                new ScoreItem("environment", environmentScore),
                new ScoreItem("accommodation", accommodationScore)
        );
    }

    private int scoreFromFlags(boolean... flags) {
        int score = 62;
        for (boolean flag : flags) {
            score += flag ? 10 : 0;
        }
        return Math.min(score, 96);
    }

    private List<String> buildReasons(EnterpriseJobPosting jobPosting) {
        List<String> reasons = new ArrayList<>();
        if (Boolean.TRUE.equals(jobPosting.getRemoteSupported()) || "REMOTE".equals(jobPosting.getWorkMode())) {
            reasons.add("岗位支持远程或混合协作，便于安排更稳定的工作环境。");
        }
        if (Boolean.TRUE.equals(jobPosting.getTextMaterialSupported())) {
            reasons.add("企业承诺提供书面材料，有助于降低信息理解门槛。");
        }
        if (Boolean.TRUE.equals(jobPosting.getTextInterviewSupported())) {
            reasons.add("面试支持文字或书面沟通，沟通方式更友好。");
        }
        if (Boolean.TRUE.equals(jobPosting.getFlexibleScheduleSupported())) {
            reasons.add("岗位允许弹性安排时间，更容易落地合理便利。");
        }
        if (reasons.isEmpty()) {
            reasons.add("岗位信息结构完整，便于求职者提前判断匹配度。");
        }
        return List.copyOf(reasons);
    }

    private List<String> buildRisks(EnterpriseJobPosting jobPosting) {
        List<String> risks = new ArrayList<>();
        if (Boolean.TRUE.equals(jobPosting.getHighFrequencyVoiceRequired())) {
            risks.add("岗位存在较高频率的语音沟通要求，可能影响部分候选人的沟通舒适度。");
        }
        if (Boolean.TRUE.equals(jobPosting.getNoisyEnvironment())) {
            risks.add("工作环境噪音较高，建议提前说明并确认支持方案。");
        }
        if (Boolean.TRUE.equals(jobPosting.getLongStandingRequired())) {
            risks.add("岗位涉及较长时间站立，需确认现场安排是否可调整。");
        }
        if (Boolean.TRUE.equals(jobPosting.getOnsiteRequired()) && !Boolean.TRUE.equals(jobPosting.getRemoteSupported())) {
            risks.add("岗位以线下到岗为主，建议在投递前明确现场可达性。");
        }
        return List.copyOf(risks);
    }

    private List<String> buildSupports(EnterpriseJobPosting jobPosting) {
        LinkedHashSet<String> supports = new LinkedHashSet<>();
        if (Boolean.TRUE.equals(jobPosting.getTextMaterialSupported())) {
            supports.add("提供书面任务说明和面试材料");
        }
        if (Boolean.TRUE.equals(jobPosting.getOnlineInterviewSupported())) {
            supports.add("支持线上面试");
        }
        if (Boolean.TRUE.equals(jobPosting.getTextInterviewSupported())) {
            supports.add("支持文字或书面面试");
        }
        if (Boolean.TRUE.equals(jobPosting.getFlexibleScheduleSupported())) {
            supports.add("支持弹性工作时间");
        }
        if (Boolean.TRUE.equals(jobPosting.getAccessibleWorkspace())) {
            supports.add("办公区域具备无障碍可达条件");
        }
        if (Boolean.TRUE.equals(jobPosting.getAssistiveSoftwareSupported())) {
            supports.add("可配合提供辅助软件或设备");
        }
        if (supports.isEmpty()) {
            supports.add("企业尚未提供额外支持说明");
        }
        return List.copyOf(supports);
    }

    private List<String> buildEnvironmentItems(EnterpriseJobPosting jobPosting) {
        List<String> items = new ArrayList<>();
        items.add("部门：" + jobPosting.getDepartment());
        items.add("招聘人数：" + jobPosting.getHeadcount() + " 人");
        items.add("截止日期：" + (jobPosting.getDeadline() == null ? "待定" : jobPosting.getDeadline()));
        items.add(Boolean.TRUE.equals(jobPosting.getOnsiteRequired()) ? "需要线下到岗" : "支持灵活到岗安排");
        items.add(Boolean.TRUE.equals(jobPosting.getRemoteSupported()) ? "支持远程/混合办公" : "以现场办公为主");
        if (Boolean.TRUE.equals(jobPosting.getNoisyEnvironment())) {
            items.add("工作环境存在较高噪音");
        }
        if (Boolean.TRUE.equals(jobPosting.getLongStandingRequired())) {
            items.add("岗位可能涉及较长时间站立");
        }
        if (Boolean.TRUE.equals(jobPosting.getAccessibleWorkspace())) {
            items.add("办公区域已确认无障碍可达");
        }
        return List.copyOf(items);
    }

    private String determineStage(int riskCount, int supportCount, String publishStatus) {
        if ("DRAFT".equals(publishStatus)) {
            return "OPEN";
        }
        if (riskCount >= 2) {
            return "CAUTION";
        }
        if (supportCount >= 4) {
            return "PRIORITY";
        }
        return "OPEN";
    }

    private String buildSummary(List<String> descriptionItems) {
        if (descriptionItems.isEmpty()) {
            return "岗位信息待补充。";
        }
        String first = descriptionItems.get(0).trim();
        return first.length() <= 54 ? first : first.substring(0, 54) + "...";
    }

    private String buildApplyHint(EnterpriseJobPosting jobPosting) {
        if (Boolean.TRUE.equals(jobPosting.getTextInterviewSupported())) {
            return "建议突出结构化书面表达和文字沟通优势。";
        }
        if (Boolean.TRUE.equals(jobPosting.getOnlineInterviewSupported())) {
            return "建议提前确认线上面试设备和沟通方式。";
        }
        return "建议在投递说明中提前写明支持需求和工作方式偏好。";
    }

    private List<String> splitTextItems(String text) {
        String normalized = trim(text).replace("\r", "");
        String[] rawItems = normalized.contains("\n")
                ? normalized.split("\\n+")
                : normalized.split("[；;]+");
        List<String> items = new ArrayList<>();
        for (String rawItem : rawItems) {
            String item = trim(rawItem);
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        return items.isEmpty() ? List.of(normalized) : List.copyOf(items);
    }

    private String formatSalaryRange(int salaryMin, int salaryMax) {
        return formatSalaryNumber(salaryMin) + "-" + formatSalaryNumber(salaryMax);
    }

    private String formatSalaryNumber(int amount) {
        if (amount % 1000 == 0) {
            return (amount / 1000) + "k";
        }
        return String.valueOf(amount);
    }

    private String normalizeEnum(String rawValue, String fieldName, List<String> allowedValues) {
        String normalized = trim(rawValue).toUpperCase(Locale.ROOT);
        if (!allowedValues.contains(normalized)) {
            throw new BusinessException(4001, fieldName + " is invalid");
        }
        return normalized;
    }

    private String generateJobId() {
        long timestamp = System.currentTimeMillis();
        int suffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "job-" + Long.toString(timestamp, 36) + "-" + suffix;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
