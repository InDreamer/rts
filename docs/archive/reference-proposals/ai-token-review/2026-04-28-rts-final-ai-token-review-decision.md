# RTS AI/LLM Token 最终审核文档

**文档目的**：基于 RTS AI Analysis Service 原始申请材料、严格 Token 审核报告、以及后续深入建议，形成一份可提交/可内部决策的最终审核结论与改写方向。

**审核对象**：RTS 是否应以 AI-powered backend service 名义申请 LLM API/token 使用权限。

**最终结论**：不建议按原始 “RTS AI Analysis Service / broad AI backend service” 口径提交；建议改为 **RTS 变更影响分析与测试规划证据评估 PoC**，先通过 evidence harness 证明 LLM 增量价值，再决定是否进入服务化。

---

## 1. 最终裁决

```yaml
verdict: Conditional approve for evidence evaluation only
not_approved_as: Broad RTS AI Backend Service
approved_direction: RTS Impact Analysis and Test Planning Evidence Evaluation PoC
recommended_name_zh: RTS 变更影响分析与测试规划证据评估 PoC
recommended_name_en: RTS Impact Analysis and Test Planning Evidence Evaluation PoC
first_round_form: Evaluation harness / decision-support workflow
not_first_round_form: Multi-caller backend AI service
```

### 审核意见

当前 RTS 方向有真实业务问题基础：报文转换规则复杂、依赖深、测试规划难、人工影响分析容易遗漏。但原始申请把过多能力一次性纳入同一个 LLM token 请求：pipeline、monitor、Chat/Copilot、内部服务、其他 agent、历史交易样本、缺陷分诊、release/rollback notes 等。这会让审批方看到的是权限扩散、数据风险、责任不清和服务化过早，而不是一个可验证的业务假设。

因此，最终审核意见是：

> **可以申请一个受控的 evidence evaluation PoC；不应申请一个广义 AI backend service。**

---

## 2. 核心审核判断

RTS 的第一轮申请必须从“建设 AI 服务”转为“验证 AI 是否值得服务化”。

原始叙事：

> RTS AI Analysis Service 是一个可复用 LLM-backed backend service，可服务 transformation impact analysis、test planning、sample discovery、defect triage、release risk、cross-role summaries，并可被 pipeline、test workflow、monitoring、Chat/Copilot、internal services 和 agents 调用。

最终建议叙事：

> 我们申请开展 **RTS 变更影响分析与测试规划证据评估 PoC**，不是申请广义 AI backend service。该 PoC 只针对两个任务：在已批准 RTS runtime projection、结构化依赖和脱敏变更材料上，生成带 URI 引用的影响面候选与回归测试候选，并由 SME/QA 评审。评估将同时运行 deterministic index/templates、Copilot/Yoda 和 RTS+LLM harness，在同一 goldenset、同一输入和同一评分规则下比较关键依赖覆盖、测试候选有效性、unsupported assertion、scope discipline、人工采纳和数据安全。是否进入服务化，将由评估证据而不是平台愿景决定。

---

## 3. 审核方主要 concern

### 3.1 范围过宽

原始材料同时请求或暗示多个消费入口和多个数据域：

- pipelines
- test workflows
- monitoring / alerts
- Chat/Copilot surfaces
- internal services
- other agents
- historical transactions
- defect context
- release / rollback context

这不是 PoC，而是平台级授权。第一轮不应批准。

### 3.2 LLM 必要性未证明

RTS 已规划 Java index layer、scope resolver、URI、dependency hints、L0/L1/L2 views、traceable retrieval。审批方会问：

> 如果 deterministic index + templates 已经能解决大部分影响分析和测试规划，为什么还需要 LLM token？

因此 LLM 必须被当作待验证假设，而不是默认必要能力。

### 3.3 与 Copilot/Yoda 边界不足

不能简单说 “Copilot/Yoda 不够”。这会被认为是防御性叙事。

更稳妥的说法是：

> Copilot/Yoda 是对照组。只有当 RTS evaluation harness 在相同输入和评分标准下，产生更高可追溯质量、更低关键漏报或更好的测试覆盖，并满足数据边界时，才申请服务化。

### 3.4 数据和权限风险高

以下内容不应进入首轮：

- raw production transaction payload
- customer/account/trade/message identifiers
- monitor alert 原文
- defect attachments
- incident timeline
- release approval records
- unapproved packs as operational truth
- chat transcripts / personal memory as truth

