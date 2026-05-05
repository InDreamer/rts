<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: 中文项目总纲，统一 RTS 最终服务愿景、多源真相治理、AI-first review、人工裁决和服务边界
read_when:
  - 第一次进入 RTS 项目
  - 需要统一项目核心思路、服务形态、治理流程和术语
  - 需要判断哪些文档是当前 baseline，哪些只是参考或历史材料
skip_when:
  - 已经明确只需要某一篇叶子文档的细节
source_of_truth:
  - docs/confirmed/system-constitution-v1.md
  - docs/reference/project-keywords-glossary-zh.md
  - docs/reference/external-review-final-2026-04-20.md
-->

# RTS 项目对齐总纲

## 一句话定义

RTS 是面向银行报文转换场景的规则真相服务。

它不是一组散文件，也不是一个外接 AI 的文档库。RTS 对外应该是一个服务：外部系统、人、agent 或 pipeline 可以通过 API、MCP、问答界面或其他入口调用它。

RTS 内部包含 Knowledge Base、索引/检索、规则整理流程和 LLM。LLM 是 RTS 的核心能力之一：它可以基于 Knowledge Base 和检索结果整理信息、解释规则、发现冲突、生成候选分析和回答问题。

但 LLM 的回答不自动等于 truth。真正的 truth 仍然来自 source、evidence、review、人工裁决和 signoff。

它也不应该被理解成单一 use case。RTS 更像一个 rule-truth foundation：只要 transformation truth 被治理清楚，就可以派生字段解释、影响分析、测试规划、发布检查、审计辅助、onboarding 等多个 use case。

RTS 的核心职责是：从尽可能准确和充分的 source 中整理 transformation rules，建立可治理、可追溯、可审查、可查询、可解释的规则真相，并把这些真相以受权限控制的方式提供给系统和人使用。

## RTS 最终要服务什么

RTS 最终应该是统一服务，而不是多个互相割裂的工具。

它可以支持：

- 事实查询：某个 target 字段如何生成，依赖哪些 source、lookup、helper 和规则对象
- 规则解释：为什么这条规则可信，证据来自哪里，review 状态是什么
- 影响分析：某个 source、字段、lookup、helper 或规则变化后，会影响哪些 target、pack、测试或下游流程
- 测试规划：基于规则、依赖和证据生成测试点、覆盖建议和风险提示
- 流程驱动：被 CI/CD、数据治理、迁移、review 或其他系统 pipeline 调用，触发检查、报告或后续任务
- Agent 接入：通过 MCP 或类似工具协议，让 AI agent 在明确权限和边界内读取 RTS truth

这些能力可以有不同入口，但底层必须共享同一个 truth service。

## RTS 不是什么

RTS 不是：

- 让 LLM 自由生成业务规则的系统
- 把 prompt、聊天记录或 agent memory 当成真相源的系统
- 只服务 `Tradition -> Stella` 的一次性项目
- 某个固定技术栈、某个检索框架或某个 agent 协议的包装

技术方案可以演进，接入方式也可以变化，但 RTS 的核心边界不能变：规则真相必须来自可追溯 source、AI 辅助校验和人工最终裁决后的治理结果。

## 核心对象

RTS 里最不能误解的是三类对象必须一起看：规则、规则包、证据链。

### Rule

Rule 描述具体 transformation logic，例如某个 target 字段如何从 source 字段、lookup、helper、常量或条件逻辑生成。

Rule 不是一句自然语言解释，而应该是结构化、可引用、可验证的规则对象。

### Rule pack

Rule pack 是规则整理、治理、review 和 signoff 的基本组织单元。

它把一组相关 rules、lookups、helpers、evidence、review 记录和报告放在同一个治理边界内，方便审查、追溯和发布。

在 canonical pack 里，`rules/`、`lookups/`、`helpers/` 是主对象层，说明转换规则本身；`evidence/`、`review/`、`reports/` 是治理侧横切层，说明这些规则为什么可信、还有什么歧义、谁做过裁决。主对象应保持精简，不把长 evidence、review 过程和 approval history 都塞进 rule body。

### Evidence chain

Evidence chain 说明规则为什么可信。

它可以来自：

- 业务文档
- Java extractor 或其他现有代码
- XSLT template
- Excel mapping workbook
- 数据库或静态映射表
- 示例输入/输出
- reviewer clarification 和人工裁决记录
- 其他可追溯 source

Source 应该尽可能准确、充分和可追溯。RTS 不应盲信单一输入，而应尽量覆盖可用权威来源，并暴露来源之间的一致、冲突和缺失。

