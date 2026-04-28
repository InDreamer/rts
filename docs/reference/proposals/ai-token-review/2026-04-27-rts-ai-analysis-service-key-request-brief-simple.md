# RTS AI Analysis Service PoC

> **A reusable LLM-backed backend service for transformation impact analysis, test planning, transaction sample discovery, and defect triage.**

---

## 1. 我们要申请什么

我们要申请 **LLM API key/token**，用于做一个 PoC：

**RTS AI Analysis Service PoC**

它不是聊天机器人，也不是个人 Copilot。  
它是一个可以被多个系统调用的后端 AI 分析服务。

它服务的对象包括：

- 开发 pipeline
- 测试流程
- 监控告警
- Chat / Copilot 入口
- 现有内部系统
- 其他 agent

---

## 2. 一句话说明

RTS AI Analysis Service 是一个可复用的 LLM 后端服务，用来帮助团队理解复杂报文转换逻辑，分析影响面，规划测试，寻找真实交易样本，并辅助缺陷排查。

---

## 3. 为什么需要它

报文转换系统有几个天然问题：

1. **报文对人不友好**  
   报文是高度结构化的数据，字段多、嵌套深、命名抽象，人很难直接读懂。

2. **业务含义藏得很深**  
   一个字段是什么意思，往往要结合产品、场景、规则、lookup、helper 和历史判断才能理解。

3. **依赖关系很深**  
   一个字段、规则或 helper 的变化，可能影响多个下游节点、测试场景和真实交易类型。

4. **准确性要求很高**  
   转换错了可能导致下游拒收、业务处理异常、返工、回滚和上线风险。

5. **测试准备成本很高**  
   测试人员需要知道该测什么、怎么测、哪里找真实样本，这些现在很依赖人工经验。

---

## 4. 它能做什么

### 4.1 理解复杂报文和规则

帮助人快速理解：

- 某个字段是什么意思
- 某个节点为什么存在
- 某条规则依赖哪些 lookup / helper
- 一个转换路径是怎么走的

**收益：**少翻文档，少问 SME，新人和测试更快进入上下文。

---

### 4.2 分析影响面

当有变更、异常、告警或下游拒收时，服务可以分析：

- 可能影响哪些字段
- 可能影响哪些规则
- 可能影响哪些 lookup / helper
- 可能影响哪些下游节点
- 哪些地方需要人工确认

**收益：**减少漏看依赖，提前发现风险。

---

### 4.3 规划测试场景

基于影响面和规则条件，服务可以给出：

- 必测场景
- 边界场景
- negative cases
- lookup miss / default branch 场景
- regression checklist

**收益：**测试准备更快，覆盖更系统，减少漏测。

---

### 4.4 寻找历史真实交易样本

服务可以把规则条件和影响模式转成历史交易搜索思路，例如：

- 需要哪些字段值
- 需要哪些节点存在
- 需要什么产品类型
- 需要什么条件组合
- 哪些场景缺少真实样本

**收益：**更快找到贴近生产的测试样本，提高回归测试可信度。

---

### 4.5 辅助缺陷分诊

当出现字段异常、下游拒收或监控告警时，服务可以给出：

- 可能原因
- 优先排查路径
- 相关规则和依赖
- 近期变更是否相关
- 需要补充哪些信息

**收益：**缩短排查时间，减少无效沟通。

---

### 4.6 辅助发布和回滚风险判断

服务可以根据影响面、测试覆盖和历史样本匹配情况，提示：

- 高风险字段
- 高风险依赖
- 上线观察点
- 回滚关注点

**收益：**上线前更早看到风险，减少盲区。

---

## 5. 为什么 Copilot 不够

Copilot 可以帮助个人：

- 写代码
- 补测试
- 解释局部代码
- 做一次性问答

但我们需要的是后端服务能力。

Copilot 不能很好解决这些问题：

- 不能稳定接入开发 pipeline
- 不能稳定接入测试 workflow
- 不能稳定接入监控告警
- 不能作为内部服务 API 被调用
- 不能统一接入 RTS 的规则、索引、依赖、历史交易上下文
- 不能统一做权限控制、审计、成本记录和结构化输出
- 不能让其他 agent 或系统复用同一套能力

所以我们需要 **LLM API key**，不是只靠个人 Copilot。

---

## 6. 极简架构

```text
                         LLM API Key
        reasoning / analysis / planning / triage
                              ▲
                              │
                    RTS AI Analysis Service
                              │
       ┌──────────────────────┼──────────────────────┐
       │                      │                      │
 Context Sources        Analysis Capabilities      Consumers

 - RTS rules            - message understanding    - Dev pipeline
 - RTS index            - dependency analysis      - Test workflow
 - dependencies         - impact analysis          - Monitor / alerts
 - message schemas      - test planning            - Chat / Copilot
 - mappings             - sample discovery         - internal services
 - historical trades    - defect triage            - other agents
 - defects / alerts     - risk notes
```

---

## 7. 它可以接入哪些地方

### 开发 pipeline

