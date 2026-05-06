package com.rts.query;

import com.rts.model.CoreModels.Direction;
import com.rts.model.CoreModels.ScopeKey;
import java.util.List;

public final class QueryRequests {
    private QueryRequests() {}

    public record PlanRequest(String query, String callerId, ScopeKey scopeHint, String outputMode, boolean useLlm) {}

    public record FindRequest(String query, ScopeKey scope, List<String> objectTypes, List<String> anchors, Integer limit, String callerId, String apiKey, String outputMode) {}

    public record ObjectGetRequest(String uri, String releaseId, String traceId, String callerId, String apiKey) {}

    public record ObjectContentRequest(String uri, String purpose, String releaseId, String traceId, String callerId, String apiKey) {}

    public record DependenciesRequest(String uri, Direction direction, String edgeType, Integer depth, String purpose, String releaseId, String callerId, String apiKey) {}

    public record QueryRequest(String query, String callerId, String apiKey, ScopeKey scopeHint, String outputMode, boolean useLlm) {}

    public record AskRequest(String query, String callerId, String apiKey, ScopeKey scopeHint, String outputMode, Integer maxToolCalls) {}
}
