package com.rongzhiqiao.matching.service;

import com.rongzhiqiao.catalog.vo.CatalogResponses.JobResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.ScoreItem;
import com.rongzhiqiao.common.api.PageResponse;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.enterprise.entity.EnterpriseJobPosting;
import com.rongzhiqiao.enterprise.repository.EnterpriseJobRepository;
import com.rongzhiqiao.jobseeker.entity.JobseekerProfile;
import com.rongzhiqiao.jobseeker.mapper.JobseekerProfileMapper;
import com.rongzhiqiao.jobseeker.repository.JobseekerSkillRepository;
import com.rongzhiqiao.jobseeker.repository.JobseekerSkillRepository.JobseekerSkillRecord;
import com.rongzhiqiao.jobseeker.service.JobseekerSupportNeedService;
import com.rongzhiqiao.jobseeker.service.JobseekerSupportNeedService.SupportNeedSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobRecommendationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final EnterpriseJobRepository enterpriseJobRepository;
    private final JobseekerProfileMapper jobseekerProfileMapper;
    private final JobseekerSkillRepository jobseekerSkillRepository;
    private final JobseekerSupportNeedService jobseekerSupportNeedService;
    private final MatchingConfigService matchingConfigService;

    public PageResponse<JobResponse> listRecommendedJobs(Integer page,
                                                         Integer pageSize,
                                                         String city,
                                                         String workMode,
                                                         String keyword) {
        Long userId = SecurityUtils.getCurrentUserId();
        MatchingContext context = loadContext(userId);
        MatchingConfigService.RuntimeConfig config = matchingConfigService.getCurrentRuntimeConfig();
        List<JobResponse> scoredJobs = enterpriseJobRepository.listPublishedJobs(city, workMode, keyword).stream()
                .map(job -> assessJob(job, context, config))
                .filter(assessment -> !assessment.hardFiltered())
                .map(MatchAssessment::response)
                .sorted(Comparator.comparingInt(JobResponse::matchScore).reversed()
                        .thenComparing(JobResponse::title)
                        .thenComparing(JobResponse::id))
                .toList();

        int safePage = sanitizePage(page);
        int safePageSize = sanitizePageSize(pageSize);
        int fromIndex = Math.min((safePage - 1) * safePageSize, scoredJobs.size());
        int toIndex = Math.min(fromIndex + safePageSize, scoredJobs.size());
        return new PageResponse<>(scoredJobs.size(), safePage, safePageSize, scoredJobs.subList(fromIndex, toIndex));
    }

    public JobResponse getPublishedJobForCurrentUser(String jobId) {
        return getPublishedJobForUser(SecurityUtils.getCurrentUserId(), jobId);
    }

    public JobResponse getPublishedJobForUser(Long userId, String jobId) {
        EnterpriseJobPosting jobPosting = enterpriseJobRepository.findPublishedByJobId(jobId);
        if (jobPosting == null) {
            throw new BusinessException(4004, "job not found");
        }
        return assessJob(
                jobPosting,
                loadContext(userId),
                matchingConfigService.getCurrentRuntimeConfig()
        ).response();
    }

    private MatchingContext loadContext(Long userId) {
        JobseekerProfile profile = jobseekerProfileMapper.selectByUserId(userId);
        List<JobseekerSkillRecord> skills = jobseekerSkillRepository.listByUserId(userId);
        SupportNeedSnapshot supportNeedSnapshot = jobseekerSupportNeedService.getCurrentSupportNeedSnapshot(userId);
        return new MatchingContext(profile, skills, supportNeedSnapshot);
    }

    private MatchAssessment assessJob(EnterpriseJobPosting jobPosting,
                                      MatchingContext context,
                                      MatchingConfigService.RuntimeConfig config) {
        MatchEvidence evidence = new MatchEvidence();
        int skillScore = computeSkillScore(jobPosting, context, evidence);
        int workModeScore = computeWorkModeScore(jobPosting, context, evidence);
        int communicationScore = computeCommunicationScore(jobPosting, context, evidence);
        int environmentScore = computeEnvironmentScore(jobPosting, context, evidence);
        int accommodationScore = computeAccommodationScore(jobPosting, context, evidence);

        List<ScoreItem> dimensionScores = List.of(
                new ScoreItem("skill", skillScore),
                new ScoreItem("workMode", workModeScore),
                new ScoreItem("communication", communicationScore),
                new ScoreItem("environment", environmentScore),
                new ScoreItem("accommodation", accommodationScore)
        );
        int matchScore = calculateMatchScore(
                skillScore,
                workModeScore,
                communicationScore,
                environmentScore,
                accommodationScore,
                config
        );

        List<String> supports = jobPosting.getSupports() == null || jobPosting.getSupports().isEmpty()
                ? buildSupports(jobPosting)
                : List.copyOf(jobPosting.getSupports());
        List<String> reasons = buildReasons(jobPosting, context, evidence, skillScore, workModeScore, communicationScore, accommodationScore);
        List<String> risks = buildRisks(jobPosting, context, skillScore, workModeScore, communicationScore, environmentScore, accommodationScore);
        RiskDecision riskDecision = evaluateRiskDecision(jobPosting, context, risks, config);
        List<String> finalRisks = mergeRisks(riskDecision.blockingRisks(), risks);
        int finalMatchScore = applyRiskPenalty(matchScore, riskDecision, config);

        JobResponse response = new JobResponse(
                jobPosting.getJobId(),
                jobPosting.getTitle(),
                jobPosting.getCompanyName(),
                jobPosting.getCity(),
                jobPosting.getSalaryRange(),
                jobPosting.getWorkMode(),
                hasText(jobPosting.getSummary()) ? jobPosting.getSummary() : buildSummary(jobPosting),
                jobPosting.getStage(),
                finalMatchScore,
                dimensionScores,
                reasons,
                finalRisks,
                supports,
                firstNonEmpty(jobPosting.getDescriptionItems(), splitTextItems(jobPosting.getDescriptionText())),
                firstNonEmpty(jobPosting.getRequirementItems(), splitTextItems(jobPosting.getRequirementText())),
                firstNonEmpty(jobPosting.getEnvironmentItems(), buildEnvironmentItems(jobPosting)),
                buildApplyHint(context, skillScore, workModeScore, communicationScore, environmentScore, accommodationScore, riskDecision)
        );
        return new MatchAssessment(response, riskDecision.hardFiltered());
    }

    private int computeSkillScore(EnterpriseJobPosting jobPosting, MatchingContext context, MatchEvidence evidence) {
        String jobText = normalize(
                String.join(" ",
                        safeText(jobPosting.getTitle()),
                        safeText(jobPosting.getSummary()),
                        safeText(jobPosting.getDescriptionText()),
                        safeText(jobPosting.getRequirementText()))
        );

        boolean expectedJobMatched = containsAnyToken(jobText, tokenize(context.expectedJob()));
        boolean majorMatched = containsAnyToken(jobText, tokenize(context.major()));
        evidence.expectedJobMatched = expectedJobMatched;

        if (context.skills().isEmpty()) {
            int score = 66;
            if (expectedJobMatched) {
                score += 14;
            }
            if (majorMatched) {
                score += 6;
            }
            if (!hasText(context.expectedJob()) && !hasText(context.major())) {
                score += 4;
            }
            return clamp(score, 48, 90);
        }

        int matchedSkills = 0;
        int matchedLevelSum = 0;
        for (JobseekerSkillRecord skill : context.skills()) {
            if (matchesSkill(jobText, skill)) {
                matchedSkills += 1;
                matchedLevelSum += skill.skillLevel();
                evidence.matchedSkills.add(skill.skillName());
            }
        }

        double coverage = matchedSkills * 1.0 / context.skills().size();
        int averageLevelBonus = matchedSkills == 0 ? 0 : (int) Math.round((matchedLevelSum * 1.0 / matchedSkills) * 3);
        int score = 48 + (int) Math.round(coverage * 32) + averageLevelBonus;
        if (expectedJobMatched) {
            score += 8;
        }
        if (majorMatched) {
            score += 4;
        }
        if (matchedSkills == 0 && context.skills().size() >= 3) {
            score -= 8;
        }
        return clamp(score, 38, 96);
    }

    private int computeWorkModeScore(EnterpriseJobPosting jobPosting, MatchingContext context, MatchEvidence evidence) {
        String preferredMode = normalizeEnum(context.workModePreference());
        String jobMode = normalizeEnum(jobPosting.getWorkMode());
        boolean remoteCapable = Boolean.TRUE.equals(jobPosting.getRemoteSupported())
                || "REMOTE".equals(jobMode)
                || "HYBRID".equals(jobMode);
        int score;

        if (!hasText(preferredMode)) {
            score = remoteCapable ? 82 : 74;
        } else {
            score = switch (preferredMode) {
                case "REMOTE" -> "REMOTE".equals(jobMode) ? 96 : remoteCapable ? 88 : 54;
                case "HYBRID" -> "HYBRID".equals(jobMode) ? 94 : remoteCapable ? 86 : 64;
                case "FULL_TIME" -> "FULL_TIME".equals(jobMode) ? 92 : "HYBRID".equals(jobMode) ? 84 : 70;
                case "PART_TIME" -> "PART_TIME".equals(jobMode) ? 94 : "INTERNSHIP".equals(jobMode) ? 80 : 62;
                case "INTERNSHIP" -> "INTERNSHIP".equals(jobMode) ? 94 : "PART_TIME".equals(jobMode) ? 78 : 64;
                default -> remoteCapable ? 80 : 72;
            };
        }

        String preferredCity = firstText(context.targetCity(), context.currentCity());
        if (hasText(preferredCity)) {
            boolean sameCity = preferredCity.equalsIgnoreCase(safeText(jobPosting.getCity()));
            boolean locationIndependent = "REMOTE".equals(jobMode)
                    || (Boolean.TRUE.equals(jobPosting.getRemoteSupported()) && !Boolean.TRUE.equals(jobPosting.getOnsiteRequired()));
            if (sameCity) {
                score += 4;
                evidence.locationFlexible = true;
            } else if (locationIndependent) {
                score += 2;
                evidence.locationFlexible = true;
            } else if (Boolean.TRUE.equals(jobPosting.getOnsiteRequired())) {
                score -= 10;
            }
        }

        evidence.workModeMatched = score >= 82;
        return clamp(score, 40, 96);
    }

    private int computeCommunicationScore(EnterpriseJobPosting jobPosting, MatchingContext context, MatchEvidence evidence) {
        SupportNeedSnapshot support = context.supportNeedSnapshot();
        int score = 74;

        if (support.textCommunicationPreferred()) {
            score += Boolean.TRUE.equals(jobPosting.getTextInterviewSupported()) ? 12 : -8;
            score += Boolean.TRUE.equals(jobPosting.getTextMaterialSupported()) ? 8 : -4;
            score += Boolean.TRUE.equals(jobPosting.getHighFrequencyVoiceRequired()) ? -18 : 6;
        }
        if (support.subtitleNeeded()) {
            score += Boolean.TRUE.equals(jobPosting.getOnlineInterviewSupported()) ? 8 : -4;
            score += Boolean.TRUE.equals(jobPosting.getTextMaterialSupported()) ? 4 : 0;
        }
        if (support.remoteInterviewPreferred()) {
            score += Boolean.TRUE.equals(jobPosting.getOnlineInterviewSupported()) ? 6 : -4;
        }
        if (!support.hasAnyNeed() && (Boolean.TRUE.equals(jobPosting.getTextInterviewSupported()) || Boolean.TRUE.equals(jobPosting.getTextMaterialSupported()))) {
            score += 4;
        }

        evidence.textFriendly = score >= 84;
        return clamp(score, 38, 96);
    }

    private int computeEnvironmentScore(EnterpriseJobPosting jobPosting, MatchingContext context, MatchEvidence evidence) {
        SupportNeedSnapshot support = context.supportNeedSnapshot();
        boolean remoteCapable = Boolean.TRUE.equals(jobPosting.getRemoteSupported())
                || "REMOTE".equalsIgnoreCase(safeText(jobPosting.getWorkMode()))
                || "HYBRID".equalsIgnoreCase(safeText(jobPosting.getWorkMode()));
        int score = 72;

        if (support.accessibleWorkspaceNeeded()) {
            score += Boolean.TRUE.equals(jobPosting.getAccessibleWorkspace()) ? 16 : -14;
        }
        if (Boolean.TRUE.equals(jobPosting.getNoisyEnvironment())) {
            score -= (support.textCommunicationPreferred() || support.subtitleNeeded()) ? 14 : 8;
        }
        if (Boolean.TRUE.equals(jobPosting.getLongStandingRequired())) {
            score -= 12;
        }
        if (remoteCapable) {
            score += 6;
        }

        String preferredCity = firstText(context.targetCity(), context.currentCity());
        if (hasText(preferredCity) && !preferredCity.equalsIgnoreCase(safeText(jobPosting.getCity())) && Boolean.TRUE.equals(jobPosting.getOnsiteRequired()) && !remoteCapable) {
            score -= 10;
        }

        evidence.environmentStable = score >= 84;
        return clamp(score, 35, 96);
    }

    private int computeAccommodationScore(EnterpriseJobPosting jobPosting, MatchingContext context, MatchEvidence evidence) {
        SupportNeedSnapshot support = context.supportNeedSnapshot();
        int score = 68;

        if (support.flexibleScheduleNeeded()) {
            score += Boolean.TRUE.equals(jobPosting.getFlexibleScheduleSupported()) ? 14 : -10;
        }
        if (support.assistiveSoftwareNeeded()) {
            score += Boolean.TRUE.equals(jobPosting.getAssistiveSoftwareSupported()) ? 16 : -12;
        }
        if (support.highContrastNeeded() || support.largeFontNeeded()) {
            score += Boolean.TRUE.equals(jobPosting.getTextMaterialSupported()) ? 10 : -8;
        }
        if (support.keyboardOnlyMode()) {
            score += (Boolean.TRUE.equals(jobPosting.getTextInterviewSupported()) || Boolean.TRUE.equals(jobPosting.getTextMaterialSupported())) ? 12 : -8;
        }
        if (support.remoteInterviewPreferred()) {
            score += Boolean.TRUE.equals(jobPosting.getOnlineInterviewSupported()) ? 8 : -6;
        }
        if (!support.hasAnyNeed() && buildSupports(jobPosting).size() >= 3) {
            score += 6;
        }

        evidence.accommodationFriendly = score >= 84;
        return clamp(score, 35, 96);
    }

    private List<String> buildReasons(EnterpriseJobPosting jobPosting,
                                      MatchingContext context,
                                      MatchEvidence evidence,
                                      int skillScore,
                                      int workModeScore,
                                      int communicationScore,
                                      int accommodationScore) {
        List<String> reasons = new ArrayList<>();
        if (evidence.expectedJobMatched && hasText(context.expectedJob())) {
            reasons.add("岗位方向与当前期望岗位接近。");
        }
        if (!evidence.matchedSkills.isEmpty()) {
            reasons.add("已匹配到技能标签：" + String.join("、", evidence.matchedSkills) + "。");
        } else if (skillScore >= 75 && hasText(context.major())) {
            reasons.add("专业背景与岗位描述存在关联，可转化为求职优势。");
        }
        if (workModeScore >= 85) {
            reasons.add("工作方式与当前偏好更契合。");
        }
        if (communicationScore >= 85) {
            reasons.add("面试与沟通方式对当前便利需求更友好。");
        }
        if (accommodationScore >= 85) {
            reasons.add("岗位已标注多项可落地的支持条件。");
        }
        if (reasons.isEmpty()) {
            reasons.add(hasProfile(context)
                    ? "岗位信息较完整，建议结合详情进一步判断匹配度。"
                    : "先补充档案和便利需求，可获得更准确的匹配解释。");
        }
        return List.copyOf(reasons);
    }

    private List<String> buildRisks(EnterpriseJobPosting jobPosting,
                                    MatchingContext context,
                                    int skillScore,
                                    int workModeScore,
                                    int communicationScore,
                                    int environmentScore,
                                    int accommodationScore) {
        List<String> risks = new ArrayList<>();
        if (skillScore < 60) {
            risks.add("岗位要求与当前技能标签重合度偏低。");
        }
        if (workModeScore < 60) {
            risks.add("工作方式或工作地点与当前偏好存在差距。");
        }
        if (communicationScore < 60) {
            risks.add("沟通方式可能需要在投递前提前协商。");
        }
        if (environmentScore < 60) {
            risks.add("现场环境和到岗条件建议提前确认。");
        }
        if (accommodationScore < 60) {
            risks.add("岗位尚未充分覆盖当前需要的支持条件。");
        }
        if (Boolean.TRUE.equals(jobPosting.getHighFrequencyVoiceRequired()) && context.supportNeedSnapshot().textCommunicationPreferred()) {
            risks.add("岗位存在高频语音沟通要求。");
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
            supports.add("企业尚未补充额外支持说明");
        }
        return List.copyOf(supports);
    }

    private List<String> buildEnvironmentItems(EnterpriseJobPosting jobPosting) {
        List<String> items = new ArrayList<>();
        items.add("部门：" + safeText(jobPosting.getDepartment()));
        items.add("招聘人数：" + jobPosting.getHeadcount() + " 人");
        items.add("工作地点：" + safeText(jobPosting.getCity()));
        items.add(Boolean.TRUE.equals(jobPosting.getOnsiteRequired()) ? "需要线下到岗" : "支持灵活到岗安排");
        items.add(Boolean.TRUE.equals(jobPosting.getRemoteSupported()) ? "支持远程/混合协作" : "以现场办公为主");
        if (Boolean.TRUE.equals(jobPosting.getNoisyEnvironment())) {
            items.add("工作环境噪音较高");
        }
        if (Boolean.TRUE.equals(jobPosting.getLongStandingRequired())) {
            items.add("岗位可能涉及较长时间站立");
        }
        if (Boolean.TRUE.equals(jobPosting.getAccessibleWorkspace())) {
            items.add("办公区域已确认无障碍可达");
        }
        return List.copyOf(items);
    }

    private String buildApplyHint(MatchingContext context,
                                  int skillScore,
                                  int workModeScore,
                                  int communicationScore,
                                  int environmentScore,
                                  int accommodationScore,
                                  RiskDecision riskDecision) {
        if (riskDecision.hardFiltered()) {
            return "当前存在硬性冲突，系统未纳入推荐列表，建议先与企业或服务机构确认是否有替代安排。";
        }
        if (skillScore < 65) {
            return "建议在投递说明中补充相关项目经验、课程训练或可迁移技能。";
        }
        if (workModeScore < 65) {
            return "建议在投递前先确认到岗方式、城市安排和工作节奏。";
        }
        if (communicationScore < 65) {
            return "建议在投递说明中提前写明偏好的面试方式和沟通安排。";
        }
        if (environmentScore < 65 || accommodationScore < 65) {
            return "建议在投递前确认现场环境与合理便利是否可落实。";
        }
        if ("SUMMARY".equalsIgnoreCase(context.supportNeedSnapshot().supportVisibility())) {
            return "可在投递时授权展示支持需求摘要，提升后续沟通效率。";
        }
        return "建议在投递说明中突出与岗位直接相关的能力和协作方式。";
    }

    private RiskDecision evaluateRiskDecision(EnterpriseJobPosting jobPosting,
                                              MatchingContext context,
                                              List<String> risks,
                                              MatchingConfigService.RuntimeConfig config) {
        MatchingConfigService.Risk riskConfig = config.risk();
        List<String> blockingRisks = new ArrayList<>();
        String preferredMode = normalizeEnum(context.workModePreference());
        boolean remoteCapable = isRemoteCapable(jobPosting);
        boolean onsiteOnly = Boolean.TRUE.equals(jobPosting.getOnsiteRequired()) && !remoteCapable;
        SupportNeedSnapshot support = context.supportNeedSnapshot();

        if ("REMOTE".equals(preferredMode) && onsiteOnly) {
            blockingRisks.add("岗位要求线下到岗且不支持远程，不纳入当前推荐。");
        }
        if (support.textCommunicationPreferred() && Boolean.TRUE.equals(jobPosting.getHighFrequencyVoiceRequired())) {
            blockingRisks.add("岗位依赖高频语音沟通，不纳入当前推荐。");
        }
        if (support.accessibleWorkspaceNeeded()
                && onsiteOnly
                && !Boolean.TRUE.equals(jobPosting.getAccessibleWorkspace())) {
            blockingRisks.add("岗位需要线下到岗但未标注无障碍办公条件，不纳入当前推荐。");
        }

        int riskPenalty = risks.size() * Math.max(riskConfig.penaltyPerRisk(), 0)
                + blockingRisks.size() * Math.max(riskConfig.penaltyPerBlockingRisk(), 0);
        return new RiskDecision(Math.min(riskPenalty, Math.max(riskConfig.maxPenalty(), 0)), List.copyOf(blockingRisks));
    }

    private List<String> mergeRisks(List<String> blockingRisks, List<String> risks) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(blockingRisks);
        merged.addAll(risks);
        return List.copyOf(merged);
    }

    private int applyRiskPenalty(int baseScore,
                                 RiskDecision riskDecision,
                                 MatchingConfigService.RuntimeConfig config) {
        MatchingConfigService.Risk riskConfig = config.risk();
        int penalized = clamp(baseScore - riskDecision.riskPenalty(), 0, 96);
        if (riskDecision.hardFiltered()) {
            return Math.min(penalized, clamp(riskConfig.hardFilteredMaxScore(), 0, 96));
        }
        return penalized;
    }

    private int calculateMatchScore(int skillScore,
                                    int workModeScore,
                                    int communicationScore,
                                    int environmentScore,
                                    int accommodationScore,
                                    MatchingConfigService.RuntimeConfig config) {
        MatchingConfigService.ScoreWeights weights = config.scoreWeights();
        double skillWeight = positiveWeight(weights.skill());
        double workModeWeight = positiveWeight(weights.workMode());
        double communicationWeight = positiveWeight(weights.communication());
        double environmentWeight = positiveWeight(weights.environment());
        double accommodationWeight = positiveWeight(weights.accommodation());
        double totalWeight = skillWeight + workModeWeight + communicationWeight + environmentWeight + accommodationWeight;

        if (totalWeight <= 0) {
            return (int) Math.round((skillScore + workModeScore + communicationScore + environmentScore + accommodationScore) / 5.0);
        }

        return (int) Math.round(
                (skillScore * skillWeight
                        + workModeScore * workModeWeight
                        + communicationScore * communicationWeight
                        + environmentScore * environmentWeight
                        + accommodationScore * accommodationWeight) / totalWeight
        );
    }

    private boolean matchesSkill(String jobText, JobseekerSkillRecord skill) {
        if (containsAnyToken(jobText, tokenize(skill.skillName()))) {
            return true;
        }
        return containsAnyToken(jobText, tokenize(skill.skillCode()));
    }

    private List<String> splitTextItems(String text) {
        String normalized = safeText(text).replace("\r", "");
        if (normalized.isBlank()) {
            return List.of();
        }
        String[] rawItems = normalized.contains("\n")
                ? normalized.split("\\n+")
                : normalized.split("[；;]+");
        List<String> items = new ArrayList<>();
        for (String rawItem : rawItems) {
            String item = rawItem.trim();
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        return items.isEmpty() ? List.of(normalized) : List.copyOf(items);
    }

    private List<String> firstNonEmpty(List<String> primary, List<String> fallback) {
        return primary != null && !primary.isEmpty() ? List.copyOf(primary) : fallback;
    }

    private String buildSummary(EnterpriseJobPosting jobPosting) {
        List<String> descriptionItems = firstNonEmpty(jobPosting.getDescriptionItems(), splitTextItems(jobPosting.getDescriptionText()));
        if (descriptionItems.isEmpty()) {
            return "岗位信息待补充。";
        }
        String first = descriptionItems.get(0).trim();
        return first.length() <= 56 ? first : first.substring(0, 56) + "...";
    }

    private Set<String> tokenize(String rawText) {
        if (!hasText(rawText)) {
            return Set.of();
        }
        String normalized = normalize(rawText);

        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        addTokenWithAliases(tokens, normalized);
        for (String token : normalized.split("[\\s/\\\\,，、;；()（）_-]+")) {
            addTokenWithAliases(tokens, token);
        }
        return tokens;
    }

    private boolean containsAnyToken(String text, Set<String> tokens) {
        if (!hasText(text) || tokens.isEmpty()) {
            return false;
        }
        String normalizedText = normalize(text);
        for (String token : tokens) {
            if (matchesNormalizedToken(normalizedText, token)) {
                return true;
            }
        }
        return false;
    }

    private void addTokenWithAliases(Set<String> tokens, String candidate) {
        String trimmed = safeText(candidate).trim();
        if (trimmed.length() >= 2) {
            tokens.add(trimmed);
            for (String alias : tokenAliases(trimmed)) {
                if (alias.length() >= 2) {
                    tokens.add(alias);
                }
            }
        }
    }

    private List<String> tokenAliases(String token) {
        return switch (token) {
            case "c++" -> List.of("cpp");
            case "cpp" -> List.of("c++");
            case "c#" -> List.of("csharp");
            case "csharp" -> List.of("c#");
            case ".net" -> List.of("dotnet");
            case "dotnet" -> List.of(".net");
            case "javascript" -> List.of("js");
            case "js" -> List.of("javascript");
            case "typescript" -> List.of("ts");
            case "ts" -> List.of("typescript");
            default -> List.of();
        };
    }

    private boolean matchesNormalizedToken(String normalizedText, String token) {
        if (!hasText(normalizedText) || !hasText(token) || token.length() < 2) {
            return false;
        }
        if (requiresWordBoundaryMatch(token)) {
            String boundaryPattern = "(?<![\\p{L}\\p{N}+#.])"
                    + Pattern.quote(token)
                    + "(?![\\p{L}\\p{N}+#.])";
            return Pattern.compile(boundaryPattern).matcher(normalizedText).find();
        }
        return normalizedText.contains(token);
    }

    private boolean requiresWordBoundaryMatch(String token) {
        for (int index = 0; index < token.length(); index++) {
            char character = token.charAt(index);
            if (character > 127) {
                return false;
            }
        }
        return true;
    }

    private boolean isRemoteCapable(EnterpriseJobPosting jobPosting) {
        String jobMode = normalizeEnum(jobPosting.getWorkMode());
        return Boolean.TRUE.equals(jobPosting.getRemoteSupported())
                || "REMOTE".equals(jobMode)
                || "HYBRID".equals(jobMode);
    }

    private String normalize(String text) {
        return safeText(text)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ");
    }

    private String normalizeEnum(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private boolean hasProfile(MatchingContext context) {
        return hasText(context.expectedJob())
                || hasText(context.workModePreference())
                || !context.skills().isEmpty()
                || context.supportNeedSnapshot().hasAnyNeed();
    }

    private int sanitizePage(Integer page) {
        return page == null ? DEFAULT_PAGE : Math.max(page, 1);
    }

    private int sanitizePageSize(Integer pageSize) {
        int safePageSize = pageSize == null ? DEFAULT_PAGE_SIZE : Math.max(pageSize, 1);
        return Math.min(safePageSize, MAX_PAGE_SIZE);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double positiveWeight(double value) {
        return Math.max(value, 0D);
    }

    private String firstText(String first, String second) {
        return hasText(first) ? first.trim() : hasText(second) ? second.trim() : "";
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record MatchingContext(
            JobseekerProfile profile,
            List<JobseekerSkillRecord> skills,
            SupportNeedSnapshot supportNeedSnapshot
    ) {
        String expectedJob() {
            return profile == null ? "" : profile.getExpectedJob();
        }

        String major() {
            return profile == null ? "" : profile.getMajor();
        }

        String targetCity() {
            return profile == null ? "" : profile.getTargetCity();
        }

        String currentCity() {
            return profile == null ? "" : profile.getCurrentCity();
        }

        String workModePreference() {
            return profile == null ? "" : profile.getWorkModePreference();
        }
    }

    private record MatchAssessment(
            JobResponse response,
            boolean hardFiltered
    ) {
    }

    private record RiskDecision(
            int riskPenalty,
            List<String> blockingRisks
    ) {
        boolean hardFiltered() {
            return !blockingRisks.isEmpty();
        }
    }

    private static final class MatchEvidence {
        private final LinkedHashSet<String> matchedSkills = new LinkedHashSet<>();
        private boolean expectedJobMatched;
        private boolean workModeMatched;
        private boolean locationFlexible;
        private boolean textFriendly;
        private boolean environmentStable;
        private boolean accommodationFriendly;
    }
}
