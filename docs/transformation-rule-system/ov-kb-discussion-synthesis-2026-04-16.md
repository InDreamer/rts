<!-- docmeta
role: leaf
layer: 3
parent: docs/transformation-rule-system/INDEX.md
summary: consolidated synthesis of the OV knowledge base discussion — user goals, key corrections, standout insights, decisions, validated scenarios, and remaining open questions
read_when:
  - the request is about preserving the full design discussion without reading the raw chat
  - the request needs both the reasoning journey and the final design decisions
  - the request is about why the OV knowledge base approach was accepted in its current shape
  - the request needs the most important corrections and standout insights from the session
skip_when:
  - the request only needs the final retrieval architecture
  - the request only needs the brief session handoff
  - the request is about constitutional system principles rather than this session's synthesis
source_of_truth:
  - docs/transformation-rule-system/ov-kb-retrieval-design.md
  - docs/transformation-rule-system/session-handoff-2026-04-16.md
  - this file
-->

# OV Knowledge Base Discussion Synthesis 2026-04-16

> Date: 2026-04-16
> Purpose: preserve the full discussion in a deduplicated form without losing the best insights, corrections, or decisions
> Status: discussion consolidated; implementation still deferred

---

## 1. Why This Document Exists

The session produced more than a final architecture. It also produced several important corrections, priority resets, and practical insights that would be easy to lose if only the final design document were kept.

This document preserves:

- the user goal behind the exploration
- the parts of OpenViking that actually matter for this project
- the corrections that changed the design materially
- the strongest insights from the conversation
- the decisions that now define the current direction
- the questions intentionally left open

It is not a raw transcript. It is a cleaned and compressed record of the thinking.

---

## 2. User Goal And Constraint Frame

The user's goal was not to adopt OpenViking as a whole, and not to redesign the full OV project.

The real goal was narrower and more precise:

- evaluate whether OV is suitable as the storage and retrieval base for a high-trust knowledge base
- borrow only the parts that are truly useful
- preserve truth quality, readability, and efficiency for agent use
- support future rule libraries from multiple banking systems
- keep engineering scope disciplined rather than allowing it to expand into a large integration effort

The user clarified several hard constraints during the discussion:

- the knowledge base must serve agent scenarios such as rule lookup, message generation, and transaction diagnosis
- hallucination cannot be eliminated, so the system must reduce it structurally rather than cosmetically
- truth matters more than convenience
- already-signed-off content should be treated differently from authoring and review artifacts
- the user is interested in retrieval quality, not in adopting OV's entire worldview

This frame turned out to be essential. Without it, the discussion would have drifted into OV internals that are not actually important to the user's goal.

---

## 3. Where The Discussion Started

The exploration began from two repositories:

- the current Transformation Rule System design repository
- the OpenViking repository

The initial design comparison established a clean split:

- TRS is the governed truth source
- OV is a possible retrieval and storage shell

That split was useful, but it was still too broad for the user's immediate need. The user then pushed the discussion toward the specific question that mattered most:

**Can the indexing and layered-loading ideas be generalized into a practical knowledge base design, without bringing in unnecessary OV complexity?**

That shift changed the entire conversation from broad architecture review into a focused retrieval-design exercise.

---

## 4. The Most Important Corrections Made During The Session

These corrections materially changed the design.

### 4.1 The discussion is not really about full TRS adoption into OV

A first pass framed the topic too much as TRS versus OV.

The user clarified that the real interest was narrower:

- the layered indexing idea
- the URI-like addressing idea
- the retrieval path after the knowledge base is built

This correction mattered because it moved the work away from "Should OV own the system?" and toward "Which OV capabilities are genuinely worth borrowing?"

### 4.2 Signed-off projection should be operational, not archival

An earlier model kept pack structure too close to the canonical repository shape and still included evidence and review thinking in the projected runtime surface.

The user corrected that sharply:

- after signoff, agent-facing projection should be clean
- evidence and review are not first-class runtime needs anymore
- the projection should be for doing work, not for replaying the approval process

This changed the runtime model from:

- rules + lookups + helpers + evidence + review

to:

- rules + lookups + helpers only

This was one of the most important design simplifications of the session.

### 4.3 Relations are useful but not first priority

The idea of explicit rule-to-evidence relations is structurally attractive, but the user deprioritized it for the current stage.

That was the right call. It kept the design focused on the first-order runtime value:

- finding the right rules
- loading them efficiently
- keeping context clean

instead of prematurely optimizing for audit navigation.

### 4.4 L0/L1/L2 are not directory levels

The user challenged the proposed structure and asked whether L0 and L1 being in the same directory was actually coherent.

This forced the most useful clarification of the session:

- L0, L1, and L2 are not filesystem hierarchy levels
- they are three precision views of the same resource
- every directory node can have its own L0 and L1

This clarification turned a vague layering idea into a precise retrieval model.

### 4.5 Rerank had to be explained concretely, not abstractly

The user explicitly said the rerank step was still unclear.

That led to a concrete example:

- L0 does the cheap recall and may over-include similar packs
- L1 provides enough structured detail to filter false positives
- only then does L2 enter the agent context

This matters because it proved that L1 is not documentation decoration. It is the key layer that makes retrieval trustworthy.

---

## 5. Standout Insights From The Session

These were the strongest ideas that emerged.

### 5.1 OV is more valuable as a findability system than as a truth-construction system

This became one of the clearest takeaways.

OV appears stronger at:

- giving content stable addresses
- adding layered summaries
- supporting scoped retrieval
- making resources easier to find

