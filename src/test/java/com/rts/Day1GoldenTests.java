package com.rts;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rts.index.LuceneIndexService;
import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.query.QueryRequests.QueryRequest;
import com.rts.query.QueryService;
import com.rts.store.FileSystemProjectionStore;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("golden")
@SpringBootTest
class Day1GoldenTests {
    private static final Path STORE_ROOT;
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    static {
        try {
            STORE_ROOT = Files.createTempDirectory("rts-day1-golden-");
        } catch (Exception ex) {
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

    @BeforeEach
    void setup() throws Exception {
        store.clearForTests();
        var snapshot = TestProjectionFactory.valid();
        ProjectionTestSupport.writeL2(store, snapshot);
        store.ingest(snapshot);
        store.activate(snapshot.activeRelease());
        lucene.rebuild(TestProjectionFactory.RELEASE);
    }

    @ParameterizedTest
    @MethodSource("goldenCases")
    void goldenAndRefusalSetPasses(GoldenCase golden) {
        var answer = queryService.query(new QueryRequest(golden.query(), golden.callerId(), golden.apiKey(), golden.scope(), "default", false));
        assertThat(answer.answerType()).isEqualTo(AnswerType.valueOf(golden.expectedAnswerType()));
        if (golden.expectedUri() != null) {
            assertThat(answer.citedObjects()).contains(golden.expectedUri());
        }
        if (golden.expectedRefusal() != null) {
            assertThat(answer.refusal().reason()).isEqualTo(RefusalReason.valueOf(golden.expectedRefusal()));
        }
        assertThat(answer.traceId()).startsWith("trace-");
    }

    static Stream<GoldenCase> goldenCases() throws Exception {
        var input = Day1GoldenTests.class.getResourceAsStream("/golden/day1-golden.jsonl");
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

    record GoldenCase(String query, String callerId, String apiKey, ScopeKey scope, String expectedAnswerType, String expectedUri, String expectedRefusal) {}
}
