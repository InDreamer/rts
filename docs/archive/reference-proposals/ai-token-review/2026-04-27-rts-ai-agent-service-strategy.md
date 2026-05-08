# RTS 作为 AI-agent 知识服务的产品与架构策略

> 日期：2026-04-27  
> 范围：战略 / 产品边界 / 架构形态调查  
> 依据：AGENTS.md、三篇 confirmed baseline docs，以及必要的 OV boundary / retrieval reference docs。

> 2026-05 对齐备注：本文是历史 proposal，保留了当时“runtime projection 剥离治理字段 / 不投影 evidence-review-reports”的旧口径。当前 confirmed baseline 已调整为：KB 和 projection 都是机器优先结构；projection 是 approved truth 的服务运行视图；operational view 默认低噪声，governance-authorized view 可通过权限和 trace 展开 evidence / review / report / adjudication summary 或 pointer。若本文与 confirmed docs 冲突，以 confirmed docs 为准。

## 1. Executive conclusion

**直接建议：RTS 不应被定位成“一个 AI agent app”。它应被定位成面向人和 agent 的“受治理真相服务”（Governed Truth Service / Governed Knowledge Service）。**

更完整的表述是：

> RTS 是一个把来源材料、业务判断和人工审核沉淀为受治理 canonical packs，并把已批准真相以可追踪、可检索、可被 agent 安全消费的方式服务出去的知识治理服务。

这比“AI agent app”更准确，因为 RTS 的核心资产不是聊天能力，而是**谁有权决定真相、真相如何形成、如何投影给 agent 使用**。

对三个现有 surface 的建议是：

| Surface | 是否纳入同一产品边界 | 是否合并成同一个内部层 | 建议 |
|---|---:|---:|---|
| Governed Knowledge Base / truth source | 是 | 否 | 作为 court of record，仍由 canonical pack + evidence + review + signoff 控制。 |
| Mapping / projection | 是 | 否 | 拆成两类：authoring mapping（来源到候选 canonical 对象）与 runtime projection（approved truth 到 `Knowledge-Bases/`）。 |
| Index / retrieval | 是 | 否 | 作为 agent-facing serving layer，严格只读取 approved projection，不决定真相。 |

**结论：可以是“一个产品 / 一个服务边界”，但不能是“一个混在一起的物理模块”。** 现有 Java index-layer 计划中 Truth Layer、Projection Layer、Index Layer、Query Layer 分责明确，这个边界应保留；新增的是上游的 human-agent workbench 与 candidate-building 能力，而不是把 truth、conversation、memory、vector index 混成一层。

最简单、最 coherent 的产品定义：

> **RTS Governed Knowledge Service：通过人机协作从证据中构建受治理知识，并通过 scoped、traceable API 向 downstream agents 提供 approved truth。**

## 2. Conceptual model

RTS 的目标模型应按“真相形成”和“真相消费”分开看。

```text
人类 / 业务材料 / 示例 / 聊天
        │
        ▼
Human-facing Workbench
  - 提取候选知识
  - 追踪证据
  - 澄清歧义
  - 形成待审 canonical 对象
        │
        ▼
Governed Truth Source
  - canonical packs
  - rules / lookups / helpers
  - evidence / review / ambiguity / signoff
        │ approved only
        ▼
Runtime Projection
  - stripped rules / lookups / helpers
  - Knowledge-Bases/ resource tree
        │
        ▼
Index / Retrieval / Query
  - URI / metadata / L0-L1-L2 / dependency hints / trace
        │
        ▼
Agent-facing Service Surface
  - REST / MCP / SDK / tools
  - scoped query / read / candidate submission
```

### 边界定义

