package com.rts.agent;

import com.rts.model.AgentServiceModels.PipelineResult;
import com.rts.model.AgentServiceModels.ReleaseReadinessRequest;
import com.rts.model.AgentServiceModels.TraceReport;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.query.QueryRefusalException;
import com.rts.query.QueryService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PipelineReportService {
    private final AgentAnalysisService analysisService;
    private final QueryService queryService;

    public PipelineReportService(AgentAnalysisService analysisService, QueryService queryService) {
        this.analysisService = analysisService;
        this.queryService = queryService;
    }

    public PipelineResult releaseReadiness(ReleaseReadinessRequest request) {
        var readiness = analysisService.checkReleaseReadiness(request);
        Map<String, Object> machine = new LinkedHashMap<>();
        machine.put("status", readiness.status());
        machine.put("blocking_issues_count", readiness.blockingIssuesCount());
        machine.put("facts", readiness.facts());
        return new PipelineResult(readiness.status(), readiness.releaseId(), readiness.scope(), machine, readiness.warnings(), readiness.traceId());
    }

    public TraceReport traceReport(String traceId, String callerId, String apiKey) {
        var trace = queryService.trace(traceId, callerId, apiKey)
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Trace not found"));
        return new TraceReport(trace.traceId(), trace.status(), trace.releaseId(), trace.resolvedScope(), trace.runId(), trace.agentRun(), trace.toolCalls(),
                trace.groundingMap(), trace.budgetUsage(), List.of("Trace report is audit/reporting output, not a release gate."));
    }
}
