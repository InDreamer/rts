package com.rts;

import static org.assertj.core.api.Assertions.assertThat;

import com.rts.index.LuceneIndexService;
import com.rts.agent.AgentAnalysisService;
import com.rts.agent.AgentToolService;
import com.rts.model.AgentServiceModels.ImpactAnalysisRequest;
import com.rts.model.AgentServiceModels.TestPlanRequest;
import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.Direction;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.model.CoreModels.ObjectType;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.query.QueryRequests.DependenciesRequest;
import com.rts.query.QueryRequests.FindRequest;
import com.rts.query.QueryRequests.ObjectContentRequest;
import com.rts.query.QueryRequests.QueryRequest;
import com.rts.query.QueryService;
import com.rts.store.FileSystemProjectionStore;
import com.rts.store.Hashing;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class PhotoPackCrossValidationTests {
    private static final Path STORE_ROOT;
    private static final Path FIXTURE_RELEASE_ROOT = Path.of("sample-projection/runtime-store/releases/rel-2026-05-06-photo-fxd-ndf-cutoff");
    private static final String RELEASE = "rel-2026-05-06-photo-fxd-ndf-cutoff";
    private static final String API_KEY = "tester-key";
    private static final ScopeKey SCOPE = new ScopeKey("tradition", "stella", "fxd-ndf-cutoff-fixing", "cutoff-fixing");
    private static final String URI_PREFIX = "rts://tradition/stella/fxd-ndf-cutoff-fixing/photo-reconstructed";
    private static final String RULE_FIXING_TIME = URI_PREFIX + "/rules/rule_fxd_ndf_fixing_time";
    private static final String RULE_FIXING_BLOCK = URI_PREFIX + "/rules/rule_fxd_ndf_fixing_block";
    private static final String RULE_QUOTED_PAIR = URI_PREFIX + "/rules/rule_fxd_ndf_fixing_quoted_currency_pair";
    private static final String RULE_PRIMARY_RATE_SOURCE = URI_PREFIX + "/rules/rule_fxd_ndf_primary_rate_source";
    private static final String RULE_SECONDARY_RATE_SOURCE = URI_PREFIX + "/rules/rule_fxd_ndf_secondary_rate_source";
    private static final String LOOKUP_CUTOFF = URI_PREFIX + "/lookups/lk_fxd_ndf_cutoff_by_pair_and_locode";
    private static final String HELPER_QUOTED_PAIR = URI_PREFIX + "/helpers/hlp_fxd_ndf_fixing_quoted_currency_pair";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    static {
        try {
            STORE_ROOT = Files.createTempDirectory("rts-photo-pack-test-");
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("rts.store-root", STORE_ROOT::toString);
    }

    @Autowired
    FileSystemProjectionStore store;

    @Autowired
    LuceneIndexService lucene;

    @Autowired
    QueryService queryService;

    @Autowired
    AgentToolService agentToolService;

    @Autowired
    AgentAnalysisService agentAnalysisService;

    @BeforeEach
    void setup() throws Exception {
        store.clearForTests();
        copyDirectory(FIXTURE_RELEASE_ROOT, store.releaseRoot(RELEASE));
        Files.writeString(STORE_ROOT.resolve("active-release.json"), """
                {
                  "active_release_id": "rel-2026-05-06-photo-fxd-ndf-cutoff",
                  "rollback_target_release_id": "rel-2026-05-06",
                  "updated_at": "2026-05-06T13:35:00Z",
                  "updated_by": "photo-pack-cross-validation-test"
                }
                """, StandardCharsets.UTF_8);
        lucene.rebuild(RELEASE);
    }

    @Test
    void packProjectionValidatesStoreAdmissionAndL2Hashes() {
        var snapshot = store.loadActiveSnapshot();
        assertThat(snapshot.manifest().releaseId()).isEqualTo(RELEASE);
        assertThat(snapshot.scopes()).hasSize(1);
        assertThat(snapshot.objectManifest()).hasSize(8);
        assertThat(snapshot.objectManifest()).extracting(ObjectManifestEntry::objectType)
                .containsExactlyInAnyOrder(
                        ObjectType.rule, ObjectType.rule, ObjectType.rule, ObjectType.rule, ObjectType.rule, ObjectType.rule,
                        ObjectType.lookup, ObjectType.helper);

        for (var entry : snapshot.objectManifest()) {
            var ref = store.getContentRef(RELEASE, entry.uri()).orElseThrow();
            var content = queryService.readContent(new ObjectContentRequest(entry.uri(), "cross_validate", null, null, "tester", API_KEY));
            assertThat(content.contentHash()).isEqualTo(entry.contentHash()).isEqualTo(ref.contentHash());
            assertThat(Hashing.sha256(content.content())).isEqualTo(entry.contentHash());
            assertThat(content.content()).contains("\"status\": \"draft-photo-reconstructed\"");
            assertThat(store.getCard(RELEASE, entry.uri()).orElseThrow().riskFlags()).contains("draft_photo_reconstructed", "not_signoff_truth");
        }
    }

    @Test
    void indexProgramValidatesPackSearchabilityAndGroundedAnswers() {
        assertFindTop("fixing time", List.of("rule"), RULE_FIXING_TIME);
        assertFindTop("primary rate source hedge source page", List.of("rule"), RULE_PRIMARY_RATE_SOURCE);
        assertFindTop("secondary rate source Cortex cutoff description", List.of("rule"), RULE_SECONDARY_RATE_SOURCE);
        assertFindTop("quoted currency pair target block quoteBasis", List.of("rule"), RULE_QUOTED_PAIR);
        assertFindTop("shared cutoff lookup reverse pair fallback", List.of("lookup"), LOOKUP_CUTOFF);
        assertFindTop("inverse quoteBasis swap currencies", List.of("helper"), HELPER_QUOTED_PAIR);

        var answer = queryService.query(new QueryRequest("fixing time 怎么生成", "tester", API_KEY, SCOPE, "default", false));
        assertThat(answer.answerType()).isEqualTo(AnswerType.answer);
        assertThat(answer.citedObjects()).contains(RULE_FIXING_TIME);
        assertThat(answer.facts().get(0).text()).contains("lk_fxd_ndf_cutoff_by_pair_and_locode");
        assertThat(answer.dependencies()).extracting("toUri").contains(LOOKUP_CUTOFF);
        assertThat(answer.warnings()).anyMatch(warning -> warning.contains("draft_photo_reconstructed"));
        assertThat(queryService.trace(answer.traceId()).orElseThrow().l2ReadUris()).containsExactly(RULE_FIXING_TIME);
    }

    @ParameterizedTest
    @MethodSource("goldenCases")
    void goldenQuestionsValidatePackThroughQueryProgram(GoldenCase golden) {
        var answer = queryService.query(new QueryRequest(golden.query(), golden.callerId(), golden.apiKey(), golden.scope(), "default", false));
        assertThat(answer.answerType()).isEqualTo(AnswerType.valueOf(golden.expectedAnswerType()));
        assertThat(answer.citedObjects()).contains(golden.expectedUri());
        if (golden.expectedDependencyUri() != null) {
            assertThat(answer.dependencies()).extracting("toUri").contains(golden.expectedDependencyUri());
        }
        assertThat(answer.facts().get(0).text()).contains(golden.expectedFactContains());
        assertThat(queryService.trace(answer.traceId()).orElseThrow().l2ReadUris()).contains(golden.expectedUri());
    }

    @Test
    void dependencyGraphLetsIndexValidatePackCompletenessAndImpactPaths() {
        var fixingTimeDeps = queryService.dependencies(new DependenciesRequest(RULE_FIXING_TIME, Direction.forward, null, 1, "cross_validate", null, "tester", API_KEY));
        assertThat(fixingTimeDeps.edges()).hasSize(1);
        assertThat(fixingTimeDeps.edges()).extracting("toUri").containsExactly(LOOKUP_CUTOFF);

        var lookupConsumers = queryService.dependencies(new DependenciesRequest(LOOKUP_CUTOFF, Direction.reverse, null, 1, "impact", null, "tester", API_KEY));
        assertThat(lookupConsumers.edges()).hasSize(3);
        assertThat(lookupConsumers.edges()).extracting("fromUri")
                .containsExactlyInAnyOrder(RULE_FIXING_TIME, RULE_PRIMARY_RATE_SOURCE, RULE_SECONDARY_RATE_SOURCE);

        var helperConsumers = queryService.dependencies(new DependenciesRequest(HELPER_QUOTED_PAIR, Direction.reverse, null, 1, "impact", null, "tester", API_KEY));
        assertThat(helperConsumers.edges()).hasSize(1);
        assertThat(helperConsumers.edges()).extracting("fromUri").containsExactly(RULE_QUOTED_PAIR);

        var blockChildren = queryService.dependencies(new DependenciesRequest(RULE_FIXING_BLOCK, Direction.forward, "rule_to_rule", 1, "cross_validate", null, "tester", API_KEY));
        assertThat(blockChildren.edges()).hasSize(5);
        assertThat(blockChildren.edges()).extracting("toUri")
                .contains(RULE_QUOTED_PAIR, RULE_FIXING_TIME, RULE_PRIMARY_RATE_SOURCE, RULE_SECONDARY_RATE_SOURCE);
    }

    @Test
    void finalAgentToolsWorkOnPhotoPackWithoutTreatingDraftAsSignoffTruth() {
        var summary = agentToolService.getScopeSummary(SCOPE, "tester", API_KEY, "default");
        assertThat(summary.objectCounts()).containsEntry("rule", 6L).containsEntry("lookup", 1L).containsEntry("helper", 1L);
        assertThat(summary.warnings()).contains("release content hash summary indicates draft material", "scope contains object risk flags; answer views must keep warnings visible");

        var impact = agentAnalysisService.analyzeImpact(new ImpactAnalysisRequest(LOOKUP_CUTOFF, null, null, SCOPE, "tester", API_KEY, "default", false, 10));
        assertThat(impact.status()).isEqualTo("candidate");
        assertThat(impact.candidates()).extracting("impactedObjectUri")
                .contains(RULE_FIXING_TIME, RULE_PRIMARY_RATE_SOURCE, RULE_SECONDARY_RATE_SOURCE);
        assertThat(impact.warnings()).contains("Impact output is a candidate analysis, not final impact approval.");

        var testPlan = agentAnalysisService.planTests(new TestPlanRequest(RULE_FIXING_TIME, SCOPE, "tester", API_KEY, "default", false, 3));
        assertThat(testPlan.status()).isEqualTo("candidate");
        assertThat(testPlan.warnings()).contains("Test plan output is a candidate set, not QA signoff.");
    }

    @Test
    void packContentValidatesIndexTargetAndSourceAnchors() {
        var snapshot = store.loadActiveSnapshot();
        var targetPaths = snapshot.objectManifest().stream()
                .filter(entry -> entry.objectType() == ObjectType.rule)
                .map(ObjectManifestEntry::targetPath)
                .toList();
        assertThat(targetPaths).contains(
                "fxd.ndf.fixing",
                "fxd.ndf.fixing.quotedCurrencyPair",
                "fxd.ndf.fixing.fixingDate",
                "fxd.ndf.fixing.primaryRateSource",
                "fxd.ndf.fixing.secondaryRateSource",
                "fxd.ndf.fixing.fixingTime");

        var sourceAnchors = snapshot.objectManifest().stream()
                .flatMap(entry -> entry.sourceAnchors().stream())
                .collect(java.util.stream.Collectors.toSet());
        assertThat(sourceAnchors).contains(
                "/FpML/trade/fxSingleLeg/nonDeliverableForward",
                "/FpML/trade/fxSingleLeg/nonDeliverableForward/fixing/fixingDate/text()",
                "/FpML/trade/fxSingleLeg/nonDeliverableForward/fixing/quotedCurrencyPair/currency1/text()",
                "/FpML/trade/fxSingleLeg/nonDeliverableForward/fixing/fixingTime/hourMinuteTime/text()");

        var exact = queryService.find(new FindRequest("target", SCOPE, List.of("rule"), List.of("fxd.ndf.fixing.fixingTime"), 5, "tester", API_KEY, "default"));
        assertThat(exact).first().extracting("uri").isEqualTo(RULE_FIXING_TIME);
        assertThat(exact).first().extracting("exactMatch").isEqualTo(true);
    }

    @Test
    void packRemainsDraftAndIsNotMistakenForSignedTruth() throws Exception {
        var reviewIndex = Files.readString(Path.of("kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split/review/review-index.yaml"), StandardCharsets.UTF_8);
        assertThat(reviewIndex).contains("signoff_status: none", "draft_not_signoff", "target_xpath_exact_suffixes");

        var snapshot = store.loadActiveSnapshot();
        assertThat(snapshot.objectCards())
                .allSatisfy(card -> assertThat(card.riskFlags()).contains("draft_photo_reconstructed", "not_signoff_truth"));

        var releaseManifest = Files.readString(store.releaseRoot(RELEASE).resolve("release-manifest.json"), StandardCharsets.UTF_8);
        assertThat(releaseManifest).contains("photo-reconstructed-draft");
    }

    private void assertFindTop(String query, List<String> objectTypes, String expectedUri) {
        var result = queryService.find(new FindRequest(query, SCOPE, objectTypes, List.of(), 5, "tester", API_KEY, "default"));
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).uri()).isEqualTo(expectedUri);
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path sourcePath : paths.sorted(Comparator.naturalOrder()).toList()) {
                Path relative = source.relativize(sourcePath);
                Path targetPath = target.resolve(relative);
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    static Stream<GoldenCase> goldenCases() throws Exception {
        var input = PhotoPackCrossValidationTests.class.getResourceAsStream("/golden/photo-pack-cross-validation.jsonl");
        assertThat(input).isNotNull();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(line -> !line.isBlank())
                    .map(line -> {
                        try {
                            return MAPPER.readValue(line, GoldenCase.class);
                        } catch (Exception ex) {
                            throw new IllegalArgumentException(ex);
                        }
                    })
                    .toList()
                    .stream();
        }
    }

    record GoldenCase(String query, String callerId, String apiKey, ScopeKey scope, String expectedAnswerType,
            String expectedUri, String expectedDependencyUri, String expectedFactContains) {}
}
