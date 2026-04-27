#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from pathlib import Path
import html
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle, KeepTogether, HRFlowable
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.pdfgen import canvas

ROOT = Path('/home/ubuntu/repos/rts')
OUT_DIR = ROOT / 'out'
OUT_DIR.mkdir(exist_ok=True)
PDF_PATH = OUT_DIR / 'rts-domain-architecture-proposal-cn-v2.pdf'
MD_PATH = OUT_DIR / 'rts-domain-architecture-proposal-cn-v2.md'

pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
FONT = 'STSong-Light'
PAGE_SIZE = (112 * mm, 198 * mm)  # phone-friendly, slightly wider than previous version
PAGE_W, PAGE_H = PAGE_SIZE

PRIMARY = colors.HexColor('#12355B')
ACCENT = colors.HexColor('#1F7A8C')
GREEN = colors.HexColor('#047857')
ORANGE = colors.HexColor('#B45309')
PURPLE = colors.HexColor('#6D28D9')
CARD = colors.HexColor('#FFFFFF')
TEXT = colors.HexColor('#111827')
MUTED = colors.HexColor('#6B7280')
LINE = colors.HexColor('#D8E4EA')
LIGHT = colors.HexColor('#F3F7FA')


def esc(x):
    return html.escape(str(x)).replace('\n', '<br/>')


def footer(canv: canvas.Canvas, doc):
    canv.saveState()
    canv.setFont(FONT, 7)
    canv.setFillColor(MUTED)
    if doc.page > 1:
        canv.drawString(8*mm, 5*mm, 'RTS — 领域架构评审版 v2')
    canv.drawRightString(PAGE_W-8*mm, 5*mm, str(doc.page))
    canv.restoreState()

styles = getSampleStyleSheet()
styles.add(ParagraphStyle('V2Title', fontName=FONT, fontSize=20.0, leading=25, alignment=TA_CENTER, textColor=PRIMARY, wordWrap='CJK', spaceAfter=7))
styles.add(ParagraphStyle('V2Sub', fontName=FONT, fontSize=9.8, leading=14.0, alignment=TA_CENTER, textColor=TEXT, wordWrap='CJK', spaceAfter=3))
styles.add(ParagraphStyle('V2H1', fontName=FONT, fontSize=14.6, leading=19.0, textColor=PRIMARY, wordWrap='CJK', spaceBefore=2, spaceAfter=6))
styles.add(ParagraphStyle('V2H2', fontName=FONT, fontSize=11.4, leading=15.2, textColor=ACCENT, wordWrap='CJK', spaceBefore=5, spaceAfter=3))
styles.add(ParagraphStyle('V2Body', fontName=FONT, fontSize=9.45, leading=13.5, textColor=TEXT, wordWrap='CJK', spaceAfter=3))
styles.add(ParagraphStyle('V2Small', fontName=FONT, fontSize=7.9, leading=10.7, textColor=MUTED, wordWrap='CJK', spaceAfter=2))
styles.add(ParagraphStyle('V2Quote', fontName=FONT, fontSize=10.0, leading=15.0, textColor=PRIMARY, wordWrap='CJK', leftIndent=3, rightIndent=2, spaceBefore=3, spaceAfter=4))
styles.add(ParagraphStyle('CardTitle', fontName=FONT, fontSize=10.0, leading=13.5, textColor=PRIMARY, wordWrap='CJK', spaceAfter=2))
styles.add(ParagraphStyle('CardBody', fontName=FONT, fontSize=8.55, leading=12.0, textColor=TEXT, wordWrap='CJK'))
styles.add(ParagraphStyle('Tag', fontName=FONT, fontSize=7.6, leading=9.5, textColor=colors.white, alignment=TA_CENTER, wordWrap='CJK'))
styles.add(ParagraphStyle('Cell', fontName=FONT, fontSize=7.35, leading=9.8, textColor=TEXT, wordWrap='CJK'))
styles.add(ParagraphStyle('CellHead', fontName=FONT, fontSize=7.3, leading=9.6, textColor=colors.white, alignment=TA_CENTER, wordWrap='CJK'))

T=styles['V2Title']; SUB=styles['V2Sub']; H1=styles['V2H1']; H2=styles['V2H2']; B=styles['V2Body']; S=styles['V2Small']; Q=styles['V2Quote']; CT=styles['CardTitle']; CB=styles['CardBody']; TAG=styles['Tag']; CELL=styles['Cell']; CELLH=styles['CellHead']

