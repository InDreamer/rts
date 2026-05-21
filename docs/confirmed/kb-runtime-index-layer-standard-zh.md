<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: standardize the concrete KB, runtime projection, and index layer shapes, file formats, generation order, and layer contracts
read_when:
  - 需要生成新的 KB pack
  - 需要发布新的 runtime projection
  - 需要定义 index layer 应该索引什么、不能索引什么
  - 需要固定 KB、runtime projection、index layer 的文件格式和生成标准
  - 需要判断下次生成器或 agent 应该产出哪些文件
skip_when:
  - 只需要理解 RTS 总体愿景
  - 只需要 PM 视角解释 runtime projection 每个目录的作用
  - 只需要调用已经运行的 API
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
  - sample-projection/runtime-store
  - kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split
  - src/main/java/com/rts/store/FileSystemProjectionStore.java
  - src/main/java/com/rts/query/QueryService.java
-->

# KB / Runtime Projection / Index Layer Standard

> 状态：confirmed generation standard
> 创建日期：2026-05-19
> 范围：固定 KB、runtime projection、index layer 三层的职责、文件长相、文件格式、生成顺序和层间契约
> 非目标：不定义 source ingestion 全流程，不定义 LLM input context envelope 细节，不把 HTML/report 变成 truth 格式

## 1. 固定结论

RTS 的规则材料必须固定成三层，而不是混成一个“可读文档库”：

```text
KB authoring layer
  -> runtime projection layer
  -> index/query layer
```

三层职责必须这样切：

| 层 | 拥有什么 | 默认格式 | 主要读者 | 是否拥有 truth |
|---|---|---|---|---|
| KB | governed truth graph：rule / lookup / helper、evidence、review、report、signoff、lineage | YAML + Markdown，必要时 CSV/TSV 附件 | 生成器、review agent、publisher、少量人工 reviewer | 是，形成 truth |
| runtime projection | approved truth 的不可变服务运行包：release、scope、manifest、L2、navigation、dependency、governance、permission、trace 边界 | JSON + JSONL + UTF-8 L2 JSON | RTS service、agent tool、publisher、index builder | 是，发布 truth |
| index layer | 从 projection 派生的定位、召回、消歧和查询计划材料 | Lucene/OpenSearch index、JSONL search docs、内存 map/table | resolver、search、query planner | 否，只定位 truth |

禁止的混层行为：

- index layer 不能直接读取 `kb/` 当默认 service truth。
- runtime query 不能读取 candidate、raw review notes、raw evidence 或 Git working tree 当默认 truth。
- object card、L0/L1、search hit、LLM answer、trace、HTML、report 都不能替代 L2 runtime object。
- LLM input context envelope 不是第四个 stored truth layer；它只是在运行时从 projection 中装箱。

## 2. 标准目录长相

### 2.1 KB authoring layer

新的 KB pack 必须写成：

```text
kb/{canonical_pack_id}/
  metadata.yaml
  README.md
  rules/
    {rule_id}.yaml
  lookups/
    {lookup_id}.yaml
  helpers/
    {helper_id}.yaml
  evidence/
    evidence-index.yaml
  review/
    review-index.yaml
  reports/
    extraction-report.md
    review-checklist.md
    closure-check.md
  attachments/            # optional; large tables, source snippets, normalized CSV/TSV
```

当前样本 `kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split/` 已经接近此标准；后续新 pack 应补齐更明确的 object type、scope inheritance 和 schema version。

### 2.2 Runtime projection layer

新的 projection 必须发布成：

```text
runtime-store/
  active-release.json
  releases/
    {release_id}/
      release-manifest.json
      scopes.jsonl
      object-manifest.jsonl
      caller-profiles.jsonl
      l2/
        rules/{rule_id}.json
        lookups/{lookup_id}.json
        helpers/{helper_id}.json
      navigation/
        object-cards.jsonl
        l0-l1-views.jsonl
        aliases.jsonl
        confusables.jsonl
      dependencies/
        dependency-edges.jsonl
        field-bindings.jsonl
      governance/
        governance-access-refs.jsonl
        evidence-summaries/{object_or_pack_id}.json
        review-summaries/{object_or_pack_id}.json
        report-summaries/{pack_or_release_id}.json
      index-artifacts/
        opensearch-docs.jsonl
        lucene/              # generated binary index; may be absent before rebuild
  traces/
    query-trace.jsonl
    llm-run-trace.jsonl
```

`runtime-store/releases/{release_id}/` 是不可变 release package。修改规则事实必须生成新 release，不允许原地修当前 release。

