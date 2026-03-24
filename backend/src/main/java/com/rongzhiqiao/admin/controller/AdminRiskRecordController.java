package com.rongzhiqiao.admin.controller;

import com.rongzhiqiao.common.api.ApiResponse;
import com.rongzhiqiao.common.api.PageResponse;
import com.rongzhiqiao.serviceorg.dto.ServiceAlertStatusUpdateRequest;
import com.rongzhiqiao.serviceorg.service.ServiceOrgService;
import com.rongzhiqiao.serviceorg.vo.ServiceAlertResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/risk-records")
@RequiredArgsConstructor
public class AdminRiskRecordController {

    private final ServiceOrgService serviceOrgService;

    @GetMapping
    public ApiResponse<PageResponse<ServiceAlertResponse>> list(@RequestParam(required = false) String status,
                                                                @RequestParam(required = false) Integer level,
                                                                @RequestParam(required = false) Integer page,
                                                                @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(serviceOrgService.listAlerts(status, level, page, pageSize));
    }

    @PostMapping("/{alertId}/status")
    public ApiResponse<ServiceAlertResponse> updateStatus(@PathVariable String alertId,
                                                          @Valid @RequestBody ServiceAlertStatusUpdateRequest request) {
        return ApiResponse.success(serviceOrgService.updateAlertStatus(alertId, request));
    }
}
