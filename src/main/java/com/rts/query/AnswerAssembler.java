package com.rts.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.DependencyEdge;
import com.rts.model.CoreModels.Fact;
import com.rts.model.CoreModels.L2Content;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.model.CoreModels.ObjectType;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.model.CoreModels.ScopeKey;
import java.util.ArrayList;
import java.util.Iterator;
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
        String factText = extractFactText(content.content(), selected.objectType());
        Fact fact = new Fact(factText, selected.uri(), releaseId, "l2:" + content.contentHash());
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

    private String extractFactText(String content, ObjectType objectType) {
        try {
            JsonNode node = mapper.readTree(content);
            List<String> parts = new ArrayList<>();
            JsonNode logic = node.get("logic");
            if (logic != null && logic.isObject()) {
                addText(parts, "Logic", logic.get("summary"));
                if (logic.has("pipeline")) {
                    parts.add("Pipeline: " + compact(logic.get("pipeline")));
                }
            } else if (logic != null && logic.isTextual()) {
                parts.add("Logic: " + logic.asText());
            }
            if (objectType == ObjectType.rule && node.has("target")) {
                JsonNode target = node.get("target");
                addText(parts, "Target", target.get("path"));
                if (target.has("emits")) {
                    parts.add("Emits: " + compact(target.get("emits")));
                } else if (target.has("children")) {
                    parts.add("Children: " + compact(target.get("children")));
                }
            }
            if ((objectType == ObjectType.lookup || objectType == ObjectType.helper) && node.has("output")) {
                parts.add("Output fields: " + compact(node.get("output").get("fields")));
            }
            if (node.has("dependencies")) {
                parts.add("Dependencies: " + compact(node.get("dependencies")));
            }
            if (node.has("examples")) {
                parts.add("Examples: " + compact(node.get("examples")));
            }
            if (!parts.isEmpty()) {
                return String.join(" ", parts);
            }
            if (node.hasNonNull("summary")) {
                return node.get("summary").asText();
            }
        } catch (Exception ignored) {
            // Plain text L2 is valid for Day1 samples.
        }
        return content.strip();
    }

    private void addText(List<String> parts, String label, JsonNode value) {
        if (value != null && !value.isNull()) {
            parts.add(label + ": " + value.asText());
        }
    }

    private String compact(JsonNode node) {
        if (node == null || node.isNull()) {
            return "[]";
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            Iterator<JsonNode> iterator = node.elements();
            while (iterator.hasNext()) {
                JsonNode value = iterator.next();
                values.add(value.isValueNode() ? value.asText() : value.toString());
            }
            return values.toString();
        }
        return node.toString();
    }

    private List<String> dependencyInferences(List<DependencyEdge> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return List.of();
        }
        return List.of("基于 released dependency edges，该对象存在 " + dependencies.size() + " 条一跳依赖或消费者关系。");
    }
}
