package com.rongzhiqiao.jobseeker.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JobseekerProjectRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<JobseekerProjectRecord> listByUserId(Long userId) {
        return jdbcTemplate.query(
                """
                        SELECT id,
                               user_id,
                               project_name,
                               role_name,
                               description,
                               start_date,
                               end_date
                        FROM jobseeker_project
                        WHERE user_id = ?
                          AND is_deleted = 0
                        ORDER BY COALESCE(end_date, start_date) DESC, id DESC
                        """,
                (rs, rowNum) -> new JobseekerProjectRecord(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("project_name"),
                        rs.getString("role_name"),
                        rs.getString("description"),
                        rs.getDate("start_date") == null ? null : rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date") == null ? null : rs.getDate("end_date").toLocalDate()
                ),
                userId
        );
    }

    public void replaceAll(Long userId, List<ProjectDraft> projects) {
        jdbcTemplate.update(
                """
                        UPDATE jobseeker_project
                        SET is_deleted = 1,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE user_id = ?
                          AND is_deleted = 0
                        """,
                userId
        );
        if (projects == null || projects.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO jobseeker_project (
                            user_id,
                            project_name,
                            role_name,
                            description,
                            start_date,
                            end_date,
                            is_deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, 0)
                        """,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                        ProjectDraft project = projects.get(i);
                        ps.setLong(1, userId);
                        ps.setString(2, project.projectName());
                        ps.setString(3, project.roleName());
                        ps.setString(4, project.description());
                        if (project.startDate() == null) {
                            ps.setNull(5, java.sql.Types.DATE);
                        } else {
                            ps.setDate(5, Date.valueOf(project.startDate()));
                        }
                        if (project.endDate() == null) {
                            ps.setNull(6, java.sql.Types.DATE);
                        } else {
                            ps.setDate(6, Date.valueOf(project.endDate()));
                        }
                    }

                    @Override
                    public int getBatchSize() {
                        return projects.size();
                    }
                }
        );
    }

    public record ProjectDraft(
            String projectName,
            String roleName,
            String description,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record JobseekerProjectRecord(
            Long id,
            Long userId,
            String projectName,
            String roleName,
            String description,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}
