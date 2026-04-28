# RTS AI Backend Service Deep Recommendations

本文基于 `.hermes/reports/2026-04-28-rts-ai-backend-service-critical-token-review.md` 的完整审阅结论，并结合 confirmed baseline 与既有申请材料，目标不是为广义 AI backend service 辩护，而是把现有方案改造成更可能通过严格审查的、证据驱动的申请路径。

## Executive recommendation / 总体建议

- **战略重定位**：不要把第一轮申请定义为“可被 pipeline、monitor、Chat/Copilot、内部服务和其他 agent 调用的 AI backend service”。第一轮应定义为 **RTS 变更影响分析与测试规划评估工作台 / evaluation harness**，用于证明 LLM 在已批准规则与依赖上下文上，是否比确定性索引、模板、Copilot/Yoda 和人工流程产生可衡量增量。
- **产品边界**：RTS 的长期核心仍应是 governed truth service / traceable retrieval service；LLM 只能作为受约束的候选分析器，不是 truth owner、release advisor、incident commander 或跨系统数据汇总器。
- **申请逻辑**：先请求“受控评估许可”和“可审计人工触发 harness”，不要请求“服务化 token + 多入口集成”。审批方首先需要看到失败边界、数据边界和对照实验，而不是愿景图。
- **价值证明顺序**：先证明两个用例：`transformation impact analysis` 与 `test planning / regression checklist`。其他用例一律降级为后续候选，不作为首轮 token justification。
- **扩展原则**：只有当 LLM 在同一 goldenset 上稳定超过 deterministic index/templates 与 Copilot/Yoda，并且没有严重漏报、越权、敏感数据和 unsupported assertion，才讨论服务化和更多 caller。

**Owner 应停止说：**“RTS AI backend service 可以服务 pipeline、monitor、Chat/Copilot、internal services、other agents，并覆盖样本发现、缺陷分诊、release/rollback notes。”

**Owner 应开始说：**“我们要做一个证据驱动的 evaluation harness，验证在 approved RTS truth projection 上，LLM 是否能为影响分析和测试规划提供可复核、可引用、可评分的增量候选。”

**Owner 必须先证明：**LLM 不是在生成漂亮文本；它能在严格 scope、URI citation、SME goldenset 和安全边界下，减少关键依赖遗漏、提升测试规划质量，并且优于确定性检索/模板与现有 Copilot/Yoda 工作流。

## Reframe the system / 重新定义系统

### 不应如何定义

不要把第一轮定义为：

- broad AI-powered backend platform；
- reusable LLM service for many workflows；
- agent ecosystem foundation；
- monitor / defect / release / historical transaction intelligence layer；
- Copilot replacement。

这些定义会让审批方直接看到权限扩散、数据出域、责任不清和成本失控，而不是看到一个可评估的业务假设。

### 应如何定义

第一轮应定义为 **评估工作台 + 人工决策支持流程**，而不是服务。它的职责是：

1. 接收已脱敏的变更描述、目标 scope、规则/依赖 URI；
2. 调用确定性索引与模板产生 baseline 输出；
3. 在同一输入上调用 LLM 产生受约束候选输出；
4. 与 Copilot/Yoda 输出做并列对照；
5. 由 SME/QA 用统一 rubric 评分；
6. 记录 failure taxonomy 和数据安全证据；
7. 形成是否值得服务化的证据包。

### 推荐名称

**RTS Impact & Test Planning Evidence Harness**  
中文：**RTS 变更影响与测试规划证据评估工作台**

### 一句话 pitch

在已批准的 RTS runtime projection、结构化依赖和脱敏变更材料上，评估 LLM 是否能比确定性检索/模板与 Copilot/Yoda 更好地产生带 URI 引用、可复核的影响面候选和回归测试候选。

### 架构姿态

- **Truth layer**：仍由 canonical packs、review、signoff 决定；LLM 不改变 truth。
- **Index/template layer**：作为第一控制组和默认能力；不是 LLM 的附属品。
- **LLM harness**：只在确定性结果之后生成候选解释、补充风险、open questions。
- **Review workflow**：人类 SME/QA 评分、采纳、驳回；模型输出不直接进入 production workflow。

## What to preserve vs demote / 保留什么，降级什么

