package com.rts.agent;

import com.rts.model.AgentServiceModels.AnswerViewResult;
import com.rts.model.AgentServiceModels.GroundedClaim;
import com.rts.model.AgentServiceModels.GroundingEvidence;
import com.rts.model.AgentServiceModels.GroundingMap;
import com.rts.model.AgentServiceModels.ValidationStatus;
import com.rts.model.CoreModels.DependencyEdge;
import com.rts.model.CoreModels.Fact;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.query.PermissionService;
import com.rts.query.QueryRefusalException;
import com.rts.store.StoreContracts.ProjectionStore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AnswerViewService {
    private final ProjectionStore projectionStore;
    private final PermissionService permissionService;

    public AnswerViewService(ProjectionStore projectionStore, PermissionService permissionService) {
        this.projectionStore = projectionStore;
        this.permissionService = permissionService;
    }

    public AnswerViewResult shape(ServiceAnswer answer, String responseView, String callerId, String apiKey) {
        if (answer == null) {
            throw new QueryRefusalException(RefusalReason.object_not_found, "Answer is required");
        }
        String view = responseView == null || responseView.isBlank() ? "human" : responseView;
        if (answer.scope() != null && answer.releaseId() != null) {
            permissionService.requireAllowed(answer.releaseId(), callerId, apiKey, answer.scope(), "view_tools", view);
        }
        GroundingMap groundingMap = groundingMap(answer);
        String status = answer.answerType().name();
        String conclusion = answer.answer() == null && answer.refusal() != null ? answer.refusal().whatIsMissing() : answer.answer();
        List<String> dependencies = answer.dependencies() == null ? List.of() : answer.dependencies().stream()
                .map(edge -> edge.edgeType() + ": " + edge.fromUri() + " -> " + edge.toUri())
                .toList();
        Map<String, Object> audit = new LinkedHashMap<>();
        Map<String, Object> pipeline = new LinkedHashMap<>();
        if ("audit".equals(view) || "debug".equals(view)) {
            audit.put("release_id", answer.releaseId());
            audit.put("scope", answer.scope());
            audit.put("cited_objects", answer.citedObjects());
            audit.put("grounding_map", groundingMap);
            audit.put("refusal", answer.refusal());
        }
        if ("pipeline".equals(view) || "debug".equals(view)) {
            pipeline.put("status", status);
            pipeline.put("trace_id", answer.traceId());
            pipeline.put("refusal_reason", answer.refusal() == null ? RefusalReason.none : answer.refusal().reason());
            pipeline.put("cited_object_count", answer.citedObjects() == null ? 0 : answer.citedObjects().size());
        }
        List<String> nextActions = answer.refusal() == null
                ? List.of("Use trace_id for audit or regression if needed.")
                : answer.refusal().whatUserCanProvide();
        return new AnswerViewResult(status, view, conclusion, answer.facts(), dependencies, answer.warnings(), groundingMap,
                answer.traceId(), audit, pipeline, nextActions);
    }

    private GroundingMap groundingMap(ServiceAnswer answer) {
        if (answer.facts() == null || answer.facts().isEmpty()) {
            return GroundingMap.empty();
        }
        List<GroundedClaim> claims = answer.facts().stream()
                .map(fact -> new GroundedClaim(
                        fact.text(),
                        List.of(new GroundingEvidence(fact.uri(), hashFor(answer.releaseId(), fact), "$")),
                        ValidationStatus.grounded,
                        null))
                .toList();
        return new GroundingMap(claims);
    }

    private String hashFor(String releaseId, Fact fact) {
        if (fact.source() != null && fact.source().startsWith("l2:")) {
            return fact.source().substring(3);
        }
        if (releaseId == null) {
            return null;
        }
        return projectionStore.getContentRef(releaseId, fact.uri()).map(ref -> ref.contentHash()).orElse(null);
    }
}