## 从 source 到 truth service 的流程

RTS 的目标流程是：

1. 收集 source：业务文档、代码、XSLT、Excel、映射表等
2. 抽取和整理：基于规则把多源材料整理成结构化 rule pack
3. AI 预审：检查结构完整性、跨 source 一致性、冲突、缺失和歧义
4. AI 简化问题：把复杂冲突整理成 reviewer 容易判断的问题、候选解释和建议
5. 人工裁决：reviewer 对关键歧义和冲突做最终决定
6. Signoff：形成可被服务默认使用的治理结果
7. 服务发布：按权限向 API、MCP、问答和 pipeline 暴露查询、解释、分析或流程驱动能力

这里的方向是 AI-first governance：尽可能让 AI 完成整理、检查、交叉验证、问题归纳和建议生成；人工介入应聚焦在最终裁决，而不是重复做机械整理。

## 冲突和裁决原则

当规则、source、证据或历史讨论互相冲突时，RTS 不应该让模型自行决定最终真相。

稳定原则是：

- AI 负责发现冲突
- AI 负责解释冲突来源和影响
- AI 可以提出建议和可信度判断
- 人工 review 结论是最终裁决
- 裁决必须被记录为可追溯治理结果

因此，RTS 的最高优先级不是“模型觉得合理”，也不是“某个 source 看起来更像权威”，而是经过 AI 辅助整理后形成的人工 review 结论。

## 可信度表达

RTS 服务不只应该返回答案，还应该能说明答案有多可信。

可信度可以综合来自：

- review / signoff 状态
- source 类型和来源可靠性
- source 覆盖是否充分
- 多个 source 是否互相一致
- 是否存在 unresolved ambiguity 或 conflict
- rule、lookup、helper 依赖是否完整

服务返回时可以按调用方需要暴露不同细节。简单问答可以只返回结论和关键依据；治理或审查场景可以返回 rule ID、evidence、review 状态、冲突记录和可信度解释。

## 权限和服务边界

RTS 服务应该支持分权限开放。

不同调用方可以看到不同范围：

- 普通问答：主要读取已 signoff 或 approved 的规则结论
- Agent 工具：读取规则、依赖、可信度和必要 evidence，用于解释和辅助分析
- Pipeline：调用影响分析、测试规划、规则检查或发布前校验
- 治理人员：查看 draft、review 中的规则、source 冲突、AI 预审结果和人工裁决记录

也就是说，RTS 可以同时给事实、给分析建议、驱动流程，但这些能力必须受权限、状态和治理边界控制。

## Scope 分区和检索边界

RTS 必须强制区分 source-target channel、product、pack 和 rule scope。

这不是单纯的索引优化，而是防止 AI 拿错相似规则的结构性安全边界。不同系统或产品线里的相似字段、相似 lookup、相似 helper，如果没有强 scope 约束，很容易被误当成当前问题的 truth。

RTS 应保留低噪声检索原则：先用 L0 短摘要做范围和候选过滤，再用 L1 结构化 overview 做 disambiguation 和导航，最后才读取 L2 原始规则或授权 evidence。L1 不是文档装饰，而是防止相似但错误的 pack/rule 被误召回的安全层。

RTS 也应优先使用 URI-like stable addressing，让 channel、product、pack、rule、lookup、helper 能被 API、MCP、Q&A、pipeline 和审计日志稳定引用。具体 URI scheme 可以后定，但对象身份必须稳定、可追溯、可审计。

API、MCP、Q&A、pipeline 和 agent 查询都应该先收窄 scope，再读取具体规则和证据。详细检索原则见 `docs/reference/rts-retrieval-principles.md`。

## 发布和服务约束

RTS 服务发布不能被理解成“生成一份索引就完成”。

RTS 对外使用的规则版本不能被悄悄覆盖。每次 approved truth 变化，都应该生成新的发布版本，并记录当前启用的是哪一版、上一版是什么、必要时可以回到哪一版。

第一阶段建议按 pack 发布：一个 pack 没有整体 review/signoff 前，其中的 rule、lookup、helper 不应该进入默认服务答案。这样可以避免外部系统读到“半个 approved pack”。

发布前必须先处理冲突。如果两个 pack/rule 覆盖同一个 target、同一个 URI，或者 COMMON 与 product-specific 规则冲突，RTS 不能等到查询时再临时猜哪个对；这些冲突应在发布前被发现、记录和裁决。

RTS 在以下情况应该拒答、降级回答或要求澄清，而不是硬给答案：

