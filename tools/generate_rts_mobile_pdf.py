#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from pathlib import Path
import html
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import portrait
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle, KeepTogether, HRFlowable
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.pdfgen import canvas

ROOT = Path('/home/ubuntu/repos/rts')
OUT_DIR = ROOT / 'out'
OUT_DIR.mkdir(exist_ok=True)
PDF_PATH = OUT_DIR / 'rts-ai-foundation-mobile.pdf'
MD_PATH = OUT_DIR / 'rts-ai-foundation-mobile.md'

pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
FONT = 'STSong-Light'

# Phone-friendly PDF: narrow 9:16-ish pages, no wide tables.
PAGE_SIZE = (108 * mm, 192 * mm)
PAGE_W, PAGE_H = PAGE_SIZE

PRIMARY = colors.HexColor('#12355B')
ACCENT = colors.HexColor('#1F7A8C')
GREEN = colors.HexColor('#047857')
ORANGE = colors.HexColor('#B45309')
BG = colors.HexColor('#F5F8FA')
CARD = colors.HexColor('#FFFFFF')
TEXT = colors.HexColor('#111827')
MUTED = colors.HexColor('#6B7280')
LINE = colors.HexColor('#D8E4EA')


def esc(x):
    return html.escape(str(x)).replace('\n', '<br/>')


def para(text, style):
    return Paragraph(esc(text), style)


def footer(canv: canvas.Canvas, doc):
    canv.saveState()
    canv.setFont(FONT, 7)
    canv.setFillColor(MUTED)
    if doc.page > 1:
        canv.drawString(8*mm, 5*mm, 'RTS — Truth-based AI Knowledge Foundation')
    canv.drawRightString(PAGE_W-8*mm, 5*mm, str(doc.page))
    canv.restoreState()

styles = getSampleStyleSheet()
styles.add(ParagraphStyle('TitleC', fontName=FONT, fontSize=21, leading=27, alignment=TA_CENTER, textColor=PRIMARY, wordWrap='CJK', spaceAfter=8))
styles.add(ParagraphStyle('SubC', fontName=FONT, fontSize=10.5, leading=15, alignment=TA_CENTER, textColor=TEXT, wordWrap='CJK', spaceAfter=4))
styles.add(ParagraphStyle('H1C', fontName=FONT, fontSize=15.2, leading=20, textColor=PRIMARY, wordWrap='CJK', spaceBefore=4, spaceAfter=7))
styles.add(ParagraphStyle('H2C', fontName=FONT, fontSize=12.2, leading=16, textColor=ACCENT, wordWrap='CJK', spaceBefore=6, spaceAfter=4))
styles.add(ParagraphStyle('BodyC', fontName=FONT, fontSize=10.4, leading=15.2, textColor=TEXT, wordWrap='CJK', spaceAfter=4))
styles.add(ParagraphStyle('SmallC', fontName=FONT, fontSize=8.5, leading=12.0, textColor=MUTED, wordWrap='CJK', spaceAfter=3))
styles.add(ParagraphStyle('QuoteC', fontName=FONT, fontSize=10.8, leading=16.2, textColor=PRIMARY, wordWrap='CJK', leftIndent=4, rightIndent=2, spaceBefore=4, spaceAfter=5))
styles.add(ParagraphStyle('CardTitle', fontName=FONT, fontSize=11.0, leading=15.0, textColor=PRIMARY, wordWrap='CJK', spaceAfter=3))
styles.add(ParagraphStyle('CardBody', fontName=FONT, fontSize=9.5, leading=13.7, textColor=TEXT, wordWrap='CJK'))
styles.add(ParagraphStyle('Tag', fontName=FONT, fontSize=8.2, leading=10.5, textColor=colors.white, alignment=TA_CENTER, wordWrap='CJK'))

T=styles['TitleC']; SUB=styles['SubC']; H1=styles['H1C']; H2=styles['H2C']; B=styles['BodyC']; S=styles['SmallC']; Q=styles['QuoteC']; CT=styles['CardTitle']; CB=styles['CardBody']; TAG=styles['Tag']

story = []


def rule():
    story.append(HRFlowable(width='100%', thickness=0.6, color=LINE, spaceBefore=4, spaceAfter=7))


def card(title, body, tag=None, tag_color=ACCENT, keep=True):
    rows = []
    if tag:
        rows.append([Paragraph(esc(tag), TAG)])
    rows.append([Paragraph(esc(title), CT)])
    rows.append([Paragraph(esc(body), CB)])
    t = Table(rows, colWidths=[PAGE_W-20*mm])
    cmds = [
        ('BOX',(0,0),(-1,-1),0.5,LINE),
        ('BACKGROUND',(0,0),(-1,-1),CARD),
        ('LEFTPADDING',(0,0),(-1,-1),6), ('RIGHTPADDING',(0,0),(-1,-1),6),
        ('TOPPADDING',(0,0),(-1,-1),5), ('BOTTOMPADDING',(0,0),(-1,-1),5),
        ('VALIGN',(0,0),(-1,-1),'TOP'),
    ]
    if tag:
        cmds += [('BACKGROUND',(0,0),(0,0),tag_color), ('ALIGN',(0,0),(0,0),'CENTER')]
    t.setStyle(TableStyle(cmds))
    obj = KeepTogether([t, Spacer(1,4)]) if keep else t
    story.append(obj)