在代码或规则变更时，自动请求影响面分析和风险提示。

### 测试 workflow

根据影响面生成测试场景、回归清单和样本搜索建议。

### 监控告警

当出现拒收、字段异常或转换失败时，辅助生成排查路径。

### Chat / Copilot

用户仍然可以在熟悉的入口提问，但背后调用的是 RTS AI Analysis Service。

### 现有内部服务

例如 mapping 管理、release 管理、defect tracking、transaction search，都可以调用它。

### 其他 agent

其他 agent 可以通过 API/MCP 调用它，而不是各自重复实现报文理解能力。

---

## 8. PoC 安全边界

这个 PoC 不做这些事：

- 不自动改生产规则
- 不自动发布
- 不自动回滚
- 不替代人工审批
- 不直接写生产系统
- 不把模型输出当最终业务结论

它只做分析辅助。

初期建议使用：

- 脱敏数据
- 非生产材料
- 受控历史样本
- 最小权限访问
- 调用日志和成本记录

---

## 9. 怎么判断 PoC 有没有价值

可以观察这些指标：

| 指标 | 看什么 |
|---|---|
| 理解复杂报文的时间 | 是否比纯人工更快进入上下文 |
| 影响面覆盖 | 是否减少关键依赖遗漏 |
| 测试准备时间 | 是否更快形成 test scenarios / regression checklist |
| 真实样本发现时间 | 是否更快找到可用历史交易样本 |
| 缺陷分诊速度 | 是否更快形成有效排查路径 |
| SME 重复解释次数 | 是否减少反复问同样问题 |
| 服务复用性 | 是否能被两个以上入口调用，比如测试流程 + Chat/Copilot |

---

## 10. 给审批人的版本

我们申请 LLM API key/token，用于建设 **RTS AI Analysis Service PoC**。这是一个可复用的 LLM 后端服务，用于报文转换影响分析、测试规划、真实交易样本发现和缺陷分诊。

报文转换系统处理的是高度结构化、对人类不易直接阅读、并且和业务上下文深度绑定的数据。字段、节点、规则、lookup、helper 和条件分支之间存在复杂依赖，任何变更、异常或下游拒收都可能涉及非直观的影响路径。转换错误可能导致下游拒收、业务处理异常、返工、回滚和上线风险。

RTS AI Analysis Service 将在受控权限下读取 RTS 索引、规则依赖、报文结构、历史交易样本、缺陷和监控上下文，调用 LLM API 提供报文/规则理解、依赖路径解释、影响面分析、测试规划、历史交易样本搜索辅助、缺陷分诊和风险提示。

该能力不能由个人 Copilot 单独替代，因为它需要作为后端服务被开发 pipeline、测试 workflow、监控告警、Chat/Copilot、现有内部服务和其他 agent 调用，并具备权限控制、审计、成本管理、结构化输入输出和可复用服务面。

PoC 不做生产自动决策，不直接写生产系统，不替代人工审批；所有输出只作为人工分析、测试准备和排查决策的辅助依据。

---

## 11. English Version

We request an LLM API key/token to build the **RTS AI Analysis Service PoC**: a reusable LLM-backed backend service for transformation impact analysis, test planning, transaction sample discovery, and defect triage.

Message transformation systems process highly structured data that is difficult for humans to read directly and deeply tied to business context. Fields, nodes, rules, lookups, helpers, and conditional branches often have complex dependencies, so any change, anomaly, or downstream rejection may involve non-obvious impact paths. Incorrect transformations may cause downstream rejection, business processing issues, rework, rollback risk, and release risk.

RTS AI Analysis Service will use authorized RTS indexes, rule dependencies, message structures, historical transaction samples, defect context, and monitoring context. It will call the LLM API to support message/rule understanding, dependency path explanation, impact surface analysis, test planning, historical transaction sample search assistance, defect triage, and risk notes.

This capability cannot be replaced by individual Copilot usage because it must operate as a backend service callable by development pipelines, testing workflows, monitoring alerts, Chat/Copilot surfaces, existing internal services, and other agents, with permission control, auditability, cost management, structured inputs/outputs, and reusable API/MCP service surfaces.

The PoC will not make autonomous production decisions, write to production systems, or replace human approval. All outputs remain decision-support material for human analysis, test preparation, and investigation.

---

## 12. 不要这么说

申请时尽量不要说：

- AI 知识库
- AI agent app
- 通用 chatbot
- RAG demo
- 自动生成业务规则
- 自动审批
- 自动发布
- 自动回滚
- 替代 BA / reviewer / tester
- 只是一个 Copilot prompt
- 保证准确、消灭错误、全自动测试

---

## 13. 最短版本

**RTS AI Analysis Service PoC** 是一个可复用的 LLM 后端服务，用于报文转换影响分析、测试规划、真实交易样本发现和缺陷分诊。它不是个人 Copilot，也不是聊天机器人，而是可以接入开发、测试、监控、Chat/Copilot、内部服务和其他 agent 的共享 AI 分析能力。

Report complete: YES
