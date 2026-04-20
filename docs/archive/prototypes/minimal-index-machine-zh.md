<!-- docmeta
role: leaf
layer: 3
parent: docs/transformation-rule-system/INDEX.md
children: []
summary: minimal OpenViking-inspired index machine prototype implemented in this repository, including runtime projection, layered indexing, and scoped retrieval
read_when:
  - 需要从设计文档进入第一版可运行实现
  - 需要理解 mini_indexer 与 OpenViking 索引层的对应关系
  - 需要知道下一步如何把 signoff pack 接进来
skip_when:
  - 只关心高层边界，不关心实现
source_of_truth:
  - docs/transformation-rule-system/minimal-ov-integration.md
  - docs/transformation-rule-system/ov-kb-retrieval-design.md
  - mini_indexer
-->

# 最小索引机实现说明

## 目标

在不引入 OpenViking 全量复杂度的前提下，先在本仓库里落一个可运行的最小索引机，验证三件最核心的能力：

- 稳定 URI 寻址
- L0/L1/L2 分层资源模型
- 作用域受限的分层检索

这个原型服务于 TRS 当前已经确定的 runtime projection 边界：只面向 signoff 后的 `rules / lookups / helpers` 检索面，不把 evidence / review / reports 带进运行时索引。

## 对齐到 OpenViking 的部分

当前原型直接借鉴了 OpenViking 的这几项设计：

- `viking://resources/...` 风格的稳定资源 URI
- `L0=.abstract.md`、`L1=.overview.md`、`L2=原始文件/目录`
- 先限定 `target_uri` 再检索，而不是全局语义扩散
- 类似 `find()` 的简单检索入口
- 目录优先的层级检索，而不是把所有文件打平成一个无边界集合

在实现里，这条链路被压缩成：

```text
Canonical Pack Tree
  -> projector.py
  -> Runtime Resource Tree
  -> indexer.py
  -> retriever.py
```

## 刻意没有实现的部分

这些能力在 OpenViking 里存在，但当前原型故意不做：

- Parser / TreeBuilder 的多模态解析
- AGFS 虚拟文件系统
- SemanticQueue 异步摘要生成
- embedding 向量索引
- rerank
- watch / 增量同步 / queue 调度
- session context / memory / skill 检索

原因很简单：TRS 当前第一步要验证的是 runtime projection 和检索边界是否干净，而不是把 OV 的整个平台搬进来。

## 当前实现

### 1. `mini_indexer/projector.py`

最小 runtime projector。

- 输入：`{product}/{pack}/{rules,lookups,helpers}` 形态的 canonical tree
- 输出：`{output_root}/{channel}/{product}/{pack}/...`
- 默认只投影 `signoff_status` 为 approved 的对象；如果对象里没有该字段，则默认保留
- 写出时剥离 governance 字段，例如：
  - `signoff_status`
  - `review_status`
  - `ambiguities`
  - `evidence_refs`
  - `trace`

### 2. `mini_indexer/indexer.py`

本地目录索引器。

- 扫描运行时资源树
- 为每个目录和文件建立 URI 节点
- 读取现有 `.abstract.md` 和 `.overview.md`
- 若缺失，则生成确定性的 fallback 摘要
- 把节点落成一个 JSON 索引文件

### 3. `mini_indexer/retriever.py`

最小层级检索器。

- 查询时先限定 `target_uri`
- 先给目录节点打分，选出起始目录
- 再递归探索子目录和叶子文件
- 用父目录分数向下传播，模拟 OV 的 hierarchy-first 检索路径

当前打分仍是词项匹配，不是 embedding。

### 4. `mini_indexer/cli.py`

提供四个命令：

- `project`
- `build`
- `find`
- `read`

现在已经扩成当前仓库内可直接使用的 MVP 命令集：

- `sync`：从 `generated_pack/` 投影到 `Knowledge-Bases/`，补齐目录级元数据并重建索引
- `query`：按 `target_uri` 做 scoped retrieval，并输出 pack L0/L1 与对象 L2

### 5. `generated_pack/` 与 `Knowledge-Bases/`

仓库里现在已经有一套最小 canonical sample：

```text
generated_pack/
  TRADITION-STELLA/
    COMMON/
      rule-pack-scbml-header/
    FXD_NDF/
      rule-pack-cutoff-split/
```

对应的运行时目录会被构建到：

```text
Knowledge-Bases/
  index.yaml
  TRADITION-STELLA/
    index.yaml
    COMMON/
      index.yaml
      rule-pack-scbml-header/
        index.yaml
    FXD_NDF/
      index.yaml
      rule-pack-cutoff-split/
        index.yaml
```

这些 `index.yaml` 就是当前项目文档里要求的“发现层”最小实现。

### 6. `scripts/`

提供两个仓库级脚本：

- `scripts/build_mvp_kb.sh`
- `scripts/query_mvp_kb.sh`

## 示例

仓库里已经放了一个最小运行时 KB：

```text
examples/mini_runtime_kb/
  tradition-stella/
    common/
      rule-pack-scbml-header/
    fxd_ndf/
      rule-pack-cutoff-split/
```

可直接运行：

```bash
./scripts/build_mvp_kb.sh
./scripts/query_mvp_kb.sh "initiated timestamp header" viking://resources/TRADITION-STELLA/COMMON
./scripts/query_mvp_kb.sh "cutoff time participant count lookup" viking://resources/TRADITION-STELLA/FXD_NDF
```

如果你想直接用 Python CLI，也可以：

```bash
python3 -m mini_indexer sync \
  generated_pack/TRADITION-STELLA \
  Knowledge-Bases \
  --channel TRADITION-STELLA \
  --index-path .mini_indexer/index.json

python3 -m mini_indexer query \
  "cutoff time participant count lookup" \
  --index-path .mini_indexer/index.json \
  --target-uri viking://resources/TRADITION-STELLA/FXD_NDF
```

## 下一步怎么接到真实 TRS

建议按下面顺序继续：

1. 把真实 signoff pack 按 `generated_pack/{CHANNEL}/{PRODUCT}/{PACK}` 组织好
2. 为 pack 级和对象级补齐人工确认的 L0/L1
3. 运行 `scripts/build_mvp_kb.sh`
4. 用真实问题集验证 scoped retrieval：
   - 某个 target field 从哪里来
   - 某个 cutoff 为什么是 14:00 而不是 15:00
   - 某个 pack 覆盖了哪些对象
5. 只有在词项检索明显不足时，再替换成 embedding + rerank

## 什么时候该升级到更接近 OV 的实现

如果出现下面任一情况，说明该从“最小索引机”升级到更完整的 OV-style ingestion/indexing：

- pack 数量上来后，全量重建成本开始明显变高
- 目录级 fallback 摘要不够，需要异步 L0/L1 生成与复用
- 需要真正的相似语义召回，而不是词项召回
- 需要增量更新、watch、或资源变更同步
- 需要把 runtime resource tree 交给多个 agent / 服务共享

在那之前，当前这版原型已经足够验证 TRS 的 runtime projection 边界和检索形状。
