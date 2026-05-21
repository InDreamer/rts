<!-- docmeta
role: leaf
layer: 2
parent: docs/INDEX.md
children: []
summary: company-computer checklist for running the RTS source-to-KB workflow against restricted corporate source assets
read_when:
  - 准备把 RTS repo 上传到公司电脑处理公司代码库
  - 需要在公司电脑上从 gRPC/FpML 到 Solace/SCBML workflow 生成 source profile 和 KB pack
  - 需要区分本地已准备事项和公司电脑上才能做的事项
skip_when:
  - 只需要查看 confirmed 架构结论
  - 只需要运行本地 sample projection
source_of_truth:
  - docs/confirmed/source-to-kb-skill-pipeline-plan-zh.md
  - docs/confirmed/kb-authoring-snapshot-runtime-final-choice-zh.md
  - docs/confirmed/kb-runtime-index-layer-standard-zh.md
-->

# 公司电脑 Source-to-KB Workflow 执行清单

> 状态：company-computer run checklist
> 日期：2026-05-21
> 用途：把 RTS repo 上传到公司电脑后，按步骤处理受限公司 source 资产
> 原则：先 profile source，再生成 KB，再 review 和问用户；第一轮最多生成 snapshot/runtime skeleton，不要宣布 production signoff 或 production service truth。

Repo 已带 portable skills：

```text
skills/rts-workflow-source-profiler/
skills/rts-source-to-kb-pack/
skills/rts-kb-pack-review/
```

在公司电脑上可以直接让 agent 使用这些路径，例如：

```text
Use the skill at rts/skills/rts-workflow-source-profiler to profile the read-only company source repo...
```

## 0. 公司电脑前置准备

在公司电脑上先确认：

- RTS repo 已复制到本机。
- 公司 source repo 可只读访问。
- 允许 agent 读取相关代码、XSLT、DB metadata、Excel、测试和样例 XML。
- 不修改公司源码。
- 不复制长源码片段到 RTS 文档，只保存 locator、hash、summary 和必要的短摘要。
- 如果不能读取 DB 真实数据，至少确认能读取 schema、mapping table definition 或脱敏样例。
- 如果不能运行测试，记录原因。

建议目录：

```text
workspace/
  rts/
  company-source/
```

## 1. 输入信息收集

运行 agent 前，先准备这些值：

```text
workflow_name:
  gRPC FpML to Solace SCBML transformation

inbound:
  protocol: gRPC
  payload: FpML or specific upstream XML
  known service/proto/handler: TBD

outbound:
  protocol: Solace
  payload: SCBML or specific downstream XML
  known producer/topic/queue: TBD

source_types:
  Java
  enum
  XSLT
  Camel
  PostgreSQL
  unit tests / integration tests
  Excel / CSV mapping
  sample XML

read_paths:
  company-source/src
  company-source/resources
  company-source/test
  company-source/mappings
  company-source/xslt
  company-source/db

write_paths:
  rts/sources/{source_bundle_id}
  rts/kb/{pack_id}
```

## 2. 第一步：Source profiling

目标：生成 source bundle，不生成 KB。

期望使用 skill：

```text
skills/rts-workflow-source-profiler
```

如果公司 Codex 没有自动发现 repo 内 skill，就把下面这段作为 agent 任务，并明确 skill 路径：

```text
Use the skill at rts/skills/rts-workflow-source-profiler.

Read the company source repo as read-only. Build an RTS source profile for the full workflow from gRPC inbound FpML/XML to Solace outbound SCBML/XML.

Do not modify source code. Do not copy long source excerpts. Produce only locators, hashes, summaries, workflow map, evidence candidates, and unresolved questions.

Output under rts/sources/{source_bundle_id}/:
- source-manifest.yaml
- source-index.yaml
- workflow-map.yaml
- extraction-notes.md
- unresolved-questions.yaml
```

检查输出：

```text
sources/{source_bundle_id}/source-manifest.yaml
sources/{source_bundle_id}/source-index.yaml
sources/{source_bundle_id}/workflow-map.yaml
sources/{source_bundle_id}/extraction-notes.md
sources/{source_bundle_id}/unresolved-questions.yaml
```

人工快速看：

- gRPC entrypoint 是否找到。
- FpML/XML parse path 是否找到。
- Java/Camel/XSLT/DB/Excel/enum 是否都有位置。
- SCBML output assembly 是否找到。
- Solace outbound 是否找到。
- UT/IT 是否被定位。
- open questions 是否真实，不是 agent 偷懒。

如果 workflow-map 只覆盖局部字段，不要进入 KB 生成。

## 3. 第二步：生成 KB pack

目标：从 source profile 生成结构化 KB。

期望使用 skill：

```text
skills/rts-source-to-kb-pack
```

如果公司 Codex 没有自动发现 repo 内 skill，就把下面这段作为 agent 任务，并明确 skill 路径：

```text
Use the skill at rts/skills/rts-source-to-kb-pack.

Use rts/sources/{source_bundle_id}/source-index.yaml and workflow-map.yaml as the required input. Generate an RTS KB pack for the full gRPC FpML to Solace SCBML workflow.

Do not bypass the source profile. Do not mark inferred behavior as confirmed truth. Put ambiguities and weak evidence into review/review-index.yaml and reports/closure-check.md.

Output under rts/kb/{pack_id}/:
- metadata.yaml
- README.md
- rules/*.yaml
- lookups/*.yaml
- helpers/*.yaml
- evidence/evidence-index.yaml
- review/review-index.yaml
- reports/extraction-report.md
- reports/review-checklist.md
- reports/closure-check.md
```

