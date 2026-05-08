# External Review Final

> Date: 2026-04-20
> Status: archived full review draft；当前默认入口已压缩为 reference summary
> Archived: 2026-05-08
> Current summary: `docs/reference/external-review-final-2026-04-20.md`
> Purpose: save the current critical review as a standalone document for later continuation

## One-Line Conclusion

The direction is correct, but the scheme still lacks five hard constraints that determine whether it will remain trustworthy in implementation: `projection publish contract`, `projection lifecycle`, `release admission granularity`, `conflict/precedence model`, and `refusal contract`.

## Overall Conclusion

Conclusion level: can proceed, but key decisions must be fixed first.

The problem is not that the principles are unclear. The problem is that the principles have not yet been translated into sufficiently hard engineering constraints. The current documents already explain what should not be done, but they still do not guarantee that the team can only implement the safe behavior.

## P0

### 1. Projection artifact lacks a complete release contract

Problem: the current scheme treats projection as something generated and used at runtime, but it has not yet been defined as a formal release unit that is releasable, traceable, revocable, and immutable.

Evidence type: baseline has principles, but lacks engineering enforcement.

Why this is project-specific: the runtime surface here is not a normal cache. It is the only runtime projection of governed truth. If the artifact identity is incomplete, runtime will no longer be reading "the projection of an approved truth revision", but merely "some current file".

3-month consequence: the team starts generating projections, but cannot answer which truth revision produced a given runtime artifact.

6-month consequence: regression, blame, and audit can only point to vague time windows instead of concrete released objects.

12-month consequence: runtime trust collapses because the relationship between truth and projection is no longer provable.

Minimum corrective action: define a `projection publish manifest` containing at least `source revision`, `projection schema version`, `L0/L1 version`, `generated_at`, `generated_by`, `released_by`, `activation state`, `superseded_by`, and `rollback target`.

Impact on phase 1: yes.

### 2. Projection lifecycle after canonical changes is undefined

Problem: the scheme does not yet define how a canonical change creates a new artifact, how activation switches, and how old artifacts are retired.

Evidence type: baseline explicit statement plus cross-document tension.

Why this is project-specific: this project cannot accept an implementation that simply overwrites "the latest projection", because the runtime surface itself must remain traceable.

3-month consequence: dev environments drift because a projection appears stable while its contents have changed.

6-month consequence: incremental sync and rollback begin to conflict, and query results drift over time.

12-month consequence: production can only recover confidence through full rebuilds.

Minimum corrective action: enforce `new immutable artifact + atomic activation switch`; old artifacts may only become `revoked`, `retired`, or `superseded`, never silently overwritten.

Impact on phase 1: yes.

### 3. Release admission contract between pack-level signoff and object-level publishing is undefined

Problem: the scheme does not clearly define whether the release unit is the whole pack or individual objects.

Evidence type: cross-document tension.

Why this is project-specific: the governance unit is the pack, but runtime consumption may descend to object level. If admission granularity is not fixed first, release, rollback, conflict detection, and audit will all become distorted.

3-month consequence: different implementers adopt different admission rules.

6-month consequence: objects from partially approved packs enter projection, creating a half-approved runtime surface.

12-month consequence: the governance boundary erodes and the pack stops functioning as a reliable truth container.

Minimum corrective action: choose a single phase-1 rule. Recommended: no object enters released projection until the entire pack passes signoff.

Impact on phase 1: yes.

### 4. Conflict and precedence model is missing

Problem: the scheme does not formalize conflict handling, and it does not turn `COMMON` versus product pack precedence into a hard rule.

Evidence type: cross-document tension.

Why this is project-specific: this project naturally contains a reusable common layer plus product-specific layers that can cover overlapping target structures. Without a precedence matrix, implementation will invent default behavior.

3-month consequence: the same target or object id is accepted from more than one pack.

6-month consequence: identical queries return different sources and the team begins relying on informal precedence habits.

12-month consequence: supersession semantics fail and `COMMON` turns into an implicit fallback layer that mutates runtime truth behavior.

Minimum corrective action: explicitly define and enforce four conflict classes: `object id conflict`, `target coverage conflict`, `URI/address conflict`, and `COMMON vs product-layer override conflict`. Conflicts must fail before release, not be resolved dynamically at query time.

Impact on phase 1: yes.

### 5. "Unknown is better than wrong" has not been translated into a refusal contract

Problem: the scheme contains the principle against unsupported answers, but does not specify when the query layer must refuse.

Evidence type: baseline explicit principle without engineering enforcement.

Why this is project-specific: this is not a chat assistant. Query results will be treated as constrained truth reads. Without a refusal contract, the principle will degrade into "try to answer anyway".