这些一旦进入 prompt/output/log，会把 LLM 申请变成敏感数据出域/复制/治理问题。

### 3.5 输出权威性风险

AI 输出一旦变成系统生成材料，就可能被误认为“系统结论”。因此输出只能是候选：

- impact candidates
- dependency path candidates
- test scenario candidates
- coverage gaps
- unknowns / open questions

禁止输出：

- final impact conclusion
- final business rule
- final test plan
- test sufficiency statement
- root cause conclusion
- go/no-go
- rollback recommendation
- release readiness statement

---

## 4. 最终批准范围

### 4.1 可批准范围

```yaml
approved_first_round:
  form: manual evaluation harness
  purpose: prove_or_disprove_LLM_incremental_value
  use_cases:
    - transformation impact analysis
    - test planning / regression checklist
  data:
    - approved runtime projection: rules/lookups/helpers
    - URI / metadata / scope / snapshot
    - dependency edges
    - sanitized change descriptions
    - sanitized diff summaries
    - target field / rule IDs
    - deterministic baseline outputs
    - SME feedback labels
  callers:
    - named pilot SME/reviewer/QA users
    - evaluation admin for batch evaluation
  output_status: candidate_only
```

### 4.2 不批准范围

```yaml
not_approved_first_round:
  integrations:
    - CI/CD pipeline automatic calls
    - monitoring / alerting automatic calls
    - defect tracking integration
    - Chatbot / Copilot extension
    - MCP tools
    - other agents
    - internal service automatic calls
  data:
    - raw production messages
    - historical transaction payloads
    - customer/account/trade/message IDs
    - production alerts
    - defect attachments
    - release approval records
  outputs:
    - final rules
    - root cause conclusions
    - release readiness
    - rollback recommendations
    - production operation steps
```

---

## 5. 用例最终处理意见

| 用例 | 最终意见 | 审核说明 |
|---|---|---|
| Transformation impact analysis | **保留为首要用例** | 只生成带 URI citation 的影响面候选，不给最终影响结论。 |
| Test planning / regression checklist | **保留为第二用例** | 必须证明 LLM 能补充 deterministic templates 未覆盖的有效边界/组合。 |
| Historical transaction sample discovery | **后置** | 首轮最多生成抽象查询条件，不接触 raw transaction。 |
| Defect triage / monitor alert investigation | **后置为离线回放候选** | 不进入事件响应链路；未来需历史脱敏案例证明 hypothesis ranking 有效。 |
| Cross-role summaries | **降级为副产品** | 只能基于已复核分析结果做表达转换，不作为 token 申请理由。 |
| Release / rollback risk notes | **首轮删除** | 过于接近治理决策；未来最多生成基于已批准 facts 的 checklist。 |
| Copilot/Yoda comparison | **提升为必需证据** | 作为 baseline comparator，不作为被贬低对象。 |
| Deterministic index/templates | **提升为控制组和默认能力** | 先证明不用 LLM 能做到什么，再证明 LLM 增量。 |

---

## 6. 证据包要求

提交 token/LLM 评审前，应先准备 **Evidence Pack**。

### 6.1 Baseline evidence

至少收集若干历史/模拟变更案例：

- 人工影响分析产物
- 人工测试规划产物
- 漏依赖、补测、返工、UAT/生产前缺陷
- 重复 SME 澄清点
- 哪些信息来自 canonical truth，哪些来自个人经验
- 哪些结论后来被证明正确/错误/不足

### 6.2 Goldenset

每个案例应包含：

```yaml
case:
  scope:
    channel: ...
    product: ...
    pack: approved_pack_uri
  change_description: sanitized_text
  changed_objects:
    - uri: ...
  expected_impacted_objects: hidden_from_model
  critical_dependencies: hidden_from_model
  minimum_regression_areas: hidden_from_model
  reviewer_rationale: stored_for_scoring
```

Goldenset 标签不得进入模型上下文。

### 6.3 Deterministic baseline

先跑 deterministic index/templates：

- dependency graph impact candidates
- URI/path-based trace
- template-generated positive/negative/boundary/regression checklist
- baseline recall / precision / miss types

### 6.4 Copilot/Yoda comparator

同一输入、同一允许上下文、同一评分规则，对比：

