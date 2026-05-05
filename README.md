<!-- docmeta
role: entry
layer: 1
parent: null
children:
  - docs/confirmed/project-alignment-summary-zh.md
summary: repository entry for RTS truth service alignment docs and reference materials
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

RTS（Rule Truth Source / Transformation Rule System）是面向银行报文转换场景的规则真相服务。

它的最终形态不是静态文档库，也不是普通 RAG 问答机器人，而是一个可通过 API、MCP 或其他适配方式接入系统 pipeline、agent 工具链和问答场景的 truth service。

RTS 的核心职责是从尽可能准确和充分的 source 中整理 transformation rules，通过 AI-first review 和人工最终裁决形成可信规则，并按权限对外提供查询、解释、影响分析、测试规划和流程驱动能力。

## 先读什么

- `docs/confirmed/project-alignment-summary-zh.md` — 当前核心总纲；先读它来统一 RTS 最终服务愿景、多源真相治理、AI-first review、人工裁决和服务边界。
- `docs/confirmed/system-constitution-v1.md` — 系统底线；定义 truth-first、LLM 角色、证据优先和不确定性处理原则。
- `docs/reference/java-index-layer-full-plan-zh.md` — 索引/查询层参考方案；不代表 RTS 全系统基线或最终技术栈决定。
- `docs/reference/README.md` — 参考材料入口；包含 OV 边界、检索设计、术语表、外部评审和历史交接材料。
- `docs/archive/README.md` — 历史材料入口；只在需要追溯旧讨论或原型时阅读。

## 文档分区

- `docs/confirmed/`：当前内部对齐的默认阅读区。
- `docs/reference/`：有价值的参考材料，但不是默认 truth baseline。
- `docs/archive/`：历史材料，不属于 active baseline。

## 当前约定

- 主名称统一使用 RTS；TRS 只作为历史名称或别名出现。
- RTS 的核心叙事是“规则真相服务”。
- 核心对象必须一起理解：规则、规则包、证据链。
- AI 应尽可能完成整理、结构校验、跨 source 验证、歧义发现和问题简化。
- 人工 review 结论是冲突和歧义的最终裁决。
- API 面向系统 pipeline，MCP 面向 agent/问答；底层共享同一个 truth service。
- 文档可以讨论候选工程方案，但不要把 Java、OV、OpenSearch 等技术写成最终决定。
