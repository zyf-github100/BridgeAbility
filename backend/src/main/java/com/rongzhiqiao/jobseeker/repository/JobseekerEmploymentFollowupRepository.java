package com.rongzhiqiao.jobseeker.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JobseekerEmploymentFollowupRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public EmploymentFollowupRecord upsert(long userId,
                                           String jobId,
                                           String followupStage,
                                           int adaptationScore,
                                           String environmentIssue,
                                           String communicationIssue,
                                           boolean supportImplemented,
                                           boolean leaveRisk,
                                           boolean needHelp,
                                           String remark,
                                           LocalDateTime updatedAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO employment_followup (
                            user_id,
                            job_id,
                            followup_stage,
                            adaptation_score,
                            environment_issue,
                            communication_issue,
                            support_implemented,
                            leave_risk,
                            need_help,
                            remark,
                            created_at,
                            updated_at,
                            is_deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        ON DUPLICATE KEY UPDATE
                            adaptation_score = VALUES(adaptation_score),
                            environment_issue = VALUES(environment_issue),
                            communication_issue = VALUES(communication_issue),
                            support_implemented = VALUES(support_implemented),
                            leave_risk = VALUES(leave_risk),
                            need_help = VALUES(need_help),
                            remark = VALUES(remark),
                            updated_at = VALUES(updated_at),
                            is_deleted = 0
                        """,
                userId,
                jobId,
                followupStage,
                adaptationScore,
                environmentIssue,
                communicationIssue,
                supportImplemented ? 1 : 0,
                leaveRisk ? 1 : 0,
                needHelp ? 1 : 0,
                remark,
                Timestamp.valueOf(updatedAt),
                Timestamp.valueOf(updatedAt)
        );
        return findByUserIdJobIdAndStage(userId, jobId, followupStage);
    }

    public EmploymentFollowupRecord findByUserIdJobIdAndStage(long userId, String jobId, String followupStage) {
        List<EmploymentFollowupRecord> records = jdbcTemplate.query(
                """
                        SELECT id,
                               user_id,
                               job_id,
                               followup_stage,
                               adaptation_score,
                               environment_issue,
                               communication_issue,
                               support_implemented,
                               leave_risk,
                               need_help,
                               remark,
                               created_at,
                               updated_at
                        FROM employment_followup
                        WHERE user_id = ?
                          AND job_id = ?
                          AND followup_stage = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                (rs, rowNum) -> new EmploymentFollowupRecord(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("job_id"),
                        rs.getString("followup_stage"),
                        rs.getInt("adaptation_score"),
                        rs.getString("environment_issue"),
                        rs.getString("communication_issue"),
                        rs.getInt("support_implemented") == 1,
                        rs.getInt("leave_risk") == 1,
                        rs.getInt("need_help") == 1,
                        rs.getString("remark"),
                        formatDateTime(rs.getTimestamp("created_at")),
                        formatDateTime(rs.getTimestamp("updated_at"))
                ),
                userId,
                jobId,
                followupStage
        );
        return records.isEmpty() ? null : records.get(0);
    }

    public List<EmploymentFollowupRecord> listByUserId(long userId) {
        return jdbcTemplate.query(
                """
                        SELECT id,
                               user_id,
                               job_id,
                               followup_stage,
                               adaptation_score,
                               environment_issue,
                               communication_issue,
                               support_implemented,
                               leave_risk,
                               need_help,
                               remark,
                               created_at,
                               updated_at
                        FROM employment_followup
                        WHERE user_id = ?
                          AND is_deleted = 0
                        ORDER BY updated_at DESC, id DESC
                        """,
                (rs, rowNum) -> new EmploymentFollowupRecord(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("job_id"),
                        rs.getString("followup_stage"),
                        rs.getInt("adaptation_score"),
                        rs.getString("environment_issue"),
                        rs.getString("communication_issue"),
                        rs.getInt("support_implemented") == 1,
                        rs.getInt("leave_risk") == 1,
                        rs.getInt("need_help") == 1,
                        rs.getString("remark"),
                        formatDateTime(rs.getTimestamp("created_at")),
                        formatDateTime(rs.getTimestamp("updated_at"))
                ),
                userId
        );
    }

    private String formatDateTime(Timestamp value) {
        return value == null ? "" : value.toLocalDateTime().format(DATE_TIME_FORMATTER);
    }

    public record EmploymentFollowupRecord(
            long id,
            long userId,
            String jobId,
            String followupStage,
            int adaptationScore,
            String environmentIssue,
            String communicationIssue,
            boolean supportImplemented,
            boolean leaveRisk,
            boolean needHelp,
            String remark,
            String createdAt,
            String updatedAt
    ) {
    }
}
