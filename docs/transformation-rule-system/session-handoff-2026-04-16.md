<!-- docmeta
role: leaf
layer: 3
parent: docs/transformation-rule-system/INDEX.md
children: []
summary: session handoff summarizing the current understanding of the transformation rule system, OV boundary, and the next retrieval-focused learning path
read_when:
  - starting a new session and needing a concise design handoff
  - needing a plain-language summary of what has already been agreed
  - needing to resume OV learning from the retrieval angle instead of the truth-construction angle
skip_when:
  - the request needs the full constitutional or boundary documents directly
  - the request is only about implementation details inside OV code
source_of_truth:
  - transformation-rule-system-handoff-2026-04-16/HANDOFF-PROMPT.md
  - docs/transformation-rule-system/system-constitution-v1.md
  - docs/transformation-rule-system/ov-boundary-and-adoption.md
  - docs/transformation-rule-system/minimal-ov-integration.md
-->

# Session Handoff 2026-04-16

## What This Covers

This document is a plain-language handoff for the next session. It captures the design stance that has already stabilized, the places where the discussion corrected itself, and the most natural next learning thread.

## Project Frame

The project is a bank XML transformation rule system.

The user position is clear:

- truth matters more than convenience
- completed pack content becomes goldensource
- hallucination must be reduced as much as possible
- the LLM should be pushed toward constrained reading rather than free invention
- the system should be AI-readable, low-noise, and high-signal

## Stable Conclusions

### 1. Core Boundary Between The Two Systems

The most important design boundary is:

- the Transformation Rule System owns **rule truth**
- OpenViking owns **context storage and retrieval mechanics**

In other words:

- your system answers: **what is the true rule**
- OV answers: **how an agent finds and loads that rule**

This boundary was explicitly accepted as correct.

### 2. Goldensource Rule

Once a pack is finished and passes the proper review and signoff boundary, it becomes truth for its scope.

OV must not become the authority that decides whether a banking rule is true.

### 3. OV Is Useful As A Shell, Not As The Court Of Record

The safest role for OV is:

- projection shell
- resource tree
- layered reading
- relation graph
- retrieval helper

The unsafe role for OV is:

- canonical rule owner
- approval engine
- ambiguity resolver
- runtime memory source for banking truth

### 4. Engineering Scope Must Stay Small

The user does not want a project that explodes into a huge codebase dependency.

The current estimate remains:

- do not think in terms of the whole OV repository
- first-pass relevant OV code is only a narrow slice
- a disciplined minimal adoption should be a thin adapter, not a deep merge

## Important Corrections Made During Discussion

### 1. AI Is Not Only A Reader In This Project

An earlier explanation leaned too hard toward “engineering builds the order first, then AI works inside it.”

That was corrected.

The user’s intended design is closer to:

- humans define boundaries, layering, and rules
- AI may generate the working file structure and structured candidate data
- agent validation and human validation both participate
- truth is still certified by the governance chain rather than by AI alone

So the better statement is:

AI may help generate candidate structure, but it does not own final truth certification.

### 2. Mixed-State Retrieval Risk Depends On Feed Discipline

A general warning was given that mixed states like draft and approved can confuse retrieval.

The user then clarified an important constraint:

- only final signoff content should enter OV

Under that rule, the mixed-state retrieval problem mostly disappears at the OV layer. The risk is no longer “OV mixes everything,” but rather “the projected truth objects may still be hard to find unless they are organized well.”

### 3. Resource Ingest Is Not The Same As Truth Construction

Another important correction:

- OV’s resource ingest path is mostly about getting content into a resource tree
- it is not the same thing as building banking truth objects

This matters because the user is currently more interested in how truth gets found in OV than in how OV imports arbitrary source material.

## What Was Learned About OV Resource Ingest

The plain-language model reached in the discussion was:

- OV first receives files or directories
- code decides how they are scanned, filtered, and placed
- some document types like Markdown may be split by headings or paragraphs
- many text-like object files such as XML and YAML are often stored more like raw originals than deeply re-modeled business objects
- after content is placed, OV adds convenience layers like summaries and indexes

The key practical insight:

OV is stronger at making content **findable** than at turning banking XML/YAML into your domain’s canonical rule model.

## Current Focus Shift

The user explicitly shifted focus away from:

- how truth is constructed inside the project

and toward:

- how already-confirmed truth gets found in the OV layer

This is now the correct learning direction for the next session.

## Retrieval Brain: The Four Things To Continue With

The discussion narrowed the next learning target to four concrete ideas:

### 1. Address

How a truth object gets a stable location.

For this project, that likely means addressability for at least:

- pack
- rule
- evidence item
- review item

### 2. Abstract / Overview

How a truth object gets a short description and a navigation layer so the model does not begin by swallowing full raw content.

### 3. Relations

How the system tells the agent what is connected to what, for example:

- rule -> evidence
- rule -> review item
- rule -> dependency
- pack -> rule

### 4. Retrieval Order

How the agent narrows scope before reading raw detail.

The preferred mental model is:

- first find the right area
- then find the right object
- then use relation links to reach evidence and review
- only then read raw detail

## One-Line Summary Of OV’s Likely Value

OV is not currently being treated as a truth builder.

OV is being evaluated as a way to turn already-confirmed truth into:

- stable addresses
- low-noise summaries
- relation-aware navigation
- less chaotic retrieval

## Recommended Starting Point For The Next Session

Resume with the retrieval angle only.

The best next question is:

**How does an already-confirmed truth object become easier to find through address, summary, relation, and retrieval order?**

That keeps the discussion aligned with the user’s actual priority and avoids drifting back into broad OV implementation detail.

## Suggested Next-Session Prompts

- “继续讲 OV 里地址、摘要、关系、检索顺序这四件事，但只围绕已 signoff 的 pack 投影。”
- “把 pack / rule / evidence / review item 映射到 OV 的资源树里，先不要讲 memory 和 session。”
- “只讲对低幻觉 retrieval 有帮助的部分，不讲大仓实现细节。”
