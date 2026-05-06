package com.rts.store;

import com.rts.model.CoreModels.ContentRef;
import com.rts.model.CoreModels.L2Content;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class LocalFileContentStore implements StoreContracts.ContentStore {
    private final FileSystemProjectionStore store;

    public LocalFileContentStore(FileSystemProjectionStore store) {
        this.store = store;
    }

    @Override
    public L2Content read(ContentRef ref) {
        if (!"local_file".equals(ref.storageKind())) {
            throw new ProjectionValidationException("Unsupported storage kind: " + ref.storageKind());
        }
        Path base = store.releaseRoot(ref.releaseId()).resolve("l2").normalize();
        Path contentPath = base.resolve(ref.storageRef()).normalize();
        if (!contentPath.startsWith(base)) {
            throw new ProjectionValidationException("Arbitrary path read is not allowed for " + ref.uri());
        }
        try {
            String content = Files.readString(contentPath, StandardCharsets.UTF_8);
            String actualHash = Hashing.sha256(content);
            if (!actualHash.equals(ref.contentHash())) {
                throw new ProjectionValidationException("L2 hash mismatch for " + ref.uri());
            }
            return new L2Content(ref.uri(), ref.releaseId(), actualHash, ref.contentType(), content);
        } catch (IOException ex) {
            throw new ProjectionValidationException("L2 content is not readable for " + ref.uri(), ex);
        }
    }
}