| 能力 / 主题 | 建议处理 | 理由与改写方向 |
|---|---|---|
| impact analysis | **保留为首要用例** | 但必须从“AI 分析影响面”改成“生成带 citation 的影响面候选”。输出必须包括 affected object、dependency path、URI、scope、unknowns、confidence，不得作为最终影响结论。 |
| test planning / regression checklist | **保留为第二用例** | 但必须证明不是模板生成。先由 deterministic templates 生成正向/负向/边界/回归基线，再看 LLM 是否能补充非显性组合、依赖路径和覆盖缺口。 |
| historical sample discovery | **降级 / 后置** | 首轮最多允许生成“抽象查询条件/样本需求描述”，禁止访问 raw production payload、trade ID、customer/account 信息。真正样本检索应由既有授权系统完成。 |
| defect triage | **后置为离线回放候选** | 不接 monitor、defect system、alert stream。若未来要做，只能先用历史脱敏案例离线比较 hypothesis ranking、误导率和需要补充证据。 |
| cross-role summaries | **降级为副产品** | 只能基于已通过 SME 复核的分析结果做格式转换，不作为 token 申请理由；不得额外读取敏感上下文。 |
| release / rollback notes | **首轮删除** | 过于接近治理决策。未来最多输出“基于已批准事实的 checklist”，禁止 go/no-go、rollback recommendation、release readiness 结论。 |
| Copilot/Yoda comparison | **提升为必须证据** | 不要把 Copilot/Yoda 描述成无能。把它们作为 baseline comparator：若 RTS+LLM 不能明显优于它们，就不应服务化。 |
| deterministic index/templates | **提升为基础能力和控制组** | Java index layer、scope、URI、L0/L1/L2、dependency hints、模板化 checklist 应先跑起来；LLM 只处理确定性方法覆盖不足的判断/组织/归纳部分。 |

## Approval strategy / 审批策略

审批策略应从“要一个 backend service token”改成“请求通过一组证据门”。不要按预算或日历讲，应按风险、学习价值和责任边界讲。

### Stage 0：边界与假设批准

第一份申请只应请求批准以下内容：

- 两个用例：impact analysis、test planning / regression checklist；
- 一个明确 channel/product/pack scope，且只使用已 sign-off 或已批准 runtime projection；
- 一个人工触发 evaluation harness；
- 一套 goldenset、评分 rubric、failure taxonomy；
- 一套数据分类、脱敏、日志最小化和调用方边界；
- 将 deterministic index/templates、Copilot/Yoda、LLM-assisted 三者放到同一对照评估中。

### Stage 1：无 LLM baseline 建立

在请求持续 LLM 调用之前，先展示 deterministic index/templates 能做到什么：

- scope resolution 是否稳定；
- URI 和 dependency hints 是否足够；
- 模板生成的测试清单覆盖哪些常规场景；
- 仍然缺少哪些非显性依赖、业务组合或 reviewer open questions。

这一步的目的不是否定 LLM，而是防止用 LLM 购买本可由结构化索引解决的问题。

### Stage 2：LLM-assisted 离线评估

只在人工 harness 中对同一 goldenset 运行 LLM 输出，输出不得进入生产流程。审批关注点应是：

- LLM 是否补充 deterministic baseline 漏掉的 material dependencies；
- LLM 是否引入 unsupported assertions；
- LLM 是否遵守 scope 与 URI citation；
- SME/QA 是否采纳其新增内容；
- 与 Copilot/Yoda 相比是否有治理和质量增量。

### Stage 3：人工工作流试用

如果离线评估过线，再考虑在变更评审或测试准备会议中作为“候选材料生成器”使用。仍然不开放给 pipeline、monitor、defect system 或其他 agent 自动调用。

### Stage 4：服务化扩展

只有当评估证据稳定、数据安全证据完整、RACI 明确、输出 schema 固化、回归评估可重复，才讨论服务化 API。服务化不等于自动化；每个 caller 都必须单独批准 scope、数据、quota、owner、kill switch 和输出责任。

### 初始申请中明确不要请求

- 不请求 pipeline / monitor / Chatbot / Copilot extension / MCP / other agents 自动调用；
- 不请求访问 raw historical transactions、defect attachments、monitor alerts、release approval records；
- 不请求 release / rollback risk notes；
- 不请求 defect triage 进入事件响应链路；
- 不请求长期 service key 或 broad AI foundation 名义；
- 不请求把 LLM 输出作为变更评审、测试充分性或发布判断的正式依据。

