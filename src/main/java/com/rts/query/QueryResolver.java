package com.rts.query;

import com.rts.model.CoreModels.QueryPlan;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ScopeKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class QueryResolver {
    private static final Pattern URI = Pattern.compile("rts://[^\\s,，。]+");
    private static final Pattern RULE = Pattern.compile("\\b(rule[_\\-][A-Za-z0-9_\\-]+|rule_[A-Za-z0-9_\\-]+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOOKUP = Pattern.compile("\\b(lookup[_\\-][A-Za-z0-9_\\-]+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern HELPER = Pattern.compile("\\b(helper[_\\-][A-Za-z0-9_\\-]+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TARGET = Pattern.compile("(target(?:\\s+field)?\\s*[:=]\\s*|字段\\s*)([A-Za-z0-9_.\\-/]+)", Pattern.CASE_INSENSITIVE);

    public QueryPlan resolve(String query, ScopeKey scopeHint) {
        List<String> anchors = extractAnchors(query);
        if (scopeHint == null) {
            return new QueryPlan(
                    "unknown_or_chitchat",
                    null,
                    anchors,
                    "released",
                    List.of("resolve_scope"),
                    true,
                    "请提供 channel/product/pack/domain scope 后再查询。",
                    RefusalReason.scope_unclear);
        }
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
        String intent = "rule_lookup";
        if (normalized.contains("raw message") || normalized.contains("target message") || normalized.contains("目标报文") || normalized.contains("生成报文")) {
            intent = "generate_target_message";
        } else if (normalized.contains("compare") || normalized.contains("对比") || normalized.contains("比较")) {
            intent = "compare_source_target";
        } else if (normalized.contains("test") || normalized.contains("测试")) {
            intent = "test_planning";
        } else if (normalized.contains("evidence") || normalized.contains("证据")) {
            intent = "evidence_check";
        } else if (normalized.contains("confidence") || normalized.contains("可信") || normalized.contains("置信")) {
            intent = "confidence_check";
        } else if (normalized.contains("release") || normalized.contains("发布")) {
            intent = "release_status_check";
        } else if (normalized.contains("review") || normalized.contains("裁决") || normalized.contains("问题")) {
            intent = "review_question";
        } else if (normalized.contains("scope") || normalized.contains("范围")) {
            intent = "scope_discovery";
        } else if (normalized.contains("impact") || normalized.contains("影响")) {
            intent = "impact_preview";
        } else if (normalized.contains("depend") || normalized.contains("依赖")) {
            intent = "dependency_lookup";
        } else if (normalized.contains("helper")) {
            intent = "helper_lookup";
        } else if (normalized.contains("lookup")) {
            intent = "lookup_lookup";
        } else if (normalized.contains("explain") || normalized.contains("解释") || normalized.contains("逻辑")) {
            intent = "explain_rule";
        }
        return new QueryPlan(
                intent,
                scopeHint,
                anchors,
                "released",
                List.of("resolve_scope", "find_objects", "get_object_card", "read_object_l2", "get_dependencies"),
                false,
                null,
                RefusalReason.object_not_found);
    }

    private List<String> extractAnchors(String query) {
        String safe = query == null ? "" : query;
        List<String> anchors = new ArrayList<>();
        collect(URI, safe, anchors, 0);
        collect(RULE, safe, anchors, 1);
        collect(LOOKUP, safe, anchors, 1);
        collect(HELPER, safe, anchors, 1);
        collect(TARGET, safe, anchors, 2);
        return anchors.stream().distinct().toList();
    }

    private void collect(Pattern pattern, String value, List<String> anchors, int group) {
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            anchors.add(matcher.group(group));
        }
    }
}
