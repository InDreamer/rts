<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: Day1 implementation plan for the RTS query service, lightweight index layer, and controlled LLM harness
read_when:
  - 需要落地第一版 RTS 查询/索引/LLM 服务
  - 需要判断 Day1 是否应该包含 LLM harness
  - 需要确定 JDK 17、文件系统 projection store、Lucene、本地 L2 存储和 API/MCP 的第一版边界
  - 需要把 OpenViking、mem0、PageIndex 的检索和 harness 思路映射到 RTS 第一版
skip_when:
  - 只需要理解 RTS 总纲或系统宪法
  - 只需要定义 KB 到索引层的 projection contract
  - 只需要讨论 Day2 以后更 agentic 的检索增强
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
-->

# RTS Day1 Query Service and Controlled LLM Harness Plan

> 状态：confirmed baseline  
> 范围：第一版 RTS 查询/索引/LLM 服务落地方案  
> 技术约束：JDK 17、对象规模初期约上百、无现成 PostgreSQL/OpenSearch/SSO/对象存储/审计平台  

## 1. Day1 一句话结论

Day1 不应该只做一个裸索引库，也不应该做成完整 agentic RAG。

Day1 应该交付一个 **RTS Query Service + Lightweight Governed Index + Controlled LLM Harness**：

- KB 侧发布 approved runtime projection。
- Query service 读取 projection，建立结构化索引和 Lucene BM25 索引。
- API/MCP/LLM harness 都通过同一组受控查询工具读取 truth。
- LLM 可以识别意图、选择工具、组织答案，但不能绕过 scope、权限、发布态和 L2 回读。
- 最终事实必须来自 L2 object；search hit、summary、LLM answer 不拥有 truth。

这个 Day1 方案刻意避免 OpenSearch、向量库、完整 DSL compiler、完整 graph database 和自由 agent。原因不是这些技术没价值，而是当前对象规模和基础设施条件不需要它们。

## 2. Day1 要解决的问题

Day1 的目标不是证明“AI 很聪明”，而是让 RTS 作为服务开始可用，并且不破坏 truth 边界。

第一版必须能支持：

- 用户或 agent 问“某个 target 字段如何生成”
- 用户或 agent 问“这条 rule 依赖哪些 lookup/helper/source”
- 用户或 agent 问“某个 lookup/helper 被哪些 rule 使用”
- 服务能在 scope 不清、对象未发布、L2 缺失、依赖未发布时拒答或要求澄清
- 服务能返回可追踪答案：release、scope、命中对象、读取 L2、query trace
- LLM 能作为入口，但只能调用服务提供的只读工具

Day1 暂不追求：

- 大规模向量召回
- 跨 release 自动推理
- 自动生成或修改规则
- pipeline 自动 gate
- UI workbench
- 自由多 agent 协作
- session memory / agent memory

## 3. 当前约束和设计影响

### 3.1 对象规模

初期对象规模可能只有上百个，达不到几千。

这意味着：

- 不需要 OpenSearch 作为第一版基础设施。
- 不需要专门向量数据库。
- 不需要复杂分布式索引更新。
- 文件系统 projection store + embedded Lucene 足够支撑 Day1 查询。
- 更重要的是把 projection contract、scope gate、L2 回读和 trace 做正确。

### 3.2 基础设施

当前没有统一 OpenSearch、SSO、对象存储和审计平台。

这意味着：

- Day1 用本地文件系统保存 runtime projection metadata、trace 和 L2 runtime content，保留未来切换 PostgreSQL/S3/MinIO/NAS 的接口。
- Day1 用简单 API key / caller profile / local allowlist 表达权限边界，保留未来接 SSO 的接口。
- Day1 用 JSONL query trace 和结构化日志表达审计，保留未来接 PostgreSQL 或企业审计平台的接口。
- Day1 不引入重型组件，否则会让基础设施先于产品价值膨胀。

### 3.3 JDK 17

JDK 17 可以满足 Day1 所有核心能力。

可行技术：

- Spring Boot 3.x 支持 Java 17。
- Apache Lucene 在 Java 中是一等能力，BM25 是强项。
- Jackson、JUnit、Testcontainers 都成熟；PostgreSQL JDBC 和 Flyway 可作为 Day1.5/Day2 的可替换存储实现。
- LLM 接入可以用 OpenAI-compatible HTTP adapter，不需要依赖 Python agent 框架。
- MCP 可以作为薄适配层，调用内部 query service。

JDK 17 的主要限制是：本地 embedding/reranker 和 agent 实验生态不如 Python 方便。但 Day1 不依赖这些能力。

## 4. 从三个项目借鉴什么

### 4.1 从 OpenViking 借鉴

