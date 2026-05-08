package com.rts.query;

import com.rts.config.RtsProperties;
import com.rts.model.CoreModels.DependencyEdge;
import com.rts.model.CoreModels.DependencyResult;
import com.rts.model.CoreModels.Direction;
import com.rts.model.CoreModels.FieldBinding;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.store.StoreContracts.ProjectionStore;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DependencyService {
    private final ProjectionStore projectionStore;
    private final RtsProperties properties;

    public DependencyService(ProjectionStore projectionStore, RtsProperties properties) {
        this.projectionStore = projectionStore;
        this.properties = properties;
    }

    public DependencyResult traverse(String releaseId, String uri, Direction direction, String edgeType, int requestedDepth, int maxObjects) {
        int depth = Math.max(1, Math.min(requestedDepth, properties.getMaxDependencyDepth()));
        List<DependencyEdge> allEdges = projectionStore.dependencies(releaseId);
        List<DependencyEdge> selected = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        queue.add(new Node(uri, 0));
        seen.add(uri);
        boolean truncated = false;
        while (!queue.isEmpty()) {
            Node node = queue.removeFirst();
            if (node.depth >= depth) {
                continue;
            }
            for (DependencyEdge edge : allEdges) {
                if (!matchesDirection(edge, node.uri, direction) || (edgeType != null && !edgeType.isBlank() && !edge.edgeType().equals(edgeType))) {
                    continue;
                }
                selected.add(edge);
                String next = edge.fromUri().equals(node.uri) ? edge.toUri() : edge.fromUri();
                if (seen.add(next)) {
                    if (seen.size() > maxObjects) {
                        truncated = true;
                        break;
                    }
                    queue.add(new Node(next, node.depth + 1));
                }
            }
            if (truncated) {
                break;
            }
        }
        List<ObjectManifestEntry> objects = seen.stream()
                .filter(value -> !value.equals(uri))
                .flatMap(value -> projectionStore.getObject(releaseId, value).stream())
                .toList();
        List<String> objectUris = new ArrayList<>(seen);
        List<FieldBinding> fieldBindings = projectionStore.fieldBindings(releaseId).stream()
                .filter(binding -> objectUris.contains(binding.objectUri())
                        || (binding.viaUri() != null && objectUris.contains(binding.viaUri())))
                .toList();
        return new DependencyResult(List.copyOf(selected), objects, truncated, fieldBindings);
    }

    private boolean matchesDirection(DependencyEdge edge, String uri, Direction direction) {
        Direction safe = direction == null ? Direction.forward : direction;
        return switch (safe) {
            case forward -> edge.fromUri().equals(uri);
            case reverse -> edge.toUri().equals(uri);
            case both -> edge.fromUri().equals(uri) || edge.toUri().equals(uri);
        };
    }

    private record Node(String uri, int depth) {}
}
