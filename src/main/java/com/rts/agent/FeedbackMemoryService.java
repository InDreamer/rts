package com.rts.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rts.config.RtsProperties;
import com.rts.model.AgentServiceModels.ContextItem;
import com.rts.model.AgentServiceModels.ContextKind;
import com.rts.model.AgentServiceModels.ContextSnapshot;
import com.rts.model.AgentServiceModels.ContextSnapshotRequest;
import com.rts.model.AgentServiceModels.FeedbackRecord;
import com.rts.model.AgentServiceModels.FeedbackRequest;
import com.rts.model.AgentServiceModels.FeedbackRoute;
import com.rts.model.AgentServiceModels.MemoryRecord;
import com.rts.model.AgentServiceModels.MemoryWriteRequest;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.query.PermissionService;
import com.rts.query.QueryRefusalException;
import com.rts.store.StoreContracts.ProjectionStore;
import com.rts.store.StoreContracts.ScopeRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FeedbackMemoryService {
    private static final List<String> ALLOWED_MEMORY_TYPES = List.of(
            "session_scope_memory",
            "workspace_default_memory",
            "user_preference_memory",
            "tool_feedback_memory",
            "retrieval_failure_memory",
            "card_improvement_candidate");

    private static final List<String> FORBIDDEN_MEMORY_TYPES = List.of(
            "rule_truth_memory",
            "lookup_truth_memory",
            "helper_truth_memory",
            "signoff_memory",
            "adjudication_memory",
            "release_truth_memory");

    private final ProjectionStore projectionStore;
    private final ScopeRegistry scopeRegistry;
    private final PermissionService permissionService;
    private final RtsProperties properties;
    private final ObjectMapper mapper;

    public FeedbackMemoryService(ProjectionStore projectionStore, ScopeRegistry scopeRegistry, PermissionService permissionService,
            RtsProperties properties, ObjectMapper mapper) {
        this.projectionStore = projectionStore;
        this.scopeRegistry = scopeRegistry;
        this.permissionService = permissionService;
        this.properties = properties;
        this.mapper = mapper;
    }

    public FeedbackRecord recordFeedback(FeedbackRequest request) {
        String releaseId = activeReleaseId();
        if (request.scope() != null) {
            requireAllowedScope(releaseId, request.scope(), request.callerId(), request.apiKey(), "feedback_tools");
        }
        FeedbackRoute route = routeFor(request.feedbackType(), request.message());
        FeedbackRecord record = new FeedbackRecord(
                "feedback-" + UUID.randomUUID(),
                request.traceId(),
                request.callerId(),
                request.sessionId(),
                request.feedbackType(),
                request.message(),
                request.scope(),
                request.selectedObjectUri(),
                route,
                false,
                Instant.now());
        append("feedback/trace-feedback.jsonl", record);
        return record;
    }

    public MemoryRecord writeMemory(MemoryWriteRequest request) {
        String type = normalize(request.memoryType());
        if (FORBIDDEN_MEMORY_TYPES.contains(type)) {
            throw new QueryRefusalException(RefusalReason.governance_unauthorized, "Runtime memory cannot store governed truth, signoff, adjudication, or release state");
        }
        if (!ALLOWED_MEMORY_TYPES.contains(type)) {
            throw new QueryRefusalException(RefusalReason.governance_unauthorized, "Unsupported memory type: " + request.memoryType());
        }
        String releaseId = activeReleaseId();
        ScopeKey scope = request.scope();
        if (scope != null) {
            requireAllowedScope(releaseId, scope, request.callerId(), request.apiKey(), "feedback_tools");
        } else {
            permissionService.requireAdmin(releaseId, request.callerId(), request.apiKey());
        }
        MemoryRecord record = new MemoryRecord(
                "memory-" + UUID.randomUUID(),
                request.sessionId(),
                request.callerId(),
                type,
                request.key(),
                request.value(),
                scope,
                false,
                Instant.now());
        append("feedback/context-memory.jsonl", record);
        return record;
    }

    public ContextSnapshot loadContext(ContextSnapshotRequest request) {
        String releaseId = activeReleaseId();
        if (request.scope() != null) {
            requireAllowedScope(releaseId, request.scope(), request.callerId(), request.apiKey(), "feedback_tools");
        }
        List<MemoryRecord> memories = readMemories().stream()
                .filter(memory -> request.sessionId() == null || request.sessionId().equals(memory.sessionId()))
                .filter(memory -> request.callerId() == null || request.callerId().equals(memory.callerId()))
                .filter(memory -> request.scope() == null || memory.scope() == null || memory.scope().matches(request.scope()))
                .filter(memory -> canReadMemory(releaseId, memory, request.callerId(), request.apiKey()))
                .toList();
        List<ContextItem> items = memories.stream()
                .map(memory -> new ContextItem(ContextKind.memory, memory.memoryType(), false, null, null,
                        memory.key() + "=" + memory.value()))
                .toList();
        return new ContextSnapshot(request.sessionId(), request.callerId(), memories, items,
                List.of("Runtime context memory is not truth-eligible and only provides scope/preference/tool feedback hints."));
    }

    private boolean canReadMemory(String releaseId, MemoryRecord memory, String callerId, String apiKey) {
        try {
            if (memory.scope() == null) {
                permissionService.requireAdmin(releaseId, callerId, apiKey);
            } else {
                permissionService.requireAllowed(releaseId, callerId, apiKey, memory.scope(), "feedback_tools", "default");
            }
            return true;
        } catch (QueryRefusalException ex) {
            return false;
        }
    }

    private void requireAllowedScope(String releaseId, ScopeKey scope, String callerId, String apiKey, String entrypoint) {
        if (scopeRegistry.resolve(releaseId, scope).isEmpty()) {
            throw new QueryRefusalException(RefusalReason.scope_unclear, "Scope is not active in release");
        }
        permissionService.requireAllowed(releaseId, callerId, apiKey, scope, entrypoint, "default");
    }

    private FeedbackRoute routeFor(String feedbackType, String message) {
        String normalized = normalize(feedbackType + " " + message);
        if (normalized.contains("wrong") || normalized.contains("miss") || normalized.contains("不对") || normalized.contains("没找到")) {
            return FeedbackRoute.retrieval_quality_queue;
        }
        if (normalized.contains("alias") || normalized.contains("search_text") || normalized.contains("card")) {
            return FeedbackRoute.card_improvement_candidate;
        }
        if (normalized.contains("review") || normalized.contains("裁决")) {
            return FeedbackRoute.review_workflow;
        }
        return FeedbackRoute.trace_feedback;
    }

    private String activeReleaseId() {
        try {
            return projectionStore.loadActiveSnapshot().manifest().releaseId();
        } catch (RuntimeException ex) {
            throw new QueryRefusalException(RefusalReason.active_release_missing, "Active release is unavailable");
        }
    }

    private void append(String relativePath, Object value) {
        try {
            var path = properties.getStoreRoot().resolve(relativePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, mapper.writeValueAsString(value) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private List<MemoryRecord> readMemories() {
        var path = properties.getStoreRoot().resolve("feedback/context-memory.jsonl");
        if (!Files.exists(path)) {
            return List.of();
        }
        List<MemoryRecord> records = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    records.add(mapper.readValue(line, MemoryRecord.class));
                }
            }
            return List.copyOf(records);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).strip();
    }
}
