package com.rts.llm;

import com.rts.model.AgentServiceModels.GroundedClaim;
import com.rts.model.AgentServiceModels.GroundingEvidence;
import com.rts.model.AgentServiceModels.ValidationStatus;
import com.rts.model.CoreModels.Fact;
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

    public record LlmDraft(
            String text,
            List<String> toolCalls,
            ServiceAnswer groundedAnswer,
            List<GroundedClaim> claims,
            List<String> inferences,
            List<String> unknowns,
            List<String> candidates,
            List<String> warnings,
            List<String> citationIntents,
            List<String> toolNeeds
    ) {
        public LlmDraft(String text, List<String> toolCalls, ServiceAnswer groundedAnswer) {
            this(text, toolCalls, groundedAnswer, claimsFrom(groundedAnswer),
                    groundedAnswer == null || groundedAnswer.inferences() == null ? List.of() : groundedAnswer.inferences(),
                    groundedAnswer == null || groundedAnswer.unknowns() == null ? List.of() : groundedAnswer.unknowns(),
                    groundedAnswer == null || groundedAnswer.candidateSuggestions() == null ? List.of() : groundedAnswer.candidateSuggestions(),
                    groundedAnswer == null || groundedAnswer.warnings() == null ? List.of() : groundedAnswer.warnings(),
                    groundedAnswer == null || groundedAnswer.citedObjects() == null ? List.of() : groundedAnswer.citedObjects(),
                    List.of());
        }

        private static List<GroundedClaim> claimsFrom(ServiceAnswer answer) {
            if (answer == null || answer.facts() == null || answer.facts().isEmpty()) {
                return List.of();
            }
            return answer.facts().stream()
                    .map(fact -> new GroundedClaim(fact.text(),
                            List.of(new GroundingEvidence(fact.uri(), hashFrom(fact), "$")),
                            ValidationStatus.grounded,
                            null))
                    .toList();
        }

        private static String hashFrom(Fact fact) {
            return fact.source() != null && fact.source().startsWith("l2:") ? fact.source().substring(3) : null;
        }
    }
}