## Evidence strategy / 证据策略

提交前必须准备一份 evidence pack。没有 evidence pack，就不应进入 token review。

### 1. Baseline / 当前基线

至少收集过去若干个同类变更的结构化记录：

- 人工影响分析产物：列出了哪些 rules/lookups/helpers/target fields；
- 人工测试规划产物：哪些 regression areas、positive/negative/boundary cases；
- 后续发现的漏依赖、补测、返工、UAT/生产前缺陷；
- 重复 SME 澄清点；
- 参与角色和责任边界；
- 哪些信息来自 canonical truth，哪些来自个人经验或聊天。

不要只记录“花了多久”。更重要的是记录：漏掉了什么、为什么漏、谁能判断正确、哪些证据可追溯。

### 2. Goldenset

建立可评分 goldenset：

- 每个案例有明确 scope、change description、changed object、expected impacted objects、critical dependencies、minimum regression areas；
- SME 预先标注 S1/S2/S3 依赖和测试区域；
- goldenset 标签不得进入模型上下文；
- 包含普通变更、lookup/helper 影响、条件分支、跨对象依赖、无影响或 scope 不足以判断的负例；
- 每个标签必须有 URI、artifact 或 reviewer rationale。

### 3. Deterministic baseline

必须先跑 deterministic retrieval/templates：

- 基于 scope、URI、dependency graph、target path、lookup/helper edges 生成 impact candidates；
- 基于规则条件和依赖类型生成测试模板；
- 明确哪些结果来自 graph，哪些来自固定模板；
- 记录 baseline 的 recall、precision、noise、miss types。

### 4. Copilot/Yoda comparison

同一输入、同一允许上下文、同一 scoring rubric，对比：

- Copilot/Yoda 普通使用方式；
- deterministic index/templates；
- RTS+LLM harness。

对比维度不是“谁更会聊天”，而是：是否有 scope enforcement、URI citation、结构化输出、可复现性、可审计性、关键依赖 recall、unsupported assertion、SME 采纳。

### 5. SME scoring

需要至少 BA/SME、developer、QA 三类视角评分。每条模型建议标注：

- accepted；
- partially accepted；
- rejected as wrong；
- rejected as unsupported；
- duplicate of deterministic baseline；
- useful but outside approved scope；
- needs more evidence。

### 6. Failure taxonomy

每次失败必须归类，不能只写“模型不准”：

- critical miss：漏掉关键依赖或必测区域；
- wrong scope：引用了错误 channel/product/pack；
- unsupported assertion：无证据或无 URI 的实质断言；
- hallucinated dependency：编造依赖路径；
- over-broad noise：泛化风险列表过多，降低 reviewer 信噪比；
- unsafe data handling：敏感字段进入 prompt/output/log；
- authority drift：输出像最终结论而不是候选；
- comparator failure：没有优于 deterministic 或 Copilot/Yoda；
- instruction injection / untrusted input failure。

### 7. Data-safety evidence

提交前必须有可检查材料：

- 数据分类表：allowed / forbidden / conditionally allowed；
- 字段黑名单：customer、account、trade ID、message ID、payload raw value、credential、incident detail 等；
- prompt 与日志样例，证明不保存原文敏感上下文；
- DLP 或人工抽查结果；
- scope isolation 测试；
- prompt injection 测试；
- 越权召回测试；
- 模型供应商数据保留与训练使用条款确认；
- log retention、log access owner、delete procedure。

## Evaluation design / 评估设计

### 评估对象

只评估两个任务：

1. **Impact analysis**：从脱敏变更描述和 approved RTS context 生成候选影响面。
2. **Test planning**：从已识别影响面和规则条件生成候选测试场景与回归清单。

### Sample inputs / 样例输入

每个案例建议使用统一结构：

