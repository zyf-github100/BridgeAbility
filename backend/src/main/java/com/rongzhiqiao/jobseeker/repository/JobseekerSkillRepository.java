package com.rongzhiqiao.jobseeker.repository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JobseekerSkillRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<JobseekerSkillRecord> listByUserId(Long userId) {
        return jdbcTemplate.query(
                """
                        SELECT id,
                               user_id,
                               skill_code,
                               skill_name,
                               skill_level
                        FROM jobseeker_skill
                        WHERE user_id = ?
                          AND is_deleted = 0
                        ORDER BY skill_level DESC, id ASC
                        """,
                (rs, rowNum) -> new JobseekerSkillRecord(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("skill_code"),
                        rs.getString("skill_name"),
                        rs.getInt("skill_level")
                ),
                userId
        );
    }

    public void replaceAll(Long userId, List<SkillDraft> skills) {
        jdbcTemplate.update(
                """
                        UPDATE jobseeker_skill
                        SET is_deleted = 1,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE user_id = ?
                          AND is_deleted = 0
                        """,
                userId
        );
        if (skills == null || skills.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO jobseeker_skill (
                            user_id,
                            skill_code,
                            skill_name,
                            skill_level,
                            is_deleted
                        ) VALUES (?, ?, ?, ?, 0)
                        """,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                        SkillDraft skill = skills.get(i);
                        ps.setLong(1, userId);
                        ps.setString(2, skill.skillCode());
                        ps.setString(3, skill.skillName());
                        ps.setInt(4, skill.skillLevel());
                    }

                    @Override
                    public int getBatchSize() {
                        return skills.size();
                    }
                }
        );
    }

    public record SkillDraft(
            String skillCode,
            String skillName,
            int skillLevel
    ) {
    }

    public record JobseekerSkillRecord(
            Long id,
            Long userId,
            String skillCode,
            String skillName,
            int skillLevel
    ) {
    }
}
