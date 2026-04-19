<!-- docmeta
role: leaf
layer: 3
parent: docs/transformation-rule-system/INDEX.md
children: []
summary: define the clean boundary between the transformation rule system truth source and OpenViking, and list the safe surfaces to borrow
read_when:
  - the request is about whether OpenViking should be merged, adopted, or constrained
  - the request is about conflict or alignment between the two system ideas
  - the request is about what can be safely borrowed from OV
skip_when:
  - the request is only about the formal constitution text
  - the request is already narrowed to the smallest OV integration plan
source_of_truth:
  - transformation-rule-system-handoff-2026-04-16/HANDOFF-PROMPT.md
  - docs/transformation-rule-system/ov-boundary-and-adoption.md
-->

# OV Boundary And Adoptable Surfaces

## What This Covers

This document draws the boundary between the Transformation Rule System and OpenViking. It explains where the two ideas align, where they conflict, and which OV capabilities are worth borrowing without giving up truth-source control.

## Source Boundaries

- `transformation-rule-system-handoff-2026-04-16/HANDOFF-PROMPT.md` — defines the governed pack model and truth-source workflow.
- This document — defines the conceptual boundary and adoption posture.

## First Principle

The two systems are not peers solving the same problem.

- The Transformation Rule System decides **what is true**.
- OpenViking decides **how an agent finds and loads context**.

That difference must stay intact.

## Where The Ideas Align

### 1. Both Prefer Structured Knowledge Over Raw Dumps

Neither system wants the LLM to ingest arbitrary source material without shape. Both benefit from explicit object boundaries and discoverable organization.

### 2. Both Need Progressive Reading

Your system already wants AI-readable and low-noise navigation. OV's layered reading model reinforces the same instinct: navigate first, dive later.

### 3. Both Benefit From Stable Addresses

Your packs and runtime objects naturally want stable identifiers. OV's URI model is useful here because it turns knowledge traversal into an explicit path problem instead of a vague search problem. In the current narrowed runtime projection, the first-phase addressable nodes are pack, rule, lookup, and helper.

### 4. Both Need Context Retrieval To Be Inspectable

You care about explainability because banking truth matters. OV's resource tree, relations, and layered retrieval support that same preference for visible structure.

## Where The Ideas Conflict

### 1. Truth Source Versus Context Container

Your system is a governed truth source. OV is a generalized context database. A context database does not inherently understand what counts as canonical banking truth.

### 2. Governance Lifecycle Versus Runtime Lifecycle

Your system revolves around authoring, review, signoff, ambiguity, and approval. OV revolves around resource ingestion, retrieval, session commit, and runtime memory.

### 3. Frozen Canonical Pack Versus Evolving Runtime Memory

Your completed pack should stabilize into goldensource. OV includes session and memory flows that are intentionally dynamic. That is useful for runtime learning but dangerous near canonical rule content.

### 4. Explicit Ambiguity Versus Convenience Retrieval

Your system treats ambiguity as a first-class state. A generic retrieval platform may optimize for useful context delivery rather than strict uncertainty preservation unless that boundary is designed in.

## Hard Boundary

### The Transformation Rule System Must Keep Control Of

- canonical pack storage
- canonical schema evolution
- evidence requirements
- review structure
- ambiguity state
- signoff state
- approval state
- final truth determination

### OpenViking Must Not Control

- whether a rule is authoritative
- whether missing evidence may be inferred away
- whether runtime observations rewrite canonical rules
- whether an ambiguous item is treated as resolved

## Safe OV Surfaces To Borrow

### 1. URI-Like Addressing

Borrow the idea of stable addressable nodes for:

- pack
- rule
- lookup
- helper

The exact URI syntax may be yours, but the addressability discipline is worth copying. Evidence, review, and reports may keep identifiers on the governed side, but they are not first-phase OV runtime nodes.

### 2. Layered Reading

Borrow the `navigate before full read` model:

- short pack abstract
- pack overview or navigation page
- detailed object content

This is one of the highest-value, lowest-risk adoptions because it directly reduces noise and therefore reduces hallucination.

### 3. Optional Lightweight Relations

If relation support is added later, start small:

- pack -> object
- object -> dependency
- pack -> related pack

Do not treat evidence or review linkage as a first-phase runtime requirement. The current design keeps relations as a deferred optimization rather than a prerequisite.

### 4. Resource Projection

Borrow OV's resource ingestion and retrieval idea, but only for projected copies of approved packs, and only for stripped runtime objects: rules, lookups, and helpers. That gives agents a retrieval shell without relocating your truth source or replaying approval artifacts at runtime.

### 5. Find/Search Split

Borrow the concept of:

- a cheaper deterministic lookup path for narrow questions
- a more context-aware retrieval path for broad task questions

That split matches your need to avoid overloading every query with full semantic search.

## OV Surfaces That Should Stay Out Of Canonical Truth

### 1. Session Memory

Session memory may help future work, but it must not be allowed to redefine pack truth.

### 2. Automatic Memory Extraction

Useful for runtime agents, dangerous for a banking truth source if not isolated. Extracted memories are too soft to be treated as canonical rule content.

### 3. Runtime Skill Learning As Truth Mutation

Agent-learned shortcuts, habits, or prompt traces should remain runtime conveniences, not part of the canonical banking rule layer.

## Recommended Relationship

The right relationship is not system merger. It is layered responsibility:

- **truth-source layer**: Transformation Rule System
- **projection layer**: controlled export of approved runtime objects only: rules, lookups, helpers, plus L0/L1 summaries
- **retrieval layer**: OV resource tree, layered summaries, optional lightweight dependency links, and bounded retrieval
- **runtime layer**: agent sessions, tool use, and optional memory that never writes back to truth

The boundary is one-way:

- pack truth may project into OV
- OV runtime state must not silently project back into pack truth

## Diagram

Open the boundary diagram for the visual version of this layering model:

- `ov-truth-boundary.html`

## Final Position

Use OV as a retrieval and context-loading shell.

Do not use OV as the owner of canonical transformation truth.

The Transformation Rule System should remain the court of record. OV may act as the librarian.
