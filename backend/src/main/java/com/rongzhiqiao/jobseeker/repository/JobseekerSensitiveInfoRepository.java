package com.rongzhiqiao.jobseeker.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JobseekerSensitiveInfoRepository {

    private final JdbcTemplate jdbcTemplate;

    public SensitiveInfoRecord findByUserId(Long userId) {
        List<SensitiveInfoRecord> list = jdbcTemplate.query(
                """
                        SELECT user_id,
                               disability_type_ciphertext,
                               disability_level_ciphertext,
                               support_need_detail_ciphertext,
                               health_note_ciphertext,
                               emergency_contact_name_ciphertext,
                               emergency_contact_phone_ciphertext,
                               updated_at
                        FROM jobseeker_sensitive_info
                        WHERE user_id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                this::mapRecord,
                userId
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public void upsert(SaveCommand command) {
        jdbcTemplate.update(
                """
                        INSERT INTO jobseeker_sensitive_info (
                            user_id,
                            disability_type_ciphertext,
                            disability_level_ciphertext,
                            support_need_detail_ciphertext,
                            health_note_ciphertext,
                            emergency_contact_name_ciphertext,
                            emergency_contact_phone_ciphertext,
                            is_deleted
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, 0)
                        ON DUPLICATE KEY UPDATE
                            disability_type_ciphertext = VALUES(disability_type_ciphertext),
                            disability_level_ciphertext = VALUES(disability_level_ciphertext),
                            support_need_detail_ciphertext = VALUES(support_need_detail_ciphertext),
                            health_note_ciphertext = VALUES(health_note_ciphertext),
                            emergency_contact_name_ciphertext = VALUES(emergency_contact_name_ciphertext),
                            emergency_contact_phone_ciphertext = VALUES(emergency_contact_phone_ciphertext),
                            is_deleted = 0
                        """,
                command.userId(),
                command.disabilityTypeCiphertext(),
                command.disabilityLevelCiphertext(),
                command.supportNeedDetailCiphertext(),
                command.healthNoteCiphertext(),
                command.emergencyContactNameCiphertext(),
                command.emergencyContactPhoneCiphertext()
        );
    }

    private SensitiveInfoRecord mapRecord(ResultSet rs, int rowNum) throws SQLException {
        return new SensitiveInfoRecord(
                rs.getLong("user_id"),
                rs.getString("disability_type_ciphertext"),
                rs.getString("disability_level_ciphertext"),
                rs.getString("support_need_detail_ciphertext"),
                rs.getString("health_note_ciphertext"),
                rs.getString("emergency_contact_name_ciphertext"),
                rs.getString("emergency_contact_phone_ciphertext"),
                toLocalDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    public record SaveCommand(
            Long userId,
            String disabilityTypeCiphertext,
            String disabilityLevelCiphertext,
            String supportNeedDetailCiphertext,
            String healthNoteCiphertext,
            String emergencyContactNameCiphertext,
            String emergencyContactPhoneCiphertext
    ) {
    }

    public record SensitiveInfoRecord(
            Long userId,
            String disabilityTypeCiphertext,
            String disabilityLevelCiphertext,
            String supportNeedDetailCiphertext,
            String healthNoteCiphertext,
            String emergencyContactNameCiphertext,
            String emergencyContactPhoneCiphertext,
            LocalDateTime updatedAt
    ) {
    }
}