OpenViking 的关键价值不是代码依赖，而是检索心智模型：

- 稳定 URI：对象必须能被服务、agent、trace 稳定引用。
- L0/L1/L2：先低噪声导航，再读取最终内容。
- `find` vs `search`：低成本检索和带 LLM intent analysis 的检索要分开。
- 分层检索：先进入目录/结构，再读取叶子对象。
- trace：检索过程应能解释和回放。

RTS Day1 对应设计：

- `find` 是无 LLM 或低 LLM 的候选定位接口。
- `ask` 是 Controlled LLM Harness，内部只能调用受控工具。
- L0/L1 用于定位，不作为最终 rule truth。
- L2 object 是最终事实读取面。

不借鉴：

- session memory 写回
- agent memory 自动学习
- 通用 skill runtime
- OpenViking runtime dependency
- 自动把聊天内容变成未来检索事实

### 4.2 从 mem0 借鉴

mem0 的关键价值是“LLM 应用通过工具访问记忆/检索层”，以及多信号检索思路：

- search tool 是明确工具，而不是让模型直接访问存储。
- 检索可以融合语义、BM25、entity boost 和 rerank。
- filters 是 scope/metadata 的核心入口。

RTS Day1 对应设计：

- LLM 只能调用 `resolve_scope`、`find_objects`、`read_object_l2` 等工具。
- Day1 先采用 BM25 + metadata filter + entity/alias boost 的轻量版本。
- `save_memory` 类能力不进入 RTS Day1，因为 runtime memory 不是 truth。

不借鉴：

- add/update/delete memory
- agent-generated facts 作为 truth
- personalization memory
- 会污染 approved truth 的自动学习机制

### 4.3 从 PageIndex 借鉴

PageIndex 的关键价值是“先读结构，再按需读内容”：

- agent 先读 document metadata。
- 再读 tree structure。
- 最后只读相关 page/content。
- 工具提示要求 tight range，不允许一次读完整文档。

RTS Day1 对应设计：

- LLM 先确定 release/scope。
- 再读 object card / pack navigation。
- 最后只读必要 rule/lookup/helper L2。
- 工具层限制 L2 读取数量和 scope，防止一次塞入大量上下文。

不借鉴：

- 纯 LLM tree search 作为默认检索引擎
- 无门禁的 agentic retrieval
- 文档 page tree 作为 RTS 主 truth shape

## 5. Day1 总体架构

```text
KB / Truth Layer
  |
  | approved immutable runtime projection
  v
Projection Ingestor
  |
  | validate manifest / schema / hash / signoff / dependencies
  v
Projection Store
  |-- Filesystem JSON/JSONL: release, scope, object manifest, cards, edges, refs, trace
  |-- Local L2 store: runtime rule/lookup/helper content
  |-- Lucene index: BM25 over card/L0/L1 within scope
  v
RTS Query Service
  |-- deterministic lookup
  |-- Lucene find
  |-- dependency traversal
  |-- L2 reader
  |-- answer assembler
  |-- refusal contract
  v
Controlled LLM Harness
  |-- intent/scope/anchor extraction
  |-- tool selection
  |-- grounded answer shaping
  |-- no direct DB access
  |-- no direct filesystem access
  v
REST API / optional MCP Adapter / future UI
```

核心原则：

- 文件系统 projection store 是 Day1 runtime projection 的主查询骨架。
- Lucene 是辅助候选定位，不是 truth。
- L2 content 是最终事实读取面。
- LLM harness 是受控工具调用层，不是独立 truth owner。
- PostgreSQL/Flyway 是后续可替换持久化实现，不是 Day1 阻塞项。

## 6. Day1 模块划分

### 6.1 Common Model

定义所有跨模块共享的数据结构。

核心模型：

- `ReleaseManifest`
- `ScopeKey`
- `ObjectUri`
- `ObjectManifestEntry`
- `ObjectCard`
- `RuleCard`
- `LookupCard`
- `HelperCard`
- `DependencyEdge`
- `ContentRef`
- `QueryPlan`
- `CandidateObject`
- `TraceRecord`
- `ServiceAnswer`
- `RefusalReason`

Common Model 应尽量稳定，因为 KB projection、index ingest、query service、LLM harness、MCP adapter 都依赖它。

### 6.2 Projection Ingestor

负责读取 KB 发布出的 runtime projection。

职责：

- 读取 `release_manifest`
- 校验 projection schema version
- 校验 release activation state
- 校验 included scope
- 校验 object manifest
- 校验 object card schema
- 校验 dependency edges
- 校验 L2 content ref 可读取
- 校验 content hash
- 拒绝不满足 admission rules 的 projection

Day1 不负责：

