# RTS LLM API Key 申请简报：AI 辅助报文转换影响分析与测试设计 PoC

## 1. 核心结论

RTS 申请 LLM API key 的最强定位不是“做一个 AI 知识库/聊天机器人”，而是建设一个可被开发流水线、测试流程、监控告警、Copilot/聊天界面、既有内部服务和其他 agent 调用的 **AI 辅助报文转换影响分析与测试设计后端服务**。报文转换变更的真实难点在于：数据结构抽象、字段业务含义依赖上下文、规则/节点依赖链深、准确性要求高、错误会带来拒收/业务处理异常/回滚风险，且测试准备成本高。LLM API key 是核心依赖，因为服务需要在受控上下文中进行跨规则推理、影响面归纳、测试场景设计、历史样本搜索策略和排查路径规划；Copilot 只能帮助个人在 IDE/聊天中写代码或解释片段，不能作为稳定、可审计、可权限控制、可被流水线/监控/测试系统/内部服务调用的后台能力。

## 2. 最强申请标题和一句话 pitch

### 候选标题

1. **AI 辅助报文转换影响分析与测试设计 PoC**  
   **AI-assisted Transformation Impact Analysis and Test Design PoC**
2. **面向报文转换系统的 RTS AI 分析服务 PoC**  
   **RTS AI Analysis Service for Transformation Systems PoC**
3. **报文转换变更风险与回归测试智能辅助 PoC**  
   **AI-assisted Change Risk and Regression Planning for Message Transformation PoC**

### 推荐标题

**AI 辅助报文转换影响分析与测试设计 PoC**  
**AI-assisted Transformation Impact Analysis and Test Design PoC**

### 一句话 pitch

通过 LLM API 赋能 RTS 后端分析服务，在报文转换变更发生时自动汇总影响面、解释依赖路径、派生测试场景、辅助寻找历史真实交易样本，并向开发、测试、监控、聊天和其他内部系统提供可复用 API 能力。

## 3. 真痛点，不是创造需求

| 工作流 | 当前痛点 | RTS + LLM 服务会做什么 | 具体收益 |
|---|---|---|---|
| 变更影响分析 | 报文字段/节点不是自然语言业务对象；一个字段变更可能影响多个 target 字段、lookup、helper、派生规则和下游校验，人工靠经验串依赖容易漏。 | 接收变更描述、受影响字段/规则、目标系统范围；读取 RTS 索引、规则依赖、消息结构和变更上下文；输出影响面、依赖链、可能受影响的目标字段/业务场景和不确定点。 | 缩短变更分析时间；减少隐藏依赖遗漏；让 reviewer/BA/开发在同一张影响面上讨论。 |
| 测试设计与回归准备 | 测试人员需要从技术变更反推业务场景、边界条件、正反例和回归范围；准备慢且依赖资深 SME。 | 基于影响面生成测试场景建议、边界条件、回归检查清单、需要覆盖的源字段组合和预期观察点。 | 更快完成测试设计；覆盖更系统；减少“只测改动点、漏测依赖点”的风险。 |
| 历史交易样本发现 | 真实源交易数据结构复杂，难以用肉眼或简单关键字找到能覆盖变更模式的样本；测试数据常常不贴近真实业务。 | 将受影响规则/字段组合转换为历史交易样本搜索条件或查询策略，提示需要哪些字段值、节点存在性、组合模式和异常边界。 | 更快找到贴近生产形态的样本；提升测试可信度；减少人工翻样本时间。 |
| 缺陷分诊 / 监控告警调查 | 下游拒收、字段异常或告警出现时，需要倒推转换链路、最近变更、依赖规则和可能数据形态，排查路径不清。 | 接收告警/缺陷信息、报文片段、错误字段、最近变更；给出可能原因假设、优先排查路径、相关规则/依赖和需补充证据。 | 缩短 MTTR；让支持/开发更快定位方向；减少无效来回询问。 |
| 跨角色协作 / 交接 | BA、开发、测试、运维关注点不同，同一变更经常被重复解释；口径不一致导致误测、漏测或误解风险。 | 将同一分析结果按角色生成简明视图：业务影响、技术依赖、测试重点、发布/回滚注意事项、未确认问题。 | 减少重复沟通；提升交接质量；让审批和 release readiness 更清晰。 |

