package com.rongzhiqiao.knowledge.vo;

import java.util.List;

public record KnowledgeAdminArticleResponse(
        String id,
        String title,
        String category,
        String summary,
        String content,
        List<String> tags,
        String publishStatus,
        String publishedAt,
        String createdAt,
        String updatedAt
) {
}
