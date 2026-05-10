package com.rts.agent;

import com.rts.model.AgentServiceModels.EvaluationCase;
import com.rts.model.AgentServiceModels.EvaluationResult;
import com.rts.model.AgentServiceModels.EvaluationRunResult;
import com.rts.model.AgentServiceModels.ManagedScenarioRequest;
import com.rts.model.AgentServiceModels.ScenarioReport;
import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.QueryPlan;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.llm.ControlledLlmHarness;
import com.rts.query.QueryRequests.AskRequest;
import com.rts.query.QueryRequests.QueryRequest;
import com.rts.query.QueryResolver;
import com.rts.query.QueryService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EvaluationService {
    private final QueryService queryService;
    private final QueryResolver queryResolver;
    private final ControlledLlmHarness controlledLlmHarness;
    private final ManagedScenarioService managedScenarioService;

    public EvaluationService(QueryService queryService, QueryResolver queryResolver, ControlledLlmHarness controlledLlmHarness,
            ManagedScenarioService managedScenarioService) {
        this.queryService = queryService;
        this.queryResolver = queryResolver;
        this.controlledLlmHarness = controlledLlmHarness;
        this.managedScenarioService = managedScenarioService;
    }

    public EvaluationRunResult run(String mode, List<EvaluationCase> cases) {
        String runId = "eval-" + UUID.randomUUID();
        String safeMode = mode == null || mode.isBlank() ? "deterministic" : mode;
        List<EvaluationResult> results = cases.stream()
                .map(testCase -> evaluate(runId, safeMode, testCase))
                .toList();
        long correctScope = results.stream().filter(EvaluationResult::correctScope).count();
        long correctObject = results.stream().filter(EvaluationResult::correctObjectFound).count();
        long refusalCorrect = results.stream().filter(EvaluationResult::refusalCorrect).count();
        long unsupported = results.stream().mapToLong(EvaluationResult::unsupportedClaimCount).sum();
        long traceComplete = results.stream().filter(result -> result.traceId() != null && !result.traceId().isBlank()).count();
        long aiValueScore = results.stream().mapToLong(EvaluationResult::aiValueScore).sum();
        long firstPassUseful = results.stream().filter(EvaluationResult::firstPassUseful).count();
        long expectedObjectCases = cases.stream()
                .filter(testCase -> testCase.expectedObjects() != null && !testCase.expectedObjects().isEmpty())
                .count();
        Map<String, Double> thresholds = thresholds();
        List<String> failures = gateFailures(safeMode, results.size(), correctScope, correctObject, expectedObjectCases, refusalCorrect,
                unsupported, traceComplete, aiValueScore, firstPassUseful, thresholds);
        return new EvaluationRunResult(
                runId,
                safeMode,
                results.size(),
                correctScope,
                correctObject,
                refusalCorrect,
                unsupported,
                aiValueScore,
                firstPassUseful,
                results,
                thresholds,
                failures.isEmpty(),
                failures);
    }

    private EvaluationResult evaluate(String runId, String mode, EvaluationCase testCase) {
        Instant start = Instant.now();
        EvaluatedOutput output = evaluateOutput(mode, testCase);
        ServiceAnswer answer = output.answer();
        QueryPlan plan = queryResolver.resolve(testCase.query(), testCase.expectedScope());
        boolean correctScope = testCase.expectedScope() == null
                ? plan.needsClarification() || answer.scope() == null
                : answer.scope() != null && testCase.expectedScope().matches(answer.scope());
        boolean correctObject = testCase.expectedObjects() == null || testCase.expectedObjects().isEmpty()
                || output.citedObjects().containsAll(testCase.expectedObjects());
        RefusalReason actualRefusal = answer.refusal() == null ? RefusalReason.none : answer.refusal().reason();
        boolean refusalCorrect = testCase.expectedRefusal() == null
                ? answer.answerType() == AnswerType.answer
                : actualRefusal == testCase.expectedRefusal();
        int unsupported = answer.refusal() != null && answer.refusal().reason() == RefusalReason.unsupported_claim ? 1 : 0;
        return new EvaluationResult(
                runId,
                testCase.caseId(),
                mode,
                correctScope,
                correctObject,
                refusalCorrect,
                unsupported,
                output.aiValueSignals(),
                output.aiValueSignals() > 0 && correctScope && correctObject && unsupported == 0,
                Duration.between(start, Instant.now()).toMillis(),
                answer.traceId(),
                output.warnings());
    }

    private EvaluatedOutput evaluateOutput(String mode, EvaluationCase testCase) {
        String normalized = mode == null ? "deterministic" : mode.toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "managed", "managed_ask", "ask", "llm" -> fromAnswer(controlledLlmHarness.ask(new AskRequest(
                    testCase.query(), testCase.callerId(), testCase.apiKey(), testCase.expectedScope(), "default", null)));
            case "scenario", "scenario_pr_diff", "pr_diff" -> fromScenario(managedScenarioService.analyzePrDiff(scenarioRequest(testCase, "pr_diff_impact")));
            case "scenario_exception", "exception" -> fromScenario(managedScenarioService.investigateException(scenarioRequest(testCase, "exception_investigation")));
            case "scenario_failed_message", "failed_message" -> fromScenario(managedScenarioService.analyzeFailedMessage(scenarioRequest(testCase, "failed_message_analysis")));
            case "scenario_test_planning", "test_planning" -> fromScenario(managedScenarioService.planTests(scenarioRequest(testCase, "test_planning")));
            case "scenario_governance", "governance_review" -> fromScenario(managedScenarioService.reviewGovernance(scenarioRequest(testCase, "governance_review")));
            default -> fromAnswer(queryService.query(new QueryRequest(testCase.query(), testCase.callerId(), testCase.apiKey(),
                    testCase.expectedScope(), "default", false)));
        };
    }

    private ManagedScenarioRequest scenarioRequest(EvaluationCase testCase, String scenarioType) {
        return new ManagedScenarioRequest(scenarioType, testCase.query(), testCase.expectedScope(), testCase.callerId(),
                testCase.apiKey(), "default", null);
    }

    private EvaluatedOutput fromAnswer(ServiceAnswer answer) {
        int aiSignals = size(answer.inferences()) + size(answer.unknowns()) + size(answer.candidateSuggestions());
        if (answer.answerType() == AnswerType.answer && answer.groundingMap() != null && answer.groundingMap().claims() != null
                && !answer.groundingMap().claims().isEmpty()) {
            aiSignals += 1;
        }
        return new EvaluatedOutput(answer, answer.citedObjects() == null ? List.of() : answer.citedObjects(),
                answer.warnings() == null ? List.of() : answer.warnings(), aiSignals);
    }

    private EvaluatedOutput fromScenario(ScenarioReport report) {
        ServiceAnswer answer = new ServiceAnswer(
                report.refusal() == null ? AnswerType.answer : AnswerType.refusal,
                report.scope(),
                report.releaseId(),
                report.facts(),
                report.inferences(),
                report.unknowns(),
                report.candidates(),
                List.of(),
                report.citations() == null ? List.of() : report.citations().stream().map(citation -> citation.objectUri()).toList(),
                List.of(),
                report.traceId(),
                report.refusal(),
                report.warnings(),
                null);
        int aiSignals = size(report.inferences()) + size(report.candidates()) + size(report.unknowns()) + size(report.nextEvidenceNeeded());
        return new EvaluatedOutput(answer, answer.citedObjects() == null ? List.of() : answer.citedObjects(),
                answer.warnings() == null ? List.of() : answer.warnings(), aiSignals);
    }

    private record EvaluatedOutput(ServiceAnswer answer, List<String> citedObjects, List<String> warnings, int aiValueSignals) {}

    private Map<String, Double> thresholds() {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("scope_resolution_accuracy", 1.0);
        values.put("refusal_correctness", 1.0);
        values.put("trace_completeness", 1.0);
        values.put("top_k_recall", 1.0);
        values.put("unsupported_claim_count_max", 0.0);
        values.put("wrong_scope_count_max", 0.0);
        values.put("first_pass_usefulness", 1.0);
        values.put("ai_value_score_min", 1.0);
        return values;
    }

    private List<String> gateFailures(String mode, int total, long correctScope, long correctObject, long expectedObjectCases,
            long refusalCorrect, long unsupported, long traceComplete, long aiValueScore, long firstPassUseful, Map<String, Double> thresholds) {
        if (total == 0) {
            return List.of("no evaluation cases supplied");
        }
        List<String> failures = new ArrayList<>();
        if (ratio(correctScope, total) < thresholds.get("scope_resolution_accuracy")) {
            failures.add("scope_resolution_accuracy below threshold");
        }
        if (ratio(refusalCorrect, total) < thresholds.get("refusal_correctness")) {
            failures.add("refusal_correctness below threshold");
        }
        if (ratio(traceComplete, total) < thresholds.get("trace_completeness")) {
            failures.add("trace_completeness below threshold");
        }
        if (expectedObjectCases > 0 && ratio(correctObject, (int) expectedObjectCases) < thresholds.get("top_k_recall")) {
            failures.add("top_k_recall below threshold");
        }
        if (unsupported > thresholds.get("unsupported_claim_count_max")) {
            failures.add("unsupported_claim_count above threshold");
        }
        long wrongScope = total - correctScope;
        if (wrongScope > thresholds.get("wrong_scope_count_max")) {
            failures.add("wrong_scope_count above threshold");
        }
        if (requiresAiValueGate(mode)) {
            if (ratio(firstPassUseful, total) < thresholds.get("first_pass_usefulness")) {
                failures.add("first_pass_usefulness below threshold");
            }
            if (ratio(aiValueScore, total) < thresholds.get("ai_value_score_min")) {
                failures.add("ai_value_score below threshold");
            }
        }
        return failures;
    }

    private boolean requiresAiValueGate(String mode) {
        String normalized = mode == null ? "" : mode.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("managed") || normalized.contains("llm") || normalized.contains("scenario") || normalized.contains("ask");
    }

    private double ratio(long numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / (double) denominator;
    }

    private int size(List<?> values) {
        return values == null ? 0 : values.size();
    }

}
