package com.rongzhiqiao.jobseeker.service;

import com.rongzhiqiao.auth.entity.SysUser;
import com.rongzhiqiao.auth.mapper.SysUserMapper;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.jobseeker.dto.JobseekerProfileUpsertRequest;
import com.rongzhiqiao.jobseeker.dto.JobseekerProjectUpsertRequest;
import com.rongzhiqiao.jobseeker.dto.JobseekerSkillUpsertRequest;
import com.rongzhiqiao.jobseeker.entity.JobseekerProfile;
import com.rongzhiqiao.jobseeker.mapper.JobseekerProfileMapper;
import com.rongzhiqiao.jobseeker.repository.JobseekerProjectRepository;
import com.rongzhiqiao.jobseeker.repository.JobseekerProjectRepository.JobseekerProjectRecord;
import com.rongzhiqiao.jobseeker.repository.JobseekerProjectRepository.ProjectDraft;
import com.rongzhiqiao.jobseeker.repository.JobseekerSkillRepository;
import com.rongzhiqiao.jobseeker.repository.JobseekerSkillRepository.JobseekerSkillRecord;
import com.rongzhiqiao.jobseeker.repository.JobseekerSkillRepository.SkillDraft;
import com.rongzhiqiao.jobseeker.vo.JobseekerAbilityCardResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerProfileResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerProjectExperienceResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerSkillTagResponse;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobseekerProfileService {

    private static final int MAX_SKILL_COUNT = 20;
    private static final int MAX_PROJECT_COUNT = 10;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final JobseekerProfileMapper jobseekerProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final JobseekerSkillRepository jobseekerSkillRepository;
    private final JobseekerProjectRepository jobseekerProjectRepository;

    public JobseekerProfileResponse getCurrentProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return getProfileAggregate(userId);
    }

    public JobseekerProfileResponse getProfileByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return getProfileAggregate(userId);
    }

    @Transactional
    public JobseekerProfileResponse saveCurrentProfile(JobseekerProfileUpsertRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(4004, "user not found");
        }
        validateRequest(request);

        JobseekerProfile existing = jobseekerProfileMapper.selectByUserId(userId);
        List<JobseekerSkillRecord> existingSkills = jobseekerSkillRepository.listByUserId(userId);
        List<JobseekerProjectRecord> existingProjects = jobseekerProjectRepository.listByUserId(userId);

        List<SkillDraft> effectiveSkills = request.getSkillTags() == null
                ? mapExistingSkills(existingSkills)
                : normalizeSkillDrafts(request.getSkillTags());
        List<ProjectDraft> effectiveProjects = request.getProjectExperiences() == null
                ? mapExistingProjects(existingProjects)
                : normalizeProjectDrafts(request.getProjectExperiences());

        JobseekerProfile profile = buildProfile(userId, request, effectiveSkills, effectiveProjects);
        if (existing == null) {
            profile.setIsDeleted(0);
            jobseekerProfileMapper.insert(profile);
        } else {
            profile.setId(existing.getId());
            jobseekerProfileMapper.updateByUserId(profile);
        }

        if (request.getSkillTags() != null) {
            jobseekerSkillRepository.replaceAll(userId, effectiveSkills);
        }
        if (request.getProjectExperiences() != null) {
            jobseekerProjectRepository.replaceAll(userId, effectiveProjects);
        }

        return getProfileAggregate(userId);
    }

    private JobseekerProfileResponse getProfileAggregate(Long userId) {
        JobseekerProfile profile = jobseekerProfileMapper.selectByUserId(userId);
        if (profile == null) {
            return null;
        }
        List<JobseekerSkillTagResponse> skillTags = jobseekerSkillRepository.listByUserId(userId).stream()
                .map(this::toSkillResponse)
                .toList();
        List<JobseekerProjectExperienceResponse> projectExperiences = jobseekerProjectRepository.listByUserId(userId).stream()
                .map(this::toProjectResponse)
                .toList();

        return JobseekerProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .realName(profile.getRealName())
                .gender(profile.getGender())
                .birthYear(profile.getBirthYear())
                .schoolName(profile.getSchoolName())
                .major(profile.getMajor())
                .degree(profile.getDegree())
                .graduationYear(profile.getGraduationYear())
                .currentCity(profile.getCurrentCity())
                .targetCity(profile.getTargetCity())
                .expectedJob(profile.getExpectedJob())
                .expectedSalaryMin(profile.getExpectedSalaryMin())
                .expectedSalaryMax(profile.getExpectedSalaryMax())
                .workModePreference(profile.getWorkModePreference())
                .intro(profile.getIntro())
                .profileCompletionRate(profile.getProfileCompletionRate())
                .skillTags(skillTags)
                .projectExperiences(projectExperiences)
                .abilityCards(buildAbilityCards(profile, skillTags, projectExperiences))
                .build();
    }

    private JobseekerProfile buildProfile(Long userId,
                                          JobseekerProfileUpsertRequest request,
                                          List<SkillDraft> skillDrafts,
                                          List<ProjectDraft> projectDrafts) {
        JobseekerProfile profile = new JobseekerProfile();
        profile.setUserId(userId);
        profile.setRealName(trimToNull(request.getRealName()));
        profile.setGender(trimToNull(request.getGender()));
        profile.setBirthYear(request.getBirthYear());
        profile.setSchoolName(trimToNull(request.getSchoolName()));
        profile.setMajor(trimToNull(request.getMajor()));
        profile.setDegree(trimToNull(request.getDegree()));
        profile.setGraduationYear(request.getGraduationYear());
        profile.setCurrentCity(trimToNull(request.getCurrentCity()));
        profile.setTargetCity(trimToNull(request.getTargetCity()));
        profile.setExpectedJob(trimToNull(request.getExpectedJob()));
        profile.setExpectedSalaryMin(request.getExpectedSalaryMin());
        profile.setExpectedSalaryMax(request.getExpectedSalaryMax());
        profile.setWorkModePreference(trimToNull(request.getWorkModePreference()));
        profile.setIntro(trimToNull(request.getIntro()));
        profile.setProfileCompletionRate(calculateCompletionRate(profile, skillDrafts, projectDrafts));
        return profile;
    }

    private void validateRequest(JobseekerProfileUpsertRequest request) {
        int currentYear = Year.now().getValue();
        if (request.getBirthYear() != null && (request.getBirthYear() < 1900 || request.getBirthYear() > currentYear)) {
            throw new BusinessException(4001, "birth year out of range");
        }
        if (request.getGraduationYear() != null
                && (request.getGraduationYear() < 1950 || request.getGraduationYear() > currentYear + 10)) {
            throw new BusinessException(4001, "graduation year out of range");
        }
        if (request.getExpectedSalaryMin() != null
                && request.getExpectedSalaryMax() != null
                && request.getExpectedSalaryMin().compareTo(request.getExpectedSalaryMax()) > 0) {
            throw new BusinessException(4001, "expectedSalaryMin cannot be greater than expectedSalaryMax");
        }
        if (request.getSkillTags() != null && request.getSkillTags().size() > MAX_SKILL_COUNT) {
            throw new BusinessException(4001, "skillTags cannot exceed " + MAX_SKILL_COUNT);
        }
        if (request.getProjectExperiences() != null && request.getProjectExperiences().size() > MAX_PROJECT_COUNT) {
            throw new BusinessException(4001, "projectExperiences cannot exceed " + MAX_PROJECT_COUNT);
        }

        Set<String> normalizedSkillKeys = new LinkedHashSet<>();
        if (request.getSkillTags() != null) {
            for (JobseekerSkillUpsertRequest skill : request.getSkillTags()) {
                String skillName = trimToNull(skill.getSkillName());
                int skillLevel = normalizeSkillLevel(skill.getSkillLevel());
                String skillCode = normalizeSkillCode(skill.getSkillCode(), skillName);
                if (!normalizedSkillKeys.add(skillCode)) {
                    throw new BusinessException(4001, "skillTags contains duplicated skills");
                }
                if (skillLevel < 1 || skillLevel > 5) {
                    throw new BusinessException(4001, "skillLevel must be between 1 and 5");
                }
            }
        }

        if (request.getProjectExperiences() != null) {
            for (JobseekerProjectUpsertRequest project : request.getProjectExperiences()) {
                LocalDate startDate = project.getStartDate();
                LocalDate endDate = project.getEndDate();
                if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                    throw new BusinessException(4001, "project endDate cannot be earlier than startDate");
                }
            }
        }
    }

    private int calculateCompletionRate(JobseekerProfile profile,
                                        List<SkillDraft> skillDrafts,
                                        List<ProjectDraft> projectDrafts) {
        int totalFields = 16;
        int filledFields = 0;
        filledFields += hasText(profile.getRealName()) ? 1 : 0;
        filledFields += hasText(profile.getGender()) ? 1 : 0;
        filledFields += profile.getBirthYear() != null ? 1 : 0;
        filledFields += hasText(profile.getSchoolName()) ? 1 : 0;
        filledFields += hasText(profile.getMajor()) ? 1 : 0;
        filledFields += hasText(profile.getDegree()) ? 1 : 0;
        filledFields += profile.getGraduationYear() != null ? 1 : 0;
        filledFields += hasText(profile.getCurrentCity()) ? 1 : 0;
        filledFields += hasText(profile.getTargetCity()) ? 1 : 0;
        filledFields += hasText(profile.getExpectedJob()) ? 1 : 0;
        filledFields += profile.getExpectedSalaryMin() != null ? 1 : 0;
        filledFields += profile.getExpectedSalaryMax() != null ? 1 : 0;
        filledFields += hasText(profile.getWorkModePreference()) ? 1 : 0;
        filledFields += hasText(profile.getIntro()) ? 1 : 0;
        filledFields += skillDrafts != null && !skillDrafts.isEmpty() ? 1 : 0;
        filledFields += projectDrafts != null && !projectDrafts.isEmpty() ? 1 : 0;
        return Math.toIntExact(Math.round(filledFields * 100.0 / totalFields));
    }

    private List<SkillDraft> normalizeSkillDrafts(List<JobseekerSkillUpsertRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<SkillDraft> drafts = new ArrayList<>();
        for (JobseekerSkillUpsertRequest request : requests) {
            String skillName = trimToNull(request.getSkillName());
            int skillLevel = normalizeSkillLevel(request.getSkillLevel());
            drafts.add(new SkillDraft(
                    normalizeSkillCode(request.getSkillCode(), skillName),
                    skillName,
                    skillLevel
            ));
        }
        return List.copyOf(drafts);
    }

    private List<ProjectDraft> normalizeProjectDrafts(List<JobseekerProjectUpsertRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<ProjectDraft> drafts = new ArrayList<>();
        for (JobseekerProjectUpsertRequest request : requests) {
            drafts.add(new ProjectDraft(
                    trimToNull(request.getProjectName()),
                    trimToNull(request.getRoleName()),
                    trimToNull(request.getDescription()),
                    request.getStartDate(),
                    request.getEndDate()
            ));
        }
        return List.copyOf(drafts);
    }

    private List<SkillDraft> mapExistingSkills(List<JobseekerSkillRecord> existingSkills) {
        if (existingSkills == null || existingSkills.isEmpty()) {
            return List.of();
        }
        return existingSkills.stream()
                .map(skill -> new SkillDraft(skill.skillCode(), skill.skillName(), skill.skillLevel()))
                .toList();
    }

    private List<ProjectDraft> mapExistingProjects(List<JobseekerProjectRecord> existingProjects) {
        if (existingProjects == null || existingProjects.isEmpty()) {
            return List.of();
        }
        return existingProjects.stream()
                .map(project -> new ProjectDraft(
                        project.projectName(),
                        project.roleName(),
                        project.description(),
                        project.startDate(),
                        project.endDate()
                ))
                .toList();
    }

    private JobseekerSkillTagResponse toSkillResponse(JobseekerSkillRecord skill) {
        return new JobseekerSkillTagResponse(
                skill.skillCode(),
                skill.skillName(),
                skill.skillLevel(),
                getSkillLevelLabel(skill.skillLevel())
        );
    }

    private JobseekerProjectExperienceResponse toProjectResponse(JobseekerProjectRecord project) {
        return new JobseekerProjectExperienceResponse(
                project.id(),
                project.projectName(),
                blankToDefault(project.roleName(), "成员"),
                blankToDefault(project.description(), "暂未补充项目描述"),
                formatDate(project.startDate()),
                formatDate(project.endDate()),
                buildPeriodLabel(project.startDate(), project.endDate())
        );
    }

    private List<JobseekerAbilityCardResponse> buildAbilityCards(JobseekerProfile profile,
                                                                 List<JobseekerSkillTagResponse> skillTags,
                                                                 List<JobseekerProjectExperienceResponse> projectExperiences) {
        List<JobseekerAbilityCardResponse> cards = new ArrayList<>();

        List<String> topSkills = skillTags.stream()
                .limit(3)
                .map(skill -> skill.skillName() + "（" + skill.skillLevelLabel() + "）")
                .toList();
        String skillSummary = topSkills.isEmpty()
                ? "先补充技能标签，系统才能更准确地输出你的能力长板。"
                : "当前最突出的能力标签集中在 " + String.join("、", topSkills) + "。";
        List<String> skillHighlights = new ArrayList<>();
        if (hasText(profile.getMajor())) {
            skillHighlights.add("专业背景：" + profile.getMajor());
        }
        if (hasText(profile.getDegree())) {
            skillHighlights.add("学历层次：" + profile.getDegree());
        }
        if (topSkills.isEmpty()) {
            skillHighlights.add("建议至少补充 3 个技能标签。");
        } else {
            skillHighlights.addAll(topSkills);
        }
        cards.add(new JobseekerAbilityCardResponse(
                "CORE_SKILLS",
                "能力长板卡",
                skillSummary,
                List.copyOf(skillHighlights)
        ));

        List<String> projectHighlights = projectExperiences.stream()
                .limit(2)
                .map(project -> project.projectName() + " / " + project.roleName())
                .toList();
        String projectSummary = projectExperiences.isEmpty()
                ? "还没有项目经历，建议补充至少一段项目或实践经历。"
                : "已沉淀 " + projectExperiences.size() + " 段项目经历，可用于说明你的任务理解和执行能力。";
        cards.add(new JobseekerAbilityCardResponse(
                "PROJECT_DELIVERY",
                "项目经历卡",
                projectSummary,
                projectHighlights.isEmpty()
                        ? List.of("优先补充项目名称、角色、任务和起止时间。")
                        : projectHighlights
        ));

        List<String> preferenceHighlights = new ArrayList<>();
        if (hasText(profile.getExpectedJob())) {
            preferenceHighlights.add("目标岗位：" + profile.getExpectedJob());
        }
        if (hasText(profile.getTargetCity())) {
            preferenceHighlights.add("意向城市：" + profile.getTargetCity());
        }
        if (hasText(profile.getWorkModePreference())) {
            preferenceHighlights.add("工作方式：" + profile.getWorkModePreference());
        }
        if (preferenceHighlights.isEmpty()) {
            preferenceHighlights.add("建议补充目标岗位、城市和工作方式偏好。");
        }
        String preferenceSummary = preferenceHighlights.size() == 1 && preferenceHighlights.get(0).startsWith("建议")
                ? "岗位偏好信息还不完整，影响系统进行能力与岗位的双向匹配。"
                : "系统将根据岗位目标、地点和工作方式偏好生成更聚焦的推荐结果。";
        cards.add(new JobseekerAbilityCardResponse(
                "JOB_PREFERENCE",
                "岗位偏好卡",
                preferenceSummary,
                List.copyOf(preferenceHighlights)
        ));

        return List.copyOf(cards);
    }

    private int normalizeSkillLevel(Integer value) {
        return value == null ? 3 : value;
    }

    private String normalizeSkillCode(String rawCode, String skillName) {
        String base = hasText(rawCode) ? rawCode : skillName;
        if (!hasText(base)) {
            throw new BusinessException(4001, "skillName is required");
        }
        String normalized = base.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.isBlank()) {
            throw new BusinessException(4001, "skillCode is invalid");
        }
        return normalized;
    }

    private String getSkillLevelLabel(int level) {
        return switch (level) {
            case 1 -> "入门";
            case 2 -> "基础";
            case 3 -> "熟练";
            case 4 -> "进阶";
            case 5 -> "擅长";
            default -> "熟练";
        };
    }

    private String buildPeriodLabel(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "时间未填写";
        }
        if (startDate != null && endDate != null) {
            long months = ChronoUnit.MONTHS.between(startDate.withDayOfMonth(1), endDate.withDayOfMonth(1)) + 1;
            if (months <= 1) {
                return formatDate(startDate) + " - " + formatDate(endDate);
            }
            return formatDate(startDate) + " - " + formatDate(endDate) + " · " + months + " 个月";
        }
        if (startDate != null) {
            return formatDate(startDate) + " 起";
        }
        return "截至 " + formatDate(endDate);
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.format(DATE_FORMATTER);
    }

    private String blankToDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
