package com.rts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rts.model.CoreModels.ContentRef;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.store.Hashing;
import com.rts.store.StoreContracts.ProjectionSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * One-shot bootstrap that writes test projection data into the runtime-store.
 * Run: java -cp target/rts-0.1.0-SNAPSHOT.jar:target/dependency/* com.rts.BootstrapData [store-root] [admin-key]
 */
public class BootstrapData {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : "runtime-store");
        String adminKey = args.length > 1 ? args[1] : "admin-key";
        String adminKeyHash = Hashing.sha256(adminKey);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ProjectionSnapshot snapshot = TestProjectionFactory.valid();
        String releaseId = snapshot.manifest().releaseId();
        Path releaseRoot = root.resolve("releases").resolve(releaseId);

        // Create directories
        for (String dir : List.of("l2/rules", "l2/lookups", "l2/helpers",
                "navigation", "governance/evidence-summaries", "governance/review-summaries",
                "governance/report-summaries", "dependencies", "index-artifacts/lucene", "traces")) {
            Files.createDirectories(releaseRoot.resolve(dir));
        }

        // L2 content
        Files.writeString(releaseRoot.resolve("l2/rules/rule_amount.json"), TestProjectionFactory.ruleContent());
        Files.writeString(releaseRoot.resolve("l2/lookups/lookup_currency.json"), TestProjectionFactory.lookupContent());
        Files.writeString(releaseRoot.resolve("l2/helpers/helper_rounding.json"), TestProjectionFactory.helperContent());
        Files.writeString(releaseRoot.resolve("l2/rules/aurora_rule_amount.json"),
                "{\"logic\":\"Aurora amount uses a different source and must not answer Stella queries.\"}");

        // Write manifest with l2StorageRef resolved from contentRefs (mimics FileSystemProjectionStore.manifestWithStorageRefs)
        List<?> patchedManifest = snapshot.objectManifest().stream().map(entry -> {
            String storageRef = snapshot.contentRefs().stream()
                    .filter(ref -> ref.uri().equals(entry.uri()))
                    .map(ContentRef::storageRef)
                    .findFirst()
                    .orElse(null);
            if (storageRef == null) {
                return entry;
            }
            return new ObjectManifestEntry(entry.uri(), entry.releaseId(), entry.objectId(), entry.objectType(),
                    entry.channel(), entry.product(), entry.pack(), entry.domain(), entry.targetPath(), entry.sourceAnchors(),
                    entry.contentHash(), entry.cardRef(), entry.contentRef(), entry.schemaVersion(), entry.state(), storageRef);
        }).toList();

        // Write manifests and index files
        writeJson(releaseRoot.resolve("release-manifest.json"), mapper, snapshot.manifest());
        writeJsonl(releaseRoot.resolve("scopes.jsonl"), mapper, snapshot.scopes());
        writeJsonl(releaseRoot.resolve("object-manifest.jsonl"), mapper, patchedManifest);
        writeJsonl(releaseRoot.resolve("navigation/object-cards.jsonl"), mapper, snapshot.objectCards());
        writeJsonl(releaseRoot.resolve("navigation/l0-l1-views.jsonl"), mapper, snapshot.navigationViews());
        writeJsonl(releaseRoot.resolve("governance/governance-access-refs.jsonl"), mapper, snapshot.governanceAccessRefs());
        writeJsonl(releaseRoot.resolve("dependencies/dependency-edges.jsonl"), mapper, snapshot.dependencyEdges());
        writeJsonl(releaseRoot.resolve("dependencies/field-bindings.jsonl"), mapper, snapshot.fieldBindings());
        writeJsonl(releaseRoot.resolve("caller-profiles.jsonl"), mapper, snapshot.callerProfiles());

        // Active release pointer
        mapper.writerWithDefaultPrettyPrinter().writeValue(
                root.resolve("active-release.json").toFile(), snapshot.activeRelease());

        System.out.println("Bootstrap complete. release=" + releaseId + " adminKeyHash=" + adminKeyHash);
    }

    private static void writeJson(Path path, ObjectMapper mapper, Object value) throws Exception {
        Files.createDirectories(path.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
    }

    private static void writeJsonl(Path path, ObjectMapper mapper, List<?> values) throws Exception {
        Files.createDirectories(path.getParent());
        StringBuilder sb = new StringBuilder();
        for (Object item : values) {
            sb.append(mapper.writeValueAsString(item)).append(System.lineSeparator());
        }
        Files.writeString(path, sb.toString());
    }
}
