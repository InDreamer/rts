<!-- docmeta
role: leaf
layer: 3
parent: docs/reference/README.md
children: []
summary: RTS project background and pack/object model distilled from the historical handoff prompt
read_when:
  - 需要了解 Tradition -> Stella 作为最早样例的业务背景
  - 需要理解 canonical pack 的目录结构和对象模型
  - 需要查看 target_rule、lookup_definition、evidence、review 的简化示例
skip_when:
  - 只需要最高层项目总纲
  - 只需要当前工程状态
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
-->

# RTS Project Background and Pack Model

This document keeps the useful project background and pack model from the historical handoff prompt. It is reference material, not the current project baseline.

## 1. Original Sample Context

The earliest concrete RTS sample was `Tradition -> Stella`.

- **Source system**: Tradition
- **Target system**: Stella
- **Source message format**: FpML XML
- **Target message format**: SCBM/FpML XML
- **Example products**: COMMON, FXD_NDF, FXO

This sample helps explain RTS, but it is not the boundary of RTS. RTS should generalize to other source-target transformation channels.

## 2. Source Materials

RTS source material can include many forms of implementation and business truth:

- business documents
- Java extractors
- XSLT templates
- Excel mapping workbooks
- database/static mapping tables
- examples and expected outputs
- reviewer decisions and clarification notes

Source material is not automatically truth. RTS uses it as evidence for candidate rules, conflicts, review decisions, and approved truth.

## 3. Minimal Rule Idea

A transformation rule should be understandable through three questions:

```text
source → logic → target
```

- **source**: where the value comes from
- **logic**: how it is transformed
- **target**: where it is emitted

This keeps rule objects readable and helps AI, reviewers, developers, and testers discuss the same rule.

## 4. Canonical Pack Structure

A canonical pack is the governance unit for related transformation rules.

Typical structure:

```text
<pack-root>/
  metadata.yaml
  README.md
  rules/
    rule_*.yaml
  lookups/
    lk_*.yaml
  helpers/
    hlp_*.yaml
  evidence/
    evidence-index.yaml
  review/
    review-index.yaml
  reports/
    extraction-report.md
    closure-check.md
    review-checklist.md
```

## 5. Object Groups

### Main objects

These describe what the transformation does:

- `target_rule`: target field or block generation logic
- `lookup_definition`: mapping or static lookup behavior
- `helper_definition`: reusable stable intermediate logic

### Governance side layers

These explain why the rule is trusted, disputed, or ready:

- `evidence/`: source references, code/XSLT/Excel support, source spans
- `review/`: ambiguity, open items, review conclusions, signoff state
- `reports/`: extraction/review summaries and handoff materials

Main objects should stay focused. Evidence, review, ambiguity, approval history, and long trace should not be stuffed into every rule body.

## 6. Runtime View Is Not The Full Pack

The canonical pack is wider than the ordinary runtime view.

Ordinary runtime retrieval should be low-noise and focused on signed/approved objects such as rules, lookups, helpers, and essential metadata.

Evidence, review, reports, conflicts, and adjudication records should remain available through permissioned governance or explanation views when needed.

This does not mean the canonical pack is for humans and the runtime view is for machines. Both should be machine-first. The runtime view is narrower because of release state, service purpose, context budget, and permission boundaries, not because it is allowed to become a lossy summary of rule semantics.

## 7. Simplified target_rule Shape

```yaml
id: rule_scbml_initiated_timestamp
status: draft
signoff_status: none

source:
  inputs:
    - name: creationTimestamp
      source_path: /fpML/header/creationTimestamp/text()

logic:
  summary: Read the source creationTimestamp, convert it, and emit initiatedTimestamp.
  pipeline:
    - op: read_xpath
      out: raw_timestamp
    - op: format_datetime
      in: raw_timestamp
      out: converted_timestamp
    - op: emit_value
      from: converted_timestamp

target:
  kind: value
  path: /scb:SCBM/scb:header/scb:originationDetails/scb:initiatedTimestamp/text()

dependencies:
  lookups: []
  helpers: []
  rules: []

examples:
  - name: iso_to_z_conversion
    input:
      creationTimestamp: "2025-09-10T14:30:00"
    result:
      target_value: "2025-09-10T14:30:00Z"
```

## 8. Simplified lookup_definition Shape

```yaml
id: lk_fxd_ndf_cutoff_by_pair_and_locode
status: draft
signoff_status: none

source:
  mapping_table: TraditionStellaCutoff
  inputs:
    - name: fixing_currency1
    - name: fixing_currency2
    - name: cutoff_code

logic:
  summary: Query cutoff mapping by forward currency-pair order, then retry reverse order.
  pipeline:
    - op: compose_key
      out: forward_key
    - op: lookup_value
      out: forward_result
    - op: lookup_value
      out: reverse_result
      when: forward_result is empty
    - op: coalesce
      out: resolved_value

output:
  fields:
    - name: cutoff_name
    - name: cutoff_description
    - name: fixing_time
```

## 9. Simplified Evidence Entry

```yaml
entries:
  - evidence_id: ev_c01_timestamp_java
    object_id: rule_scbml_initiated_timestamp
    kind: java
    role: logic_support
    summary: Java extractor supports the timestamp conversion behavior.
    locator:
      java:
        path: extractor/common/MessageFields.java
        members: [CREATION_TIMESTAMP]
```

## 10. Simplified Review Entry

```yaml
items:
  - review_id: rev_c01_tracking_id_source
    object_id: rule_scbml_tracking_id
    kind: ambiguity
    status: open
    summary: Source field has no matching Java enum definition.
    follow_up: Confirm with Dev/BA what source produces this value.
    supporting_evidence:
      - ev_c01_tracking_id_xslt_only
```

## 11. Historical Snapshot

The historical handoff recorded this snapshot on 2026-04-16:

- 18 packs authored
- 9 packs reviewed
- COMMON and FXD_NDF had reviewed packs
- FXO packs were authored but not yet reviewed

This is historical context only. Do not treat it as current project state without re-checking the current repository.
