# RTS 作为 AI-powered backend service 的严格 Token 审核挑战报告

审阅范围：已检查指定 Markdown 文档、confirmed baseline 文档、`out/` 架构提案，并补充查看了同日发现的 `.hermes/reports/2026-04-27-rts-ai-analysis-service-key-request-brief-simple.md`、`...brief-v2.md`、`out/rts-ai-business-proposal.md`、`out/rts-ai-foundation-mobile.md`。指定 Markdown 文件均存在；PDF 存在但未单独作为依据，因为 Markdown 为主。

## 1. Verdict / 总体裁决

**Conditional approve for narrow PoC**。

当前广义申请口径应被视为不可批准：它把 pipeline、测试、监控、Chat/Copilot、内部服务、其他 agent、历史交易、缺陷、发布/回滚等全部放进一个 token 申请里，范围过宽，且没有证明 LLM 输出相对确定性检索、Copilot/Yoda、人工 SME baseline 的增量价值。

我只可能批准一个窄 PoC：**2-4 周；仅限一个已 sign-off 的 channel/product/pack 子集；仅做两个用例：transformation impact analysis 与 test planning / regression checklist；仅人工触发；仅用已批准 runtime 规则/依赖与脱敏变更材料；禁止接入生产历史交易、监控告警、缺陷系统、release/rollback 决策链和自动化 pipeline；设置硬预算、硬调用上限、硬评估指标，到期自动失效。**

## 2. Core challenge / 核心质疑

RTS AI backend service 最大未解风险是：它把“受治理规则真相服务”的长期架构愿景，包装成一个需要立即获得 LLM API token 的宽泛后端能力，但尚未证明哪些具体高价值工作必须由 LLM 完成、哪些可由确定性索引/现有 Copilot/Yoda/人工流程完成、以及模型错误如何被量化和拦截。若按现有叙事授权，token 很容易从窄 PoC 变成跨系统、跨数据域、跨角色的通用 AI 预算入口，先消耗敏感上下文和成本，再倒逼组织接受其存在。

## 3. Reviewer challenges / 审核挑战清单

### 1. 范围把 PoC 写成平台
- **Challenge**：申请同时覆盖开发、测试、监控、Chat/Copilot、内部服务、其他 agent、缺陷、发布、回滚、历史样本。
- **Why it matters for token approval**：token 授权必须绑定可测场景；跨入口授权会造成不可控调用量、权限扩散和责任不清。
- **What evidence would satisfy me**：只选 1 个业务范围、1-2 个用例、1 个调用入口，并列出排除项。
- **If not satisfied, decision impact**：Reject。

### 2. 没有硬 ROI baseline
- **Challenge**：文档说“减少时间、减少遗漏、降低沟通”，但没有当前平均耗时、案例量、错误成本、SME 工时成本。
- **Why it matters for token approval**：没有 baseline 就无法判断 token 消耗是否值得。
- **What evidence would satisfy me**：过去 10-20 个变更/缺陷的人工耗时、参与角色、返工次数、样本查找耗时、测试准备耗时。
- **If not satisfied, decision impact**：最多允许离线评估，不给服务化 token。

### 3. 未证明 LLM 相对确定性索引的必要性
- **Challenge**：Java index layer 计划已覆盖 scope、URI、L0/L1/L2、dependency hints、trace；申请没有说明哪些结果必须依赖 LLM。
- **Why it matters for token approval**：若检索、依赖图和模板能解决 70% 问题，LLM token 应只用于剩余高不确定环节。
- **What evidence would satisfy me**：A/B 对比：deterministic retrieval/template vs LLM-assisted，在同一 goldenset 上比较耗时、覆盖率、错误率。
- **If not satisfied, decision impact**：Reject 广义服务；只允许无 LLM baseline 先跑。

### 4. 数据边界过于危险
- **Challenge**：历史交易、缺陷、监控、release context 都可能含客户、交易、生产事件、系统脆弱点。
- **Why it matters for token approval**：token 请求实质上是敏感数据出域/入模授权请求。
- **What evidence would satisfy me**：数据分类表、脱敏规则、字段黑名单、日志脱敏、模型数据保留条款、DLP 测试结果。
- **If not satisfied, decision impact**：禁止接入这些数据源。

