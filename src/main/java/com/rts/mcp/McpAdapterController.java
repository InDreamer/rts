package com.rts.mcp;

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
import com.rts.llm.ControlledLlmHarness;
import com.rts.model.AgentServiceModels.AnswerViewRequest;
import com.rts.model.AgentServiceModels.ConfusableObjectsRequest;
import com.rts.model.AgentServiceModels.ConflictExplainRequest;
import com.rts.model.AgentServiceModels.ContextSnapshotRequest;
import com.rts.model.AgentServiceModels.ContextualAskRequest;
import com.rts.model.AgentServiceModels.EvaluationRunRequest;
import com.rts.model.AgentServiceModels.FeedbackRequest;
import com.rts.model.AgentServiceModels.GroundingCheckRequest;
import com.rts.model.AgentServiceModels.GovernanceReviewRequest;
import com.rts.model.AgentServiceModels.HumanDecisionRecordRequest;
import com.rts.model.AgentServiceModels.ImpactAnalysisRequest;
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
import com.rts.model.AgentServiceModels.ToolCatalogEntry;
import com.rts.model.AgentServiceModels.TraceReportRequest;
import com.rts.model.CoreModels.Direction;
import com.rts.query.QueryRequests.AskRequest;
import com.rts.query.QueryRequests.DependenciesRequest;
import com.rts.query.QueryRequests.FindRequest;
import com.rts.query.QueryRequests.ObjectContentRequest;
import com.rts.query.QueryService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mcp")
public class McpAdapterController {
    private final QueryService queryService;
    private final ControlledLlmHarness harness;
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

