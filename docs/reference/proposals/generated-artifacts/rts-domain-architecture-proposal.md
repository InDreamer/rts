# Transformation Rule System — Domain Architecture Proposal

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