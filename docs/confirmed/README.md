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
  - docs/confirmed/llm-harness-and-agent-integration-alignment-zh.md
  - docs/confirmed/final-llm-agent-service-plan-zh.md
  - docs/confirmed/internal-llm-agent-service-implementation-plan-zh.md
  - docs/confirmed/document-decision-register-zh.md
summary: route confirmed RTS baseline documents
read_when:
  - 需要选择当前 RTS confirmed baseline 文档
  - 需要判断 Day1 或 Day2 服务方案该读哪一篇
  - 需要理解 RTS 最终如何服务 LLM agent
  - 需要完整落地 RTS service 内部 LLM agent 接入
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
6. `llm-harness-and-agent-integration-alignment-zh.md` — RTS 内置 LLM harness、外部 agent tool mode、runtime projection access boundary 和 PR diff / exception 等场景接入的当前对齐结论。
7. `final-llm-agent-service-plan-zh.md` — RTS 最终面向 LLM agent 的服务路线图摘要，定义 managed/tool mode、能力地图、阶段路线、answer contract 和不可越界边界。
8. `internal-llm-agent-service-implementation-plan-zh.md` — RTS service 内部 LLM agent 接入的完整非阶段性落地计划，覆盖 managed harness、tool orchestration、scenario endpoints、MCP/tool mode、memory、grounding、evaluation 和运维控制。
9. `document-decision-register-zh.md` — 文档审核中的关键抉择点、歧义点和下一轮删减规则。

## Read triggers

- 只需要统一 RTS 总体方向：读 1 和 2。
- 需要开发 KB 和索引层之间的边界：读 1、2、3。
- 需要落地第一版服务：读 1、2、3、4。
- 需要规划 Day1 后的检索/LLM/MCP 增强：读 1、2、3、4、5。
- 需要判断 LLM 放在 RTS service 内部还是外部 agent、如何选择 OpenAI/Claude/LangChain/PageIndex 等框架、如何接 PR diff 或 exception 场景：读 6，再按需读 4、5、7。
- 需要规划最终 LLM agent 服务形态、harness、MCP/API/Q&A/pipeline 统一入口：读 1、2、3、4、5、6、7。
- 需要把 RTS service 内部 LLM agent 接入落到完整功能、接口、控制点、测试和运维计划：读 8。
- 需要判断历史文档哪些保留、压缩、归档或删除：读 9。

## Conflict rule

Confirmed docs use topic-specific authority. Resolve conflicts with this table before applying any general reading order:

| Topic | Authority |
|---|---|
| RTS identity, truth ownership, source/evidence/review/human adjudication/signoff, and document zone meaning | `project-alignment-summary-zh.md` |
| Non-negotiable system principles, safety boundaries, permissioned access, AI-first review, and human final decision | `system-constitution-v1.md` |
| KB/runtime projection/index boundary, release artifact shape, L0/L1/L2 meaning, schema/release/read constraints | `kb-to-index-projection-contract-zh.md` |
| Current Day1 query/tool service, filesystem store, Lucene, L2 read/hash, REST/MCP skeleton, `/query`, `/ask` Day1 DoD | `day1-query-service-and-llm-harness-plan-zh.md` |
| LLM placement, managed mode vs tool mode, framework/SDK boundary, and PR diff / exception / failed-message scenario integration | `llm-harness-and-agent-integration-alignment-zh.md` |
| Day2 retrieval evolution, planner/orchestrator, rerank/vector/confusable active use, expanded MCP, impact/test candidate evolution | `day2-agentic-retrieval-evolution-plan-zh.md` |
| Final long-range service roadmap, capability map, consumer views, and sequencing after current alignment decisions | `final-llm-agent-service-plan-zh.md` |
| Complete internal LLM agent implementation work packages, service contracts, validation gates, scenario endpoints, memory/context, evaluation, and rollout controls | `internal-llm-agent-service-implementation-plan-zh.md` |
| Documentation cleanup, compression, archive/delete/retain decisions | `document-decision-register-zh.md` |

If a topic is not listed, follow the current baseline order above. `docs/reference/` materials may explain rationale but do not override confirmed docs. `docs/archive/` materials never override confirmed docs.

`docs/reference/java-index-layer-full-plan-zh.md` is a distilled index/query layer reference summary. The old full plan is archived and does not decide the current infrastructure stack.

Use `docs/reference/` for supporting rationale, engineering references, external review, OV history, and historical context. Use `docs/archive/` only when tracing older discussions or prototypes before deletion/consolidation.
