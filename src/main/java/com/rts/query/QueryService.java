package com.rts.query;

import com.rts.config.RtsProperties;
import com.rts.agent.ContextBuilder;
import com.rts.index.LuceneIndexService;
import com.rts.model.AgentServiceModels.BudgetUsage;
import com.rts.model.AgentServiceModels.AgentRun;
import com.rts.model.AgentServiceModels.AgentStep;
import com.rts.model.AgentServiceModels.ArgumentBinding;
import com.rts.model.AgentServiceModels.FeatureFlags;
import com.rts.model.AgentServiceModels.GroundedClaim;
import com.rts.model.AgentServiceModels.GroundingEvidence;
import com.rts.model.AgentServiceModels.GroundingMap;
import com.rts.model.AgentServiceModels.LoopTransition;
import com.rts.model.AgentServiceModels.ToolInvocation;
import com.rts.model.AgentServiceModels.ToolObservation;
import com.rts.model.AgentServiceModels.ToolStepTrace;
import com.rts.model.AgentServiceModels.ValidatedClaim;
import com.rts.model.AgentServiceModels.ValidationStatus;
import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.AgentPlan;
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
import com.rts.store.Hashing;
import com.rts.store.StoreContracts.ContentStore;
import com.rts.store.StoreContracts.ProjectionSnapshot;
import com.rts.store.StoreContracts.ProjectionStore;
import com.rts.store.StoreContracts.ScopeRegistry;
import com.rts.store.StoreContracts.TraceStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
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
    private final PromptPolicyGuard promptPolicyGuard;
    private final AliasEntityService aliasEntityService;
    private final RtsProperties properties;
    private final ContextBuilder contextBuilder;

    public QueryService(ProjectionStore projectionStore, ScopeRegistry scopeRegistry, TraceStore traceStore,
            ContentStore contentStore, LuceneIndexService luceneIndexService, QueryResolver queryResolver,
            DependencyService dependencyService, PermissionService permissionService, AnswerAssembler answerAssembler,
            FinalAnswerValidator finalAnswerValidator, PromptPolicyGuard promptPolicyGuard, AliasEntityService aliasEntityService,
            RtsProperties properties, ContextBuilder contextBuilder) {
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
        this.promptPolicyGuard = promptPolicyGuard;
        this.aliasEntityService = aliasEntityService;
        this.properties = properties;
        this.contextBuilder = contextBuilder;
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
        int depth = boundedDependencyDepth(request.depth());
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
            promptPolicyGuard.validateUserText(request.query());
            ProjectionSnapshot snapshot = activeSnapshot();
            ReleaseManifest manifest = snapshot.manifest();
            releaseId = manifest.releaseId();
            plan = queryResolver.resolve(request.query(), scope);
            if (plan.needsClarification()) {
                return tracedRefusal(start, traceId, request, plan, scope, releaseId, RefusalReason.scope_unclear, plan.clarificationQuestion(), candidateUris, selectedUris, l2ReadUris);
            }
            scope = requireScope(releaseId, plan.scope());
            permissionService.requireAllowed(releaseId, request.callerId(), request.apiKey(), scope, "query", outputMode(request.outputMode()));
            List<CandidateObject> candidates = findCandidates(releaseId, scope, request.query(), safe(plan.anchors()), objectTypesForIntent(plan.intent()), 10);
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
            ServiceAnswer answer = answerAssembler.answer(scope, releaseId, entry, content, dependencies.edges(), traceId, warningsFor(releaseId, entry));
            finalAnswerValidator.validate(answer, Map.of(content.uri(), content.contentHash()));
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
        for (var binding : result.fieldBindings()) {
            ObjectManifestEntry object = projectionStore.getObject(releaseId, binding.objectUri())
                    .orElseThrow(() -> new QueryRefusalException(RefusalReason.dependency_unreleased, "Field binding object is not released"));
            permissionService.requireAllowed(releaseId, callerId, apiKey, object.scope(), "objects_dependencies", "default");
            if (binding.viaUri() != null && !binding.viaUri().isBlank()) {
                ObjectManifestEntry via = projectionStore.getObject(releaseId, binding.viaUri())
                        .orElseThrow(() -> new QueryRefusalException(RefusalReason.dependency_unreleased, "Field binding dependency is not released"));
                permissionService.requireAllowed(releaseId, callerId, apiKey, via.scope(), "objects_dependencies", "default");
            }
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
        String expandedQuery = aliasEntityService.expand(query == null ? String.join(" ", anchors) : query, anchors);
        List<CandidateObject> bm25 = luceneIndexService.search(releaseId, scope, expandedQuery, objectTypes, limit);
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

    private List<String> objectTypesForIntent(String intent) {
        if ("lookup_lookup".equals(intent)) {
            return List.of("lookup");
        }
        if ("helper_lookup".equals(intent)) {
            return List.of("helper");
        }
        if ("rule_lookup".equals(intent)
                || "explain_rule".equals(intent)
                || "generate_target_message".equals(intent)
                || "compare_source_target".equals(intent)
                || "test_planning".equals(intent)
                || "confidence_check".equals(intent)) {
            return List.of("rule");
        }
        return List.of();
    }

    private List<String> warningsFor(String releaseId, ObjectManifestEntry entry) {
        List<String> warnings = new ArrayList<>();
        projectionStore.getCard(releaseId, entry.uri()).ifPresent(card -> {
            if (card.riskFlags() != null && !card.riskFlags().isEmpty()) {
                warnings.add("Object risk flags: " + card.riskFlags());
            }
            if (card.cardJson() != null && card.cardJson().containsKey("status")) {
                warnings.add("Object governance status: " + card.cardJson().get("status"));
            }
        });
        return List.copyOf(warnings);
    }

    private ServiceAnswer tracedRefusal(Instant start, String traceId, QueryRequest request, QueryPlan plan, ScopeKey scope, String releaseId,
            RefusalReason reason, String message, List<String> candidateUris, List<String> selectedUris, List<String> l2ReadUris) {
        appendTrace(start, traceId, "query", request.callerId(), request.query(), plan, scope, releaseId, candidateUris, selectedUris, l2ReadUris, reason, List.of());
        return new ServiceAnswer(AnswerType.refusal, scope, releaseId, List.of(), List.of(), List.of(message), List.of(), List.of(), List.of(), List.of(),
                traceId, new Refusal(reason, message, List.of("提供明确 scope、released object URI、target path、rule/lookup/helper id"), !candidateUris.isEmpty()), List.of(), null);
    }

    public void appendTrace(Instant start, String traceId, String entrypoint, String callerId, String queryText, QueryPlan plan, ScopeKey scope,
            String releaseId, List<String> candidateUris, List<String> selectedUris, List<String> l2ReadUris, RefusalReason refusal, List<String> toolCalls) {
        appendTrace(start, traceId, entrypoint, callerId, queryText, plan, scope, releaseId, candidateUris, selectedUris, l2ReadUris, refusal, toolCalls, null);
    }

    public void appendTrace(Instant start, String traceId, String entrypoint, String callerId, String queryText, QueryPlan plan, ScopeKey scope,
            String releaseId, List<String> candidateUris, List<String> selectedUris, List<String> l2ReadUris, RefusalReason refusal, List<String> toolCalls,
            String answerView) {
        appendTrace(start, traceId, entrypoint, callerId, queryText, plan, null, scope, releaseId, candidateUris, selectedUris, l2ReadUris,
                refusal, toolCalls, answerView, null, null);
    }

    public void appendTrace(Instant start, String traceId, String entrypoint, String callerId, String queryText, QueryPlan plan, AgentPlan agentPlan,
            ScopeKey scope, String releaseId, List<String> candidateUris, List<String> selectedUris, List<String> l2ReadUris, RefusalReason refusal,
            List<String> toolCalls, String answerView, String scenarioInputSummary, String scenarioInputHash) {
        appendTrace(start, traceId, entrypoint, callerId, queryText, plan, agentPlan, scope, releaseId, candidateUris, selectedUris, l2ReadUris,
                refusal, toolCalls, null, answerView, scenarioInputSummary, scenarioInputHash);
    }

    public void appendTrace(Instant start, String traceId, String entrypoint, String callerId, String queryText, QueryPlan plan, AgentPlan agentPlan,
            ScopeKey scope, String releaseId, List<String> candidateUris, List<String> selectedUris, List<String> l2ReadUris, RefusalReason refusal,
            List<String> toolCalls, List<ToolStepTrace> suppliedToolSteps, String answerView, String scenarioInputSummary, String scenarioInputHash) {
        appendTrace(start, traceId, null, entrypoint, callerId, queryText, plan, agentPlan, scope, releaseId, candidateUris, selectedUris,
                l2ReadUris, refusal, toolCalls, suppliedToolSteps, answerView, scenarioInputSummary, scenarioInputHash);
    }

    public void appendTrace(Instant start, String traceId, String runId, String entrypoint, String callerId, String queryText, QueryPlan plan,
            AgentPlan agentPlan, ScopeKey scope, String releaseId, List<String> candidateUris, List<String> selectedUris, List<String> l2ReadUris,
            RefusalReason refusal, List<String> toolCalls, List<ToolStepTrace> suppliedToolSteps, String answerView, String scenarioInputSummary,
            String scenarioInputHash) {
        appendTrace(start, traceId, runId, entrypoint, callerId, queryText, plan, agentPlan, scope, releaseId, candidateUris, selectedUris,
                l2ReadUris, refusal, toolCalls, suppliedToolSteps, answerView, scenarioInputSummary, scenarioInputHash, (AgentRun) null);
    }

    public void appendTrace(Instant start, String traceId, String runId, String entrypoint, String callerId, String queryText, QueryPlan plan,
            AgentPlan agentPlan, ScopeKey scope, String releaseId, List<String> candidateUris, List<String> selectedUris, List<String> l2ReadUris,
            RefusalReason refusal, List<String> toolCalls, List<ToolStepTrace> suppliedToolSteps, String answerView, String scenarioInputSummary,
            String scenarioInputHash, GroundingMap suppliedGroundingMap) {
        appendTrace(start, traceId, runId, entrypoint, callerId, queryText, plan, agentPlan, scope, releaseId, candidateUris, selectedUris,
                l2ReadUris, refusal, toolCalls, suppliedToolSteps, answerView, scenarioInputSummary, scenarioInputHash, null, suppliedGroundingMap);
    }

    public void appendTrace(Instant start, String traceId, String runId, String entrypoint, String callerId, String queryText, QueryPlan plan,
            AgentPlan agentPlan, ScopeKey scope, String releaseId, List<String> candidateUris, List<String> selectedUris, List<String> l2ReadUris,
            RefusalReason refusal, List<String> toolCalls, List<ToolStepTrace> suppliedToolSteps, String answerView, String scenarioInputSummary,
            String scenarioInputHash, AgentRun preservedAgentRun) {
        appendTrace(start, traceId, runId, entrypoint, callerId, queryText, plan, agentPlan, scope, releaseId, candidateUris, selectedUris,
                l2ReadUris, refusal, toolCalls, suppliedToolSteps, answerView, scenarioInputSummary, scenarioInputHash, preservedAgentRun, null);
    }

    public void appendTrace(Instant start, String traceId, String runId, String entrypoint, String callerId, String queryText, QueryPlan plan,
            AgentPlan agentPlan, ScopeKey scope, String releaseId, List<String> candidateUris, List<String> selectedUris, List<String> l2ReadUris,
            RefusalReason refusal, List<String> toolCalls, List<ToolStepTrace> suppliedToolSteps, String answerView, String scenarioInputSummary,
            String scenarioInputHash, AgentRun preservedAgentRun, GroundingMap suppliedGroundingMap) {
        List<ToolStepTrace> toolSteps = suppliedToolSteps == null || suppliedToolSteps.isEmpty()
                ? toolSteps(toolCalls, candidateUris, selectedUris, l2ReadUris)
                : List.copyOf(suppliedToolSteps);
        GroundingMap groundingMap = suppliedGroundingMap == null ? groundingMap(releaseId, l2ReadUris) : suppliedGroundingMap;
        BudgetUsage budgetUsage = new BudgetUsage(
                toolCalls == null ? 0 : toolCalls.size(),
                properties.getMaxToolCalls(),
                l2ReadUris == null ? 0 : l2ReadUris.size(),
                properties.getMaxL2Objects(),
                dependencyDepthFromPlan(plan),
                properties.getMaxDependencyDepth(),
                estimateRetrievedTokens(candidateUris, selectedUris, l2ReadUris),
                properties.getMaxRetrievedTokens(),
                "ask".equals(entrypoint) ? 1 : 0,
                properties.getMaxModelCalls(),
                Duration.between(start, Instant.now()).toMillis(),
                properties.getMaxLatencyMs());
        traceStore.appendQueryTrace(TraceRecord.builder(traceId, entrypoint)
                .callerId(callerId)
                .queryText(redactedQueryText(entrypoint, queryText))
                .queryTextHash(queryText == null ? null : Hashing.sha256(queryText))
                .queryPlan(plan)
                .agentPlan(agentPlan)
                .resolvedScope(scope)
                .candidateUris(candidateUris)
                .selectedUris(selectedUris)
                .l2ReadUris(l2ReadUris)
                .refusalReason(refusal)
                .releaseId(releaseId)
                .runId(runId)
                .agentRun(preservedAgentRun == null
                        ? agentRun(runId, traceId, entrypoint, callerId, answerView, releaseId, scope, agentPlan, budgetUsage, toolSteps, groundingMap)
                        : preservedAgentRun)
                .durationMs(Duration.between(start, Instant.now()).toMillis())
                .toolCalls(toolCalls)
                .toolSteps(toolSteps)
                .groundingMap(groundingMap)
                .answerView(answerView == null || answerView.isBlank() ? outputModeFromEntrypoint(entrypoint) : answerView)
                .budgetUsage(budgetUsage)
                .scenarioInputSummary(scenarioInputSummary)
                .scenarioInputHash(scenarioInputHash)
                .contextHash(Hashing.sha256(String.valueOf(candidateUris) + String.valueOf(selectedUris) + String.valueOf(l2ReadUris)))
                .status(status(refusal))
                .build());
    }

    private AgentRun agentRun(String runId, String traceId, String entrypoint, String callerId, String answerView, String releaseId,
            ScopeKey scope, AgentPlan agentPlan, BudgetUsage budgetUsage, List<ToolStepTrace> toolSteps, GroundingMap groundingMap) {
        if (runId == null || runId.isBlank()) {
            return null;
        }
        List<AgentStep> steps = new ArrayList<>();
        steps.add(new AgentStep(1, "run_start", "allowed", null, null, null, List.of(), "release_scope_caller_bound"));
        int nextStep = 2;
        if (toolSteps != null) {
            for (ToolStepTrace toolStep : toolSteps) {
                steps.add(new AgentStep(nextStep++, "tool_call", toolStep.policyResult() != null && toolStep.policyResult().startsWith("refused")
                        ? "refused" : "allowed", toolStep.toolName(), toolStep.toolInputHash(), toolStep.toolOutputHash(),
                        toolStep.selectedUris(), toolStep.policyResult()));
            }
        }
        steps.add(new AgentStep(nextStep, "answer_compile", "completed", null, null, null, List.of(), "service_validated"));
        return new AgentRun(runId, traceId, entrypoint, callerId, answerView == null || answerView.isBlank() ? outputModeFromEntrypoint(entrypoint) : answerView,
                releaseId, scope, agentPlan == null ? null : agentPlan.recipeVersion(), budgetUsage, featureFlags(),
                contextBuilder.agentRunSnapshot(callerId, scope, groundingMap), List.copyOf(steps), loopTransitions(toolSteps, groundingMap),
                stopReason(groundingMap, toolSteps),
                argumentBindings(toolSteps, releaseId, scope, callerId),
                toolInvocations(toolSteps), toolObservations(toolSteps), validatedClaims(groundingMap));
    }

    private List<LoopTransition> loopTransitions(List<ToolStepTrace> toolSteps, GroundingMap groundingMap) {
        if (toolSteps == null || toolSteps.isEmpty()) {
            return groundingMap == null || groundingMap.claims() == null || groundingMap.claims().isEmpty()
                    ? List.of(new LoopTransition(1, "no_tool_observation", "clarify_or_refuse", "no truth-eligible observation was produced", "hard_stop"))
                    : List.of();
        }
        List<LoopTransition> transitions = new ArrayList<>();
        for (int i = 0; i < toolSteps.size(); i++) {
            ToolStepTrace step = toolSteps.get(i);
            if (step.policyResult() != null && step.policyResult().startsWith("refused")) {
                transitions.add(new LoopTransition(i + 1, step.toolName(), "refuse", step.policyResult(), "hard_stop"));
            } else if (step.policyResult() != null && step.policyResult().contains("revise_dependency_l2")) {
                transitions.add(new LoopTransition(i + 1, "dependency_gap_observed", "revise",
                        "service policy required dependency L2 evidence after dependency observation", "read_object_l2"));
            } else if ("read_object_l2".equals(step.toolName())) {
                transitions.add(new LoopTransition(i + 1, "l2_observed", "continue", "truth-eligible L2 evidence observed", "validate_claims"));
            } else if ("get_dependencies".equals(step.toolName())) {
                transitions.add(new LoopTransition(i + 1, "dependency_observed", "continue", "dependency evidence observed", "compile_or_validate"));
            } else {
                transitions.add(new LoopTransition(i + 1, step.toolName() + "_observed", "continue", "recipe tool completed", "next_recipe_tool"));
            }
        }
        if (groundingMap != null && groundingMap.claims() != null && !groundingMap.claims().isEmpty()) {
            transitions.add(new LoopTransition(toolSteps.size() + 1, "grounding_observed", "answer", "grounded claims available", "hard_stop"));
        } else {
            transitions.add(new LoopTransition(toolSteps.size() + 1, "no_grounded_claim", "clarify_or_refuse", "no validated claim available", "hard_stop"));
        }
        return List.copyOf(transitions);
    }

    private String stopReason(GroundingMap groundingMap, List<ToolStepTrace> toolSteps) {
        if (toolSteps != null && toolSteps.stream().anyMatch(step -> step.policyResult() != null && step.policyResult().startsWith("refused"))) {
            return "tool_refusal";
        }
        if (groundingMap != null && groundingMap.claims() != null && !groundingMap.claims().isEmpty()) {
            return "answer_ready_grounded";
        }
        return "clarify_or_refuse_no_grounded_claim";
    }

    private List<ToolInvocation> toolInvocations(List<ToolStepTrace> toolSteps) {
        if (toolSteps == null || toolSteps.isEmpty()) {
            return List.of();
        }
        return toolSteps.stream()
                .map(step -> new ToolInvocation(step.toolName(), toolSchema(step.toolName(), "input"), step.toolInputHash(),
                        step.selectedUris(), step.policyResult()))
                .toList();
    }

    private List<ArgumentBinding> argumentBindings(List<ToolStepTrace> toolSteps, String releaseId, ScopeKey scope, String callerId) {
        if (toolSteps == null || toolSteps.isEmpty()) {
            return List.of();
        }
        return toolSteps.stream()
                .map(step -> new ArgumentBinding(step.toolName(), step.toolInputHash(), releaseId, scope, callerId, purposeFor(step.toolName()),
                        step.policyResult()))
                .toList();
    }

    private String purposeFor(String toolName) {
        if ("read_object_l2".equals(toolName) || "get_dependencies".equals(toolName)) {
            return "answer";
        }
        if ("resolve_scope".equals(toolName)) {
            return "scope_resolution";
        }
        if ("find_objects".equals(toolName)) {
            return "navigation";
        }
        return "managed_analysis";
    }

    private List<ToolObservation> toolObservations(List<ToolStepTrace> toolSteps) {
        if (toolSteps == null || toolSteps.isEmpty()) {
            return List.of();
        }
        return toolSteps.stream()
                .map(step -> new ToolObservation(step.toolName(), toolSchema(step.toolName(), "output"), step.toolOutputHash(),
                        step.selectedUris(), "hash_inputs_keep_release_scope_uri_and_content_hash"))
                .toList();
    }

    private List<ValidatedClaim> validatedClaims(GroundingMap groundingMap) {
        if (groundingMap == null || groundingMap.claims() == null || groundingMap.claims().isEmpty()) {
            return List.of();
        }
        return groundingMap.claims().stream()
                .map(claim -> new ValidatedClaim(claim.claim(), "fact", claim.groundedBy(), claim.validation(), claim.reason()))
                .toList();
    }

    private String toolSchema(String toolName, String direction) {
        return "rts.tool." + toolName + ".v1." + direction;
    }

    private FeatureFlags featureFlags() {
        return new FeatureFlags(
                properties.isPlannerV2Enabled(),
                properties.isToolOrchestratorEnabled(),
                properties.isRerankerEnabled(),
                properties.isConfusableCheckEnabled(),
                properties.isVectorRecallEnabled(),
                properties.isImpactCandidatesEnabled(),
                properties.isTestPlanCandidatesEnabled(),
                properties.isMcpExpandedToolsEnabled());
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

    private String redactedQueryText(String entrypoint, String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return null;
        }
        if (entrypoint != null && (entrypoint.contains("scenario") || entrypoint.contains("message") || entrypoint.contains("exception")
                || entrypoint.contains("pr_diff") || entrypoint.contains("failed"))) {
            return "[redacted external input]";
        }
        String normalized = queryText.replaceAll("\\s+", " ").strip();
        return normalized.length() > 160 ? normalized.substring(0, 160) + "..." : normalized;
    }

    private List<ToolStepTrace> toolSteps(List<String> toolCalls, List<String> candidateUris, List<String> selectedUris, List<String> l2ReadUris) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }
        List<ToolStepTrace> steps = new ArrayList<>();
        for (int i = 0; i < toolCalls.size(); i++) {
            String tool = toolCalls.get(i);
            List<String> selected = selectedUris == null ? List.of() : selectedUris;
            if ("find_objects".equals(tool) && candidateUris != null) {
                selected = candidateUris;
            } else if ("read_object_l2".equals(tool) && l2ReadUris != null) {
                selected = l2ReadUris;
            }
            steps.add(new ToolStepTrace(
                    i + 1,
                    tool,
                    Hashing.sha256(tool + ":input:" + i),
                    Hashing.sha256(tool + ":output:" + selected),
                    selected,
                    "allowed"));
        }
        return steps;
    }

    private GroundingMap groundingMap(String releaseId, List<String> l2ReadUris) {
        if (releaseId == null || l2ReadUris == null || l2ReadUris.isEmpty()) {
            return GroundingMap.empty();
        }
        List<GroundedClaim> claims = l2ReadUris.stream()
                .distinct()
                .map(uri -> projectionStore.getContentRef(releaseId, uri)
                        .map(ref -> new GroundedClaim(
                                "L2 object read in trace: " + uri,
                                List.of(new GroundingEvidence(uri, ref.contentHash(), "$")),
                                ValidationStatus.grounded,
                                null))
                        .orElse(new GroundedClaim(
                                "L2 object read but content ref unavailable: " + uri,
                                List.of(),
                                ValidationStatus.warning,
                                "content ref not available while building trace grounding map")))
                .toList();
        return new GroundingMap(claims);
    }

    private int dependencyDepthFromPlan(QueryPlan plan) {
        if (plan == null || plan.toolPlan() == null || !plan.toolPlan().contains("get_dependencies")) {
            return 0;
        }
        return 1;
    }

    private int boundedDependencyDepth(Integer requestedDepth) {
        int depth = requestedDepth == null ? 1 : requestedDepth;
        if (depth < 0 || depth > properties.getMaxDependencyDepth()) {
            throw new QueryRefusalException(RefusalReason.tool_budget_exhausted, "Dependency depth exceeds RTS tool budget");
        }
        return depth;
    }

    private int estimateRetrievedTokens(List<String> candidateUris, List<String> selectedUris, List<String> l2ReadUris) {
        int textChars = 0;
        for (List<String> values : List.of(candidateUris, selectedUris, l2ReadUris)) {
            if (values != null) {
                textChars += values.stream().mapToInt(value -> value == null ? 0 : value.length()).sum();
            }
        }
        return Math.max(0, textChars / 4);
    }

    private String outputModeFromEntrypoint(String entrypoint) {
        if ("ask".equals(entrypoint)) {
            return "human";
        }
        if (entrypoint != null && entrypoint.startsWith("analyze")) {
            return "agent";
        }
        return "default";
    }

    private String status(RefusalReason refusal) {
        if (refusal == null || refusal == RefusalReason.none) {
            return "answered";
        }
        if (refusal == RefusalReason.scope_unclear) {
            return "clarification_required";
        }
        return "refused";
    }

    public record ObjectEnvelope(ObjectManifestEntry objectManifest, ObjectCard objectCard, DependencyResult dependencySummary) {}
}
