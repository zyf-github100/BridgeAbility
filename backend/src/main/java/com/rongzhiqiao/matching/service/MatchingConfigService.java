package com.rongzhiqiao.matching.service;

import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.config.MatchingProperties;
import com.rongzhiqiao.matching.repository.MatchingConfigRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchingConfigService {

    private static final String DEFAULT_CONFIG_CODE = "default";
    private static final int MAX_SCORE = 96;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MatchingProperties matchingProperties;
    private final MatchingConfigRepository matchingConfigRepository;

    public Snapshot getCurrentSnapshot() {
        MatchingConfigRepository.StoredConfig storedConfig = matchingConfigRepository.findByCode(DEFAULT_CONFIG_CODE);
        if (storedConfig == null) {
            return new Snapshot(
                    DEFAULT_CONFIG_CODE,
                    defaultScoreWeights(),
                    defaultRisk(),
                    defaultCandidateStage(),
                    false,
                    null,
                    ""
            );
        }
        return new Snapshot(
                storedConfig.configCode(),
                new ScoreWeights(
                        storedConfig.skillWeight(),
                        storedConfig.workModeWeight(),
                        storedConfig.communicationWeight(),
                        storedConfig.environmentWeight(),
                        storedConfig.accommodationWeight()
                ),
                new Risk(
                        storedConfig.penaltyPerRisk(),
                        storedConfig.penaltyPerBlockingRisk(),
                        storedConfig.maxPenalty(),
                        storedConfig.hardFilteredMaxScore()
                ),
                new CandidateStage(
                        storedConfig.matchScoreWeight(),
                        storedConfig.profileCompletionWeight(),
                        storedConfig.priorityThreshold(),
                        storedConfig.followUpThreshold()
                ),
                true,
                storedConfig.updatedByUserId(),
                formatDateTime(storedConfig.updatedAt())
        );
    }

    public RuntimeConfig getCurrentRuntimeConfig() {
        Snapshot snapshot = getCurrentSnapshot();
        return new RuntimeConfig(snapshot.scoreWeights(), snapshot.risk(), snapshot.candidateStage());
    }

    @Transactional
    public Snapshot update(UpdateCommand command, Long updatedByUserId) {
        validate(command);
        matchingConfigRepository.upsert(DEFAULT_CONFIG_CODE, command, updatedByUserId);
        return getCurrentSnapshot();
    }

    @Transactional
    public Snapshot resetToDefault() {
        matchingConfigRepository.deleteByCode(DEFAULT_CONFIG_CODE);
        return getCurrentSnapshot();
    }

    private void validate(UpdateCommand command) {
        if (command == null || command.scoreWeights() == null || command.risk() == null || command.candidateStage() == null) {
            throw new BusinessException(4001, "matching config is invalid");
        }

        double totalWeight = positive(command.scoreWeights().skill())
                + positive(command.scoreWeights().workMode())
                + positive(command.scoreWeights().communication())
                + positive(command.scoreWeights().environment())
                + positive(command.scoreWeights().accommodation());
        if (totalWeight <= 0) {
            throw new BusinessException(4001, "scoreWeights must contain at least one positive value");
        }

        validateNonNegative(command.risk().penaltyPerRisk(), "penaltyPerRisk");
        validateNonNegative(command.risk().penaltyPerBlockingRisk(), "penaltyPerBlockingRisk");
        validateNonNegative(command.risk().maxPenalty(), "maxPenalty");
        validateRange(command.risk().hardFilteredMaxScore(), "hardFilteredMaxScore", 0, MAX_SCORE);

        if (positive(command.candidateStage().matchScoreWeight()) + positive(command.candidateStage().profileCompletionWeight()) <= 0) {
            throw new BusinessException(4001, "candidateStage weights must contain at least one positive value");
        }
        validateRange(command.candidateStage().followUpThreshold(), "followUpThreshold", 0, MAX_SCORE);
        validateRange(command.candidateStage().priorityThreshold(), "priorityThreshold", 0, MAX_SCORE);
        if (command.candidateStage().followUpThreshold() > command.candidateStage().priorityThreshold()) {
            throw new BusinessException(4001, "followUpThreshold cannot be greater than priorityThreshold");
        }
    }

    private ScoreWeights defaultScoreWeights() {
        MatchingProperties.ScoreWeights weights = matchingProperties.getScoreWeights();
        return new ScoreWeights(
                weights.getSkill(),
                weights.getWorkMode(),
                weights.getCommunication(),
                weights.getEnvironment(),
                weights.getAccommodation()
        );
    }

    private Risk defaultRisk() {
        MatchingProperties.Risk risk = matchingProperties.getRisk();
        return new Risk(
                risk.getPenaltyPerRisk(),
                risk.getPenaltyPerBlockingRisk(),
                risk.getMaxPenalty(),
                risk.getHardFilteredMaxScore()
        );
    }

    private CandidateStage defaultCandidateStage() {
        MatchingProperties.CandidateStage candidateStage = matchingProperties.getCandidateStage();
        return new CandidateStage(
                candidateStage.getMatchScoreWeight(),
                candidateStage.getProfileCompletionWeight(),
                candidateStage.getPriorityThreshold(),
                candidateStage.getFollowUpThreshold()
        );
    }

    private void validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new BusinessException(4001, fieldName + " cannot be negative");
        }
    }

    private void validateRange(int value, String fieldName, int min, int max) {
        if (value < min || value > max) {
            throw new BusinessException(4001, fieldName + " is out of range");
        }
    }

    private double positive(double value) {
        return Math.max(value, 0D);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    public record Snapshot(
            String code,
            ScoreWeights scoreWeights,
            Risk risk,
            CandidateStage candidateStage,
            boolean customized,
            Long updatedByUserId,
            String updatedAt
    ) {
    }

    public record RuntimeConfig(
            ScoreWeights scoreWeights,
            Risk risk,
            CandidateStage candidateStage
    ) {
    }

    public record UpdateCommand(
            ScoreWeights scoreWeights,
            Risk risk,
            CandidateStage candidateStage
    ) {
    }

    public record ScoreWeights(
            double skill,
            double workMode,
            double communication,
            double environment,
            double accommodation
    ) {
    }

    public record Risk(
            int penaltyPerRisk,
            int penaltyPerBlockingRisk,
            int maxPenalty,
            int hardFilteredMaxScore
    ) {
    }

    public record CandidateStage(
            double matchScoreWeight,
            double profileCompletionWeight,
            int priorityThreshold,
            int followUpThreshold
    ) {
    }
}
