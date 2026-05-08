# 项目关键词词汇表

> 状态：archived full glossary；当前默认入口已压缩为短词表
> 归档日期：2026-05-08
> 当前摘要：`docs/reference/project-keywords-glossary-zh.md`

## 这篇文档解决什么问题

当前仓库已经有稳定的总纲、constitution 和多份 reference 文档，但这些文档更偏架构和边界，不适合拿来做术语速查。

这篇文档的目标是：

- 用中文解释当前项目的高频关键词
- 给每个词一个最小示例
- 帮助读者建立统一心智模型

这里的示例主要用于说明概念，不等同于正式业务规则。

## 一张总图

先记住主链路：

```text
source materials
  -> evidence / extracted assertions
  -> candidate rules / conflicts / open questions
  -> human review / signoff
  -> approved canonical pack
  -> published runtime projection
  -> Knowledge Base + index + retrieval
  -> RTS internal LLM answer / analysis
  -> API / MCP / Q&A / pipeline response
```

一句话概括：

- RTS 对外是一个服务，不是一堆散文件
- Knowledge Base、索引/检索和 LLM 都是 RTS 内部能力
- LLM 可以整理、解释和分析，但它的回答不自动等于 truth
- truth 来自 source、evidence、review、人工裁决和 signoff
- 发布后的答案要能 trace 回 rule、evidence、review 和发布版本

## 1. 当前核心定位词

### RTS service

含义：RTS 对外表现为一个服务。外部系统、人、agent、pipeline 可以通过 API、MCP、问答界面或其他入口调用它。

最小示例：外部系统问 RTS“这个 target 字段怎么生成？”，RTS 内部读取 Knowledge Base、检索相关规则，并让 LLM 组织答案。

### Knowledge Base

含义：RTS 内部的规则知识基座，不只是文件夹。它承载已治理或可服务的规则对象、索引、摘要、依赖和可追溯信息。

最小示例：某个 signed-off pack 被发布后，会进入 RTS 的 Knowledge Base，供 API/MCP/Q&A 查询。

### internal LLM

含义：RTS 内部的 LLM 能力。它可以整理 source、解释规则、发现冲突、生成候选分析和回答问题。

最小示例：LLM 根据 `rule_cutoff_time` 和 `lk_cutoff_matrix` 解释为什么 cutoff time 是 14:00。

### LLM answer is not truth

含义：LLM 的回答不是自动成立的真相。它必须能追溯到 source、evidence、review、人工裁决和 signoff。

最小示例：LLM 说“这个字段可能来自 trade date”只是候选解释；只有 evidence 和 review 支持后，才能成为 approved truth。

### approved truth

含义：已经经过 review、人工裁决和 signoff，可以作为默认服务答案使用的规则真相。

最小示例：一个 pack 整体 signoff 后，其中的 rules/lookups/helpers 可以进入默认服务面。

### candidate

含义：AI、系统或人提出的候选规则、候选解释、候选影响面或候选问题。candidate 还不是 approved truth。

最小示例：AI 从 XSLT 中抽出一条可能规则，但 reviewer 还没确认，它就是 candidate。

### open question / ambiguity queue

含义：当 source 冲突、证据不足或 AI 无法判断时，RTS 应把问题放入待裁决队列，而不是硬给答案。

最小示例：Excel 和 XSLT 对同一字段给出不同逻辑，RTS 生成一个 open question 交给 reviewer 裁决。

### evidence-first drafting

含义：先有 evidence，再形成 extracted assertion，再生成 candidate rule，最后由 reviewer 决定是否成为 approved truth。

最小示例：先定位 Java extractor 中的字段逻辑，再抽取成候选 rule，而不是让 LLM 凭经验写 rule。

### facts / inference / unknown / candidate / decision

含义：RTS 输出要区分五类内容：事实、推断、不确定点、候选建议、人工决定。

最小示例：`rule_A 依赖 lookup_X` 是事实；“可能需要补测场景 Y”是候选建议；“reviewer confirmed”才是人工决定。

## 2. 四层心智模型词

### L1 Truth Layer

含义：决定什么是真的。保存 evidence、canonical pack、review、ambiguity、人工裁决、signoff 和 truth 版本。

