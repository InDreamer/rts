package com.rts.query;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AliasEntityService {
    private static final Map<String, List<String>> ALIASES = Map.of(
            "fixing time", List.of("fixingTime", "hourMinuteTime", "businessCenter", "cutoff"),
            "cutoff", List.of("fixing time", "cutoff lookup", "business center"),
            "cortex", List.of("secondary rate source", "source page"),
            "quoted currency pair", List.of("quotedCurrencyPair", "quoteBasis", "currency1", "currency2"),
            "amount", List.of("payment.amount", "src.amount", "rounding"),
            "currency", List.of("lookup_currency", "ISO currency", "src.currency"));

    public String expand(String query, List<String> anchors) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        if (query != null && !query.isBlank()) {
            terms.add(query);
        }
        if (anchors != null) {
            terms.addAll(anchors);
        }
        String normalized = String.join(" ", terms).toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entry : ALIASES.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                terms.add(entry.getKey());
                terms.addAll(entry.getValue());
            }
        }
        return String.join(" ", terms);
    }
}
