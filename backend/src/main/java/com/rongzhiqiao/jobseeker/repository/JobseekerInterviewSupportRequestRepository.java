package com.rongzhiqiao.jobseeker.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JobseekerInterviewSupportRequestRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public InterviewSupportRequestRecord insert(long applicationId,
                                                String requestType,
                                                String requestContent,
                                                String requestStatus,
                                                LocalDateTime createdAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO interview_support_request (
                                application_id,
                                request_type,
                                request_content,
                                request_status,
                                created_at,
                                updated_at,
                                is_deleted
                            ) VALUES (?, ?, ?, ?, ?, ?, 0)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, applicationId);
            statement.setString(2, requestType);
            statement.setString(3, requestContent);
            statement.setString(4, requestStatus);
            statement.setTimestamp(5, Timestamp.valueOf(createdAt));
            statement.setTimestamp(6, Timestamp.valueOf(createdAt));
            return statement;
        }, keyHolder);

        long requestId = keyHolder.getKey() == null ? 0L : keyHolder.getKey().longValue();
        return findById(requestId);
    }

    public InterviewSupportRequestRecord findById(long id) {
        List<InterviewSupportRequestRecord> records = jdbcTemplate.query(
                """
                        SELECT id,
                               application_id,
                               request_type,
                               request_content,
                               request_status,
                               created_at,
                               updated_at
                        FROM interview_support_request
                        WHERE id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                this::mapRecord,
                id
        );
        return records.isEmpty() ? null : records.get(0);
    }

    public List<InterviewSupportRequestRecord> listByUserId(long userId) {
        return jdbcTemplate.query(
                """
                        SELECT request.id,
                               request.application_id,
                               request.request_type,
                               request.request_content,
                               request.request_status,
                               request.created_at,
                               request.updated_at
                        FROM interview_support_request request
                        INNER JOIN job_application application
                                ON application.id = request.application_id
                               AND application.is_deleted = 0
                        WHERE application.user_id = ?
                          AND request.is_deleted = 0
                        ORDER BY request.updated_at DESC, request.id DESC
                        """,
                this::mapRecord,
                userId
        );
    }

    private InterviewSupportRequestRecord mapRecord(ResultSet rs, int rowNum) throws SQLException {
        return new InterviewSupportRequestRecord(
                rs.getLong("id"),
                rs.getLong("application_id"),
                rs.getString("request_type"),
                rs.getString("request_content"),
                rs.getString("request_status"),
                formatDateTime(rs.getTimestamp("created_at")),
                formatDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private String formatDateTime(Timestamp value) {
        return value == null ? "" : value.toLocalDateTime().format(DATE_TIME_FORMATTER);
    }

    public record InterviewSupportRequestRecord(
            long id,
            long applicationId,
            String requestType,
            String requestContent,
            String requestStatus,
            String createdAt,
            String updatedAt
    ) {
    }
}
