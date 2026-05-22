<!-- docmeta
role: leaf
layer: 2
parent: docs/INDEX.md
children: []
summary: quick company-computer checklist for running the RTS source-backed KB draft MVP against restricted corporate source assets
read_when:
  - 准备把 RTS repo 上传到公司电脑处理公司代码库
  - 需要快速确认 source-backed KB draft MVP 执行顺序
  - 需要区分本地已准备事项和公司电脑上才能做的事项
skip_when:
  - 需要完整执行细节和成功/失败标准
  - 只需要查看 confirmed 架构结论
  - 只需要运行本地 sample projection
source_of_truth:
  - docs/company-source-to-kb-mvp-runbook-zh.md
  - docs/confirmed/source-to-kb-skill-pipeline-plan-zh.md
  - skills/AGENTS.md
-->

# 公司电脑 Source-Backed KB Draft MVP 快速清单

> 状态：quick checklist
> 日期：2026-05-21
> 完整 runbook：`docs/company-source-to-kb-mvp-runbook-zh.md`
> 边界：真实公司 source 只在公司环境执行；本地不能声称已验证真实 source、准确完整 KB 或 production readiness。

## 0. 先读完整 runbook

执行前先打开：

```text
docs/company-source-to-kb-mvp-runbook-zh.md
skills/AGENTS.md
```

本文件只做快速核对，不替代完整 runbook。

## 1. 公司环境准备

- RTS repo 已复制到公司电脑。
- 公司 source repo 可只读访问。
- 已明确 allowed read paths 和 forbidden paths。
- 已明确测试、DB metadata/sample rows、runtime config、Excel/CSV 是否可读。
- 不修改公司源码。
- 不复制长源码片段进 RTS。

建议目录：

```text
workspace/
  rts/
  company-source/
```

## 2. 执行链路

固定链路：

```text
source codebase
  -> source inventory / coverage map
  -> evidence-backed claims
  -> KB draft
  -> source-backed review
  -> blocker questions
  -> completion report
```

## 3. Step 1：Source Inventory / Claims

使用：

```text
skills/rts-workflow-source-profiler
```

输出：

```text
sources/{source_bundle_id}/source-manifest.yaml
sources/{source_bundle_id}/source-index.yaml
sources/{source_bundle_id}/workflow-map.yaml
sources/{source_bundle_id}/claims.jsonl
sources/{source_bundle_id}/extraction-notes.md
sources/{source_bundle_id}/unresolved-questions.yaml
```

检查：

- `source-index.yaml` 和 `workflow-map.yaml` 只是导航/覆盖，不是 truth。
- `claims.jsonl` 存在。
- 每个 workflow 区域有状态。
- 每个 claim 有 source anchor 或明确非 supported 状态。

## 4. Step 2：KB Draft

使用：

```text
skills/rts-source-to-kb-pack
```

规则：

- KB 生成必须读取 `claims.jsonl` 和真实 source anchors。
- 非平凡 KB truth 必须有 `claim_refs`。
- 只有 `supported`、`user_confirmed`、`runtime_observed` claims 可进入 KB truth。
- `blocked`、`unsupported`、`inferred`、`contradicted`、`not_accessible` 只能进 review/warning/blocker。

输出：

```text
kb/{pack_id}/metadata.yaml
kb/{pack_id}/README.md
kb/{pack_id}/rules/
kb/{pack_id}/lookups/
kb/{pack_id}/helpers/
kb/{pack_id}/evidence/evidence-index.yaml
kb/{pack_id}/review/review-index.yaml
kb/{pack_id}/reports/
```

## 5. Step 3：Source-Backed Review

使用：

```text
skills/rts-kb-pack-review
```

Review 必查：

- unsupported claim
- anchor laundering
- coverage gap
- contradiction
- runtime config gap
- negative claim hallucination
- claim status gate
- source anchor support
- blocking count

输出：

```text
kb/{pack_id}/reports/review-checklist.md
kb/{pack_id}/reports/closure-check.md
kb/{pack_id}/reports/completion-report.md
kb/{pack_id}/review/review-index.yaml
kb/{pack_id}/review/ask-user-questions.json
```

## 6. Step 4：Blocker Questions

读取：

```text
kb/{pack_id}/review/ask-user-questions.json
```

只优先处理 `severity=blocking` 的问题。用户确认必须结构化写回 claims/review/report，不能只留在聊天里。

## 7. 完成标准

```text
source inventory / coverage map exists
claims.jsonl exists
all nontrivial KB truth has claim_id and source anchor
KB truth uses only supported/user_confirmed/runtime_observed claims
source-backed review completed
blocking count recorded
blocker questions produced or explicitly zero
completion-report.md exists
completion report states non-production boundary
```

## 8. 禁止事项

- 跳过 `claims.jsonl` 直接从 source map 生成 KB。
- 把 `source-index.yaml` 或 `workflow-map.yaml` 当 truth。
- 让 unsupported/inferred/blocked claims 进入 KB truth。
- 用“没搜到”证明不存在 fallback/default/error path。
- 第一轮声明 production snapshot、formal signoff、immutable runtime projection 或 production readiness。
