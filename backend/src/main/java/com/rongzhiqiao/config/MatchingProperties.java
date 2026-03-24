package com.rongzhiqiao.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.matching")
public class MatchingProperties {

    private final ScoreWeights scoreWeights = new ScoreWeights();
    private final Risk risk = new Risk();
    private final CandidateStage candidateStage = new CandidateStage();

    @Data
    public static class ScoreWeights {

        private double skill = 34;
        private double workMode = 19;
        private double communication = 18;
        private double environment = 14;
        private double accommodation = 15;
    }

    @Data
    public static class Risk {

        private int penaltyPerRisk = 5;
        private int penaltyPerBlockingRisk = 12;
        private int maxPenalty = 40;
        private int hardFilteredMaxScore = 24;
    }

    @Data
    public static class CandidateStage {

        private double matchScoreWeight = 7;
        private double profileCompletionWeight = 3;
        private int priorityThreshold = 85;
        private int followUpThreshold = 68;
    }
}