```yaml
case_id: CASE-001
scope:
  channel: Tradition->Stella
  product: FXD_NDF
  pack: approved-pack-uri
change:
  type: modify_target_rule
  changed_objects:
    - uri: rts://.../rules/<rule_id>
  sanitized_description: "修改某 target field 的生成条件；当 lookup 返回缺省值时采用新的 fallback branch。"
  diff_summary: "condition A 新增 branch B；target path 不变；helper H 被调用条件改变。"
allowed_context:
  - approved rules/lookups/helpers
  - dependency edges
  - sanitized examples
forbidden_context:
  - raw production messages
  - customer/account/trade identifiers
  - defect/monitor/release records
```

### Expected outputs / 期望输出

**Impact analysis 输出必须结构化：**

```yaml
answered_scope: ...
cited_facts:
  - uri: ...
    fact: ...
impacted_candidates:
  - object_uri: ...
    impact_type: direct | dependency | regression_related
    dependency_path: [uri1, uri2, uri3]
    rationale: ...
    evidence_uris: [...]
    severity_candidate: S1 | S2 | S3
unknowns:
  - question: ...
    blocking_reason: ...
out_of_scope:
  - ...
no_final_decision_statement: true
```

**Test planning 输出必须结构化：**

```yaml
test_candidates:
  - scenario_id: ...
    linked_impact_uri: ...
    scenario_type: positive | negative | boundary | regression | lookup_default | dependency_chain
    preconditions: ...
    input_pattern: ...
    expected_observation: ...
    evidence_uris: [...]
    unsupported_if_no_evidence: false
coverage_gaps:
  - ...
open_questions:
  - ...
not_a_final_test_plan_statement: true
```

### Scoring rubric / 评分规则

**Impact analysis：100 分**

- Critical dependency recall：40 分。S1 依赖不得漏；S2/S3 按覆盖率计分。
- Precision / noise control：15 分。泛化候选过多、不可执行、重复 baseline 扣分。
- Citation and traceability：15 分。每个 material claim 必须有 URI 或明确标为 hypothesis。
- Scope discipline：10 分。不得跨 channel/product/pack 混用。
- Uncertainty handling：10 分。证据不足时必须提出 open question，而不是补结论。
- Reviewability：10 分。输出结构是否便于 SME 接受/驳回。

**Test planning：100 分**

- Minimum regression area recall：30 分。必须覆盖 SME 标注的必测区域。
- Scenario usefulness：20 分。QA 是否认为可执行、可转化为测试。
- Boundary/negative/dependency coverage：20 分。是否补充模板未覆盖的有价值组合。
- Evidence linkage：10 分。每个场景能否链接到 impact item 或 rule condition。
- Unsupported case rate：10 分。无证据测试建议不得高置信输出。
- Prioritization clarity：10 分。能否区分 must-test、should-test、exploratory。

### Critical miss definition / 关键漏报定义

满足任一条件即为 critical miss：

- 漏掉 SME 标注为 S1 的直接或间接依赖；
- 漏掉会改变 target output、lookup/helper branch、downstream validation 或 mandatory regression area 的对象；
- 漏掉后会让 reviewer 或 QA 形成错误的“测试范围足够”印象；
- 对 scope 不足以判断的案例，没有提出 blocking unknown，反而输出确定结论。

### Unsupported assertion definition / 无依据断言定义

满足任一条件即为 unsupported assertion：

- 对业务规则、依赖、影响或测试必要性作出 material statement，但没有 URI、输入片段或 approved context 支撑；
- 把相似 pack、相似字段或历史经验当成当前 scope 的事实；
- 与 approved truth 冲突；
- 本应标为 hypothesis/open question，却以确定语气输出；
- 用“通常/可能/建议”包装，但实际会影响测试范围或评审结论。

### Reviewer workflow / 评审流程

1. Case owner 准备脱敏输入与 hidden goldenset 标签。
2. 分别生成 deterministic baseline、Copilot/Yoda 输出、RTS+LLM harness 输出。
3. 输出匿名化或至少并列展示，避免 reviewer 被“AI 服务”标签影响。
4. BA/SME、developer、QA 独立评分。
5. Adjudication meeting 只处理分歧，不允许现场改 prompt 后重算当作同一轮成绩。
6. 所有 accepted / rejected / unsupported / critical miss 进入 failure log。
7. Prompt、schema、retrieval 变更后必须对全部 goldenset 做 regression。
8. 只有连续通过同一 rubric，才允许进入人工工作流试用。

### Pass/fail thresholds / 通过门槛

