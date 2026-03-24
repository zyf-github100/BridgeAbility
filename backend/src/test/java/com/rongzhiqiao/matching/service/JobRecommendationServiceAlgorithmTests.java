package com.rongzhiqiao.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.rongzhiqiao.catalog.vo.CatalogResponses.JobResponse;
import com.rongzhiqiao.common.api.PageResponse;
import com.rongzhiqiao.config.MatchingProperties;
import com.rongzhiqiao.enterprise.entity.EnterpriseJobPosting;
import com.rongzhiqiao.enterprise.repository.EnterpriseJobRepository;
import com.rongzhiqiao.jobseeker.entity.JobseekerProfile;
import com.rongzhiqiao.jobseeker.mapper.JobseekerProfileMapper;
import com.rongzhiqiao.jobseeker.repository.JobseekerSkillRepository;
import com.rongzhiqiao.jobseeker.repository.JobseekerSkillRepository.JobseekerSkillRecord;
import com.rongzhiqiao.jobseeker.service.JobseekerSupportNeedService;
import com.rongzhiqiao.jobseeker.service.JobseekerSupportNeedService.SupportNeedSnapshot;
import com.rongzhiqiao.jobseeker.vo.InterviewCommunicationCardResponse;
import com.rongzhiqiao.matching.repository.MatchingConfigRepository;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JobRecommendationServiceAlgorithmTests {

    private static final long USER_ID = 1001L;

    @Mock
    private EnterpriseJobRepository enterpriseJobRepository;

    @Mock
    private JobseekerProfileMapper jobseekerProfileMapper;

    @Mock
    private JobseekerSkillRepository jobseekerSkillRepository;

    @Mock
    private JobseekerSupportNeedService jobseekerSupportNeedService;

    @Mock
    private MatchingConfigRepository matchingConfigRepository;

    private JobRecommendationService service;
    private MatchingProperties matchingProperties;

    @BeforeEach
    void setUp() {
        matchingProperties = new MatchingProperties();
        MatchingConfigService matchingConfigService = new MatchingConfigService(
                matchingProperties,
                matchingConfigRepository
        );
        service = new JobRecommendationService(
                enterpriseJobRepository,
                jobseekerProfileMapper,
                jobseekerSkillRepository,
                jobseekerSupportNeedService,
                matchingConfigService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void javaSkillShouldNotBeMatchedFromJavascriptSubstring() {
        EnterpriseJobPosting javascriptJob = job("frontend-js", posting -> {
            posting.setTitle("JavaScript Engineer");
            posting.setDescriptionText("Build frontend applications with TypeScript and React.");
            posting.setRequirementText("Strong JavaScript fundamentals.");
        });
        when(enterpriseJobRepository.findPublishedByJobId("frontend-js")).thenReturn(javascriptJob);
        when(jobseekerProfileMapper.selectByUserId(USER_ID)).thenReturn(null);
        when(jobseekerSkillRepository.listByUserId(USER_ID)).thenReturn(List.of(
                skill("java", "Java", 5)
        ));
        when(jobseekerSupportNeedService.getCurrentSupportNeedSnapshot(USER_ID)).thenReturn(emptySupportNeed());

        JobResponse response = service.getPublishedJobForUser(USER_ID, "frontend-js");

        assertThat(findDimensionValue(response, "skill")).isLessThan(60);
        assertThat(response.reasons()).noneMatch(reason -> reason.contains("Java"));
    }

    @Test
    void cppSkillShouldMatchCppJobInsteadOfFallingThroughTokenizer() {
        EnterpriseJobPosting cppJob = job("cpp-dev", posting -> {
            posting.setTitle("C++ Developer");
            posting.setDescriptionText("Develop C++ desktop tools and performance-sensitive modules.");
            posting.setRequirementText("Experience with modern C++.");
        });
        when(enterpriseJobRepository.findPublishedByJobId("cpp-dev")).thenReturn(cppJob);
        when(jobseekerProfileMapper.selectByUserId(USER_ID)).thenReturn(null);
        when(jobseekerSkillRepository.listByUserId(USER_ID)).thenReturn(List.of(
                skill("cpp", "C++", 5)
        ));
        when(jobseekerSupportNeedService.getCurrentSupportNeedSnapshot(USER_ID)).thenReturn(emptySupportNeed());

        JobResponse response = service.getPublishedJobForUser(USER_ID, "cpp-dev");

        assertThat(findDimensionValue(response, "skill")).isGreaterThanOrEqualTo(80);
        assertThat(response.reasons()).anyMatch(reason -> reason.contains("C++"));
    }

    @Test
    void jsAliasShouldMatchJavascriptRole() {
        EnterpriseJobPosting javascriptJob = job("js-role", posting -> {
            posting.setTitle("JavaScript Engineer");
            posting.setDescriptionText("Build JavaScript interfaces and maintain browser-side modules.");
            posting.setRequirementText("Solid JavaScript experience.");
        });
        when(enterpriseJobRepository.findPublishedByJobId("js-role")).thenReturn(javascriptJob);
        when(jobseekerProfileMapper.selectByUserId(USER_ID)).thenReturn(null);
        when(jobseekerSkillRepository.listByUserId(USER_ID)).thenReturn(List.of(
                skill("js", "JS", 4)
        ));
        when(jobseekerSupportNeedService.getCurrentSupportNeedSnapshot(USER_ID)).thenReturn(emptySupportNeed());

        JobResponse response = service.getPublishedJobForUser(USER_ID, "js-role");

        assertThat(findDimensionValue(response, "skill")).isGreaterThanOrEqualTo(80);
        assertThat(response.reasons()).anyMatch(reason -> reason.contains("JS"));
    }

    @Test
    void sameCityHybridRoleShouldScoreHigherThanCrossCityHybridRole() {
        authenticateAs(USER_ID);

        EnterpriseJobPosting sameCityHybridJob = job("hybrid-shanghai", posting -> {
            posting.setTitle("Operations Coordinator");
            posting.setCity("Shanghai");
            posting.setWorkMode("HYBRID");
            posting.setOnsiteRequired(true);
        });
        EnterpriseJobPosting crossCityHybridJob = job("hybrid-beijing", posting -> {
            posting.setTitle("Operations Coordinator");
            posting.setCity("Beijing");
            posting.setWorkMode("HYBRID");
            posting.setOnsiteRequired(true);
        });

        when(enterpriseJobRepository.listPublishedJobs(null, null, null)).thenReturn(List.of(
                crossCityHybridJob,
                sameCityHybridJob
        ));
        when(jobseekerProfileMapper.selectByUserId(USER_ID)).thenReturn(profile("Shanghai", "HYBRID"));
        when(jobseekerSkillRepository.listByUserId(USER_ID)).thenReturn(List.of());
        when(jobseekerSupportNeedService.getCurrentSupportNeedSnapshot(USER_ID)).thenReturn(emptySupportNeed());

        PageResponse<JobResponse> response = service.listRecommendedJobs(null, null, null, null, null);
        JobResponse first = response.list().get(0);
        JobResponse second = response.list().get(1);

        assertThat(first.id()).isEqualTo("hybrid-shanghai");
        assertThat(findDimensionValue(first, "workMode")).isGreaterThan(findDimensionValue(second, "workMode"));
    }

    @Test
    void hardConflictsShouldRemoveJobFromRecommendationList() {
        authenticateAs(USER_ID);

        EnterpriseJobPosting safeRemoteJob = job("safe-remote", posting -> {
            posting.setTitle("Content Editor");
            posting.setCity("Taipei");
            posting.setWorkMode("REMOTE");
            posting.setRemoteSupported(true);
            posting.setOnsiteRequired(false);
            posting.setTextMaterialSupported(true);
            posting.setTextInterviewSupported(true);
        });
        EnterpriseJobPosting blockedVoiceJob = job("blocked-voice", posting -> {
            posting.setTitle("Phone Outreach Specialist");
            posting.setCity("Taipei");
            posting.setWorkMode("FULL_TIME");
            posting.setRemoteSupported(false);
            posting.setOnsiteRequired(true);
            posting.setHighFrequencyVoiceRequired(true);
            posting.setTextMaterialSupported(false);
            posting.setTextInterviewSupported(false);
        });

        when(enterpriseJobRepository.listPublishedJobs(null, null, null)).thenReturn(List.of(
                blockedVoiceJob,
                safeRemoteJob
        ));
        when(jobseekerProfileMapper.selectByUserId(USER_ID)).thenReturn(profile("Taipei", "REMOTE"));
        when(jobseekerSkillRepository.listByUserId(USER_ID)).thenReturn(List.of());
        when(jobseekerSupportNeedService.getCurrentSupportNeedSnapshot(USER_ID)).thenReturn(
                supportNeed(true, false, true, false, false, false)
        );

        PageResponse<JobResponse> response = service.listRecommendedJobs(null, null, null, null, null);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.list()).extracting(JobResponse::id).containsExactly("safe-remote");
    }

    @Test
    void riskPenaltyShouldPushRiskyJobClearlyBelowSafeAlternative() {
        authenticateAs(USER_ID);

        EnterpriseJobPosting safeJob = job("safe-ops", posting -> {
            posting.setTitle("Documentation Specialist");
            posting.setCity("Shanghai");
            posting.setWorkMode("FULL_TIME");
            posting.setOnsiteRequired(true);
            posting.setDescriptionText("Handle documentation review and structured content updates.");
            posting.setRequirementText("Documentation skill and structured editing experience.");
        });
        EnterpriseJobPosting riskyJob = job("risky-ops", posting -> {
            posting.setTitle("Documentation Specialist");
            posting.setCity("Shanghai");
            posting.setWorkMode("FULL_TIME");
            posting.setOnsiteRequired(true);
            posting.setDescriptionText("Handle documentation review and structured content updates.");
            posting.setRequirementText("Documentation skill and structured editing experience.");
            posting.setNoisyEnvironment(true);
            posting.setLongStandingRequired(true);
        });

        when(enterpriseJobRepository.listPublishedJobs(null, null, null)).thenReturn(List.of(
                riskyJob,
                safeJob
        ));
        when(jobseekerProfileMapper.selectByUserId(USER_ID)).thenReturn(profile("Shanghai", "FULL_TIME"));
        when(jobseekerSkillRepository.listByUserId(USER_ID)).thenReturn(List.of(
                skill("documentation", "Documentation", 4)
        ));
        when(jobseekerSupportNeedService.getCurrentSupportNeedSnapshot(USER_ID)).thenReturn(emptySupportNeed());

        PageResponse<JobResponse> response = service.listRecommendedJobs(null, null, null, null, null);
        JobResponse first = response.list().get(0);
        JobResponse second = response.list().get(1);

        assertThat(first.id()).isEqualTo("safe-ops");
        assertThat(first.matchScore() - second.matchScore()).isGreaterThanOrEqualTo(8);
        assertThat(second.risks()).isNotEmpty();
    }

    @Test
    void configuredWeightsShouldAllowWorkModeToOutweighSkillScore() {
        authenticateAs(USER_ID);

        EnterpriseJobPosting skillHeavyJob = job("skill-heavy", posting -> {
            posting.setTitle("Documentation Specialist");
            posting.setCity("Shanghai");
            posting.setWorkMode("FULL_TIME");
            posting.setOnsiteRequired(true);
            posting.setDescriptionText("Review documentation and maintain structured editing records.");
            posting.setRequirementText("Strong documentation skill and structured editing experience.");
        });
        EnterpriseJobPosting flexibleModeJob = job("flexible-mode", posting -> {
            posting.setTitle("Operations Coordinator");
            posting.setCity("Beijing");
            posting.setWorkMode("REMOTE");
            posting.setRemoteSupported(true);
            posting.setOnsiteRequired(false);
            posting.setDescriptionText("Coordinate recurring tasks and remote project handoffs.");
            posting.setRequirementText("Comfortable with remote collaboration and written updates.");
        });

        when(enterpriseJobRepository.listPublishedJobs(null, null, null)).thenReturn(List.of(
                skillHeavyJob,
                flexibleModeJob
        ));
        when(jobseekerProfileMapper.selectByUserId(USER_ID)).thenReturn(profile("Shanghai", "HYBRID"));
        when(jobseekerSkillRepository.listByUserId(USER_ID)).thenReturn(List.of(
                skill("documentation", "Documentation", 5)
        ));
        when(jobseekerSupportNeedService.getCurrentSupportNeedSnapshot(USER_ID)).thenReturn(emptySupportNeed());

        PageResponse<JobResponse> defaultResponse = service.listRecommendedJobs(null, null, null, null, null);
        assertThat(defaultResponse.list().get(0).id()).isEqualTo("skill-heavy");

        matchingProperties.getScoreWeights().setSkill(5);
        matchingProperties.getScoreWeights().setWorkMode(70);
        matchingProperties.getScoreWeights().setCommunication(10);
        matchingProperties.getScoreWeights().setEnvironment(10);
        matchingProperties.getScoreWeights().setAccommodation(5);

        PageResponse<JobResponse> tunedResponse = service.listRecommendedJobs(null, null, null, null, null);
        assertThat(tunedResponse.list().get(0).id()).isEqualTo("flexible-mode");
    }

    private EnterpriseJobPosting job(String id, Consumer<EnterpriseJobPosting> customizer) {
        EnterpriseJobPosting posting = new EnterpriseJobPosting();
        posting.setJobId(id);
        posting.setTitle("Test Role");
        posting.setCompanyName("Test Company");
        posting.setDepartment("Engineering");
        posting.setCity("Shanghai");
        posting.setSalaryRange("10k-12k");
        posting.setHeadcount(1);
        posting.setDescriptionText("Default description.");
        posting.setRequirementText("Default requirement.");
        posting.setWorkMode("FULL_TIME");
        posting.setSummary("");
        posting.setStage("OPEN");
        posting.setPublishStatus("PUBLISHED");
        posting.setOnsiteRequired(false);
        posting.setRemoteSupported(false);
        posting.setHighFrequencyVoiceRequired(false);
        posting.setNoisyEnvironment(false);
        posting.setLongStandingRequired(false);
        posting.setTextMaterialSupported(false);
        posting.setOnlineInterviewSupported(false);
        posting.setTextInterviewSupported(false);
        posting.setFlexibleScheduleSupported(false);
        posting.setAccessibleWorkspace(false);
        posting.setAssistiveSoftwareSupported(false);
        customizer.accept(posting);
        return posting;
    }

    private JobseekerSkillRecord skill(String code, String name, int level) {
        return new JobseekerSkillRecord(null, USER_ID, code, name, level);
    }

    private JobseekerProfile profile(String targetCity, String workModePreference) {
        JobseekerProfile profile = new JobseekerProfile();
        profile.setTargetCity(targetCity);
        profile.setWorkModePreference(workModePreference);
        return profile;
    }

    private SupportNeedSnapshot emptySupportNeed() {
        return supportNeed(false, false, false, false, false, false);
    }

    private SupportNeedSnapshot supportNeed(boolean textCommunicationPreferred,
                                            boolean subtitleNeeded,
                                            boolean remoteInterviewPreferred,
                                            boolean accessibleWorkspaceNeeded,
                                            boolean flexibleScheduleNeeded,
                                            boolean assistiveSoftwareNeeded) {
        return new SupportNeedSnapshot(
                "HIDDEN",
                false,
                textCommunicationPreferred
                        || subtitleNeeded
                        || remoteInterviewPreferred
                        || accessibleWorkspaceNeeded
                        || flexibleScheduleNeeded
                        || assistiveSoftwareNeeded,
                textCommunicationPreferred,
                subtitleNeeded,
                remoteInterviewPreferred,
                false,
                false,
                false,
                flexibleScheduleNeeded,
                accessibleWorkspaceNeeded,
                assistiveSoftwareNeeded,
                null,
                List.of(),
                null,
                new InterviewCommunicationCardResponse("Card", "None", List.of(), ""),
                ""
        );
    }

    private int findDimensionValue(JobResponse response, String label) {
        return response.dimensionScores().stream()
                .filter(item -> label.equals(item.label()))
                .findFirst()
                .map(item -> item.value())
                .orElse(0);
    }

    private void authenticateAs(long userId) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                Long.toString(userId),
                "n/a",
                List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
