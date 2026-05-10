package com.rts.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.query.QueryRefusalException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableConfigurationProperties(RtsProperties.class)
public class RtsConfiguration implements WebMvcConfigurer {
    private static final java.util.Set<String> MINIMAL_MCP_TOOLS = java.util.Set.of(
            "rts_find_objects",
            "rts_read_object",
            "rts_get_dependencies",
            "rts_ask",
            "rts_get_trace");

    private final RtsProperties properties;

    public RtsConfiguration(RtsProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mcpExpandedToolGuard()).addPathPatterns("/mcp/tools/**");
    }

    private HandlerInterceptor mcpExpandedToolGuard() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (properties.isMcpExpandedToolsEnabled() || !"POST".equalsIgnoreCase(request.getMethod())) {
                    return true;
                }
                String toolName = request.getRequestURI().replaceFirst("^.*/mcp/tools/", "");
                if (MINIMAL_MCP_TOOLS.contains(toolName)) {
                    return true;
                }
                throw new QueryRefusalException(RefusalReason.governance_unauthorized,
                        "Expanded MCP tool is disabled by RTS_MCP_EXPANDED_TOOLS_ENABLED=false: " + toolName);
            }
        };
    }
}
