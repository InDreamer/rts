package com.rts.agent;

import com.rts.model.AgentServiceModels.AgentObjectEnvelope;
import com.rts.model.AgentServiceModels.CandidateSearchResult;
import com.rts.model.AgentServiceModels.ConfusableObject;
import com.rts.model.AgentServiceModels.ConfusableScope;
import com.rts.model.AgentServiceModels.ContextItem;
import com.rts.model.AgentServiceModels.ContextKind;
import com.rts.model.AgentServiceModels.EvidenceSummary;
import com.rts.model.AgentServiceModels.GroundedClaim;
import com.rts.model.AgentServiceModels.GroundingEvidence;
import com.rts.model.AgentServiceModels.GroundingMap;
import com.rts.model.AgentServiceModels.PackNavigation;
import com.rts.model.AgentServiceModels.ScopeSummary;
import com.rts.model.AgentServiceModels.ScopeTree;
import com.rts.model.CoreModels.CandidateObject;
import com.rts.model.CoreModels.DependencyEdge;
import com.rts.model.CoreModels.Direction;
import com.rts.model.CoreModels.L2Content;
import com.rts.model.CoreModels.ObjectCard;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ReleaseManifest;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.model.CoreModels.ScopeRecord;
import com.rts.query.PermissionService;
import com.rts.query.QueryRefusalException;
import com.rts.query.QueryRequests.DependenciesRequest;
import com.rts.query.QueryRequests.FindRequest;
import com.rts.query.QueryRequests.ObjectContentRequest;
import com.rts.query.QueryRequests.ObjectGetRequest;
import com.rts.query.QueryService;
import com.rts.query.QueryService.ObjectEnvelope;
import com.rts.store.StoreContracts.ProjectionSnapshot;
import com.rts.store.StoreContracts.ProjectionStore;
import com.rts.store.StoreContracts.ScopeRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AgentToolService {
    private final ProjectionStore projectionStore;
    private final ScopeRegistry scopeRegistry;
    private final QueryService queryService;
    private final PermissionService permissionService;
    private final FeatureFlagService featureFlagService;

    public AgentToolService(ProjectionStore projectionStore, ScopeRegistry scopeRegistry, QueryService queryService,
            PermissionService permissionService, FeatureFlagService featureFlagService) {
        this.projectionStore = projectionStore;
        this.scopeRegistry = scopeRegistry;
        this.queryService = queryService;
        this.permissionService = permissionService;
        this.featureFlagService = featureFlagService;
    }

    public ScopeTree listScopes(String callerId, String apiKey, String outputMode) {
        ProjectionSnapshot snapshot = activeSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        List<ScopeSummary> scopes = scopeRegistry.activeScopes(releaseId).stream()
                .filter(scope -> isAllowed(releaseId, callerId, apiKey, scope.key(), "scope_tools", outputMode))
                .map(scope -> summarizeScope(snapshot, scope))
                .toList();
        return new ScopeTree(releaseId, scopes);
    }

    public ScopeTree searchScopes(String query, String callerId, String apiKey, String outputMode) {
        String normalized = normalize(query);
        ScopeTree tree = listScopes(callerId, apiKey, outputMode);
        if (normalized.isBlank()) {
            return tree;
        }
        List<ScopeSummary> matches = tree.scopes().stream()
                .filter(scope -> normalize(scope.scope().value()).contains(normalized)
                        || normalize(scope.permissionBoundary()).contains(normalized)
                        || normalize(scope.precedencePolicy()).contains(normalized))
                .toList();
        return new ScopeTree(tree.releaseId(), matches);
    }

    public ScopeSummary getScopeSummary(ScopeKey scope, String callerId, String apiKey, String outputMode) {
        ProjectionSnapshot snapshot = activeSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        ScopeRecord record = requireScopeRecord(releaseId, scope);
        permissionService.requireAllowed(releaseId, callerId, apiKey, scope, "scope_tools", mode(outputMode));
        return summarizeScope(snapshot, record);
    }

    public PackNavigation getPackNavigation(ScopeKey scope, String callerId, String apiKey, String outputMode) {
        ProjectionSnapshot snapshot = activeSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        requireScopeRecord(releaseId, scope);
        permissionService.requireAllowed(releaseId, callerId, apiKey, scope, "navigation_tools", mode(outputMode));
        List<ObjectManifestEntry> objects = snapshot.objectManifest().stream()
                .filter(entry -> entry.scope().matches(scope))
                .sorted(Comparator.comparing(ObjectManifestEntry::objectType).thenComparing(ObjectManifestEntry::objectId))
                .toList();
        List<String> uris = objects.stream().map(ObjectManifestEntry::uri).toList();
        List<ObjectCard> cards = snapshot.objectCards().stream()
                .filter(card -> uris.contains(card.uri()))
                .toList();
        List<DependencyEdge> edges = snapshot.dependencyEdges().stream()
                .filter(edge -> uris.contains(edge.fromUri()) || uris.contains(edge.toUri()))
                .toList();
        return new PackNavigation(releaseId, scope, objects, cards, edges, scopeWarnings(snapshot.manifest(), scope, cards));
    }

    public List<ObjectCard> getObjectCards(List<String> uris, String callerId, String apiKey, String outputMode) {
        if (uris == null || uris.isEmpty()) {
            return List.of();
        }
        ProjectionSnapshot snapshot = activeSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        List<ObjectCard> cards = new ArrayList<>();
        for (String uri : uris) {
            ObjectManifestEntry object = requireObject(releaseId, uri);
            permissionService.requireAllowed(releaseId, callerId, apiKey, object.scope(), "navigation_tools", mode(outputMode));
            cards.add(projectionStore.getCard(releaseId, uri)
                    .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Object card not found")));
        }
        return cards;
    }

    public CandidateSearchResult searchObjects(FindRequest request) {
        List<CandidateObject> candidates = queryService.find(request);
        List<ConfusableObject> confusables = candidates.stream()
                .flatMap(candidate -> findConfusableObjects(candidate.uri(), request.scope(), request.callerId(), request.apiKey(), request.outputMode()).stream())
                .distinct()
                .toList();
        return new CandidateSearchResult(candidates, confusables, confusables.isEmpty() ? List.of() : List.of("Similar but non-selected objects are listed as confusables; they are not truth for this scope."));
    }

    public List<CandidateObject> findByTargetPath(String targetPath, ScopeKey scope, String callerId, String apiKey, String outputMode) {
        return queryService.find(new FindRequest(targetPath, scope, List.of("rule"), List.of(targetPath), 10, callerId, apiKey, outputMode));
    }

    public List<CandidateObject> findBySourceAnchor(String sourceAnchor, ScopeKey scope, String callerId, String apiKey, String outputMode) {
        return queryService.find(new FindRequest(sourceAnchor, scope, List.of(), List.of(sourceAnchor), 10, callerId, apiKey, outputMode));
    }

    public List<CandidateObject> findByLookupKey(String lookupIdOrTerm, ScopeKey scope, String callerId, String apiKey, String outputMode) {
        return queryService.find(new FindRequest(lookupIdOrTerm, scope, List.of("lookup"), List.of(lookupIdOrTerm), 10, callerId, apiKey, outputMode));
    }

    public ObjectEnvelope getObjectCard(String uri, String releaseId, String traceId, String callerId, String apiKey) {
        return queryService.getObject(new ObjectGetRequest(uri, releaseId, traceId, callerId, apiKey));
    }

    public L2Content readObjectL2(String uri, String releaseId, String traceId, String callerId, String apiKey, String purpose) {
        return queryService.readContent(new ObjectContentRequest(uri, purpose, releaseId, traceId, callerId, apiKey));
    }

    public AgentObjectEnvelope readAgentObject(String uri, String releaseId, String traceId, String callerId, String apiKey, String purpose) {
        ObjectEnvelope object = getObjectCard(uri, releaseId, traceId, callerId, apiKey);
        L2Content l2 = readObjectL2(uri, releaseId, traceId, callerId, apiKey, purpose);
        List<DependencyEdge> dependencies = queryService.dependencies(new DependenciesRequest(uri, Direction.forward, null, 1, purpose, releaseId, callerId, apiKey)).edges();
        GroundingMap grounding = groundingFor(l2);
        List<ContextItem> context = contextFor(object, l2, dependencies);
        return new AgentObjectEnvelope(object.objectManifest(), object.objectCard(), l2, dependencies, grounding, context);
    }

    public List<DependencyEdge> getDependencySubgraph(String uri, Direction direction, String edgeType, Integer depth, String purpose,
            String releaseId, String callerId, String apiKey) {
        return queryService.dependencies(new DependenciesRequest(uri, direction, edgeType, depth, purpose, releaseId, callerId, apiKey)).edges();
    }

    public List<ConfusableObject> findConfusableObjects(String uri, ScopeKey scope, String callerId, String apiKey, String outputMode) {
        if (!featureFlagService.current().confusableCheckEnabled()) {
            return List.of();
        }
        ProjectionSnapshot snapshot = activeSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        ObjectManifestEntry selected = uri == null || uri.isBlank() ? null : projectionStore.getObject(releaseId, uri).orElse(null);
        ScopeKey baseScope = selected == null ? scope : selected.scope();
        if (baseScope == null) {
            throw new QueryRefusalException(RefusalReason.scope_unclear, "Scope or object URI is required for confusable lookup");
        }
        permissionService.requireAllowed(releaseId, callerId, apiKey, baseScope, "navigation_tools", mode(outputMode));
        String selectedTarget = selected == null ? null : selected.targetPath();
        String selectedObjectId = selected == null ? null : selected.objectId();
        return snapshot.objectManifest().stream()
                .filter(candidate -> selected == null || !candidate.uri().equals(selected.uri()))
                .filter(candidate -> selected == null
                        || Objects.equals(candidate.targetPath(), selectedTarget)
                        || candidate.objectId().equals(selectedObjectId))
                .filter(candidate -> !candidate.scope().matches(baseScope))
                .filter(candidate -> isAllowed(releaseId, callerId, apiKey, candidate.scope(), "navigation_tools", outputMode))
                .map(candidate -> new ConfusableObject(
                        selected == null ? null : selected.uri(),
                        candidate.uri(),
                        "Similar object identity or target exists in a different scope; do not use it unless that scope is explicitly selected.",
                        baseScope,
                        candidate.scope()))
                .toList();
    }

    public List<ConfusableScope> findConfusableScopes(ScopeKey scope, String callerId, String apiKey, String outputMode) {
        if (!featureFlagService.current().confusableCheckEnabled()) {
            return List.of();
        }
        ProjectionSnapshot snapshot = activeSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        requireScopeRecord(releaseId, scope);
        permissionService.requireAllowed(releaseId, callerId, apiKey, scope, "navigation_tools", mode(outputMode));
        return snapshot.scopes().stream()
                .filter(candidate -> !candidate.key().matches(scope))
                .filter(candidate -> candidate.channel().equals(scope.channel())
                        || candidate.product().equals(scope.product())
                        || candidate.pack().equals(scope.pack())
                        || candidate.domain().equals(scope.domain()))
                .filter(candidate -> isAllowed(releaseId, callerId, apiKey, candidate.key(), "navigation_tools", outputMode))
                .map(candidate -> new ConfusableScope(scope, candidate.key(), "Scope shares channel/product/pack/domain terms and must be explicitly selected before use."))
                .toList();
    }

    public L2Content readLookupSample(String uri, String releaseId, String traceId, String callerId, String apiKey) {
        ObjectManifestEntry object = requireObject(activeSnapshotForRequestRelease(releaseId).manifest().releaseId(), uri);
        if (object.objectType() != com.rts.model.CoreModels.ObjectType.lookup) {
            throw new QueryRefusalException(RefusalReason.object_not_found, "Object is not a lookup");
        }
        return readObjectL2(uri, releaseId, traceId, callerId, apiKey, "lookup_sample");
    }

    public L2Content readHelperContract(String uri, String releaseId, String traceId, String callerId, String apiKey) {
        ObjectManifestEntry object = requireObject(activeSnapshotForRequestRelease(releaseId).manifest().releaseId(), uri);
        if (object.objectType() != com.rts.model.CoreModels.ObjectType.helper) {
            throw new QueryRefusalException(RefusalReason.object_not_found, "Object is not a helper");
        }
        return readObjectL2(uri, releaseId, traceId, callerId, apiKey, "helper_contract");
    }

    public EvidenceSummary readEvidenceSummary(String uri, String callerId, String apiKey, String outputMode) {
        ProjectionSnapshot snapshot = activeSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        ObjectManifestEntry object = requireObject(releaseId, uri);
        permissionService.requireAllowed(releaseId, callerId, apiKey, object.scope(), "evidence_tools", mode(outputMode));
        ObjectCard card = projectionStore.getCard(releaseId, uri)
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Object card not found"));
        Object status = card.cardJson() == null ? null : card.cardJson().get("status");
        Object source = card.cardJson() == null ? null : card.cardJson().get("source");
        return new EvidenceSummary(uri, releaseId, status == null ? object.state() : String.valueOf(status),
                source == null ? "Evidence summary is limited to projection-approved card metadata." : String.valueOf(source),
                card.riskFlags(), List.of(object.cardRef()), false,
                List.of("Raw evidence is not included in default operational context."));
    }

    private ScopeSummary summarizeScope(ProjectionSnapshot snapshot, ScopeRecord scope) {
        Map<String, Long> counts = new LinkedHashMap<>();
        snapshot.objectManifest().stream()
                .filter(entry -> entry.scope().matches(scope.key()))
                .collect(java.util.stream.Collectors.groupingBy(entry -> entry.objectType().name(), LinkedHashMap::new, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> counts.put(entry.getKey(), entry.getValue()));
        List<ObjectCard> cards = snapshot.objectCards().stream()
                .filter(card -> snapshot.objectManifest().stream()
                        .anyMatch(entry -> entry.uri().equals(card.uri()) && entry.scope().matches(scope.key())))
                .toList();
        return AgentServiceModelsHelper.scopeSummary(snapshot.manifest(), scope, counts, scopeWarnings(snapshot.manifest(), scope.key(), cards));
    }

    private List<String> scopeWarnings(ReleaseManifest manifest, ScopeKey scope, List<ObjectCard> cards) {
        List<String> warnings = new ArrayList<>();
        if (manifest.blockingIssuesCount() > 0) {
            warnings.add("release has blocking issues");
        }
        if (manifest.contentHashSummary() != null && manifest.contentHashSummary().toLowerCase(Locale.ROOT).contains("draft")) {
            warnings.add("release content hash summary indicates draft material");
        }
        if (cards.stream().anyMatch(card -> card.riskFlags() != null && !card.riskFlags().isEmpty())) {
            warnings.add("scope contains object risk flags; answer views must keep warnings visible");
        }
        if (scope == null) {
            warnings.add("scope unresolved");
        }
        return List.copyOf(warnings);
    }

    private GroundingMap groundingFor(L2Content l2) {
        return new GroundingMap(List.of(new GroundedClaim(
                "L2 object was read and hash-validated",
                List.of(new GroundingEvidence(l2.uri(), l2.contentHash(), "$")),
                com.rts.model.AgentServiceModels.ValidationStatus.grounded,
                null)));
    }

    private List<ContextItem> contextFor(ObjectEnvelope object, L2Content l2, List<DependencyEdge> dependencies) {
        List<ContextItem> context = new ArrayList<>();
        context.add(new ContextItem(ContextKind.object_card, "rts_tool", false, object.objectManifest().uri(), null,
                object.objectCard().searchText()));
        context.add(new ContextItem(ContextKind.l2_fact, "rts_tool", true, l2.uri(), l2.contentHash(), l2.content()));
        for (DependencyEdge edge : dependencies) {
            context.add(new ContextItem(ContextKind.dependency, "rts_tool", true, edge.fromUri(), null,
                    edge.edgeType() + ": " + edge.fromUri() + " -> " + edge.toUri()));
        }
        if (object.objectCard().riskFlags() != null) {
            for (String risk : object.objectCard().riskFlags()) {
                context.add(new ContextItem(ContextKind.governance_warning, "projection_card", false, object.objectManifest().uri(), null, risk));
            }
        }
        return List.copyOf(context);
    }

    private ProjectionSnapshot activeSnapshot() {
        try {
            return projectionStore.loadActiveSnapshot();
        } catch (RuntimeException ex) {
            throw new QueryRefusalException(RefusalReason.active_release_missing, "Active release is unavailable");
        }
    }

    private ProjectionSnapshot activeSnapshotForRequestRelease(String requestedReleaseId) {
        ProjectionSnapshot snapshot = activeSnapshot();
        if (requestedReleaseId != null && !requestedReleaseId.isBlank() && !snapshot.manifest().releaseId().equals(requestedReleaseId)) {
            throw new QueryRefusalException(RefusalReason.unauthorized_scope, "Runtime tools can only read the active release");
        }
        return snapshot;
    }

    private ScopeRecord requireScopeRecord(String releaseId, ScopeKey scope) {
        if (scope == null) {
            throw new QueryRefusalException(RefusalReason.scope_unclear, "Scope is required");
        }
        return scopeRegistry.resolve(releaseId, scope)
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.scope_unclear, "Scope is not active in release"));
    }

    private ObjectManifestEntry requireObject(String releaseId, String uri) {
        return Optional.ofNullable(uri)
                .flatMap(value -> projectionStore.getObject(releaseId, value))
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Object not found"));
    }

    private boolean isAllowed(String releaseId, String callerId, String apiKey, ScopeKey scope, String entrypoint, String outputMode) {
        try {
            permissionService.requireAllowed(releaseId, callerId, apiKey, scope, entrypoint, mode(outputMode));
            return true;
        } catch (QueryRefusalException ex) {
            return false;
        }
    }

    private String mode(String outputMode) {
        return outputMode == null || outputMode.isBlank() ? "default" : outputMode;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static final class AgentServiceModelsHelper {
        private static ScopeSummary scopeSummary(ReleaseManifest manifest, ScopeRecord scope, Map<String, Long> counts, List<String> warnings) {
            return com.rts.model.AgentServiceModels.summarize(manifest.releaseId(), scope, counts, warnings);
        }
    }
}
