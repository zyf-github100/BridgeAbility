package com.rongzhiqiao.matching.controller;

import com.rongzhiqiao.catalog.service.PlatformCatalogService;
import com.rongzhiqiao.catalog.vo.CatalogResponses.MatchingStatusResponse;
import com.rongzhiqiao.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final PlatformCatalogService platformCatalogService;

    @GetMapping("/status")
    public ApiResponse<MatchingStatusResponse> status() {
        return ApiResponse.success(platformCatalogService.getMatchingStatus());
    }
}
