package com.rongzhiqiao.enterprise.repository;

import com.rongzhiqiao.common.exception.BusinessException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnterpriseInterviewRepository {

    private final JdbcTemplate jdbcTemplate;

    public long createInterviewRecord(Long applicationId,
                                      LocalDateTime interviewTime,
                                      String interviewMode,
                                      String interviewerName,
                                      String inviteNote) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO interview_record (
                                application_id,
                                interview_time,
                                interview_mode,
                                interviewer_name,
                                invite_note,
                                result_status,
                                is_deleted
                            ) VALUES (?, ?, ?, ?, ?, 'PENDING', 0)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, applicationId);
            statement.setTimestamp(2, Timestamp.valueOf(interviewTime));
            statement.setString(3, interviewMode);
            statement.setString(4, interviewerName);
            statement.setString(5, inviteNote);
            return statement;
        }, keyHolder);

        if (keyHolder.getKey() == null) {
            throw new IllegalStateException("Failed to create interview record");
        }
        return keyHolder.getKey().longValue();
    }

    public void updatePendingInterviewRecord(long interviewId,
                                             LocalDateTime interviewTime,
                                             String interviewMode,
                                             String interviewerName,
                                             String inviteNote) {
        int updated = jdbcTemplate.update(
                """
                        UPDATE interview_record
                        SET interview_time = ?,
                            interview_mode = ?,
                            interviewer_name = ?,
                            invite_note = ?
                        WHERE id = ?
                          AND is_deleted = 0
                        """,
                Timestamp.valueOf(interviewTime),
                interviewMode,
                interviewerName,
                inviteNote,
                interviewId
        );
        if (updated == 0) {
            throw new BusinessException(4004, "interview record not found");
        }
    }

    public void updateInterviewResult(long interviewId,
                                      String resultStatus,
                                      String feedbackNote,
                                      String rejectReason) {
        int updated = jdbcTemplate.update(
                """
                        UPDATE interview_record
                        SET result_status = ?,
                            feedback_note = ?,
                            reject_reason = ?
                        WHERE id = ?
                          AND is_deleted = 0
                        """,
                resultStatus,
                feedbackNote,
                rejectReason,
                interviewId
        );
        if (updated == 0) {
            throw new BusinessException(4004, "interview record not found");
        }
    }

    public void updateApplicationStatus(Long applicationId, String status) {
        int updated = jdbcTemplate.update(
                """
                        UPDATE job_application
                        SET status = ?
                        WHERE id = ?
                          AND is_deleted = 0
                        """,
                status,
                applicationId
        );
        if (updated == 0) {
            throw new BusinessException(4004, "candidate application not found");
        }
    }
}
