<!-- docmeta
role: leaf
layer: 3
parent: docs/reference/README.md
children: []
summary: reference implementation plan for a Java-based RTS index/query layer; not the full RTS system baseline
read_when:
  - 需要理解一个 Java index/query layer 参考方案
  - 需要从已有索引层方案中提取工程实现思路
  - 需要从头到尾的实现范围、节奏与交付物说明
skip_when:
  - 只想看当前 RTS 全系统 baseline
  - 只想看 source governance、AI review 或 human adjudication 设计
source_of_truth:
  - docs/confirmed/system-constitution-v1.md
  - docs/reference/minimal-ov-integration.md
  - docs/reference/ov-kb-retrieval-design.md
  - docs/reference/java-index-layer-full-plan-zh.md
-->

# Java 索引层参考计划

> 状态：参考方案，不是 RTS 全系统基线
> 日期：2026-04-20
> 适用范围：RTS 的 index/query layer 参考设计
> 目标语言与主栈：Java

## 0. 当前定位

本文只描述一个 Java index/query layer 的参考实现思路。

它不定义 RTS 的完整系统边界，不覆盖 source ingestion、AI-first governance、human adjudication、service publication、API/MCP/Q&A/pipeline 产品面，也不决定最终技术栈。

当前 RTS 全系统定义以 `docs/confirmed/project-alignment-summary-zh.md` 和 `docs/confirmed/system-constitution-v1.md` 为准。

2026-05 对齐备注：本文早期把 projection 描述成“剥离治理字段后的 runtime Knowledge-Bases”。当前 confirmed baseline 已调整为：KB 和 projection 都是机器优先知识结构；projection 是 approved truth 的服务运行视图，不是摘要替身。Operational view 默认低噪声，governance view 可在权限允许时展开 evidence / review / report / adjudication summary 或 pointer。以 confirmed 文档为准。

## 1. 这份文档的用途

这份文档不是概念讨论，也不是最小原型说明。

这份文档记录的是：

**如何把 RTS 的索引/查询层设计成一个可交付、可分阶段推进、可长期维护的 Java 参考方案。**

如果它与 confirmed 总纲或 Constitution v2 冲突，以 confirmed 文档为准。

这份文档处理的是：

- 为什么要单做索引层
- 索引层到底负责什么
- Java 项目应该怎么搭
- 从第一行代码到生产上线分哪些阶段
- 每一阶段交付什么
- 什么算完成，什么不在当前范围

## 2. 当前决策

当前已经明确的决策如下：

- Truth layer 继续由现有 TRS 治理侧控制
- 索引层不接管 canonical truth、不接管 signoff、不接管 review
- 当前不引入 OpenViking runtime 作为正式运行时依赖
- 可以借鉴 OpenViking 的设计思想，但实现归属于本项目
- 正式实现优先采用 Java，而不是延续当前仓库中的 Python 原型
- Python 原型只作为设计验证与查询行为参考，不作为正式生产实现

这意味着本项目的定位是：

**一个面向 TRS 的独立 Java 索引层项目，而不是 OpenViking 的瘦身分叉。**

## 3. 项目目标

项目目标分成四类。

### 3.1 运行时资源目标

从 approved runtime input 中构建一个稳定的运行时知识库目录：

- 支持 channel / product / pack / object 层级
- 支持稳定 URI
- 支持 `L0 / L1 / L2`
- 支持 `rules / lookups / helpers`

### 3.2 检索目标

提供可解释、可控、可限制范围的检索能力：

- 支持 scope 查询
- 支持 pack 命中
- 支持 object 命中
- 支持读取 L0 / L1 / L2
- 支持 dependency hint
- 支持后续接入向量检索与 rerank

### 3.3 工程目标

做成一个可维护、可上线、可持续演进的 Java 服务：

- 有清晰模块边界
- 有持久化
- 有索引重建与增量同步
- 有测试、日志、指标、审计
- 能融入企业内部运维体系

### 3.4 组织目标

降低后续协作与接手成本：

- 让 Java 团队可直接维护
- 不依赖混合语言运行时
- 不把系统未来绑定在 OpenViking 代码结构上

## 4. 非目标

当前项目不做这些事情：

- 不负责 authoring
- 不负责 review 结论
- 不负责 signoff 决策
- 不把 session memory 纳入 truth retrieval
- 不做通用 agent platform
- 不追求复刻 OpenViking 全部能力
- 不自己实现 C++ 向量索引引擎
- 不在第一版支持所有多模态资源

如果未来需要扩展，可以新增阶段，但不改变当前项目边界。

