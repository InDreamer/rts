<!-- docmeta
role: domain
layer: 2
parent: docs/INDEX.md
children:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
  - docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md
  - docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md
summary: route confirmed RTS baseline documents
read_when:
  - 需要选择当前 RTS confirmed baseline 文档
  - 需要判断 Day1 或 Day2 服务方案该读哪一篇
  - 第一次进入 confirmed 文档区
skip_when:
  - 已经明确要读取某一篇 confirmed leaf
  - 只需要历史参考材料或 archived 材料
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
-->

# Confirmed Docs

This directory contains the current internal alignment baseline for RTS.

## Current baseline order

1. `project-alignment-summary-zh.md` — 核心总纲，定义 RTS 是 rule truth service，而不是静态文档库、普通 RAG 或单一实现方案。
2. `system-constitution-v1.md` — RTS Constitution v2，定义多源真相治理、AI-first review、人工最终裁决、权限化服务访问和运行时边界。
3. `kb-to-index-projection-contract-zh.md` — KB 到查询/索引层的运行时投影契约，定义 KB 必须产出什么、索引层只能读取什么、查询层如何解释 projection。
4. `day1-query-service-and-llm-harness-plan-zh.md` — Day1 查询服务、轻量索引层和受控 LLM harness 落地方案。
5. `day2-agentic-retrieval-evolution-plan-zh.md` — Day2 受控 agentic retrieval、rerank、MCP 扩展、影响分析和测试规划演进方向。

## Read triggers

- 只需要统一 RTS 总体方向：读 1 和 2。
- 需要开发 KB 和索引层之间的边界：读 1、2、3。
- 需要落地第一版服务：读 1、2、3、4。
- 需要规划 Day1 后的检索/LLM/MCP 增强：读 1、2、3、4、5。

## Conflict rule

If confirmed documents disagree, use this order:

1. `project-alignment-summary-zh.md`
2. `system-constitution-v1.md`
3. `kb-to-index-projection-contract-zh.md`
4. `day1-query-service-and-llm-harness-plan-zh.md`
5. `day2-agentic-retrieval-evolution-plan-zh.md`
6. `docs/reference/` materials
7. `docs/archive/` materials

`docs/reference/java-index-layer-full-plan-zh.md` is an index/query layer reference plan. It is not the full RTS system baseline and does not decide the final technology stack.

Use `docs/reference/` for supporting rationale, engineering references, external review, OV history, and historical context. Use `docs/archive/` only when tracing older discussions or prototypes before deletion/consolidation.