    public McpAdapterController(QueryService queryService, ControlledLlmHarness harness, AgentToolService toolService,
            AgentAnalysisService analysisService, AnswerViewService answerViewService, ContextualAskService contextualAskService,
            EvaluationService evaluationService, FeatureFlagService featureFlagService, GovernanceAssistantService governanceAssistantService,
            MetricsService metricsService, MessageSupportService messageSupportService, PipelineReportService pipelineReportService,
            FeedbackMemoryService feedbackMemoryService, ManagedScenarioService managedScenarioService, RtsToolRegistry toolRegistry) {
        this.queryService = queryService;
        this.harness = harness;
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

    @GetMapping("/tools")
    public Map<String, Object> tools() {
        if (!featureFlagService.current().mcpExpandedToolsEnabled()) {
            List<String> minimal = List.of("rts_find_objects", "rts_read_object", "rts_get_dependencies", "rts_ask", "rts_get_trace");
            return Map.of("tools", minimal, "catalog", minimal.stream().map(this::catalogEntry).toList());
        }
        List<String> tools = List.of(
                "rts_feature_flags",
                "rts_list_scopes",
                "rts_search_scopes",
                "rts_get_scope_summary",
                "rts_get_pack_status",
                "rts_find_confusable_scopes",
                "rts_get_pack_navigation",
                "rts_get_scope_tree",
                "rts_get_object_card",
                "rts_get_object_cards",
                "rts_get_dependency_subgraph",
                "rts_find_confusable_objects",
                "rts_find_objects",
                "rts_search_objects",
                "rts_find_by_target_path",
                "rts_find_by_source_anchor",
                "rts_find_by_lookup_key",
                "rts_find_reverse_dependencies",
                "rts_read_object",
                "rts_read_object_l2",
                "rts_read_agent_object",
                "rts_read_rule_dependencies",
                "rts_read_lookup_sample",
                "rts_read_helper_contract",
                "rts_read_evidence_summary",
                "rts_analyze_impact",
                "rts_analyze_pr_diff",
                "rts_investigate_exception",
                "rts_analyze_failed_message",
                "rts_scenario_plan_tests",
                "rts_scenario_governance_review",
                "rts_plan_tests",
                "rts_generate_target_message_candidate",
                "rts_parse_raw_message_candidate",
                "rts_map_source_fields_to_rules",
                "rts_resolve_required_lookups",
                "rts_simulate_rule_application",
                "rts_assemble_target_message_candidate",
                "rts_validate_target_message_grounding",
                "rts_compare_rules",
                "rts_explain_conflict",
                "rts_check_release_readiness",
                "rts_check_grounding",
                "rts_shape_answer_view",
                "rts_get_dependencies",
                "rts_ask",
                "rts_contextual_ask",
                "rts_get_trace",
                "rts_run_evaluation",
                "rts_governance_review",
                "rts_record_human_decision",
                "rts_metrics_snapshot",
                "rts_pipeline_release_readiness",
                "rts_trace_report",
                "rts_record_feedback",
                "rts_write_context_memory",
                "rts_get_context_memory");
        return Map.of("tools", tools, "catalog", tools.stream().map(this::catalogEntry).toList());
    }

    @PostMapping("/tools/rts_feature_flags")
    public Object featureFlags(@RequestBody(required = false) Map<String, Object> request) {
        return featureFlagService.current();
    }

    @PostMapping("/tools/rts_list_scopes")
    public Object listScopes(@RequestBody ScopeListRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.listScopes(request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/rts_search_scopes")
    public Object searchScopes(@RequestBody ScopeSearchRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.searchScopes(request.query(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/rts_get_scope_summary")
    public Object scopeSummary(@RequestBody ScopedToolRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.getScopeSummary(request.scope(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/rts_get_pack_status")
    public Object packStatus(@RequestBody ScopedToolRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.getScopeSummary(request.scope(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/rts_find_confusable_scopes")
    public Object confusableScopes(@RequestBody ScopedToolRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.findConfusableScopes(request.scope(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/rts_get_pack_navigation")
    public Object packNavigation(@RequestBody ScopedToolRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.getPackNavigation(request.scope(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/rts_get_scope_tree")
    public Object scopeTree(@RequestBody ScopeListRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.listScopes(request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/rts_get_object_card")
    public Object objectCard(@RequestBody Map<String, String> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.getObjectCard(request.get("uri"), request.get("release_id"), request.get("trace_id"), request.get("caller_id"), apiKey);
    }

    @PostMapping("/tools/rts_get_object_cards")
    public Object objectCards(@RequestBody ObjectCardsRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.getObjectCards(request.uris(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/rts_get_dependency_subgraph")
    public Object dependencySubgraph(@RequestBody DependenciesRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.getDependencySubgraph(request.uri(), request.direction(), request.edgeType(), request.depth(), request.purpose(),
                request.releaseId(), request.callerId(), apiKey);
    }

    @PostMapping("/tools/rts_find_confusable_objects")
    public Object confusableObjects(@RequestBody ConfusableObjectsRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.findConfusableObjects(request.uri(), request.scope(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/rts_find_objects")
    public Object find(@RequestBody FindRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.find(new FindRequest(request.query(), request.scope(), request.objectTypes(), request.anchors(), request.limit(), request.callerId(), apiKey, request.outputMode()));
    }

    @PostMapping("/tools/rts_search_objects")
    public Object search(@RequestBody FindRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.searchObjects(new FindRequest(request.query(), request.scope(), request.objectTypes(), request.anchors(), request.limit(), request.callerId(), apiKey, request.outputMode()));
    }

    @PostMapping("/tools/rts_find_by_target_path")
    public Object byTarget(@RequestBody Map<String, Object> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.findByTargetPath(String.valueOf(request.get("target_path")), scope(request.get("scope")),
                String.valueOf(request.get("caller_id")), apiKey, string(request.get("output_mode")));
    }

    @PostMapping("/tools/rts_find_by_source_anchor")
    public Object bySource(@RequestBody Map<String, Object> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.findBySourceAnchor(String.valueOf(request.get("source_anchor")), scope(request.get("scope")),
                String.valueOf(request.get("caller_id")), apiKey, string(request.get("output_mode")));
    }

    @PostMapping("/tools/rts_find_by_lookup_key")
    public Object byLookup(@RequestBody Map<String, Object> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.findByLookupKey(String.valueOf(request.get("lookup_key")), scope(request.get("scope")),
                String.valueOf(request.get("caller_id")), apiKey, string(request.get("output_mode")));
    }

    @PostMapping("/tools/rts_find_reverse_dependencies")
    public Object reverseDependencies(@RequestBody DependenciesRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.dependencies(new DependenciesRequest(request.uri(), Direction.reverse, request.edgeType(), request.depth(), request.purpose(),
                request.releaseId(), request.callerId(), apiKey));
    }

    @PostMapping("/tools/rts_read_object")
    public Object read(@RequestBody ObjectContentRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.readContent(new ObjectContentRequest(request.uri(), request.purpose(), request.releaseId(), request.traceId(), request.callerId(), apiKey));
    }

    @PostMapping("/tools/rts_read_agent_object")
    public Object readAgentObject(@RequestBody Map<String, String> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.readAgentObject(request.get("uri"), request.get("release_id"), request.get("trace_id"), request.get("caller_id"), apiKey, request.get("purpose"));
    }

    @PostMapping("/tools/rts_read_object_l2")
    public Object readObjectL2(@RequestBody ObjectContentRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.readContent(new ObjectContentRequest(request.uri(), request.purpose(), request.releaseId(), request.traceId(), request.callerId(), apiKey));
    }

    @PostMapping("/tools/rts_read_rule_dependencies")
    public Object readRuleDependencies(@RequestBody DependenciesRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.dependencies(new DependenciesRequest(request.uri(), Direction.forward, request.edgeType(), request.depth(), request.purpose(),
                request.releaseId(), request.callerId(), apiKey));
    }

    @PostMapping("/tools/rts_read_lookup_sample")
    public Object lookupSample(@RequestBody Map<String, String> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.readLookupSample(request.get("uri"), request.get("release_id"), request.get("trace_id"), request.get("caller_id"), apiKey);
    }

    @PostMapping("/tools/rts_read_helper_contract")
    public Object helperContract(@RequestBody Map<String, String> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.readHelperContract(request.get("uri"), request.get("release_id"), request.get("trace_id"), request.get("caller_id"), apiKey);
    }

    @PostMapping("/tools/rts_read_evidence_summary")
    public Object evidenceSummary(@RequestBody Map<String, String> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return toolService.readEvidenceSummary(request.get("uri"), request.get("caller_id"), apiKey, request.get("output_mode"));
    }

    @PostMapping("/tools/rts_analyze_impact")
    public Object impact(@RequestBody ImpactAnalysisRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return analysisService.analyzeImpact(new ImpactAnalysisRequest(request.changedUri(), request.changedTargetPath(), request.changedSourceAnchor(),
                request.scope(), request.callerId(), apiKey, request.outputMode(), request.readL2(), request.maxObjects()));
    }

    @PostMapping("/tools/rts_analyze_pr_diff")
    public Object prDiff(@RequestBody ManagedScenarioRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return managedScenarioService.analyzePrDiff(new ManagedScenarioRequest("pr_diff_impact", request.input(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/tools/rts_investigate_exception")
    public Object exceptionInvestigation(@RequestBody ManagedScenarioRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return managedScenarioService.investigateException(new ManagedScenarioRequest("exception_investigation", request.input(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/tools/rts_analyze_failed_message")
    public Object failedMessage(@RequestBody ManagedScenarioRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return managedScenarioService.analyzeFailedMessage(new ManagedScenarioRequest("failed_message_analysis", request.input(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/tools/rts_scenario_plan_tests")
    public Object scenarioPlanTests(@RequestBody ManagedScenarioRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return managedScenarioService.planTests(new ManagedScenarioRequest("test_planning", request.input(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/tools/rts_scenario_governance_review")
    public Object scenarioGovernanceReview(@RequestBody ManagedScenarioRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return managedScenarioService.reviewGovernance(new ManagedScenarioRequest("governance_review", request.input(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/tools/rts_plan_tests")
    public Object planTests(@RequestBody TestPlanRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return analysisService.planTests(new TestPlanRequest(request.seedUri(), request.scope(), request.callerId(), apiKey,
                request.outputMode(), request.includeL2(), request.maxObjects()));
    }

    @PostMapping("/tools/rts_generate_target_message_candidate")
    public Object rawMessageCandidate(@RequestBody RawMessageCandidateRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return analysisService.generateRawMessageCandidate(new RawMessageCandidateRequest(request.rawMessage(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/tools/rts_parse_raw_message_candidate")
    public Object parseRawMessageCandidate(@RequestBody RawMessageCandidateRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return messageSupportService.parseRawMessageCandidate(new RawMessageCandidateRequest(request.rawMessage(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/tools/rts_map_source_fields_to_rules")
    public Object mapSourceFieldsToRules(@RequestBody RawMessageCandidateRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return messageSupportService.mapSourceFieldsToRules(new RawMessageCandidateRequest(request.rawMessage(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/tools/rts_resolve_required_lookups")
    public Object resolveRequiredLookups(@RequestBody RawMessageCandidateRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return messageSupportService.resolveRequiredLookups(new RawMessageCandidateRequest(request.rawMessage(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/tools/rts_simulate_rule_application")
    public Object simulateRuleApplication(@RequestBody RawMessageCandidateRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return messageSupportService.simulateRuleApplication(new RawMessageCandidateRequest(request.rawMessage(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/tools/rts_assemble_target_message_candidate")
    public Object assembleTargetMessageCandidate(@RequestBody RawMessageCandidateRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return messageSupportService.assembleTargetMessageCandidate(new RawMessageCandidateRequest(request.rawMessage(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/tools/rts_validate_target_message_grounding")
    public Object validateTargetMessageGrounding(@RequestBody RawMessageCandidateRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return messageSupportService.validateTargetMessageGrounding(new RawMessageCandidateRequest(request.rawMessage(), request.scope(),
                request.callerId(), apiKey, request.outputMode(), request.maxObjects()));
    }

    @PostMapping("/tools/rts_compare_rules")
    public Object compareRules(@RequestBody RuleCompareRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return analysisService.compareRules(new RuleCompareRequest(request.leftUri(), request.rightUri(), request.callerId(), apiKey,
                request.outputMode(), request.readL2()));
    }

    @PostMapping("/tools/rts_explain_conflict")
    public Object explainConflict(@RequestBody ConflictExplainRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return analysisService.explainConflict(new ConflictExplainRequest(request.uri(), request.scope(), request.callerId(), apiKey, request.outputMode()));
    }

    @PostMapping("/tools/rts_check_release_readiness")
    public Object readiness(@RequestBody ReleaseReadinessRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return analysisService.checkReleaseReadiness(new ReleaseReadinessRequest(request.scope(), request.callerId(), apiKey, request.outputMode()));
    }

    @PostMapping("/tools/rts_check_grounding")
    public Object grounding(@RequestBody GroundingCheckRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return analysisService.checkGrounding(request.traceId(), request.callerId(), apiKey, request.outputMode());
    }

    @PostMapping("/tools/rts_shape_answer_view")
    public Object shapeAnswerView(@RequestBody AnswerViewRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return answerViewService.shape(request.answer(), request.responseView(), request.callerId(), apiKey);
    }

    @PostMapping("/tools/rts_get_dependencies")
    public Object dependencies(@RequestBody DependenciesRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.dependencies(new DependenciesRequest(request.uri(), request.direction(), request.edgeType(), request.depth(), request.purpose(), request.releaseId(), request.callerId(), apiKey));
    }

    @PostMapping("/tools/rts_ask")
    public Object ask(@RequestBody AskRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return harness.ask(new AskRequest(request.query(), request.callerId(), apiKey, request.scopeHint(), request.outputMode(), request.maxToolCalls()));
    }

    @PostMapping("/tools/rts_contextual_ask")
    public Object contextualAsk(@RequestBody ContextualAskRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return contextualAskService.ask(new ContextualAskRequest(request.query(), request.callerId(), apiKey, request.scopeHint(),
                request.outputMode(), request.maxToolCalls(), request.sessionId(), request.workspaceScope()));
    }

    @PostMapping("/tools/rts_get_trace")
    public Object trace(@RequestBody Map<String, String> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.trace(request.get("trace_id"), request.get("caller_id"), apiKey);
    }

    @PostMapping("/tools/rts_run_evaluation")
    public Object runEvaluation(@RequestBody EvaluationRunRequest request) {
        return evaluationService.run(request.mode(), request.cases());
    }

    @PostMapping("/tools/rts_governance_review")
    public Object governanceReview(@RequestBody GovernanceReviewRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return governanceAssistantService.review(new GovernanceReviewRequest(request.scope(), request.objectUri(), request.callerId(), apiKey,
                request.outputMode(), request.readL2()));
    }

    @PostMapping("/tools/rts_record_human_decision")
    public Object humanDecision(@RequestBody HumanDecisionRecordRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return governanceAssistantService.recordDecision(new HumanDecisionRecordRequest(request.traceId(), request.objectUri(), request.scope(),
                request.decisionType(), request.decisionText(), request.decidedBy(), request.callerId(), apiKey));
    }

    @PostMapping("/tools/rts_metrics_snapshot")
    public Object metricsSnapshot(@RequestBody MetricsSnapshotRequest request) {
        return metricsService.snapshot(request.evaluationResults());
    }

    @PostMapping("/tools/rts_pipeline_release_readiness")
    public Object pipelineReleaseReadiness(@RequestBody ReleaseReadinessRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return pipelineReportService.releaseReadiness(new ReleaseReadinessRequest(request.scope(), request.callerId(), apiKey, request.outputMode()));
    }

    @PostMapping("/tools/rts_trace_report")
    public Object traceReport(@RequestBody TraceReportRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return pipelineReportService.traceReport(request.traceId(), request.callerId(), apiKey);
    }

    @PostMapping("/tools/rts_record_feedback")
    public Object feedback(@RequestBody FeedbackRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return feedbackMemoryService.recordFeedback(new FeedbackRequest(request.traceId(), request.feedbackType(), request.message(),
                request.scope(), request.selectedObjectUri(), request.callerId(), apiKey, request.sessionId()));
    }

    @PostMapping("/tools/rts_write_context_memory")
    public Object memory(@RequestBody MemoryWriteRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return feedbackMemoryService.writeMemory(new MemoryWriteRequest(request.sessionId(), request.memoryType(), request.key(), request.value(),
                request.scope(), request.callerId(), apiKey));
    }

    @PostMapping("/tools/rts_get_context_memory")
    public Object contextMemory(@RequestBody ContextSnapshotRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return feedbackMemoryService.loadContext(new ContextSnapshotRequest(request.sessionId(), request.scope(), request.callerId(), apiKey));
    }

    @SuppressWarnings("unchecked")
    private com.rts.model.CoreModels.ScopeKey scope(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        return new com.rts.model.CoreModels.ScopeKey(String.valueOf(map.get("channel")), String.valueOf(map.get("product")),
                String.valueOf(map.get("pack")), String.valueOf(map.get("domain")));
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private ToolCatalogEntry catalogEntry(String name) {
        ToolCatalogEntry source = toolRegistry.catalogEntry(registryName(name));
        return new ToolCatalogEntry(name, purpose(name, source), permission(name, source), requiredFields(name, source), source.possibleRefusalReasons(),
                schema(name, "input"), schema(name, "output"), source.budgetCost(), allowedIntents(name, source), redactionRule(name, source));
    }

    private String purpose(String name, ToolCatalogEntry source) {
        return switch (name) {
            case "rts_ask" -> "Managed RTS /ask wrapper: RTS plans, reads truth tools, validates claims, and returns grounded answer/refusal.";
            case "rts_contextual_ask" -> "Managed contextual ask wrapper; session/workspace memory is hint-only and never truth.";
            case "rts_record_feedback" -> "Record feedback as non-truth workflow input.";
            case "rts_write_context_memory" -> "Write non-truth context memory; forbidden truth/signoff memory is refused.";
            case "rts_get_context_memory" -> "Read permission-gated, truth-ineligible context memory.";
            default -> source.purpose();
        };
    }

    private String permission(String name, ToolCatalogEntry source) {
        return switch (name) {
            case "rts_ask", "rts_contextual_ask" -> "query";
            case "rts_record_feedback", "rts_write_context_memory", "rts_get_context_memory" -> "feedback_tools";
            case "rts_search_objects" -> "find";
            case "rts_read_object" -> "objects_content";
            case "rts_get_object_card", "rts_get_object_cards" -> "objects_get";
            case "rts_check_release_readiness", "rts_pipeline_release_readiness" -> "analysis_tools";
            case "rts_shape_answer_view", "rts_check_grounding", "rts_get_trace", "rts_trace_report" -> "trace";
            default -> source.requiredPermission();
        };
    }

    private List<String> requiredFields(String name, ToolCatalogEntry source) {
        return switch (name) {
            case "rts_ask" -> List.of("query", "caller_id", "scope_hint");
            case "rts_contextual_ask" -> List.of("query", "caller_id", "scope_hint or session_id/workspace_scope");
            case "rts_record_feedback" -> List.of("trace_id", "feedback_type", "caller_id");
            case "rts_write_context_memory" -> List.of("session_id", "memory_type", "key", "value", "caller_id");
            case "rts_get_context_memory" -> List.of("session_id", "caller_id");
            default -> source.requiredFields();
        };
    }

    private String schema(String name, String direction) {
        return "rts.mcp." + name + ".v1." + direction;
    }

    private List<String> allowedIntents(String name, ToolCatalogEntry source) {
        return switch (name) {
            case "rts_ask", "rts_contextual_ask" -> List.of("managed_answer");
            case "rts_record_feedback", "rts_write_context_memory", "rts_get_context_memory" -> List.of("feedback", "context_memory");
            default -> source.allowedIntents();
        };
    }

    private String redactionRule(String name, ToolCatalogEntry source) {
        if ("rts_record_feedback".equals(name) || "rts_write_context_memory".equals(name) || "rts_get_context_memory".equals(name)) {
            return "hash_feedback_or_memory_payload_keep_scope_and_trace";
        }
        if ("rts_ask".equals(name) || "rts_contextual_ask".equals(name)) {
            return "hash_query_keep_release_scope_trace_uri_and_content_hash";
        }
        return source.traceRedactionRule();
    }

    private String registryName(String mcpName) {
        return switch (mcpName) {
            case "rts_find_objects", "rts_search_objects" -> "find_objects";
            case "rts_read_object", "rts_read_object_l2" -> "read_object_l2";
            case "rts_get_dependencies", "rts_read_rule_dependencies" -> "get_dependencies";
            case "rts_ask", "rts_contextual_ask" -> "resolve_scope";
            case "rts_get_trace" -> "get_trace";
            case "rts_feature_flags" -> "feature_flags";
            case "rts_list_scopes", "rts_get_scope_tree" -> "list_scopes";
            case "rts_search_scopes" -> "search_scopes";
            case "rts_get_scope_summary", "rts_get_pack_status" -> "get_scope_summary";
            case "rts_get_pack_navigation" -> "get_pack_navigation";
            case "rts_get_object_card", "rts_get_object_cards" -> "get_object_card";
            case "rts_get_dependency_subgraph" -> "get_dependency_subgraph";
            case "rts_find_confusable_objects" -> "find_confusable_objects";
            case "rts_find_confusable_scopes" -> "find_confusable_scopes";
            case "rts_find_by_target_path" -> "find_by_target_path";
            case "rts_find_by_source_anchor" -> "find_by_source_anchor";
            case "rts_find_by_lookup_key" -> "find_by_lookup_key";
            case "rts_find_reverse_dependencies" -> "find_reverse_dependencies";
            case "rts_read_agent_object" -> "read_object_l2";
            case "rts_read_lookup_sample" -> "read_object_l2";
            case "rts_read_helper_contract" -> "read_object_l2";
            case "rts_read_evidence_summary" -> "read_evidence_summary";
            case "rts_analyze_impact", "rts_analyze_pr_diff", "rts_investigate_exception" -> "analyze_impact";
            case "rts_analyze_failed_message", "rts_generate_target_message_candidate" -> "parse_raw_message_candidate";
            case "rts_plan_tests", "rts_scenario_plan_tests" -> "plan_tests";
            case "rts_scenario_governance_review" -> "governance_review";
            case "rts_parse_raw_message_candidate" -> "parse_raw_message_candidate";
            case "rts_map_source_fields_to_rules" -> "map_source_fields_to_rules";
            case "rts_resolve_required_lookups" -> "resolve_required_lookups";
            case "rts_simulate_rule_application" -> "simulate_rule_application";
            case "rts_assemble_target_message_candidate" -> "assemble_target_message_candidate";
            case "rts_validate_target_message_grounding" -> "validate_target_message_grounding";
            case "rts_compare_rules" -> "compare_rules";
            case "rts_explain_conflict" -> "explain_conflict";
            case "rts_check_release_readiness" -> "analyze_impact";
            case "rts_check_grounding" -> "check_grounding";
            case "rts_shape_answer_view" -> "check_grounding";
            case "rts_run_evaluation" -> "run_evaluation";
            case "rts_governance_review" -> "governance_review";
            case "rts_record_human_decision" -> "record_human_decision";
            case "rts_metrics_snapshot" -> "metrics_snapshot";
            case "rts_pipeline_release_readiness" -> "analyze_impact";
            case "rts_trace_report" -> "trace_report";
            case "rts_record_feedback", "rts_write_context_memory", "rts_get_context_memory" -> "feature_flags";
            default -> mcpName.startsWith("rts_") ? mcpName.substring(4) : mcpName;
        };
    }
}
