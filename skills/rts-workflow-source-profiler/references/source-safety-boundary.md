# Source Safety Boundary

Use this boundary when profiling restricted company source.

Allowed:

- Read source files needed for the named workflow.
- Record stable locators: repo, revision, path, symbol, line range, table name, test name, XML path, XSLT template name.
- Record short summaries of behavior.
- Record hashes when available.
- Record unresolved questions.
- Write output under `sources/{source_bundle_id}/`.

Forbidden:

- Modify source code.
- Copy long proprietary source excerpts into RTS.
- Store secrets, credentials, tokens, connection strings, private keys, or production data samples.
- Treat inferred behavior as confirmed truth.
- Generate `kb/{pack_id}/`, snapshots, or runtime projection files from this skill.
- Claim full workflow coverage if only a local field or partial path was inspected.

If a source cannot be read, write it as a limitation:

```yaml
limitation_id: lim-db-runtime-data-unavailable
source_type: postgresql
reason: "Runtime table rows are not accessible in this environment."
impact: "Lookup behavior can be modeled from schema but not confirmed from data."
blocks:
  - kb_truth
  - mvp_completion
```