3-month consequence: the query layer starts using "closest candidate" in place of "provable candidate".

6-month consequence: wrong explanations and wrong navigation become mixed together.

12-month consequence: the system remains superficially usable while regularly emitting unauditable half-true answers.

Minimum corrective action: define a formal refusal policy. At minimum, refuse when `scope is unresolved`, `active artifact is not unique`, `candidate conflicts remain`, `required dependency is not released`, `signoff granularity is insufficient`, `precedence is unclear`, or `only summaries exist without object truth`.

Impact on phase 1: yes.

## P1

### 6. L0/L1 are not yet formal governed outputs

Problem: the scheme treats L0/L1 as high-value navigation layers, but versioning, acceptance, and regression are not yet complete.

Evidence type: cross-document tension.

Why this is project-specific: L0/L1 here are not generic search summaries. They are structural inputs to runtime navigation and reranking. If their quality is not verifiable, they become a new hallucination surface.

3-month consequence: summary quality varies across batches.

6-month consequence: identical queries may be misrouted before L2 is even opened.

12-month consequence: the team stops trusting summary layers and falls back to full-object reading.

Minimum corrective action: define L0/L1 as independently versioned outputs with acceptance criteria and a regression sample set.

Impact on phase 1: yes, but manageable.

### 7. Retrieval routing contract is still too soft

Problem: the scheme describes layered retrieval, but does not fix whether phase 1 starts with deterministic narrowing or semantic recall.

Evidence type: cross-document tension.

Why this is project-specific: the project emphasizes scope and governance. It is not appropriate to let semantic recall lead and then repair boundaries later.

3-month consequence: two incompatible entry paths appear in implementation.

6-month consequence: scope narrowing weakens and adjacent packs become over-recalled.

12-month consequence: the query layer behaves more like generic RAG than a governed truth reader.

Minimum corrective action: phase 1 must use `deterministic metadata narrowing -> L1 navigation -> L2 object read`; vector recall must not be the main entry path.

Impact on phase 1: yes.

### 8. Dependency hint responsibility is unclear and can easily become a recall signal

Problem: the scheme does not hard-limit dependency hints to display/navigation versus ranking/expansion.

Evidence type: reference supplement plus cross-document tension.

Why this is project-specific: dependencies here are governed relations, not general relevance features. If hints affect ranking or scope expansion, controlled dependency becomes speculative retrieval.

3-month consequence: engineers wire hint data into ranking to improve apparent hit rate.

6-month consequence: dependency relations begin to widen retrieval scope.

12-month consequence: runtime behavior reintroduces hidden inference paths.

Minimum corrective action: in phase 1, `dependency hint` is display and navigation only. It must not rank, rerank, or expand retrieval scope.

Impact on phase 1: no. This is a tightening decision.

### 9. Consistency model for derived stores is missing

Problem: the scheme introduces multiple derived stores but does not define rebuild, incremental sync, dependency invalidation, and backfill semantics.

Evidence type: baseline has goals but lacks key decisions.

Why this is project-specific: these stores are not truth owners, but together they determine the runtime surface. Without a consistency model, each derived layer can be locally correct while the whole system is wrong.

3-month consequence: incremental publish occasionally becomes inconsistent.

6-month consequence: one layer switches to the new artifact while another still points at the old one.

12-month consequence: operations fall back to full rebuilds because partial trust is impossible.

Minimum corrective action: define a single publish flow with idempotent semantics, and explicitly state which layers must switch atomically and which may lag.

Impact on phase 1: yes.

### 10. Trace model does not yet connect canonical, projection, and query

Problem: logs, audit, and trace are mentioned, but not yet unified into one provenance chain.

Evidence type: baseline principles without engineering enforcement.

Why this is project-specific: one of the main product values is being able to answer why a result is trustworthy. If trace breaks anywhere, the system can only prove activity, not credibility.

3-month consequence: the system can show what was found, but not why it was found.

6-month consequence: incident investigation requires manually stitching query logs and release records.

12-month consequence: audit cannot answer which approved truth backed a specific runtime result.

Minimum corrective action: use a unified trace key that connects `canonical revision -> projection artifact -> query result`.

Impact on phase 1: yes, but a lightweight version is sufficient.

### 11. Permission boundaries and retrieval boundaries are not aligned

Problem: the scheme emphasizes scope, but permission granularity still does not clearly land at channel, product, pack, or object.

Evidence type: baseline lacks a key decision.

Why this is project-specific: scope here is not just a technical filter. It is part of the governed runtime visibility boundary. If permission and retrieval drift apart, the system can retrieve what it should not expose.

3-month consequence: dev environments expose objects that are query-visible but not governance-ready.

6-month consequence: different callers see inconsistent pack sets.

