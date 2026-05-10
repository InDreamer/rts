package com.rts.agent;

import com.rts.model.AgentServiceModels.BudgetUsage;
import com.rts.model.AgentServiceModels.Citation;
import com.rts.model.AgentServiceModels.GroundedClaim;
import com.rts.model.AgentServiceModels.GroundingEvidence;
import com.rts.model.AgentServiceModels.GroundingMap;
import com.rts.model.AgentServiceModels.ImpactAnalysisRequest;
import com.rts.model.AgentServiceModels.ImpactCandidate;
import com.rts.model.AgentServiceModels.GovernanceReviewRequest;
import com.rts.model.AgentServiceModels.ManagedScenarioRequest;
import com.rts.model.AgentServiceModels.RawMessageCandidateRequest;
import com.rts.model.AgentServiceModels.ScenarioReport;
import com.rts.model.AgentServiceModels.TestPlanRequest;
import com.rts.model.AgentServiceModels.ValidationStatus;
import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.Fact;
import com.rts.model.CoreModels.Refusal;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.llm.ControlledLlmHarness;
import com.rts.query.QueryRequests.AskRequest;
import com.rts.model.CoreModels.TraceRecord;
import com.rts.query.PromptPolicyGuard;
import com.rts.query.QueryRefusalException;
import com.rts.query.QueryService;
import com.rts.store.Hashing;
import com.rts.store.StoreContracts.ProjectionStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ManagedScenarioService {
    private final AgentAnalysisService analysisService;
    private final GovernanceAssistantService governanceAssistantService;
    private final ProjectionStore projectionStore;
    private final QueryService queryService;
    private final PromptPolicyGuard promptPolicyGuard;
    private final ControlledLlmHarness controlledLlmHarness;
    private final AgentRecipeService agentRecipeService;

    public ManagedScenarioService(AgentAnalysisService analysisService, GovernanceAssistantService governanceAssistantService,
            ProjectionStore projectionStore, QueryService queryService, PromptPolicyGuard promptPolicyGuard,
            ControlledLlmHarness controlledLlmHarness, AgentRecipeService agentRecipeService) {
        this.analysisService = analysisService;
        this.governanceAssistantService = governanceAssistantService;
        this.projectionStore = projectionStore;
        this.queryService = queryService;
        this.promptPolicyGuard = promptPolicyGuard;
        this.controlledLlmHarness = controlledLlmHarness;
        this.agentRecipeService = agentRecipeService;
    }

    public ScenarioReport analyzePrDiff(ManagedScenarioRequest request) {
        return withScenarioRefusal(request, "pr_diff_impact", "PR diff input is required", input -> {
            String anchor = extractFirstAnchor(input);
            var impact = analysisService.analyzeImpact(new ImpactAnalysisRequest(
                    null,
                    anchor,
                    anchor,
                    request.scope(),
                    request.callerId(),
                    request.apiKey(),
                    request.outputMode(),
                    true,
                    request.maxObjects()));
            List<String> candidates = impact.candidates().stream()
                    .map(candidate -> "Impact candidate: " + candidate.impactedObjectUri() + " via " + candidate.dependencyPath())
                    .toList();
            if (candidates.isEmpty() && !impact.facts().isEmpty()) {
                candidates = impact.facts().stream()
                        .map(fact -> "Impact candidate: changed governed object itself " + fact.uri())
                        .toList();
            }
            List<String> nextEvidence = new ArrayList<>();
            if (anchor == null || anchor.isBlank()) {
                nextEvidence.add("Provide changed source field, target path, rule id, lookup id, helper id, or object URI.");
            }
            if (impact.candidates().isEmpty()) {
                nextEvidence.add("Provide a stronger governed anchor from the diff.");
            }
            ServiceAnswer managed = managedScenarioAsk(request, "pr_diff_impact", input, anchor);
            List<String> inferences = new ArrayList<>(impact.inferences());
            List<String> warnings = new ArrayList<>(impact.warnings());
            String traceId = impact.traceId();
            String runId = null;
            if (managed != null) {
                traceId = managed.traceId();
                runId = managed.runId();
                inferences.addAll(managed.inferences());
                if (managed.answerType() == AnswerType.answer) {
                    inferences.add("Managed harness recipe " + queryService.trace(managed.traceId())
                            .map(trace -> trace.agentRun() == null ? "unknown" : trace.agentRun().recipeVersion())
                            .orElse("unknown") + " executed before scenario report compilation.");
                } else if (managed.refusal() != null) {
                    warnings.add("Managed harness returned refusal before scenario compilation: " + managed.refusal().reason());
                }
                nextEvidence.addAll(managed.unknowns());
            }
            ScenarioReport report = report("pr_diff_impact", summarize(input), impact.status(), impact.scope(), impact.releaseId(), impact.facts(),
                    inferences, candidates, impact.unknowns(), nextEvidence, impact.groundingMap(), traceId, runId,
                    citations(impact.releaseId(), impact.facts()), null, warnings);
            appendScenarioTrace(request, input, report, List.of("extract_diff_anchors", "analyze_impact", "check_grounding"));
            return report;
        });
    }

    public ScenarioReport investigateException(ManagedScenarioRequest request) {
        return withScenarioRefusal(request, "exception_investigation", "Exception or log input is required", input -> {
            String anchor = extractFirstAnchor(input);
            var impact = analysisService.analyzeImpact(new ImpactAnalysisRequest(
                    null,
                    anchor,
                    anchor,
                    request.scope(),
                    request.callerId(),
                    request.apiKey(),
                    request.outputMode(),
                    true,
                    request.maxObjects()));
            List<String> candidates = impact.candidates().stream()
                    .map(ImpactCandidate::impactedObjectUri)
                    .map(uri -> "Investigation path candidate: inspect " + uri + " and its released dependencies")
                    .toList();
            if (candidates.isEmpty() && !impact.facts().isEmpty()) {
                candidates = impact.facts().stream()
                        .map(fact -> "Investigation path candidate: inspect " + fact.uri() + " and its released dependencies")
                        .toList();
            }
            List<String> unknowns = new ArrayList<>(impact.unknowns());
            unknowns.add("External stack trace, log text, and failure location are not truth sources.");
            List<String> nextEvidence = List.of("Provide failed message fields, exact target path, rule/helper/lookup id, and runtime trace id when available.");
            ServiceAnswer managed = managedScenarioAsk(request, "exception_investigation", input, anchor);
            List<String> inferences = new ArrayList<>(impact.inferences());
            List<String> warnings = new ArrayList<>(impact.warnings());
            List<String> mutableNextEvidence = new ArrayList<>(nextEvidence);
            mergeManaged(managed, inferences, mutableNextEvidence, warnings);
            ScenarioReport report = report("exception_investigation", summarize(input), impact.status(), impact.scope(), impact.releaseId(), impact.facts(),
                    inferences, candidates, unknowns, mutableNextEvidence, impact.groundingMap(), traceId(managed, impact.traceId()), runId(managed),
                    citations(impact.releaseId(), impact.facts()), null, warnings);
            appendScenarioTrace(request, input, report, List.of("extract_exception_anchors", "analyze_impact", "check_grounding"));
            return report;
        });
    }

    public ScenarioReport analyzeFailedMessage(ManagedScenarioRequest request) {
        return withScenarioRefusal(request, "failed_message_analysis", "Failed message input is required", input -> {
            var result = analysisService.generateRawMessageCandidate(new RawMessageCandidateRequest(
                    input,
                    request.scope(),
                    request.callerId(),
                    request.apiKey(),
                    request.outputMode(),
                    request.maxObjects()));
            List<String> candidates = result.targetCandidates().stream()
                    .map(candidate -> "Target candidate: " + candidate.targetPath() + " from " + candidate.ruleUri())
                    .toList();
            List<Fact> facts = result.targetCandidates().stream()
                    .filter(candidate -> !candidate.groundedBy().isEmpty())
                    .map(candidate -> new Fact("Failed message candidate grounded by rule " + candidate.ruleUri(),
                            candidate.ruleUri(), result.releaseId(), "l2:" + candidate.groundedBy().get(0).l2Hash()))
                    .toList();
            List<String> nextEvidence = result.targetCandidates().isEmpty()
                    ? List.of("Provide parseable source fields that map to released source anchors.")
                    : List.of("Provide production execution context if final transformation behavior is required.");
            ServiceAnswer managed = managedScenarioAsk(request, "failed_message_analysis", input, null);
            List<String> inferences = new ArrayList<>();
            inferences.add("Parsed message fields are clues only; grounded candidates come from released RTS rules.");
            List<String> warnings = new ArrayList<>(result.warnings());
            List<String> mutableNextEvidence = new ArrayList<>(nextEvidence);
            mergeManaged(managed, inferences, mutableNextEvidence, warnings);
            ScenarioReport report = report("failed_message_analysis", summarize(input), result.status(), result.scope(), result.releaseId(), facts,
                    inferences, candidates, result.unknowns(), mutableNextEvidence, result.groundingMap(), traceId(managed, result.traceId()), runId(managed),
                    citations(result.releaseId(), facts), null, warnings);
            appendScenarioTrace(request, input, report, List.of("parse_raw_message_candidate", "map_source_fields_to_rules", "validate_target_message_grounding"));
            return report;
        });
    }

    public ScenarioReport planTests(ManagedScenarioRequest request) {
        return withScenarioRefusal(request, "test_planning", "Test planning input is required", input -> {
            var result = analysisService.planTests(new TestPlanRequest(
                    extractFirstAnchor(input),
                    request.scope(),
                    request.callerId(),
                    request.apiKey(),
                    request.outputMode(),
                    true,
                    request.maxObjects()));
            List<String> candidates = new ArrayList<>();
            candidates.addAll(result.positiveTestCandidates());
            candidates.addAll(result.negativeTestCandidates());
            candidates.addAll(result.boundaryCases());
            candidates.addAll(result.regressionFocus());
            ServiceAnswer managed = managedScenarioAsk(request, "test_planning", input, extractFirstAnchor(input));
            List<String> inferences = new ArrayList<>();
            inferences.add("Test planning candidates are derived from released RTS objects and dependency traversal.");
            List<String> warnings = new ArrayList<>(result.warnings());
            List<String> nextEvidence = new ArrayList<>(result.dependencyCoverageSuggestions());
            mergeManaged(managed, inferences, nextEvidence, warnings);
            ScenarioReport report = report("test_planning", summarize(input), result.status(), result.scope(), result.releaseId(),
                    result.facts(),
                    inferences,
                    candidates,
                    result.unknowns(),
                    nextEvidence,
                    groundingFromFacts(result.facts()),
                    traceId(managed, result.traceId()),
                    runId(managed),
                    citations(result.releaseId(), result.facts()),
                    null,
                    warnings);
            appendScenarioTrace(request, input, report, List.of("plan_tests", "check_grounding"));
            return report;
        });
    }

    public ScenarioReport reviewGovernance(ManagedScenarioRequest request) {
        return withScenarioRefusal(request, "governance_review", "Governance review input is required", input -> {
            var review = governanceAssistantService.review(new GovernanceReviewRequest(
                    request.scope(),
                    extractFirstAnchor(input),
                    request.callerId(),
                    request.apiKey(),
                    request.outputMode(),
                    true));
            List<String> candidates = new ArrayList<>();
            candidates.addAll(review.conflictCandidates());
            candidates.addAll(review.ambiguityCandidates());
            candidates.addAll(review.reviewerQuestions().stream().map(question -> question.question() + " options=" + question.options()).toList());
            ServiceAnswer managed = managedScenarioAsk(request, "governance_review", input, extractFirstAnchor(input));
            List<String> inferences = new ArrayList<>();
            inferences.add("Governance review output is candidate material and cannot mutate runtime truth.");
            List<String> warnings = new ArrayList<>(review.warnings());
            List<String> nextEvidence = new ArrayList<>();
            nextEvidence.add("Read authorized governance summaries or provide recorded human decision references when needed.");
            mergeManaged(managed, inferences, nextEvidence, warnings);
            ScenarioReport report = report("governance_review", summarize(input), review.status(), review.scope(), review.releaseId(),
                    review.facts(),
                    inferences,
                    candidates,
                    List.of("Human adjudication is required for material governance decisions."),
                    nextEvidence,
                    groundingFromFacts(review.facts()),
                    traceId(managed, review.traceId()),
                    runId(managed),
                    citations(review.releaseId(), review.facts()),
                    null,
                    warnings);
            appendScenarioTrace(request, input, report, List.of("governance_review", "check_grounding"));
            return report;
        });
    }

    private ScenarioReport report(String type, String summary, String status, com.rts.model.CoreModels.ScopeKey scope, String releaseId,
            List<Fact> facts, List<String> inferences, List<String> candidates, List<String> unknowns, List<String> nextEvidenceNeeded,
            GroundingMap groundingMap, String traceId, List<Citation> citations, Refusal refusal, List<String> warnings) {
        return report(type, summary, status, scope, releaseId, facts, inferences, candidates, unknowns, nextEvidenceNeeded, groundingMap,
                traceId, newRunId(), citations, refusal, warnings);
    }

    private ScenarioReport report(String type, String summary, String status, com.rts.model.CoreModels.ScopeKey scope, String releaseId,
            List<Fact> facts, List<String> inferences, List<String> candidates, List<String> unknowns, List<String> nextEvidenceNeeded,
            GroundingMap groundingMap, String traceId, String runId, List<Citation> citations, Refusal refusal, List<String> warnings) {
        List<String> safeWarnings = new ArrayList<>(warnings == null ? List.of() : warnings);
        safeWarnings.add("Scenario output is a grounded candidate report, not release approval, final root cause, QA signoff, or human decision.");
        return new ScenarioReport("scenario-report.v1", status == null ? "candidate" : status, type, summary, scope, releaseId,
                facts == null ? List.of() : facts,
                inferences == null ? List.of() : inferences,
                candidates == null ? List.of() : candidates,
                unknowns == null ? List.of() : unknowns,
                nextEvidenceNeeded == null ? List.of() : nextEvidenceNeeded,
                citations == null ? List.of() : citations,
                groundingMap == null ? GroundingMap.empty() : groundingMap,
                traceId,
                runId == null || runId.isBlank() ? newRunId() : runId,
                budgetFrom(groundingMap, candidates),
                refusal,
                List.copyOf(safeWarnings));
    }

    private ServiceAnswer managedScenarioAsk(ManagedScenarioRequest request, String scenarioType, String input, String anchor) {
        String query = agentRecipeService.scenarioManagedQuery(scenarioType, input, anchor);
        if (query == null || query.isBlank() || query.endsWith(" null")) {
            return null;
        }
        return controlledLlmHarness.ask(new AskRequest(query, request.callerId(), request.apiKey(), request.scope(), request.outputMode(), 6));
    }

    private void mergeManaged(ServiceAnswer managed, List<String> inferences, List<String> nextEvidence, List<String> warnings) {
        if (managed == null) {
            return;
        }
        if (managed.inferences() != null) {
            inferences.addAll(managed.inferences());
        }
        if (managed.unknowns() != null) {
            nextEvidence.addAll(managed.unknowns());
        }
        if (managed.answerType() == AnswerType.answer) {
            inferences.add("Managed harness recipe " + queryService.trace(managed.traceId())
                    .map(trace -> trace.agentRun() == null ? "unknown" : trace.agentRun().recipeVersion())
                    .orElse("unknown") + " executed before scenario report compilation.");
        } else if (managed.refusal() != null) {
            warnings.add("Managed harness returned refusal before scenario compilation: " + managed.refusal().reason());
        }
    }

    private String traceId(ServiceAnswer managed, String fallbackTraceId) {
        return managed != null && managed.traceId() != null && !managed.traceId().isBlank() ? managed.traceId() : fallbackTraceId;
    }

    private String runId(ServiceAnswer managed) {
        return managed == null ? null : managed.runId();
    }

    private ScenarioReport refusalReport(ManagedScenarioRequest request, String type, String input, RefusalReason reason, String message) {
        String traceId = queryService.newTraceId();
        Refusal refusal = new Refusal(reason, message, List.of("Provide a valid scope, caller credentials, and a stronger governed anchor."), false);
        ScenarioReport report = report(type, summarize(input == null ? "" : input), "refusal", request.scope(), null,
                List.of(), List.of(), List.of(), List.of(message), List.of(), GroundingMap.empty(), traceId, List.of(), refusal,
                List.of("Scenario request was refused before producing candidate facts."));
        appendScenarioTrace(request, input == null ? "" : input, report, List.of("scenario_policy_guard"));
        return report;
    }

    private ScenarioReport withScenarioRefusal(ManagedScenarioRequest request, String type, String missingInputMessage,
            java.util.function.Function<String, ScenarioReport> action) {
        String input = null;
        try {
            input = safeInput(request.input(), missingInputMessage);
            return action.apply(input);
        } catch (QueryRefusalException ex) {
            return refusalReport(request, type, input == null ? request.input() : input, ex.reason(), ex.getMessage());
        }
    }

    private BudgetUsage budgetFrom(GroundingMap groundingMap, List<String> candidates) {
        int l2Reads = groundingMap == null || groundingMap.claims() == null ? 0 : groundingMap.claims().size();
        int candidateCount = candidates == null ? 0 : candidates.size();
        return new BudgetUsage(3 + candidateCount, 50, l2Reads, 3, 2, 2, Math.max(1, candidateCount) * 20, 12000, 0, 1, 0, 8000);
    }

    private void appendScenarioTrace(ManagedScenarioRequest request, String rawInput, ScenarioReport report, List<String> toolCalls) {
        List<String> factUris = report.facts().stream().map(Fact::uri).distinct().toList();
        RefusalReason refusalReason = report.refusal() == null ? RefusalReason.none : report.refusal().reason();
        Optional<TraceRecord> existing = queryService.trace(report.traceId());
        queryService.appendTrace(Instant.now(), report.traceId(), report.runId(), report.scenarioType(), request.callerId(), report.inputSummary(), null, null,
                report.scope(), report.releaseId(),
                merge(existing.map(TraceRecord::candidateUris).orElse(List.of()), factUris),
                merge(existing.map(TraceRecord::selectedUris).orElse(List.of()), factUris),
                merge(existing.map(TraceRecord::l2ReadUris).orElse(List.of()), factUris),
                refusalReason,
                merge(existing.map(TraceRecord::toolCalls).orElse(List.of()), toolCalls),
                null,
                request.outputMode(),
                report.inputSummary(), Hashing.sha256(rawInput == null ? "" : rawInput),
                existing.map(TraceRecord::agentRun).orElse(null));
    }

    private String newRunId() {
        return "run-" + java.util.UUID.randomUUID();
    }

    private List<String> merge(Collection<String> first, Collection<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (first != null) {
            first.stream().filter(value -> value != null && !value.isBlank()).forEach(merged::add);
        }
        if (second != null) {
            second.stream().filter(value -> value != null && !value.isBlank()).forEach(merged::add);
        }
        return List.copyOf(merged);
    }

    private List<Citation> citations(String releaseId, List<Fact> facts) {
        if (facts == null || facts.isEmpty()) {
            return List.of();
        }
        return facts.stream()
                .map(fact -> new Citation(releaseId == null ? fact.releaseId() : releaseId, fact.uri(), hashFrom(fact), "l2"))
                .toList();
    }

    private String hashFrom(Fact fact) {
        String source = fact.source();
        if (source != null && source.startsWith("l2:")) {
            return source.substring(3);
        }
        return projectionStore.getContentRef(fact.releaseId(), fact.uri()).map(ref -> ref.contentHash()).orElse(null);
    }

    private String safeInput(String input, String message) {
        if (input == null || input.isBlank()) {
            throw new QueryRefusalException(RefusalReason.object_not_found, message);
        }
        String safe = input.strip();
        validateExternalInput(safe);
        return safe;
    }

    private void validateExternalInput(String input) {
        if (input != null && !input.isBlank()) {
            promptPolicyGuard.validateUserText(input);
        }
    }

    private String summarize(String input) {
        String normalized = input.replaceAll("\\s+", " ").strip();
        return normalized.length() > 160 ? normalized.substring(0, 160) + "..." : normalized;
    }

    private String extractFirstAnchor(String input) {
        List<String> anchors = new ArrayList<>();
        for (String token : input.split("[\\s,;:(){}\\[\\]\"']+")) {
            String cleaned = token.strip();
            if (cleaned.startsWith("rts://")) {
                return cleaned;
            }
            String normalized = cleaned.toLowerCase(Locale.ROOT)
                    .replaceAll("^[^a-z0-9_.\\-/]+", "")
                    .replaceAll("[^a-z0-9_.\\-/]+$", "");
            if (normalized.startsWith("rule_") || normalized.startsWith("lookup_") || normalized.startsWith("helper_")
                    || normalized.startsWith("src.") || normalized.contains(".")) {
                anchors.add(cleaned);
            }
        }
        return anchors.isEmpty() ? null : anchors.get(0);
    }

    private GroundingMap groundingFromFacts(List<Fact> facts) {
        if (facts == null || facts.isEmpty()) {
            return GroundingMap.empty();
        }
        return new GroundingMap(facts.stream()
                .map(fact -> new GroundedClaim(fact.text(),
                        List.of(new GroundingEvidence(fact.uri(), hashFrom(fact), "$")),
                        ValidationStatus.grounded,
                        null))
                .toList());
    }
}
