# Transformation Rule System — 领域架构方案说明

RTS 建议定位为面向银行报文转换场景的受治理领域知识架构。

它不是另一个聊天机器人，而是一套可复用语义基座，让多个 AI 能力安全地建立在同一套规则真相之上。

## 架构分层
1. 真相层：canonical packs、rules、lookups、helpers、evidence、review、signoff
2. 投影层：只投影已批准运行时对象
3. 索引层：URI、metadata、L0/L1/L2 摘要、依赖图、检索轨迹
4. 查询 / AI 层：受 scope 约束的 API 化 AI 消费
5. 流程层：onboarding、UAT、release、incident、audit workflows

## Use case landscape
- 基座：规则解释、字段血缘、依赖导航、证据支撑回答、歧义感知回答
- 流程：影响分析、回归测试推荐、发布准备度、incident 解释、变更意图校验
- 业务：客户接入、新产品准备度、客户影响分析、审计解释、组织知识恢复

## 为什么不是 Copilot / Yoda
Copilot 是个人生产力；Yoda 是 Confluence 问答。RTS 是受治理规则真相架构，API 化 AI 是可嵌入流程的受控消费者。

## 为什么需要 API Token
API token 用于流程集成、结构化材料、受控输出、审计、安全控制和平台扩展。