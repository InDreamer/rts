package com.rts.api;

import com.rts.agent.AgentAnalysisService;
import com.rts.agent.AgentToolService;
import com.rts.agent.AnswerViewService;
import com.rts.agent.ContextualAskService;
import com.rts.agent.EvaluationService;
import com.rts.agent.FeatureFlagService;
import com.rts.agent.FeedbackMemoryService;
import com.rts.agent.GovernanceAssistantService;
import com.rts.agent.MessageSupportService;
import com.rts.agent.MetricsService;
import com.rts.agent.ManagedScenarioService;
import com.rts.agent.PipelineReportService;
import com.rts.agent.RtsToolRegistry;
import com.rts.model.AgentServiceModels.AnswerViewRequest;
import com.rts.model.AgentServiceModels.ConfusableObjectsRequest;
import com.rts.model.AgentServiceModels.ConflictExplainRequest;
import com.rts.model.AgentServiceModels.ContextSnapshotRequest;
import com.rts.model.AgentServiceModels.ContextualAskRequest;
import com.rts.model.AgentServiceModels.EvaluationRunRequest;
import com.rts.model.AgentServiceModels.FeedbackRecord;
import com.rts.model.AgentServiceModels.FeedbackRequest;
import com.rts.model.AgentServiceModels.GroundingCheckRequest;
import com.rts.model.AgentServiceModels.GovernanceReviewRequest;
import com.rts.model.AgentServiceModels.HumanDecisionRecordRequest;
import com.rts.model.AgentServiceModels.ImpactAnalysisRequest;
import com.rts.model.AgentServiceModels.MemoryRecord;
import com.rts.model.AgentServiceModels.MemoryWriteRequest;
import com.rts.model.AgentServiceModels.MetricsSnapshotRequest;
import com.rts.model.AgentServiceModels.ManagedScenarioRequest;
import com.rts.model.AgentServiceModels.ObjectCardsRequest;
import com.rts.model.AgentServiceModels.RawMessageCandidateRequest;
import com.rts.model.AgentServiceModels.ReleaseReadinessRequest;
import com.rts.model.AgentServiceModels.RuleCompareRequest;
import com.rts.model.AgentServiceModels.ScopeListRequest;
import com.rts.model.AgentServiceModels.ScopeSearchRequest;
import com.rts.model.AgentServiceModels.ScopedToolRequest;
import com.rts.model.AgentServiceModels.TestPlanRequest;
import com.rts.model.AgentServiceModels.TraceReportRequest;
import com.rts.model.CoreModels.Direction;
import com.rts.query.QueryRequests.FindRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AgentServiceController {
    private final AgentToolService toolService;
    private final AgentAnalysisService analysisService;
    private final AnswerViewService answerViewService;
    private final ContextualAskService contextualAskService;
    private final EvaluationService evaluationService;
    private final FeatureFlagService featureFlagService;
    private final GovernanceAssistantService governanceAssistantService;
    private final MetricsService metricsService;
    private final MessageSupportService messageSupportService;
    private final PipelineReportService pipelineReportService;
    private final FeedbackMemoryService feedbackMemoryService;
    private final ManagedScenarioService managedScenarioService;
    private final RtsToolRegistry toolRegistry;

    public AgentServiceController(AgentToolService toolService, AgentAnalysisService analysisService,
            AnswerViewService answerViewService, ContextualAskService contextualAskService, FeatureFlagService featureFlagService,
            EvaluationService evaluationService, GovernanceAssistantService governanceAssistantService, MetricsService metricsService,
            MessageSupportService messageSupportService, PipelineReportService pipelineReportService, FeedbackMemoryService feedbackMemoryService,
            ManagedScenarioService managedScenarioService, RtsToolRegistry toolRegistry) {
        this.toolService = toolService;
        this.analysisService = analysisService;
        this.answerViewService = answerViewService;
        this.contextualAskService = contextualAskService;
        this.evaluationService = evaluationService;
        this.featureFlagService = featureFlagService;
        this.governanceAssistantService = governanceAssistantService;
        this.metricsService = metricsService;
        this.messageSupportService = messageSupportService;
        this.pipelineReportService = pipelineReportService;
        this.feedbackMemoryService = feedbackMemoryService;
        this.managedScenarioService = managedScenarioService;
        this.toolRegistry = toolRegistry;
    }

    @PostMapping("/tools/feature_flags")
    public Object featureFlags(@RequestBody(required = false) Map<String, Object> request) {
        return featureFlagService.current();
    }

    @PostMapping("/tools/catalog")
    public Object toolCatalog(@RequestBody(required = false) Map<String, Object> request) {
        return toolRegistry.catalog();
    }

    @PostMapping("/tools/list_scopes")
    public Object listScopes(@RequestBody ScopeListRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.listScopes(request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/search_scopes")
    public Object searchScopes(@RequestBody ScopeSearchRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.searchScopes(request.query(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/get_scope_summary")
    public Object getScopeSummary(@RequestBody ScopedToolRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.getScopeSummary(request.scope(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/get_pack_status")
    public Object getPackStatus(@RequestBody ScopedToolRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.getScopeSummary(request.scope(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/get_pack_navigation")
    public Object getPackNavigation(@RequestBody ScopedToolRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.getPackNavigation(request.scope(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/get_scope_tree")
    public Object getScopeTree(@RequestBody ScopeListRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.listScopes(request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/get_object_cards")
    public Object getObjectCards(@RequestBody ObjectCardsRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.getObjectCards(request.uris(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/get_object_card")
    public Object getObjectCard(@RequestBody Map<String, String> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.getObjectCard(request.get("uri"), request.get("release_id"), request.get("trace_id"), request.get("caller_id"), apiKey);
    }

    @PostMapping("/tools/search_objects")
    public Object searchObjects(@RequestBody FindRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.searchObjects(new FindRequest(request.query(), request.scope(), request.objectTypes(), request.anchors(), request.limit(), request.callerId(), apiKey, request.outputMode()));
    }

    @PostMapping("/tools/find_by_target_path")
    public Object findByTargetPath(@RequestBody Map<String, Object> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        var scope = mapperScope(request.get("scope"));
        return toolService.findByTargetPath(String.valueOf(request.get("target_path")), scope, string(request.get("caller_id")), apiKey, string(request.get("output_mode")));
    }

    @PostMapping("/tools/find_by_source_anchor")
    public Object findBySourceAnchor(@RequestBody Map<String, Object> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        var scope = mapperScope(request.get("scope"));
        return toolService.findBySourceAnchor(String.valueOf(request.get("source_anchor")), scope, string(request.get("caller_id")), apiKey, string(request.get("output_mode")));
    }

    @PostMapping("/tools/find_by_lookup_key")
    public Object findByLookupKey(@RequestBody Map<String, Object> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        var scope = mapperScope(request.get("scope"));
        return toolService.findByLookupKey(String.valueOf(request.get("lookup_key")), scope, string(request.get("caller_id")), apiKey, string(request.get("output_mode")));
    }

    @PostMapping("/tools/find_objects")
    public Object findObjects(@RequestBody FindRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.searchObjects(new FindRequest(request.query(), request.scope(), request.objectTypes(), request.anchors(), request.limit(), request.callerId(), apiKey, request.outputMode()));
    }

    @PostMapping("/tools/find_reverse_dependencies")
    public Object findReverseDependencies(@RequestBody Map<String, Object> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        Integer depth = request.get("depth") == null ? 1 : Integer.valueOf(String.valueOf(request.get("depth")));
        return toolService.getDependencySubgraph(string(request.get("uri")), Direction.reverse, string(request.get("edge_type")), depth,
                string(request.get("purpose")), string(request.get("release_id")), string(request.get("caller_id")), apiKey);
    }

    @PostMapping("/tools/find_confusable_objects")
    public Object findConfusableObjects(@RequestBody ConfusableObjectsRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.findConfusableObjects(request.uri(), request.scope(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/find_confusable_scopes")
    public Object findConfusableScopes(@RequestBody ScopedToolRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.findConfusableScopes(request.scope(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/read_lookup_sample")
    public Object readLookupSample(@RequestBody Map<String, Object> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.readLookupSample(string(request.get("uri")), string(request.get("release_id")), string(request.get("trace_id")),
                string(request.get("caller_id")), apiKey);
    }

    @PostMapping("/tools/read_helper_contract")
    public Object readHelperContract(@RequestBody Map<String, Object> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.readHelperContract(string(request.get("uri")), string(request.get("release_id")), string(request.get("trace_id")),
                string(request.get("caller_id")), apiKey);
    }

    @PostMapping("/tools/read_evidence_summary")
    public Object readEvidenceSummary(@RequestBody Map<String, Object> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.readEvidenceSummary(string(request.get("uri")), string(request.get("caller_id")), apiKey, string(request.get("output_mode")));
    }

    @PostMapping("/tools/get_dependency_subgraph")
    public Object getDependencySubgraph(@RequestBody Map<String, Object> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        Direction direction = request.get("direction") == null ? Direction.forward : Direction.valueOf(String.valueOf(request.get("direction")));
        Integer depth = request.get("depth") == null ? 1 : Integer.valueOf(String.valueOf(request.get("depth")));
        return toolService.getDependencySubgraph(string(request.get("uri")), direction, string(request.get("edge_type")), depth,
                string(request.get("purpose")), string(request.get("release_id")), string(request.get("caller_id")), apiKey);
    }

    @PostMapping("/tools/read_agent_object")
    public Object readAgentObject(@RequestBody Map<String, Object> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.readAgentObject(string(request.get("uri")), string(request.get("release_id")), string(request.get("trace_id")),
                string(request.get("caller_id")), apiKey, string(request.get("purpose")));
    }

    @PostMapping("/tools/read_object_l2")
    public Object readObjectL2(@RequestBody Map<String, Object> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.readObjectL2(string(request.get("uri")), string(request.get("release_id")), string(request.get("trace_id")),
                string(request.get("caller_id")), apiKey, string(request.get("purpose")));
    }

    @PostMapping("/tools/read_rule_dependencies")
    public Object readRuleDependencies(@RequestBody Map<String, Object> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        Integer depth = request.get("depth") == null ? 1 : Integer.valueOf(String.valueOf(request.get("depth")));
        return toolService.getDependencySubgraph(string(request.get("uri")), Direction.forward, string(request.get("edge_type")), depth,
                string(request.get("purpose")), string(request.get("release_id")), string(request.get("caller_id")), apiKey);
    }

    @PostMapping("/analyze/impact")
    public Object analyzeImpact(@RequestBody ImpactAnalysisRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return analysisService.analyzeImpact(new ImpactAnalysisRequest(request.changedUri(), request.changedTargetPath(), request.changedSourceAnchor(),
                request.scope(), request.callerId(), apiKey, request.outputMode(), request.readL2(), request.maxObjects()));
    }

    @PostMapping("/scenario/analyze-pr-diff")
    public Object analyzePrDiff(@RequestBody ManagedScenarioRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return managedScenarioService.analyzePrDiff(new ManagedScenarioRequest("pr_diff_impact", request.input(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/scenario/investigate-exception")
    public Object investigateException(@RequestBody ManagedScenarioRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return managedScenarioService.investigateException(new ManagedScenarioRequest("exception_investigation", request.input(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/scenario/analyze-failed-message")
    public Object analyzeFailedMessage(@RequestBody ManagedScenarioRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return managedScenarioService.analyzeFailedMessage(new ManagedScenarioRequest("failed_message_analysis", request.input(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/scenario/plan-tests")
    public Object scenarioPlanTests(@RequestBody ManagedScenarioRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return managedScenarioService.planTests(new ManagedScenarioRequest("test_planning", request.input(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/scenario/governance-review")
    public Object scenarioGovernanceReview(@RequestBody ManagedScenarioRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return managedScenarioService.reviewGovernance(new ManagedScenarioRequest("governance_review", request.input(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/analyze/test-plan")
    public Object planTests(@RequestBody TestPlanRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return analysisService.planTests(new TestPlanRequest(request.seedUri(), request.scope(), request.callerId(), apiKey,
                request.outputMode(), request.includeL2(), request.maxObjects()));
    }

    @PostMapping("/analyze/release-readiness")
    public Object releaseReadiness(@RequestBody ReleaseReadinessRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return analysisService.checkReleaseReadiness(new ReleaseReadinessRequest(request.scope(), request.callerId(), apiKey, request.outputMode()));
    }

    @PostMapping("/analyze/grounding")
    public Object grounding(@RequestBody GroundingCheckRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return analysisService.checkGrounding(request.traceId(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/analyze/compare-rules")
    public Object compareRules(@RequestBody RuleCompareRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return analysisService.compareRules(new RuleCompareRequest(request.leftUri(), request.rightUri(), request.callerId(), apiKey,
                request.outputMode(), request.readL2()));
    }

    @PostMapping("/analyze/explain-conflict")
    public Object explainConflict(@RequestBody ConflictExplainRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return analysisService.explainConflict(new ConflictExplainRequest(request.uri(), request.scope(), request.callerId(), apiKey, request.outputMode()));
    }

    @PostMapping("/analyze/raw-message-candidate")
    public Object rawMessageCandidate(@RequestBody RawMessageCandidateRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return analysisService.generateRawMessageCandidate(new RawMessageCandidateRequest(request.rawMessage(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/views/answer")
    public Object answerView(@RequestBody AnswerViewRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return answerViewService.shape(request.answer(), request.responseView(), request.callerId(), apiKey);
    }

    @PostMapping("/ask/contextual")
    public Object contextualAsk(@RequestBody ContextualAskRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return contextualAskService.ask(new ContextualAskRequest(request.query(), request.callerId(), apiKey, request.scopeHint(),
                request.outputMode(), request.maxToolCalls(), request.sessionId(), request.workspaceScope()));
    }

    @PostMapping("/feedback/record")
    public FeedbackRecord feedback(@RequestBody FeedbackRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return feedbackMemoryService.recordFeedback(new FeedbackRequest(request.traceId(), request.feedbackType(), request.message(),
                request.scope(), request.selectedObjectUri(), request.callerId(), apiKey, request.sessionId()));
    }

    @PostMapping("/feedback/memory")
    public MemoryRecord memory(@RequestBody MemoryWriteRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return feedbackMemoryService.writeMemory(new MemoryWriteRequest(request.sessionId(), request.memoryType(), request.key(), request.value(),
                request.scope(), request.callerId(), apiKey));
    }

    @PostMapping("/feedback/context")
    public Object context(@RequestBody ContextSnapshotRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return feedbackMemoryService.loadContext(new ContextSnapshotRequest(request.sessionId(), request.scope(), request.callerId(), apiKey));
    }

    @PostMapping("/evaluation/run")
    public Object runEvaluation(@RequestBody EvaluationRunRequest request) {
        return evaluationService.run(request.mode(), request.cases());
    }

    @PostMapping("/governance/review")
    public Object governanceReview(@RequestBody GovernanceReviewRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return governanceAssistantService.review(new GovernanceReviewRequest(request.scope(), request.objectUri(), request.callerId(), apiKey,
                request.outputMode(), request.readL2()));
    }

    @PostMapping("/governance/decision")
    public Object humanDecision(@RequestBody HumanDecisionRecordRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return governanceAssistantService.recordDecision(new HumanDecisionRecordRequest(request.traceId(), request.objectUri(), request.scope(),
                request.decisionType(), request.decisionText(), request.decidedBy(), request.callerId(), apiKey));
    }

    @PostMapping("/metrics/snapshot")
    public Object metricsSnapshot(@RequestBody MetricsSnapshotRequest request) {
        return metricsService.snapshot(request.evaluationResults());
    }

    @PostMapping("/message/parse-raw")
    public Object parseRaw(@RequestBody RawMessageCandidateRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return messageSupportService.parseRawMessageCandidate(new RawMessageCandidateRequest(request.rawMessage(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/message/map-source-fields")
    public Object mapSourceFields(@RequestBody RawMessageCandidateRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return messageSupportService.mapSourceFieldsToRules(new RawMessageCandidateRequest(request.rawMessage(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/message/resolve-lookups")
    public Object resolveLookups(@RequestBody RawMessageCandidateRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return messageSupportService.resolveRequiredLookups(new RawMessageCandidateRequest(request.rawMessage(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/message/simulate-rule-application")
    public Object simulateRuleApplication(@RequestBody RawMessageCandidateRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return messageSupportService.simulateRuleApplication(new RawMessageCandidateRequest(request.rawMessage(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/message/assemble-target-candidate")
    public Object assembleTargetCandidate(@RequestBody RawMessageCandidateRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return messageSupportService.assembleTargetMessageCandidate(new RawMessageCandidateRequest(request.rawMessage(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/message/validate-grounding")
    public Object validateTargetGrounding(@RequestBody RawMessageCandidateRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return messageSupportService.validateTargetMessageGrounding(new RawMessageCandidateRequest(request.rawMessage(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/pipeline/release-readiness")
    public Object pipelineReleaseReadiness(@RequestBody ReleaseReadinessRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return pipelineReportService.releaseReadiness(new ReleaseReadinessRequest(request.scope(), request.callerId(), apiKey, request.outputMode()));
    }

    @PostMapping("/reports/trace")
    public Object traceReport(@RequestBody TraceReportRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return pipelineReportService.traceReport(request.traceId(), request.callerId(), apiKey);
    }

    @SuppressWarnings("unchecked")
    private com.rts.model.CoreModels.ScopeKey mapperScope(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        return new com.rts.model.CoreModels.ScopeKey(
                String.valueOf(map.get("channel")),
                String.valueOf(map.get("product")),
                String.valueOf(map.get("pack")),
                String.valueOf(map.get("domain")));
    }

    private String string(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }
}