### 2.3 Index layer

Index layer 不新增人工 authoring 目录。它只能从 active projection 或指定 release projection 派生：

```text
runtime-store/releases/{release_id}/index-artifacts/
  opensearch-docs.jsonl      # portable search document export
  lucene/                    # local Java service index files
```

服务内还可以建立内存 map 或数据库表，但它们都必须等价于 projection 派生视图：

```text
by_uri
by_scope_object_id
by_scope_target_path
by_scope_source_anchor
dependency_adjacency
field_binding_index
```

这些索引可以丢弃重建；不能成为 canonical truth，也不能包含 projection 中不存在的业务断言。

## 3. KB 文件格式标准

### 3.1 `metadata.yaml`

`metadata.yaml` 是 pack 级入口，必须使用 YAML。

必须包含：

```yaml
schema_version: kb-pack-v1
pack:
  id: tradition-to-stella-fxd-ndf-cutoff-fixing-split
  name: Tradition -> Stella FXD.NDF cutoff-fixing split
  status: demo_signoff
  version: 0.2.0-photo-reconstructed
  created_on: 2026-04-02
  canonical_revision: photo-reconstructed-2026-05-06
scope:
  channel: tradition
  product: stella
  pack: fxd-ndf-cutoff-fixing
  domain: cutoff-fixing
  source_system: Tradition
  target_system: Stella
  product_scope: FXD.NDF
  target_area:
    - /scb:SCBML/.../conf:fixing
objects:
  rules: 6
  lookups: 1
  helpers: 1
indexes:
  evidence_index: evidence/evidence-index.yaml
  review_index: review/review-index.yaml
  extraction_report: reports/extraction-report.md
  review_checklist: reports/review-checklist.md
  closure_check: reports/closure-check.md
sources:
  pointer_file: Tradition_SCBML_mapping-ai-bundle.json
  sheets: []
  java: []
  xslt: []
  tests: []
modeling_decisions:
  - Main objects are kept thin.
```

Rules:

- `scope.channel/product/pack/domain` 是发布到 projection 的默认 scope。
- object 文件可以继承 pack scope；如果 object 自己声明 scope，必须与 pack scope 兼容。
- `status` 不能靠自然语言表达，必须是机器可读枚举。
- `canonical_revision` 必须能被 projection manifest 引用。

推荐状态枚举：

```text
candidate
ai_reviewed
human_review_required
demo_signoff
production_signoff
deprecated
superseded
```

### 3.2 `rules/{rule_id}.yaml`

Rule 文件必须描述一个 target 规则对象。格式使用 YAML。

标准形状：

```yaml
schema_version: kb-object-v1
id: rule_fxd_ndf_primary_rate_source
object_type: rule
status: demo_signoff
signoff_status: demo_signoff
source:
  reconstructed_from_photos:
    - IMG_9619.JPG
inputs:
  - name: fixing_currency1
    source_path: /FpML/trade/fxSingleLeg/nonDeliverableForward/fixing/quotedCurrencyPair/currency1/text()
logic:
  summary: >
    Resolve the hedge primary rate source from the shared cutoff lookup.
  pipeline:
    - op: read_xpath
      source: /FpML/.../currency1/text()
      out: fixing_currency1
    - op: lookup_value
      lookup: lk_fxd_ndf_cutoff_by_pair_and_locode
      return_field: hedge_primary_source
      using_inputs:
        fixing_currency1: fixing_currency1
      out: hedge_primary_source
target:
  kind: block
  path: /scb:SCBML/.../conf:primaryRateSource
  emits:
    - path: /scb:SCBML/.../conf:rateSource
      from_lookup: hedge_primary_source
dependencies:
  lookups:
    - lk_fxd_ndf_cutoff_by_pair_and_locode
  helpers: []
  rules: []
examples:
  - name: sample_usdtwd_primary_rate_source
    input:
      fixing_currency1: USD
    key_decision:
      matched_key: USDTWD|TW
    result:
      hedge_primary_source: RTRS
warnings:
  - Photo-reconstructed source accepted for local demo signoff.
```

Required rule fields:

- `id`
- `object_type: rule`
- `status`
- `signoff_status`
- `source`
- `inputs`
- `logic.summary`
- `logic.pipeline`
- `target.kind`
- `target.path`
- `dependencies`
- `examples`

Rule 文件不应该包含长 evidence 原文、长 review 过程、chat history 或 HTML 展示内容。那些内容放到 `evidence/`、`review/`、`reports/`。

