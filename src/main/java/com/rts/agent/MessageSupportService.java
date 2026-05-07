package com.rts.agent;

import com.rts.model.AgentServiceModels.RawMessageCandidateRequest;
import com.rts.model.AgentServiceModels.RawMessageCandidateResult;
import com.rts.model.AgentServiceModels.RequiredLookupResolution;
import com.rts.model.AgentServiceModels.SourceFieldMappingResult;
import com.rts.model.AgentServiceModels.TargetMessageGroundingValidation;
import com.rts.model.CoreModels.DependencyEdge;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.model.CoreModels.ObjectType;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.query.QueryRefusalException;
import com.rts.store.StoreContracts.ProjectionStore;
import com.rts.store.StoreContracts.ScopeRegistry;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class MessageSupportService {
    private final AgentAnalysisService analysisService;
    private final ProjectionStore projectionStore;
    private final ScopeRegistry scopeRegistry;

    public MessageSupportService(AgentAnalysisService analysisService, ProjectionStore projectionStore, ScopeRegistry scopeRegistry) {
        this.analysisService = analysisService;
        this.projectionStore = projectionStore;
        this.scopeRegistry = scopeRegistry;
    }

    public RawMessageCandidateResult parseRawMessageCandidate(RawMessageCandidateRequest request) {
        return analysisService.generateRawMessageCandidate(request);
    }

    public SourceFieldMappingResult mapSourceFieldsToRules(RawMessageCandidateRequest request) {
        RawMessageCandidateResult result = analysisService.generateRawMessageCandidate(request);
        List<String> matchedRules = result.targetCandidates().stream()
                .map(candidate -> candidate.ruleUri())
                .distinct()
                .toList();
        var releasedRules = releasedRules(result.releaseId(), result.scope());
        List<String> unknownSources = result.parsedFields().stream()
                .filter(field -> releasedRules.stream().noneMatch(rule -> sourceMatchesRule(rule, field.sourcePath())))
                .map(field -> field.sourcePath())
                .distinct()
                .toList();
        return new SourceFieldMappingResult(result.releaseId(), result.scope(), result.parsedFields(), matchedRules, unknownSources, result.traceId());
    }

    public List<RequiredLookupResolution> resolveRequiredLookups(RawMessageCandidateRequest request) {
        RawMessageCandidateResult result = analysisService.generateRawMessageCandidate(request);
        return result.targetCandidates().stream()
                .map(candidate -> new RequiredLookupResolution(candidate.ruleUri(),
                        candidate.dependencyUris().stream().filter(uri -> uri.contains("/lookups/")).toList(),
                        candidate.dependencyUris().stream().filter(uri -> uri.contains("/helpers/")).toList(),
                        candidate.unknowns()))
                .toList();
    }

    public RawMessageCandidateResult simulateRuleApplication(RawMessageCandidateRequest request) {
        return analysisService.generateRawMessageCandidate(request);
    }

    public RawMessageCandidateResult assembleTargetMessageCandidate(RawMessageCandidateRequest request) {
        return analysisService.generateRawMessageCandidate(request);
    }

    public TargetMessageGroundingValidation validateTargetMessageGrounding(RawMessageCandidateRequest request) {
        RawMessageCandidateResult result = analysisService.generateRawMessageCandidate(request);
        List<String> groundedRules = result.targetCandidates().stream()
                .filter(candidate -> !candidate.groundedBy().isEmpty())
                .map(candidate -> candidate.ruleUri())
                .distinct()
                .toList();
        List<String> unsupportedTargets = new java.util.ArrayList<>(result.targetCandidates().stream()
                .filter(candidate -> candidate.groundedBy().isEmpty())
                .map(candidate -> candidate.targetPath())
                .distinct()
                .toList());
        if (result.targetCandidates().isEmpty()) {
            unsupportedTargets.add("no_target_candidate");
        }
        return new TargetMessageGroundingValidation(unsupportedTargets.isEmpty() ? "grounded_candidate" : "partial",
                groundedRules, unsupportedTargets, result.groundingMap(),
                List.of("Validation covers RTS grounding only; it is not a production transformation engine."));
    }

    private List<ObjectManifestEntry> releasedRules(String releaseId, com.rts.model.CoreModels.ScopeKey scope) {
        if (scope == null || scopeRegistry.resolve(releaseId, scope).isEmpty()) {
            throw new QueryRefusalException(RefusalReason.scope_unclear, "Scope is not active in release");
        }
        return projectionStore.allObjects(releaseId).stream()
                .filter(object -> object.scope().matches(scope))
                .filter(object -> object.objectType() == ObjectType.rule)
                .toList();
    }

    private boolean sourceMatchesRule(ObjectManifestEntry rule, String sourcePath) {
        if (rule.sourceAnchors() == null || rule.sourceAnchors().isEmpty()) {
            return false;
        }
        return rule.sourceAnchors().stream().anyMatch(anchor -> sourceMatches(anchor, sourcePath));
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
}
