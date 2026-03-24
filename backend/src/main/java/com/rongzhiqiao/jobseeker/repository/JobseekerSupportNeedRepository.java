package com.rongzhiqiao.jobseeker.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JobseekerSupportNeedRepository {

    private final JdbcTemplate jdbcTemplate;

    public JobseekerSupportNeedRecord findByUserId(Long userId) {
        return jdbcTemplate.query(
                """
                        SELECT user_id,
                               support_visibility,
                               text_communication_preferred,
                               subtitle_needed,
                               remote_interview_preferred,
                               keyboard_only_mode,
                               high_contrast_needed,
                               large_font_needed,
                               flexible_schedule_needed,
                               accessible_workspace_needed,
                               assistive_software_needed,
                               remark,
                               updated_at
                        FROM jobseeker_support_need
                        WHERE user_id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    return new JobseekerSupportNeedRecord(
                            rs.getLong("user_id"),
                            rs.getString("support_visibility"),
                            rs.getInt("text_communication_preferred") == 1,
                            rs.getInt("subtitle_needed") == 1,
                            rs.getInt("remote_interview_preferred") == 1,
                            rs.getInt("keyboard_only_mode") == 1,
                            rs.getInt("high_contrast_needed") == 1,
                            rs.getInt("large_font_needed") == 1,
                            rs.getInt("flexible_schedule_needed") == 1,
                            rs.getInt("accessible_workspace_needed") == 1,
                            rs.getInt("assistive_software_needed") == 1,
                            rs.getString("remark"),
                            updatedAt == null ? null : updatedAt.toLocalDateTime()
                    );
                },
                userId
        );
    }

    public JobseekerSupportNeedRecord upsert(SaveCommand command) {
        jdbcTemplate.update(
                """
                        INSERT INTO jobseeker_support_need (
                            user_id,
                            support_visibility,
                            text_communication_preferred,
                            subtitle_needed,
                            remote_interview_preferred,
                            keyboard_only_mode,
                            high_contrast_needed,
                            large_font_needed,
                            flexible_schedule_needed,
                            accessible_workspace_needed,
                            assistive_software_needed,
                            remark,
                            is_deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        ON DUPLICATE KEY UPDATE
                            support_visibility = VALUES(support_visibility),
                            text_communication_preferred = VALUES(text_communication_preferred),
                            subtitle_needed = VALUES(subtitle_needed),
                            remote_interview_preferred = VALUES(remote_interview_preferred),
                            keyboard_only_mode = VALUES(keyboard_only_mode),
                            high_contrast_needed = VALUES(high_contrast_needed),
                            large_font_needed = VALUES(large_font_needed),
                            flexible_schedule_needed = VALUES(flexible_schedule_needed),
                            accessible_workspace_needed = VALUES(accessible_workspace_needed),
                            assistive_software_needed = VALUES(assistive_software_needed),
                            remark = VALUES(remark),
                            is_deleted = 0,
                            updated_at = CURRENT_TIMESTAMP
                        """,
                command.userId(),
                command.supportVisibility(),
                command.textCommunicationPreferred() ? 1 : 0,
                command.subtitleNeeded() ? 1 : 0,
                command.remoteInterviewPreferred() ? 1 : 0,
                command.keyboardOnlyMode() ? 1 : 0,
                command.highContrastNeeded() ? 1 : 0,
                command.largeFontNeeded() ? 1 : 0,
                command.flexibleScheduleNeeded() ? 1 : 0,
                command.accessibleWorkspaceNeeded() ? 1 : 0,
                command.assistiveSoftwareNeeded() ? 1 : 0,
                command.remark()
        );
        return findByUserId(command.userId());
    }

    public record SaveCommand(
            Long userId,
            String supportVisibility,
            boolean textCommunicationPreferred,
            boolean subtitleNeeded,
            boolean remoteInterviewPreferred,
            boolean keyboardOnlyMode,
            boolean highContrastNeeded,
            boolean largeFontNeeded,
            boolean flexibleScheduleNeeded,
            boolean accessibleWorkspaceNeeded,
            boolean assistiveSoftwareNeeded,
            String remark
    ) {
    }

    public record JobseekerSupportNeedRecord(
            Long userId,
            String supportVisibility,
            boolean textCommunicationPreferred,
            boolean subtitleNeeded,
            boolean remoteInterviewPreferred,
            boolean keyboardOnlyMode,
            boolean highContrastNeeded,
            boolean largeFontNeeded,
            boolean flexibleScheduleNeeded,
            boolean accessibleWorkspaceNeeded,
            boolean assistiveSoftwareNeeded,
            String remark,
            LocalDateTime updatedAt
    ) {
    }
}