### 5. 调用方授权模型不成立
- **Challenge**：文档宣称可被 pipeline、monitor、agents 调用，但没有调用方清单、scope、quota、审批人、撤销机制。
- **Why it matters for token approval**：机器到机器调用比人工聊天更容易失控和放大错误。
- **What evidence would satisfy me**：每个 caller 的 owner、purpose、allowed endpoints、rate limit、data scope、kill switch。
- **If not satisfied, decision impact**：只允许人工手动调用，不允许系统集成。

### 6. 输出权威性风险没有被压住
- **Challenge**：impact、triage、release risk notes 很容易被团队当作“系统结论”。
- **Why it matters for token approval**：模型建议若影响测试范围、排查优先级或发布讨论，即使标注“人工复核”也会形成事实权威。
- **What evidence would satisfy me**：输出必须分为 cited facts、inferences、unknowns、human decisions；所有结论必须有 URI/证据或标为 hypothesis。
- **If not satisfied, decision impact**：禁止生产流程使用；只能离线实验。

### 7. 正确性评估缺失
- **Challenge**：指标是方向性描述，没有 recall/precision、unsupported assertion、critical miss、review adoption 的阈值。
- **Why it matters for token approval**：LLM 文字质量不能等同于业务正确性。
- **What evidence would satisfy me**：goldenset、SME 标注、评分 rubric、失败样例、每周评估报告。
- **If not satisfied, decision impact**：不批准连续 token；只批准一次性离线测试额度。

### 8. 影响分析的假阴性风险高
- **Challenge**：系统漏掉关键依赖比多报几个风险更危险。
- **Why it matters for token approval**：token 不能换来“看起来完整”的假安全感。
- **What evidence would satisfy me**：关键依赖 recall ≥85%，Severity-1 依赖遗漏为 0；漏报必须分类复盘。
- **If not satisfied, decision impact**：不得用于变更评审输入。

### 9. 测试规划可能只是模板生成
- **Challenge**：test scenarios、negative cases、regression checklist 可能由固定模板和依赖图生成，不一定需要 LLM。
- **Why it matters for token approval**：token 不应购买可模板化文本。
- **What evidence would satisfy me**：LLM 版本相对模板版本能发现更多有效边界/组合，且 unsupported cases ≤5%。
- **If not satisfied, decision impact**：砍掉 LLM 测试规划，只保留规则模板。

### 10. 历史交易样本发现是高风险数据通道
- **Challenge**：该用例若真正有价值，往往需要访问真实交易 payload 或准生产样本。
- **Why it matters for token approval**：这是最容易发生敏感数据泄漏和越权查询的场景。
- **What evidence would satisfy me**：只输出查询条件，不接触 raw payload；样本搜索由现有授权系统执行；返回数据严格脱敏。
- **If not satisfied, decision impact**：不批准进入首轮 PoC。

### 11. 缺陷分诊/监控告警会放大调用量和误导风险
- **Challenge**：alert burst 会触发大量模型调用；triage 假设可能误导 MTTR。
- **Why it matters for token approval**：监控链路是高压场景，不能让未验证模型进入事件响应节奏。
- **What evidence would satisfy me**：离线回放 20 个历史告警，证明 hypothesis 排名、误导率、成本和延迟可控。
- **If not satisfied, decision impact**：禁止接入 monitor/defect system。

### 12. Release / rollback risk notes 过于接近治理决策
- **Challenge**：发布和回滚属于高责任决策链，AI notes 容易变成审批附件。
- **Why it matters for token approval**：token 授权不应绕过 release governance。
- **What evidence would satisfy me**：仅限从已批准 facts 生成 checklist，不输出 go/no-go、不输出 rollback recommendation。
- **If not satisfied, decision impact**：从申请中删除。

### 13. 与 Copilot/Yoda/检索工具的重叠没有被证明
- **Challenge**：文档反复说 Copilot/Yoda 不够，但缺少对照实验。
- **Why it matters for token approval**：组织已有 AI 工具时，新 token 必须证明非重复建设。
- **What evidence would satisfy me**：同一任务由 Copilot/Yoda/普通搜索/RTS LLM 服务完成的时间、质量、治理差异对比。
- **If not satisfied, decision impact**：不批准新后端 token。

