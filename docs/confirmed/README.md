<!-- docmeta
role: domain
layer: 2
parent: docs/INDEX.md
children:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
  - docs/confirmed/runtime-projection-product-guide-zh.md
  - docs/confirmed/llm-harness-and-agent-integration-alignment-zh.md
  - docs/confirmed/internal-llm-agent-service-implementation-plan-zh.md
  - docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md
  - docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md
  - docs/confirmed/final-llm-agent-service-plan-zh.md
  - docs/confirmed/document-decision-register-zh.md
summary: route confirmed RTS baseline documents
read_when:
  - 需要选择当前 RTS confirmed baseline 文档
  - 需要判断双核心服务定位、Day1/Day2 路线、managed mode / tool mode 边界
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

1. `project-alignment-summary-zh.md` — 双核心总纲，定义 RTS 是规则真相服务 + 托管 LLM agent 分析服务，而不是静态文档库、普通 RAG 或单一实现方案。
2. `system-constitution-v1.md` — RTS Constitution v2，定义多源真相治理、受控分析与表达、人工最终裁决、权限化服务访问和运行时边界。
3. `kb-to-index-projection-contract-zh.md` — KB 到查询/索引层的运行时投影契约，定义 KB 必须产出什么、索引层只能读取什么、稳定原子能力面如何解释 projection。
4. `runtime-projection-product-guide-zh.md` — runtime projection 的产品/PM 视角说明，解释运行包中 active release、manifest、scope、L2、navigation、dependency、governance、permission、index 和 trace 各自做什么，不替代字段契约。
5. `llm-harness-and-agent-integration-alignment-zh.md` — RTS 内置 managed mode、外部 tool mode、runtime projection access boundary 和 PR diff / exception 等场景接入的当前对齐结论。
6. `internal-llm-agent-service-implementation-plan-zh.md` — RTS service 内部 LLM agent 接入的完整非阶段性落地计划，覆盖 agent runtime、多轮工具调用、工具参数权威、managed harness、tool orchestration、scenario endpoints、MCP/tool mode、memory/context、grounding、evaluation 和运维控制。
7. `day1-query-service-and-llm-harness-plan-zh.md` — Day1：建立受控真相源原子能力面和第一版 managed LLM harness 的 confirmed baseline。
8. `day2-agentic-retrieval-evolution-plan-zh.md` — Day2：在同一边界内扩展 controlled agentic retrieval、rerank、MCP、影响分析和测试规划演进方向。
9. `final-llm-agent-service-plan-zh.md` — RTS 最终面向 LLM agent 的服务路线图摘要，定义双核心栈、消费模式、能力地图、阶段路线和不可越界边界。
10. `document-decision-register-zh.md` — 文档审核中的关键抉择点、冻结口径和下一轮删减规则。

## Read triggers

- 只需要统一 RTS 总体方向：读 1 和 2。
- 需要开发 KB 和索引层之间的边界：读 1、2、3。
- 需要向 PM、产品 owner、集成方或 AI agent 解释 runtime projection 运行包各目录/内容的产品含义：读 1、3、4。
- 需要判断 managed mode、tool mode、场景正常态和降级态：读 1、2、5、6。
- 需要落地第一版服务：读 1、2、3、5、6、7。
- 需要规划 Day1 后的检索/LLM/MCP 增强：读 1、2、3、5、6、7、8。
- 需要规划最终 LLM agent 服务形态、harness、MCP/API/Q&A/pipeline 统一入口：读 1、2、3、5、6、9。
- 需要判断长期路线与当前 phased implementation 的关系：读 1、5、6、7、8、9。
- 需要判断历史文档哪些保留、压缩、归档或删除：读 10。

## Conflict rule

Confirmed docs use topic-specific authority. Resolve conflicts with this table before applying any general reading order:

| Topic | Authority |
|---|---|
| RTS identity, dual-core positioning, truth ownership, source/evidence/review/human adjudication/signoff, scenario normal mode vs degraded mode, and document zone meaning | `project-alignment-summary-zh.md` |
| Non-negotiable system principles, safety boundaries, permissioned access, controlled analysis-and-expression, AI-first review, human final decision, and AI value guardrails | `system-constitution-v1.md` |
| KB/runtime projection/index boundary, release artifact shape, L0/L1/L2 meaning, schema/release/read constraints, and stable truth-source atomic capability surface | `kb-to-index-projection-contract-zh.md` |
| PM/product explanation of runtime projection package roles, including active release, manifest, scope, L2, navigation, dependencies, governance, permissions, index artifacts, and traces | `runtime-projection-product-guide-zh.md` |
| LLM placement, managed mode vs tool mode, framework/SDK boundary, shared atomic capability reuse, and PR diff / exception / failed-message scenario integration | `llm-harness-and-agent-integration-alignment-zh.md` |
| Complete internal LLM agent implementation work packages, current shipped state, runtime defaults, agent runtime, multi-step tool loop, tool parameter authority, scenario endpoint availability, service contracts, validation gates, memory/context, evaluation, rollout controls, and degraded-mode semantics | `internal-llm-agent-service-implementation-plan-zh.md` |
| Current Day1 query/tool service, filesystem store, Lucene, L2 read/hash, REST/MCP skeleton, `/query`, `/ask`, and Day1 DoD | `day1-query-service-and-llm-harness-plan-zh.md` |
| Day2 retrieval evolution, planner/orchestrator, rerank/vector/confusable active use, expanded MCP, impact/test candidate evolution, and evaluation metrics | `day2-agentic-retrieval-evolution-plan-zh.md` |
| Final long-range service roadmap, capability map, consumer views, and sequencing after current alignment decisions | `final-llm-agent-service-plan-zh.md` |
| Documentation cleanup, compression, archive/delete/retain decisions | `document-decision-register-zh.md` |

If a topic is not listed, follow the current baseline order above. `docs/reference/` materials may explain rationale but do not override confirmed docs. `docs/archive/` materials never override confirmed docs.

`docs/reference/java-index-layer-full-plan-zh.md` is a distilled index/query layer reference summary. The old full plan is archived and does not decide the current infrastructure stack.

Use `docs/reference/` for supporting rationale, engineering references, external review, OV history, and historical context. Use `docs/archive/` only when tracing older discussions or prototypes before deletion/consolidation.
