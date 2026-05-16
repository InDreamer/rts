window.RTS_DOCS = [
  {
    "title": "RTS 仓库入口",
    "path": "README.md",
    "category": "入口",
    "summary": "RTS 仓库入口，指向 KB、runtime projection、运行手册和参考材料。",
    "url": "README.html"
  },
  {
    "title": "开发导航",
    "path": "AGENTS.md",
    "category": "开发指引",
    "summary": "面向开发协作的文档索引、阅读顺序和修改边界。",
    "url": "AGENTS.html"
  },
  {
    "title": "RTS 文档总路由",
    "path": "docs/INDEX.md",
    "category": "文档导航",
    "summary": "RTS 文档的主路由，帮助读者进入 confirmed、operational 或 reference 区域。",
    "url": "docs/INDEX.html"
  },
  {
    "title": "已确认基线阅读顺序",
    "path": "docs/confirmed/README.md",
    "category": "已确认基线",
    "summary": "已确认基线文档的权威顺序、阅读触发条件和冲突处理规则。",
    "url": "docs/confirmed/README.html"
  },
  {
    "title": "RTS 项目对齐总纲",
    "path": "docs/confirmed/project-alignment-summary-zh.md",
    "category": "已确认基线",
    "summary": "中文项目总纲，统一 RTS 最终服务愿景、多源真相治理、AI-first review、人工裁决和服务边界",
    "url": "docs/confirmed/project-alignment-summary-zh.html"
  },
  {
    "title": "RTS 系统宪法 v2",
    "path": "docs/confirmed/system-constitution-v1.md",
    "category": "已确认基线",
    "summary": "定义 RTS 作为规则真相服务的核心原则、权限边界和系统纪律。",
    "url": "docs/confirmed/system-constitution-v1.html"
  },
  {
    "title": "KB 到索引的运行时投影契约",
    "path": "docs/confirmed/kb-to-index-projection-contract-zh.md",
    "category": "已确认基线",
    "summary": "定义 KB truth layer 到 query/index layer 的 runtime projection 契约。",
    "url": "docs/confirmed/kb-to-index-projection-contract-zh.html"
  },
  {
    "title": "AI 辅助接入边界说明",
    "path": "docs/confirmed/llm-harness-and-agent-integration-alignment-zh.md",
    "category": "已确认基线",
    "summary": "说明 AI 辅助能力如何读取 runtime projection，以及哪些边界仍由 RTS 服务控制。",
    "url": "docs/confirmed/llm-harness-and-agent-integration-alignment-zh.html"
  },
  {
    "title": "RTS 内部 AI 辅助服务落地计划",
    "path": "docs/confirmed/internal-llm-agent-service-implementation-plan-zh.md",
    "category": "已确认基线",
    "summary": "把 AI 辅助放在 RTS 规则服务边界内落地，避免绕过 KB 和 projection。",
    "url": "docs/confirmed/internal-llm-agent-service-implementation-plan-zh.html"
  },
  {
    "title": "Day1 查询服务与 AI 辅助计划",
    "path": "docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md",
    "category": "已确认基线",
    "summary": "Day1 查询服务、工具接口和基础 AI 辅助能力的实现计划。",
    "url": "docs/confirmed/day1-query-service-and-llm-harness-plan-zh.html"
  },
  {
    "title": "Day2 受控检索演进路线图",
    "path": "docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md",
    "category": "已确认基线",
    "summary": "Day2 在受控 projection 上扩展检索、排序、影响分析和测试建议。",
    "url": "docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.html"
  },
  {
    "title": "长期服务能力路线图",
    "path": "docs/confirmed/final-llm-agent-service-plan-zh.md",
    "category": "已确认基线",
    "summary": "长期服务路线：先稳住规则真相和运行包，再扩展解释、分析和场景能力。",
    "url": "docs/confirmed/final-llm-agent-service-plan-zh.html"
  },
  {
    "title": "RTS Documentation Decision Register",
    "path": "docs/confirmed/document-decision-register-zh.md",
    "category": "已确认基线",
    "summary": "active decision register for resolving outdated, ambiguous, or conflicting RTS documentation directions",
    "url": "docs/confirmed/document-decision-register-zh.html"
  },
  {
    "title": "Runtime Projection 产品说明",
    "path": "docs/confirmed/runtime-projection-product-guide-zh.md",
    "category": "已确认基线",
    "summary": "从产品和集成视角解释 runtime projection 运行包里每类内容解决什么问题。",
    "url": "docs/confirmed/runtime-projection-product-guide-zh.html"
  },
  {
    "title": "Java 服务运行手册",
    "path": "docs/java-service-runbook-zh.md",
    "category": "运行与调用",
    "summary": "本地运行、配置、验证、排错和提交前检查手册。",
    "url": "docs/java-service-runbook-zh.html"
  },
  {
    "title": "API 调用方指南",
    "path": "docs/api-caller-guide-zh.md",
    "category": "运行与调用",
    "summary": "查询、/ask、object、dependency、trace 和 refusal 响应的调用方说明。",
    "url": "docs/api-caller-guide-zh.html"
  },
  {
    "title": "Day1 Sample Pack 测试命令",
    "path": "docs/day1-sample-pack-test-commands.md",
    "category": "运行与调用",
    "summary": "manual curl commands for current sample runtime projection smoke testing",
    "url": "docs/day1-sample-pack-test-commands.html"
  },
  {
    "title": "Reference 文档索引",
    "path": "docs/reference/README.md",
    "category": "背景参考",
    "summary": "route RTS reference materials that support but do not override confirmed baseline",
    "url": "docs/reference/README.html"
  },
  {
    "title": "External Review Constraints Summary",
    "path": "docs/reference/external-review-final-2026-04-20.md",
    "category": "背景参考",
    "summary": "distilled external review constraints for projection release, lifecycle, conflict, trace, permission, and refusal",
    "url": "docs/reference/external-review-final-2026-04-20.html"
  },
  {
    "title": "Java Index Layer Reference Summary",
    "path": "docs/reference/java-index-layer-full-plan-zh.md",
    "category": "背景参考",
    "summary": "distilled Java index/query layer reference; current safe ideas without old infrastructure commitments",
    "url": "docs/reference/java-index-layer-full-plan-zh.html"
  },
  {
    "title": "LLM-Mediated Query And Harness Reference",
    "path": "docs/reference/llm-enhanced-index-and-harness-design-zh.md",
    "category": "背景参考",
    "summary": "历史参考：在索引和服务边界内引入 AI 辅助的设计材料。",
    "url": "docs/reference/llm-enhanced-index-and-harness-design-zh.html"
  },
  {
    "title": "Minimal OV Integration",
    "path": "docs/reference/minimal-ov-integration.md",
    "category": "背景参考",
    "summary": "define the smallest OV integration that improves retrieval without merging systems or risking truth pollution",
    "url": "docs/reference/minimal-ov-integration.html"
  },
  {
    "title": "OV Boundary And Adoptable Surfaces",
    "path": "docs/reference/ov-boundary-and-adoption.md",
    "category": "背景参考",
    "summary": "define the clean boundary between the transformation rule system truth source and OpenViking, and list the safe surfaces to borrow",
    "url": "docs/reference/ov-boundary-and-adoption.html"
  },
  {
    "title": "OV Retrieval Lessons Summary",
    "path": "docs/reference/ov-kb-retrieval-design.md",
    "category": "背景参考",
    "summary": "distilled OpenViking-inspired retrieval lessons for RTS without making OV the runtime baseline",
    "url": "docs/reference/ov-kb-retrieval-design.html"
  },
  {
    "title": "RTS Project Glossary",
    "path": "docs/reference/project-keywords-glossary-zh.md",
    "category": "背景参考",
    "summary": "compact Chinese glossary for current RTS project terms and boundary concepts",
    "url": "docs/reference/project-keywords-glossary-zh.html"
  },
  {
    "title": "RTS AI Token Review Rationale",
    "path": "docs/reference/proposals/ai-token-review/README.md",
    "category": "背景参考",
    "summary": "历史参考：AI token review 的风险原则和保留结论。",
    "url": "docs/reference/proposals/ai-token-review/README.html"
  },
  {
    "title": "RTS Project Background and Pack Model",
    "path": "docs/reference/rts-project-background-and-pack-model.md",
    "category": "背景参考",
    "summary": "RTS project background and pack/object model distilled from the historical handoff prompt",
    "url": "docs/reference/rts-project-background-and-pack-model.html"
  },
  {
    "title": "RTS Publication and Refusal Principles",
    "path": "docs/reference/rts-publication-and-refusal-principles.md",
    "category": "背景参考",
    "summary": "RTS publication, refusal, trace, and release safety principles distilled from external review",
    "url": "docs/reference/rts-publication-and-refusal-principles.html"
  },
  {
    "title": "RTS Retrieval Principles",
    "path": "docs/reference/rts-retrieval-principles.md",
    "category": "背景参考",
    "summary": "RTS retrieval principles distilled from archived OV/KB discussions, expressed without making OV the engineering baseline",
    "url": "docs/reference/rts-retrieval-principles.html"
  },
  {
    "title": "RTS Service Interface and Output Principles",
    "path": "docs/reference/rts-service-interface-and-output-principles.md",
    "category": "背景参考",
    "summary": "RTS service interface and output quality principles distilled from AI service strategy materials",
    "url": "docs/reference/rts-service-interface-and-output-principles.html"
  },
  {
    "title": "Tradition -> Stella FXD.NDF cutoff-fixing split pack",
    "path": "kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split/README.md",
    "category": "KB 知识包",
    "summary": "This pack was reconstructed from photographed VS Code screenshots supplied on 2026-05-06. It captures local demo-signoff canonical rules for the Tradition-to-Stella FXD.NDF fixing subtree:",
    "url": "kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split/README.html"
  },
  {
    "title": "Local Pipeline Ops Reference",
    "path": "kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split/ops-reference.md",
    "category": "KB 知识包",
    "summary": "These ops are lightweight semantic labels used inside this draft pack's logic.pipeline sections.",
    "url": "kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split/ops-reference.html"
  },
  {
    "title": "Closure Check",
    "path": "kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split/reports/closure-check.md",
    "category": "KB 知识包",
    "summary": "Status: not closed.",
    "url": "kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split/reports/closure-check.html"
  },
  {
    "title": "Extraction Report",
    "path": "kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split/reports/extraction-report.md",
    "category": "KB 知识包",
    "summary": "This draft pack was reconstructed from 16 photographed screenshots. macOS Vision OCR was used for first-pass extraction and then corrected against visible YAML fragments.",
    "url": "kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split/reports/extraction-report.html"
  },
  {
    "title": "Review Checklist",
    "path": "kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split/reports/review-checklist.md",
    "category": "KB 知识包",
    "summary": "",
    "url": "kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split/reports/review-checklist.html"
  },
  {
    "title": "AI-first Format Follow-up Notes",
    "path": "docs/ai-first-format-conversation-followup-zh.md",
    "category": "其他活跃文档",
    "summary": "补充说明：AI 可读格式讨论中关于 KB、projection 和展示层的边界澄清。",
    "url": "docs/ai-first-format-conversation-followup-zh.html"
  }
];