12-month consequence: runtime surface leakage becomes an organizational problem rather than a pure implementation bug.

Minimum corrective action: in phase 1, align permission and retrieval at pack level with explicit inheritance rules.

Impact on phase 1: yes, but this is scope reduction.

## P2

### 12. Ambiguity reopen/reproject contract is not formalized

Problem: ambiguity is treated as a first-class state, but the scheme does not define how it is reopened and how reopening affects projection.

Evidence type: baseline principle without a full decision.

Why this is project-specific: ambiguity here is not a side note. It is part of the truth boundary and must influence release eligibility.

3-month consequence: reopen is handled informally in comments or process memory.

6-month consequence: canonical and projection drift on the status of the same ambiguity.

12-month consequence: ambiguity is treated as documentation noise instead of a release condition.

Minimum corrective action: require ambiguity changes to create a new canonical revision and a new projection publish path.

Impact on phase 1: can be deferred, but not for long.

### 13. Schema evolution is still not fully layered

Problem: the scheme distinguishes canonical and runtime projection, but schema evolution is not yet fully split across canonical, projection, and summary layers.

Evidence type: baseline lacks a key decision.

Why this is project-specific: the system has both governed truth objects and governed navigation objects. If the three schema layers are not separated, any field addition can silently become a runtime behavior change.

3-month consequence: small field changes unexpectedly alter summary or retrieval behavior.

6-month consequence: upgrades require full rebuild experiments rather than controlled rollout.

12-month consequence: version evolution becomes high risk.

Minimum corrective action: separate `canonical schema version`, `projection schema version`, and `summary schema version`.

Impact on phase 1: manageable.

### 14. Failure operations are not yet in design scope

Problem: the scheme does not yet define the minimum operational actions for failed publish, partial sync success, and rollback drills.

Evidence type: inference.

Why this is project-specific: once projection becomes a release artifact, operational failure is not a normal task failure. It is a failed publication of the truth surface.

3-month consequence: failures are handled manually and inconsistently.

6-month consequence: the team becomes afraid to release.

12-month consequence: the system freezes into a low-change mode because release confidence is gone.

Minimum corrective action: add a minimal failure playbook covering failed publish, partial sync, and rollback.

Impact on phase 1: can be deferred.

## Five Most Likely Misimplementations

1. Projection is implemented as mutable records instead of immutable release artifacts.
2. Object-level partial release is treated as a default capability instead of an explicit governance choice.
3. `COMMON` is treated as an implicit fallback instead of a controlled precedence layer.
4. Dependency hints are treated as ranking features to improve hit rate.
5. Canonical changes overwrite the current projection in place instead of creating a new artifact and activating it atomically.

## Minimum Required Design Artifacts

1. Projection publish manifest
2. Projection release state machine
3. Release admission rule
   Clarify whether release is pack-level or object-level
4. Conflict and precedence matrix
   Cover `COMMON`, product layer, supersession, target override, and URI conflict
5. Query refusal policy
6. Retrieval routing contract
7. Canonical revision to active projection mapping model
8. Unified provenance/trace schema
9. Derived stores consistency model
10. L0/L1 acceptance checklist and minimum regression sample set
11. Permission and scope alignment rule
12. Ambiguity reopen/reproject contract

## More Conservative Phase-1 Plan

1. The only truth owner remains the governed pack. No database, search index, metadata store, or object store may become the truth owner.
2. Phase 1 release unit is the full pack. Object-level independent release is out of scope.
3. Phase 1 query entry path is `deterministic metadata narrowing -> L1 navigation -> L2 object read`.
4. Phase 1 does not enable vector recall, hint-based ranking, or cross-pack rerank.
5. `COMMON` may only influence results through an explicit precedence matrix. It must not behave as an implicit fallback.
6. Dependency hints in phase 1 are display/navigation only. They do not rank and do not widen scope.
7. Every canonical change must create a new immutable projection artifact and switch activation atomically.
8. Each pack may have at most one active released projection artifact at any time.
9. Conflict detection must run before release. Conflicts must not be deferred to query time.
10. Query must refuse when scope is unresolved, candidate conflicts remain, the active artifact is missing, or required dependencies are not released.
11. L0/L1 remain in phase 1, but as reviewed, versioned navigation outputs rather than free-form generated summaries.
12. Derived stores must be rebuildable and publish flow must be idempotent. Phase 1 keeps only the minimum necessary metadata and artifact layers.
13. Phase 1 trace only needs to connect `canonical revision -> projection artifact -> query result`.
14. Phase 1 permissions align to pack level and match retrieval scope. Finer granularity is deferred.
15. Ambiguity close, reopen, and reproject must all go through a new revision and a new release flow. In-place repair is not allowed.
