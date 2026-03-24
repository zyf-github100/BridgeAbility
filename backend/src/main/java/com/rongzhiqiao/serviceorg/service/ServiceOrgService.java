package com.rongzhiqiao.serviceorg.service;

import com.rongzhiqiao.auth.entity.SysUser;
import com.rongzhiqiao.auth.mapper.SysRoleMapper;
import com.rongzhiqiao.auth.mapper.SysUserMapper;
import com.rongzhiqiao.common.api.PageResponse;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.jobseeker.repository.JobseekerServiceAuthorizationRepository;
import com.rongzhiqiao.jobseeker.service.JobseekerProfileService;
import com.rongzhiqiao.jobseeker.service.JobseekerSupportNeedService;
import com.rongzhiqiao.jobseeker.vo.JobseekerProfileResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerSupportNeedResponse;
import com.rongzhiqiao.serviceorg.dto.ServiceAlertStatusUpdateRequest;
import com.rongzhiqiao.serviceorg.dto.ServiceCaseCreateRequest;
import com.rongzhiqiao.serviceorg.dto.ServiceCaseProfileAccessUpdateRequest;
import com.rongzhiqiao.serviceorg.dto.ServiceFollowupCreateRequest;
import com.rongzhiqiao.serviceorg.dto.ServiceInterventionCreateRequest;
import com.rongzhiqiao.serviceorg.dto.ServiceResourceReferralCreateRequest;
import com.rongzhiqiao.serviceorg.dto.ServiceResourceReferralStatusUpdateRequest;
import com.rongzhiqiao.serviceorg.repository.ServiceOrgRepository;
import com.rongzhiqiao.serviceorg.repository.ServiceOrgRepository.ServiceCaseRecord;
import com.rongzhiqiao.serviceorg.vo.ServiceAlertResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceCaseDetailResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceCaseSummaryResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceFollowupResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceInterventionResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceProfileAccessResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceResourceReferralResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceOrgService {

    private static final Set<String> ALLOWED_INTERVENTION_TYPES = Set.of(
            "RESUME_GUIDANCE",
            "INTERVIEW_GUIDANCE",
            "JOB_RECOMMENDATION",
            "PSYCHOLOGICAL_SUPPORT",
            "TRAINING_REFERRAL",
            "EMPLOYER_COMMUNICATION",
            "ONBOARDING_SUPPORT"
    );
    private static final Set<String> ALLOWED_FOLLOWUP_STAGES = Set.of("DAY_7", "DAY_30");
    private static final Set<String> ALLOWED_ALERT_STATUSES = Set.of("OPEN", "RESOLVED", "CLOSED", "ESCALATED");
    private static final Set<String> ALLOWED_REFERRAL_TYPES = Set.of(
            "TRAINING",
            "EMPLOYMENT",
            "COUNSELING",
            "ASSISTIVE_SUPPORT",
            "POLICY_SUPPORT",
            "OTHER"
    );
    private static final Set<String> ALLOWED_REFERRAL_STATUSES = Set.of(
            "PLANNED",
            "IN_PROGRESS",
            "CONNECTED",
            "COMPLETED",
            "CANCELLED"
    );
    private static final List<DateTimeFormatter> SUPPORTED_DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    );
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ServiceOrgRepository serviceOrgRepository;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final JobseekerProfileService jobseekerProfileService;
    private final JobseekerSupportNeedService jobseekerSupportNeedService;
    private final JobseekerServiceAuthorizationRepository jobseekerServiceAuthorizationRepository;

    public List<ServiceCaseSummaryResponse> listCases() {
        return serviceOrgRepository.listCases();
    }

    @Transactional
    public ServiceCaseDetailResponse createCase(ServiceCaseCreateRequest request) {
        String linkedAccount = trimToNull(request.getJobseekerAccount());
        Long linkedUserId = resolveLinkedJobseekerUserId(linkedAccount);
        if (linkedUserId != null && serviceOrgRepository.findCaseByUserId(linkedUserId) != null) {
            throw new BusinessException(4001, "service case already exists for the linked jobseeker");
        }

        boolean profileAuthorized = Boolean.TRUE.equals(request.getProfileAuthorized());
        if (profileAuthorized && linkedUserId == null) {
            throw new BusinessException(4001, "linked jobseeker account is required when profile access is authorized");
        }

        LocalDateTime now = LocalDateTime.now();
        String caseId = generateCaseId("case-manual-");
        List<String> timeline = buildInitialTimeline(
                now,
                trim(request.getStage()),
                trim(request.getNextAction()),
                linkedAccount,
                trimToNull(request.getIntakeNote()),
                profileAuthorized
        );

        serviceOrgRepository.insertCase(
                caseId,
                linkedUserId,
                trim(request.getName()),
                trim(request.getStage()),
                trim(request.getOwnerName()),
                trim(request.getNextAction()),
                "无",
                trimToNull(request.getIntakeNote()),
                profileAuthorized,
                trimToNull(request.getAuthorizationNote()),
                trim(request.getOperatorName()),
                now,
                timeline,
                serviceOrgRepository.nextCaseSortNo()
        );
        syncProfileAuthorization(linkedUserId, profileAuthorized, now);
        return getCaseDetail(caseId);
    }

    public ServiceCaseDetailResponse getCaseDetail(String caseId) {
        ServiceCaseRecord record = requireCase(caseId);
        return toCaseDetail(record);
    }

    @Transactional
    public ServiceCaseDetailResponse updateProfileAccess(String caseId, ServiceCaseProfileAccessUpdateRequest request) {
        ServiceCaseRecord record = requireCase(caseId);
        boolean profileAuthorized = Boolean.TRUE.equals(request.getProfileAuthorized());
        if (profileAuthorized && record.userId() == null) {
            throw new BusinessException(4001, "this case is not linked to a jobseeker account");
        }

        LocalDateTime now = LocalDateTime.now();
        serviceOrgRepository.updateCaseProfileAccess(
                caseId,
                profileAuthorized,
                trimToNull(request.getAuthorizationNote()),
                trim(request.getOperatorName()),
                now
        );
        syncProfileAuthorization(record.userId(), profileAuthorized, now);

        List<String> nextTimeline = new ArrayList<>(record.timeline());
        nextTimeline.add(formatDateTime(now) + " " + (profileAuthorized ? "已记录求职者档案授权" : "已撤回求职者档案授权"));
        if (trimToNull(request.getAuthorizationNote()) != null) {
            nextTimeline.add(formatDateTime(now) + " 授权说明：" + trim(request.getAuthorizationNote()));
        }
        serviceOrgRepository.updateCaseTimeline(caseId, nextTimeline);
        return getCaseDetail(caseId);
    }

    public PageResponse<ServiceAlertResponse> listAlerts(String status, Integer level, Integer page, Integer pageSize) {
        return serviceOrgRepository.listAlerts(status, level, page, pageSize);
    }

    @Transactional
    public ServiceCaseDetailResponse addIntervention(String caseId, ServiceInterventionCreateRequest request) {
        requireCase(caseId);
        String interventionType = normalizeEnum(request.getInterventionType(), "interventionType", ALLOWED_INTERVENTION_TYPES);
        LocalDateTime now = LocalDateTime.now();
        serviceOrgRepository.insertIntervention(
                caseId,
                interventionType,
                trim(request.getContent()),
                trimToNull(request.getAttachmentNote()),
                trim(request.getOperatorName()),
                now
        );
        serviceOrgRepository.updateCaseProgress(
                caseId,
                buildNextActionForIntervention(interventionType),
                toAlertLevelLabel(serviceOrgRepository.maxOpenAlertLevel(caseId))
        );
        return getCaseDetail(caseId);
    }

    @Transactional
    public ServiceCaseDetailResponse addFollowup(String caseId, ServiceFollowupCreateRequest request) {
        ServiceCaseRecord record = requireCase(caseId);
        String followupStage = normalizeEnum(request.getFollowupStage(), "followupStage", ALLOWED_FOLLOWUP_STAGES);
        LocalDateTime now = LocalDateTime.now();
        serviceOrgRepository.insertFollowup(
                caseId,
                record.userId(),
                trimToNull(request.getJobId()),
                followupStage,
                request.getAdaptationScore(),
                trimToNull(request.getEnvironmentIssue()),
                trimToNull(request.getCommunicationIssue()),
                Boolean.TRUE.equals(request.getSupportImplemented()),
                Boolean.TRUE.equals(request.getLeaveRisk()),
                Boolean.TRUE.equals(request.getNeedHelp()),
                trim(request.getOperatorName()),
                now
        );

        if (shouldCreateAlert(request)) {
            serviceOrgRepository.insertAlert(
                    generateAlertId(),
                    caseId,
                    record.userId() == null ? 0L : record.userId(),
                    record.name(),
                    "DAY_30".equals(followupStage) ? "30天回访预警" : "7天回访预警",
                    determineAlertLevel(request),
                    buildFollowupAlertReason(request),
                    "OPEN",
                    now,
                    serviceOrgRepository.nextAlertSortNo()
            );
        }

        serviceOrgRepository.updateCaseProgress(
                caseId,
                buildNextActionForFollowup(request, followupStage),
                toAlertLevelLabel(serviceOrgRepository.maxOpenAlertLevel(caseId))
        );
        return getCaseDetail(caseId);
    }

    @Transactional
    public ServiceCaseDetailResponse addReferral(String caseId, ServiceResourceReferralCreateRequest request) {
        requireCase(caseId);
        String referralType = normalizeEnum(request.getReferralType(), "referralType", ALLOWED_REFERRAL_TYPES);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledAt = parseDateTime(request.getScheduledAt(), "scheduledAt");

        serviceOrgRepository.insertReferral(
                caseId,
                referralType,
                trim(request.getResourceName()),
                trimToNull(request.getProviderName()),
                trimToNull(request.getContactName()),
                trimToNull(request.getContactPhone()),
                scheduledAt,
                "PLANNED",
                trimToNull(request.getStatusNote()),
                trim(request.getOperatorName()),
                now
        );

        serviceOrgRepository.updateCaseProgress(
                caseId,
                buildNextActionForReferral(referralType),
                toAlertLevelLabel(serviceOrgRepository.maxOpenAlertLevel(caseId))
        );
        return getCaseDetail(caseId);
    }

    @Transactional
    public ServiceCaseDetailResponse updateReferralStatus(Long referralId, ServiceResourceReferralStatusUpdateRequest request) {
        if (referralId == null || referralId <= 0) {
            throw new BusinessException(4001, "referralId is invalid");
        }
        String targetStatus = normalizeEnum(request.getTargetStatus(), "targetStatus", ALLOWED_REFERRAL_STATUSES);
        ServiceResourceReferralResponse referral = serviceOrgRepository.updateReferralStatus(
                referralId,
                targetStatus,
                trimToNull(request.getStatusNote()),
                trim(request.getOperatorName()),
                LocalDateTime.now()
        );

        serviceOrgRepository.updateCaseProgress(
                referral.caseId(),
                buildNextActionForReferralStatus(targetStatus),
                toAlertLevelLabel(serviceOrgRepository.maxOpenAlertLevel(referral.caseId()))
        );
        return getCaseDetail(referral.caseId());
    }

    @Transactional
    public ServiceAlertResponse updateAlertStatus(String alertId, ServiceAlertStatusUpdateRequest request) {
        String targetStatus = normalizeEnum(request.getTargetStatus(), "targetStatus", ALLOWED_ALERT_STATUSES);
        ServiceAlertResponse alert = serviceOrgRepository.updateAlertStatus(
                alertId,
                targetStatus,
                trim(request.getResolutionNote()),
                trim(request.getOperatorName()),
                LocalDateTime.now()
        );

        if (alert.caseId() != null && !alert.caseId().isBlank()) {
            String nextAction = "ESCALATED".equals(targetStatus)
                    ? "升级处理当前风险预警"
                    : serviceOrgRepository.maxOpenAlertLevel(alert.caseId()) > 0
                    ? "继续跟进未处理预警"
                    : "继续常规稳定跟进";
            serviceOrgRepository.updateCaseProgress(
                    alert.caseId(),
                    nextAction,
                    toAlertLevelLabel(serviceOrgRepository.maxOpenAlertLevel(alert.caseId()))
            );
        }
        return alert;
    }

    private ServiceCaseDetailResponse toCaseDetail(ServiceCaseRecord record) {
        List<ServiceInterventionResponse> interventions = serviceOrgRepository.listInterventions(record.id());
        List<ServiceFollowupResponse> followups = serviceOrgRepository.listFollowups(record.id());
        List<ServiceAlertResponse> alerts = serviceOrgRepository.listAlertsByCaseId(record.id());
        List<ServiceResourceReferralResponse> referrals = serviceOrgRepository.listReferrals(record.id());
        return new ServiceCaseDetailResponse(
                record.id(),
                record.name(),
                record.stage(),
                record.owner(),
                record.nextAction(),
                record.alertLevel(),
                record.intakeNote(),
                buildTimeline(record.timeline(), interventions, followups, alerts, referrals),
                buildProfileAccess(record),
                interventions,
                followups,
                alerts,
                referrals
        );
    }

    private ServiceProfileAccessResponse buildProfileAccess(ServiceCaseRecord record) {
        if (record.userId() == null) {
            return new ServiceProfileAccessResponse(
                    false,
                    false,
                    null,
                    null,
                    record.authorizationNote(),
                    record.authorizationUpdatedBy(),
                    record.authorizationUpdatedAt(),
                    null,
                    null
            );
        }

        SysUser linkedUser = sysUserMapper.selectById(record.userId());
        boolean effectiveProfileAuthorized =
                jobseekerServiceAuthorizationRepository.hasProfileAccess(record.userId(), SecurityUtils.getCurrentUserId());
        JobseekerProfileResponse profile = null;
        JobseekerSupportNeedResponse supportNeeds = null;
        if (effectiveProfileAuthorized) {
            profile = jobseekerProfileService.getProfileByUserId(record.userId());
            supportNeeds = jobseekerSupportNeedService.getSupportNeedsForUser(record.userId());
        }

        String linkedDisplayName = profile != null && trimToNull(profile.getRealName()) != null
                ? profile.getRealName()
                : linkedUser != null && trimToNull(linkedUser.getNickname()) != null
                ? linkedUser.getNickname()
                : linkedUser == null ? null : linkedUser.getAccount();

        return new ServiceProfileAccessResponse(
                true,
                effectiveProfileAuthorized,
                linkedUser == null ? null : linkedUser.getAccount(),
                linkedDisplayName,
                record.authorizationNote(),
                record.authorizationUpdatedBy(),
                record.authorizationUpdatedAt(),
                profile,
                supportNeeds
        );
    }

    private List<String> buildTimeline(List<String> baseTimeline,
                                       List<ServiceInterventionResponse> interventions,
                                       List<ServiceFollowupResponse> followups,
                                       List<ServiceAlertResponse> alerts,
                                       List<ServiceResourceReferralResponse> referrals) {
        List<String> lines = new ArrayList<>(baseTimeline);
        for (ServiceInterventionResponse intervention : interventions) {
            lines.add(intervention.createdAt() + " 干预记录：" + getInterventionTypeLabel(intervention.interventionType()) + " - " + intervention.content());
        }
        for (ServiceFollowupResponse followup : followups) {
            if ("PENDING".equalsIgnoreCase(followup.recordStatus())) {
                String scheduleTime = followup.dueAt().isBlank() ? followup.createdAt() : followup.dueAt();
                lines.add(scheduleTime + " 计划" + getFollowupStageLabel(followup.followupStage()) + "回访");
            } else {
                lines.add(followup.completedAt() + " 完成" + getFollowupStageLabel(followup.followupStage()) + "回访：适应度 " + followup.adaptationScore());
            }
        }
        for (ServiceResourceReferralResponse referral : referrals) {
            lines.add(referral.createdAt() + " 新增资源转介：" + getReferralTypeLabel(referral.referralType()) + " - " + referral.resourceName());
            if (trimToNull(referral.scheduledAt()) != null) {
                lines.add(referral.scheduledAt() + " 计划资源对接：" + referral.resourceName());
            }
            if (trimToNull(referral.updatedAt()) != null && !referral.updatedAt().equals(referral.createdAt())) {
                String suffix = trimToNull(referral.statusNote()) == null ? "" : "：" + referral.statusNote();
                lines.add(referral.updatedAt() + " 资源转介" + getReferralStatusLabel(referral.referralStatus()) + "：" + referral.resourceName() + suffix);
            }
        }
        for (ServiceAlertResponse alert : alerts) {
            lines.add(alert.createdAt() + " 预警触发：" + alert.triggerReason());
            if (trimToNull(alert.handledAt()) != null) {
                String resolution = trimToNull(alert.resolutionNote()) == null ? "已更新状态" : alert.resolutionNote();
                lines.add(alert.handledAt() + " 预警" + getAlertStatusLabel(alert.alertStatus()) + "：" + resolution);
            }
        }
        lines.sort(String::compareTo);
        return List.copyOf(lines);
    }

    private List<String> buildInitialTimeline(LocalDateTime now,
                                              String stage,
                                              String nextAction,
                                              String linkedAccount,
                                              String intakeNote,
                                              boolean profileAuthorized) {
        List<String> lines = new ArrayList<>();
        String nowText = formatDateTime(now);
        lines.add(nowText + " 创建服务对象档案");
        lines.add(nowText + " 当前阶段：" + stage);
        lines.add(nowText + " 下一步：" + nextAction);
        if (linkedAccount != null) {
            lines.add(nowText + " 已绑定求职者账号：" + linkedAccount);
        }
        if (intakeNote != null) {
            lines.add(nowText + " 建档备注：" + intakeNote);
        }
        if (profileAuthorized) {
            lines.add(nowText + " 已记录求职者档案授权");
        }
        return List.copyOf(lines);
    }

    private ServiceCaseRecord requireCase(String caseId) {
        ServiceCaseRecord record = serviceOrgRepository.findCaseById(caseId);
        if (record == null) {
            throw new BusinessException(4004, "service case not found");
        }
        return record;
    }

    private Long resolveLinkedJobseekerUserId(String account) {
        if (account == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectByAccount(account);
        if (user == null) {
            throw new BusinessException(4004, "linked jobseeker account not found");
        }
        List<String> roleCodes = sysRoleMapper.selectRoleCodesByUserId(user.getId());
        if (!roleCodes.contains("ROLE_JOBSEEKER")) {
            throw new BusinessException(4001, "linked account is not a jobseeker");
        }
        return user.getId();
    }

    private void syncProfileAuthorization(Long jobseekerUserId, boolean profileAuthorized, LocalDateTime now) {
        if (jobseekerUserId == null) {
            return;
        }
        Long serviceOrgUserId = SecurityUtils.getCurrentUserId();
        if (profileAuthorized) {
            jobseekerServiceAuthorizationRepository.save(jobseekerUserId, serviceOrgUserId, true, false, now);
            return;
        }
        jobseekerServiceAuthorizationRepository.revoke(jobseekerUserId, serviceOrgUserId, now);
    }

    private boolean shouldCreateAlert(ServiceFollowupCreateRequest request) {
        return Boolean.TRUE.equals(request.getLeaveRisk())
                || Boolean.TRUE.equals(request.getNeedHelp())
                || !Boolean.TRUE.equals(request.getSupportImplemented())
                || request.getAdaptationScore() < 70;
    }

    private int determineAlertLevel(ServiceFollowupCreateRequest request) {
        if (Boolean.TRUE.equals(request.getLeaveRisk())) {
            return 3;
        }
        if (Boolean.TRUE.equals(request.getNeedHelp())
                || !Boolean.TRUE.equals(request.getSupportImplemented())
                || request.getAdaptationScore() < 60) {
            return 2;
        }
        return 1;
    }

    private String buildFollowupAlertReason(ServiceFollowupCreateRequest request) {
        List<String> reasons = new ArrayList<>();
        if (request.getAdaptationScore() < 70) {
            reasons.add("适应评分偏低");
        }
        if (!Boolean.TRUE.equals(request.getSupportImplemented())) {
            reasons.add("便利支持未完全落实");
        }
        if (Boolean.TRUE.equals(request.getLeaveRisk())) {
            reasons.add("存在离职风险");
        }
        if (Boolean.TRUE.equals(request.getNeedHelp())) {
            reasons.add("需要机构继续介入");
        }
        if (trimToNull(request.getEnvironmentIssue()) != null) {
            reasons.add("环境问题：" + trim(request.getEnvironmentIssue()));
        }
        if (trimToNull(request.getCommunicationIssue()) != null) {
            reasons.add("沟通问题：" + trim(request.getCommunicationIssue()));
        }
        return String.join("；", reasons);
    }

    private String buildNextActionForIntervention(String interventionType) {
        return switch (interventionType) {
            case "RESUME_GUIDANCE" -> "复核简历修改结果并确认下一轮投递";
            case "INTERVIEW_GUIDANCE" -> "确认面试支持方案已同步到企业";
            case "JOB_RECOMMENDATION" -> "跟进推荐岗位后的投递反馈";
            case "PSYCHOLOGICAL_SUPPORT" -> "观察情绪状态并安排下一次支持";
            case "TRAINING_REFERRAL" -> "确认培训转介是否已对接完成";
            case "EMPLOYER_COMMUNICATION" -> "跟进企业反馈并确认支持条件";
            case "ONBOARDING_SUPPORT" -> "继续跟进入职后的支持落实情况";
            default -> "继续跟进当前干预事项";
        };
    }

    private String buildNextActionForFollowup(ServiceFollowupCreateRequest request, String followupStage) {
        if (Boolean.TRUE.equals(request.getLeaveRisk()) || Boolean.TRUE.equals(request.getNeedHelp())) {
            return "处理回访风险并确认支持落实";
        }
        if (!Boolean.TRUE.equals(request.getSupportImplemented())) {
            return "推动企业落实已承诺的便利支持";
        }
        if ("DAY_7".equals(followupStage)) {
            return "准备 30 天回访";
        }
        return "继续常规稳定跟进";
    }

    private String buildNextActionForReferral(String referralType) {
        return switch (referralType) {
            case "TRAINING" -> "确认培训资源报名与时间安排";
            case "EMPLOYMENT" -> "跟进就业资源转介后的反馈";
            case "COUNSELING" -> "确认辅导支持资源是否已对接";
            case "ASSISTIVE_SUPPORT" -> "落实辅助设备或无障碍支持方案";
            case "POLICY_SUPPORT" -> "推进政策支持材料与资格核验";
            default -> "继续跟进资源转介进度";
        };
    }

    private String buildNextActionForReferralStatus(String targetStatus) {
        return switch (targetStatus) {
            case "CONNECTED", "IN_PROGRESS" -> "确认资源对接进度并记录反馈";
            case "COMPLETED" -> "评估资源落实效果并安排后续跟进";
            case "CANCELLED" -> "补充新的资源转介方案";
            default -> "落实资源转介排期";
        };
    }

    private String normalizeEnum(String rawValue, String fieldName, Set<String> allowedValues) {
        String normalized = trim(rawValue).toUpperCase(Locale.ROOT);
        if (!allowedValues.contains(normalized)) {
            throw new BusinessException(4001, fieldName + " is invalid");
        }
        return normalized;
    }

    private LocalDateTime parseDateTime(String rawValue, String fieldName) {
        String trimmed = trimToNull(rawValue);
        if (trimmed == null) {
            return null;
        }
        for (DateTimeFormatter formatter : SUPPORTED_DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        throw new BusinessException(4001, fieldName + " is invalid");
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

    private String getAlertStatusLabel(String status) {
        return switch (status) {
            case "RESOLVED" -> "已处理";
            case "CLOSED" -> "已关闭";
            case "ESCALATED" -> "已升级";
            default -> status;
        };
    }

    private String getInterventionTypeLabel(String type) {
        return switch (type) {
            case "RESUME_GUIDANCE" -> "简历指导";
            case "INTERVIEW_GUIDANCE" -> "面试辅导";
            case "JOB_RECOMMENDATION" -> "岗位推荐";
            case "PSYCHOLOGICAL_SUPPORT" -> "心理支持";
            case "TRAINING_REFERRAL" -> "培训转介";
            case "EMPLOYER_COMMUNICATION" -> "企业沟通";
            case "ONBOARDING_SUPPORT" -> "入职支持";
            default -> type;
        };
    }

    private String getFollowupStageLabel(String stage) {
        return switch (stage) {
            case "DAY_7" -> "7 天";
            case "DAY_30" -> "30 天";
            default -> stage;
        };
    }

    private String getReferralTypeLabel(String type) {
        return switch (type) {
            case "TRAINING" -> "培训资源";
            case "EMPLOYMENT" -> "就业转介";
            case "COUNSELING" -> "辅导支持";
            case "ASSISTIVE_SUPPORT" -> "无障碍支持";
            case "POLICY_SUPPORT" -> "政策支持";
            default -> "其他资源";
        };
    }

    private String getReferralStatusLabel(String status) {
        return switch (status) {
            case "PLANNED" -> "已排期";
            case "IN_PROGRESS" -> "跟进中";
            case "CONNECTED" -> "已对接";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private String generateCaseId(String prefix) {
        long timestamp = System.currentTimeMillis();
        int suffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return prefix + Long.toString(timestamp, 36) + "-" + suffix;
    }

    private String generateAlertId() {
        long timestamp = System.currentTimeMillis();
        int suffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "alert-auto-" + Long.toString(timestamp, 36) + "-" + suffix;
    }

    private String formatDateTime(LocalDateTime value) {
        return value.format(DATE_TIME_FORMATTER);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed.isEmpty() ? null : trimmed;
    }
}