最小示例：reviewer 裁决 Excel 和 XSLT 冲突后，决定写入 canonical pack。

### L2 Mapping / Projection Layer

含义：做两件事：把 source 整理成 candidate；把 approved truth 投影成运行时可用视图。它不决定 truth。

最小示例：L2 可以从 Java/XSLT/Excel 抽取 candidate rule，也可以把 signed-off pack 发布成 runtime projection。

### L3 Retrieval Layer

含义：负责找到信息，例如 URI、索引、L0/L1/L2、dependency hints、query trace。

最小示例：用户问 cutoff time，L3 先定位 FXD_NDF，再找到 cutoff pack 和相关 rule。

### L4 Application Layer

含义：人和系统使用 RTS 的入口，例如 Workbench、Review UI、API、MCP、Q&A、pipeline、agent tools。

最小示例：Q&A 界面调用 RTS，返回带 rule URI 和 trace 的解释。

### Workbench

含义：给人和 AI 协作整理规则的平台。它不直接产生 approved truth，而是帮助生成 candidate、冲突点和待裁决问题。

最小示例：上传 Excel 和 XSLT 后，Workbench 展示候选 rules、证据来源和 open questions。

### truth change summary

含义：approved truth 变化后生成的摘要，说明哪些规则变了、影响什么、下游应使用哪个版本。

最小示例：`rule_cutoff_time` 修改后，RTS 生成 summary，提示 FXD_NDF cutoff 相关测试需要重看。

### publication version

含义：RTS 对外服务使用的已发布 truth 版本。不能悄悄覆盖 latest。

最小示例：Pack A v1 正在服务；Pack A v2 signoff 后生成新发布版本，再显式切换为 active。

### refusal contract

含义：当 scope 不清、冲突未解决、依赖未发布或证据不足时，RTS 应拒答、降级回答或要求澄清。

最小示例：用户没有说明 product，系统不能混搜 COMMON、FXD_NDF、FXO 后硬答。

### dependency hint

含义：提示某条 rule 还关联哪些 lookup/helper/rule。第一阶段只用于展示和导航，不自动扩大搜索范围。

最小示例：命中 `rule_cutoff_time` 后，RTS 提示“还应查看 `lk_cutoff_matrix`”。

### stable URI

含义：规则、pack、lookup、helper 等对象的稳定地址，方便 API/MCP/Q&A 引用和审计。

最小示例：`kb://resources/TRADITION-STELLA/FXD_NDF/rule-pack-cutoff-split/rules/rule_cutoff_time`



含义：`Rule Truth Source / Transformation Rule System`，整个项目本体。文档中遇到 TRS 时，通常可理解为 RTS 的历史名称或别名。

最小示例：项目要回答“`Tradition -> Stella` 的某个 target 字段如何生成”，RTS 负责定义这条规则本身。

### truth-first

含义：先保证真相、证据和边界，再考虑 AI 检索和问答体验。

最小示例：一条规则还没签核完成，就不能因为“看起来合理”而进入运行时知识库。

### goldensource

含义：某个范围内最终可信、可引用的正式真相源。

最小示例：一个 pack 通过 review 和 signoff 之后，它就是这个 pack 范围内的 goldensource。

### governance

含义：保证真相可信的治理机制，包括 evidence、review、signoff、audit 等。

最小示例：不是只写出一条 cutoff rule，还要说明它的证据来源、审核状态和遗留问题。

### hallucination

含义：模型给出流畅答案，但答案缺少规则或证据支持。

最小示例：系统里没有任何规则说明 `cutoff time = 14:00`，模型却自行补出一个理由。

## 3. 真相层词

### canonical pack

含义：治理侧的正式规则包，是项目的 court of record。

最小示例：

```text
generated_pack/TRADITION-STELLA/FXD_NDF/rule-pack-cutoff-split/
  rules/
  lookups/
  helpers/
  evidence/
  review/
  reports/
```

### pack

含义：一个相对完整的问题域或规则簇的组织单元。

最小示例：`rule-pack-cutoff-split` 表示“与 cutoff split 相关的一组规则”。

### object

含义：pack 内的单个主对象。

