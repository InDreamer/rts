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

- one-way export from governed packs into OV resources
- pack, object, evidence, and review projection
- generated short abstracts and overviews
- explicit relation building
- deterministic or semi-deterministic retrieval entry points for agents

### Out Of Scope

- OV core storage changes
- OV retrieval algorithm changes
- OV session or memory customization
- bidirectional sync
- automatic write-back into pack YAML

## Minimum Artifact Mapping

Project only governed pack material.

### Resource Tree Shape

Project reviewed or approved pack content into an OV resource subtree shaped roughly like this:

```text
resources/transformation-rule-system/tradition-stella/{product}/{pack}/
  pack-summary.md
  objects/
    rules/
    lookups/
    helpers/
  evidence/
    index.yaml
    entries/
  review/
    index.yaml
    items/
  reports/
```

### Projection Rule

- canonical repository remains primary
- OV receives a generated projection copy
- only governed states are projected by default

## Recommended State Filter

Default projection set:

- `approved`
- `ready for BA review`
- `ready for Dev review`

Do not project raw authoring drafts into the same retrieval namespace unless they are clearly isolated and labeled as non-authoritative.

## L0 / L1 / L2 Strategy

### Pack Level

- **L0**: one-sentence scope and state
- **L1**: pack overview with objects, dependencies, review status, and open items
- **L2**: raw projected files

### Object Level

- **L0**: target field or lookup purpose in one line
- **L1**: source, logic summary, dependencies, evidence refs, review refs
- **L2**: canonical projected object file

### Review Item Level

- **L0**: short ambiguity or finding statement
- **L1**: affected object, why unresolved, supporting evidence
- **L2**: full review item payload

## Relation Set

Build relations explicitly instead of hoping text similarity does all the work.

Minimum relation types:

- pack -> rule
- pack -> lookup
- pack -> helper
- object -> evidence entry
- object -> review item
- object -> dependency object
- review item -> supporting evidence
- pack -> upstream or adjacent pack when needed

## Runtime Contract

Agent usage may retrieve projected pack material through OV. Runtime sessions may keep their own memory, but that memory must not be treated as truth-source content and must not update canonical pack files.

## Delivery Shape

The smallest implementation should be a thin adapter, not a platform rewrite.

### Adapter Responsibilities

- export governed pack files
- generate projection metadata
- generate pack and object overviews
- push the projection into OV resources
- build or refresh relations
- keep state labels visible

### Adapter Must Not

- interpret banking logic
- repair incomplete rules
- decide approval state
- suppress ambiguity

## Engineering Envelope

Expected size for a disciplined MVP:

- about 1200 to 3800 lines of new code and tests

That envelope assumes:

- no OV core modification
- no bidirectional sync
- no custom runtime memory integration
- no deep UI

## Suggested Work Breakdown

### Phase 1: Exporter

Build a local exporter that reads reviewed or approved packs and writes a clean projection tree.

### Phase 2: Metadata And Layering

Generate pack-level and object-level summaries plus relation manifests.

### Phase 3: OV Ingestion Wrapper

Push the projection tree into OV resources using a repeatable ingestion command or wrapper.

### Phase 4: Retrieval Evaluation

Check whether common questions become easier to answer with less noise:

- where does this target value come from
- why is this field generated this way
- what evidence supports this rule
- what remains ambiguous

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
- easier evidence traceability
- no weakening of approval or ambiguity boundaries
- no truth-source ownership shift away from the pack repository

## Final Recommendation

Adopt OV only as a projection and retrieval shell.

Keep the Transformation Rule System as the source of truth, the review authority, and the only place where canonical banking transformation rules are created or changed.
