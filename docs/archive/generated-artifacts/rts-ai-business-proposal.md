# 从 Transformation Rule System 到客户交易流接入智能平台

## 建议总定位
**AI-assisted Client Flow Enablement & Legacy Operations Intelligence**

核心不是再做一个 chatbot，而是把 RTS 作为银行报文转换规则真相源，继续升级为可嵌入 onboarding、UAT、release、incident 流程的受控 AI 分析能力。

## 核心思想
- RTS 是面向银行报文转换场景的规则真相源系统。
- AI 的角色是 constrained reader / navigator / explainer / ambiguity marker。
- AI 不发明规则、不替代 signoff、不把 unknown 说成 certainty。
- 真相来自 governed pack objects：rules / lookups / helpers + evidence / review / signoff。
- 运行时只投影低噪声、高确定性的规则对象。

## 业务化包装
不要叫 Message Transformation AI 或 Support Copilot。建议叫：

**AI-assisted Client Flow Enablement Platform**

第一阶段模块：
1. Client / Upstream Onboarding Accelerator
2. Product / Flow Readiness Analyzer
3. Client Impact Intelligence

## Business Benefits
- 缩短客户/上游接入周期，提升 time-to-revenue。
- 加快新产品/新交易流支持度评估。
- 减少 BA/Tech/SME 手工比对和反复解释。
- 生产异常时更快输出客户影响和沟通摘要。
- 提升 release governance、auditability 和 legacy knowledge retention。

## 为什么 Copilot / Yoda 不能替代
Copilot 是个人生产力助手；Yoda 是 Confluence 知识库问答。本项目需要的是可被流程调用的后台 AI capability：
- 处理 XML/FpML/SCBML、mapping Excel、rule object、UAT defect、incident、release diff。
- 固定 prompt、固定 output schema、validation、retry、evidence link、human review。
- 嵌入 onboarding / UAT / release / incident，而不是人工打开聊天窗口提问。

## 为什么需要 API Token
API token 用于流程集成、结构化输入输出、可重复执行、审计追踪、安全边界和后续产品化。AI 不直接改生产、不做最终业务决策，只输出草稿和分析结果供人工复核。