### 3.3 `lookups/{lookup_id}.yaml`

Lookup 文件描述可复用查表逻辑，不应该只写表名。

标准形状：

```yaml
schema_version: kb-object-v1
id: lk_fxd_ndf_cutoff_by_pair_and_locode
object_type: lookup
status: demo_signoff
signoff_status: demo_signoff
source:
  mapping_table: TraditionStella Cutoff
inputs:
  - name: fixing_currency1
    source_path: /FpML/.../currency1/text()
logic:
  summary: >
    Query by forward pair first, then reverse pair fallback.
  pipeline:
    - op: concat
      args: [fixing_currency1, fixing_currency2]
      out: forward_pair
    - op: compose_key
      template: "{forward_pair}|{cutoff_code}"
      out: forward_key
    - op: lookup_value
      key: "{forward_key}"
      return_field: requested_field
      out: forward_result
output:
  fields:
    - name: cutoff_name
      return_field: CUTOFF
    - name: hedge_primary_source
      return_field: PRIMARY_SOUCE_FOR_HEDGE
dependencies: []
examples: []
```

If the lookup table is large, keep the table itself in `attachments/` as CSV/TSV and reference it from `source` or `table_ref`; do not inline large tables into the lookup object.

### 3.4 `helpers/{helper_id}.yaml`

Helper 文件描述中间语义或复用逻辑。

标准形状：

```yaml
schema_version: kb-object-v1
id: hlp_fxd_ndf_fixing_quoted_currency_pair
object_type: helper
status: demo_signoff
signoff_status: demo_signoff
source:
  reconstructed_from_photos:
    - IMG_9611.JPG
inputs:
  - name: source_currency1
    source_path: /FpML/.../currency1/text()
logic:
  summary: >
    Detect inverse quoteBasis and swap currencies.
  pipeline:
    - op: concat
      args: [source_currency1, source_currency2]
      out: source_pair
    - op: equals
      left: source_quote_basis
      right: "{source_pair}"
      out: is_inverse_quote
output:
  fields:
    - target_currency1
    - target_currency2
    - target_quote_basis
dependencies: []
examples: []
```

Helper 的输出必须能被 rule 的 `logic.pipeline` 或 `target.emits` 引用。

### 3.5 Governance side files

`evidence/evidence-index.yaml` 记录 source-backed evidence，不直接成为默认 runtime answer。

标准形状：

```yaml
schema_version: evidence-index-v1
evidence:
  - evidence_id: ev_rule_fxd_ndf_primary_rate_source_photo
    object_ids:
      - rule_fxd_ndf_primary_rate_source
    source_type: photo
    locator:
      file: IMG_9619.JPG
      region: visible VS Code editor
    claims:
      - hedge primary source is read from shared cutoff lookup
    limitations:
      - photo reconstruction requires production recheck
```

`review/review-index.yaml` 记录 review、open question、human decision 和 signoff gate。

标准形状：

```yaml
schema_version: review-index-v1
reviews:
  - review_id: rv_rule_fxd_ndf_primary_rate_source
    object_ids:
      - rule_fxd_ndf_primary_rate_source
    status: demo_signoff
    open_questions:
      - cutoff_code_semantics
    human_decisions:
      - accepted for local demo signoff
    production_gate: recheck source-photo reconstruction before production signoff
```

`reports/*.md` 可以解释提取、review、closure，但 report body 不是默认 runtime truth。Publisher 只能把允许暴露的摘要投影到 governance summary。

## 4. Runtime projection 文件格式标准

Runtime projection 使用 JSON/JSONL。所有 JSONL 文件必须是一行一个完整 JSON object，UTF-8，无 markdown fence，无注释。

### 4.1 `active-release.json`

```json
{
  "active_release_id": "rel-2026-05-06-photo-fxd-ndf-cutoff",
  "rollback_target_release_id": "rel-2026-05-06",
  "updated_at": "2026-05-07T00:00:00Z",
  "updated_by": "publisher"
}
```

Contract:

- service 默认只读取 `active_release_id` 指向的 release。
- 切换 active release 是服务运行态操作，不修改 release package 内容。
- rollback target 必须指向一个已存在、可加载的旧 release。

### 4.2 `release-manifest.json`