## 4. 为什么 LLM API 是必要核心，而不是 Copilot 能解决

### Copilot 的强项

- 适合个人开发者在 IDE 中补全代码、解释局部代码、生成单元测试草稿。
- 适合交互式问答和一次性辅助理解。
- 适合提升单人编码效率。

### Copilot 的限制

- 不是可被流水线、测试平台、监控系统、内部服务或其他 agent 稳定调用的后端 API。
- 难以统一接入 RTS 的受控索引、权限、审计、成本日志和企业数据边界。
- 难以把同一套分析能力复用到开发、测试、告警、发布、聊天等多个入口。
- 输出依赖个人 prompt，难以形成标准输入/输出 schema、回归评估和服务级治理。

### 为什么后端 LLM API 必须存在

- 影响分析需要跨规则、跨节点、跨依赖链推理，不是单个代码片段解释。
- 测试设计需要把“技术变更”转成“业务场景 + 数据组合 + 回归范围”，需要模型的归纳和规划能力。
- 历史样本发现需要把规则条件转成可搜索的数据模式，而不是简单全文检索。
- 告警/缺陷调查需要结合错误现象、最近变更、转换路径和历史上下文形成排查假设。
- 服务必须可审计、可限权、可计费、可记录输入输出、可做 PoC 指标评估。

### 为什么可复用服务面重要

如果只是 Copilot prompt，收益停留在个人效率；如果是 RTS AI Analysis Service，能力可以被：

- CI/CD 在变更时自动请求影响分析；
- 测试平台在建测时请求场景和样本搜索建议；
- 监控告警在异常时请求排查路径；
- Chat/Copilot 界面作为入口调用同一后端能力；
- 既有内部服务通过 API/MCP 等协议复用能力。

这就是 LLM API key 的必要性：它不是为了“让人聊天”，而是为了把 LLM 推理能力嵌入企业工作流。

## 5. 极简架构表达

```text
                         [ LLM API Key ]
          reasoning / impact analysis / test planning / triage
                                  ▲
                                  │ core dependency
                                  │
                    [ RTS AI Analysis Service ]
        - scoped context assembly
        - message structure understanding
        - rule and dependency impact analysis
        - test scenario and regression planning
        - historical transaction sample search assistance
        - defect triage and release/rollback risk notes
                    ▲                              │
                    │ reads / searches             │ reusable API/MCP/service surface
                    │                              ▼
 [ Context Sources ]                         [ Consumers ]
 - RTS index, rules, dependencies            - Dev pipeline
 - message schemas / source-target mapping   - Test workflow
 - historical source transactions            - Monitor / alert investigation
 - change / release metadata                 - Chat / Copilot surfaces
 - defect / monitoring context               - Existing internal services
                                              - Other agents
```

### 模块说明

- **LLM API Key**：核心推理能力来源，用于影响面归纳、依赖解释、测试规划、样本搜索策略和排查路径生成；不是可选装饰。
- **RTS AI Analysis Service**：企业内部可控后端服务，负责组装上下文、调用 LLM、执行结构化输出校验、记录审计和向外暴露 API。
- **Context Sources**：RTS 规则/索引/依赖/报文结构是主上下文，历史交易、缺陷、监控、发布数据是按权限接入的增强上下文。
- **Consumers**：同一服务可服务开发、测试、运维、聊天入口、Copilot 扩展、内部系统和其他 agent，避免把能力锁在个人 IDE 里。

## 6. 可复用服务能力清单

