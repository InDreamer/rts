package com.rts;

import com.rts.store.FileSystemProjectionStore;
import com.rts.store.StoreContracts.ProjectionSnapshot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class ProjectionTestSupport {
    private ProjectionTestSupport() {}

    public static void writeL2(FileSystemProjectionStore store, ProjectionSnapshot snapshot) throws IOException {
        var l2 = store.releaseRoot(snapshot.manifest().releaseId()).resolve("l2");
        Files.createDirectories(l2.resolve("rules"));
        Files.createDirectories(l2.resolve("lookups"));
        Files.createDirectories(l2.resolve("helpers"));
        Files.writeString(l2.resolve("rules/rule_amount.json"), TestProjectionFactory.ruleContent(), StandardCharsets.UTF_8);
        Files.writeString(l2.resolve("lookups/lookup_currency.json"), TestProjectionFactory.lookupContent(), StandardCharsets.UTF_8);
        Files.writeString(l2.resolve("helpers/helper_rounding.json"), TestProjectionFactory.helperContent(), StandardCharsets.UTF_8);
        Files.writeString(l2.resolve("rules/aurora_rule_amount.json"), "{\"logic\":\"Aurora amount uses a different source and must not answer Stella queries.\"}", StandardCharsets.UTF_8);
    }
}