story=[]


def p(text, style=B): return Paragraph(esc(text), style)

def section(title): story.append(p(title, H1))
def rule(): story.append(HRFlowable(width='100%', thickness=0.55, color=LINE, spaceBefore=3, spaceAfter=5))

def card(title, body, tag=None, tag_color=ACCENT, keep=True):
    rows=[]
    if tag:
        rows.append([p(tag, TAG)])
    rows.append([p(title, CT)])
    rows.append([p(body, CB)])
    t=Table(rows, colWidths=[PAGE_W-18*mm])
    style=[('BOX',(0,0),(-1,-1),0.45,LINE),('BACKGROUND',(0,0),(-1,-1),CARD),('LEFTPADDING',(0,0),(-1,-1),5),('RIGHTPADDING',(0,0),(-1,-1),5),('TOPPADDING',(0,0),(-1,-1),4),('BOTTOMPADDING',(0,0),(-1,-1),4),('VALIGN',(0,0),(-1,-1),'TOP')]
    if tag:
        style += [('BACKGROUND',(0,0),(0,0),tag_color),('ALIGN',(0,0),(0,0),'CENTER')]
    t.setStyle(TableStyle(style))
    story.append(KeepTogether([t, Spacer(1,3)]) if keep else t)

# 1 cover + decision summary
story.append(Spacer(1, 10*mm))
story.append(p('报文转换规则系统（RTS）', T))
story.append(p('领域架构评审版', T))
story.append(p('面向银行报文转换场景的可治理 AI 语义基座', SUB))
story.append(p('领域架构讨论稿 · v2 · 2026-04-27', S))
story.append(Spacer(1, 5*mm))
card('一句话决策摘要', 'RTS（本文中指 Transformation Rule System，与既有 TRS 文档语义一致）不是聊天机器人，也不是一次性检索增强生成应用；它是面向银行报文转换场景的受治理语义基座。', '定位', PRIMARY)
card('核心架构不变量', 'RTS 决定什么是真的；索引层决定如何找到真相；AI 解释真相，但不创造真相。', '原则', GREEN)
card('本次评审请求', '确认 RTS 作为规则驱动 AI 能力的架构模式；确认试点范围、受控 API 接入路径、职责分工与治理护栏。', '决策', PURPLE)
story.append(PageBreak())

# 2 problem
section('1. 当前问题：报文转换知识尚未适合 AI 使用')
story.append(p('银行报文转换知识通常分散在 XML 样本、映射表、Java/XSLT、查表逻辑、工单、邮件、支持记录和 Confluence 页面中。若直接让通用 AI 或普通检索增强生成读取这些材料，容易产生范围错配、证据缺失、签核边界丢失和流畅但不可追溯的回答。'))
card('规则真相分散', '同一条转换逻辑可能同时存在于代码、Excel、XSLT、历史工单和专家口头经验中，缺少统一的受治理规则对象。', '风险', ORANGE)
card('解释链条不完整', '很难稳定回答：目标字段从哪里来、为什么这样生成、依赖哪个查表逻辑或辅助逻辑、是否经过评审和签核。', '风险', ORANGE)
card('AI 上下文不可控', '更多文本不等于更安全。若没有通道、产品、规则包、规则对象的范围约束，AI 很容易跨产品线引用错误规则。', '风险', ORANGE)
card('重复建设风险', '每个 AI 场景各自做提示词、检索和业务假设，会造成不一致答案和不可复用的临时能力。', '风险', ORANGE)
story.append(PageBreak())

# 3 what is/not + truth boundary
section('2. RTS 是什么 / 不是什么')
story.append(p('RTS 的目标是建立受治理的报文转换规则真相层，再通过受控投影、索引和 API 化 AI 消费支撑多个场景。'))
card('RTS 是什么', '一个把来源、逻辑、目标、依赖、样例、评审、签核等规则知识结构化的领域语义基座。它让 AI 基于规则对象工作，而不是基于散落文档猜测。', '是', GREEN)
card('RTS 不是什么', '不复制大语言模型、聊天界面、Confluence 搜索或通用检索增强生成；不替代专家签核；不让 AI 修改生产规则、映射、代码或配置。', '不是', ORANGE)
card('真相边界', '规则真相由经过评审和签核的规范规则对象建立；AI 只能在该边界内读取、导航、解释和标记不确定性，不能创造业务逻辑或替代签核。', '边界', PRIMARY)
card('运行时投影边界', '运行时默认只暴露已批准的规则、查表逻辑和辅助逻辑；证据、评审和报告保持在治理侧，并通过受控引用支持审计和解释。', '投影', PURPLE)
story.append(PageBreak())

