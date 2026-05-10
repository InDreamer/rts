package com.rts.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.rts.model.CoreModels.CandidateObject;
import com.rts.model.CoreModels.DependencyEdge;
import com.rts.model.CoreModels.Fact;
import com.rts.model.CoreModels.L2Content;
import com.rts.model.CoreModels.ObjectCard;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.model.CoreModels.Refusal;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.model.CoreModels.ScopeRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AgentServiceModels {
    private AgentServiceModels() {}

    public enum ContextKind {
        system_policy,
        caller_profile,
        ui_context,
        workspace_context,
        session_context,
        object_card,
        l2_fact,
        dependency,
        governance_warning,
        trace_metadata,
        memory,
        inference,
        unknown
    }

    public enum ValidationStatus {
        grounded,
        rejected,
        warning
    }

    public enum FeedbackRoute {
        trace_feedback,
        retrieval_quality_queue,
        card_improvement_candidate,
        review_workflow,
        ignored_not_truth
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContextItem(
            ContextKind kind,
            String source,
            boolean truthEligible,
            String objectUri,
            String hash,
            String text
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroundingEvidence(
            String objectUri,
            String l2Hash,
            String fieldPath
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Citation(
            String releaseId,
            String objectUri,
            String contentHash,
            String evidenceType
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroundedClaim(
            String claim,
            List<GroundingEvidence> groundedBy,
            ValidationStatus validation,
            String reason
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroundingMap(List<GroundedClaim> claims) {
        public static GroundingMap empty() {
            return new GroundingMap(List.of());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolStepTrace(
            int stepNo,
            String toolName,
            String toolInputHash,
            String toolOutputHash,
            List<String> selectedUris,
            String policyResult
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BudgetUsage(
            int toolCallsUsed,
            int maxToolCalls,
            int l2ReadsUsed,
            int maxL2Reads,
            int dependencyDepthUsed,
            int maxDependencyDepth,
            int retrievedTokensUsed,
            int maxRetrievedTokens,
            int modelCallsUsed,
            int maxModelCalls,
            long latencyMs,
            long maxLatencyMs
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScenarioReport(
            String schemaVersion,
            String status,
            String scenarioType,
            String inputSummary,
            ScopeKey scope,
            String releaseId,
            List<Fact> facts,
            List<String> inferences,
            List<String> candidates,
            List<String> unknowns,
            List<String> nextEvidenceNeeded,
            List<Citation> citations,
            GroundingMap groundingMap,
            String traceId,
            BudgetUsage budgetUsage,
            Refusal refusal,
            List<String> warnings
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ManagedScenarioRequest(
            String scenarioType,
            String input,
            ScopeKey scope,
            String callerId,
            String apiKey,
            String outputMode,
            Integer maxObjects
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolCatalogEntry(
            String name,
            String purpose,
            String requiredPermission,
            List<String> requiredFields,
            List<RefusalReason> possibleRefusalReasons,
            String inputSchema,
            String outputSchema,
            int budgetCost,
            List<String> allowedIntents,
            String traceRedactionRule
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScopeSummary(
            String releaseId,
            ScopeKey scope,
            String permissionBoundary,
            String precedencePolicy,
            boolean deprecated,
            String supersededBy,
            Map<String, Long> objectCounts,
            List<String> warnings
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScopeTree(
            String releaseId,
            List<ScopeSummary> scopes
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PackNavigation(
            String releaseId,
            ScopeKey scope,
            List<ObjectManifestEntry> objects,
            List<ObjectCard> cards,
            List<DependencyEdge> dependencyEdges,
            List<String> warnings
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConfusableObject(
            String objectUri,
            String confusableWithUri,
            String reason,
            ScopeKey scope,
            ScopeKey confusableScope
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConfusableScope(
            ScopeKey requestedScope,
            ScopeKey confusableScope,
            String reason
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EvidenceSummary(
            String objectUri,
            String releaseId,
            String status,
            String sourceSummary,
            List<String> riskFlags,
            List<String> reviewPointers,
            boolean rawEvidenceIncluded,
            List<String> warnings
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImpactCandidate(
            String impactedObjectUri,
            String impactedTargetPath,
            List<String> dependencyPath,
            double confidence,
            String reason
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImpactAnalysisResult(
            String status,
            String releaseId,
            ScopeKey scope,
            List<Fact> facts,
            List<String> inferences,
            List<ImpactCandidate> candidates,
            List<String> unknowns,
            List<String> warnings,
            String traceId,
            GroundingMap groundingMap
    ) {
        public ImpactAnalysisResult(String status, String releaseId, ScopeKey scope, List<Fact> facts, List<String> inferences,
                List<ImpactCandidate> candidates, List<String> unknowns, List<String> warnings, String traceId) {
            this(status, releaseId, scope, facts, inferences, candidates, unknowns, warnings, traceId, GroundingMap.empty());
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TestPlanResult(
            String status,
            String releaseId,
            ScopeKey scope,
            List<Fact> facts,
            List<String> positiveTestCandidates,
            List<String> negativeTestCandidates,
            List<String> boundaryCases,
            List<String> dependencyCoverageSuggestions,
            List<String> regressionFocus,
            List<String> unknowns,
            List<String> warnings,
            String traceId
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReleaseReadinessResult(
            String status,
            String releaseId,
            ScopeKey scope,
            int blockingIssuesCount,
            List<String> facts,
            List<String> warnings,
            String traceId
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroundingCheckResult(
            String status,
            String traceId,
            GroundingMap groundingMap,
            RefusalReason refusalReason,
            List<String> warnings
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScopeListRequest(String callerId, String apiKey, String outputMode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScopeSearchRequest(String query, String callerId, String apiKey, String outputMode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScopedToolRequest(ScopeKey scope, String callerId, String apiKey, String outputMode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ObjectCardsRequest(List<String> uris, String callerId, String apiKey, String outputMode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConfusableObjectsRequest(String uri, ScopeKey scope, String callerId, String apiKey, String outputMode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImpactAnalysisRequest(
            String changedUri,
            String changedTargetPath,
            String changedSourceAnchor,
            ScopeKey scope,
            String callerId,
            String apiKey,
            String outputMode,
            boolean readL2,
            Integer maxObjects
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TestPlanRequest(
            String seedUri,
            ScopeKey scope,
            String callerId,
            String apiKey,
            String outputMode,
            boolean includeL2,
            Integer maxObjects
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReleaseReadinessRequest(ScopeKey scope, String callerId, String apiKey, String outputMode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroundingCheckRequest(String traceId, String callerId, String apiKey, String outputMode) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RuleCompareRequest(
            String leftUri,
            String rightUri,
            String callerId,
            String apiKey,
            String outputMode,
            boolean readL2
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RuleCompareResult(
            String status,
            String releaseId,
            List<Fact> facts,
            List<String> inferences,
            List<String> unknowns,
            List<String> warnings,
            GroundingMap groundingMap,
            String traceId
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConflictExplainRequest(
            String uri,
            ScopeKey scope,
            String callerId,
            String apiKey,
            String outputMode
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConflictExplanation(
            String status,
            String releaseId,
            ScopeKey scope,
            String objectUri,
            List<String> facts,
            List<String> inferences,
            List<String> unknowns,
            List<String> warnings,
            String traceId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AnswerViewRequest(
            com.rts.model.CoreModels.ServiceAnswer answer,
            String responseView,
            String callerId,
            String apiKey
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AnswerViewResult(
            String status,
            String responseView,
            String conclusion,
            List<Fact> facts,
            List<String> dependencies,
            List<String> warnings,
            GroundingMap groundingMap,
            String traceId,
            Map<String, Object> audit,
            Map<String, Object> pipeline,
            List<String> nextActions
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ParsedRawField(
            String sourcePath,
            String value,
            boolean uncertain,
            String reason
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TargetFieldCandidate(
            String targetPath,
            String candidateValue,
            String ruleUri,
            List<String> dependencyUris,
            String status,
            List<GroundingEvidence> groundedBy,
            List<String> unknowns,
            List<String> warnings
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawMessageCandidateRequest(
            String rawMessage,
            ScopeKey scope,
            String callerId,
            String apiKey,
            String outputMode,
            Integer maxObjects
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawMessageCandidateResult(
            String status,
            String releaseId,
            ScopeKey scope,
            List<ParsedRawField> parsedFields,
            List<TargetFieldCandidate> targetCandidates,
            List<String> unknowns,
            List<String> warnings,
            GroundingMap groundingMap,
            String traceId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SourceFieldMappingResult(
            String releaseId,
            ScopeKey scope,
            List<ParsedRawField> parsedFields,
            List<String> matchedRuleUris,
            List<String> unknownSourceFields,
            String traceId
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RequiredLookupResolution(
            String ruleUri,
            List<String> lookupUris,
            List<String> helperUris,
            List<String> unknowns
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TargetMessageGroundingValidation(
            String status,
            List<String> groundedRuleUris,
            List<String> unsupportedTargetPaths,
            GroundingMap groundingMap,
            List<String> warnings
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FeedbackRequest(
            String traceId,
            String feedbackType,
            String message,
            ScopeKey scope,
            String selectedObjectUri,
            String callerId,
            String apiKey,
            String sessionId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FeedbackRecord(
            String feedbackId,
            String traceId,
            String callerId,
            String sessionId,
            String feedbackType,
            String message,
            ScopeKey scope,
            String selectedObjectUri,
            FeedbackRoute route,
            boolean truthEligible,
            Instant createdAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MemoryWriteRequest(
            String sessionId,
            String memoryType,
            String key,
            String value,
            ScopeKey scope,
            String callerId,
            String apiKey
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MemoryRecord(
            String memoryId,
            String sessionId,
            String callerId,
            String memoryType,
            String key,
            String value,
            ScopeKey scope,
            boolean truthEligible,
            Instant createdAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContextSnapshotRequest(
            String sessionId,
            ScopeKey scope,
            String callerId,
            String apiKey
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContextSnapshot(
            String sessionId,
            String callerId,
            List<MemoryRecord> memories,
            List<ContextItem> contextItems,
            List<String> warnings
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContextualAskRequest(
            String query,
            String callerId,
            String apiKey,
            ScopeKey scopeHint,
            String outputMode,
            Integer maxToolCalls,
            String sessionId,
            ScopeKey workspaceScope
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FeatureFlags(
            boolean plannerV2Enabled,
            boolean toolOrchestratorEnabled,
            boolean rerankerEnabled,
            boolean confusableCheckEnabled,
            boolean vectorRecallEnabled,
            boolean impactCandidatesEnabled,
            boolean testPlanCandidatesEnabled,
            boolean mcpExpandedToolsEnabled
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EvaluationCase(
            String caseId,
            String query,
            ScopeKey expectedScope,
            List<String> expectedObjects,
            RefusalReason expectedRefusal,
            String caseType,
            String callerId,
            String apiKey
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EvaluationResult(
            String runId,
            String caseId,
            String mode,
            boolean correctScope,
            boolean correctObjectFound,
            boolean refusalCorrect,
            int unsupportedClaimCount,
            long latencyMs,
            String traceId,
            List<String> warnings
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EvaluationRunResult(
            String runId,
            String mode,
            int totalCases,
            long correctScopeCount,
            long correctObjectFoundCount,
            long refusalCorrectCount,
            long unsupportedClaimCount,
            List<EvaluationResult> results,
            Map<String, Double> thresholds,
            boolean passed,
            List<String> gateFailures
    ) {
        public EvaluationRunResult(String runId, String mode, int totalCases, long correctScopeCount, long correctObjectFoundCount,
                long refusalCorrectCount, long unsupportedClaimCount, List<EvaluationResult> results) {
            this(runId, mode, totalCases, correctScopeCount, correctObjectFoundCount, refusalCorrectCount, unsupportedClaimCount,
                    results, Map.of(), false, List.of("evaluation thresholds were not supplied"));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EvaluationRunRequest(
            String mode,
            List<EvaluationCase> cases
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GovernanceReviewRequest(
            ScopeKey scope,
            String objectUri,
            String callerId,
            String apiKey,
            String outputMode,
            boolean readL2
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReviewerQuestion(
            String questionId,
            String objectUri,
            String question,
            List<String> options,
            String reason
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GovernanceReviewResult(
            String status,
            String releaseId,
            ScopeKey scope,
            List<Fact> facts,
            List<String> conflictCandidates,
            List<String> ambiguityCandidates,
            List<ReviewerQuestion> reviewerQuestions,
            List<String> warnings,
            String traceId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HumanDecisionRecordRequest(
            String traceId,
            String objectUri,
            ScopeKey scope,
            String decisionType,
            String decisionText,
            String decidedBy,
            String callerId,
            String apiKey
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HumanDecisionRecord(
            String decisionId,
            String traceId,
            String objectUri,
            ScopeKey scope,
            String decisionType,
            String decisionText,
            String decidedBy,
            boolean runtimeTruthChanged,
            Instant createdAt
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MetricsSnapshot(
            long scopeResolutionAccuracyNumerator,
            long scopeResolutionAccuracyDenominator,
            long clarificationPrecisionNumerator,
            long clarificationPrecisionDenominator,
            long topKRecallNumerator,
            long topKRecallDenominator,
            long groundedAnswerRateNumerator,
            long groundedAnswerRateDenominator,
            long unsupportedClaimRateNumerator,
            long unsupportedClaimRateDenominator,
            long unsupportedClaimCount,
            long wrongScopeCount,
            long refusalCorrectnessNumerator,
            long refusalCorrectnessDenominator,
            long l2ReadEfficiencyNumerator,
            long l2ReadEfficiencyDenominator,
            long traceCompletenessNumerator,
            long traceCompletenessDenominator,
            long userCorrectionRateNumerator,
            long userCorrectionRateDenominator,
            long cardImprovementYieldNumerator,
            long cardImprovementYieldDenominator,
            long memoryAsTruthCount,
            long permissionLeakCount,
            long draftAsApprovedCount,
            long conflictHiddenCount,
            List<String> warnings
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MetricsSnapshotRequest(
            List<EvaluationResult> evaluationResults
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PipelineResult(
            String status,
            String releaseId,
            ScopeKey scope,
            Map<String, Object> machineResult,
            List<String> warnings,
            String traceId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TraceReportRequest(String traceId, String callerId, String apiKey) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TraceReport(
            String traceId,
            String status,
            String releaseId,
            ScopeKey scope,
            List<String> toolCalls,
            GroundingMap groundingMap,
            BudgetUsage budgetUsage,
            List<String> warnings
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgentObjectEnvelope(
            ObjectManifestEntry objectManifest,
            ObjectCard objectCard,
            L2Content l2Content,
            List<DependencyEdge> dependencies,
            GroundingMap groundingMap,
            List<ContextItem> context
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CandidateSearchResult(
            List<CandidateObject> candidates,
            List<ConfusableObject> confusables,
            List<String> warnings
    ) {}

    public static ScopeSummary summarize(String releaseId, ScopeRecord scope, Map<String, Long> objectCounts, List<String> warnings) {
        return new ScopeSummary(
                releaseId,
                scope.key(),
                scope.permissionBoundary(),
                scope.precedencePolicy(),
                scope.deprecatedFlag(),
                scope.supersededBy(),
                objectCounts,
                warnings);
    }
}
