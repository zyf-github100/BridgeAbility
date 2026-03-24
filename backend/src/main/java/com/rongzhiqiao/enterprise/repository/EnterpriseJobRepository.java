package com.rongzhiqiao.enterprise.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rongzhiqiao.catalog.vo.CatalogResponses.ScoreItem;
import com.rongzhiqiao.common.api.PageResponse;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.enterprise.entity.EnterpriseJobPosting;
import com.rongzhiqiao.enterprise.vo.EnterpriseJobSummaryResponse;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnterpriseJobRepository {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PageResponse<EnterpriseJobSummaryResponse> listJobs(Long ownerUserId,
                                                               Integer page,
                                                               Integer pageSize,
                                                               String city,
                                                               String workMode,
                                                               String publishStatus) {
        List<Object> filters = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder(" WHERE ejp.is_deleted = 0 AND ejp.created_by_user_id = ?");
        filters.add(ownerUserId);
        appendContainsFilter(whereClause, filters, "cj.city", city);
        appendContainsFilter(whereClause, filters, "cj.work_mode", workMode);
        appendContainsFilter(whereClause, filters, "ejp.publish_status", publishStatus);

        int safePage = sanitizePage(page);
        int safePageSize = sanitizePageSize(pageSize);
        Long total = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM enterprise_job_posting ejp
                        INNER JOIN catalog_job cj
                                ON cj.id = ejp.job_id
                               AND cj.is_deleted = 0
                        """
                        + whereClause,
                Long.class,
                filters.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(filters);
        queryArgs.add(safePageSize);
        queryArgs.add((safePage - 1) * safePageSize);

        List<EnterpriseJobSummaryResponse> list = jdbcTemplate.query(
                """
                        SELECT cj.id,
                               cj.title,
                               ejp.department,
                               cj.city,
                               cj.work_mode,
                               cj.salary_range,
                               ejp.headcount,
                               ejp.deadline,
                               ejp.publish_status,
                               cj.stage,
                               cj.match_score,
                               ejp.onsite_required,
                               ejp.remote_supported,
                               ejp.high_frequency_voice_required,
                               ejp.noisy_environment,
                               ejp.long_standing_required,
                               ejp.text_material_supported,
                               ejp.online_interview_supported,
                               ejp.text_interview_supported,
                               ejp.flexible_schedule_supported,
                               ejp.accessible_workspace,
                               ejp.assistive_software_supported,
                               (
                                   SELECT COUNT(1)
                                   FROM job_application ja
                                   WHERE ja.job_id = cj.id
                                     AND ja.is_deleted = 0
                               ) AS candidate_count
                        FROM enterprise_job_posting ejp
                        INNER JOIN catalog_job cj
                                ON cj.id = ejp.job_id
                               AND cj.is_deleted = 0
                        """
                        + whereClause
                        + """

                           ORDER BY ejp.updated_at DESC, cj.sort_no ASC, cj.id ASC
                           LIMIT ?
                           OFFSET ?
                           """,
                this::mapSummary,
                queryArgs.toArray()
        );
        return new PageResponse<>(total == null ? 0 : total, safePage, safePageSize, list);
    }

    public EnterpriseJobPosting findByJobId(String jobId, Long ownerUserId) {
        return querySingle(
                """
                        SELECT cj.id,
                               cj.title,
                               cj.company_name,
                               ejp.department,
                               cj.city,
                               ejp.salary_min,
                               ejp.salary_max,
                               cj.salary_range,
                               ejp.headcount,
                               ejp.description_text,
                               ejp.requirement_text,
                               cj.work_mode,
                               ejp.deadline,
                               ejp.interview_mode,
                               ejp.publish_status,
                               cj.summary,
                               cj.stage,
                               cj.match_score,
                               cj.dimension_scores,
                               cj.reasons,
                               cj.risks,
                               cj.supports,
                               cj.description_items,
                               cj.requirement_items,
                               cj.environment_items,
                               cj.apply_hint,
                               ejp.onsite_required,
                               ejp.remote_supported,
                               ejp.high_frequency_voice_required,
                               ejp.noisy_environment,
                               ejp.long_standing_required,
                               ejp.text_material_supported,
                               ejp.online_interview_supported,
                               ejp.text_interview_supported,
                               ejp.flexible_schedule_supported,
                               ejp.accessible_workspace,
                               ejp.assistive_software_supported,
                               ejp.created_by_user_id,
                               cj.sort_no
                        FROM enterprise_job_posting ejp
                        INNER JOIN catalog_job cj
                                ON cj.id = ejp.job_id
                               AND cj.is_deleted = 0
                        WHERE ejp.job_id = ?
                          AND ejp.created_by_user_id = ?
                          AND ejp.is_deleted = 0
                        LIMIT 1
                        """,
                this::mapDetail,
                jobId,
                ownerUserId
        );
    }

    public List<EnterpriseJobPosting> listPublishedJobs(String city, String workMode, String keyword) {
        List<Object> filters = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder(
                """
                         WHERE ejp.is_deleted = 0
                           AND ejp.publish_status = 'PUBLISHED'
                           AND cj.is_deleted = 0
                        """
        );
        appendContainsFilter(whereClause, filters, "cj.city", city);
        appendContainsFilter(whereClause, filters, "cj.work_mode", workMode);
        appendKeywordFilter(whereClause, filters, keyword);

        return jdbcTemplate.query(
                """
                        SELECT cj.id,
                               cj.title,
                               cj.company_name,
                               ejp.department,
                               cj.city,
                               ejp.salary_min,
                               ejp.salary_max,
                               cj.salary_range,
                               ejp.headcount,
                               ejp.description_text,
                               ejp.requirement_text,
                               cj.work_mode,
                               ejp.deadline,
                               ejp.interview_mode,
                               ejp.publish_status,
                               cj.summary,
                               cj.stage,
                               cj.match_score,
                               cj.dimension_scores,
                               cj.reasons,
                               cj.risks,
                               cj.supports,
                               cj.description_items,
                               cj.requirement_items,
                               cj.environment_items,
                               cj.apply_hint,
                               ejp.onsite_required,
                               ejp.remote_supported,
                               ejp.high_frequency_voice_required,
                               ejp.noisy_environment,
                               ejp.long_standing_required,
                               ejp.text_material_supported,
                               ejp.online_interview_supported,
                               ejp.text_interview_supported,
                               ejp.flexible_schedule_supported,
                               ejp.accessible_workspace,
                               ejp.assistive_software_supported,
                               ejp.created_by_user_id,
                               cj.sort_no
                        FROM enterprise_job_posting ejp
                        INNER JOIN catalog_job cj
                                ON cj.id = ejp.job_id
                        """
                        + whereClause
                        + """

                           ORDER BY cj.sort_no ASC, cj.id ASC
                        """,
                this::mapDetail,
                filters.toArray()
        );
    }

    public EnterpriseJobPosting findPublishedByJobId(String jobId) {
        return querySingle(
                """
                        SELECT cj.id,
                               cj.title,
                               cj.company_name,
                               ejp.department,
                               cj.city,
                               ejp.salary_min,
                               ejp.salary_max,
                               cj.salary_range,
                               ejp.headcount,
                               ejp.description_text,
                               ejp.requirement_text,
                               cj.work_mode,
                               ejp.deadline,
                               ejp.interview_mode,
                               ejp.publish_status,
                               cj.summary,
                               cj.stage,
                               cj.match_score,
                               cj.dimension_scores,
                               cj.reasons,
                               cj.risks,
                               cj.supports,
                               cj.description_items,
                               cj.requirement_items,
                               cj.environment_items,
                               cj.apply_hint,
                               ejp.onsite_required,
                               ejp.remote_supported,
                               ejp.high_frequency_voice_required,
                               ejp.noisy_environment,
                               ejp.long_standing_required,
                               ejp.text_material_supported,
                               ejp.online_interview_supported,
                               ejp.text_interview_supported,
                               ejp.flexible_schedule_supported,
                               ejp.accessible_workspace,
                               ejp.assistive_software_supported,
                               ejp.created_by_user_id,
                               cj.sort_no
                        FROM enterprise_job_posting ejp
                        INNER JOIN catalog_job cj
                                ON cj.id = ejp.job_id
                               AND cj.is_deleted = 0
                        WHERE ejp.job_id = ?
                          AND ejp.publish_status = 'PUBLISHED'
                          AND ejp.is_deleted = 0
                        LIMIT 1
                        """,
                this::mapDetail,
                jobId
        );
    }

    public String getDefaultCompanyName() {
        String companyName = jdbcTemplate.query(
                """
                        SELECT company_name
                        FROM catalog_enterprise_profile
                        WHERE is_deleted = 0
                        ORDER BY sort_no ASC, code ASC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getString("company_name") : null
        );
        return companyName == null || companyName.isBlank() ? "BridgeAbility 企业" : companyName;
    }

    public int nextSortNo() {
        Integer current = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_no), 0) FROM catalog_job WHERE is_deleted = 0",
                Integer.class
        );
        return (current == null ? 0 : current) + 10;
    }

    public void upsert(EnterpriseJobPosting jobPosting) {
        jdbcTemplate.update(
                """
                        INSERT INTO catalog_job (
                            id,
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
                            apply_hint,
                            sort_no,
                            is_deleted
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        ON DUPLICATE KEY UPDATE
                            title = VALUES(title),
                            company_name = VALUES(company_name),
                            city = VALUES(city),
                            salary_range = VALUES(salary_range),
                            work_mode = VALUES(work_mode),
                            summary = VALUES(summary),
                            stage = VALUES(stage),
                            match_score = VALUES(match_score),
                            dimension_scores = VALUES(dimension_scores),
                            reasons = VALUES(reasons),
                            risks = VALUES(risks),
                            supports = VALUES(supports),
                            description_items = VALUES(description_items),
                            requirement_items = VALUES(requirement_items),
                            environment_items = VALUES(environment_items),
                            apply_hint = VALUES(apply_hint),
                            sort_no = VALUES(sort_no),
                            is_deleted = 0
                        """,
                jobPosting.getJobId(),
                jobPosting.getTitle(),
                jobPosting.getCompanyName(),
                jobPosting.getCity(),
                jobPosting.getSalaryRange(),
                jobPosting.getWorkMode(),
                jobPosting.getSummary(),
                jobPosting.getStage(),
                jobPosting.getMatchScore(),
                writeJson(jobPosting.getDimensionScores()),
                writeJson(jobPosting.getReasons()),
                writeJson(jobPosting.getRisks()),
                writeJson(jobPosting.getSupports()),
                writeJson(jobPosting.getDescriptionItems()),
                writeJson(jobPosting.getRequirementItems()),
                writeJson(jobPosting.getEnvironmentItems()),
                jobPosting.getApplyHint(),
                jobPosting.getSortNo()
        );

        jdbcTemplate.update(
                """
                        INSERT INTO enterprise_job_posting (
                            job_id,
                            department,
                            salary_min,
                            salary_max,
                            headcount,
                            description_text,
                            requirement_text,
                            deadline,
                            interview_mode,
                            publish_status,
                            onsite_required,
                            remote_supported,
                            high_frequency_voice_required,
                            noisy_environment,
                            long_standing_required,
                            text_material_supported,
                            online_interview_supported,
                            text_interview_supported,
                            flexible_schedule_supported,
                            accessible_workspace,
                            assistive_software_supported,
                            created_by_user_id,
                            is_deleted
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        ON DUPLICATE KEY UPDATE
                            department = VALUES(department),
                            salary_min = VALUES(salary_min),
                            salary_max = VALUES(salary_max),
                            headcount = VALUES(headcount),
                            description_text = VALUES(description_text),
                            requirement_text = VALUES(requirement_text),
                            deadline = VALUES(deadline),
                            interview_mode = VALUES(interview_mode),
                            publish_status = VALUES(publish_status),
                            onsite_required = VALUES(onsite_required),
                            remote_supported = VALUES(remote_supported),
                            high_frequency_voice_required = VALUES(high_frequency_voice_required),
                            noisy_environment = VALUES(noisy_environment),
                            long_standing_required = VALUES(long_standing_required),
                            text_material_supported = VALUES(text_material_supported),
                            online_interview_supported = VALUES(online_interview_supported),
                            text_interview_supported = VALUES(text_interview_supported),
                            flexible_schedule_supported = VALUES(flexible_schedule_supported),
                            accessible_workspace = VALUES(accessible_workspace),
                            assistive_software_supported = VALUES(assistive_software_supported),
                            created_by_user_id = COALESCE(enterprise_job_posting.created_by_user_id, VALUES(created_by_user_id)),
                            is_deleted = 0
                        """,
                jobPosting.getJobId(),
                jobPosting.getDepartment(),
                jobPosting.getSalaryMin(),
                jobPosting.getSalaryMax(),
                jobPosting.getHeadcount(),
                jobPosting.getDescriptionText(),
                jobPosting.getRequirementText(),
                toSqlDate(jobPosting.getDeadline()),
                jobPosting.getInterviewMode(),
                jobPosting.getPublishStatus(),
                toTinyInt(jobPosting.getOnsiteRequired()),
                toTinyInt(jobPosting.getRemoteSupported()),
                toTinyInt(jobPosting.getHighFrequencyVoiceRequired()),
                toTinyInt(jobPosting.getNoisyEnvironment()),
                toTinyInt(jobPosting.getLongStandingRequired()),
                toTinyInt(jobPosting.getTextMaterialSupported()),
                toTinyInt(jobPosting.getOnlineInterviewSupported()),
                toTinyInt(jobPosting.getTextInterviewSupported()),
                toTinyInt(jobPosting.getFlexibleScheduleSupported()),
                toTinyInt(jobPosting.getAccessibleWorkspace()),
                toTinyInt(jobPosting.getAssistiveSoftwareSupported()),
                jobPosting.getCreatedByUserId()
        );
    }

    public void updatePublishStatus(String jobId, Long ownerUserId, String publishStatus) {
        int updated = jdbcTemplate.update(
                """
                        UPDATE enterprise_job_posting
                        SET publish_status = ?
                        WHERE job_id = ?
                          AND created_by_user_id = ?
                          AND is_deleted = 0
                        """,
                publishStatus,
                jobId,
                ownerUserId
        );
        if (updated == 0) {
            throw new BusinessException(4004, "job not found");
        }
    }

    public long countPublishedJobs() {
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

    private EnterpriseJobSummaryResponse mapSummary(ResultSet rs, int rowNum) throws SQLException {
        int completionRate = calculateAccessibilityCompletionRate(
                getNullableBoolean(rs, "onsite_required"),
                getNullableBoolean(rs, "remote_supported"),
                getNullableBoolean(rs, "high_frequency_voice_required"),
                getNullableBoolean(rs, "noisy_environment"),
                getNullableBoolean(rs, "long_standing_required"),
                getNullableBoolean(rs, "text_material_supported"),
                getNullableBoolean(rs, "online_interview_supported"),
                getNullableBoolean(rs, "text_interview_supported"),
                getNullableBoolean(rs, "flexible_schedule_supported"),
                getNullableBoolean(rs, "accessible_workspace"),
                getNullableBoolean(rs, "assistive_software_supported")
        );
        return new EnterpriseJobSummaryResponse(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("department"),
                rs.getString("city"),
                rs.getString("work_mode"),
                rs.getString("salary_range"),
                rs.getInt("headcount"),
                formatDate(rs.getDate("deadline")),
                rs.getString("publish_status"),
                rs.getString("stage"),
                rs.getInt("match_score"),
                completionRate,
                completionRate == 100,
                rs.getLong("candidate_count")
        );
    }

    private EnterpriseJobPosting mapDetail(ResultSet rs, int rowNum) throws SQLException {
        EnterpriseJobPosting jobPosting = new EnterpriseJobPosting();
        jobPosting.setJobId(rs.getString("id"));
        jobPosting.setTitle(rs.getString("title"));
        jobPosting.setCompanyName(rs.getString("company_name"));
        jobPosting.setDepartment(rs.getString("department"));
        jobPosting.setCity(rs.getString("city"));
        jobPosting.setSalaryMin(rs.getInt("salary_min"));
        jobPosting.setSalaryMax(rs.getInt("salary_max"));
        jobPosting.setSalaryRange(rs.getString("salary_range"));
        jobPosting.setHeadcount(rs.getInt("headcount"));
        jobPosting.setDescriptionText(rs.getString("description_text"));
        jobPosting.setRequirementText(rs.getString("requirement_text"));
        jobPosting.setWorkMode(rs.getString("work_mode"));
        jobPosting.setDeadline(readLocalDate(rs.getDate("deadline")));
        jobPosting.setInterviewMode(rs.getString("interview_mode"));
        jobPosting.setPublishStatus(rs.getString("publish_status"));
        jobPosting.setSummary(rs.getString("summary"));
        jobPosting.setStage(rs.getString("stage"));
        jobPosting.setMatchScore(rs.getInt("match_score"));
        jobPosting.setDimensionScores(readScoreItems(rs.getString("dimension_scores")));
        jobPosting.setReasons(readStringList(rs.getString("reasons")));
        jobPosting.setRisks(readStringList(rs.getString("risks")));
        jobPosting.setSupports(readStringList(rs.getString("supports")));
        jobPosting.setDescriptionItems(readStringList(rs.getString("description_items")));
        jobPosting.setRequirementItems(readStringList(rs.getString("requirement_items")));
        jobPosting.setEnvironmentItems(readStringList(rs.getString("environment_items")));
        jobPosting.setApplyHint(rs.getString("apply_hint"));
        jobPosting.setOnsiteRequired(getNullableBoolean(rs, "onsite_required"));
        jobPosting.setRemoteSupported(getNullableBoolean(rs, "remote_supported"));
        jobPosting.setHighFrequencyVoiceRequired(getNullableBoolean(rs, "high_frequency_voice_required"));
        jobPosting.setNoisyEnvironment(getNullableBoolean(rs, "noisy_environment"));
        jobPosting.setLongStandingRequired(getNullableBoolean(rs, "long_standing_required"));
        jobPosting.setTextMaterialSupported(getNullableBoolean(rs, "text_material_supported"));
        jobPosting.setOnlineInterviewSupported(getNullableBoolean(rs, "online_interview_supported"));
        jobPosting.setTextInterviewSupported(getNullableBoolean(rs, "text_interview_supported"));
        jobPosting.setFlexibleScheduleSupported(getNullableBoolean(rs, "flexible_schedule_supported"));
        jobPosting.setAccessibleWorkspace(getNullableBoolean(rs, "accessible_workspace"));
        jobPosting.setAssistiveSoftwareSupported(getNullableBoolean(rs, "assistive_software_supported"));
        Object createdBy = rs.getObject("created_by_user_id");
        jobPosting.setCreatedByUserId(createdBy == null ? null : rs.getLong("created_by_user_id"));
        jobPosting.setSortNo(rs.getInt("sort_no"));
        return jobPosting;
    }

    private void appendContainsFilter(StringBuilder whereClause, List<Object> filters, String columnName, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        whereClause.append(" AND LOWER(").append(columnName).append(") LIKE ?");
        filters.add("%" + value.trim().toLowerCase(Locale.ROOT) + "%");
    }

    private void appendKeywordFilter(StringBuilder whereClause, List<Object> filters, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        String normalized = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        whereClause.append(
                """
                         AND (
                             LOWER(cj.title) LIKE ?
                             OR LOWER(cj.company_name) LIKE ?
                             OR LOWER(cj.summary) LIKE ?
                             OR LOWER(ejp.description_text) LIKE ?
                             OR LOWER(ejp.requirement_text) LIKE ?
                         )
                        """
        );
        filters.add(normalized);
        filters.add(normalized);
        filters.add(normalized);
        filters.add(normalized);
        filters.add(normalized);
    }

    private int sanitizePage(Integer page) {
        return page == null ? DEFAULT_PAGE : Math.max(page, 1);
    }

    private int sanitizePageSize(Integer pageSize) {
        int safePageSize = pageSize == null ? DEFAULT_PAGE_SIZE : Math.max(pageSize, 1);
        return Math.min(safePageSize, MAX_PAGE_SIZE);
    }

    private int calculateAccessibilityCompletionRate(Boolean... values) {
        int filled = 0;
        for (Boolean value : values) {
            if (value != null) {
                filled += 1;
            }
        }
        return Math.toIntExact(Math.round(filled * 100.0 / values.length));
    }

    private Boolean getNullableBoolean(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        return value == null ? null : rs.getInt(columnName) == 1;
    }

    private Integer toTinyInt(Boolean value) {
        if (value == null) {
            return null;
        }
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private Date toSqlDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    private LocalDate readLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private String formatDate(Date value) {
        LocalDate date = readLocalDate(value);
        return date == null ? "" : date.toString();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to write JSON column", ex);
        }
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
}
