package com.rongzhiqiao.serviceorg.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rongzhiqiao.common.api.PageResponse;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.serviceorg.vo.ServiceAlertResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceCaseSummaryResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceFollowupResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceInterventionResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceResourceReferralResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
public class ServiceOrgRepository {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public List<ServiceCaseSummaryResponse> listCases() {
        return jdbcTemplate.query(
                """
                        SELECT c.id,
                               c.user_id,
                               c.name,
                               c.stage,
                               c.owner_name,
                               c.next_action,
                               c.alert_level,
                               c.timeline,
                               c.profile_authorized,
                                (
                                    SELECT COUNT(1)
                                    FROM catalog_service_alert a
                                    WHERE a.case_id = c.id
                                      AND a.is_deleted = 0
                                     AND a.alert_status IN ('待处理', 'OPEN', 'PENDING', 'ESCALATED')
                               ) AS pending_alert_count,
                                (
                                    SELECT COUNT(1)
                                    FROM service_followup_record f
                                    WHERE f.case_id = c.id
                                      AND f.is_deleted = 0
                                ) AS followup_count,
                                (
                                    SELECT COUNT(1)
                                    FROM service_resource_referral r
                                    WHERE r.case_id = c.id
                                      AND r.is_deleted = 0
                                ) AS referral_count
                        FROM catalog_service_case c
                        WHERE c.is_deleted = 0
                        ORDER BY c.sort_no ASC, c.id ASC
                        """,
                (rs, rowNum) -> new ServiceCaseSummaryResponse(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("stage"),
                        rs.getString("owner_name"),
                        rs.getString("next_action"),
                        rs.getString("alert_level"),
                        readStringList(rs.getString("timeline")),
                        rs.getInt("profile_authorized") == 1,
                        rs.getInt("pending_alert_count"),
                        rs.getInt("followup_count"),
                        rs.getInt("referral_count")
                )
        );
    }

