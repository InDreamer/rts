<!-- docmeta
role: leaf
layer: 3
parent: docs/transformation-rule-system/INDEX.md
children: []
summary: define the smallest OV integration that improves retrieval without merging systems or risking truth pollution
read_when:
  - the request is about a controlled OV adoption path
  - the request is about engineering scope or stop rules before implementation
  - the request is about mapping reviewed packs into a retrieval shell
skip_when:
  - the request is only about high-level conceptual boundary
  - the request is only about constitutional principles
source_of_truth:
  - transformation-rule-system-handoff-2026-04-16/HANDOFF-PROMPT.md
  - docs/transformation-rule-system/minimal-ov-integration.md
-->

# Minimal OV Integration

## What This Covers

This document defines the smallest acceptable OV integration path: one that improves discoverability and low-noise retrieval while keeping the Transformation Rule System as the sole owner of canonical truth.

## Source Boundaries

- `transformation-rule-system-handoff-2026-04-16/HANDOFF-PROMPT.md` — canonical pack structure, workflow, and current truth-source model.
- This document — engineering scope guardrails for controlled OV adoption.

## Goal

Improve agent retrieval quality and reduce hallucination pressure by projecting governed pack content into a low-noise, layered resource shell.

## Non-Goals

This integration does not attempt to:

- replace the pack repository
- rewrite canonical objects through OV
- merge review and signoff logic into OV
- use OV session memory as authoritative banking knowledge
- deep-customize OV internals

## Scope Boundary

### In Scope

- one-way export from approved packs into OV resources
- runtime projection of rules, lookups, and helpers only
- LLM-assisted, human-confirmed abstracts and overviews
- deterministic or semi-deterministic retrieval entry points for agents
- optional lightweight dependency manifests only if they prove necessary

### Out Of Scope

- OV core storage changes
- OV retrieval algorithm changes
- OV session or memory customization
- bidirectional sync
- automatic write-back into pack YAML

## Minimum Artifact Mapping

Project only governed pack material.

### Resource Tree Shape

Project approved pack runtime content into an OV resource subtree shaped roughly like this:

```text
resources/transformation-rule-system/tradition-stella/{product}/{pack}/
  .abstract.md
  .overview.md
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

### Projection Rule

- canonical repository remains primary
- OV receives a generated projection copy
- only signoff-approved main objects are projected by default
- evidence, review, and reports remain on the governed side and do not enter the first runtime shell

## Recommended State Filter

Default projection set:

- `approved`

Do not project review-ready or raw authoring content into the runtime retrieval namespace. The first runtime shell should expose only signoff-approved truth.

## L0 / L1 / L2 Strategy

L0, L1, and L2 are three precision views of the same resource, not separate governance levels.

Recommended layering for the narrowed projection:

- **channel / product / pack**: L0 + L1
- **rule / lookup / helper object**: L0 recommended, L2 YAML required
- **L2**: raw projected runtime files only

Pack L1 should focus on object list, target coverage, and dependency hints. It should not replay review state, open ambiguities, or evidence discussions.

There is no review-item layer in the first runtime projection because review does not enter the runtime shell.

## Relation Set

Relations are not required for the first usable version.

If a lightweight dependency layer is added later, start with:

- pack -> rule
- pack -> lookup
- pack -> helper
- object -> dependency object
- pack -> upstream or adjacent pack when needed

## Runtime Contract

Agent usage may retrieve projected pack material through OV. Runtime sessions may keep their own memory, but that memory must not be treated as truth-source content and must not update canonical pack files. Runtime retrieval does not require evidence or review access in the first phase.

## Delivery Shape

The smallest implementation should be a thin adapter, not a platform rewrite.

### Adapter Responsibilities

- export approved main-object files
- strip governance-only fields from projected YAML
- generate pack and object summaries
- push the projection into OV resources
- optionally emit lightweight dependency metadata later
- preserve read-only semantics

### Adapter Must Not

- interpret banking logic
- repair incomplete rules
- decide approval state
- suppress ambiguity
- project evidence, review, or reports into the first runtime shell

## Engineering Envelope

Expected size for a disciplined MVP:

- about 1200 to 3800 lines of new code and tests

That envelope assumes:

- narrowed runtime projection centered on rules, lookups, and helpers
- no OV core modification
- no bidirectional sync
- no custom runtime memory integration
- no deep UI

The current narrowed scope should aim for the low end of that range. If evidence, review, or a rich relation graph come back into scope, reassess.

## Suggested Work Breakdown

### Phase 1: Exporter

Build a local exporter that reads approved packs and writes a clean runtime projection tree.

### Phase 2: Metadata And Layering

Generate pack-level and object-level summaries plus optional lightweight dependency manifests.

### Phase 3: OV Ingestion Wrapper

Push the projection tree into OV resources using a repeatable ingestion command or wrapper.

### Phase 4: Retrieval Evaluation

Check whether common questions become easier to answer with less noise:

- where does this target value come from
- why is this field generated this way
- which pack covers this field
- which lookup or helper does this rule depend on

## Stop Rules

Stop and reassess if any of these becomes true:

- the adapter needs OV core changes
- projected content must write back into canonical packs
- projected draft content becomes indistinguishable from approved truth
- engineering scope climbs beyond roughly 5000 lines for the first usable version
- runtime memory starts influencing canonical answers

## Success Criteria

The integration is successful only if it produces all of these outcomes:

- lower retrieval noise
- clearer scope narrowing
- easier dependency traceability
- no weakening of approval or ambiguity boundaries
- no truth-source ownership shift away from the pack repository

## Final Recommendation

Adopt OV only as a projection and retrieval shell.

Keep the Transformation Rule System as the source of truth, the review authority, and the only place where canonical banking transformation rules are created or changed.
