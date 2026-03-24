import { createRouter, createWebHistory } from 'vue-router'

import { pinia } from '../stores'
import { useAuthStore } from '../stores/auth'
import AdminAuditLogsView from '../views/AdminAuditLogsView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'
import AdminDataStatsView from '../views/AdminDataStatsView.vue'
import AdminEnterpriseReviewView from '../views/AdminEnterpriseReviewView.vue'
import AdminMatchingEngineView from '../views/AdminMatchingEngineView.vue'
import AdminIssueQueueView from '../views/AdminIssueQueueView.vue'
import AdminRiskRecordsView from '../views/AdminRiskRecordsView.vue'
import AdminUserManagementView from '../views/AdminUserManagementView.vue'
import AccessibilitySettingsView from '../views/AccessibilitySettingsView.vue'
import ApplicationRecordsView from '../views/ApplicationRecordsView.vue'
import CandidatePipelineView from '../views/CandidatePipelineView.vue'
import EnterpriseConsoleView from '../views/EnterpriseConsoleView.vue'
import EnterpriseCandidateDetailView from '../views/EnterpriseCandidateDetailView.vue'
import EnterpriseInterviewManagementView from '../views/EnterpriseInterviewManagementView.vue'
import EnterpriseJobDetailView from '../views/EnterpriseJobDetailView.vue'
import EnterpriseJobPublishView from '../views/EnterpriseJobPublishView.vue'
import EnterpriseJobsView from '../views/EnterpriseJobsView.vue'
import EnterpriseStatsView from '../views/EnterpriseStatsView.vue'
import EnterpriseVerificationView from '../views/EnterpriseVerificationView.vue'
import ForgotPasswordView from '../views/ForgotPasswordView.vue'
import HelpCenterView from '../views/HelpCenterView.vue'
import HomeView from '../views/HomeView.vue'
import JobseekerAbilityView from '../views/JobseekerAbilityView.vue'
import JobseekerInterviewAssistantView from '../views/JobseekerInterviewAssistantView.vue'
import JobseekerProfileView from '../views/JobseekerProfileView.vue'
import JobDetailView from '../views/JobDetailView.vue'
import JobListView from '../views/JobListView.vue'
import OnboardingFeedbackView from '../views/OnboardingFeedbackView.vue'
import JobseekerServiceRecordsView from '../views/JobseekerServiceRecordsView.vue'
import JobseekerSupportNeedsView from '../views/JobseekerSupportNeedsView.vue'
import JobseekerWorkspaceView from '../views/JobseekerWorkspaceView.vue'
import KnowledgeAdminView from '../views/KnowledgeAdminView.vue'
import LoginView from '../views/LoginView.vue'
import NotificationCenterView from '../views/NotificationCenterView.vue'
import ResumePreviewView from '../views/ResumePreviewView.vue'
import ServiceAlertsView from '../views/ServiceAlertsView.vue'
import ServiceCaseDetailView from '../views/ServiceCaseDetailView.vue'
import ServiceFollowupManagementView from '../views/ServiceFollowupManagementView.vue'
import ServiceInterventionRecordsView from '../views/ServiceInterventionRecordsView.vue'
import ServiceCasesView from '../views/ServiceCasesView.vue'
import ServiceResourceReferralView from '../views/ServiceResourceReferralView.vue'
import ServiceWorkbenchView from '../views/ServiceWorkbenchView.vue'
import TagDictionaryAdminView from '../views/TagDictionaryAdminView.vue'

