package com.rts.agent;

import com.rts.llm.ControlledLlmHarness;
import com.rts.model.AgentServiceModels.ContextSnapshotRequest;
import com.rts.model.AgentServiceModels.ContextualAskRequest;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.query.QueryRequests.AskRequest;
import org.springframework.stereotype.Service;

@Service
public class ContextualAskService {
    private final ControlledLlmHarness harness;
    private final FeedbackMemoryService feedbackMemoryService;

    public ContextualAskService(ControlledLlmHarness harness, FeedbackMemoryService feedbackMemoryService) {
        this.harness = harness;
        this.feedbackMemoryService = feedbackMemoryService;
    }

    public ServiceAnswer ask(ContextualAskRequest request) {
        ScopeKey scope = request.scopeHint();
        if (scope == null && request.workspaceScope() != null) {
            scope = request.workspaceScope();
        }
        if (scope == null && request.sessionId() != null && !request.sessionId().isBlank()) {
            var context = feedbackMemoryService.loadContext(new ContextSnapshotRequest(request.sessionId(), null, request.callerId(), request.apiKey()));
            scope = context.memories().stream()
                    .filter(memory -> "session_scope_memory".equals(memory.memoryType()) || "workspace_default_memory".equals(memory.memoryType()))
                    .filter(memory -> memory.scope() != null)
                    .map(memory -> memory.scope())
                    .findFirst()
                    .orElse(null);
        }
        return harness.ask(new AskRequest(request.query(), request.callerId(), request.apiKey(), scope, request.outputMode(), request.maxToolCalls()));
    }
}
