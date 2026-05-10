package com.rts.llm;

import com.rts.config.RtsProperties;
import com.rts.agent.AgentPlannerService;
import com.rts.agent.AgentRecipeService.AgentRecipe;
import com.rts.agent.AgentRecipeService;
import com.rts.agent.RtsToolRegistry;
import com.rts.llm.LlmContracts.LlmClient;
import com.rts.llm.LlmContracts.LlmDraft;
import com.rts.llm.LlmContracts.ToolContext;
import com.rts.llm.LlmContracts.ToolResult;
import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.DependencyEdge;
import com.rts.model.CoreModels.Fact;
import com.rts.model.CoreModels.AgentPlan;
import com.rts.model.CoreModels.LlmRunTrace;
import com.rts.model.CoreModels.Refusal;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.model.AgentServiceModels.BudgetUsage;
import com.rts.model.AgentServiceModels.GroundingMap;
import com.rts.model.AgentServiceModels.ToolStepTrace;
import com.rts.query.FinalAnswerValidator;
import com.rts.query.PromptPolicyGuard;
import com.rts.query.QueryRefusalException;
import com.rts.query.QueryRequests.AskRequest;
import com.rts.query.QueryRequests.DependenciesRequest;
import com.rts.query.QueryRequests.FindRequest;
import com.rts.query.QueryRequests.ObjectContentRequest;
import com.rts.query.QueryRequests.ObjectGetRequest;
import com.rts.query.QueryRequests.PlanRequest;
import com.rts.query.QueryRequests.QueryRequest;
import com.rts.query.QueryService;
import com.rts.store.StoreContracts.ProjectionStore;
import com.rts.store.Hashing;
import com.rts.store.StoreContracts.TraceStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ControlledLlmHarness {
    private final QueryService queryService;
    private final LlmClient llmClient;
    private final FinalAnswerValidator finalAnswerValidator;
    private final PromptPolicyGuard promptPolicyGuard;
    private final TraceStore traceStore;
    private final RtsProperties properties;
    private final RtsToolRegistry toolRegistry;
    private final AgentPlannerService agentPlannerService;
    private final ProjectionStore projectionStore;
    private final AgentRecipeService agentRecipeService;

    public ControlledLlmHarness(QueryService queryService, LlmClient llmClient, FinalAnswerValidator finalAnswerValidator,
            PromptPolicyGuard promptPolicyGuard, TraceStore traceStore, RtsProperties properties) {
        this(queryService, llmClient, finalAnswerValidator, promptPolicyGuard, traceStore, properties, new RtsToolRegistry(), null, null, new AgentRecipeService());
    }

    @Autowired
    public ControlledLlmHarness(QueryService queryService, LlmClient llmClient, FinalAnswerValidator finalAnswerValidator,
            PromptPolicyGuard promptPolicyGuard, TraceStore traceStore, RtsProperties properties, RtsToolRegistry toolRegistry,
            AgentPlannerService agentPlannerService, ProjectionStore projectionStore, AgentRecipeService agentRecipeService) {
        this.queryService = queryService;
        this.llmClient = llmClient;
        this.finalAnswerValidator = finalAnswerValidator;
        this.promptPolicyGuard = promptPolicyGuard;
        this.traceStore = traceStore;
        this.properties = properties;
        this.toolRegistry = toolRegistry;
        this.agentPlannerService = agentPlannerService;
        this.projectionStore = projectionStore;
        this.agentRecipeService = agentRecipeService;
    }

    public ServiceAnswer ask(AskRequest request) {
        if (!properties.isToolOrchestratorEnabled()) {
            return deterministicFallback(request);
        }
        Instant start = Instant.now();
        GuardedToolContext tools = new GuardedToolContext(request.maxToolCalls() == null ? properties.getMaxToolCalls() : request.maxToolCalls());
        String traceId = queryService.newTraceId();
        String runId = newRunId();
        LlmDraft draft;
        AgentPlan agentPlan = null;
        try {
            if (properties.getMaxModelCalls() < 1) {
                throw new QueryRefusalException(RefusalReason.tool_budget_exhausted, "LLM model call budget exhausted");
            }
            promptPolicyGuard.validateUserText(request.query());
            agentPlan = plan(request);
            if (agentPlan.clarificationQuestion() != null && !agentPlan.clarificationQuestion().isBlank()) {
                throw new QueryRefusalException(RefusalReason.scope_unclear, agentPlan.clarificationQuestion());
            }
            tools.bindPlan(agentPlan);
            tools.executeRecipe(request, agentRecipeService.recipeFor(agentPlan));
            try {
                draft = llmClient.draftAnswer(request, tools);
            } catch (QueryRefusalException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                return providerFailure(start, traceId, runId, request, agentPlan, tools, ex);
            }
            enforceLatencyBudget(start);
        } catch (QueryRefusalException ex) {
            queryService.appendTrace(start, traceId, runId, "ask", request.callerId(), request.query(), null,
                    agentPlan, request.scopeHint(), null, List.of(), List.copyOf(tools.l2ReadUris), List.copyOf(tools.l2ReadUris),
                    ex.reason(), tools.calls, tools.toolSteps(), request.outputMode(), null, null);
            BudgetUsage budgetUsage = budgetUsage(start, tools);
            ServiceAnswer refusal = new ServiceAnswer(AnswerType.refusal, request.scopeHint(), null, List.of(), List.of(), List.of(ex.getMessage()),
                    List.of(), List.of(), List.of(), List.of(), traceId, new Refusal(ex.reason(), ex.getMessage(), List.of(), false), List.of(), null);
            traceStore.appendLlmRunTrace(new LlmRunTrace("llm-" + UUID.randomUUID(), traceId, properties.getLlmModel(), "day1-controlled-v1",
                    List.copyOf(tools.calls), Hashing.sha256("refusal"), Hashing.sha256(ex.getMessage()), "invalid:" + ex.reason(),
                    Duration.between(start, Instant.now()).toMillis(), Instant.now()));
            return refusal.withRunId(runId).withValidation(GroundingMap.empty(), budgetUsage, request.outputMode(), null);
        }
        ServiceAnswer grounded = withTraceId(draft.groundedAnswer(), traceId);
        try {
            recheckFinalEmit(request, agentPlan, grounded);
            String finalAnswerText = answerText(grounded, draft);
            ServiceAnswer candidate = new ServiceAnswer(
                    grounded.answerType(),
                    grounded.scope(),
                    grounded.releaseId(),
                    grounded.facts(),
                    grounded.inferences(),
                    grounded.unknowns(),
                    grounded.candidateSuggestions(),
                    grounded.humanDecisions(),
                    grounded.citedObjects(),
                    grounded.dependencies(),
                    grounded.traceId(),
                    grounded.refusal(),
                    grounded.warnings(),
                    finalAnswerText);
            GroundingMap groundingMap = GroundingMap.empty();
            if (candidate.answerType() == AnswerType.answer) {
                groundingMap = validateFinalClaims(candidate, tools);
            } else {
                promptPolicyGuard.validateGeneratedAnswer(finalAnswerText);
            }
            enforceLatencyBudget(start);
            queryService.appendTrace(start, traceId, runId, "ask", request.callerId(), request.query(), null,
                    agentPlan, grounded.scope(), grounded.releaseId(), grounded.citedObjects(), grounded.citedObjects(), tools.l2ReadUris,
                    grounded.refusal() == null ? RefusalReason.none : grounded.refusal().reason(), draft.toolCalls(), tools.toolSteps(),
                    request.outputMode(), null, null, groundingMap);
            appendLlmTrace(start, grounded.traceId(), draft, "valid");
            return candidate.withRunId(runId).withValidation(groundingMap, budgetUsage(start, tools), request.outputMode(), finalAnswerText);
        } catch (QueryRefusalException ex) {
            queryService.appendTrace(start, traceId, runId, "ask", request.callerId(), request.query(), null,
                    agentPlan, grounded.scope(), grounded.releaseId(), List.of(), List.of(), List.of(), ex.reason(), draft.toolCalls(),
                    tools.toolSteps(), request.outputMode(), null, null);
            appendLlmTrace(start, grounded.traceId(), draft, "invalid:" + ex.reason());
            return new ServiceAnswer(AnswerType.refusal, grounded.scope(), grounded.releaseId(), List.of(), List.of(),
                    List.of(ex.getMessage()), List.of(), List.of(), List.of(), List.of(), grounded.traceId(),
                    new Refusal(ex.reason(), ex.getMessage(), List.of(), false), List.of(), null)
                    .withRunId(runId)
                    .withValidation(GroundingMap.empty(), budgetUsage(start, tools), request.outputMode(), null);
        }
    }

    private GroundingMap validateFinalClaims(ServiceAnswer candidate, GuardedToolContext tools) {
        if (properties.isClaimValidatorEnabled()) {
            return finalAnswerValidator.validateClaims(candidate, Map.copyOf(tools.l2ReadHashes));
        }
        // This bypass is intentionally narrow and observable: it helps debug model
        // shaping without changing the service-owned tool, scope, release, or prompt guards.
        promptPolicyGuard.validateGeneratedAnswer(candidate.answer());
        return GroundingMap.empty();
    }

    private ServiceAnswer deterministicFallback(AskRequest request) {
        ServiceAnswer answer = queryService.query(new QueryRequest(request.query(), request.callerId(), request.apiKey(),
                request.scopeHint(), request.outputMode(), false));
        List<String> warnings = new ArrayList<>(answer.warnings() == null ? List.of() : answer.warnings());
        warnings.add("Managed tool orchestrator is disabled; returned deterministic query fallback.");
        return new ServiceAnswer(answer.answerType(), answer.scope(), answer.releaseId(), answer.facts(), answer.inferences(),
                answer.unknowns(), answer.candidateSuggestions(), answer.humanDecisions(), answer.citedObjects(),
                answer.dependencies(), answer.traceId(), answer.runId(), answer.refusal(), List.copyOf(warnings), answer.answer(),
                answer.schemaVersion(), answer.compatibilityNote(), answer.answerView(), answer.groundingMap(), answer.budgetUsage());
    }

    private ServiceAnswer providerFailure(Instant start, String traceId, String runId, AskRequest request, AgentPlan agentPlan,
            GuardedToolContext tools, RuntimeException ex) {
        RefusalReason reason = RefusalReason.model_provider_failure;
        queryService.appendTrace(start, traceId, runId, "ask", request.callerId(), request.query(), null,
                agentPlan, request.scopeHint(), null, List.of(), List.copyOf(tools.l2ReadUris), List.copyOf(tools.l2ReadUris),
                reason, tools.calls, tools.toolSteps(), request.outputMode(), null, null);
        BudgetUsage budgetUsage = budgetUsage(start, tools);
        String message = "LLM provider failure; deterministic truth APIs remain available";
        traceStore.appendLlmRunTrace(new LlmRunTrace("llm-" + UUID.randomUUID(), traceId, properties.getLlmModel(), "day1-controlled-v1",
                List.copyOf(tools.calls), Hashing.sha256("provider_failure"), Hashing.sha256(ex.getClass().getName()), "invalid:provider_failure",
                Duration.between(start, Instant.now()).toMillis(), Instant.now()));
        return new ServiceAnswer(AnswerType.refusal, request.scopeHint(), null, List.of(), List.of(), List.of(message),
                List.of(), List.of(), List.of(), List.of(), traceId,
                new Refusal(reason, message, List.of("retry later", "use /api/v1/query for deterministic truth read"), false),
                List.of("Provider error was converted to structured refusal."), null)
                .withRunId(runId)
                .withValidation(GroundingMap.empty(), budgetUsage, request.outputMode(), null);
    }

    private ServiceAnswer withTraceId(ServiceAnswer answer, String traceId) {
        return new ServiceAnswer(answer.answerType(), answer.scope(), answer.releaseId(), safeFacts(answer.facts()), safeStrings(answer.inferences()),
                safeStrings(answer.unknowns()), safeStrings(answer.candidateSuggestions()), safeStrings(answer.humanDecisions()),
                safeStrings(answer.citedObjects()), safeEdges(answer.dependencies()), traceId, answer.refusal(), safeStrings(answer.warnings()),
                replaceTraceId(answer.answer(), answer.traceId(), traceId))
                .withRunId(answer.runId());
    }

    private String newRunId() {
        return "run-" + UUID.randomUUID();
    }

    private String answerText(ServiceAnswer grounded, LlmDraft draft) {
        if (grounded.answer() != null && !grounded.answer().isBlank()) {
            return grounded.answer();
        }
        return draft.text();
    }

    private String replaceTraceId(String answerText, String oldTraceId, String newTraceId) {
        if (answerText == null) {
            return null;
        }
        if (oldTraceId == null || oldTraceId.isBlank()) {
            return answerText;
        }
        return answerText.replace(oldTraceId, newTraceId);
    }

    private List<Fact> safeFacts(List<Fact> values) {
        return values == null ? List.of() : values;
    }

    private List<String> safeStrings(List<String> values) {
        return values == null ? List.of() : values;
    }

    private List<DependencyEdge> safeEdges(List<DependencyEdge> values) {
        return values == null ? List.of() : values;
    }

    private void appendLlmTrace(Instant start, String traceId, LlmDraft draft, String validation) {
        traceStore.appendLlmRunTrace(new LlmRunTrace(
                "llm-" + UUID.randomUUID(),
                traceId,
                properties.getLlmModel(),
                "day1-controlled-v1",
                draft.toolCalls(),
                Hashing.sha256(String.valueOf(draft.groundedAnswer())),
                Hashing.sha256(String.valueOf(draft.text())),
                validation,
                Duration.between(start, Instant.now()).toMillis(),
                Instant.now()));
    }

    private void enforceLatencyBudget(Instant start) {
        if (Duration.between(start, Instant.now()).toMillis() > properties.getMaxLatencyMs()) {
            throw new QueryRefusalException(RefusalReason.tool_budget_exhausted, "LLM latency budget exhausted");
        }
    }

    private AgentPlan plan(AskRequest request) {
        if (agentPlannerService != null) {
            return agentPlannerService.plan(request.query(), request.callerId(), request.scopeHint(), request.outputMode(), "ask");
        }
        var queryPlan = queryService.plan(new PlanRequest(request.query(), request.callerId(), request.scopeHint(), request.outputMode(), true));
        return agentRecipeService.applyRecipe(AgentPlan.fromQueryPlan(queryPlan, "ask", null));
    }

    private void recheckFinalEmit(AskRequest request, AgentPlan plan, ServiceAnswer answer) {
        if (answer.answerType() != AnswerType.answer) {
            return;
        }
        if (plan != null && plan.scope() != null && !plan.scope().matches(answer.scope())) {
            throw new QueryRefusalException(RefusalReason.unauthorized_scope, "Final answer scope drifted from the planner scope snapshot");
        }
        if (plan != null && plan.releaseId() != null && answer.releaseId() != null && !plan.releaseId().equals(answer.releaseId())) {
            throw new QueryRefusalException(RefusalReason.unauthorized_scope, "Final answer release drifted from the planner release snapshot");
        }
        if (projectionStore != null && answer.releaseId() != null) {
            String activeReleaseId = projectionStore.loadActiveSnapshot().manifest().releaseId();
            if (!activeReleaseId.equals(answer.releaseId())) {
                throw new QueryRefusalException(RefusalReason.unauthorized_scope, "Final answer release is no longer the active release");
            }
        }
        if (answer.scope() != null && answer.citedObjects() != null) {
            for (String uri : answer.citedObjects()) {
                queryService.getObject(new ObjectGetRequest(uri, answer.releaseId(), answer.traceId(), request.callerId(), request.apiKey()));
            }
        }
    }

    private BudgetUsage budgetUsage(Instant start, GuardedToolContext tools) {
        return new BudgetUsage(
                tools.calls.size(),
                tools.maxCalls,
                tools.l2ReadUris.size(),
                properties.getMaxL2Objects(),
                dependencyDepthUsed(tools.calls),
                properties.getMaxDependencyDepth(),
                tools.retrievedTokensUsed,
                properties.getMaxRetrievedTokens(),
                1,
                properties.getMaxModelCalls(),
                Duration.between(start, Instant.now()).toMillis(),
                properties.getMaxLatencyMs());
    }

    private int dependencyDepthUsed(List<String> calls) {
        return calls.stream().anyMatch("get_dependencies"::equals) ? 1 : 0;
    }

    private final class GuardedToolContext implements ToolContext {
        private final int maxCalls;
        private final List<String> calls = new ArrayList<>();
        private final List<String> l2ReadUris = new ArrayList<>();
        private final Map<String, String> l2ReadHashes = new LinkedHashMap<>();
        private final List<ToolStepTrace> toolSteps = new ArrayList<>();
        private final Map<String, Object> plannedOutputs = new LinkedHashMap<>();
        private AgentPlan activePlan;
        private AgentRecipe activeRecipe;
        private List<String> plannedTools = new ArrayList<>();
        private int replayCursor;
        private boolean replayingPlan;
        private boolean planExecuted;
        private int retrievedTokensUsed;

        private GuardedToolContext(int maxCalls) {
            this.maxCalls = maxCalls;
        }

        private void bindPlan(AgentPlan plan) {
            this.activePlan = plan;
            this.plannedTools = plan == null || plan.toolPlan() == null ? new ArrayList<>() : new ArrayList<>(plan.toolPlan());
        }

        private void executeRecipe(AskRequest request, AgentRecipe recipe) {
            this.activeRecipe = recipe;
            if (recipe != null) {
                this.plannedTools = recipe.toolSequence() == null ? new ArrayList<>() : new ArrayList<>(recipe.toolSequence());
            }
            if (plannedTools.isEmpty()) {
                return;
            }
            replayingPlan = true;
            try {
                com.rts.model.CoreModels.QueryPlan queryPlan = null;
                List<com.rts.model.CoreModels.CandidateObject> candidates = List.of();
                String selectedUri = null;
                int planIndex = 0;
                while (planIndex < plannedTools.size()) {
                    String toolName = plannedTools.get(planIndex++);
                    if ("resolve_scope".equals(toolName)) {
                        queryPlan = (com.rts.model.CoreModels.QueryPlan) call(toolName,
                                new PlanRequest(request.query(), request.callerId(), request.scopeHint(), request.outputMode(), true)).output();
                    } else if ("find_objects".equals(toolName)) {
                        com.rts.model.CoreModels.QueryPlan safePlan = queryPlan == null ? queryService.plan(
                                new PlanRequest(request.query(), request.callerId(), request.scopeHint(), request.outputMode(), true)) : queryPlan;
                        @SuppressWarnings("unchecked")
                        List<com.rts.model.CoreModels.CandidateObject> found = (List<com.rts.model.CoreModels.CandidateObject>) call(toolName,
                                new FindRequest(request.query(), safePlan.scope(), objectTypesForIntent(safePlan.intent()), safePlan.anchors(),
                                        5, request.callerId(), request.apiKey(), request.outputMode())).output();
                        candidates = found;
                        if (candidates.isEmpty()) {
                            throw new QueryRefusalException(RefusalReason.object_not_found, "No released structured object matched the query");
                        }
                        selectedUri = candidates.get(0).uri();
                    } else if ("get_object_card".equals(toolName)) {
                        selectedUri = requireSelectedUri(selectedUri, candidates);
                        call(toolName, new ObjectGetRequest(selectedUri, null, null, request.callerId(), request.apiKey()));
                    } else if ("read_object_l2".equals(toolName)) {
                        selectedUri = requireSelectedUri(selectedUri, candidates);
                        call(toolName, new ObjectContentRequest(selectedUri, "answer", null, null, request.callerId(), request.apiKey()));
                    } else if ("get_dependencies".equals(toolName)) {
                        selectedUri = requireSelectedUri(selectedUri, candidates);
                        ToolResult dependencies = call(toolName, new DependenciesRequest(selectedUri, dependencyDirection(), null,
                                1, toolPurpose(), null, request.callerId(), request.apiKey()));
                        reviseFromObservation(request, selectedUri, dependencies);
                        planIndex = Math.max(planIndex, replayCursor);
                    } else {
                        throw new QueryRefusalException(RefusalReason.unsupported_claim,
                                "Tool is registered but not enabled for managed harness: " + toolName);
                    }
                }
                planExecuted = true;
            } finally {
                replayingPlan = false;
                replayCursor = 0;
                activeRecipe = null;
            }
        }

        private void reviseFromObservation(AskRequest request, String selectedUri, ToolResult observation) {
            if (!requiresDependencyL2Evidence() || observation == null
                    || !(observation.output() instanceof com.rts.model.CoreModels.DependencyResult dependencies)) {
                return;
            }
            List<String> dependencyUris = dependencies.objects().stream()
                    .map(com.rts.model.CoreModels.ObjectManifestEntry::uri)
                    .filter(uri -> uri != null && !uri.isBlank())
                    .filter(uri -> !Objects.equals(uri, selectedUri))
                    .filter(uri -> !l2ReadUris.contains(uri))
                    .distinct()
                    .limit(Math.max(0, properties.getMaxL2Objects() - l2ReadUris.size()))
                    .toList();
            if (dependencyUris.isEmpty()) {
                return;
            }
            for (String dependencyUri : dependencyUris) {
                plannedTools.add(replayCursor, "read_object_l2");
                call("read_object_l2", new ObjectContentRequest(dependencyUri, toolPurpose(), null, null, request.callerId(), request.apiKey()));
                markLastStepAsServiceRevise("revise_dependency_l2");
            }
        }

        private boolean requiresDependencyL2Evidence() {
            return activeRecipe != null
                    && activeRecipe.requiredEvidence() != null
                    && activeRecipe.requiredEvidence().stream().anyMatch("dependency L2 runtime object"::equals);
        }

        @Override
        public ToolResult call(String toolName, Object input) {
            if (planExecuted && !replayingPlan) {
                recordRefusedStep(toolName, input, selectedUrisFor(toolName, input), RefusalReason.unsupported_claim);
                throw new QueryRefusalException(RefusalReason.unsupported_claim,
                        "Model clients cannot initiate RTS tool calls after the service recipe executor executes the plan; use plannedOutputs");
            }
            if (calls.size() >= maxCalls) {
                recordRefusedStep(toolName, input, selectedUrisFor(toolName, input), RefusalReason.tool_budget_exhausted);
                throw new QueryRefusalException(RefusalReason.tool_budget_exhausted, "LLM tool budget exhausted");
            }
            if ("read_object_l2".equals(toolName) && l2ReadUris.size() >= properties.getMaxL2Objects()) {
                recordRefusedStep(toolName, input, selectedUrisFor(toolName, input), RefusalReason.tool_budget_exhausted);
                throw new QueryRefusalException(RefusalReason.tool_budget_exhausted, "LLM L2 read budget exhausted");
            }
            if (estimateRetrievedTokens(input) >= properties.getMaxRetrievedTokens()) {
                recordRefusedStep(toolName, input, selectedUrisFor(toolName, input), RefusalReason.tool_budget_exhausted);
                throw new QueryRefusalException(RefusalReason.tool_budget_exhausted, "LLM retrieved-token budget exhausted");
            }
            try {
                toolRegistry.require(toolName);
                requirePlannedTool(toolName);
            } catch (QueryRefusalException ex) {
                recordRefusedStep(toolName, input, selectedUrisFor(toolName, input), ex.reason());
                throw ex;
            }
            calls.add(toolName);
            int stepNo = calls.size();
            String inputHash = Hashing.sha256(String.valueOf(input));
            String policyResult = "allowed";
            List<String> selectedUris = selectedUrisFor(toolName, input);
            Object output;
            try {
                output = toolRegistry.execute(toolName, input, ignored -> switch (toolName) {
                    case "resolve_scope" -> queryService.plan((PlanRequest) input);
                    case "find_objects" -> queryService.find((FindRequest) input);
                    case "get_object_card" -> queryService.getObject((ObjectGetRequest) input);
                    case "read_object_l2" -> queryService.readContent((ObjectContentRequest) input);
                    case "get_dependencies" -> queryService.dependencies((com.rts.query.QueryRequests.DependenciesRequest) input);
                    default -> throw new QueryRefusalException(RefusalReason.unsupported_claim, "Tool is registered but not enabled for managed harness: " + toolName);
                });
            } catch (QueryRefusalException ex) {
                toolSteps.add(new ToolStepTrace(stepNo, toolName, inputHash, Hashing.sha256("refused:" + ex.reason()), selectedUris,
                        "refused:" + ex.reason()));
                throw ex;
            }
            selectedUris = selectedUrisFor(toolName, output, selectedUris);
            if ("read_object_l2".equals(toolName) && output instanceof com.rts.model.CoreModels.L2Content l2Content) {
                l2ReadUris.add(l2Content.uri());
                l2ReadHashes.put(l2Content.uri(), l2Content.contentHash());
            }
            plannedOutputs.put(toolName + "#" + stepNo, output);
            plannedOutputs.putIfAbsent(toolName, output);
            if ("read_object_l2".equals(toolName) && output instanceof com.rts.model.CoreModels.L2Content l2Content) {
                @SuppressWarnings("unchecked")
                List<com.rts.model.CoreModels.L2Content> existing =
                        (List<com.rts.model.CoreModels.L2Content>) plannedOutputs.getOrDefault("read_object_l2:all", List.of());
                List<com.rts.model.CoreModels.L2Content> all = new ArrayList<>(existing);
                all.add(l2Content);
                plannedOutputs.put("read_object_l2:all", List.copyOf(all));
            }
            int outputTokens = estimateRetrievedTokens(output);
            if (retrievedTokensUsed + outputTokens >= properties.getMaxRetrievedTokens()) {
                toolSteps.add(new ToolStepTrace(stepNo, toolName, inputHash, Hashing.sha256(String.valueOf(output)), selectedUris,
                        "refused:" + RefusalReason.tool_budget_exhausted));
                throw new QueryRefusalException(RefusalReason.tool_budget_exhausted, "LLM retrieved-token budget exhausted");
            }
            retrievedTokensUsed += outputTokens;
            toolSteps.add(new ToolStepTrace(stepNo, toolName, inputHash, Hashing.sha256(String.valueOf(output)), selectedUris, policyResult));
            return new ToolResult(toolName, output);
        }

        private void markLastStepAsServiceRevise(String reason) {
            if (toolSteps.isEmpty()) {
                return;
            }
            ToolStepTrace last = toolSteps.remove(toolSteps.size() - 1);
            toolSteps.add(new ToolStepTrace(last.stepNo(), last.toolName(), last.toolInputHash(), last.toolOutputHash(),
                    last.selectedUris(), "allowed:" + reason));
        }

        @Override
        public Map<String, Object> plannedOutputs() {
            return Collections.unmodifiableMap(plannedOutputs);
        }

        private List<ToolStepTrace> toolSteps() {
            return List.copyOf(toolSteps);
        }

        private void recordRefusedStep(String toolName, Object input, List<String> selectedUris, RefusalReason reason) {
            toolSteps.add(new ToolStepTrace(
                    calls.size() + 1,
                    toolName,
                    Hashing.sha256(String.valueOf(input)),
                    Hashing.sha256("refused:" + reason),
                    selectedUris == null ? List.of() : selectedUris,
                    "refused:" + reason));
        }

        private void requirePlannedTool(String toolName) {
            if (plannedTools.isEmpty()) {
                return;
            }
            int step = replayingPlan ? replayCursor++ : calls.size();
            if (step >= plannedTools.size()) {
                throw new QueryRefusalException(RefusalReason.unsupported_claim, "Tool call exceeds the planned RTS tool sequence: " + toolName);
            }
            String expected = plannedTools.get(step);
            if (!"*".equals(expected) && !expected.equals(toolName)) {
                String recipe = activeRecipe == null ? "unknown" : activeRecipe.recipeVersion();
                throw new QueryRefusalException(RefusalReason.unsupported_claim,
                        "Tool call does not match RTS recipe " + recipe + "; expected " + expected + " but got " + toolName);
            }
        }

        private int estimateRetrievedTokens(Object value) {
            return String.valueOf(value).length() / 4;
        }

        private List<String> selectedUrisFor(String toolName, Object value) {
            return selectedUrisFor(toolName, value, List.of());
        }

        private List<String> selectedUrisFor(String toolName, Object value, List<String> fallback) {
            if (value instanceof ObjectGetRequest request) {
                return request.uri() == null ? List.of() : List.of(request.uri());
            }
            if (value instanceof ObjectContentRequest request) {
                return request.uri() == null ? List.of() : List.of(request.uri());
            }
            if (value instanceof DependenciesRequest request) {
                return request.uri() == null ? List.of() : List.of(request.uri());
            }
            if (value instanceof com.rts.model.CoreModels.L2Content l2Content) {
                return List.of(l2Content.uri());
            }
            if (value instanceof com.rts.model.CoreModels.DependencyResult dependencyResult) {
                List<String> uris = dependencyResult.objects().stream().map(com.rts.model.CoreModels.ObjectManifestEntry::uri).toList();
                return uris.isEmpty() ? fallback : uris;
            }
            if (value instanceof com.rts.query.QueryService.ObjectEnvelope envelope) {
                return List.of(envelope.objectManifest().uri());
            }
            if (value instanceof List<?> values && ("find_objects".equals(toolName) || "find_by_target_path".equals(toolName))) {
                List<String> uris = values.stream()
                        .filter(com.rts.model.CoreModels.CandidateObject.class::isInstance)
                        .map(com.rts.model.CoreModels.CandidateObject.class::cast)
                        .map(com.rts.model.CoreModels.CandidateObject::uri)
                        .toList();
                return uris.isEmpty() ? fallback : uris;
            }
            return fallback == null ? List.of() : fallback;
        }

        private String requireSelectedUri(String selectedUri, List<com.rts.model.CoreModels.CandidateObject> candidates) {
            if (selectedUri != null && !selectedUri.isBlank()) {
                return selectedUri;
            }
            if (candidates != null && !candidates.isEmpty()) {
                return candidates.get(0).uri();
            }
            throw new QueryRefusalException(RefusalReason.object_not_found, "No selected object available for planned tool execution");
        }

        private List<String> objectTypesForIntent(String intent) {
            if ("lookup_lookup".equals(intent)) {
                return List.of("lookup");
            }
            if ("helper_lookup".equals(intent)) {
                return List.of("helper");
            }
            if ("rule_lookup".equals(intent) || "explain_rule".equals(intent) || "generate_target_message".equals(intent)
                    || "compare_source_target".equals(intent) || "test_planning".equals(intent) || "confidence_check".equals(intent)) {
                return List.of("rule");
            }
            return List.of();
        }

        private com.rts.model.CoreModels.Direction dependencyDirection() {
            if (activePlan != null && "impact_preview".equals(activePlan.intent())) {
                return com.rts.model.CoreModels.Direction.reverse;
            }
            if (activePlan != null && "dependency_lookup".equals(activePlan.intent())) {
                return com.rts.model.CoreModels.Direction.both;
            }
            return com.rts.model.CoreModels.Direction.forward;
        }

        private String toolPurpose() {
            if (activePlan == null || activePlan.intent() == null || activePlan.intent().isBlank()) {
                return "answer";
            }
            return activePlan.intent();
        }
    }
}