def section(title):
    story.append(Paragraph(esc(title), H1))

# Cover
story.append(Spacer(1, 14*mm))
story.append(Paragraph('RTS 不是一个 AI 应用', T))
story.append(Paragraph('而是基于真相的 AI Knowledge Foundation', T))
story.append(Spacer(1, 4*mm))
story.append(Paragraph('Truth-based · Governed · Explainable · Extensible', SUB))
story.append(Spacer(1, 7*mm))
card('一句话定位', 'RTS 把银行报文转换知识从 XML、mapping、Java/XSLT、lookup、evidence、review、signoff 中抽象成可治理、可寻址、可检索、可解释的规则对象。', 'CORE', PRIMARY)
card('正确叙事', '不是申请一个 chatbot，而是建设一个 truth-based AI 基座；基座之上可以持续衍生规则解释、影响分析、测试推荐、客户接入、新产品准备度、客户影响、审计解释等 use cases。', 'POSITION', GREEN)
story.append(Spacer(1, 8*mm))
story.append(Paragraph('Mobile-friendly revision · generated from /home/ubuntu/repos/rts', S))
story.append(PageBreak())

section('1. 这次应该怎么改叙事')
story.append(Paragraph('之前的 PDF 偏像“一个 proposal”。这版改成更强的 foundation 叙事：RTS 是底座，use cases 是长在底座上的能力。这样更能解释为什么它长期有价值，也更能拉赞助。', B))
card('不要把 RTS 锁死成单一 use case', '如果 RTS 被描述成“报文转换问答”或“support copilot”，很容易被质疑：Yoda / Copilot 不是也能问吗？', 'AVOID', ORANGE)
card('要把 RTS 说成 use-case generator', 'RTS 的价值在于：它先建立可信、可追溯、可检索的规则真相层；有了这个层，上面可以安全地产生很多 AI 应用。', 'TARGET', GREEN)
story.append(Paragraph('推荐总句：', H2))
story.append(Paragraph('RTS is not a single AI use case. RTS is a truth-based AI knowledge foundation that enables many safe, rule-grounded AI applications.', Q))
story.append(PageBreak())

section('2. 三层 use case landscape')
story.append(Paragraph('建议在 RTS 文档和申请材料里使用“三层结构”。它比罗列案例更有说服力，因为它说明：哪些能力是基座，哪些进入交付流程，哪些面向业务赞助。', B))
card('Layer 1 — Foundation Capabilities', '证明 RTS 作为真相基座必须存在：规则解释、字段血缘、依赖导航、证据支撑、ambiguity/signoff-aware answering。', 'FOUNDATION', PRIMARY)
card('Layer 2 — Delivery & Governance Workflows', '证明 RTS 可以进入真实交付流程：影响分析、测试推荐、发布准备度、incident 解释、变更意图校验。', 'WORKFLOW', ACCENT)
card('Layer 3 — Business-facing Applications', '证明 RTS 可以拉业务赞助：客户/上游接入、新产品准备度、客户影响分析、审计解释、知识恢复。', 'BUSINESS', GREEN)
story.append(PageBreak())

section('3. Layer 1：必须放进 RTS 的核心能力')
story.append(Paragraph('这些不是上层应用，而是 RTS 的基础能力。它们直接证明：为什么需要 truth-based knowledge base，而不是普通 RAG / chatbot。', B))
core = [
    ('Transformation Rule Explainer', '回答 target field 从哪里来、为什么这样生成、依赖哪个 lookup/helper、是否已 signoff。'),
    ('Field Lineage / Source-to-Target Traceability', '从 source XML path 追到 transformation logic，再追到 target XML path；也支持反向查询 source field 影响哪些 target fields。'),
    ('Rule Dependency Navigation', '把 rule、lookup、helper 变成可寻址依赖图：改一个 lookup，会影响哪些 rule / pack / product。'),
    ('Evidence-backed Explanation', '解释必须指向规则对象、证据、review 状态，不能只给流畅自然语言。'),
    ('Ambiguity / Signoff-aware Answering', '把 unknown、open item、未 signoff 显式保留下来。RTS 的招牌原则是：Unknown is better than wrong。'),
]
for title, body in core:
    card(title, body, 'CORE', PRIMARY)
story.append(PageBreak())

