package com.rts.query;

import com.rts.config.RtsProperties;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.store.Hashing;
import com.rts.store.StoreContracts.CallerProfileStore;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PermissionService {
    private final CallerProfileStore callerProfileStore;
    private final RtsProperties properties;

    public PermissionService(CallerProfileStore callerProfileStore, RtsProperties properties) {
        this.callerProfileStore = callerProfileStore;
        this.properties = properties;
    }

    public void requireAllowed(String releaseId, String callerId, String apiKey, ScopeKey scope, String entrypoint, String outputMode) {
        if (callerId == null || callerId.isBlank()) {
            throw new QueryRefusalException(RefusalReason.unauthorized_scope, "caller_id is required");
        }
        boolean allowed = callerProfileStore.find(callerId, releaseId)
                .map(profile -> apiKeyMatches(profile.apiKeyHash(), apiKey)
                        && profile.allows(scope, entrypoint, outputMode == null ? "default" : outputMode))
                .orElse(false);
        if (!allowed) {
            throw new QueryRefusalException(RefusalReason.unauthorized_scope, "Caller cannot access requested scope");
        }
    }

    public void requireAdmin(String releaseId, String callerId, String apiKey) {
        if (callerId == null || callerId.isBlank()) {
            throw new QueryRefusalException(RefusalReason.unauthorized_scope, "caller_id is required");
        }
        if (localAdminMatches(callerId, apiKey)) {
            return;
        }
        boolean allowed = callerProfileStore.find(callerId, releaseId)
                .map(profile -> profile.activeFlag()
                        && apiKeyMatches(profile.apiKeyHash(), apiKey)
                        && containsOrWildcard(profile.allowedEntrypoints(), "admin_ingest"))
                .orElse(false);
        if (!allowed) {
            throw new QueryRefusalException(RefusalReason.unauthorized_scope, "Caller cannot access admin ingest");
        }
    }

    public void requireAdminForNewRelease(String currentReleaseId, String callerId, String apiKey) {
        requireAdmin(currentReleaseId, callerId, apiKey);
    }

    public void requireLocalAdmin(String callerId, String apiKey) {
        if (callerId == null || callerId.isBlank()) {
            throw new QueryRefusalException(RefusalReason.unauthorized_scope, "caller_id is required");
        }
        if (!localAdminMatches(callerId, apiKey)) {
            throw new QueryRefusalException(RefusalReason.unauthorized_scope, "Local admin credentials are required for first bootstrap");
        }
    }

    private boolean apiKeyMatches(String expectedHash, String apiKey) {
        return expectedHash != null
                && !expectedHash.isBlank()
                && apiKey != null
                && !apiKey.isBlank()
                && expectedHash.equals(Hashing.sha256(apiKey));
    }

    private boolean localAdminMatches(String callerId, String apiKey) {
        return properties.getAdminCallerId() != null
                && properties.getAdminCallerId().equals(callerId)
                && apiKeyMatches(properties.getAdminApiKeyHash(), apiKey);
    }

    private boolean containsOrWildcard(List<String> values, String value) {
        return values != null && (values.contains("*") || values.contains(value));
    }
}
