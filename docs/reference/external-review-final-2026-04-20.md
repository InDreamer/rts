<!-- docmeta
role: leaf
layer: 3
parent: docs/reference/README.md
children: []
summary: distilled external review constraints for projection release, lifecycle, conflict, trace, permission, and refusal
read_when:
  - 需要理解 projection/refusal/release constraints 的历史动机
  - 需要快速查看 external review 提炼出的 P0/P1/P2 约束
skip_when:
  - 需要当前 active baseline 或实现计划
  - 需要完整 external review 原文
source_of_truth:
  - docs/confirmed/kb-to-index-projection-contract-zh.md
  - docs/confirmed/system-constitution-v1.md
-->

# External Review Constraints Summary

> 状态：distilled reference
> 原完整原文：`docs/archive/reference-proposals/external-review-final-2026-04-20.md`

## One-Line Conclusion

RTS 方向正确，但必须把原则变成硬工程约束：projection release、lifecycle、admission granularity、conflict/precedence、refusal、trace、permission 和 operational failure 都不能靠实现者自由解释。

## P0 Constraints

1. **Projection artifact must be a release unit**
   Include source revision, projection schema version, generated/released metadata, activation state, supersession/rollback identity.

2. **Projection lifecycle must be immutable**
   Canonical changes create a new projection artifact and activate atomically. Never silently overwrite current truth surface.

3. **Release admission granularity must be explicit**
   Phase-1 recommendation: no object enters released projection until the whole pack passes signoff.

4. **Conflict/precedence must fail before release**
   Object id, target coverage, URI/address, COMMON/product override conflicts must not be resolved dynamically at query time.

5. **Refusal contract must be formal**
   Refuse or ask clarification when scope unresolved, active artifact missing/ambiguous, conflicts remain, dependencies unreleased, permission insufficient, or only summaries exist without L2.

## P1/P2 Constraints

- L0/L1/card summaries are governed navigation outputs, not unchecked text.
- Retrieval route should be deterministic narrowing -> navigation -> L2 read before any vector/rerank path.
- Dependency hints should not rank, rerank, or expand retrieval scope without explicit purpose.
- Derived stores must be rebuildable and switch consistently with projection activation.
- Trace must connect canonical revision -> projection artifact -> query result.
- Permission boundaries and retrieval boundaries must align.
- Ambiguity reopen/close must create a new revision and reproject path.
- Canonical schema, projection schema, and summary/card schema versions should be separated.
- Failure playbook should cover failed publish, partial sync, rollback, and idempotent rebuild.

## Common Misimplementations To Avoid

- Mutable “latest projection” records instead of immutable release artifacts.
- Object-level partial releases accidentally becoming default.
- Vector/rerank used before deterministic scope narrowing.
- L0/L1 summaries treated as fact owners.
- Canonical changes overwriting active projection in place.

## Current Mapping

Most of these concerns now live in:

- runtime projection contract
- Day1 refusal and trace rules
- service constitution
- final LLM agent roadmap

Use the archived full review only when you need the original risk rationale and consequence analysis.
