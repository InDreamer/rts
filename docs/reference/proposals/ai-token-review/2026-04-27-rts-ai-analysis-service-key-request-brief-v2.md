# RTS AI Analysis Service PoC — LLM API Key 申请简报 v2

> Subtitle: **A reusable LLM-backed backend service for transformation impact analysis, test planning, transaction sample discovery, and defect triage.**  
> 中文副标题：**一个可复用的 LLM 后端服务，用于报文转换影响分析、测试规划、真实交易样本发现和缺陷分诊。**

## 1. 核心定位

RTS AI Analysis Service PoC 不是 AI 知识库、聊天机器人、Copilot prompt 或单点测试工具。它要验证的是：能否通过 LLM API 给 RTS 增加一个**可复用的后端 AI 分析能力**，把报文转换系统中难读、依赖深、准确性要求高、回滚风险大的结构化数据和规则关系，转化为开发、测试、运维、监控、聊天入口、Copilot 和其他 agent 都可以调用的分析服务。

申请 LLM API key 的理由不是“让某个人更方便聊天”，而是：这类能力必须作为后端服务运行，能够接入 pipeline、测试 workflow、监控告警、内部服务和其他 agent，并具备权限控制、审计、成本管理、结构化输入输出和可复用服务面。个人 Copilot 可以辅助写代码或解释片段，但不能替代这种共享、可审计、可系统集成的 AI 分析后端。

## 2. 最终标题与一句话 Pitch

### 推荐标题

**RTS AI Analysis Service PoC**

### 推荐副标题

**A reusable LLM-backed backend service for transformation impact analysis, test planning, transaction sample discovery, and defect triage.**

### 一句话 Pitch

通过 LLM API 赋能 RTS 后端分析服务，为报文转换相关工作流提供可复用能力：理解复杂报文结构与业务规则，解释字段/规则/节点依赖，分析潜在影响面，推导测试规划，辅助定位历史真实交易样本，支持缺陷分诊和风险判断，并通过 API/MCP 接入开发、测试、监控、聊天、Copilot、现有内部系统和其他 agent。

## 3. 这不是“变更发生时”的单点工具

“变更影响分析”是高价值入口，但不是唯一场景。RTS AI Analysis Service 应覆盖任何需要理解、分析、验证、排查、复用报文转换逻辑的工作流：

- **理解**：解释复杂报文结构、字段含义、规则链路和业务上下文。
- **影响分析**：分析字段、规则、lookup、helper、节点或错误现象可能影响的范围。
- **测试规划**：基于影响面、规则条件和交易模式推导测试场景和回归重点。
- **样本发现**：把规则/场景条件转成历史真实交易样本搜索策略。
- **缺陷分诊**：从告警、拒收、字段异常或报文片段中生成排查路径。
- **交接复用**：把复杂分析结果转成开发、测试、BA、运维都能使用的说明。
- **系统集成**：通过 API/MCP 被 pipeline、monitor、chat、Copilot、内部系统和 agents 复用。

因此它不是 change assistant，而是 **Transformation Analysis Backend Capability**。

## 4. 真痛点与收益

