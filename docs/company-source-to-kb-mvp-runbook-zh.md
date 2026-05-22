<!-- docmeta
role: leaf
layer: 2
parent: docs/INDEX.md
children: []
summary: company-executable runbook for source-backed KB draft MVP using source inventory, evidence-backed claims, KB draft, source-backed review, blocker questions, and completion report
read_when:
  - 准备在公司环境用真实 source 生成 source-backed KB draft
  - 需要执行 source inventory、claims.jsonl、KB draft、source-backed review、blocker questions 和 completion report
  - 需要确认 MVP 成功/失败标准和非 production 边界
skip_when:
  - 只需要调用本地 RTS Java service
  - 只需要查看已生成 KB pack
  - 只需要 production snapshot 或 runtime projection 设计
source_of_truth:
  - docs/confirmed/source-to-kb-skill-pipeline-plan-zh.md
  - skills/AGENTS.md
  - skills/rts-workflow-source-profiler/SKILL.md
  - skills/rts-source-to-kb-pack/SKILL.md
  - skills/rts-kb-pack-review/SKILL.md
-->

# Company Source-Backed KB Draft MVP Runbook

> 状态：company-executable MVP runbook
> 日期：2026-05-21
> 本地边界：真实公司 source 不在本地。本 runbook 只定义公司环境执行步骤和验收标准；本地不能声称已验证真实 source、准确完整 KB 或 production readiness。

## 0. MVP Scope

公司执行链路固定为：

```text
source codebase
  -> source inventory / coverage map
  -> evidence-backed claims
  -> KB draft
  -> source-backed review
  -> blocker questions
  -> completion report
```

MVP 输出是可审核 KB draft，不是 production truth。

MVP 不包含：

- production snapshot
- formal signoff workflow
- immutable runtime projection
- full AST/LSP/call graph platform
- 本地真实 source 验证
- 准确完整 KB 或 production readiness 声明

## 1. 公司环境输入

在公司电脑上先准备：

```text
workspace/
  rts/
  company-source/
```

必填输入：

```text
workflow_name:
source_repo_root:
source_revision:
source_bundle_id:
pack_id:
inbound_protocol: grpc
upstream_payload: FpML or specific XML
outbound_protocol: solace
downstream_payload: SCBML or specific XML
allowed_read_paths:
forbidden_paths:
tests_may_run: yes/no
db_metadata_may_read: yes/no
runtime_config_may_read: yes/no
excel_csv_may_read: yes/no
```

安全要求：

- 公司 source repo 只读。
- 不修改公司源码。
- 不复制长源码片段到 RTS。
- 保存 locator、hash、line range、短摘要和 claim，不保存大段 proprietary source。
- 无法访问的 DB、Solace、runtime config、Excel、测试必须进入 gap 或 blocker。

## 2. Step 1：Source Inventory / Coverage Map

使用：

```text
skills/rts-workflow-source-profiler
```

任务口径：

```text
Use rts/skills/rts-workflow-source-profiler.
Read the company source repo as read-only.
Build source inventory / coverage map and claims.jsonl.
Do not generate KB truth.
Do not treat source-index.yaml or workflow-map.yaml as business truth.
Write only under rts/sources/{source_bundle_id}/.
```

必须输出：

```text
sources/{source_bundle_id}/source-manifest.yaml
sources/{source_bundle_id}/source-index.yaml
sources/{source_bundle_id}/workflow-map.yaml
sources/{source_bundle_id}/claims.jsonl
sources/{source_bundle_id}/extraction-notes.md
sources/{source_bundle_id}/unresolved-questions.yaml
```

检查点：

- 每个 workflow 区域都有 `found / not_found / not_applicable / not_accessible / needs_user_confirmation`。
- 每个 workflow step 有 source refs 或 unresolved question。
- `claims.jsonl` 每条 claim 有 `claim_id`、`claim_type`、`status`、`source_anchors`、`evidence_type`、`extraction_method`、`limits`。
- 没有把 source map 摘要写成最终业务 truth。

## 3. Step 2：Evidence-Backed Claims Gate

进入 KB 前先检查 `claims.jsonl`。

可进入 KB truth 的 status：

```text
supported
user_confirmed
runtime_observed
```

只能进入 review/warning/blocker 的 status：

```text
blocked
unsupported
inferred
contradicted
not_accessible
```

失败条件：

- KB 所需的非平凡事实没有 claim。
- claim 没有 source anchor。
- claim 引用的是 source map 摘要而不是真实 source anchor。
- negative claim 只靠“没搜到”成立，例如声称没有 fallback/default/error path。

## 4. Step 3：KB Draft

使用：

```text
skills/rts-source-to-kb-pack
```

任务口径：

