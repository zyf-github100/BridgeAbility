package com.rongzhiqiao.jobseeker.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rongzhiqiao.catalog.vo.CatalogResponses.ScoreItem;
import com.rongzhiqiao.jobseeker.vo.JobApplicationResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerInterviewRecordResponse;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JobApplicationRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public boolean existsByUserIdAndJobId(Long userId, String jobId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM job_application
                        WHERE user_id = ?
                          AND job_id = ?
                          AND is_deleted = 0
                        """,
                Long.class,
                userId,
                jobId
        );
        return count != null && count > 0;
    }

    public ApplicationRecord findByIdAndUserId(Long applicationId, Long userId) {
        List<ApplicationRecord> records = jdbcTemplate.query(
                """
                        SELECT id,
                               user_id,
                               job_id,
                               job_title,
                               company_name,
                               status
                        FROM job_application
                        WHERE id = ?
                          AND user_id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                (rs, rowNum) -> new ApplicationRecord(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("job_id"),
                        rs.getString("job_title"),
                        rs.getString("company_name"),
                        rs.getString("status")
                ),
                applicationId,
                userId
        );
        return records.isEmpty() ? null : records.get(0);
    }

    public ApplicationRecord findByUserIdAndJobId(Long userId, String jobId) {
        List<ApplicationRecord> records = jdbcTemplate.query(
                """
                        SELECT id,
                               user_id,
                               job_id,
                               job_title,
                               company_name,
                               status
                        FROM job_application
                        WHERE user_id = ?
                          AND job_id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                (rs, rowNum) -> new ApplicationRecord(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("job_id"),
                        rs.getString("job_title"),
                        rs.getString("company_name"),
                        rs.getString("status")
                ),
                userId,
                jobId
        );
        return records.isEmpty() ? null : records.get(0);
    }

    public JobApplicationResponse insert(Long userId,
                                         String jobId,
                                         String jobTitle,
                                         String companyName,
                                         boolean consentToShareSupportNeed,
                                         String coverNote,
                                         String preferredInterviewMode,
                                         String supportVisibility,
                                         String additionalSupport,
                                         int matchScoreSnapshot,
                                         List<ScoreItem> dimensionScores,
                                         List<String> explanationSnapshot,
                                         LocalDateTime submittedAt) {
        List<String> safeExplanationSnapshot = explanationSnapshot == null ? List.of() : List.copyOf(explanationSnapshot);
        List<ScoreItem> safeDimensionScores = dimensionScores == null ? List.of() : List.copyOf(dimensionScores);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO job_application (
                                user_id,
                                job_id,
                                job_title,
                                company_name,
                                status,
                                consent_to_share_support_need,
                                match_score_snapshot,
                                explanation_snapshot,
                                submitted_at,
                                is_deleted
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, userId);
            statement.setString(2, jobId);
            statement.setString(3, jobTitle);
            statement.setString(4, companyName);
            statement.setString(5, "APPLIED");
            statement.setInt(6, consentToShareSupportNeed ? 1 : 0);
            statement.setInt(7, matchScoreSnapshot);
            statement.setString(
                    8,
                    writeApplicationSnapshot(
                            coverNote,
                            preferredInterviewMode,
                            supportVisibility,
                            additionalSupport,
                            safeDimensionScores,
                            safeExplanationSnapshot
                    )
            );
            statement.setTimestamp(9, Timestamp.valueOf(submittedAt));
            return statement;
        }, keyHolder);

        long applicationId = keyHolder.getKey() == null ? 0L : keyHolder.getKey().longValue();
        return new JobApplicationResponse(
                applicationId,
                jobId,
                jobTitle,
                companyName,
                "APPLIED",
                consentToShareSupportNeed,
                supportVisibility,
                preferredInterviewMode,
                coverNote,
                additionalSupport,
                matchScoreSnapshot,
                safeExplanationSnapshot,
                null,
                List.of(),
                submittedAt.format(DATE_TIME_FORMATTER)
        );
    }

    public List<JobApplicationResponse> listByUserId(Long userId) {
        List<JobApplicationRow> rows = jdbcTemplate.query(
                """
                        SELECT id,
                               job_id,
                               job_title,
                               company_name,
                               status,
                               consent_to_share_support_need,
                               match_score_snapshot,
                               explanation_snapshot,
                               submitted_at
                        FROM job_application
                        WHERE user_id = ?
                          AND is_deleted = 0
                        ORDER BY submitted_at DESC, id DESC
                        """,
                (rs, rowNum) -> {
                    boolean consentToShareSupportNeed = rs.getInt("consent_to_share_support_need") == 1;
                    ApplicationSnapshot snapshot = readApplicationSnapshot(rs.getString("explanation_snapshot"));
                    String supportVisibility = snapshot.supportVisibility();
                    if (supportVisibility == null || supportVisibility.isBlank()) {
                        supportVisibility = consentToShareSupportNeed ? "SUMMARY" : "HIDDEN";
                    }

                    return new JobApplicationRow(
                            rs.getLong("id"),
                            rs.getString("job_id"),
                            rs.getString("job_title"),
                            rs.getString("company_name"),
                            rs.getString("status"),
                            consentToShareSupportNeed,
                            supportVisibility,
                            snapshot.preferredInterviewMode(),
                            snapshot.coverNote(),
                            snapshot.additionalSupport(),
                            rs.getInt("match_score_snapshot"),
                            snapshot.explanationSnapshot(),
                            rs.getTimestamp("submitted_at").toLocalDateTime().format(DATE_TIME_FORMATTER)
                    );
                },
                userId
        );

        Map<Long, List<JobseekerInterviewRecordResponse>> interviewRecords = loadInterviewRecords(
                rows.stream().map(JobApplicationRow::applicationId).toList()
        );

        return rows.stream()
                .map(row -> {
                    List<JobseekerInterviewRecordResponse> records =
                            interviewRecords.getOrDefault(row.applicationId(), List.of());
                    JobseekerInterviewRecordResponse latestInterview = records.isEmpty() ? null : records.get(0);
                    return new JobApplicationResponse(
                            row.applicationId(),
                            row.jobId(),
                            row.jobTitle(),
                            row.companyName(),
                            row.status(),
                            row.consentToShareSupportNeed(),
                            row.supportVisibility(),
                            row.preferredInterviewMode(),
                            row.coverNote(),
                            row.additionalSupport(),
                            row.matchScoreSnapshot(),
                            row.explanationSnapshot(),
                            latestInterview,
                            records,
                            row.submittedAt()
                    );
                })
                .toList();
    }

    private Map<Long, List<JobseekerInterviewRecordResponse>> loadInterviewRecords(List<Long> applicationIds) {
        if (applicationIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(applicationIds.size(), "?"));
        List<InterviewRecordRow> rows = jdbcTemplate.query(
                """
                        SELECT id,
                               application_id,
                               interview_time,
                               interview_mode,
                               interviewer_name,
                               invite_note,
                               result_status,
                               feedback_note,
                               reject_reason,
                               created_at,
                               updated_at
                        FROM interview_record
                        WHERE is_deleted = 0
                          AND application_id IN (%s)
                        ORDER BY application_id ASC,
                                 COALESCE(interview_time, created_at) DESC,
                                 id DESC
                        """.formatted(placeholders),
                (rs, rowNum) -> new InterviewRecordRow(
                        rs.getLong("id"),
                        rs.getLong("application_id"),
                        formatDateTime(rs.getTimestamp("interview_time")),
                        blankToDefault(rs.getString("interview_mode"), ""),
                        blankToDefault(rs.getString("interviewer_name"), ""),
                        blankToNull(rs.getString("invite_note")),
                        blankToDefault(rs.getString("result_status"), "PENDING"),
                        blankToNull(rs.getString("feedback_note")),
                        blankToNull(rs.getString("reject_reason")),
                        formatDateTime(rs.getTimestamp("created_at")),
                        formatDateTime(rs.getTimestamp("updated_at"))
                ),
                applicationIds.toArray()
        );

        Map<Long, List<JobseekerInterviewRecordResponse>> grouped = new java.util.LinkedHashMap<>();
        for (InterviewRecordRow row : rows) {
            grouped.computeIfAbsent(row.applicationId(), ignored -> new ArrayList<>()).add(
                    new JobseekerInterviewRecordResponse(
                            row.interviewId(),
                            row.interviewTime(),
                            row.interviewMode(),
                            row.interviewerName(),
                            row.inviteNote(),
                            row.resultStatus(),
                            row.feedbackNote(),
                            row.rejectReason(),
                            row.createdAt(),
                            row.updatedAt()
                    )
            );
        }

        Map<Long, List<JobseekerInterviewRecordResponse>> immutableGrouped = new java.util.LinkedHashMap<>();
        grouped.forEach((key, value) -> immutableGrouped.put(key, List.copyOf(value)));
        return Map.copyOf(immutableGrouped);
    }

    private String writeApplicationSnapshot(String coverNote,
                                            String preferredInterviewMode,
                                            String supportVisibility,
                                            String additionalSupport,
                                            List<ScoreItem> dimensionScores,
                                            List<String> explanationSnapshot) {
        try {
            return objectMapper.writeValueAsString(new ApplicationSnapshot(
                    coverNote,
                    preferredInterviewMode,
                    supportVisibility,
                    additionalSupport,
                    dimensionScores == null ? List.of() : List.copyOf(dimensionScores),
                    explanationSnapshot == null ? List.of() : List.copyOf(explanationSnapshot)
            ));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize application snapshot", ex);
        }
    }

    private ApplicationSnapshot readApplicationSnapshot(String value) {
        if (value == null || value.isBlank()) {
            return ApplicationSnapshot.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(value);
            if (root == null || root.isNull()) {
                return ApplicationSnapshot.empty();
            }
            if (root.isArray()) {
                return new ApplicationSnapshot("", "", "", null, List.of(), readStringList(root));
            }
            return new ApplicationSnapshot(
                    textValue(root.get("coverNote")),
                    textValue(root.get("preferredInterviewMode")),
                    textValue(root.get("supportVisibility")),
                    nullableTextValue(root.get("additionalSupport")),
                    readScoreItems(root.get("dimensionScores")),
                    readStringList(root.has("explanationSnapshot") ? root.get("explanationSnapshot") : root.get("jobReasons"))
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse application snapshot", ex);
        }
    }

    private List<ScoreItem> readScoreItems(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<ScoreItem> items = new ArrayList<>();
        node.forEach(item -> {
            if (item == null || item.isNull()) {
                return;
            }
            String label = textValue(item.get("label"));
            if (label.isBlank()) {
                return;
            }
            JsonNode valueNode = item.get("value");
            int value = valueNode == null || valueNode.isNull() ? 0 : valueNode.asInt(0);
            items.add(new ScoreItem(label, value));
        });
        return List.copyOf(items);
    }

    private List<String> readStringList(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            String value = item == null || item.isNull() ? "" : item.asText("");
            if (!value.isBlank()) {
                values.add(value);
            }
        });
        return List.copyOf(values);
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }

    private String nullableTextValue(JsonNode node) {
        String value = textValue(node);
        return value.isEmpty() ? null : value;
    }

    private String formatDateTime(Timestamp value) {
        return value == null ? "" : value.toLocalDateTime().format(DATE_TIME_FORMATTER);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ApplicationSnapshot(
            String coverNote,
            String preferredInterviewMode,
            String supportVisibility,
            String additionalSupport,
            List<ScoreItem> dimensionScores,
            List<String> explanationSnapshot
    ) {
        private static ApplicationSnapshot empty() {
            return new ApplicationSnapshot("", "", "", null, List.of(), List.of());
        }
    }

    public record ApplicationRecord(
            long applicationId,
            long userId,
            String jobId,
            String jobTitle,
            String companyName,
            String status
    ) {
    }

    private record JobApplicationRow(
            long applicationId,
            String jobId,
            String jobTitle,
            String companyName,
            String status,
            boolean consentToShareSupportNeed,
            String supportVisibility,
            String preferredInterviewMode,
            String coverNote,
            String additionalSupport,
            int matchScoreSnapshot,
            List<String> explanationSnapshot,
            String submittedAt
    ) {
    }

    private record InterviewRecordRow(
            long interviewId,
            long applicationId,
            String interviewTime,
            String interviewMode,
            String interviewerName,
            String inviteNote,
            String resultStatus,
            String feedbackNote,
            String rejectReason,
            String createdAt,
            String updatedAt
    ) {
    }
}
