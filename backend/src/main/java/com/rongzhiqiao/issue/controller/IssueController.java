package com.rongzhiqiao.issue.controller;

import com.rongzhiqiao.common.api.ApiResponse;
import com.rongzhiqiao.issue.dto.IssueReportRequest;
import com.rongzhiqiao.issue.service.IssueService;
import com.rongzhiqiao.issue.vo.AdminIssueResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @PostMapping("/report")
    public ApiResponse<AdminIssueResponse> report(@Valid @RequestBody IssueReportRequest request) {
        return ApiResponse.success(issueService.reportIssue(request));
    }
}
