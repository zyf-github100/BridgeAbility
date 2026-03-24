package com.rongzhiqiao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.storage.provider=local")
@AutoConfigureMockMvc
class PersonalizedJobRecommendationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Set<String> accounts = new java.util.LinkedHashSet<>();

    @AfterEach
    void cleanup() {
        for (String account : accounts) {
            Long userId = jdbcTemplate.query(
                    "SELECT id FROM sys_user WHERE account = ?",
                    rs -> rs.next() ? rs.getLong("id") : null,
                    account
            );
            if (userId != null) {
                jdbcTemplate.update("DELETE FROM job_application WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM jobseeker_project WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM jobseeker_skill WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM jobseeker_support_need WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM jobseeker_profile WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", userId);
            }
        }
        accounts.clear();
    }

    @Test
    void recommendationOrderReflectsJobseekerProfileAndSupportNeeds() throws Exception {
        String token = registerAndGetToken();

        Map<String, Object> profileRequest = new HashMap<>();
        profileRequest.put("realName", "测试候选人");
        profileRequest.put("major", "数字媒体技术");
        profileRequest.put("targetCity", "远程");
        profileRequest.put("expectedJob", "内容无障碍编辑");
        profileRequest.put("workModePreference", "REMOTE");
        profileRequest.put("skillTags", java.util.List.of(
                java.util.Map.of("skillCode", "content", "skillName", "内容", "skillLevel", 4),
                java.util.Map.of("skillCode", "subtitle", "skillName", "字幕", "skillLevel", 5)
        ));

        mockMvc.perform(put("/api/jobseeker/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Map<String, Object> supportRequest = new HashMap<>();
        supportRequest.put("supportVisibility", "SUMMARY");
        supportRequest.put("textCommunicationPreferred", true);
        supportRequest.put("subtitleNeeded", true);
        supportRequest.put("remoteInterviewPreferred", true);
        supportRequest.put("highContrastNeeded", true);

        mockMvc.perform(put("/api/jobseeker/support-needs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(supportRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MvcResult listResult = mockMvc.perform(get("/api/jobseeker/recommend-jobs")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
        JsonNode jobs = listJson.path("data").path("list");
        assertThat(jobs.size()).isGreaterThanOrEqualTo(2);
        assertThat(jobs.get(0).path("id").asText()).isEqualTo("content-accessibility-editor");
        assertThat(jobs.get(0).path("matchScore").asInt()).isGreaterThan(jobs.get(1).path("matchScore").asInt());

        JsonNode dimensions = jobs.get(0).path("dimensionScores");
        assertThat(findDimensionValue(dimensions, "workMode")).isGreaterThanOrEqualTo(90);
        assertThat(findDimensionValue(dimensions, "communication")).isGreaterThanOrEqualTo(85);

        mockMvc.perform(get("/api/jobseeker/recommend-jobs/content-accessibility-editor")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value("content-accessibility-editor"))
                .andExpect(jsonPath("$.data.reasons[0]").isNotEmpty())
                .andExpect(jsonPath("$.data.supports[0]").isNotEmpty());
    }

    private int findDimensionValue(JsonNode dimensions, String label) {
        for (JsonNode item : dimensions) {
            if (label.equals(item.path("label").asText())) {
                return item.path("value").asInt();
            }
        }
        return 0;
    }

    private String registerAndGetToken() throws Exception {
        String account = "job_match_" + System.currentTimeMillis() + "_" + accounts.size();
        accounts.add(account);

        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("account", account);
        registerRequest.put("password", "Pass@123456");
        registerRequest.put("nickname", "job-match");
        registerRequest.put("roles", java.util.List.of("ROLE_JOBSEEKER"));

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode json = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }
}
