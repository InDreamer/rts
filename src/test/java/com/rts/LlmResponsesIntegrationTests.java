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
            byte[] body = """
                    {"id":"resp_test","object":"response","status":"completed","output":[{"type":"message","role":"assistant","content":[{"type":"output_text","text":"Grounded rewrite only."}]}]}
                    """.getBytes(StandardCharsets.UTF_8);
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
        registry.add("rts.llm-api-key", () -> "dummy");
        registry.add("rts.llm-base-url", () -> "http://localhost:" + server.getAddress().getPort());
        registry.add("rts.llm-model", () -> "gpt-5.5");
        registry.add("rts.llm-wire-api", () -> "responses");
        registry.add("rts.llm-store-responses", () -> false);
        registry.add("rts.llm-reasoning-effort", () -> "xhigh");
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
        assertThat(answer.answer()).isEqualTo(TestProjectionFactory.ruleContent());
        assertThat(answer.facts()).extracting("uri").contains(TestProjectionFactory.RULE_URI);
        assertThat(Files.exists(STORE_ROOT.resolve("traces").resolve("llm-run-trace.jsonl"))).isTrue();
        assertThat(RESPONSE_REQUEST.get()).contains("\"model\":\"gpt-5.5\"");
        assertThat(RESPONSE_REQUEST.get()).contains("\"store\":false");
        assertThat(RESPONSE_REQUEST.get()).contains("\"reasoning\":{\"effort\":\"xhigh\"}");
        assertThat(RESPONSE_REQUEST.get()).contains("Grounded RTS service result");
    }
}
