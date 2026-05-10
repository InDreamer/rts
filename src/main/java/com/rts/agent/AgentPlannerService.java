package com.rts.agent;

import com.rts.config.RtsProperties;
import com.rts.model.AgentServiceModels.BudgetUsage;
import com.rts.model.CoreModels.AgentPlan;
import com.rts.model.CoreModels.QueryPlan;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.query.QueryRefusalException;
import com.rts.query.QueryRequests.PlanRequest;
import com.rts.query.QueryService;
import com.rts.store.StoreContracts.ProjectionStore;
import org.springframework.stereotype.Service;

@Service
public class AgentPlannerService {
    private final QueryService queryService;
    private final ProjectionStore projectionStore;
    private final RtsToolRegistry toolRegistry;
    private final RtsProperties properties;

    public AgentPlannerService(QueryService queryService, ProjectionStore projectionStore, RtsToolRegistry toolRegistry,
            RtsProperties properties) {
        this.queryService = queryService;
        this.projectionStore = projectionStore;
        this.toolRegistry = toolRegistry;
        this.properties = properties;
    }

    public AgentPlan plan(String query, String callerId, ScopeKey scopeHint, String outputMode, String scenarioType) {
        QueryPlan queryPlan = queryService.plan(new PlanRequest(query, callerId, scopeHint, outputMode, true));
        if (queryPlan.needsClarification()) {
            return AgentPlan.fromQueryPlan(queryPlan, scenarioType, activeReleaseId(), budgetEnvelope());
        }
        if (queryPlan.scope() == null) {
            throw new QueryRefusalException(RefusalReason.scope_unclear, "Agent planner requires resolved scope before tool execution");
        }
        AgentPlan agentPlan = AgentPlan.fromQueryPlan(queryPlan, scenarioType, activeReleaseId(), budgetEnvelope());
        validate(agentPlan);
        return agentPlan;
    }

    public void validate(AgentPlan plan) {
        if (plan == null) {
            throw new QueryRefusalException(RefusalReason.unsupported_claim, "Agent plan is required");
        }
        if (plan.clarificationQuestion() != null && !plan.clarificationQuestion().isBlank()) {
            return;
        }
        if (plan.scope() == null || plan.releaseId() == null || plan.releaseId().isBlank()) {
            throw new QueryRefusalException(RefusalReason.scope_unclear, "Agent plan must include release and scope snapshots");
        }
        if (plan.toolPlan() == null || plan.toolPlan().isEmpty()) {
            throw new QueryRefusalException(RefusalReason.unsupported_claim, "Agent plan must name allowed RTS tools");
        }
        for (String tool : plan.toolPlan()) {
            toolRegistry.require(tool);
        }
    }

    private String activeReleaseId() {
        return projectionStore.loadActiveSnapshot().manifest().releaseId();
    }

    private BudgetUsage budgetEnvelope() {
        return new BudgetUsage(0, properties.getMaxToolCalls(), 0, properties.getMaxL2Objects(), 0,
                properties.getMaxDependencyDepth(), 0, properties.getMaxRetrievedTokens(), 0, properties.getMaxModelCalls(),
                0, properties.getMaxLatencyMs());
    }
}