最小示例：一条 `rule`、一个 `lookup`、一个 `helper` 都是 object。

### rule

含义：目标字段或业务规则的主定义对象。

最小示例：`rule_cutoff_time.yaml` 规定目标字段 `cutoffTime` 如何生成。

### lookup

含义：规则依赖的静态映射或矩阵数据。

最小示例：`lk_cutoff_matrix.yaml` 按币种、参与方数、产品线查出 cutoff time。

### helper

含义：规则复用的辅助逻辑定义。

最小示例：`hlp_normalize_currency_pair.yaml` 可以负责把 `USD/CNY` 的表达做统一化。

### evidence

含义：支撑规则成立的证据材料。

最小示例：原始报文样本、业务口径说明、系统截图。

### review

含义：审核中的问题、分歧、风险和待确认项。

最小示例：某条规则是否适用于 3-party transaction 还在 review 中。

### reports

含义：过程性产物，不是运行时知识主体。

最小示例：一次分析会输出的核对报告。

### signoff

含义：签核，表示规则跨过正式可信边界。

最小示例：只有 `signed-off` 的 pack 才能进入 runtime projection。

### ambiguity

含义：规则尚存在歧义或证据不足。

最小示例：同一个 target 字段在不同交易类型里表现不同，但暂时还没有证据说明采用哪一个版本。

### unknown is better than wrong

含义：宁可明确返回“不足以判断”，也不能给出未经证据支持的确定答案。

最小示例：如果 source message 不足以判断参与方数，就不应该硬断言 cutoff time。

## 4. 真相对象字段词

### id

含义：对象的稳定标识。

最小示例：`rule_cutoff_time`

### source

含义：规则从哪里取值、依赖哪些源字段。

最小示例：`source.xpath = /Trade/Participants/Count`

### logic

含义：规则的转换逻辑本体。

最小示例：如果参与方数为 `2`，则查询 `lk_cutoff_matrix` 获取 cutoff time。

### target

含义：规则要写入的目标字段或目标路径。

最小示例：`target.output_xpath = /StellaTrade/CutoffTime`

### dependencies

含义：本对象依赖的 lookup、helper 或其他对象。

最小示例：`dependencies = [lk_cutoff_matrix, hlp_normalize_currency_pair]`

### examples

含义：帮助理解规则的输入输出样例。

最小示例：输入 `USD/CNY + 2-party`，输出 `14:00 UTC`。

## 5. 投影层词

### Projection Layer

含义：把 approved canonical truth 转成服务运行机器视图的那一层。

最小示例：读取已批准 pack，生成 operational rule/lookup/helper L2、object cards、dependency edges、index views，以及权限化 governance access refs。

### approved input

含义：Projection 的合法输入，必须来自 truth layer 已批准内容。

最小示例：只有 `approved / signed-off` 的 pack 才能被扫描。

### runtime projection

含义：给 query service、索引、AI harness、API/MCP tools 使用的只读投影，不是 canonical authoring 原件，也不是摘要替身。

最小示例：运行时 rule L2 保留 inputs、target emits、logic pipeline、dependencies、examples 和允许暴露的 warnings；raw review/report 通过授权 governance view 展开。

### stripped-down projection

含义：投影时主动裁掉运行时不需要的治理信息。

最小示例：剥离 `signoff_status`、`review_status`、`evidence_refs`、`approval_history`。

### one-way read-only

含义：投影是单向、只读的，不会写回 canonical truth。

最小示例：Query 层读取过一条 rule，不会把会话里的总结写回原 pack。

## 6. 运行时资源树词

### Knowledge-Bases

含义：投影后的运行时知识树根目录。

最小示例：

```text
Knowledge-Bases/
  TRADITION-STELLA/
    FXD_NDF/
      rule-pack-cutoff-split/
```

### channel

含义：顶层分区，通常表示一个 source-target 组合。

最小示例：`TRADITION-STELLA`

### product

含义：channel 下的业务线或产品域。

最小示例：`COMMON`、`FXD_NDF`

### rules / lookups / helpers

含义：pack 内运行时可读的三类主对象目录。

最小示例：