### 14. 成本模型缺失
- **Challenge**：没有每类请求的 token 估算、日/月请求量、最大上下文、重试策略、缓存策略、预算上限。
- **Why it matters for token approval**：backend service 会把个体 prompt 成本变成组织级自动成本。
- **What evidence would satisfy me**：每用例 cost envelope、daily cap、total PoC cap、per-caller quota、超限停机规则。
- **If not satisfied, decision impact**：不发 token。

### 15. 审计日志本身可能成为敏感数据湖
- **Challenge**：为了审计记录 prompt、context、output、trace，会复制敏感规则、交易和缺陷信息。
- **Why it matters for token approval**：治理 AI 调用不能制造另一个未治理数据存储。
- **What evidence would satisfy me**：日志字段最小化、脱敏、保留期、访问审批、删除机制、审计审计本身的 owner。
- **If not satisfied, decision impact**：禁止记录原文上下文；服务不可上线。

### 16. 支持与事故责任不清
- **Challenge**：谁维护 prompt、schema、模型版本、评估集、告警、误报复盘、用户支持，没有定义。
- **Why it matters for token approval**：API key 不是一次性实验物；服务化后需要 owner。
- **What evidence would satisfy me**：RACI、on-call/office-hour 支持模式、prompt change approval、rollback procedure。
- **If not satisfied, decision impact**：只给临时离线 key，不给 service key。

### 17. 模型依赖与版本漂移未管控
- **Challenge**：模型版本、供应商策略、输出风格、token 价格都可能变化。
- **Why it matters for token approval**：没有 pinned evaluation 和回归测试，服务质量会悄悄漂移。
- **What evidence would satisfy me**：模型版本记录、prompt/output versioning、变更前后 goldenset regression、fallback mode。
- **If not satisfied, decision impact**：禁止接入生产工作流。

### 18. Prompt injection 与跨 scope 污染被低估
- **Challenge**：报文片段、缺陷描述、历史样本都可能携带恶意或误导文本；相似规则也可能跨 product/channel 混淆。
- **Why it matters for token approval**：token 服务会把不可信输入送进高权限上下文组装器。
- **What evidence would satisfy me**：prompt injection test、scope isolation test、越权召回测试、红队样例。
- **If not satisfied, decision impact**：禁止多数据源上下文拼接。

## 4. Use-case validity stress test / 用例有效性压力测试

| 用例 | 价值质疑 | 可行性质疑 | 数据依赖 | 可衡量结果 | 只是 prompt/demo 的风险 |
|---|---|---|---|---|---|
| transformation impact analysis | 价值取决于是否能少漏关键依赖，而不是写出漂亮影响面。 | 若依赖图不完整，LLM 只能猜；若依赖图完整，确定性算法可能已足够。 | 已批准规则、依赖、变更描述；不应需要生产交易。 | 关键依赖 recall ≥85%，Severity-1 miss = 0，人工耗时下降 ≥30%。 | 高：很容易生成“可能影响 A/B/C”的泛化清单。 |
| test planning / regression checklist | 价值在于发现非直观组合与边界，不是生成通用测试模板。 | 规则条件结构化后可模板化，LLM 增量需证明。 | 影响面、规则条件、历史缺陷类别；首轮不应接 raw payload。 | 被 QA 采纳/部分采纳 ≥60%，unsupported test case ≤5%，准备时间下降 ≥30%。 | 中高：常见正向/反向/边界用例很容易套话。 |
| historical transaction sample discovery | 价值可能大，但数据风险最大。 | 不接真实交易时价值下降；接真实交易时审批复杂。 | 交易样本、字段、节点、客户/产品信息，敏感度高。 | 找样本时间下降 ≥40%，且 0 敏感字段出模/入日志。 | 高：如果只输出搜索思路，可能只是“请查字段 X/Y”的提示。 |
| defect triage / monitor alert investigation | 价值在 MTTR，但误导路径会造成更大损失。 | 需要近期变更、告警、缺陷、报文片段、依赖，集成复杂。 | 生产事件、缺陷单、监控信息、报文片段。 | 离线历史告警 top-3 hypothesis 命中率、误导率、成本/延迟。 | 高：容易产出“可能原因列表”，无法证明实际缩短 MTTR。 |
| cross-role summaries | 主要是沟通包装，不是核心 token 理由。 | 技术上容易，但价值弱且难量化。 | 依赖已有分析结果，不应额外读取敏感数据。 | 重复 SME 问询次数下降，但归因困难。 | 很高：最像文本生成 demo。 |
| release / rollback risk notes | 高风险决策附件，审批敏感。 | 需要测试覆盖、缺陷状态、业务影响、回滚方案，责任边界复杂。 | release context、测试结果、已知问题、依赖。 | 只能衡量 checklist 完整性，不能让 AI 影响 go/no-go。 | 高：容易把泛化风险话术伪装成治理输入。 |