**继续评估门槛：**

- S1 critical miss = 0；
- critical dependency recall ≥ 85%；
- material unsupported assertion ≤ 5%；
- scope violation = 0；
- sensitive data incident = 0；
- SME/QA accepted or partially accepted ≥ 60%；
- 相对 deterministic baseline 有明确增量：新增被采纳的 material impacts 或 test scenarios，而不是只重写同样内容。

**允许进入人工工作流试用门槛：**

- S1 critical miss = 0 且重复评估稳定；
- critical dependency recall ≥ 90%；
- material unsupported assertion ≤ 3%；
- Copilot/Yoda 对照下仍有可解释增量；
- failure taxonomy 中没有未解决的数据安全或 authority drift 问题；
- 输出 schema、citation、unknown handling 固化。

**失败处理：**

- 若 deterministic index/templates 已达到同等或更好质量，停止 LLM 服务化申请，优先产品化 deterministic workflow。
- 若 LLM 增量主要是 cross-role summary 或措辞优化，停止把它作为 backend service 申请理由。
- 若出现 S1 漏报、敏感数据事件或越权召回，退回 sandbox，不进入人工流程。

## System boundary design / 系统边界设计

以下内容可直接贴进 review-team 文档。

### Allowed data / 允许数据

- 已批准 runtime projection：`rules / lookups / helpers`；
- 对应 URI、metadata、scope、snapshot、dependency edges；
- 已脱敏变更描述、diff summary、目标字段/规则 ID；
- 人工构造或脱敏样例；
- goldenset 的输入部分，不含 hidden expected labels；
- deterministic baseline 输出；
- SME 复核后的反馈标签。

### Forbidden data / 禁止数据

- raw production transaction payload；
- customer、account、trade ID、message ID、counterparty、personal data；
- 生产告警原文、defect 附件、incident timeline、release approval record；
- credentials、tokens、system secrets、security control detail；
- 未批准或未 sign-off 的 pack 作为 operational truth；
- chat transcript、个人 memory、meeting note 直接作为 truth；
- evidence/review/reports 原文进入普通 operational LLM context，除非另有 governance-mode 审批。

### Allowed callers / 允许调用方

首轮只允许：

- 具名 pilot reviewer / SME / QA 通过手动 evaluation harness 调用；
- 具名 evaluation admin 运行 batch evaluation；
- 只读服务账号读取 approved projection 和 dependency metadata。

每个 caller 必须记录：owner、purpose、scope、allowed data、allowed output、log access、revocation path。

### Forbidden callers / 禁止调用方

首轮禁止：

- CI/CD pipeline 自动调用；
- monitor / alerting system 自动调用；
- defect tracking system 自动调用；
- Chatbot、Copilot extension、MCP tool、其他 agent 自动调用；
- release management 或 rollback workflow 调用；
- 任何未具名用户或通用 service account。

### Allowed outputs / 允许输出

- 影响面候选；
- 依赖路径候选；
- cited facts；
- uncertainty / open questions；
- test scenario candidates；
- regression checklist candidates；
- coverage gaps；
- “insufficient evidence / ambiguous scope” 结论；
- review notes for human acceptance/rejection。

### Forbidden outputs / 禁止输出

- final business rule；
- final impact conclusion；
- final test plan 或 test sufficiency statement；
- root cause conclusion；
- go/no-go；
- rollback recommendation；
- production operation steps；
- release readiness statement；
- raw or reconstructed sensitive data；
- 跨 scope 的规则合并结论；
- 无 citation 的高置信 material assertion。

### Human decision boundaries / 人工决策边界

- BA/SME 决定业务影响是否成立；
- developer/reviewer 决定依赖解释是否技术成立；
- QA 决定测试计划是否采纳；
- release owner 决定上线与回滚；
- support/ops 决定 incident 排查路径；
- model 输出不得被引用为 approval evidence，只能作为 “candidate generated by evaluation harness”；
- 每条被采纳建议必须有 human owner 和 evidence reference。

## Copilot/Yoda positioning / 与 Copilot/Yoda 的边界

不要用防御性说法攻击 Copilot/Yoda。更稳妥的定位是：Copilot/Yoda 是必须尊重的现有 baseline；RTS+LLM 只有在可复核场景下证明增量，才值得存在。

