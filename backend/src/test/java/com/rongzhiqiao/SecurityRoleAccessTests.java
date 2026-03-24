package com.rongzhiqiao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class SecurityRoleAccessTests {

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
                jdbcTemplate.update("DELETE FROM jobseeker_service_authorization WHERE jobseeker_user_id = ? OR service_org_user_id = ?", userId, userId);
                jdbcTemplate.update("DELETE FROM jobseeker_sensitive_info WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM enterprise_verification_audit_log WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM enterprise_verification_material WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM enterprise_verification_profile WHERE user_id = ?", userId);
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
    void roleSpecificApisRejectCrossRoleAccess() throws Exception {
        String jobseekerToken = registerAndGetToken("ROLE_JOBSEEKER");
        String enterpriseToken = registerAndGetToken("ROLE_ENTERPRISE");
        String serviceToken = registerAndGetToken("ROLE_SERVICE_ORG");

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + jobseekerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(4003))
                .andExpect(jsonPath("$.message").value("无权执行该操作"));

        mockMvc.perform(get("/api/admin/matching-config")
                        .header("Authorization", "Bearer " + jobseekerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(4003));

        mockMvc.perform(get("/api/enterprise/jobs")
                        .header("Authorization", "Bearer " + jobseekerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(4003));

        mockMvc.perform(get("/api/jobseeker/profile")
                        .header("Authorization", "Bearer " + enterpriseToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(4003));

        mockMvc.perform(get("/api/service/cases")
                        .header("Authorization", "Bearer " + enterpriseToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(4003));

        mockMvc.perform(get("/api/enterprise/jobs")
                        .header("Authorization", "Bearer " + serviceToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(4003));
    }

    private String registerAndGetToken(String role) throws Exception {
        String account = "security_role_" + System.currentTimeMillis() + "_" + accounts.size();
        accounts.add(account);

        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("account", account);
        registerRequest.put("password", "Pass@123456");
        registerRequest.put("nickname", "security-role");
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
