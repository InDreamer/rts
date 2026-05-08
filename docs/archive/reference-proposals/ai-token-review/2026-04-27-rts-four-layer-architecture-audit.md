# RTS 四层架构审计与画图指导

> 日期：2026-04-27  
> 范围：只读架构审计；用于绘制最终 RTS 四层架构图  
> 依据：`AGENTS.md`、三篇 confirmed baseline docs、`.hermes/reports/2026-04-27-rts-ai-agent-service-strategy.md`

## 1. Verdict

四层架构是正确的，且比“AI agent app / vector KB / 检索系统”更符合当前 repo baseline。

推荐保留这条核心句：

> **L1 决定什么是真的；L2 决定真相如何被候选化与发布；L3 决定已发布真相如何被找到；L4 决定人和 agent 如何使用真相。**

需要做四点精修：

1. **L1 不只是“KB 构建”，而是 Governed Truth / Canonical KB 构建层**：canonical truth 只在受治理 pack objects 中成立。
2. **L2 必须拆成两条不同通道**：Authoring Mapping（证据到候选）和 Runtime Projection（approved canonical 到 runtime KB）。两者方向不同，不能画成一个黑盒。
3. **L3 只能服务 approved runtime projection**：索引、向量、L0/L1/L2、query trace 都不拥有 truth authority。
4. **L4 是应用和访问表面，不是治理权威**：Review Console 在 L4，但 signoff 状态机和 canonical commit 权属于 L1。

一句话结论：**可以是一个 RTS Governed Knowledge Service 产品边界，但内部必须保持 Truth / Mapping-Projection / Index-Retrieval / Applications 四层分离。**

## 2. Corrected layer definitions

### L1 — 受治理真相 / Canonical KB 构建层

- **English name**：Governed Truth / Canonical Knowledge Base Construction Layer
- **Responsibility**：建立、保存、审核和版本化 canonical truth；作为 court of record。
- **Inputs**：
  - source materials / examples / business documents / conversation-derived evidence candidates
  - L2 Authoring Mapping 产出的 schema-valid candidate objects
  - reviewer decisions、ambiguity resolution、signoff decisions
- **Outputs**：
  - approved canonical pack snapshots
  - canonical `rules / lookups / helpers`
  - evidence refs、review records、ambiguity/open items、truth change records
  - projection trigger / approved input contract
- **Must not do**：
  - 不把 chat、agent memory、vector chunk 当 truth
  - 不让未审核 candidate 变成 approved rule
  - 不把 runtime projection 当 canonical store
  - 不让 LLM 自动 signoff 或补业务逻辑
- **Main subcomponents**：
  - Evidence Registry / Source Store
  - Canonical Pack Store
  - Candidate / Draft Object Store
  - Review / Ambiguity / Signoff Gates
  - Versioning / Truth Change Ledger
  - Governance Audit Trail

### L2 — 候选映射与运行时投影层

- **English name**：Mapping & Runtime Projection Layer
- **Responsibility**：执行“转换”，但不决定 truth；分为 authoring-time mapping 和 serving-time projection。
- **Inputs**：
  - Authoring Mapping：L1 evidence refs、source spans、uploaded materials、conversation extracts
  - Runtime Projection：L1 approved canonical pack snapshot
- **Outputs**：
  - Authoring Mapping：candidate `target_rule / lookup_definition / helper_definition`、open questions、conflict records
  - Runtime Projection：runtime `Knowledge-Bases/` resource tree、projection snapshot、stripped runtime YAML
- **Must not do**：
  - 不 signoff、不批准、不覆盖 canonical truth
  - 不把 evidence/review/reports 投影进普通 operational runtime KB
  - 不把 candidate 直接发布给 L3 operational index
  - 不在 projection 中重写业务语义
- **Main subcomponents**：
  - Authoring Mapping lane：Evidence Extractor、Candidate Capture、Schema Mapper、Ambiguity/Conflict Detector
  - Runtime Projection lane：Approved Snapshot Reader、Governance Field Stripper、KB Tree Generator、Projection Snapshot Writer

