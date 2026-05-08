<!-- docmeta
role: leaf
layer: 3
parent: docs/reference/README.md
children: []
summary: distilled Java index/query layer reference; current safe ideas without old infrastructure commitments
read_when:
  - 需要从旧 Java 索引层方案中提取仍适用的工程原则
  - 需要判断当前 Java service 为什么采用 lightweight projection store、Lucene、L2 回读和受控 query boundary
  - 需要确认哪些早期 OpenSearch/vector/Knowledge-Bases 设想已经降级为历史参考
skip_when:
  - 需要当前 RTS 全系统 baseline
  - 需要 Day1 服务的具体运行命令
  - 需要完整历史方案原文
source_of_truth:
  - docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
-->

# Java Index Layer Reference Summary

> 状态：distilled reference
> 日期：2026-05-08
> 原始长文：`docs/archive/reference-proposals/java-index-layer-full-plan-zh.md`
> 目的：保留旧 Java index/query layer 方案里仍然有用的工程思想，删除默认阅读路径中的过时基础设施假设。

## Current Interpretation

这份摘要不是当前 RTS 全系统 baseline，也不是最终技术栈决定。

当前 baseline 以 confirmed docs 为准：

- RTS 是 governed rule truth service。
- runtime projection 是 approved truth 的服务运行视图，不是摘要替身。
- query/index service 只能读取 approved runtime projection。
- search hit、LLM answer、agent memory、index document 都不是 truth。
- Day1 采用 JDK 17 Java service、filesystem projection store、Lucene、本地 L2 content、受控 LLM harness。

原始长文保留在 archive 中，仅用于历史追溯。

## Still Useful Ideas

旧方案中仍然适用的原则：

1. **Java service as maintainable runtime boundary**
   查询/索引服务应是可测试、可部署、可观测的 Java 服务，而不是临时脚本或直接文件检索。

2. **Truth ownership stays outside index/search**
   索引层不接管 canonical truth、review、signoff 或 human adjudication。

3. **Projection is a released artifact**
   runtime projection 必须可追踪、可重建、可激活/回滚，并带 release manifest。

4. **Scope before content**
   查询应先确定 channel/product/pack/domain/object 范围，再读取 L2 结构化内容。

5. **L0/L1/card help navigation, L2 supports final facts**
   L0/L1/card/search text 可以帮助召回和 disambiguation，但最终事实必须回到 L2 object 或授权 governance view。

6. **Dependency graph is first-class**
   rule、lookup、helper、source anchor、target path 之间的依赖边应可查询、可追踪、可解释。

7. **Derived stores are rebuildable**
   Lucene/OpenSearch/vector index 都只能是派生视图，不能成为 authoritative runtime boundary。

8. **Refusal is product behavior**
   unresolved scope、missing release、hash mismatch、permission denied、dependency not released、only summary hit 等情况必须拒答或要求澄清。

## Retired Or Downgraded Ideas

以下旧设想不再作为默认 reference 路线：

| Old idea | Current treatment |
|---|---|
| `Knowledge-Bases/` tree as required runtime shape | 历史术语。当前使用 runtime projection，底层可在 filesystem、PostgreSQL、object store 或其他介质。 |
| Java 21 as baseline | 当前实现使用 JDK 17。未来升级另行决定。 |
| PostgreSQL + OpenSearch/Elasticsearch as initial infrastructure | Day1 不要求。当前使用 filesystem store + Lucene，并保留替换接口。 |
| Vector/hybrid retrieval as first implementation | Day1 不做；Day2 以后先证明 BM25/alias/card 不够，再评估。 |
| Projection strips governance fields absolutely | 已替换为 operational view / authorized governance view 模型。 |
| Full production-style audit/SSO/object-store stack on first pass | 当前不是 Day1 必选；后续按 deployment/security 需要推进。 |
| “TRS” as current primary name | 只作为历史名称或别名；当前统一 RTS。 |

## Current Safe Boundary

推荐用下面的心智模型理解 Java query/index service：

```text
KB / governance side
  -> approved immutable runtime projection
  -> RTS Java query/index service
     -> active release and permission gate
     -> scope resolver
     -> object manifest / card / Lucene candidate search
     -> L2 read and hash validation
     -> dependency traversal
     -> trace / refusal / grounding
  -> REST / MCP / managed LLM harness / external tool callers
```

关键边界：

- Java service 可以拥有 runtime truth access rules。
- Java service 不拥有 canonical truth。
- Index can find candidates.
- L2 read proves facts.
- LLM can explain and plan tool use.
- Grounding/refusal must remain service-controlled.

## When To Read The Archived Full Plan

只有在以下场景才读 archive 原文：

- 需要追溯 2026-04-20 Java index layer 的完整阶段设想。
- 需要比较早期 OpenSearch/vector/PostgreSQL 方案与当前 Day1 方案的差异。
- 需要从旧方案中挖出被当前摘要遗漏的实现 checklist。

读 archive 原文时必须套用当前裁决：

- confirmed docs 覆盖 archive。
- old infrastructure choices are not commitments.
- old projection wording is historical.
- old TRS naming maps to current RTS.
