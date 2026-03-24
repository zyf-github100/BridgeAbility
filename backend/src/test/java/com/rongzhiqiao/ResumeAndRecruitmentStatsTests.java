package com.rongzhiqiao;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
class ResumeAndRecruitmentStatsTests {

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
        for (String account : accounts) {
            Long userId = jdbcTemplate.query(
                    "SELECT id FROM sys_user WHERE account = ?",
                    rs -> rs.next() ? rs.getLong("id") : null,
                    account
            );
            if (userId != null) {
                jdbcTemplate.query(
                        "SELECT id FROM job_application WHERE user_id = ?",
                        rs -> {
                            while (rs.next()) {
                                long applicationId = rs.getLong("id");
                                jdbcTemplate.update("DELETE FROM interview_support_request WHERE application_id = ?", applicationId);
                                jdbcTemplate.update("DELETE FROM interview_record WHERE application_id = ?", applicationId);
                            }
                            return null;
                        },
                        userId
                );
                jdbcTemplate.query(
                        "SELECT id FROM catalog_service_case WHERE user_id = ?",
                        rs -> {
                            while (rs.next()) {
                                String caseId = rs.getString("id");
                                jdbcTemplate.update("DELETE FROM service_resource_referral WHERE case_id = ?", caseId);
                                jdbcTemplate.update("DELETE FROM service_followup_record WHERE case_id = ?", caseId);
                                jdbcTemplate.update("DELETE FROM catalog_service_alert WHERE case_id = ?", caseId);
                                jdbcTemplate.update("DELETE FROM catalog_service_case WHERE id = ?", caseId);
                            }
                            return null;
                        },
                        userId
                );
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
                jdbcTemplate.update("DELETE FROM employment_followup WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM jobseeker_service_authorization WHERE jobseeker_user_id = ? OR service_org_user_id = ?", userId, userId);
                jdbcTemplate.update("DELETE FROM jobseeker_sensitive_info WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM job_application WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM enterprise_verification_audit_log WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM enterprise_verification_material WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM enterprise_verification_profile WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM jobseeker_project WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM jobseeker_skill WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM jobseeker_support_need WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM jobseeker_profile WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", userId);
            }
        }
        accounts.clear();