| 工作流 | 当前痛点 | RTS + LLM Service 做什么 | 真实收益 |
|---|---|---|---|
| 报文/规则理解 | 报文本质是高度结构化抽象数据，不适合人类直接阅读；字段含义依赖产品、节点、上下文和历史实现判断。 | 结合 RTS 规则、报文结构、依赖路径和上下文，把字段、节点、规则链路解释成人能理解的分析结果。 | 降低复杂报文理解成本；减少反复询问 SME；提升新人、开发、测试进入上下文的速度。 |
| 影响面分析 | 一个字段、lookup、helper 或条件分支变化，可能穿透多个 target 字段、下游节点、测试场景和历史交易模式，人工容易漏。 | 基于规则依赖、节点关系、message structure 和变更/错误上下文，列出可能影响面、依赖链和不确定点。 | 减少隐藏依赖遗漏；更早暴露风险；让 BA/Dev/QA/Reviewer 在同一张影响面上协作。 |
| 测试规划 | 测试人员需要从技术规则反推业务场景、边界条件、正反例、回归范围，准备成本高。 | 基于影响面和规则条件推导必测场景、边界场景、negative cases、lookup miss/default branch、regression checklist。 | 降低测试准备成本；提升测试覆盖结构化程度；减少 UAT/生产后才发现的问题。 |
| 历史交易样本发现 | 真实交易源信息结构复杂，人工很难快速找到覆盖某类节点/字段/条件组合的代表性样本。 | 将影响模式转成历史交易搜索条件：字段值、节点存在性、产品类型、条件组合、异常边界和缺口提示。 | 更快找到真实代表性样本；提升回归测试贴近生产的程度；减少人工翻样本时间。 |
| 缺陷分诊/监控告警 | 下游拒收、字段异常或监控告警出现时，需要倒推转换链路、近期变更、依赖规则和数据形态，排查慢。 | 接收告警、错误字段、报文片段、近期变更等上下文，生成可能原因、优先排查路径和相关规则/依赖。 | 缩短 MTTR；减少无效排查；让监控不只是报警，还能给调查上下文。 |
| 发布/回滚风险 | 转换系统准确性严苛，错误可能导致下游拒收、业务处理异常、修复成本和回滚风险。 | 基于影响面、测试覆盖、历史样本匹配和依赖关系生成 release/rollback risk notes。 | 更早识别上线和回滚关注点；支持更稳妥的发布判断。 |
| 跨角色协作 | BA、Dev、QA、Support 关注点不同，同一问题被反复解释，口径不一致。 | 将同一分析结果按角色输出：业务影响、技术依赖、测试重点、排查路径、待确认事项。 | 减少重复沟通；提升交接质量；沉淀可复用上下文。 |

## 5. 为什么必须是 LLM API，而不是 Copilot

### Copilot 适合什么

- 个人开发时补代码、写测试草稿、解释局部代码。
- 在 IDE 或聊天里做一次性辅助。
- 提升单个开发者的即时效率。

### Copilot 解决不了什么

- 不能稳定作为 RTS 后端服务被 pipeline、测试平台、监控告警、内部服务和其他 agent 调用。
- 难以统一接入 RTS 的索引、依赖图、报文结构、历史交易样本和缺陷/监控上下文。
- 难以做权限控制、审计、调用日志、成本管理和结构化 I/O 评估。
- 难以让同一套分析能力在 Dev/Test/Ops/Chat/Copilot/Agent 多个入口复用。
- 输出依赖个人 prompt，不适合形成组织级、可复用、可治理的分析能力。

### LLM API key 的必要性

LLM API key 是构建 RTS AI Analysis Service 的核心依赖。没有它，RTS 只能提供静态索引和传统查询；有了它，RTS 才能在受控上下文中完成跨规则、跨节点、跨交易模式的推理、归纳、规划和排查建议，并把这些能力暴露为后端服务。

## 6. 极简架构表达

```text
                         [ LLM API Key ]
        reasoning / impact analysis / test planning / triage
                                  ▲
                                  │ core unavailable capability
                                  │
                    [ RTS AI Analysis Service ]
        - scoped context assembly
        - message structure understanding
        - rule / dependency analysis
        - transformation impact analysis
        - test planning and regression support
        - historical transaction sample discovery
        - defect triage and risk notes
                    ▲                              │
                    │ reads / searches             │ reusable API / MCP / service surface
                    │                              ▼
 [ Context Sources ]                         [ Consumers ]
 - RTS index / rules / dependencies          - Dev pipeline
 - message schemas / mappings                - Test workflow
 - lookup / helper / branch relations        - Monitor / alerting
 - historical source transactions            - Chat / Copilot surfaces
 - defect / monitoring / release context     - Existing internal services
                                              - Other agents
```

### 架构要点

