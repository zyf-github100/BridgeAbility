package com.rongzhiqiao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
class ServiceOrgWorkflowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Set<String> accounts = new java.util.LinkedHashSet<>();
    private final Set<String> createdCaseIds = new java.util.LinkedHashSet<>();
    private final Set<Long> interventionIds = new java.util.LinkedHashSet<>();
    private final Set<Long> followupIds = new java.util.LinkedHashSet<>();
    private final Set<Long> referralIds = new java.util.LinkedHashSet<>();
    private final Set<String> alertIds = new java.util.LinkedHashSet<>();
    private final Map<String, CaseProgressSnapshot> caseSnapshots = new LinkedHashMap<>();

    @AfterEach
    void cleanup() {
        for (String alertId : alertIds) {
            jdbcTemplate.update("DELETE FROM catalog_service_alert WHERE alert_id = ?", alertId);
        }
        alertIds.clear();

        for (Long referralId : referralIds) {
            jdbcTemplate.update("DELETE FROM service_resource_referral WHERE id = ?", referralId);
        }
        referralIds.clear();

        for (Long followupId : followupIds) {
            jdbcTemplate.update("DELETE FROM service_followup_record WHERE id = ?", followupId);
        }
        followupIds.clear();

        for (Long interventionId : interventionIds) {
            jdbcTemplate.update("DELETE FROM service_case_intervention WHERE id = ?", interventionId);
        }
        interventionIds.clear();

        for (Map.Entry<String, CaseProgressSnapshot> entry : caseSnapshots.entrySet()) {
            jdbcTemplate.update(
                    """
                            UPDATE catalog_service_case
                            SET next_action = ?,
                                alert_level = ?
                            WHERE id = ?
                            """,
                    entry.getValue().nextAction(),
                    entry.getValue().alertLevel(),
                    entry.getKey()
            );
        }
        caseSnapshots.clear();

        for (String caseId : createdCaseIds) {
            jdbcTemplate.update("DELETE FROM service_resource_referral WHERE case_id = ?", caseId);
            jdbcTemplate.update("DELETE FROM service_case_intervention WHERE case_id = ?", caseId);
            jdbcTemplate.update("DELETE FROM service_followup_record WHERE case_id = ?", caseId);
            jdbcTemplate.update("DELETE FROM catalog_service_alert WHERE case_id = ?", caseId);
            jdbcTemplate.update("DELETE FROM catalog_service_case WHERE id = ?", caseId);
        }
        createdCaseIds.clear();

        for (String account : accounts) {
            Long userId = jdbcTemplate.query(
                    "SELECT id FROM sys_user WHERE account = ?",
                    rs -> rs.next() ? rs.getLong("id") : null,
                    account
            );
            if (userId != null) {
                jdbcTemplate.update("DELETE FROM jobseeker_service_authorization WHERE jobseeker_user_id = ? OR service_org_user_id = ?", userId, userId);
                jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", userId);
            }
        }
        accounts.clear();
    }

    @Test
    void addingInterventionShowsUpInCaseDetail() throws Exception {
        String serviceToken = registerAndGetToken("ROLE_SERVICE_ORG");
        String caseId = "case-2026-011";
        String marker = "svc-intervention-" + System.currentTimeMillis();
        snapshotCase(caseId);

        Map<String, Object> request = new HashMap<>();
        request.put("interventionType", "RESUME_GUIDANCE");
        request.put("content", "补充无障碍岗位关键词 " + marker);
        request.put("attachmentNote", "resume-note-" + marker);
        request.put("operatorName", marker);

        mockMvc.perform(post("/api/service/cases/{caseId}/interventions", caseId)
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(caseId))
                .andExpect(jsonPath("$.data.interventions[0].interventionType").value("RESUME_GUIDANCE"))
                .andExpect(jsonPath("$.data.interventions[0].operatorName").value(marker))
                .andExpect(jsonPath("$.data.interventions[0].content").value("补充无障碍岗位关键词 " + marker));

        Long interventionId = jdbcTemplate.queryForObject(
                """
                        SELECT id
                        FROM service_case_intervention
                        WHERE case_id = ?
                          AND operator_name = ?
                          AND content = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                Long.class,
                caseId,
                marker,
                "补充无障碍岗位关键词 " + marker
        );
        if (interventionId != null) {
            interventionIds.add(interventionId);
        }

        mockMvc.perform(get("/api/service/cases/{caseId}", caseId)
                        .header("Authorization", "Bearer " + serviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.interventions[0].operatorName").value(marker));
    }

    @Test
    void riskyFollowupCreatesAlertThatCanBeResolved() throws Exception {
        String serviceToken = registerAndGetToken("ROLE_SERVICE_ORG");
        String caseId = "case-2026-014";
        String marker = "svc-followup-" + System.currentTimeMillis();
        snapshotCase(caseId);

        Map<String, Object> followupRequest = new HashMap<>();
        followupRequest.put("jobId", "job-risk-" + marker);
        followupRequest.put("followupStage", "DAY_30");
        followupRequest.put("adaptationScore", 42);
        followupRequest.put("environmentIssue", "workspace issue " + marker);
        followupRequest.put("communicationIssue", "communication issue " + marker);
        followupRequest.put("supportImplemented", false);
        followupRequest.put("leaveRisk", true);
        followupRequest.put("needHelp", true);
        followupRequest.put("operatorName", marker);

        mockMvc.perform(post("/api/service/cases/{caseId}/followups", caseId)
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(followupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.followups[0].followupStage").value("DAY_30"))
                .andExpect(jsonPath("$.data.followups[0].leaveRisk").value(true))
                .andExpect(jsonPath("$.data.alerts[0].alertStatus").value("OPEN"));

        Long followupId = jdbcTemplate.queryForObject(
                """
                        SELECT id
                        FROM service_followup_record
                        WHERE case_id = ?
                          AND operator_name = ?
                          AND environment_issue = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                Long.class,
                caseId,
                marker,
                "workspace issue " + marker
        );
        if (followupId != null) {
            followupIds.add(followupId);
        }

        String alertId = jdbcTemplate.query(
                """
                        SELECT alert_id
                        FROM catalog_service_alert
                        WHERE case_id = ?
                          AND trigger_reason LIKE ?
                        ORDER BY created_at DESC, alert_id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getString("alert_id") : null,
                caseId,
                "%" + marker + "%"
        );
        if (alertId != null) {
            alertIds.add(alertId);
        }

        Map<String, Object> statusRequest = new HashMap<>();
        statusRequest.put("targetStatus", "RESOLVED");
        statusRequest.put("resolutionNote", "已协调企业补齐支持资源 " + marker);
        statusRequest.put("operatorName", marker);

        mockMvc.perform(post("/api/service/alerts/{alertId}/status", alertId)
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.alertId").value(alertId))
                .andExpect(jsonPath("$.data.alertStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolutionNote").value("已协调企业补齐支持资源 " + marker))
                .andExpect(jsonPath("$.data.handledBy").value(marker));
    }

    @Test
    void serviceOrgCanCreateManualCaseAuthorizeProfileAndManageReferralFlow() throws Exception {
        AuthSession serviceSession = registerAccountAndGetToken("ROLE_SERVICE_ORG");
        AuthSession jobseekerSession = registerAccountAndGetToken("ROLE_JOBSEEKER");
        String marker = "svc-case-" + System.currentTimeMillis();

        saveJobseekerProfile(jobseekerSession.token(), "Profile User " + marker);
        saveSupportNeeds(jobseekerSession.token());

        Map<String, Object> createRequest = new LinkedHashMap<>();
        createRequest.put("name", "Manual Case " + marker);
        createRequest.put("stage", "INTAKE");
        createRequest.put("ownerName", "Service Owner " + marker);
        createRequest.put("nextAction", "Arrange referral");
        createRequest.put("jobseekerAccount", jobseekerSession.account());
        createRequest.put("intakeNote", "manual case note " + marker);
        createRequest.put("profileAuthorized", true);
        createRequest.put("authorizationNote", "authorized for support services " + marker);
        createRequest.put("operatorName", "Service Owner " + marker);

        MvcResult createResult = mockMvc.perform(post("/api/service/cases")
                        .header("Authorization", "Bearer " + serviceSession.token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("Manual Case " + marker))
                .andExpect(jsonPath("$.data.profileAccess.linkedJobseeker").value(true))
                .andExpect(jsonPath("$.data.profileAccess.profileAuthorized").value(true))
                .andExpect(jsonPath("$.data.profileAccess.linkedAccount").value(jobseekerSession.account()))
                .andExpect(jsonPath("$.data.profileAccess.jobseekerProfile.realName").value("Profile User " + marker))
                .andExpect(jsonPath("$.data.profileAccess.supportNeeds.hasAnyNeed").value(true))
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String caseId = createJson.path("data").path("id").asText();
        createdCaseIds.add(caseId);

        Long jobseekerUserId = findUserIdByAccount(jobseekerSession.account());

        mockMvc.perform(get("/api/service/jobseekers/{userId}/profile", jobseekerUserId)
                        .header("Authorization", "Bearer " + serviceSession.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.realName").value("Profile User " + marker))
                .andExpect(jsonPath("$.data.targetCity").value("Nanjing"));

        Map<String, Object> referralRequest = new LinkedHashMap<>();
        referralRequest.put("referralType", "TRAINING");
        referralRequest.put("resourceName", "Training Program " + marker);
        referralRequest.put("providerName", "Support Center");
        referralRequest.put("contactName", "Coach Wang");
        referralRequest.put("contactPhone", "13800001111");
        referralRequest.put("scheduledAt", "2026-04-08 14:00:00");
        referralRequest.put("statusNote", "first appointment " + marker);
        referralRequest.put("operatorName", "Service Owner " + marker);

        mockMvc.perform(post("/api/service/cases/{caseId}/referrals", caseId)
                        .header("Authorization", "Bearer " + serviceSession.token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(referralRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.referrals[0].referralType").value("TRAINING"))
                .andExpect(jsonPath("$.data.referrals[0].resourceName").value("Training Program " + marker))
                .andExpect(jsonPath("$.data.referrals[0].referralStatus").value("PLANNED"));

        Long referralId = jdbcTemplate.query(
                """
                        SELECT id
                        FROM service_resource_referral
                        WHERE case_id = ?
                          AND resource_name = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                caseId,
                "Training Program " + marker
        );
        if (referralId != null) {
            referralIds.add(referralId);
        }

        Map<String, Object> statusRequest = new LinkedHashMap<>();
        statusRequest.put("targetStatus", "COMPLETED");
        statusRequest.put("statusNote", "referral completed " + marker);
        statusRequest.put("operatorName", "Service Owner " + marker);

        mockMvc.perform(post("/api/service/referrals/{referralId}/status", referralId)
                        .header("Authorization", "Bearer " + serviceSession.token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.referrals[0].referralStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.referrals[0].statusNote").value("referral completed " + marker));
    }

    private void snapshotCase(String caseId) {
        if (caseSnapshots.containsKey(caseId)) {
            return;
        }

        CaseProgressSnapshot snapshot = jdbcTemplate.query(
                """
                        SELECT next_action, alert_level
                        FROM catalog_service_case
                        WHERE id = ?
                        """,
                rs -> rs.next() ? new CaseProgressSnapshot(rs.getString("next_action"), rs.getString("alert_level")) : null,
                caseId
        );
        if (snapshot != null) {
            caseSnapshots.put(caseId, snapshot);
        }
    }

    private void saveJobseekerProfile(String token, String realName) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("realName", realName);
        request.put("schoolName", "BridgeAbility University");
        request.put("major", "Information Management");
        request.put("targetCity", "Nanjing");
        request.put("expectedJob", "Data Assistant");
        request.put("workModePreference", "HYBRID");
        request.put("intro", "Prefer structured written communication.");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/jobseeker/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void saveSupportNeeds(String token) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("supportVisibility", "SUMMARY");
        request.put("textCommunicationPreferred", true);
        request.put("subtitleNeeded", true);
        request.put("remoteInterviewPreferred", true);
        request.put("keyboardOnlyMode", false);
        request.put("highContrastNeeded", false);
        request.put("largeFontNeeded", false);
        request.put("flexibleScheduleNeeded", true);
        request.put("accessibleWorkspaceNeeded", true);
        request.put("assistiveSoftwareNeeded", false);
        request.put("remark", "Provide written instructions in advance.");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/jobseeker/support-needs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private Long findUserIdByAccount(String account) {
        return jdbcTemplate.query(
                "SELECT id FROM sys_user WHERE account = ?",
                rs -> rs.next() ? rs.getLong("id") : null,
                account
        );
    }

    private AuthSession registerAccountAndGetToken(String role) throws Exception {
        String account = "service_flow_" + System.currentTimeMillis() + "_" + accounts.size();
        accounts.add(account);

        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("account", account);
        registerRequest.put("password", "Pass@123456");
        registerRequest.put("nickname", "service-flow");
        registerRequest.put("roles", java.util.List.of(role));

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode json = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        return new AuthSession(account, json.path("data").path("accessToken").asText());
    }

    private String registerAndGetToken(String role) throws Exception {
        return registerAccountAndGetToken(role).token();
    }

    private record CaseProgressSnapshot(String nextAction, String alertLevel) {
    }

    private record AuthSession(String account, String token) {
    }
}
