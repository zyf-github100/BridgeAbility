package com.rongzhiqiao.knowledge.controller;

import com.rongzhiqiao.common.api.ApiResponse;
import com.rongzhiqiao.knowledge.dto.KnowledgeArticleUpsertRequest;
import com.rongzhiqiao.knowledge.service.KnowledgeService;
import com.rongzhiqiao.knowledge.vo.KnowledgeAdminArticleResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/knowledge/articles")
@RequiredArgsConstructor
public class AdminKnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping
    public ApiResponse<List<KnowledgeAdminArticleResponse>> list() {
        return ApiResponse.success(knowledgeService.listAdminArticles());
    }

    @PostMapping
    public ApiResponse<KnowledgeAdminArticleResponse> create(@Valid @RequestBody KnowledgeArticleUpsertRequest request) {
        return ApiResponse.success(knowledgeService.createArticle(request));
    }

    @PutMapping("/{articleId}")
    public ApiResponse<KnowledgeAdminArticleResponse> update(@PathVariable String articleId,
                                                             @Valid @RequestBody KnowledgeArticleUpsertRequest request) {
        return ApiResponse.success(knowledgeService.updateArticle(articleId, request));
    }

    @PostMapping("/{articleId}/publish")
    public ApiResponse<KnowledgeAdminArticleResponse> publish(@PathVariable String articleId) {
        return ApiResponse.success(knowledgeService.publishArticle(articleId));
    }

    @PostMapping("/{articleId}/offline")
    public ApiResponse<KnowledgeAdminArticleResponse> offline(@PathVariable String articleId) {
        return ApiResponse.success(knowledgeService.offlineArticle(articleId));
    }
}
