# 完整项目交接提示词 — Transformation Rule System

- 生成日期：2026-04-15
- 用途：在一台没有任何背景的新主机上，让 AI Agent 完全理解项目并继续剩余工作。

---

## 一、项目背景

### 1.1 你是谁

你是一个 AI Agent，负责维护和推进一个名为 **Transformation Rule System** 的项目。这个项目属于 Standard Chartered Bank 的 VPA (Virtual Processing Agent) 体系，服务于金融交易系统间的报文转换（transformation）场景。

### 1.2 这个项目是什么

这是一套面向 `source_system -> target_system` 转换场景的 **规则真相源和知识库（Rule Truth Source）**。它不是运行时引擎，也不是 Java/XSLT 代码的镜像。它的目标是：

- **用 YAML 定义规则真相源**（canonical rule）
- **用 Git 做版本控制与追溯**
- **用 PostgreSQL 做规则查询与索引**（规划中）
- **让 AI 可检索、人工审核、治理可追溯**

当前以 **Tradition -> Stella** 作为第一批验证样本。Tradition 是源系统（发送 FpML 交易报文），Stella 是目标系统（接收 SCBM/FpML 格式报文）。

### 1.3 核心思想

1. **规则最小抽象层**：所有规则都围绕 `source + logic + target` 这个核心表达。source 回答从哪里取数，logic 回答如何转换，target 回答写到哪里。
2. **规则对象和实现边界分离**：YAML 规则文件是规则对象；Java/XSLT/Excel 是实现证据（evidence），不混入主对象。
3. **AI 可查询优先**：规则对象要天然支持 AI 问“某个 target 字段的值从哪里来”“某个节点为什么这么生成”“某个 lookup 等”问题。
4. **渐进式分层管理**：规则库不平铺展开，而是分三层架构 —— 先按 source-target 系统、再按产品、再按 fragment/业务片段，逐步缩小范围。

### 1.4 业务领域

- **产品范围**：FXD（外汇远期，含 NDF 无本金交割远期）、FXO（外汇期权，含 NDO 无本金交割期权）、COMMON（共用的 header/party/portfolio 规则）
- **源系统报文格式**：FpML XML
- **目标系统报文格式**：SCBM/FpML XML（Standard Chartered 自有格式扩展）
- **现行事实来源**：Java extractor + XSLT template（不在本项目中，但作为 evidence 材料）
- **映射表**：Excel workbook `Tradition_SCBM_mapping.xlsx`（9 sheets） + 数据库静态映射表

---

## 二、项目架构

### 2.1 分层架构（四层分离）

每个 Rule Pack 必须按以下四层组织：

| 层 | 目录 | 职责 |
|---|---|---|
| **主对象层** | `rules/` `lookups/` `helpers/` | 规则是什么（source + logic + target），AI 直接使用 |
| **Evidence 层** | `evidence/evidence-index.yaml` | 规则依据是什么，追溯到 Java/XSLT/Excel |
| **Review 层** | `review/review-index.yaml` | 未决点、风险、审核结论 |
| **Report 层** | `reports/*.md` | 阶段汇总、交接沟通 |

### 2.2 三种主对象

| 对象类型 | 文件名格式 | 作用 |
|---|---|---|
| `target_rule` | `rules/rule_*.yaml` | 表达目标字段或 block 的生成逻辑（核心） |
| `lookup_definition` | `lookups/lk_*.yaml` | 表达查表逻辑（映射时 lookup 或静态查询封装） |
| `helper_definition` | `helpers/hlp_*.yaml` | 表达复用的稳定中间概念（仅在高度复用或稳定业务概念时建立） |

### 2.3 主对象只保留什么

- `id`、`status`、`signoff_status`
- `source`（输入及 XPath）
- `logic`（summary + pipeline）
- `target/output`（目标 XPath 或输出字段）
- `dependencies`（依赖的 lookups/helpers/rules）
- `8 型 example`（结果向示例：input + key_decision + result）

**不放入主对象的**：evidence、review、ambiguities、approval、长 trace

### 2.4 Pack 目录结构模板

