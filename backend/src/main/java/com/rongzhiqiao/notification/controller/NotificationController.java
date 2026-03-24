package com.rongzhiqiao.notification.controller;

import com.rongzhiqiao.common.api.ApiResponse;
import com.rongzhiqiao.notification.service.NotificationService;
import com.rongzhiqiao.notification.vo.NotificationResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(@RequestParam(required = false) Boolean read,
                                                        @RequestParam(required = false) String targetRole) {
        return ApiResponse.success(notificationService.listNotifications(read, targetRole));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<Boolean> markAsRead(@PathVariable String notificationId) {
        return ApiResponse.success(notificationService.markAsRead(notificationId));
    }
}
