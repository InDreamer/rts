package com.rts.agent;

import com.rts.model.AgentServiceModels.EvaluationCase;
import com.rts.model.AgentServiceModels.EvaluationResult;
import com.rts.model.AgentServiceModels.EvaluationRunResult;
import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.QueryPlan;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.query.QueryRequests.QueryRequest;
import com.rts.query.QueryResolver;
import com.rts.query.QueryService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EvaluationService {
    private final QueryService queryService;
    private final QueryResolver queryResolver;

    public EvaluationService(QueryService queryService, QueryResolver queryResolver) {
        this.queryService = queryService;
        this.queryResolver = queryResolver;
    }

    public EvaluationRunResult run(String mode, List<EvaluationCase> cases) {
        String runId = "eval-" + UUID.randomUUID();
        String safeMode = mode == null || mode.isBlank() ? "deterministic" : mode;
        List<EvaluationResult> results = cases.stream()
                .map(testCase -> evaluate(runId, safeMode, testCase))
                .toList();
        return new EvaluationRunResult(
                runId,
                safeMode,
                results.size(),
                results.stream().filter(EvaluationResult::correctScope).count(),
                results.stream().filter(EvaluationResult::correctObjectFound).count(),
                results.stream().filter(EvaluationResult::refusalCorrect).count(),
                results.stream().mapToLong(EvaluationResult::unsupportedClaimCount).sum(),
                results);
    }

    private EvaluationResult evaluate(String runId, String mode, EvaluationCase testCase) {
        Instant start = Instant.now();
        var answer = queryService.query(new QueryRequest(testCase.query(), testCase.callerId(), testCase.apiKey(),
                testCase.expectedScope(), "default", false));
        QueryPlan plan = queryResolver.resolve(testCase.query(), testCase.expectedScope());
        boolean correctScope = testCase.expectedScope() == null
                ? plan.needsClarification()
                : testCase.expectedScope().matches(answer.scope());
        boolean correctObject = testCase.expectedObjects() == null || testCase.expectedObjects().isEmpty()
                || answer.citedObjects().containsAll(testCase.expectedObjects());
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
                Duration.between(start, Instant.now()).toMillis(),
                answer.traceId(),
                answer.warnings());
    }
}