## 5. 总体边界

整个系统按责任分成四层：

### 5.1 Truth Layer

由 TRS 原有治理侧控制。

职责：

- canonical pack
- review
- signoff
- approved truth 的定义

### 5.2 Projection Layer

由本索引项目实现。

职责：

- 读取 approved input
- 生成服务运行视图
- 保留服务事实所需的结构化 rule / lookup / helper L2 语义
- 生成 operational view、index view 和权限化 governance view
- 控制 evidence / review / report 的默认可见性，而不是让它们永久不可投影

### 5.3 Index Layer

由本索引项目实现。

职责：

- URI
- metadata
- L0/L1/card/index documents
- search index
- dependency graph

### 5.4 Query Layer

由本索引项目实现。

职责：

- query / find / read
- retrieval trace
- result shaping
- scope enforcement

一句话：

**TRS 决定什么是真的；索引层负责把真相整理成可查询、可导航、可解释的运行时表面。**

## 6. 最终交付形态

建议把项目做成一个独立仓库或独立子系统，采用“模块化单体 + 可拆任务进程”的结构。

初期部署形态：

- 一个 Spring Boot 主服务
- 一个 PostgreSQL
- 一个 OpenSearch 或 Elasticsearch
- 一个对象存储或文件存储

后期如果任务量明显上升，可把同步和建索引任务拆成 worker。

## 7. 技术选型

### 7.1 主语言与框架

- Java 21
- Spring Boot 3.x
- Gradle

### 7.2 结构化存储

- PostgreSQL

适用内容：

- metadata
- job 记录
- query log
- audit log
- dependency edge

### 7.3 搜索存储

默认建议：

- OpenSearch

可选：

- Elasticsearch

适用内容：

- L0 recall
- L1 rerank input
- hybrid retrieval
- 向量检索
- 过滤与聚合

### 7.4 文件存储

三选一即可：

- S3
- MinIO
- NAS

### 7.5 数据格式与工具

- Jackson
- SnakeYAML

### 7.6 调度与运行

- Spring Scheduler 或 Quartz
- Micrometer + Prometheus + Grafana
- Logback + JSON logging

### 7.7 权限与认证

接入公司现有方案：

- SSO
- OAuth2
- JWT

## 8. 为什么选择 Java

这里不是在比较语言优雅度，而是在比较成功概率。

选择 Java 的理由：

- 团队主栈一致
- 企业内部部署与运维更顺
- 和现有 Java 系统集成更容易
- 长期 ownership 更稳
- 不需要引入 Python + Rust + C++ 混合运行时

这不意味着 Java 更适合做所有事情。

它只是意味着：**对这个项目的组织环境而言，Java 更稳。**

## 9. 资源树定义

运行时目录结构固定如下：

```text
Knowledge-Bases/
  index.yaml
  {CHANNEL}/
    .abstract.md
    .overview.md
    index.yaml
    {PRODUCT}/
      .abstract.md
      .overview.md
      index.yaml
      {PACK}/
        .abstract.md
        .overview.md
        index.yaml
        rules/
          rule_*.yaml
          rule_*.abstract.md
        lookups/
          lk_*.yaml
          lk_*.abstract.md
        helpers/
          hlp_*.yaml
          hlp_*.abstract.md
```

这个目录树是正式运行时输出，不是 canonical authoring tree。

## 10. URI 规范

URI 是正式寻址规范，建议从第一版起固定。

推荐格式：

```text
kb://resources/{CHANNEL}/{PRODUCT}/{PACK}/...
```

如果为了延续已有概念，也可以继续用：

```text
viking://resources/{CHANNEL}/{PRODUCT}/{PACK}/...
```

本项目只要求 URI 稳定、唯一、可逆推，不要求兼容 OpenViking runtime。

## 11. 输入契约

本项目只接受来自 truth layer 的 approved input。

输入组织建议固定为：

```text
generated_pack/{CHANNEL}/{PRODUCT}/{PACK}/
  rules/
  lookups/
  helpers/
  evidence/
  review/
  reports/
```

进入 runtime projection 时：

- 默认 operational view 投影 `rules / lookups / helpers` 及必要 release/governance summary
- 权限化 governance view 可投影 evidence / review / reports 的 summary、pointer、redaction state 和 adjudication status
- raw evidence / raw review / full reports 不进入默认 operational query context
- 只接受 approved / signed-off 状态

这条边界必须写死在实现里，而不是依赖使用者自觉。

## 12. Projection 规则

Projection engine 负责把 canonical-ish 输入转成运行时资源树。

