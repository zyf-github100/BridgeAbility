package com.rongzhiqiao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class PrivacyControlsIntegrationTests {

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
            Long userId = lookupUserId(account);
            if (userId == null) {
                continue;
            }

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
        accounts.clear();
    }

    @Test
    void sensitiveInfoIsEncryptedAndRequiresExplicitServiceAuthorization() throws Exception {
        TestSession jobseeker = registerAndGetSession("ROLE_JOBSEEKER");
        TestSession authorizedService = registerAndGetSession("ROLE_SERVICE_ORG");
        TestSession unauthorizedService = registerAndGetSession("ROLE_SERVICE_ORG");

        saveJobseekerProfile(jobseeker.token(), "Privacy Candidate");

        mockMvc.perform(put("/api/jobseeker/sensitive-info")
                        .header("Authorization", "Bearer " + jobseeker.token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "disabilityType", "hearing",
                                "disabilityLevel", "level-2",
                                "supportNeedDetail", "needs live captions",
                                "healthNote", "avoid phone-first workflows",
                                "emergencyContactName", "Alice",
                                "emergencyContactPhone", "13911112222"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.disabilityType").value("hearing"));

        Long jobseekerUserId = lookupUserId(jobseeker.account());
        assertThat(jobseekerUserId).isNotNull();

        String disabilityCiphertext = jdbcTemplate.query(
                """
                        SELECT disability_type_ciphertext
                        FROM jobseeker_sensitive_info
                        WHERE user_id = ?
                        """,
                rs -> rs.next() ? rs.getString("disability_type_ciphertext") : null,
                jobseekerUserId
        );
        assertThat(disabilityCiphertext).isNotBlank();
        assertThat(disabilityCiphertext).isNotEqualTo("hearing");

        mockMvc.perform(get("/api/service/jobseekers/{userId}/profile", jobseekerUserId)
                        .header("Authorization", "Bearer " + authorizedService.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003));

        mockMvc.perform(get("/api/service/jobseekers/{userId}/sensitive-info", jobseekerUserId)
                        .header("Authorization", "Bearer " + authorizedService.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003));

        mockMvc.perform(put("/api/jobseeker/service-authorizations")
                        .header("Authorization", "Bearer " + jobseeker.token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "serviceOrgAccount", authorizedService.account(),
                                "profileAccessGranted", true,
                                "sensitiveAccessGranted", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.serviceOrgAccount").value(authorizedService.account()))
                .andExpect(jsonPath("$.data.profileAccessGranted").value(true))
                .andExpect(jsonPath("$.data.sensitiveAccessGranted").value(true));

        mockMvc.perform(get("/api/service/jobseekers/{userId}/profile", jobseekerUserId)
                        .header("Authorization", "Bearer " + authorizedService.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.realName").value("Privacy Candidate"));

        mockMvc.perform(get("/api/service/jobseekers/{userId}/sensitive-info", jobseekerUserId)
                        .header("Authorization", "Bearer " + authorizedService.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.disabilityType").value("hearing"))
                .andExpect(jsonPath("$.data.emergencyContactName").value("Alice"));

        mockMvc.perform(get("/api/service/jobseekers/{userId}/sensitive-info", jobseekerUserId)
                        .header("Authorization", "Bearer " + unauthorizedService.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003));
    }

    @Test
    void enterpriseCanOnlyAccessOwnedCandidatesAndGetsMinimalProfileFields() throws Exception {
        TestSession admin = registerAndGetSession("ROLE_ADMIN");
        TestSession enterpriseA = registerAndGetSession("ROLE_ENTERPRISE");
        TestSession enterpriseB = registerAndGetSession("ROLE_ENTERPRISE");
        TestSession jobseeker = registerAndGetSession("ROLE_JOBSEEKER");

        approveEnterpriseVerification(enterpriseA, admin, "Privacy Company A");
        approveEnterpriseVerification(enterpriseB, admin, "Privacy Company B");
        String jobId = createPublishedJob(enterpriseA.token(), "privacy-job-" + System.currentTimeMillis());
        saveJobseekerProfile(jobseeker.token(), "Enterprise Candidate");
        saveSupportNeeds(jobseeker.token(), "SUMMARY");
        long applicationId = applyJob(jobseeker.token(), jobId);

        mockMvc.perform(get("/api/enterprise/jobs/{jobId}/candidates", jobId)
                        .header("Authorization", "Bearer " + enterpriseA.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].candidateName").value("E***"))
                .andExpect(jsonPath("$.data.list[0].schoolName").value(""))
                .andExpect(jsonPath("$.data.list[0].major").value(""))
                .andExpect(jsonPath("$.data.list[0].intro").value(""))
                .andExpect(jsonPath("$.data.list[0].preferredInterviewMode").value("TEXT"));

        mockMvc.perform(get("/api/enterprise/jobs/{jobId}/candidates", jobId)
                        .header("Authorization", "Bearer " + enterpriseB.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4004));

        mockMvc.perform(post("/api/enterprise/applications/{applicationId}/status", applicationId)
                        .header("Authorization", "Bearer " + enterpriseB.token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("targetStatus", "VIEWED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4004));
    }

    @Test
    void adminReviewDetailMasksSensitiveFieldsWhileEnterpriseKeepsFullValues() throws Exception {
        TestSession admin = registerAndGetSession("ROLE_ADMIN");
        TestSession enterprise = registerAndGetSession("ROLE_ENTERPRISE");

        long enterpriseUserId = submitEnterpriseVerification(
                enterprise.token(),
                "Masking Company",
                "91320000MASK1234",
                "Mask Admin",
                "13800000000",
                "Nanjing Software Avenue"
        );

        mockMvc.perform(get("/api/enterprise/profile")
                        .header("Authorization", "Bearer " + enterprise.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.unifiedSocialCreditCode").value("91320000MASK1234"))
                .andExpect(jsonPath("$.data.contactName").value("Mask Admin"))
                .andExpect(jsonPath("$.data.contactPhone").value("13800000000"))
                .andExpect(jsonPath("$.data.officeAddress").value("Nanjing Software Avenue"));

        mockMvc.perform(get("/api/admin/enterprises/{userId}/review", enterpriseUserId)
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.unifiedSocialCreditCode").value("9132****1234"))
                .andExpect(jsonPath("$.data.contactName").value("M***"))
                .andExpect(jsonPath("$.data.contactPhone").value("138****0000"))
                .andExpect(jsonPath("$.data.officeAddress").value("Nanjin****"));
    }

    private void saveJobseekerProfile(String token, String realName) throws Exception {
        Map<String, Object> profileRequest = new HashMap<>();
        profileRequest.put("realName", realName);
        profileRequest.put("schoolName", "BridgeAbility University");
        profileRequest.put("major", "Software Engineering");
        profileRequest.put("targetCity", "Nanjing");
        profileRequest.put("expectedJob", "Data Ops Assistant");
        profileRequest.put("workModePreference", "HYBRID");
        profileRequest.put("intro", "Prefer written-first collaboration.");

        mockMvc.perform(put("/api/jobseeker/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void saveSupportNeeds(String token, String supportVisibility) throws Exception {
        mockMvc.perform(put("/api/jobseeker/support-needs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "supportVisibility", supportVisibility,
                                "textCommunicationPreferred", true,
                                "subtitleNeeded", true,
                                "remoteInterviewPreferred", true,
                                "remark", "Please share written instructions in advance."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private long applyJob(String token, String jobId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/jobseeker/apply/{jobId}", jobId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "coverNote", "I work well with structured ledgers and text-first communication.",
                                "preferredInterviewMode", "TEXT"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("applicationId")
                .asLong();
    }

    private void approveEnterpriseVerification(TestSession enterprise, TestSession admin, String companyName) throws Exception {
        long enterpriseUserId = submitEnterpriseVerification(
                enterprise.token(),
                companyName,
                "91320000" + System.currentTimeMillis(),
                "Flow Admin",
                "13800000000",
                "Nanjing Software Avenue"
        );

        mockMvc.perform(post("/api/admin/enterprises/{userId}/review", enterpriseUserId)
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "decision", "APPROVED",
                                "note", "approved for privacy tests"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.verificationStatus").value("APPROVED"));
    }

    private long submitEnterpriseVerification(String enterpriseToken,
                                              String companyName,
                                              String unifiedSocialCreditCode,
                                              String contactName,
                                              String contactPhone,
                                              String officeAddress) throws Exception {
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

        MvcResult submitResult = mockMvc.perform(put("/api/enterprise/profile")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", companyName,
                                "industry", "Data Services",
                                "city", "Nanjing",
                                "unifiedSocialCreditCode", unifiedSocialCreditCode,
                                "contactName", contactName,
                                "contactPhone", contactPhone,
                                "officeAddress", officeAddress,
                                "accessibilityCommitment", "Provide accessible interview and workspace support.",
                                "submitForReview", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"))
                .andReturn();

        return objectMapper.readTree(submitResult.getResponse().getContentAsString())
                .path("data")
                .path("userId")
                .asLong();
    }

    private String createPublishedJob(String enterpriseToken, String title) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("title", title);
        request.put("department", "Privacy Ops");
        request.put("city", "Nanjing");
        request.put("salaryMin", 6000);
        request.put("salaryMax", 8000);
        request.put("headcount", 1);
        request.put("description", "Maintain structured hiring workflows and candidate ledgers.");
        request.put("requirementText", "Strong written communication and async collaboration.");
        request.put("workMode", "HYBRID");
        request.put("deadline", "2026-06-30");
        request.put("interviewMode", "ONLINE");
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

        MvcResult createResult = mockMvc.perform(post("/api/enterprise/jobs")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.publishStatus").value("PUBLISHED"))
                .andReturn();

        String jobId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText();
        jobIds.add(jobId);
        return jobId;
    }

    private TestSession registerAndGetSession(String role) throws Exception {
        String account = "privacy_" + System.currentTimeMillis() + "_" + accounts.size();
        accounts.add(account);

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "account", account,
                                "password", "Pass@123456",
                                "nickname", "privacy-user",
                                "roles", java.util.List.of(role)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode payload = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        return new TestSession(account, payload.path("data").path("accessToken").asText());
    }

    private Long lookupUserId(String account) {
        return jdbcTemplate.query(
                "SELECT id FROM sys_user WHERE account = ?",
                rs -> rs.next() ? rs.getLong("id") : null,
                account
        );
    }

    private record TestSession(String account, String token) {
    }
}