| 概念 | 在 RTS 中的角色 | 关键边界 |
|---|---|---|
| Human-facing interaction surface | 帮人更低成本地理解材料、抽取规则、处理歧义、完成 review。 | conversation 只能产生 candidate，不直接产生 canonical truth。 |
| Agent-facing service surface | 让其他 agent 查询、读取、引用、提交候选问题。 | 默认只读 approved truth；写入只能进入 candidate / issue queue。 |
| Governed knowledge base / truth source | canonical pack objects 是唯一真相源。 | 真相不在 memory、chat、runtime session 或 vector chunks 中。 |
| Projection / mapping layer | 把原始材料映射为候选对象；把 approved canonical truth 投影为 runtime KB。 | mapping 可由 LLM 辅助，但 signoff 必须由治理流程完成。 |
| Index / retrieval layer | 帮 agent 找到和读取真相。 | retrieval 不决定 authoritative status。 |
| mem0 类 memory service | 可记住用户偏好、会话上下文、未完成任务。 | memory 不是证据，不是规则，不可写回 canonical truth。 |
| OpenViking 类 context shell | 可作为 projected truth 的 library / retrieval shell。 | OV 不拥有 canonical pack，不写回 truth，不处理 signoff。 |

需要特别固定一个术语区分：

- **Governed KB / Truth Packs**：canonical truth，含 evidence、review、ambiguity、signoff。
- **Runtime `Knowledge-Bases/`**：approved truth 的只读投影，供 agent 检索和读取。

如果继续把二者都叫 Knowledge Base，会导致“vector KB 是否就是 truth source”的误解。

## 3. Proposed architecture

建议采用**一个产品服务边界 + 模块化单体起步 + 可拆 worker** 的架构。初期可以是一个 Java/Spring 服务加前端 workbench；内部模块边界必须清楚，未来再按吞吐量拆成 ingestion、projection、indexing worker。

### 核心组件

| 组件 | 职责 | 是否可被 LLM 辅助 | 真相权限 |
|---|---|---:|---:|
| Conversation / Workbench layer | 人机协作入口，导入文档、聊天、示例，展示候选与歧义。 | 是 | 无 canonical commit 权。 |
| Evidence Registry | 记录来源、版本、片段、hash、引用位置、业务 owner。 | 是，做定位和摘要 | 证据登记，不等于真相结论。 |
| Candidate Knowledge Capture | 从材料中抽取 candidate rules/lookups/helpers、open questions、conflicts。 | 是 | candidate only。 |
| Structuring / Authoring Mapping | 将 candidate 映射到 `target_rule`、`lookup_definition`、`helper_definition`。 | 是，受 schema 校验 | 生成待审对象，不签署。 |
| Review / Ambiguity / Signoff Gates | 人工审核、歧义处理、批准、拒绝、版本冻结。 | 可辅助解释 diff | 决定 canonical truth。 |
| Canonical Pack Store | 保存 pack objects、evidence、review、reports、schema version。 | 读取辅助 | court of record。 |
| Runtime Projection | 只投影 signed-off 的 rules/lookups/helpers，剥离治理字段。 | 摘要可辅助但需校验 | 不改变 truth。 |
| Index / Retrieval | URI、metadata、L0/L1/L2、dependency hints、search index、query trace。 | rerank/summary 可辅助 | 不决定 truth。 |
| Agent Access Layer | REST/MCP/SDK/tooling，提供 query/read/trace/candidate submission。 | 可作为解释器 | 默认只读 approved truth。 |
| Audit / Observability | 记录 query、candidate、review、projection、index rebuild。 | 否 | 支撑治理与追责。 |

### 推荐数据流

```text
1. Source docs / chats / examples
   → Evidence Registry

2. Evidence Registry
   → Candidate Knowledge Capture
   → candidate rule / lookup / helper / ambiguity / conflict

3. Candidate objects
   → Structuring / Mapping
   → schema-valid draft canonical objects

4. Draft canonical objects
   → Review / ambiguity resolution / signoff
   → approved canonical pack objects

5. Approved canonical pack objects
   → Runtime Projection
   → Knowledge-Bases/{channel}/{product}/{pack}/rules|lookups|helpers

6. Runtime projection
   → L0/L1/L2 summaries, metadata, dependency edges, search index

7. Downstream agents
   → scoped query/read API
   → answer with URI, snapshot, retrieval trace, uncertainty status

8. Runtime observations / agent feedback
   → candidate submission or open item
   → never direct canonical mutation
```

