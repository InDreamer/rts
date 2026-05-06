package com.rts.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.DependencyEdge;
import com.rts.model.CoreModels.Fact;
import com.rts.model.CoreModels.L2Content;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.model.CoreModels.ScopeKey;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AnswerAssembler {
    private final ObjectMapper mapper;

    public AnswerAssembler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ServiceAnswer answer(ScopeKey scope, String releaseId, ObjectManifestEntry selected, L2Content content,
            List<DependencyEdge> dependencies, String traceId, List<String> warnings) {
        String factText = extractFactText(content.content());
        Fact fact = new Fact(factText, selected.uri(), releaseId, "l2");
        String answer = factText + " (trace: " + traceId + ")";
        return new ServiceAnswer(
                AnswerType.answer,
                scope,
                releaseId,
                List.of(fact),
                dependencyInferences(dependencies),
                List.of(),
                List.of(),
                List.of(),
                List.of(selected.uri()),
                dependencies,
                traceId,
                null,
                warnings,
                answer);
    }

    private String extractFactText(String content) {
        try {
            JsonNode node = mapper.readTree(content);
            if (node.hasNonNull("logic")) {
                return node.get("logic").asText();
            }
            if (node.hasNonNull("summary")) {
                return node.get("summary").asText();
            }
        } catch (Exception ignored) {
            // Plain text L2 is valid for Day1 samples.
        }
        return content.strip();
    }

    private List<String> dependencyInferences(List<DependencyEdge> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return List.of();
        }
        return List.of("基于 released dependency edges，该对象存在 " + dependencies.size() + " 条一跳依赖或消费者关系。");
    }
}
