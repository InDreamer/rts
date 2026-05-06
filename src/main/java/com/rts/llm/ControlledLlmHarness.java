package com.rts.llm;

import com.rts.config.RtsProperties;
import com.rts.llm.LlmContracts.LlmClient;
import com.rts.llm.LlmContracts.LlmDraft;
import com.rts.llm.LlmContracts.ToolContext;
import com.rts.llm.LlmContracts.ToolResult;
import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.DependencyEdge;
import com.rts.model.CoreModels.Fact;
import com.rts.model.CoreModels.LlmRunTrace;
import com.rts.model.CoreModels.Refusal;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.query.FinalAnswerValidator;
import com.rts.query.QueryRefusalException;
import com.rts.query.QueryRequests.AskRequest;
import com.rts.query.QueryRequests.FindRequest;
import com.rts.query.QueryRequests.ObjectContentRequest;
import com.rts.query.QueryRequests.ObjectGetRequest;
import com.rts.query.QueryRequests.PlanRequest;
import com.rts.query.QueryService;
import com.rts.store.Hashing;
import com.rts.store.StoreContracts.TraceStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ControlledLlmHarness {
    private final QueryService queryService;
    private final LlmClient llmClient;
    private final FinalAnswerValidator finalAnswerValidator;
    private final TraceStore traceStore;
    private final RtsProperties properties;

    public ControlledLlmHarness(QueryService queryService, LlmClient llmClient, FinalAnswerValidator finalAnswerValidator,
            TraceStore traceStore, RtsProperties properties) {
        this.queryService = queryService;
        this.llmClient = llmClient;
        this.finalAnswerValidator = finalAnswerValidator;
        this.traceStore = traceStore;
        this.properties = properties;
    }

    public ServiceAnswer ask(AskRequest request) {
        Instant start = Instant.now();
        GuardedToolContext tools = new GuardedToolContext(request.maxToolCalls() == null ? properties.getMaxToolCalls() : request.maxToolCalls());
        String traceId = queryService.newTraceId();
        LlmDraft draft;
        try {
            draft = llmClient.draftAnswer(request, tools);
        } catch (QueryRefusalException ex) {
            queryService.appendTrace(start, traceId, "ask", request.callerId(), request.query(), null, request.scopeHint(), null,
                    List.of(), List.of(), List.of(), ex.reason(), tools.calls);
            ServiceAnswer refusal = new ServiceAnswer(AnswerType.refusal, request.scopeHint(), null, List.of(), List.of(), List.of(ex.getMessage()),
                    List.of(), List.of(), List.of(), List.of(), traceId, new Refusal(ex.reason(), ex.getMessage(), List.of(), false), List.of(), null);
            traceStore.appendLlmRunTrace(new LlmRunTrace("llm-" + UUID.randomUUID(), traceId, properties.getLlmModel(), "day1-controlled-v1",
                    List.copyOf(tools.calls), Hashing.sha256("refusal"), Hashing.sha256(ex.getMessage()), "invalid:" + ex.reason(),
                    Duration.between(start, Instant.now()).toMillis(), Instant.now()));
            return refusal;
        }
        ServiceAnswer grounded = withTraceId(draft.groundedAnswer(), traceId);
        try {
            if (grounded.answerType() == AnswerType.answer) {
                finalAnswerValidator.validate(grounded, Set.copyOf(tools.l2ReadUris));
            }
            queryService.appendTrace(start, traceId, "ask", request.callerId(), request.query(), null, grounded.scope(), grounded.releaseId(),
                    grounded.citedObjects(), grounded.citedObjects(), tools.l2ReadUris, grounded.refusal() == null ? RefusalReason.none : grounded.refusal().reason(),
                    draft.toolCalls());
            appendLlmTrace(start, grounded.traceId(), draft, "valid");
            return new ServiceAnswer(
                    grounded.answerType(),
                    grounded.scope(),
                    grounded.releaseId(),
                    grounded.facts(),
                    grounded.inferences(),
                    grounded.unknowns(),
                    grounded.candidateSuggestions(),
                    grounded.humanDecisions(),
                    grounded.citedObjects(),
                    grounded.dependencies(),
                    grounded.traceId(),
                    grounded.refusal(),
                    grounded.warnings(),
                    grounded.answer() == null ? draft.text() : grounded.answer());
        } catch (QueryRefusalException ex) {
            queryService.appendTrace(start, traceId, "ask", request.callerId(), request.query(), null, grounded.scope(), grounded.releaseId(),
                    List.of(), List.of(), List.of(), ex.reason(), draft.toolCalls());
            appendLlmTrace(start, grounded.traceId(), draft, "invalid:" + ex.reason());
            return new ServiceAnswer(AnswerType.refusal, grounded.scope(), grounded.releaseId(), List.of(), List.of(),
                    List.of(ex.getMessage()), List.of(), List.of(), List.of(), List.of(), grounded.traceId(),
                    new Refusal(ex.reason(), ex.getMessage(), List.of(), false), List.of(), null);
        }
    }

    private ServiceAnswer withTraceId(ServiceAnswer answer, String traceId) {
        return new ServiceAnswer(answer.answerType(), answer.scope(), answer.releaseId(), safeFacts(answer.facts()), safeStrings(answer.inferences()),
                safeStrings(answer.unknowns()), safeStrings(answer.candidateSuggestions()), safeStrings(answer.humanDecisions()),
                safeStrings(answer.citedObjects()), safeEdges(answer.dependencies()), traceId, answer.refusal(), safeStrings(answer.warnings()),
                answer.answer() == null ? null : answer.answer().replace(answer.traceId(), traceId));
    }

    private List<Fact> safeFacts(List<Fact> values) {
        return values == null ? List.of() : values;
    }

    private List<String> safeStrings(List<String> values) {
        return values == null ? List.of() : values;
    }

    private List<DependencyEdge> safeEdges(List<DependencyEdge> values) {
        return values == null ? List.of() : values;
    }

    private void appendLlmTrace(Instant start, String traceId, LlmDraft draft, String validation) {
        traceStore.appendLlmRunTrace(new LlmRunTrace(
                "llm-" + UUID.randomUUID(),
                traceId,
                properties.getLlmModel(),
                "day1-controlled-v1",
                draft.toolCalls(),
                Hashing.sha256(String.valueOf(draft.groundedAnswer())),
                Hashing.sha256(String.valueOf(draft.text())),
                validation,
                Duration.between(start, Instant.now()).toMillis(),
                Instant.now()));
    }

    private final class GuardedToolContext implements ToolContext {
        private final int maxCalls;
        private final List<String> calls = new ArrayList<>();
        private final List<String> l2ReadUris = new ArrayList<>();

        private GuardedToolContext(int maxCalls) {
            this.maxCalls = maxCalls;
        }

        @Override
        public ToolResult call(String toolName, Object input) {
            if (calls.size() >= maxCalls) {
                throw new QueryRefusalException(RefusalReason.tool_budget_exhausted, "LLM tool budget exhausted");
            }
            calls.add(toolName);
            Object output = switch (toolName) {
                case "resolve_scope" -> queryService.plan((PlanRequest) input);
                case "find_objects" -> queryService.find((FindRequest) input);
                case "get_object_card" -> queryService.getObject((ObjectGetRequest) input);
                case "read_object_l2" -> queryService.readContent((ObjectContentRequest) input);
                case "get_dependencies" -> queryService.dependencies((com.rts.query.QueryRequests.DependenciesRequest) input);
                default -> throw new QueryRefusalException(RefusalReason.unsupported_claim, "Unknown controlled tool: " + toolName);
            };
            if ("read_object_l2".equals(toolName) && output instanceof com.rts.model.CoreModels.L2Content l2Content) {
                l2ReadUris.add(l2Content.uri());
            }
            return new ToolResult(toolName, output);
        }
    }
}
