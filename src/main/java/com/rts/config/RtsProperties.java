package com.rts.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rts")
public class RtsProperties {
    private Path storeRoot = Path.of("runtime-store");
    private boolean llmEnabled = false;
    private String llmModel = "gpt-5.5";
    private String llmBaseUrl = "https://api.jankan.com";
    private String llmApiKey = "";
    private String llmWireApi = "responses";
    private boolean llmStoreResponses = false;
    private String llmReasoningEffort = "xhigh";
    private int llmMaxTokens = 600;
    private String adminCallerId = "admin";
    private String adminApiKeyHash = "";
    private int maxToolCalls = 8;
    private int maxL2Objects = 5;
    private int maxDependencyDepth = 2;
    private int maxRetrievedTokens = 12000;
    private int maxModelCalls = 1;
    private long maxLatencyMs = 30000;
    private boolean plannerV2Enabled = false;
    private boolean toolOrchestratorEnabled = true;
    private boolean rerankerEnabled = false;
    private boolean confusableCheckEnabled = true;
    private boolean vectorRecallEnabled = false;
    private boolean impactCandidatesEnabled = true;
    private boolean testPlanCandidatesEnabled = true;
    private boolean mcpExpandedToolsEnabled = true;

    public Path getStoreRoot() {
        return storeRoot;
    }

    public void setStoreRoot(Path storeRoot) {
        this.storeRoot = storeRoot;
    }

    public boolean isLlmEnabled() {
        return llmEnabled;
    }

    public void setLlmEnabled(boolean llmEnabled) {
        this.llmEnabled = llmEnabled;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(String llmModel) {
        this.llmModel = llmModel;
    }

    public String getLlmBaseUrl() {
        return llmBaseUrl;
    }

    public void setLlmBaseUrl(String llmBaseUrl) {
        this.llmBaseUrl = llmBaseUrl;
    }

    public String getLlmApiKey() {
        return llmApiKey;
    }

    public void setLlmApiKey(String llmApiKey) {
        this.llmApiKey = llmApiKey;
    }

    public String getLlmWireApi() {
        return llmWireApi;
    }

    public void setLlmWireApi(String llmWireApi) {
        this.llmWireApi = llmWireApi;
    }

    public boolean isLlmStoreResponses() {
        return llmStoreResponses;
    }

    public void setLlmStoreResponses(boolean llmStoreResponses) {
        this.llmStoreResponses = llmStoreResponses;
    }

    public String getLlmReasoningEffort() {
        return llmReasoningEffort;
    }

    public void setLlmReasoningEffort(String llmReasoningEffort) {
        this.llmReasoningEffort = llmReasoningEffort;
    }

    public int getLlmMaxTokens() {
        return llmMaxTokens;
    }

    public void setLlmMaxTokens(int llmMaxTokens) {
        this.llmMaxTokens = llmMaxTokens;
    }

    public String getAdminCallerId() {
        return adminCallerId;
    }

    public void setAdminCallerId(String adminCallerId) {
        this.adminCallerId = adminCallerId;
    }

    public String getAdminApiKeyHash() {
        return adminApiKeyHash;
    }

    public void setAdminApiKeyHash(String adminApiKeyHash) {
        this.adminApiKeyHash = adminApiKeyHash;
    }

    public int getMaxToolCalls() {
        return maxToolCalls;
    }

    public void setMaxToolCalls(int maxToolCalls) {
        this.maxToolCalls = maxToolCalls;
    }

    public int getMaxL2Objects() {
        return maxL2Objects;
    }

    public void setMaxL2Objects(int maxL2Objects) {
        this.maxL2Objects = maxL2Objects;
    }

    public int getMaxDependencyDepth() {
        return maxDependencyDepth;
    }

    public void setMaxDependencyDepth(int maxDependencyDepth) {
        this.maxDependencyDepth = maxDependencyDepth;
    }

    public int getMaxRetrievedTokens() {
        return maxRetrievedTokens;
    }

    public void setMaxRetrievedTokens(int maxRetrievedTokens) {
        this.maxRetrievedTokens = maxRetrievedTokens;
    }

    public int getMaxModelCalls() {
        return maxModelCalls;
    }

    public void setMaxModelCalls(int maxModelCalls) {
        this.maxModelCalls = maxModelCalls;
    }

    public long getMaxLatencyMs() {
        return maxLatencyMs;
    }

    public void setMaxLatencyMs(long maxLatencyMs) {
        this.maxLatencyMs = maxLatencyMs;
    }

    public boolean isPlannerV2Enabled() {
        return plannerV2Enabled;
    }

    public void setPlannerV2Enabled(boolean plannerV2Enabled) {
        this.plannerV2Enabled = plannerV2Enabled;
    }

    public boolean isToolOrchestratorEnabled() {
        return toolOrchestratorEnabled;
    }

    public void setToolOrchestratorEnabled(boolean toolOrchestratorEnabled) {
        this.toolOrchestratorEnabled = toolOrchestratorEnabled;
    }

    public boolean isRerankerEnabled() {
        return rerankerEnabled;
    }

    public void setRerankerEnabled(boolean rerankerEnabled) {
        this.rerankerEnabled = rerankerEnabled;
    }

    public boolean isConfusableCheckEnabled() {
        return confusableCheckEnabled;
    }

    public void setConfusableCheckEnabled(boolean confusableCheckEnabled) {
        this.confusableCheckEnabled = confusableCheckEnabled;
    }

    public boolean isVectorRecallEnabled() {
        return vectorRecallEnabled;
    }

    public void setVectorRecallEnabled(boolean vectorRecallEnabled) {
        this.vectorRecallEnabled = vectorRecallEnabled;
    }

    public boolean isImpactCandidatesEnabled() {
        return impactCandidatesEnabled;
    }

    public void setImpactCandidatesEnabled(boolean impactCandidatesEnabled) {
        this.impactCandidatesEnabled = impactCandidatesEnabled;
    }

    public boolean isTestPlanCandidatesEnabled() {
        return testPlanCandidatesEnabled;
    }

    public void setTestPlanCandidatesEnabled(boolean testPlanCandidatesEnabled) {
        this.testPlanCandidatesEnabled = testPlanCandidatesEnabled;
    }

    public boolean isMcpExpandedToolsEnabled() {
        return mcpExpandedToolsEnabled;
    }

    public void setMcpExpandedToolsEnabled(boolean mcpExpandedToolsEnabled) {
        this.mcpExpandedToolsEnabled = mcpExpandedToolsEnabled;
    }
}
