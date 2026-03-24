package com.rongzhiqiao.jobseeker.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JobseekerServiceAuthorizationRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<ServiceAuthorizationRecord> listActiveByJobseekerUserId(Long jobseekerUserId) {
        return jdbcTemplate.query(
                """
                        SELECT a.jobseeker_user_id,
                               a.service_org_user_id,
                               a.profile_access_granted,
                               a.sensitive_access_granted,
                               a.granted_at,
                               su.account AS service_org_account,
                               su.nickname AS service_org_nickname
                        FROM jobseeker_service_authorization a
                        INNER JOIN sys_user su
                                ON su.id = a.service_org_user_id
                               AND su.is_deleted = 0
                        WHERE a.jobseeker_user_id = ?
                          AND a.is_deleted = 0
                          AND (a.profile_access_granted = 1 OR a.sensitive_access_granted = 1)
                        ORDER BY a.granted_at DESC, a.service_org_user_id ASC
                        """,
                this::mapRecord,
                jobseekerUserId
        );
    }

    public ServiceAuthorizationRecord findActiveByPair(Long jobseekerUserId, Long serviceOrgUserId) {
        List<ServiceAuthorizationRecord> list = jdbcTemplate.query(
                """
                        SELECT a.jobseeker_user_id,
                               a.service_org_user_id,
                               a.profile_access_granted,
                               a.sensitive_access_granted,
                               a.granted_at,
                               su.account AS service_org_account,
                               su.nickname AS service_org_nickname
                        FROM jobseeker_service_authorization a
                        INNER JOIN sys_user su
                                ON su.id = a.service_org_user_id
                               AND su.is_deleted = 0
                        WHERE a.jobseeker_user_id = ?
                          AND a.service_org_user_id = ?
                          AND a.is_deleted = 0
                        LIMIT 1
                        """,
                this::mapRecord,
                jobseekerUserId,
                serviceOrgUserId
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean hasProfileAccess(Long jobseekerUserId, Long serviceOrgUserId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM jobseeker_service_authorization
                        WHERE jobseeker_user_id = ?
                          AND service_org_user_id = ?
                          AND is_deleted = 0
                          AND profile_access_granted = 1
                        """,
                Integer.class,
                jobseekerUserId,
                serviceOrgUserId
        );
        return count != null && count > 0;
    }

    public boolean hasSensitiveAccess(Long jobseekerUserId, Long serviceOrgUserId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM jobseeker_service_authorization
                        WHERE jobseeker_user_id = ?
                          AND service_org_user_id = ?
                          AND is_deleted = 0
                          AND sensitive_access_granted = 1
                        """,
                Integer.class,
                jobseekerUserId,
                serviceOrgUserId
        );
        return count != null && count > 0;
    }

    public void save(Long jobseekerUserId,
                     Long serviceOrgUserId,
                     boolean profileAccessGranted,
                     boolean sensitiveAccessGranted,
                     LocalDateTime grantedAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO jobseeker_service_authorization (
                            jobseeker_user_id,
                            service_org_user_id,
                            profile_access_granted,
                            sensitive_access_granted,
                            granted_at,
                            revoked_at,
                            is_deleted
                        )
                        VALUES (?, ?, ?, ?, ?, NULL, 0)
                        ON DUPLICATE KEY UPDATE
                            profile_access_granted = VALUES(profile_access_granted),
                            sensitive_access_granted = VALUES(sensitive_access_granted),
                            granted_at = VALUES(granted_at),
                            revoked_at = NULL,
                            is_deleted = 0
                        """,
                jobseekerUserId,
                serviceOrgUserId,
                profileAccessGranted ? 1 : 0,
                sensitiveAccessGranted ? 1 : 0,
                Timestamp.valueOf(grantedAt)
        );
    }

    public void revoke(Long jobseekerUserId, Long serviceOrgUserId, LocalDateTime revokedAt) {
        jdbcTemplate.update(
                """
                        UPDATE jobseeker_service_authorization
                        SET profile_access_granted = 0,
                            sensitive_access_granted = 0,
                            revoked_at = ?,
                            is_deleted = 1
                        WHERE jobseeker_user_id = ?
                          AND service_org_user_id = ?
                        """,
                Timestamp.valueOf(revokedAt),
                jobseekerUserId,
                serviceOrgUserId
        );
    }

    private ServiceAuthorizationRecord mapRecord(ResultSet rs, int rowNum) throws SQLException {
        return new ServiceAuthorizationRecord(
                rs.getLong("jobseeker_user_id"),
                rs.getLong("service_org_user_id"),
                rs.getInt("profile_access_granted") == 1,
                rs.getInt("sensitive_access_granted") == 1,
                toLocalDateTime(rs.getTimestamp("granted_at")),
                rs.getString("service_org_account"),
                rs.getString("service_org_nickname")
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    public record ServiceAuthorizationRecord(
            Long jobseekerUserId,
            Long serviceOrgUserId,
            boolean profileAccessGranted,
            boolean sensitiveAccessGranted,
            LocalDateTime grantedAt,
            String serviceOrgAccount,
            String serviceOrgNickname
    ) {
    }
}
