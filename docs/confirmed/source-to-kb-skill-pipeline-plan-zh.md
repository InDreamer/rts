<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: skill and agent pipeline plan for turning full workflow source assets into RTS source profiles, KB packs, review questions, snapshots, and runtime projection packages
read_when:
  - 需要设计从公司代码库或完整 workflow source 生成 RTS KB pack 的 agent skill 体系
  - 需要判断哪些步骤应该做成 skill，哪些步骤应该是 deterministic publisher 或 validator
  - 需要把 gRPC 到 Solace、FpML 到 SCBML 的完整转换 workflow 纳入 KB 生成
  - 需要规划在公司电脑上处理受限 source 资产前的执行边界
skip_when:
  - 只需要查看已经生成好的 KB pack
  - 只需要调用现有 RTS API
  - 只需要 PM 视角解释 runtime projection
source_of_truth:
  - docs/confirmed/kb-authoring-snapshot-runtime-final-choice-zh.md
  - docs/confirmed/kb-runtime-index-layer-standard-zh.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
-->

# Source to KB Skill Pipeline Plan

> 状态：confirmed planning baseline
> 日期：2026-05-21
> 范围：固定 agent skill 体系，用于在公司电脑上从完整 workflow source 生成 `sources/`、`kb/`，并为 snapshot/runtime projection 做准备
> 当前边界：本地没有公司代码库，因此本文只定义 skill、handoff artifact、验证门槛和公司电脑执行方式；真实 source scanning、KB 生成、测试运行在公司电脑上完成。
> Portable skill 状态：前三个 skill 已随 repo 放在 `skills/` 下，可以复制到公司电脑后直接按路径使用或安装到本机 Codex skills 目录。

## 1. 背景和目标 workflow

目标 workflow 不是单点 cutoff、timestamp 或某个局部字段，而是完整报文转换链路：

```text
gRPC inbound
  -> message classification
  -> FpML / upstream XML parsing
  -> Java / enum / Camel / XSLT / PostgreSQL / Excel mapping rules
  -> SCBML / downstream XML assembly
  -> Solace outbound
```

Source 类型包括：

- Java 代码
- enum
- XSLT
- Camel route / processor
- PostgreSQL schema、SQL、seed/mapping data
- unit tests / integration tests
- Excel / CSV mapping
- 上游 XML 样例，例如 FpML
- 下游 XML 样例，例如 SCBML

第一目标：

```text
source assets
  -> source profile
  -> structured KB pack
  -> KB review
  -> user questions
```

第二目标：

```text
validated KB pack
  -> canonical signoff snapshot skeleton
```

第三目标：

```text
signed snapshot
  -> runtime projection skeleton / release package
```

不要第一步就追求完整 production runtime projection。没有经过 source profile、KB review 和用户确认的问题，直接生成 runtime projection 会把未确认的误读包装成 service truth。

## 2. 需要的 skill 和非 skill 组件

建议拆成五个能力，其中前三个先做 skill，后两个先做 skill + deterministic scripts/validators。

| 编号 | 名称 | 类型 | 当前状态 | 目的 |
|---|---|---|---|---|
| 1 | `rts-workflow-source-profiler` | portable repo skill | implemented at `skills/rts-workflow-source-profiler/` | 只分析 source，生成 workflow map 和 source evidence，不生成 KB truth |
| 2 | `rts-source-to-kb-pack` | portable repo skill | implemented at `skills/rts-source-to-kb-pack/` | 基于 source profile 生成结构化 KB pack |
| 3 | `rts-kb-pack-review` | portable repo skill | implemented at `skills/rts-kb-pack-review/` | 独立 review KB，生成必须问用户的问题 |
| 4 | `rts-kb-to-snapshot-publisher` | skill + deterministic publisher/validator | pending | 从 validated KB 生成 canonical signoff snapshot |
| 5 | `rts-snapshot-to-runtime-projection-publisher` | skill + deterministic publisher/validator | pending | 从 signed snapshot 生成 runtime projection |

必须拆开的原因：

- Source profiling 是证据和 workflow 理解，不应该直接创造 KB truth。
- KB generation 是结构化建模，不应该顺手完成 signoff。
- KB review 必须独立于 generator，否则容易自证正确。
- Snapshot 和 runtime projection 涉及 canonical hash、write-once、manifest provenance，不能靠纯 LLM 手写。

## 3. Skill 1：`rts-workflow-source-profiler`

### 3.1 职责

从完整 workflow codebase 中建立 source profile，不生成 KB pack。

必须回答：

```text
gRPC 入口在哪里
消息类型如何识别
FpML / upstream XML 如何解析
Java / Camel / XSLT / DB / Excel / enum 分别参与什么转换
哪些字段由代码决定
哪些字段由 XSLT 决定
哪些字段由 DB mapping 决定
哪些字段由 Excel 或配置决定
哪些 fallback/default/error path 存在
Solace outbound 在哪里构造和发送
UT/IT 覆盖了哪些规则和场景
哪些点无法确定，必须问用户
```

### 3.2 输入

