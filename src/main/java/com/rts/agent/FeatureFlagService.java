package com.rts.agent;

import com.rts.config.RtsProperties;
import com.rts.model.AgentServiceModels.FeatureFlags;
import org.springframework.stereotype.Service;

@Service
public class FeatureFlagService {
    private final RtsProperties properties;

    public FeatureFlagService(RtsProperties properties) {
        this.properties = properties;
    }

    public FeatureFlags current() {
        return new FeatureFlags(
                properties.isPlannerV2Enabled(),
                properties.isToolOrchestratorEnabled(),
                properties.isRerankerEnabled(),
                properties.isConfusableCheckEnabled(),
                properties.isVectorRecallEnabled(),
                properties.isImpactCandidatesEnabled(),
                properties.isTestPlanCandidatesEnabled(),
                properties.isMcpExpandedToolsEnabled());
    }
}
