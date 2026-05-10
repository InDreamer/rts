<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: RTS Constitution v2 for a rule truth service with multi-source governance, AI-first review, human final adjudication, permissioned service access, and runtime truth boundaries
read_when:
  - the request is about core RTS principles
  - the request is about AI-first governance or human final adjudication
  - the request is about API/MCP/pipeline/Q&A service boundaries
  - the request is about hallucination minimization by architecture
skip_when:
  - the request is only about a specific implementation detail
  - the request is only about historical OV adoption mechanics
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
-->

# RTS Constitution v2

## What This Covers

This document defines the constitutional rules for RTS as a dual-core rule truth service.

RTS exists to turn multi-source transformation knowledge into governed rule truth that can be safely used through its internal Knowledge Base, retrieval layer, truth-source atomic capabilities, managed LLM analysis service, APIs, MCP tools, Q&A surfaces, and system pipelines.

RTS includes LLM as a core internal capability. The LLM may retrieve, explain, analyze, detect conflicts, propose impact/test candidates, surface unsupported assertions, and express results based on governed Knowledge Base content, but an LLM answer is not automatically truth.

The goal is not maximum generative fluency. The goal is controlled truth creation, controlled truth publication, controlled truth consumption, and high-value AI analysis that remains inside explicit truth and authority boundaries.

## Constitutional Purpose

RTS protects five things:

- truth must come from traceable sources
- source conflicts must be exposed, not hidden
- AI should do as much governed review and analysis work as possible, but must not become the final truth owner
- human review is the final裁决 for material ambiguity and conflict
- LLM outputs, candidates, and investigation paths must be separated from approved truth and recorded human decisions
- service outputs must stay permissioned, traceable, state-aware, and useful enough to create measurable AI value within those boundaries

## Core Objects

RTS truth is built around three object groups that must not be separated in thinking:

- **Rule**: structured transformation logic for target fields, lookups, helpers, constants, conditions, and dependencies
- **Rule pack**: the governance boundary for related rules, lookups, helpers, evidence, review records, reports, and signoff state
- **Evidence chain**: the source-backed explanation of why a rule is trusted, disputed, incomplete, or superseded

A rule without evidence is not enough. Evidence without review is not enough. A pack without explicit state is not safe to publish as default service truth.

Canonical packs should keep main transformation objects separate from governance side layers. `rules/`, `lookups/`, and `helpers/` describe what the transformation does. `evidence/`, `review/`, and `reports/` explain why it is trusted, disputed, reviewed, or ready. Long evidence, review history, ambiguity discussion, and approval history should not be mixed into every main rule object.

## Constitutional Principles

### 1. Governed Review Establishes Default Service Truth

A rule becomes default service truth only after the relevant source material has been collected, AI review has exposed structure issues and conflicts, human review has裁决 material ambiguity, and signoff/publication state has been recorded.

Pack completion alone is not sufficient if material conflicts remain unresolved.

### 2. Truth Lives In Governed Objects, Not Runtime Behavior

The system of record is governed rule objects, evidence chains, review/adjudication records, and publication state.

Chat logs, prompt outputs, runtime memory, retrieval traces, and agent behavior do not redefine truth.

### 3. AI Is A Controlled Analysis-And-Expression Layer, Not A Final Authority

AI may:

- extract candidate rules from documents, code, XSLT, Excel, mapping tables, and other source material
- assemble draft rule packs
- check structural completeness
- cross-validate sources
- detect missing evidence, inconsistent logic, unsupported assertions, and ambiguity
- simplify reviewer questions and conflicts
- explain dependencies and non-obvious impact paths
- recommend confidence, risk, impact, test coverage, and next evidence needed
- explain signed truth under permission and scope constraints
- turn governed truth material into reviewer-friendly, agent-friendly, and pipeline-friendly analysis outputs

AI may not:

- invent missing business logic
- silently resolve material conflicts
- override human review裁决
- publish truth without signoff
- write runtime learning back into canonical truth
- use candidate or investigative output as if it were recorded human decision or approved truth

### 4. Human Review Is The Final裁决 For Material Conflict

When sources, rules, historical notes, or AI recommendations conflict, AI should expose and explain the conflict. The final decision must come from human review and must be recorded as governed adjudication.

A hidden model judgment is not a裁决.

### 5. Unknown Is Better Than Wrong

If available evidence is insufficient, RTS should return uncertainty, ambiguity, conflict-open, or not-enough-evidence rather than a polished unsupported answer.

Refusal or qualified output is a correct service behavior when truth state does not support a confident answer.

### 6. Evidence Outranks Narrative; Adjudication Outranks Raw Conflict

