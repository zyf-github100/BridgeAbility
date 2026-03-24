package com.rongzhiqiao.common.exception;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ErrorMessageLocalizer {

    private static final Pattern REQUIRED_PATTERN = Pattern.compile("^([A-Za-z][A-Za-z0-9_.\\[\\] ]*) is required$");
    private static final Pattern INVALID_PATTERN = Pattern.compile("^([A-Za-z][A-Za-z0-9_.\\[\\] ]*) is invalid$");
    private static final Pattern LESS_THAN_PATTERN = Pattern.compile("^([A-Za-z][A-Za-z0-9_.\\[\\] ]*) must be less than (\\d+) characters$");
    private static final Pattern BETWEEN_CHARACTERS_PATTERN =
            Pattern.compile("^([A-Za-z][A-Za-z0-9_.\\[\\] ]*) must be between (\\d+) and (\\d+) characters$");
    private static final Pattern AT_LEAST_PATTERN = Pattern.compile("^([A-Za-z][A-Za-z0-9_.\\[\\] ]*) must be at least (\\d+)$");
    private static final Pattern AT_MOST_PATTERN = Pattern.compile("^([A-Za-z][A-Za-z0-9_.\\[\\] ]*) must be at most (\\d+)$");
    private static final Pattern BETWEEN_VALUES_PATTERN =
            Pattern.compile("^([A-Za-z][A-Za-z0-9_.\\[\\] ]*) must be between (\\d+) and (\\d+)$");
    private static final Pattern GREATER_THAN_PATTERN =
            Pattern.compile("^([A-Za-z][A-Za-z0-9_.\\[\\] ]*) cannot be greater than ([A-Za-z][A-Za-z0-9_.\\[\\] ]*)$");
    private static final Pattern CANNOT_EXCEED_PATTERN = Pattern.compile("^([A-Za-z][A-Za-z0-9_.\\[\\] ]*) cannot exceed (\\d+)$");
    private static final Pattern NEGATIVE_PATTERN = Pattern.compile("^([A-Za-z][A-Za-z0-9_.\\[\\] ]*) cannot be negative$");
    private static final Pattern OUT_OF_RANGE_PATTERN = Pattern.compile("^([A-Za-z][A-Za-z0-9_.\\[\\] ]*) is out of range$");

    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
            Map.entry("decision", "审核结果"),
            Map.entry("note", "备注"),
            Map.entry("targetStatus", "目标状态"),
            Map.entry("applicationId", "申请记录"),
            Map.entry("applicationStatus", "申请状态"),
            Map.entry("interviewTime", "面试时间"),
            Map.entry("interviewMode", "面试方式"),
            Map.entry("interviewerName", "面试官"),
            Map.entry("resultStatus", "面试结果"),
            Map.entry("feedbackNote", "反馈说明"),
            Map.entry("rejectReason", "未通过原因"),
            Map.entry("companyName", "企业名称"),
            Map.entry("industry", "所属行业"),
            Map.entry("city", "所在城市"),
            Map.entry("unifiedSocialCreditCode", "统一社会信用代码"),
            Map.entry("contactName", "联系人"),
            Map.entry("contactPhone", "联系电话"),
            Map.entry("officeAddress", "办公地址"),
            Map.entry("accessibilityCommitment", "无障碍承诺"),
            Map.entry("jobId", "岗位"),
            Map.entry("followupStage", "回访阶段"),
            Map.entry("adaptationScore", "适应评分"),
            Map.entry("environmentIssue", "环境问题"),
            Map.entry("communicationIssue", "沟通问题"),
            Map.entry("supportImplemented", "支持落实情况"),
            Map.entry("leaveRisk", "离职风险"),
            Map.entry("needHelp", "是否需要帮助"),
            Map.entry("remark", "备注"),
            Map.entry("coverNote", "投递说明"),
            Map.entry("preferredInterviewMode", "首选面试方式"),
            Map.entry("additionalSupport", "补充支持说明"),
            Map.entry("requestType", "申请类型"),
            Map.entry("requestContent", "申请内容"),
            Map.entry("projectName", "项目名称"),
            Map.entry("roleName", "担任角色"),
            Map.entry("description", "描述"),
            Map.entry("skillCode", "技能编码"),
            Map.entry("skillName", "技能名称"),
            Map.entry("skillLevel", "技能等级"),
            Map.entry("supportVisibility", "展示范围"),
            Map.entry("publishStatus", "发布状态"),
            Map.entry("salaryMin", "最低薪资"),
            Map.entry("salaryMax", "最高薪资"),
            Map.entry("deadline", "截止日期"),
            Map.entry("accessibilityTag", "无障碍标签"),
            Map.entry("tagCode", "标签编码"),
            Map.entry("tagStatus", "标签状态"),
            Map.entry("text", "文本内容"),
            Map.entry("targetRole", "目标角色"),
            Map.entry("severityLevel", "严重程度"),
            Map.entry("issueType", "问题类型"),
            Map.entry("referralId", "转介记录"),
            Map.entry("workMode", "工作方式"),
            Map.entry("followUpThreshold", "跟进阈值"),
            Map.entry("priorityThreshold", "优先阈值"),
            Map.entry("scoreWeights", "评分权重"),
            Map.entry("candidateStage", "候选人阶段"),
            Map.entry("birthYear", "出生年份"),
            Map.entry("graduationYear", "毕业年份"),
            Map.entry("expectedSalaryMin", "期望薪资下限"),
            Map.entry("expectedSalaryMax", "期望薪资上限"),
            Map.entry("skillTags", "技能标签"),
            Map.entry("projectExperiences", "项目经历")
    );

    private static final Map<String, String> EXACT_MESSAGES = Map.ofEntries(
            Map.entry("forbidden", "无权执行该操作"),
            Map.entry("job not found", "未找到岗位"),
            Map.entry("enterprise profile not found", "未找到企业资料"),
            Map.entry("service case not found", "未找到服务个案"),
            Map.entry("admin dashboard not found", "未找到管理首页数据"),
            Map.entry("matching status not found", "未找到匹配状态"),
            Map.entry("verification material file not found", "未找到认证材料文件"),
            Map.entry("material upload failed", "认证材料上传失败"),
            Map.entry("verification material file load failed", "认证材料文件加载失败"),
            Map.entry("unsupported storage provider", "不支持当前存储服务"),
            Map.entry("R2 storage is not configured", "R2 存储未配置"),
            Map.entry("matching config is invalid", "匹配配置不合法"),
            Map.entry("scoreWeights must contain at least one positive value", "评分权重至少需要包含一个大于 0 的值"),
            Map.entry("candidateStage weights must contain at least one positive value", "候选人阶段权重至少需要包含一个大于 0 的值"),
            Map.entry("followUpThreshold cannot be greater than priorityThreshold", "跟进阈值不能大于优先阈值"),
            Map.entry("tagCode already exists", "标签编码已存在"),
            Map.entry("tag not found", "未找到标签"),
            Map.entry("candidate application not found", "未找到候选人申请"),
            Map.entry("terminal application status cannot be changed", "终态申请记录不能修改"),
            Map.entry("notification not found", "未找到通知"),
            Map.entry("pending interview record not found", "未找到待处理的面试记录"),
            Map.entry("application status does not allow interview invite", "当前申请状态不允许发起面试邀约"),
            Map.entry("applicationStatus must be OFFERED or HIRED when resultStatus is PASS", "面试结果为通过时，申请状态只能为待录用或已录用"),
            Map.entry("rejectReason is required when resultStatus is FAIL", "面试结果为未通过时，必须填写未通过原因"),
            Map.entry("applicationStatus must be REJECTED when resultStatus is FAIL", "面试结果为未通过时，申请状态只能为未通过"),
            Map.entry("interviewTime is invalid", "面试时间格式不正确"),
            Map.entry("new job cannot start as offline", "新建岗位时不能直接设为已下线"),
            Map.entry("accessibilityTag must be completed before publishing", "发布前请先补齐无障碍标签"),
            Map.entry("material insert failed", "认证材料保存失败"),
            Map.entry("service case already exists for the linked jobseeker", "该求职者已关联服务个案"),
            Map.entry("linked jobseeker account is required when profile access is authorized", "开启档案授权时必须填写关联求职者账号"),
            Map.entry("this case is not linked to a jobseeker account", "当前个案未关联求职者账号"),
            Map.entry("knowledge article not found", "未找到知识库文章"),
            Map.entry("knowledge tags write failed", "知识标签写入失败"),
            Map.entry("issue not found", "未找到工单"),
            Map.entry("job has already been applied", "该岗位已投递"),
            Map.entry("only hired applications can submit followup", "只有已录用的申请可以提交入职反馈"),
            Map.entry("jobseeker profile not found", "未找到求职者档案"),
            Map.entry("service organization user not found", "未找到服务机构用户"),
            Map.entry("target user is not a service organization account", "目标用户不是服务机构账号"),
            Map.entry("user not found", "未找到用户"),
            Map.entry("birth year out of range", "出生年份超出允许范围"),
            Map.entry("graduation year out of range", "毕业年份超出允许范围"),
            Map.entry("expectedSalaryMin cannot be greater than expectedSalaryMax", "期望薪资下限不能大于上限"),
            Map.entry("skillTags contains duplicated skills", "技能标签存在重复项"),
            Map.entry("skillLevel must be between 1 and 5", "技能等级需在 1 到 5 之间"),
            Map.entry("project endDate cannot be earlier than startDate", "项目结束时间不能早于开始时间"),
            Map.entry("Failed to encrypt sensitive data", "敏感数据加密失败"),
            Map.entry("Sensitive data payload is invalid", "敏感数据内容不合法"),
            Map.entry("Failed to decrypt sensitive data", "敏感数据解密失败"),
            Map.entry("Failed to initialize sensitive data key", "敏感数据密钥初始化失败"),
            Map.entry("Failed to parse JSON column", "JSON 字段解析失败"),
            Map.entry("Failed to serialize JSON column", "JSON 字段序列化失败"),
            Map.entry("Failed to parse knowledge tags", "知识标签解析失败"),
            Map.entry("Failed to create interview record", "创建面试记录失败"),
            Map.entry("Failed to export resume PDF", "导出 PDF 简历失败"),
            Map.entry("Failed to export resume Word document", "导出 Word 简历失败"),
            Map.entry("Failed to parse application snapshot", "申请快照解析失败"),
            Map.entry("Failed to write JSON column", "JSON 字段写入失败"),
            Map.entry("Failed to serialize application snapshot", "申请快照序列化失败"),
            Map.entry("Failed to persist service authorization", "保存服务授权失败"),
            Map.entry("followup record not found", "未找到回访记录"),
            Map.entry("resource referral not found", "未找到资源转介记录"),
            Map.entry("alert not found", "未找到预警记录"),
            Map.entry("linked jobseeker account not found", "未找到关联求职者账号"),
            Map.entry("linked account is not a jobseeker", "关联账号不是求职者账号")
    );

    private ErrorMessageLocalizer() {
    }

    public static String localize(String message) {
        if (message == null || message.isBlank()) {
            return "系统异常，请稍后再试";
        }
        String normalized = message.trim();
        String exact = EXACT_MESSAGES.get(normalized);
        if (exact != null) {
            return exact;
        }

        String translated = translatePattern(normalized);
        if (translated != null) {
            return translated;
        }

        return containsAsciiLetter(normalized) ? "系统异常，请稍后再试" : normalized;
    }

    public static String localizeFieldError(String fieldName, String defaultMessage) {
        String localizedMessage = localize(defaultMessage);
        String fieldLabel = localizeFieldName(fieldName);
        if (localizedMessage.contains(fieldLabel)) {
            return localizedMessage;
        }
        if (!containsAsciiLetter(localizedMessage)) {
            return fieldLabel + "：" + localizedMessage;
        }
        return fieldLabel + "填写不正确";
    }

    public static String localizeFieldName(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return "参数";
        }
        String normalized = fieldName.replaceAll("\\[[0-9]+\\]", "");
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex >= 0) {
            normalized = normalized.substring(dotIndex + 1);
        }
        normalized = normalized.trim();
        return FIELD_LABELS.getOrDefault(normalized, normalized);
    }

    private static String translatePattern(String message) {
        Matcher matcher = REQUIRED_PATTERN.matcher(message);
        if (matcher.matches()) {
            return localizeFieldName(matcher.group(1)) + "不能为空";
        }

        matcher = INVALID_PATTERN.matcher(message);
        if (matcher.matches()) {
            return localizeFieldName(matcher.group(1)) + "不合法";
        }

        matcher = LESS_THAN_PATTERN.matcher(message);
        if (matcher.matches()) {
            return localizeFieldName(matcher.group(1)) + "不能超过" + matcher.group(2) + "个字符";
        }

        matcher = BETWEEN_CHARACTERS_PATTERN.matcher(message);
        if (matcher.matches()) {
            return localizeFieldName(matcher.group(1)) + "长度需在" + matcher.group(2) + "到" + matcher.group(3) + "个字符之间";
        }

        matcher = AT_LEAST_PATTERN.matcher(message);
        if (matcher.matches()) {
            return localizeFieldName(matcher.group(1)) + "不能小于" + matcher.group(2);
        }

        matcher = AT_MOST_PATTERN.matcher(message);
        if (matcher.matches()) {
            return localizeFieldName(matcher.group(1)) + "不能大于" + matcher.group(2);
        }

        matcher = BETWEEN_VALUES_PATTERN.matcher(message);
        if (matcher.matches()) {
            return localizeFieldName(matcher.group(1)) + "需在" + matcher.group(2) + "到" + matcher.group(3) + "之间";
        }

        matcher = GREATER_THAN_PATTERN.matcher(message);
        if (matcher.matches()) {
            return localizeFieldName(matcher.group(1)) + "不能大于" + localizeFieldName(matcher.group(2));
        }

        matcher = CANNOT_EXCEED_PATTERN.matcher(message);
        if (matcher.matches()) {
            return localizeFieldName(matcher.group(1)) + "不能超过" + matcher.group(2) + "项";
        }

        matcher = NEGATIVE_PATTERN.matcher(message);
        if (matcher.matches()) {
            return localizeFieldName(matcher.group(1)) + "不能为负数";
        }

        matcher = OUT_OF_RANGE_PATTERN.matcher(message);
        if (matcher.matches()) {
            return localizeFieldName(matcher.group(1)) + "超出允许范围";
        }

        return null;
    }

    private static boolean containsAsciiLetter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if ((current >= 'A' && current <= 'Z') || (current >= 'a' && current <= 'z')) {
                return true;
            }
        }
        return false;
    }
}