```json
{
  "release_id": "rel-2026-05-06-photo-fxd-ndf-cutoff",
  "canonical_revision": "photo-reconstructed-2026-05-06",
  "projection_schema_version": "runtime-multiview-v1",
  "card_schema_version": "card-v1",
  "summary_schema_version": "summary-v1",
  "activation_state": "released",
  "generated_at": "2026-05-06T13:30:00Z",
  "released_at": "2026-05-06T13:31:00Z",
  "rollback_target_release_id": "rel-2026-05-06",
  "content_hash_summary": "sha256-or-release-hash-summary",
  "blocking_issues_count": 0,
  "created_at": "2026-05-06T13:30:00Z"
}
```

Contract:

- `projection_schema_version` 当前固定为 `runtime-multiview-v1`。
- `activation_state` 只允许默认服务读取 `active` 或 `released`。
- `blocking_issues_count` 必须为 `0` 才能进入默认服务面。
- manifest 是 projection admission 的第一道门；没有有效 manifest，就没有 runtime truth。

### 4.3 `scopes.jsonl`

每行一个 scope：

```json
{"release_id":"rel-2026-05-06-photo-fxd-ndf-cutoff","channel":"tradition","product":"stella","pack":"fxd-ndf-cutoff-fixing","domain":"cutoff-fixing","active_flag":true,"permission_boundary":"demo-signoff-photo","precedence_policy":"product-specific","deprecated_flag":false,"superseded_by":null}
```

Contract:

- Query resolver 必须先解析 scope，再召回对象。
- Scope 是 truth safety boundary，不是普通搜索 filter。
- Object manifest 中每个对象必须匹配一个 active scope。

### 4.4 `object-manifest.jsonl`

每行一个 released object：

```json
{
  "uri": "rts://tradition/stella/fxd-ndf-cutoff-fixing/photo-reconstructed/rules/rule_fxd_ndf_primary_rate_source",
  "release_id": "rel-2026-05-06-photo-fxd-ndf-cutoff",
  "object_id": "rule_fxd_ndf_primary_rate_source",
  "object_type": "rule",
  "channel": "tradition",
  "product": "stella",
  "pack": "fxd-ndf-cutoff-fixing",
  "domain": "cutoff-fixing",
  "target_path": "fxd.ndf.fixing.primaryRateSource",
  "source_anchors": [
    "/FpML/trade/fxSingleLeg/nonDeliverableForward/fixing/quotedCurrencyPair/currency1/text()"
  ],
  "content_hash": "sha256:...",
  "card_ref": "rts://.../rule_fxd_ndf_primary_rate_source#card",
  "content_ref": "rts://.../rule_fxd_ndf_primary_rate_source#l2",
  "schema_version": "object-v1",
  "state": "released",
  "l2_storage_ref": "rules/rule_fxd_ndf_primary_rate_source.json"
}
```

Contract:

- `uri` must be globally stable for a release.
- `(release_id, channel, product, pack, domain, target_path)` must not produce duplicate active rules unless explicit precedence/supersession is modeled.
- `content_hash` must equal the exact L2 file hash.
- `l2_storage_ref` must stay under `l2/`; arbitrary path reads are forbidden.

### 4.5 L2 runtime objects

L2 is the service-readable fact body. It must be JSON and must not be reduced to a prose summary.

Common envelope:

```json
{
  "schema_version": "l2-runtime-v1",
  "truth_role": "l2_fact",
  "uri": "rts://...",
  "id": "rule_fxd_ndf_primary_rate_source",
  "object_type": "rule",
  "release_id": "rel-2026-05-06-photo-fxd-ndf-cutoff",
  "canonical_revision": "photo-reconstructed-2026-05-06",
  "status": "demo-signoff-photo-reconstructed",
  "signoff_status": "demo_signoff",
  "runtime_state": "released",
  "scope": {
    "channel": "tradition",
    "product": "stella",
    "pack": "fxd-ndf-cutoff-fixing",
    "domain": "cutoff-fixing"
  },
  "visibility": "runtime_operational",
  "source_lineage": {},
  "review_state": {},
  "inputs": [],
  "logic": {},
  "dependencies": [],
  "examples": [],
  "operational_warnings": [],
  "governance_summary_refs": [],
  "content_hash": "sha256:..."
}
```

Rule L2 must also contain `target`. Lookup L2 must also contain `output`. Helper L2 must also contain `output`.

Allowed L2 truth fields:

- `inputs`
- `logic.pipeline`
- `target`
- `output`
- `dependencies`
- `examples`
- `operational_warnings`
- `review_state.status`
- `governance_summary_refs`
- `source_lineage`

Disallowed L2 content:

- raw chat transcript
- raw review history that is not authorized for operational view
- long evidence body
- HTML/dashboard text
- generated LLM explanation that has not been accepted into governed object fields

### 4.6 `navigation/object-cards.jsonl`

Object card is navigation and resolver material, not final truth.

```json
{
  "uri": "rts://...",
  "release_id": "rel-2026-05-06-photo-fxd-ndf-cutoff",
  "object_type": "rule",
  "card_json": {
    "object_id": "rule_fxd_ndf_primary_rate_source",
    "summary": "Primary rate source comes from shared cutoff lookup.",
    "target_path": "fxd.ndf.fixing.primaryRateSource",
    "logic_operation_types": ["read_xpath", "lookup_value", "emit_value"],
    "lookup_dependencies": ["lk_fxd_ndf_cutoff_by_pair_and_locode"],
    "l2_content_ref": "rts://...#l2",
    "l2_content_hash": "sha256:...",
    "view_role": "navigation_not_truth"
  },
  "search_text": "Primary rate source lookup reverse pair fallback ...",
  "risk_flags": [],
  "applicability": [],
  "not_applicable": [],
  "override_refs": [],
  "supersession_refs": []
}
```

Contract:

- Card can summarize and flatten for search.
- Card must point to L2.
- Card cannot add business logic that is absent from L2/KB.

### 4.7 `navigation/l0-l1-views.jsonl`

L0/L1 are deterministic, regenerable navigation views.

```json
{
  "uri": "rts://...",
  "release_id": "rel-2026-05-06-photo-fxd-ndf-cutoff",
  "view_type": "l0_l1",
  "scope": {
    "channel": "tradition",
    "product": "stella",
    "pack": "fxd-ndf-cutoff-fixing",
    "domain": "cutoff-fixing"
  },
  "l0_text": "Primary rate source and source page come from shared cutoff lookup.",
  "l1_json": {
    "object_id": "rule_fxd_ndf_primary_rate_source",
    "target_path": "fxd.ndf.fixing.primaryRateSource",
    "lookup_dependencies": ["lk_fxd_ndf_cutoff_by_pair_and_locode"],
    "view_role": "navigation_not_truth"
  },
  "search_text": "Primary rate source lookup ...",
  "content_hash": "sha256:...",
  "schema_version": "navigation-v1"
}
```

Contract:

- `view_role` must be `navigation_not_truth` or equivalent.
- L0/L1 must be release-bound and hashable.
- L0/L1 can guide selection, but final answer must read L2.

### 4.8 `navigation/aliases.jsonl`

Alias records are optional but recommended:

```json
{"release_id":"rel-2026-05-06-photo-fxd-ndf-cutoff","uri":"rts://.../rules/rule_fxd_ndf_primary_rate_source","alias":"primary source page","alias_type":"business_term","weight":0.8}
```

Aliases help recall; they do not create truth.

### 4.9 `navigation/confusables.jsonl`

Confusable records prevent similar-but-wrong matches:

```json
{"release_id":"rel-2026-05-06-photo-fxd-ndf-cutoff","uri":"rts://.../rules/rule_fxd_ndf_primary_rate_source","confusable_with_uri":"rts://.../rules/rule_fxo_primary_rate_source","reason":"FXO option source columns are outside FXD.NDF cutoff-fixing scope."}
```

Confusable records are resolver safety material. They must not broaden query scope.

### 4.10 `dependencies/dependency-edges.jsonl`

```json
{"release_id":"rel-2026-05-06-photo-fxd-ndf-cutoff","from_uri":"rts://.../rules/rule_fxd_ndf_primary_rate_source","to_uri":"rts://.../lookups/lk_fxd_ndf_cutoff_by_pair_and_locode","edge_type":"rule_to_lookup","required_flag":true,"direction":"forward","traversal_purpose":"explain_rule"}
```

Allowed edge types:

```text
rule_to_lookup
rule_to_helper
helper_to_lookup
helper_to_helper
rule_to_rule
rule_to_source_anchor
rule_to_target_path
lookup_to_consumer
helper_to_consumer
```

Contract:

- Required object dependencies must point to released objects in the same release.
- Dependencies can support explanation, impact, test planning and context loading.
- Dependencies cannot silently widen scope or bypass permission.

### 4.11 `dependencies/field-bindings.jsonl`

```json
{"release_id":"rel-2026-05-06-photo-fxd-ndf-cutoff","object_uri":"rts://.../rules/rule_fxd_ndf_primary_rate_source","binding_type":"target_output","source_field":"fixing_currency1","target_field":"fxd.ndf.fixing.primaryRateSource.rateSource","output_field":"hedge_primary_source","via_uri":"rts://.../lookups/lk_fxd_ndf_cutoff_by_pair_and_locode","purpose":"explain_rule"}
```

