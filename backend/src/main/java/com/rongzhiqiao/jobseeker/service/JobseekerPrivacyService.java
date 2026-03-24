package com.rongzhiqiao.jobseeker.service;

import com.rongzhiqiao.auth.entity.SysUser;
import com.rongzhiqiao.auth.mapper.SysRoleMapper;
import com.rongzhiqiao.auth.mapper.SysUserMapper;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.jobseeker.dto.JobseekerSensitiveInfoUpsertRequest;
import com.rongzhiqiao.jobseeker.dto.JobseekerServiceAuthorizationUpsertRequest;
import com.rongzhiqiao.jobseeker.repository.JobseekerSensitiveInfoRepository;
import com.rongzhiqiao.jobseeker.repository.JobseekerSensitiveInfoRepository.SaveCommand;
import com.rongzhiqiao.jobseeker.repository.JobseekerSensitiveInfoRepository.SensitiveInfoRecord;
import com.rongzhiqiao.jobseeker.repository.JobseekerServiceAuthorizationRepository;
import com.rongzhiqiao.jobseeker.repository.JobseekerServiceAuthorizationRepository.ServiceAuthorizationRecord;
import com.rongzhiqiao.jobseeker.vo.JobseekerProfileResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerSensitiveInfoResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerServiceAuthorizationResponse;
import com.rongzhiqiao.privacy.service.SensitiveDataCryptoService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobseekerPrivacyService {

    private static final String ROLE_SERVICE_ORG = "ROLE_SERVICE_ORG";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JobseekerSensitiveInfoRepository jobseekerSensitiveInfoRepository;
    private final JobseekerServiceAuthorizationRepository jobseekerServiceAuthorizationRepository;
    private final SensitiveDataCryptoService sensitiveDataCryptoService;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final JobseekerProfileService jobseekerProfileService;

    public JobseekerSensitiveInfoResponse getCurrentSensitiveInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        return toSensitiveInfoResponse(jobseekerSensitiveInfoRepository.findByUserId(userId));
    }

    @Transactional
    public JobseekerSensitiveInfoResponse saveCurrentSensitiveInfo(JobseekerSensitiveInfoUpsertRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        jobseekerSensitiveInfoRepository.upsert(new SaveCommand(
                userId,
                sensitiveDataCryptoService.encrypt(normalizeOptional(request.getDisabilityType())),
                sensitiveDataCryptoService.encrypt(normalizeOptional(request.getDisabilityLevel())),
                sensitiveDataCryptoService.encrypt(normalizeOptional(request.getSupportNeedDetail())),
                sensitiveDataCryptoService.encrypt(normalizeOptional(request.getHealthNote())),
                sensitiveDataCryptoService.encrypt(normalizeOptional(request.getEmergencyContactName())),
                sensitiveDataCryptoService.encrypt(normalizeOptional(request.getEmergencyContactPhone()))
        ));
        return getCurrentSensitiveInfo();
    }

    public List<JobseekerServiceAuthorizationResponse> listCurrentServiceAuthorizations() {
        Long userId = SecurityUtils.getCurrentUserId();
        return jobseekerServiceAuthorizationRepository.listActiveByJobseekerUserId(userId).stream()
                .map(this::toAuthorizationResponse)
                .toList();
    }

    @Transactional
    public JobseekerServiceAuthorizationResponse saveCurrentServiceAuthorization(JobseekerServiceAuthorizationUpsertRequest request) {
        Long jobseekerUserId = SecurityUtils.getCurrentUserId();
        SysUser serviceOrgUser = requireServiceOrgUser(request.getServiceOrgAccount());
        boolean sensitiveAccessGranted = Boolean.TRUE.equals(request.getSensitiveAccessGranted());
        boolean profileAccessGranted = Boolean.TRUE.equals(request.getProfileAccessGranted()) || sensitiveAccessGranted;
        LocalDateTime now = LocalDateTime.now();

        if (!profileAccessGranted && !sensitiveAccessGranted) {
            jobseekerServiceAuthorizationRepository.revoke(jobseekerUserId, serviceOrgUser.getId(), now);
            return new JobseekerServiceAuthorizationResponse(
                    serviceOrgUser.getId(),
                    serviceOrgUser.getAccount(),
                    normalizeOptional(serviceOrgUser.getNickname()),
                    false,
                    false,
                    formatDateTime(now)
            );
        }

        jobseekerServiceAuthorizationRepository.save(
                jobseekerUserId,
                serviceOrgUser.getId(),
                profileAccessGranted,
                sensitiveAccessGranted,
                now
        );
        ServiceAuthorizationRecord record =
                jobseekerServiceAuthorizationRepository.findActiveByPair(jobseekerUserId, serviceOrgUser.getId());
        if (record == null) {
            throw new IllegalStateException("Failed to persist service authorization");
        }
        return toAuthorizationResponse(record);
    }

    public JobseekerProfileResponse getAuthorizedProfile(Long targetJobseekerUserId) {
        Long serviceOrgUserId = SecurityUtils.getCurrentUserId();
        ensureProfileAccess(targetJobseekerUserId, serviceOrgUserId);
        JobseekerProfileResponse profile = jobseekerProfileService.getProfileByUserId(targetJobseekerUserId);
        if (profile == null) {
            throw new BusinessException(4004, "jobseeker profile not found");
        }
        return profile;
    }

    public JobseekerSensitiveInfoResponse getAuthorizedSensitiveInfo(Long targetJobseekerUserId) {
        Long serviceOrgUserId = SecurityUtils.getCurrentUserId();
        ensureSensitiveAccess(targetJobseekerUserId, serviceOrgUserId);
        return toSensitiveInfoResponse(jobseekerSensitiveInfoRepository.findByUserId(targetJobseekerUserId));
    }

    public void ensureProfileAccess(Long targetJobseekerUserId, Long serviceOrgUserId) {
        if (!hasProfileAccess(targetJobseekerUserId, serviceOrgUserId)) {
            throw new BusinessException(4003, "forbidden");
        }
    }

    public void ensureSensitiveAccess(Long targetJobseekerUserId, Long serviceOrgUserId) {
        if (!hasSensitiveAccess(targetJobseekerUserId, serviceOrgUserId)) {
            throw new BusinessException(4003, "forbidden");
        }
    }

    public boolean hasProfileAccess(Long targetJobseekerUserId, Long serviceOrgUserId) {
        return jobseekerServiceAuthorizationRepository.hasProfileAccess(targetJobseekerUserId, serviceOrgUserId);
    }

    public boolean hasSensitiveAccess(Long targetJobseekerUserId, Long serviceOrgUserId) {
        return jobseekerServiceAuthorizationRepository.hasSensitiveAccess(targetJobseekerUserId, serviceOrgUserId);
    }

    private SysUser requireServiceOrgUser(String account) {
        String normalizedAccount = normalizeRequired(account, "serviceOrgAccount");
        SysUser user = sysUserMapper.selectByAccount(normalizedAccount);
        if (user == null) {
            throw new BusinessException(4004, "service organization user not found");
        }
        boolean isServiceOrg = sysRoleMapper.selectRoleCodesByUserId(user.getId()).stream()
                .map(value -> value == null ? "" : value.trim().toUpperCase(Locale.ROOT))
                .anyMatch(ROLE_SERVICE_ORG::equals);
        if (!isServiceOrg) {
            throw new BusinessException(4001, "target user is not a service organization account");
        }
        return user;
    }

    private JobseekerSensitiveInfoResponse toSensitiveInfoResponse(SensitiveInfoRecord record) {
        if (record == null) {
            return new JobseekerSensitiveInfoResponse(false, null, null, null, null, null, null, "");
        }

        String disabilityType = sensitiveDataCryptoService.decrypt(record.disabilityTypeCiphertext());
        String disabilityLevel = sensitiveDataCryptoService.decrypt(record.disabilityLevelCiphertext());
        String supportNeedDetail = sensitiveDataCryptoService.decrypt(record.supportNeedDetailCiphertext());
        String healthNote = sensitiveDataCryptoService.decrypt(record.healthNoteCiphertext());
        String emergencyContactName = sensitiveDataCryptoService.decrypt(record.emergencyContactNameCiphertext());
        String emergencyContactPhone = sensitiveDataCryptoService.decrypt(record.emergencyContactPhoneCiphertext());

        boolean hasSensitiveInfo = disabilityType != null
                || disabilityLevel != null
                || supportNeedDetail != null
                || healthNote != null
                || emergencyContactName != null
                || emergencyContactPhone != null;

        return new JobseekerSensitiveInfoResponse(
                hasSensitiveInfo,
                disabilityType,
                disabilityLevel,
                supportNeedDetail,
                healthNote,
                emergencyContactName,
                emergencyContactPhone,
                formatDateTime(record.updatedAt())
        );
    }

    private JobseekerServiceAuthorizationResponse toAuthorizationResponse(ServiceAuthorizationRecord record) {
        return new JobseekerServiceAuthorizationResponse(
                record.serviceOrgUserId(),
                record.serviceOrgAccount(),
                record.serviceOrgNickname(),
                record.profileAccessGranted(),
                record.sensitiveAccessGranted(),
                formatDateTime(record.grantedAt())
        );
    }

    private String normalizeRequired(String rawValue, String fieldName) {
        String normalized = normalizeOptional(rawValue);
        if (normalized == null) {
            throw new BusinessException(4001, fieldName + " is required");
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
}