```text
<pack-root>/
  metadata.yaml             # pack 元信息：scope、对象计数、材料清单
  README.md                 # pack 概述与对象清单
  ops-reference.md          # 仅在 pipeline op 需要解释时保留
  rules/
    rule_*.yaml
  lookups/
    lk_*.yaml
  helpers/
    hlp_*.yaml
  evidence/
    evidence-index.yaml     # pack 级横切证据索引
  review/
    review-index.yaml       # pack 级横切审核索引
  reports/
    extraction-report.md    # authoring 阶段产出
    closure-check.md        # review 阶段产出
    review-checklist.md     # review 阶段产出
```

---

## 三、工作流系统

### 3.1 双主 Skill

项目应该两个核心 Skill 运转：

| Skill | 路径 | 职责 |
|---|---|---|
| **rule-pack-authoring** | `skills/rule-pack-authoring/SKILL.md` | 从 Excel/Java/XSLT/BA notes 抽取规则，生成 draft pack |
| **rule-pack-review** | `skills/rule-pack-review/SKILL.md` | 检查 draft 的结构完整性、引用闭环、生成 review 与 report |

还有一个查询 Skill：

| Skill | 路径 | 职责 |
|---|---|---|
| **kb-query** | `skills/kb-query/SKILL.md` | 渐进式查询 Knowledge-Bases/ 中的规则 |

### 3.2 工作流流程

输入材料（Excel/Java/XSLT/BA notes）

↓

rule-pack-authoring -> Draft Pack (rules/lookups/helpers/evidence/review)

↓

rule-pack-review -> Review-Ready Pack (closure-check/review-checklist)

↓

BA/Dev signoff

### 3.3 Skill 参考文件

Authoring Skill 有 5 个 references：

- `references/supported-scope.md` - scope 切分规范
- `references/storage-split.md` - 存储层分离详细规则
- `references/output-pack-shape.md` - 最小输出结构要求
- `references/object-boundary-guide.md` - 何时建 rule/lookup/helper
- `references/evidence-guidelines.md` - 证据处理规范

Review Skill 有 2 个 references：

- `references/closure-criteria.md` - 闭环检查标准
- `references/review-output-shape.md` - review 报告格式

---

## 四、Knowledge Base 结构

```text
Knowledge-Bases/
  index.yaml                       # Layer 0: 列出 workflows
  TRADITION-STELLA/
    index.yaml                     # Layer 1: 列出 products (COMMON, FXD_NDF, FXO)
    COMMON/
      index.yaml                   # Layer 2: 列出 COMMON 下的 packs
    FXD_NDF/
      index.yaml                   # Layer 2: 列出 FXD 下的 packs（含 AI 可查询版本）
    FXO/
      index.yaml                   # Layer 2: 列出 FXO 下的 packs
```

KB 要给 AI 查询用的“可发现层”，与 `generated_pack/` 下的规范 pack 物件互为映射。

---

## 五、材料位置

所有源材料位于另一个 Java 项目中：

```text
C:\Users\1664901\Desktop\VPA_TRADITION_RTNS\54982-flow-traditionStella-plugin\
```

| 材料类型 | 路径 |
|---|---|
| workbook | `Tradition_SCBM_mapping.xlsx`（9 sheets） |
| rule doc inspect | `Tradition_SCBM_mapping-Inspect.md` |
| AI bundle | `Tradition_SCBM_mapping_ai-bundle.jsonl` |
| XSLT (header) | `src/main/resources/xslt/header/*.xsl` |
| XSLT (module) | `src/main/resources/xslt/module/*.xsl` |
| java extractors (common) | `extractor/common/MessageFields.java`、`TradeHeaderFields.java`、`TradePartyFields.java`、`PortfolioFields.java`、`OtherPartyPaymentFields.java` |
| java extractors (fxd) | `extractor/fx/FXDBasicFields.java`、`FXDCurrencyFields.java`、`FXDExchangeRateFields.java`、`FXDNdfFields.java` |
| java extractors (fxo) | `extractor/fx/FXOBasicFields.java`、`FXOCurrencyFields.java`、`FXOPremiumFields.java`、`FXOSettlementFields.java` |
| lookup utility | `utils/StaticMappingLookupUtils.java`、`StaticMappingLookupCore.java` |