Every material rule assertion must be traceable to source evidence.

If a narrative explanation disagrees with evidence, the explanation must be corrected. If sources conflict, the recorded human adjudication determines the governed service truth for that scope.

### 7. Retrieval Tools Are Librarians, Not Truth Owners

Indexing, search, MCP tools, agent memory, vector retrieval, and OV-like systems may help locate and load truth. They must not own truth, certify truth, or mutate truth.

A retrieval result is only useful because it points back to governed rule objects, evidence, review state, and publication state.

### 8. Scope Partitioning Is A Truth Safety Boundary

Different source-target channels, products, packs, and rule scopes must be strongly separated in retrieval and service access.

This is not only an indexing optimization. It is an anti-contamination principle: similar rules from the wrong system are dangerous when presented as relevant truth.

### 9. Context Must Be Precise, Not Maximal

More context is not safer. Safer context is:

- within the correct channel/product/pack scope
- low noise
- structurally precise
- explicit about review and publication state
- traceable back to governed objects

Navigation and scope narrowing should precede full reads.

### 10. LLM Output Must Not Masquerade As Approved Truth

RTS outputs should distinguish facts, inferences, unknowns, candidate suggestions, and human decisions.

The LLM may produce useful answers and analysis, but the service must not present a candidate, hypothesis, retrieval result, or model answer as if it were an approved rule or final human decision.

### 11. Service Access Must Be Permissioned And State-Aware

RTS may expose facts, explanations, analysis, test planning, and workflow triggers. Access must depend on both caller permission and object state.

Examples:

- ordinary Q&A should primarily use signed or approved truth
- agents may receive rule IDs, dependencies, evidence references, and review state when authorized
- pipelines may trigger checks, reports, and publication gates
- governance users may inspect draft, conflict-open, AI-reviewed, adjudicated, deprecated, or superseded material

Permission is not only user login. It includes service entry point, data state, field visibility, action type, and audit requirement.

### 12. Publication Requires An Explicit Contract

Published service truth must have explicit release boundaries.

RTS must not silently overwrite the rules used by external callers. Every approved truth change should create a new released version or artifact, with recorded activation state and rollback target.

In the first safe stage, the pack is the default release unit: objects from a pack should not enter the default service surface until the pack has passed review/signoff as a whole.

Conflicts must be handled before release, not guessed at query time. This includes object identity conflicts, target coverage conflicts, URI/address conflicts, and COMMON vs product-specific precedence conflicts.

The service must refuse, degrade, or ask for clarification when truth state is insufficient: unresolved scope, unresolved conflict, unreleased dependency, missing active artifact, summary-only result without object truth, or unclear precedence.

L0/L1 navigation outputs affect retrieval behavior, so they must be versioned and reviewable enough to support regression and audit.

Every answer should trace back to canonical truth version, projection/release version, and query result.

Dependency hints may guide display and navigation, but must not silently widen scope or override approval state.

Permissions and retrieval scope must align: callers should not retrieve what they are not allowed to see.

Reopened ambiguity must create a new truth/release path, not an in-place repair of the current answer surface.

### 13. Evolution Requires Controlled Schema And Workflow Change

RTS may evolve, but schema, state, workflow, permissions, and publication semantics must change explicitly.

Silent drift weakens trust, breaks explainability, and makes AI outputs harder to audit.

## Default Output Discipline

When answering a rule or impact question, RTS should prefer outputs that:

- name the answered scope
- distinguish signed truth, draft material, conflict-open material, and unknowns
- cite rule IDs or stable object references
- expose evidence and review state when the caller is allowed to see them
- separate factual rule statements from AI analysis or recommendations
- label degraded information-service output explicitly when managed analysis is unavailable
- refuse or qualify conclusions when truth state is insufficient
- prevent candidate or inference output from being consumed as approval, release gate, root-cause closure, or human decision without an explicit deterministic or recorded-decision contract

## Service Non-Goals

RTS is not trying to:

- maximize free-form conversational fluency
- make LLMs final business decision makers
- treat all documents as equal truth sources
- collapse source ingestion, governance, publication, retrieval, and runtime memory into one uncontrolled surface
- let pipeline or agent integrations bypass review/signoff state
- let degraded continuity behavior redefine RTS product identity as “just retrieval”
- let candidate or inference outputs silently become automation decisions

## Decision Rule

When a future design choice is unclear, choose the option that best preserves governed truth, exposes source conflict, maximizes authorized AI value inside explicit safety boundaries, records human裁决, and keeps service outputs permissioned, state-aware, traceable, and reviewable.
