package com.rongzhiqiao.issue.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IssueRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public List<IssueTicketRecord> listIssues(String issueType, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder(" WHERE is_deleted = 0");
        if (issueType != null) {
            whereClause.append(" AND issue_type = ?");
            args.add(issueType);
        }
        if (status != null) {
            whereClause.append(" AND ticket_status = ?");
            args.add(status);
        }

        return jdbcTemplate.query(
                """
                        SELECT id,
                               issue_type,
                               source_role,
                               source_user_id,
                               source_name,
                               title,
                               content,
                               related_type,
                               related_id,
                               severity_level,
                               ticket_status,
                               resolution_note,
                               handled_by,
                               handled_at,
                               created_at,
                               updated_at,
                               sort_no
                        FROM admin_issue_ticket
                        """
                        + whereClause
                        + """

                           ORDER BY severity_level DESC,
                                    CASE ticket_status
                                        WHEN 'PENDING' THEN 0
                                        WHEN 'IN_PROGRESS' THEN 1
                                        ELSE 2
                                    END,
                                    created_at DESC,
                                    sort_no ASC,
                                    id ASC
                           """,
                (rs, rowNum) -> new IssueTicketRecord(
                        rs.getString("id"),
                        rs.getString("issue_type"),
                        rs.getString("source_role"),
                        rs.getObject("source_user_id") == null ? null : rs.getLong("source_user_id"),
                        rs.getString("source_name"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("related_type"),
                        rs.getString("related_id"),
                        rs.getInt("severity_level"),
                        rs.getString("ticket_status"),
                        rs.getString("resolution_note"),
                        rs.getString("handled_by"),
                        toLocalDateTime(rs.getTimestamp("handled_at")),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        rs.getInt("sort_no")
                ),
                args.toArray()
        );
    }

    public IssueTicketRecord findById(String issueId) {
        List<IssueTicketRecord> list = jdbcTemplate.query(
                """
                        SELECT id,
                               issue_type,
                               source_role,
                               source_user_id,
                               source_name,
                               title,
                               content,
                               related_type,
                               related_id,
                               severity_level,
                               ticket_status,
                               resolution_note,
                               handled_by,
                               handled_at,
                               created_at,
                               updated_at,
                               sort_no
                        FROM admin_issue_ticket
                        WHERE id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                (rs, rowNum) -> new IssueTicketRecord(
                        rs.getString("id"),
                        rs.getString("issue_type"),
                        rs.getString("source_role"),
                        rs.getObject("source_user_id") == null ? null : rs.getLong("source_user_id"),
                        rs.getString("source_name"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("related_type"),
                        rs.getString("related_id"),
                        rs.getInt("severity_level"),
                        rs.getString("ticket_status"),
                        rs.getString("resolution_note"),
                        rs.getString("handled_by"),
                        toLocalDateTime(rs.getTimestamp("handled_at")),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        rs.getInt("sort_no")
                ),
                issueId
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public IssueTicketRecord insert(IssueTicketRecord record) {
        jdbcTemplate.update(
                """
                        INSERT INTO admin_issue_ticket (
                            id,
                            issue_type,
                            source_role,
                            source_user_id,
                            source_name,
                            title,
                            content,
                            related_type,
                            related_id,
                            severity_level,
                            ticket_status,
                            resolution_note,
                            handled_by,
                            handled_at,
                            created_at,
                            updated_at,
                            sort_no,
                            is_deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                record.id(),
                record.issueType(),
                record.sourceRole(),
                record.sourceUserId(),
                record.sourceName(),
                record.title(),
                record.content(),
                record.relatedType(),
                record.relatedId(),
                record.severityLevel(),
                record.ticketStatus(),
                record.resolutionNote(),
                record.handledBy(),
                toTimestamp(record.handledAt()),
                toTimestamp(record.createdAt()),
                toTimestamp(record.updatedAt()),
                record.sortNo()
        );
        return findById(record.id());
    }

    public IssueTicketRecord updateStatus(String issueId,
                                          String targetStatus,
                                          String resolutionNote,
                                          String handledBy,
                                          LocalDateTime handledAt) {
        jdbcTemplate.update(
                """
                        UPDATE admin_issue_ticket
                        SET ticket_status = ?,
                            resolution_note = ?,
                            handled_by = ?,
                            handled_at = ?,
                            updated_at = ?
                        WHERE id = ?
                          AND is_deleted = 0
                        """,
                targetStatus,
                resolutionNote,
                handledBy,
                toTimestamp(handledAt),
                toTimestamp(handledAt),
                issueId
        );
        return findById(issueId);
    }

    public int nextSortNo() {
        Integer sortNo = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_no), 0) + 10 FROM admin_issue_ticket WHERE is_deleted = 0",
                Integer.class
        );
        return sortNo == null ? 10 : sortNo;
    }

    public String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    public record IssueTicketRecord(
            String id,
            String issueType,
            String sourceRole,
            Long sourceUserId,
            String sourceName,
            String title,
            String content,
            String relatedType,
            String relatedId,
            int severityLevel,
            String ticketStatus,
            String resolutionNote,
            String handledBy,
            LocalDateTime handledAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int sortNo
    ) {
    }
}