---

## 六、当前完成状态（2026-04-16 快照）

### 6.1 合计 18 个 Pack

| 产品线 | Pack 数 | 已 Authored | 已 Reviewed |
|---|---:|---:|---:|
| COMMON | 4 | 4 | 4 |
| FXD_NDF | 5 | 5 | 5 |
| FXO | 9 | 9 | 0 |
| **合计** | **18** | **18** | **9** |

### 6.2 已完成 Authoring + Review 的 Pack（9 个）

| Pack | 产品线 | Review 结论 | Open Items |
|---|---|---|---|
| `rule-pack-cutoff-split` | FXD_NDF | ready for BA review | 0 |
| `rule-pack-party-split` | FXD_NDF | ready for BA review | 6 ambiguities |
| `rule-pack-scbml-header` | COMMON | ready for BA review | 1 ambiguity（trackingId source） |
| `rule-pack-trade-identifiers` | COMMON | ready for BA review | 1 ambiguity（UTI issuer） |
| `rule-pack-trade-date-product-summary` | COMMON | ready for Dev review | 0 |
| `rule-pack-portfolio` | COMMON | ready for Dev review | 0 |
| `rule-pack-fxd-product-identification` | FXD_NDF | ready for BA review | 0 |
| `rule-pack-fxd-currency-legs` | FXD_NDF | ready for BA review | 1 ambiguity |
| `rule-pack-fxd-exchange-rate` | FXD_NDF | ready for BA review | 1 ambiguity |

### 6.3 Authored 但未 Review 的 Pack（9 个 FXO Pack）

| Pack | 类别 | 规则数 | lookups | helpers |
|---|---|---|---|---|
| `rule-pack-fxo-product-identification` | O-01 | 9 rules | 0 | 0 |
| `rule-pack-fxo-currency` | O-02 | 3 rules | 0 | 0 |
| `rule-pack-fxo-expiry-and-strike` | O-03 | 5 rules | 0 | 0 |
| `rule-pack-fxo-premium` | O-04 | 5 rules | 0 | 1 (`hlp_fxo_premium_rate_to_percent`) |
| `rule-pack-fxo-cutoff` | O-05 | 4 rules | 1 | 0 |
| `rule-pack-fxo-party` | O-06 | 4 rules | 2 (refs) | 0 |
| `rule-pack-fxo-strategy` | O-07 | 2 rules | 1 | 0 |
| `rule-pack-fxo-ndo-settlement` | O-08 | 5 rules | 0 (refs O-05) | 0 |
| `rule-pack-fxo-broker-payment` | O-09 | 5 rules | 0 | 0 |

---

## 七、当前任务

### 任务 A：对 9 个 FXO Pack 进行 Review（覆盖空缺）

对每个 FXO pack，使用 `rule-pack-review` skill 执行 review workflow。

1. 读取 pack 下所有 YAML 对象、evidence-index、review-index
2. 按 `references/closure-criteria.md` 逐项检查：
   - 结构完整性
   - 引用闭环（dependencies 是否都有对应文件）
   - evidence 覆盖率
   - example 充分性
   - placeholder 或未完成痕迹
   - review-index 中 ambiguity 是否已收敛
   - ops-reference.md 必要
3. 产出：
   - `reports/closure-check.md`
   - `reports/review-checklist.md`
   - 更新 `review/review-index.yaml`（如发现新增问题）
4. 给出状态（ready for BA review / ready for Dev review / needs authoring follow-up）

**建议执行顺序**：O-01 -> O-02 -> O-03 -> O-04 -> O-05 -> O-06 -> O-07 -> O-08 -> O-09

### 任务 B：更新 open-review-items-summary.md

每完成一批 FXO pack review 后，将结果汇总到总目录的 `open-review-items-summary.md`。

### 任务 C：将 reviewed pack 同步到 Knowledge-Bases/（可延迟）

将 review 通过的 pack 映射到 `Knowledge-Bases/TRADITION-STELLA/{PRODUCT}/` 下的 `-for-AI` 目录，供 kb-query skill 可查询。