- 生成 canonical truth
- 修改 pack
- 修改 review/signoff 状态
- 生成 reviewer decision

### 6.3 Projection Store

负责把 projection 写入可查询存储。

Day1 推荐：

- 文件系统 JSON/JSONL 保存结构化 projection。
- 本地文件系统保存 L2 runtime content。
- Lucene 保存 card/L0/L1 的 BM25 索引。

Projection Store 必须支持：

- 全量 clean rebuild
- active release 切换
- release rollback 指针
- query-time active release 读取
- immutable content hash 校验
- 通过 `ProjectionStore`、`TraceStore`、`ScopeRegistry`、`ContentStore` 接口访问
- 未来可替换为 PostgreSQL/Flyway 实现而不改 Query Service 语义

Day1 可以暂缓：

- object-level 增量发布
- 多 active release 并行服务
- 分布式索引同步
- PostgreSQL schema 和 Flyway migration

### 6.4 Lucene Indexer

负责把可搜索文本写入 embedded Lucene。

Lucene 文档建议字段：

- `uri`
- `release_id`
- `canonical_revision`
- `channel`
- `product`
- `pack`
- `domain`
- `object_type`
- `object_id`
- `target_path`
- `source_anchor`
- `lookup_id`
- `helper_id`
- `rule_id`
- `business_terms`
- `searchable_text`
- `card_text`
- `l0_text`
- `l1_text`

索引内容来源：

- object card
- L0
- L1
- 结构化字段拼接文本
- alias / glossary terms

注意：

- Lucene 只用于候选定位。
- Lucene 命中必须回 ProjectionStore 校验 state/scope/release。
- Lucene 命中不能直接进入最终答案。

### 6.5 Query Resolver Lite

Day1 的 resolver 不需要完整 compiler，只需要把用户问题变成可执行 query plan。

输入：

- natural language question
- caller profile
- optional explicit scope
- optional release preference
- optional output mode

输出：

- `intent`
- `scope`
- `anchors`
- `required_state`
- `tool_plan`
- `refusal_if_missing`

Day1 intent 建议：

- `rule_lookup`
- `lookup_lookup`
- `helper_lookup`
- `explain_rule`
- `dependency_lookup`
- `impact_preview`
- `coverage_check`
- `unknown_or_chitchat`

Day1 anchors 建议：

- target path
- source path
- rule id
- lookup id
- helper id
- URI
- field name
- business term

实现方式：

- 先用 deterministic parser 识别 URI、rule id、lookup id、helper id、target/source path。
- 再用 alias dictionary 和 glossary 识别业务术语。
- LLM 只作为辅助 JSON extractor，不能直接决定最终对象。
- resolver 结果必须经过 schema validation。

### 6.6 Retrieval Engine

负责执行 query plan。

执行顺序：

1. Validate manifest and active release。
2. Resolve scope。
3. Check caller permission。
4. Deterministic lookup。
5. Lucene BM25 find inside scope。
6. Load object cards。
7. Rerank by deterministic signals。
8. Load selected L2。
9. Load required dependency cards/L2 under policy。
10. Produce trace and answer package。

Day1 排名信号：

- exact URI match
- exact object id match
- exact target/source path match
- object type match
- channel/product/pack/domain match
- BM25 score
- alias/entity match
- dependency proximity
- deprecated/superseded penalty

不要做：

- 让 BM25 分数越过 scope gate
- 让 LLM 直接重排未校验候选
- 让 dependency 自动跨 scope 扩大候选

### 6.7 Dependency Service

负责 dependency edge 查询。

Day1 支持：

- rule -> lookup
- rule -> helper
- helper -> lookup
- helper -> helper
- rule -> rule
- rule -> source anchor
- rule -> target path
- lookup/helper -> consumer

Day1 traversal 建议：

- 默认一跳。
- impact preview 可以反向查 consumer。
- explain rule 可以读取 required dependency。
- 深度 traversal 必须有最大深度和最大对象数。

### 6.8 L2 Reader

负责读取最终 runtime content。

规则：

- 只能读取 active projection 中登记的 L2。
- 每次读取校验 content hash。
- 不允许根据任意文件路径读取。
- L2 读取必须记录 trace。
- L2 不可读时，不能输出事实答案。

### 6.9 Answer Assembler

负责把结构化查询结果组装成 service answer。

它不一定调用 LLM。

无 LLM 时也应能返回：

- scope
- matched objects
- facts from L2
- dependencies
- unknowns
- refusal reason
- trace id

有 LLM 时，Answer Assembler 给 LLM 的上下文必须已经经过 gate 和 L2 读取。

### 6.10 Controlled LLM Harness

这是 Day1 必须有的服务能力，但必须受控。

LLM harness 职责：