职责：

- 读取 product / pack / object
- 筛掉未批准对象
- 写出 runtime structured objects
- 复制或生成 object-level L0
- 生成 pack-level资源骨架
- 生成权限化 governance access refs

默认 operational query 不展开的字段包括但不限于：

- review_status
- ambiguities
- evidence_refs
- trace
- approval_history

这些字段不应被简单丢弃。它们应进入 governance-authorized view、summary 或 pointer，并受 caller profile、purpose、redaction state 和 trace 控制。

保留字段集中在：

- id
- source
- logic
- target
- dependencies
- examples

## 13. L0 / L1 / L2 规则

### 13.1 L0

最短摘要，目标是 recall 过滤。

要求：

- pack / product / channel 必须有
- object 建议有
- 长度短
- 能回答“它大概管什么”

### 13.2 L1

结构化概览，目标是 rerank 和导航。

要求：

- pack 必须有
- product 和 channel 建议有
- 需列出对象、目标范围、dependency hints

### 13.3 L2

原始对象正文。

对 object 来说通常就是 YAML 文件本身。

## 14. Summary 生成策略

Summary engine 必须支持双模式。

### 14.1 确定性保底模式

不依赖 LLM。

做法：

- 从 `id / logic.summary / target / dependencies / examples` 生成 L0
- 从 object list / dependency list / target list 生成 L1

这个模式保证系统永远可构建。

### 14.2 LLM 辅助增强模式

在需要更高质量文案时引入。

要求：

- 必须有固定输出 schema
- 必须有 validation
- 高风险内容可要求人工确认

结论：

系统不能把 L0/L1 的存在建立在“模型一定可用”之上。

## 15. Metadata 模型

项目必须维护一套正式 metadata。

至少包含：

- URI
- node type
- parent / child
- channel
- product
- pack
- object type
- object id
- storage path
- snapshot version
- summary version

Metadata 是整个系统的骨架。没有它，后续 audit、增量更新、查询 trace 都很难做好。

## 16. 数据库设计

建议至少建设以下表。

### 16.1 `projection_snapshot`

记录一次同步快照。

字段建议：

- id
- source_version
- source_location
- started_at
- finished_at
- status
- notes

### 16.2 `resource_node`

每个 URI 一条记录。

字段建议：

- id
- uri
- node_type
- channel
- product
- pack
- object_type
- object_id
- parent_uri
- storage_path
- snapshot_id
- active_flag

### 16.3 `resource_content`

记录 L0/L1/L2 内容引用。

字段建议：

- node_id
- abstract_path
- overview_path
- content_path
- abstract_hash
- overview_hash
- content_hash

### 16.4 `dependency_edge`

记录依赖关系。

字段建议：

- from_uri
- to_uri
- edge_type
- snapshot_id

### 16.5 `index_job`

记录 sync/rebuild 任务。

字段建议：

- job_id
- job_type
- status
- requested_by
- started_at
- finished_at
- error_message

### 16.6 `query_log`

记录查询行为。

字段建议：

- query_id
- query_text
- target_scope
- selected_pack_uri
- selected_object_uri
- duration_ms
- result_count
- trace_ref

### 16.7 `summary_revision`

记录摘要版本。

字段建议：

- node_uri
- revision_no
- generation_mode
- reviewer
- created_at

### 16.8 `resource_audit`

记录资源变化审计。

字段建议：

- audit_id
- action
- target_uri
- before_hash
- after_hash
- actor
- created_at

## 17. 搜索索引设计

建议在 OpenSearch 或 Elasticsearch 中至少建立两类索引。

### 17.1 `kb-l0`

面向 recall。

存储：

- uri
- node type
- scope fields
- L0 text
- vector

### 17.2 `kb-l1`

面向 rerank/navigation。

存储：

- uri
- pack/product/channel metadata
- L1 text
- object summaries
- vector

### 17.3 可选 `kb-object`

面向对象精确过滤。

存储：

- object id
- object type
- target path
- dependencies
- examples_count

## 18. 查询策略

查询不能只做全文搜索，必须走分层策略。

推荐策略如下。

### 18.1 Scope Resolution

先收窄范围。

可由以下信息确定：

- channel
- product
- optional pack
- optional object type

如果输入没有明确 scope，可通过规则或轻量模型推断，但最终必须得到明确范围。

### 18.2 Recall

在 `kb-l0` 上做候选召回。

第一版建议：

- BM25
- filter by scope

增强版建议：

- BM25 + vector hybrid recall

### 18.3 Rerank

读取 pack/product L1，进行二次排序与误召回过滤。

