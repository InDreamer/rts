package com.rts.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rts")
public class RtsProperties {
    private Path storeRoot = Path.of("runtime-store");
    private boolean llmEnabled = false;
    private String llmModel = "disabled-local";
    private String llmBaseUrl = "https://api.openai.com/v1";
    private String llmApiKey = "";
    private int llmMaxTokens = 600;
    private String adminCallerId = "admin";
    private String adminApiKeyHash = "";
    private int maxToolCalls = 8;
    private int maxL2Objects = 5;
    private int maxDependencyDepth = 2;

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
}
