<!-- docmeta
role: entry
layer: 1
parent: null
children: []
summary: repository entry for RTS dual-core baseline, operational docs, and supporting references
read_when:
  - first entering this repository
  - deciding which RTS document to read first
skip_when:
  - the exact design leaf document is already known
source_of_truth:
  - README.md
  - docs/confirmed/project-alignment-summary-zh.md
-->

# RTS

RTS（Rule Truth Source / Transformation Rule System）是面向银行报文转换场景的**双核心服务**：

- **受控真相源 / 原子能力服务**
- **托管 LLM agent 分析服务**

它不是静态文档库、普通 RAG 问答机器人，也不是某个固定 agent SDK 的包装。RTS 的核心职责是从尽可能准确和充分的 source 中整理 transformation rules，通过 AI-first review、人工最终裁决和 signoff 形成可信规则真相，并按权限把这些真相能力和受控分析能力提供给系统、人和 agent 使用。

这两个核心能力是并列的一等公民：托管分析服务不是可选 UI 糖衣，真相源原子能力服务也不是只在 AI 不可用时才存在的 fallback substrate。

AI 在 RTS 中不是 answer organizer，而是建立在 governed truth 之上的**受控分析与表达层**。deterministic 能力不是低级 fallback；它是 RTS 的第一等公民信息服务能力。对 PR diff、exception、failed message、test planning 等场景，托管 AI 分析是目标正常产品形态；当 LLM 不可用时，RTS 会降级为结构化信息提供服务，继续为调用方或外部 agent 提供真相材料。

## 先读什么

- `docs/INDEX.md` — 文档主入口；用于在 confirmed、reference、archive、operational 之间选择路径。
- `docs/confirmed/project-alignment-summary-zh.md` — 当前双核心总纲；先读它来统一 RTS 最终服务愿景、多源真相治理、AI-first review、人工裁决和服务边界。
- `docs/confirmed/system-constitution-v1.md` — 系统底线；定义 truth-first、受控分析与表达、证据优先和不确定性处理原则。
- `docs/confirmed/kb-to-index-projection-contract-zh.md` — KB 到索引/查询层的运行时投影契约，以及稳定原子能力面的边界。
- `docs/confirmed/llm-harness-and-agent-integration-alignment-zh.md` — RTS 内置 managed mode、外部 tool mode、runtime projection access boundary 和场景接入的当前对齐结论。
- `docs/confirmed/internal-llm-agent-service-implementation-plan-zh.md` — RTS service 内部 LLM agent 接入的完整非阶段性落地计划。
- `docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md` — Day1：建立受控真相源原子能力面和第一版 managed LLM harness。
- `docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md` — Day2：在同一边界内扩展 controlled agentic retrieval、场景 AI 能力和外部 agent 复用面。
- `docs/confirmed/final-llm-agent-service-plan-zh.md` — 最终面向 LLM agent 的 RTS 服务路线图摘要。
- `docs/confirmed/document-decision-register-zh.md` — 文档审核中的关键抉择点、冻结口径和下一轮删减规则。
- `docs/java-service-runbook-zh.md` — 本地运行、配置、测试、排障和服务维护。
- `docs/api-caller-guide-zh.md` — API 调用方如何发起 deterministic truth/information 查询、managed analysis 请求和场景分析请求。
- `docs/reference/README.md` — 参考材料入口；包含 OV 边界、检索设计、术语表、外部评审和历史 rationale。
- `docs/archive/README.md` — 历史材料入口；只在需要追溯旧讨论或原型时阅读。

## 文档分区

- `docs/confirmed/`：当前内部对齐的默认阅读区，定义双核心产品定位和活跃 baseline。
- `docs/java-service-runbook-zh.md` / `docs/api-caller-guide-zh.md`：运行和调用 RTS service 的操作文档。
- `docs/reference/`：有价值的支撑材料，但不覆盖 confirmed baseline。
- `docs/archive/`：历史材料，不属于 active baseline。

## 当前约定

- 主名称统一使用 RTS；TRS 只作为历史名称或别名出现。
- RTS 的核心叙事是“双核心规则真相服务”。
- 核心对象必须一起理解：规则、规则包、证据链。
- AI 应尽可能完成受控分析、结构校验、跨 source 验证、歧义发现、冲突简化、依赖解释、影响候选、测试候选和 reviewer-friendly 表达。
- 人工 review 结论是冲突和歧义的最终裁决。
- candidate-only / human decision 是权威边界，不是 AI 能力上限。
- API 与 MCP/tool mode 共享同一个真相源原子能力面；managed mode 与外部 agent tool mode 共享同一 truth access boundary。
- Day1 工程方向是 JDK 17 Java 查询/索引服务 + filesystem projection store + Lucene + local L2 store + controlled LLM harness。
- 最终服务方向是：受控真相源原子能力服务 + 托管 LLM agent 分析服务；两者都是第一等公民。
- 文档可以讨论候选工程方案，但不要把 OV、OpenSearch、向量库等技术写成 Day1 必选。

## 本地运行

Java 服务运行手册见 `docs/java-service-runbook-zh.md`。

最短启动示例：

```bash
RTS_STORE_ROOT="$PWD/sample-projection/runtime-store" \
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
mvn spring-boot:run
```

默认端口是 `8080`，启动后验证：

```bash
curl -s http://localhost:8080/mcp/tools | jq
```