Field bindings make source/target/output relationships queryable without parsing L2 every time.

### 4.12 `governance/governance-access-refs.jsonl`

```json
{
  "uri": "rts://...",
  "release_id": "rel-2026-05-06-photo-fxd-ndf-cutoff",
  "access_level": "governance_tools",
  "redaction_state": "summary_only",
  "evidence_summary_refs": ["evidence-summary-rule_fxd_ndf_primary_rate_source"],
  "review_summary_refs": ["review-summary-rule_fxd_ndf_primary_rate_source"],
  "report_summary_refs": ["report-summary-photo-pack-closure"],
  "open_questions": ["cutoff_code_semantics"],
  "production_gate": "recheck before production signoff",
  "source_locator_summary": "Photo reconstruction plus workbook/code locators."
}
```

Contract:

- Governance access refs only point to published summaries, not raw evidence.
- Governance material requires permission and must be traced when used.

### 4.13 Governance summaries

`governance/*-summaries/*.json` must use the same summary shape:

```json
{
  "summary_id": "review-summary-rule_fxd_ndf_primary_rate_source",
  "uri": "rts://...",
  "release_id": "rel-2026-05-06-photo-fxd-ndf-cutoff",
  "summary_type": "review",
  "truth_role": "authorized_governance_summary",
  "title": "Review summary",
  "summary": "Accepted for local demo signoff; production recheck required.",
  "source_locator": {},
  "warnings": []
}
```

Allowed `summary_type` values:

```text
evidence
review
report
```

### 4.14 `caller-profiles.jsonl`

```json
{"caller_id":"tester","api_key_hash":"sha256:...","allowed_channels":["tradition"],"allowed_products":["stella"],"allowed_packs":["fxd-ndf-cutoff-fixing"],"allowed_entrypoints":["find","query","ask","objects_get","objects_content","objects_dependencies","trace"],"allowed_output_modes":["*"],"active_flag":true}
```

Caller profiles are runtime access control, not KB authoring content.

### 4.15 `traces/*.jsonl`

Trace files are append-only runtime audit logs. They do not live under a release because traces are produced by runtime use of the active or requested release.

Trace may include:

- `trace_id`
- `entrypoint`
- `query_plan`
- `resolved_scope`
- `candidate_uris`
- `selected_uris`
- `l2_read_uris`
- `refusal_reason`
- `release_id`
- `tool_steps`
- `grounding_map`
- `context_hash`
- `status`

Trace is proof of service behavior, not new truth.

## 5. Index layer standard

### 5.1 What the index may read

Index builder may read only:

- `release-manifest.json`
- `scopes.jsonl`
- `object-manifest.jsonl`
- `navigation/object-cards.jsonl`
- `navigation/l0-l1-views.jsonl`
- `navigation/aliases.jsonl`
- `navigation/confusables.jsonl`
- `dependencies/*.jsonl` when building graph-aware indexes

Index builder must not read:

- raw `kb/` files
- candidate packs
- raw evidence
- raw review notes
- report body as truth
- LLM output as truth
- traces as truth

### 5.2 `index-artifacts/opensearch-docs.jsonl`

Portable search docs should be JSONL with one document per object:

```json
{
  "uri": "rts://...",
  "release_id": "rel-2026-05-06-photo-fxd-ndf-cutoff",
  "channel": "tradition",
  "product": "stella",
  "pack": "fxd-ndf-cutoff-fixing",
  "domain": "cutoff-fixing",
  "object_type": "rule",
  "object_id": "rule_fxd_ndf_primary_rate_source",
  "target_path": "fxd.ndf.fixing.primaryRateSource",
  "source_anchors": [],
  "searchable_text": "Primary rate source lookup reverse pair fallback ...",
  "card_text": "Primary rate source comes from shared cutoff lookup.",
  "l0_text": "Primary rate source and source page come from shared cutoff lookup.",
  "l1_text": "Primary rate source lookup ...",
  "view_role": "navigation_not_truth",
  "content_hash": "sha256:..."
}
```

Contract:

- Every search doc must point back to an object manifest entry.
- Search score is never truth confidence.
- Search doc may repeat text from card/L0/L1, but may not add new rule assertions.

### 5.3 Lucene index

