package com.rts.query;

import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ServiceAnswer;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class FinalAnswerValidator {
    private final PromptPolicyGuard promptPolicyGuard;

    public FinalAnswerValidator(PromptPolicyGuard promptPolicyGuard) {
        this.promptPolicyGuard = promptPolicyGuard;
    }

    public void validate(ServiceAnswer answer, Set<String> l2ReadUris) {
        if (answer.traceId() == null || answer.traceId().isBlank()) {
            throw new QueryRefusalException(RefusalReason.unsupported_claim, "Final answer has no trace id");
        }
        Set<String> cited = new HashSet<>(answer.citedObjects() == null ? Set.of() : answer.citedObjects());
        if (answer.facts() != null) {
            answer.facts().forEach(fact -> {
                if (!cited.contains(fact.uri()) || !l2ReadUris.contains(fact.uri())) {
                    throw new QueryRefusalException(RefusalReason.unsupported_claim, "Fact is not backed by an L2-read cited object");
                }
            });
        }
        if (answer.answer() != null && answer.answer().contains("raw review")) {
            throw new QueryRefusalException(RefusalReason.governance_unauthorized, "Forbidden governance claim");
        }
        promptPolicyGuard.validateGeneratedAnswer(answer.answer());
    }
}
