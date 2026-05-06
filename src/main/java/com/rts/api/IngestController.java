package com.rts.api;

import com.rts.index.LuceneIndexService;
import com.rts.query.PermissionService;
import com.rts.query.QueryRefusalException;
import com.rts.store.ProjectionValidationException;
import com.rts.store.StoreContracts.ProjectionSnapshot;
import com.rts.store.StoreContracts.ProjectionStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class IngestController {
    private final ProjectionStore projectionStore;
    private final LuceneIndexService luceneIndexService;
    private final PermissionService permissionService;

    public IngestController(ProjectionStore projectionStore, LuceneIndexService luceneIndexService, PermissionService permissionService) {
        this.projectionStore = projectionStore;
        this.luceneIndexService = luceneIndexService;
        this.permissionService = permissionService;
    }

    @PostMapping("/projection/ingest")
    public IngestResponse ingest(@RequestBody ProjectionSnapshot snapshot,
            @RequestHeader(name = "X-RTS-Caller-Id", required = false) String callerId,
            @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        try {
            String authReleaseId = projectionStore.loadActiveSnapshot().manifest().releaseId();
            permissionService.requireAdminForNewRelease(authReleaseId, callerId, apiKey);
        } catch (ProjectionValidationException ex) {
            permissionService.requireLocalAdmin(callerId, apiKey);
        }
        projectionStore.ingest(snapshot);
        luceneIndexService.rebuild(snapshot.manifest().releaseId());
        projectionStore.activate(snapshot.activeRelease());
        return new IngestResponse(snapshot.manifest().releaseId(), "ingested");
    }

    public record IngestResponse(String releaseId, String status) {}
}
