package com.rongzhiqiao;

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
class AdminMatchingConfigApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Set<String> accounts = new java.util.LinkedHashSet<>();

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM admin_matching_config WHERE config_code = 'default'");
        for (String account : accounts) {
            Long userId = jdbcTemplate.query(
                    "SELECT id FROM sys_user WHERE account = ?",
                    rs -> rs.next() ? rs.getLong("id") : null,
                    account
            );
            if (userId != null) {
                jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", userId);
            }
        }
        accounts.clear();
    }

    @Test
    void adminCanReadUpdateAndResetMatchingConfig() throws Exception {
        String adminToken = registerAndGetToken("ROLE_ADMIN");

        mockMvc.perform(get("/api/admin/matching-config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.customized").value(false))
                .andExpect(jsonPath("$.data.scoreWeights.skill").value(34.0))
                .andExpect(jsonPath("$.data.candidateStage.priorityThreshold").value(85));

        Map<String, Object> updatePayload = Map.of(
                "scoreWeights", Map.of(
                        "skill", 20.0,
                        "workMode", 28.0,
                        "communication", 18.0,
                        "environment", 14.0,
                        "accommodation", 20.0
                ),
                "risk", Map.of(
                        "penaltyPerRisk", 6,
                        "penaltyPerBlockingRisk", 15,
                        "maxPenalty", 44,
                        "hardFilteredMaxScore", 18
                ),
                "candidateStage", Map.of(
                        "matchScoreWeight", 6.0,
                        "profileCompletionWeight", 4.0,
                        "priorityThreshold", 82,
                        "followUpThreshold", 64
                )
        );

        mockMvc.perform(put("/api/admin/matching-config")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.customized").value(true))
                .andExpect(jsonPath("$.data.scoreWeights.workMode").value(28.0))
                .andExpect(jsonPath("$.data.risk.hardFilteredMaxScore").value(18))
                .andExpect(jsonPath("$.data.candidateStage.followUpThreshold").value(64));

        mockMvc.perform(get("/api/admin/matching-config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.customized").value(true))
                .andExpect(jsonPath("$.data.scoreWeights.skill").value(20.0))
                .andExpect(jsonPath("$.data.updatedByUserId").isNumber());

        mockMvc.perform(post("/api/admin/matching-config/reset")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.customized").value(false))
                .andExpect(jsonPath("$.data.scoreWeights.skill").value(34.0))
                .andExpect(jsonPath("$.data.risk.maxPenalty").value(40))
                .andExpect(jsonPath("$.data.candidateStage.priorityThreshold").value(85));
    }

    private String registerAndGetToken(String role) throws Exception {
        String account = "admin_matching_" + System.currentTimeMillis() + "_" + accounts.size();
        accounts.add(account);

        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("account", account);
        registerRequest.put("password", "Pass@123456");
        registerRequest.put("nickname", "admin-matching");
        registerRequest.put("roles", java.util.List.of(role));

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