---

## 八、YAML 对象示例

### 8.1 target_rule 示例（value 类型）

```yaml
id: rule_scbml_initiated_timestamp
status: draft

signoff_status: none

source:
  inputs:
    - name: creationTimestamp
      source_path: /fpML/header/creationTimestamp/text()

logic:
  summary: Read the source creationTimestamp, convert from ISO datetime format to Z-suffixed format, and emit as initiatedTimestamp.
  pipeline:
    - op: read_xpath
      out: raw_timestamp
      source: /fpML/header/creationTimestamp/text()
    - op: format_datetime
      in: raw_timestamp
      from_format: "yyyy-MM-dd'T'HH:mm:ss"
      to_format: "yyyy-MM-dd'T'HH:mm:ss'Z'"
      out: converted_timestamp
    - op: emit_value
      path: /scb:SCBM/scb:header/scb:originationDetails/scb:initiatedTimestamp/text()
      from: converted_timestamp

target:
  kind: value
  path: /scb:SCBM/scb:header/scb:originationDetails/scb:initiatedTimestamp/text()

dependencies:
  lookups: []
  helpers: []
  rules: []

examples:
  - name: iso_to_z_conversion
    input:
      creationTimestamp: "2025-09-10T14:30:00"
    key_decision:
      conversion: "append Z suffix"
    result:
      target_value: "2025-09-10T14:30:00Z"
```

### 8.2 lookup_definition 示例

```yaml
id: lk_fxd_ndf_cutoff_by_pair_and_locode
status: draft
signoff_status: none

source:
  mapping_table: TraditionStellaCutoff
  inputs:
    - name: fixing_currency1
      source_path: /FpML/trade/trade/fxSingleLeg/nonDeliverableForward/fixing/quotedCurrencyPair/currency1/text()
    - name: fixing_currency2
      source_path: /FpML/trade/trade/fxSingleLeg/nonDeliverableForward/fixing/quotedCurrencyPair/currency2/text()
    - name: cutoff_code
      source_path: /FpML/trade/trade/fxSingleLeg/nonDeliverableForward/fixing/fixingTime/hourMinuteTime/text()

logic:
  summary: Query the cutoff mapping by forward currency-pair order first, then retry the reverse order.
  pipeline:
    - op: concat
      out: forward_pair
      args: [fixing_currency1, fixing_currency2]
    - op: compose_key
      out: forward_key
      template: "{forward_pair}|{cutoff_code}"
    - op: lookup_value
      out: forward_result
      key: "{forward_key}"
      return_field: requested_field
    - op: compose_key
      out: reverse_key
      template: "{reverse_pair}|{cutoff_code}"
    - op: lookup_value
      out: reverse_result
      key: "{reverse_key}"
      return_field: requested_field
      when: forward_result is empty
    - op: coalesce
      out: resolved_value
      args: [forward_result, reverse_result]

output:
  fields:
    - name: cutoff_name
      return_field: CUTOFF
    - name: cutoff_description
      return_field: CUTOFF_DESC
    - name: fixing_time
      return_field: FIXING_TIME
    - name: fixing_business_center
      return_field: FIXING_BUSINESS_CENTER

dependencies: []

examples:
  - name: sample_usdtwd_tw
    input:
      fixing_currency1: USD
      fixing_currency2: TWD
      cutoff_code: TW
    key_decision:
      matched_key: USDTWD|TW
      lookup_path: forward
    result:
      cutoff_name: TPE
```

### 8.3 evidence-index.yaml 示例

```yaml
entries:
  - evidence_id: ev_c01_timestamp_java
    object_id: rule_scbml_initiated_timestamp
    kind: java
    role: logic_support
    summary: MessageFields.CREATION_TIMESTAMP uses DateFormatConverterUtils.convertIsoDatetimeToZ.
    locator:
      java:
        path: extractor/common/MessageFields.java
        members: [CREATION_TIMESTAMP]
      utils:
        path: utils/DateFormatConverterUtils.java
        members: [convertIsoDatetimeToZ]
```

### 8.4 review-index.yaml 示例