type RoleCode = 'ROLE_JOBSEEKER' | 'ROLE_ENTERPRISE' | 'ROLE_SERVICE_ORG' | 'ROLE_ADMIN'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
  routes: [
    {
      path: '/',
      redirect: '/overview',
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: {
        layout: 'auth',
        title: '登录',
      },
    },
    {
      path: '/register',
      name: 'register',
      component: LoginView,
      meta: {
        layout: 'auth',
        title: '注册',
        authMode: 'register',
      },
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: ForgotPasswordView,
      meta: {
        layout: 'auth',
        title: '忘记密码',
      },
    },
    {
      path: '/overview',
      name: 'home',
      component: HomeView,
      meta: {
        section: 'home',
        title: '首页',
      },
    },
    {
      path: '/help-center',
      name: 'help-center',
      component: HelpCenterView,
      meta: {
        section: 'home',
        title: '帮助中心',
      },
    },
    {
      path: '/accessibility',
      name: 'accessibility-settings',
      component: AccessibilitySettingsView,
      meta: {
        section: 'home',
        title: '无障碍模式设置',
      },
    },
    {
      path: '/workspace',
      name: 'workspace',
      component: JobseekerWorkspaceView,
      meta: {
        section: 'jobseeker',
        title: '求职者首页',
        requiresAuth: true,
        allowedRoles: ['ROLE_JOBSEEKER'] satisfies RoleCode[],
      },
    },
    {
      path: '/jobseeker/profile',
      name: 'jobseeker-profile',
      component: JobseekerProfileView,
      meta: {
        section: 'jobseeker',
        title: '基础档案',
        requiresAuth: true,
        allowedRoles: ['ROLE_JOBSEEKER'] satisfies RoleCode[],
      },
    },
    {
      path: '/jobseeker/ability',
      name: 'jobseeker-ability',
      component: JobseekerAbilityView,
      meta: {
        section: 'jobseeker',
        title: '技能与经历',
        requiresAuth: true,
        allowedRoles: ['ROLE_JOBSEEKER'] satisfies RoleCode[],
      },
    },
    {
      path: '/jobseeker/support-needs',
      name: 'jobseeker-support-needs',
      component: JobseekerSupportNeedsView,
      meta: {
        section: 'jobseeker',
        title: '便利需求',
        requiresAuth: true,
        allowedRoles: ['ROLE_JOBSEEKER'] satisfies RoleCode[],
      },
    },
    {
      path: '/jobseeker/resume-preview',
      name: 'jobseeker-resume-preview',
      component: ResumePreviewView,
      meta: {
        section: 'jobseeker',
        title: '简历预览',
        requiresAuth: true,
        allowedRoles: ['ROLE_JOBSEEKER'] satisfies RoleCode[],
      },
    },
    {
      path: '/jobseeker/service-records',
      name: 'jobseeker-service-records',
      component: JobseekerServiceRecordsView,
      meta: {
        section: 'jobseeker',
        title: '服务支持记录',
        requiresAuth: true,
        allowedRoles: ['ROLE_JOBSEEKER'] satisfies RoleCode[],
      },
    },
    {
      path: '/jobseeker/onboarding-feedback',
      name: 'jobseeker-onboarding-feedback',
      component: OnboardingFeedbackView,
      meta: {
        section: 'jobseeker',
        title: '入职反馈',
        requiresAuth: true,
        allowedRoles: ['ROLE_JOBSEEKER'] satisfies RoleCode[],
      },
    },
    {
      path: '/jobseeker/notifications',
      name: 'jobseeker-notifications',
      component: NotificationCenterView,
      meta: {
        section: 'jobseeker',
        title: '通知中心',
        requiresAuth: true,
        allowedRoles: ['ROLE_JOBSEEKER'] satisfies RoleCode[],
      },
    },
    {
      path: '/jobs',
      name: 'jobs',
      component: JobListView,
      meta: {
        section: 'jobseeker',
        title: '推荐岗位',
        requiresAuth: true,
        allowedRoles: ['ROLE_JOBSEEKER'] satisfies RoleCode[],
      },
    },
    {
      path: '/jobs/:jobId',
      name: 'job-detail',
      component: JobDetailView,
      meta: {
        section: 'jobseeker',
        title: '投递岗位',
        requiresAuth: true,
        allowedRoles: ['ROLE_JOBSEEKER'] satisfies RoleCode[],
      },
    },
    {
      path: '/applications',
      name: 'applications',
      component: ApplicationRecordsView,
      meta: {
        section: 'jobseeker',
        title: '投递记录',
        requiresAuth: true,
        allowedRoles: ['ROLE_JOBSEEKER'] satisfies RoleCode[],
      },
    },
    {
      path: '/jobseeker/interview-assistance',
      name: 'jobseeker-interview-assistance',
      component: JobseekerInterviewAssistantView,
      meta: {
        section: 'jobseeker',
        title: '面试辅助',
        requiresAuth: true,
        allowedRoles: ['ROLE_JOBSEEKER'] satisfies RoleCode[],
      },
    },
    {
      path: '/enterprise',
      name: 'enterprise-console',
      component: EnterpriseConsoleView,
      meta: {
        section: 'enterprise',
        title: '企业首页',
        requiresAuth: true,
        allowedRoles: ['ROLE_ENTERPRISE'] satisfies RoleCode[],
      },
    },
    {
      path: '/enterprise/verification',
      name: 'enterprise-verification',
      component: EnterpriseVerificationView,
      meta: {
        section: 'enterprise',
        title: '企业认证',
        requiresAuth: true,
        allowedRoles: ['ROLE_ENTERPRISE'] satisfies RoleCode[],
      },
    },
    {
      path: '/enterprise/jobs',
      name: 'enterprise-jobs',
      component: EnterpriseJobsView,
      meta: {
        section: 'enterprise',
        title: '岗位管理',
        requiresAuth: true,
        allowedRoles: ['ROLE_ENTERPRISE'] satisfies RoleCode[],
      },
    },
    {
      path: '/enterprise/jobs/publish',
      name: 'enterprise-job-publish',
      component: EnterpriseJobPublishView,
      meta: {
        section: 'enterprise',
        title: '发布岗位',
        requiresAuth: true,
        allowedRoles: ['ROLE_ENTERPRISE'] satisfies RoleCode[],
      },
    },
    {
      path: '/enterprise/jobs/:jobId/edit',
      name: 'enterprise-job-edit',
      component: EnterpriseJobPublishView,
      meta: {
        section: 'enterprise',
        title: '编辑岗位',
        requiresAuth: true,
        allowedRoles: ['ROLE_ENTERPRISE'] satisfies RoleCode[],
      },
    },
    {
      path: '/enterprise/jobs/:jobId',
      name: 'enterprise-job-detail',
      component: EnterpriseJobDetailView,
      meta: {
        section: 'enterprise',
        title: '企业岗位详情',
        requiresAuth: true,
        allowedRoles: ['ROLE_ENTERPRISE'] satisfies RoleCode[],
      },
    },
    {
      path: '/enterprise/candidates',
      name: 'candidate-pipeline',
      component: CandidatePipelineView,
      meta: {
        section: 'enterprise',
        title: '候选人列表',
        requiresAuth: true,
        allowedRoles: ['ROLE_ENTERPRISE'] satisfies RoleCode[],
      },
    },
    {
      path: '/enterprise/candidates/:jobId/:applicationId',
      name: 'enterprise-candidate-detail',
      component: EnterpriseCandidateDetailView,
      meta: {
        section: 'enterprise',
        title: '候选人详情',
        requiresAuth: true,
        allowedRoles: ['ROLE_ENTERPRISE'] satisfies RoleCode[],
      },
    },
    {
      path: '/enterprise/interviews',
      name: 'enterprise-interviews',
      component: EnterpriseInterviewManagementView,
      meta: {
        section: 'enterprise',
        title: '面试管理',
        requiresAuth: true,
        allowedRoles: ['ROLE_ENTERPRISE'] satisfies RoleCode[],
      },
    },
    {
      path: '/enterprise/interviews/:jobId/:applicationId',
      name: 'enterprise-interview-detail',
      component: EnterpriseInterviewManagementView,
      meta: {
        section: 'enterprise',
        title: '面试管理',
        requiresAuth: true,
        allowedRoles: ['ROLE_ENTERPRISE'] satisfies RoleCode[],
      },
    },
    {
      path: '/enterprise/stats',
      name: 'enterprise-stats',
      component: EnterpriseStatsView,
      meta: {
        section: 'enterprise',
        title: '招聘统计',
        requiresAuth: true,
        allowedRoles: ['ROLE_ENTERPRISE'] satisfies RoleCode[],
      },
    },
    {
      path: '/enterprise/notifications',
      name: 'enterprise-notifications',
      component: NotificationCenterView,
      meta: {
        section: 'enterprise',
        title: '通知中心',
        requiresAuth: true,
        allowedRoles: ['ROLE_ENTERPRISE'] satisfies RoleCode[],
      },
    },
    {
      path: '/service',
      name: 'service',
      component: ServiceWorkbenchView,
      meta: {
        section: 'service',
        title: '服务首页',
        requiresAuth: true,
        allowedRoles: ['ROLE_SERVICE_ORG'] satisfies RoleCode[],
      },
    },
    {
      path: '/service/cases',
      name: 'service-cases',
      component: ServiceCasesView,
      meta: {
        section: 'service',
        title: '服务对象列表',
        requiresAuth: true,
        allowedRoles: ['ROLE_SERVICE_ORG'] satisfies RoleCode[],
      },
    },
    {
      path: '/service/cases/:caseId',
      name: 'service-case-detail',
      component: ServiceCaseDetailView,
      meta: {
        section: 'service',
        title: '个案详情',
        requiresAuth: true,
        allowedRoles: ['ROLE_SERVICE_ORG'] satisfies RoleCode[],
      },
    },
    {
      path: '/service/cases/:caseId/interventions',
      name: 'service-case-interventions',
      component: ServiceInterventionRecordsView,
      meta: {
        section: 'service',
        title: '干预记录',
        requiresAuth: true,
        allowedRoles: ['ROLE_SERVICE_ORG'] satisfies RoleCode[],
      },
    },
    {
      path: '/service/cases/:caseId/followups',
      name: 'service-case-followups',
      component: ServiceFollowupManagementView,
      meta: {
        section: 'service',
        title: '回访管理',
        requiresAuth: true,
        allowedRoles: ['ROLE_SERVICE_ORG'] satisfies RoleCode[],
      },
    },
    {
      path: '/service/cases/:caseId/referrals',
      name: 'service-case-referrals',
      component: ServiceResourceReferralView,
      meta: {
        section: 'service',
        title: '资源转介',
        requiresAuth: true,
        allowedRoles: ['ROLE_SERVICE_ORG'] satisfies RoleCode[],
      },
    },
    {
      path: '/service/alerts',
      name: 'service-alerts',
      component: ServiceAlertsView,
      meta: {
        section: 'service',
        title: '预警处理',
        requiresAuth: true,
        allowedRoles: ['ROLE_SERVICE_ORG'] satisfies RoleCode[],
      },
    },
    {
      path: '/service/notifications',
      name: 'service-notifications',
      component: NotificationCenterView,
      meta: {
        section: 'service',
        title: '通知中心',
        requiresAuth: true,
        allowedRoles: ['ROLE_SERVICE_ORG'] satisfies RoleCode[],
      },
    },
    {
      path: '/admin',
      name: 'admin',
      component: AdminDashboardView,
      meta: {
        section: 'admin',
        title: '管理端首页',
        requiresAuth: true,
        allowedRoles: ['ROLE_ADMIN'] satisfies RoleCode[],
      },
    },
    {
      path: '/admin/notifications',
      name: 'admin-notifications',
      component: NotificationCenterView,
      meta: {
        section: 'admin',
        title: '通知中心',
        requiresAuth: true,
        allowedRoles: ['ROLE_ADMIN'] satisfies RoleCode[],
      },
    },
    {
      path: '/admin/reviews',
      name: 'admin-reviews',
      component: AdminEnterpriseReviewView,
      meta: {
        section: 'admin',
        title: '企业审核',
        requiresAuth: true,
        allowedRoles: ['ROLE_ADMIN'] satisfies RoleCode[],
      },
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: AdminUserManagementView,
      meta: {
        section: 'admin',
        title: '用户管理',
        requiresAuth: true,
        allowedRoles: ['ROLE_ADMIN'] satisfies RoleCode[],
      },
    },
    {
      path: '/admin/knowledge',
      name: 'admin-knowledge',
      component: KnowledgeAdminView,
      meta: {
        section: 'admin',
        title: '政策内容管理',
        requiresAuth: true,
        allowedRoles: ['ROLE_ADMIN'] satisfies RoleCode[],
      },
    },
    {
      path: '/admin/tags',
      name: 'admin-tags',
      component: TagDictionaryAdminView,
      meta: {
        section: 'admin',
        title: '标签字典管理',
        requiresAuth: true,
        allowedRoles: ['ROLE_ADMIN'] satisfies RoleCode[],
      },
    },
    {
      path: '/admin/matching-engine',
      name: 'admin-matching-engine',
      component: AdminMatchingEngineView,
      meta: {
        section: 'admin',
        title: '匹配引擎参数',
        requiresAuth: true,
        allowedRoles: ['ROLE_ADMIN'] satisfies RoleCode[],
      },
    },
    {
      path: '/admin/logs',
      name: 'admin-logs',
      component: AdminAuditLogsView,
      meta: {
        section: 'admin',
        title: '日志管理',
        requiresAuth: true,
        allowedRoles: ['ROLE_ADMIN'] satisfies RoleCode[],
      },
    },
    {
      path: '/admin/stats',
      name: 'admin-stats',
      component: AdminDataStatsView,
      meta: {
        section: 'admin',
        title: '数据统计',
        requiresAuth: true,
        allowedRoles: ['ROLE_ADMIN'] satisfies RoleCode[],
      },
    },
    {
      path: '/admin/risk-records',
      name: 'admin-risk-records',
      component: AdminRiskRecordsView,
      meta: {
        section: 'admin',
        title: '风险记录管理',
        requiresAuth: true,
        allowedRoles: ['ROLE_ADMIN'] satisfies RoleCode[],
      },
    },
    {
      path: '/admin/issues',
      name: 'admin-issues',
      component: AdminIssueQueueView,
      meta: {
        section: 'admin',
        title: '异常申诉与数据纠错',
        requiresAuth: true,
        allowedRoles: ['ROLE_ADMIN'] satisfies RoleCode[],
      },
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore(pinia)
  authStore.hydrate()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return {
      name: 'login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if ((to.name === 'login' || to.name === 'register') && authStore.isAuthenticated) {
    return authStore.defaultRoute
  }

  const allowedRoles = to.meta.allowedRoles as RoleCode[] | undefined
  if (allowedRoles && !allowedRoles.some((role) => authStore.roles.includes(role))) {
    return authStore.defaultRoute
  }

  return true
})

export default router
