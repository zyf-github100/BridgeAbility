package com.rongzhiqiao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.storage.provider=local")
@AutoConfigureMockMvc
class EnterpriseJobPostingFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Set<String> accounts = new java.util.LinkedHashSet<>();
    private final Set<String> jobIds = new java.util.LinkedHashSet<>();

    @AfterEach
    void cleanup() {
        for (String jobId : jobIds) {
            jdbcTemplate.update("DELETE FROM enterprise_job_posting WHERE job_id = ?", jobId);
            jdbcTemplate.update("DELETE FROM catalog_job WHERE id = ?", jobId);
        }
        jobIds.clear();

        for (String account : accounts) {
            Long userId = jdbcTemplate.query(
                    "SELECT id FROM sys_user WHERE account = ?",
                    rs -> rs.next() ? rs.getLong("id") : null,
                    account
            );
            if (userId != null) {
                jdbcTemplate.update("DELETE FROM jobseeker_service_authorization WHERE jobseeker_user_id = ? OR service_org_user_id = ?", userId, userId);
                jdbcTemplate.update("DELETE FROM jobseeker_sensitive_info WHERE user_id = ?", userId);
                jdbcTemplate.query(
                        "SELECT storage_path FROM enterprise_verification_material WHERE user_id = ?",
                        rs -> {
                            while (rs.next()) {
                                try {
                                    Files.deleteIfExists(Path.of(rs.getString("storage_path")));
                                } catch (java.io.IOException ignored) {
                                    // ignore cleanup failure in tests
                                }
                            }
                            return null;
                        },
                        userId
                );
                jdbcTemplate.update("DELETE FROM enterprise_verification_audit_log WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM enterprise_verification_material WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM enterprise_verification_profile WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", userId);
            }
        }
        accounts.clear();
    }

    @Test
    void enterpriseCanDownloadUploadedVerificationMaterial() throws Exception {
        String enterpriseToken = registerAndGetToken("ROLE_ENTERPRISE");
        byte[] fileContent = "download-check".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "license.pdf",
                "application/pdf",
                fileContent
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/enterprise/profile/materials")
                        .file(file)
                        .param("materialType", "BUSINESS_LICENSE")
                        .param("note", "business license")
                        .header("Authorization", "Bearer " + enterpriseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        long materialId = uploadJson.path("data").path("id").asLong();

        mockMvc.perform(get("/api/enterprise/profile/materials/{materialId}/download", materialId)
                        .header("Authorization", "Bearer " + enterpriseToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes(fileContent));
    }

    @Test
    void enterpriseCannotPublishJobBeforeVerificationApproved() throws Exception {
        String enterpriseToken = registerAndGetToken("ROLE_ENTERPRISE");

        Map<String, Object> request = createJobRequest("publish-check-" + System.currentTimeMillis());
        request.put("publishStatus", "PUBLISHED");
        request.put("accessibilityTag", Map.ofEntries(
                Map.entry("onsiteRequired", false),
                Map.entry("remoteSupported", true),
                Map.entry("highFrequencyVoiceRequired", false),
                Map.entry("noisyEnvironment", false),
                Map.entry("longStandingRequired", false),
                Map.entry("textMaterialSupported", true),
                Map.entry("onlineInterviewSupported", true),
                Map.entry("textInterviewSupported", true),
                Map.entry("flexibleScheduleSupported", true),
                Map.entry("accessibleWorkspace", true),
                Map.entry("assistiveSoftwareSupported", true)
        ));

        mockMvc.perform(post("/api/enterprise/jobs")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("企业认证通过后才能发布岗位"));
    }

    @Test
    void enterpriseCannotPublishJobWithoutAccessibilityTags() throws Exception {
        String adminToken = registerAndGetToken("ROLE_ADMIN");
        String enterpriseToken = registerAndGetToken("ROLE_ENTERPRISE");
        approveEnterpriseVerification(enterpriseToken, adminToken);

        Map<String, Object> request = createJobRequest("publish-check-" + System.currentTimeMillis());
        request.put("publishStatus", "PUBLISHED");
        request.put("accessibilityTag", Map.of(
                "onsiteRequired", false,
                "remoteSupported", true,
                "textMaterialSupported", true
        ));

        mockMvc.perform(post("/api/enterprise/jobs")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("发布前请先补齐无障碍标签"));
    }

    @Test
    void enterpriseCannotCreateJobWithPlaceholderCorruptedText() throws Exception {
        String enterpriseToken = registerAndGetToken("ROLE_ENTERPRISE");

        Map<String, Object> request = createJobRequest("VIEWED ?????? job-viewed-check-1774219417076");
        request.put("department", "???");
        request.put("city", "??");
        request.put("publishStatus", "DRAFT");

        mockMvc.perform(post("/api/enterprise/jobs")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("岗位标题包含异常占位字符，请检查输入编码后重试"));
    }

    @Test
    void draftJobRemainsHiddenUntilPublished() throws Exception {
        String adminToken = registerAndGetToken("ROLE_ADMIN");
        String enterpriseToken = registerAndGetToken("ROLE_ENTERPRISE");
        String jobseekerToken = registerAndGetToken("ROLE_JOBSEEKER");
        String keyword = "qa-job-" + System.currentTimeMillis();
        approveEnterpriseVerification(enterpriseToken, adminToken);

        Map<String, Object> draftRequest = createJobRequest(keyword);
        draftRequest.put("publishStatus", "DRAFT");
        draftRequest.put("accessibilityTag", Map.of(
                "onsiteRequired", false,
                "remoteSupported", true,
                "textMaterialSupported", true,
                "onlineInterviewSupported", true,
                "textInterviewSupported", true
        ));

        MvcResult createResult = mockMvc.perform(post("/api/enterprise/jobs")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(draftRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.publishStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.readyToPublish").value(false))
                .andReturn();

        String jobId = extractJobId(createResult);
        jobIds.add(jobId);

        mockMvc.perform(get("/api/jobseeker/recommend-jobs")
                        .header("Authorization", "Bearer " + jobseekerToken)
                        .param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));

        Map<String, Object> publishRequest = createJobRequest(keyword);
        publishRequest.put("publishStatus", "PUBLISHED");
        publishRequest.put("accessibilityTag", Map.ofEntries(
                Map.entry("onsiteRequired", false),
                Map.entry("remoteSupported", true),
                Map.entry("highFrequencyVoiceRequired", false),
                Map.entry("noisyEnvironment", false),
                Map.entry("longStandingRequired", false),
                Map.entry("textMaterialSupported", true),
                Map.entry("onlineInterviewSupported", true),
                Map.entry("textInterviewSupported", true),
                Map.entry("flexibleScheduleSupported", true),
                Map.entry("accessibleWorkspace", true),
                Map.entry("assistiveSoftwareSupported", true)
        ));

        mockMvc.perform(put("/api/enterprise/jobs/{jobId}", jobId)
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(publishRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.publishStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.readyToPublish").value(true));

        mockMvc.perform(get("/api/jobseeker/recommend-jobs")
                        .header("Authorization", "Bearer " + jobseekerToken)
                        .param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(jobId));
    }

    private Map<String, Object> createJobRequest(String title) {
        Map<String, Object> request = new HashMap<>();
        request.put("title", title);
        request.put("department", "测试部门");
        request.put("city", "南京");
        request.put("salaryMin", 5000);
        request.put("salaryMax", 7000);
        request.put("headcount", 2);
        request.put("description", "负责结构化内容录入\n维护运营台账");
        request.put("requirementText", "具备文字处理能力\n适应异步协作");
        request.put("workMode", "HYBRID");
        request.put("deadline", "2026-06-30");
        request.put("interviewMode", "ONLINE");
        return request;
    }

    private void approveEnterpriseVerification(String enterpriseToken, String adminToken) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "license.pdf",
                "application/pdf",
                "fake-license".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/enterprise/profile/materials")
                        .file(file)
                        .param("materialType", "BUSINESS_LICENSE")
                        .param("note", "business license")
                        .header("Authorization", "Bearer " + enterpriseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Map<String, Object> profileRequest = new HashMap<>();
        profileRequest.put("companyName", "Flow Enterprise " + System.currentTimeMillis());
        profileRequest.put("industry", "Data Services");
        profileRequest.put("city", "Nanjing");
        profileRequest.put("unifiedSocialCreditCode", "91320000FLOW" + System.currentTimeMillis());
        profileRequest.put("contactName", "Flow Admin");
        profileRequest.put("contactPhone", "13800000000");
        profileRequest.put("officeAddress", "Nanjing Software Avenue");
        profileRequest.put("accessibilityCommitment", "Provide accessible interview and workspace support.");
        profileRequest.put("submitForReview", true);

        MvcResult submitResult = mockMvc.perform(put("/api/enterprise/profile")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"))
                .andReturn();

        JsonNode profileJson = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        long enterpriseUserId = profileJson.path("data").path("userId").asLong();

        Map<String, Object> reviewRequest = new HashMap<>();
        reviewRequest.put("decision", "APPROVED");
        reviewRequest.put("note", "approved for publishing");

        mockMvc.perform(post("/api/admin/enterprises/{userId}/review", enterpriseUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.verificationStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.canPublishJobs").value(true));
    }

    private String registerAndGetToken(String role) throws Exception {
        String account = "enterprise_flow_" + System.currentTimeMillis() + "_" + accounts.size();
        accounts.add(account);

        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("account", account);
        registerRequest.put("password", "Pass@123456");
        registerRequest.put("nickname", "enterprise-flow");
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

    private String extractJobId(MvcResult mvcResult) throws Exception {
        JsonNode json = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return json.path("data").path("id").asText();
    }
}