### Copilot/Yoda 已经足够的场景

- 单个开发者理解局部代码、生成单元测试草稿、解释片段；
- 对 Confluence、文档或普通知识做问答；
- 一次性总结会议材料或文本；
- 没有严格 scope、URI、approved truth、审计和复现要求的个人辅助；
- 仅需重写、翻译、角色化表达，而不需要依赖图与 goldenset 评估。

### RTS+LLM 可能有增量的场景，但必须证明

- 需要强制 scope 到 channel/product/pack；
- 需要每条 material claim 带 URI、snapshot、dependency path；
- 需要把 deterministic index/templates、LLM、SME feedback 放进同一评估闭环；
- 需要多人复用同一结构化输出，而不是个人 prompt；
- 需要记录 failure taxonomy、unsupported assertion、critical miss；
- 需要可撤销 caller、可审计日志、数据边界和人工采纳记录。

### 推荐表述

不要说：**“Copilot/Yoda 不够，所以需要 RTS AI backend service。”**  
改成：**“Copilot/Yoda 是对照组。只有当 RTS evaluation harness 在相同输入和评分标准下，产生更高可追溯质量、更低关键漏报或更好的测试覆盖，并满足数据边界时，才申请服务化。”**

## Messaging rewrite / 申请材料改写建议

### Before / after 表

| 删除或弱化的说法 | 替代表述 |
|---|---|
| “RTS AI Analysis Service 是可复用 LLM backend service。” | “RTS Impact & Test Planning Evidence Harness 是用于验证 LLM 增量价值的人工触发评估工作台。” |
| “可被 pipeline、monitor、Chat/Copilot、内部服务、其他 agent 调用。” | “首轮只允许具名 reviewer/SME/QA 通过手动 harness 调用；任何系统集成需后续单独审批。” |
| “LLM API key 是核心不可替代能力。” | “LLM 的必要性必须通过 deterministic baseline、Copilot/Yoda 对照和 SME goldenset 证明。” |
| “历史交易样本发现。” | “首轮不访问历史交易；最多输出抽象样本查询条件，真实检索由既有授权系统完成。” |
| “缺陷分诊 / monitor alert investigation。” | “缺陷和告警仅作为未来离线回放候选，不进入首轮调用范围。” |
| “release / rollback risk notes。” | “首轮删除；未来最多基于已批准事实生成 checklist，不输出 go/no-go 或 rollback 建议。” |
| “跨角色摘要降低沟通成本。” | “角色化摘要不是申请理由，只能作为已复核分析结果的格式化副产物。” |
| “AI foundation / platform / agent ecosystem。” | “评估两个窄用例的证据工作台；服务化取决于评估结果。” |
| “减少时间、减少遗漏、降低沟通。” | “在 goldenset 上证明 critical dependency recall、unsupported assertion、SME adoption、data safety 和 comparator lift。” |
| “Copilot 不能替代。” | “Copilot/Yoda 是评估对照组；RTS 需证明治理和质量增量。” |
| “AI 输出风险提示。” | “AI 输出候选、unknowns 和 open questions；最终判断由 human owner 作出。” |

### 推荐申请标题

**RTS 变更影响分析与测试规划证据评估 PoC**  
英文：**RTS Impact Analysis and Test Planning Evidence Evaluation PoC**

### 可直接粘贴的 executive paragraph

我们申请开展 **RTS 变更影响分析与测试规划证据评估 PoC**，不是申请广义 AI backend service。该 PoC 只针对两个任务：在已批准 RTS runtime projection、结构化依赖和脱敏变更材料上，生成带 URI 引用的影响面候选与回归测试候选，并由 SME/QA 评审。评估将同时运行 deterministic index/templates、Copilot/Yoda 和 RTS+LLM harness，在同一 goldenset、同一输入和同一评分规则下比较关键依赖覆盖、测试候选有效性、unsupported assertion、scope discipline、人工采纳和数据安全。首轮不接入生产历史交易、监控告警、缺陷系统、release/rollback 流程、pipeline、Chatbot、MCP 或其他 agent；模型输出不作为最终规则、最终影响结论、测试充分性或发布判断，只作为人工复核的候选材料。是否进入服务化，将由评估证据而不是平台愿景决定。