- Copilot/Yoda 普通使用方式
- deterministic index/templates
- RTS+LLM harness

比较维度：

- critical dependency recall
- unsupported assertion
- citation / traceability
- scope discipline
- SME/QA adoption
- output reviewability

### 6.5 Failure taxonomy

所有失败必须归类：

- critical miss
- wrong scope
- unsupported assertion
- hallucinated dependency
- over-broad noise
- unsafe data handling
- authority drift
- comparator failure
- prompt injection / untrusted input failure

---

## 7. 评估设计

### 7.1 Impact analysis 输出 schema

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

### 7.2 Test planning 输出 schema

```yaml
test_candidates:
  - scenario_id: ...
    linked_impact_uri: ...
    scenario_type: positive | negative | boundary | regression | lookup_default | dependency_chain
    preconditions: ...
    input_pattern: ...
    expected_observation: ...
    evidence_uris: [...]
coverage_gaps:
  - ...
open_questions:
  - ...
not_a_final_test_plan_statement: true
```

### 7.3 评分规则

Impact analysis：

```yaml
impact_analysis_score:
  critical_dependency_recall: 40
  precision_noise_control: 15
  citation_traceability: 15
  scope_discipline: 10
  uncertainty_handling: 10
  reviewability: 10
```

Test planning：

```yaml
test_planning_score:
  minimum_regression_area_recall: 30
  scenario_usefulness: 20
  boundary_negative_dependency_coverage: 20
  evidence_linkage: 10
  unsupported_case_rate: 10
  prioritization_clarity: 10
```

### 7.4 通过门槛

```yaml
pass_thresholds:
  S1_critical_miss: 0
  critical_dependency_recall: ">= 85%"
  material_unsupported_assertion: "<= 5%"
  scope_violation: 0
  sensitive_data_incident: 0
  SME_QA_accepted_or_partially_accepted: ">= 60%"
  comparator_lift: must_show_clear_increment_over_deterministic_baseline
```

若 deterministic index/templates 已达到同等或更好质量，应停止 LLM 服务化申请，优先产品化 deterministic workflow。

---

## 8. 系统边界政策

### 8.1 Allowed data

```yaml
allowed_data:
  - approved runtime projection: rules/lookups/helpers
  - URI / metadata / scope / snapshot / dependency edges
  - sanitized change description
  - sanitized diff summary
  - target field / rule ID
  - synthetic or sanitized examples
  - deterministic baseline output
  - SME reviewed feedback labels
```

### 8.2 Forbidden data

```yaml
forbidden_data:
  - raw production transaction payload
  - customer / account / trade ID / message ID / counterparty / personal data
  - production alert text
  - defect attachments
  - incident timeline
  - release approval record
  - credentials / tokens / system secrets
  - unapproved packs as operational truth
  - chat transcript / personal memory / meeting note as truth
```

### 8.3 Allowed callers

```yaml
allowed_callers:
  - named pilot reviewer
  - named SME
  - named QA reviewer
  - evaluation admin
  - read-only service account for approved projection and dependency metadata
```

### 8.4 Forbidden callers

```yaml
forbidden_callers:
  - CI/CD pipeline
  - monitor / alerting system
  - defect tracking system
  - Chatbot
  - Copilot extension
  - MCP tool
  - other agent
  - release / rollback workflow
  - generic service account
```

### 8.5 Human decision boundaries

```yaml
human_decisions:
  BA_SME: decide_business_impact_validity
  developer_reviewer: decide_technical_dependency_validity
  QA: decide_test_plan_adoption
  release_owner: decide_release_or_rollback
  support_ops: decide_incident_investigation_path
```

LLM 输出不得作为 approval evidence，只能作为：

```text
candidate generated by evaluation harness
```

---

## 9. 申请材料改写建议

