package com.rts.agent;

import com.rts.llm.ControlledLlmHarness;
import com.rts.model.AgentServiceModels.ContextSnapshotRequest;
import com.rts.model.AgentServiceModels.ContextualAskRequest;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.query.QueryRequests.AskRequest;
import com.rts.query.PermissionService;
import com.rts.query.QueryRefusalException;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.store.StoreContracts.ProjectionStore;
import org.springframework.stereotype.Service;

@Service
public class ContextualAskService {
    private final ControlledLlmHarness harness;
    private final FeedbackMemoryService feedbackMemoryService;
    private final ProjectionStore projectionStore;
    private final PermissionService permissionService;

    public ContextualAskService(ControlledLlmHarness harness, FeedbackMemoryService feedbackMemoryService,
            ProjectionStore projectionStore, PermissionService permissionService) {
        this.harness = harness;
        this.feedbackMemoryService = feedbackMemoryService;
        this.projectionStore = projectionStore;
        this.permissionService = permissionService;
    }

    public ServiceAnswer ask(ContextualAskRequest request) {
        ScopeKey scope = request.scopeHint();
        if (scope == null && request.workspaceScope() != null) {
            scope = request.workspaceScope();
        }
        if (scope == null && request.sessionId() != null && !request.sessionId().isBlank()) {
            var context = feedbackMemoryService.loadContext(new ContextSnapshotRequest(request.sessionId(), null, request.callerId(), request.apiKey()));
            scope = context.agentSession() != null && !context.agentSession().truthEligible()
                    ? context.agentSession().recentScope()
                    : null;
            if (scope == null && context.agentSession() != null && !context.agentSession().truthEligible()) {
                scope = selectedObjectScope(context.agentSession().selectedObjectUri(), request.callerId(), request.apiKey());
            }
            if (scope == null) {
                scope = context.memories().stream()
                    .filter(memory -> "session_scope_memory".equals(memory.memoryType()) || "workspace_default_memory".equals(memory.memoryType()))
                    .filter(memory -> memory.scope() != null)
                    .map(memory -> memory.scope())
                    .findFirst()
                    .orElse(null);
            }
        }
        return harness.ask(new AskRequest(request.query(), request.callerId(), request.apiKey(), scope, request.outputMode(), request.maxToolCalls()));
    }

    private ScopeKey selectedObjectScope(String selectedObjectUri, String callerId, String apiKey) {
        if (selectedObjectUri == null || selectedObjectUri.isBlank()) {
            return null;
        }
        String releaseId;
        try {
            releaseId = projectionStore.loadActiveSnapshot().manifest().releaseId();
        } catch (RuntimeException ex) {
            throw new QueryRefusalException(RefusalReason.active_release_missing, "Active release is unavailable");
        }
        var object = projectionStore.getObject(releaseId, selectedObjectUri)
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Selected object hint is not in the active release"));
        permissionService.requireAllowed(releaseId, callerId, apiKey, object.scope(), "objects_get", "default");
        return object.scope();
    }
}
