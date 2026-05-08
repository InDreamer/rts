package com.rts.store;

import com.rts.model.CoreModels.ActiveReleasePointer;
import com.rts.model.CoreModels.CallerProfile;
import com.rts.model.CoreModels.ContentRef;
import com.rts.model.CoreModels.DependencyEdge;
import com.rts.model.CoreModels.FieldBinding;
import com.rts.model.CoreModels.GovernanceAccessRef;
import com.rts.model.CoreModels.GovernanceSummary;
import com.rts.model.CoreModels.L2Content;
import com.rts.model.CoreModels.LlmRunTrace;
import com.rts.model.CoreModels.NavigationView;
import com.rts.model.CoreModels.ObjectCard;
import com.rts.model.CoreModels.ObjectManifestEntry;
import com.rts.model.CoreModels.ReleaseManifest;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.model.CoreModels.ScopeRecord;
import com.rts.model.CoreModels.TraceRecord;
import java.util.List;
import java.util.Map;
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
        List<FieldBinding> fieldBindings(String releaseId);
        List<NavigationView> navigationViews(String releaseId);
        Optional<GovernanceAccessRef> getGovernanceAccessRef(String releaseId, String uri);
        List<GovernanceSummary> governanceSummaries(String releaseId, String uri);
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
            List<NavigationView> navigationViews,
            List<GovernanceAccessRef> governanceAccessRefs,
            List<GovernanceSummary> governanceSummaries,
            List<DependencyEdge> dependencyEdges,
            List<FieldBinding> fieldBindings,
            List<ContentRef> contentRefs,
            List<CallerProfile> callerProfiles
    ) {
        public ProjectionSnapshot(
                ActiveReleasePointer activeRelease,
                ReleaseManifest manifest,
                List<ScopeRecord> scopes,
                List<ObjectManifestEntry> objectManifest,
                List<ObjectCard> objectCards,
                List<DependencyEdge> dependencyEdges,
                List<ContentRef> contentRefs,
                List<CallerProfile> callerProfiles
        ) {
            this(activeRelease, manifest, scopes, objectManifest, objectCards,
                    defaultNavigationViews(manifest, objectManifest, objectCards),
                    defaultGovernanceAccessRefs(manifest, objectManifest),
                    List.of(),
                    dependencyEdges, List.of(), contentRefs, callerProfiles);
        }

        private static List<NavigationView> defaultNavigationViews(ReleaseManifest manifest, List<ObjectManifestEntry> objects, List<ObjectCard> cards) {
            if (manifest == null || objects == null) {
                return List.of();
            }
            return objects.stream()
                    .map(object -> {
                        ObjectCard card = cards == null ? null : cards.stream()
                                .filter(candidate -> candidate.uri().equals(object.uri()))
                                .findFirst()
                                .orElse(null);
                        String text = card == null || card.searchText() == null ? object.objectId() : card.searchText();
                        return new NavigationView(object.uri(), manifest.releaseId(), "l0_l1", object.scope(), text, Map.of("summary", text), text,
                                "inline-generated", "navigation-v1");
                    })
                    .toList();
        }

        private static List<GovernanceAccessRef> defaultGovernanceAccessRefs(ReleaseManifest manifest, List<ObjectManifestEntry> objects) {
            if (manifest == null || objects == null) {
                return List.of();
            }
            return objects.stream()
                    .map(object -> new GovernanceAccessRef(object.uri(), manifest.releaseId(), "governance_tools", "summary_only",
                            List.of(), List.of(), List.of(), List.of(), "not specified", "No governance summary published."))
                    .toList();
        }
    }
}
