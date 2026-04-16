<!-- docmeta
role: leaf
layer: 3
parent: docs/transformation-rule-system/INDEX.md
children: []
summary: formal constitution for a truth-first transformation rule system with explicit LLM constraints
read_when:
  - the request is about core system principles
  - the request is about hallucination minimization by architecture
  - the request is about what the LLM may or may not do
skip_when:
  - the request is only about OV adoption mechanics
  - the request is only about a specific pack implementation detail
source_of_truth:
  - transformation-rule-system-handoff-2026-04-16/HANDOFF-PROMPT.md
  - docs/transformation-rule-system/system-constitution-v1.md
-->

# System Constitution v1

## What This Covers

This document defines the constitutional rules for the Transformation Rule System. It describes what the system exists to protect, how truth is established, and how the LLM must be constrained so that helpfulness never outruns evidence.

## Source Boundaries

- `transformation-rule-system-handoff-2026-04-16/HANDOFF-PROMPT.md` — canonical project shape, pack structure, workflow model, and current operating assumptions.
- This document — governing interpretation layer for future design and implementation decisions.

## Constitutional Purpose

The system exists to turn bank XML transformation knowledge into a governed truth source that an LLM can read safely.

The primary goal is not generative power. The primary goal is controlled truth access:

- truth must be explicit
- evidence must be traceable
- unknowns must remain unknown
- the LLM must operate as a constrained reader, navigator, and explainer

## Constitutional Principles

### 1. Pack Completion Establishes Truth

Once a pack has passed the defined review and signoff boundary, it becomes the goldensource for its scope. It is no longer a draft interpretation or advisory note.

### 2. Canonical Truth Lives In Pack Objects, Not In Conversations

The system of record is the governed pack content and its controlled companion layers. Chat logs, agent outputs, and working notes do not redefine truth.

### 3. The LLM Reads Truth; It Does Not Invent Truth

The LLM may:

- locate the right pack
- locate the right object
- summarize the canonical rule
- explain the rule in bounded language
- point to supporting evidence and open review items

The LLM may not:

- infer missing business logic
- repair incomplete rules by intuition
- fill evidence gaps with plausible language
- convert open ambiguity into silent certainty

### 4. Unknown Is Better Than Wrong

If the available evidence is insufficient, the system should return ambiguity, uncertainty, or not-enough-evidence rather than a polished but unsupported answer.

### 5. Evidence Outranks Narrative

Every material rule assertion must be traceable to evidence. If explanation and evidence disagree, evidence wins and the explanation must be corrected.

### 6. Review Is Part Of Truth Governance

Open items, risks, ambiguity, and signoff status are not side chatter. They are part of the operational truth boundary that determines whether a rule may be trusted.

### 7. Main Objects Must Stay Minimal

Canonical objects should remain small and stable. They should express source, logic, target, dependencies, and examples without absorbing long traces, meeting notes, or uncontrolled commentary.

### 8. Structural Clarity Reduces Hallucination

The system must prefer explicit object boundaries over blended prose. The clearer the separation between rule, evidence, review, and report, the less freedom the model has to confuse them.

### 9. Context Must Be Precise, Not Maximal

More text is not safer. Safer context is:

- closer to the requested scope
- lower in noise
- higher in structural precision
- explicit about state and uncertainty

### 10. Navigation Precedes Full Read

The model should first determine the correct workflow, product, pack, and object before reading detailed content. Full text should be a last-mile action, not the default starting point.

### 11. Runtime Learning Must Not Pollute Canonical Rules

Session memory, usage traces, and operational learning may inform future authoring or review, but they must never silently rewrite the goldensource.

### 12. Evolution Requires Controlled Schema Change

The system may evolve, but only through explicit schema, status, and workflow versioning. Silent drift is unacceptable because it weakens trust and breaks explainability.

## LLM Role Boundary

The LLM is a constrained operating component with four legitimate roles:

- **reader**: reads canonical objects and governed side layers
- **navigator**: chooses which governed object to open next
- **explainer**: rewrites truth in clearer human language without changing meaning
- **disputer marker**: explicitly flags ambiguity, evidence gaps, or state conflicts

The LLM is not:

- a rule author of record
- a business logic interpolator
- a hidden signoff agent
- a substitute for missing evidence

## Default Output Discipline

When asked a rule question, the system should prefer answers that:

- name the exact scope being answered
- distinguish approved truth from draft or ambiguous content
- separate rule statement from evidence statement
- mention open ambiguity when it exists
- stop short of unsupported conclusions

## Non-Goals

This system is not trying to:

- maximize free-form conversational fluency
- produce speculative convenience answers
- treat all project documents as equal sources of truth
- collapse governance, runtime memory, and authoring into one surface

## Decision Rule

When a future design choice is unclear, choose the option that most reduces unauthorized LLM freedom, preserves evidentiary traceability, and keeps the goldensource separate from runtime behavior.