- scope 不清
- 冲突还没裁决
- 依赖的 rule / lookup / helper 尚未发布
- 只有 L0/L1 摘要，但没有可读取的原始规则对象
- 当前启用的发布版本不唯一或不可追溯

L0/L1 摘要不是随便生成的说明文字。它们会影响 AI 找到什么、排除什么，因此也需要版本、质量检查和回归样例。

每个 RTS 答案都应该能追溯到 canonical rule 版本、projection 发布版本和 query 结果，而不是只记录“搜到了什么”。

Dependency hints 第一阶段只用于展示和导航。它可以告诉用户“这条规则还关联哪些 lookup/helper”，但不应该自动扩大查询范围，也不应该替代 scope 和 approved 状态来决定答案。

权限边界要和检索边界对齐：用户或 agent 不能检索到自己无权看的 pack、rule、evidence 或 review 信息。

如果已经关闭的 ambiguity 后来被重开，必须产生新的 truth 版本和发布流程，不能偷偷修补当前服务结果。

详细发布、拒答、trace 和权限原则见 `docs/reference/rts-publication-and-refusal-principles.md`。

## RTS 的四层心智模型

RTS 对外可以是一个服务，但内部必须分清几件事：什么是 source，什么是 candidate，什么是 approved truth，什么是 retrieval result，什么是 LLM answer。

一个简单的四层模型是：

1. **L1：Truth 层**  
   这里决定什么是真的。它保存 evidence、canonical pack、review、ambiguity、人工裁决、signoff 和 truth 版本。

2. **L2：整理和投影层**  
   这一层做两件事：一是把 source 整理成 candidate rules、冲突点和待裁决问题；二是把 approved truth 投影成给运行时使用的低噪声视图。它不决定 truth。

3. **L3：查找和追踪层**  
   这一层负责让 truth 被找到，例如 URI、索引、L0/L1/L2、dependency hints、query trace。它帮助定位 truth，但不拥有 truth。

4. **L4：使用入口层**  
   这一层负责人和系统怎么使用 RTS，例如 Workbench、Review UI、API、MCP、Q&A、pipeline、agent tools。它可以展示、解释、提交 candidate 或 open question，但不能直接改 approved truth。

这个模型不是为了增加术语，而是为了避免混淆：LLM answer、retrieval hit、candidate rule、approved truth 不是同一件事。

## Workbench 和 AI-first governance

RTS 的工作台不应该让 AI 直接写最终规则。它应该让 AI 把复杂 source 整理成人更容易裁决的材料：

- 候选规则、lookup、helper
- 每个候选对应的 source/evidence
- source 之间的冲突
- 缺失证据
- 需要 reviewer 裁决的问题
- 可能影响的 pack、field、dependency 和测试范围

推荐的 truth 形成流程是：

```text
Evidence → extracted assertion → candidate rule → reviewer decision → approved truth
```

如果 AI 只能说“看起来应该是这样”，但找不到 evidence，那它只能形成 open question 或 hypothesis，不能进入 approved truth。

当 AI 发现无法判断、source 冲突或证据不足时，RTS 应把问题放进 ambiguity / open question 队列，而不是生成一个流畅但不可靠的答案。

## RTS 输出纪律

RTS/LLM 的输出应该明确区分：

- **事实**：能引用 rule、source、evidence 或 review 记录的内容
- **推断**：基于事实做出的分析或候选解释
- **不确定点**：证据不足、scope 不清或存在冲突的地方
- **候选建议**：AI 生成、等待人确认的影响面、测试点、规则候选或问题
- **人工决定**：reviewer 已经裁决或 signoff 的结果

RTS 不能把候选建议写得像最终结论。尤其是影响分析、测试规划、root cause、release/rollback 这类内容，必须能看出哪些是事实，哪些只是候选或推断。

## Agent / API / MCP 边界

外部 agent、API 或 MCP 工具可以调用 RTS 来读取、引用、解释和分析 approved truth，也可以提交 candidate、发现的问题或 open question。

它们不能直接修改 approved truth。任何新发现、新规则、新解释或新冲突，都应该进入 candidate/review 流程，而不是直接写回 canonical pack。

一个简单规则是：

```text
read approved truth, cite it, submit candidates; never rewrite truth directly
```

## Truth change summary

每次 approved truth 发生变化后，RTS 应生成 truth change summary，说明：

