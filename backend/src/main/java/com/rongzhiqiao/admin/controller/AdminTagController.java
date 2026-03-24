package com.rongzhiqiao.admin.controller;

import com.rongzhiqiao.admin.dto.TagDictionaryUpsertRequest;
import com.rongzhiqiao.admin.service.TagDictionaryService;
import com.rongzhiqiao.admin.vo.TagDictionaryResponse;
import com.rongzhiqiao.common.api.ApiResponse;
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
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
public class AdminTagController {

    private final TagDictionaryService tagDictionaryService;

    @GetMapping
    public ApiResponse<List<TagDictionaryResponse>> list() {
        return ApiResponse.success(tagDictionaryService.listTags());
    }

    @PostMapping
    public ApiResponse<TagDictionaryResponse> create(@Valid @RequestBody TagDictionaryUpsertRequest request) {
        return ApiResponse.success(tagDictionaryService.createTag(request));
    }

    @PutMapping("/{tagId}")
    public ApiResponse<TagDictionaryResponse> update(@PathVariable Long tagId,
                                                     @Valid @RequestBody TagDictionaryUpsertRequest request) {
        return ApiResponse.success(tagDictionaryService.updateTag(tagId, request));
    }
}