Rerank 的核心目标不是“更聪明”，而是：

- 让 pack 选得更准
- 让 dependency chain 更早暴露

### 18.4 L2 Load

只对确认候选的 object 读取 L2 正文。

这样既减少噪声，也提升可解释性。

### 18.5 Query Result Shaping

返回结果必须结构化，而不是只返回命中列表。

至少返回：

- query
- target scope
- selected pack URI
- selected object URI
- top candidates
- pack L0
- pack L1
- object L0
- object L2
- dependency hints
- retrieval trace

## 19. API 设计

建议第一版就定义正式 API。

### 19.1 索引相关

- `POST /api/index/sync`
- `POST /api/index/rebuild`
- `GET /api/jobs/{jobId}`

### 19.2 查询相关

- `POST /api/query`
- `POST /api/find`

### 19.3 资源读取相关

- `GET /api/resources/read`
- `GET /api/resources/abstract`
- `GET /api/resources/overview`
- `GET /api/resources/tree`

### 19.4 管理相关

- `GET /actuator/health`
- `GET /actuator/metrics`

## 20. 增量同步与重建策略

系统必须同时支持：

- 全量重建
- 增量同步

### 20.1 全量重建

适用于：

- 首次构建
- schema 变化
- 大规模修复

### 20.2 增量同步

适用于：

- pack 新增
- pack 修改
- pack 删除

第一版不要求做到对象级最细 diff，但至少要做到：

- pack 级 diff
- 删除时无残留
- 只重刷受影响的 metadata 与 search index

## 21. 项目模块划分

建议采用 Gradle 多模块结构。

示例：

```text
trs-index/
  settings.gradle
  build.gradle
  modules/
    index-api/
    projection-engine/
    summary-engine/
    metadata-engine/
    search-indexer/
    retrieval-engine/
    job-engine/
    common-model/
    infra-storage/
    infra-search/
    infra-db/
```

### 21.1 `common-model`

公共 DTO、schema、enum、uri model。

### 21.2 `projection-engine`

做 projection。

### 21.3 `summary-engine`

做 L0/L1。

### 21.4 `metadata-engine`

做扫描、URI、dependency graph。

### 21.5 `search-indexer`

写入 OpenSearch/Elasticsearch。

### 21.6 `retrieval-engine`

实现 find/query/read 的核心逻辑。

### 21.7 `job-engine`

实现 sync/rebuild job 编排。

### 21.8 `index-api`

对外暴露 REST API。

## 22. 开发顺序

整个项目建议按阶段推进，而不是并行乱开。

### 阶段 0：范围冻结

目标：

- 固定输入契约
- 固定输出契约
- 固定 URI 规范
- 固定 query response schema

交付物：

- 设计冻结记录
- schema 文档
- 决策记录

退出条件：

- 后续开发不再反复改资源树结构

### 阶段 1：工程骨架

目标：

- 搭起正式 Java 项目骨架

交付物：

- Gradle multi-module
- Spring Boot app
- Docker compose
- PostgreSQL schema baseline
- search backend bootstrap
- CI pipeline

退出条件：

- 本地一键启动可用

### 阶段 2：Projection Engine

目标：

- 把 approved input 稳定投影成 runtime tree

交付物：

- input reader
- governance field stripping
- runtime writer
- clean rebuild support

退出条件：

- 真实 sample packs 能稳定产出 `Knowledge-Bases/`

### 阶段 3：Summary Engine

目标：

- 让每个资源节点都有可用 L0/L1

交付物：

- deterministic summary
- summary validation
- optional llm-assisted path

退出条件：

- 在无 LLM 条件下仍能完整构建

### 阶段 4：Metadata Engine

目标：

- 让所有资源可寻址、可追踪

交付物：

- resource scanner
- URI builder
- dependency extraction
- index.yaml generation

退出条件：

- 任意对象可通过 URI 定位

### 阶段 5：Search Indexer

目标：

- 让资源树进入正式检索面

交付物：

- L0 index
- L1 index
- full rebuild
- pack-level delta indexing

退出条件：

- recall 对固定样本稳定

### 阶段 6：Retrieval Engine

目标：

- 提供正式 query/find/read 服务

交付物：

- scope resolution
- hybrid search
- rerank integration
- L2 read
- retrieval trace

退出条件：

- 真实问题集命中 pack/object

### 阶段 7：生产能力

目标：

- 把系统变成可上线服务

交付物：

- auth
- audit
- metrics
- alerting
- retry policy
- failure handling

退出条件：

