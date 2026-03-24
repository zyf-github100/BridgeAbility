package com.rongzhiqiao.catalog.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rongzhiqiao.catalog.vo.CatalogResponses.AdminDashboardResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.AdminMetricResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.CandidateResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.EnterpriseProfileResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.EnterpriseReviewItemResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.JobResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.KnowledgeArticleResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.MatchingStatusResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.NotificationResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.ScoreItem;
import com.rongzhiqiao.catalog.vo.CatalogResponses.ServiceAlertResponse;
import com.rongzhiqiao.catalog.vo.CatalogResponses.ServiceCaseResponse;
import com.rongzhiqiao.common.api.PageResponse;
import com.rongzhiqiao.common.exception.BusinessException;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CatalogRepository {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PageResponse<JobResponse> listRecommendedJobs(String city, String workMode, String keyword, Integer page, Integer pageSize) {
        List<Object> filters = new ArrayList<>();
        String whereClause = buildJobWhereClause(city, workMode, keyword, filters);
        whereClause += """
                 AND id IN (
                     SELECT job_id
                     FROM enterprise_job_posting
                     WHERE publish_status = 'PUBLISHED'
                       AND is_deleted = 0
                 )
                """;
        return paginateJobs(whereClause, filters, page, pageSize);
    }

    public JobResponse getPublishedJob(String jobId) {
        JobResponse job = querySingle(
                """
                        SELECT id,
                               title,
                               company_name,
                               city,
                               salary_range,
                               work_mode,
                               summary,
                               stage,
                               match_score,
                               dimension_scores,
                               reasons,
                               risks,
                               supports,
                               description_items,
                               requirement_items,
                               environment_items,
                               apply_hint
                        FROM catalog_job
                        WHERE id = ?
                          AND is_deleted = 0
                          AND id IN (
                              SELECT job_id
                              FROM enterprise_job_posting
                              WHERE publish_status = 'PUBLISHED'
                                AND is_deleted = 0
                          )
                        LIMIT 1
                        """,
                this::mapJob,
                jobId
        );
        if (job == null) {
            throw new BusinessException(4004, "job not found");
        }
        return job;
    }

    public JobResponse getJob(String jobId) {
        JobResponse job = querySingle(
                """
                        SELECT id,
                               title,
                               company_name,
                               city,
                               salary_range,
                               work_mode,
                               summary,
                               stage,
                               match_score,
                               dimension_scores,
                               reasons,
                               risks,
                               supports,
                               description_items,
                               requirement_items,
                               environment_items,
                               apply_hint
                        FROM catalog_job
                        WHERE id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                this::mapJob,
                jobId
        );
        if (job == null) {
            throw new BusinessException(4004, "job not found");
        }
        return job;
    }

    public PageResponse<JobResponse> listEnterpriseJobs(Integer page, Integer pageSize, String city, String workMode) {
        List<Object> filters = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder();
        appendContainsFilter(whereClause, filters, "city", city);
        appendContainsFilter(whereClause, filters, "work_mode", workMode);
        return paginateJobs(whereClause.toString(), filters, page, pageSize);
    }

    public PageResponse<CandidateResponse> listCandidates(String jobId, Boolean consentGranted, Integer page, Integer pageSize) {
        getJob(jobId);

        List<Object> filters = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder(" AND job_id = ?");
        filters.add(jobId);
        if (consentGranted != null) {
            whereClause.append(" AND consent_granted = ?");
            filters.add(Boolean.TRUE.equals(consentGranted) ? 1 : 0);
        }

        int safePage = sanitizePage(page);
        int safePageSize = sanitizePageSize(pageSize);
        long total = queryCount("SELECT COUNT(1) FROM catalog_candidate WHERE is_deleted = 0" + whereClause, filters);

        List<Object> queryArgs = new ArrayList<>(filters);
        queryArgs.add(safePageSize);
        queryArgs.add((safePage - 1) * safePageSize);

        List<CandidateResponse> list = jdbcTemplate.query(
                """
                        SELECT id,
                               job_id,
                               name,
                               city,
                               expected_job,
                               work_mode,
                               match_score,
                               stage,
                               consent_granted,
                               skills,
                               summary,
                               risks,
                               support_summary,
                               suggestions
                        FROM catalog_candidate
                        WHERE is_deleted = 0
                        """
                        + whereClause
                        + """

                           ORDER BY sort_no ASC, id ASC
                           LIMIT ?
                           OFFSET ?
                           """,
                this::mapCandidate,
                queryArgs.toArray()
        );
        return new PageResponse<>(total, safePage, safePageSize, list);
    }

    public EnterpriseProfileResponse getEnterpriseProfile() {
        EnterpriseProfileResponse profile = querySingle(
                """
                        SELECT company_name,
                               industry,
                               city,
                               verification_status,
                               accessibility_commitment,
                               published_job_count,
                               open_candidate_count,
                               interview_count
                        FROM catalog_enterprise_profile
                        WHERE is_deleted = 0
                        ORDER BY sort_no ASC, code ASC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new EnterpriseProfileResponse(
                        rs.getString("company_name"),
                        rs.getString("industry"),
                        rs.getString("city"),
                        rs.getString("verification_status"),
                        rs.getString("accessibility_commitment"),
                        rs.getInt("published_job_count"),
                        rs.getInt("open_candidate_count"),
                        rs.getInt("interview_count")
                )
        );
        if (profile == null) {
            throw new BusinessException(4004, "enterprise profile not found");
        }
        return profile;
    }

    public List<ServiceCaseResponse> listServiceCases() {
        return jdbcTemplate.query(
                """
                        SELECT id,
                               name,
                               stage,
                               owner_name,
                               next_action,
                               alert_level,
                               timeline
                        FROM catalog_service_case
                        WHERE is_deleted = 0
                        ORDER BY sort_no ASC, id ASC
                        """,
                this::mapServiceCase
        );
    }

    public ServiceCaseResponse getServiceCase(String caseId) {
        ServiceCaseResponse response = querySingle(
                """
                        SELECT id,
                               name,
                               stage,
                               owner_name,
                               next_action,
                               alert_level,
                               timeline
                        FROM catalog_service_case
                        WHERE id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                this::mapServiceCase,
                caseId
        );
        if (response == null) {
            throw new BusinessException(4004, "service case not found");
        }
        return response;
    }

    public PageResponse<ServiceAlertResponse> listServiceAlerts(String status, Integer level, Integer page, Integer pageSize) {
        List<Object> filters = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder();
        appendContainsFilter(whereClause, filters, "alert_status", status);
        if (level != null) {
            whereClause.append(" AND alert_level = ?");
            filters.add(level);
        }

        int safePage = sanitizePage(page);
        int safePageSize = sanitizePageSize(pageSize);
        long total = queryCount("SELECT COUNT(1) FROM catalog_service_alert WHERE is_deleted = 0" + whereClause, filters);

        List<Object> queryArgs = new ArrayList<>(filters);
        queryArgs.add(safePageSize);
        queryArgs.add((safePage - 1) * safePageSize);

        List<ServiceAlertResponse> list = jdbcTemplate.query(
                """
                        SELECT alert_id,
                               user_id,
                               name,
                               alert_type,
                               alert_level,
                               trigger_reason,
                               created_at,
                               alert_status
                        FROM catalog_service_alert
                        WHERE is_deleted = 0
                        """
                        + whereClause
                        + """

                           ORDER BY sort_no ASC, alert_id ASC
                           LIMIT ?
                           OFFSET ?
                           """,
                this::mapServiceAlert,
                queryArgs.toArray()
        );
        return new PageResponse<>(total, safePage, safePageSize, list);
    }

    public AdminDashboardResponse getAdminDashboard() {
        DashboardCounts counts = querySingle(
                """
                        SELECT jobseeker_count,
                               enterprise_count,
                               published_job_count,
                               application_count,
                               hired_count,
                               open_alert_count
                        FROM catalog_admin_dashboard
                        WHERE code = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new DashboardCounts(
                        rs.getInt("jobseeker_count"),
                        rs.getInt("enterprise_count"),
                        rs.getInt("published_job_count"),
                        rs.getInt("application_count"),
                        rs.getInt("hired_count"),
                        rs.getInt("open_alert_count")
                ),
                "default"
        );
        if (counts == null) {
            throw new BusinessException(4004, "admin dashboard not found");
        }
        return new AdminDashboardResponse(
                counts.jobseekerCount(),
                counts.enterpriseCount(),
                Math.toIntExact(countPublishedJobs()),
                counts.applicationCount(),
                counts.hiredCount(),
                counts.openAlertCount(),
                listAdminMetrics(),
                listPendingEnterpriseReviews(),
                listAuditLogs()
        );
    }

    public List<EnterpriseReviewItemResponse> listPendingEnterpriseReviews() {
        return jdbcTemplate.query(
                """
                        SELECT company,
                               industry,
                               city,
                               status,
                               note
                        FROM catalog_enterprise_review
                        WHERE is_deleted = 0
                        ORDER BY sort_no ASC, id ASC
                        """,
                (rs, rowNum) -> new EnterpriseReviewItemResponse(
                        rs.getString("company"),
                        rs.getString("industry"),
                        rs.getString("city"),
                        rs.getString("status"),
                        rs.getString("note")
                )
        );
    }

    public List<String> listAuditLogs() {
        return jdbcTemplate.query(
                """
                        SELECT content
                        FROM catalog_audit_log
                        WHERE is_deleted = 0
                        ORDER BY sort_no ASC, id ASC
                        """,
                (rs, rowNum) -> rs.getString("content")
        );
    }

    public List<KnowledgeArticleResponse> listKnowledgeArticles() {
        return jdbcTemplate.query(
                """
                        SELECT id,
                               title,
                               category,
                               summary,
                               tags,
                               publish_date
                        FROM catalog_knowledge_article
                        WHERE is_deleted = 0
                        ORDER BY sort_no ASC, id ASC
                        """,
                (rs, rowNum) -> new KnowledgeArticleResponse(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("category"),
                        rs.getString("summary"),
                        readStringList(rs.getString("tags")),
                        formatDate(rs.getDate("publish_date"))
                )
        );
    }

    public List<NotificationResponse> listNotifications(Boolean read) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                """
                        SELECT id,
                               type,
                               title,
                               content,
                               created_at,
                               read_flag
                        FROM catalog_notification
                        WHERE is_deleted = 0
                        """
        );
        if (read != null) {
            sql.append(" AND read_flag = ?");
            args.add(Boolean.TRUE.equals(read) ? 1 : 0);
        }
        sql.append(" ORDER BY sort_no ASC, id ASC");
        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new NotificationResponse(
                        rs.getString("id"),
                        rs.getString("type"),
                        rs.getString("title"),
                        rs.getString("content"),
                        formatDateTime(rs.getTimestamp("created_at")),
                        rs.getInt("read_flag") == 1
                ),
                args.toArray()
        );
    }

    public MatchingStatusResponse getMatchingStatus() {
        MatchingStatusResponse response = querySingle(
                """
                        SELECT mode,
                               version,
                               dimensions,
                               rule_count,
                               available_job_count,
                               candidate_count
                        FROM catalog_matching_status
                        WHERE code = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new MatchingStatusResponse(
                        rs.getString("mode"),
                        rs.getString("version"),
                        readStringList(rs.getString("dimensions")),
                        rs.getInt("rule_count"),
                        Math.toIntExact(countPublishedJobs()),
                        rs.getInt("candidate_count")
                ),
                "default"
        );
        if (response == null) {
            throw new BusinessException(4004, "matching status not found");
        }
        return response;
    }

    private PageResponse<JobResponse> paginateJobs(String whereClause, List<Object> filters, Integer page, Integer pageSize) {
        int safePage = sanitizePage(page);
        int safePageSize = sanitizePageSize(pageSize);
        long total = queryCount("SELECT COUNT(1) FROM catalog_job WHERE is_deleted = 0" + whereClause, filters);

        List<Object> queryArgs = new ArrayList<>(filters);
        queryArgs.add(safePageSize);
        queryArgs.add((safePage - 1) * safePageSize);

        List<JobResponse> list = jdbcTemplate.query(
                """
                        SELECT id,
                               title,
                               company_name,
                               city,
                               salary_range,
                               work_mode,
                               summary,
                               stage,
                               match_score,
                               dimension_scores,
                               reasons,
                               risks,
                               supports,
                               description_items,
                               requirement_items,
                               environment_items,
                               apply_hint
                        FROM catalog_job
                        WHERE is_deleted = 0
                        """
                        + whereClause
                        + """

                           ORDER BY sort_no ASC, id ASC
                           LIMIT ?
                           OFFSET ?
                           """,
                this::mapJob,
                queryArgs.toArray()
        );
        return new PageResponse<>(total, safePage, safePageSize, list);
    }

    private List<AdminMetricResponse> listAdminMetrics() {
        return jdbcTemplate.query(
                """
                        SELECT label,
                               metric_value,
                               hint
                        FROM catalog_admin_metric
                        ORDER BY sort_no ASC, metric_key ASC
                        """,
                (rs, rowNum) -> new AdminMetricResponse(
                        rs.getString("label"),
                        rs.getString("metric_value"),
                        rs.getString("hint")
                )
        );
    }

    private JobResponse mapJob(ResultSet rs, int rowNum) throws SQLException {
        return new JobResponse(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("company_name"),
                rs.getString("city"),
                rs.getString("salary_range"),
                rs.getString("work_mode"),
                rs.getString("summary"),
                rs.getString("stage"),
                rs.getInt("match_score"),
                readScoreItems(rs.getString("dimension_scores")),
                readStringList(rs.getString("reasons")),
                readStringList(rs.getString("risks")),
                readStringList(rs.getString("supports")),
                readStringList(rs.getString("description_items")),
                readStringList(rs.getString("requirement_items")),
                readStringList(rs.getString("environment_items")),
                rs.getString("apply_hint")
        );
    }

    private CandidateResponse mapCandidate(ResultSet rs, int rowNum) throws SQLException {
        return new CandidateResponse(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("city"),
                rs.getString("expected_job"),
                rs.getString("work_mode"),
                rs.getInt("match_score"),
                rs.getString("stage"),
                rs.getInt("consent_granted") == 1,
                readStringList(rs.getString("skills")),
                rs.getString("summary"),
                readStringList(rs.getString("risks")),
                readStringList(rs.getString("support_summary")),
                readStringList(rs.getString("suggestions"))
        );
    }

    private ServiceCaseResponse mapServiceCase(ResultSet rs, int rowNum) throws SQLException {
        return new ServiceCaseResponse(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("stage"),
                rs.getString("owner_name"),
                rs.getString("next_action"),
                rs.getString("alert_level"),
                readStringList(rs.getString("timeline"))
        );
    }

    private ServiceAlertResponse mapServiceAlert(ResultSet rs, int rowNum) throws SQLException {
        return new ServiceAlertResponse(
                rs.getString("alert_id"),
                rs.getLong("user_id"),
                rs.getString("name"),
                rs.getString("alert_type"),
                rs.getInt("alert_level"),
                rs.getString("trigger_reason"),
                formatDateTime(rs.getTimestamp("created_at")),
                rs.getString("alert_status")
        );
    }

    private String buildJobWhereClause(String city, String workMode, String keyword, List<Object> filters) {
        StringBuilder whereClause = new StringBuilder();
        appendContainsFilter(whereClause, filters, "city", city);
        appendContainsFilter(whereClause, filters, "work_mode", workMode);
        if (hasText(keyword)) {
            String like = toContainsParam(keyword);
            whereClause.append(" AND (LOWER(title) LIKE ? OR LOWER(company_name) LIKE ? OR LOWER(summary) LIKE ? OR LOWER(city) LIKE ?)");
            filters.add(like);
            filters.add(like);
            filters.add(like);
            filters.add(like);
        }
        return whereClause.toString();
    }

    private void appendContainsFilter(StringBuilder whereClause, List<Object> filters, String columnName, String value) {
        if (!hasText(value)) {
            return;
        }
        whereClause.append(" AND LOWER(").append(columnName).append(") LIKE ?");
        filters.add(toContainsParam(value));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String toContainsParam(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private int sanitizePage(Integer page) {
        return page == null ? DEFAULT_PAGE : Math.max(page, 1);
    }

    private int sanitizePageSize(Integer pageSize) {
        int safePageSize = pageSize == null ? DEFAULT_PAGE_SIZE : Math.max(pageSize, 1);
        return Math.min(safePageSize, MAX_PAGE_SIZE);
    }

    private long countPublishedJobs() {
        Long total = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM enterprise_job_posting
                        WHERE publish_status = 'PUBLISHED'
                          AND is_deleted = 0
                        """,
                Long.class
        );
        return total == null ? 0 : total;
    }

    private long queryCount(String sql, List<Object> args) {
        Long total = jdbcTemplate.queryForObject(sql, Long.class, args.toArray());
        return total == null ? 0 : total;
    }

    private String formatDateTime(Timestamp value) {
        if (value == null) {
            return "";
        }
        return value.toLocalDateTime().format(DATE_TIME_FORMATTER);
    }

    private String formatDate(Date value) {
        LocalDate date = value == null ? null : value.toLocalDate();
        return date == null ? "" : date.toString();
    }

    private List<ScoreItem> readScoreItems(String json) {
        return readList(json, new TypeReference<List<ScoreItem>>() {
        });
    }

    private List<String> readStringList(String json) {
        return readList(json, new TypeReference<List<String>>() {
        });
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> typeReference) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<T> list = objectMapper.readValue(json, typeReference);
            return list == null ? List.of() : List.copyOf(list);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse JSON column", ex);
        }
    }

    private <T> T querySingle(String sql, RowMapper<T> rowMapper, Object... args) {
        List<T> result = jdbcTemplate.query(sql, rowMapper, args);
        return result.isEmpty() ? null : result.get(0);
    }

    private record DashboardCounts(
            int jobseekerCount,
            int enterpriseCount,
            int publishedJobCount,
            int applicationCount,
            int hiredCount,
            int openAlertCount
    ) {
    }
}