### L3 — 索引与可追踪检索层

- **English name**：Index & Traceable Retrieval Layer
- **Responsibility**：让 approved runtime truth 可寻址、可检索、可解释地被读取。
- **Inputs**：
  - L2 runtime `Knowledge-Bases/`
  - projection snapshot metadata
  - approved `rules / lookups / helpers`
- **Outputs**：
  - stable URI / metadata / resource tree
  - L0 recall summaries、L1 navigation overviews、L2 object reads
  - keyword / vector / hybrid search indexes
  - dependency hints
  - scoped query results with retrieval trace
- **Must not do**：
  - 不接管 canonical truth、review、signoff
  - 不索引未批准 candidate 作为 operational truth
  - 不让 vector similarity 覆盖 scope 和 approval status
  - 不把 query logs 或 retrieval traces 写回 truth
- **Main subcomponents**：
  - URI / Resource Metadata Registry
  - Scope Resolver
  - L0 / L1 / L2 View Builder
  - Search Index Builder
  - Dependency Edge Store
  - Query Engine / Result Shaper / Retrieval Trace

### L4 — 应用与访问层

- **English name**：Application & Access Layer / Human & Agent Applications
- **Responsibility**：提供人和 downstream agents 使用 RTS 的入口；把读、问、审、提交候选做成产品表面。
- **Inputs**：
  - human questions、agent queries、source uploads、candidate feedback
  - reviewer actions from UI
  - L3 traced retrieval results
- **Outputs**：
  - scoped answers with URI / snapshot / trace / uncertainty status
  - candidate submissions / open ambiguity submissions
  - review actions routed to L1 governance workflow
  - truth change views and audit query views
- **Must not do**：
  - 不直接修改 canonical packs
  - 不把 review UI 误画成 signoff authority 本身
  - 不把 conversation summary、mem0 memory、agent output 当 approved truth
  - 不在无 scope、无 citation、无 approved truth 时输出确定答案
- **Main subcomponents**：
  - Human Workbench / Candidate Board
  - Review Console / Truth Change View
  - Agent Query API / REST / MCP / SDK
  - Candidate Submission API
  - Business Q&A / Rule Explanation UI
  - Audit Query UI

## 3. Boundary fixes / important refinements

1. **Evidence Registry 应归 L1 所有**。L2 Authoring Mapping 可以读取 evidence、抽取 evidence span、生成 candidate，但 evidence registry 是治理基础，不应属于 mapping 黑盒。

2. **Review Console 在 L4；review/signoff 权威在 L1**。图上可以把 Review Console 画在应用层，但箭头必须进入 L1 的 Review / Signoff Gates；不能让 UI box 看起来能直接改 truth。

3. **Authoring Mapping 与 Runtime Projection 必须在 L2 内分成两条 lane**：
   - Authoring Mapping：evidence/source → candidate/draft/open question。
   - Runtime Projection：approved canonical → stripped runtime `Knowledge-Bases/`。
   它们共享“mapping/projection”能力，但处在 truth 生命周期的两侧。

4. **Ambiguity/conflict 的检测可在 L2，状态归 L1**。L2 可以发现歧义、冲突、证据缺口；持久化的 open item、blocked status、review disposition 应在 L1。

5. **Audit/logging 是横切能力，但 truth audit 要归 L1**。建议画一条横向 Audit / Observability rail：query log、projection job、index rebuild 是运行审计；review decision、signoff、truth change 是治理审计，后者才影响 truth boundary。

6. **L3 的 Query API 要区分“mechanism”和“surface”**。L3 负责 scope resolution、retrieval trace、result shaping；L4 的 Agent Query API / MCP / SDK 是访问 façade。