OV is not the right place to define banking truth, signoff discipline, or approval semantics.

This single insight prevents a lot of architectural confusion.

### 5.2 Anti-hallucination is primarily a knowledge-base structure problem

The session repeatedly returned to the user's concern about hallucination.

The practical conclusion was:

- you will not solve hallucination mainly by asking the model to behave better
- you reduce it by controlling what the model is allowed to see, and in what order

That means:

- scoped retrieval before semantic expansion
- layered loading before raw document dumping
- approved-only projection instead of mixed-state ingestion
- channel partitioning instead of global soup

This insight is central to the whole design.

### 5.3 Signoff radically changes what the runtime needs

Before signoff, evidence, review, and ambiguity are essential.

After signoff, the runtime question changes from:

- why should we trust this candidate?

to:

- how can the agent use this truth efficiently and safely?

That is why the runtime projection can be much smaller than the canonical pack.

### 5.4 L1 quality determines rerank quality

This became one of the most operationally important insights.

If L1 is weak, retrieval remains noisy even when L0 is useful.

If L1 is strong, the system can:

- reject false positives early
- expose internal object boundaries clearly
- reveal dependencies such as lookups before loading all raw content

So L1 is not secondary. It is the control surface for precise retrieval.

### 5.5 Channel-level physical partitioning is a real defense, not just organization

The discussion made clear that when multiple banking systems are involved, channel separation is not merely convenient.

It actively prevents:

- cross-system contamination
- wrong-pack recall
- accidental blending of similar rules from different systems

This is both a retrieval design choice and a hallucination-control choice.

---

## 6. Final Design Direction Reached In The Session

The current direction can be summarized in one sentence:

**Use OV as a scoped, read-only retrieval shell for signed-off truth projections, not as the owner of truth, review, or memory.**

The main design decisions were:

### 6.1 Projection scope

Project only:

- rules
- lookups
- helpers

Do not project for runtime use:

- evidence
- review
- reports

### 6.2 YAML projection shape

Projected YAML should be stripped down to runtime-useful fields only:

- `id`
- `source`
- `logic`
- `target`
- `dependencies`
- `examples`

Governance-only fields should be removed.

### 6.3 Read-only rule

- projection is one-way
- OV has no write-back path to the canonical repository
- runtime notes and memory do not redefine approved rules

### 6.4 L0/L1/L2 layering rule

- channel: L0 + L1
- product: L0 + L1
- pack: L0 + L1
- rule: L0 recommended, L1 unnecessary
- L2 remains the complete YAML object

### 6.5 L0/L1 generation rule

- generate from approved YAML with LLM assistance
- confirm by human review before projection

### 6.6 OV capability selection

Use:

- Viking URI addressing
- layered context loading
- directory-recursive retrieval
- ingestion and indexing pipeline

Use carefully:

- vector search, always under scope constraints
- automated summaries, only with human confirmation for governed content

Avoid:

- session memory extraction
- skill system as part of this design
- bidirectional sync
- unnecessary queue machinery

---

## 7. Why The Query Scenarios Mattered

Three concrete scenarios were used to test whether the design was just elegant on paper or actually useful.

### 7.1 Single rule query

This validated that the system can locate one precise rule cheaply and cleanly.

Most important takeaway:

- the agent can reach the correct rule with very little irrelevant context

### 7.2 Full message generation

This validated that the same structure can support composition across multiple packs rather than only single-object lookup.

Most important takeaway:

- retrieval can remain scoped and modular even when many rules are needed together

### 7.3 Transaction diagnosis

This validated that lookup-backed reasoning can expose root cause rather than just restate rule text.

Most important takeaway:

- the knowledge base can support operational diagnosis, not just static explanation

Together, these three scenarios proved that the proposed structure is useful across:

- understanding
- generation
- debugging

That was a major confidence point in the discussion.

---

## 8. What Was Intentionally Left Open

The session deliberately did not force closure on several things.

These remain the next design surfaces:

- exact L0 template specification
- exact L1 template specification
- projection adapter design from canonical pack to OV tree
- OV-side engineering setup and ingestion details
- whether lightweight dependency relations should be added in the first implementation
- how to reduce error risk in the multi-rule message generation scenario

This was appropriate. The conceptual shape is now much clearer than it was at the start, and implementation can proceed later with less confusion.

---

## 9. Relationship To The Other Documents

This synthesis plays a different role from the other leaf documents:

| Document | Purpose |
|----------|---------|
| `system-constitution-v1.md` | formal principles of truth-source governance |
| `ov-boundary-and-adoption.md` | hard boundary between truth source and OV |
| `minimal-ov-integration.md` | smallest acceptable engineering integration path |
| `ov-kb-retrieval-design.md` | final retrieval-oriented architecture |
| `session-handoff-2026-04-16.md` | short restart summary for the next conversation |
| **this document** | full cleaned synthesis of the discussion, including reasoning journey and corrections |

---

## 10. One-Paragraph Executive Summary

The discussion concluded that OpenViking is promising for this project only when used narrowly: as a read-only retrieval shell for already-signed-off truth. The winning design is not to push the full canonical pack into OV, but to project a stripped runtime surface made of rules, lookups, and helpers, then layer it with L0/L1/L2 views so agents can narrow scope before reading raw details. The most important insights were that anti-hallucination is mainly a knowledge-base structure problem, that L1 quality determines rerank quality, and that signoff allows the runtime projection to drop evidence and review entirely. The result is a smaller, cleaner, and more practical path than full OV adoption.