```text
Use rts/skills/rts-source-to-kb-pack.
Generate kb/{pack_id}/ from sources/{source_bundle_id}/claims.jsonl plus real source anchors.
Source inventory is navigation only.
Only supported, user_confirmed, and runtime_observed claims may enter KB truth.
Put blocked, unsupported, inferred, contradicted, and not_accessible claims into review/warnings.
Do not generate snapshot or runtime projection.
```

必须输出：

```text
kb/{pack_id}/metadata.yaml
kb/{pack_id}/README.md
kb/{pack_id}/rules/*.yaml
kb/{pack_id}/lookups/*.yaml
kb/{pack_id}/helpers/*.yaml
kb/{pack_id}/evidence/evidence-index.yaml
kb/{pack_id}/review/review-index.yaml
kb/{pack_id}/reports/extraction-report.md
kb/{pack_id}/reports/review-checklist.md
kb/{pack_id}/reports/closure-check.md
kb/{pack_id}/reports/completion-report.md
```

检查点：

- 每个 rule / lookup / helper 有 `claim_refs`。
- `claim_refs` 能回到 `claims.jsonl`。
- KB truth 只使用允许 status 的 claim。
- workflow 覆盖 gRPC inbound 到 Solace outbound。
- runtime config 不可访问时有 explicit gap。

## 5. Step 4：Source-Backed Review

使用：

```text
skills/rts-kb-pack-review
```

任务口径：

```text
Use rts/skills/rts-kb-pack-review.
Review kb/{pack_id}/ against sources/{source_bundle_id}/ and real source anchors.
Check unsupported claim, anchor laundering, coverage gap, contradiction, runtime config gap, and negative claim hallucination.
Do not silently rewrite truth.
Write blocking count, blocker questions, and completion report.
```

必须输出或更新：

```text
kb/{pack_id}/reports/review-checklist.md
kb/{pack_id}/reports/closure-check.md
kb/{pack_id}/reports/completion-report.md
kb/{pack_id}/review/review-index.yaml
kb/{pack_id}/review/ask-user-questions.json
```

Review 必查：

- `claims.jsonl` 是否存在且可解析。
- KB truth 的 claim status 是否符合 gate。
- source anchors 是否真的支持 claim。
- 是否存在 anchor laundering。
- 是否存在 coverage gap。
- 是否存在 Java/XSLT/DB/Excel/enum/test contradiction。
- 是否存在 runtime config gap。
- 是否存在 negative claim hallucination。
- blocker questions 是否只包含 source 无法裁决、必须用户判断的问题。

## 6. Step 5：Blocker Questions

读取：

```text
kb/{pack_id}/review/ask-user-questions.json
```

只优先处理：

```text
severity = blocking
```

每个用户确认必须结构化落入：

```text
sources/{source_bundle_id}/claims.jsonl
kb/{pack_id}/review/review-index.yaml
kb/{pack_id}/reports/closure-check.md
```

不要让关键确认只留在聊天记录里。

## 7. Step 6：Completion Report

最终必须生成：

```text
kb/{pack_id}/reports/completion-report.md
```

必须包含：

- source bundle id
- pack id
- company source revision / hash / unavailable reason
- workflow scope
- coverage summary
- claim counts by status
- KB object counts
- unsupported / inferred / blocked / contradicted / not_accessible counts
- blocking count
- blocker questions path
- test execution status
- runtime config access status
- what was not verified
- MVP completion decision
- explicit non-production statement

## 8. Success Criteria

MVP 完成标准：

```text
source inventory / coverage map exists
claims.jsonl exists
all nontrivial KB truth has claim_id and source anchor
KB truth uses only supported/user_confirmed/runtime_observed claims
blocked/unsupported/inferred/contradicted/not_accessible claims are excluded from truth
source-backed review completed
blocking count is recorded
blocker questions are produced or explicitly zero
completion-report.md exists
completion report states non-production boundary
```

生成 artifacts 后运行 repo-level validator：

```bash
python3 scripts/source_to_kb/validate_mvp.py \
  --repo-root . \
  --source-bundle {source_bundle_id} \
  --kb-pack {pack_id}
```

如果公司 source root 可读，可以加：

```bash
--source-root {company_source_root}
```

失败标准：

```text
source-index.yaml or workflow-map.yaml is used as truth
nontrivial KB truth lacks claim_refs
unsupported/inferred/blocked claims enter KB truth
review does not check source anchors
coverage gaps are silent
runtime config gaps are silent
negative absence claims are made without scoped evidence
completion report is missing
output claims production readiness
```

## 9. Local Verification Boundary

本地可以验证 docs、skills、contracts、templates 和 runbook 一致性。

本地不能验证：

- 真实公司 source 是否可读。
- source inventory 是否覆盖真实 workflow。
- claims 是否准确。
- KB draft 是否准确完整。
- 公司测试是否通过。
- production readiness。
