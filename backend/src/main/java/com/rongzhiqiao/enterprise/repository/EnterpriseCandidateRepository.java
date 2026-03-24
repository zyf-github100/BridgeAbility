package com.rongzhiqiao.enterprise.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rongzhiqiao.catalog.vo.CatalogResponses.ScoreItem;
import com.rongzhiqiao.common.api.PageResponse;
import com.rongzhiqiao.enterprise.vo.EnterpriseCandidateApplicationResponse;
import com.rongzhiqiao.enterprise.vo.EnterpriseInterviewRecordResponse;
import com.rongzhiqiao.enterprise.vo.EnterpriseSupportRequestResponse;
import com.rongzhiqiao.matching.service.MatchingConfigService;
import com.rongzhiqiao.privacy.util.DataMaskingUtils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnterpriseCandidateRepository {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String BASE_SELECT = """
            SELECT ja.id AS application_id,
                   ja.user_id,
                   ja.job_id,
                   ja.job_title,
                   ja.company_name,
                   ja.status,
                   ja.consent_to_share_support_need,
                   ja.match_score_snapshot,
                   ja.explanation_snapshot,
                   ja.submitted_at,
                   jp.real_name,
                   jp.current_city,
                   jp.target_city,
                   jp.expected_job,
                   jp.work_mode_preference,
                   jp.intro,
                   jp.school_name,
                   jp.major,
                   jp.profile_completion_rate,
                   su.nickname,
                   su.account,
                   jsn.text_communication_preferred,
                   jsn.subtitle_needed,
                   jsn.remote_interview_preferred,
                   jsn.keyboard_only_mode,
                   jsn.high_contrast_needed,
                   jsn.large_font_needed,
                   jsn.flexible_schedule_needed,
                   jsn.accessible_workspace_needed,
                   jsn.assistive_software_needed,
                   jsn.remark AS support_need_remark
            FROM job_application ja
            LEFT JOIN jobseeker_profile jp
                   ON jp.user_id = ja.user_id
                  AND jp.is_deleted = 0
            LEFT JOIN sys_user su
                   ON su.id = ja.user_id
                  AND su.is_deleted = 0
            LEFT JOIN jobseeker_support_need jsn
                   ON jsn.user_id = ja.user_id
                  AND jsn.is_deleted = 0
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final MatchingConfigService matchingConfigService;

    public PageResponse<EnterpriseCandidateApplicationResponse> listByJobId(String jobId,
                                                                            Long ownerUserId,
                                                                            Boolean consentGranted,
                                                                            Integer page,
                                                                            Integer pageSize) {
        List<Object> filters = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder(
                """
                         WHERE ja.job_id = ?
                           AND ja.is_deleted = 0
                           AND EXISTS (
                               SELECT 1
                               FROM enterprise_job_posting ejp
                               WHERE ejp.job_id = ja.job_id
                                 AND ejp.created_by_user_id = ?
                                 AND ejp.is_deleted = 0
                           )
                        """
        );
        filters.add(jobId);
        filters.add(ownerUserId);
        if (consentGranted != null) {
            whereClause.append(" AND ja.consent_to_share_support_need = ?");
            filters.add(Boolean.TRUE.equals(consentGranted) ? 1 : 0);
        }

        int safePage = sanitizePage(page);
        int safePageSize = sanitizePageSize(pageSize);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM job_application ja" + whereClause,
                Long.class,
                filters.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(filters);
        queryArgs.add(safePageSize);
        queryArgs.add((safePage - 1) * safePageSize);

        List<CandidateRow> rows = jdbcTemplate.query(
                BASE_SELECT
                        + whereClause
                        + """

                           ORDER BY ja.submitted_at DESC, ja.id DESC
                           LIMIT ?
                           OFFSET ?
                           """,
                this::mapCandidateRow,
                queryArgs.toArray()
        );

        return new PageResponse<>(total == null ? 0 : total, safePage, safePageSize, assembleResponses(rows));
    }

    public EnterpriseCandidateApplicationResponse findByApplicationId(Long applicationId, Long ownerUserId) {
        List<CandidateRow> rows = jdbcTemplate.query(
                BASE_SELECT
                        + """
                          WHERE ja.id = ?
                            AND ja.is_deleted = 0
                            AND EXISTS (
                                SELECT 1
                                FROM enterprise_job_posting ejp
                                WHERE ejp.job_id = ja.job_id
                                  AND ejp.created_by_user_id = ?
                                  AND ejp.is_deleted = 0
                            )
                          LIMIT 1
                        """,
                this::mapCandidateRow,
                applicationId,
                ownerUserId
        );
        if (rows.isEmpty()) {
            return null;
        }
        return assembleResponses(rows).get(0);
    }

    private List<EnterpriseCandidateApplicationResponse> assembleResponses(List<CandidateRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> applicationIds = rows.stream().map(CandidateRow::applicationId).toList();
        Map<Long, List<EnterpriseInterviewRecordResponse>> interviewRecords = loadInterviewRecords(applicationIds);
        Map<Long, List<EnterpriseSupportRequestResponse>> interviewSupportRequests = loadInterviewSupportRequests(applicationIds);
        MatchingConfigService.CandidateStage candidateStage = matchingConfigService.getCurrentRuntimeConfig().candidateStage();
        List<EnterpriseCandidateApplicationResponse> responses = new ArrayList<>(rows.size());

        for (CandidateRow row : rows) {
            List<EnterpriseInterviewRecordResponse> records =
                    interviewRecords.getOrDefault(row.applicationId(), List.of());
            EnterpriseInterviewRecordResponse latestInterview = records.isEmpty() ? null : records.get(0);
            boolean supportVisible = row.consentGranted() && !"HIDDEN".equalsIgnoreCase(row.supportVisibility());
            String additionalSupport = supportVisible ? row.additionalSupport() : null;

            responses.add(new EnterpriseCandidateApplicationResponse(
                    row.applicationId(),
                    row.userId(),
                    row.jobId(),
                    row.jobTitle(),
                    row.companyName(),
                    DataMaskingUtils.maskName(row.candidateName(), "候选人"),
                    row.city(),
                    row.expectedJob(),
                    row.workModePreference(),
                    "",
                    "",
                    "",
                    row.profileCompletionRate(),
                    row.matchScore(),
                    determineRecommendationStage(row.matchScore(), row.profileCompletionRate(), candidateStage),
                    buildRecommendationSummary(row.matchScore(), row.profileCompletionRate(), row.explanationSnapshot(), candidateStage),
                    row.dimensionScores(),
                    row.explanationSnapshot(),
                    row.status(),
                    row.consentGranted(),
                    row.supportVisibility(),
                    row.preferredInterviewMode(),
                    row.coverNote(),
                    additionalSupport,
                    buildSupportSummary(row.consentGranted(), row.supportVisibility(), additionalSupport),
                    row.submittedAt(),
                    latestInterview,
                    records,
                    buildSupportRequests(row, interviewSupportRequests.getOrDefault(row.applicationId(), List.of()))
            ));
        }
        return List.copyOf(responses);
    }

    private Map<Long, List<EnterpriseInterviewRecordResponse>> loadInterviewRecords(List<Long> applicationIds) {
        if (applicationIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = String.join(",", Collections.nCopies(applicationIds.size(), "?"));
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
                this::mapInterviewRecordRow,
                applicationIds.toArray()
        );

        Map<Long, List<EnterpriseInterviewRecordResponse>> grouped = new LinkedHashMap<>();
        for (InterviewRecordRow row : rows) {
            grouped.computeIfAbsent(row.applicationId(), ignored -> new ArrayList<>()).add(
                    new EnterpriseInterviewRecordResponse(
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

        Map<Long, List<EnterpriseInterviewRecordResponse>> immutableGrouped = new LinkedHashMap<>();
        grouped.forEach((key, value) -> immutableGrouped.put(key, List.copyOf(value)));
        return Map.copyOf(immutableGrouped);
    }

    private Map<Long, List<EnterpriseSupportRequestResponse>> loadInterviewSupportRequests(List<Long> applicationIds) {
        if (applicationIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = String.join(",", Collections.nCopies(applicationIds.size(), "?"));
        List<InterviewSupportRequestRow> rows = jdbcTemplate.query(
                """
                        SELECT application_id,
                               request_type,
                               request_content,
                               request_status,
                               created_at
                        FROM interview_support_request
                        WHERE is_deleted = 0
                          AND application_id IN (%s)
                        ORDER BY application_id ASC, created_at DESC, id DESC
                        """.formatted(placeholders),
                (rs, rowNum) -> new InterviewSupportRequestRow(
                        rs.getLong("application_id"),
                        blankToDefault(rs.getString("request_type"), "OTHER"),
                        blankToDefault(rs.getString("request_content"), ""),
                        blankToDefault(rs.getString("request_status"), "PENDING")
                ),
                applicationIds.toArray()
        );

        Map<Long, List<EnterpriseSupportRequestResponse>> grouped = new LinkedHashMap<>();
        for (InterviewSupportRequestRow row : rows) {
            grouped.computeIfAbsent(row.applicationId(), ignored -> new ArrayList<>()).add(
                    new EnterpriseSupportRequestResponse(
                            row.requestType(),
                            toInterviewSupportRequestLabel(row.requestType()),
                            row.requestContent(),
                            "INTERVIEW_SUPPORT_REQUEST",
                            row.requestStatus()
                    )
            );
        }

        Map<Long, List<EnterpriseSupportRequestResponse>> immutableGrouped = new LinkedHashMap<>();
        grouped.forEach((key, value) -> immutableGrouped.put(key, List.copyOf(value)));
        return Map.copyOf(immutableGrouped);
    }

    private CandidateRow mapCandidateRow(ResultSet rs, int rowNum) throws SQLException {
        boolean consent = rs.getInt("consent_to_share_support_need") == 1;
        ApplicationSnapshot snapshot = readApplicationSnapshot(rs.getString("explanation_snapshot"));
        String supportVisibility = snapshot.supportVisibility();
        if (supportVisibility.isBlank()) {
            supportVisibility = consent ? "SUMMARY" : "HIDDEN";
        }

        return new CandidateRow(
                rs.getLong("application_id"),
                rs.getLong("user_id"),
                rs.getString("job_id"),
                rs.getString("job_title"),
                rs.getString("company_name"),
                firstNonBlank(
                        rs.getString("real_name"),
                        rs.getString("nickname"),
                        rs.getString("account"),
                        "候选人"
                ),
                firstNonBlank(rs.getString("target_city"), rs.getString("current_city"), "未填写"),
                firstNonBlank(rs.getString("expected_job"), "未填写"),
                firstNonBlank(rs.getString("work_mode_preference"), "未填写"),
                blankToDefault(rs.getString("school_name"), "未填写"),
                blankToDefault(rs.getString("major"), "未填写"),
                blankToDefault(rs.getString("intro"), ""),
                rs.getInt("profile_completion_rate"),
                rs.getInt("match_score_snapshot"),
                snapshot.dimensionScores(),
                snapshot.explanationSnapshot(),
                rs.getString("status"),
                consent,
                supportVisibility,
                snapshot.preferredInterviewMode(),
                snapshot.coverNote(),
                snapshot.additionalSupport(),
                readBooleanFlag(rs, "text_communication_preferred"),
                readBooleanFlag(rs, "subtitle_needed"),
                readBooleanFlag(rs, "remote_interview_preferred"),
                readBooleanFlag(rs, "keyboard_only_mode"),
                readBooleanFlag(rs, "high_contrast_needed"),
                readBooleanFlag(rs, "large_font_needed"),
                readBooleanFlag(rs, "flexible_schedule_needed"),
                readBooleanFlag(rs, "accessible_workspace_needed"),
                readBooleanFlag(rs, "assistive_software_needed"),
                blankToNull(rs.getString("support_need_remark")),
                formatDateTime(rs.getTimestamp("submitted_at"))
        );
    }

    private String determineRecommendationStage(int matchScore,
                                                int profileCompletionRate,
                                                MatchingConfigService.CandidateStage candidateStage) {
        double matchScoreWeight = Math.max(candidateStage.matchScoreWeight(), 0D);
        double profileCompletionWeight = Math.max(candidateStage.profileCompletionWeight(), 0D);
        double totalWeight = matchScoreWeight + profileCompletionWeight;
        int blendedScore = totalWeight <= 0
                ? Math.round((matchScore + profileCompletionRate) / 2.0f)
                : (int) Math.round((matchScore * matchScoreWeight + profileCompletionRate * profileCompletionWeight) / totalWeight);
        int followUpThreshold = Math.max(candidateStage.followUpThreshold(), 0);
        int priorityThreshold = Math.max(candidateStage.priorityThreshold(), followUpThreshold);
        if (blendedScore >= priorityThreshold) {
            return "PRIORITY";
        }
        if (blendedScore >= followUpThreshold) {
            return "FOLLOW_UP";
        }
        return "CAUTION";
    }

    private String buildRecommendationSummary(int matchScore,
                                              int profileCompletionRate,
                                              List<String> explanationSnapshot,
                                              MatchingConfigService.CandidateStage candidateStage) {
        String stage = determineRecommendationStage(matchScore, profileCompletionRate, candidateStage);
        if (!explanationSnapshot.isEmpty()) {
            return explanationSnapshot.get(0);
        }
        return switch (stage) {
            case "PRIORITY" -> "匹配度较高，建议优先安排沟通或进入面试推进。";
            case "FOLLOW_UP" -> "基础匹配可继续推进，建议先确认岗位节奏与支持条件。";
            default -> "匹配存在明显差距，建议结合岗位要求谨慎推进。";
        };
    }

    private InterviewRecordRow mapInterviewRecordRow(ResultSet rs, int rowNum) throws SQLException {
        return new InterviewRecordRow(
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
        );
    }

    private List<String> buildSupportSummary(boolean consentGranted, String supportVisibility, String additionalSupport) {
        if (!consentGranted || "HIDDEN".equalsIgnoreCase(supportVisibility)) {
            return List.of("候选人未授权企业查看便利需求摘要。");
        }
        if (additionalSupport != null && !additionalSupport.isBlank()) {
            return List.of("候选人已授权展示便利需求摘要。", additionalSupport);
        }
        return List.of("候选人已授权展示便利需求摘要。");
    }

    private List<EnterpriseSupportRequestResponse> buildSupportRequests(CandidateRow row,
                                                                       List<EnterpriseSupportRequestResponse> interviewSupportRequests) {
        List<EnterpriseSupportRequestResponse> requests = new ArrayList<>(interviewSupportRequests);
        if (!row.consentGranted() || "HIDDEN".equalsIgnoreCase(row.supportVisibility())) {
            return List.copyOf(requests);
        }

        appendSupportRequest(requests, row.textCommunicationPreferred(), "TEXT_COMMUNICATION", "文字沟通优先", "优先采用文字或书面沟通");
        appendSupportRequest(requests, row.subtitleNeeded(), "SUBTITLE", "字幕支持", "需要字幕或同步文字支持");
        appendSupportRequest(requests, row.remoteInterviewPreferred(), "REMOTE_INTERVIEW", "远程面试", "更适合线上或远程面试");
        appendSupportRequest(requests, row.keyboardOnlyMode(), "KEYBOARD_ONLY", "键盘操作", "流程需兼容纯键盘操作");
        appendSupportRequest(requests, row.highContrastNeeded(), "HIGH_CONTRAST", "高对比材料", "材料需要高对比度版本");
        appendSupportRequest(requests, row.largeFontNeeded(), "LARGE_FONT", "大字号材料", "材料需要更大字号版本");
        appendSupportRequest(requests, row.flexibleScheduleNeeded(), "FLEXIBLE_SCHEDULE", "弹性安排", "希望预留更灵活的签到或测试时间");
        appendSupportRequest(requests, row.accessibleWorkspaceNeeded(), "ACCESSIBLE_WORKSPACE", "无障碍场地", "线下场地需确认无障碍通行与办公条件");
        appendSupportRequest(requests, row.assistiveSoftwareNeeded(), "ASSISTIVE_SOFTWARE", "辅助软件", "可能需要辅助软件或设备支持");

        if (row.supportNeedRemark() != null) {
            requests.add(new EnterpriseSupportRequestResponse("OTHER", "其他说明", row.supportNeedRemark(), "PROFILE_SUMMARY", null));
        }
        if (requests.isEmpty() && row.additionalSupport() != null) {
            requests.add(new EnterpriseSupportRequestResponse("OTHER", "其他说明", row.additionalSupport(), "PROFILE_SUMMARY", null));
        }
        return List.copyOf(requests);
    }

    private void appendSupportRequest(List<EnterpriseSupportRequestResponse> requests,
                                      boolean enabled,
                                      String requestType,
                                      String requestTypeLabel,
                                      String requestContent) {
        if (!enabled) {
            return;
        }
        requests.add(new EnterpriseSupportRequestResponse(requestType, requestTypeLabel, requestContent, "PROFILE_SUMMARY", null));
    }

    private String toInterviewSupportRequestLabel(String requestType) {
        return switch (requestType) {
            case "TEXT_INTERVIEW", "TEXT" -> "文字面试";
            case "SUBTITLE" -> "字幕支持";
            case "REMOTE_INTERVIEW", "REMOTE" -> "远程面试";
            case "FLEXIBLE_TIME" -> "弹性时间";
            default -> "其他说明";
        };
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
            items.add(new ScoreItem(label, valueNode == null || valueNode.isNull() ? 0 : valueNode.asInt(0)));
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
                values.add(value.trim());
            }
        });
        return List.copyOf(values);
    }

    private boolean readBooleanFlag(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        return value != null && rs.getInt(columnName) == 1;
    }

    private String formatDateTime(Timestamp value) {
        return value == null ? "" : value.toLocalDateTime().format(DATE_TIME_FORMATTER);
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }

    private String nullableTextValue(JsonNode node) {
        String value = textValue(node);
        return value.isBlank() ? null : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
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

    private int sanitizePage(Integer page) {
        return page == null ? DEFAULT_PAGE : Math.max(page, 1);
    }

    private int sanitizePageSize(Integer pageSize) {
        int safePageSize = pageSize == null ? DEFAULT_PAGE_SIZE : Math.max(pageSize, 1);
        return Math.min(safePageSize, MAX_PAGE_SIZE);
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

    private record CandidateRow(
            long applicationId,
            Long userId,
            String jobId,
            String jobTitle,
            String companyName,
            String candidateName,
            String city,
            String expectedJob,
            String workModePreference,
            String schoolName,
            String major,
            String intro,
            int profileCompletionRate,
            int matchScore,
            List<ScoreItem> dimensionScores,
            List<String> explanationSnapshot,
            String status,
            boolean consentGranted,
            String supportVisibility,
            String preferredInterviewMode,
            String coverNote,
            String additionalSupport,
            boolean textCommunicationPreferred,
            boolean subtitleNeeded,
            boolean remoteInterviewPreferred,
            boolean keyboardOnlyMode,
            boolean highContrastNeeded,
            boolean largeFontNeeded,
            boolean flexibleScheduleNeeded,
            boolean accessibleWorkspaceNeeded,
            boolean assistiveSoftwareNeeded,
            String supportNeedRemark,
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

    private record InterviewSupportRequestRow(
            long applicationId,
            String requestType,
            String requestContent,
            String requestStatus
    ) {
    }
}