section('4. Layer 2：交付与治理流程')
story.append(Paragraph('这些 use cases 适合放在 “RTS-enabled workflows” 章节，证明 RTS 不只是查规则，还能提升交付质量和治理能力。', B))
workflow = [
    ('Rule-grounded Impact Analysis', '基于结构化依赖，而不是让 AI 看文档猜：rule / lookup / helper 改动会影响哪些字段、pack、产品、测试范围。'),
    ('Regression Test Recommendation', '根据受影响的 rule、target fields、examples 和 dependency，推荐 regression scope、edge cases 和测试样例。'),
    ('Release Readiness Review', '检查本次 release 是否有未关闭 ambiguity、signoff 缺口、测试 evidence 缺口，以及 declared intent 与实际 rule change 是否一致。'),
    ('Rule-grounded Incident Explanation', '把 runtime symptom 连接到 governed rule object：失败字段对应哪条 rule、哪个 lookup/helper、是否历史上有同类 ambiguity。'),
    ('Change Intent Verification', '比对 Jira/release note 声明意图与实际规则变化，发现“说是小改，实际影响更大”的风险。'),
]
for title, body in workflow:
    card(title, body, 'WORKFLOW', ACCENT)
story.append(PageBreak())

section('5. Layer 3：业务赞助型应用')
story.append(Paragraph('这些应用更容易让业务方看到 benefit。它们不应该被说成 RTS 本体，而应该说成“建立在 RTS 之上的业务能力”。', B))
business = [
    ('Client / Upstream Onboarding Accelerator', '用新客户/上游 XML 样本对比 RTS 的 approved transformation expectations，生成 mapping gap、missing field、clarification questions、UAT checklist。Business benefit：缩短接入周期，提升 time-to-revenue。'),
    ('Product / Flow Readiness Analyzer', '评估新产品或新交易流在现有规则基座上的支持度：supported / partially supported / not supported，缺什么规则，哪些 pack 可复用。Business benefit：加快 new product launch。'),
    ('Client Impact Intelligence', 'RTS 提供语义层，把 technical failure 转成业务概念；再结合 client、trade、SLA 数据生成客户影响摘要。Business benefit：提升 client experience，降低声誉风险。'),
    ('Regulatory / Audit Explainability Layer', '把 XML path、lookup、helper、rule dependency 翻译成审计可读解释，并保留 evidence / signoff 边界。Business benefit：auditability / traceability。'),
    ('Institutional Knowledge Recovery', 'AI 可从 legacy code、Excel、ticket、邮件中提取候选知识；但只有经过 review/signoff 才进入 RTS truth。Business benefit：降低 key-person dependency。'),
]
for title, body in business:
    card(title, body, 'BUSINESS', GREEN, keep=False)
story.append(PageBreak())

section('6. 哪些暂时只放 Future Opportunities')
story.append(Paragraph('这些方向有想象力，但不建议放在 RTS core。它们更依赖 incident/log/ticket/ops 数据，适合放在后续机会。', B))
future = [
    ('On-call Handover Assistant', '主要依赖 incident、ticket、log，不是规则真相源本身。'),
    ('Message Quality Radar', '更像 monitoring / analytics，RTS 只能提供语义解释层。'),
    ('Knowledge Decay Detector', '很好，但适合 Phase 2/3；需要比较文档、规则、运行时行为和历史处理。'),
    ('Silent Dependency Discovery', '偏组织风险和流程挖掘，可作为 future business expansion。'),
    ('Operational Friction Intelligence', '适合更大范围 operations intelligence，不适合当前主线。'),
]
for title, body in future:
    card(title, body, 'FUTURE', ORANGE)
story.append(PageBreak())

section('7. Copilot / Yoda 为什么不能替代')
story.append(Paragraph('这部分要和 foundation 叙事绑定：Copilot / Yoda 可以帮助人写或查，但不能建立和执行 truth-based rule foundation。', B))
card('Copilot 的定位', '个人生产力助手。适合写邮件、总结文档、辅助个人分析；不适合成为业务流程里的受控后台 AI 服务。', 'COPILOT', ORANGE)
card('Yoda 的定位', 'Confluence 知识库问答。适合问已索引 page；不适合分析 XML、mapping、rule object、UAT defect、incident、release diff 等非页面材料。', 'YODA', ORANGE)
card('RTS + API Token 的定位', '可嵌入流程的 governed AI capability：固定输入模板、固定输出 schema、evidence links、review log、human-in-the-loop、scope control。', 'API', GREEN)
story.append(Paragraph('最关键一句：', H2))
story.append(Paragraph('Copilot and Yoda help users retrieve or draft information; API access is needed to turn AI into a governed workflow component grounded in RTS truth.', Q))
story.append(PageBreak())