```text
rule-pack-cutoff-split/
  rules/rule_cutoff_time.yaml
  lookups/lk_cutoff_matrix.yaml
  helpers/hlp_normalize_currency_pair.yaml
```

### URI

含义：资源的稳定地址，不依赖语义搜索猜测。

最小示例：`kb://resources/TRADITION-STELLA/FXD_NDF/rule-pack-cutoff-split/rules/rule_cutoff_time`

## 7. L0 / L1 / L2 词

### L0

含义：最短摘要，主要用于 recall 过滤。

最小示例：`这个 pack 处理 FXD NDF 的 cutoff time 规则。`

### L1

含义：结构化 overview，主要用于 rerank 和导航。

最小示例：列出 `rule_cutoff_time`、`lk_cutoff_matrix`，并说明它们之间的依赖关系。

### L2

含义：原始对象正文，对 object 来说通常就是 YAML 文件本体。

最小示例：`rule_cutoff_time.yaml` 中完整记录 source、logic、target、dependencies。

### layered loading

含义：按层次逐步读取上下文，而不是一开始就把全部 YAML 丢给模型。

最小示例：先读 pack 的 L0 判断是否相关，再读 L1 看对象列表，最后才加载具体 rule YAML。

## 8. 索引层词

### Index Layer

含义：在运行时知识树之上建立稳定寻址、metadata、summary、search index 和 dependency graph。

最小示例：给 `rule_cutoff_time` 生成 URI，记录它属于哪个 pack，并把它的 L0/L1/L2 路径注册进索引系统。

### metadata

含义：资源节点的结构化骨架信息。

最小示例：记录 `uri`、`channel`、`product`、`pack`、`object_type`、`storage_path`。

### resource_node

含义：数据库里表示一个资源节点的记录。

最小示例：`rule_cutoff_time` 在 `resource_node` 表里会有一条记录。

### resource_content

含义：节点对应的内容引用，通常指向 L0/L1/L2 的物理存储路径。

最小示例：同一节点记录 `.abstract.md`、`.overview.md`、`.yaml` 的路径和 hash。

### dependency_edge

含义：资源间的依赖边。

最小示例：`rule_cutoff_time -> lk_cutoff_matrix`

### projection_snapshot

含义：一次投影和同步的快照。

最小示例：某次全量重建会生成一个 `snapshot_id`，本次导入的所有节点都挂到这个快照上。

### summary_revision

含义：摘要版本记录。

最小示例：pack 的 L1 重新生成一次，`revision_no` 从 `1` 变成 `2`。

### resource_audit

含义：资源变化审计记录。

最小示例：`before_hash != after_hash`，说明某个对象内容发生了变化。

## 9. 搜索索引词

### kb-l0

含义：用于 recall 的搜索索引。

最小示例：存放 pack 的 L0 文本、scope 字段和向量。

### kb-l1

含义：用于 rerank 和导航的搜索索引。

最小示例：存放 pack 的 L1 文本、对象摘要和 metadata。

### kb-object

含义：可选的对象级精确过滤索引。

最小示例：按 `object_id = rule_cutoff_time` 或 `object_type = lookup` 进行精确过滤。

### BM25

含义：第一版推荐使用的关键词召回方法。

最小示例：用户搜索 `cutoff time` 时，先用 BM25 从 `kb-l0` 拉出候选 pack。

### hybrid recall

含义：关键词召回和向量召回结合。

最小示例：`BM25 + vector` 同时考虑字面匹配和语义相似度。

## 10. 查询层词

### Query Layer

含义：对外提供 `query / find / read` 能力的层。

最小示例：用户问“initiatedTimestamp 怎么生成”，Query Layer 决定先搜哪个 scope，再加载哪条 rule。

### scope

含义：查询范围约束。

最小示例：`channel = TRADITION-STELLA`，`product = FXD_NDF`

### scope resolution

含义：从问题中先解析出明确查询范围。

最小示例：用户提到 `FXD`，系统把范围解析为 `FXD_NDF`，而不是去全库混搜。

### recall

含义：第一轮粗召回候选。

最小示例：在 `kb-l0` 中找到 5 个可能相关的 pack。

### rerank

含义：读取更丰富的 L1 上下文，对候选做二次排序和误召回过滤。