    public List<ServiceCaseSummaryResponse> listCasesByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return jdbcTemplate.query(
                """
                        SELECT c.id,
                               c.user_id,
                               c.name,
                               c.stage,
                               c.owner_name,
                               c.next_action,
                               c.alert_level,
                               c.timeline,
                               c.profile_authorized,
                                (
                                    SELECT COUNT(1)
                                    FROM catalog_service_alert a
                                    WHERE a.case_id = c.id
                                      AND a.is_deleted = 0
                                     AND a.alert_status IN ('待处理', 'OPEN', 'PENDING', 'ESCALATED')
                               ) AS pending_alert_count,
                                (
                                    SELECT COUNT(1)
                                    FROM service_followup_record f
                                    WHERE f.case_id = c.id
                                      AND f.is_deleted = 0
                                ) AS followup_count,
                                (
                                    SELECT COUNT(1)
                                    FROM service_resource_referral r
                                    WHERE r.case_id = c.id
                                      AND r.is_deleted = 0
                                ) AS referral_count
                        FROM catalog_service_case c
                        WHERE c.user_id = ?
                          AND c.is_deleted = 0
                        ORDER BY c.sort_no DESC, c.id DESC
                        """,
                (rs, rowNum) -> new ServiceCaseSummaryResponse(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("stage"),
                        rs.getString("owner_name"),
                        rs.getString("next_action"),
                        rs.getString("alert_level"),
                        readStringList(rs.getString("timeline")),
                        rs.getInt("profile_authorized") == 1,
                        rs.getInt("pending_alert_count"),
                        rs.getInt("followup_count"),
                        rs.getInt("referral_count")
                ),
                userId
        );
    }

    public ServiceCaseRecord findCaseById(String caseId) {
        return querySingle(
                """
                        SELECT id,
                               user_id,
                               name,
                               stage,
                               owner_name,
                               next_action,
                               alert_level,
                               intake_note,
                               profile_authorized,
                               authorization_note,
                               authorization_updated_by,
                               authorization_updated_at,
                               timeline,
                               sort_no
                        FROM catalog_service_case
                        WHERE id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                (rs, rowNum) -> new ServiceCaseRecord(
                        rs.getString("id"),
                        rs.getObject("user_id") == null ? null : rs.getLong("user_id"),
                        rs.getString("name"),
                        rs.getString("stage"),
                        rs.getString("owner_name"),
                        rs.getString("next_action"),
                        rs.getString("alert_level"),
                        rs.getString("intake_note"),
                        rs.getInt("profile_authorized") == 1,
                        rs.getString("authorization_note"),
                        rs.getString("authorization_updated_by"),
                        formatDateTime(rs.getTimestamp("authorization_updated_at")),
                        readStringList(rs.getString("timeline")),
                        rs.getInt("sort_no")
                ),
                caseId
        );
    }

    public ServiceCaseRecord findCaseByIdAndUserId(String caseId, Long userId) {
        if (userId == null) {
            return null;
        }
        return querySingle(
                """
                        SELECT id,
                               user_id,
                               name,
                               stage,
                               owner_name,
                               next_action,
                               alert_level,
                               intake_note,
                               profile_authorized,
                               authorization_note,
                               authorization_updated_by,
                               authorization_updated_at,
                               timeline,
                               sort_no
                        FROM catalog_service_case
                        WHERE id = ?
                          AND user_id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                (rs, rowNum) -> new ServiceCaseRecord(
                        rs.getString("id"),
                        rs.getObject("user_id") == null ? null : rs.getLong("user_id"),
                        rs.getString("name"),
                        rs.getString("stage"),
                        rs.getString("owner_name"),
                        rs.getString("next_action"),
                        rs.getString("alert_level"),
                        rs.getString("intake_note"),
                        rs.getInt("profile_authorized") == 1,
                        rs.getString("authorization_note"),
                        rs.getString("authorization_updated_by"),
                        formatDateTime(rs.getTimestamp("authorization_updated_at")),
                        readStringList(rs.getString("timeline")),
                        rs.getInt("sort_no")
                ),
                caseId,
                userId
        );
    }

    public ServiceCaseRecord findCaseByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return querySingle(
                """
                        SELECT id,
                               user_id,
                               name,
                               stage,
                               owner_name,
                               next_action,
                               alert_level,
                               intake_note,
                               profile_authorized,
                               authorization_note,
                               authorization_updated_by,
                               authorization_updated_at,
                               timeline,
                               sort_no
                        FROM catalog_service_case
                        WHERE user_id = ?
                          AND is_deleted = 0
                        ORDER BY sort_no DESC, id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new ServiceCaseRecord(
                        rs.getString("id"),
                        rs.getObject("user_id") == null ? null : rs.getLong("user_id"),
                        rs.getString("name"),
                        rs.getString("stage"),
                        rs.getString("owner_name"),
                        rs.getString("next_action"),
                        rs.getString("alert_level"),
                        rs.getString("intake_note"),
                        rs.getInt("profile_authorized") == 1,
                        rs.getString("authorization_note"),
                        rs.getString("authorization_updated_by"),
                        formatDateTime(rs.getTimestamp("authorization_updated_at")),
                        readStringList(rs.getString("timeline")),
                        rs.getInt("sort_no")
                ),
                userId
        );
    }

    public List<ServiceInterventionResponse> listInterventions(String caseId) {
        return jdbcTemplate.query(
                """
                        SELECT id,
                               case_id,
                               intervention_type,
                               content,
                               attachment_note,
                               operator_name,
                               created_at
                        FROM service_case_intervention
                        WHERE case_id = ?
                          AND is_deleted = 0
                        ORDER BY created_at DESC, id DESC
                        """,
                this::mapIntervention,
                caseId
        );
    }

    public List<ServiceFollowupResponse> listFollowups(String caseId) {
        return jdbcTemplate.query(
                """
                        SELECT id,
                               case_id,
                               job_id,
                               followup_stage,
                               adaptation_score,
                               environment_issue,
                               communication_issue,
                               support_implemented,
                               leave_risk,
                               need_help,
                               record_status,
                               operator_name,
                               due_at,
                               created_at,
                               completed_at
                        FROM service_followup_record
                        WHERE case_id = ?
                          AND is_deleted = 0
                        ORDER BY COALESCE(completed_at, due_at, created_at) DESC, id DESC
                        """,
                this::mapFollowup,
                caseId
        );
    }

    public List<ServiceResourceReferralResponse> listReferrals(String caseId) {
        return jdbcTemplate.query(
                """
                        SELECT id,
                               case_id,
                               referral_type,
                               resource_name,
                               provider_name,
                               contact_name,
                               contact_phone,
                               scheduled_at,
                               referral_status,
                               status_note,
                               operator_name,
                               created_at,
                               updated_at
                        FROM service_resource_referral
                        WHERE case_id = ?
                          AND is_deleted = 0
                        ORDER BY COALESCE(scheduled_at, updated_at, created_at) DESC, id DESC
                        """,
                this::mapReferral,
                caseId
        );
    }

    public List<ServiceAlertResponse> listAlertsByCaseId(String caseId) {
        return jdbcTemplate.query(
                """
                        SELECT alert_id,
                               case_id,
                               user_id,
                               name,
                               alert_type,
                               alert_level,
                               trigger_reason,
                               created_at,
                               alert_status,
                               resolution_note,
                               handled_by,
                               handled_at
                        FROM catalog_service_alert
                        WHERE case_id = ?
                          AND is_deleted = 0
                        ORDER BY created_at DESC, alert_id DESC
                        """,
                this::mapAlert,
                caseId
        );
    }

    public ServiceAlertResponse findAlertById(String alertId) {
        return querySingle(
                """
                        SELECT alert_id,
                               case_id,
                               user_id,
                               name,
                               alert_type,
                               alert_level,
                               trigger_reason,
                               created_at,
                               alert_status,
                               resolution_note,
                               handled_by,
                               handled_at
                        FROM catalog_service_alert
                        WHERE alert_id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                this::mapAlert,
                alertId
        );
    }

    public PageResponse<ServiceAlertResponse> listAlerts(String status, Integer level, Integer page, Integer pageSize) {
        List<Object> filters = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder(" WHERE is_deleted = 0");
        appendContainsFilter(whereClause, filters, "alert_status", status);
        if (level != null) {
            whereClause.append(" AND alert_level = ?");
            filters.add(level);
        }

        int safePage = sanitizePage(page);
        int safePageSize = sanitizePageSize(pageSize);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM catalog_service_alert" + whereClause,
                Long.class,
                filters.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(filters);
        queryArgs.add(safePageSize);
        queryArgs.add((safePage - 1) * safePageSize);

        List<ServiceAlertResponse> list = jdbcTemplate.query(
                """
                        SELECT alert_id,
                               case_id,
                               user_id,
                               name,
                               alert_type,
                               alert_level,
                               trigger_reason,
                               created_at,
                               alert_status,
                               resolution_note,
                               handled_by,
                               handled_at
                        FROM catalog_service_alert
                        """
                        + whereClause
                        + """

                           ORDER BY alert_level DESC, created_at DESC, alert_id DESC
                           LIMIT ?
                           OFFSET ?
                           """,
                this::mapAlert,
                queryArgs.toArray()
        );
        return new PageResponse<>(total == null ? 0 : total, safePage, safePageSize, list);
    }

    public ServiceInterventionResponse insertIntervention(String caseId,
                                                         String interventionType,
                                                         String content,
                                                         String attachmentNote,
                                                         String operatorName,
                                                         LocalDateTime createdAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO service_case_intervention (
                            case_id,
                            intervention_type,
                            content,
                            attachment_note,
                            operator_name,
                            created_at,
                            is_deleted
                        )
                        VALUES (?, ?, ?, ?, ?, ?, 0)
                        """,
                caseId,
                interventionType,
                content,
                attachmentNote,
                operatorName,
                Timestamp.valueOf(createdAt)
        );

        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return new ServiceInterventionResponse(
                id == null ? 0L : id,
                caseId,
                interventionType,
                content,
                attachmentNote,
                operatorName,
                formatDateTime(Timestamp.valueOf(createdAt))
        );
    }

    public ServiceCaseRecord insertCase(String caseId,
                                        Long userId,
                                        String name,
                                        String stage,
                                        String ownerName,
                                        String nextAction,
                                        String alertLevel,
                                        String intakeNote,
                                        boolean profileAuthorized,
                                        String authorizationNote,
                                        String authorizationUpdatedBy,
                                        LocalDateTime authorizationUpdatedAt,
                                        List<String> timeline,
                                        int sortNo) {
        jdbcTemplate.update(
                """
                        INSERT INTO catalog_service_case (
                            id,
                            user_id,
                            name,
                            stage,
                            owner_name,
                            next_action,
                            alert_level,
                            intake_note,
                            profile_authorized,
                            authorization_note,
                            authorization_updated_by,
                            authorization_updated_at,
                            timeline,
                            sort_no,
                            is_deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                caseId,
                userId,
                name,
                stage,
                ownerName,
                nextAction,
                alertLevel,
                blankToNull(intakeNote),
                profileAuthorized ? 1 : 0,
                blankToNull(authorizationNote),
                blankToNull(authorizationUpdatedBy),
                authorizationUpdatedAt == null ? null : Timestamp.valueOf(authorizationUpdatedAt),
                writeStringList(timeline),
                sortNo
        );
        return findCaseById(caseId);
    }

    public void updateCaseProfileAccess(String caseId,
                                        boolean profileAuthorized,
                                        String authorizationNote,
                                        String operatorName,
                                        LocalDateTime authorizationUpdatedAt) {
        jdbcTemplate.update(
                """
                        UPDATE catalog_service_case
                        SET profile_authorized = ?,
                            authorization_note = ?,
                            authorization_updated_by = ?,
                            authorization_updated_at = ?
                        WHERE id = ?
                          AND is_deleted = 0
                        """,
                profileAuthorized ? 1 : 0,
                blankToNull(authorizationNote),
                blankToNull(operatorName),
                authorizationUpdatedAt == null ? null : Timestamp.valueOf(authorizationUpdatedAt),
                caseId
        );
    }

    public ServiceFollowupResponse insertFollowup(String caseId,
                                                  Long userId,
                                                  String jobId,
                                                  String followupStage,
                                                  int adaptationScore,
                                                  String environmentIssue,
                                                  String communicationIssue,
                                                  boolean supportImplemented,
                                                  boolean leaveRisk,
                                                  boolean needHelp,
                                                  String operatorName,
                                                  LocalDateTime createdAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO service_followup_record (
                            case_id,
                            job_id,
                            followup_stage,
                            adaptation_score,
                            environment_issue,
                            communication_issue,
                            support_implemented,
                            leave_risk,
                            need_help,
                            record_status,
                            operator_name,
                            due_at,
                            created_at,
                            completed_at,
                            is_deleted
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'COMPLETED', ?, NULL, ?, ?, 0)
                        """,
                caseId,
                blankToNull(jobId),
                followupStage,
                adaptationScore,
                blankToNull(environmentIssue),
                blankToNull(communicationIssue),
                supportImplemented ? 1 : 0,
                leaveRisk ? 1 : 0,
                needHelp ? 1 : 0,
                operatorName,
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(createdAt)
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return new ServiceFollowupResponse(
                id == null ? 0L : id,
                caseId,
                blankToNull(jobId),
                followupStage,
                adaptationScore,
                blankToNull(environmentIssue),
                blankToNull(communicationIssue),
                supportImplemented,
                leaveRisk,
                needHelp,
                "COMPLETED",
                operatorName,
                "",
                formatDateTime(Timestamp.valueOf(createdAt)),
                formatDateTime(Timestamp.valueOf(createdAt))
        );
    }

    public ServiceFollowupResponse insertScheduledFollowup(String caseId,
                                                           String jobId,
                                                           String followupStage,
                                                           String operatorName,
                                                           LocalDateTime createdAt,
                                                           LocalDateTime dueAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO service_followup_record (
                            case_id,
                            job_id,
                            followup_stage,
                            adaptation_score,
                            environment_issue,
                            communication_issue,
                            support_implemented,
                            leave_risk,
                            need_help,
                            record_status,
                            operator_name,
                            due_at,
                            created_at,
                            completed_at,
                            is_deleted
                        ) VALUES (?, ?, ?, 0, NULL, NULL, 0, 0, 0, 'PENDING', ?, ?, ?, NULL, 0)
                        """,
                caseId,
                blankToNull(jobId),
                followupStage,
                operatorName,
                Timestamp.valueOf(dueAt),
                Timestamp.valueOf(createdAt)
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return new ServiceFollowupResponse(
                id == null ? 0L : id,
                caseId,
                blankToNull(jobId),
                followupStage,
                0,
                null,
                null,
                false,
                false,
                false,
                "PENDING",
                operatorName,
                formatDateTime(Timestamp.valueOf(dueAt)),
                formatDateTime(Timestamp.valueOf(createdAt)),
                ""
        );
    }

    public ServiceFollowupRecord findLatestFollowupRecord(String caseId, String jobId, String followupStage) {
        return querySingle(
                """
                        SELECT id,
                               case_id,
                               job_id,
                               followup_stage,
                               record_status,
                               due_at,
                               created_at,
                               completed_at
                        FROM service_followup_record
                        WHERE case_id = ?
                          AND ((? IS NULL AND job_id IS NULL) OR job_id = ?)
                          AND followup_stage = ?
                          AND is_deleted = 0
                        ORDER BY COALESCE(completed_at, due_at, created_at) DESC, id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new ServiceFollowupRecord(
                        rs.getLong("id"),
                        rs.getString("case_id"),
                        rs.getString("job_id"),
                        rs.getString("followup_stage"),
                        rs.getString("record_status"),
                        formatDateTime(rs.getTimestamp("due_at")),
                        formatDateTime(rs.getTimestamp("created_at")),
                        formatDateTime(rs.getTimestamp("completed_at"))
                ),
                caseId,
                blankToNull(jobId),
                blankToNull(jobId),
                followupStage
        );
    }

    public boolean hasFollowupRecord(String caseId, String jobId, String followupStage) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM service_followup_record
                        WHERE case_id = ?
                          AND ((? IS NULL AND job_id IS NULL) OR job_id = ?)
                          AND followup_stage = ?
                          AND is_deleted = 0
                        """,
                Integer.class,
                caseId,
                blankToNull(jobId),
                blankToNull(jobId),
                followupStage
        );
        return count != null && count > 0;
    }

    public void updateFollowupCompletion(long followupId,
                                         int adaptationScore,
                                         String environmentIssue,
                                         String communicationIssue,
                                         boolean supportImplemented,
                                         boolean leaveRisk,
                                         boolean needHelp,
                                         String operatorName,
                                         LocalDateTime completedAt) {
        int updated = jdbcTemplate.update(
                """
                        UPDATE service_followup_record
                        SET adaptation_score = ?,
                            environment_issue = ?,
                            communication_issue = ?,
                            support_implemented = ?,
                            leave_risk = ?,
                            need_help = ?,
                            record_status = 'COMPLETED',
                            operator_name = ?,
                            completed_at = ?
                        WHERE id = ?
                          AND is_deleted = 0
                        """,
                adaptationScore,
                blankToNull(environmentIssue),
                blankToNull(communicationIssue),
                supportImplemented ? 1 : 0,
                leaveRisk ? 1 : 0,
                needHelp ? 1 : 0,
                operatorName,
                Timestamp.valueOf(completedAt),
                followupId
        );
        if (updated == 0) {
            throw new BusinessException(4004, "followup record not found");
        }
    }

    public ServiceResourceReferralResponse insertReferral(String caseId,
                                                          String referralType,
                                                          String resourceName,
                                                          String providerName,
                                                          String contactName,
                                                          String contactPhone,
                                                          LocalDateTime scheduledAt,
                                                          String referralStatus,
                                                          String statusNote,
                                                          String operatorName,
                                                          LocalDateTime createdAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO service_resource_referral (
                            case_id,
                            referral_type,
                            resource_name,
                            provider_name,
                            contact_name,
                            contact_phone,
                            scheduled_at,
                            referral_status,
                            status_note,
                            operator_name,
                            created_at,
                            updated_at,
                            is_deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                caseId,
                referralType,
                resourceName,
                blankToNull(providerName),
                blankToNull(contactName),
                blankToNull(contactPhone),
                scheduledAt == null ? null : Timestamp.valueOf(scheduledAt),
                referralStatus,
                blankToNull(statusNote),
                operatorName,
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(createdAt)
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return findReferralById(id == null ? 0L : id);
    }

    public ServiceResourceReferralResponse findReferralById(long referralId) {
        return querySingle(
                """
                        SELECT id,
                               case_id,
                               referral_type,
                               resource_name,
                               provider_name,
                               contact_name,
                               contact_phone,
                               scheduled_at,
                               referral_status,
                               status_note,
                               operator_name,
                               created_at,
                               updated_at
                        FROM service_resource_referral
                        WHERE id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                this::mapReferral,
                referralId
        );
    }

    public ServiceResourceReferralResponse updateReferralStatus(long referralId,
                                                                String targetStatus,
                                                                String statusNote,
                                                                String operatorName,
                                                                LocalDateTime updatedAt) {
        int updated = jdbcTemplate.update(
                """
                        UPDATE service_resource_referral
                        SET referral_status = ?,
                            status_note = ?,
                            operator_name = ?,
                            updated_at = ?
                        WHERE id = ?
                          AND is_deleted = 0
                        """,
                targetStatus,
                blankToNull(statusNote),
                operatorName,
                Timestamp.valueOf(updatedAt),
                referralId
        );
        if (updated == 0) {
            throw new BusinessException(4004, "resource referral not found");
        }
        return findReferralById(referralId);
    }

    public ServiceAlertResponse insertAlert(String alertId,
                                            String caseId,
                                            Long userId,
                                            String name,
                                            String alertType,
                                            int alertLevel,
                                            String triggerReason,
                                            String alertStatus,
                                            LocalDateTime createdAt,
                                            int sortNo) {
        jdbcTemplate.update(
                """
                        INSERT INTO catalog_service_alert (
                            alert_id,
                            case_id,
                            user_id,
                            name,
                            alert_type,
                            alert_level,
                            trigger_reason,
                            created_at,
                            alert_status,
                            resolution_note,
                            handled_by,
                            handled_at,
                            sort_no,
                            is_deleted
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, ?, 0)
                        """,
                alertId,
                caseId,
                userId,
                name,
                alertType,
                alertLevel,
                triggerReason,
                Timestamp.valueOf(createdAt),
                alertStatus,
                sortNo
        );
        return new ServiceAlertResponse(
                alertId,
                caseId,
                userId,
                name,
                alertType,
                alertLevel,
                triggerReason,
                formatDateTime(Timestamp.valueOf(createdAt)),
                alertStatus,
                null,
                null,
                null
        );
    }

    public ServiceAlertResponse updateAlertStatus(String alertId,
                                                  String targetStatus,
                                                  String resolutionNote,
                                                  String operatorName,
                                                  LocalDateTime handledAt) {
        int updated = jdbcTemplate.update(
                """
                        UPDATE catalog_service_alert
                        SET alert_status = ?,
                            resolution_note = ?,
                            handled_by = ?,
                            handled_at = ?
                        WHERE alert_id = ?
                          AND is_deleted = 0
                        """,
                targetStatus,
                resolutionNote,
                operatorName,
                Timestamp.valueOf(handledAt),
                alertId
        );
        if (updated == 0) {
            throw new BusinessException(4004, "alert not found");
        }
        return querySingle(
                """
                        SELECT alert_id,
                               case_id,
                               user_id,
                               name,
                               alert_type,
                               alert_level,
                               trigger_reason,
                               created_at,
                               alert_status,
                               resolution_note,
                               handled_by,
                               handled_at
                        FROM catalog_service_alert
                        WHERE alert_id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                this::mapAlert,
                alertId
        );
    }

    public void updateCaseProgress(String caseId, String nextAction, String alertLevel) {
        jdbcTemplate.update(
                """
                        UPDATE catalog_service_case
                        SET next_action = ?,
                            alert_level = ?
                        WHERE id = ?
                          AND is_deleted = 0
                        """,
                nextAction,
                alertLevel,
                caseId
        );
    }

    public void updateCaseTimeline(String caseId, List<String> timeline) {
        jdbcTemplate.update(
                """
                        UPDATE catalog_service_case
                        SET timeline = ?
                        WHERE id = ?
                          AND is_deleted = 0
                        """,
                writeStringList(timeline),
                caseId
        );
    }

    public int nextAlertSortNo() {
        Integer current = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_no), 0) FROM catalog_service_alert WHERE is_deleted = 0",
                Integer.class
        );
        return (current == null ? 0 : current) + 10;
    }

    public int nextCaseSortNo() {
        Integer current = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_no), 0) FROM catalog_service_case WHERE is_deleted = 0",
                Integer.class
        );
        return (current == null ? 0 : current) + 10;
    }

    public int maxOpenAlertLevel(String caseId) {
        Integer level = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(MAX(alert_level), 0)
                        FROM catalog_service_alert
                        WHERE case_id = ?
                          AND is_deleted = 0
                          AND alert_status IN ('待处理', 'OPEN', 'PENDING', 'ESCALATED')
                        """,
                Integer.class,
                caseId
        );
        return level == null ? 0 : level;
    }

    private ServiceInterventionResponse mapIntervention(ResultSet rs, int rowNum) throws SQLException {
        return new ServiceInterventionResponse(
                rs.getLong("id"),
                rs.getString("case_id"),
                rs.getString("intervention_type"),
                rs.getString("content"),
                rs.getString("attachment_note"),
                rs.getString("operator_name"),
                formatDateTime(rs.getTimestamp("created_at"))
        );
    }

    private ServiceFollowupResponse mapFollowup(ResultSet rs, int rowNum) throws SQLException {
        return new ServiceFollowupResponse(
                rs.getLong("id"),
                rs.getString("case_id"),
                rs.getString("job_id"),
                rs.getString("followup_stage"),
                rs.getInt("adaptation_score"),
                rs.getString("environment_issue"),
                rs.getString("communication_issue"),
                rs.getInt("support_implemented") == 1,
                rs.getInt("leave_risk") == 1,
                rs.getInt("need_help") == 1,
                rs.getString("record_status"),
                rs.getString("operator_name"),
                formatDateTime(rs.getTimestamp("due_at")),
                formatDateTime(rs.getTimestamp("created_at")),
                formatDateTime(rs.getTimestamp("completed_at"))
        );
    }

    private ServiceResourceReferralResponse mapReferral(ResultSet rs, int rowNum) throws SQLException {
        return new ServiceResourceReferralResponse(
                rs.getLong("id"),
                rs.getString("case_id"),
                rs.getString("referral_type"),
                rs.getString("resource_name"),
                rs.getString("provider_name"),
                rs.getString("contact_name"),
                rs.getString("contact_phone"),
                formatDateTime(rs.getTimestamp("scheduled_at")),
                rs.getString("referral_status"),
                rs.getString("status_note"),
                rs.getString("operator_name"),
                formatDateTime(rs.getTimestamp("created_at")),
                formatDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private ServiceAlertResponse mapAlert(ResultSet rs, int rowNum) throws SQLException {
        return new ServiceAlertResponse(
                rs.getString("alert_id"),
                rs.getString("case_id"),
                rs.getLong("user_id"),
                rs.getString("name"),
                rs.getString("alert_type"),
                rs.getInt("alert_level"),
                rs.getString("trigger_reason"),
                formatDateTime(rs.getTimestamp("created_at")),
                rs.getString("alert_status"),
                rs.getString("resolution_note"),
                rs.getString("handled_by"),
                formatDateTime(rs.getTimestamp("handled_at"))
        );
    }

    private void appendContainsFilter(StringBuilder whereClause, List<Object> filters, String columnName, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        whereClause.append(" AND LOWER(").append(columnName).append(") LIKE ?");
        filters.add("%" + value.trim().toLowerCase(Locale.ROOT) + "%");
    }

    private int sanitizePage(Integer page) {
        return page == null ? DEFAULT_PAGE : Math.max(page, 1);
    }

    private int sanitizePageSize(Integer pageSize) {
        int safePageSize = pageSize == null ? DEFAULT_PAGE_SIZE : Math.max(pageSize, 1);
        return Math.min(safePageSize, MAX_PAGE_SIZE);
    }

    private String formatDateTime(Timestamp value) {
        return value == null ? "" : value.toLocalDateTime().format(DATE_TIME_FORMATTER);
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            return list == null ? List.of() : List.copyOf(list);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse JSON column", ex);
        }
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : List.copyOf(values));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize JSON column", ex);
        }
    }

    private <T> T querySingle(String sql, RowMapper<T> rowMapper, Object... args) {
        List<T> result = jdbcTemplate.query(sql, rowMapper, args);
        return result.isEmpty() ? null : result.get(0);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ServiceCaseRecord(
            String id,
            Long userId,
            String name,
            String stage,
            String owner,
            String nextAction,
            String alertLevel,
            String intakeNote,
            boolean profileAuthorized,
            String authorizationNote,
            String authorizationUpdatedBy,
            String authorizationUpdatedAt,
            List<String> timeline,
            int sortNo
    ) {
    }

    public record ServiceFollowupRecord(
            long id,
            String caseId,
            String jobId,
            String followupStage,
            String recordStatus,
            String dueAt,
            String createdAt,
            String completedAt
    ) {
    }
}