- 故障可观察、权限可控、任务可追踪

### 阶段 8：验收与灰度

目标：

- 完成正式上线前验收

交付物：

- goldenset 报告
- 性能测试报告
- 安全评审记录
- 灰度与回滚方案

退出条件：

- UAT 通过

## 23. 测试计划

测试不能后补，必须和开发同步。

### 23.1 单元测试

覆盖：

- projector
- URI builder
- summary fallback
- dependency extraction
- scope resolver

### 23.2 集成测试

覆盖：

- PostgreSQL
- OpenSearch/Elasticsearch
- file/object store
- API

### 23.3 回归测试

使用固定 sample packs + 固定 query set。

### 23.4 Goldenset 测试

至少准备 20-50 条真实业务问题。

### 23.5 增量同步测试

覆盖：

- add pack
- update pack
- delete pack

### 23.6 性能测试

覆盖：

- 1k objects
- 10k objects
- 50k objects

### 23.7 权限测试

覆盖：

- channel 级隔离
- 越权读取
- 管理接口隔离

## 24. 观测与运维要求

上线前必须具备以下指标：

- sync 成功率
- rebuild 耗时
- query P50/P95/P99
- recall hit rate
- zero-result rate
- index write failure rate
- summary generation failure rate

日志最少记录：

- query text
- scope
- top candidates
- selected pack/object
- duration
- job lifecycle

## 25. 安全与审计要求

这不是附属功能，而是正式项目要求。

必须有：

- 只读查询权限控制
- 管理接口隔离
- query log
- sync/rebuild audit
- 操作人标识
- 资源变更审计

## 26. 上线完成标准

要宣布第一版项目完成，至少满足：

- 能从 approved input 构建 runtime `Knowledge-Bases/`
- 能生成 L0/L1/L2
- 能通过 URI 读取资源
- 能执行 scope query
- 能命中正确 pack/object
- 能做全量重建
- 能做 pack 级增量同步
- 有基础监控与审计
- 有真实问题集回归测试

## 27. 风险与主要控制点

### 风险 1：资源树结构反复变化

控制：

- 在阶段 0 冻结结构

### 风险 2：L1 质量不足导致误召回

控制：

- pack L1 模板优先打磨

### 风险 3：向量检索过早引入复杂度

控制：

- 第一版允许先以 BM25 + filter 起步

### 风险 4：增量同步过早做到过细

控制：

- 先做到 pack-level delta

### 风险 5：项目膨胀成通用 agent platform

控制：

- 明确不做 memory/skills/session

## 28. 代码量预估

这是正式项目，不是 MVP 脚本。

以 Java 实现估算：

- 核心业务代码：15k - 25k 行
- 测试代码：8k - 15k 行
- 配置、脚本、部署：2k - 5k 行

如果后续加入：

- admin UI
- 更复杂的 LLM 工作流
- 更细粒度增量机制
- 更强的权限与审计

整体会进一步扩大。

## 29. 时间与人力预估

在范围稳定、依赖环境明确的前提下：

- 第一版可用：6 - 10 周
- 生产可用：3 - 5 个月

建议最低人员配置：

- 1 名技术负责人
- 2 名 Java 后端
- 1 名测试/质量支持
- 0.5 名运维支持

## 30. 当前仓库中的实施意义

当前仓库里已经有 Python 原型与 sample data。它们的价值是：

- 验证资源树形状
- 验证 L0/L1/L2 的查询体验
- 验证 scoped retrieval 的方向

但它们不是正式生产方案。

Java 项目落地时，应把当前原型视为：

- 参考行为样本
- 参考目录结构样本
- 参考 query 输出样本

而不是直接延伸为正式实现。

## 31. 后续开发建议顺序

如果现在马上进入开发，建议按这个顺序执行：

1. 基于本文件冻结输入输出契约
2. 立新仓或新模块，搭 Java 工程骨架
3. 先实现 projection + metadata + read API
4. 再实现 summary + find/query
5. 最后补增量同步、监控、权限、审计

不要一开始就同时开做：

- 向量检索
- LLM 摘要增强
- 完整 admin UI
- 复杂调度体系

## 32. 这份文档之后如何使用

建议把这份文档用作三类活动的基线：

- 架构评审
- 需求拆解
- 开发排期

如果后续出现重大偏移，建议新建决议文档，而不是口头修改。

## 33. 最终建议

最终建议只有一句：

**按 Java 正式项目来做，借鉴 OV 的设计思想，但不要把项目做成 OV 的瘦身复制品。**

这个方向最符合当前边界、团队主栈和后续可维护性。
