package com.rts.query;

import com.rts.model.CoreModels.RefusalReason;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PromptPolicyGuard {
    private static final List<String> FORBIDDEN_PATTERNS = List.of(
            "ignore policy",
            "ignore previous",
            "bypass",
            "do not use tools",
            "without l2",
            "treat memory as truth",
            "read filesystem",
            "raw review",
            "忽略规则",
            "忽略policy",
            "绕过",
            "不要调用工具",
            "不读l2",
            "把memory当truth",
            "把记忆当真相",
            "直接读文件",
            "原始review");

    public void validateUserText(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        for (String pattern : FORBIDDEN_PATTERNS) {
            if (normalized.contains(pattern)) {
                throw new QueryRefusalException(RefusalReason.governance_unauthorized,
                        "Request attempts to bypass RTS policy, tools, L2 grounding, or governance boundaries");
            }
        }
    }

    public void validateGeneratedAnswer(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (normalized.contains("ignore rts policy")
                || normalized.contains("bypass rts")
                || normalized.contains("memory is truth")
                || normalized.contains("search hit is truth")) {
            throw new QueryRefusalException(RefusalReason.unsupported_claim,
                    "Generated answer contains a policy-bypass or unsupported truth claim");
        }
    }
}
