package com.rts.agent;

import com.rts.model.AgentServiceModels.GroundedClaim;
import com.rts.model.AgentServiceModels.GroundingCheckResult;
import com.rts.model.AgentServiceModels.GroundingEvidence;
import com.rts.model.AgentServiceModels.GroundingMap;
import com.rts.model.AgentServiceModels.ConflictExplainRequest;
import com.rts.model.AgentServiceModels.ConflictExplanation;
import com.rts.model.AgentServiceModels.ImpactAnalysisRequest;
import com.rts.model.AgentServiceModels.ImpactAnalysisResult;
import com.rts.model.AgentServiceModels.ImpactCandidate;
import com.rts.model.AgentServiceModels.ParsedRawField;
import com.rts.model.AgentServiceModels.RawMessageCandidateRequest;
import com.rts.model.AgentServiceModels.RawMessageCandidateResult;
import com.rts.model.AgentServiceModels.ReleaseReadinessRequest;
import com.rts.model.AgentServiceModels.ReleaseReadinessResult;
import com.rts.model.AgentServiceModels.RuleCompareRequest;
import com.rts.model.AgentServiceModels.RuleCompareResult;
import com.rts.model.AgentServiceModels.TargetFieldCandidate;
import com.rts.model.AgentServiceModels.TestPlanRequest;
import com.rts.model.AgentServiceModels.TestPlanResult;
import com.rts.model.AgentServiceModels.ValidationStatus;
import com.rts.model.CoreModels.DependencyEdge;
import com.rts.model.CoreModels.DependencyResult;
import com.rts.model.CoreModels.Direction;
import com.rts.model.CoreModels.Fact;
import com.rts.model.CoreModels.L2Content;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.model.CoreModels.ObjectType;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.model.CoreModels.TraceRecord;
import com.rts.query.PermissionService;
import com.rts.query.QueryRefusalException;
import com.rts.query.QueryRequests.DependenciesRequest;
import com.rts.query.QueryRequests.FindRequest;
import com.rts.query.QueryRequests.ObjectContentRequest;
import com.rts.query.QueryService;
import com.rts.store.StoreContracts.ProjectionSnapshot;
import com.rts.store.StoreContracts.ProjectionStore;
import com.rts.store.StoreContracts.ScopeRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AgentAnalysisService {
    private final ProjectionStore projectionStore;
    private final ScopeRegistry scopeRegistry;
    private final QueryService queryService;
    private final PermissionService permissionService;
    private final FeatureFlagService featureFlagService;

    public AgentAnalysisService(ProjectionStore projectionStore, ScopeRegistry scopeRegistry, QueryService queryService,
            PermissionService permissionService, FeatureFlagService featureFlagService) {
        this.projectionStore = projectionStore;
        this.scopeRegistry = scopeRegistry;
        this.queryService = queryService;
        this.permissionService = permissionService;
        this.featureFlagService = featureFlagService;
    }

    public ImpactAnalysisResult analyzeImpact(ImpactAnalysisRequest request) {
        if (!featureFlagService.current().impactCandidatesEnabled()) {
            throw new QueryRefusalException(RefusalReason.governance_unauthorized, "Impact candidates feature is disabled");
        }
        ProjectionSnapshot snapshot = activeSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        ScopeKey scope = requireScope(releaseId, request.scope());
        permissionService.requireAllowed(releaseId, request.callerId(), request.apiKey(), scope, "analysis_tools", mode(request.outputMode()));
        String traceId = queryService.newTraceId();
        String seedUri = resolveSeedUri(request, releaseId, scope);
        DependencyResult reverse = queryService.dependencies(new DependenciesRequest(seedUri, Direction.reverse, null, 2, "impact", null,
                request.callerId(), request.apiKey()));
        int limit = bounded(request.maxObjects(), 20);
        List<ImpactCandidate> candidates = reverse.objects().stream()
                .filter(object -> object.scope().matches(scope))
                .filter(object -> object.objectType() == ObjectType.rule || object.objectType() == ObjectType.helper)
                .limit(limit)
                .map(object -> impactCandidate(seedUri, object, reverse.edges()))
                .toList();
        List<Fact> facts = new ArrayList<>();
        List<GroundedClaim> claims = new ArrayList<>();
        if (request.readL2()) {
            for (ImpactCandidate candidate : candidates) {
                L2Content l2 = queryService.readContent(new ObjectContentRequest(candidate.impactedObjectUri(), "impact", null, traceId,
                        request.callerId(), request.apiKey()));
                facts.add(new Fact("Impacted object L2 read for candidate " + candidate.impactedObjectUri(), candidate.impactedObjectUri(), l2.releaseId(), "l2:" + l2.contentHash()));
                claims.add(new GroundedClaim("Impact candidate grounded by L2 " + candidate.impactedObjectUri(),
                        List.of(new GroundingEvidence(candidate.impactedObjectUri(), l2.contentHash(), "$")),
                        ValidationStatus.grounded, null));
            }
            if (candidates.isEmpty()) {
                L2Content l2 = queryService.readContent(new ObjectContentRequest(seedUri, "impact_seed", null, traceId,
                        request.callerId(), request.apiKey()));
                facts.add(new Fact("Changed seed object L2 read for candidate analysis " + seedUri, seedUri, l2.releaseId(), "l2:" + l2.contentHash()));
                claims.add(new GroundedClaim("Changed seed object grounded by L2 " + seedUri,
                        List.of(new GroundingEvidence(seedUri, l2.contentHash(), "$")),
                        ValidationStatus.grounded, null));
            }
        }
        List<String> unknowns = candidates.isEmpty() ? List.of("No reverse dependency candidates were found in the selected scope.") : List.of();
        List<String> warnings = List.of("Impact output is a candidate analysis, not final impact approval.");
        queryService.appendTrace(Instant.now(), traceId, "analyze_impact", request.callerId(), firstNonBlank(request.changedUri(), request.changedTargetPath(), request.changedSourceAnchor()),
                null, scope, releaseId, List.of(seedUri), candidates.stream().map(ImpactCandidate::impactedObjectUri).toList(),
                facts.stream().map(Fact::uri).toList(), RefusalReason.none, List.of("find_seed", "get_reverse_dependencies", request.readL2() ? "read_object_l2" : "cards_only"));
        return new ImpactAnalysisResult("candidate", releaseId, scope, facts,
                List.of("Reverse released dependency traversal produced " + candidates.size() + " impact candidate(s)."),
                candidates, unknowns, warnings, traceId, new GroundingMap(claims));
    }

    public TestPlanResult planTests(TestPlanRequest request) {
        if (!featureFlagService.current().testPlanCandidatesEnabled()) {
            throw new QueryRefusalException(RefusalReason.governance_unauthorized, "Test plan candidates feature is disabled");
        }
        ProjectionSnapshot snapshot = activeSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        ScopeKey scope = requireScope(releaseId, request.scope());
        permissionService.requireAllowed(releaseId, request.callerId(), request.apiKey(), scope, "analysis_tools", mode(request.outputMode()));
        String traceId = queryService.newTraceId();
        List<ObjectManifestEntry> seeds = seedObjects(snapshot, releaseId, scope, request.seedUri(), bounded(request.maxObjects(), 10));
        List<Fact> facts = new ArrayList<>();
        List<String> positive = new ArrayList<>();
        List<String> negative = new ArrayList<>();
        List<String> boundary = new ArrayList<>();
        List<String> coverage = new ArrayList<>();
        List<String> regression = new ArrayList<>();
        List<String> l2Reads = new ArrayList<>();
        for (ObjectManifestEntry seed : seeds) {
            if (request.includeL2()) {
                L2Content l2 = queryService.readContent(new ObjectContentRequest(seed.uri(), "test_planning", null, traceId,
                        request.callerId(), request.apiKey()));
                facts.add(new Fact("Test planning read L2 for " + seed.objectId(), seed.uri(), l2.releaseId(), "l2:" + l2.contentHash()));
                l2Reads.add(seed.uri());
            }
            positive.add("Positive candidate: verify " + seed.objectId() + " produces its governed target or output when applicability conditions hold.");
            negative.add("Negative candidate: verify " + seed.objectId() + " is not applied outside listed not-applicable conditions.");
            boundary.add("Boundary candidate: exercise missing, fallback, or edge source values for " + seed.objectId() + ".");
            DependencyResult deps = queryService.dependencies(new DependenciesRequest(seed.uri(), Direction.forward, null, 1, "test_planning", null,
                    request.callerId(), request.apiKey()));
            if (!deps.edges().isEmpty()) {
                coverage.add("Cover dependencies for " + seed.objectId() + ": " + deps.edges().stream().map(DependencyEdge::toUri).distinct().toList());
                regression.add("Regression focus: re-run dependent lookup/helper paths for " + seed.objectId() + ".");
            }
        }
        List<String> unknowns = seeds.isEmpty() ? List.of("No released seed objects found for test planning.") : List.of("LLM or QA review must decide final test sufficiency.");
        queryService.appendTrace(Instant.now(), traceId, "plan_tests", request.callerId(), request.seedUri(), null, scope, releaseId,
                seeds.stream().map(ObjectManifestEntry::uri).toList(), seeds.stream().map(ObjectManifestEntry::uri).toList(), l2Reads,
                RefusalReason.none, List.of("get_pack_navigation", "get_dependencies", request.includeL2() ? "read_object_l2" : "cards_only"));
        return new TestPlanResult("candidate", releaseId, scope, facts, positive, negative, boundary, coverage, regression, unknowns,
                List.of("Test plan output is a candidate set, not QA signoff."), traceId);
    }

    public ReleaseReadinessResult checkReleaseReadiness(ReleaseReadinessRequest request) {
        ProjectionSnapshot snapshot = activeSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        ScopeKey scope = requireScope(releaseId, request.scope());
        permissionService.requireAllowed(releaseId, request.callerId(), request.apiKey(), scope, "analysis_tools", mode(request.outputMode()));
        String traceId = queryService.newTraceId();
        List<ObjectManifestEntry> objects = snapshot.objectManifest().stream()
                .filter(object -> object.scope().matches(scope))
                .toList();
        List<String> facts = new ArrayList<>();
        facts.add("release_id=" + releaseId);
        facts.add("activation_state=" + snapshot.manifest().activationState());
        facts.add("blocking_issues_count=" + snapshot.manifest().blockingIssuesCount());
        facts.add("object_count=" + objects.size());
        List<String> warnings = new ArrayList<>();
        if (snapshot.manifest().contentHashSummary() != null && snapshot.manifest().contentHashSummary().toLowerCase(Locale.ROOT).contains("draft")) {
            warnings.add("Release content summary indicates draft/reconstructed material; do not present as signoff truth.");
        }
        snapshot.objectCards().stream()
                .filter(card -> objects.stream().anyMatch(object -> object.uri().equals(card.uri())))
                .filter(card -> card.riskFlags() != null && !card.riskFlags().isEmpty())
                .findFirst()
                .ifPresent(card -> warnings.add("Object risk flags are present in this scope."));
        String status = snapshot.manifest().blockingIssuesCount() == 0 ? "ready_for_runtime_projection" : "blocked";
        queryService.appendTrace(Instant.now(), traceId, "check_release_readiness", request.callerId(), scope.value(), null, scope, releaseId,
                objects.stream().map(ObjectManifestEntry::uri).toList(), List.of(), List.of(),
                RefusalReason.none, List.of("validate_manifest", "inspect_scope_cards"));
        return new ReleaseReadinessResult(status, releaseId, scope, snapshot.manifest().blockingIssuesCount(), facts, warnings, traceId);
    }

    public GroundingCheckResult checkGrounding(String traceId, String callerId, String apiKey, String outputMode) {
        TraceRecord trace = queryService.trace(traceId, callerId, apiKey)
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Trace not found"));
        List<GroundedClaim> claims = new ArrayList<>();
        for (String uri : trace.l2ReadUris()) {
            projectionStore.getContentRef(trace.releaseId(), uri).ifPresent(ref ->
                    claims.add(new GroundedClaim(
                            "Trace read L2 object " + uri,
                            List.of(new GroundingEvidence(uri, ref.contentHash(), "$")),
                            ValidationStatus.grounded,
                            null)));
        }
        Set<String> l2Reads = new LinkedHashSet<>(trace.l2ReadUris());
        if (trace.selectedUris() != null) {
            for (String selected : trace.selectedUris()) {
                if (!l2Reads.contains(selected)) {
                    claims.add(new GroundedClaim(
                            "Selected object was not read as L2 in this trace: " + selected,
                            List.of(),
                            ValidationStatus.warning,
                            "Object selection alone is navigation, not final fact grounding."));
                }
            }
        }
        RefusalReason refusal = trace.refusalReason() == null ? RefusalReason.none : trace.refusalReason();
        String status = refusal == RefusalReason.none ? "grounded_or_navigation_only" : "refused";
        return new GroundingCheckResult(status, trace.traceId(), new GroundingMap(claims), refusal,
                claims.isEmpty() ? List.of("No L2 reads were recorded; facts must not be inferred from search hits alone.") : List.of());
    }

    public RawMessageCandidateResult generateRawMessageCandidate(RawMessageCandidateRequest request) {
        ProjectionSnapshot snapshot = activeSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        ScopeKey scope = requireScope(releaseId, request.scope());
        permissionService.requireAllowed(releaseId, request.callerId(), request.apiKey(), scope, "analysis_tools", mode(request.outputMode()));
        String traceId = queryService.newTraceId();
        List<ParsedRawField> parsedFields = parseRawFields(request.rawMessage());
        if (parsedFields.isEmpty()) {
            throw new QueryRefusalException(RefusalReason.object_not_found, "Raw message did not contain parseable source fields");
        }
        int maxObjects = bounded(request.maxObjects(), 10);
        List<ObjectManifestEntry> rules = snapshot.objectManifest().stream()
                .filter(object -> object.scope().matches(scope))
                .filter(object -> object.objectType() == ObjectType.rule)
                .filter(object -> object.sourceAnchors() != null && object.sourceAnchors().stream().anyMatch(anchor -> parsedFields.stream()
                        .anyMatch(field -> sourceMatches(anchor, field.sourcePath()))))
                .sorted(Comparator.comparing(ObjectManifestEntry::objectId))
                .limit(maxObjects)
                .toList();
        List<TargetFieldCandidate> candidates = new ArrayList<>();
        List<GroundedClaim> claims = new ArrayList<>();
        List<String> l2Reads = new ArrayList<>();
        for (ObjectManifestEntry rule : rules) {
            L2Content l2 = queryService.readContent(new ObjectContentRequest(rule.uri(), "raw_message_candidate", null, traceId,
                    request.callerId(), request.apiKey()));
            l2Reads.add(rule.uri());
            DependencyResult dependencies = queryService.dependencies(new DependenciesRequest(rule.uri(), Direction.forward, null, 1,
                    "raw_message_candidate", null, request.callerId(), request.apiKey()));
            List<String> dependencyUris = dependencies.edges().stream().map(DependencyEdge::toUri).distinct().toList();
            List<GroundingEvidence> evidence = List.of(new GroundingEvidence(rule.uri(), l2.contentHash(), "$"));
            claims.add(new GroundedClaim("Target candidate for " + rule.targetPath() + " is based on read L2 rule " + rule.uri(),
                    evidence, ValidationStatus.grounded, null));
            candidates.add(new TargetFieldCandidate(
                    rule.targetPath(),
                    "candidate requires applying governed rule logic to parsed source fields; RTS does not execute production transformation here",
                    rule.uri(),
                    dependencyUris,
                    "candidate",
                    evidence,
                    dependencyUris.isEmpty() ? List.of() : List.of("Lookup/helper inputs may still be incomplete; unresolved values remain unknown."),
                    List.of("Generated output is a grounded candidate, not a production transformation engine result.")));
        }
        List<String> unknowns = new ArrayList<>();
        if (candidates.isEmpty()) {
            unknowns.add("No released rule source anchor matched the parsed raw message fields.");
        }
        parsedFields.stream().filter(ParsedRawField::uncertain).forEach(field -> unknowns.add("Uncertain parsed source field: " + field.sourcePath()));
        queryService.appendTrace(java.time.Instant.now(), traceId, "raw_message_candidate", request.callerId(), "raw-message", null, scope, releaseId,
                rules.stream().map(ObjectManifestEntry::uri).toList(), candidates.stream().map(TargetFieldCandidate::ruleUri).toList(), l2Reads,
                RefusalReason.none, List.of("parse_raw_message_candidate", "map_source_fields_to_rules", "read_object_l2", "resolve_required_lookups", "assemble_target_message_candidate", "validate_target_message_grounding"));
        return new RawMessageCandidateResult("candidate", releaseId, scope, parsedFields, candidates, unknowns,
                List.of("Raw message output is a grounded candidate generation based on governed RTS truth and provided raw message."),
                new GroundingMap(claims), traceId);
    }

    public RuleCompareResult compareRules(RuleCompareRequest request) {
        ProjectionSnapshot snapshot = activeSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        ObjectManifestEntry left = requireObject(releaseId, request.leftUri());
        ObjectManifestEntry right = requireObject(releaseId, request.rightUri());
        permissionService.requireAllowed(releaseId, request.callerId(), request.apiKey(), left.scope(), "analysis_tools", mode(request.outputMode()));
        permissionService.requireAllowed(releaseId, request.callerId(), request.apiKey(), right.scope(), "analysis_tools", mode(request.outputMode()));
        String traceId = queryService.newTraceId();
        List<Fact> facts = new ArrayList<>();
        List<GroundedClaim> claims = new ArrayList<>();
        List<String> l2Reads = new ArrayList<>();
        if (request.readL2()) {
            for (ObjectManifestEntry object : List.of(left, right)) {
                L2Content l2 = queryService.readContent(new ObjectContentRequest(object.uri(), "compare_rules", null, traceId,
                        request.callerId(), request.apiKey()));
                facts.add(new Fact("Compared L2 object " + object.uri(), object.uri(), releaseId, "l2:" + l2.contentHash()));
                claims.add(new GroundedClaim("Compared object " + object.uri(), List.of(new GroundingEvidence(object.uri(), l2.contentHash(), "$")),
                        ValidationStatus.grounded, null));
                l2Reads.add(object.uri());
            }
        }
        List<String> inferences = List.of(
                "same_scope=" + left.scope().matches(right.scope()),
                "same_target_path=" + java.util.Objects.equals(left.targetPath(), right.targetPath()),
                "same_object_id=" + left.objectId().equals(right.objectId()));
        List<String> warnings = List.of("Comparison output is analysis; precedence or conflict decisions require governed adjudication.");
        queryService.appendTrace(Instant.now(), traceId, "compare_rules", request.callerId(), left.uri() + " vs " + right.uri(), null,
                left.scope(), releaseId, List.of(left.uri(), right.uri()), List.of(left.uri(), right.uri()), l2Reads,
                RefusalReason.none, List.of("get_object_card", request.readL2() ? "read_object_l2" : "cards_only", "compare_rules"));
        return new RuleCompareResult("analysis", releaseId, facts, inferences, List.of(), warnings, new GroundingMap(claims), traceId);
    }

    public ConflictExplanation explainConflict(ConflictExplainRequest request) {
        ProjectionSnapshot snapshot = activeSnapshot();
        String releaseId = snapshot.manifest().releaseId();
        ScopeKey scope = request.scope();
        ObjectManifestEntry object = null;
        if (request.uri() != null && !request.uri().isBlank()) {
            object = requireObject(releaseId, request.uri());
            scope = object.scope();
        }
        scope = requireScope(releaseId, scope);
        permissionService.requireAllowed(releaseId, request.callerId(), request.apiKey(), scope, "analysis_tools", mode(request.outputMode()));
        ObjectManifestEntry selected = object;
        List<ObjectManifestEntry> sameTarget = selected == null ? List.of() : snapshot.objectManifest().stream()
                .filter(candidate -> !candidate.uri().equals(selected.uri()))
                .filter(candidate -> java.util.Objects.equals(candidate.targetPath(), selected.targetPath()))
                .filter(candidate -> isAllowed(releaseId, request.callerId(), request.apiKey(), candidate.scope(), "analysis_tools", request.outputMode()))
                .toList();
        String traceId = queryService.newTraceId();
        List<String> facts = new ArrayList<>();
        facts.add("scope=" + scope.value());
        if (selected != null) {
            facts.add("object_uri=" + selected.uri());
            facts.add("target_path=" + selected.targetPath());
        }
        List<String> warnings = new ArrayList<>();
        if (sameTarget.isEmpty()) {
            warnings.add("No same-target released object conflict was detected in the active projection.");
        } else {
            warnings.add("Same-target objects exist and require explicit precedence/adjudication before publication use.");
        }
        if (snapshot.manifest().contentHashSummary() != null && snapshot.manifest().contentHashSummary().toLowerCase(Locale.ROOT).contains("draft")) {
            warnings.add("Release summary indicates draft material; conflict explanation is not signoff truth.");
        }
        queryService.appendTrace(Instant.now(), traceId, "explain_conflict", request.callerId(), request.uri(), null, scope, releaseId,
                selected == null ? List.of() : List.of(selected.uri()), sameTarget.stream().map(ObjectManifestEntry::uri).toList(), List.of(),
                RefusalReason.none, List.of("inspect_object_manifest", "inspect_scope_precedence", "explain_conflict"));
        return new ConflictExplanation(sameTarget.isEmpty() ? "no_open_conflict_detected" : "conflict_candidate", releaseId, scope,
                selected == null ? null : selected.uri(), facts,
                sameTarget.stream().map(candidate -> "same target candidate: " + candidate.uri()).toList(),
                List.of("Human adjudication is required for material conflict decisions."), warnings, traceId);
    }

    private String resolveSeedUri(ImpactAnalysisRequest request, String releaseId, ScopeKey scope) {
        if (request.changedUri() != null && !request.changedUri().isBlank()) {
            ObjectManifestEntry entry = projectionStore.getObject(releaseId, request.changedUri())
                    .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Changed object URI not found"));
            if (!entry.scope().matches(scope)) {
                throw new QueryRefusalException(RefusalReason.scope_unclear, "Changed object is outside requested scope");
            }
            return entry.uri();
        }
        List<String> anchors = new ArrayList<>();
        if (request.changedTargetPath() != null && !request.changedTargetPath().isBlank()) {
            anchors.add(request.changedTargetPath());
        }
        if (request.changedSourceAnchor() != null && !request.changedSourceAnchor().isBlank()) {
            anchors.add(request.changedSourceAnchor());
        }
        if (anchors.isEmpty()) {
            throw new QueryRefusalException(RefusalReason.object_not_found, "Impact analysis requires changedUri, changedTargetPath, or changedSourceAnchor");
        }
        var candidates = queryService.find(new FindRequest(String.join(" ", anchors), scope, List.of(), anchors, 5, request.callerId(), request.apiKey(), request.outputMode()));
        if (candidates.isEmpty()) {
            throw new QueryRefusalException(RefusalReason.object_not_found, "No released changed object candidate found");
        }
        return candidates.get(0).uri();
    }

    private ImpactCandidate impactCandidate(String seedUri, ObjectManifestEntry object, List<DependencyEdge> edges) {
        List<String> path = edges.stream()
                .filter(edge -> edge.fromUri().equals(object.uri()) || edge.toUri().equals(object.uri()))
                .map(edge -> edge.fromUri() + " -> " + edge.toUri())
                .distinct()
                .toList();
        return new ImpactCandidate(object.uri(), object.targetPath(), path, 0.8,
                "Released reverse dependency path from changed object " + seedUri + " reaches this object.");
    }

    private List<ObjectManifestEntry> seedObjects(ProjectionSnapshot snapshot, String releaseId, ScopeKey scope, String seedUri, int maxObjects) {
        if (seedUri != null && !seedUri.isBlank()) {
            ObjectManifestEntry object = projectionStore.getObject(releaseId, seedUri)
                    .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Seed object not found"));
            if (!object.scope().matches(scope)) {
                throw new QueryRefusalException(RefusalReason.scope_unclear, "Seed object is outside requested scope");
            }
            return List.of(object);
        }
        return snapshot.objectManifest().stream()
                .filter(object -> object.scope().matches(scope))
                .filter(object -> object.objectType() == ObjectType.rule)
                .sorted(Comparator.comparing(ObjectManifestEntry::objectId))
                .limit(maxObjects)
                .toList();
    }

    private ObjectManifestEntry requireObject(String releaseId, String uri) {
        return projectionStore.getObject(releaseId, uri)
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.object_not_found, "Object not found"));
    }

    private List<ParsedRawField> parseRawFields(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return List.of();
        }
        Map<String, ParsedRawField> fields = new LinkedHashMap<>();
        for (String line : rawMessage.split("\\R")) {
            String text = line.strip();
            if (text.isBlank()) {
                continue;
            }
            int separator = separatorIndex(text);
            if (separator < 1) {
                fields.putIfAbsent(text, new ParsedRawField(text, null, true, "No key/value separator was detected"));
                continue;
            }
            String key = text.substring(0, separator).strip();
            String value = text.substring(separator + 1).strip();
            if (!key.isBlank()) {
                fields.putIfAbsent(key, new ParsedRawField(key, value, value.isBlank(), value.isBlank() ? "Value is blank" : null));
            }
        }
        return List.copyOf(fields.values());
    }

    private int separatorIndex(String text) {
        int colon = text.indexOf(':');
        int equals = text.indexOf('=');
        if (colon < 0) {
            return equals;
        }
        if (equals < 0) {
            return colon;
        }
        return Math.min(colon, equals);
    }

    private boolean sourceMatches(String anchor, String sourcePath) {
        String normalizedAnchor = normalizeSource(anchor);
        String normalizedSource = normalizeSource(sourcePath);
        return normalizedAnchor.equals(normalizedSource)
                || normalizedAnchor.endsWith("/" + normalizedSource)
                || normalizedSource.endsWith("/" + normalizedAnchor)
                || normalizedAnchor.endsWith("." + normalizedSource)
                || normalizedSource.endsWith("." + normalizedAnchor);
    }

    private String normalizeSource(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("/text()", "")
                .replaceAll("^[/$]+", "")
                .replaceAll("[^a-z0-9_.\\-/]+", "")
                .strip();
    }

    private ScopeKey requireScope(String releaseId, ScopeKey scope) {
        if (scope == null) {
            throw new QueryRefusalException(RefusalReason.scope_unclear, "Scope is required");
        }
        return scopeRegistry.resolve(releaseId, scope)
                .map(ignored -> scope)
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.scope_unclear, "Scope is not active in release"));
    }

    private ProjectionSnapshot activeSnapshot() {
        try {
            return projectionStore.loadActiveSnapshot();
        } catch (RuntimeException ex) {
            throw new QueryRefusalException(RefusalReason.active_release_missing, "Active release is unavailable");
        }
    }

    private int bounded(Integer value, int fallback) {
        int safe = value == null ? fallback : value;
        return Math.max(1, Math.min(safe, fallback));
    }

    private String mode(String outputMode) {
        return outputMode == null || outputMode.isBlank() ? "default" : outputMode;
    }

    private boolean isAllowed(String releaseId, String callerId, String apiKey, ScopeKey scope, String entrypoint, String outputMode) {
        try {
            permissionService.requireAllowed(releaseId, callerId, apiKey, scope, entrypoint, mode(outputMode));
            return true;
        } catch (QueryRefusalException ex) {
            return false;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