section('8. 为什么需要 API Token')
story.append(Paragraph('申请 token 不要说“我要试 AI”。要说：我们要把 AI 作为受控能力嵌入 RTS-enabled workflows。', B))
api = [
    ('Workflow integration', '系统上传 sample XML 后自动触发 gap analysis，而不是人工开聊天窗口。'),
    ('Structured data', '输入 XML / JSON metadata / mapping extract / defect list；输出固定 JSON 或报告模板。'),
    ('Repeatability', '统一 prompt version、model parameter、output schema、validation 和 retry。'),
    ('Auditability', '记录 input source、model version、prompt version、output、reviewer、approval status。'),
    ('Governance', '支持 redaction、scope control、human review；AI 不改生产、不做最终 signoff。'),
    ('Extensibility', '同一 RTS foundation 后续可接 onboarding、release、incident、audit、product readiness 多个场景。'),
]
for title, body in api:
    card(title, body, 'TOKEN', PRIMARY)
story.append(PageBreak())

section('9. 推荐放进 RTS 文档的结构')
story.append(Paragraph('建议在 RTS repo 里新增一章或一份文档：RTS-enabled AI Use Case Landscape。结构如下：', B))
card('1. Foundation Capabilities', 'Rule Explainer · Field Lineage · Dependency Navigation · Evidence-backed Explanation · Ambiguity-aware Answering', 'DOC', PRIMARY)
card('2. Delivery & Governance Workflows', 'Impact Analysis · Regression Test Recommendation · Release Readiness · Incident Explanation · Change Intent Verification', 'DOC', ACCENT)
card('3. Business-facing Applications', 'Client Onboarding · Product Readiness · Client Impact · Audit Explainability · Knowledge Recovery', 'DOC', GREEN)
card('4. Future Opportunities', 'On-call Handover · Message Quality Radar · Knowledge Decay · Silent Dependency · Operational Friction', 'DOC', ORANGE)
story.append(PageBreak())

section('10. 可直接使用的英文文案')
story.append(Paragraph('RTS is not a single AI use case. RTS is a truth-based AI knowledge foundation for banking message transformation.', Q))
story.append(Paragraph('It converts fragmented transformation knowledge — XML paths, mapping rules, Java/XSLT logic, lookup tables, evidence, review comments and signoff status — into governed, addressable, retrievable rule objects.', Q))
story.append(Paragraph('Once this foundation exists, multiple AI applications can be safely built on top of it: rule explanation, field lineage, impact analysis, regression test recommendation, release readiness, onboarding gap analysis, product readiness, client impact intelligence and audit explainability.', Q))
story.append(Paragraph('Without RTS, these applications would rely on raw documents or generic RAG and would risk hallucination, wrong scope, missing signoff boundaries and unsupported answers.', Q))
rule()
story.append(Paragraph('Control statement:', H2))
story.append(Paragraph('The AI will not make production decisions, approve rules, modify mappings or change system behavior. It will generate evidence-linked analysis drafts for authorized human review.', Q))

md = '''# RTS — Truth-based AI Knowledge Foundation

RTS 不是一个 AI 应用，而是基于真相的 AI Knowledge Foundation。

## 三层 Use Case Landscape

### Layer 1 — Foundation Capabilities
- Transformation Rule Explainer
- Field Lineage / Source-to-Target Traceability
- Rule Dependency Navigation
- Evidence-backed Explanation
- Ambiguity / Signoff-aware Answering

### Layer 2 — Delivery & Governance Workflows
- Rule-grounded Impact Analysis
- Regression Test Recommendation
- Release Readiness Review
- Rule-grounded Incident Explanation
- Change Intent Verification

### Layer 3 — Business-facing Applications
- Client / Upstream Onboarding Accelerator
- Product / Flow Readiness Analyzer
- Client Impact Intelligence
- Regulatory / Audit Explainability Layer
- Institutional Knowledge Recovery

## Future Opportunities
- On-call Handover Assistant
- Message Quality Radar
- Knowledge Decay Detector
- Silent Dependency Discovery
- Operational Friction Intelligence

## 核心叙事
RTS is not a single AI use case. RTS is a truth-based AI knowledge foundation that enables many safe, rule-grounded AI applications.

## 为什么 Copilot / Yoda 不可替代
Copilot 是个人生产力助手；Yoda 是 Confluence 知识库问答。RTS + API Token 是可嵌入流程的 governed AI capability，能处理 XML、mapping、rule object、defect、incident、release evidence，并输出固定 schema、evidence-linked、human-reviewed 的结果。
'''.strip()
MD_PATH.write_text(md, encoding='utf-8')

doc = SimpleDocTemplate(str(PDF_PATH), pagesize=PAGE_SIZE, leftMargin=8*mm, rightMargin=8*mm, topMargin=8*mm, bottomMargin=9*mm)
doc.build(story, onFirstPage=footer, onLaterPages=footer)
print(PDF_PATH)
print(MD_PATH)
