package com.rongzhiqiao.knowledge.service;

import com.rongzhiqiao.catalog.vo.CatalogResponses.KnowledgeArticleResponse;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.knowledge.dto.KnowledgeArticleUpsertRequest;
import com.rongzhiqiao.knowledge.repository.KnowledgeArticleRecord;
import com.rongzhiqiao.knowledge.repository.KnowledgeRepository;
import com.rongzhiqiao.knowledge.vo.KnowledgeAdminArticleResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFFLINE = "OFFLINE";

    private final KnowledgeRepository knowledgeRepository;

    public List<KnowledgeArticleResponse> listPublishedArticles() {
        return knowledgeRepository.listPublishedArticles().stream()
                .map(this::toPublicResponse)
                .toList();
    }

    public List<KnowledgeAdminArticleResponse> listAdminArticles() {
        return knowledgeRepository.listAdminArticles().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public KnowledgeAdminArticleResponse createArticle(KnowledgeArticleUpsertRequest request) {
        LocalDateTime now = LocalDateTime.now();
        KnowledgeArticleRecord article = new KnowledgeArticleRecord(
                generateArticleId(),
                normalizeText(request.getTitle()),
                normalizeText(request.getCategory()),
                normalizeText(request.getSummary()),
                normalizeText(request.getContent()),
                normalizeTags(request.getTags()),
                STATUS_OFFLINE,
                null,
                now,
                now,
                knowledgeRepository.nextSortNo()
        );
        knowledgeRepository.insert(article);
        return toAdminResponse(article);
    }

    @Transactional
    public KnowledgeAdminArticleResponse updateArticle(String articleId, KnowledgeArticleUpsertRequest request) {
        KnowledgeArticleRecord existing = requireArticle(articleId);
        KnowledgeArticleRecord updated = new KnowledgeArticleRecord(
                existing.id(),
                normalizeText(request.getTitle()),
                normalizeText(request.getCategory()),
                normalizeText(request.getSummary()),
                normalizeText(request.getContent()),
                normalizeTags(request.getTags()),
                existing.publishStatus(),
                existing.publishedAt(),
                existing.createdAt(),
                LocalDateTime.now(),
                existing.sortNo()
        );
        knowledgeRepository.update(updated);
        return toAdminResponse(updated);
    }

    @Transactional
    public KnowledgeAdminArticleResponse publishArticle(String articleId) {
        KnowledgeArticleRecord existing = requireArticle(articleId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime publishedAt = existing.publishedAt() == null ? now : existing.publishedAt();
        knowledgeRepository.updatePublishStatus(articleId, STATUS_PUBLISHED, publishedAt, now);
        return toAdminResponse(requireArticle(articleId));
    }

    @Transactional
    public KnowledgeAdminArticleResponse offlineArticle(String articleId) {
        requireArticle(articleId);
        knowledgeRepository.updatePublishStatus(articleId, STATUS_OFFLINE, null, LocalDateTime.now());
        return toAdminResponse(requireArticle(articleId));
    }

    private KnowledgeArticleRecord requireArticle(String articleId) {
        KnowledgeArticleRecord article = knowledgeRepository.findById(articleId);
        if (article == null) {
            throw new BusinessException(4004, "knowledge article not found");
        }
        return article;
    }

    private KnowledgeArticleResponse toPublicResponse(KnowledgeArticleRecord article) {
        return new KnowledgeArticleResponse(
                article.id(),
                article.title(),
                article.category(),
                article.summary(),
                article.tags(),
                article.publishedAt() == null ? "" : article.publishedAt().toLocalDate().toString()
        );
    }

    private KnowledgeAdminArticleResponse toAdminResponse(KnowledgeArticleRecord article) {
        return new KnowledgeAdminArticleResponse(
                article.id(),
                article.title(),
                article.category(),
                article.summary(),
                article.content(),
                article.tags(),
                article.publishStatus(),
                knowledgeRepository.formatDateTime(article.publishedAt()),
                knowledgeRepository.formatDateTime(article.createdAt()),
                knowledgeRepository.formatDateTime(article.updatedAt())
        );
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                .map(this::normalizeText)
                .filter(value -> !value.isEmpty())
                .distinct()
                .limit(20)
                .toList();
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String generateArticleId() {
        long timestamp = System.currentTimeMillis();
        int suffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "knowledge-" + Long.toString(timestamp, 36).toLowerCase(Locale.ROOT) + "-" + suffix;
    }
}
