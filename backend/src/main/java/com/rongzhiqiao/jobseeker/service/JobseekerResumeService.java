package com.rongzhiqiao.jobseeker.service;

import com.rongzhiqiao.auth.entity.SysUser;
import com.rongzhiqiao.auth.mapper.SysUserMapper;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.jobseeker.vo.JobApplicationResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerAbilityCardResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerProfileResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerResumePreviewResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerSkillTagResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerSupportNeedResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobseekerResumeService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JobseekerProfileService jobseekerProfileService;
    private final JobseekerSupportNeedService jobseekerSupportNeedService;
    private final JobApplicationService jobApplicationService;
    private final SysUserMapper sysUserMapper;
    private final JobseekerResumeExporter jobseekerResumeExporter;

    public JobseekerResumePreviewResponse getCurrentResumePreview() {
        Long userId = SecurityUtils.getCurrentUserId();
        JobseekerProfileResponse profile = jobseekerProfileService.getCurrentProfile();
        JobseekerSupportNeedResponse supportNeeds = jobseekerSupportNeedService.getCurrentSupportNeeds();
        List<JobApplicationResponse> recentApplications = jobApplicationService.listCurrentUserApplications().stream()
                .limit(5)
                .toList();
        SysUser user = sysUserMapper.selectById(userId);

        String displayName = resolveDisplayName(profile, user);
        String headline = buildHeadline(profile);
        String summary = buildSummary(profile, supportNeeds);
        int completionRate = profile == null || profile.getProfileCompletionRate() == null
                ? 0
                : profile.getProfileCompletionRate();

        return new JobseekerResumePreviewResponse(
                displayName,
                headline,
                summary,
                completionRate,
                profile,
                supportNeeds,
                recentApplications,
                buildStrengths(profile, supportNeeds, recentApplications),
                buildSuggestions(profile, supportNeeds),
                "bridgeability-resume-" + userId,
                LocalDateTime.now().format(DATE_TIME_FORMATTER)
        );
    }

    public JobseekerResumeExporter.ResumeExportPayload exportCurrentResume(String format) {
        JobseekerResumePreviewResponse preview = getCurrentResumePreview();
        return jobseekerResumeExporter.export(preview, preview.exportFileName(), format);
    }

    private String resolveDisplayName(JobseekerProfileResponse profile, SysUser user) {
        if (profile != null && hasText(profile.getRealName())) {
            return profile.getRealName().trim();
        }
        if (user != null && hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (user != null && hasText(user.getAccount())) {
            return user.getAccount().trim();
        }
        return "未命名求职者";
    }

    private String buildHeadline(JobseekerProfileResponse profile) {
        if (profile == null) {
            return "结构化无障碍简历";
        }

        List<String> parts = new ArrayList<>();
        if (hasText(profile.getExpectedJob())) {
            parts.add(profile.getExpectedJob().trim());
        }
        if (hasText(profile.getTargetCity())) {
            parts.add(profile.getTargetCity().trim());
        }
        if (hasText(profile.getWorkModePreference())) {
            parts.add(profile.getWorkModePreference().trim());
        }
        return parts.isEmpty() ? "结构化无障碍简历" : String.join(" / ", parts);
    }

    private String buildSummary(JobseekerProfileResponse profile, JobseekerSupportNeedResponse supportNeeds) {
        if (profile != null && hasText(profile.getIntro())) {
            return profile.getIntro().trim();
        }
        if (profile != null && profile.getAbilityCards() != null && !profile.getAbilityCards().isEmpty()) {
            return profile.getAbilityCards().get(0).summary();
        }
        if (supportNeeds != null && hasText(supportNeeds.summaryText())) {
            return "已整理沟通与协作偏好，可用于安排更稳定的面试与工作协同流程。";
        }
        return "当前简历仍在完善中，建议优先补充目标岗位、核心技能和项目经历。";
    }

    private List<String> buildStrengths(JobseekerProfileResponse profile,
                                        JobseekerSupportNeedResponse supportNeeds,
                                        List<JobApplicationResponse> recentApplications) {
        List<String> strengths = new ArrayList<>();

        if (profile != null && profile.getSkillTags() != null && !profile.getSkillTags().isEmpty()) {
            List<String> topSkills = profile.getSkillTags().stream()
                    .limit(3)
                    .map(JobseekerSkillTagResponse::skillName)
                    .toList();
            strengths.add("已沉淀核心技能：" + String.join("、", topSkills));
        }
        if (profile != null && profile.getProjectExperiences() != null && !profile.getProjectExperiences().isEmpty()) {
            strengths.add("已补充 " + profile.getProjectExperiences().size() + " 段项目经历，可直接用于面试表达。");
        }
        if (supportNeeds != null && supportNeeds.hasAnyNeed()) {
            strengths.add("已建立沟通与便利需求摘要，便于后续投递复用。");
        }
        if (recentApplications != null && !recentApplications.isEmpty()) {
            strengths.add("最近已有 " + recentApplications.size() + " 条投递记录，可回看岗位方向是否聚焦。");
        }
        if (profile != null && profile.getAbilityCards() != null) {
            strengths.addAll(profile.getAbilityCards().stream().limit(2).map(JobseekerAbilityCardResponse::summary).toList());
        }
        return List.copyOf(strengths.stream().filter(this::hasText).distinct().toList());
    }

    private List<String> buildSuggestions(JobseekerProfileResponse profile, JobseekerSupportNeedResponse supportNeeds) {
        List<String> suggestions = new ArrayList<>();

        if (profile == null) {
            suggestions.add("先补充真实姓名、目标岗位和学校专业，预览页才能形成完整结构。");
            suggestions.add("至少补充 3 个技能标签和 1 段项目经历，导出内容会更完整。");
            return List.copyOf(suggestions);
        }

        if (!hasText(profile.getExpectedJob())) {
            suggestions.add("补充目标岗位，便于简历标题和岗位偏好更聚焦。");
        }
        if (!hasText(profile.getTargetCity())) {
            suggestions.add("补充意向城市，方便企业快速判断岗位地点适配度。");
        }
        if (profile.getSkillTags() == null || profile.getSkillTags().isEmpty()) {
            suggestions.add("至少补充 3 个技能标签，能力画像才会更稳定。");
        }
        if (profile.getProjectExperiences() == null || profile.getProjectExperiences().isEmpty()) {
            suggestions.add("补充至少 1 段项目经历，用事实支撑技能与执行能力。");
        }
        if (!hasText(profile.getIntro())) {
            suggestions.add("补一段 2 到 3 句的个人简介，方便预览时形成完整开场。");
        }
        if (supportNeeds != null && !supportNeeds.hasAnyNeed()) {
            suggestions.add("如有长期沟通偏好，可在便利需求页补充，后续投递时可直接复用。");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("当前简历信息较完整，可直接导出并用于岗位投递。");
        }
        return List.copyOf(suggestions);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