用户或公司电脑上的 agent 至少提供：

```text
source repo root
workflow name
inbound protocol: gRPC
upstream payload type: FpML or specific XML
outbound protocol: Solace
downstream payload type: SCBML or specific XML
allowed read paths
forbidden paths, if any
whether tests may run
whether DB metadata or sample rows may be read
```

### 3.3 输出

固定输出：

```text
sources/{source_bundle_id}/
  source-manifest.yaml
  source-index.yaml
  workflow-map.yaml
  extraction-notes.md
  unresolved-questions.yaml
  raw/                         # optional; only if allowed
  normalized/                  # optional; extracted XML/table summaries
```

### 3.4 `workflow-map.yaml`

`workflow-map.yaml` 是 profiler 的核心产物。最小结构：

```yaml
schema_version: workflow-map-v1
workflow:
  id: grpc-fpml-to-solace-scbml
  name: gRPC FpML to Solace SCBML transformation
  status: profiled
entrypoints:
  grpc:
    service_classes: []
    proto_files: []
    handlers: []
classification:
  message_type_rules: []
input_payload:
  format: XML
  business_schema: FpML
  sample_refs: []
transformation_flow:
  - step_id: receive_grpc
    kind: inbound
    source_refs: []
  - step_id: parse_fpml
    kind: parse
    source_refs: []
  - step_id: apply_java_rules
    kind: java
    source_refs: []
  - step_id: apply_xslt
    kind: xslt
    source_refs: []
  - step_id: apply_db_mapping
    kind: db_mapping
    source_refs: []
  - step_id: build_scbml
    kind: output_xml
    source_refs: []
  - step_id: publish_solace
    kind: outbound
    source_refs: []
field_bindings:
  - binding_id: TBD
    source_path: TBD
    target_path: TBD
    via_steps: []
    evidence_refs: []
rule_candidates: []
lookup_candidates: []
helper_candidates: []
fallbacks_and_defaults: []
error_paths: []
test_coverage:
  unit_tests: []
  integration_tests: []
open_questions: []
```

### 3.5 禁止行为

Profiler 禁止：

- 修改公司源码。
- 把代码长片段复制进 RTS 文档。
- 把未确认 inference 当作 KB truth。
- 直接生成 runtime projection。
- 只看局部字段后声称覆盖完整 workflow。

## 4. Skill 2：`rts-source-to-kb-pack`

### 4.1 职责

把 source profile 转换成结构化 KB pack。

它必须以 `sources/{source_bundle_id}/workflow-map.yaml` 和 `source-index.yaml` 为主要输入。不能绕过 source profile 直接从源码拍脑袋写 KB。

### 4.2 输出

固定输出：

```text
kb/{pack_id}/
  metadata.yaml
  README.md
  rules/{rule_id}.yaml
  lookups/{lookup_id}.yaml
  helpers/{helper_id}.yaml
  evidence/evidence-index.yaml
  review/review-index.yaml
  reports/extraction-report.md
  reports/review-checklist.md
  reports/closure-check.md
  attachments/                 # optional
```

### 4.3 建模原则

Rule / Lookup / Helper 拆分标准：

| 类型 | 放什么 |
|---|---|
| Rule | 一个业务输出、目标 XML 结构、路由决策或转换规则 |
| Lookup | DB/Excel/enum/config 支撑的可复用映射表或枚举 |
| Helper | 可复用解析、拼接、标准化、fallback 或选择逻辑 |

对完整 workflow，KB 必须覆盖：

- inbound entrypoint
- message classification
- input XML path / input semantic field
- target XML path / output semantic field
- Java/Camel/XSLT/DB/Excel/enum evidence
- fallback/default/error path
- field binding
- dependency graph
- tests as evidence
- unresolved ambiguity

### 4.4 不确定性处理

如果无法确定，必须写入 `review/review-index.yaml` 和 `reports/closure-check.md`，而不是沉默。

必须标记：

```text
missing_source
conflicting_source
weak_evidence
inferred_behavior
requires_runtime_db_sample
requires_user_confirmation
out_of_scope
```

## 5. Skill 3：`rts-kb-pack-review`

### 5.1 职责

独立 review 已生成 KB pack，包括格式、完整度、歧义、证据覆盖和 workflow 闭合性。

Review 不应该默认修改 KB truth。默认只输出 review 结果和用户问题。只有用户明确要求“根据 review 修复 KB”，才修改 KB。

### 5.2 检查项

必须检查：

```text
目录和文件是否符合 contract
metadata scope 是否稳定
source bundle 是否可追溯
每个 rule/lookup/helper 是否有 evidence_refs
grpc -> parse -> transform -> scbml -> solace 是否闭合
Java / XSLT / DB / Excel / enum 是否覆盖到各自参与的规则
field binding 是否能解释 source XML 到 target XML
dependencies 是否闭合
fallback/default/error path 是否记录
UT/IT 是否被纳入 evidence 或 gap
report prose 是否没有承担唯一 truth
是否存在必须用户确认的问题
```

### 5.3 输出

