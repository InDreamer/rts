<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: active decision register for resolving outdated, ambiguous, or conflicting RTS documentation directions
read_when:
  - 需要决定哪些历史文档思想继续保留、降级或删除
  - 需要审核 RTS 文档中跨时期产生的关键抉择点
  - 需要整理 confirmed/reference/archive 的下一轮删减计划
skip_when:
  - 只需要运行服务或调用 API
  - 只需要某个具体接口或字段说明
source_of_truth:
  - docs/confirmed/README.md
  - docs/reference/README.md
  - docs/archive/README.md
-->

# RTS Documentation Decision Register

> 状态：active review register
> 日期：2026-05-08
> 目的：把长期讨论中留下的关键抉择点集中登记，防止旧 proposal、旧 token 审核和新 confirmed baseline 同时指导实现。

## 1. 已固定的文档裁决

这些点不再作为开放问题处理。

| 主题 | 当前裁决 | 文档处理 |
|---|---|---|
| RTS 定位 | RTS 是双核心规则真相服务：既是受控真相源 / 原子能力服务，也是托管 LLM agent 分析服务；不是普通 RAG、聊天机器人或单个 agent app。 | 保留在 confirmed；旧 proposal 只作历史材料。 |
| Truth 权威 | canonical truth 来自 source / evidence / review / human adjudication / signoff；LLM answer、retrieval hit、agent memory 不是 truth。 | 保留在 confirmed 和 distilled reference。 |
| Runtime projection | projection 是 approved truth 的服务运行视图，不能丢失 L2 结构化规则语义。 | 以 projection contract 为准；旧“摘要式 KB”口径废弃。 |
| RTS service 边界 | RTS service 拥有 runtime truth access boundary：release、scope、permission、hash、dependency、grounding、refusal、trace。 | 保留在 confirmed。 |
| Agent/SDK 边界 | OpenAI/Claude/LangChain/PageIndex 等可做 adapter、harness 或 sidecar，不拥有 truth access core。 | 保留在 confirmed alignment note。 |
| 接入模式 | RTS 同时支持 managed LLM harness 和 external tool mode；两者共享同一组稳定原子能力面。 | 保留在 confirmed alignment note。 |
| 历史 AI token 审核稿 | 长稿已合并为风险原则摘要；不再让 token PoC 口径限制长期服务方向。 | 原始长稿移出默认 reference 路径并归档；保留 consolidated summary。 |
| Java index/reference 长稿 | 旧全量方案已归档；reference 默认只保留当前适用的短摘要。 | 原文在 archive；短摘要保留旧方案中的安全工程原则。 |
| Final LLM agent plan 长稿 | 旧全量 final plan 已归档；confirmed 默认只保留最终路线图。 | 细节回收到 Day1、Day2 和 LLM alignment；原文在 archive。 |
| LLM enhanced reference 长稿 | 旧 LLM/index/harness 长讨论已归档；reference 默认只保留 scope、context、memory 和 controlled tools 摘要。 | 原文在 archive；当前优先级回到 confirmed Day1/Day2/final roadmap/alignment。 |
| Day1/Day2 长稿 | 旧全量 Day1/Day2 计划已归档；confirmed 默认保留 compact baseline/roadmap。 | Day1 保留实现边界和 DoD；Day2 保留演进能力和 guardrails。 |
| External/OV/glossary 长 reference | 旧长 reference 已归档；reference 默认保留约束摘要、OV lessons 和短词表。 | 用于 rationale，不覆盖 confirmed baseline。 |

## 2. 需要 owner 决定的抉择点

这些点会影响后续删文档、拆文档和实现优先级。

### D1. Day1 是否代表当前实现阶段，而不是历史 Phase 1

状态：已执行。

裁决：Day1 = 当前 Java query/tool service + controlled LLM harness 的落地阶段；Day2 = Day1 后的 controlled agentic retrieval / MCP / impact / test planning 演进阶段。历史材料里的 Phase 1/Stage 1/token PoC 不再作为当前阶段命名。

已采用：A。confirmed 默认文档使用 Day1/Day2 或 roadmap step；历史 Phase/Stage 只在 archive/reference 中作为原文或历史术语出现。

### D2. Oversized confirmed docs 是保留原样，还是拆成决策面

