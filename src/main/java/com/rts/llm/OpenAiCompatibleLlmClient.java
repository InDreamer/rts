package com.rts.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rts.config.RtsProperties;
import com.rts.llm.LlmContracts.LlmClient;
import com.rts.llm.LlmContracts.LlmDraft;
import com.rts.llm.LlmContracts.ToolContext;
import com.rts.model.CoreModels.CandidateObject;
import com.rts.model.CoreModels.DependencyResult;
import com.rts.model.CoreModels.Direction;
import com.rts.model.CoreModels.Fact;
import com.rts.model.CoreModels.L2Content;
import com.rts.model.CoreModels.QueryPlan;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.query.QueryRequests.AskRequest;
import com.rts.query.QueryRequests.DependenciesRequest;
import com.rts.query.QueryRequests.FindRequest;
import com.rts.query.QueryRequests.ObjectContentRequest;
import com.rts.query.QueryRequests.ObjectGetRequest;
import com.rts.query.QueryRequests.PlanRequest;
import com.rts.query.QueryRefusalException;
import com.rts.query.QueryService.ObjectEnvelope;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "rts", name = "llm-enabled", havingValue = "true")
public class OpenAiCompatibleLlmClient implements LlmClient {
    private final RtsProperties properties;
    private final ObjectMapper mapper;
    private final RestClient restClient;

    public OpenAiCompatibleLlmClient(RtsProperties properties, ObjectMapper mapper, RestClient.Builder builder) {
        this.properties = properties;
        this.mapper = mapper;
        this.restClient = builder.baseUrl(properties.getLlmBaseUrl()).build();
    }

    @Override
    public LlmDraft draftAnswer(AskRequest request, ToolContext toolContext) {
        GroundedContext context = loadGroundedContext(request, toolContext);
        callChatCompletion(request.query(), context);
        ServiceAnswer base = context.answer();
        ServiceAnswer shaped = new ServiceAnswer(
                base.answerType(),
                base.scope(),
                base.releaseId(),
                base.facts(),
                base.inferences(),
                base.unknowns(),
                base.candidateSuggestions(),
                base.humanDecisions(),
                base.citedObjects(),
                base.dependencies(),
                base.traceId(),
                base.refusal(),
                base.warnings(),
                base.answer());
        return new LlmDraft("OpenAI-compatible adapter shaped a grounded answer.",
                List.of("resolve_scope", "find_objects", "get_object_card", "read_object_l2", "get_dependencies", "chat_completions"),
                shaped);
    }

    private GroundedContext loadGroundedContext(AskRequest request, ToolContext toolContext) {
        QueryPlan plan = (QueryPlan) toolContext.call("resolve_scope",
                new PlanRequest(request.query(), request.callerId(), request.scopeHint(), request.outputMode(), true)).output();
        if (plan.needsClarification()) {
            throw new QueryRefusalException(com.rts.model.CoreModels.RefusalReason.scope_unclear, plan.clarificationQuestion());
        }
        @SuppressWarnings("unchecked")
        List<CandidateObject> candidates = (List<CandidateObject>) toolContext.call("find_objects",
                new FindRequest(request.query(), plan.scope(), List.of(), plan.anchors(), 5, request.callerId(), request.apiKey(), request.outputMode())).output();
        if (candidates.isEmpty()) {
            throw new QueryRefusalException(com.rts.model.CoreModels.RefusalReason.object_not_found, "No released structured object matched the query");
        }
        CandidateObject selected = candidates.get(0);
        ObjectEnvelope object = (ObjectEnvelope) toolContext.call("get_object_card",
                new ObjectGetRequest(selected.uri(), null, null, request.callerId(), request.apiKey())).output();
        L2Content l2 = (L2Content) toolContext.call("read_object_l2",
                new ObjectContentRequest(selected.uri(), "answer", null, null, request.callerId(), request.apiKey())).output();
        DependencyResult dependencies = (DependencyResult) toolContext.call("get_dependencies",
                new DependenciesRequest(selected.uri(), Direction.forward, null, 1, "answer", null, request.callerId(), request.apiKey())).output();
        Fact fact = new Fact(l2.content(), selected.uri(), l2.releaseId(), "l2");
        ServiceAnswer answer = new ServiceAnswer(
                com.rts.model.CoreModels.AnswerType.answer,
                plan.scope(),
                l2.releaseId(),
                List.of(fact),
                List.of("Object card loaded for " + object.objectManifest().objectId()),
                List.of(),
                List.of(),
                List.of(),
                List.of(selected.uri()),
                dependencies.edges(),
                "trace-llm-grounded",
                null,
                List.of(),
                l2.content());
        return new GroundedContext(answer, l2);
    }

    private String callChatCompletion(String query, GroundedContext context) {
        if (properties.getLlmApiKey() == null || properties.getLlmApiKey().isBlank()) {
            throw new IllegalStateException("RTS_LLM_API_KEY is required when rts.llm-enabled=true");
        }
        Map<String, Object> body = Map.of(
                "model", properties.getLlmModel(),
                "max_tokens", properties.getLlmMaxTokens(),
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are an RTS answer organizer, not a truth owner. Rewrite only the provided grounded facts. Do not add any business claim that is not present verbatim in the facts. Include cited object URIs and the trace id placeholder exactly as provided."),
                        Map.of("role", "user", "content", query),
                        Map.of("role", "tool", "content", String.valueOf(context.answer()))));
        String response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.getLlmApiKey())
                .body(body)
                .retrieve()
                .body(String.class);
        try {
            JsonNode root = mapper.readTree(response);
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid OpenAI-compatible response", ex);
        }
    }

    private record GroundedContext(ServiceAnswer answer, L2Content l2) {}
}
