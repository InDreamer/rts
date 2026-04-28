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
PDF_PATH = OUT_DIR / 'rts-domain-architecture-proposal.pdf'
MD_PATH = OUT_DIR / 'rts-domain-architecture-proposal.md'

pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
FONT = 'STSong-Light'
PAGE_SIZE = (108 * mm, 192 * mm)  # mobile-friendly portrait
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


def esc(x): return html.escape(str(x)).replace('\n', '<br/>')

def footer(canv: canvas.Canvas, doc):
    canv.saveState()
    canv.setFont(FONT, 7)
    canv.setFillColor(MUTED)
    if doc.page > 1:
        canv.drawString(8*mm, 5*mm, 'RTS — Domain Architecture Proposal')
    canv.drawRightString(PAGE_W-8*mm, 5*mm, str(doc.page))
    canv.restoreState()

styles = getSampleStyleSheet()
styles.add(ParagraphStyle('TitleC', fontName=FONT, fontSize=20.5, leading=26, alignment=TA_CENTER, textColor=PRIMARY, wordWrap='CJK', spaceAfter=8))
styles.add(ParagraphStyle('SubC', fontName=FONT, fontSize=10.2, leading=14.5, alignment=TA_CENTER, textColor=TEXT, wordWrap='CJK', spaceAfter=4))
styles.add(ParagraphStyle('H1C', fontName=FONT, fontSize=15.0, leading=20, textColor=PRIMARY, wordWrap='CJK', spaceBefore=4, spaceAfter=7))
styles.add(ParagraphStyle('H2C', fontName=FONT, fontSize=12.0, leading=16, textColor=ACCENT, wordWrap='CJK', spaceBefore=6, spaceAfter=4))
styles.add(ParagraphStyle('BodyC', fontName=FONT, fontSize=10.1, leading=14.8, textColor=TEXT, wordWrap='CJK', spaceAfter=4))
styles.add(ParagraphStyle('SmallC', fontName=FONT, fontSize=8.3, leading=11.8, textColor=MUTED, wordWrap='CJK', spaceAfter=3))
styles.add(ParagraphStyle('QuoteC', fontName=FONT, fontSize=10.6, leading=16.0, textColor=PRIMARY, wordWrap='CJK', leftIndent=4, rightIndent=2, spaceBefore=4, spaceAfter=5))
styles.add(ParagraphStyle('CardTitle', fontName=FONT, fontSize=10.7, leading=14.8, textColor=PRIMARY, wordWrap='CJK', spaceAfter=3))
styles.add(ParagraphStyle('CardBody', fontName=FONT, fontSize=9.25, leading=13.4, textColor=TEXT, wordWrap='CJK'))
styles.add(ParagraphStyle('Tag', fontName=FONT, fontSize=8.0, leading=10.2, textColor=colors.white, alignment=TA_CENTER, wordWrap='CJK'))

T=styles['TitleC']; SUB=styles['SubC']; H1=styles['H1C']; H2=styles['H2C']; B=styles['BodyC']; S=styles['SmallC']; Q=styles['QuoteC']; CT=styles['CardTitle']; CB=styles['CardBody']; TAG=styles['Tag']

story=[]