Local Lucene files under `index-artifacts/lucene/` are generated binary artifacts. They may be absent, stale, or rebuilt.

The current Java index uses fields equivalent to:

```text
uri
release_id
channel
product
pack
domain
object_type
object_id
target_path
source_anchor
business_terms
searchable_text
card_text
l0_text
l1_text
```

Lucene search must filter by `release_id`, `channel`, `product`, `pack`, and `domain` before returning candidates.

## 6. Layer contracts

### 6.1 KB -> Runtime Projection

Publisher input:

- signed or explicitly accepted KB pack
- pack metadata
- rule / lookup / helper YAML
- evidence index
- review index
- allowed report summaries

Publisher output:

- immutable release package
- L2 runtime JSON objects
- object manifest and content refs
- navigation views
- dependency and field binding views
- governance access refs and summaries
- caller profiles or permission seed
- optional index export

Publisher must:

1. Reject pack if required object files are missing.
2. Reject pack if required dependency objects are absent.
3. Reject pack if target coverage conflict is unresolved.
4. Reject pack if signoff state is insufficient for default service truth.
5. Generate stable URIs.
6. Generate release id and manifest.
7. Write L2 JSON first.
8. Compute `sha256:` hash over exact UTF-8 L2 JSON content.
9. Write object manifest with matching content hashes.
10. Write navigation, dependency, governance and permission files.
11. Validate projection before activation.

### 6.2 Runtime Projection -> Index Layer

Index builder input:

- projection manifest
- object manifest
- object cards
- L0/L1 views
- aliases and confusables
- dependency edges and field bindings when needed

Index builder output:

- Lucene or OpenSearch documents
- deterministic lookup maps
- dependency adjacency maps

Index builder must:

- preserve release and scope filters
- keep navigation-only role labels
- keep object URI back pointers
- remain rebuildable from projection
- avoid direct KB reads

### 6.3 Index Layer -> Query Service

Query service flow:

```text
load active release
  -> validate manifest
  -> parse or receive scope
  -> check caller permission
  -> deterministic lookup by URI/object id/target path/source anchor
  -> scoped search over card/L0/L1/index docs
  -> select candidate
  -> read object manifest entry
  -> read L2 content and verify hash
  -> traverse allowed dependencies
  -> assemble answer
  -> validate grounding
  -> append trace
```

Query service must refuse or degrade when:

- scope is unclear
- caller is unauthorized
- active release is missing
- manifest is invalid
- schema version is unsupported
- only card/L0/L1/search hit exists but L2 is missing
- dependency points to unreleased object
- governance material is requested without permission
- only semantic similarity exists without structured object match

## 7. Generation standard for next packs

When generating a new KB and projection, follow this order exactly.

### Step 1: choose identity and scope

Decide:

```text
canonical_pack_id
channel
product
pack
domain
canonical_revision
release_id
```

Use these identifiers consistently across KB, projection and index.

### Step 2: generate KB pack

Produce:

```text
metadata.yaml
README.md
rules/*.yaml
lookups/*.yaml
helpers/*.yaml
evidence/evidence-index.yaml
review/review-index.yaml
reports/extraction-report.md
reports/review-checklist.md
reports/closure-check.md
```

Minimum quality gate:

- all object IDs are stable and unique inside the pack
- every rule has inputs, logic, target, dependencies and examples
- every lookup has inputs, logic, outputs and examples
- every helper has inputs, logic, outputs and examples
- evidence index references object IDs
- review index records status, open questions and production gate
- reports do not carry the only copy of rule truth

### Step 3: run KB validation

Validation should check:

- YAML parse success
- required fields by object type
- dependency IDs exist
- target path duplicates
- source anchors present
- status and signoff state are allowed
- evidence/review references point to existing object IDs

### Step 4: publish runtime projection

Generate:

```text
active-release.json
release-manifest.json
scopes.jsonl
object-manifest.jsonl
l2/rules/*.json
l2/lookups/*.json
l2/helpers/*.json
navigation/object-cards.jsonl
navigation/l0-l1-views.jsonl
navigation/aliases.jsonl
navigation/confusables.jsonl
dependencies/dependency-edges.jsonl
dependencies/field-bindings.jsonl
governance/governance-access-refs.jsonl
governance/evidence-summaries/*.json
governance/review-summaries/*.json
governance/report-summaries/*.json
caller-profiles.jsonl
```

Projection generation rules:

