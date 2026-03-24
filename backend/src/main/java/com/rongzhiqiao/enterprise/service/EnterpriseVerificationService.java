package com.rongzhiqiao.enterprise.service;

import com.rongzhiqiao.admin.vo.EnterpriseReviewItemResponse;
import com.rongzhiqiao.auth.entity.SysUser;
import com.rongzhiqiao.auth.mapper.SysUserMapper;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.enterprise.dto.EnterpriseVerificationProfileUpsertRequest;
import com.rongzhiqiao.enterprise.repository.EnterpriseVerificationRepository;
import com.rongzhiqiao.enterprise.repository.EnterpriseVerificationRepository.AuditLogCommand;
import com.rongzhiqiao.enterprise.repository.EnterpriseVerificationRepository.MaterialInsertCommand;
import com.rongzhiqiao.enterprise.repository.EnterpriseVerificationRepository.MaterialRecord;
import com.rongzhiqiao.enterprise.repository.EnterpriseVerificationRepository.ProfileRecord;
import com.rongzhiqiao.enterprise.repository.EnterpriseVerificationRepository.ProfileSaveCommand;
import com.rongzhiqiao.enterprise.vo.EnterpriseVerificationMaterialResponse;
import com.rongzhiqiao.enterprise.vo.EnterpriseVerificationProfileResponse;
import com.rongzhiqiao.privacy.util.DataMaskingUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import com.rongzhiqiao.storage.VerificationMaterialStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EnterpriseVerificationService {

    private static final List<String> ALLOWED_STATUSES = List.of("DRAFT", "PENDING", "APPROVED", "REJECTED");
    private static final List<String> ALLOWED_DECISIONS = List.of("APPROVED", "REJECTED");
    private static final List<String> ALLOWED_MATERIAL_TYPES = List.of(
            "BUSINESS_LICENSE",
            "ACCESSIBILITY_POLICY",
            "LEGAL_REPRESENTATIVE_ID",
            "OTHER"
    );
    private static final Map<String, String> MATERIAL_TYPE_LABELS = Map.of(
            "BUSINESS_LICENSE", "营业执照",
            "ACCESSIBILITY_POLICY", "无障碍承诺",
            "LEGAL_REPRESENTATIVE_ID", "法人/经办人材料",
            "OTHER", "其他资料"
    );
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png"
    );
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EnterpriseVerificationRepository enterpriseVerificationRepository;
    private final SysUserMapper sysUserMapper;
    private final VerificationMaterialStorageService verificationMaterialStorageService;

    public EnterpriseVerificationProfileResponse getCurrentProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return toProfileResponse(userId, enterpriseVerificationRepository.findProfileByUserId(userId), false);
    }

    @Transactional
    public EnterpriseVerificationProfileResponse saveCurrentProfile(EnterpriseVerificationProfileUpsertRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ProfileRecord existing = enterpriseVerificationRepository.findProfileByUserId(userId);
        String nextStatus = request.isSubmitForReview()
                ? "PENDING"
                : existing == null ? "DRAFT" : normalizeStatus(existing.verificationStatus());

        int materialCount = enterpriseVerificationRepository.countMaterialsByUserId(userId);
        if (request.isSubmitForReview()) {
            validateReadyForReview(request, materialCount);
        }

        enterpriseVerificationRepository.upsertProfile(new ProfileSaveCommand(
                userId,
                trimToEmpty(request.getCompanyName()),
                trimToNull(request.getIndustry()),
                trimToNull(request.getCity()),
                trimToNull(request.getUnifiedSocialCreditCode()),
                trimToNull(request.getContactName()),
                trimToNull(request.getContactPhone()),
                trimToNull(request.getOfficeAddress()),
                trimToNull(request.getAccessibilityCommitment()),
                nextStatus,
                request.isSubmitForReview() ? null : existing == null ? null : existing.reviewNote(),
                request.isSubmitForReview() ? LocalDateTime.now() : existing == null ? null : existing.submittedAt(),
                request.isSubmitForReview() ? null : existing == null ? null : existing.reviewedAt(),
                request.isSubmitForReview() ? null : existing == null ? null : existing.reviewedByUserId()
        ));

        logEnterpriseAction(
                userId,
                request.isSubmitForReview() ? "SUBMITTED" : "SAVED",
                nextStatus,
                request.isSubmitForReview()
                        ? "submitted enterprise verification profile"
                        : "saved enterprise verification draft"
        );
        return getCurrentProfile();
    }

    @Transactional
    public EnterpriseVerificationMaterialResponse uploadCurrentMaterial(String materialType, String note, MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        String normalizedType = normalizeMaterialType(materialType);
        validateUpload(file);
        String safeOriginalFileName = sanitizeFileName(file.getOriginalFilename());
        String storagePath = verificationMaterialStorageService.store(userId, safeOriginalFileName, file);
        try {
            MaterialRecord saved = enterpriseVerificationRepository.insertMaterial(new MaterialInsertCommand(
                    userId,
                    normalizedType,
                    safeOriginalFileName,
                    storagePath,
                    trimToNull(file.getContentType()),
                    file.getSize(),
                    trimToNull(note)
            ));
            logEnterpriseAction(userId, "MATERIAL_UPLOADED", currentStatusOrDraft(userId), "uploaded verification material");
            return toMaterialResponse(saved);
        } catch (RuntimeException exception) {
            verificationMaterialStorageService.deleteIfExists(storagePath);
            throw exception;
        }
    }

    @Transactional
    public void deleteCurrentMaterial(Long materialId) {
        Long userId = SecurityUtils.getCurrentUserId();
        MaterialRecord material = requireMaterial(userId, materialId);
        enterpriseVerificationRepository.softDeleteMaterial(userId, materialId);
        deleteFileIfExists(material.storagePath());
        logEnterpriseAction(userId, "MATERIAL_DELETED", currentStatusOrDraft(userId), "deleted verification material");
    }

    public StoredMaterial loadCurrentMaterial(Long materialId) {
        Long userId = SecurityUtils.getCurrentUserId();
        MaterialRecord material = requireMaterial(userId, materialId);
        return toStoredMaterial(material);
    }

    public List<EnterpriseReviewItemResponse> listPendingReviews() {
        SecurityUtils.requireAuthority("ROLE_ADMIN");
        return enterpriseVerificationRepository.listPendingReviews().stream()
                .map(item -> new EnterpriseReviewItemResponse(
                        item.userId(),
                        item.companyName(),
                        item.industry(),
                        item.city(),
                        item.verificationStatus(),
                        item.reviewNote() == null || item.reviewNote().isBlank()
                                ? "submitted " + item.materialCount() + " materials"
                                : item.reviewNote(),
                        formatDateTime(item.submittedAt()),
                        item.materialCount()
                ))
                .toList();
    }

    public EnterpriseVerificationProfileResponse getReviewDetail(Long targetUserId) {
        SecurityUtils.requireAuthority("ROLE_ADMIN");
        ProfileRecord profile = enterpriseVerificationRepository.findProfileByUserId(targetUserId);
        if (profile == null) {
            throw new BusinessException(4004, "未找到企业认证资料");
        }
        return toProfileResponse(targetUserId, profile, true);
    }

    @Transactional
    public EnterpriseVerificationProfileResponse reviewEnterprise(Long targetUserId, String decision, String note) {
        SecurityUtils.requireAuthority("ROLE_ADMIN");
        ProfileRecord existing = enterpriseVerificationRepository.findProfileByUserId(targetUserId);
        if (existing == null) {
            throw new BusinessException(4004, "未找到企业认证资料");
        }
        if (!"PENDING".equals(normalizeStatus(existing.verificationStatus()))) {
            throw new BusinessException(4001, "当前认证状态不是待审核，无法执行审核操作");
        }

        String normalizedDecision = normalizeDecision(decision);
        Long operatorUserId = SecurityUtils.getCurrentUserId();
        String normalizedNote = trimToNull(note);
        enterpriseVerificationRepository.upsertProfile(new ProfileSaveCommand(
                targetUserId,
                existing.companyName(),
                existing.industry(),
                existing.city(),
                existing.unifiedSocialCreditCode(),
                existing.contactName(),
                existing.contactPhone(),
                existing.officeAddress(),
                existing.accessibilityCommitment(),
                normalizedDecision,
                normalizedNote,
                existing.submittedAt(),
                LocalDateTime.now(),
                operatorUserId
        ));

        String action = "APPROVED".equals(normalizedDecision) ? "APPROVED" : "REJECTED";
        String content = "APPROVED".equals(normalizedDecision)
                ? "approved enterprise verification"
                : "rejected enterprise verification";
        logAdminAction(targetUserId, action, normalizedDecision, content);
        return getReviewDetail(targetUserId);
    }

    public StoredMaterial loadReviewMaterial(Long targetUserId, Long materialId) {
        SecurityUtils.requireAuthority("ROLE_ADMIN");
        MaterialRecord material = requireMaterial(targetUserId, materialId);
        return toStoredMaterial(material);
    }

    public List<String> listAuditLogs() {
        SecurityUtils.requireAuthority("ROLE_ADMIN");
        return enterpriseVerificationRepository.listAuditLogs(20);
    }

    public void ensureCurrentEnterpriseApprovedForPublishing() {
        Long userId = SecurityUtils.getCurrentUserId();
        ProfileRecord profile = enterpriseVerificationRepository.findProfileByUserId(userId);
        if (profile == null || !"APPROVED".equals(normalizeStatus(profile.verificationStatus()))) {
            throw new BusinessException(4001, "企业认证通过后才能发布岗位");
        }
    }

    public String getCurrentCompanyName() {
        Long userId = SecurityUtils.getCurrentUserId();
        ProfileRecord profile = enterpriseVerificationRepository.findProfileByUserId(userId);
        if (profile != null && profile.companyName() != null && !profile.companyName().isBlank()) {
            return profile.companyName();
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return "BridgeAbility Enterprise";
        }
        String nickname = trimToNull(user.getNickname());
        if (nickname != null) {
            return nickname;
        }
        return trimToNull(user.getAccount()) == null ? "BridgeAbility Enterprise" : user.getAccount();
    }

    private void validateReadyForReview(EnterpriseVerificationProfileUpsertRequest request, int materialCount) {
        if (trimToNull(request.getIndustry()) == null
                || trimToNull(request.getCity()) == null
                || trimToNull(request.getUnifiedSocialCreditCode()) == null
                || trimToNull(request.getContactName()) == null
                || trimToNull(request.getContactPhone()) == null
                || trimToNull(request.getOfficeAddress()) == null
                || trimToNull(request.getAccessibilityCommitment()) == null) {
            throw new BusinessException(4001, "提交审核前，请先完善企业基础信息");
        }
        if (materialCount <= 0) {
            throw new BusinessException(4001, "提交审核前，至少需要上传一份认证材料");
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(4001, "请先选择要上传的认证材料");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(4001, "上传文件过大，请上传 10 MB 以内的文件");
        }
        String contentType = trimToNull(file.getContentType());
        if (contentType == null || ALLOWED_CONTENT_TYPES.stream().noneMatch(contentType::equalsIgnoreCase)) {
            throw new BusinessException(4001, "上传文件格式不支持，请上传 PDF、Word、JPG 或 PNG 文件");
        }
    }

    private String normalizeMaterialType(String materialType) {
        String normalized = trimToEmpty(materialType).toUpperCase(Locale.ROOT);
        if (!ALLOWED_MATERIAL_TYPES.contains(normalized)) {
            throw new BusinessException(4001, "认证材料类型无效");
        }
        return normalized;
    }

    private String normalizeDecision(String decision) {
        String normalized = trimToEmpty(decision).toUpperCase(Locale.ROOT);
        if (!ALLOWED_DECISIONS.contains(normalized)) {
            throw new BusinessException(4001, "审核结果无效");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = trimToEmpty(status).toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            return "DRAFT";
        }
        return normalized;
    }

    private EnterpriseVerificationProfileResponse toProfileResponse(Long userId,
                                                                   ProfileRecord profile,
                                                                   boolean maskSensitiveFields) {
        List<EnterpriseVerificationMaterialResponse> materials = enterpriseVerificationRepository.listMaterialsByUserId(userId).stream()
                .map(this::toMaterialResponse)
                .toList();
        if (profile == null) {
            return new EnterpriseVerificationProfileResponse(
                    userId,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "DRAFT",
                    null,
                    "",
                    "",
                    false,
                    enterpriseVerificationRepository.countPublishedJobsByUserId(userId),
                    materials
            );
        }
        String status = normalizeStatus(profile.verificationStatus());
        return new EnterpriseVerificationProfileResponse(
                userId,
                nullToEmpty(profile.companyName()),
                nullToEmpty(profile.industry()),
                nullToEmpty(profile.city()),
                maskSensitiveFields
                        ? DataMaskingUtils.maskGovernmentId(profile.unifiedSocialCreditCode())
                        : nullToEmpty(profile.unifiedSocialCreditCode()),
                maskSensitiveFields
                        ? DataMaskingUtils.maskName(profile.contactName(), "")
                        : nullToEmpty(profile.contactName()),
                maskSensitiveFields
                        ? DataMaskingUtils.maskPhone(profile.contactPhone())
                        : nullToEmpty(profile.contactPhone()),
                maskSensitiveFields
                        ? DataMaskingUtils.maskAddress(profile.officeAddress())
                        : nullToEmpty(profile.officeAddress()),
                nullToEmpty(profile.accessibilityCommitment()),
                status,
                profile.reviewNote(),
                formatDateTime(profile.submittedAt()),
                formatDateTime(profile.reviewedAt()),
                "APPROVED".equals(status),
                enterpriseVerificationRepository.countPublishedJobsByUserId(userId),
                materials
        );
    }

    private EnterpriseVerificationMaterialResponse toMaterialResponse(MaterialRecord material) {
        return new EnterpriseVerificationMaterialResponse(
                material.id(),
                material.materialType(),
                MATERIAL_TYPE_LABELS.getOrDefault(material.materialType(), material.materialType()),
                material.originalFileName(),
                nullToEmpty(material.contentType()),
                material.fileSize(),
                material.note(),
                formatDateTime(material.createdAt())
        );
    }

    private MaterialRecord requireMaterial(Long userId, Long materialId) {
        MaterialRecord material = enterpriseVerificationRepository.findMaterialByUserIdAndId(userId, materialId);
        if (material == null) {
            throw new BusinessException(4004, "未找到认证材料");
        }
        return material;
    }

    private StoredMaterial toStoredMaterial(MaterialRecord material) {
        Resource resource = verificationMaterialStorageService.load(material.storagePath());
        return new StoredMaterial(
                material.id(),
                material.userId(),
                material.originalFileName(),
                material.contentType(),
                resource
        );
    }

    private String currentStatusOrDraft(Long userId) {
        ProfileRecord profile = enterpriseVerificationRepository.findProfileByUserId(userId);
        return profile == null ? "DRAFT" : normalizeStatus(profile.verificationStatus());
    }

    private void logEnterpriseAction(Long userId, String action, String statusAfter, String actionText) {
        SysUser user = sysUserMapper.selectById(userId);
        String operatorName = user == null ? "enterprise" : preferredDisplayName(user);
        enterpriseVerificationRepository.insertAuditLog(new AuditLogCommand(
                userId,
                action,
                statusAfter,
                userId,
                operatorName,
                formatDateTime(LocalDateTime.now()) + " " + operatorName + " " + actionText
        ));
    }

    private void logAdminAction(Long targetUserId, String action, String statusAfter, String actionText) {
        Long operatorUserId = SecurityUtils.getCurrentUserId();
        SysUser admin = sysUserMapper.selectById(operatorUserId);
        String operatorName = admin == null ? "admin" : preferredDisplayName(admin);
        enterpriseVerificationRepository.insertAuditLog(new AuditLogCommand(
                targetUserId,
                action,
                statusAfter,
                operatorUserId,
                operatorName,
                formatDateTime(LocalDateTime.now()) + " " + operatorName + " " + actionText
        ));
    }

    private String preferredDisplayName(SysUser user) {
        String nickname = trimToNull(user.getNickname());
        if (nickname != null) {
            return nickname;
        }
        String account = trimToNull(user.getAccount());
        return account == null ? "operator" : account;
    }

    private void deleteFileIfExists(String storagePath) {
        verificationMaterialStorageService.deleteIfExists(storagePath);
    }

    private String sanitizeFileName(String value) {
        String raw = value == null || value.isBlank() ? "material.bin" : value.trim();
        return raw.replaceAll("[\\\\/:*?\"<>|]+", "_");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    public record StoredMaterial(
            Long id,
            Long userId,
            String originalFileName,
            String contentType,
            Resource resource
    ) {
    }
}