| 原表述 | 替代表述 |
|---|---|
| RTS AI Analysis Service 是可复用 LLM backend service | RTS Impact & Test Planning Evidence Harness 是用于验证 LLM 增量价值的人工触发评估工作台 |
| 可被 pipeline、monitor、Chat/Copilot、内部服务、其他 agent 调用 | 首轮只允许具名 reviewer/SME/QA 通过手动 harness 调用；系统集成需后续单独审批 |
| LLM API key 是核心不可替代能力 | LLM 的必要性必须通过 deterministic baseline、Copilot/Yoda 对照和 SME goldenset 证明 |
| 历史交易样本发现 | 首轮不访问历史交易；最多输出抽象样本查询条件 |
| 缺陷分诊 / monitor alert investigation | 缺陷和告警仅作为未来离线回放候选 |
| Release / rollback risk notes | 首轮删除；未来最多生成基于已批准事实的 checklist |
| 跨角色摘要降低沟通成本 | 角色化摘要不是申请理由，只能作为已复核分析结果的格式化副产物 |
| AI foundation / platform / agent ecosystem | 评估两个窄用例的证据工作台；服务化取决于评估结果 |
| Copilot 不能替代 | Copilot/Yoda 是评估对照组；RTS 需证明治理和质量增量 |
| AI 输出风险提示 | AI 输出候选、unknowns 和 open questions；最终判断由 human owner 作出 |

---

## 10. 推荐提交标题与摘要

### 推荐标题

```text
RTS 变更影响分析与测试规划证据评估 PoC
```

英文：

```text
RTS Impact Analysis and Test Planning Evidence Evaluation PoC
```

### 推荐摘要

> 我们申请开展 **RTS 变更影响分析与测试规划证据评估 PoC**，不是申请广义 AI backend service。该 PoC 只针对两个任务：在已批准 RTS runtime projection、结构化依赖和脱敏变更材料上，生成带 URI 引用的影响面候选与回归测试候选，并由 SME/QA 评审。评估将同时运行 deterministic index/templates、Copilot/Yoda 和 RTS+LLM harness，在同一 goldenset、同一输入和同一评分规则下比较关键依赖覆盖、测试候选有效性、unsupported assertion、scope discipline、人工采纳和数据安全。首轮不接入生产历史交易、监控告警、缺陷系统、release/rollback 流程、pipeline、Chatbot、MCP 或其他 agent；模型输出不作为最终规则、最终影响结论、测试充分性或发布判断，只作为人工复核的候选材料。是否进入服务化，将由评估证据而不是平台愿景决定。

---

## 11. Owner 决策清单

提交前，owner 需要明确决定：

1. 首轮 scope 选择哪个已批准 channel/product/pack 子集。
2. 是否承诺首轮只做 impact analysis 与 test planning。
3. 哪些字段和系统数据绝对禁止进入 prompt/output/log。
4. 是否同意首轮无 pipeline、monitor、Chat/Copilot、MCP、agent 集成。
5. deterministic baseline 由谁负责生成。
6. Copilot/Yoda 如何公平对照。
7. goldenset 由谁标注。
8. 什么结果允许继续，什么结果必须停止。
9. 是否接受 deterministic 足够则不继续 LLM。
10. 哪些词禁止出现在输出中，例如 final、root cause、go/no-go、rollback、approved。
11. 谁拥有 prompt、schema、model version、evaluation set、logs、failure review。
12. 未来新增 sample discovery、defect triage、release checklist、service API 各自需要什么额外证据。

---

## 12. 最终建议

下一步不要准备“更好看的 RTS AI Backend Service 申请材料”。应先准备：

```text
RTS Impact Analysis and Test Planning Evidence Evaluation Charter
```

Charter 应包含：

- 首轮 scope
- allowed / forbidden data
- caller policy
- goldenset protocol
- deterministic baseline
- Copilot/Yoda comparator
- LLM output schema
- SME scoring rubric
- failure taxonomy
- data-safety evidence
- RACI
- 服务化扩展证据门槛

同时准备 3 个附件：

1. redacted sample cases
2. deterministic baseline outputs
3. executable system boundary policy

只有这套 charter 和附件完成后，才值得提交 token/LLM 调用评审。否则，任何 “AI backend service” 叙事都会继续被视为范围过宽、证据不足和数据风险过高。

---

## 13. Final decision statement

```yaml
final_decision:
  approve_broad_ai_backend_service: false
  approve_evidence_evaluation_poc: true
  required_next_artifact: RTS Impact Analysis and Test Planning Evidence Evaluation Charter
  service_expansion_condition: evidence_passes_goldenset_comparator_and_boundary_review
```

**最终审核结论**：RTS 的 AI 方向应从“申请服务化 token”降级并重塑为“证明 LLM 增量价值的证据评估 PoC”。通过评估前，不应承诺 backend service、多入口集成、生产数据接入或自动化工作流调用。
