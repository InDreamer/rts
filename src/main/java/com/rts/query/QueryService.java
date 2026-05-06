package com.rts.query;

import com.rts.config.RtsProperties;
import com.rts.index.LuceneIndexService;
import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.CandidateObject;
import com.rts.model.CoreModels.DependencyResult;
import com.rts.model.CoreModels.Direction;
import com.rts.model.CoreModels.L2Content;
import com.rts.model.CoreModels.ObjectCard;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.model.CoreModels.QueryPlan;
import com.rts.model.CoreModels.Refusal;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ReleaseManifest;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.model.CoreModels.TraceRecord;
import com.rts.query.QueryRequests.DependenciesRequest;
import com.rts.query.QueryRequests.FindRequest;
import com.rts.query.QueryRequests.ObjectContentRequest;
import com.rts.query.QueryRequests.ObjectGetRequest;
import com.rts.query.QueryRequests.PlanRequest;
import com.rts.query.QueryRequests.QueryRequest;
import com.rts.store.ProjectionValidationException;
import com.rts.store.StoreContracts.ContentStore;
import com.rts.store.StoreContracts.ProjectionSnapshot;
import com.rts.store.StoreContracts.ProjectionStore;
import com.rts.store.StoreContracts.ScopeRegistry;
import com.rts.store.StoreContracts.TraceStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class QueryService {
    private final ProjectionStore projectionStore;
    private final ScopeRegistry scopeRegistry;
    private final TraceStore traceStore;
    private final ContentStore contentStore;
    private final LuceneIndexService luceneIndexService;
    private final QueryResolver queryResolver;
    private final DependencyService dependencyService;
    private final PermissionService permissionService;
    private final AnswerAssembler answerAssembler;
    private final FinalAnswerValidator finalAnswerValidator;
    private final RtsProperties properties;

    public QueryService(ProjectionStore projectionStore, ScopeRegistry scopeRegistry, TraceStore traceStore,
            ContentStore contentStore, LuceneIndexService luceneIndexService, QueryResolver queryResolver,
            DependencyService dependencyService, PermissionService permissionService, AnswerAssembler answerAssembler,
            FinalAnswerValidator finalAnswerValidator, RtsProperties properties) {
        this.projectionStore = projectionStore;
        this.scopeRegistry = scopeRegistry;
        this.traceStore = traceStore;
        this.contentStore = contentStore;
        this.luceneIndexService = luceneIndexService;
        this.queryResolver = queryResolver;
        this.dependencyService = dependencyService;
        this.permissionService = permissionService;
        this.answerAssembler = answerAssembler;
        this.finalAnswerValidator = finalAnswerValidator;
        this.properties = properties;
    }

    public QueryPlan plan(PlanRequest request) {
        return queryResolver.resolve(request.query(), request.scopeHint());
    }

    public List<CandidateObject> find(FindRequest request) {
        ProjectionSnapshot snapshot = activeSnapshot();
        ScopeKey scope = requireScope(snapshot.manifest().releaseId(), request.scope());
        permissionService.requireAllowed(snapshot.manifest().releaseId(), request.callerId(), request.apiKey(), scope, "find", outputMode(request.outputMode()));
        return findCandidates(snapshot.manifest().releaseId(), scope, request.query(), safe(request.anchors()), safe(request.objectTypes()), limit(request.limit()));
    }

    public ObjectEnvelope getObject(ObjectGetRequest request) {
        ProjectionSnapshot snapshot = activeSnapshotForRequestRelease(request.releaseId());
        ObjectManifestEntry entry = projectionStore.getObject(snapshot.manifest().releaseId(), request.uri())
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Object not found"));
        requireScope(snapshot.manifest().releaseId(), entry.scope());
        permissionService.requireAllowed(snapshot.manifest().releaseId(), request.callerId(), request.apiKey(), entry.scope(), "objects_get", "default");
        ObjectCard card = projectionStore.getCard(snapshot.manifest().releaseId(), request.uri())
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Object card not found"));
        DependencyResult dependencySummary = dependencyService.traverse(snapshot.manifest().releaseId(), request.uri(), Direction.both, null, 1, 20);
        requireDependencyResultAllowed(snapshot.manifest().releaseId(), request.callerId(), request.apiKey(), dependencySummary);
        return new ObjectEnvelope(entry, card, dependencySummary);
    }

    public L2Content readContent(ObjectContentRequest request) {
        ProjectionSnapshot snapshot = activeSnapshotForRequestRelease(request.releaseId());
        ObjectManifestEntry entry = projectionStore.getObject(snapshot.manifest().releaseId(), request.uri())
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Object not found"));
        requireScope(snapshot.manifest().releaseId(), entry.scope());
        permissionService.requireAllowed(snapshot.manifest().releaseId(), request.callerId(), request.apiKey(), entry.scope(), "objects_content", "default");
        L2Content content = projectionStore.getContentRef(snapshot.manifest().releaseId(), request.uri())
                .map(contentStore::read)
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.l2_missing, "L2 content ref missing"));
        if (request.traceId() != null && !request.traceId().isBlank()) {
            appendTrace(Instant.now(), request.traceId(), "objects_content", request.callerId(), request.uri(), null, entry.scope(), snapshot.manifest().releaseId(),
                    List.of(request.uri()), List.of(request.uri()), List.of(request.uri()), RefusalReason.none, List.of("read_object_l2"));
        }
        return content;
    }

    public DependencyResult dependencies(DependenciesRequest request) {
        ProjectionSnapshot snapshot = activeSnapshotForRequestRelease(request.releaseId());
        ObjectManifestEntry entry = projectionStore.getObject(snapshot.manifest().releaseId(), request.uri())
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Object not found"));
        requireScope(snapshot.manifest().releaseId(), entry.scope());
        permissionService.requireAllowed(snapshot.manifest().releaseId(), request.callerId(), request.apiKey(), entry.scope(), "objects_dependencies", "default");
        int depth = request.depth() == null ? 1 : request.depth();
        DependencyResult result = dependencyService.traverse(snapshot.manifest().releaseId(), request.uri(), request.direction(), request.edgeType(), depth, 50);
        requireDependencyResultAllowed(snapshot.manifest().releaseId(), request.callerId(), request.apiKey(), result);
        return result;
    }

    public ServiceAnswer query(QueryRequest request) {
        Instant start = Instant.now();
        String traceId = newTraceId();
        List<String> candidateUris = new ArrayList<>();
        List<String> selectedUris = new ArrayList<>();
        List<String> l2ReadUris = new ArrayList<>();
        QueryPlan plan = null;
        ScopeKey scope = request.scopeHint();
        String releaseId = null;
        try {
            ProjectionSnapshot snapshot = activeSnapshot();
            ReleaseManifest manifest = snapshot.manifest();
            releaseId = manifest.releaseId();
            plan = queryResolver.resolve(request.query(), scope);
            if (plan.needsClarification()) {
                return tracedRefusal(start, traceId, request, plan, scope, releaseId, RefusalReason.scope_unclear, plan.clarificationQuestion(), candidateUris, selectedUris, l2ReadUris);
            }
            scope = requireScope(releaseId, plan.scope());
            permissionService.requireAllowed(releaseId, request.callerId(), request.apiKey(), scope, "query", outputMode(request.outputMode()));
            List<CandidateObject> candidates = findCandidates(releaseId, scope, request.query(), safe(plan.anchors()), List.of(), 10);
            candidateUris.addAll(candidates.stream().map(CandidateObject::uri).toList());
            if (candidates.isEmpty()) {
                return tracedRefusal(start, traceId, request, plan, scope, releaseId, RefusalReason.object_not_found, "No released structured object matched the query", candidateUris, selectedUris, l2ReadUris);
            }
            CandidateObject selected = candidates.get(0);
            selectedUris.add(selected.uri());
            ObjectManifestEntry entry = projectionStore.getObject(releaseId, selected.uri())
                    .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Selected object disappeared"));
            L2Content content = projectionStore.getContentRef(releaseId, selected.uri())
                    .map(contentStore::read)
                    .orElseThrow(() -> new QueryRefusalException(RefusalReason.l2_missing, "Selected object has no L2 content"));
            l2ReadUris.add(content.uri());
            DependencyResult dependencies = dependencyService.traverse(releaseId, selected.uri(), dependencyDirection(plan.intent()), null, 1, 20);
            requireDependencyResultAllowed(releaseId, request.callerId(), request.apiKey(), dependencies);
            ServiceAnswer answer = answerAssembler.answer(scope, releaseId, entry, content, dependencies.edges(), traceId, List.of());
            finalAnswerValidator.validate(answer, Set.copyOf(l2ReadUris));
            appendTrace(start, traceId, "query", request.callerId(), request.query(), plan, scope, releaseId, candidateUris, selectedUris, l2ReadUris, RefusalReason.none, List.of());
            return answer;
        } catch (ProjectionValidationException ex) {
            return tracedRefusal(start, traceId, request, plan, scope, releaseId, RefusalReason.manifest_invalid, ex.getMessage(), candidateUris, selectedUris, l2ReadUris);
        } catch (QueryRefusalException ex) {
            return tracedRefusal(start, traceId, request, plan, scope, releaseId, ex.reason(), ex.getMessage(), candidateUris, selectedUris, l2ReadUris);
        }
    }

    public Optional<TraceRecord> trace(String traceId) {
        return traceStore.getQueryTrace(traceId);
    }

    public Optional<TraceRecord> trace(String traceId, String callerId, String apiKey) {
        Optional<TraceRecord> trace = traceStore.getQueryTrace(traceId);
        trace.ifPresent(record -> {
            if (record.resolvedScope() == null) {
                permissionService.requireAdmin(loadActiveReleaseId(), callerId, apiKey);
            } else {
                permissionService.requireAllowed(record.releaseId() == null ? loadActiveReleaseId() : record.releaseId(), callerId, apiKey, record.resolvedScope(), "trace", "default");
            }
        });
        return trace;
    }

    private void requireDependencyResultAllowed(String releaseId, String callerId, String apiKey, DependencyResult result) {
        for (ObjectManifestEntry object : result.objects()) {
            requireScope(releaseId, object.scope());
            permissionService.requireAllowed(releaseId, callerId, apiKey, object.scope(), "objects_dependencies", "default");
        }
        for (var edge : result.edges()) {
            ObjectManifestEntry from = projectionStore.getObject(releaseId, edge.fromUri())
                    .orElseThrow(() -> new QueryRefusalException(RefusalReason.dependency_unreleased, "Dependency source is not released"));
            ObjectManifestEntry to = projectionStore.getObject(releaseId, edge.toUri())
                    .orElseThrow(() -> new QueryRefusalException(RefusalReason.dependency_unreleased, "Dependency target is not released"));
            permissionService.requireAllowed(releaseId, callerId, apiKey, from.scope(), "objects_dependencies", "default");
            permissionService.requireAllowed(releaseId, callerId, apiKey, to.scope(), "objects_dependencies", "default");
        }
    }

    private String loadActiveReleaseId() {
        return activeSnapshot().manifest().releaseId();
    }

    private List<CandidateObject> findCandidates(String releaseId, ScopeKey scope, String query, List<String> anchors, List<String> objectTypes, int limit) {
        List<CandidateObject> exact = deterministicLookup(releaseId, scope, anchors);
        if (exact.isEmpty() && hasStrongAnchor(anchors)) {
            return List.of();
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<CandidateObject> combined = new ArrayList<>();
        exact.forEach(candidate -> {
            seen.add(candidate.uri());
            combined.add(candidate);
        });
        List<CandidateObject> bm25 = luceneIndexService.search(releaseId, scope, query == null ? String.join(" ", anchors) : query, objectTypes, limit);
        for (CandidateObject candidate : bm25) {
            if (seen.add(candidate.uri())) {
                combined.add(candidate);
            }
        }
        return combined.stream()
                .sorted(Comparator.<CandidateObject, Boolean>comparing(CandidateObject::exactMatch).reversed()
                        .thenComparing(Comparator.comparingDouble(CandidateObject::score).reversed()))
                .limit(limit)
                .toList();
    }

    private boolean hasStrongAnchor(List<String> anchors) {
        if (anchors == null) {
            return false;
        }
        return anchors.stream().anyMatch(anchor -> {
            String value = anchor.toLowerCase(Locale.ROOT);
            return value.startsWith("rts://")
                    || value.startsWith("rule_")
                    || value.startsWith("lookup_")
                    || value.startsWith("helper_")
                    || value.contains(".");
        });
    }

    private List<CandidateObject> deterministicLookup(String releaseId, ScopeKey scope, List<String> anchors) {
        if (anchors == null || anchors.isEmpty()) {
            return List.of();
        }
        List<String> normalizedAnchors = anchors.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList();
        return projectionStore.allObjects(releaseId).stream()
                .filter(entry -> entry.scope().matches(scope))
                .filter(entry -> normalizedAnchors.stream().anyMatch(anchor -> matches(entry, anchor)))
                .map(entry -> new CandidateObject(entry.uri(), entry.objectType(), 1000.0, List.of("deterministic"), true))
                .toList();
    }

    private boolean matches(ObjectManifestEntry entry, String anchor) {
        if (entry.uri().toLowerCase(Locale.ROOT).equals(anchor)) {
            return true;
        }
        if (entry.objectId().toLowerCase(Locale.ROOT).equals(anchor)) {
            return true;
        }
        if (entry.targetPath() != null && entry.targetPath().toLowerCase(Locale.ROOT).equals(anchor)) {
            return true;
        }
        return entry.sourceAnchors() != null
                && entry.sourceAnchors().stream().anyMatch(source -> source.toLowerCase(Locale.ROOT).equals(anchor));
    }

    private ScopeKey requireScope(String releaseId, ScopeKey scope) {
        if (scope == null) {
            throw new QueryRefusalException(RefusalReason.scope_unclear, "Scope is required");
        }
        return scopeRegistry.resolve(releaseId, scope)
                .map(record -> scope)
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.scope_unclear, "Scope is not active in release"));
    }

    private ProjectionSnapshot activeSnapshot() {
        try {
            return projectionStore.loadActiveSnapshot();
        } catch (ProjectionValidationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new QueryRefusalException(RefusalReason.active_release_missing, "Active release is unavailable");
        }
    }

    private ProjectionSnapshot activeSnapshotForRequestRelease(String requestedReleaseId) {
        ProjectionSnapshot snapshot = activeSnapshot();
        if (requestedReleaseId != null && !requestedReleaseId.isBlank() && !snapshot.manifest().releaseId().equals(requestedReleaseId)) {
            throw new QueryRefusalException(RefusalReason.unauthorized_scope, "Day1 runtime tools can only read the active release");
        }
        return snapshot;
    }

    private Direction dependencyDirection(String intent) {
        if ("impact_preview".equals(intent)) {
            return Direction.reverse;
        }
        if ("dependency_lookup".equals(intent) || "explain_rule".equals(intent)) {
            return Direction.forward;
        }
        return Direction.forward;
    }

    private ServiceAnswer tracedRefusal(Instant start, String traceId, QueryRequest request, QueryPlan plan, ScopeKey scope, String releaseId,
            RefusalReason reason, String message, List<String> candidateUris, List<String> selectedUris, List<String> l2ReadUris) {
        appendTrace(start, traceId, "query", request.callerId(), request.query(), plan, scope, releaseId, candidateUris, selectedUris, l2ReadUris, reason, List.of());
        return new ServiceAnswer(AnswerType.refusal, scope, releaseId, List.of(), List.of(), List.of(message), List.of(), List.of(), List.of(), List.of(),
                traceId, new Refusal(reason, message, List.of("提供明确 scope、released object URI、target path、rule/lookup/helper id"), !candidateUris.isEmpty()), List.of(), null);
    }

    public void appendTrace(Instant start, String traceId, String entrypoint, String callerId, String queryText, QueryPlan plan, ScopeKey scope,
            String releaseId, List<String> candidateUris, List<String> selectedUris, List<String> l2ReadUris, RefusalReason refusal, List<String> toolCalls) {
        traceStore.appendQueryTrace(TraceRecord.builder(traceId, entrypoint)
                .callerId(callerId)
                .queryText(queryText)
                .queryPlan(plan)
                .resolvedScope(scope)
                .candidateUris(candidateUris)
                .selectedUris(selectedUris)
                .l2ReadUris(l2ReadUris)
                .refusalReason(refusal)
                .releaseId(releaseId)
                .durationMs(Duration.between(start, Instant.now()).toMillis())
                .toolCalls(toolCalls)
                .build());
    }

    public String newTraceId() {
        return "trace-" + UUID.randomUUID();
    }

    private List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }

    private int limit(Integer value) {
        return value == null ? 10 : Math.max(1, Math.min(value, 50));
    }

    private String outputMode(String value) {
        return value == null || value.isBlank() ? "default" : value;
    }

    public record ObjectEnvelope(ObjectManifestEntry objectManifest, ObjectCard objectCard, DependencyResult dependencySummary) {}
}
