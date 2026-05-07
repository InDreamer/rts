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

---

## Operational Docs

面向运行、维护和集成人员，按角色选择：

| 文档 | 受众 | 记录什么 |
|---|---|---|
| `docs/java-service-runbook-zh.md` | **运维 / 部署人员** | 启动服务、切换 release、配置环境变量、排查常见问题、提交前检查 |
| `docs/api-caller-guide-zh.md` | **业务集成方 / 查询调用者** | 如何发起查询、scope 说明、响应字段含义、拒答原因处理 |

### 写入选择指引

**应该写入 `java-service-runbook-zh.md` 的内容：**
- 启动命令、环境变量配置（`RTS_STORE_ROOT`、`RTS_LLM_*`、`RTS_ADMIN_API_KEY_HASH` 等）
- Active release 切换步骤（文件方式或 ingest API 方式）
- 端口占用、依赖下载等部署排障
- Admin 权限配置和 ingest API 操作步骤
- 测试执行和提交前检查

**应该写入 `api-caller-guide-zh.md` 的内容：**
- 新增或变更的 API 入口（path、请求字段、返回字段）
- 新的 scope（channel/product/pack/domain）上线说明
- 响应字段含义变化
- 新的拒答 reason 及其处理建议
- 调用方需要知道的 warnings 和限制（如 draft 标注语义）

**应该写入 `docs/confirmed/` 的内容：**
- 架构决策、系统边界、KB-to-index projection 合约变更
- Day1/Day2 演进规划调整
- LLM harness、检索策略的设计变更
