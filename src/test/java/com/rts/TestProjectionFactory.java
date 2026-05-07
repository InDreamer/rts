package com.rts;

import com.rts.model.CoreModels;
import com.rts.model.CoreModels.ActiveReleasePointer;
import com.rts.model.CoreModels.CallerProfile;
import com.rts.model.CoreModels.ContentRef;
import com.rts.model.CoreModels.DependencyEdge;
import com.rts.model.CoreModels.ObjectCard;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.model.CoreModels.ObjectType;
import com.rts.model.CoreModels.ReleaseManifest;
import com.rts.model.CoreModels.ScopeRecord;
import com.rts.store.Hashing;
import com.rts.store.StoreContracts.ProjectionSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class TestProjectionFactory {
    public static final String RELEASE = "rel-2026-05-06";
    public static final String RULE_URI = "rts://tradition/stella/payments/day1/rules/rule_amount";
    public static final String LOOKUP_URI = "rts://tradition/stella/payments/day1/lookups/lookup_currency";
    public static final String HELPER_URI = "rts://tradition/stella/payments/day1/helpers/helper_rounding";
    public static final String OTHER_RULE_URI = "rts://tradition/aurora/payments/day1/rules/rule_amount";
    public static final String MISSING_L2_URI = "rts://tradition/stella/payments/day1/rules/rule_missing_l2";
    public static final String TESTER_KEY = "tester-key";
    public static final String ADMIN_KEY = "admin-key";

    private TestProjectionFactory() {}

    public static ProjectionSnapshot valid() {
        String ruleContent = ruleContent();
        String lookupContent = lookupContent();
        String helperContent = helperContent();
        String otherContent = "{\"logic\":\"Aurora amount uses a different source and must not answer Stella queries.\"}";
        return new ProjectionSnapshot(
                new ActiveReleasePointer(RELEASE, null, Instant.parse("2026-05-06T00:00:00Z"), "test"),
                manifest(0, "day1-v1"),
                List.of(
                        new ScopeRecord(RELEASE, "tradition", "stella", "payments", "core", true, "default", "product-specific", false, null),
                        new ScopeRecord(RELEASE, "tradition", "aurora", "payments", "core", true, "default", "product-specific", false, null)),
                List.of(
                        object(RULE_URI, "rule_amount", ObjectType.rule, "tradition", "stella", "payments", "core", "payment.amount", List.of("src.amount"), Hashing.sha256(ruleContent), "rules/rule_amount.json"),
                        object(LOOKUP_URI, "lookup_currency", ObjectType.lookup, "tradition", "stella", "payments", "core", null, List.of("src.currency"), Hashing.sha256(lookupContent), "lookups/lookup_currency.json"),
                        object(HELPER_URI, "helper_rounding", ObjectType.helper, "tradition", "stella", "payments", "core", null, List.of("src.amount"), Hashing.sha256(helperContent), "helpers/helper_rounding.json"),
                        object(OTHER_RULE_URI, "rule_amount", ObjectType.rule, "tradition", "aurora", "payments", "core", "payment.amount", List.of("src.amount"), Hashing.sha256(otherContent), "rules/aurora_rule_amount.json")),
                List.of(
                        card(RULE_URI, ObjectType.rule, "payment amount target field uses lookup_currency and helper_rounding. 金额 字段 生成 逻辑"),
                        card(LOOKUP_URI, ObjectType.lookup, "lookup currency maps source currency to ISO currency code"),
                        card(HELPER_URI, ObjectType.helper, "helper rounding rounds payment amount to two decimals"),
                        card(OTHER_RULE_URI, ObjectType.rule, "aurora payment amount target field wrong product confusable")),
                List.of(
                        new DependencyEdge(RELEASE, RULE_URI, LOOKUP_URI, "rule_to_lookup", true, "forward", "explain_rule"),
                        new DependencyEdge(RELEASE, RULE_URI, HELPER_URI, "rule_to_helper", true, "forward", "explain_rule")),
                List.of(
                        ref(RULE_URI, Hashing.sha256(ruleContent), "rules/rule_amount.json"),
                        ref(LOOKUP_URI, Hashing.sha256(lookupContent), "lookups/lookup_currency.json"),
                        ref(HELPER_URI, Hashing.sha256(helperContent), "helpers/helper_rounding.json"),
                        ref(OTHER_RULE_URI, Hashing.sha256(otherContent), "rules/aurora_rule_amount.json")),
                List.of(
                        new CallerProfile("tester", Hashing.sha256(TESTER_KEY), List.of("tradition"), List.of("stella"), List.of("payments"),
                                List.of("find", "query", "ask", "objects_get", "objects_content", "objects_dependencies", "trace",
                                        "scope_tools", "navigation_tools", "analysis_tools", "feedback_tools", "evidence_tools", "view_tools",
                                        "governance_tools"), List.of("*"), true),
                        new CallerProfile("admin", Hashing.sha256(ADMIN_KEY), List.of("*"), List.of("*"), List.of("*"), List.of("*"), List.of("*"), true)));
    }

    public static ProjectionSnapshot missingL2() {
        String content = ruleContent();
        return new ProjectionSnapshot(
                new ActiveReleasePointer(RELEASE, null, Instant.now(), "test"),
                manifest(0, "day1-v1"),
                List.of(new ScopeRecord(RELEASE, "tradition", "stella", "payments", "core", true, "default", "product-specific", false, null)),
                List.of(object(MISSING_L2_URI, "rule_missing_l2", ObjectType.rule, "tradition", "stella", "payments", "core", "payment.missing", List.of(), Hashing.sha256(content), "rules/missing.json")),
                List.of(card(MISSING_L2_URI, ObjectType.rule, "missing l2 rule")),
                List.of(),
                List.of(ref(MISSING_L2_URI, Hashing.sha256(content), "rules/missing.json")),
                List.of(new CallerProfile("tester", Hashing.sha256(TESTER_KEY), List.of("*"), List.of("*"), List.of("*"), List.of("*"), List.of("*"), true)));
    }

    public static ReleaseManifest manifest(int blocking, String schema) {
        return new ReleaseManifest(RELEASE, "canonical-abc", schema, "card-v1", "summary-v1", "active",
                Instant.parse("2026-05-06T00:00:00Z"), Instant.parse("2026-05-06T00:01:00Z"), null,
                "sample", blocking, Instant.parse("2026-05-06T00:00:00Z"));
    }

    public static ObjectManifestEntry object(String uri, String id, ObjectType type, String channel, String product, String pack, String domain,
            String targetPath, List<String> sources, String hash, String storageRef) {
        return new ObjectManifestEntry(uri, RELEASE, id, type, channel, product, pack, domain, targetPath, sources, hash, uri + "#card", uri,
                "object-v1", "released");
    }

    public static ObjectCard card(String uri, ObjectType type, String searchText) {
        return new ObjectCard(uri, RELEASE, type, Map.of("summary", searchText), searchText, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public static ContentRef ref(String uri, String hash, String storageRef) {
        return new ContentRef(uri, RELEASE, uri + "#l2", "local_file", storageRef, hash, "application/json", "l2-v1");
    }

    public static String ruleContent() {
        return "{\"logic\":\"payment.amount is generated from src.amount, rounded with helper_rounding, and currency-normalized using lookup_currency.\"}";
    }

    public static String lookupContent() {
        return "{\"logic\":\"lookup_currency maps source currency values to ISO currency codes with fallback UNKNOWN.\"}";
    }

    public static String helperContent() {
        return "{\"logic\":\"helper_rounding rounds decimal amounts to two places using half-up rounding.\"}";
    }
}
