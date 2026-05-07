package com.rts.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rts.config.RtsProperties;
import com.rts.model.AgentServiceModels.GovernanceReviewRequest;
import com.rts.model.AgentServiceModels.GovernanceReviewResult;
import com.rts.model.AgentServiceModels.HumanDecisionRecord;
import com.rts.model.AgentServiceModels.HumanDecisionRecordRequest;
import com.rts.model.AgentServiceModels.ReviewerQuestion;
import com.rts.model.CoreModels.Fact;
import com.rts.model.CoreModels.L2Content;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.query.PermissionService;
import com.rts.query.QueryRefusalException;
import com.rts.query.QueryRequests.ObjectContentRequest;
import com.rts.query.QueryService;
import com.rts.store.StoreContracts.ProjectionSnapshot;
import com.rts.store.StoreContracts.ProjectionStore;
import com.rts.store.StoreContracts.ScopeRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GovernanceAssistantService {
    private final ProjectionStore projectionStore;
    private final ScopeRegistry scopeRegistry;
    private final PermissionService permissionService;
    private final QueryService queryService;
    private final RtsProperties properties;
    private final ObjectMapper mapper;

    public GovernanceAssistantService(ProjectionStore projectionStore, ScopeRegistry scopeRegistry, PermissionService permissionService,
            QueryService queryService, RtsProperties properties, ObjectMapper mapper) {
        this.projectionStore = projectionStore;
        this.scopeRegistry = scopeRegistry;
        this.permissionService = permissionService;
        this.queryService = queryService;
        this.properties = properties;
        this.mapper = mapper;
    }

    public GovernanceReviewResult review(GovernanceReviewRequest request) {
        ProjectionSnapshot snapshot = projectionStore.loadActiveSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        ScopeKey scope = resolveScope(releaseId, request.scope(), request.objectUri());
        permissionService.requireAllowed(releaseId, request.callerId(), request.apiKey(), scope, "governance_tools", mode(request.outputMode()));
        List<ObjectManifestEntry> objects = objectsFor(snapshot, scope, request.objectUri());
        String traceId = queryService.newTraceId();
        List<Fact> facts = new ArrayList<>();
        if (request.readL2()) {
            for (ObjectManifestEntry object : objects) {
                L2Content l2 = queryService.readContent(new ObjectContentRequest(object.uri(), "governance_review", null, traceId,
                        request.callerId(), request.apiKey()));
                facts.add(new Fact("Governance review read L2 for " + object.objectId(), object.uri(), releaseId, "l2:" + l2.contentHash()));
            }
        }
        List<String> conflictCandidates = conflictCandidates(snapshot, objects);
        List<String> ambiguityCandidates = ambiguityCandidates(snapshot, objects);
        List<ReviewerQuestion> questions = objects.stream()
                .flatMap(object -> List.of(
                        new ReviewerQuestion("rq-" + object.objectId() + "-evidence", object.uri(),
                                "Is the released evidence sufficient for " + object.objectId() + "?",
                                List.of("approve evidence as sufficient", "request more evidence", "mark conflict/ambiguity"),
                                "AI can propose this question, but human review decides."),
                        new ReviewerQuestion("rq-" + object.objectId() + "-applicability", object.uri(),
                                "Are applicability and not-applicable conditions complete for " + object.objectId() + "?",
                                List.of("complete", "needs clarification", "not applicable to this pack"),
                                "Applicability gaps can cause wrong-scope answers."))
                        .stream())
                .toList();
        queryService.appendTrace(Instant.now(), traceId, "governance_review", request.callerId(), request.objectUri(), null, scope, releaseId,
                objects.stream().map(ObjectManifestEntry::uri).toList(), objects.stream().map(ObjectManifestEntry::uri).toList(),
                facts.stream().map(Fact::uri).toList(), RefusalReason.none,
                List.of("inspect_projection", "summarize_conflicts", "generate_reviewer_questions"));
        return new GovernanceReviewResult("candidate", releaseId, scope, facts, conflictCandidates, ambiguityCandidates, questions,
                List.of("Governance review output is candidate material; it does not change truth, signoff, or release state."), traceId);
    }

    public HumanDecisionRecord recordDecision(HumanDecisionRecordRequest request) {
        ProjectionSnapshot snapshot = projectionStore.loadActiveSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        ScopeKey scope = resolveScope(releaseId, request.scope(), request.objectUri());
        permissionService.requireAllowed(releaseId, request.callerId(), request.apiKey(), scope, "governance_tools", "default");
        HumanDecisionRecord record = new HumanDecisionRecord(
                "decision-" + UUID.randomUUID(),
                request.traceId(),
                request.objectUri(),
                scope,
                request.decisionType(),
                request.decisionText(),
                request.decidedBy(),
                false,
                Instant.now());
        append("governance/human-decisions.jsonl", record);
        return record;
    }

    private ScopeKey resolveScope(String releaseId, ScopeKey scope, String objectUri) {
        if (scope != null) {
            if (scopeRegistry.resolve(releaseId, scope).isEmpty()) {
                throw new QueryRefusalException(RefusalReason.scope_unclear, "Scope is not active in release");
            }
            return scope;
        }
        if (objectUri != null && !objectUri.isBlank()) {
            return projectionStore.getObject(releaseId, objectUri)
                    .map(ObjectManifestEntry::scope)
                    .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Object not found"));
        }
        throw new QueryRefusalException(RefusalReason.scope_unclear, "Scope or object URI is required");
    }

    private List<ObjectManifestEntry> objectsFor(ProjectionSnapshot snapshot, ScopeKey scope, String objectUri) {
        if (objectUri != null && !objectUri.isBlank()) {
            return List.of(projectionStore.getObject(snapshot.manifest().releaseId(), objectUri)
                    .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Object not found")));
        }
        return snapshot.objectManifest().stream()
                .filter(object -> object.scope().matches(scope))
                .toList();
    }

    private List<String> conflictCandidates(ProjectionSnapshot snapshot, List<ObjectManifestEntry> objects) {
        List<String> conflicts = new ArrayList<>();
        for (ObjectManifestEntry object : objects) {
            snapshot.objectManifest().stream()
                    .filter(candidate -> !candidate.uri().equals(object.uri()))
                    .filter(candidate -> candidate.scope().matches(object.scope()))
                    .filter(candidate -> candidate.targetPath() != null && candidate.targetPath().equals(object.targetPath()))
                    .findFirst()
                    .ifPresent(candidate -> conflicts.add("same target candidate: " + object.uri() + " and " + candidate.uri()));
        }
        return conflicts;
    }

    private List<String> ambiguityCandidates(ProjectionSnapshot snapshot, List<ObjectManifestEntry> objects) {
        return snapshot.objectCards().stream()
                .filter(card -> objects.stream().anyMatch(object -> object.uri().equals(card.uri())))
                .filter(card -> card.riskFlags() != null && !card.riskFlags().isEmpty())
                .map(card -> "risk flags on " + card.uri() + ": " + card.riskFlags())
                .toList();
    }

    private void append(String relativePath, Object value) {
        try {
            var path = properties.getStoreRoot().resolve(relativePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, mapper.writeValueAsString(value) + System.lineSeparator(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String mode(String outputMode) {
        return outputMode == null || outputMode.isBlank() ? "default" : outputMode;
    }
}