### 关于 evidence / review 是否进入 runtime

基线文档已经明确第一阶段 runtime projection 不投影 `evidence / review / reports`。这个原则应保留。

但作为“一个 RTS 服务”时，可以提供两类读取模式：

- **Operational mode**：给普通 downstream agents，只读 approved projection，返回 canonical object URI、snapshot、retrieval trace。
- **Governance / audit mode**：给有权限的人或审核 agent，可读取 evidence、review、open items，用于解释为什么可信或为什么还不能批准。

这样既不污染运行时检索面，又能满足 truth construction 和审计场景。

## 4. Interaction design

RTS 的交互目标不是“让 AI 直接写知识库”，而是让人更快看懂材料、更快定位证据、更少漏掉歧义。

### 1. Guided extraction from documents / chats / examples

用户上传或引用一组材料后，Workbench 不应直接生成最终规则，而应生成一个 extraction board：

- 可能的 target field / lookup / helper
- 每条候选断言对应的 source span
- 候选对象所属 channel / product / pack
- 证据强弱、缺失点、冲突点
- 推荐的下一步澄清问题

输出状态应是 `candidate`，不是 `approved`。

### 2. Evidence-first drafting

每条 material assertion 必须先有 evidence，再有 rule statement：

```text
Evidence → extracted assertion → canonical field draft → reviewer decision
```

如果 LLM 只能给出“看起来应该如此”的推断，没有 evidence，就只能进入 `open_question` 或 `hypothesis`，不能进入 rule body。

### 3. Clarification loops

系统应主动把“不足以判断”的地方转成短问题，而不是产出长篇解释。例如：

- “该字段在 FXD_NDF 和 FXO 中是否共享同一 lookup？”
- “这个 cutoff time 是按 currency pair、participant count，还是 settlement center 决定？”
- “示例 A 与文档 B 冲突，应以哪个为准？”

澄清回答也必须被记录为 evidence 或 reviewer decision，不能只留在 chat transcript。

### 4. Ambiguity queue

所有 unresolved ambiguity 应进入队列，并带上：

- 影响的 pack / object
- blocked projection status
- 缺失证据
- 冲突证据
- owner
- severity
- due date / release impact（如需要）

这让“unknown is better than wrong”成为产品机制，而不是一句原则。

### 5. Review diff / signoff flow

审核界面应展示结构化 diff，而不是让 reviewer 读整段聊天：

- rule source / logic / target 有何变化
- dependencies 是否新增或删除
- examples 是否覆盖关键分支
- evidence refs 是否满足要求
- runtime projection 会新增 / 修改 / 删除哪些 URI

signoff 只作用于 canonical pack objects；聊天总结、LLM 草稿、memory notes 都不能被 signoff 为 truth。

### 6. “What changed in truth” summaries

每次 pack 或 snapshot 变化后，系统应生成 truth change summary：

- 新增、修改、废弃的 rules/lookups/helpers
- 影响的 target fields 和 dependency chain
- 是否改变 runtime projection
- 是否存在仍未解决的 ambiguity
- downstream agents 应使用的新 snapshot/version

这比普通 release notes 更重要，因为 agent 需要知道“真相边界”什么时候变了。

### 7. Agent query + trace answer flow

面向 agent 的回答流程应固定：

```text
query → scope resolution → L0 recall → L1 rerank/navigation → L2 object read → structured answer + trace
```

回答必须包括：

- answered scope
- pack/object URI
- snapshot/version
- dependencies read
- retrieval trace
- uncertainty status

如果 scope 不清、命中多个相似 pack、或没有 signed-off truth，正确输出是 clarification / unknown，而不是补一个流畅答案。

## 5. Access model for downstream agents

RTS 对其他 agent 的默认姿态应是：**read approved truth, cite it, and submit candidates; never rewrite truth.**

### Read / query APIs

建议第一批 API 保持少而硬：