7. **Runtime `Knowledge-Bases/` 不是 canonical KB**。它只是 approved truth 的只读投影；第一阶段 operational mode 只含 `rules / lookups / helpers`，不含 `evidence / review / reports`。

8. **mem0 和 OpenViking 只能画成 sidecar**。mem0 是 session/user memory sidecar；OpenViking 若使用，也只是 read-only context shell。二者都不在 canonical truth path 上。

## 4. Recommended data flows for the diagram

### A. Truth construction flow

1. `External source docs / examples / chats` → `L4 Workbench`  
   Label：`upload / import / capture source`

2. `L4 Workbench` → `L1 Evidence Registry`  
   Label：`register evidence: source/version/span/hash/owner; not truth yet`

3. `L1 Evidence Registry` → `L2 Authoring Mapping`  
   Label：`evidence refs + source spans`

4. `L2 Authoring Mapping` → `L1 Candidate / Draft / Ambiguity Queue`  
   Label：`schema-valid candidate + evidence refs + conflicts/open questions`

5. `L1 Candidate / Draft / Ambiguity Queue` → `L1 Review / Signoff Gates`  
   Label：`human review / ambiguity resolution`

6. `L1 Review / Signoff Gates` → `L1 Approved Canonical Pack Snapshot`  
   Label：`approved truth + version + change record`

### B. Truth serving flow

1. `L1 Approved Canonical Pack Snapshot` → `L2 Runtime Projection`  
   Label：`approved only`

2. `L2 Runtime Projection` → `Runtime Knowledge-Bases/`  
   Label：`strip governance fields; rules/lookups/helpers only`

3. `Runtime Knowledge-Bases/` → `L3 Index Builder`  
   Label：`build URI + metadata + L0/L1/L2 + dependency hints + search index`

4. `L3 Query / Retrieval` → `L4 Agent API / Workbench`  
   Label：`scoped query/read result with URI + snapshot + trace + uncertainty`

5. `L4 Agent API / Workbench` → `Human / Downstream Agent`  
   Label：`answer / explanation / citation`

### C. Agent candidate feedback flow

1. `Downstream Agent / Human` → `L4 Candidate Submission API`  
   Label：`observation / failed case / proposed rule / open question`

2. `L4 Candidate Submission API` → `L1 Evidence Registry or Open Item Store`  
   Label：`record as evidence candidate or issue; not canonical`

3. `L1 Evidence/Open Item` → `L2 Authoring Mapping`  
   Label：`optional candidate drafting`

4. `L2 Authoring Mapping` → `L1 Review / Signoff Gates`  
   Label：`candidate only; requires review`

5. `L1 Approved Snapshot` → `L2 Runtime Projection`  
   Label：`only after signoff`

### D. Sidecar memory/context flow

1. `L4 Workbench / Agent Session` ⇄ `mem0 sidecar`  
   Label：`user preference / task continuity / session memory only`

2. `L2 Runtime Projection or L3 Index` → `optional OpenViking context shell`  
   Label：`read-only approved projection`

3. `OpenViking context shell` → `L4 Agent Applications`  
   Label：`context retrieval; no truth ownership`

Use dashed gray arrows for all sidecar flows.

### E. Forbidden writeback flow

Draw red blocked arrows or red “X” callouts:

- `mem0 / session memory` -X→ `L1 Canonical Packs`
- `chat transcript / conversation summary` -X→ `L1 Approved Truth`
- `L3 vector chunks / retrieval hits` -X→ `L1 Signoff / Canonical Truth`
- `OpenViking runtime` -X→ `L1 Canonical Packs`
- `candidate / draft object` -X→ `L2 Runtime Projection / L3 Operational Index`
- `raw evidence` -X→ `approved rule without review/signoff`
- `L4 Agent API` -X→ `direct update_truth / create_rule`

## 5. Diagram content spec

### Title

**RTS Governed Knowledge Service：从证据到受治理真相，再到可追踪检索服务**

### Layer order top-to-bottom