检查输出：

```text
kb/{pack_id}/metadata.yaml
kb/{pack_id}/rules/
kb/{pack_id}/lookups/
kb/{pack_id}/helpers/
kb/{pack_id}/evidence/evidence-index.yaml
kb/{pack_id}/review/review-index.yaml
kb/{pack_id}/reports/
```

必须覆盖：

- inbound gRPC handling
- message classification
- input FpML/XML semantic fields
- output SCBML/XML target paths
- Java rules
- XSLT rules
- DB / Excel / enum lookups
- helper logic
- fallback/default/error path
- tests as evidence
- field bindings

## 4. 第三步：KB review

目标：独立 review KB，生成必须问用户的问题。

期望使用 skill：

```text
skills/rts-kb-pack-review
```

如果公司 Codex 没有自动发现 repo 内 skill，就把下面这段作为 agent 任务，并明确 skill 路径：

```text
Use the skill at rts/skills/rts-kb-pack-review.

Review rts/kb/{pack_id}/ as an independent RTS KB reviewer.

Check format, completeness, evidence coverage, workflow closure, source locators, dependencies, field bindings, ambiguity handling, and whether the pack can move toward snapshot.

Do not silently rewrite truth. Produce review results and ask-user questions.

Output/update:
- kb/{pack_id}/reports/review-checklist.md
- kb/{pack_id}/reports/closure-check.md
- kb/{pack_id}/review/review-index.yaml
- kb/{pack_id}/review/ask-user-questions.json
```

Review 必须回答：

- 是否覆盖了 gRPC -> transform -> Solace 全链路。
- 是否每个 rule/lookup/helper 都能追到 source。
- 是否 DB/Excel/enum/XSLT/Java 冲突。
- 是否有 test evidence。
- 是否有 blocking uncertainty。
- 是否能进入 snapshot skeleton。

## 5. 第四步：用户确认

读取：

```text
kb/{pack_id}/review/ask-user-questions.json
```

只优先确认：

```text
severity = blocking
```

每个问题确认后，要求 agent 更新：

```text
kb/{pack_id}/review/review-index.yaml
kb/{pack_id}/reports/closure-check.md
相关 rules/lookups/helpers
```

不要让 agent 把“用户口头确认”只留在聊天里。影响 truth 的确认必须结构化落入 review decision 或 future `human-confirmation-log.jsonl`。

## 6. 第五步：再次 review

重新运行 KB review，直到：

```text
blocking issues = 0
all required refs resolve
workflow closure accepted
source evidence coverage accepted
```

如果仍有 non-blocking risk，可以保留在 `closure-check.md`，但必须明确不阻塞 demo snapshot。

## 7. 第六步：Snapshot skeleton

当前建议只生成 skeleton，不做 production signoff。Skeleton 只用于检查目录、字段、引用和 publisher 设计，不代表 signed snapshot。

进入条件：

- KB review blocking 为 0。
- 所有 projected objects 有 evidence refs。
- 用户确认的问题已经结构化记录。
- 没有 unresolved source conflict。

输出目标：

```text
kb/{pack_id}/snapshots/{snapshot_id}/
  snapshot-manifest.json
  objects.jsonl
  dependency-edges.jsonl
  evidence-refs.jsonl
  review-decisions.jsonl
  human-confirmation-log.jsonl
  change-log.jsonl
  signoff.json
  projection-map.jsonl
```

注意：

- 没有 deterministic canonical hash publisher 前，不要宣称 production signoff。
- 可以标记 `release_tier: demo` 或 `snapshot_status: skeleton`。

## 8. 第七步：Runtime projection skeleton

只有 snapshot skeleton 稳定后再做。Runtime projection skeleton 可以验证 release 形状和服务读取路径，但在 deterministic publisher、canonical hash、signoff 和 smoke test 完成前，不得作为 production service truth。

输出目标：

```text
runtime-store/releases/{release_id}/
  release-manifest.json
  scopes.jsonl
  object-manifest.jsonl
  caller-profiles.jsonl
  l2/rules/*.json
  l2/lookups/*.json
  l2/helpers/*.json
  navigation/*.jsonl
  dependencies/*.jsonl
  governance/*
  index-artifacts/opensearch-docs.jsonl
```

第一轮可以不生成 Lucene binary index。Lucene 是 rebuildable index，不是 truth。

## 9. 禁止事项

公司电脑执行时禁止：

- 修改公司源码。
- 把公司源码长片段复制进 RTS repo。
- 跳过 source profile 直接生成 KB。
- 只抽局部字段就声明覆盖完整 workflow。
- 把 report prose 当唯一 truth。
- 把 unresolved question 当已确认。
- 第一轮直接 production signoff。
- 第一轮直接把 runtime projection skeleton 当 production service truth。

## 10. 完成标准

第一阶段完成标准：

```text
sources/{source_bundle_id}/ workflow profile complete
kb/{pack_id}/ structured KB generated
kb review completed
ask-user-questions.json produced
blocking questions answered or explicitly deferred
closure-check.md says whether snapshot skeleton may proceed
```

第二阶段完成标准：

```text
snapshot skeleton generated
all snapshot refs resolve
all projected objects have source/evidence/review lineage
release tier clearly marked demo/skeleton unless production publisher exists
```

第三阶段完成标准：

```text
runtime projection skeleton generated
release manifest references snapshot id/hash/signoff id where available
object manifest covers all L2 objects
L2 content hashes validate
query smoke test passes against local RTS service
```
