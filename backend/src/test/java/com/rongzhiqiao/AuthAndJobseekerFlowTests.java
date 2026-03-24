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
class AuthAndJobseekerFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String account;

    @AfterEach
    void cleanup() {
        if (account == null) {
            return;
        }
        Long userId = jdbcTemplate.query(
                "SELECT id FROM sys_user WHERE account = ?",
                rs -> rs.next() ? rs.getLong("id") : null,
                account
        );
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM jobseeker_service_authorization WHERE jobseeker_user_id = ? OR service_org_user_id = ?", userId, userId);
            jdbcTemplate.update("DELETE FROM jobseeker_sensitive_info WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM job_application WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM jobseeker_project WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM jobseeker_skill WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM jobseeker_support_need WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM jobseeker_profile WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", userId);
        }
    }

    @Test
    void registerAndPersistJobseekerProfile() throws Exception {
        String token = registerJobseeker();

        Map<String, Object> profileRequest = new HashMap<>();
        profileRequest.put("realName", "测试候选人");
        profileRequest.put("gender", "FEMALE");
        profileRequest.put("schoolName", "融职桥大学");
        profileRequest.put("major", "软件工程");
        profileRequest.put("targetCity", "上海");
        profileRequest.put("expectedJob", "后端开发工程师");
        profileRequest.put("workModePreference", "FULL_TIME");
        profileRequest.put("skillTags", java.util.List.of(
                java.util.Map.of("skillCode", "java", "skillName", "Java", "skillLevel", 4),
                java.util.Map.of("skillCode", "sql", "skillName", "SQL", "skillLevel", 3)
        ));
        profileRequest.put("projectExperiences", java.util.List.of(
                java.util.Map.of(
                        "projectName", "Campus Data Dashboard",
                        "roleName", "Backend Intern",
                        "description", "Built APIs and data sync jobs for dashboard reporting.",
                        "startDate", "2025-03-01",
                        "endDate", "2025-06-30"
                )
        ));

        mockMvc.perform(put("/api/jobseeker/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.realName").value("测试候选人"))
                .andExpect(jsonPath("$.data.skillTags[0].skillName").value("Java"))
                .andExpect(jsonPath("$.data.projectExperiences[0].projectName").value("Campus Data Dashboard"))
                .andExpect(jsonPath("$.data.abilityCards[0].title").isNotEmpty());

        mockMvc.perform(get("/api/jobseeker/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.realName").value("测试候选人"))
                .andExpect(jsonPath("$.data.schoolName").value("融职桥大学"))
                .andExpect(jsonPath("$.data.expectedJob").value("后端开发工程师"))
                .andExpect(jsonPath("$.data.skillTags[1].skillCode").value("sql"))
                .andExpect(jsonPath("$.data.projectExperiences[0].periodLabel").isNotEmpty())
                .andExpect(jsonPath("$.data.abilityCards[1].summary").isNotEmpty());
    }

    @Test
    void applyJobAndQueryApplicationRecords() throws Exception {
        String token = registerJobseeker();
        saveSupportNeeds(token);

        Map<String, Object> applyRequest = new HashMap<>();
        applyRequest.put("coverNote", "我擅长结构化数据工作，希望采用文字优先的面试方式。");
        applyRequest.put("preferredInterviewMode", "TEXT");

        mockMvc.perform(post("/api/jobseeker/apply/data-ops-assistant")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(applyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.jobId").value("data-ops-assistant"))
                .andExpect(jsonPath("$.data.coverNote").value(applyRequest.get("coverNote")))
                .andExpect(jsonPath("$.data.preferredInterviewMode").value("TEXT"))
                .andExpect(jsonPath("$.data.supportVisibility").value("SUMMARY"))
                .andExpect(jsonPath("$.data.additionalSupport").isNotEmpty())
                .andExpect(jsonPath("$.data.explanationSnapshot[0]").isNotEmpty());

        mockMvc.perform(get("/api/jobseeker/applications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].jobId").value("data-ops-assistant"))
                .andExpect(jsonPath("$.data[0].coverNote").value(applyRequest.get("coverNote")))
                .andExpect(jsonPath("$.data[0].preferredInterviewMode").value("TEXT"))
                .andExpect(jsonPath("$.data[0].supportVisibility").value("SUMMARY"))
                .andExpect(jsonPath("$.data[0].status").value("APPLIED"));
    }

    @Test
    void saveAndReadSupportNeeds() throws Exception {
        String token = registerJobseeker();

        mockMvc.perform(put("/api/jobseeker/support-needs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(buildSupportNeedsRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.supportVisibility").value("SUMMARY"))
                .andExpect(jsonPath("$.data.textCommunicationPreferred").value(true))
                .andExpect(jsonPath("$.data.supportSummary[0]").isNotEmpty())
                .andExpect(jsonPath("$.data.interviewCommunicationCard.lines[0]").isNotEmpty());

        mockMvc.perform(get("/api/jobseeker/support-needs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.supportVisibility").value("SUMMARY"))
                .andExpect(jsonPath("$.data.subtitleNeeded").value(true))
                .andExpect(jsonPath("$.data.remark").isNotEmpty());
    }

    private void saveSupportNeeds(String token) throws Exception {
        mockMvc.perform(put("/api/jobseeker/support-needs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(buildSupportNeedsRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private Map<String, Object> buildSupportNeedsRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("supportVisibility", "SUMMARY");
        request.put("textCommunicationPreferred", true);
        request.put("subtitleNeeded", true);
        request.put("remoteInterviewPreferred", true);
        request.put("highContrastNeeded", true);
        request.put("remark", "请在面试前提供高对比度材料，可先文字沟通。");
        return request;
    }

    private String registerJobseeker() throws Exception {
        account = "it_" + System.currentTimeMillis();

        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("account", account);
        registerRequest.put("password", "Pass@123456");
        registerRequest.put("nickname", "集成测试用户");

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.account").value(account))
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        return registerJson.path("data").path("accessToken").asText();
    }
}
