package com.rongzhiqiao.issue.controller;

import com.rongzhiqiao.common.api.ApiResponse;
import com.rongzhiqiao.issue.dto.AdminIssueStatusUpdateRequest;
import com.rongzhiqiao.issue.service.IssueService;
import com.rongzhiqiao.issue.vo.AdminIssueResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/issues")
@RequiredArgsConstructor
public class AdminIssueController {

    private final IssueService issueService;

    @GetMapping
    public ApiResponse<List<AdminIssueResponse>> list(@RequestParam(required = false) String issueType,
                                                      @RequestParam(required = false) String status) {
        return ApiResponse.success(issueService.listIssues(issueType, status));
    }

    @PostMapping("/{issueId}/status")
    public ApiResponse<AdminIssueResponse> updateStatus(@PathVariable String issueId,
                                                        @Valid @RequestBody AdminIssueStatusUpdateRequest request) {
        return ApiResponse.success(issueService.updateIssueStatus(issueId, request));
    }
}
