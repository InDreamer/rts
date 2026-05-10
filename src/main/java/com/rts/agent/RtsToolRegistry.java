package com.rts.agent;

import com.rts.model.AgentServiceModels.ToolCatalogEntry;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.query.QueryRequests.DependenciesRequest;
import com.rts.query.QueryRequests.FindRequest;
import com.rts.query.QueryRequests.ObjectContentRequest;
import com.rts.query.QueryRequests.ObjectGetRequest;
import com.rts.query.QueryRequests.PlanRequest;
import com.rts.query.PermissionService;
import com.rts.query.QueryRefusalException;
import com.rts.store.StoreContracts.ProjectionStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class RtsToolRegistry {
    private final Map<String, ToolDefinition> tools;
    private final PermissionService permissionService;
    private final ProjectionStore projectionStore;

    public RtsToolRegistry() {
        this(null, null);
    }

    public RtsToolRegistry(PermissionService permissionService, ProjectionStore projectionStore) {
        this.permissionService = permissionService;
        this.projectionStore = projectionStore;
        this.tools = Map.ofEntries(
                tool("resolve_scope", "Resolve intent, anchors, and scoped plan before retrieval.", "scope_tools", List.of("query", "scope_hint")),
                tool("list_scopes", "List permission-filtered active scopes.", "scope_tools", List.of("caller_id")),
                tool("search_scopes", "Search permission-filtered active scopes.", "scope_tools", List.of("query", "caller_id")),
                tool("get_scope_summary", "Read a scope summary.", "scope_tools", List.of("scope", "caller_id")),
                tool("get_pack_navigation", "Read scoped object and dependency navigation.", "navigation_tools", List.of("scope", "caller_id")),
                tool("find_objects", "Find candidate RTS objects inside a resolved scope.", "find", List.of("query", "scope", "caller_id")),
                tool("find_by_target_path", "Find objects by deterministic target path.", "find", List.of("target_path", "scope", "caller_id")),
                tool("find_by_source_anchor", "Find objects by deterministic source anchor.", "find", List.of("source_anchor", "scope", "caller_id")),
                tool("find_by_lookup_key", "Find lookup objects by key or term.", "find", List.of("lookup_key", "scope", "caller_id")),
                tool("get_object_card", "Read object manifest/card through RTS policy.", "objects_get", List.of("uri", "caller_id")),
                tool("read_object_l2", "Read hash-validated L2 content through RTS policy.", "objects_content", List.of("uri", "caller_id")),
                tool("get_dependencies", "Traverse released dependency edges through RTS policy.", "objects_dependencies", List.of("uri", "caller_id")),
                tool("find_reverse_dependencies", "Traverse reverse dependency edges through RTS policy.", "objects_dependencies", List.of("uri", "caller_id")),
                tool("get_dependency_subgraph", "Read bounded dependency subgraph.", "objects_dependencies", List.of("uri", "caller_id")),
                tool("find_confusable_objects", "List similar non-selected objects for scope safety.", "navigation_tools", List.of("uri", "scope", "caller_id")),
                tool("find_confusable_scopes", "List similar scopes for clarification.", "scope_tools", List.of("scope", "caller_id")),
                tool("read_evidence_summary", "Read authorized governance evidence summary.", "evidence_tools", List.of("uri", "caller_id")),
                tool("governance_review", "Generate candidate governance review questions.", "governance_tools", List.of("scope or object_uri", "caller_id")),
                tool("record_human_decision", "Record human decision draft without mutating runtime truth.", "governance_tools", List.of("trace_id", "object_uri", "caller_id")),
                tool("analyze_impact", "Generate grounded impact candidates.", "analysis_tools", List.of("changed anchor", "scope", "caller_id")),
                tool("plan_tests", "Generate candidate test plan suggestions.", "analysis_tools", List.of("scope", "caller_id")),
                tool("compare_rules", "Compare two released rules as analysis.", "analysis_tools", List.of("left_uri", "right_uri", "caller_id")),
                tool("explain_conflict", "Explain conflict candidates without adjudication.", "analysis_tools", List.of("scope or uri", "caller_id")),
                tool("check_grounding", "Report trace grounding status.", "trace", List.of("trace_id", "caller_id")),
                tool("parse_raw_message_candidate", "Parse raw message candidate fields.", "analysis_tools", List.of("raw_message", "scope", "caller_id")),
                tool("map_source_fields_to_rules", "Map source fields to released rules.", "analysis_tools", List.of("raw_message", "scope", "caller_id")),
                tool("resolve_required_lookups", "Resolve required lookup/helper candidates.", "analysis_tools", List.of("raw_message", "scope", "caller_id")),
                tool("simulate_rule_application", "Simulate candidate rule application boundary.", "analysis_tools", List.of("raw_message", "scope", "caller_id")),
                tool("assemble_target_message_candidate", "Assemble target message candidate.", "analysis_tools", List.of("raw_message", "scope", "caller_id")),
                tool("validate_target_message_grounding", "Validate target candidate grounding.", "analysis_tools", List.of("raw_message", "scope", "caller_id")),
                tool("get_trace", "Read permission-gated trace.", "trace", List.of("trace_id", "caller_id")),
                tool("trace_report", "Return audit/pipeline trace report.", "trace", List.of("trace_id", "caller_id")),
                tool("metrics_snapshot", "Summarize evaluation/runtime metrics.", "metrics", List.of()),
                tool("run_evaluation", "Run deterministic or LLM-safe evaluation cases.", "evaluation", List.of("cases")),
                tool("feature_flags", "Read runtime feature flags.", "config", List.of()));
    }

    public ToolDefinition require(String name) {
        return Optional.ofNullable(tools.get(name))
                .orElseThrow(() -> new QueryRefusalException(RefusalReason.unsupported_claim, "Unknown RTS tool: " + name));
    }

    public boolean contains(String name) {
        return tools.containsKey(name);
    }

    public ToolCatalogEntry catalogEntry(String name) {
        ToolDefinition definition = require(name);
        return new ToolCatalogEntry(name, definition.purpose(), definition.requiredPermission(), definition.requiredFields(),
                List.of(RefusalReason.scope_unclear, RefusalReason.unauthorized_scope, RefusalReason.object_not_found,
                        RefusalReason.l2_missing, RefusalReason.hash_mismatch, RefusalReason.tool_budget_exhausted,
                        RefusalReason.model_provider_failure, RefusalReason.unsupported_claim),
                definition.inputSchema(),
                definition.outputSchema(),
                definition.budgetCost(),
                definition.allowedIntents(),
                definition.traceRedactionRule());
    }

    public List<ToolCatalogEntry> catalog() {
        return tools.keySet().stream()
                .sorted()
                .map(this::catalogEntry)
                .toList();
    }

    public Object execute(String name, Object input, Function<Object, Object> implementation) {
        ToolDefinition definition = require(name);
        enforce(definition, input);
        return implementation.apply(input);
    }

    public void enforce(String name, Object input) {
        enforce(require(name), input);
    }

    private void enforce(ToolDefinition definition, Object input) {
        if (permissionService == null || projectionStore == null) {
            return;
        }
        ToolCaller caller = caller(input);
        if (caller.callerId() == null || caller.callerId().isBlank()) {
            throw new QueryRefusalException(RefusalReason.unauthorized_scope, "caller_id is required for RTS tool " + definition.name());
        }
        if (caller.scope() != null) {
            String releaseId = activeReleaseId();
            permissionService.requireAllowed(releaseId, caller.callerId(), caller.apiKey(), caller.scope(), definition.requiredPermission(), caller.outputMode());
        }
    }

    private String activeReleaseId() {
        try {
            return projectionStore.loadActiveSnapshot().manifest().releaseId();
        } catch (RuntimeException ex) {
            throw new QueryRefusalException(RefusalReason.active_release_missing, "Active release is unavailable");
        }
    }

    private ToolCaller caller(Object input) {
        if (input instanceof PlanRequest request) {
            return new ToolCaller(request.callerId(), null, request.scopeHint(), mode(request.outputMode()));
        }
        if (input instanceof FindRequest request) {
            return new ToolCaller(request.callerId(), request.apiKey(), request.scope(), mode(request.outputMode()));
        }
        if (input instanceof ObjectGetRequest request) {
            return new ToolCaller(request.callerId(), request.apiKey(), null, "default");
        }
        if (input instanceof ObjectContentRequest request) {
            return new ToolCaller(request.callerId(), request.apiKey(), null, "default");
        }
        if (input instanceof DependenciesRequest request) {
            return new ToolCaller(request.callerId(), request.apiKey(), null, "default");
        }
        return new ToolCaller(null, null, null, "default");
    }

    private String mode(String outputMode) {
        return outputMode == null || outputMode.isBlank() ? "default" : outputMode;
    }

    private static Map.Entry<String, ToolDefinition> tool(String name, String purpose, String permission, List<String> requiredFields) {
        String schemaBase = "rts.tool." + name + ".v1";
        return Map.entry(name, new ToolDefinition(name, purpose, permission, requiredFields,
                schemaBase + ".input", schemaBase + ".output", budgetCost(name), allowedIntents(name), redactionRule(name)));
    }

    private static int budgetCost(String name) {
        if ("read_object_l2".equals(name) || "read_evidence_summary".equals(name)) {
            return 3;
        }
        if (name.contains("dependencies") || name.contains("subgraph")) {
            return 2;
        }
        return 1;
    }

    private static List<String> allowedIntents(String name) {
        if (name.contains("governance") || name.contains("decision") || name.contains("evidence")) {
            return List.of("evidence_check", "review_question", "governance_review");
        }
        if (name.contains("message") || name.contains("source_fields") || name.contains("lookup") || name.contains("rule_application")) {
            return List.of("generate_target_message", "failed_message_analysis");
        }
        if (name.contains("impact")) {
            return List.of("impact_preview", "pr_diff_impact", "exception_investigation");
        }
        if (name.contains("test")) {
            return List.of("test_planning");
        }
        return List.of("*");
    }

    private static String redactionRule(String name) {
        if (name.contains("raw_message") || name.contains("message")) {
            return "hash_raw_external_payload_keep_field_names";
        }
        if (name.contains("evidence") || name.contains("governance")) {
            return "keep_summary_refs_redact_raw_evidence";
        }
        return "hash_inputs_keep_release_scope_uri_and_content_hash";
    }

    public record ToolDefinition(
            String name,
            String purpose,
            String requiredPermission,
            List<String> requiredFields,
            String inputSchema,
            String outputSchema,
            int budgetCost,
            List<String> allowedIntents,
            String traceRedactionRule
    ) {}

    private record ToolCaller(String callerId, String apiKey, ScopeKey scope, String outputMode) {}
}