- 哪些 rules、lookups、helpers 新增、修改或废弃
- 影响哪些 target fields、pack、dependency 或测试范围
- 是否改变 runtime projection
- 是否还有 unresolved ambiguity
- 下游 API、MCP、agent 或 pipeline 应使用哪个 snapshot/version

这让 RTS 不只是保存规则，还能管理 truth 的变化。

LLM 是 RTS 的重要参与者，但不是 truth owner。

LLM 可以：

- 从多源材料中抽取候选规则
- 整理 rule pack
- 检查结构完整性
- 交叉验证不同 source
- 标出冲突、缺失和歧义
- 把复杂问题简化成人工可裁决的问题
- 在证据约束下做解释、影响分析和测试建议

LLM 不能：

- 在没有依据时补业务逻辑
- 把 unresolved ambiguity 改写成确定事实
- 用模型推断覆盖人工 review 结论
- 把 runtime memory 写回 canonical truth
- 绕过 signoff 把规则发布为默认可用真相

## 为什么 RTS 不是普通 RAG

普通 RAG 的重点通常是“从文档里找相似内容并回答”。

RTS 的重点是“先把规则真相治理清楚，再让服务读取和使用这些真相”。

区别在于：

- RTS 关心 source 是否准确、充分、互相一致
- RTS 关心 rule pack 是否结构完整
- RTS 关心歧义是否被发现、简化和裁决
- RTS 关心 review/signoff 状态
- RTS 关心不同调用方是否有权限看到不同层级的信息
- RTS 的答案应该能回到 rule、evidence 和 review 记录

## 当前样例的定位

`Tradition -> Stella` 可以继续作为具体样例，但它不是 RTS 的边界。

它的作用是帮助说明 source-to-target transformation rule 的治理方式。RTS 应该被设计成可扩展到更多 source-target channel、更多产品线和更多系统 pipeline 的 truth service。

## OpenViking / OV 的定位

OV 在 RTS 文档中只作为检索和知识组织思想参考。

可以借鉴的思想包括：

- 稳定寻址
- 分层阅读
- scope-aware retrieval
- 低噪声上下文加载

但 OV 不是 RTS 的 truth owner，也不是当前必须采用的工程基线。

## 文档分区怎么理解

### `docs/confirmed/`

当前内部对齐的默认阅读区。

- `project-alignment-summary-zh.md`：本文，核心总纲
- `system-constitution-v1.md`：RTS Constitution v2，定义系统底线和治理原则
- `kb-to-index-projection-contract-zh.md`：KB 到查询/索引层的运行时投影契约，定义 KB 发布后必须产出的 projection，以及索引层的读取边界
- `day1-query-service-and-llm-harness-plan-zh.md`：Day1 查询服务、轻量索引层和受控 LLM harness 落地方案
- `day2-agentic-retrieval-evolution-plan-zh.md`：Day2 受控 agentic retrieval、rerank、MCP 扩展、影响分析和测试规划演进方向

### `docs/reference/`

有价值的参考区，但不是默认 truth baseline。

适合查项目背景、pack/object model、OV 边界、检索设计、术语表、外部评审、历史交接材料和提案背景。

### `docs/archive/`

历史材料区，不属于 active baseline。

只在需要追溯旧讨论、旧目录或原型时阅读。

## 推荐阅读顺序

第一次进入项目时，建议按这个顺序：

1. `README.md`
2. `docs/confirmed/project-alignment-summary-zh.md`
3. `docs/confirmed/system-constitution-v1.md`
4. `docs/confirmed/kb-to-index-projection-contract-zh.md`
5. 需要落地第一版服务时，读 `docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md`
6. 需要规划 Day1 后的检索/LLM/MCP 增强时，读 `docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md`
7. `docs/reference/project-keywords-glossary-zh.md`
8. 需要历史 Java 工程参考时，再读 `docs/reference/java-index-layer-full-plan-zh.md`
9. 需要 OV 背景时，再读 `docs/reference/ov-boundary-and-adoption.md` 和 `docs/reference/ov-kb-retrieval-design.md`
10. 需要历史样例背景或 pack/object model 时，再读 `docs/reference/rts-project-background-and-pack-model.md`

## 当前对齐结论

RTS 的核心不是“把规则文档放进 AI 问答”，而是建设一个以多源准确 evidence、AI-first governance、人工最终裁决和权限化服务发布为基础的规则真相服务。

最终的 RTS 应该能被 API、MCP、agent、问答界面和系统 pipeline 调用；它可以给事实、给分析建议、驱动流程，但所有输出都必须回到可追溯 source、结构化 rule pack、AI review 结果和人工 review 裁决。