## Owner decision points / owner 需要先决定的事

1. **首轮 scope**：选择哪个已批准 channel/product/pack 子集；是否完全排除未 sign-off 内容。
2. **首轮用例**：是否承诺只做 impact analysis 与 test planning，不把样本发现、缺陷分诊、release/rollback 放进申请。
3. **数据边界**：哪些字段和系统数据绝对禁止进入 prompt、output、log。
4. **调用方边界**：谁是具名 caller；是否同意首轮无 pipeline、monitor、Chat/Copilot、MCP、agent 集成。
5. **deterministic baseline**：由谁负责生成 retrieval/template baseline；baseline 产物长什么样。
6. **Copilot/Yoda 对照方法**：用什么输入、由谁操作、如何保证与 RTS+LLM 公平比较。
7. **goldenset owner**：谁标注 expected impacted objects、critical dependencies、minimum regression areas。
8. **评分阈值**：什么结果允许继续，什么结果必须停止；是否接受“deterministic 足够则不继续 LLM”。
9. **输出权威边界**：哪些词禁止出现在输出中，例如 final、root cause、go/no-go、rollback、approved。
10. **RACI**：谁拥有 prompt、schema、model version、evaluation set、logs、failure review、data-safety approval。
11. **日志策略**：记录哪些元数据，不记录哪些上下文原文，保留多久，谁可访问。
12. **扩展门槛**：未来新增 historical sample、defect triage、release checklist 或 service API 时，各自需要什么额外证据。

## If I were the approval board / 如果我是审批方

### 我会说 yes 的条件

- 申请对象是 evaluation harness / decision-support workflow，不是 broad backend service；
- 首轮只做 impact analysis 与 test planning；
- 已有 goldenset、hidden expected labels、SME scoring rubric；
- deterministic index/templates baseline 已建立；
- Copilot/Yoda 对照计划明确；
- allowed/forbidden data、caller、output 边界可执行；
- 输出 schema 强制 URI citation、unknowns、scope、no-final-decision statement；
- S1 critical miss、unsupported assertion、scope violation、sensitive data incident 都有硬门槛；
- RACI、日志、prompt/schema/model version、failure review owner 清楚；
- owner 明确同意：如果 LLM 不优于 baseline，就不推进服务化。

### 我会说 no 的条件

- 仍然申请“可复用 AI backend service / AI foundation / agent ecosystem”；
- 首轮仍包含 pipeline、monitor、defect、historical transaction raw data、release/rollback、Chatbot、MCP 或 other agents；
- 没有人工 baseline、goldenset、deterministic baseline 或 Copilot/Yoda 对照；
- 用“Copilot 不够”代替证据；
- 数据分类、脱敏、日志边界不清；
- 输出可能被当作 root cause、final impact、test sufficiency、go/no-go 或 rollback 建议；
- 没有 owner 承担 prompt、evaluation、logs、failure review；
- 申请材料仍把 LLM 描述为核心不可替代能力，而不是待验证假设。

### 我只会允许 sandbox evaluation 的条件

- 问题确实存在，但 goldenset 或 baseline 还不成熟；
- 数据可以完全脱敏，且不接生产系统；
- caller 仅限具名人员手动运行；
- 输出只用于离线评分，不进入变更评审或测试计划；
- 目标是判断“LLM 是否值得继续”，不是证明服务已经成立；
- 任一安全、scope 或 critical miss 问题出现，即停止并回到证据准备。

## Final recommendation / 最终建议

Owner 下一步不应重写一份更强营销版 token 申请，而应先准备一份 **《RTS Impact Analysis and Test Planning Evidence Evaluation Charter》**。该文档应包含：首轮 scope、allowed/forbidden data、caller policy、goldenset protocol、deterministic baseline、Copilot/Yoda comparator、LLM output schema、SME scoring rubric、failure taxonomy、data-safety evidence、RACI、以及进入服务化的证据门槛。

同时准备 3 个附件：

1. redacted sample cases；
2. deterministic baseline outputs；
3. review-team 可执行的 system boundary policy。

只有这套 charter 和附件完成后，才值得提交 token/LLM 调用评审。否则，任何“AI backend service”叙事都会继续被视为范围过宽、证据不足和数据风险过高。

Report complete: YES
