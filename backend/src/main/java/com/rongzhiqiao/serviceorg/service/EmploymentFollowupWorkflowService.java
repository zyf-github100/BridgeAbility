package com.rongzhiqiao.serviceorg.service;

import com.rongzhiqiao.auth.entity.SysUser;
import com.rongzhiqiao.auth.mapper.SysUserMapper;
import com.rongzhiqiao.jobseeker.entity.JobseekerProfile;
import com.rongzhiqiao.jobseeker.mapper.JobseekerProfileMapper;
import com.rongzhiqiao.serviceorg.repository.ServiceOrgRepository;
import com.rongzhiqiao.serviceorg.repository.ServiceOrgRepository.ServiceCaseRecord;
import com.rongzhiqiao.serviceorg.repository.ServiceOrgRepository.ServiceFollowupRecord;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmploymentFollowupWorkflowService {

    private final ServiceOrgRepository serviceOrgRepository;
    private final JobseekerProfileMapper jobseekerProfileMapper;
    private final SysUserMapper sysUserMapper;

    @Transactional
    public void scheduleFollowupsForHire(Long userId,
                                         String candidateName,
                                         String jobId,
                                         String jobTitle,
                                         LocalDateTime hiredAt) {
        if (userId == null || jobId == null || jobId.isBlank()) {
            return;
        }

        ServiceCaseRecord serviceCase = ensureCase(userId, candidateName, jobTitle, hiredAt);
        ensureScheduledFollowup(serviceCase.id(), jobId, "DAY_7", hiredAt, hiredAt.plusDays(7));
        ensureScheduledFollowup(serviceCase.id(), jobId, "DAY_30", hiredAt, hiredAt.plusDays(30));

        int openAlertLevel = serviceOrgRepository.maxOpenAlertLevel(serviceCase.id());
        serviceOrgRepository.updateCaseProgress(
                serviceCase.id(),
                openAlertLevel > 0 ? "继续跟进未处理预警" : "待完成 7 天回访",
                toAlertLevelLabel(openAlertLevel)
        );
    }

    @Transactional
    public void recordJobseekerFollowup(Long userId,
                                        String candidateName,
                                        String jobId,
                                        String jobTitle,
                                        String followupStage,
                                        int adaptationScore,
                                        String environmentIssue,
                                        String communicationIssue,
                                        boolean supportImplemented,
                                        boolean leaveRisk,
                                        boolean needHelp,
                                        String operatorName) {
        if (userId == null || jobId == null || jobId.isBlank()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        ServiceCaseRecord serviceCase = ensureCase(userId, candidateName, jobTitle, now);
        ServiceFollowupRecord existing = serviceOrgRepository.findLatestFollowupRecord(serviceCase.id(), jobId, followupStage);
        if (existing == null) {
            serviceOrgRepository.insertFollowup(
                    serviceCase.id(),
                    userId,
                    jobId,
                    followupStage,
                    adaptationScore,
                    environmentIssue,
                    communicationIssue,
                    supportImplemented,
                    leaveRisk,
                    needHelp,
                    operatorName,
                    now
            );
        } else {
            serviceOrgRepository.updateFollowupCompletion(
                    existing.id(),
                    adaptationScore,
                    environmentIssue,
                    communicationIssue,
                    supportImplemented,
                    leaveRisk,
                    needHelp,
                    operatorName,
                    now
            );
        }

        if (shouldCreateAlert(adaptationScore, supportImplemented, leaveRisk, needHelp)) {
            serviceOrgRepository.insertAlert(
                    generateAlertId(),
                    serviceCase.id(),
                    userId,
                    serviceCase.name(),
                    "DAY_30".equals(followupStage) ? "30天回访预警" : "7天回访预警",
                    determineAlertLevel(adaptationScore, supportImplemented, leaveRisk, needHelp),
                    buildFollowupAlertReason(adaptationScore, environmentIssue, communicationIssue, supportImplemented, leaveRisk, needHelp),
                    "OPEN",
                    now,
                    serviceOrgRepository.nextAlertSortNo()
            );
        }

        serviceOrgRepository.updateCaseProgress(
                serviceCase.id(),
                buildNextActionForFollowup(followupStage, supportImplemented, leaveRisk, needHelp),
                toAlertLevelLabel(serviceOrgRepository.maxOpenAlertLevel(serviceCase.id()))
        );
    }

    private ServiceCaseRecord ensureCase(Long userId,
                                         String preferredName,
                                         String jobTitle,
                                         LocalDateTime referenceTime) {
        ServiceCaseRecord existing = serviceOrgRepository.findCaseByUserId(userId);
        if (existing != null) {
            return existing;
        }

        String displayName = resolveDisplayName(userId, preferredName);
        List<String> timeline = new ArrayList<>();
        timeline.add(referenceTime.toLocalDate().toString() + " 系统自动创建入职跟进个案");
        if (jobTitle != null && !jobTitle.isBlank()) {
            timeline.add(referenceTime.toLocalDate().toString() + " 入职岗位：" + jobTitle.trim());
        }
        return serviceOrgRepository.insertCase(
                generateCaseId(userId),
                userId,
                displayName,
                "入职跟进",
                "待分配",
                "待完成 7 天回访",
                "无",
                null,
                false,
                null,
                "SYSTEM",
                referenceTime,
                timeline,
                serviceOrgRepository.nextCaseSortNo()
        );
    }

    private void ensureScheduledFollowup(String caseId,
                                         String jobId,
                                         String followupStage,
                                         LocalDateTime createdAt,
                                         LocalDateTime dueAt) {
        if (serviceOrgRepository.hasFollowupRecord(caseId, jobId, followupStage)) {
            return;
        }
        serviceOrgRepository.insertScheduledFollowup(caseId, jobId, followupStage, "SYSTEM", createdAt, dueAt);
    }

    private String resolveDisplayName(Long userId, String preferredName) {
        String trimmedPreferredName = trimToNull(preferredName);
        if (trimmedPreferredName != null) {
            return trimmedPreferredName;
        }
        JobseekerProfile profile = jobseekerProfileMapper.selectByUserId(userId);
        if (profile != null && trimToNull(profile.getRealName()) != null) {
            return profile.getRealName().trim();
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null && trimToNull(user.getNickname()) != null) {
            return user.getNickname().trim();
        }
        if (user != null && trimToNull(user.getAccount()) != null) {
            return user.getAccount().trim();
        }
        return "求职者";
    }

    private boolean shouldCreateAlert(int adaptationScore,
                                      boolean supportImplemented,
                                      boolean leaveRisk,
                                      boolean needHelp) {
        return leaveRisk || needHelp || !supportImplemented || adaptationScore < 70;
    }

    private int determineAlertLevel(int adaptationScore,
                                    boolean supportImplemented,
                                    boolean leaveRisk,
                                    boolean needHelp) {
        if (leaveRisk) {
            return 3;
        }
        if (needHelp || !supportImplemented || adaptationScore < 60) {
            return 2;
        }
        return 1;
    }

    private String buildFollowupAlertReason(int adaptationScore,
                                            String environmentIssue,
                                            String communicationIssue,
                                            boolean supportImplemented,
                                            boolean leaveRisk,
                                            boolean needHelp) {
        List<String> reasons = new ArrayList<>();
        if (adaptationScore < 70) {
            reasons.add("适应评分偏低");
        }
        if (!supportImplemented) {
            reasons.add("便利支持未完全落实");
        }
        if (leaveRisk) {
            reasons.add("存在离职风险");
        }
        if (needHelp) {
            reasons.add("需要机构继续介入");
        }
        String normalizedEnvironmentIssue = trimToNull(environmentIssue);
        if (normalizedEnvironmentIssue != null) {
            reasons.add("环境问题：" + normalizedEnvironmentIssue);
        }
        String normalizedCommunicationIssue = trimToNull(communicationIssue);
        if (normalizedCommunicationIssue != null) {
            reasons.add("沟通问题：" + normalizedCommunicationIssue);
        }
        return String.join("；", reasons);
    }

    private String buildNextActionForFollowup(String followupStage,
                                              boolean supportImplemented,
                                              boolean leaveRisk,
                                              boolean needHelp) {
        if (leaveRisk || needHelp) {
            return "处理回访风险并确认支持落实";
        }
        if (!supportImplemented) {
            return "推动企业落实已承诺的便利支持";
        }
        if ("DAY_7".equals(followupStage)) {
            return "准备 30 天回访";
        }
        return "继续常规稳定跟进";
    }

    private String toAlertLevelLabel(int level) {
        if (level >= 3) {
            return "高";
        }
        if (level == 2) {
            return "中";
        }
        if (level == 1) {
            return "低";
        }
        return "无";
    }

    private String generateCaseId(Long userId) {
        long timestamp = System.currentTimeMillis();
        int suffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "case-auto-" + Long.toString(timestamp, 36) + "-" + userId + "-" + suffix;
    }

    private String generateAlertId() {
        long timestamp = System.currentTimeMillis();
        int suffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "alert-auto-" + Long.toString(timestamp, 36) + "-" + suffix;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
