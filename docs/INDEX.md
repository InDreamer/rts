<!-- docmeta
role: entry
layer: 1
parent: null
children:
  - docs/confirmed/README.md
  - docs/reference/README.md
summary: primary RTS documentation router
read_when:
  - 需要选择 RTS 文档阅读路径
  - 需要区分 confirmed baseline、reference materials 和 archive
  - 第一次进入 docs 目录
skip_when:
  - 已经明确要读取某一篇 leaf 文档
source_of_truth:
  - README.md
  - docs/confirmed/README.md
  - docs/reference/README.md
-->

# RTS Documentation Index

This is the primary documentation router for RTS.

## Open One Path

- `docs/confirmed/README.md` — 当前 baseline。做项目对齐、KB/index contract、Day1 实现或 Day2 演进规划时优先打开。
- `docs/reference/README.md` — 参考材料。需要历史 rationale、OV 背景、检索原则、术语表或早期 Java 参考方案时打开。
- `docs/archive/README.md` — 历史材料。只在明确需要追溯旧讨论、旧产物或 archived prototype 时打开。

## Default Rule

For active design and implementation, use confirmed docs first. Reference docs may explain why a decision exists, but they do not override confirmed baseline.
