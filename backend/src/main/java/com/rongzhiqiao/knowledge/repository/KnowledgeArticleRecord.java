package com.rongzhiqiao.knowledge.repository;

import java.time.LocalDateTime;
import java.util.List;

public record KnowledgeArticleRecord(
        String id,
        String title,
        String category,
        String summary,
        String content,
        List<String> tags,
        String publishStatus,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int sortNo
) {
}
