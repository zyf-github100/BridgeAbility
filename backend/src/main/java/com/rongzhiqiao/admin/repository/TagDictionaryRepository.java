package com.rongzhiqiao.admin.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TagDictionaryRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public List<TagDictionaryRecord> listTags() {
        return jdbcTemplate.query(
                """
                        SELECT id,
                               tag_code,
                               tag_name,
                               tag_category,
                               tag_status,
                               description,
                               created_at,
                               updated_at,
                               sort_no
                        FROM tag_dictionary
                        WHERE is_deleted = 0
                        ORDER BY sort_no ASC, id ASC
                        """,
                (rs, rowNum) -> new TagDictionaryRecord(
                        rs.getLong("id"),
                        rs.getString("tag_code"),
                        rs.getString("tag_name"),
                        rs.getString("tag_category"),
                        rs.getString("tag_status"),
                        rs.getString("description"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        rs.getInt("sort_no")
                )
        );
    }

    public TagDictionaryRecord findById(Long tagId) {
        List<TagDictionaryRecord> list = jdbcTemplate.query(
                """
                        SELECT id,
                               tag_code,
                               tag_name,
                               tag_category,
                               tag_status,
                               description,
                               created_at,
                               updated_at,
                               sort_no
                        FROM tag_dictionary
                        WHERE id = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                (rs, rowNum) -> new TagDictionaryRecord(
                        rs.getLong("id"),
                        rs.getString("tag_code"),
                        rs.getString("tag_name"),
                        rs.getString("tag_category"),
                        rs.getString("tag_status"),
                        rs.getString("description"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        rs.getInt("sort_no")
                ),
                tagId
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public TagDictionaryRecord findByCode(String tagCode) {
        List<TagDictionaryRecord> list = jdbcTemplate.query(
                """
                        SELECT id,
                               tag_code,
                               tag_name,
                               tag_category,
                               tag_status,
                               description,
                               created_at,
                               updated_at,
                               sort_no
                        FROM tag_dictionary
                        WHERE tag_code = ?
                          AND is_deleted = 0
                        LIMIT 1
                        """,
                (rs, rowNum) -> new TagDictionaryRecord(
                        rs.getLong("id"),
                        rs.getString("tag_code"),
                        rs.getString("tag_name"),
                        rs.getString("tag_category"),
                        rs.getString("tag_status"),
                        rs.getString("description"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        rs.getInt("sort_no")
                ),
                tagCode
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public TagDictionaryRecord insert(TagDictionaryRecord record) {
        jdbcTemplate.update(
                """
                        INSERT INTO tag_dictionary (
                            tag_code,
                            tag_name,
                            tag_category,
                            tag_status,
                            description,
                            sort_no,
                            is_deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, 0)
                        """,
                record.tagCode(),
                record.tagName(),
                record.tagCategory(),
                record.tagStatus(),
                record.description(),
                record.sortNo()
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return findById(id);
    }

    public TagDictionaryRecord update(TagDictionaryRecord record) {
        jdbcTemplate.update(
                """
                        UPDATE tag_dictionary
                        SET tag_code = ?,
                            tag_name = ?,
                            tag_category = ?,
                            tag_status = ?,
                            description = ?,
                            updated_at = ?
                        WHERE id = ?
                          AND is_deleted = 0
                        """,
                record.tagCode(),
                record.tagName(),
                record.tagCategory(),
                record.tagStatus(),
                record.description(),
                Timestamp.valueOf(record.updatedAt()),
                record.id()
        );
        return findById(record.id());
    }

    public int nextSortNo() {
        Integer sortNo = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_no), 0) + 10 FROM tag_dictionary WHERE is_deleted = 0",
                Integer.class
        );
        return sortNo == null ? 10 : sortNo;
    }

    public String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    public record TagDictionaryRecord(
            Long id,
            String tagCode,
            String tagName,
            String tagCategory,
            String tagStatus,
            String description,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int sortNo
    ) {
    }
}