| 能力 | 输入 | 输出 | 消费方 | 收益 |
|---|---|---|---|---|
| 影响面分析 | 变更描述、字段/规则 ID、source/target 范围、版本差异 | 受影响字段、规则、依赖链、业务场景、不确定点 | 开发流水线、架构评审、BA | 让变更评估更快、更完整。 |
| 依赖路径解释 | 起点字段/规则、目标字段、依赖图 | 人类可读的转换路径、lookup/helper 参与点、关键条件 | 开发、BA、Reviewer | 降低结构化报文和规则链的理解成本。 |
| 测试场景派生 | 影响面、规则条件、消息结构、业务上下文 | 正向/反向/边界/回归场景建议 | 测试平台、QA、开发 | 把技术影响转成可执行测试设计。 |
| 回归检查清单生成 | 变更范围、依赖链、历史缺陷类别、发布范围 | 回归检查项、优先级、需确认问题 | QA、Release owner | 减少漏测依赖点，提高 release readiness。 |
| 历史交易样本搜索辅助 | 目标场景、字段条件、节点存在性、规则组合 | 样本搜索条件、候选数据模式、覆盖缺口提示 | 测试数据准备、QA | 更快找到真实交易样本，降低人工筛选成本。 |
| 缺陷分诊假设与调查路径 | 告警/缺陷、错误字段、报文片段、近期变更 | 可能原因、优先排查步骤、相关规则和需补证据 | Support、Ops、开发 | 缩短排查路径，减少无效沟通。 |
| 发布/回滚风险提示 | 变更影响面、回归结果、关键依赖、已知问题 | 风险点、回滚关注字段、上线观察点 | Release manager、运维、管理者 | 让发布决策有结构化输入，但不自动替人决策。 |
| 跨角色摘要 | 完整分析结果、目标角色 | BA/开发/测试/运维视角摘要和待确认事项 | 项目团队、交接会议 | 减少重复解释，统一沟通口径。 |

## 7. PoC 边界和安全说法

- **数据边界**：PoC 优先使用脱敏、非生产或受控样本；如需连接内部历史交易或缺陷/监控数据，必须按最小权限和审批范围接入。
- **权限边界**：服务按角色、系统、产品线、环境限制可见数据；不得绕过现有访问控制。
- **决策边界**：LLM 只提供分析建议、测试建议、排查假设和风险提示；不做自动业务签核、不做生产决策、不替代 reviewer。
- **写入边界**：PoC 不直接写生产系统、不自动修改转换规则、不自动触发发布/回滚。
- **准确性边界**：输出必须标记依据、假设、不确定点和需人工确认事项；证据不足时明确说明“不足以判断”。
- **审计与成本**：记录请求来源、上下文范围、模型调用、token/成本、输出版本和人工反馈；设置调用频率、预算和日志保留策略。
- **安全控制**：对 prompt injection、敏感字段泄露、跨 scope 召回、越权查询和日志敏感信息做基础防护与测试。

## 8. 衡量成功的指标

以下是 PoC 评估指标，不是上线前承诺值；需先建立人工 baseline，再比较 RTS + LLM 辅助后的结果。

| 指标 | 衡量方式 | PoC 观察目标 |
|---|---|---|
| 变更影响分析耗时 | 同类变更从接收需求到形成影响面初稿的时间 | 是否明显减少人工梳理时间。 |
| 依赖遗漏率 | Goldenset 中人工标注依赖 vs 服务提示依赖的覆盖情况 | 是否减少关键依赖漏看。 |
| 测试准备耗时 | 从影响分析到测试场景/回归清单可评审的时间 | 是否加快测试设计。 |
| 历史样本发现时间 | 找到满足特定字段/节点/规则组合样本所需时间 | 是否减少翻样本和试错成本。 |
| 回归覆盖质量 | 场景是否覆盖直接变更、依赖规则、边界条件和负向条件 | 是否提升覆盖结构化程度。 |
| 缺陷分诊速度 | 从告警/缺陷到形成第一批有效排查假设的时间 | 是否缩短初始定位时间。 |
| 重复 SME 澄清次数 | 同一变更中 BA/开发/测试/运维反复询问次数 | 是否降低重复解释和交接成本。 |
| 输出采纳率 | 人工评审后被采纳/部分采纳/废弃的建议比例 | 判断服务是否真正有用，而不是只会生成文字。 |

## 9. Stakeholder-specific benefit framing

