package com.rongzhiqiao.enterprise.repository;

import com.rongzhiqiao.common.exception.BusinessException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnterpriseVerificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProfileRecord findProfileByUserId(Long userId) {
        return querySingle(
                """
                        SELECT id,
                               user_id,
                               company_name,
                               industry,
                               city,
                               unified_social_credit_code,
                               contact_name,
                               contact_phone,
                               office_address,
                               accessibility_commitment,
                               verification_status,
                               review_note,
                               submitted_at,
                               reviewed_at,
                               reviewed_by_user_id
                        FROM enterprise_verification_profile
                        WHERE user_id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                (rs, rowNum) -> new ProfileRecord(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("company_name"),
                        rs.getString("industry"),
                        rs.getString("city"),
                        rs.getString("unified_social_credit_code"),
                        rs.getString("contact_name"),
                        rs.getString("contact_phone"),
                        rs.getString("office_address"),
                        rs.getString("accessibility_commitment"),
                        rs.getString("verification_status"),
                        rs.getString("review_note"),
                        readDateTime(rs.getTimestamp("submitted_at")),
                        readDateTime(rs.getTimestamp("reviewed_at")),
                        rs.getObject("reviewed_by_user_id") == null ? null : rs.getLong("reviewed_by_user_id")
                ),
                userId
        );
    }

    public void upsertProfile(ProfileSaveCommand command) {
        jdbcTemplate.update(
                """
                        INSERT INTO enterprise_verification_profile (
                            user_id,
                            company_name,
                            industry,
                            city,
                            unified_social_credit_code,
                            contact_name,
                            contact_phone,
                            office_address,
                            accessibility_commitment,
                            verification_status,
                            review_note,
                            submitted_at,
                            reviewed_at,
                            reviewed_by_user_id,
                            is_deleted
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        ON DUPLICATE KEY UPDATE
                            company_name = VALUES(company_name),
                            industry = VALUES(industry),
                            city = VALUES(city),
                            unified_social_credit_code = VALUES(unified_social_credit_code),
                            contact_name = VALUES(contact_name),
                            contact_phone = VALUES(contact_phone),
                            office_address = VALUES(office_address),
                            accessibility_commitment = VALUES(accessibility_commitment),
                            verification_status = VALUES(verification_status),
                            review_note = VALUES(review_note),
                            submitted_at = VALUES(submitted_at),
                            reviewed_at = VALUES(reviewed_at),
                            reviewed_by_user_id = VALUES(reviewed_by_user_id),
                            is_deleted = 0
                        """,
                command.userId(),
                command.companyName(),
                command.industry(),
                command.city(),
                command.unifiedSocialCreditCode(),
                command.contactName(),
                command.contactPhone(),
                command.officeAddress(),
                command.accessibilityCommitment(),
                command.verificationStatus(),
                command.reviewNote(),
                toTimestamp(command.submittedAt()),
                toTimestamp(command.reviewedAt()),
                command.reviewedByUserId()
        );
    }

    public List<MaterialRecord> listMaterialsByUserId(Long userId) {
        return jdbcTemplate.query(
                """
                        SELECT id,
                               user_id,
                               material_type,
                               original_file_name,
                               storage_path,
                               content_type,
                               file_size,
                               note,
                               created_at
                        FROM enterprise_verification_material
                        WHERE user_id = ?
                          AND is_deleted = 0
                        ORDER BY sort_no ASC, id ASC
                        """,
                (rs, rowNum) -> new MaterialRecord(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("material_type"),
                        rs.getString("original_file_name"),
                        rs.getString("storage_path"),
                        rs.getString("content_type"),
                        rs.getLong("file_size"),
                        rs.getString("note"),
                        readDateTime(rs.getTimestamp("created_at"))
                ),
                userId
        );
    }

    public MaterialRecord findMaterialByUserIdAndId(Long userId, Long materialId) {
        return querySingle(
                """
                        SELECT id,
                               user_id,
                               material_type,
                               original_file_name,
                               storage_path,
                               content_type,
                               file_size,
                               note,
                               created_at
                        FROM enterprise_verification_material
                        WHERE user_id = ?
                          AND id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                (rs, rowNum) -> new MaterialRecord(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("material_type"),
                        rs.getString("original_file_name"),
                        rs.getString("storage_path"),
                        rs.getString("content_type"),
                        rs.getLong("file_size"),
                        rs.getString("note"),
                        readDateTime(rs.getTimestamp("created_at"))
                ),
                userId,
                materialId
        );
    }

    public MaterialRecord findMaterialById(Long materialId) {
        return querySingle(
                """
                        SELECT id,
                               user_id,
                               material_type,
                               original_file_name,
                               storage_path,
                               content_type,
                               file_size,
                               note,
                               created_at
                        FROM enterprise_verification_material
                        WHERE id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                (rs, rowNum) -> new MaterialRecord(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("material_type"),
                        rs.getString("original_file_name"),
                        rs.getString("storage_path"),
                        rs.getString("content_type"),
                        rs.getLong("file_size"),
                        rs.getString("note"),
                        readDateTime(rs.getTimestamp("created_at"))
                ),
                materialId
        );
    }

    public MaterialRecord insertMaterial(MaterialInsertCommand command) {
        jdbcTemplate.update(
                """
                        INSERT INTO enterprise_verification_material (
                            user_id,
                            material_type,
                            original_file_name,
                            storage_path,
                            content_type,
                            file_size,
                            note,
                            sort_no,
                            is_deleted
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                command.userId(),
                command.materialType(),
                command.originalFileName(),
                command.storagePath(),
                command.contentType(),
                command.fileSize(),
                command.note(),
                nextMaterialSortNo(command.userId())
        );
        Long materialId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (materialId == null) {
            throw new BusinessException(5000, "material insert failed");
        }
        return findMaterialByUserIdAndId(command.userId(), materialId);
    }

    public void softDeleteMaterial(Long userId, Long materialId) {
        jdbcTemplate.update(
                """
                        UPDATE enterprise_verification_material
                        SET is_deleted = 1,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE user_id = ?
                          AND id = ?
                          AND is_deleted = 0
                        """,
                userId,
                materialId
        );
    }

    public int countMaterialsByUserId(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM enterprise_verification_material
                        WHERE user_id = ?
                          AND is_deleted = 0
                        """,
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public int countPublishedJobsByUserId(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM enterprise_job_posting
                        WHERE created_by_user_id = ?
                          AND publish_status = 'PUBLISHED'
                          AND is_deleted = 0
                        """,
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public List<ReviewQueueRecord> listPendingReviews() {
        return jdbcTemplate.query(
                """
                        SELECT p.user_id,
                               p.company_name,
                               p.industry,
                               p.city,
                               p.verification_status,
                               p.review_note,
                               p.submitted_at,
                               (
                                   SELECT COUNT(1)
                                   FROM enterprise_verification_material m
                                   WHERE m.user_id = p.user_id
                                     AND m.is_deleted = 0
                               ) AS material_count
                        FROM enterprise_verification_profile p
                        WHERE p.is_deleted = 0
                          AND p.verification_status = 'PENDING'
                        ORDER BY p.submitted_at ASC, p.id ASC
                        """,
                (rs, rowNum) -> new ReviewQueueRecord(
                        rs.getLong("user_id"),
                        rs.getString("company_name"),
                        rs.getString("industry"),
                        rs.getString("city"),
                        rs.getString("verification_status"),
                        rs.getString("review_note"),
                        readDateTime(rs.getTimestamp("submitted_at")),
                        rs.getInt("material_count")
                )
        );
    }

    public void insertAuditLog(AuditLogCommand command) {
        jdbcTemplate.update(
                """
                        INSERT INTO enterprise_verification_audit_log (
                            user_id,
                            action,
                            status_after,
                            operator_user_id,
                            operator_name,
                            content,
                            is_deleted
                        )
                        VALUES (?, ?, ?, ?, ?, ?, 0)
                        """,
                command.userId(),
                command.action(),
                command.statusAfter(),
                command.operatorUserId(),
                command.operatorName(),
                command.content()
        );
    }

    public List<String> listAuditLogs(int limit) {
        return jdbcTemplate.query(
                """
                        SELECT content
                        FROM enterprise_verification_audit_log
                        WHERE is_deleted = 0
                        ORDER BY created_at DESC, id DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> rs.getString("content"),
                limit
        );
    }

    private int nextMaterialSortNo(Long userId) {
        Integer current = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(MAX(sort_no), 0)
                        FROM enterprise_verification_material
                        WHERE user_id = ?
                        """,
                Integer.class,
                userId
        );
        return (current == null ? 0 : current) + 10;
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private LocalDateTime readDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private <T> T querySingle(String sql, org.springframework.jdbc.core.RowMapper<T> rowMapper, Object... args) {
        List<T> result = jdbcTemplate.query(sql, rowMapper, args);
        return result.isEmpty() ? null : result.get(0);
    }

    public record ProfileRecord(
            Long id,
            Long userId,
            String companyName,
            String industry,
            String city,
            String unifiedSocialCreditCode,
            String contactName,
            String contactPhone,
            String officeAddress,
            String accessibilityCommitment,
            String verificationStatus,
            String reviewNote,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt,
            Long reviewedByUserId
    ) {
    }

    public record ProfileSaveCommand(
            Long userId,
            String companyName,
            String industry,
            String city,
            String unifiedSocialCreditCode,
            String contactName,
            String contactPhone,
            String officeAddress,
            String accessibilityCommitment,
            String verificationStatus,
            String reviewNote,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt,
            Long reviewedByUserId
    ) {
    }

    public record MaterialRecord(
            Long id,
            Long userId,
            String materialType,
            String originalFileName,
            String storagePath,
            String contentType,
            long fileSize,
            String note,
            LocalDateTime createdAt
    ) {
    }

    public record MaterialInsertCommand(
            Long userId,
            String materialType,
            String originalFileName,
            String storagePath,
            String contentType,
            long fileSize,
            String note
    ) {
    }

    public record ReviewQueueRecord(
            Long userId,
            String companyName,
            String industry,
            String city,
            String verificationStatus,
            String reviewNote,
            LocalDateTime submittedAt,
            int materialCount
    ) {
    }

    public record AuditLogCommand(
            Long userId,
            String action,
            String statusAfter,
            Long operatorUserId,
            String operatorName,
            String content
    ) {
    }
}
