package com.rongzhiqiao.matching.repository;

import com.rongzhiqiao.matching.service.MatchingConfigService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MatchingConfigRepository {

    private final JdbcTemplate jdbcTemplate;

    public StoredConfig findByCode(String configCode) {
        List<StoredConfig> rows = jdbcTemplate.query(
                """
                        SELECT config_code,
                               skill_weight,
                               work_mode_weight,
                               communication_weight,
                               environment_weight,
                               accommodation_weight,
                               penalty_per_risk,
                               penalty_per_blocking_risk,
                               max_penalty,
                               hard_filtered_max_score,
                               match_score_weight,
                               profile_completion_weight,
                               priority_threshold,
                               follow_up_threshold,
                               updated_by_user_id,
                               updated_at
                        FROM admin_matching_config
                        WHERE config_code = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                this::mapRow,
                configCode
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void upsert(String configCode, MatchingConfigService.UpdateCommand command, Long updatedByUserId) {
        jdbcTemplate.update(
                """
                        INSERT INTO admin_matching_config (
                            config_code,
                            skill_weight,
                            work_mode_weight,
                            communication_weight,
                            environment_weight,
                            accommodation_weight,
                            penalty_per_risk,
                            penalty_per_blocking_risk,
                            max_penalty,
                            hard_filtered_max_score,
                            match_score_weight,
                            profile_completion_weight,
                            priority_threshold,
                            follow_up_threshold,
                            updated_by_user_id,
                            is_deleted
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        ON DUPLICATE KEY UPDATE
                            skill_weight = VALUES(skill_weight),
                            work_mode_weight = VALUES(work_mode_weight),
                            communication_weight = VALUES(communication_weight),
                            environment_weight = VALUES(environment_weight),
                            accommodation_weight = VALUES(accommodation_weight),
                            penalty_per_risk = VALUES(penalty_per_risk),
                            penalty_per_blocking_risk = VALUES(penalty_per_blocking_risk),
                            max_penalty = VALUES(max_penalty),
                            hard_filtered_max_score = VALUES(hard_filtered_max_score),
                            match_score_weight = VALUES(match_score_weight),
                            profile_completion_weight = VALUES(profile_completion_weight),
                            priority_threshold = VALUES(priority_threshold),
                            follow_up_threshold = VALUES(follow_up_threshold),
                            updated_by_user_id = VALUES(updated_by_user_id),
                            is_deleted = 0
                        """,
                configCode,
                command.scoreWeights().skill(),
                command.scoreWeights().workMode(),
                command.scoreWeights().communication(),
                command.scoreWeights().environment(),
                command.scoreWeights().accommodation(),
                command.risk().penaltyPerRisk(),
                command.risk().penaltyPerBlockingRisk(),
                command.risk().maxPenalty(),
                command.risk().hardFilteredMaxScore(),
                command.candidateStage().matchScoreWeight(),
                command.candidateStage().profileCompletionWeight(),
                command.candidateStage().priorityThreshold(),
                command.candidateStage().followUpThreshold(),
                updatedByUserId
        );
    }

    public void deleteByCode(String configCode) {
        jdbcTemplate.update("DELETE FROM admin_matching_config WHERE config_code = ?", configCode);
    }

    private StoredConfig mapRow(ResultSet rs, int rowNum) throws SQLException {
        Object updatedByValue = rs.getObject("updated_by_user_id");
        return new StoredConfig(
                rs.getString("config_code"),
                rs.getDouble("skill_weight"),
                rs.getDouble("work_mode_weight"),
                rs.getDouble("communication_weight"),
                rs.getDouble("environment_weight"),
                rs.getDouble("accommodation_weight"),
                rs.getInt("penalty_per_risk"),
                rs.getInt("penalty_per_blocking_risk"),
                rs.getInt("max_penalty"),
                rs.getInt("hard_filtered_max_score"),
                rs.getDouble("match_score_weight"),
                rs.getDouble("profile_completion_weight"),
                rs.getInt("priority_threshold"),
                rs.getInt("follow_up_threshold"),
                updatedByValue == null ? null : rs.getLong("updated_by_user_id"),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    public record StoredConfig(
            String configCode,
            double skillWeight,
            double workModeWeight,
            double communicationWeight,
            double environmentWeight,
            double accommodationWeight,
            int penaltyPerRisk,
            int penaltyPerBlockingRisk,
            int maxPenalty,
            int hardFilteredMaxScore,
            double matchScoreWeight,
            double profileCompletionWeight,
            int priorityThreshold,
            int followUpThreshold,
            Long updatedByUserId,
            LocalDateTime updatedAt
    ) {
    }
}
