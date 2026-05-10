package com.rts.agent;

import com.rts.model.CoreModels.AgentPlan;
import com.rts.model.CoreModels.QueryPlan;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.query.QueryRefusalException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentRecipeService {
    public record AgentRecipe(
            String recipeVersion,
            List<String> toolSequence,
            List<String> requiredEvidence,
            String stopCondition,
            String reportCompiler,
            String validationRule
    ) {}

    public AgentPlan applyRecipe(AgentPlan plan) {
        if (plan == null) {
            throw new QueryRefusalException(RefusalReason.unsupported_claim, "Agent recipe requires a plan");
        }
        AgentRecipe recipe = recipeFor(plan.intent(), plan.scenarioType(), plan.toolPlan());
        return new AgentPlan(
                plan.intent(),
                plan.scenarioType(),
                plan.scope(),
                plan.anchors(),
                plan.requiredState(),
                recipe.toolSequence(),
                recipe.recipeVersion(),
                plan.budgets(),
                recipe.requiredEvidence(),
                plan.clarificationQuestion(),
                plan.refusalIfMissing(),
                plan.releaseId(),
                plan.scopeSnapshot());
    }

    public AgentRecipe recipeFor(AgentPlan plan) {
        if (plan == null) {
            throw new QueryRefusalException(RefusalReason.unsupported_claim, "Agent recipe requires a plan");
        }
        return recipeFor(plan.intent(), plan.scenarioType(), plan.toolPlan());
    }

    public AgentRecipe recipeFor(String intent, String scenarioType, List<String> fallbackTools) {
        String recipeVersion = recipeVersion(intent, scenarioType);
        return new AgentRecipe(
                recipeVersion,
                toolsFor(intent, scenarioType, fallbackTools),
                expectedEvidenceFor(recipeVersion),
                "service_owned_observe_revise_until_required_evidence_or_budget",
                compilerFor(recipeVersion),
                "claim_level_l2_hash_grounding");
    }

    public String scenarioManagedQuery(String scenarioType, String input, String anchor) {
        String safeAnchor = anchor == null || anchor.isBlank() ? summarize(input) : anchor;
        if (safeAnchor == null || safeAnchor.isBlank()) {
            return null;
        }
        return switch (normalize(scenarioType)) {
            case "pr_diff_impact", "exception_investigation" -> "impact " + safeAnchor;
            case "failed_message_analysis" -> "target message " + summarize(input);
            case "test_planning" -> "test " + safeAnchor;
            case "governance_review" -> "review " + safeAnchor;
            default -> safeAnchor;
        };
    }

    public String recipeVersion(String intent, String scenarioType) {
        String normalizedScenario = normalize(scenarioType);
        if (!normalizedScenario.isBlank() && !"ask".equals(normalizedScenario)) {
            return normalizedScenario + ".v1";
        }
        return switch (normalize(intent)) {
            case "impact_preview" -> "managed_ask.impact_preview.v1";
            case "test_planning" -> "managed_ask.test_planning.v1";
            case "evidence_check" -> "managed_ask.evidence_check.v1";
            case "dependency_lookup" -> "managed_ask.dependency_lookup.v1";
            default -> "managed_ask.v1";
        };
    }

    private List<String> toolsFor(String intent, String scenarioType, List<String> fallback) {
        List<String> core = switch (recipeVersion(intent, scenarioType)) {
            case "managed_ask.evidence_check.v1" -> List.of("resolve_scope", "find_objects", "get_object_card", "read_object_l2");
            default -> List.of("resolve_scope", "find_objects", "get_object_card", "read_object_l2", "get_dependencies");
        };
        return fallback == null || fallback.isEmpty() ? core : fallback.stream()
                .filter(tool -> tool != null && !tool.isBlank())
                .distinct()
                .toList();
    }

    private List<String> expectedEvidenceFor(String recipeVersion) {
        if (recipeVersion != null && recipeVersion.contains("evidence")) {
            return List.of("L2 runtime object", "content hash", "authorized governance summary or trace");
        }
        if (recipeVersion != null && (recipeVersion.contains("impact_preview") || recipeVersion.contains("dependency_lookup"))) {
            return List.of("L2 runtime object", "content hash", "dependency edge", "dependency L2 runtime object", "trace");
        }
        return List.of("L2 runtime object", "content hash", "dependency edge", "trace");
    }

    private String compilerFor(String recipeVersion) {
        if (recipeVersion != null && recipeVersion.endsWith(".v1") && !recipeVersion.startsWith("managed_ask")) {
            return "scenario_report_compiler";
        }
        return "managed_answer_compiler";
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
    }

    private String summarize(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.replaceAll("\\s+", " ").strip();
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }
}