- **LLM API Key**：核心不可替代能力，用于推理、归纳、测试规划、样本搜索策略和排查路径生成。
- **RTS AI Analysis Service**：企业内部可控后端服务，负责组装上下文、调用 LLM、校验结构化输出、记录审计并暴露 API/MCP。
- **Context Sources**：RTS 规则/索引/依赖/报文结构是主上下文，历史交易、缺陷、监控、发布数据按权限增强。
- **Consumers**：开发、测试、监控、聊天、Copilot、内部服务和其他 agents 都可复用同一能力。

## 7. 可复用服务能力清单

| 能力 | 输入 | 输出 | 消费方 | Benefit |
|---|---|---|---|---|
| Message / Rule Understanding | 字段、节点、规则、报文片段、产品范围 | 人类可读解释、依赖路径、相关上下文 | BA、Dev、QA、Chat/Copilot | 降低复杂报文理解成本。 |
| Impact Surface Analysis | 字段/规则/lookup/helper/错误现象/变更描述 | 可能影响面、依赖链、不确定点、风险区域 | Dev pipeline、Reviewer、BA | 减少影响遗漏，更早暴露风险。 |
| Dependency Path Explanation | 起点字段/规则、目标字段、依赖图 | 转换路径、lookup/helper/branch 参与点 | Dev、Reviewer、Support | 让深依赖链可解释。 |
| Test Planning | 影响面、规则条件、历史缺陷、报文结构 | 测试场景、边界条件、negative cases、回归清单 | QA、Test workflow | 降低测试准备成本，提升覆盖质量。 |
| Historical Transaction Sample Discovery | 场景条件、字段组合、节点存在性、产品类型 | 历史交易搜索条件、候选样本模式、覆盖缺口 | QA、数据准备、Support | 更快找到真实代表性样本。 |
| Defect Triage | 告警、拒收、错误字段、报文片段、近期变更 | 可能原因、排查顺序、相关规则/依赖 | Monitor、Ops、Support、Dev | 缩短初始定位时间。 |
| Release / Rollback Risk Notes | 影响面、测试覆盖、依赖关系、历史样本匹配 | 高风险点、上线观察点、回滚关注项 | Release owner、Ops、管理层 | 降低发布和回滚盲区。 |
| Cross-role Summary | 完整分析结果、目标角色 | BA/Dev/QA/Ops 视角摘要和待确认事项 | 项目团队、交接流程 | 减少重复解释，统一口径。 |

## 8. PoC 边界与安全说法

- PoC 初期使用脱敏、非生产或受控材料；如接入历史交易/缺陷/监控数据，必须最小权限、明确范围。
- LLM 输出只作为分析辅助，不作为最终业务规则、生产变更或发布决策。
- 不直接写生产系统，不自动修改转换规则，不自动触发发布或回滚。
- 最终确认、测试判断、上线决策仍由人工负责。
- 服务记录调用来源、上下文范围、模型调用、token/成本、输出版本和人工反馈。
- 对越权查询、跨 scope 召回、敏感字段泄露、prompt injection 和日志敏感信息做基础控制。

## 9. PoC 成功指标

| 指标 | 观察方式 |
|---|---|
| 复杂报文/规则理解时间 | 对比人工独立理解 vs RTS+LLM 辅助后的理解时间。 |
| 影响面覆盖 | 与 SME/人工 goldenset 对比，观察关键依赖是否更少遗漏。 |
| 测试准备时间 | 从分析结果到 test scenario / regression checklist 可评审的时间。 |
| 历史样本发现时间 | 找到符合字段/节点/条件组合的真实样本所需时间。 |
| 缺陷分诊速度 | 从告警/拒收到形成第一批有效排查假设的时间。 |
| 重复 SME 澄清次数 | 同一问题在 BA/Dev/QA/Ops 间重复解释次数。 |
| 输出采纳率 | 人工评审后建议被采纳、部分采纳、废弃的比例。 |
| 服务复用性 | 是否能被至少两个不同入口调用，例如 test workflow + chat/Copilot，或 pipeline + monitor。 |