# 4 architecture diagram as text cards
section('3. 目标架构：真相 → 投影 → 索引 → API')
story.append(p('建议采用分层架构，避免领域治理和 AI 运行时行为混在同一个不可控表面里。'))
for title, body in [
('01 源材料层', 'XML/FpML/SCBML 样本、映射表、Java/XSLT、查表逻辑、缺陷记录、评审意见、业务澄清。'),
('02 真相层', '规范规则包；规则 / 查表逻辑 / 辅助逻辑；证据、评审、歧义、签核。'),
('03 投影层', '只读导出已批准规则、查表逻辑和辅助逻辑，形成低噪声运行时资源。'),
('04 索引/查询层', '稳定地址、元数据、L0/L1/L2、依赖图、范围感知检索、检索轨迹。'),
('05 API / 工作流层', '客户接入、用户验收测试、发布、事件、审计等流程通过受控 API 调用 AI 能力。'),
]:
    card(title, body, '架构层', ACCENT, keep=False)
story.append(PageBreak())

# 5 boundary matrix compact
section('4. 能力边界矩阵：为什么不是造轮子')
story.append(p('RTS 与现有工具是互补关系。Copilot 和 Yoda 可以消费或展示 RTS 产出的解释，但不能成为报文转换规则的受治理真相层。'))
rows = [
[p('能力边界', CELLH), p('Copilot', CELLH), p('Yoda / Confluence', CELLH), p('普通检索增强生成', CELLH), p('RTS + 受控 API', CELLH)],
[p('个人写作/总结', CELL), p('强', CELL), p('中', CELL), p('中', CELL), p('非重点', CELL)],
[p('页面知识问答', CELL), p('中', CELL), p('强', CELL), p('中', CELL), p('可消费输出', CELL)],
[p('规则真相层', CELL), p('无', CELL), p('无', CELL), p('弱', CELL), p('核心能力', CELL)],
[p('签核/歧义边界', CELL), p('无', CELL), p('弱', CELL), p('弱', CELL), p('核心能力', CELL)],
[p('字段血缘/依赖图', CELL), p('弱', CELL), p('弱', CELL), p('弱', CELL), p('核心能力', CELL)],
[p('固定结构输出', CELL), p('弱', CELL), p('弱', CELL), p('中', CELL), p('核心能力', CELL)],
[p('流程 API / 审计', CELL), p('无', CELL), p('无', CELL), p('需自建', CELL), p('核心能力', CELL)],
]
t = Table(rows, colWidths=[24*mm, 18*mm, 22*mm, 18*mm, 30*mm], repeatRows=1)
t.setStyle(TableStyle([
('BACKGROUND',(0,0),(-1,0),PRIMARY),('GRID',(0,0),(-1,-1),0.25,LINE),('VALIGN',(0,0),(-1,-1),'MIDDLE'),('LEFTPADDING',(0,0),(-1,-1),2),('RIGHTPADDING',(0,0),(-1,-1),2),('TOPPADDING',(0,0),(-1,-1),3),('BOTTOMPADDING',(0,0),(-1,-1),3),('BACKGROUND',(0,1),(-1,-1),colors.white)
]))
story.append(t)
story.append(Spacer(1,4))
story.append(p('结论：RTS 不复制现有工具；它补齐的是现有工具缺失的受治理规则真相层、范围感知检索、签核边界、字段级血缘和依赖图。', Q))
story.append(PageBreak())

# 6 API service integration
section('5. 为什么需要受控 API 化 AI 服务接入')
story.append(p('这里需要的不是面向个人聊天的 token，而是把 AI 能力嵌入后台流程的受控 API 接入。token 只是认证手段，架构诉求是服务级集成。'))
card('不用 API 化的缺陷', '人工复制到聊天工具、个人提示词或页面问答无法保证固定输出结构、提示词/模型版本记录、可重复调用、审计链路、范围约束和流程触发。', '替代方案', ORANGE)
card('API 化的必要能力', '结构化输入：XML、映射摘录、规则对象、缺陷列表、元数据；结构化输出：JSON、检查清单、评估结论、可追溯摘要。', '能力', PRIMARY)
card('治理与审计', '记录输入来源、提示词版本、模型版本、输出、复核人和证据链接；支持脱敏、访问控制、范围约束和人工复核。', '治理', PURPLE)
card('流程集成', '客户接入、用户验收测试、发布、事件、审计流程可自动触发分析，而不是依赖人工打开聊天窗口。', '流程', GREEN)
story.append(PageBreak())

