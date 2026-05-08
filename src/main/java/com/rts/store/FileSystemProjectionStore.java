package com.rts.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rts.config.RtsProperties;
import com.rts.model.CoreModels.ActiveReleasePointer;
import com.rts.model.CoreModels.CallerProfile;
import com.rts.model.CoreModels.ContentRef;
import com.rts.model.CoreModels.DependencyEdge;
import com.rts.model.CoreModels.FieldBinding;
import com.rts.model.CoreModels.GovernanceAccessRef;
import com.rts.model.CoreModels.GovernanceSummary;
import com.rts.model.CoreModels.LlmRunTrace;
import com.rts.model.CoreModels.NavigationView;
import com.rts.model.CoreModels.ObjectCard;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.model.CoreModels.ReleaseManifest;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.model.CoreModels.ScopeRecord;
import com.rts.model.CoreModels.TraceRecord;
import com.rts.store.StoreContracts.CallerProfileStore;
import com.rts.store.StoreContracts.ProjectionSnapshot;
import com.rts.store.StoreContracts.ProjectionStore;
import com.rts.store.StoreContracts.ScopeRegistry;
import com.rts.store.StoreContracts.TraceStore;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class FileSystemProjectionStore implements ProjectionStore, ScopeRegistry, TraceStore, CallerProfileStore {
    private static final String SUPPORTED_PROJECTION_SCHEMA = "runtime-multiview-v1";

    private final ObjectMapper mapper;
    private final RtsProperties properties;
    private final Map<String, ProjectionSnapshot> snapshots = new ConcurrentHashMap<>();

    public FileSystemProjectionStore(ObjectMapper mapper, RtsProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        Path root = properties.getStoreRoot();
        if (Files.exists(root.resolve("active-release.json"))) {
            loadActiveSnapshot();
        }
    }

    public Path root() {
        return properties.getStoreRoot();
    }

    public Path releaseRoot(String releaseId) {
        return root().resolve("releases").resolve(releaseId);
    }

    @Override
    public ProjectionSnapshot loadActiveSnapshot() {
        ActiveReleasePointer pointer = readJson(root().resolve("active-release.json"), ActiveReleasePointer.class);
        return loadSnapshot(pointer.activeReleaseId());
    }

    @Override
    public ProjectionSnapshot loadSnapshot(String releaseId) {
        ProjectionSnapshot cached = snapshots.get(releaseId);
        if (cached != null) {
            return cached;
        }
        Path releaseRoot = releaseRoot(releaseId);
        ActiveReleasePointer pointer = Files.exists(root().resolve("active-release.json"))
                ? readJson(root().resolve("active-release.json"), ActiveReleasePointer.class)
                : new ActiveReleasePointer(releaseId, null, Instant.now(), "local");
        ProjectionSnapshot snapshot = new ProjectionSnapshot(
                pointer,
                readJson(releaseRoot.resolve("release-manifest.json"), ReleaseManifest.class),
                readJsonl(releaseRoot.resolve("scopes.jsonl"), ScopeRecord.class),
                readJsonl(releaseRoot.resolve("object-manifest.jsonl"), ObjectManifestEntry.class),
                readJsonl(releaseRoot.resolve("navigation").resolve("object-cards.jsonl"), ObjectCard.class),
                readJsonl(releaseRoot.resolve("navigation").resolve("l0-l1-views.jsonl"), NavigationView.class),
                readJsonl(releaseRoot.resolve("governance").resolve("governance-access-refs.jsonl"), GovernanceAccessRef.class),
                readGovernanceSummaries(releaseRoot),
                readJsonl(releaseRoot.resolve("dependencies").resolve("dependency-edges.jsonl"), DependencyEdge.class),
                readJsonl(releaseRoot.resolve("dependencies").resolve("field-bindings.jsonl"), FieldBinding.class),
                readContentRefs(releaseRoot),
                readJsonl(releaseRoot.resolve("caller-profiles.jsonl"), CallerProfile.class));
        validateSnapshot(snapshot);
        snapshots.put(releaseId, snapshot);
        return snapshot;
    }

    @Override
    public void ingest(ProjectionSnapshot snapshot) {
        validateSnapshot(snapshot);
        Path releaseRoot = releaseRoot(snapshot.manifest().releaseId());
        try {
            Files.createDirectories(releaseRoot.resolve("l2").resolve("rules"));
            Files.createDirectories(releaseRoot.resolve("l2").resolve("lookups"));
            Files.createDirectories(releaseRoot.resolve("l2").resolve("helpers"));
            Files.createDirectories(releaseRoot.resolve("navigation"));
            Files.createDirectories(releaseRoot.resolve("governance").resolve("evidence-summaries"));
            Files.createDirectories(releaseRoot.resolve("governance").resolve("review-summaries"));
            Files.createDirectories(releaseRoot.resolve("governance").resolve("report-summaries"));
            Files.createDirectories(releaseRoot.resolve("dependencies"));
            Files.createDirectories(releaseRoot.resolve("index-artifacts").resolve("lucene"));
            Files.createDirectories(root().resolve("traces"));
            writeJson(releaseRoot.resolve("release-manifest.json"), snapshot.manifest());
            writeJsonl(releaseRoot.resolve("scopes.jsonl"), snapshot.scopes());
            writeJsonl(releaseRoot.resolve("object-manifest.jsonl"), manifestWithStorageRefs(snapshot));
            writeJsonl(releaseRoot.resolve("navigation").resolve("object-cards.jsonl"), snapshot.objectCards());
            writeJsonl(releaseRoot.resolve("navigation").resolve("l0-l1-views.jsonl"), snapshot.navigationViews());
            writeJsonl(releaseRoot.resolve("governance").resolve("governance-access-refs.jsonl"), snapshot.governanceAccessRefs());
            writeGovernanceSummaries(releaseRoot, snapshot.governanceSummaries());
            writeJsonl(releaseRoot.resolve("dependencies").resolve("dependency-edges.jsonl"), snapshot.dependencyEdges());
            writeJsonl(releaseRoot.resolve("dependencies").resolve("field-bindings.jsonl"), snapshot.fieldBindings());
            writeJsonl(releaseRoot.resolve("index-artifacts").resolve("opensearch-docs.jsonl"), snapshot.navigationViews());
            writeJsonl(releaseRoot.resolve("caller-profiles.jsonl"), snapshot.callerProfiles());
            snapshots.put(snapshot.manifest().releaseId(), snapshot);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void activate(ActiveReleasePointer pointer) {
        Path active = root().resolve("active-release.json");
        Path tmp = root().resolve("active-release.json.tmp");
        try {
            Files.createDirectories(root());
            mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), pointer);
            Files.move(tmp, active, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            snapshots.remove(pointer.activeReleaseId());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public Optional<ObjectManifestEntry> getObject(String releaseId, String uri) {
        return loadSnapshot(releaseId).objectManifest().stream()
                .filter(entry -> entry.uri().equals(uri))
                .findFirst();
    }

    @Override
    public Optional<ObjectCard> getCard(String releaseId, String uri) {
        return loadSnapshot(releaseId).objectCards().stream()
                .filter(card -> card.uri().equals(uri))
                .findFirst();
    }

    @Override
    public Optional<ContentRef> getContentRef(String releaseId, String uri) {
        return loadSnapshot(releaseId).contentRefs().stream()
                .filter(ref -> ref.uri().equals(uri))
                .findFirst();
    }

    @Override
    public List<ObjectManifestEntry> allObjects(String releaseId) {
        return loadSnapshot(releaseId).objectManifest();
    }

    @Override
    public List<DependencyEdge> dependencies(String releaseId) {
        return loadSnapshot(releaseId).dependencyEdges();
    }

    @Override
    public List<FieldBinding> fieldBindings(String releaseId) {
        return loadSnapshot(releaseId).fieldBindings();
    }

    @Override
    public List<NavigationView> navigationViews(String releaseId) {
        return loadSnapshot(releaseId).navigationViews();
    }

    @Override
    public Optional<GovernanceAccessRef> getGovernanceAccessRef(String releaseId, String uri) {
        return loadSnapshot(releaseId).governanceAccessRefs().stream()
                .filter(ref -> ref.uri().equals(uri))
                .findFirst();
    }

    @Override
    public List<GovernanceSummary> governanceSummaries(String releaseId, String uri) {
        return loadSnapshot(releaseId).governanceSummaries().stream()
                .filter(summary -> uri == null || uri.isBlank() || summary.uri().equals(uri))
                .toList();
    }

    @Override
    public List<ScopeRecord> activeScopes(String releaseId) {
        return loadSnapshot(releaseId).scopes().stream()
                .filter(ScopeRecord::activeFlag)
                .toList();
    }

    @Override
    public Optional<ScopeRecord> resolve(String releaseId, ScopeKey scope) {
        return activeScopes(releaseId).stream()
                .filter(record -> record.key().matches(scope))
                .findFirst();
    }

    @Override
    public void appendQueryTrace(TraceRecord trace) {
        appendJsonl(root().resolve("traces").resolve("query-trace.jsonl"), trace);
    }

    @Override
    public void appendLlmRunTrace(LlmRunTrace trace) {
        appendJsonl(root().resolve("traces").resolve("llm-run-trace.jsonl"), trace);
    }

    @Override
    public Optional<TraceRecord> getQueryTrace(String traceId) {
        Path path = root().resolve("traces").resolve("query-trace.jsonl");
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return readJsonl(path, TraceRecord.class).stream()
                .filter(trace -> trace.traceId().equals(traceId))
                .max(Comparator.comparing(TraceRecord::createdAt));
    }

    @Override
    public Optional<CallerProfile> find(String callerId, String releaseId) {
        return loadSnapshot(releaseId).callerProfiles().stream()
                .filter(profile -> profile.callerId().equals(callerId))
                .findFirst();
    }

    public void validateSnapshot(ProjectionSnapshot snapshot) {
        ReleaseManifest manifest = snapshot.manifest();
        if (!SUPPORTED_PROJECTION_SCHEMA.equals(manifest.projectionSchemaVersion())) {
            throw new ProjectionValidationException("Unsupported projection schema: " + manifest.projectionSchemaVersion());
        }
        if (!"active".equals(manifest.activationState()) && !"released".equals(manifest.activationState())) {
            throw new ProjectionValidationException("Release is not active/released: " + manifest.releaseId());
        }
        if (manifest.blockingIssuesCount() > 0) {
            throw new ProjectionValidationException("Release has blocking issues: " + manifest.releaseId());
        }
        Set<String> uris = new HashSet<>();
        Map<String, ObjectManifestEntry> targetRules = new HashMap<>();
        for (ObjectManifestEntry entry : snapshot.objectManifest()) {
            if (!uris.add(entry.uri())) {
                throw new ProjectionValidationException("Duplicate object URI: " + entry.uri());
            }
            if (!manifest.releaseId().equals(entry.releaseId()) || !"released".equals(entry.state())) {
                throw new ProjectionValidationException("Object is not released in manifest: " + entry.uri());
            }
            if (snapshot.scopes().stream().noneMatch(scope -> scope.activeFlag() && scope.key().matches(entry.scope()))) {
                throw new ProjectionValidationException("Object scope is not active in release: " + entry.uri());
            }
            if ("rule".equals(entry.objectType().name()) && entry.targetPath() != null && !entry.targetPath().isBlank()) {
                String targetKey = entry.releaseId() + "|" + entry.channel() + "|" + entry.product() + "|" + entry.pack() + "|" + entry.domain() + "|" + entry.targetPath();
                ObjectManifestEntry existing = targetRules.putIfAbsent(targetKey, entry);
                if (existing != null) {
                    throw new ProjectionValidationException("Duplicate target rule in same scope: " + entry.targetPath());
                }
            }
            ContentRef ref = snapshot.contentRefs().stream()
                    .filter(candidate -> candidate.uri().equals(entry.uri()))
                    .findFirst()
                    .orElseThrow(() -> new ProjectionValidationException("Missing content ref: " + entry.uri()));
            if (!manifest.releaseId().equals(ref.releaseId())) {
                throw new ProjectionValidationException("Content ref release mismatch: " + entry.uri());
            }
            if (!entry.contentHash().equals(ref.contentHash())) {
                throw new ProjectionValidationException("Manifest/content-ref hash mismatch: " + entry.uri());
            }
            ObjectCard card = snapshot.objectCards().stream()
                    .filter(candidateCard -> candidateCard.uri().equals(entry.uri()))
                    .findFirst()
                    .orElseThrow(() -> new ProjectionValidationException("Missing object card: " + entry.uri()));
            if (!manifest.releaseId().equals(card.releaseId()) || card.objectType() != entry.objectType()) {
                throw new ProjectionValidationException("Object card release/type mismatch: " + entry.uri());
            }
            if ("local_file".equals(ref.storageKind())) {
                Path l2Path = releaseRoot(manifest.releaseId()).resolve("l2").resolve(ref.storageRef()).normalize();
                if (!l2Path.startsWith(releaseRoot(manifest.releaseId()).resolve("l2").normalize())) {
                    throw new ProjectionValidationException("Invalid L2 storage ref: " + entry.uri());
                }
                if (!Files.exists(l2Path)) {
                    throw new ProjectionValidationException("L2 missing: " + entry.uri());
                }
                try {
                    String content = Files.readString(l2Path, StandardCharsets.UTF_8);
                    if (!Hashing.sha256(content).equals(ref.contentHash())) {
                        throw new ProjectionValidationException("L2 hash mismatch: " + entry.uri());
                    }
                } catch (IOException ex) {
                    throw new ProjectionValidationException("L2 unreadable: " + entry.uri(), ex);
                }
            }
            if (snapshot.navigationViews().stream().noneMatch(view -> view.uri().equals(entry.uri()))) {
                throw new ProjectionValidationException("Missing navigation view: " + entry.uri());
            }
            GovernanceAccessRef governanceRef = snapshot.governanceAccessRefs().stream()
                    .filter(candidate -> candidate.uri().equals(entry.uri()))
                    .findFirst()
                    .orElseThrow(() -> new ProjectionValidationException("Missing governance access ref: " + entry.uri()));
            if (!manifest.releaseId().equals(governanceRef.releaseId())) {
                throw new ProjectionValidationException("Governance ref release mismatch: " + entry.uri());
            }
            Set<String> summaryIds = snapshot.governanceSummaries().stream()
                    .map(GovernanceSummary::summaryId)
                    .collect(java.util.stream.Collectors.toSet());
            for (String summaryRef : allSummaryRefs(governanceRef)) {
                if (!summaryIds.contains(summaryRef)) {
                    throw new ProjectionValidationException("Missing governance summary: " + summaryRef);
                }
            }
        }
        for (DependencyEdge edge : snapshot.dependencyEdges()) {
            if (!manifest.releaseId().equals(edge.releaseId())) {
                throw new ProjectionValidationException("Dependency release mismatch: " + edge.fromUri() + " -> " + edge.toUri());
            }
            if (edge.requiredFlag() && (findObject(snapshot, edge.fromUri()).isEmpty() || findObject(snapshot, edge.toUri()).isEmpty())) {
                throw new ProjectionValidationException("Required dependency is not released: " + edge.fromUri() + " -> " + edge.toUri());
            }
        }
        for (FieldBinding binding : snapshot.fieldBindings()) {
            if (!manifest.releaseId().equals(binding.releaseId())) {
                throw new ProjectionValidationException("Field binding release mismatch: " + binding.objectUri());
            }
            if (findObject(snapshot, binding.objectUri()).isEmpty()) {
                throw new ProjectionValidationException("Field binding object is not released: " + binding.objectUri());
            }
            if (binding.viaUri() != null && !binding.viaUri().isBlank() && findObject(snapshot, binding.viaUri()).isEmpty()) {
                throw new ProjectionValidationException("Field binding dependency is not released: " + binding.objectUri() + " -> " + binding.viaUri());
            }
        }
    }

    private Optional<ObjectManifestEntry> findObject(ProjectionSnapshot snapshot, String uri) {
        return snapshot.objectManifest().stream().filter(entry -> entry.uri().equals(uri)).findFirst();
    }

    private List<ObjectManifestEntry> manifestWithStorageRefs(ProjectionSnapshot snapshot) {
        return snapshot.objectManifest().stream()
                .map(entry -> {
                    if (entry.l2StorageRef() != null && !entry.l2StorageRef().isBlank()) {
                        return entry;
                    }
                    String storageRef = snapshot.contentRefs().stream()
                            .filter(ref -> ref.uri().equals(entry.uri()))
                            .map(ContentRef::storageRef)
                            .findFirst()
                            .orElse(storageRefFor(entry));
                    return new ObjectManifestEntry(entry.uri(), entry.releaseId(), entry.objectId(), entry.objectType(),
                            entry.channel(), entry.product(), entry.pack(), entry.domain(), entry.targetPath(), entry.sourceAnchors(),
                            entry.contentHash(), entry.cardRef(), entry.contentRef(), entry.schemaVersion(), entry.state(), storageRef);
                })
                .toList();
    }

    private <T> T readJson(Path path, Class<T> type) {
        try {
            return mapper.readValue(path.toFile(), type);
        } catch (IOException ex) {
            throw new ProjectionValidationException("Cannot read " + path, ex);
        }
    }

    private void writeJson(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
    }

    private <T> List<T> readJsonl(Path path, Class<T> type) {
        if (!Files.exists(path)) {
            return List.of();
        }
        List<T> values = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    values.add(mapper.readValue(line, type));
                }
            }
            return List.copyOf(values);
        } catch (IOException ex) {
            throw new ProjectionValidationException("Cannot read " + path, ex);
        }
    }

    private List<ContentRef> readContentRefs(Path releaseRoot) {
        List<ContentRef> refs = new ArrayList<>();
        for (ObjectManifestEntry entry : readJsonl(releaseRoot.resolve("object-manifest.jsonl"), ObjectManifestEntry.class)) {
            refs.add(new ContentRef(
                    entry.uri(),
                    entry.releaseId(),
                    entry.contentRef(),
                    "local_file",
                    storageRefFor(entry),
                    entry.contentHash(),
                    "application/json",
                    "l2-runtime-v1"));
        }
        return List.copyOf(refs);
    }

    private String storageRefFor(ObjectManifestEntry entry) {
        if (entry.l2StorageRef() != null && !entry.l2StorageRef().isBlank()) {
            return entry.l2StorageRef();
        }
        return switch (entry.objectType()) {
            case rule -> "rules/" + entry.objectId() + ".json";
            case lookup -> "lookups/" + entry.objectId() + ".json";
            case helper -> "helpers/" + entry.objectId() + ".json";
            default -> entry.objectId() + ".json";
        };
    }

    private List<GovernanceSummary> readGovernanceSummaries(Path releaseRoot) {
        List<GovernanceSummary> summaries = new ArrayList<>();
        for (String folder : List.of("evidence-summaries", "review-summaries", "report-summaries")) {
            Path directory = releaseRoot.resolve("governance").resolve(folder);
            if (!Files.exists(directory)) {
                continue;
            }
            try (Stream<Path> stream = Files.list(directory)) {
                for (Path path : stream
                        .filter(candidate -> candidate.getFileName().toString().endsWith(".json"))
                        .sorted()
                        .toList()) {
                    summaries.add(readJson(path, GovernanceSummary.class));
                }
            } catch (IOException ex) {
                throw new ProjectionValidationException("Cannot read governance summaries under " + directory, ex);
            }
        }
        return List.copyOf(summaries);
    }

    private void writeGovernanceSummaries(Path releaseRoot, List<GovernanceSummary> summaries) throws IOException {
        for (GovernanceSummary summary : summaries == null ? List.<GovernanceSummary>of() : summaries) {
            String folder = switch (summary.summaryType()) {
                case "evidence" -> "evidence-summaries";
                case "review" -> "review-summaries";
                case "report" -> "report-summaries";
                default -> "evidence-summaries";
            };
            writeJson(releaseRoot.resolve("governance").resolve(folder).resolve(fileName(summary.summaryId())), summary);
        }
    }

    private String fileName(String value) {
        String safe = value == null || value.isBlank() ? "summary" : value.replaceAll("[^A-Za-z0-9_.-]", "_");
        return safe + ".json";
    }

    private List<String> allSummaryRefs(GovernanceAccessRef ref) {
        List<String> refs = new ArrayList<>();
        if (ref.evidenceSummaryRefs() != null) {
            refs.addAll(ref.evidenceSummaryRefs());
        }
        if (ref.reviewSummaryRefs() != null) {
            refs.addAll(ref.reviewSummaryRefs());
        }
        if (ref.reportSummaryRefs() != null) {
            refs.addAll(ref.reportSummaryRefs());
        }
        return refs;
    }

    private void writeJsonl(Path path, List<?> values) throws IOException {
        Files.createDirectories(path.getParent());
        List<String> lines = new ArrayList<>();
        for (Object value : values) {
            lines.add(mapper.writeValueAsString(value));
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private void appendJsonl(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, mapper.writeValueAsString(value) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public void clearForTests() {
        snapshots.clear();
        if (Files.exists(root())) {
            try (Stream<Path> stream = Files.walk(root())) {
                stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                });
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }
}