## 10. Stakeholder Benefit Framing

| Stakeholder | 该怎么讲 |
|---|---|
| Management / Approver | 这是一个受控 PoC，用来验证 LLM API 在高风险、高人工成本的报文转换场景中是否能减少分析、测试、排查和回滚成本，不是泛 AI demo。 |
| BA / Analyst | 更快解释字段和业务上下文，识别待确认问题，减少重复解释。 |
| Developer | 改动前能看到依赖链和潜在影响，降低“改完才发现影响别处”的风险。 |
| Tester / QA | 从影响面直接得到测试场景、回归清单和真实交易样本搜索策略。 |
| Support / Operations | 告警/拒收后更快得到相关规则、可能原因和排查路径。 |
| Architecture / Platform | 把 AI 分析能力做成可复用、可权限控制、可审计、可服务化接入的 backend capability，而不是散落在个人 prompt 里。 |

## 11. 可直接粘贴的申请段落

### 中文

我们申请 LLM API key/token，用于建设 **RTS AI Analysis Service PoC**：一个可复用的 LLM 后端服务，用于报文转换影响分析、测试规划、真实交易样本发现和缺陷分诊。报文转换系统处理的是高度结构化、对人类不易直接阅读、且与业务上下文深度绑定的数据；字段、节点、规则、lookup、helper 和条件分支之间存在复杂依赖，任何变更、异常或下游拒收都可能涉及非直观的影响路径。RTS AI Analysis Service 将在受控权限下读取 RTS 索引、规则依赖、报文结构、历史交易样本、缺陷和监控上下文，调用 LLM API 提供报文/规则理解、依赖路径解释、影响面分析、测试规划、历史交易样本搜索辅助、缺陷分诊和风险提示，并通过 API/MCP 接入开发 pipeline、测试 workflow、监控告警、Chat/Copilot、现有内部服务和其他 agent。该能力不能由个人 Copilot 单独替代，因为它需要作为后端服务被多工作流调用，并具备权限控制、审计、成本管理、结构化输入输出和可复用服务面。PoC 不做生产自动决策、不直接写生产系统、不替代人工审批；所有输出均作为人工分析、测试准备和排查决策的辅助依据。

### English

We request an LLM API key/token to build the **RTS AI Analysis Service PoC**: a reusable LLM-backed backend service for transformation impact analysis, test planning, transaction sample discovery, and defect triage. Message transformation systems process highly structured data that is difficult for humans to read directly and deeply tied to business context. Fields, nodes, rules, lookups, helpers, and conditional branches often have complex dependencies, so any change, anomaly, or downstream rejection may involve non-obvious impact paths. RTS AI Analysis Service will use authorized RTS indexes, rule dependencies, message structures, historical transaction samples, defect context, and monitoring context, then call the LLM API to support message/rule understanding, dependency path explanation, impact surface analysis, test planning, historical transaction sample search assistance, defect triage, and risk notes. The capability cannot be replaced by individual Copilot usage because it must operate as a backend service callable by multiple workflows, with permission control, auditability, cost management, structured inputs/outputs, and reusable API/MCP service surfaces. The PoC will not make autonomous production decisions, write to production systems, or replace human approval; all outputs remain decision-support material for human analysis, test preparation, and investigation.

## 12. 避免说法

避免把申请讲成：

- AI knowledge base / AI agent app / generic chatbot / RAG demo。
- truth source、pack、projection、L0/L1/L2 作为主叙事。
- AI 自动生成业务规则、自动审批、自动发布、自动回滚。
- 替代 BA / reviewer / tester / approver。
- 只是给 Copilot 一个更好的 prompt。
- 保证准确、消灭错误、全自动生成完整测试。
- 把“规则候选提取、证据绑定、歧义检测、结构化 draft”作为孤立卖点。

## 13. Completion

Report complete: YES
