package com.rts.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CoreModels {
    private CoreModels() {}

    public enum ObjectType {
        rule,
        lookup,
        helper,
        source_anchor,
        target_path
    }

    public enum RefusalReason {
        none,
        scope_unclear,
        unauthorized_scope,
        active_release_missing,
        active_release_not_unique,
        manifest_invalid,
        schema_incompatible,
        object_not_found,
        l2_missing,
        hash_mismatch,
        dependency_unreleased,
        ambiguity_unresolved,
        only_similarity_no_structured_match,
        governance_unauthorized,
        tool_budget_exhausted,
        unsupported_claim
    }

    public enum AnswerType {
        answer,
        clarification,
        refusal,
        partial
    }

    public enum Direction {
        forward,
        reverse,
        both
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ActiveReleasePointer(
            @NotBlank String activeReleaseId,
            String rollbackTargetReleaseId,
            Instant updatedAt,
            String updatedBy
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReleaseManifest(
            @NotBlank String releaseId,
            @NotBlank String canonicalRevision,
            @NotBlank String projectionSchemaVersion,
            @NotBlank String cardSchemaVersion,
            String summarySchemaVersion,
            @NotBlank String activationState,
            Instant generatedAt,
            Instant releasedAt,
            String rollbackTargetReleaseId,
            String contentHashSummary,
            int blockingIssuesCount,
            Instant createdAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScopeKey(
            @NotBlank String channel,
            @NotBlank String product,
            @NotBlank String pack,
            @NotBlank String domain
    ) {
        public boolean matches(ScopeKey other) {
            return other != null
                    && channel.equals(other.channel)
                    && product.equals(other.product)
                    && pack.equals(other.pack)
                    && domain.equals(other.domain);
        }

        public String value() {
            return channel + "/" + product + "/" + pack + "/" + domain;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScopeRecord(
            @NotBlank String releaseId,
            @NotBlank String channel,
            @NotBlank String product,
            @NotBlank String pack,
            @NotBlank String domain,
            boolean activeFlag,
            String permissionBoundary,
            String precedencePolicy,
            boolean deprecatedFlag,
            String supersededBy
    ) {
        public ScopeKey key() {
            return new ScopeKey(channel, product, pack, domain);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ObjectManifestEntry(
            @NotBlank String uri,
            @NotBlank String releaseId,
            @NotBlank String objectId,
            @NotNull ObjectType objectType,
            @NotBlank String channel,
            @NotBlank String product,
            @NotBlank String pack,
            @NotBlank String domain,
            String targetPath,
            List<String> sourceAnchors,
            @NotBlank String contentHash,
            String cardRef,
            @NotBlank String contentRef,
            @NotBlank String schemaVersion,
            @NotBlank String state
    ) {
        public ScopeKey scope() {
            return new ScopeKey(channel, product, pack, domain);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ObjectCard(
            @NotBlank String uri,
            @NotBlank String releaseId,
            @NotNull ObjectType objectType,
            Map<String, Object> cardJson,
            String searchText,
            List<String> riskFlags,
            List<String> applicability,
            List<String> notApplicable,
            List<String> overrideRefs,
            List<String> supersessionRefs
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DependencyEdge(
            @NotBlank String releaseId,
            @NotBlank String fromUri,
            @NotBlank String toUri,
            @NotBlank String edgeType,
            boolean requiredFlag,
            @NotBlank String direction,
            String traversalPurpose
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentRef(
            @NotBlank String uri,
            @NotBlank String releaseId,
            @NotBlank String contentUri,
            @NotBlank String storageKind,
            @NotBlank String storageRef,
            @NotBlank String contentHash,
            @NotBlank String contentType,
            @NotBlank String schemaVersion
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallerProfile(
            @NotBlank String callerId,
            String apiKeyHash,
            List<String> allowedChannels,
            List<String> allowedProducts,
            List<String> allowedPacks,
            List<String> allowedEntrypoints,
            List<String> allowedOutputModes,
            boolean activeFlag
    ) {
        public boolean allows(ScopeKey scope, String entrypoint, String outputMode) {
            return activeFlag
                    && containsOrWildcard(allowedChannels, scope.channel)
                    && containsOrWildcard(allowedProducts, scope.product)
                    && containsOrWildcard(allowedPacks, scope.pack)
                    && containsOrWildcard(allowedEntrypoints, entrypoint)
                    && containsOrWildcard(allowedOutputModes, outputMode);
        }

        private static boolean containsOrWildcard(List<String> values, String value) {
            return values != null && (values.contains("*") || values.contains(value));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QueryPlan(
            @NotBlank String intent,
            ScopeKey scope,
            List<String> anchors,
            String requiredState,
            List<String> toolPlan,
            boolean needsClarification,
            String clarificationQuestion,
            RefusalReason refusalIfMissing
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CandidateObject(
            @NotBlank String uri,
            @NotNull ObjectType objectType,
            double score,
            List<String> matchedFields,
            boolean exactMatch
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fact(
            @NotBlank String text,
            @NotBlank String uri,
            @NotBlank String releaseId,
            String source
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DependencyResult(
            List<DependencyEdge> edges,
            List<ObjectManifestEntry> objects,
            boolean truncated
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record L2Content(
            @NotBlank String uri,
            @NotBlank String releaseId,
            @NotBlank String contentHash,
            @NotBlank String contentType,
            @NotBlank String content
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ServiceAnswer(
            @NotNull AnswerType answerType,
            ScopeKey scope,
            String releaseId,
            List<Fact> facts,
            List<String> inferences,
            List<String> unknowns,
            List<String> candidateSuggestions,
            List<String> humanDecisions,
            List<String> citedObjects,
            List<DependencyEdge> dependencies,
            String traceId,
            Refusal refusal,
            List<String> warnings,
            String answer
    ) {
        public static ServiceAnswer refusal(RefusalReason reason, String message, String traceId, ScopeKey scope, String releaseId) {
            return new ServiceAnswer(
                    AnswerType.refusal,
                    scope,
                    releaseId,
                    List.of(),
                    List.of(),
                    message == null ? List.of() : List.of(message),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    traceId,
                    new Refusal(reason, message, List.of(), false),
                    List.of(),
                    null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Refusal(
            @NotNull RefusalReason reason,
            String whatIsMissing,
            List<String> whatUserCanProvide,
            boolean partialCandidatesExist
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TraceRecord(
            @NotBlank String traceId,
            String callerId,
            @NotBlank String entrypoint,
            String queryText,
            QueryPlan queryPlan,
            ScopeKey resolvedScope,
            List<String> candidateUris,
            List<String> selectedUris,
            List<String> l2ReadUris,
            RefusalReason refusalReason,
            String releaseId,
            long durationMs,
            Instant createdAt,
            List<String> toolCalls
    ) {
        public static Builder builder(String traceId, String entrypoint) {
            return new Builder(traceId, entrypoint);
        }

        public static final class Builder {
            private final String traceId;
            private final String entrypoint;
            private String callerId;
            private String queryText;
            private QueryPlan queryPlan;
            private ScopeKey resolvedScope;
            private List<String> candidateUris = new ArrayList<>();
            private List<String> selectedUris = new ArrayList<>();
            private List<String> l2ReadUris = new ArrayList<>();
            private RefusalReason refusalReason = RefusalReason.none;
            private String releaseId;
            private long durationMs;
            private Instant createdAt = Instant.now();
            private List<String> toolCalls = new ArrayList<>();

            private Builder(String traceId, String entrypoint) {
                this.traceId = traceId;
                this.entrypoint = entrypoint;
            }

            public Builder callerId(String callerId) { this.callerId = callerId; return this; }
            public Builder queryText(String queryText) { this.queryText = queryText; return this; }
            public Builder queryPlan(QueryPlan queryPlan) { this.queryPlan = queryPlan; return this; }
            public Builder resolvedScope(ScopeKey resolvedScope) { this.resolvedScope = resolvedScope; return this; }
            public Builder candidateUris(List<String> candidateUris) { this.candidateUris = safe(candidateUris); return this; }
            public Builder selectedUris(List<String> selectedUris) { this.selectedUris = safe(selectedUris); return this; }
            public Builder l2ReadUris(List<String> l2ReadUris) { this.l2ReadUris = safe(l2ReadUris); return this; }
            public Builder refusalReason(RefusalReason refusalReason) { this.refusalReason = refusalReason; return this; }
            public Builder releaseId(String releaseId) { this.releaseId = releaseId; return this; }
            public Builder durationMs(long durationMs) { this.durationMs = durationMs; return this; }
            public Builder toolCalls(List<String> toolCalls) { this.toolCalls = safe(toolCalls); return this; }

            public TraceRecord build() {
                return new TraceRecord(traceId, callerId, entrypoint, queryText, queryPlan, resolvedScope,
                        candidateUris, selectedUris, l2ReadUris, refusalReason, releaseId, durationMs, createdAt, toolCalls);
            }

            private static List<String> safe(List<String> values) {
                return values == null ? new ArrayList<>() : new ArrayList<>(values);
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LlmRunTrace(
            @NotBlank String llmRunId,
            @NotBlank String traceId,
            String model,
            String promptVersion,
            List<String> toolCalls,
            String toolOutputsHash,
            String finalOutputHash,
            String validationResult,
            long durationMs,
            Instant createdAt
    ) {}

    public static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
