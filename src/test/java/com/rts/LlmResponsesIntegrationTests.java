package com.rts;

import static org.assertj.core.api.Assertions.assertThat;

import com.rts.index.LuceneIndexService;
import com.rts.llm.ControlledLlmHarness;
import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.query.QueryRequests.AskRequest;
import com.rts.store.FileSystemProjectionStore;
import com.rts.store.StoreContracts.ProjectionSnapshot;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class LlmResponsesIntegrationTests {
    private static final Path STORE_ROOT;
    private static final AtomicReference<String> RESPONSE_REQUEST = new AtomicReference<>();
    private static HttpServer server;

    static {
        try {
            STORE_ROOT = Files.createTempDirectory("rts-llm-responses-");
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @BeforeAll
    static void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/responses", exchange -> {
            RESPONSE_REQUEST.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String text = "{\"analysis_text\":\"payment.amount is generated from src.amount, rounded with helper_rounding, and currency-normalized using lookup_currency.\","
                    + "\"claims\":[\"payment.amount is generated from src.amount\"],"
                    + "\"inferences\":[\"Use dependency evidence to review lookup/helper impact.\"],"
                    + "\"unknowns\":[\"No runtime transaction sample was provided.\"],"
                    + "\"candidates\":[\"Review tests covering lookup_currency and helper_rounding.\"],"
                    + "\"warnings\":[\"Draft claims are not final truth authority.\"],"
                    + "\"citation_intents\":[\"" + TestProjectionFactory.RULE_URI + "\"],"
                    + "\"tool_needs\":[]}";
            byte[] body = ("""
                    {"id":"resp_test","object":"response","status":"completed","output":[{"type":"message","role":"assistant","content":[{"type":"output_text","text":%s}]}]}
                    """.formatted(jsonString(text))).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("rts.store-root", STORE_ROOT::toString);
        registry.add("rts.llm-enabled", () -> true);
        registry.add("rts.tool-orchestrator-enabled", () -> true);
        registry.add("rts.llm-api-key", () -> "dummy");
        registry.add("rts.llm-base-url", () -> "http://localhost:" + server.getAddress().getPort());
        registry.add("rts.llm-model", () -> "gpt-5.5");
        registry.add("rts.llm-wire-api", () -> "responses");
        registry.add("rts.llm-store-responses", () -> false);
        registry.add("rts.llm-reasoning-effort", () -> "low");
        registry.add("rts.llm-debug-raw-output", () -> true);
    }

    @Autowired
    FileSystemProjectionStore store;

    @Autowired
    LuceneIndexService lucene;

    @Autowired
    ControlledLlmHarness harness;

    ScopeKey stella = new ScopeKey("tradition", "stella", "payments", "core");

    @BeforeEach
    void setup() throws Exception {
        RESPONSE_REQUEST.set(null);
        store.clearForTests();
        ProjectionSnapshot snapshot = TestProjectionFactory.valid();
        ProjectionTestSupport.writeL2(store, snapshot);
        store.ingest(snapshot);
        store.activate(snapshot.activeRelease());
        lucene.rebuild(TestProjectionFactory.RELEASE);
    }

    @Test
    void askUsesSpringWiredResponsesClientAndReturnsGroundedAnswer() {
        var answer = harness.ask(new AskRequest("payment amount target field", "tester", TestProjectionFactory.TESTER_KEY, stella, "default", 6));

        assertThat(answer.answerType()).isEqualTo(AnswerType.answer);
        assertThat(answer.answer()).isEqualTo("payment.amount is generated from src.amount, rounded with helper_rounding, and currency-normalized using lookup_currency.");
        assertThat(answer.facts()).extracting("uri").contains(TestProjectionFactory.RULE_URI);
        assertThat(answer.inferences()).contains("Use dependency evidence to review lookup/helper impact.");
        assertThat(answer.unknowns()).contains("No runtime transaction sample was provided.");
        assertThat(answer.candidateSuggestions()).contains("Review tests covering lookup_currency and helper_rounding.");
        assertThat(Files.exists(STORE_ROOT.resolve("traces").resolve("llm-run-trace.jsonl"))).isTrue();
        assertThat(RESPONSE_REQUEST.get()).contains("\"model\":\"gpt-5.5\"");
        assertThat(RESPONSE_REQUEST.get()).contains("\"store\":false");
        assertThat(RESPONSE_REQUEST.get()).contains("\"reasoning\":{\"effort\":\"low\"}");
        assertThat(RESPONSE_REQUEST.get()).contains("Grounded RTS service result");
        assertThat(RESPONSE_REQUEST.get()).contains("controlled analysis draft generator");
        assertThat(RESPONSE_REQUEST.get()).contains("\"type\":\"json_schema\"");
        assertThat(RESPONSE_REQUEST.get()).contains("analysis_text");
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
