#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle,
    Image, KeepTogether, HRFlowable, ListFlowable, ListItem
)
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas
from pathlib import Path
import html
import textwrap

ROOT = Path('/home/ubuntu/repos/rts')
OUT_DIR = ROOT / 'out'
OUT_DIR.mkdir(exist_ok=True)
PDF_PATH = OUT_DIR / 'rts-ai-business-proposal.pdf'
MD_PATH = OUT_DIR / 'rts-ai-business-proposal.md'
ASSET = ROOT / 'docs/reference/assets/trs-architecture.png'

# Fonts: STSong-Light is a built-in CID font in ReportLab and supports Chinese.
pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
try:
    pdfmetrics.registerFont(TTFont('DejaVuSans', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'))
    LATIN_FONT = 'DejaVuSans'
except Exception:
    LATIN_FONT = 'Helvetica'
CJK_FONT = 'STSong-Light'

PRIMARY = colors.HexColor('#12355B')
ACCENT = colors.HexColor('#1F7A8C')
LIGHT = colors.HexColor('#EEF5F7')
MUTED = colors.HexColor('#6B7280')
DARK = colors.HexColor('#111827')
WARN = colors.HexColor('#B45309')
GREEN = colors.HexColor('#047857')

PAGE_W, PAGE_H = A4


def esc(s):
    return html.escape(str(s)).replace('\n', '<br/>')


def p(txt, style):
    return Paragraph(esc(txt), style)


def bullets(items, style, bulletType='bullet'):
    return ListFlowable([ListItem(p(i, style), leftIndent=0) for i in items], bulletType=bulletType, start='circle', leftIndent=14)


def header_footer(canv: canvas.Canvas, doc):
    canv.saveState()
    if doc.page > 1:
        canv.setStrokeColor(colors.HexColor('#D1D5DB'))
        canv.line(18*mm, PAGE_H-14*mm, PAGE_W-18*mm, PAGE_H-14*mm)
        canv.setFont(CJK_FONT, 8)
        canv.setFillColor(MUTED)
        canv.drawString(18*mm, PAGE_H-10*mm, 'RTS AI Proposal — Client Flow Enablement & Legacy Operations Intelligence')
        canv.drawRightString(PAGE_W-18*mm, 10*mm, f'{doc.page}')
    canv.restoreState()


styles = getSampleStyleSheet()
styles.add(ParagraphStyle(
    name='CoverTitle', fontName=CJK_FONT, fontSize=25, leading=33, textColor=PRIMARY,
    alignment=TA_CENTER, wordWrap='CJK', spaceAfter=12
))
styles.add(ParagraphStyle(
    name='CoverSub', fontName=CJK_FONT, fontSize=13, leading=21, textColor=DARK,
    alignment=TA_CENTER, wordWrap='CJK', spaceAfter=8
))
styles.add(ParagraphStyle(
    name='H1C', fontName=CJK_FONT, fontSize=18, leading=24, textColor=PRIMARY,
    spaceBefore=8, spaceAfter=10, wordWrap='CJK'
))
styles.add(ParagraphStyle(
    name='H2C', fontName=CJK_FONT, fontSize=13.5, leading=18, textColor=ACCENT,
    spaceBefore=10, spaceAfter=6, wordWrap='CJK'
))
styles.add(ParagraphStyle(
    name='BodyC', fontName=CJK_FONT, fontSize=10.2, leading=15.4, textColor=DARK,
    spaceAfter=5, wordWrap='CJK'
))
styles.add(ParagraphStyle(
    name='SmallC', fontName=CJK_FONT, fontSize=8.8, leading=12.5, textColor=DARK,
    wordWrap='CJK'
))
styles.add(ParagraphStyle(
    name='NoteC', fontName=CJK_FONT, fontSize=9.3, leading=13.5, textColor=colors.HexColor('#374151'),
    backColor=LIGHT, borderColor=colors.HexColor('#C7E1E8'), borderWidth=0.5, borderPadding=7,
    wordWrap='CJK', spaceBefore=4, spaceAfter=8
))
styles.add(ParagraphStyle(
    name='QuoteC', fontName=CJK_FONT, fontSize=11.2, leading=17, textColor=PRIMARY,
    leftIndent=8, borderColor=ACCENT, borderWidth=0, borderPadding=0,
    wordWrap='CJK', spaceBefore=3, spaceAfter=8
))
styles.add(ParagraphStyle(
    name='TableHead', fontName=CJK_FONT, fontSize=9, leading=12, textColor=colors.white,
    alignment=TA_CENTER, wordWrap='CJK'
))
styles.add(ParagraphStyle(
    name='TableCell', fontName=CJK_FONT, fontSize=8.3, leading=11.2, textColor=DARK,
    wordWrap='CJK'
))

H1=styles['H1C']; H2=styles['H2C']; BODY=styles['BodyC']; SMALL=styles['SmallC']; NOTE=styles['NoteC']; QUOTE=styles['QuoteC']

story = []

# Cover
story += [Spacer(1, 25*mm)]
story.append(Paragraph('从 Transformation Rule System 到<br/>客户交易流接入智能平台', styles['CoverTitle']))
story.append(Paragraph('AI Token Use Case Proposal', styles['CoverSub']))
story.append(Paragraph('核心思想 · 可拓展案例 · Business Benefits · 为什么 Copilot / Yoda 不可替代 · 为什么需要 API Token', styles['CoverSub']))
story += [Spacer(1, 16*mm)]
cover_box = Table([[p('建议总定位', SMALL), p('AI-assisted Client Flow Enablement & Legacy Operations Intelligence', BODY)],
                   [p('首个落地点', SMALL), p('银行 XML / FpML / SCBML 报文转换与交易流接入场景', BODY)],
                   [p('核心原则', SMALL), p('AI 不改生产、不做最终业务决策；只做受控分析、结构化草稿、证据引用与人工复核前置工作。', BODY)]],
                  colWidths=[35*mm, 115*mm])
cover_box.setStyle(TableStyle([
    ('BACKGROUND',(0,0),(-1,-1),LIGHT), ('BOX',(0,0),(-1,-1),0.7,ACCENT),
    ('INNERGRID',(0,0),(-1,-1),0.25,colors.HexColor('#B9D8E0')),
    ('VALIGN',(0,0),(-1,-1),'TOP'), ('LEFTPADDING',(0,0),(-1,-1),8), ('RIGHTPADDING',(0,0),(-1,-1),8),
    ('TOPPADDING',(0,0),(-1,-1),7), ('BOTTOMPADDING',(0,0),(-1,-1),7)
]))
story.append(cover_box)
story += [Spacer(1, 14*mm)]
story.append(p('Prepared from repository: /home/ubuntu/repos/rts', SMALL))
story.append(p('Source baseline: project-alignment-summary, system-constitution-v1, java-index-layer-full-plan, RTS glossary.', SMALL))
story.append(PageBreak())

# 1 Executive summary
story.append(Paragraph('1. Executive Summary', H1))
story.append(p('RTS 的初始想法不是再做一个聊天机器人，而是把银行报文转换知识从代码、Excel、XSLT、ticket、review 记录中抽象成可治理、可追溯、可检索、可解释的规则真相源。这个思想可以继续向业务侧升级：把 RTS 作为底座，支持客户交易流接入、新产品准备度、客户影响分析和遗留系统运营智能。', BODY))
story.append(Paragraph('一句话定位', H2))
story.append(Paragraph('AI-assisted Client Flow Enablement Platform：用受控 AI 分析客户/上游报文样本、转换规则、UAT defect、incident 和 release evidence，生成接入缺口、准备度、客户影响和治理叙事，帮助业务更快落地交易流。', QUOTE))
story.append(Paragraph('需要避免的误解', H2))
story.append(bullets([
    '不是“Message Transformation Chatbot”：目标不是让人随便问答，而是把 AI 嵌入 onboarding、UAT、release、incident 等流程。',
    '不是让 AI 改规则或改生产：AI 只生成草稿、解释、gap、checklist、impact summary，最终由授权人员确认。',
    '不是 Confluence Q&A：核心输入包含 XML、mapping、rule object、defect、incident、release diff、evidence 等非普通文档材料。',
], BODY))

# 2 RTS core idea
story.append(Paragraph('2. RTS 的核心思想', H1))
story.append(p('RTS 当前仓库中的稳定结论是：Transformation Rule System 是面向银行报文转换场景的规则真相源系统。核心目标不是让 LLM 更会“编”，而是把已审核规则整理成可治理、可追溯、可检索、可解释的结构化真相源，并把 LLM 限制为受约束的读取者、导航者和解释者。', BODY))
core_table = Table([
    [p('设计原则', styles['TableHead']), p('含义', styles['TableHead']), p('业务意义', styles['TableHead'])],
    [p('Truth-first', styles['TableCell']), p('先定义什么是真相，再让 AI 读取。', styles['TableCell']), p('减少错误解释、口头经验和隐性规则带来的业务风险。', styles['TableCell'])],
    [p('Pack completion establishes truth', styles['TableCell']), p('规则包通过 review / signoff 后成为该范围 goldensource。', styles['TableCell']), p('让规则解释可以被审计、复核和复用。', styles['TableCell'])],
    [p('Unknown is better than wrong', styles['TableCell']), p('证据不足时输出未知或歧义，而不是流畅瞎答。', styles['TableCell']), p('适合银行环境，避免“看似合理但不可追溯”的风险。', styles['TableCell'])],
    [p('Layered retrieval', styles['TableCell']), p('先找 channel/product/pack，再读 rule/lookup/helper。', styles['TableCell']), p('避免跨产品线误召回，提升回答稳定性。', styles['TableCell'])],
], colWidths=[35*mm, 70*mm, 65*mm])
core_table.setStyle(TableStyle([
    ('BACKGROUND',(0,0),(-1,0),PRIMARY), ('GRID',(0,0),(-1,-1),0.3,colors.HexColor('#CBD5E1')),
    ('BACKGROUND',(0,1),(-1,-1),colors.white), ('VALIGN',(0,0),(-1,-1),'TOP'),
    ('LEFTPADDING',(0,0),(-1,-1),6), ('RIGHTPADDING',(0,0),(-1,-1),6),
    ('TOPPADDING',(0,0),(-1,-1),5), ('BOTTOMPADDING',(0,0),(-1,-1),5)
]))
story.append(core_table)
story.append(Spacer(1, 6))
story.append(p('技术层面，RTS 可分为 Truth Layer、Projection Layer、Index Layer、Query Layer。TRS 决定什么是真的；投影与索引层决定如何把真相变成稳定可查询、可导航、低噪声的运行时表面。', BODY))
if ASSET.exists():
    story.append(Spacer(1, 4))
    img = Image(str(ASSET))
    img._restrictSize(160*mm, 80*mm)
    story.append(img)
    story.append(p('图：RTS 既有架构资产（来自仓库 docs/reference/assets/trs-architecture.png）', SMALL))

# 3 Business reframing
story.append(PageBreak())
story.append(Paragraph('3. 从技术系统升级为业务能力', H1))
story.append(p('如果只说“报文转换规则知识库”，赞助空间会局限在 IT 支持效率。更适合申请 AI token 和拉业务赞助的说法，是把 RTS 作为底座，升级为“客户交易流接入智能平台”。这样它直接连接 time-to-revenue、client experience、new product launch、operational capacity 和 risk avoidance。', BODY))
position_table = Table([
    [p('旧表达', styles['TableHead']), p('问题', styles['TableHead']), p('新表达', styles['TableHead'])],
    [p('Message Transformation AI', styles['TableCell']), p('听起来像技术团队自用工具。', styles['TableCell']), p('Client Flow Enablement Platform', styles['TableCell'])],
    [p('Support Copilot', styles['TableCell']), p('容易被认为 Copilot / Yoda 已经能做。', styles['TableCell']), p('Workflow-embedded Readiness & Impact Analysis', styles['TableCell'])],
    [p('Knowledge Base Chatbot', styles['TableCell']), p('容易被归类为 Confluence 问答。', styles['TableCell']), p('Structured analysis over XML, rules, defects and operational evidence', styles['TableCell'])],
], colWidths=[50*mm, 60*mm, 60*mm])
position_table.setStyle(TableStyle([
    ('BACKGROUND',(0,0),(-1,0),ACCENT), ('GRID',(0,0),(-1,-1),0.3,colors.HexColor('#CBD5E1')),
    ('VALIGN',(0,0),(-1,-1),'TOP'), ('LEFTPADDING',(0,0),(-1,-1),6), ('RIGHTPADDING',(0,0),(-1,-1),6),
    ('TOPPADDING',(0,0),(-1,-1),5), ('BOTTOMPADDING',(0,0),(-1,-1),5)
]))
story.append(position_table)
story.append(Paragraph('推荐总主题', H2))
story.append(Paragraph('AI-assisted Client Flow Enablement & Legacy Operations Intelligence', QUOTE))
story.append(p('中文可说成：客户交易流接入与遗留系统运营智能。第一阶段用报文转换平台作为 pilot，因为它有丰富的 XML 样本、mapping 规则、exception、UAT defect、release evidence 和历史支持记录。', BODY))

# 4 Use cases
story.append(Paragraph('4. 可拓展案例：从 RTS 延伸出的 7 个业务场景', H1))
use_cases = [
    ('1. Client / Upstream Onboarding Accelerator', '分析客户/上游 XML 样本、目标 schema、现有 mapping 和历史 defect，生成 field mapping gap、missing information checklist、client clarification questions、UAT readiness checklist。', '缩短客户接入周期，提升 time-to-revenue。'),
    ('2. Product / Flow Readiness Analyzer', '输入新产品或新交易流需求，评估现有报文基础设施支持度、缺失字段、可复用规则、受影响团队和测试样本。', '加快新产品 launch 和市场响应。'),
    ('3. Client Impact Intelligence', '把 failed / delayed messages、client id、product type、SLA、exception reason 转成受影响客户、产品、地区、SLA 风险和沟通摘要。', '提升客户体验，降低声誉风险。'),
    ('4. Change Intent Verification', '比对 Jira / release note 声明意图与实际 mapping / rule / code diff 变化，发现“说是小改、实际影响更大”的 gap。', '提升 release governance，降低生产变更风险。'),
    ('5. Regulatory / Audit Explainability Layer', '把低层 XML path、lookup、helper、rule dependency 翻译成审计可读的转换解释，并引用 evidence / signoff 状态。', '提升 auditability、traceability 和监管解释能力。'),
    ('6. Institutional Knowledge Recovery', '从 legacy code、mapping Excel、ticket、邮件、incident 中恢复隐性业务知识和历史 workaround。', '降低 key-person dependency，保护组织知识。'),
    ('7. Knowledge Decay Detector', '比较 runbook、Confluence、实际规则、release 变化和 incident 处理方式，发现过期、矛盾、误导性知识。', '减少错误操作和重复沟通，提升运营稳定性。'),
]
uc_data = [[p('场景', styles['TableHead']), p('AI 做什么', styles['TableHead']), p('Business Benefit', styles['TableHead'])]]
for a,b,c in use_cases:
    uc_data.append([p(a, styles['TableCell']), p(b, styles['TableCell']), p(c, styles['TableCell'])])
uc_table = Table(uc_data, colWidths=[45*mm, 85*mm, 40*mm], repeatRows=1)
uc_table.setStyle(TableStyle([
    ('BACKGROUND',(0,0),(-1,0),PRIMARY), ('GRID',(0,0),(-1,-1),0.25,colors.HexColor('#CBD5E1')),
    ('VALIGN',(0,0),(-1,-1),'TOP'), ('LEFTPADDING',(0,0),(-1,-1),5), ('RIGHTPADDING',(0,0),(-1,-1),5),
    ('TOPPADDING',(0,0),(-1,-1),4), ('BOTTOMPADDING',(0,0),(-1,-1),4),
    ('BACKGROUND',(0,1),(-1,-1),colors.white),
]))
story.append(uc_table)
story.append(p('建议第一阶段优先押前三个：Onboarding Accelerator、Product/Flow Readiness、Client Impact Intelligence。它们最容易被业务 sponsor 理解，也最容易扩展需求。', NOTE))

# 5 Benefits
story.append(PageBreak())
story.append(Paragraph('5. Business Benefits：为什么值得赞助', H1))
story.append(p('赞助方关心的不是“AI 能不能回答问题”，而是它能否带来可感知的业务价值。RTS 的业务化包装应围绕以下收益指标。', BODY))
benefit_rows = [
    ('Time-to-revenue', '新客户/新上游交易流接入更快，UAT 准备更早完成。', 'Onboarding cycle time、UAT defect turnaround、go-live lead time'),
    ('Revenue enablement', '支持新产品、新市场、新交易结构的准备度评估。', 'readiness assessment lead time、supported flow coverage'),
    ('Client experience', '生产异常时更快定位客户影响，生成安全可控的沟通摘要。', 'impact assessment time、client update SLA、repeat query reduction'),
    ('Operational capacity', '减少 BA/Tech/SME 在字段解释、规则查找、历史问题回溯上的消耗。', 'SME hours saved、handoff count、clarification cycles'),
    ('Risk avoidance', '发现变更意图偏差、知识过期、隐藏依赖，减少低估风险。', 'missed impact count、release review findings、post-release issue rate'),
    ('Audit readiness', '把规则和证据关系整理成可解释 narrative。', 'audit evidence preparation time、traceability coverage'),
]
benefit_table = Table([[p('收益维度', styles['TableHead']), p('业务价值', styles['TableHead']), p('可衡量指标', styles['TableHead'])]] + [[p(a, styles['TableCell']), p(b, styles['TableCell']), p(c, styles['TableCell'])] for a,b,c in benefit_rows], colWidths=[42*mm, 80*mm, 48*mm], repeatRows=1)
benefit_table.setStyle(TableStyle([
    ('BACKGROUND',(0,0),(-1,0),GREEN), ('GRID',(0,0),(-1,-1),0.25,colors.HexColor('#CBD5E1')),
    ('VALIGN',(0,0),(-1,-1),'TOP'), ('LEFTPADDING',(0,0),(-1,-1),5), ('RIGHTPADDING',(0,0),(-1,-1),5),
    ('TOPPADDING',(0,0),(-1,-1),5), ('BOTTOMPADDING',(0,0),(-1,-1),5),
]))
story.append(benefit_table)
story.append(Paragraph('赞助话术', H2))
story.append(Paragraph('This is not an IT chatbot. It is a business enablement capability that reduces the time needed to onboard new client flows, assess product readiness, and understand client impact from message issues.', QUOTE))

# 6 Why not Copilot/Yoda
story.append(Paragraph('6. 为什么 Copilot / Yoda 不能替代', H1))
story.append(p('关键不是说 Copilot 或 Yoda 不好，而是说明它们的产品定位不同：Copilot 是个人生产力助手，Yoda 是 Confluence 知识库问答；本项目需要的是可被业务流程调用、可处理结构化和半结构化交易流材料、可输出标准化结果的后台 AI capability。', BODY))
compare = Table([
    [p('能力/要求', styles['TableHead']), p('Copilot', styles['TableHead']), p('Yoda', styles['TableHead']), p('API Token 方案', styles['TableHead'])],
    [p('定位', styles['TableCell']), p('个人办公与内容生成。', styles['TableCell']), p('Confluence page 知识问答。', styles['TableCell']), p('嵌入 onboarding/UAT/release/incident 的流程组件。', styles['TableCell'])],
    [p('数据类型', styles['TableCell']), p('适合用户手动提供文档片段。', styles['TableCell']), p('主要依赖已索引页面。', styles['TableCell']), p('XML/FpML/SCBML、mapping Excel、rule object、defect、ticket、release diff、exception pattern。', styles['TableCell'])],
    [p('执行方式', styles['TableCell']), p('人工打开、人工提问。', styles['TableCell']), p('人工问知识库问题。', styles['TableCell']), p('系统自动调用、批处理、按固定 schema 输出。', styles['TableCell'])],
    [p('输出控制', styles['TableCell']), p('输出格式随 prompt 和用户变化。', styles['TableCell']), p('以问答摘要为主。', styles['TableCell']), p('固定模板/JSON/schema validation/retry/human review。', styles['TableCell'])],
    [p('审计与追踪', styles['TableCell']), p('难以作为流程证据链。', styles['TableCell']), p('回答基于文档检索，非流程级 evidence trace。', styles['TableCell']), p('记录 input source、prompt version、model version、output、reviewer、evidence links。', styles['TableCell'])],
    [p('结论', styles['TableCell']), p('适合个人提效，不适合作为受控业务分析服务。', styles['TableCell']), p('适合查文档，不适合分析新报文样本和运行时证据。', styles['TableCell']), p('适合构建专用、可治理、可集成的 domain analysis capability。', styles['TableCell'])],
], colWidths=[32*mm, 42*mm, 42*mm, 54*mm], repeatRows=1)
compare.setStyle(TableStyle([
    ('BACKGROUND',(0,0),(-1,0),PRIMARY), ('GRID',(0,0),(-1,-1),0.25,colors.HexColor('#CBD5E1')),
    ('VALIGN',(0,0),(-1,-1),'TOP'), ('LEFTPADDING',(0,0),(-1,-1),4), ('RIGHTPADDING',(0,0),(-1,-1),4),
    ('TOPPADDING',(0,0),(-1,-1),4), ('BOTTOMPADDING',(0,0),(-1,-1),4),
]))
story.append(compare)
story.append(p('可以对外解释为：Copilot and Yoda help users retrieve or draft information; API access is needed to turn AI into a governed workflow component that analyzes transaction-flow artifacts and produces standardized business readiness outputs.', NOTE))

# 7 Why API token
story.append(PageBreak())
story.append(Paragraph('7. 为什么需要 API Key / Token', H1))
story.append(p('申请 API token 的理由应聚焦在“能力层”和“流程集成”，而不是“我要另一个聊天入口”。API token 允许系统在受控边界内调用模型，对交易流材料做标准化分析，并把结果写回工作流。', BODY))
api_points = [
    ('Workflow integration', '上传客户 XML 样本后自动触发 gap analysis，生成 BA review checklist 或 Jira draft。'),
    ('Structured input/output', '可传入 XML、JSON metadata、mapping extract、defect list，并要求模型输出固定 schema。'),
    ('Repeatability', '统一 prompt template、model parameter、output validation，避免每个人问法不同。'),
    ('Auditability', '记录 input source、prompt version、model version、output、reviewer、approval status。'),
    ('Security & governance', '可做 redaction、scope control、human-in-the-loop、no-production-decision guardrail。'),
    ('Productization', '未来可以接入 onboarding portal、Jira、release tool、message archive、log platform。'),
]
api_table = Table([[p('API Token 需求', styles['TableHead']), p('具体原因', styles['TableHead'])]] + [[p(a, styles['TableCell']), p(b, styles['TableCell'])] for a,b in api_points], colWidths=[55*mm, 115*mm])
api_table.setStyle(TableStyle([
    ('BACKGROUND',(0,0),(-1,0),ACCENT), ('GRID',(0,0),(-1,-1),0.25,colors.HexColor('#CBD5E1')),
    ('VALIGN',(0,0),(-1,-1),'TOP'), ('LEFTPADDING',(0,0),(-1,-1),6), ('RIGHTPADDING',(0,0),(-1,-1),6),
    ('TOPPADDING',(0,0),(-1,-1),5), ('BOTTOMPADDING',(0,0),(-1,-1),5),
]))
story.append(api_table)
story.append(Paragraph('边界承诺', H2))
story.append(bullets([
    'AI 不直接修改 production rule、mapping、configuration 或 code。',
    'AI 不替代 BA/Tech/MO 的最终判断或 signoff。',
    'AI 输出只作为 analysis draft / checklist / narrative / recommendation，必须人工复核。',
    '对证据不足的问题保持 unknown / ambiguity，不生成确定结论。',
], BODY))

# 8 Proposed pilot
story.append(Paragraph('8. 建议 Pilot 范围', H1))
story.append(p('为了让项目更容易获批，建议第一阶段不要做大而全的平台，而是用 RTS 现有材料做一个可演示的轻量 pilot：Client Flow Readiness Pack。', BODY))
pilot_rows = [
    ('输入', '1-3 组客户/上游 sample XML；现有 TRS rule/lookup/helper；mapping extract；历史 UAT defect；少量 incident 摘要。'),
    ('处理', '字段抽取 → 与目标 schema / existing mapping 对比 → 规则复用识别 → 缺口和澄清点生成 → UAT checklist。'),
    ('输出 1', 'Onboarding Gap Report：缺失字段、unsupported fields、疑似 mapping reuse、需要客户确认的问题。'),
    ('输出 2', 'Readiness Assessment：supported / partially supported / not supported，复杂度和风险说明。'),
    ('输出 3', 'Client-safe Communication Draft：把技术缺口转换成客户可读、合规谨慎的沟通草稿。'),
    ('验收指标', '是否减少初步分析时间；是否覆盖历史缺陷；BA/Tech 是否认为 checklist 可用；输出是否可追溯到 evidence。'),
]
pilot_table = Table([[p('模块', styles['TableHead']), p('内容', styles['TableHead'])]] + [[p(a, styles['TableCell']), p(b, styles['TableCell'])] for a,b in pilot_rows], colWidths=[40*mm, 130*mm])
pilot_table.setStyle(TableStyle([
    ('BACKGROUND',(0,0),(-1,0),PRIMARY), ('GRID',(0,0),(-1,-1),0.25,colors.HexColor('#CBD5E1')),
    ('VALIGN',(0,0),(-1,-1),'TOP'), ('LEFTPADDING',(0,0),(-1,-1),6), ('RIGHTPADDING',(0,0),(-1,-1),6),
    ('TOPPADDING',(0,0),(-1,-1),5), ('BOTTOMPADDING',(0,0),(-1,-1),5),
]))
story.append(pilot_table)
story.append(Paragraph('4-6 周交付建议', H2))
story.append(bullets([
    'Week 1：冻结 use case、输入输出 schema、样本范围和安全边界。',
    'Week 2：准备 sample XML、mapping extract、TRS rule projection、历史 defect。',
    'Week 3-4：实现 prompt template、structured output、evidence links、人工复核页面或报告模板。',
    'Week 5：用历史 onboarding / UAT case 回放验证。',
    'Week 6：整理 sponsor demo、benefit estimate、下一阶段需求 backlog。',
], BODY))

# 9 stakeholder ask
story.append(PageBreak())
story.append(Paragraph('9. Sponsor Narrative：怎么对业务方讲', H1))
story.append(Paragraph('Problem', H2))
story.append(p('新客户、新上游系统、新交易流或新产品接入时，团队需要反复读取报文样本、mapping、规则、UAT defect 和历史问题，才能判断“能不能接、缺什么、要问客户什么、UAT 怎么测”。这导致 onboarding 周期长、SME 依赖强、客户沟通慢、上线准备度不透明。', BODY))
story.append(Paragraph('Proposal', H2))
story.append(p('使用 AI token 构建一个受控的 Client Flow Enablement capability。它不是聊天机器人，而是流程中的分析组件：读取交易流材料，生成标准化 gap analysis、readiness checklist、client clarification questions 和 impact summary。', BODY))
story.append(Paragraph('Business Outcome', H2))
story.append(bullets([
    '缩短新客户/新交易流接入评估时间。',
    '更快发现缺失字段、unsupported mapping 和 UAT 风险。',
    '减少 BA/Tech/SME 反复解释和手工比对。',
    '生产问题发生时更快形成客户影响摘要和沟通草稿。',
    '为后续新产品 readiness、release governance、audit explainability 打开需求空间。',
], BODY))
story.append(Paragraph('Suggested English pitch', H2))
story.append(Paragraph('We propose to use API-based AI capability to build a controlled Client Flow Enablement assistant for banking message platforms. The assistant will analyze client message samples, transformation rules, mapping extracts, UAT defects and incident records to generate onboarding gap reports, readiness assessments, clarification checklists and client impact summaries. It will not make production decisions or modify system behavior; all outputs are evidence-linked drafts for human review. This can reduce onboarding cycle time, improve time-to-revenue, reduce SME dependency and improve client experience.', QUOTE))

# 10 roadmap
story.append(Paragraph('10. Roadmap：从 RTS 到平台化需求', H1))
roadmap = Table([
    [p('阶段', styles['TableHead']), p('目标', styles['TableHead']), p('业务可见产物', styles['TableHead'])],
    [p('Phase 0 — RTS Baseline', styles['TableCell']), p('整理 rule / lookup / helper、signoff 边界、runtime projection。', styles['TableCell']), p('可信规则底座；可解释 transformation truth。', styles['TableCell'])],
    [p('Phase 1 — Onboarding Pilot', styles['TableCell']), p('客户 XML 样本 + existing rules → gap analysis / questions / UAT checklist。', styles['TableCell']), p('客户接入评估报告；demo 给 onboarding / ops / product。', styles['TableCell'])],
    [p('Phase 2 — Product Readiness', styles['TableCell']), p('对新产品/交易流做支持度评估和测试范围推荐。', styles['TableCell']), p('产品准备度 scorecard；新产品 launch support。', styles['TableCell'])],
    [p('Phase 3 — Client Impact', styles['TableCell']), p('生产异常转成客户影响、SLA 风险和沟通草稿。', styles['TableCell']), p('Incident impact summary；client service toolkit。', styles['TableCell'])],
    [p('Phase 4 — Governance Expansion', styles['TableCell']), p('变更意图验证、知识过期检测、审计解释层。', styles['TableCell']), p('Release governance、audit readiness、legacy operations intelligence。', styles['TableCell'])],
], colWidths=[42*mm, 75*mm, 53*mm])
roadmap.setStyle(TableStyle([
    ('BACKGROUND',(0,0),(-1,0),GREEN), ('GRID',(0,0),(-1,-1),0.25,colors.HexColor('#CBD5E1')),
    ('VALIGN',(0,0),(-1,-1),'TOP'), ('LEFTPADDING',(0,0),(-1,-1),5), ('RIGHTPADDING',(0,0),(-1,-1),5),
    ('TOPPADDING',(0,0),(-1,-1),5), ('BOTTOMPADDING',(0,0),(-1,-1),5),
]))
story.append(roadmap)

# 11 appendices
story.append(PageBreak())
story.append(Paragraph('Appendix A — 可直接放进申请的短文案', H1))
story.append(Paragraph('Use Case Name', H2))
story.append(p('AI-assisted Client Flow Enablement & Readiness Analysis', BODY))
story.append(Paragraph('Description', H2))
story.append(p('The proposed use case is to build a controlled AI-assisted capability for client flow enablement on top of our banking message transformation platform. The assistant will analyze client or upstream message samples, target schemas, transformation rules, mapping extracts, UAT defects, incident records and release evidence to generate standardized onboarding gap reports, readiness assessments, client clarification checklists, UAT test suggestions and client impact summaries.', BODY))
story.append(Paragraph('Why API Token Is Required', H2))
story.append(p('Existing tools such as Copilot and Yoda are valuable for individual productivity and Confluence-based knowledge retrieval. However, this use case requires a backend AI service embedded into operational workflows. It needs to process structured and semi-structured transaction-flow artifacts beyond Confluence pages, enforce standardized prompts and structured outputs, keep evidence references and support human-in-the-loop review. API token access is required for workflow integration, repeatability, auditability and controlled domain-specific analysis.', BODY))
story.append(Paragraph('Control Statement', H2))
story.append(p('The AI will not make production decisions, approve rules, modify mappings or change system behavior. It will generate analysis drafts and evidence-linked summaries for authorized users to review.', BODY))

story.append(Paragraph('Appendix B — 关键词', H1))
story.append(p('TRS / Transformation Rule System; canonical pack; runtime projection; rule / lookup / helper; evidence / review / signoff; unknown is better than wrong; Client Flow Enablement; Onboarding Gap Analysis; Product Readiness; Client Impact Intelligence; API-based workflow component; governed AI capability.', BODY))

# Markdown source for easy copy/edit
md = r'''
# 从 Transformation Rule System 到客户交易流接入智能平台

## 建议总定位
**AI-assisted Client Flow Enablement & Legacy Operations Intelligence**

核心不是再做一个 chatbot，而是把 RTS 作为银行报文转换规则真相源，继续升级为可嵌入 onboarding、UAT、release、incident 流程的受控 AI 分析能力。

## 核心思想
- RTS 是面向银行报文转换场景的规则真相源系统。
- AI 的角色是 constrained reader / navigator / explainer / ambiguity marker。
- AI 不发明规则、不替代 signoff、不把 unknown 说成 certainty。
- 真相来自 governed pack objects：rules / lookups / helpers + evidence / review / signoff。
- 运行时只投影低噪声、高确定性的规则对象。

## 业务化包装
不要叫 Message Transformation AI 或 Support Copilot。建议叫：

**AI-assisted Client Flow Enablement Platform**

第一阶段模块：
1. Client / Upstream Onboarding Accelerator
2. Product / Flow Readiness Analyzer
3. Client Impact Intelligence

## Business Benefits
- 缩短客户/上游接入周期，提升 time-to-revenue。
- 加快新产品/新交易流支持度评估。
- 减少 BA/Tech/SME 手工比对和反复解释。
- 生产异常时更快输出客户影响和沟通摘要。
- 提升 release governance、auditability 和 legacy knowledge retention。

## 为什么 Copilot / Yoda 不能替代
Copilot 是个人生产力助手；Yoda 是 Confluence 知识库问答。本项目需要的是可被流程调用的后台 AI capability：
- 处理 XML/FpML/SCBML、mapping Excel、rule object、UAT defect、incident、release diff。
- 固定 prompt、固定 output schema、validation、retry、evidence link、human review。
- 嵌入 onboarding / UAT / release / incident，而不是人工打开聊天窗口提问。

## 为什么需要 API Token
API token 用于流程集成、结构化输入输出、可重复执行、审计追踪、安全边界和后续产品化。AI 不直接改生产、不做最终业务决策，只输出草稿和分析结果供人工复核。
'''.strip()
MD_PATH.write_text(md, encoding='utf-8')

# Build PDF
# Use tighter margins for tables.
doc = SimpleDocTemplate(str(PDF_PATH), pagesize=A4, rightMargin=18*mm, leftMargin=18*mm, topMargin=18*mm, bottomMargin=15*mm)
doc.build(story, onFirstPage=header_footer, onLaterPages=header_footer)
print(PDF_PATH)
print(MD_PATH)
