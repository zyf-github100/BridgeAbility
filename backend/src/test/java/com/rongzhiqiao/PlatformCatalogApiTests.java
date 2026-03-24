package com.rongzhiqiao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.storage.provider=local")
@AutoConfigureMockMvc
class PlatformCatalogApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Set<String> accounts = new java.util.LinkedHashSet<>();
    private final Set<String> knowledgeArticleIds = new java.util.LinkedHashSet<>();
    private final Set<String> notificationIds = new java.util.LinkedHashSet<>();
    private final Set<String> tagCodes = new java.util.LinkedHashSet<>();
    private final Set<String> issueIds = new java.util.LinkedHashSet<>();
    private final Set<String> jobIds = new java.util.LinkedHashSet<>();

    @AfterEach
    void cleanup() {
        for (String articleId : knowledgeArticleIds) {
            jdbcTemplate.update("DELETE FROM knowledge_article WHERE id = ?", articleId);
        }
        knowledgeArticleIds.clear();

        for (String notificationId : notificationIds) {
            jdbcTemplate.update("DELETE FROM notification_read_log WHERE notification_id = ?", notificationId);
            jdbcTemplate.update("DELETE FROM notification_message WHERE id = ?", notificationId);
        }
        notificationIds.clear();

        for (String tagCode : tagCodes) {
            jdbcTemplate.update("DELETE FROM tag_dictionary WHERE tag_code = ?", tagCode);
        }
        tagCodes.clear();

        for (String issueId : issueIds) {
            jdbcTemplate.update("DELETE FROM admin_issue_ticket WHERE id = ?", issueId);
        }
        issueIds.clear();

        for (String jobId : jobIds) {
            jdbcTemplate.update("DELETE FROM enterprise_job_posting WHERE job_id = ?", jobId);
            jdbcTemplate.update("DELETE FROM catalog_job WHERE id = ?", jobId);
        }
        jobIds.clear();

        jdbcTemplate.update(
                """
                        UPDATE catalog_service_alert
                        SET alert_status = 'OPEN',
                            resolution_note = NULL,
                            handled_by = NULL,
                            handled_at = NULL
                        WHERE alert_id IN ('alert-9001', 'alert-9002', 'alert-9003')
                        """
        );
        jdbcTemplate.update(
                """
                        UPDATE admin_issue_ticket
                        SET ticket_status = CASE id
                                WHEN 'issue-appeal-1001' THEN 'PENDING'
                                WHEN 'issue-correction-1002' THEN 'IN_PROGRESS'
                                WHEN 'issue-appeal-1003' THEN 'PENDING'
                                ELSE ticket_status
                            END,
                            resolution_note = CASE id
                                WHEN 'issue-correction-1002' THEN '已通知值班管理员核对原始回访记录。'
                                ELSE NULL
                            END,
                            handled_by = CASE id
                                WHEN 'issue-correction-1002' THEN '平台值班台'
                                ELSE NULL
                            END,
                            handled_at = CASE id
                                WHEN 'issue-correction-1002' THEN '2026-03-21 17:20:00'
                                ELSE NULL
                            END
                        WHERE id IN ('issue-appeal-1001', 'issue-correction-1002', 'issue-appeal-1003')
                        """
        );

        for (String account : accounts) {
            Long userId = jdbcTemplate.query(
                    "SELECT id FROM sys_user WHERE account = ?",
                    rs -> rs.next() ? rs.getLong("id") : null,
                    account
            );
            if (userId != null) {
                jdbcTemplate.update("DELETE FROM jobseeker_service_authorization WHERE jobseeker_user_id = ? OR service_org_user_id = ?", userId, userId);
                jdbcTemplate.update("DELETE FROM jobseeker_sensitive_info WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM notification_read_log WHERE user_id = ?", userId);
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
    void jobseekerCanBrowseRecommendedJobsAndApply() throws Exception {
        String token = registerAndGetToken(List.of("ROLE_JOBSEEKER"));
        saveSupportNeeds(token);

        mockMvc.perform(get("/api/jobseeker/recommend-jobs")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "数据"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value("data-ops-assistant"))
                .andExpect(jsonPath("$.data.list[0].dimensionScores[0].label").value("skill"));

        mockMvc.perform(get("/api/jobseeker/recommend-jobs/data-ops-assistant")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.company").value("宁行数科"))
                .andExpect(jsonPath("$.data.description[0]").isNotEmpty());

        Map<String, Object> applyRequest = new HashMap<>();
        applyRequest.put("coverNote", "I can handle structured data tasks well and prefer a text-first interview process.");
        applyRequest.put("preferredInterviewMode", "TEXT");

        mockMvc.perform(post("/api/jobseeker/apply/data-ops-assistant")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(applyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andExpect(jsonPath("$.data.jobTitle").value("数据运营助理"))
                .andExpect(jsonPath("$.data.preferredInterviewMode").value("TEXT"))
                .andExpect(jsonPath("$.data.supportVisibility").value("SUMMARY"));

        mockMvc.perform(get("/api/jobseeker/applications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].jobId").value("data-ops-assistant"))
                .andExpect(jsonPath("$.data[0].consentToShareSupportNeed").value(true))
                .andExpect(jsonPath("$.data[0].coverNote").value(applyRequest.get("coverNote")))
                .andExpect(jsonPath("$.data[0].preferredInterviewMode").value("TEXT"));
    }

    @Disabled("Enterprise candidate access now requires enterprise-owned jobs and masked profile fields; covered by PrivacyControlsIntegrationTests.")
    @Test
    void platformCatalogEndpointsExposeStructuredData() throws Exception {
        String adminToken = registerAndGetToken(List.of("ROLE_ADMIN"));
        String enterpriseToken = registerAndGetToken(List.of("ROLE_ENTERPRISE"));
        String serviceToken = registerAndGetToken(List.of("ROLE_SERVICE_ORG"));
        String jobseekerToken = registerAndGetToken(List.of("ROLE_JOBSEEKER"));

        Map<String, Object> profileRequest = new HashMap<>();
        profileRequest.put("realName", "企业候选人");
        profileRequest.put("schoolName", "融职桥大学");
        profileRequest.put("major", "软件工程");
        profileRequest.put("targetCity", "南京");
        profileRequest.put("expectedJob", "数据运营助理");
        profileRequest.put("workModePreference", "HYBRID");
        profileRequest.put("intro", "偏好文字优先协作和结构化工作流。");

        mockMvc.perform(put("/api/jobseeker/profile")
                        .header("Authorization", "Bearer " + jobseekerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        saveSupportNeeds(jobseekerToken);

        Map<String, Object> applyRequest = new HashMap<>();
        applyRequest.put("coverNote", "我能稳定处理数据台账，也希望采用文字优先的面试方式。");
        applyRequest.put("preferredInterviewMode", "TEXT");

        mockMvc.perform(post("/api/jobseeker/apply/data-ops-assistant")
                        .header("Authorization", "Bearer " + jobseekerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(applyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/enterprise/jobs/data-ops-assistant/candidates")
                        .header("Authorization", "Bearer " + enterpriseToken)
                        .param("consentGranted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[*].candidateName", hasItem("企业候选人")))
                .andExpect(jsonPath("$.data.list[?(@.candidateName=='企业候选人')].preferredInterviewMode", hasItem("TEXT")));

        mockMvc.perform(get("/api/service/cases")
                        .header("Authorization", "Bearer " + serviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value("case-2026-011"));

        mockMvc.perform(get("/api/service/alerts")
                        .header("Authorization", "Bearer " + serviceToken)
                        .param("level", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].alertId").value("alert-9003"));

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.jobseekerCount").value(128))
                .andExpect(jsonPath("$.data.reviewQueue[*].company", hasItem("桥群基金会")))
                .andExpect(jsonPath("$.data.auditLogs[*]", hasItem("2026-03-21 10:03 管理员复核了宁行数科的认证材料")));

        mockMvc.perform(get("/api/admin/enterprises/pending")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].company").isNotEmpty());

        mockMvc.perform(get("/api/knowledge/articles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value("knowledge-101"));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("read", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/matching/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.mode").value("可解释匹配"))
                .andExpect(jsonPath("$.data.availableJobCount").value(3));
    }

    @Test
    void adminCanManageKnowledgeArticles() throws Exception {
        String adminToken = registerAndGetToken(List.of("ROLE_ADMIN"));

        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("title", "管理端知识库测试文章");
        createRequest.put("category", "政策管理");
        createRequest.put("summary", "用于验证新增、编辑、发布和下线流程。");
        createRequest.put("content", "第一段：这是测试文章。\n第二段：用于验证管理动作。");
        createRequest.put("tags", List.of("测试", "知识库"));

        MvcResult createResult = mockMvc.perform(post("/api/admin/knowledge/articles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.publishStatus").value("OFFLINE"))
                .andReturn();

        String articleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText();
        knowledgeArticleIds.add(articleId);

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("title", "管理端知识库测试文章（已更新）");
        updateRequest.put("category", "政策管理");
        updateRequest.put("summary", "更新后的摘要。");
        updateRequest.put("content", "更新后的内容。");
        updateRequest.put("tags", List.of("测试", "更新"));

        mockMvc.perform(put("/api/admin/knowledge/articles/{articleId}", articleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("管理端知识库测试文章（已更新）"));

        mockMvc.perform(post("/api/admin/knowledge/articles/{articleId}/publish", articleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.publishStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());

        mockMvc.perform(get("/api/knowledge/articles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].id", hasItem(articleId)));

        mockMvc.perform(post("/api/admin/knowledge/articles/{articleId}/offline", articleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.publishStatus").value("OFFLINE"));
    }

    @Test
    void notificationsSupportRoleFilteringReadAndAnnouncements() throws Exception {
        String adminToken = registerAndGetToken(List.of("ROLE_ADMIN"));
        String jobseekerToken = registerAndGetToken(List.of("ROLE_JOBSEEKER"));
        String serviceToken = registerAndGetToken(List.of("ROLE_SERVICE_ORG"));

        MvcResult publishResult = mockMvc.perform(post("/api/admin/notifications")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "求职者公告测试",
                                "content", "仅面向求职者的系统公告。",
                                "targetRole", "ROLE_JOBSEEKER"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.targetRole").value("ROLE_JOBSEEKER"))
                .andReturn();

        String notificationId = objectMapper.readTree(publishResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText();
        notificationIds.add(notificationId);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + jobseekerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].id", hasItem(notificationId)));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + serviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.id=='" + notificationId + "')]").isEmpty());

        mockMvc.perform(post("/api/notifications/{notificationId}/read", notificationId)
                        .header("Authorization", "Bearer " + jobseekerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + jobseekerToken)
                        .param("read", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].id", hasItem(notificationId)));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("targetRole", "ROLE_JOBSEEKER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].id", hasItem(notificationId)));
    }

    @Test
    void adminCanManageTagDictionary() throws Exception {
        String adminToken = registerAndGetToken(List.of("ROLE_ADMIN"));
        String tagCode = "tag-" + System.currentTimeMillis();
        tagCodes.add(tagCode);

        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("tagCode", tagCode);
        createRequest.put("tagName", "测试标签");
        createRequest.put("tagCategory", "ACCESSIBILITY");
        createRequest.put("tagStatus", "ACTIVE");
        createRequest.put("description", "用于验证标签字典新增与修改流程。");

        MvcResult createResult = mockMvc.perform(post("/api/admin/tags")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.tagCode").value(tagCode))
                .andReturn();

        long tagId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("tagCode", tagCode);
        updateRequest.put("tagName", "测试标签（已停用）");
        updateRequest.put("tagCategory", "ACCESSIBILITY");
        updateRequest.put("tagStatus", "INACTIVE");
        updateRequest.put("description", "用于验证标签字典更新流程。");

        mockMvc.perform(put("/api/admin/tags/{tagId}", tagId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.tagStatus").value("INACTIVE"))
                .andExpect(jsonPath("$.data.tagName").value("测试标签（已停用）"));

        mockMvc.perform(get("/api/admin/tags")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.tagCode=='" + tagCode + "')].tagStatus", hasItem("INACTIVE")));
    }

    @Test
    void adminCanManageRiskRecords() throws Exception {
        String adminToken = registerAndGetToken(List.of("ROLE_ADMIN"));

        mockMvc.perform(get("/api/admin/risk-records")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "OPEN")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[*].alertId", hasItem("alert-9003")));

        mockMvc.perform(post("/api/admin/risk-records/{alertId}/status", "alert-9003")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetStatus", "RESOLVED",
                                "resolutionNote", "管理员已完成复核并同步处置方案。",
                                "operatorName", "admin-risk"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.alertStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.data.handledBy").value("admin-risk"));
    }

    @Test
    void usersCanReportIssuesAndAdminsCanProcessThem() throws Exception {
        String adminToken = registerAndGetToken(List.of("ROLE_ADMIN"));
        String jobseekerToken = registerAndGetToken(List.of("ROLE_JOBSEEKER"));

        MvcResult reportResult = mockMvc.perform(post("/api/issues/report")
                        .header("Authorization", "Bearer " + jobseekerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "issueType", "APPEAL",
                                "title", "测试申诉工单",
                                "content", "需要管理员复核测试申诉记录。",
                                "relatedType", "JOB_APPLICATION",
                                "relatedId", "data-ops-assistant",
                                "severityLevel", 3
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.issueType").value("APPEAL"))
                .andExpect(jsonPath("$.data.sourceRole").value("ROLE_JOBSEEKER"))
                .andExpect(jsonPath("$.data.ticketStatus").value("PENDING"))
                .andReturn();

        String issueId = objectMapper.readTree(reportResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText();
        issueIds.add(issueId);

        mockMvc.perform(get("/api/admin/issues")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].id", hasItem(issueId)));

        mockMvc.perform(post("/api/admin/issues/{issueId}/status", issueId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetStatus", "RESOLVED",
                                "resolutionNote", "管理员已复核并关闭测试工单。",
                                "handlerName", "admin-issue"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ticketStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.data.handledBy").value("admin-issue"));
    }

    private String registerAndGetToken(List<String> roles) throws Exception {
        String account = "catalog_" + System.currentTimeMillis() + "_" + accounts.size();
        accounts.add(account);

        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("account", account);
        registerRequest.put("password", "Pass@123456");
        registerRequest.put("nickname", "catalog-user");
        registerRequest.put("roles", roles);

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        return registerJson.path("data").path("accessToken").asText();
    }

    private void saveSupportNeeds(String token) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("supportVisibility", "SUMMARY");
        request.put("textCommunicationPreferred", true);
        request.put("subtitleNeeded", true);
        request.put("remoteInterviewPreferred", true);
        request.put("remark", "Please share written instructions before the interview.");

        mockMvc.perform(put("/api/jobseeker/support-needs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
