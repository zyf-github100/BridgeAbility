package com.rongzhiqiao.knowledge.controller;

import com.rongzhiqiao.catalog.vo.CatalogResponses.KnowledgeArticleResponse;
import com.rongzhiqiao.common.api.ApiResponse;
import com.rongzhiqiao.knowledge.service.KnowledgeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping("/articles")
    public ApiResponse<List<KnowledgeArticleResponse>> articles() {
        return ApiResponse.success(knowledgeService.listPublishedArticles());
    }
}
