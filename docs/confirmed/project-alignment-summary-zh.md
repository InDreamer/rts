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

RTS 是面向银行报文转换场景的 **规则真相服务**。

它不是静态文档库、普通 RAG、聊天机器人，也不是某个固定 agent SDK 的包装。RTS 的核心职责是从尽可能准确和充分的 source 中整理 transformation rules，经过 AI-first review、人工裁决和 signoff，形成可治理、可追溯、可审查、可查询、可解释的规则真相，并按权限提供给系统、人和 agent 使用。

LLM 是 RTS 的核心能力之一，但不是 truth owner。LLM 可以整理、检索、解释、发现冲突、提出候选分析和把输出写得更容易读；真正的 truth 仍来自 source、evidence、review、人工裁决、signoff 和发布版本。

## 最终服务形态

RTS 最终应该是统一 truth service，而不是多个割裂工具。

它要支持：

- 事实查询：某个 target 字段如何生成，依赖哪些 source、lookup、helper 和 rule。
- 规则解释：规则为什么可信，证据、review、signoff 和风险状态是什么。
- 影响分析：某个 source、字段、lookup、helper 或规则变化后影响哪些对象、测试或下游流程。
- 测试规划：基于规则、依赖和条件生成测试点、覆盖建议和风险提示。
- 流程驱动：被 CI/CD、数据治理、迁移、review、incident 或其他系统 pipeline 调用。
- Agent 接入：通过 REST/MCP/tool mode，让 AI agent 在权限和 trace 边界内读取 RTS truth。

不同入口可以有不同体验，但底层必须共享同一个 runtime truth access boundary。

## 核心对象

RTS 里必须一起理解三类对象。

**Rule** 描述具体 transformation logic，例如某个 target 字段如何从 source 字段、lookup、helper、常量或条件逻辑生成。Rule 不是一句自然语言解释，而是结构化、可引用、可验证的规则对象。

**Rule pack** 是规则整理、治理、review 和 signoff 的基本组织单元。`rules/`、`lookups/`、`helpers/` 是主对象层；`evidence/`、`review/`、`reports/` 是治理侧横切层。主对象应保持结构化和精简，不把长 review 过程塞进 rule body。

**Evidence chain** 说明规则为什么可信。它可以来自业务文档、Java/XSLT、Excel mapping、lookup table、示例输入输出、reviewer clarification、人工裁决和其他可追溯 source。

## Truth 形成流程

推荐流程：

```text
source material
  -> extracted assertion
  -> candidate rule / lookup / helper
  -> AI pre-review and conflict simplification
  -> human adjudication
  -> signoff
  -> runtime projection release
  -> API / MCP / Q&A / pipeline consumption
```

AI-first governance 的意思是：AI 尽可能完成整理、结构校验、跨 source 验证、冲突发现、缺失标注和问题简化；人工介入聚焦在最终裁决，而不是重复做机械整理。

如果 AI 只能说“看起来应该是这样”，但找不到 evidence，它只能形成 hypothesis 或 open question，不能进入 approved truth。

## 冲突和可信度

当规则、source、证据或历史讨论互相冲突时，RTS 不让模型自行决定最终真相。

稳定原则：

- AI 发现冲突、解释来源和影响，并提出候选建议。
- 人工 review 结论是最终裁决。
- 裁决必须记录为可追溯治理结果。
- 服务回答必须能区分事实、推断、不确定点、候选建议和人工决定。

可信度不是搜索分数。它来自 review/signoff 状态、source 可靠性、source 覆盖度、多源一致性、未解决歧义、依赖完整性和发布状态。

## KB、Projection、Index 和 Answer

KB 和 runtime projection 都是机器优先知识结构，不是“KB 给人读、projection 给机器读”的区别。

当前边界：

- **KB** 是 governed truth 的完整机器知识图，包含 rule / lookup / helper 逻辑，也包含 source、evidence、review、report、ambiguity、adjudication、signoff 和 lineage。
- **Runtime projection** 是 approved truth 的服务运行视图，面向查询、索引、诊断、影响分析、测试规划和 agent tools。它可以裁剪治理噪声、重排结构和拆分视图，但不能丢失服务回答事实所需的结构化 L2 规则语义。
- **Index document** 是 projection 派生出的召回和导航视图，例如 object card、L0/L1、alias、confusable、search text。它帮助找到对象，不拥有 truth，也不能替代 L2 runtime object。
- **Governance view** 是在权限允许时展开 evidence、review、report、conflict、adjudication 的视图。
- **LLM answer** 是对上述材料的解释、整理或候选分析，不是新的 truth。

详细字段和索引读取边界以 runtime projection contract 为准。

## Scope 和检索边界

RTS 必须强制区分 channel、product、pack、domain 和 rule scope。

这不是单纯的索引优化，而是防止相似字段、相似 lookup、相似 helper 被误当成当前问题的 truth。API、MCP、Q&A、pipeline 和 agent 查询都应该先收窄 scope，再读取具体规则、依赖和授权治理视图。

