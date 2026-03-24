package com.rongzhiqiao.notification.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public List<NotificationViewRecord> listNotifications(Long userId,
                                                          Set<String> currentRoles,
                                                          Boolean read,
                                                          String targetRole,
                                                          boolean adminView) {
        List<Object> args = new ArrayList<>();
        args.add(userId);

        String readExpression = "CASE WHEN message.read_flag = 1 OR read_log.notification_id IS NOT NULL THEN 1 ELSE 0 END";
        StringBuilder sql = new StringBuilder(
                """
                        SELECT message.id,
                               message.type,
                               message.title,
                               message.content,
                               message.target_role,
                               message.created_at,
                               """
                        + readExpression
                        + """
                               AS effective_read
                        FROM notification_message message
                        LEFT JOIN notification_read_log read_log
                          ON read_log.notification_id = message.id
                         AND read_log.user_id = ?
                        WHERE message.is_deleted = 0
                        """
        );

        appendRoleFilter(sql, args, currentRoles, targetRole, adminView);
        if (read != null) {
            sql.append(" AND ").append(readExpression).append(" = ?");
            args.add(Boolean.TRUE.equals(read) ? 1 : 0);
        }
        sql.append(" ORDER BY message.created_at DESC, message.sort_no ASC, message.id ASC");

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new NotificationViewRecord(
                        rs.getString("id"),
                        rs.getString("type"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("target_role"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        rs.getInt("effective_read") == 1
                ),
                args.toArray()
        );
    }

    public NotificationMessageRecord findById(String notificationId) {
        List<NotificationMessageRecord> list = jdbcTemplate.query(
                """
                        SELECT id,
                               type,
                               title,
                               content,
                               target_role,
                               created_at,
                               created_by_user_id,
                               read_flag,
                               sort_no
                        FROM notification_message
                        WHERE id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                (rs, rowNum) -> new NotificationMessageRecord(
                        rs.getString("id"),
                        rs.getString("type"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("target_role"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        rs.getObject("created_by_user_id") == null ? null : rs.getLong("created_by_user_id"),
                        rs.getInt("read_flag") == 1,
                        rs.getInt("sort_no")
                ),
                notificationId
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public void markAsRead(String notificationId, Long userId, LocalDateTime readAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO notification_read_log (
                            notification_id,
                            user_id,
                            read_at
                        ) VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE read_at = VALUES(read_at)
                        """,
                notificationId,
                userId,
                Timestamp.valueOf(readAt)
        );
    }

    public void insert(NotificationMessageRecord notification) {
        jdbcTemplate.update(
                """
                        INSERT INTO notification_message (
                            id,
                            type,
                            title,
                            content,
                            target_role,
                            created_at,
                            created_by_user_id,
                            read_flag,
                            sort_no,
                            is_deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                notification.id(),
                notification.type(),
                notification.title(),
                notification.content(),
                notification.targetRole(),
                Timestamp.valueOf(notification.createdAt()),
                notification.createdByUserId(),
                notification.defaultRead() ? 1 : 0,
                notification.sortNo()
        );
    }

    public int nextSortNo() {
        Integer sortNo = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_no), 0) + 10 FROM notification_message WHERE is_deleted = 0",
                Integer.class
        );
        return sortNo == null ? 10 : sortNo;
    }

    public String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    private void appendRoleFilter(StringBuilder sql,
                                  List<Object> args,
                                  Set<String> currentRoles,
                                  String targetRole,
                                  boolean adminView) {
        if (adminView) {
            if (targetRole != null && !targetRole.isBlank()) {
                sql.append(" AND message.target_role = ?");
                args.add(targetRole);
            }
            return;
        }

        List<String> roles = currentRoles.stream()
                .filter(role -> role != null && role.startsWith("ROLE_"))
                .collect(Collectors.toList());

        sql.append(" AND (message.target_role = 'ALL'");
        if (!roles.isEmpty()) {
            sql.append(" OR message.target_role IN (");
            for (int index = 0; index < roles.size(); index++) {
                if (index > 0) {
                    sql.append(", ");
                }
                sql.append("?");
                args.add(roles.get(index));
            }
            sql.append(")");
        }
        sql.append(")");
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
