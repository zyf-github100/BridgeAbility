package com.rongzhiqiao.issue.service;

import com.rongzhiqiao.auth.entity.SysUser;
import com.rongzhiqiao.auth.mapper.SysUserMapper;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.issue.dto.AdminIssueStatusUpdateRequest;
import com.rongzhiqiao.issue.dto.IssueReportRequest;
import com.rongzhiqiao.issue.repository.IssueRepository;
import com.rongzhiqiao.issue.repository.IssueRepository.IssueTicketRecord;
import com.rongzhiqiao.issue.vo.AdminIssueResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueService {

    private static final List<String> ROLE_PRIORITY = List.of(
            "ROLE_JOBSEEKER",
            "ROLE_ENTERPRISE",
            "ROLE_SERVICE_ORG",
            "ROLE_ADMIN"
    );
    private static final Set<String> ALLOWED_TYPES = Set.of("APPEAL", "DATA_CORRECTION");
    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDING", "IN_PROGRESS", "RESOLVED", "REJECTED");

    private final IssueRepository issueRepository;
    private final SysUserMapper sysUserMapper;

    public List<AdminIssueResponse> listIssues(String issueType, String status) {
        SecurityUtils.requireAuthority("ROLE_ADMIN");
        return issueRepository.listIssues(normalizeIssueType(issueType, false), normalizeStatus(status, false)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminIssueResponse reportIssue(IssueReportRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Set<String> authorities = SecurityUtils.getCurrentAuthorities();
        LocalDateTime now = LocalDateTime.now();

        IssueTicketRecord saved = issueRepository.insert(new IssueTicketRecord(
                generateIssueId(),
                normalizeIssueType(request.getIssueType(), true),
                resolveSourceRole(authorities),
                userId,
                resolveSourceName(userId),
                normalizeRequiredText(request.getTitle()),
                normalizeRequiredText(request.getContent()),
                normalizeOptionalText(request.getRelatedType()),
                normalizeOptionalText(request.getRelatedId()),
                normalizeSeverityLevel(request.getSeverityLevel()),
                "PENDING",
                null,
                null,
                null,
                now,
                now,
                issueRepository.nextSortNo()
        ));
        return toResponse(saved);
    }

    @Transactional
    public AdminIssueResponse updateIssueStatus(String issueId, AdminIssueStatusUpdateRequest request) {
        SecurityUtils.requireAuthority("ROLE_ADMIN");
        requireIssue(issueId);
        IssueTicketRecord saved = issueRepository.updateStatus(
                issueId,
                normalizeStatus(request.getTargetStatus(), true),
                normalizeRequiredText(request.getResolutionNote()),
                normalizeRequiredText(request.getHandlerName()),
                LocalDateTime.now()
        );
        return toResponse(saved);
    }

    private IssueTicketRecord requireIssue(String issueId) {
        IssueTicketRecord record = issueRepository.findById(issueId);
        if (record == null) {
            throw new BusinessException(4004, "issue not found");
        }
        return record;
    }

    private AdminIssueResponse toResponse(IssueTicketRecord record) {
        return new AdminIssueResponse(
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
                issueRepository.formatDateTime(record.handledAt()),
                issueRepository.formatDateTime(record.createdAt())
        );
    }

    private String resolveSourceRole(Set<String> authorities) {
        for (String role : ROLE_PRIORITY) {
            if (authorities.contains(role)) {
                return role;
            }
        }
        throw new BusinessException(4003, "forbidden");
    }

    private String resolveSourceName(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return "unknown";
        }
        String nickname = normalizeOptionalText(user.getNickname());
        if (nickname != null) {
            return nickname;
        }
        String account = normalizeOptionalText(user.getAccount());
        return account == null ? "unknown" : account;
    }

    private int normalizeSeverityLevel(Integer value) {
        if (value == null) {
            return 1;
        }
        if (value < 1 || value > 3) {
            throw new BusinessException(4001, "severityLevel is invalid");
        }
        return value;
    }

    private String normalizeIssueType(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                throw new BusinessException(4001, "issueType is invalid");
            }
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(normalized)) {
            throw new BusinessException(4001, "issueType is invalid");
        }
        return normalized;
    }

    private String normalizeStatus(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                throw new BusinessException(4001, "targetStatus is invalid");
            }
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new BusinessException(4001, "targetStatus is invalid");
        }
        return normalized;
    }

    private String normalizeRequiredText(String value) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new BusinessException(4001, "text is invalid");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String generateIssueId() {
        long timestamp = System.currentTimeMillis();
        int suffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "issue-" + Long.toString(timestamp, 36).toLowerCase(Locale.ROOT) + "-" + suffix;
    }
}