## 5. Token governance objections / Token 治理反对意见

- **Scope creep**：现有叙事从两个 PoC 用例扩散到“AI foundation / backend capability / agent ecosystem”。token 申请必须砍回单一范围。
- **Cost control**：没有调用量、上下文长度、重试、缓存、月预算。后端化后成本会从“人问一次”变成“系统自动多次”。
- **Context exfiltration**：context assembly 会把规则、交易、缺陷、监控、release 信息拼成高价值上下文包；这是泄露面扩大。
- **Sensitive data**：历史交易和缺陷/告警可能包含客户、交易、账户、系统故障和内部控制信息；默认不得进入模型。
- **Hallucinated authority**：服务化输出带有系统背书，用户会降低怀疑程度；“human review”不是充分控制。
- **Duplicate Copilot/Yoda capability**：没有对照实验前，不能接受“Copilot 不够”作为 token 理由。
- **Audit burden**：要审计模型调用就要保存输入输出；保存越多，敏感数据湖越大；保存越少，又无法追责。
- **Support ownership**：谁处理错误输出、用户申诉、prompt 更新、模型升级、预算告警，文档未回答。
- **Model dependency**：模型质量、价格、上下文窗口、供应商策略变化会直接影响服务；必须有回归和 fallback。
- **Evaluation difficulty**：impact/test/triage 没有唯一标准答案；需要 SME goldenset，否则会用主观满意度替代质量。
- **Permission transitivity**：一个调用者若能请求服务汇总多个系统上下文，可能间接获得自己原本无权查看的信息。
- **Operational blast radius**：monitor/pipeline 一旦接入，错误输出和费用都会自动放大。

## 6. Minimum approval bar / 最低批准门槛

批准 token 前必须满足以下条件：

1. **PoC 范围**：最多 3 周；最多 12 个离线案例；只覆盖一个已 sign-off 的 channel/product/pack 子集；只做 impact analysis 与 test planning/regression checklist。
2. **Allowed data**：仅使用 approved runtime projection 中的 rules/lookups/helpers、结构化依赖、脱敏变更描述、脱敏 expected outcome。禁止 raw production transaction payload、客户标识、账户/交易 ID、生产告警原文、缺陷附件、release approval 记录。
3. **Allowed callers**：仅 3-5 名具名 pilot 用户通过一个手动 test harness 调用；不允许 pipeline、monitor、Chatbot、Copilot extension、MCP、其他 agent 或内部系统自动调用。
4. **Allowed outputs**：只允许输出影响面候选、依赖 citation、测试场景候选、uncertainty/open questions。禁止输出最终业务规则、root cause 结论、go/no-go、rollback recommendation、生产操作步骤。
5. **Goldenset**：至少 10 个历史变更/分析案例，由 SME 预先标注 expected impacted objects、critical dependencies、minimum regression areas。
6. **Quality metrics**：critical dependency recall ≥85%；Severity-1 dependency miss = 0；material unsupported assertion ≤5%；QA/SME 采纳或部分采纳 ≥60%；人工准备时间相对 baseline 下降 ≥30%。
7. **Cost metrics**：PoC 总预算上限 USD 300 或组织等价预算；最多 100 次 API calls；单案例最多 8 次模型调用；超过即自动停机。
8. **Token limits**：单次请求上下文不得超过 30k tokens；输出不得超过 2.5k tokens；禁止把完整 pack corpus 一次性塞入上下文。
9. **Security metrics**：0 起敏感字段进入模型或日志；0 起跨 scope 返回；prompt injection 测试必须通过。
10. **Governance**：必须有 owner、审批人、评估人、日志访问人；prompt/schema/model 版本变更必须记录。
11. **Exit rule**：任一硬指标失败，PoC 不得续期或扩容；只能回到离线分析。

