package com.rongzhiqiao.enterprise.service;

import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.enterprise.repository.EnterpriseStatsRepository;
import com.rongzhiqiao.enterprise.repository.EnterpriseStatsRepository.ApplicationStatsRow;
import com.rongzhiqiao.enterprise.repository.EnterpriseStatsRepository.JobStatsRow;
import com.rongzhiqiao.enterprise.vo.EnterpriseRecruitmentStatsResponse;
import com.rongzhiqiao.enterprise.vo.EnterpriseStatsBucketResponse;
import com.rongzhiqiao.enterprise.vo.EnterpriseStatsJobResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnterpriseRecruitmentStatsService {

    private final EnterpriseStatsRepository enterpriseStatsRepository;

    public EnterpriseRecruitmentStatsResponse getCurrentStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<JobStatsRow> jobs = enterpriseStatsRepository.listVisibleJobs(userId);
        ApplicationStatsRow applicationStats = enterpriseStatsRepository.getVisibleApplicationStats(userId);

        long publishedJobs = jobs.stream().filter(job -> "PUBLISHED".equals(job.publishStatus())).count();
        long draftJobs = jobs.stream().filter(job -> "DRAFT".equals(job.publishStatus())).count();
        long offlineJobs = jobs.stream().filter(job -> "OFFLINE".equals(job.publishStatus())).count();
        int averageAccessibilityCompletionRate = jobs.isEmpty()
                ? 0
                : Math.toIntExact(Math.round(jobs.stream()
                        .mapToInt(JobStatsRow::accessibilityCompletionRate)
                        .average()
                        .orElse(0)));

        List<EnterpriseStatsJobResponse> topJobs = jobs.stream()
                .limit(6)
                .map(job -> new EnterpriseStatsJobResponse(
                        job.jobId(),
                        job.title(),
                        job.publishStatus(),
                        job.stage(),
                        job.accessibilityCompletionRate(),
                        job.candidateCount(),
                        job.interviewingCount(),
                        job.hiredCount(),
                        job.averageMatchScore()
                ))
                .toList();

        return new EnterpriseRecruitmentStatsResponse(
                jobs.size(),
                publishedJobs,
                draftJobs,
                offlineJobs,
                applicationStats.totalApplications(),
                applicationStats.appliedCount(),
                applicationStats.interviewingCount(),
                applicationStats.offeredCount(),
                applicationStats.hiredCount(),
                applicationStats.rejectedCount(),
                applicationStats.consentGrantedCount(),
                applicationStats.averageMatchScore(),
                averageAccessibilityCompletionRate,
                buildPublishStatusBreakdown(publishedJobs, draftJobs, offlineJobs),
                buildApplicationStatusBreakdown(applicationStats),
                topJobs,
                buildInsights(jobs, applicationStats, averageAccessibilityCompletionRate, draftJobs)
        );
    }

    private List<EnterpriseStatsBucketResponse> buildPublishStatusBreakdown(long publishedJobs,
                                                                            long draftJobs,
                                                                            long offlineJobs) {
        return List.of(
                new EnterpriseStatsBucketResponse("PUBLISHED", "已发布岗位", publishedJobs, "当前对求职者可见的岗位数量"),
                new EnterpriseStatsBucketResponse("DRAFT", "草稿岗位", draftJobs, "仍需补齐后再发布的岗位"),
                new EnterpriseStatsBucketResponse("OFFLINE", "已下线岗位", offlineJobs, "已停止对外展示但仍保留记录")
        );
    }

    private List<EnterpriseStatsBucketResponse> buildApplicationStatusBreakdown(ApplicationStatsRow applicationStats) {
        return List.of(
                new EnterpriseStatsBucketResponse("APPLIED", "待处理投递", applicationStats.appliedCount(), "尚未推进到面试的候选人"),
                new EnterpriseStatsBucketResponse("INTERVIEWING", "面试中", applicationStats.interviewingCount(), "正在安排或进行面试的候选人"),
                new EnterpriseStatsBucketResponse("OFFERED", "待录用", applicationStats.offeredCount(), "已通过面试但仍待确认入职"),
                new EnterpriseStatsBucketResponse("HIRED", "已录用", applicationStats.hiredCount(), "已经完成录用的候选人"),
                new EnterpriseStatsBucketResponse("REJECTED", "未通过", applicationStats.rejectedCount(), "已归档的未通过记录")
        );
    }

    private List<String> buildInsights(List<JobStatsRow> jobs,
                                       ApplicationStatsRow applicationStats,
                                       int averageAccessibilityCompletionRate,
                                       long draftJobs) {
        List<String> insights = new ArrayList<>();

        if (jobs.isEmpty()) {
            insights.add("当前还没有可统计的岗位，建议先完善岗位并发布。");
            return List.copyOf(insights);
        }
        if (draftJobs > 0) {
            insights.add("仍有 " + draftJobs + " 个岗位处于草稿状态，补齐无障碍标注后可继续发布。");
        }
        if (averageAccessibilityCompletionRate < 100) {
            insights.add("岗位无障碍标注平均完成度为 " + averageAccessibilityCompletionRate + "%，仍有继续补齐空间。");
        }
        if (applicationStats.totalApplications() == 0) {
            insights.add("当前还没有收到投递，建议优先优化岗位说明和发布节奏。");
        } else {
            insights.add("当前累计收到 " + applicationStats.totalApplications() + " 份投递，其中 "
                    + applicationStats.interviewingCount() + " 份处于面试推进阶段。");
        }
        if (applicationStats.consentGrantedCount() < applicationStats.totalApplications()) {
            insights.add("有 " + applicationStats.consentGrantedCount() + " 位候选人授权展示便利需求摘要，可优先安排适配沟通。");
        }
        if (insights.isEmpty()) {
            insights.add("岗位发布、候选人推进和无障碍标注整体较稳定，可继续关注录用转化。");
        }
        return List.copyOf(insights);
    }
}
