package com.rongzhiqiao.notification.controller;

import com.rongzhiqiao.common.api.ApiResponse;
import com.rongzhiqiao.notification.dto.SystemAnnouncementRequest;
import com.rongzhiqiao.notification.service.NotificationService;
import com.rongzhiqiao.notification.vo.NotificationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ApiResponse<NotificationResponse> publish(@Valid @RequestBody SystemAnnouncementRequest request) {
        return ApiResponse.success(notificationService.publishSystemAnnouncement(request));
    }
}
