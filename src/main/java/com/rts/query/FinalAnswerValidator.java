package com.rts.query;

import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.model.AgentServiceModels.GroundedClaim;
import com.rts.model.AgentServiceModels.GroundingEvidence;
import com.rts.model.AgentServiceModels.GroundingMap;
import com.rts.model.AgentServiceModels.ValidationStatus;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class FinalAnswerValidator {
    private final PromptPolicyGuard promptPolicyGuard;

    public FinalAnswerValidator(PromptPolicyGuard promptPolicyGuard) {
        this.promptPolicyGuard = promptPolicyGuard;
    }

    public void validate(ServiceAnswer answer, Set<String> l2ReadUris) {
        validateClaims(answer, l2ReadUris);
    }

    public GroundingMap validateClaims(ServiceAnswer answer, Set<String> l2ReadUris) {
        Map<String, String> l2Hashes = l2ReadUris == null ? Map.of() : l2ReadUris.stream().collect(Collectors.toMap(uri -> uri, uri -> "", (a, b) -> a));
        return validateClaims(answer, l2Hashes);
    }

    public void validate(ServiceAnswer answer, Map<String, String> l2ReadHashes) {
        validateClaims(answer, l2ReadHashes);
    }

    public GroundingMap validateClaims(ServiceAnswer answer, Map<String, String> l2ReadHashes) {
        if (answer.traceId() == null || answer.traceId().isBlank()) {
            throw new QueryRefusalException(RefusalReason.unsupported_claim, "Final answer has no trace id");
        }
        if (answer.answerType() == com.rts.model.CoreModels.AnswerType.answer) {
            if (answer.scope() == null || answer.releaseId() == null || answer.releaseId().isBlank()) {
                throw new QueryRefusalException(RefusalReason.unsupported_claim, "Successful answer is missing scope or release id");
            }
            if ((answer.citedObjects() == null || answer.citedObjects().isEmpty()) && (answer.facts() == null || answer.facts().isEmpty())) {
                throw new QueryRefusalException(RefusalReason.unsupported_claim, "Successful answer has no cited object or grounded fact");
            }
        }
        Map<String, String> safeHashes = l2ReadHashes == null ? Map.of() : l2ReadHashes;
        Set<String> cited = new HashSet<>(answer.citedObjects() == null ? Set.of() : answer.citedObjects());
        List<GroundedClaim> claims = new ArrayList<>();
        if (answer.facts() != null) {
            answer.facts().forEach(fact -> {
                if (!cited.contains(fact.uri()) || !safeHashes.containsKey(fact.uri())) {
                    throw new QueryRefusalException(RefusalReason.unsupported_claim, "Fact is not backed by an L2-read cited object");
                }
                String hash = hashFromSource(fact.source());
                if (hash == null || hash.isBlank()) {
                    throw new QueryRefusalException(RefusalReason.unsupported_claim, "Fact is missing L2 content hash evidence");
                }
                String expectedHash = safeHashes.get(fact.uri());
                if (expectedHash != null && !expectedHash.isBlank() && !expectedHash.equals(hash)) {
                    throw new QueryRefusalException(RefusalReason.hash_mismatch, "Fact L2 content hash does not match the object read in this trace");
                }
                claims.add(new GroundedClaim(
                        fact.text(),
                        List.of(new GroundingEvidence(fact.uri(), hash, "$")),
                        ValidationStatus.grounded,
                        null));
            });
        }
        requireAnswerUsesStructuredClaims(answer);
        if (answer.answer() != null && answer.answer().contains("raw review")) {
            throw new QueryRefusalException(RefusalReason.governance_unauthorized, "Forbidden governance claim");
        }
        promptPolicyGuard.validateGeneratedAnswer(answer.answer());
        return new GroundingMap(claims);
    }

    private void requireAnswerUsesStructuredClaims(ServiceAnswer answer) {
        if (answer.answer() == null || answer.answer().isBlank() || answer.facts() == null || answer.facts().isEmpty()) {
            return;
        }
        String normalizedAnswer = normalize(answer.answer());
        boolean hasFactText = answer.facts().stream()
                .map(fact -> normalize(fact.text()))
                .filter(text -> !text.isBlank())
                .anyMatch(text -> normalizedAnswer.contains(text) || text.contains(normalizedAnswer));
        boolean hasGroundedCitation = answer.facts().stream()
                .filter(fact -> fact.uri() != null && !fact.uri().isBlank())
                .anyMatch(fact -> answer.answer().contains(fact.uri()));
        if ((!hasFactText && !hasGroundedCitation) || !answerVocabularyIsGrounded(answer)) {
            throw new QueryRefusalException(RefusalReason.unsupported_claim,
                    "Answer text is not derived from validated facts or cited objects");
        }
    }

    private boolean answerVocabularyIsGrounded(ServiceAnswer answer) {
        Set<String> factTokens = new HashSet<>();
        answer.facts().forEach(fact -> {
            factTokens.addAll(tokens(fact.text()));
            factTokens.addAll(tokens(fact.uri()));
            factTokens.addAll(tokens(hashFromSource(fact.source())));
        });
        List<String> answerTokens = tokens(answer.answer()).stream()
                .filter(token -> !groundingStopwords().contains(token))
                .filter(token -> !token.startsWith("trace-"))
                .filter(token -> !token.startsWith("sha256"))
                .filter(token -> token.length() > 2 || token.contains("."))
                .toList();
        if (answerTokens.isEmpty()) {
            return false;
        }
        return factTokens.containsAll(answerTokens);
    }

    private List<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("rts://[^\\s,，。)\\]}]+", " ")
                .replaceAll("sha256:[0-9a-f]+", " ")
                .replaceAll("[^a-z0-9_.\\-/]+", " ")
                .strip();
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(normalized.split("\\s+"));
    }

    private Set<String> groundingStopwords() {
        return Set.of(
                "a", "an", "and", "are", "as", "based", "by", "cited", "citation", "derived", "for", "from",
                "grounded", "hash", "in", "is", "l2", "object", "of", "on", "only", "provided", "read", "release",
                "rewrite", "the", "this", "trace", "uri", "with", "引用", "证据", "基于");
    }

    private String hashFromSource(String source) {
        if (source != null && source.startsWith("l2:") && source.length() > 3) {
            return source.substring(3);
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").strip();
    }
}
