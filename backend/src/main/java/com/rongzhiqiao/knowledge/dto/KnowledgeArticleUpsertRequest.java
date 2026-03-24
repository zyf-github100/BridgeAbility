package com.rongzhiqiao.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class KnowledgeArticleUpsertRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 64)
    private String category;

    @NotBlank
    @Size(max = 500)
    private String summary;

    @NotBlank
    private String content;

    @Size(max = 20)
    private List<@NotBlank @Size(max = 32) String> tags;
}