最小示例：本来 `COMMON` 和 `FXD_NDF` 都命中了关键词，但读过 L1 后确认真正相关的是 `FXD_NDF`。

### L2 load

含义：只对确认候选加载完整正文。

最小示例：只加载 `rule_cutoff_time.yaml` 和 `lk_cutoff_matrix.yaml`，不把整个 product 全读进来。

### dependency hint

含义：在回答问题前提前暴露“还应查看哪些依赖”。

最小示例：命中 `rule_cutoff_time` 时，提示还要读取 `lk_cutoff_matrix`。

### retrieval trace

含义：记录系统如何一步步找到答案。

最小示例：`scope = FXD_NDF -> hit pack L0 -> confirm pack L1 -> load rule L2 + lookup L2`

### result shaping

含义：把查询结果整理成结构化响应，而不是只给命中列表。

最小示例：返回 `selected_pack_uri`、`selected_object_uri`、`dependency_hints`、`retrieval_trace`。

## 11. 运维与演进词

### full rebuild

含义：全量重建索引和 metadata。

最小示例：首次上线或 schema 变化时，重新扫描所有 approved input。

### incremental sync

含义：增量同步，只更新变化的 pack。

最小示例：新增一个 pack，只重刷该 pack 相关的 metadata 和 search index。

### pack-level delta

含义：第一版要求的最小增量粒度，以 pack 为单位比较和更新。

最小示例：`rule-pack-cutoff-split` 内容变化，则只重建这个 pack，而不是整库重跑。

## 12. LLM 角色词

### reader

含义：读取 canonical object 或 runtime projection。

最小示例：打开 `rule_cutoff_time.yaml` 阅读其逻辑。

### navigator

含义：决定下一步该打开哪个 channel、product、pack、object。

最小示例：先进入 `FXD_NDF`，再进入 `rule-pack-cutoff-split`。

### explainer

含义：把规则改写成更容易理解的人话，但不改变原意。

最小示例：把 YAML 解释成“当参与方数为 2 时，从 cutoff matrix 取 14:00 UTC”。

### disputer marker

含义：明确指出歧义、证据不足或状态冲突。

最小示例：如果某条规则还存在 review open item，就必须提示这个风险，而不是假装规则已完全稳定。

## 13. 一个完整的小例子

用户问题：

```text
FXD NDF 的 cutoff time 为什么是 14:00？
```

系统中的关键词会这样串起来：

1. 做 `scope resolution`
   得到 `channel = TRADITION-STELLA`，`product = FXD_NDF`
2. 在 `kb-l0` 做 `recall`
   命中 `rule-pack-cutoff-split`
3. 读取该 pack 的 `L1`
   发现其中有 `rule_cutoff_time`，并依赖 `lk_cutoff_matrix`
4. 执行 `L2 load`
   读取 `rule_cutoff_time.yaml` 和 `lk_cutoff_matrix.yaml`
5. 生成 `result shaping`
   返回选中的 pack、选中的 object、依赖提示和 retrieval trace

如果 lookup 内容显示：

```text
USD/CNY + 2-party -> 14:00 UTC
USD/CNY + 3-party -> 15:00 UTC
```

那么系统就可以解释：

- 当前答案来自 `rule_cutoff_time`
- 它依赖 `lk_cutoff_matrix`
- `14:00 UTC` 不是模型猜的，而是 lookup 给出的结果

这正是项目强调的核心原则：

- 先找对 scope
- 再找对 pack
- 再读对 object
- 最后才回答

## 14. 最容易混淆的几组词

### canonical pack vs runtime projection

- canonical pack 是治理真相
- runtime projection 是运行时只读副本

### evidence / review vs rules / lookups / helpers

- evidence 和 review 用来证明与治理
- rules / lookups / helpers 用来运行时读取和回答

### L0 / L1 / L2

- L0 是短摘要
- L1 是结构化概览
- L2 是原始正文

### Index Layer vs Query Layer

- Index Layer 负责把资源整理好、建好索引
- Query Layer 负责按正确顺序把它们找出来并返回

### “会搜索” vs “知道真相”

- 搜索能力不等于真相能力
- 真相仍然来自 approved pack 和其投影