- 解析用户自然语言意图
- 当 scope 不完整时生成澄清问题
- 选择受控工具
- 把工具结果组织成易读答案
- 分离事实、推断、未知和候选建议

LLM harness 禁止：

- 直接访问数据库
- 直接读取文件系统
- 修改 projection
- 修改 KB/canonical truth
- 创建 memory
- 把未读 L2 的摘要写成事实
- 自行解决 conflict/precedence
- 输出没有对象引用支持的 material claim

## 7. Day1 技术选型

### 7.1 Runtime

推荐：

- JDK 17
- Spring Boot 3.x
- Gradle

理由：

- 公司使用 JDK 17。
- Spring Boot 3.x 是 Java 17 兼容基线。
- 长期服务化、API、配置、监控、测试生态成熟。

### 7.2 Structured Store

推荐：

- Filesystem-backed projection store
- JSON / JSONL
- Jackson

用途：

- projection release
- scope registry
- object manifest
- object cards
- dependency edges
- content refs
- query trace
- LLM run trace
- simple caller profile / permission mapping

接口设计：

- `ProjectionStore`
- `ScopeRegistry`
- `TraceStore`
- `CallerProfileStore`
- future `PostgresProjectionStore`
- future `PostgresTraceStore`

要求：

- 业务层不能直接依赖文件路径或 JSON 文件布局。
- 读取必须经过 release/scope/state gate。
- trace 写入使用 append-only JSONL。
- projection metadata 可以在启动或 ingest 后装载到内存索引以提升查询速度。
- PostgreSQL/Flyway 仅作为 Day1.5/Day2 替换实现预留，不是 Day1 必需组件。

### 7.3 Text Index

推荐：

- Apache Lucene embedded

理由：

- 对象规模小，不需要 OpenSearch。
- BM25 是 Lucene 原生强项。
- Java 内嵌，部署简单。
- 未来可把 Lucene document schema 映射到 OpenSearch。

注意：

- Lucene index 是 rebuildable artifact。
- 文件系统 projection metadata 是 Day1 主存储。
- Lucene 目录可以本地持久化，也可以在启动时从 projection metadata 重建。

### 7.4 L2 Store

Day1 推荐：

- 本地文件系统

接口设计：

- `ContentStore`
- `LocalFileContentStore`
- future `S3ContentStore`
- future `MinioContentStore`

要求：

- 不接受任意 caller path。
- 只通过 content ref 读取。
- 校验 hash。

### 7.5 LLM Client

Day1 推荐：

- OpenAI-compatible HTTP adapter
- 模型供应商可配置
- 支持禁用 LLM
- 支持 request/response trace
- 支持 JSON schema extraction

不建议 Day1 直接引入重型 agent framework。

原因：

- Java 生态下 agent framework 选择不如 Python 稳定。
- RTS Day1 需要的是受控工具调用，不是自由 agent。
- 工具编排自己实现更容易审计。

### 7.6 API / MCP

Day1 推荐：

- REST API 是主服务面。
- MCP adapter 可以作为薄包装，直接调用 REST/Service 层。

MCP 不应该拥有单独查询逻辑。

### 7.7 Auth / Permission

没有企业 SSO 时，Day1 用最小权限模型：

- local API key
- caller profile
- allowed channel/product/pack
- allowed output mode
- admin endpoint 单独 key

未来接入：

- SSO
- OAuth2
- JWT
- centralized audit

## 8. Filesystem Projection Store 最小布局

Day1 先不依赖 PostgreSQL。Projection metadata、权限配置和 trace 以本地文件系统 JSON/JSONL 保存，但必须通过存储接口访问，避免未来迁移 PostgreSQL 时改动查询语义。

建议根目录：

```text
runtime-store/
  active-release.json
  releases/
    <release_id>/
      release-manifest.json
      scopes.jsonl
      object-manifest.jsonl
      object-cards.jsonl
      dependency-edges.jsonl
      content-refs.jsonl
      caller-profiles.jsonl
      l2/
        ...
      lucene/
        ...
  traces/
    query-trace.jsonl
    llm-run-trace.jsonl
```

### 8.1 `active-release.json`

记录当前 active release 指针和 rollback 目标。

关键字段：

- `active_release_id`
- `rollback_target_release_id`
- `updated_at`
- `updated_by`

### 8.2 `release-manifest.json`

记录 released projection。

关键字段：

- `release_id`
- `canonical_revision`
- `projection_schema_version`
- `card_schema_version`
- `summary_schema_version`
- `activation_state`
- `generated_at`
- `released_at`
- `rollback_target_release_id`
- `content_hash_summary`
- `blocking_issues_count`
- `created_at`

### 8.3 `scopes.jsonl`

