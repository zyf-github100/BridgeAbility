package com.rongzhiqiao.audit.controller;

import com.rongzhiqiao.catalog.service.PlatformCatalogService;
import com.rongzhiqiao.common.api.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final PlatformCatalogService platformCatalogService;

    @GetMapping("/logs")
    public ApiResponse<List<String>> logs() {
        return ApiResponse.success(platformCatalogService.listAuditLogs());
    }
}