# 7 roadmap use cases + benefits
section('6. 用例路线图与业务价值')
story.append(p('用例不应平铺成愿望清单，而应作为同一语义基座的分阶段消费者。'))
road_rows = [
[p('阶段', CELLH), p('能力范围', CELLH), p('业务价值 / 指标', CELLH)],
[p('P0 基座', CELL), p('规则解释、字段血缘、依赖导航、范围感知检索、歧义感知回答。', CELL), p('正确规则包/对象命中率；回答可追溯率；范围误召回率。', CELL)],
[p('P1 流程', CELL), p('影响分析、回归测试推荐、发布准备度检查清单。', CELL), p('降低发布回归风险；测试推荐采纳率；专家复核时间下降。', CELL)],
[p('P2 业务', CELL), p('客户/上游接入缺口、新产品准备度、客户影响可见性。', CELL), p('缩短收入落地周期；提升客户影响可见性。', CELL)],
[p('后续扩展', CELL), p('审计叙事、知识过期检测、报文质量雷达、值班交接。', CELL), p('审计准备度；知识过期发现率；重复问题下降。', CELL)],
]
rt=Table(road_rows, colWidths=[22*mm, 49*mm, 41*mm], repeatRows=1)
rt.setStyle(TableStyle([
('BACKGROUND',(0,0),(-1,0),GREEN),('GRID',(0,0),(-1,-1),0.25,LINE),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),3),('RIGHTPADDING',(0,0),(-1,-1),3),('TOPPADDING',(0,0),(-1,-1),3),('BOTTOMPADDING',(0,0),(-1,-1),3)
]))
story.append(rt)
story.append(Spacer(1,4))
story.append(p('优先价值不是让 AI 回答更多问题，而是缩短规则理解和客户接入周期、降低发布回归风险、提升审计准备度，并减少对少数专家口头知识的依赖。', Q))
story.append(PageBreak())

# 8 guardrails & ownership
section('7. 架构护栏与职责分工')
story.append(p('领域架构审批时最需要确认的是：AI 能力如何被约束，谁拥有规则真相，谁批准投影和消费。'))
card('AI 只读消费', 'AI 读取投影后的规则对象和受控追溯信息，不修改生产规则、映射、代码或配置。', '护栏', PURPLE)
card('保留歧义', 'open question、缺失证据、未签核规则必须保持可见。Unknown is better than wrong。', '护栏', PURPLE)
card('运行时学习不写回真相', '会话记忆或用户反馈不能静默改写规范规则真相；更新必须经过规则编写、评审和签核。', '护栏', PURPLE)
own_rows = [
[p('角色', CELLH), p('职责', CELLH)],
[p('领域负责人', CELL), p('拥有规范规则真相、评审/签核边界和业务规则解释权。', CELL)],
[p('平台负责人', CELL), p('拥有投影、索引/查询、API、安全控制、审计日志和运行质量。', CELL)],
[p('流程负责人', CELL), p('拥有客户接入、用户验收测试、发布、事件、审计等消费流程和人工复核。', CELL)],
]
ot=Table(own_rows, colWidths=[35*mm, 77*mm], repeatRows=1)
ot.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),PRIMARY),('GRID',(0,0),(-1,-1),0.25,LINE),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),4),('RIGHTPADDING',(0,0),(-1,-1),4),('TOPPADDING',(0,0),(-1,-1),3),('BOTTOMPADDING',(0,0),(-1,-1),3)]))
story.append(Spacer(1,4)); story.append(ot)
story.append(PageBreak())

