package com.rts.llm;

import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.query.QueryRequests.AskRequest;
import java.util.List;
import java.util.Map;

public final class LlmContracts {
    private LlmContracts() {}

    public interface LlmClient {
        LlmDraft draftAnswer(AskRequest request, ToolContext toolContext);
    }

    public interface ToolContext {
        ToolResult call(String toolName, Object input);

        default Map<String, Object> plannedOutputs() {
            return Map.of();
        }
    }

    public record ToolResult(String toolName, Object output) {}

    public record LlmDraft(String text, List<String> toolCalls, ServiceAnswer groundedAnswer) {}
}
