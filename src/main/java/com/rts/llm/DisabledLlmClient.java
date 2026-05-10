package com.rts.llm;

import com.rts.llm.LlmContracts.LlmClient;
import com.rts.llm.LlmContracts.LlmDraft;
import com.rts.llm.LlmContracts.ToolContext;
import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.CandidateObject;
import com.rts.model.CoreModels.DependencyResult;
import com.rts.model.CoreModels.Direction;
import com.rts.model.CoreModels.Fact;
import com.rts.model.CoreModels.L2Content;
import com.rts.model.CoreModels.QueryPlan;
import com.rts.model.CoreModels.Refusal;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.query.QueryRequests.DependenciesRequest;
import com.rts.query.QueryRequests.FindRequest;
import com.rts.query.QueryRequests.ObjectContentRequest;
import com.rts.query.QueryRequests.ObjectGetRequest;
import com.rts.query.QueryRequests.PlanRequest;
import com.rts.query.QueryRequests.AskRequest;
import com.rts.query.QueryService.ObjectEnvelope;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "rts", name = "llm-enabled", havingValue = "false", matchIfMissing = true)
public class DisabledLlmClient implements LlmClient {
    @Override
    public LlmDraft draftAnswer(AskRequest request, ToolContext toolContext) {
        QueryPlan plan = planned(toolContext, "resolve_scope", QueryPlan.class);
        if (plan == null) {
            plan = (QueryPlan) toolContext.call("resolve_scope",
                    new PlanRequest(request.query(), request.callerId(), request.scopeHint(), request.outputMode(), true)).output();
        }
        if (plan.needsClarification()) {
            ServiceAnswer refusal = new ServiceAnswer(AnswerType.refusal, request.scopeHint(), null, List.of(), List.of(),
                    List.of(plan.clarificationQuestion()), List.of(), List.of(), List.of(), List.of(), "trace-llm-refusal",
                    new Refusal(RefusalReason.scope_unclear, plan.clarificationQuestion(), List.of(), false), List.of(), null);
            return new LlmDraft("Scope clarification required.", List.of("resolve_scope"), refusal);
        }
        @SuppressWarnings("unchecked")
        List<CandidateObject> candidates = (List<CandidateObject>) toolContext.plannedOutputs().get("find_objects");
        if (candidates == null) {
            candidates = (List<CandidateObject>) toolContext.call("find_objects",
                    new FindRequest(request.query(), plan.scope(), objectTypesForIntent(plan.intent()), plan.anchors(), 5, request.callerId(), request.apiKey(), request.outputMode())).output();
        }
        if (candidates.isEmpty()) {
            ServiceAnswer refusal = new ServiceAnswer(AnswerType.refusal, plan.scope(), null, List.of(), List.of(),
                    List.of("No released structured object matched the query"), List.of(), List.of(), List.of(), List.of(), "trace-llm-refusal",
                    new Refusal(RefusalReason.object_not_found, "No released structured object matched the query", List.of(), false), List.of(), null);
            return new LlmDraft("No grounded object found.", List.of("resolve_scope", "find_objects"), refusal);
        }
        CandidateObject selected = candidates.get(0);
        ObjectEnvelope object = planned(toolContext, "get_object_card", ObjectEnvelope.class);
        if (object == null) {
            object = (ObjectEnvelope) toolContext.call("get_object_card",
                    new ObjectGetRequest(selected.uri(), null, null, request.callerId(), request.apiKey())).output();
        }
        L2Content l2 = planned(toolContext, "read_object_l2", L2Content.class);
        if (l2 == null) {
            l2 = (L2Content) toolContext.call("read_object_l2",
                    new ObjectContentRequest(selected.uri(), "answer", null, null, request.callerId(), request.apiKey())).output();
        }
        DependencyResult dependencies = planned(toolContext, "get_dependencies", DependencyResult.class);
        if (dependencies == null) {
            dependencies = (DependencyResult) toolContext.call("get_dependencies",
                    new DependenciesRequest(selected.uri(), Direction.forward, null, 1, "answer", null, request.callerId(), request.apiKey())).output();
        }
        String factText = l2.content();
        Fact fact = new Fact(factText, selected.uri(), l2.releaseId(), "l2:" + l2.contentHash());
        ServiceAnswer answer = new ServiceAnswer(AnswerType.answer, plan.scope(), l2.releaseId(), List.of(fact),
                List.of("Object card loaded for " + object.objectManifest().objectId()), List.of(), List.of(), List.of(),
                List.of(selected.uri()), dependencies.edges(), "trace-llm-grounded", null, warningsFor(object),
                factText + " (trace: trace-llm-grounded)");
        return new LlmDraft("LLM disabled; controlled tools produced grounded answer.",
                List.of("resolve_scope", "find_objects", "get_object_card", "read_object_l2", "get_dependencies"), answer);
    }

    private List<String> objectTypesForIntent(String intent) {
        if ("lookup_lookup".equals(intent)) {
            return List.of("lookup");
        }
        if ("helper_lookup".equals(intent)) {
            return List.of("helper");
        }
        if ("rule_lookup".equals(intent) || "explain_rule".equals(intent) || "generate_target_message".equals(intent)) {
            return List.of("rule");
        }
        return List.of();
    }

    private List<String> warningsFor(ObjectEnvelope object) {
        List<String> warnings = new java.util.ArrayList<>();
        if (object.objectCard().riskFlags() != null && !object.objectCard().riskFlags().isEmpty()) {
            warnings.add("Object risk flags: " + object.objectCard().riskFlags());
        }
        if (object.objectCard().cardJson() != null && object.objectCard().cardJson().containsKey("status")) {
            warnings.add("Object governance status: " + object.objectCard().cardJson().get("status"));
        }
        return List.copyOf(warnings);
    }

    private <T> T planned(ToolContext toolContext, String toolName, Class<T> type) {
        Object value = toolContext.plannedOutputs().get(toolName);
        return type.isInstance(value) ? type.cast(value) : null;
    }
}
