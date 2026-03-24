package com.rongzhiqiao.admin.service;

import com.rongzhiqiao.admin.dto.TagDictionaryUpsertRequest;
import com.rongzhiqiao.admin.repository.TagDictionaryRepository;
import com.rongzhiqiao.admin.repository.TagDictionaryRepository.TagDictionaryRecord;
import com.rongzhiqiao.admin.vo.TagDictionaryResponse;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagDictionaryService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "INACTIVE");

    private final TagDictionaryRepository tagDictionaryRepository;

    public List<TagDictionaryResponse> listTags() {
        SecurityUtils.requireAuthority("ROLE_ADMIN");
        return tagDictionaryRepository.listTags().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TagDictionaryResponse createTag(TagDictionaryUpsertRequest request) {
        SecurityUtils.requireAuthority("ROLE_ADMIN");
        String tagCode = normalizeRequiredText(request.getTagCode());
        if (tagDictionaryRepository.findByCode(tagCode) != null) {
            throw new BusinessException(4001, "tagCode already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        TagDictionaryRecord saved = tagDictionaryRepository.insert(new TagDictionaryRecord(
                null,
                tagCode,
                normalizeRequiredText(request.getTagName()),
                normalizeRequiredText(request.getTagCategory()).toUpperCase(Locale.ROOT),
                normalizeStatus(request.getTagStatus()),
                normalizeOptionalText(request.getDescription()),
                now,
                now,
                tagDictionaryRepository.nextSortNo()
        ));
        return toResponse(saved);
    }

    @Transactional
    public TagDictionaryResponse updateTag(Long tagId, TagDictionaryUpsertRequest request) {
        SecurityUtils.requireAuthority("ROLE_ADMIN");
        TagDictionaryRecord existing = requireTag(tagId);
        String tagCode = normalizeRequiredText(request.getTagCode());
        TagDictionaryRecord duplicated = tagDictionaryRepository.findByCode(tagCode);
        if (duplicated != null && !duplicated.id().equals(tagId)) {
            throw new BusinessException(4001, "tagCode already exists");
        }

        TagDictionaryRecord saved = tagDictionaryRepository.update(new TagDictionaryRecord(
                existing.id(),
                tagCode,
                normalizeRequiredText(request.getTagName()),
                normalizeRequiredText(request.getTagCategory()).toUpperCase(Locale.ROOT),
                normalizeStatus(request.getTagStatus()),
                normalizeOptionalText(request.getDescription()),
                existing.createdAt(),
                LocalDateTime.now(),
                existing.sortNo()
        ));
        return toResponse(saved);
    }

    private TagDictionaryRecord requireTag(Long tagId) {
        TagDictionaryRecord record = tagDictionaryRepository.findById(tagId);
        if (record == null) {
            throw new BusinessException(4004, "tag not found");
        }
        return record;
    }

    private TagDictionaryResponse toResponse(TagDictionaryRecord record) {
        return new TagDictionaryResponse(
                record.id(),
                record.tagCode(),
                record.tagName(),
                record.tagCategory(),
                record.tagStatus(),
                record.description(),
                tagDictionaryRepository.formatDateTime(record.createdAt()),
                tagDictionaryRepository.formatDateTime(record.updatedAt())
        );
    }

    private String normalizeStatus(String value) {
        String normalized = normalizeRequiredText(value).toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new BusinessException(4001, "tagStatus is invalid");
        }
        return normalized;
    }

    private String normalizeRequiredText(String value) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new BusinessException(4001, "text is invalid");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
