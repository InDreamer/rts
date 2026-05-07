package com.rts.agent;

import com.rts.model.AgentServiceModels.EvaluationResult;
import com.rts.model.AgentServiceModels.MetricsSnapshot;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    public MetricsSnapshot snapshot(List<EvaluationResult> evaluationResults) {
        List<EvaluationResult> results = evaluationResults == null ? List.of() : evaluationResults;
        long total = results.size();
        long correctScope = results.stream().filter(EvaluationResult::correctScope).count();
        long grounded = results.stream().filter(result -> result.unsupportedClaimCount() == 0 && result.correctObjectFound()).count();
        long unsupported = results.stream().mapToLong(EvaluationResult::unsupportedClaimCount).sum();
        long wrongScope = total - correctScope;
        long refusalCorrect = results.stream().filter(EvaluationResult::refusalCorrect).count();
        long traceComplete = results.stream().filter(result -> result.traceId() != null && !result.traceId().isBlank()).count();
        long objectFound = results.stream().filter(EvaluationResult::correctObjectFound).count();
        return new MetricsSnapshot(
                correctScope,
                total,
                refusalCorrect,
                total,
                objectFound,
                total,
                grounded,
                total,
                unsupported,
                total,
                unsupported,
                wrongScope,
                refusalCorrect,
                total,
                grounded,
                Math.max(1, objectFound),
                traceComplete,
                total,
                0,
                total,
                0,
                total,
                0,
                0,
                0,
                0,
                List.of("Metrics snapshot is derived from supplied evaluation results; production telemetry can replace this source later."));
    }
}
