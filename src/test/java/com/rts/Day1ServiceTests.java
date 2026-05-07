package com.rts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rts.index.LuceneIndexService;
import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.CallerProfile;
import com.rts.model.CoreModels.Direction;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.api.IngestController;
import com.rts.api.ApiExceptionHandler;
import com.rts.llm.LlmContracts.LlmClient;
import com.rts.llm.LlmContracts.LlmDraft;
import com.rts.llm.LlmContracts.ToolContext;
import com.rts.llm.LlmContracts.ToolResult;
import com.rts.llm.OpenAiCompatibleLlmClient;
import com.rts.query.QueryRequests.AskRequest;
import com.rts.query.QueryRequests.DependenciesRequest;
import com.rts.query.QueryRequests.FindRequest;
import com.rts.query.QueryRequests.ObjectGetRequest;
import com.rts.query.QueryRequests.ObjectContentRequest;
import com.rts.query.QueryRequests.QueryRequest;
import com.rts.query.QueryService;
import com.rts.query.FinalAnswerValidator;
import com.rts.query.QueryRefusalException;
import com.rts.llm.ControlledLlmHarness;
import com.rts.config.RtsProperties;
import com.rts.model.CoreModels.Fact;
import com.rts.model.CoreModels.LlmRunTrace;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.store.Hashing;
import com.rts.store.StoreContracts.TraceStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;
import com.sun.net.httpserver.HttpServer;
import com.rts.store.FileSystemProjectionStore;
import com.rts.store.ProjectionValidationException;
import com.rts.store.StoreContracts.ProjectionSnapshot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class Day1ServiceTests {
    private static final Path STORE_ROOT;

    static {
        try {
            STORE_ROOT = Files.createTempDirectory("rts-day1-test-");
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

    ScopeKey stella = new ScopeKey("tradition", "stella", "payments", "core");
    ScopeKey aurora = new ScopeKey("tradition", "aurora", "payments", "core");

    @BeforeEach
    void setup() throws Exception {
        store.clearForTests();
        ProjectionSnapshot snapshot = TestProjectionFactory.valid();
        ProjectionTestSupport.writeL2(store, snapshot);
        store.ingest(snapshot);
        store.activate(snapshot.activeRelease());
        lucene.rebuild(TestProjectionFactory.RELEASE);
    }

    @Test
    void validProjectionLoadsAndWritesFilesystemProjectionStore() {
        var snapshot = store.loadActiveSnapshot();
        assertThat(snapshot.manifest().releaseId()).isEqualTo(TestProjectionFactory.RELEASE);
        assertThat(Files.exists(STORE_ROOT.resolve("active-release.json"))).isTrue();
        assertThat(Files.exists(STORE_ROOT.resolve("releases").resolve(TestProjectionFactory.RELEASE).resolve("object-manifest.jsonl"))).isTrue();
        assertThat(Files.exists(STORE_ROOT.resolve("traces"))).isTrue();
    }

    @Test
    void invalidSchemaRejectsProjection() {
        ProjectionSnapshot invalid = new ProjectionSnapshot(
                TestProjectionFactory.valid().activeRelease(),
                TestProjectionFactory.manifest(0, "future-v9"),
                TestProjectionFactory.valid().scopes(),
                TestProjectionFactory.valid().objectManifest(),
                TestProjectionFactory.valid().objectCards(),
                TestProjectionFactory.valid().dependencyEdges(),
                TestProjectionFactory.valid().contentRefs(),
                TestProjectionFactory.valid().callerProfiles());
        assertThatThrownBy(() -> store.ingest(invalid)).isInstanceOf(ProjectionValidationException.class);
    }

    @Test
    void dependencyNotReleasedRejectsRelease() {
        ProjectionSnapshot base = TestProjectionFactory.valid();
        ProjectionSnapshot invalid = new ProjectionSnapshot(base.activeRelease(), base.manifest(), base.scopes(),
                base.objectManifest(), base.objectCards(),
                java.util.List.of(new com.rts.model.CoreModels.DependencyEdge(TestProjectionFactory.RELEASE, TestProjectionFactory.RULE_URI, "rts://missing", "rule_to_lookup", true, "forward", "test")),
                base.contentRefs(), base.callerProfiles());
        assertThatThrownBy(() -> store.ingest(invalid)).isInstanceOf(ProjectionValidationException.class);
    }

    @Test
    void deterministicLookupFindsRuleByTargetAndUri() {
        var byTarget = queryService.find(new FindRequest("target field payment.amount", stella, java.util.List.of(), java.util.List.of("payment.amount"), 5, "tester", TestProjectionFactory.TESTER_KEY, "default"));
        assertThat(byTarget).extracting("uri").contains(TestProjectionFactory.RULE_URI);
        var byUri = queryService.find(new FindRequest(TestProjectionFactory.RULE_URI, stella, java.util.List.of(), java.util.List.of(TestProjectionFactory.RULE_URI), 5, "tester", TestProjectionFactory.TESTER_KEY, "default"));
        assertThat(byUri.get(0).exactMatch()).isTrue();
    }

    @Test
    void luceneBm25FindsWithinScopeAndDoesNotCrossProduct() {
        var result = queryService.find(new FindRequest("payment amount target field", stella, java.util.List.of("rule"), java.util.List.of(), 10, "tester", TestProjectionFactory.TESTER_KEY, "default"));
        assertThat(result).extracting("uri").contains(TestProjectionFactory.RULE_URI);
        assertThat(result).extracting("uri").doesNotContain(TestProjectionFactory.OTHER_RULE_URI);
    }

    @Test
    void callerCannotSeeDisallowedProduct() {
        var answer = queryService.query(new QueryRequest("target field payment.amount", "tester", TestProjectionFactory.TESTER_KEY, aurora, "default", false));
        assertThat(answer.answerType()).isEqualTo(AnswerType.refusal);
        assertThat(answer.refusal().reason()).isEqualTo(RefusalReason.unauthorized_scope);
    }

    @Test
    void callerIdCannotBeSpoofedWithoutMatchingApiKey() {
        var answer = queryService.query(new QueryRequest("target field payment.amount", "admin", TestProjectionFactory.TESTER_KEY, aurora, "default", false));
        assertThat(answer.answerType()).isEqualTo(AnswerType.refusal);
        assertThat(answer.refusal().reason()).isEqualTo(RefusalReason.unauthorized_scope);
    }

    @Test
    void directObjectL2AndDependencyToolsEnforceScopePermission() {
        assertThatThrownBy(() -> queryService.getObject(new ObjectGetRequest(TestProjectionFactory.OTHER_RULE_URI, null, null, "tester", TestProjectionFactory.TESTER_KEY)))
                .isInstanceOf(QueryRefusalException.class);
        assertThatThrownBy(() -> queryService.readContent(new ObjectContentRequest(TestProjectionFactory.OTHER_RULE_URI, "answer", null, null, "tester", TestProjectionFactory.TESTER_KEY)))
                .isInstanceOf(QueryRefusalException.class);
        assertThatThrownBy(() -> queryService.dependencies(new DependenciesRequest(TestProjectionFactory.OTHER_RULE_URI, Direction.forward, null, 1, "answer", null, "tester", TestProjectionFactory.TESTER_KEY)))
                .isInstanceOf(QueryRefusalException.class);
    }

    @Test
    void directToolsRejectNonActiveRelease() {
        assertThatThrownBy(() -> queryService.readContent(new ObjectContentRequest(TestProjectionFactory.RULE_URI, "answer", "old-release", null, "tester", TestProjectionFactory.TESTER_KEY)))
                .isInstanceOf(QueryRefusalException.class);
        assertThatThrownBy(() -> queryService.getObject(new ObjectGetRequest(TestProjectionFactory.RULE_URI, "old-release", null, "tester", TestProjectionFactory.TESTER_KEY)))
                .isInstanceOf(QueryRefusalException.class);
        assertThatThrownBy(() -> queryService.dependencies(new DependenciesRequest(TestProjectionFactory.RULE_URI, Direction.forward, null, 1, "answer", "old-release", "tester", TestProjectionFactory.TESTER_KEY)))
                .isInstanceOf(QueryRefusalException.class);
    }

    @Test
    void directL2ReadWritesTraceWhenTraceIdProvided() {
        String traceId = "trace-direct-l2";
        queryService.readContent(new ObjectContentRequest(TestProjectionFactory.RULE_URI, "answer", null, traceId, "tester", TestProjectionFactory.TESTER_KEY));
        assertThat(queryService.trace(traceId)).isPresent();
        assertThat(queryService.trace(traceId).orElseThrow().l2ReadUris()).contains(TestProjectionFactory.RULE_URI);
    }

    @Test
    void l2ReadChecksHashAndBlocksArbitraryPath() {
        var content = queryService.readContent(new ObjectContentRequest(TestProjectionFactory.RULE_URI, "answer", null, null, "tester", TestProjectionFactory.TESTER_KEY));
        assertThat(content.contentHash()).isEqualTo(com.rts.store.Hashing.sha256(TestProjectionFactory.ruleContent()));

        ProjectionSnapshot base = TestProjectionFactory.valid();
        ProjectionSnapshot invalid = new ProjectionSnapshot(base.activeRelease(), base.manifest(), base.scopes(), base.objectManifest(), base.objectCards(), base.dependencyEdges(),
                java.util.List.of(TestProjectionFactory.ref(TestProjectionFactory.RULE_URI, com.rts.store.Hashing.sha256(TestProjectionFactory.ruleContent()), "../secret.json")),
                base.callerProfiles());
        assertThatThrownBy(() -> store.ingest(invalid)).isInstanceOf(ProjectionValidationException.class);
    }

    @Test
    void dependencyTraversalSupportsForwardAndReverse() {
        var forward = queryService.dependencies(new DependenciesRequest(TestProjectionFactory.RULE_URI, Direction.forward, null, 1, "explain", null, "tester", TestProjectionFactory.TESTER_KEY));
        assertThat(forward.edges()).hasSize(2);
        var reverse = queryService.dependencies(new DependenciesRequest(TestProjectionFactory.LOOKUP_URI, Direction.reverse, null, 1, "impact", null, "tester", TestProjectionFactory.TESTER_KEY));
        assertThat(reverse.edges()).hasSize(1);
        assertThat(reverse.objects()).extracting("uri").contains(TestProjectionFactory.RULE_URI);
    }

    @Test
    void dependencyTraversalRefusesCrossScopeResults() {
        var base = TestProjectionFactory.valid();
        var crossScope = new com.rts.store.StoreContracts.ProjectionSnapshot(base.activeRelease(), base.manifest(), base.scopes(),
                base.objectManifest(), base.objectCards(),
                java.util.List.of(new com.rts.model.CoreModels.DependencyEdge(TestProjectionFactory.RELEASE, TestProjectionFactory.RULE_URI, TestProjectionFactory.OTHER_RULE_URI, "rule_to_rule", true, "forward", "test")),
                base.contentRefs(), base.callerProfiles());
        store.ingest(crossScope);
        assertThatThrownBy(() -> queryService.dependencies(new DependenciesRequest(TestProjectionFactory.RULE_URI, Direction.forward, null, 1, "answer", null, "tester", TestProjectionFactory.TESTER_KEY)))
                .isInstanceOf(QueryRefusalException.class);
    }

    @Test
    void dependencyAuthAppliesToObjectGetQueryAndAsk() {
        var base = TestProjectionFactory.valid();
        var crossScope = new com.rts.store.StoreContracts.ProjectionSnapshot(base.activeRelease(), base.manifest(), base.scopes(),
                base.objectManifest(), base.objectCards(),
                java.util.List.of(new com.rts.model.CoreModels.DependencyEdge(TestProjectionFactory.RELEASE, TestProjectionFactory.RULE_URI, TestProjectionFactory.OTHER_RULE_URI, "rule_to_rule", true, "forward", "test")),
                base.contentRefs(), base.callerProfiles());
        store.ingest(crossScope);

        assertThatThrownBy(() -> queryService.getObject(new ObjectGetRequest(TestProjectionFactory.RULE_URI, null, null, "tester", TestProjectionFactory.TESTER_KEY)))
                .isInstanceOf(QueryRefusalException.class);

        var answer = queryService.query(new QueryRequest("target field payment.amount", "tester", TestProjectionFactory.TESTER_KEY, stella, "default", false));
        assertThat(answer.answerType()).isEqualTo(AnswerType.refusal);
        assertThat(answer.refusal().reason()).isEqualTo(RefusalReason.unauthorized_scope);

        var askAnswer = new ControlledLlmHarness(queryService, new com.rts.llm.DisabledLlmClient(), new FinalAnswerValidator(), new TraceStore() {
            @Override public void appendQueryTrace(com.rts.model.CoreModels.TraceRecord trace) {}
            @Override public void appendLlmRunTrace(LlmRunTrace trace) {}
            @Override public java.util.Optional<com.rts.model.CoreModels.TraceRecord> getQueryTrace(String traceId) { return java.util.Optional.empty(); }
        }, new RtsProperties()).ask(new AskRequest("payment amount target field", "tester", TestProjectionFactory.TESTER_KEY, stella, "default", 6));
        assertThat(askAnswer.answerType()).isEqualTo(AnswerType.refusal);
        assertThat(askAnswer.refusal().reason()).isEqualTo(RefusalReason.unauthorized_scope);
    }

    @Test
    void projectionAdmissionRejectsDuplicateUriAndDuplicateTargetRule() {
        var base = TestProjectionFactory.valid();
        var duplicateUri = new com.rts.store.StoreContracts.ProjectionSnapshot(base.activeRelease(), base.manifest(), base.scopes(),
                java.util.List.of(base.objectManifest().get(0), base.objectManifest().get(0)), base.objectCards(), base.dependencyEdges(), base.contentRefs(), base.callerProfiles());
        assertThatThrownBy(() -> store.ingest(duplicateUri)).isInstanceOf(ProjectionValidationException.class);

        var duplicateTarget = TestProjectionFactory.object(
                "rts://tradition/stella/payments/day1/rules/rule_amount_dup",
                "rule_amount_dup",
                com.rts.model.CoreModels.ObjectType.rule,
                "tradition",
                "stella",
                "payments",
                "core",
                "payment.amount",
                java.util.List.of("src.amount"),
                Hashing.sha256(TestProjectionFactory.ruleContent()),
                "rules/rule_amount.json");
        var duplicateTargetSnapshot = new com.rts.store.StoreContracts.ProjectionSnapshot(base.activeRelease(), base.manifest(), base.scopes(),
                java.util.List.of(base.objectManifest().get(0), duplicateTarget),
                java.util.List.of(base.objectCards().get(0), TestProjectionFactory.card(duplicateTarget.uri(), duplicateTarget.objectType(), "duplicate target")),
                java.util.List.of(),
                java.util.List.of(base.contentRefs().get(0), TestProjectionFactory.ref(duplicateTarget.uri(), Hashing.sha256(TestProjectionFactory.ruleContent()), "rules/rule_amount.json")),
                base.callerProfiles());
        assertThatThrownBy(() -> store.ingest(duplicateTargetSnapshot)).isInstanceOf(ProjectionValidationException.class);
    }

    @Test
    void queryReturnsGroundedAnswerTraceAndCitations() {
        var answer = queryService.query(new QueryRequest("这个 target field payment.amount 怎么生成？", "tester", TestProjectionFactory.TESTER_KEY, stella, "default", false));
        assertThat(answer.answerType()).isEqualTo(AnswerType.answer);
        assertThat(answer.facts()).hasSize(1);
        assertThat(answer.citedObjects()).contains(TestProjectionFactory.RULE_URI);
        assertThat(answer.traceId()).startsWith("trace-");
        assertThat(queryService.trace(answer.traceId())).isPresent();
        assertThat(queryService.trace(answer.traceId()).orElseThrow().l2ReadUris()).contains(TestProjectionFactory.RULE_URI);
    }

    @Test
    void missingScopeRefusesWithClarification() {
        var answer = queryService.query(new QueryRequest("payment amount", "tester", TestProjectionFactory.TESTER_KEY, null, "default", false));
        assertThat(answer.answerType()).isEqualTo(AnswerType.refusal);
        assertThat(answer.refusal().reason()).isEqualTo(RefusalReason.scope_unclear);
    }

    @Test
    void controlledLlmHarnessUsesToolsAndReturnsGroundedAnswer(@Autowired com.rts.llm.ControlledLlmHarness harness) {
        var answer = harness.ask(new AskRequest("payment amount target field", "tester", TestProjectionFactory.TESTER_KEY, stella, "default", 6));
        assertThat(answer.answerType()).isEqualTo(AnswerType.answer);
        assertThat(answer.facts()).isNotEmpty();
        assertThat(Files.exists(STORE_ROOT.resolve("traces").resolve("llm-run-trace.jsonl"))).isTrue();
        assertThat(queryService.trace(answer.traceId())).isPresent();
    }

    @Test
    void controlledLlmHarnessRefusesWhenScopeUnresolved(@Autowired com.rts.llm.ControlledLlmHarness harness) {
        var answer = harness.ask(new AskRequest("payment amount target field", "tester", TestProjectionFactory.TESTER_KEY, null, "default", 6));
        assertThat(answer.answerType()).isEqualTo(AnswerType.refusal);
        assertThat(answer.refusal().reason()).isEqualTo(RefusalReason.scope_unclear);
    }

    @Test
    void controlledLlmHarnessEnforcesToolBudget(@Autowired com.rts.llm.ControlledLlmHarness harness) {
        var answer = harness.ask(new AskRequest("payment amount target field", "tester", TestProjectionFactory.TESTER_KEY, stella, "default", 1));
        assertThat(answer.answerType()).isEqualTo(AnswerType.refusal);
        assertThat(answer.refusal().reason()).isEqualTo(RefusalReason.tool_budget_exhausted);
    }

    @Test
    void llmValidationUsesActualL2ReadsNotForgedCitations() {
        LlmClient malicious = new LlmClient() {
            @Override
            public LlmDraft draftAnswer(AskRequest request, ToolContext toolContext) {
                ServiceAnswer answer = new ServiceAnswer(AnswerType.answer, stella, TestProjectionFactory.RELEASE,
                        java.util.List.of(new Fact("forged", TestProjectionFactory.RULE_URI, TestProjectionFactory.RELEASE, "l2")),
                        java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(TestProjectionFactory.RULE_URI),
                        java.util.List.of(), "trace-forged", null, java.util.List.of(), "forged");
                return new LlmDraft("forged", java.util.List.of(), answer);
            }
        };
        TraceStore traceStore = new TraceStore() {
            @Override public void appendQueryTrace(com.rts.model.CoreModels.TraceRecord trace) {}
            @Override public void appendLlmRunTrace(LlmRunTrace trace) {}
            @Override public java.util.Optional<com.rts.model.CoreModels.TraceRecord> getQueryTrace(String traceId) { return java.util.Optional.empty(); }
        };
        ControlledLlmHarness harness = new ControlledLlmHarness(queryService, malicious, new FinalAnswerValidator(), traceStore, new RtsProperties());
        var answer = harness.ask(new AskRequest("malicious", "tester", TestProjectionFactory.TESTER_KEY, stella, "default", 6));
        assertThat(answer.answerType()).isEqualTo(AnswerType.refusal);
        assertThat(answer.refusal().reason()).isEqualTo(RefusalReason.unsupported_claim);
    }

    @Test
    void adminIngestRequiresAdminApiKey(@Autowired IngestController ingestController) {
        assertThatThrownBy(() -> ingestController.ingest(TestProjectionFactory.valid(), "admin", TestProjectionFactory.TESTER_KEY))
                .isInstanceOf(QueryRefusalException.class);
        assertThat(ingestController.ingest(TestProjectionFactory.valid(), "admin", TestProjectionFactory.ADMIN_KEY).status()).isEqualTo("ingested");
    }

    @Test
    void adminIngestAuthenticatesAgainstCurrentReleaseForNewRelease(@Autowired IngestController ingestController) {
        var base = TestProjectionFactory.valid();
        var nextManifest = new com.rts.model.CoreModels.ReleaseManifest("rel-next", base.manifest().canonicalRevision(), base.manifest().projectionSchemaVersion(),
                base.manifest().cardSchemaVersion(), base.manifest().summarySchemaVersion(), base.manifest().activationState(), base.manifest().generatedAt(),
                base.manifest().releasedAt(), base.manifest().rollbackTargetReleaseId(), base.manifest().contentHashSummary(), base.manifest().blockingIssuesCount(),
                base.manifest().createdAt());
        var next = new com.rts.store.StoreContracts.ProjectionSnapshot(
                new com.rts.model.CoreModels.ActiveReleasePointer("rel-next", TestProjectionFactory.RELEASE, Instant.now(), "admin"),
                nextManifest,
                base.scopes().stream().map(scope -> new com.rts.model.CoreModels.ScopeRecord("rel-next", scope.channel(), scope.product(), scope.pack(), scope.domain(), scope.activeFlag(), scope.permissionBoundary(), scope.precedencePolicy(), scope.deprecatedFlag(), scope.supersededBy())).toList(),
                base.objectManifest().stream().map(entry -> new com.rts.model.CoreModels.ObjectManifestEntry(entry.uri(), "rel-next", entry.objectId(), entry.objectType(), entry.channel(), entry.product(), entry.pack(), entry.domain(), entry.targetPath(), entry.sourceAnchors(), entry.contentHash(), entry.cardRef(), entry.contentRef(), entry.schemaVersion(), entry.state())).toList(),
                base.objectCards().stream().map(card -> new com.rts.model.CoreModels.ObjectCard(card.uri(), "rel-next", card.objectType(), card.cardJson(), card.searchText(), card.riskFlags(), card.applicability(), card.notApplicable(), card.overrideRefs(), card.supersessionRefs())).toList(),
                base.dependencyEdges().stream().map(edge -> new com.rts.model.CoreModels.DependencyEdge("rel-next", edge.fromUri(), edge.toUri(), edge.edgeType(), edge.requiredFlag(), edge.direction(), edge.traversalPurpose())).toList(),
                base.contentRefs().stream().map(ref -> new com.rts.model.CoreModels.ContentRef(ref.uri(), "rel-next", ref.contentUri(), ref.storageKind(), ref.storageRef(), ref.contentHash(), ref.contentType(), ref.schemaVersion())).toList(),
                base.callerProfiles());
        var l2 = store.releaseRoot("rel-next").resolve("l2");
        try {
            Files.createDirectories(l2);
            java.nio.file.Files.walk(store.releaseRoot(TestProjectionFactory.RELEASE).resolve("l2")).forEach(path -> {
                try {
                    var relative = store.releaseRoot(TestProjectionFactory.RELEASE).resolve("l2").relativize(path);
                    var target = l2.resolve(relative);
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(target);
                    } else {
                        Files.copy(path, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        assertThat(ingestController.ingest(next, "admin", TestProjectionFactory.ADMIN_KEY).releaseId()).isEqualTo("rel-next");
    }

    @Test
    void ingestDoesNotSwitchActiveBeforeExplicitActivation() throws Exception {
        var base = TestProjectionFactory.valid();
        var activeBefore = store.loadActiveSnapshot().manifest().releaseId();
        store.ingest(base);
        assertThat(store.loadActiveSnapshot().manifest().releaseId()).isEqualTo(activeBefore);
        store.activate(base.activeRelease());
        assertThat(store.loadActiveSnapshot().manifest().releaseId()).isEqualTo(base.activeRelease().activeReleaseId());
    }

    @Test
    void adminIngestCanBootstrapEmptyStoreWithLocalAdmin(@Autowired IngestController ingestController, @Autowired RtsProperties properties) throws Exception {
        store.clearForTests();
        properties.setAdminApiKeyHash(Hashing.sha256(TestProjectionFactory.ADMIN_KEY));
        ProjectionSnapshot snapshot = TestProjectionFactory.valid();
        ProjectionTestSupport.writeL2(store, snapshot);
        assertThat(ingestController.ingest(snapshot, "admin", TestProjectionFactory.ADMIN_KEY).releaseId()).isEqualTo(TestProjectionFactory.RELEASE);
    }

    @Test
    void traceReadRequiresCallerPermission() {
        var answer = queryService.query(new QueryRequest("target field payment.amount", "tester", TestProjectionFactory.TESTER_KEY, stella, "default", false));
        assertThat(queryService.trace(answer.traceId(), "tester", TestProjectionFactory.TESTER_KEY)).isPresent();
        assertThatThrownBy(() -> queryService.trace(answer.traceId(), "admin", TestProjectionFactory.TESTER_KEY))
                .isInstanceOf(QueryRefusalException.class);
    }

    @Test
    void apiExceptionHandlerReturnsStructuredRefusal() {
        var response = new ApiExceptionHandler().queryRefusal(new QueryRefusalException(RefusalReason.unauthorized_scope, "blocked"));
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().answerType()).isEqualTo(AnswerType.refusal);
        assertThat(response.getBody().refusal().reason()).isEqualTo(RefusalReason.unauthorized_scope);
    }

    @Test
    void openAiAdapterUsesResponsesEndpointAndNeverUsesFreeformModelTextAsFactAnswer() {
        HttpServer server;
        try {
            server = HttpServer.create(new java.net.InetSocketAddress(0), 0);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/responses", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            byte[] body = ("{\"id\":\"resp_test\",\"object\":\"response\",\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\""
                    + TestProjectionFactory.ruleContent().replace("\"", "\\\"")
                    + " rts://tradition/stella/payments/day1/rules/rule_amount trace-llm-grounded. Unsupported new business claim.\"}]}]}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        RtsProperties properties = new RtsProperties();
        properties.setLlmEnabled(true);
        properties.setLlmApiKey("dummy");
        properties.setLlmModel("dummy");
        properties.setLlmBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.setLlmWireApi("responses");
        properties.setLlmStoreResponses(false);
        properties.setLlmReasoningEffort("xhigh");
        try {
            OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(properties, new ObjectMapper(), RestClient.builder());
            LlmDraft draft = client.draftAnswer(new AskRequest("payment amount", "tester", TestProjectionFactory.TESTER_KEY, stella, "default", 6), (toolName, input) -> {
                Object output = switch (toolName) {
                    case "resolve_scope" -> new com.rts.model.CoreModels.QueryPlan("rule_lookup", stella, java.util.List.of("payment.amount"), "released", java.util.List.of(), false, null, RefusalReason.object_not_found);
                    case "find_objects" -> java.util.List.of(new com.rts.model.CoreModels.CandidateObject(TestProjectionFactory.RULE_URI, com.rts.model.CoreModels.ObjectType.rule, 1, java.util.List.of("test"), true));
                    case "get_object_card" -> new QueryService.ObjectEnvelope(TestProjectionFactory.valid().objectManifest().get(0), TestProjectionFactory.valid().objectCards().get(0), new com.rts.model.CoreModels.DependencyResult(java.util.List.of(), java.util.List.of(), false));
                    case "read_object_l2" -> new com.rts.model.CoreModels.L2Content(TestProjectionFactory.RULE_URI, TestProjectionFactory.RELEASE, Hashing.sha256(TestProjectionFactory.ruleContent()), "application/json", TestProjectionFactory.ruleContent());
                    case "get_dependencies" -> new com.rts.model.CoreModels.DependencyResult(java.util.List.of(), java.util.List.of(), false);
                    default -> throw new IllegalArgumentException(toolName);
                };
                return new ToolResult(toolName, output);
            });
            assertThat(draft.groundedAnswer().answer()).isEqualTo(TestProjectionFactory.ruleContent());
            assertThat(draft.toolCalls()).contains("responses");
            assertThat(requestBody.get()).contains("\"model\":\"dummy\"");
            assertThat(requestBody.get()).contains("\"store\":false");
            assertThat(requestBody.get()).contains("\"max_output_tokens\":600");
            assertThat(requestBody.get()).contains("\"reasoning\":{\"effort\":\"xhigh\"}");
            assertThat(requestBody.get()).contains("Grounded RTS service result");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void missingL2RejectsProjectionAdmission() {
        store.clearForTests();
        assertThatThrownBy(() -> store.ingest(TestProjectionFactory.missingL2()))
                .isInstanceOf(ProjectionValidationException.class);
    }

    @Test
    void missingL2AfterAdmissionCausesRefusal() throws Exception {
        Files.delete(STORE_ROOT.resolve("releases").resolve(TestProjectionFactory.RELEASE).resolve("l2/rules/rule_amount.json"));
        var answer = queryService.query(new QueryRequest("target field payment.amount", "tester", TestProjectionFactory.TESTER_KEY, stella, "default", false));
        assertThat(answer.answerType()).isEqualTo(AnswerType.refusal);
        assertThat(answer.refusal().reason()).isEqualTo(RefusalReason.manifest_invalid);
    }

    @Test
    void finalAnswerValidationCatchesUnsupportedClaim(@Autowired FinalAnswerValidator validator) {
        var answer = new com.rts.model.CoreModels.ServiceAnswer(AnswerType.answer, stella, TestProjectionFactory.RELEASE,
                java.util.List.of(new com.rts.model.CoreModels.Fact("unsupported", TestProjectionFactory.RULE_URI, TestProjectionFactory.RELEASE, "l2")),
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(),
                "trace-test", null, java.util.List.of(), "unsupported");
        assertThatThrownBy(() -> validator.validate(answer, java.util.Set.of()))
                .isInstanceOf(QueryRefusalException.class);
    }
}