低噪声检索原则：

- 先用 scope、manifest、object identity 做硬过滤。
- 再用 card / L0 / L1 做导航、消歧和排序。
- 最终事实必须回到 L2 runtime object 或授权 governance view。
- dependency hints 可以用于导航、解释和影响候选，但不能自动越过 scope、权限或 release 状态。

## 服务和权限边界

RTS service 不拥有 canonical truth，但它拥有 runtime truth access boundary。

它负责：

- active release
- scope partition
- caller permission
- object state / release gate
- L2 hash validation
- dependency traversal limit
- grounding validation
- refusal contract
- trace and audit

普通问答、agent tools、pipeline、治理人员可以看到不同视图。权限边界必须和检索边界对齐：caller 不能检索或读取自己无权访问的 pack、rule、evidence 或 review 信息。

## LLM 和 Agent 边界

RTS 不需要在“内置 LLM harness”和“外部 agent 调 API”之间二选一。

当前结论：

```text
managed mode:
  caller -> RTS /ask or scenario endpoint
  RTS internal LLM harness plans and calls RTS tools

tool mode:
  external agent -> RTS REST/MCP tools
  external agent plans broader workflow
```

两种模式共享同一套 release、scope、permission、L2、grounding、refusal 和 trace 规则。OpenAI/Claude/LangChain/LangGraph/PageIndex 等可以作为 adapter、harness、sidecar 或 ingestion 实验参考，但不能拥有 truth core，也不能绕过 RTS service 直接读取 projection store 后自行回答。

一句话：

```text
LLM investigates and explains.
RTS service gates and proves.
Runtime projection supplies governed truth material.
```

## 输出纪律

RTS/LLM 输出必须明确区分：

- **facts**：能引用 rule、L2、dependency、source、evidence 或 review 记录的内容。
- **inferences**：基于 facts 做出的分析。
- **unknowns**：证据不足、scope 不清或存在冲突的地方。
- **candidates**：AI 生成、等待人确认的影响面、测试点、调查路径或规则候选。
- **human decisions**：reviewer 已裁决或 signoff 的结果。

影响分析、测试规划、root cause、release/rollback 这类输出尤其不能把候选建议写成最终结论。

## 当前阶段

截至 2026-05-08，按当前实现和本地验证结果，当前实现阶段可理解为：

```text
Day1 RTS Query / Tool Service mostly working
Controlled LLM Harness partially wired
Deterministic impact / test / message / governance / pipeline support tools exist
PR diff / exception / failed-message managed LLM scenario endpoints not yet built
```

也就是说，结构化 API 输出多是合理的当前状态。下一步的关键不是把 truth core 交给某个 agent framework，而是完成 managed `/ask` 的可读 grounded answer、把现有 deterministic 工具面稳定成统一工具层，然后做 PR diff impact、exception investigation 或 failed-message analysis 这类 managed LLM 端到端场景 endpoint。

## OpenViking / OV 定位

OV 在 RTS 中只作为检索和知识组织思想参考。

可借鉴：

- 稳定寻址
- 分层阅读
- scope-aware retrieval
- 低噪声上下文加载

不能误解：

- OV 不是 RTS 的 truth owner。
- OV 不是当前必须采用的工程基线。
- OV/向量/外部搜索框架不能替代 runtime projection、L2 truth surface 和 RTS service gate。

## 文档分区

**confirmed** 是当前 baseline。做项目对齐、投影契约、Day1/Day2 实现、LLM harness 和 agent 接入时优先读。

**reference** 是有价值的参考材料。它解释背景、rationale、OV lessons、检索原则、术语和旧方案中仍可借鉴的工程思想，但不覆盖 confirmed。

**archive** 是历史材料。只在需要追溯旧讨论、旧产物或原型时阅读。

## 推荐阅读顺序

第一次进入项目时：

1. 先读本文，建立 RTS 的 truth service 心智模型。
2. 再读 system constitution，确认不可越界原则。
3. 做 KB/index 边界时读 runtime projection contract。
4. 落地当前服务时读 Day1 baseline。
5. 规划检索、LLM、MCP、impact/test 演进时读 Day2 roadmap。
6. 规划最终 LLM agent service 和场景接入时读 final roadmap 与 LLM alignment note。
7. 审核、压缩、归档历史文档时读 document decision register。

## 当前对齐结论

RTS 的核心不是“把规则文档放进 AI 问答”，而是建设一个以多源 evidence、AI-first governance、人工最终裁决和权限化服务发布为基础的规则真相服务。

最终 RTS 应能被 API、MCP、agent、问答界面和系统 pipeline 调用。它可以给事实、给分析建议、驱动流程，但所有输出都必须回到可追溯 source、结构化 rule pack、runtime projection、L2 runtime object、AI review 结果和人工 review 裁决。
