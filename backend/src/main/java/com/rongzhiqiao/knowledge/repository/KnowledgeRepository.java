package com.rongzhiqiao.knowledge.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rongzhiqiao.common.exception.BusinessException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KnowledgeRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public List<KnowledgeArticleRecord> listPublishedArticles() {
        return jdbcTemplate.query(
                """
                        SELECT id,
                               title,
                               category,
                               summary,
                               content,
                               tags,
                               publish_status,
                               published_at,
                               created_at,
                               updated_at,
                               sort_no
                        FROM knowledge_article
                        WHERE is_deleted = 0
                          AND publish_status = 'PUBLISHED'
                        ORDER BY published_at DESC, sort_no ASC, id ASC
                        """,
                (rs, rowNum) -> mapRecord(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("category"),
                        rs.getString("summary"),
                        rs.getString("content"),
                        rs.getString("tags"),
                        rs.getString("publish_status"),
                        toLocalDateTime(rs.getTimestamp("published_at")),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        rs.getInt("sort_no")
                )
        );
    }

    public List<KnowledgeArticleRecord> listAdminArticles() {
        return jdbcTemplate.query(
                """
                        SELECT id,
                               title,
                               category,
                               summary,
                               content,
                               tags,
                               publish_status,
                               published_at,
                               created_at,
                               updated_at,
                               sort_no
                        FROM knowledge_article
                        WHERE is_deleted = 0
                        ORDER BY CASE publish_status WHEN 'PUBLISHED' THEN 0 ELSE 1 END,
                                 updated_at DESC,
                                 sort_no ASC,
                                 id ASC
                        """,
                (rs, rowNum) -> mapRecord(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("category"),
                        rs.getString("summary"),
                        rs.getString("content"),
                        rs.getString("tags"),
                        rs.getString("publish_status"),
                        toLocalDateTime(rs.getTimestamp("published_at")),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        rs.getInt("sort_no")
                )
        );
    }

    public KnowledgeArticleRecord findById(String articleId) {
        List<KnowledgeArticleRecord> list = jdbcTemplate.query(
                """
                        SELECT id,
                               title,
                               category,
                               summary,
                               content,
                               tags,
                               publish_status,
                               published_at,
                               created_at,
                               updated_at,
                               sort_no
                        FROM knowledge_article
                        WHERE id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                (rs, rowNum) -> mapRecord(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("category"),
                        rs.getString("summary"),
                        rs.getString("content"),
                        rs.getString("tags"),
                        rs.getString("publish_status"),
                        toLocalDateTime(rs.getTimestamp("published_at")),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        rs.getInt("sort_no")
                ),
                articleId
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public void insert(KnowledgeArticleRecord article) {
        jdbcTemplate.update(
                """
                        INSERT INTO knowledge_article (
                            id,
                            title,
                            category,
                            summary,
                            content,
                            tags,
                            publish_status,
                            published_at,
                            created_at,
                            updated_at,
                            sort_no,
                            is_deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                article.id(),
                article.title(),
                article.category(),
                article.summary(),
                article.content(),
                writeStringList(article.tags()),
                article.publishStatus(),
                toTimestamp(article.publishedAt()),
                toTimestamp(article.createdAt()),
                toTimestamp(article.updatedAt()),
                article.sortNo()
        );
    }

    public void update(KnowledgeArticleRecord article) {
        jdbcTemplate.update(
                """
                        UPDATE knowledge_article
                        SET title = ?,
                            category = ?,
                            summary = ?,
                            content = ?,
                            tags = ?,
                            publish_status = ?,
                            published_at = ?,
                            updated_at = ?,
                            sort_no = ?
                        WHERE id = ?
                          AND is_deleted = 0
                        """,
                article.title(),
                article.category(),
                article.summary(),
                article.content(),
                writeStringList(article.tags()),
                article.publishStatus(),
                toTimestamp(article.publishedAt()),
                toTimestamp(article.updatedAt()),
                article.sortNo(),
                article.id()
        );
    }

    public void updatePublishStatus(String articleId, String publishStatus, LocalDateTime publishedAt, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                """
                        UPDATE knowledge_article
                        SET publish_status = ?,
                            published_at = ?,
                            updated_at = ?
                        WHERE id = ?
                          AND is_deleted = 0
                        """,
                publishStatus,
                toTimestamp(publishedAt),
                toTimestamp(updatedAt),
                articleId
        );
    }

    public int nextSortNo() {
        Integer sortNo = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_no), 0) + 10 FROM knowledge_article WHERE is_deleted = 0",
                Integer.class
        );
        return sortNo == null ? 10 : sortNo;
    }

    public String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    private KnowledgeArticleRecord mapRecord(String id,
                                             String title,
                                             String category,
                                             String summary,
                                             String content,
                                             String tags,
                                             String publishStatus,
                                             LocalDateTime publishedAt,
                                             LocalDateTime createdAt,
                                             LocalDateTime updatedAt,
                                             int sortNo) {
        return new KnowledgeArticleRecord(
                id,
                title,
                category,
                summary,
                content,
                readStringList(tags),
                publishStatus,
                publishedAt,
                createdAt,
                updatedAt,
                sortNo
        );
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> tags = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            return tags == null ? List.of() : List.copyOf(tags);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse knowledge tags", exception);
        }
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(5000, "knowledge tags write failed");
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