        for (String jobId : jobIds) {
            jdbcTemplate.update("DELETE FROM enterprise_job_posting WHERE job_id = ?", jobId);
            jdbcTemplate.update("DELETE FROM catalog_job WHERE id = ?", jobId);
        }
        jobIds.clear();
    }

    @Test
    void jobseekerCanPreviewAndExportResume() throws Exception {
        String token = registerAndGetToken("ROLE_JOBSEEKER");
        saveJobseekerProfile(token, "简历预览候选人");
        saveSupportNeeds(token, "SUMMARY");
        applyJob(token, "data-ops-assistant");

        mockMvc.perform(get("/api/jobseeker/resume-preview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.displayName").value("简历预览候选人"))
                .andExpect(jsonPath("$.data.profile.skillTags[*].skillName", hasItems("Java", "Excel")))
                .andExpect(jsonPath("$.data.recentApplications[0].jobId").value("data-ops-assistant"))
                .andExpect(jsonPath("$.data.exportFileName", containsString("bridgeability-resume-")))
                .andExpect(jsonPath("$.data.strengths.length()", greaterThanOrEqualTo(1)));

        MvcResult pdfResult = mockMvc.perform(get("/api/jobseeker/resume-export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".pdf")))
                .andExpect(header().string("Content-Type", containsString("application/pdf")))
                .andReturn();

        byte[] pdfBytes = pdfResult.getResponse().getContentAsByteArray();
        org.junit.jupiter.api.Assertions.assertTrue(pdfBytes.length > 4, "PDF export should not be empty");
        org.junit.jupiter.api.Assertions.assertEquals("%PDF",
                new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII));

        MvcResult docxResult = mockMvc.perform(get("/api/jobseeker/resume-export")
                        .param("format", "docx")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".docx")))
                .andExpect(header().string("Content-Type",
                        containsString("application/vnd.openxmlformats-officedocument.wordprocessingml.document")))
                .andReturn();

        byte[] docxBytes = docxResult.getResponse().getContentAsByteArray();
        org.junit.jupiter.api.Assertions.assertTrue(docxBytes.length > 2, "DOCX export should not be empty");
        org.junit.jupiter.api.Assertions.assertEquals("PK",
                new String(docxBytes, 0, 2, StandardCharsets.US_ASCII));
    }

    @Test
    void enterpriseCanViewRecruitmentStatsDashboard() throws Exception {
        String adminToken = registerAndGetToken("ROLE_ADMIN");
        String enterpriseToken = registerAndGetToken("ROLE_ENTERPRISE");
        String jobseekerToken = registerAndGetToken("ROLE_JOBSEEKER");

        approveEnterpriseVerification(enterpriseToken, adminToken);
        String publishedJobId = createJob(enterpriseToken, "stats-live-" + System.currentTimeMillis(), "PUBLISHED");
        createJob(enterpriseToken, "stats-draft-" + System.currentTimeMillis(), "DRAFT");

        saveJobseekerProfile(jobseekerToken, "统计候选人");
        saveSupportNeeds(jobseekerToken, "SUMMARY");
        long applicationId = applyJob(jobseekerToken, publishedJobId);
        inviteAndHireCandidate(enterpriseToken, applicationId);

        mockMvc.perform(get("/api/enterprise/stats")
                        .header("Authorization", "Bearer " + enterpriseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalJobs", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.publishedJobs", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.draftJobs", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalApplications", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.hiredCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.publishStatusBreakdown[?(@.code=='PUBLISHED')].value", hasItem(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.topJobs[?(@.jobId=='" + publishedJobId + "')].candidateCount", hasItem(1)))
                .andExpect(jsonPath("$.data.insights.length()", greaterThanOrEqualTo(1)));
    }

    private void inviteAndHireCandidate(String enterpriseToken, long applicationId) throws Exception {
        Map<String, Object> inviteRequest = new HashMap<>();
        inviteRequest.put("applicationId", applicationId);
        inviteRequest.put("interviewTime", "2026-04-18 15:00:00");
        inviteRequest.put("interviewMode", "ONLINE");
        inviteRequest.put("interviewerName", "统计面试官");

        mockMvc.perform(post("/api/enterprise/interview/invite")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(inviteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Map<String, Object> resultRequest = new HashMap<>();
        resultRequest.put("applicationId", applicationId);
        resultRequest.put("resultStatus", "PASS");
        resultRequest.put("applicationStatus", "HIRED");
        resultRequest.put("feedbackNote", "统计面板测试通过。");

        mockMvc.perform(post("/api/enterprise/interview/result")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(resultRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("HIRED"));
    }

    private String createJob(String enterpriseToken, String title, String publishStatus) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("title", title);
        request.put("department", "招聘运营部");
        request.put("city", "南京");
        request.put("salaryMin", 6000);
        request.put("salaryMax", 8000);
        request.put("headcount", 1);
        request.put("description", "负责候选人台账维护与招聘流程推进。");
        request.put("requirementText", "具备结构化沟通与文档整理能力。");
        request.put("workMode", "HYBRID");
        request.put("deadline", "2026-06-30");
        request.put("interviewMode", "ONLINE");
        request.put("publishStatus", publishStatus);
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

        MvcResult result = mockMvc.perform(post("/api/enterprise/jobs")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.publishStatus").value(publishStatus))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String jobId = json.path("data").path("id").asText();
        jobIds.add(jobId);
        return jobId;
    }

    private void saveJobseekerProfile(String token, String realName) throws Exception {
        Map<String, Object> profileRequest = new HashMap<>();
        profileRequest.put("realName", realName);
        profileRequest.put("schoolName", "融职桥大学");
        profileRequest.put("major", "软件工程");
        profileRequest.put("targetCity", "南京");
        profileRequest.put("expectedJob", "招聘运营助理");
        profileRequest.put("workModePreference", "HYBRID");
        profileRequest.put("intro", "擅长结构化信息整理和异步协作。");
        profileRequest.put("skillTags", java.util.List.of(
                java.util.Map.of("skillCode", "java", "skillName", "Java", "skillLevel", 4),
                java.util.Map.of("skillCode", "excel", "skillName", "Excel", "skillLevel", 4),
                java.util.Map.of("skillCode", "communication", "skillName", "结构化沟通", "skillLevel", 5)
        ));
        profileRequest.put("projectExperiences", java.util.List.of(
                java.util.Map.of(
                        "projectName", "Campus Career Dashboard",
                        "roleName", "Data Assistant",
                        "description", "负责台账整理、问题分类与日报同步。",
                        "startDate", "2025-03-01",
                        "endDate", "2025-06-30"
                )
        ));

        mockMvc.perform(put("/api/jobseeker/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void saveSupportNeeds(String token, String supportVisibility) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("supportVisibility", supportVisibility);
        request.put("textCommunicationPreferred", true);
        request.put("subtitleNeeded", true);
        request.put("remoteInterviewPreferred", true);
        request.put("remark", "请优先提供书面说明。");

        mockMvc.perform(put("/api/jobseeker/support-needs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private long applyJob(String token, String jobId) throws Exception {
        Map<String, Object> applyRequest = new HashMap<>();
        applyRequest.put("coverNote", "我可以稳定维护候选人台账、整理结构化信息，并支持文字优先沟通与异步协作。");
        applyRequest.put("preferredInterviewMode", "TEXT");

        MvcResult result = mockMvc.perform(post("/api/jobseeker/apply/{jobId}", jobId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(applyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("applicationId").asLong();
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
        profileRequest.put("companyName", "Stats Enterprise " + System.currentTimeMillis());
        profileRequest.put("industry", "Data Services");
        profileRequest.put("city", "Nanjing");
        profileRequest.put("unifiedSocialCreditCode", "91320000STATS" + System.currentTimeMillis());
        profileRequest.put("contactName", "Stats Admin");
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
        reviewRequest.put("note", "approved for stats testing");

        mockMvc.perform(post("/api/admin/enterprises/{userId}/review", enterpriseUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.verificationStatus").value("APPROVED"));
    }

    private String registerAndGetToken(String role) throws Exception {
        String account = "resume_stats_" + System.currentTimeMillis() + "_" + accounts.size();
        accounts.add(account);

        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("account", account);
        registerRequest.put("password", "Pass@123456");
        registerRequest.put("nickname", "resume-stats-user");
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