| API / Tool | 用途 | 要求 |
|---|---|---|
| `POST /api/query` / `rts.query_truth` | 用自然语言问题查询 approved truth。 | 必须有 scope 或触发 scope clarification。 |
| `POST /api/find` / `rts.find_objects` | 查找 pack/object candidates。 | 返回候选、分数、scope、URI。 |
| `GET /api/resources/read` / `rts.read_object` | 读取指定 URI 的 L2 object。 | 只读 projection，带 snapshot。 |
| `GET /api/resources/overview` | 读取 L1 overview。 | 用于导航，不作为最终规则答案。 |
| `GET /api/resources/tree` | 遍历 channel/product/pack。 | 权限按 channel / product 控制。 |
| `GET /api/changes` / `rts.get_truth_changes` | 查询 snapshot 之间 truth changes。 | 给 agent 做缓存失效和影响分析。 |
| `POST /api/candidates` / `rts.submit_candidate` | 提交发现、疑问、可能的新规则。 | 只能进入 candidate queue，不能改 canonical。 |

### Scope and citation requirements

对所有 agent query，应强制：

- channel scope 优先；没有 scope 时先 resolve 或反问。
- 结果必须引用 URI，而不是只给自然语言答案。
- answer 应区分 `approved_truth`、`not_found`、`ambiguous_scope`、`not_signed_off`、`evidence_gap`。
- 多条候选相近时，不允许模型自行挑一个当真相；应返回 ambiguity 或要求更窄 scope。

### Write / candidate-submission boundary

agent 可以提交：

- 新来源材料
- 可能的新 rule / lookup / helper candidate
- 与已批准规则冲突的 observation
- 用户会话中出现的 open question
- 运行时失败案例或 test example

agent 不可以：

- 直接修改 canonical pack
- 直接改变 signoff status
- 把 mem0 / session memory 写回 truth
- 把 vector search chunk 当作 authoritative object
- 在没有 evidence 的情况下补齐业务逻辑

### MCP / tool packaging

MCP 适合作为 agent ecosystem 的标准入口，但工具名和描述必须把边界写死。例如：

```text
rts.query_truth(scope, question) -> traced approved answer or uncertainty
rts.read_truth_object(uri, snapshot) -> canonical projected object
rts.get_truth_changes(from_snapshot, to_snapshot, scope) -> change summary
rts.submit_candidate(scope, evidence, proposed_object) -> candidate id, never approved id
rts.open_ambiguity(scope, issue, evidence_refs) -> ambiguity id
```

所有 write-like tools 都应命名为 `submit`、`open`、`propose`，不要命名为 `create_rule`、`update_truth` 这类会误导 agent 的接口。

## 6. Relationship to mem0 and OpenViking

| 维度 | mem0 类服务 | OpenViking 类服务 | RTS |
|---|---|---|---|
| 核心定位 | 用户 / session memory 服务。 | agent context / resource retrieval shell。 | governed truth construction + truth serving service。 |
| 主要问题 | “这个用户 / agent 之前说过什么、偏好什么、做过什么？” | “agent 如何找到、加载、组织上下文？” | “什么经过证据和 signoff 后算真相，agent 如何安全读取？” |
| 是否拥有 canonical truth | 否。 | 否，在 RTS 架构内不能拥有。 | 是，canonical packs 是 court of record。 |
| 写入来源 | 会话、用户行为、agent memory extraction。 | 资源 ingestion、runtime context、可选 session flows。 | evidence + candidate + review + signoff workflow。 |
| LLM 角色 | 抽取和压缩 memory。 | 摘要、导航、检索辅助。 | 读取、抽取候选、解释、标记歧义；不签署 truth。 |
| 对 RTS 的价值 | 改善 workbench 体验，如记住用户偏好和任务进度。 | 可作为 approved projection 的 library / retrieval shell。 | 主系统，决定和服务 governed truth。 |
| 主要风险 | soft memory 被误当业务规则。 | context retrieval 被误当 truth authority，或 runtime 写回。 | 过度产品化后把 app、truth、retrieval 混成一层。 |
| 推荐关系 | 可选旁路，不进入 canonical path。 | 可选集成；也可被 Java index layer 替代。 | 坐在中心，向外服务 truth。 |