```yaml
items:
  - review_id: rev_c01_tracking_id_source
    object_id: rule_scbml_tracking_id
    kind: ambiguity
    status: open
    summary: "trackingId source field 'tradeIdUnique' has no matching Java enum definition."
    follow_up: Confirm with Dev/BA what source produces `tradeIdUnique`.
    supporting_evidence:
      - ev_c01_tracking_id_xslt_only
```

---

## 九、Stella 输出结构摘要

目标 XML 的骨架结构如下，每个区域对应一个或多个 Pack：

```text
/scb:SCBM
  /scb:header                                <- C-01 rule-pack-scbml-header
  /scb:payload/scb:FPMLPayload
    conf:trade
      conf:tradeHeader
        conf:partyTradeIdentifier[*]         <- C-02 rule-pack-trade-identifiers
        conf:partyTradeInformation           <- F-05/O-06 party packs
        conf:productSummary                  <- C-03 rule-pack-trade-date-product-summary
        conf:tradeDate                       <- C-03
      scbextn:settlementDate                 <- C-03

      --- FXD path ---
      scbextn:fxSingleLeg
        conf:primaryAssetClass               <- F-01 fxd-product-identification
        conf:productType[*]                  <- F-01
        conf:productId[*]                    <- F-01
        conf:exchangedCurrency1/2            <- F-02 fxd-currency-legs
        conf:exchangeRate                    <- F-03 fxd-exchange-rate
        conf:nonDeliverableForward/fixing    <- F-04 cutoff-split

      --- FXO path ---
      scbextn:fxOption
        conf:primaryAssetClass               <- O-01 fxo-product-identification
        conf:productType[*]                  <- O-01
        conf:productId[*]                    <- O-01
        conf:buyer/sellerPartyReference      <- O-06
        conf:europeanExercise                <- O-03 fxo-expiry-and-strike
        conf:putCurrencyAmount               <- O-02 fxo-currency
        conf:callCurrencyAmount              <- O-02
        conf:strike                          <- O-03
        conf:fxOptionPremium                 <- O-04 fxo-premium
        conf:cashSettlementTerms (NDO)       <- O-08 fxo-ndo-settlement

      conf:otherPartyPayment                 <- O-09 fxo-broker-payment
      conf:party[@id='party1/party2/...']    <- F-05/O-06 party packs
      portfolio block                        <- C-04 rule-pack-portfolio
```

---

## 十、关键约束与注意事项

1. **不要发明业务逻辑**：如果原材料中找不到逻辑依据，记录为 ambiguity，不要猜测。
2. **主对象保持精简**：只保留 source/logic/target/dependencies/examples。
3. **Evidence 与 Review 是 Pack 级横切层**：不在每个对象文件中重复。
4. **状态机只能是 `draft` / `in_review` / `approved`，需要人工 signoff。**
5. **Authoring 不负责 review 结论，Review 不负责重写规则。**
6. **历史交叉项与 2026-04-02 存储层分离的结论冲突，以后者为准。**
7. **所有 pack 都放在 `generated_pack/` 目录下。**
8. **Knowledge-Bases/ 是给 AI 查询的发现层，与 `generated_pack/` 互为映射。**

---

## 十一、快速开始

### 1. 确认项目目录

```bash
cd transformation-rule-system/
```

### 2. 先读入口文档了解状态

```bash
cat open-review-items-summary.md
```

### 3. 读取 Review Skill 和 references

```bash
cat skills/rule-pack-review/SKILL.md
cat skills/rule-pack-review/references/closure-criteria.md
cat skills/rule-pack-review/references/review-output-shape.md
```

### 4. 开始 Review 第一个 FXO Pack

```bash
# 读取 pack 所有 YAML 文件、执行闭环检查，产出 closure-check.md + review-checklist.md
cd generated_pack/rule-pack-fxo-product-identification/
```

### 5. 完成后更新汇总

```bash
# 编辑 open-review-items-summary.md
```

---

*本文档交接基于 2026-04-16 的项目快照生成。如有疑问，从 README.cn.md 和 2026-04-15-全链Pack生成试跑 开始回溯。*
