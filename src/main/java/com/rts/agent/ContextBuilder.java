package com.rts.agent;

import com.rts.model.AgentServiceModels.AgentSession;
import com.rts.model.AgentServiceModels.ContextItem;
import com.rts.model.AgentServiceModels.ContextKind;
import com.rts.model.AgentServiceModels.ContextSnapshot;
import com.rts.model.AgentServiceModels.GroundingEvidence;
import com.rts.model.AgentServiceModels.GroundingMap;
import com.rts.model.CoreModels.ScopeKey;
import com.rts.store.Hashing;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ContextBuilder {
    public ContextSnapshot agentRunSnapshot(String callerId, ScopeKey scope, GroundingMap groundingMap) {
        List<ContextItem> items = groundingMap == null || groundingMap.claims() == null ? List.of() : groundingMap.claims().stream()
                .flatMap(claim -> claim.groundedBy() == null ? java.util.stream.Stream.<GroundingEvidence>empty() : claim.groundedBy().stream())
                .filter(evidence -> evidence.objectUri() != null && evidence.l2Hash() != null)
                .map(evidence -> new ContextItem(ContextKind.l2_fact, "agent_run", true, evidence.objectUri(), evidence.l2Hash(), null))
                .distinct()
                .toList();
        return new ContextSnapshot(null, callerId, new AgentSession(null, callerId, scope, null, null, false), List.of(), items,
                List.of("AgentRun context snapshot preserves truth-eligible L2 URI/hash pointers only; memory and external inputs remain truth-ineligible."),
                Hashing.sha256(String.valueOf(items)),
                items.size(),
                (int) items.stream().filter(ContextItem::truthEligible).count(),
                estimatedTokens(items),
                "hash_inputs_keep_uri_and_content_hash_only");
    }

    private int estimatedTokens(List<ContextItem> items) {
        int chars = items.stream()
                .mapToInt(item -> String.valueOf(item.objectUri()).length()
                        + String.valueOf(item.hash()).length()
                        + String.valueOf(item.text()).length())
                .sum();
        return items.isEmpty() ? 0 : Math.max(1, chars / 4);
    }
}