因此：

- RTS **不应替代 mem0**；二者解决不同层次。mem0 可辅助个人化和会话连续性，但不能进入 truth path。
- RTS **可替代或旁接 OpenViking 的 retrieval 职能**。如果 Java index layer 成熟，可以不依赖 OV；如果短期使用 OV，也只能消费 approved projection，且单向、只读、无 writeback。
- RTS 与二者的关系不是“谁是 agent platform”，而是“memory / context / truth”三类服务并列，各有治理等级。

## 7. Naming and product framing

建议避免一开始使用 “AI Agent App” 或 “Knowledge OS” 作为正式定位。前者低估治理，后者容易造成平台膨胀预期。

| 名称 / Label | 风格 | 优点 | 风险 |
|---|---|---|---|
| **RTS Governed Knowledge Service** | 保守企业名 | 准确表达服务边界、治理属性和 agent-facing 能力。 | 不够产品化，但最适合架构评审。 |
| **RTS Truth Service** | 简洁技术名 | 强调 RTS decides what is true。 | “truth” 语义强，需配合 evidence/signoff 解释。 |
| **Knowledge Governance Platform** | 企业平台名 | 适合多团队、多流程、多领域扩展。 | 对当前阶段偏大，可能引发 scope creep。 |
| **Truth Foundry** | 产品化名称 | 强调从材料“锻造”受治理知识，适合 workbench。 | 需要副标题解释其严肃治理边界。 |
| **Agent Knowledge Base Service** | agent-facing 名 | 容易让 downstream agent 团队理解怎么接入。 | 容易被误解成普通 RAG / vector KB。 |

推荐正式组合：

- 架构 / 企业语境：**RTS Governed Knowledge Service**
- 内部简称：**RTS Truth Service**
- 产品化 workbench 名称：**Truth Foundry Workbench** 或 **RTS Knowledge Workbench**

“Knowledge OS” 可以作为长期愿景词，但不建议作为当前 MVP 名称。

## 8. MVP / phased roadmap

### Phase 0：concept / docs boundary

| 项目 | 内容 |
|---|---|
| Goal | 冻结产品边界：RTS 是 governed knowledge service，不是通用 agent platform。 |
| Deliverables | 术语表、边界 ADR、canonical vs runtime KB 命名、API persona、non-goals、agent write policy。 |
| Out-of-scope | 代码实现、完整 workbench、mem0/OV 深度集成。 |
| Validation | owner 明确签署：one product boundary, separate internal layers；agents cannot commit truth。 |

### Phase 1：read-only truth serving from existing packs

| 项目 | 内容 |
|---|---|
| Goal | 让 agent 能通过 RTS 查询已批准 packs。 |
| Deliverables | approved input contract、runtime `Knowledge-Bases/` projection、URI、deterministic L0/L1、metadata、read/find/query API、retrieval trace。 |
| Out-of-scope | authoring workbench、candidate write API、自动 signoff、session memory。 |
| Validation | 固定 goldenset 查询能命中正确 pack/object；所有答案带 URI/snapshot/trace；未命中时返回 unknown。 |

### Phase 2：interactive candidate-building workbench

| 项目 | 内容 |
|---|---|
| Goal | 降低人从文档、聊天、示例中构建知识的理解成本。 |
| Deliverables | evidence registry、guided extraction、candidate board、schema-valid draft objects、clarification loop、ambiguity queue。 |
| Out-of-scope | 自动批准、复杂 BPM、外部 agent 大规模写入。 |
| Validation | SME 能更快产出待审对象；candidate 中每条 material assertion 都有 evidence 或 open question。 |

### Phase 3：review / signoff workflows

| 项目 | 内容 |
|---|---|
| Goal | 把 candidate 安全变成 canonical pack truth。 |
| Deliverables | review diff、approval gate、signoff status、pack versioning、audit trail、truth change summaries、projection trigger。 |
| Out-of-scope | 完整企业流程引擎、跨部门复杂审批矩阵、自动业务裁决。 |
| Validation | 只有 signed-off objects 进入 runtime projection；每次 truth change 可追踪到 reviewer、evidence、diff。 |