## 7. What I would cut / 我会砍掉什么

- 砍掉“可被 pipeline、monitor、Chat/Copilot、内部服务、其他 agent 全部调用”的首轮表述。
- 砍掉 historical transaction sample discovery，除非只输出抽象查询条件且完全不接 raw transaction。
- 砍掉 defect triage / monitor alert investigation，直到离线回放证明 top hypothesis 有效且不会误导。
- 砍掉 release / rollback risk notes；这不应出现在第一张 token 申请里。
- 砍掉 cross-role summaries 作为审批理由；它是副产物，不是 token justification。
- 砍掉 “AI foundation / platform / client flow enablement / institutional knowledge recovery” 等扩张性包装。
- 砍掉 API/MCP/agent ecosystem 集成承诺；首轮只允许手动 harness。
- 砍掉“LLM API key 是核心不可替代能力”的绝对说法；必须先用对照实验证明。
- 砍掉任何暗示 AI 可形成 root cause、release readiness、rollback decision 的 wording。
- 砍掉对长期架构层次的审批叙事；token 审核只关心用例、数据、成本、风险、证据。

## 8. Narrow PoC I might approve / 我可能批准的窄 PoC

**周期**：3 周，key 到期自动失效。

**范围**：一个已 review/sign-off 的 `Tradition -> Stella` 子范围，例如已批准 COMMON 或 FXD_NDF pack 子集；不得使用未 review 的 FXO 内容。

**用例**：
1. transformation impact analysis：输入脱敏变更描述 + 目标字段/规则范围，输出影响面候选、依赖路径 citation、不确定点。
2. test planning / regression checklist：基于上述影响面输出测试场景候选、边界/负向/回归关注点。

**数据限制**：approved runtime rules/lookups/helpers + dependency metadata + 脱敏历史变更描述。无 raw production messages，无客户/账户/交易 ID，无 monitor/defect/release 系统接入。

**调用限制**：手动 test harness；3-5 名具名用户；无自动 pipeline；无 external agent；无 chat surface 对外开放。

**成功指标**：
- 10 个 goldenset 案例；
- 人工影响分析/测试准备耗时下降 ≥30%；
- critical dependency recall ≥85%；
- Severity-1 miss = 0；
- unsupported material assertion ≤5%；
- SME/QA 采纳或部分采纳 ≥60%；
- PoC 总成本 ≤USD 300；
- 0 敏感数据泄露、0 越权 scope 返回。

**Stop conditions**：出现敏感数据进入模型/日志、一次 Severity-1 漏报、两次以上 unsupported 高置信结论、成本超过 80% 但未完成半数案例、或第 2 周结束仍无法建立 goldenset，即停止。

## 9. Questions for the owner / 给 owner 的尖锐问题

1. 你要批准的是一个 3 周离线 PoC，还是一个可被多个系统调用的长期 backend service？只能选一个。
2. 过去 10 个类似变更中，人工影响分析、测试准备、样本查找分别花了多少小时？谁记录的？
3. 哪些结论必须由 LLM 生成，哪些可由确定性索引、依赖图、模板或 Copilot/Yoda 完成？
4. 如果模型漏掉一个关键依赖导致测试范围不足，责任由谁承担？如何发现？
5. 你愿意在首轮完全不接生产历史交易、缺陷系统和监控告警吗？如果不愿意，为什么？
6. 哪些数据字段绝对不能进入模型上下文、输出和日志？是否已有脱敏规则？
7. 谁拥有 API key、预算、prompt、schema、评估集、日志和事故复盘？
8. 成功标准是“用户觉得有帮助”，还是有可审计的 recall、precision、time saving、cost 指标？
9. 如果 PoC 发现 deterministic retrieval + templates 已经足够，你是否同意不继续申请 LLM 服务化？
10. 你希望 reviewer 批准的是业务价值，还是批准一个尚未证明边界的 AI platform 入口？

## 10. Report complete

Report complete: YES