状态：已执行。final LLM agent plan、Day1 plan 和 Day2 plan 都已压缩，并归档原长版。

原问题：Day1 plan、Day2 plan、final LLM agent plan 都很长，且部分内容重叠。它们现在可读，但会让 agent 和人类读者很难判断“这段是否仍是当前事实”。

建议决定：

- **A 保留原长文**：只靠 README 路由和 conflict rule 控制。
- **B 拆分为更小叶子文档**：按 query service、LLM harness、MCP/tools、scenario adapters、memory/context、observability 拆。
- **C 先不拆，先把 final plan 压缩成路线图**：详细实现事实回收到 Day1/Day2/新的 alignment docs。

已采用：C。Day1/Day2 后续若实现继续增长，再按实际模块拆 B。

### D3. Java index layer reference plan 是否继续保留为长 reference

状态：已执行。旧全量方案已归档，默认 reference 路径改为短摘要。

原问题：它保留了早期 Java 索引层全量设想，包含 OpenSearch/vector/Knowledge-Bases 等旧术语。虽然已有对齐备注，但仍可能让读者以为这是当前工程基线。

建议决定：

- **A 保留长 reference**：继续靠顶部备注说明它不是 baseline。
- **B 压缩为短 reference 摘要**：只保留仍适用的 Java implementation ideas，删除旧阶段大纲。
- **C 归档原文，新增短 distilled reference**：让默认 reference 只暴露当前可用思想。

已采用：C。

### D4. Runtime projection 的 governance view 具体暴露到什么程度

当前 confirmed 方向：default operational view 低噪声；authorized governance view 可展开 evidence/review/report/adjudication pointer 或 summary。

仍待决定：

- **A 只暴露 pointers**：runtime service 默认不返回 evidence/review 原文。
- **B 暴露 structured summaries**：授权后返回摘要、状态、裁决理由和引用。
- **C 暴露原文片段**：仅在 audit/review 权限下读取受控片段。

推荐：B 起步，C 作为后续权限化扩展。

### D5. 第一个场景 endpoint 选择哪个

状态：已冻结。

当前缺口：结构化 API 已有价值，但用户体感需要一个端到端场景展示 managed AI 正常模式。

裁决：先把 `/ask` 的 grounded managed analysis answer 做完整，再优先 `analyze_pr_diff`，然后是 `investigate_exception`。这些场景的 authority boundary 仍保留，但不再把 candidate 边界写成能力上限；LLM 不可用时，能力降级为信息提供服务。

### D6. LLM provider/framework 选择是否进入文档主线

当前方向：core service provider-neutral，provider SDK 只是 adapter；复杂 agent framework 不进入 truth core。

仍待决定：

- **A 只记录 provider-neutral 接口**：不把 OpenAI/Claude/LangChain 写成路线承诺。
- **B 选 OpenAI SDK 做第一 adapter**：文档记录为 implementation choice，不是架构绑定。
- **C 引入 LangGraph/LangChain 做 workflow sidecar**：只在有 long-running workflow 需求后考虑。

推荐：A+B，暂不做 C。

### D7. reference/proposals 是否继续保留

当前状态：AI token review 已压缩；archive proposal 已去重。仍有一些 reference/proposal 历史材料可能继续产生噪声。

建议决定：

- **A 保留当前 reference/proposals 目录**：只在 README 标注历史。
- **B 继续压缩 reference/proposals**：每个历史主题只留一个摘要。
- **C 移到 archive**：reference 只保留 distilled docs。

推荐：B 到 C 的渐进方案。

## 3. 下一轮整理规则

之后删除或合并文档时使用以下规则：

1. confirmed 中的当前事实优先；reference 不覆盖 confirmed；archive 只作历史。
2. 如果一篇历史文档的有用内容已经被 distilled reference 或 confirmed 吸收，删除原长稿或移动到 archive。
3. 如果一个文档主要贡献是决策背景，而不是当前事实，保留为短 rationale，不保留完整讨论流。
4. 如果一篇文档仍在表达旧口径，但只靠顶部备注纠偏，优先压缩或归档。
5. 如果一个决策点还需要 owner 拍板，先登记到本文，不在实现文档中同时保留多个互相竞争的方向。