### Phase 4：agent ecosystem integration

| 项目 | 内容 |
|---|---|
| Goal | 让其他 agents 安全、标准化地使用 RTS。 |
| Deliverables | MCP tools、SDK、API token / OAuth scopes、agent usage policy、candidate submission API、optional mem0/OV adapters、query audit。 |
| Out-of-scope | 通用 agent orchestration platform、skills marketplace、runtime autonomous truth mutation。 |
| Validation | 至少两个 downstream agent/use case 通过 scoped API 使用 RTS，并能正确处理 citation、unknown、candidate submission。 |

## 9. Risks and design guardrails

| 风险 | 具体表现 | Guardrail |
|---|---|---|
| AI-generated false truth | LLM 从上下文中补业务逻辑，生成看似合理的规则。 | evidence-required schema；无证据只能进 open question；human signoff 才能 canonical。 |
| Conversational notes becoming canonical accidentally | 聊天记录、会议纪要、agent memory 被直接当成规则。 | transcript、candidate、canonical 三库分离；状态机禁止跨级直接提升；所有提升需 review diff。 |
| Vector retrieval overriding governance | 向量命中相似 chunk 后，agent 把相似内容当 authoritative。 | 搜索只覆盖 approved projection；强制 scope filter；最终答案只能来自 L2 object URI；返回 trace。 |
| Over-merging app/service concerns | workbench、truth store、index、agent runtime 耦合成一个不可维护平台。 | 一个产品边界，多个内部模块；Workbench 通过 API 消费 truth service；index 不写 truth。 |
| UX becoming too heavy | 审核表单和证据要求太重，用户绕开系统。 | progressive disclosure；先 candidate board，再按风险补证据；自动生成 diff 和 change summary；高风险字段才强 gate。 |
| Enterprise integration cost | 过早接入 BPM、SSO、OV、mem0、复杂 vector stack，拖慢 MVP。 | Phase 1 先 read-only；REST/MCP + existing auth；BM25/deterministic summary 可先行；外部集成作为 adapter。 |
| Governance data leaking into runtime | evidence/review/reports 进入普通 agent context，引入噪声或敏感信息。 | operational projection 只含 rules/lookups/helpers；governance/audit mode 单独授权。 |
| Product scope drifting into generic agent platform | 开始做 skills、memory、chatbot、workflow automation。 | non-goals 写入 Phase 0 ADR：RTS 不做 memory/session/agent orchestration；只做 truth construction and serving。 |

## 10. Decisions for the owner

建议 owner 按优先级做以下决策：

1. **确认定位**：采用 “RTS Governed Knowledge Service / RTS Truth Service”，不要把正式边界命名为 AI agent app。
2. **确认合并方式**：Knowledge Base、mapping/projection、index/retrieval 进入同一产品服务边界，但保持独立内部层；不把 truth、conversation、index 合成一层。
3. **确认写权限原则**：human/agent conversation 只能生成 candidate；canonical truth 只能通过 review/signoff 改变。
4. **确认术语**：区分 Governed Truth Packs 与 runtime `Knowledge-Bases/` projection，避免把 vector KB 当 truth source。
5. **确认 MVP 起点**：先做 Phase 1 read-only truth serving from existing approved packs，再做 interactive workbench。
6. **确认 API contract**：所有 agent answers 必须带 scope、URI、snapshot、trace、uncertainty；没有 approved truth 时返回 unknown。
7. **确认 OV/mem0 姿态**：mem0 只做旁路 memory；OV 只可作为只读 retrieval shell；Java index layer 是 RTS 自主路线。
8. **确认 governance read mode**：是否允许特定 audit/review agents 读取 evidence/review；普通 operational agents 默认不可见。
9. **确认第一个试点范围**：选择一个 channel/product/pack 集合作为 Phase 1 goldenset，定义 20-50 个真实问题验收。
10. **确认产品化名称**：企业评审用 RTS Governed Knowledge Service；workbench 如需更产品化，可用 Truth Foundry Workbench。
