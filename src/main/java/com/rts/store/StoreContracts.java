package com.rts.store;

import com.rts.model.CoreModels.ActiveReleasePointer;
import com.rts.model.CoreModels.CallerProfile;
import com.rts.model.CoreModels.ContentRef;
import com.rts.model.CoreModels.DependencyEdge;
import com.rts.model.CoreModels.L2Content;
import com.rts.model.CoreModels.LlmRunTrace;
import com.rts.model.CoreModels.ObjectCard;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.model.CoreModels.ReleaseManifest;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.model.CoreModels.ScopeRecord;
import com.rts.model.CoreModels.TraceRecord;
import java.util.List;
import java.util.Optional;

public final class StoreContracts {
    private StoreContracts() {}

    public interface ProjectionStore {
        ProjectionSnapshot loadActiveSnapshot();
        ProjectionSnapshot loadSnapshot(String releaseId);
        void ingest(ProjectionSnapshot snapshot);
        void activate(ActiveReleasePointer pointer);
        Optional<ObjectManifestEntry> getObject(String releaseId, String uri);
        Optional<ObjectCard> getCard(String releaseId, String uri);
        Optional<ContentRef> getContentRef(String releaseId, String uri);
        List<ObjectManifestEntry> allObjects(String releaseId);
        List<DependencyEdge> dependencies(String releaseId);
    }

    public interface ScopeRegistry {
        List<ScopeRecord> activeScopes(String releaseId);
        Optional<ScopeRecord> resolve(String releaseId, ScopeKey scope);
    }

    public interface TraceStore {
        void appendQueryTrace(TraceRecord trace);
        void appendLlmRunTrace(LlmRunTrace trace);
        Optional<TraceRecord> getQueryTrace(String traceId);
    }

    public interface CallerProfileStore {
        Optional<CallerProfile> find(String callerId, String releaseId);
    }

    public interface ContentStore {
        L2Content read(ContentRef ref);
    }

    public record ProjectionSnapshot(
            ActiveReleasePointer activeRelease,
            ReleaseManifest manifest,
            List<ScopeRecord> scopes,
            List<ObjectManifestEntry> objectManifest,
            List<ObjectCard> objectCards,
            List<DependencyEdge> dependencyEdges,
            List<ContentRef> contentRefs,
            List<CallerProfile> callerProfiles
    ) {}
}