# 9 pilot
section('8. 试点范围与验收指标')
story.append(p('试点应验证架构模式，而不是展示聊天。建议从一个源系统到目标系统的通道和少量已批准规则包开始。'))
card('建议范围', '选择 Tradition → Stella 等一个通道，覆盖少量 COMMON / FXD / FXO 已批准规则包。先验证 P0/P1，不在第一阶段追求全量业务应用。', '范围', ACCENT)
card('基础输出', '规则解释、字段血缘、依赖导航、歧义感知回答；针对一个样本变更生成影响分析和回归测试推荐。', '输出', GREEN)
metrics = [
[p('验收项', CELLH), p('目标', CELLH)],
[p('检索准确性', CELL), p('稳定命中正确通道、产品、规则包和规则对象，记录检索轨迹。', CELL)],
[p('可追溯性', CELL), p('关键结论可回到规则对象、依赖关系和受控证据引用。', CELL)],
[p('治理边界', CELL), p('保留歧义，不把未签核内容输出成确定答案。', CELL)],
[p('流程可用性', CELL), p('通过 API 重复调用，输出固定结构，可被人工复核。', CELL)],
[p('业务价值', CELL), p('减少专家复核时间，提升测试推荐采纳率和审计轨迹完整率。', CELL)],
]
mt=Table(metrics, colWidths=[32*mm, 80*mm], repeatRows=1)
mt.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),ACCENT),('GRID',(0,0),(-1,-1),0.25,LINE),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),4),('RIGHTPADDING',(0,0),(-1,-1),4),('TOPPADDING',(0,0),(-1,-1),3),('BOTTOMPADDING',(0,0),(-1,-1),3)]))
story.append(mt)
story.append(PageBreak())

# 10 decision asks + non goals
section('9. 决策请求与非目标')
story.append(p('本材料请求领域架构对架构模式和受控边界进行确认，而不是批准一个开放式 AI 平台。'))
for title, body in [
('决策 1：批准架构模式', '真相层 → 投影层 → 索引/查询层 → API 化 AI 消费 → 流程消费者。'),
('决策 2：批准试点范围', '限定通道、产品和规则包，优先验证 P0/P1 能力与可追溯输出。'),
('决策 3：批准受控 API 接入路径', '将 AI 作为后台流程组件接入，要求固定结构、审计日志、范围约束和人工复核。'),
('决策 4：确认职责分工', '领域负责人、平台负责人、流程负责人的职责边界需在试点前明确。'),
('决策 5：确认治理护栏', 'AI 不决策、不签核、不改生产；运行时学习不写回规则真相；歧义必须保留。'),
]:
    card(title, body, '决策', PRIMARY, keep=False)
rule()
story.append(p('非目标：不建设通用智能体平台；不替代 Confluence / Copilot / Yoda；不在第一阶段做运行时学习写回；不让 AI 修改生产规则或替代专家签核。', Q))
story.append(PageBreak())

# 11 glossary / L0 L1 L2
section('10. 术语与补充说明')
story.append(p('为降低架构评审中的概念歧义，本页固定几个核心术语。'))
for title, body in [
('RTS / TRS', '本文使用 RTS 指 Transformation Rule System；与仓库既有 TRS 文档语义一致，后续正式命名可由架构评审统一。'),
('规范规则包', '受治理的规则包，是规则真相的正式记录，包含规则、查表逻辑、辅助逻辑、证据、评审、报告等治理结构。'),
('运行时投影', '面向运行时 AI 消费的只读投影。第一阶段默认只投影已批准规则、查表逻辑和辅助逻辑。'),
('L0 / L1 / L2', '同一资源的不同精度视图：L0 用于召回过滤，L1 用于导航与重排，L2 是最终对象正文。它们不是三层目录。'),
('受控 API 接入', '把 AI 能力作为可审计、可复用、可验证的服务组件嵌入流程，而不是让用户手工聊天。'),
]:
    card(title, body, '术语', ACCENT, keep=False)

md = '''# RTS 领域架构评审版 v2

RTS（本文中指 Transformation Rule System，与既有 TRS 文档语义一致）不是聊天机器人，也不是一次性检索增强生成应用；它是面向银行报文转换场景的受治理语义基座。

核心架构不变量：RTS 决定什么是真的；索引层决定如何找到真相；AI 解释真相，但不创造真相。

## 主要改动
- 删除内部改稿口吻
- 将 API Token 改为受控 API 化 AI 服务接入
- 新增能力边界矩阵，回答 Copilot / Yoda / Confluence / 普通检索增强生成 与 RTS 的边界
- 修正规则证据/评审的运行时投影边界
- 用例改成 P0 / P1 / P2 / 后续扩展路线图
- 加入业务价值、试点指标、职责分工、正式决策请求
'''.strip()
MD_PATH.write_text(md, encoding='utf-8')

doc=SimpleDocTemplate(str(PDF_PATH), pagesize=PAGE_SIZE, leftMargin=7*mm, rightMargin=7*mm, topMargin=7*mm, bottomMargin=8*mm)
doc.build(story, onFirstPage=footer, onLaterPages=footer)
print(PDF_PATH)
print(MD_PATH)
