# Rule / Lookup / Helper Modeling

Use this split:

| Type | Use For | Examples |
|---|---|---|
| Rule | A business output, target XML structure, routing decision, field transformation, or emitted downstream block | Set SCBML target path from FpML source fields; choose outbound payload section |
| Lookup | A reusable table, enum, DB mapping, Excel mapping, static code map, or config-backed mapping | currency mapping, product mapping, rate source table |
| Helper | Reusable parsing, normalization, string composition, fallback, or selection logic consumed by rules/lookups | normalize pair, parse date/time, choose first non-empty value |

Workflow coverage must include:

- inbound gRPC
- message classification
- input XML/FpML semantic fields
- Java/Camel/XSLT transformation steps
- DB/Excel/enum/config mapping
- output XML/SCBML target paths
- Solace outbound
- fallback/default/error paths
- test evidence or test gaps

When sources conflict:

1. Do not choose silently.
2. Create a review decision or unresolved review item.
3. If user judgment is required, mark `requires_user_confirmation`.