记录可查询 scope。

关键字段：

- `release_id`
- `channel`
- `product`
- `pack`
- `domain`
- `active_flag`
- `permission_boundary`
- `precedence_policy`
- `deprecated_flag`
- `superseded_by`

### 8.4 `object-manifest.jsonl`

记录可索引对象。

关键字段：

- `uri`
- `release_id`
- `object_id`
- `object_type`
- `channel`
- `product`
- `pack`
- `domain`
- `target_path`
- `source_anchors`
- `content_hash`
- `card_ref`
- `content_ref`
- `schema_version`
- `state`

### 8.5 `object-cards.jsonl`

记录 rule/lookup/helper card。

关键字段：

- `uri`
- `release_id`
- `object_type`
- `card_json`
- `search_text`
- `risk_flags`
- `applicability`
- `not_applicable`
- `override_refs`
- `supersession_refs`

### 8.6 `dependency-edges.jsonl`

记录依赖图。

关键字段：

- `release_id`
- `from_uri`
- `to_uri`
- `edge_type`
- `required_flag`
- `direction`
- `traversal_purpose`

### 8.7 `content-refs.jsonl`

记录 L2 内容引用。

关键字段：

- `uri`
- `release_id`
- `content_uri`
- `storage_kind`
- `storage_ref`
- `content_hash`
- `content_type`
- `schema_version`

### 8.8 `traces/query-trace.jsonl`

记录每次查询。

关键字段：

- `trace_id`
- `caller_id`
- `entrypoint`
- `query_text`
- `query_plan_json`
- `resolved_scope_json`
- `candidate_uris_json`
- `selected_uris_json`
- `l2_read_uris_json`
- `refusal_reason`
- `release_id`
- `duration_ms`
- `created_at`

### 8.9 `traces/llm-run-trace.jsonl`

记录 LLM harness 调用。

关键字段：

- `llm_run_id`
- `trace_id`
- `model`
- `prompt_version`
- `tool_calls_json`
- `tool_outputs_hash`
- `final_output_hash`
- `validation_result`
- `duration_ms`
- `created_at`

### 8.10 `caller-profiles.jsonl`

Day1 简化权限。

关键字段：

- `caller_id`
- `api_key_hash`
- `allowed_channels`
- `allowed_products`
- `allowed_packs`
- `allowed_entrypoints`
- `allowed_output_modes`
- `active_flag`

### 8.11 PostgreSQL 迁移边界

未来迁移 PostgreSQL 时，上述 JSON/JSONL shape 可以直接映射为关系表：

- `runtime_release`
- `scope_registry`
- `object_manifest`
- `object_card`
- `dependency_edge`
- `runtime_content_ref`
- `query_trace`
- `llm_run_trace`
- `caller_profile`

Day1 代码必须保证 Query Service 只依赖 store 接口，不直接依赖 filesystem layout。

## 9. Day1 API Surface

API 命名可以后续统一，这里定义语义边界。

### 9.1 `POST /api/v1/query/plan`

用途：

- 把自然语言或结构化请求转为 query plan。
- 可选择禁用 LLM，只使用 deterministic resolver。

输入要点：

- `query`
- `caller_id`
- `scope_hint`
- `output_mode`
- `use_llm`

输出要点：

- `intent`
- `scope`
- `anchors`
- `required_state`
- `needs_clarification`
- `clarification_question`
- `trace_id`

### 9.2 `POST /api/v1/find`

用途：

- 在已确定 scope 内找候选对象。
- 不输出最终事实答案。

输入要点：

- `query`
- `scope`
- `object_types`
- `anchors`
- `limit`

输出要点：

- `candidates`
- `scores`
- `matched_fields`
- `release_id`
- `trace_id`

### 9.3 `POST /api/v1/objects/get`

用途：

- 读取 object manifest 和 card。
- 默认不读取 L2。

输入要点：

- `uri`
- `release_id`, optional
- `trace_id`, optional

输出要点：

- `object_manifest`
- `object_card`
- `dependency_summary`
- `release_id`

说明：

- 不建议把 URI 直接放进 URL path 参数，因为 RTS stable URI 可能包含 `/`、`:` 或其他需要 escaping 的字符。
- 对象读取接口应使用 JSON body 或 query parameter 传递 `uri`，避免路由层错误截断。

### 9.4 `POST /api/v1/objects/content`

用途：

- 读取 L2 runtime content。

输入要点：

- `uri`
- `purpose`
- `release_id`, optional
- `trace_id`, optional

规则：

- 只允许读取 active projection 中登记的 content ref。
- 读取时校验 hash。
- 读取记录 trace。

### 9.5 `POST /api/v1/objects/dependencies`

用途：

- 查询 dependency edges。

