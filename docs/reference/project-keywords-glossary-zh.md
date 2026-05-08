<!-- docmeta
role: leaf
layer: 3
parent: docs/reference/README.md
children: []
summary: compact Chinese glossary for current RTS project terms and boundary concepts
read_when:
  - 需要快速理解当前项目高频术语
  - 需要区分 canonical truth、runtime projection、index、query、LLM answer 和 candidate
skip_when:
  - 需要正式架构决策
  - 需要完整历史术语长表
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
-->

# RTS Project Glossary

> 状态：compact glossary
> 原完整长表：`docs/archive/reference-proposals/project-keywords-glossary-zh.md`

## Core Terms

| Term | Meaning |
|---|---|
| RTS service | 面向规则真相读取、解释、影响分析、测试规划和流程接入的服务边界。 |
| Knowledge Base | RTS 内部治理知识和已发布 truth material 的统称，不等于普通文档库。 |
| canonical truth | source/evidence/review/human adjudication/signoff 后成立的规则真相。 |
| candidate | 候选规则、候选解释、候选影响面或候选测试点，等待 review。 |
| open question / ambiguity | 尚未裁决的问题或歧义，不能被模型硬答成事实。 |
| LLM answer | 模型组织出的回答，不自动等于 truth。 |
| approved truth | 已经过治理流程批准，可以进入 runtime projection 的 truth。 |

## Layer Terms

| Term | Meaning |
|---|---|
| Truth Layer | 管理 source、candidate、review、signoff、canonical pack。 |
| Projection Layer | 把 approved truth 发布成服务运行视图。 |
| Retrieval/Index Layer | 帮助定位、导航、搜索、读取 truth；不决定 truth。 |
| Application Layer | API、MCP、Q&A、pipeline、workbench、agent tools 等消费入口。 |

## Object Terms

| Term | Meaning |
|---|---|
| pack | 一组可 review/signoff/release 的 rule/lookup/helper 集合。 |
| rule | source-to-target transformation rule。 |
| lookup | 查表定义或 lookup behavior。 |
| helper | 可复用辅助逻辑。 |
| evidence | 支撑候选或规则判断的来源材料。 |
| review | 对 candidate/rule/evidence 的人工或流程审核记录。 |
| signoff | 使 truth 可以发布的最终治理动作。 |

## Projection And Retrieval Terms

| Term | Meaning |
|---|---|
| runtime projection | approved truth 的服务运行视图，面向 API/MCP/LLM/index。 |
| release manifest | projection release 的身份、来源、schema、状态和激活信息。 |
| object manifest | 可查询对象清单。 |
| object card / L1 | 导航和消歧视图，不是最终事实。 |
| L2 runtime object | 服务可读事实对象，最终回答必须回到它或授权 governance view。 |
| dependency edge | rule/lookup/helper/source/target 之间的依赖关系。 |
| stable URI | 可追踪、可审计、可被 API/MCP/agent 引用的对象地址。 |
| retrieval trace | 记录 scope、候选、工具调用、L2 read、refusal 的查询轨迹。 |

## Output Terms

| Term | Meaning |
|---|---|
| fact | 被 L2/dependency/evidence/review/release 支撑的事实。 |
| inference | 基于事实做出的分析，需要标注来源和不确定性。 |
| unknown | 缺证据、scope 不清、冲突未裁决或权限不足。 |
| candidate suggestion | 候选影响面、测试点、问题、解释或规则修改。 |
| human decision | 人工 adjudication、review 或 signoff 结论。 |
| refusal contract | 不满足 scope/release/permission/L2/grounding 时拒答或要求澄清。 |

## Common Confusions

- Runtime projection is not canonical source; it is a released service view.
- L0/L1/card helps find truth; L2 supports final facts.
- Search result is not truth.
- LLM answer is not truth.
- Memory can improve convenience, not authority.
- MCP/API/tool mode changes access style, not truth ownership.
- Vector/rerank can reorder candidates only after gates.