固定输出：

```text
kb/{pack_id}/reports/review-checklist.md
kb/{pack_id}/reports/closure-check.md
kb/{pack_id}/review/review-index.yaml
kb/{pack_id}/review/ask-user-questions.json
```

### 5.4 `ask-user-questions.json`

该文件是 askUserQuestionTool 的输入前体。最小结构：

```json
{
  "schema_version": "ask-user-questions-v1",
  "pack_id": "grpc-fpml-to-solace-scbml",
  "questions": [
    {
      "question_id": "q-primary-source-of-truth-001",
      "severity": "blocking",
      "object_refs": ["rule_example"],
      "question": "DB mapping and XSLT disagree on this target field. Which source should win?",
      "why_needed": "Production signoff cannot proceed with conflicting truth sources.",
      "options": [
        {
          "label": "DB mapping wins",
          "effect": "Rule will reference DB mapping as primary evidence."
        },
        {
          "label": "XSLT wins",
          "effect": "Rule will reference XSLT as primary evidence."
        }
      ],
      "freeform_allowed": true,
      "blocks": ["snapshot", "runtime_projection"]
    }
  ]
}
```

Severity:

```text
blocking        # blocks snapshot/runtime projection
important       # should be answered before production signoff
clarifying      # improves KB but can remain as documented uncertainty
```

### 5.5 何时调用 askUserQuestionTool

如果运行环境支持 askUserQuestionTool，review agent 应该只对 `severity=blocking` 或用户要求立即对齐的问题调用。

限制：

- 一次问 1 到 3 个短问题。
- 问题必须是用户能裁决的问题，不要问“要不要继续”这类流程问题。
- 每个问题必须说明如果用户选择某项，会改变哪些 object 或 gate。
- 不能把 source 能自己验证的问题推给用户。

## 6. Skill 4：`rts-kb-to-snapshot-publisher`

当前先 pending，不建议只做成 prompt skill。

原因：

- 需要 canonical JSON normalization。
- 需要 deterministic hash。
- 需要 write-once snapshot。
- 需要引用解析和 signoff coverage。
- 需要 golden tests。

建议形态：

```text
skill instructions
  -> scripts/validate_kb_pack.py
  -> scripts/canonicalize_snapshot.py
  -> scripts/validate_snapshot.py
```

固定输出：

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

在没有 deterministic publisher 前，最多生成 snapshot skeleton，不要宣布 production signoff。

## 7. Skill 5：`rts-snapshot-to-runtime-projection-publisher`

当前也先 pending，不建议只做成 prompt skill。

原因：

- Runtime projection 是 service truth。
- `release-manifest.json`、`object-manifest.jsonl`、L2 hash、dependency、navigation、governance summary 都需要一致性校验。
- Release id 必须 write-once。
- Java store 还需要 production provenance validation。

建议形态：

```text
skill instructions
  -> scripts/validate_snapshot.py
  -> scripts/generate_runtime_projection.py
  -> scripts/validate_runtime_projection.py
  -> service smoke tests
```

固定输出：

```text
runtime-store/releases/{release_id}/
  release-manifest.json
  scopes.jsonl
  object-manifest.jsonl
  caller-profiles.jsonl
  l2/**/*.json
  navigation/*.jsonl
  dependencies/*.jsonl
  governance/*
  index-artifacts/*
```

## 8. 公司电脑执行模式

将 RTS repo 上传到公司电脑后，建议执行顺序：

```text
1. 确认公司 source repo 只读可访问
2. 运行 rts-workflow-source-profiler
3. 人工快速检查 workflow-map.yaml
4. 运行 rts-source-to-kb-pack
5. 运行 rts-kb-pack-review
6. 对 blocking ask-user-questions 做人工确认
7. 根据确认修订 KB
8. 再跑 review，直到 blocking 为 0
9. 生成 snapshot skeleton
10. 再决定是否进入 runtime projection skeleton
```

公司电脑上第一轮不要直接 production signoff。第一轮目标是得到一个能被 review 的 KB pack 和清楚的问题清单。

## 9. 当前 repo 应先准备什么

不依赖公司资产、可以现在完成：

- 固定本设计文档。
- 固定公司电脑执行清单。
- 创建前三个 repo-bundled portable skills，但不要写死公司代码结构。
- 后续可以补 `workflow-map-v1`、`ask-user-questions-v1` 的 schema 草案。
- 后续可以补 KB pack validator 的最小脚本。

依赖公司资产、必须到公司电脑完成：

- 真实 source scanning。
- Java/Camel/XSLT/DB/Excel/UT 解析。
- workflow-map 生成。
- KB pack 生成和 review。
- 用户问题确认。
- source-backed evidence 校验。
- 运行公司测试。

## 10. 最短版

```text
先不要让 agent 从公司代码直接生成 runtime projection。

第一步只 profile 完整 workflow source。
第二步从 profile 生成 KB。
第三步独立 review KB 并问用户 blocking questions。
第四步再做 snapshot publisher。
第五步最后做 runtime projection publisher。
```