输入要点：

- `uri`
- `direction`
- `edge_type`
- `depth`
- `purpose`

Day1 默认：

- depth 最大 1 或 2。
- 超过限制需要显式参数和权限。

### 9.6 `POST /api/v1/query`

用途：

- 执行结构化 query plan。
- 可以不调用 LLM。

输出：

- `facts`
- `objects`
- `dependencies`
- `unknowns`
- `refusal`
- `trace_id`

### 9.7 `POST /api/v1/ask`

用途：

- Controlled LLM Harness。
- 用自然语言问答，但 LLM 只能调用受控服务工具。

输入要点：

- `query`
- `caller_id`
- `scope_hint`
- `output_mode`
- `max_tool_calls`

输出要点：

- `answer`
- `facts`
- `inferences`
- `unknowns`
- `candidate_suggestions`
- `human_decisions`
- `cited_objects`
- `release_id`
- `trace_id`
- `refusal`

### 9.8 `GET /api/v1/traces/{trace_id}`

用途：

- 查询一次服务回答的 trace。

Day1 trace 至少展示：

- query text
- query plan
- release id
- scope
- candidates
- selected objects
- L2 reads
- refusal reason
- LLM tool calls if any

## 10. LLM Harness 工具契约

LLM 不直接调用内部 Java 方法，也不直接访问 store 或文件系统。它看到的是受控工具 schema。

### 10.1 `resolve_scope`

用途：

- 根据用户问题和 hint 解析 scope。

输入：

- `query`
- `scope_hint`

输出：

- `resolved_scope`
- `confidence`
- `needs_clarification`
- `clarification_question`
- `allowed_releases`

规则：

- scope 不清时必须澄清。
- 不允许用相似对象替代 scope。

### 10.2 `find_objects`

用途：

- 在 scope 内找 rule/lookup/helper 候选。

输入：

- `query`
- `scope`
- `anchors`
- `object_types`
- `limit`

输出：

- `candidates`
- `matched_fields`
- `scores`
- `warnings`

规则：

- 工具内部执行权限和 release gate。
- 返回候选不是最终 truth。

### 10.3 `get_object_card`

用途：

- 读取对象结构化 card。

输入：

- `uri`

输出：

- `object_card`
- `release_id`
- `state`

规则：

- card 只能用于导航和消歧。
- card 不能替代 L2 事实。

### 10.4 `read_object_l2`

用途：

- 读取最终事实对象。

输入：

- `uri`
- `purpose`

输出：

- `l2_content`
- `content_hash`
- `release_id`

规则：

- 没有 L2 就不能回答事实。
- 工具返回应标明 content hash。

### 10.5 `get_dependencies`

用途：

- 查询依赖。

输入：

- `uri`
- `direction`
- `edge_type`
- `depth`
- `purpose`

输出：

- `edges`
- `objects`
- `truncated`

规则：

- dependency traversal 不自动扩大 scope。
- 如果依赖未发布，返回 refusal/degraded reason。

### 10.6 `explain_trace`

用途：

- 给 LLM 或 caller 一个可读 trace 摘要。

输入：

- `trace_id`

输出：

- `release`
- `scope`
- `selected_objects`
- `l2_reads`
- `refusal`

## 11. LLM Harness 运行流程

### 11.1 Ask 请求流程

```text
User query
  -> create trace
  -> deterministic pre-parse
  -> LLM optional intent/scope extraction
  -> validate query plan
  -> resolve_scope tool
  -> if scope unclear: clarification/refusal
  -> find_objects tool
  -> get_object_card tool for top candidates
  -> choose candidate under deterministic constraints
  -> read_object_l2 tool
  -> get_dependencies tool when needed
  -> assemble grounded context
  -> LLM final answer
  -> citation/claim validation
  -> service response
```

### 11.2 Tool budget

Day1 建议限制：

- 最大 tool calls：6 到 10。
- 最大 L2 objects：3 到 5。
- 最大 dependency depth：1 或 2。
- 最大 final context tokens：可配置。
- 超限时返回 partial answer 或要求用户收窄 scope。

### 11.3 Prompt discipline

LLM system prompt 必须表达：

- 你是 RTS 服务回答组织器，不是 truth owner。
- 只能基于工具输出回答。
- 每个 material fact 必须来自 L2 或工具返回的 released object。
- 不确定就输出 unknown/refusal。
- 不能把候选建议写成事实。
- 不能跨 scope 使用相似规则。
- 不能引用未授权 evidence/review。
- 不能保存 memory。

### 11.4 Final answer validation

Day1 至少做轻量校验：

- final answer 是否包含 trace id。
- material facts 是否有 cited object。
- cited object 是否已读取 L2。
- output 是否包含 forbidden claim 类型。
- refusal 场景是否被 LLM 硬答绕过。