| Stakeholder | 主要卖点 |
|---|---|
| Management / Approver | 用一个受控 PoC 验证 LLM API 在高成本、高风险的报文转换工作流中的实际 ROI；不是泛 AI demo，而是减少分析、测试、回滚和支持成本。 |
| BA / Analyst | 更快看懂技术变更对应的业务影响、受影响场景和待确认问题，减少重复解释。 |
| Developer | 变更前更快看到依赖链和潜在下游影响，减少“改完才发现影响别处”。 |
| Tester / QA | 从影响面直接得到测试场景、边界条件、回归清单和样本搜索策略，降低测试准备成本。 |
| Support / Operations | 告警或拒收后更快获得可能原因和排查路径，降低 MTTR 和跨团队等待时间。 |
| Architecture / Platform Team | 形成可复用、可权限控制、可审计的后端 AI 能力，而不是分散在个人 Copilot prompt 中的不可治理经验。 |

## 10. Final application paragraph

### 中文申请段落

我们申请 LLM API key/token，用于建设 **AI 辅助报文转换影响分析与测试设计 PoC**。本 PoC 面向报文转换系统的真实痛点：报文数据高度结构化且不易读，字段业务含义依赖上下文，规则和节点之间存在深层依赖，小变更可能产生非显性的下游影响，而转换错误会导致下游拒收、业务处理异常、返工和回滚风险。RTS 将作为受控后端分析服务，读取已授权的规则索引、依赖关系、报文结构、历史交易样本和缺陷/监控上下文，调用 LLM API 进行影响面分析、依赖路径解释、测试场景设计、历史样本搜索辅助和缺陷分诊建议，并通过 API/MCP/聊天或流水线集成提供给开发、测试、监控和既有内部系统。该能力不能由个人 Copilot 单独替代，因为它需要服务化调用、权限控制、审计、成本管理、结构化输入输出和多工作流复用。PoC 不做生产自动决策、不直接写生产系统、不替代人工审批，所有输出均作为人工评审和执行的辅助依据。

### English application paragraph

We request an LLM API key/token to build an **AI-assisted Transformation Impact Analysis and Test Design PoC** for RTS. The PoC targets a concrete pain point in message transformation systems: transformation messages are highly structured and hard to read, field meaning depends heavily on business and message context, rule and node dependencies can be deep, and a small change may create non-obvious downstream impact. Incorrect transformations can cause downstream rejection, business processing issues, expensive remediation, and rollback risk. RTS will act as a controlled backend analysis service that reads authorized rule indexes, dependency data, message structures, historical transaction samples, and defect/monitoring context, then uses the LLM API to support impact analysis, dependency explanation, test scenario design, historical sample search assistance, and defect triage suggestions. This cannot be replaced by individual Copilot usage because the required capability must be service-callable, auditable, permission-controlled, cost-managed, schema-based, and reusable across development pipelines, testing workflows, monitoring, chat surfaces, existing internal services, and other agents. The PoC will not make autonomous production decisions, write to production systems, or replace human approval; all outputs remain decision-support material for human review.

## 11. What to avoid saying

避免把申请讲成以下方向，因为它们听起来泛、虚、风险高，或偏离真实工作流价值：

- “AI agent app” / “智能 agent 平台”作为主叙事。
- “truth source / canonical pack / projection / runtime memory”作为申请主故事。
- “AI 自动生成业务规则”或“自动修正规则”。
- “替代 BA / reviewer / tester / approver”。
- “自动生产决策”“自动发布”“自动回滚”。
- “通用 chatbot / 通用 RAG / 企业知识库问答”。
- “规则候选抽取”“证据绑定”“歧义检测”“结构化草稿生成”作为孤立卖点。
- “让 Copilot 更聪明”或“给开发者一个更好的 prompt”。
- “保证准确”“消灭错误”“全自动覆盖所有测试”。
- 过度强调内部术语而不是业务痛点：pack、projection、goldensource、L0/L1/L2 等只能作为实现背景，不应成为审批叙事核心。

## 12. Completion

Report complete: YES
