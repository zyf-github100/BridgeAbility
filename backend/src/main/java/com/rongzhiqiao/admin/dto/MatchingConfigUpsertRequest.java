package com.rongzhiqiao.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MatchingConfigUpsertRequest {

    @Valid
    @NotNull
    private ScoreWeightsRequest scoreWeights;

    @Valid
    @NotNull
    private RiskRequest risk;

    @Valid
    @NotNull
    private CandidateStageRequest candidateStage;

    @Data
    public static class ScoreWeightsRequest {

        @NotNull
        @DecimalMin("0.0")
        private Double skill;

        @NotNull
        @DecimalMin("0.0")
        private Double workMode;

        @NotNull
        @DecimalMin("0.0")
        private Double communication;

        @NotNull
        @DecimalMin("0.0")
        private Double environment;

        @NotNull
        @DecimalMin("0.0")
        private Double accommodation;
    }

    @Data
    public static class RiskRequest {

        @NotNull
        @Min(0)
        private Integer penaltyPerRisk;

        @NotNull
        @Min(0)
        private Integer penaltyPerBlockingRisk;

        @NotNull
        @Min(0)
        private Integer maxPenalty;

        @NotNull
        @Min(0)
        @Max(96)
        private Integer hardFilteredMaxScore;
    }

    @Data
    public static class CandidateStageRequest {

        @NotNull
        @DecimalMin("0.0")
        private Double matchScoreWeight;

        @NotNull
        @DecimalMin("0.0")
        private Double profileCompletionWeight;

        @NotNull
        @Min(0)
        @Max(96)
        private Integer priorityThreshold;

        @NotNull
        @Min(0)
        @Max(96)
        private Integer followUpThreshold;
    }
}