校验失败时：

- 重新要求 LLM 修正一次，或
- 返回 deterministic degraded answer，或
- 返回 refusal。

## 12. Query 行为细则

### 12.1 Rule lookup

问题示例：

```text
这个 target field 怎么生成？
rule_xxx 是什么逻辑？
```

流程：

- 解析 target/rule anchor。
- scope gate。
- exact target/rule lookup。
- 无 exact match 时 Lucene find。
- 读取 rule card。
- 读取 rule L2。
- 读取 required lookup/helper card，必要时读取 L2。
- 输出 rule fact。

### 12.2 Lookup/helper lookup

流程：

- 解析 lookup/helper id 或 business term。
- scope gate。
- exact lookup/helper lookup。
- Lucene find。
- 读取 card 和 L2。
- 输出 key columns、output columns、fallback、consumers。

### 12.3 Explain rule

流程：

- 定位 rule。
- 读取 rule L2。
- 查询 dependency edges。
- 读取必要 lookup/helper L2。
- 输出“规则做什么、依赖什么、适用条件、不适用条件、未知点”。

### 12.4 Impact preview

Day1 只做 preview，不做正式 release impact approval。

流程：

- 定位 source/lookup/helper/rule anchor。
- 反向查 consumer。
- 返回 impacted objects。
- 标明这是 impact candidates，不是最终人工裁决。

### 12.5 Coverage check

Day1 可以做基础覆盖检查：

- 某 scope 下有哪些 target path。
- 哪些 target path 没有 rule。
- 哪些 rule 缺 L2 或 dependency。

不做：

- 自动判断业务覆盖充分性。
- 自动 release gate。

## 13. Refusal Contract

Day1 必须拒答或要求澄清的场景：

- scope 不清
- caller 无权访问 scope
- active release 不存在
- active release 不唯一
- manifest 校验失败
- projection schema 不兼容
- object card 命中但 L2 不可读
- required dependency 未发布
- conflict / ambiguity 未解决
- COMMON 与 product-specific precedence 不明确
- 只有语义或 BM25 相似结果，没有结构化对象命中
- 查询要求 evidence/review，但 caller 无权限
- LLM tool budget 已耗尽且无法保证事实完整

Refusal 输出应包含：

- refusal type
- what is missing
- what user can provide
- whether partial candidates exist
- trace id

## 14. Output Schema

Day1 服务输出应结构化。

建议字段：

- `answer_type`: `answer | clarification | refusal | partial`
- `scope`
- `release_id`
- `facts`
- `inferences`
- `unknowns`
- `candidate_suggestions`
- `human_decisions`
- `cited_objects`
- `dependencies`
- `trace_id`
- `warnings`

### 14.1 Facts

事实必须满足：

- 来自 L2。
- 或来自 projection manifest / object manifest / dependency edge。
- 有 stable URI。
- 有 release id。

### 14.2 Inferences

推断必须明确标记。

例如：

- “基于依赖关系，这个 lookup 变化可能影响以下 rules。”

### 14.3 Unknowns

未知必须明确。

例如：

- “当前 scope 下没有 released L2 object 能证明该字段逻辑。”

### 14.4 Candidate suggestions

候选建议不能写成事实。

例如：

- “可让 reviewer 检查是否需要新增 test case。”

### 14.5 Human decisions

人工决定只来自 projection 中已授权暴露的 review/adjudication summary。

Day1 默认 operational query 可以不暴露 raw review。

## 15. MCP Adapter Day1 边界

Day1 可以提供最小 MCP adapter，但它应该只是 API 的工具包装。

建议 MCP tools：

- `rts_find_objects`
- `rts_read_object`
- `rts_get_dependencies`
- `rts_ask`
- `rts_get_trace`

MCP tools 必须遵守：

- caller profile
- scope gate
- release gate
- L2 read requirement
- no write to truth

不要提供：

- `rts_write_rule`
- `rts_update_projection`
- `rts_save_memory`
- `rts_publish_truth`

## 16. Day1 Test Plan

### 16.1 Projection ingestion tests

覆盖：

- valid projection loads
- missing manifest rejects
- invalid schema rejects
- hash mismatch rejects
- missing L2 rejects object or release
- dependency not released rejects release or marks blocking

### 16.2 Scope and permission tests

覆盖：

- caller can query allowed scope
- caller cannot see disallowed product
- same field name in another product is not returned
- missing scope asks clarification

### 16.3 Deterministic lookup tests

覆盖：

- URI lookup
- rule id lookup
- lookup/helper id lookup
- target path lookup
- source path lookup

### 16.4 Lucene retrieval tests