def card(title, body, tag=None, tag_color=ACCENT, keep=True):
    rows=[]
    if tag: rows.append([Paragraph(esc(tag), TAG)])
    rows.append([Paragraph(esc(title), CT)])
    rows.append([Paragraph(esc(body), CB)])
    t=Table(rows, colWidths=[PAGE_W-20*mm])
    style=[('BOX',(0,0),(-1,-1),0.5,LINE),('BACKGROUND',(0,0),(-1,-1),CARD),('LEFTPADDING',(0,0),(-1,-1),6),('RIGHTPADDING',(0,0),(-1,-1),6),('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),5),('VALIGN',(0,0),(-1,-1),'TOP')]
    if tag: style += [('BACKGROUND',(0,0),(0,0),tag_color),('ALIGN',(0,0),(0,0),'CENTER')]
    t.setStyle(TableStyle(style))
    story.append(KeepTogether([t, Spacer(1,4)]) if keep else t)

def section(title): story.append(Paragraph(esc(title), H1))
def rule(): story.append(HRFlowable(width='100%', thickness=0.6, color=LINE, spaceBefore=4, spaceAfter=7))

# Cover
story.append(Spacer(1, 13*mm))
story.append(Paragraph('Transformation Rule System', T))
story.append(Paragraph('Domain Architecture Proposal', T))
story.append(Spacer(1, 3*mm))
story.append(Paragraph('A governed semantic foundation for AI-enabled banking message transformation', SUB))
story.append(Paragraph('Audience: Domain Architecture / Platform Architecture / Technology Governance', S))
story.append(Spacer(1, 5*mm))
card('Architecture positioning', 'RTS is a domain architecture capability: a governed transformation-rule knowledge foundation that separates rule truth, runtime projection, indexing, retrieval, and AI consumption.', 'POSITION', PRIMARY)
card('Strategic value', 'It provides a reusable, auditable and extensible semantic layer for multiple AI-enabled capabilities, instead of creating isolated chatbot-style solutions.', 'VALUE', GREEN)
story.append(PageBreak())

section('1. Executive architecture summary')
story.append(Paragraph('RTS should be assessed as a domain architecture pattern, not as an internal knowledge note or a single AI use case. Its purpose is to establish a governed semantic foundation for banking message transformation, so that AI capabilities can operate on approved rule truth rather than raw documents or informal explanations.', B))
card('Core thesis', 'Generic RAG can retrieve text. RTS defines governed rule truth. This distinction matters for banking domains where field-level transformation logic, signoff status, ambiguity and evidence boundaries must remain explicit.', 'THESIS', PRIMARY)
card('Architecture outcome', 'A reusable foundation where multiple applications — rule explanation, lineage, impact analysis, release readiness, onboarding readiness and audit explainability — all consume the same governed rule layer.', 'OUTCOME', GREEN)
story.append(Paragraph('Recommended framing:', H2))
story.append(Paragraph('RTS is not another chatbot. It is a governed domain knowledge architecture that enables safe, rule-grounded AI applications across message transformation workflows.', Q))
story.append(PageBreak())

section('2. Problem statement for architecture')
story.append(Paragraph('Current transformation knowledge is usually fragmented across code, XSLT, mapping spreadsheets, XML samples, tickets, emails, support notes and Confluence pages. This creates architecture-level risks when AI is introduced directly on top of raw or weakly governed content.', B))
problems=[
('Fragmented truth', 'Different teams rely on different artifacts: code, Excel, documents, old tickets and SME memory. There is no single governed rule object model.'),
('Weak explainability', 'It is hard to explain why a target field is generated, which source path it depends on, and whether the logic has been reviewed or signed off.'),
('Unsafe generic AI', 'A generic chatbot may produce fluent answers without respecting scope, product boundaries, ambiguity, signoff or evidence state.'),
('Limited reuse', 'If every AI use case builds its own prompt and document retrieval layer, the enterprise gets duplicated logic and inconsistent answers.'),
]
for t,b in problems: card(t,b,'ARCH RISK',ORANGE)
story.append(PageBreak())

section('3. RTS architecture principle')
story.append(Paragraph('The core architecture principle is truth separation. RTS separates governed truth from runtime retrieval and AI interaction. The LLM is allowed to read, navigate and explain; it is not allowed to invent business logic or promote ambiguity into certainty.', B))
principles=[
('Truth Layer', 'Canonical packs define approved rules, lookups, helpers, evidence, review state, ambiguity and signoff boundaries.'),
('Projection Layer', 'Only approved runtime objects are projected for AI consumption. Governance noise is not blindly mixed into runtime context.'),
('Index Layer', 'Projected resources get stable URIs, metadata, L0/L1/L2 summaries, dependency links and retrieval traces.'),
('Query / AI Layer', 'Agents consume the indexed layer through scoped retrieval and structured result shaping; outputs remain drafts for human review where required.'),
]
for t,b in principles: card(t,b,'LAYER',PRIMARY)
story.append(Paragraph('Architecture invariant:', H2))
story.append(Paragraph('TRS decides what is true. Indexing decides how truth is found. AI explains truth; it does not create truth.', Q))
story.append(PageBreak())

section('4. Reference architecture')
story.append(Paragraph('The proposed architecture is intentionally layered so domain governance and AI runtime behavior do not collapse into one uncontrolled surface.', B))
arch=[
('01 Governed source artifacts', 'XML/FpML/SCBML samples, mapping spreadsheets, Java/XSLT logic, lookup tables, defect history, review comments and business clarification.'),
('02 Canonical rule objects', 'rules / lookups / helpers with source, logic, target, dependencies and examples; evidence and review remain explicit governance layers.'),
('03 Runtime projection', 'Approved rule objects are exported as read-only, low-noise runtime resources with stable scope boundaries.'),
('04 Metadata and retrieval index', 'URI, channel, product, pack, object type, dependency graph, L0 recall summary, L1 navigation overview and L2 object content.'),
('05 API-based AI services', 'Structured prompts and outputs for rule explanation, lineage, impact analysis, readiness assessment and audit narrative generation.'),
('06 Human review and workflow integration', 'Outputs are reviewed, linked to evidence and integrated into onboarding, release, incident or audit workflows.'),
]
for t,b in arch: card(t,b,'REF',ACCENT)
story.append(PageBreak())

section('5. Use case landscape enabled by RTS')
story.append(Paragraph('For Domain Architecture, the important point is not any single use case. The important point is that multiple use cases consume the same governed semantic foundation.', B))
card('Foundation capabilities', 'Transformation Rule Explainer; Field Lineage; Source-to-Target Traceability; Rule Dependency Navigation; Evidence-backed Explanation; Ambiguity and Signoff-aware Answering.', 'FOUNDATION', PRIMARY)
card('Delivery & governance workflows', 'Rule-grounded Impact Analysis; Regression Test Recommendation; Release Readiness Review; Rule-grounded Incident Explanation; Change Intent Verification.', 'WORKFLOW', ACCENT)
card('Business-facing applications', 'Client / Upstream Onboarding Accelerator; Product / Flow Readiness Analyzer; Client Impact Intelligence; Regulatory / Audit Explainability; Institutional Knowledge Recovery.', 'BUSINESS', GREEN)
card('Future expansion', 'Knowledge Decay Detection; Message Quality Radar; On-call Handover; Silent Dependency Discovery; Operational Friction Intelligence.', 'FUTURE', ORANGE)
story.append(PageBreak())

section('6. Why this is stronger than isolated AI use cases')
story.append(Paragraph('Isolated AI use cases are easier to prototype but harder to govern, reuse and scale. RTS turns use cases into consumers of a shared domain foundation.', B))
strong=[
('Reuse', 'One governed rule foundation supports many applications instead of duplicating prompts, retrieval logic and assumptions per use case.'),
('Consistency', 'Different applications answer from the same approved rule objects, reducing inconsistent interpretations across teams.'),
('Traceability', 'Answers can point back to rule objects, dependencies, evidence and review state instead of vague document snippets.'),
('Governance', 'Ambiguity and signoff state remain first-class architecture concepts, not afterthoughts inside prompts.'),
('Scalability', 'New channels, products and packs can follow the same architecture pattern: truth → projection → index → scoped AI consumption.'),
]
for t,b in strong: card(t,b,'BENEFIT',GREEN)
story.append(PageBreak())

section('7. Why Copilot / Yoda are not substitutes')
story.append(Paragraph('This point should be framed as product-fit, not tool criticism. Copilot and Yoda remain useful, but they do not provide the architecture layer RTS is proposing.', B))
card('Copilot', 'Copilot is a personal productivity assistant. It can help users draft, summarize and reason over materials, but it is not a governed transformation-rule truth layer, nor a workflow-embedded backend analysis service.', 'NOT SUBSTITUTE', ORANGE)
card('Yoda', 'Yoda is a Confluence knowledge-base chatbot. It can answer questions over indexed pages, but RTS needs to reason over rule objects, XML paths, mapping extracts, lookup dependencies, signoff state and operational artifacts beyond Confluence.', 'NOT SUBSTITUTE', ORANGE)
card('RTS + API token', 'API-based AI allows controlled service integration: structured input, fixed output schema, prompt versioning, validation, evidence references, human review and audit logging.', 'REQUIRED', GREEN)
story.append(Paragraph('Architecture distinction:', H2))
story.append(Paragraph('Copilot and Yoda are user-facing assistance tools. RTS is a governed domain architecture foundation with AI as a controlled consumer of rule truth.', Q))
story.append(PageBreak())

section('8. Why API token is required')
story.append(Paragraph('The API token is required because the proposed capability is not manual Q&A. It is a backend AI capability embedded into domain workflows and controlled by architecture rules.', B))
api=[
('Workflow integration', 'Trigger analysis from onboarding, UAT, release, incident or audit workflows instead of relying on manual chat sessions.'),
('Structured artifacts', 'Pass XML samples, mapping extracts, rule objects, defect lists and metadata as structured inputs.'),
('Controlled outputs', 'Require JSON / checklist / assessment templates with validation and retry handling.'),
('Auditability', 'Record input sources, prompt version, model version, output, reviewer and evidence links.'),
('Security controls', 'Support redaction, scope enforcement, access control and human-in-the-loop approval.'),
('Platform extensibility', 'Expose a reusable AI service layer that multiple RTS-enabled applications can call.'),
]
for t,b in api: card(t,b,'TOKEN',PRIMARY)
story.append(PageBreak())

section('9. Architecture guardrails')
story.append(Paragraph('These guardrails are important for Domain Architecture approval. They ensure the proposal remains a governed foundation rather than an uncontrolled generative system.', B))
guards=[
('Read-only AI consumption', 'AI reads projected rule truth and operational evidence; it does not modify production rules, mappings, code or configuration.'),
('Human decision boundary', 'AI outputs are analysis drafts, checklists, summaries or recommendations. Final signoff remains with authorized owners.'),
('Scope enforcement', 'Retrieval must first resolve channel, product, pack and object scope to avoid cross-product or cross-system contamination.'),
('Ambiguity preservation', 'Open questions, missing evidence and unsigned rules must remain visible. Unknown is better than wrong.'),
('No runtime learning into truth', 'Session memory or user feedback cannot silently rewrite canonical rule truth; updates must go through controlled authoring/review/signoff.'),
('Traceable outputs', 'Material conclusions should reference rule object, dependency, evidence or review state where applicable.'),
]
for t,b in guards: card(t,b,'GUARDRAIL',PURPLE)
story.append(PageBreak())

section('10. Proposed pilot for architecture validation')
story.append(Paragraph('The pilot should validate the architecture pattern, not merely demonstrate a chatbot. A good pilot proves that governed rule objects can support multiple outputs safely.', B))
pilot=[
('Pilot scope', 'Use one source-target channel such as Tradition → Stella and a small set of approved packs across COMMON / FXD / FXO.'),
('Foundation output', 'Rule explanation, field lineage, dependency navigation and ambiguity-aware answering from projected rule objects.'),
('Workflow output', 'Rule-grounded impact analysis and regression test recommendation for a controlled sample change.'),
('Business output', 'Onboarding gap checklist or product readiness summary using sample XML and existing rule coverage.'),
('Architecture success criteria', 'Correct scope resolution, traceable rule references, preserved ambiguity, structured output, human review boundary and repeatable API invocation.'),
]
for t,b in pilot: card(t,b,'PILOT',ACCENT)
story.append(PageBreak())

section('11. Decision points for Domain Architecture')
story.append(Paragraph('The architecture review should focus on whether RTS is accepted as the semantic foundation pattern for AI-enabled message transformation, and what boundaries are required for safe adoption.', B))
decisions=[
('1. Approve the layered architecture', 'Truth Layer → Projection Layer → Index Layer → API-based AI consumption.'),
('2. Approve RTS as source of governed rule truth', 'Canonical packs remain the court of record for transformation rules.'),
('3. Approve API-based AI as controlled consumer', 'AI may explain, summarize, classify and draft outputs, but not decide or mutate truth.'),
('4. Approve pilot scope', 'Start with a narrow source-target channel and selected approved packs to validate retrieval and workflow outputs.'),
('5. Define ownership', 'Domain owner for rule truth; platform owner for index/API; workflow owners for consuming applications.'),
]
for t,b in decisions: card(t,b,'DECISION',PRIMARY)
story.append(PageBreak())

section('12. Suggested wording for architecture paper')
story.append(Paragraph('Transformation Rule System is proposed as a governed domain knowledge architecture for banking message transformation. It converts fragmented transformation knowledge into reviewed, signoff-aware and addressable rule objects, then exposes approved runtime projections through indexed retrieval and API-based AI consumption.', Q))
story.append(Paragraph('The intent is not to build another chatbot. The intent is to establish a reusable semantic foundation on which multiple AI-enabled capabilities can be safely built, including rule explanation, field lineage, impact analysis, regression recommendation, release readiness, onboarding readiness and audit explainability.', Q))
story.append(Paragraph('Existing tools such as Copilot and Yoda remain useful for user productivity and Confluence knowledge retrieval, but they do not replace a governed rule truth layer, structured runtime projection, scoped retrieval, evidence-linked outputs or workflow-integrated API services.', Q))
rule()
story.append(Paragraph('Control statement:', H2))
story.append(Paragraph('AI will not make production decisions, approve rules, modify mappings or change system behavior. AI-generated outputs are structured, evidence-linked drafts for authorized human review.', Q))

md = '''# Transformation Rule System — Domain Architecture Proposal

RTS is proposed as a governed domain knowledge architecture for banking message transformation.

It is not another chatbot. It is a reusable semantic foundation on which multiple AI-enabled capabilities can be safely built.

## Architecture layers
1. Truth Layer — canonical packs, rules, lookups, helpers, evidence, review, signoff
2. Projection Layer — approved runtime objects only
3. Index Layer — URI, metadata, L0/L1/L2 summaries, dependency graph, retrieval trace
4. Query / AI Layer — scoped API-based AI consumption
5. Workflow Layer — onboarding, UAT, release, incident, audit workflows

## Use case landscape
- Foundation: rule explanation, field lineage, dependency navigation, evidence-backed answers, ambiguity-aware answering
- Workflow: impact analysis, regression test recommendation, release readiness, incident explanation, change intent verification
- Business: client onboarding, product readiness, client impact intelligence, audit explainability, institutional knowledge recovery

## Why not Copilot / Yoda
Copilot is personal productivity. Yoda is Confluence Q&A. RTS is a governed rule truth architecture with API-based AI as a controlled workflow component.

## Why API token
API token is required for workflow integration, structured artifacts, controlled outputs, auditability, security controls and platform extensibility.
'''.strip()
MD_PATH.write_text(md, encoding='utf-8')

doc=SimpleDocTemplate(str(PDF_PATH), pagesize=PAGE_SIZE, leftMargin=8*mm, rightMargin=8*mm, topMargin=8*mm, bottomMargin=9*mm)
doc.build(story, onFirstPage=footer, onLaterPages=footer)
print(PDF_PATH)
print(MD_PATH)
