package com.rongzhiqiao.enterprise.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnterpriseStatsRepository {

    private static final int ACCESSIBILITY_FIELD_COUNT = 11;

    private final JdbcTemplate jdbcTemplate;

    public List<JobStatsRow> listVisibleJobs(Long userId) {
        return jdbcTemplate.query(
                """
                        SELECT ejp.job_id,
                               cj.title,
                               ejp.publish_status,
                               cj.stage,
                               (
                                   CASE WHEN ejp.onsite_required IS NOT NULL THEN 1 ELSE 0 END
                                 + CASE WHEN ejp.remote_supported IS NOT NULL THEN 1 ELSE 0 END
                                 + CASE WHEN ejp.high_frequency_voice_required IS NOT NULL THEN 1 ELSE 0 END
                                 + CASE WHEN ejp.noisy_environment IS NOT NULL THEN 1 ELSE 0 END
                                 + CASE WHEN ejp.long_standing_required IS NOT NULL THEN 1 ELSE 0 END
                                 + CASE WHEN ejp.text_material_supported IS NOT NULL THEN 1 ELSE 0 END
                                 + CASE WHEN ejp.online_interview_supported IS NOT NULL THEN 1 ELSE 0 END
                                 + CASE WHEN ejp.text_interview_supported IS NOT NULL THEN 1 ELSE 0 END
                                 + CASE WHEN ejp.flexible_schedule_supported IS NOT NULL THEN 1 ELSE 0 END
                                 + CASE WHEN ejp.accessible_workspace IS NOT NULL THEN 1 ELSE 0 END
                                 + CASE WHEN ejp.assistive_software_supported IS NOT NULL THEN 1 ELSE 0 END
                               ) AS accessibility_filled,
                               COALESCE(app.candidate_count, 0) AS candidate_count,
                               COALESCE(app.interviewing_count, 0) AS interviewing_count,
                               COALESCE(app.hired_count, 0) AS hired_count,
                               COALESCE(app.average_match_score, 0) AS average_match_score
                        FROM enterprise_job_posting ejp
                        INNER JOIN catalog_job cj
                                ON cj.id = ejp.job_id
                               AND cj.is_deleted = 0
                        LEFT JOIN (
                            SELECT ja.job_id,
                                   COUNT(1) AS candidate_count,
                                   SUM(CASE WHEN ja.status IN ('INTERVIEW', 'INTERVIEWING') THEN 1 ELSE 0 END) AS interviewing_count,
                                   SUM(CASE WHEN ja.status = 'HIRED' THEN 1 ELSE 0 END) AS hired_count,
                                   ROUND(AVG(ja.match_score_snapshot)) AS average_match_score
                            FROM job_application ja
                            WHERE ja.is_deleted = 0
                            GROUP BY ja.job_id
                        ) app
                               ON app.job_id = ejp.job_id
                        WHERE ejp.is_deleted = 0
                          AND ejp.created_by_user_id = ?
                        ORDER BY candidate_count DESC, ejp.updated_at DESC, ejp.job_id ASC
                        """,
                this::mapJobStatsRow,
                userId
        );
    }

    public ApplicationStatsRow getVisibleApplicationStats(Long userId) {
        return jdbcTemplate.query(
                """
                        SELECT COUNT(1) AS total_applications,
                               SUM(CASE WHEN ja.status = 'APPLIED' THEN 1 ELSE 0 END) AS applied_count,
                               SUM(CASE WHEN ja.status IN ('INTERVIEW', 'INTERVIEWING') THEN 1 ELSE 0 END) AS interviewing_count,
                               SUM(CASE WHEN ja.status = 'OFFERED' THEN 1 ELSE 0 END) AS offered_count,
                               SUM(CASE WHEN ja.status = 'HIRED' THEN 1 ELSE 0 END) AS hired_count,
                               SUM(CASE WHEN ja.status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected_count,
                               SUM(CASE WHEN ja.consent_to_share_support_need = 1 THEN 1 ELSE 0 END) AS consent_granted_count,
                               ROUND(AVG(ja.match_score_snapshot)) AS average_match_score
                        FROM job_application ja
                        INNER JOIN enterprise_job_posting ejp
                                ON ejp.job_id = ja.job_id
                               AND ejp.is_deleted = 0
                        WHERE ja.is_deleted = 0
                          AND ejp.created_by_user_id = ?
                        """,
                rs -> rs.next() ? mapApplicationStatsRow(rs) : ApplicationStatsRow.empty(),
                userId
        );
    }

    private JobStatsRow mapJobStatsRow(ResultSet rs, int rowNum) throws SQLException {
        int accessibilityFilled = rs.getInt("accessibility_filled");
        int completionRate = Math.toIntExact(Math.round(accessibilityFilled * 100.0 / ACCESSIBILITY_FIELD_COUNT));
        return new JobStatsRow(
                rs.getString("job_id"),
                rs.getString("title"),
                rs.getString("publish_status"),
                rs.getString("stage"),
                completionRate,
                rs.getLong("candidate_count"),
                rs.getLong("interviewing_count"),
                rs.getLong("hired_count"),
                rs.getInt("average_match_score")
        );
    }

    private ApplicationStatsRow mapApplicationStatsRow(ResultSet rs) throws SQLException {
        return new ApplicationStatsRow(
                rs.getLong("total_applications"),
                rs.getLong("applied_count"),
                rs.getLong("interviewing_count"),
                rs.getLong("offered_count"),
                rs.getLong("hired_count"),
                rs.getLong("rejected_count"),
                rs.getLong("consent_granted_count"),
                rs.getInt("average_match_score")
        );
    }

    public record JobStatsRow(
            String jobId,
            String title,
            String publishStatus,
            String stage,
            int accessibilityCompletionRate,
            long candidateCount,
            long interviewingCount,
            long hiredCount,
            int averageMatchScore
    ) {
    }

    public record ApplicationStatsRow(
            long totalApplications,
            long appliedCount,
            long interviewingCount,
            long offeredCount,
            long hiredCount,
            long rejectedCount,
            long consentGrantedCount,
            int averageMatchScore
    ) {
        static ApplicationStatsRow empty() {
            return new ApplicationStatsRow(0, 0, 0, 0, 0, 0, 0, 0);
        }
    }
}