覆盖：

- BM25 finds correct card
- BM25 cannot cross scope
- alias boosts correct object
- deprecated object is penalized

### 16.5 L2 read tests

覆盖：

- L2 readable with hash check
- L2 missing causes refusal
- arbitrary path read is impossible

### 16.6 LLM harness tests

覆盖：

- LLM calls tools before answering
- LLM refuses when scope unresolved
- LLM does not answer facts without L2
- LLM separates facts/inference/unknowns
- LLM cannot use disallowed object
- final answer validation catches unsupported claim

### 16.7 Golden set

Day1 至少准备 20 到 50 条真实或接近真实的问题。

分类：

- exact target lookup
- fuzzy field lookup
- lookup/helper lookup
- dependency explanation
- impact preview
- ambiguous scope
- wrong product confusable
- missing L2
- unauthorized governance info

核心指标：

- exact anchor hit rate
- correct object top-1 / top-3
- refusal correctness
- unsupported claim count
- average latency
- trace completeness

## 17. Day1 Definition of Done

Day1 完成标准：

- 能 ingest approved runtime projection。
- 能校验 manifest/schema/hash/L2 refs。
- 能把 release/scope/object/card/dependency/content ref 写入 filesystem-backed projection store。
- 能建立 Lucene BM25 索引。
- 能通过 API 做 deterministic lookup。
- 能在 scope 内做 BM25 find。
- 能读取 L2 并校验 hash。
- 能做基础 dependency traversal。
- 能返回 trace。
- 能在拒答场景拒答。
- 能提供 `/ask` Controlled LLM Harness。
- LLM harness 只能调用受控工具。
- LLM final answer 有 citation/trace validation。
- Golden set 跑通。

## 18. Day1 主要风险和控制

### 风险 1：LLM 变成 truth owner

控制：

- LLM 无 DB/file direct access。
- LLM 只能调用工具。
- 工具强制 scope/release/L2 gate。
- final answer validation。

### 风险 2：第一版过重

控制：

- 不上 OpenSearch。
- 不上向量库。
- 不做完整 compiler。
- 不做完整 graph DB。
- 不做 UI。

### 风险 3：BM25 找不准

控制：

- 优先 deterministic lookup。
- card/search_text 由 KB projection 提供结构化字段。
- 建 alias/glossary。
- 用 golden set 调整字段 boost。

### 风险 4：scope 混淆

控制：

- scope 先于 recall。
- scope 不清就澄清。
- Lucene query 必须带 scope filter。
- trace 记录 rejected candidates。

### 风险 5：未来升级困难

控制：

- Lucene document schema 对齐未来 OpenSearch schema。
- ProjectionStore/TraceStore 接口对齐未来 PostgreSQL schema。
- ContentStore 抽象。
- LlmClient 抽象。
- QueryPlan schema version。
- Projection schema version。

## 19. Day1 不做清单

Day1 不做：

- OpenSearch
- PostgreSQL/Flyway 作为 Day1 必需组件
- vector database
- full graph database
- full DSL compiler
- autonomous multi-agent retrieval
- runtime memory
- raw evidence default query
- automatic KB writeback
- pipeline release gate
- UI workbench
- object-level independent publication

这些不是永久不做，而是留给 Day2 或更后续阶段。

## 20. 推荐实施顺序

1. 固定 Day1 API schema、QueryPlan schema、ServiceAnswer schema。
2. 搭 JDK 17 + Spring Boot 3.x 工程骨架。
3. 固定 `ProjectionStore`、`ScopeRegistry`、`TraceStore`、`CallerProfileStore`、`ContentStore` 接口。
4. 实现 filesystem-backed projection store 和 JSON/JSONL layout。
5. 实现 projection ingest 和 manifest validation。
6. 实现 object/card/dependency/content ref 存储。
7. 实现 local L2 content store。
8. 实现 deterministic lookup。
9. 实现 Lucene indexing 和 scoped BM25 find。
10. 实现 query trace。
11. 实现 dependency traversal。
12. 实现 `/query` 和 `/find`。
13. 实现 Controlled LLM Harness `/ask`。
14. 实现 final answer validation。
15. 实现最小 MCP adapter。
16. 跑 golden set 和 refusal tests。

## 21. 最终判断

Day1 的正确目标不是“做一个强大的 AI agent”，也不是“做一个完整企业搜索平台”。

Day1 的正确目标是：

**让 RTS 作为一个受治理 truth service 可以被 API/MCP/LLM 安全调用，并且每个事实答案都能回到 released projection 和 L2 object。**

只要这个边界做对，Day2 再增强 agentic retrieval、rerank、向量、negative retrieval 和更复杂的 impact/test planning 才有安全基础。
