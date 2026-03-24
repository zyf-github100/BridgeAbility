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
class EnterpriseInterviewFlowTests {

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
                                jdbcTemplate.update("DELETE FROM interview_support_request WHERE application_id = ?", rs.getLong("id"));
                                jdbcTemplate.update("DELETE FROM interview_record WHERE application_id = ?", rs.getLong("id"));
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
    void enterpriseCanInviteCandidateAndRecordPassResult() throws Exception {
        String adminToken = registerAndGetToken("ROLE_ADMIN");
        String enterpriseToken = registerAndGetToken("ROLE_ENTERPRISE");
        String jobseekerToken = registerAndGetToken("ROLE_JOBSEEKER");

        approveEnterpriseVerification(enterpriseToken, adminToken);
        String jobId = createPublishedJob(enterpriseToken, "interview-pass-" + System.currentTimeMillis());
        saveJobseekerProfile(jobseekerToken, "面试候选人");
        saveSupportNeeds(jobseekerToken);
        long applicationId = applyJob(jobseekerToken, jobId);

        Map<String, Object> inviteRequest = new HashMap<>();
        inviteRequest.put("applicationId", applicationId);
        inviteRequest.put("interviewTime", "2026-04-10 15:00:00");
        inviteRequest.put("interviewMode", "ONLINE");
        inviteRequest.put("interviewerName", "王经理");
        inviteRequest.put("note", "请提前准备项目经历说明。");

        mockMvc.perform(post("/api/enterprise/interview/invite")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(inviteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                .andExpect(jsonPath("$.data.status").value("INTERVIEW"))
                .andExpect(jsonPath("$.data.latestInterview.interviewMode").value("ONLINE"))
                .andExpect(jsonPath("$.data.latestInterview.resultStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.supportRequests.length()").value(4));

        Map<String, Object> resultRequest = new HashMap<>();
        resultRequest.put("applicationId", applicationId);
        resultRequest.put("resultStatus", "PASS");
        resultRequest.put("feedbackNote", "项目能力匹配较好，建议进入试岗。");
        resultRequest.put("applicationStatus", "OFFERED");

        mockMvc.perform(post("/api/enterprise/interview/result")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(resultRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("OFFERED"))
                .andExpect(jsonPath("$.data.latestInterview.resultStatus").value("PASS"))
                .andExpect(jsonPath("$.data.latestInterview.feedbackNote").value("项目能力匹配较好，建议进入试岗。"));

        mockMvc.perform(get("/api/jobseeker/applications")
                        .header("Authorization", "Bearer " + jobseekerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].status").value("OFFERED"))
                .andExpect(jsonPath("$.data[0].latestInterview.interviewTime").value("2026-04-10 15:00:00"))
                .andExpect(jsonPath("$.data[0].latestInterview.resultStatus").value("PASS"))
                .andExpect(jsonPath("$.data[0].interviewRecords.length()").value(1));
    }

    @Test
    void failedInterviewRequiresRejectReasonAndEndsAsRejected() throws Exception {
        String adminToken = registerAndGetToken("ROLE_ADMIN");
        String enterpriseToken = registerAndGetToken("ROLE_ENTERPRISE");
        String jobseekerToken = registerAndGetToken("ROLE_JOBSEEKER");

        approveEnterpriseVerification(enterpriseToken, adminToken);
        String jobId = createPublishedJob(enterpriseToken, "interview-fail-" + System.currentTimeMillis());
        saveJobseekerProfile(jobseekerToken, "待拒绝候选人");
        saveSupportNeeds(jobseekerToken);
        long applicationId = applyJob(jobseekerToken, jobId);

        Map<String, Object> inviteRequest = new HashMap<>();
        inviteRequest.put("applicationId", applicationId);
        inviteRequest.put("interviewTime", "2026-04-12 10:30:00");
        inviteRequest.put("interviewMode", "TEXT");
        inviteRequest.put("interviewerName", "李主管");

        mockMvc.perform(post("/api/enterprise/interview/invite")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(inviteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("INTERVIEW"));

        Map<String, Object> invalidResultRequest = new HashMap<>();
        invalidResultRequest.put("applicationId", applicationId);
        invalidResultRequest.put("resultStatus", "FAIL");
        invalidResultRequest.put("applicationStatus", "REJECTED");

        mockMvc.perform(post("/api/enterprise/interview/result")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                .content(objectMapper.writeValueAsString(invalidResultRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("面试结果为未通过时，必须填写未通过原因"));

        Map<String, Object> validResultRequest = new HashMap<>();
        validResultRequest.put("applicationId", applicationId);
        validResultRequest.put("resultStatus", "FAIL");
        validResultRequest.put("applicationStatus", "REJECTED");
        validResultRequest.put("feedbackNote", "岗位节奏和职责要求暂不匹配。");
        validResultRequest.put("rejectReason", "缺少岗位要求的高频协同经验");

        mockMvc.perform(post("/api/enterprise/interview/result")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validResultRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.latestInterview.resultStatus").value("FAIL"))
                .andExpect(jsonPath("$.data.latestInterview.rejectReason").value("缺少岗位要求的高频协同经验"));
    }

    @Test
    void enterpriseCanViewCandidateMatchExplanationAndUpdateStatusIndependently() throws Exception {
        String adminToken = registerAndGetToken("ROLE_ADMIN");
        String enterpriseToken = registerAndGetToken("ROLE_ENTERPRISE");
        String jobseekerToken = registerAndGetToken("ROLE_JOBSEEKER");

        approveEnterpriseVerification(enterpriseToken, adminToken);
        String jobId = createPublishedJob(enterpriseToken, "candidate-explanation-" + System.currentTimeMillis());
        saveJobseekerProfile(jobseekerToken, "匹配解释候选人");
        saveSupportNeeds(jobseekerToken, "SUMMARY");
        long applicationId = applyJob(jobseekerToken, jobId);

        mockMvc.perform(get("/api/enterprise/jobs/{jobId}/candidates", jobId)
                        .header("Authorization", "Bearer " + enterpriseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].applicationId").value(applicationId))
                .andExpect(jsonPath("$.data.list[0].recommendationStage").isString())
                .andExpect(jsonPath("$.data.list[0].recommendationSummary").isString())
                .andExpect(jsonPath("$.data.list[0].dimensionScores").isArray())
                .andExpect(jsonPath("$.data.list[0].explanationSnapshot").isArray())
                .andExpect(jsonPath("$.data.list[0].explanationSnapshot[0]").isNotEmpty());

        Map<String, Object> statusRequest = new HashMap<>();
        statusRequest.put("targetStatus", "VIEWED");

        mockMvc.perform(post("/api/enterprise/applications/{applicationId}/status", applicationId)
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("VIEWED"));

        mockMvc.perform(get("/api/jobseeker/applications")
                        .header("Authorization", "Bearer " + jobseekerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].status").value("VIEWED"));
    }

    @Test
    void hiredCandidateCreatesAutoFollowupsAndIndependentInterviewRequestIsVisible() throws Exception {
        String adminToken = registerAndGetToken("ROLE_ADMIN");
        String enterpriseToken = registerAndGetToken("ROLE_ENTERPRISE");
        String jobseekerToken = registerAndGetToken("ROLE_JOBSEEKER");
        String serviceToken = registerAndGetToken("ROLE_SERVICE_ORG");

        approveEnterpriseVerification(enterpriseToken, adminToken);
        String jobId = createPublishedJob(enterpriseToken, "interview-followup-" + System.currentTimeMillis());
        saveJobseekerProfile(jobseekerToken, "入职回访候选人");
        saveSupportNeeds(jobseekerToken, "HIDDEN");
        long applicationId = applyJob(jobseekerToken, jobId);

        Map<String, Object> supportRequest = new HashMap<>();
        supportRequest.put("applicationId", applicationId);
        supportRequest.put("requestType", "TEXT_INTERVIEW");
        supportRequest.put("requestContent", "希望优先采用文字面试，并同步提供字幕支持。");

        mockMvc.perform(post("/api/jobseeker/interview-support-request")
                        .header("Authorization", "Bearer " + jobseekerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(supportRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.requestType").value("TEXT_INTERVIEW"))
                .andExpect(jsonPath("$.data.requestStatus").value("PENDING"));

        mockMvc.perform(get("/api/enterprise/jobs/{jobId}/candidates", jobId)
                        .header("Authorization", "Bearer " + enterpriseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].applicationId").value(applicationId))
                .andExpect(jsonPath("$.data.list[0].supportRequests.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].supportRequests[0].requestType").value("TEXT_INTERVIEW"))
                .andExpect(jsonPath("$.data.list[0].supportRequests[0].requestSource").value("INTERVIEW_SUPPORT_REQUEST"))
                .andExpect(jsonPath("$.data.list[0].supportRequests[0].requestStatus").value("PENDING"));

        Map<String, Object> inviteRequest = new HashMap<>();
        inviteRequest.put("applicationId", applicationId);
        inviteRequest.put("interviewTime", "2026-04-15 14:00:00");
        inviteRequest.put("interviewMode", "TEXT");
        inviteRequest.put("interviewerName", "张主管");

        mockMvc.perform(post("/api/enterprise/interview/invite")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(inviteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("INTERVIEW"));

        Map<String, Object> resultRequest = new HashMap<>();
        resultRequest.put("applicationId", applicationId);
        resultRequest.put("resultStatus", "PASS");
        resultRequest.put("feedbackNote", "候选人可以进入正式入职阶段。");
        resultRequest.put("applicationStatus", "HIRED");

        mockMvc.perform(post("/api/enterprise/interview/result")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(resultRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("HIRED"));

        String caseId = jdbcTemplate.query(
                "SELECT id FROM catalog_service_case WHERE user_id = (SELECT user_id FROM job_application WHERE id = ?)",
                rs -> rs.next() ? rs.getString("id") : null,
                applicationId
        );
        assertThat(caseId).isNotBlank();

        Integer pendingFollowupCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM service_followup_record WHERE case_id = ? AND record_status = 'PENDING'",
                Integer.class,
                caseId
        );
        Integer scheduledFollowupCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM service_followup_record WHERE case_id = ? AND record_status = 'PENDING' AND due_at IS NOT NULL",
                Integer.class,
                caseId
        );
        assertThat(pendingFollowupCount).isEqualTo(2);
        assertThat(scheduledFollowupCount).isEqualTo(2);

        Map<String, Object> followupRequest = new HashMap<>();
        followupRequest.put("jobId", jobId);
        followupRequest.put("followupStage", "DAY_7");
        followupRequest.put("adaptationScore", 58);
        followupRequest.put("environmentIssue", "工位附近环境偏嘈杂");
        followupRequest.put("communicationIssue", "希望继续保留文字同步说明");
        followupRequest.put("supportImplemented", false);
        followupRequest.put("leaveRisk", true);
        followupRequest.put("needHelp", true);
        followupRequest.put("remark", "岗位内容能适应，但需要机构继续协助。");

        mockMvc.perform(post("/api/jobseeker/followup")
                        .header("Authorization", "Bearer " + jobseekerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(followupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.followupStage").value("DAY_7"))
                .andExpect(jsonPath("$.data.leaveRisk").value(true))
                .andExpect(jsonPath("$.data.needHelp").value(true));

        Integer employmentFollowupCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM employment_followup WHERE user_id = (SELECT user_id FROM job_application WHERE id = ?) AND job_id = ?",
                Integer.class,
                applicationId,
                jobId
        );
        String day7RecordStatus = jdbcTemplate.query(
                """
                        SELECT record_status
                        FROM service_followup_record
                        WHERE case_id = ?
                          AND job_id = ?
                          AND followup_stage = 'DAY_7'
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getString("record_status") : null,
                caseId,
                jobId
        );
        Integer openAlertCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM catalog_service_alert WHERE case_id = ? AND alert_status = 'OPEN'",
                Integer.class,
                caseId
        );
        Integer remainingPendingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM service_followup_record WHERE case_id = ? AND record_status = 'PENDING'",
                Integer.class,
                caseId
        );
        assertThat(employmentFollowupCount).isEqualTo(1);
        assertThat(day7RecordStatus).isEqualTo("COMPLETED");
        assertThat(openAlertCount).isEqualTo(1);
        assertThat(remainingPendingCount).isEqualTo(1);

        mockMvc.perform(get("/api/service/cases/{caseId}", caseId)
                        .header("Authorization", "Bearer " + serviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.followups.length()").value(2))
                .andExpect(jsonPath("$.data.alerts[0].alertStatus").value("OPEN"));

        mockMvc.perform(get("/api/jobseeker/service-records")
                        .header("Authorization", "Bearer " + jobseekerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(caseId));

        mockMvc.perform(get("/api/jobseeker/service-records/{caseId}", caseId)
                        .header("Authorization", "Bearer " + jobseekerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(caseId))
                .andExpect(jsonPath("$.data.followups.length()").value(2))
                .andExpect(jsonPath("$.data.alerts.length()").value(1));
    }

    private String createPublishedJob(String enterpriseToken, String title) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("title", title);
        request.put("department", "数据运营部");
        request.put("city", "南京");
        request.put("salaryMin", 6000);
        request.put("salaryMax", 8000);
        request.put("headcount", 1);
        request.put("description", "负责候选人台账与数据核对。\n配合招聘流程跟进。");
        request.put("requirementText", "具备结构化表达能力\n能够适应异步协作");
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

        MvcResult result = mockMvc.perform(post("/api/enterprise/jobs")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.publishStatus").value("PUBLISHED"))
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
        profileRequest.put("expectedJob", "数据运营助理");
        profileRequest.put("workModePreference", "HYBRID");
        profileRequest.put("intro", "偏好文字优先协作和结构化工作流。");

        mockMvc.perform(put("/api/jobseeker/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void saveSupportNeeds(String token) throws Exception {
        saveSupportNeeds(token, "SUMMARY");
    }

    private void saveSupportNeeds(String token, String supportVisibility) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("supportVisibility", supportVisibility);
        request.put("textCommunicationPreferred", true);
        request.put("subtitleNeeded", true);
        request.put("remoteInterviewPreferred", true);
        request.put("remark", "请提前发送书面材料。");

        mockMvc.perform(put("/api/jobseeker/support-needs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private long applyJob(String token, String jobId) throws Exception {
        Map<String, Object> applyRequest = new HashMap<>();
        applyRequest.put("coverNote", "我擅长结构化整理台账，也希望优先采用文字沟通的面试方式。");
        applyRequest.put("preferredInterviewMode", "TEXT");

        MvcResult result = mockMvc.perform(post("/api/jobseeker/apply/{jobId}", jobId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(applyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
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
        profileRequest.put("companyName", "Interview Flow Enterprise " + System.currentTimeMillis());
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
        String account = "enterprise_interview_" + System.currentTimeMillis() + "_" + accounts.size();
        accounts.add(account);

        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("account", account);
        registerRequest.put("password", "Pass@123456");
        registerRequest.put("nickname", "enterprise-interview");
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
