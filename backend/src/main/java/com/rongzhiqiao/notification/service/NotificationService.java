package com.rongzhiqiao.notification.service;

import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.notification.dto.SystemAnnouncementRequest;
import com.rongzhiqiao.notification.repository.NotificationMessageRecord;
import com.rongzhiqiao.notification.repository.NotificationRepository;
import com.rongzhiqiao.notification.repository.NotificationViewRecord;
import com.rongzhiqiao.notification.vo.NotificationResponse;
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
public class NotificationService {

    private static final String ROLE_ALL = "ALL";
    private static final String TYPE_SYSTEM = "SYSTEM";
    private static final Set<String> ALLOWED_TARGET_ROLES = Set.of(
            ROLE_ALL,
            "ROLE_JOBSEEKER",
            "ROLE_ENTERPRISE",
            "ROLE_SERVICE_ORG",
            "ROLE_ADMIN"
    );

    private final NotificationRepository notificationRepository;

    public List<NotificationResponse> listNotifications(Boolean read, String targetRole) {
        Long userId = SecurityUtils.getCurrentUserId();
        Set<String> authorities = SecurityUtils.getCurrentAuthorities();
        boolean adminView = authorities.contains("ROLE_ADMIN");
        String normalizedTargetRole = normalizeTargetRole(targetRole, false);

        if (normalizedTargetRole != null && !adminView) {
            throw new BusinessException(4003, "forbidden");
        }

        return notificationRepository.listNotifications(userId, authorities, read, normalizedTargetRole, adminView).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public boolean markAsRead(String notificationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Set<String> authorities = SecurityUtils.getCurrentAuthorities();
        NotificationMessageRecord notification = requireNotification(notificationId);
        if (!canAccess(notification.targetRole(), authorities)) {
            throw new BusinessException(4003, "forbidden");
        }
        notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());
        return true;
    }

    @Transactional
    public NotificationResponse publishSystemAnnouncement(SystemAnnouncementRequest request) {
        SecurityUtils.requireAuthority("ROLE_ADMIN");
        LocalDateTime now = LocalDateTime.now();
        NotificationMessageRecord notification = new NotificationMessageRecord(
                generateNotificationId(),
                TYPE_SYSTEM,
                normalizeText(request.getTitle()),
                normalizeText(request.getContent()),
                normalizeTargetRole(request.getTargetRole(), true),
                now,
                SecurityUtils.getCurrentUserId(),
                false,
                notificationRepository.nextSortNo()
        );
        notificationRepository.insert(notification);
        return new NotificationResponse(
                notification.id(),
                notification.type(),
                notification.title(),
                notification.content(),
                notification.targetRole(),
                notificationRepository.formatDateTime(notification.createdAt()),
                notification.defaultRead()
        );
    }

    private NotificationMessageRecord requireNotification(String notificationId) {
        NotificationMessageRecord notification = notificationRepository.findById(notificationId);
        if (notification == null) {
            throw new BusinessException(4004, "notification not found");
        }
        return notification;
    }

    private NotificationResponse toResponse(NotificationViewRecord notification) {
        return new NotificationResponse(
                notification.id(),
                notification.type(),
                notification.title(),
                notification.content(),
                notification.targetRole(),
                notificationRepository.formatDateTime(notification.createdAt()),
                notification.read()
        );
    }

    private boolean canAccess(String targetRole, Set<String> authorities) {
        if (ROLE_ALL.equals(targetRole) || authorities.contains("ROLE_ADMIN")) {
            return true;
        }
        return authorities.contains(targetRole);
    }

    private String normalizeTargetRole(String targetRole, boolean required) {
        if (targetRole == null || targetRole.isBlank()) {
            if (required) {
                throw new BusinessException(4001, "targetRole is invalid");
            }
            return null;
        }
        String normalized = targetRole.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_TARGET_ROLES.contains(normalized)) {
            throw new BusinessException(4001, "targetRole is invalid");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String generateNotificationId() {
        long timestamp = System.currentTimeMillis();
        int suffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "notice-" + Long.toString(timestamp, 36).toLowerCase(Locale.ROOT) + "-" + suffix;
    }
}