1. **L4 Application & Access Layer / 应用与访问层**
2. **L3 Index & Traceable Retrieval Layer / 索引与可追踪检索层**
3. **L2 Mapping & Runtime Projection Layer / 候选映射与运行时投影层**
4. **L1 Governed Truth / Canonical KB Construction Layer / 受治理真相构建层**

建议把 L1 放最底部作为 foundation；truth serving arrows 从底部向上，truth construction/candidate arrows 从顶部向下。

### Boxes in each layer

- **L4**：
  - Human Workbench / Candidate Board
  - Review Console / Truth Change View
  - Agent Query API / REST / MCP / SDK
  - Candidate Submission API
  - Business Q&A / Rule Explanation
  - Audit Query UI

- **L3**：
  - Scope Resolver
  - URI / Resource Metadata Registry
  - L0 Recall / L1 Navigation / L2 Object Read
  - Keyword / Vector / Hybrid Search Index
  - Dependency Hints
  - Query Engine / Retrieval Trace / Result Shaping

- **L2**：画成两个 side-by-side lanes。
  - **Authoring Mapping**：Evidence Extractor、Candidate Capture、Schema Mapper、Ambiguity/Conflict Detector
  - **Runtime Projection**：Approved Snapshot Reader、Governance Field Stripper、`Knowledge-Bases/` Generator、Projection Snapshot Writer

- **L1**：
  - Evidence Registry / Source Store
  - Candidate / Draft Object Store
  - Canonical Pack Store
  - Rules / Lookups / Helpers
  - Review / Ambiguity / Signoff Gates
  - Versioning / Truth Change Ledger
  - Governance Audit Trail

### Arrow labels

Use these exact labels where possible:

- `register evidence; not truth yet`
- `map evidence to candidate; no signoff`
- `submit candidate / open ambiguity`
- `human review + signoff`
- `approved canonical snapshot`
- `approved only`
- `strip governance fields`
- `runtime KB: rules/lookups/helpers only`
- `build URI + L0/L1/L2 + search index`
- `scoped query/read`
- `answer with URI + snapshot + trace + uncertainty`
- `candidate feedback only`
- `no direct writeback to truth`

### Guardrail callouts

Put these as bold callouts on the diagram:

1. **Canonical truth lives only in L1 governed pack objects.**
2. **Conversation, memory, runtime session, and vector chunks are not truth.**
3. **L2/L3/L4 serve or propose truth; they do not approve truth.**
4. **Only approved canonical snapshots enter runtime projection.**
5. **Operational runtime projection excludes evidence/review/reports.**
6. **Unknown / ambiguous is a valid outcome; unsupported certainty is not.**

### Color / legend suggestions

- **L1**：dark blue or gold — authority / governed truth
- **L2**：green — transformation / projection
- **L3**：purple or teal — index / retrieval
- **L4**：orange — human and agent applications
- **Sidecars**：gray dashed boxes — optional memory/context helpers
- **Approved truth path**：solid green arrows
- **Candidate / feedback path**：dashed blue arrows
- **Forbidden writeback**：red blocked arrows or red X
- **Audit / Observability**：thin horizontal rail across layers; distinguish governance audit from runtime logs

## 6. Short naming recommendation

### Overall product/service

Recommended formal name：**RTS Governed Knowledge Service**

Recommended short technical name：**RTS Truth Service**

Avoid as primary label：**AI Agent App**、**Vector Knowledge Base**、**Generic Agent Platform**。

### Final four layer labels

1. **L1 — Governed Truth / Canonical KB Construction Layer**  
   中文：**受治理真相 / Canonical KB 构建层**

2. **L2 — Mapping & Runtime Projection Layer**  
   中文：**候选映射与运行时投影层**

3. **L3 — Index & Traceable Retrieval Layer**  
   中文：**索引与可追踪检索层**

4. **L4 — Application & Access Layer**  
   中文：**应用与访问层**

## 7. Completion

Report complete: YES