- L2 objects keep structured logic, not only prose.
- Object cards and L0/L1 are generated from L2/KB and marked navigation-only.
- Governance summaries are permissioned summaries, not raw evidence dumps.
- Dependencies are explicit graph edges, not inferred at query time.
- Field bindings are generated when source/target/output explanation is needed.
- Content hashes are computed after final JSON serialization and stored in manifest/content refs.

### Step 5: validate runtime projection

Projection validation must reject:

- unsupported `projection_schema_version`
- activation state not `active` or `released`
- nonzero `blocking_issues_count`
- duplicate object URI
- object not `released`
- object scope not active
- duplicate target rule in same scope
- missing content ref
- content hash mismatch
- missing L2 file
- L2 path outside `l2/`
- missing object card
- missing navigation view
- missing governance access ref
- governance summary ref without summary file
- required dependency to unreleased object
- field binding to unreleased object

### Step 6: build index layer

Generate or rebuild:

```text
index-artifacts/opensearch-docs.jsonl
index-artifacts/lucene/
```

Index quality gate:

- every index doc points to a released object URI
- every index doc carries release and scope fields
- search text comes from card/L0/L1/object identity only
- target path and object id are boosted or deterministically searchable
- no index document includes raw KB-only notes or unauthorized governance details

### Step 7: smoke test service reads

Minimum smoke tests:

```text
load active release
find object by business query
get object envelope
read object L2
read dependencies
run query
read trace
```

The success condition is not merely "search returns something"; success requires L2 read, hash validation and trace.

## 8. Format decisions

Use these formats unless a confirmed doc changes the standard:

| Material | Format | Reason |
|---|---|---|
| KB metadata | YAML | authorable, structured, diffable |
| KB rule / lookup / helper | YAML | structured but easy for AI/reviewer maintenance |
| KB evidence/review indexes | YAML | governance relationships and source locators |
| KB reports | Markdown | narrative review and closure notes |
| Large lookup/source tables | CSV/TSV under `attachments/` | table-native and diffable |
| runtime manifest/scope/object/navigation/dependency/governance | JSON/JSONL | service-parseable, schema-validatable, streamable |
| L2 runtime objects | JSON | exact hash, stable service parse, structured truth |
| index export | JSONL | portable search-document export |
| local Lucene | binary generated files | implementation artifact, rebuildable |
| HTML/PDF/dashboard | presentation only | never truth source |

## 9. Role labels

New projection artifacts should standardize role labels:

```text
truth_role: l2_fact
truth_role: authorized_governance_summary
truth_role: dependency_fact
view_role: navigation_not_truth
view_role: search_only
view_role: presentation_only
```

Default interpretation:

- `l2_fact` can support factual claims.
- `dependency_fact` can support relationship and impact claims, subject to traversal purpose and permission.
- `authorized_governance_summary` can support evidence/review/risk claims when caller is authorized.
- `navigation_not_truth` can locate candidates but cannot support final factual claims alone.
- `search_only` can rank candidates but cannot support final factual claims alone.
- `presentation_only` is never consumed as service truth.

## 10. Compatibility with current repository

Current files already implement most of this standard:

- `kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split/` demonstrates the KB pack shape.
- `sample-projection/runtime-store/releases/rel-2026-05-06-photo-fxd-ndf-cutoff/` demonstrates the runtime projection shape.
- `src/main/java/com/rts/store/FileSystemProjectionStore.java` is the current admission and load boundary.
- `src/main/java/com/rts/store/LocalFileContentStore.java` enforces local L2 path and hash checks.
- `src/main/java/com/rts/index/LuceneIndexService.java` demonstrates index derivation from object manifest, object card and L0/L1.
- `src/main/java/com/rts/query/QueryService.java` demonstrates active release, scope, permission, candidate, L2, dependency and trace flow.

Known compatibility note:

- Some existing KB YAML files do not yet declare `schema_version` or `object_type`. They remain valid historical samples, but new generated KB packs should include those fields.
- Some existing L2 JSON files may not yet declare `truth_role`. New projection releases should add it.

## 11. The shortest standard

If a future agent only remembers one version, use this:

```text
KB:
  YAML/Markdown authoring pack.
  Owns full governed truth formation.
  Main objects are rules/lookups/helpers; governance side files are evidence/review/reports.

Runtime projection:
  JSON/JSONL immutable release package.
  Owns approved truth publication.
  L2 is fact body; manifest/scope/object manifest gate access; navigation finds; dependency explains; governance is permissioned.

Index layer:
  Generated, rebuildable, navigation-only.
  Reads projection, never raw KB.
  Helps find objects, then query must return to L2 and trace the read.
```
