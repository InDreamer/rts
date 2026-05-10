<!-- docmeta
role: entry
layer: 1
parent: null
children:
  - docs/api-caller-guide-zh.md
  - docs/java-service-runbook-zh.md
  - docs/day1-sample-pack-test-commands.md
  - docs/confirmed/README.md
  - docs/reference/README.md
  - docs/archive/README.md
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

- `docs/confirmed/README.md` — 当前 active baseline。先在这里建立 RTS 的双核心定位、truth boundary、managed mode / tool mode 和阶段路线。
- `docs/reference/README.md` — 参考材料。需要历史 rationale、OV 背景、检索原则、术语表、早期 Java 参考方案或旧评审背景时打开；它只做支撑，不覆盖 confirmed baseline。
- `docs/java-service-runbook-zh.md` — 本地运行、配置、测试、排障和服务维护。
- `docs/api-caller-guide-zh.md` — API 调用方如何发起 deterministic truth/information 查询、managed analysis 请求、场景分析请求，并理解降级语义。
- `docs/day1-sample-pack-test-commands.md` — 当前 sample runtime projection 的手工 curl 验证命令。
- `docs/archive/README.md` — 历史材料。只在明确需要追溯旧讨论、旧产物或 archived prototype 时打开。

## Default Rule

For active design and implementation, use confirmed docs first. Reference docs may explain why a decision exists, but they do not override confirmed baseline. Archived docs never override active baseline.

---

## Operational Docs

面向运行、维护和集成人员，按角色选择：

| 文档 | 受众 | 记录什么 |
|---|---|---|
| `docs/java-service-runbook-zh.md` | **运维 / 部署人员** | 启动服务、切换 release、配置环境变量、排查常见问题、提交前检查，以及 LLM disabled 时的降级运行语义 |
| `docs/api-caller-guide-zh.md` | **业务集成方 / 查询调用者 / agent 集成方** | 如何发起 truth/information 查询、managed analysis 请求、scope 说明、响应字段含义、拒答原因与降级行为 |

### 写入选择指引

**应该写入 `java-service-runbook-zh.md` 的内容：**
- 启动命令、环境变量配置（`RTS_STORE_ROOT`、`RTS_LLM_*`、`RTS_ADMIN_API_KEY_HASH` 等）
- Active release 切换步骤（文件方式或 ingest API 方式）
- 端口占用、依赖下载等部署排障
- Admin 权限配置和 ingest API 操作步骤
- 测试执行和提交前检查
- LLM / orchestrator / scenario feature flag 关闭后的降级运行说明

**应该写入 `api-caller-guide-zh.md` 的内容：**
- 新增或变更的 API 入口（path、请求字段、返回字段）
- 新的 scope（channel/product/pack/domain）上线说明
- 响应字段含义变化
- 新的拒答 reason 及其处理建议
- 调用方需要知道的 warnings、限制和降级语义
- truth/information service 与 managed analysis service 的调用边界

**应该写入 `docs/confirmed/` 的内容：**
- 架构决策、系统边界、KB-to-index projection 合约变更
- 双核心定位、managed mode / tool mode、场景正常态与降级态
- Day1/Day2 演进规划调整
- LLM harness、原子能力面和检索策略的设计变更
