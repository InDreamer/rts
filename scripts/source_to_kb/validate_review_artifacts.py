#!/usr/bin/env python3
"""Validate review artifacts for the source-backed KB draft MVP."""

from __future__ import annotations

import re
from pathlib import Path
from typing import Any

from validate_source_bundle import add_issue, load_json, load_yaml


READINESS_VALUES = {
    "not_ready_blocking_questions",
    "not_ready_missing_source_inventory",
    "not_ready_missing_claims",
    "not_ready_contract_errors",
    "ready_for_kb_draft_mvp_completion",
}
REQUIRED_REVIEW_FILES = [
    "review/review-index.yaml",
    "review/ask-user-questions.json",
    "reports/review-checklist.md",
    "reports/closure-check.md",
    "reports/completion-report.md",
]
NON_PRODUCTION_PATTERNS = [
    re.compile(r"not\s+claim\s+production", re.IGNORECASE),
    re.compile(r"does\s+not\s+claim\s+production", re.IGNORECASE),
    re.compile(r"not\s+production\s+truth", re.IGNORECASE),
    re.compile(r"不是\s*production\s*truth", re.IGNORECASE),
    re.compile(r"不能声称.*production", re.IGNORECASE),
    re.compile(r"非\s*production", re.IGNORECASE),
]


def _summary_from_review_index(review_index: Any) -> dict[str, Any]:
    if not isinstance(review_index, dict):
        return {}
    summary = review_index.get("summary")
    return summary if isinstance(summary, dict) else {}


def _blocking_count_from_findings(review_index: Any) -> int | None:
    if not isinstance(review_index, dict):
        return None
    findings = review_index.get("findings")
    if not isinstance(findings, list):
        return None
    count = 0
    for finding in findings:
        if isinstance(finding, dict) and finding.get("severity") == "blocking":
            count += 1
    return count


def _question_blocking_count(ask_questions: Any) -> int | None:
    if not isinstance(ask_questions, dict):
        return None
    explicit = ask_questions.get("blocking_count")
    if isinstance(explicit, int):
        return explicit
    questions = ask_questions.get("questions")
    if not isinstance(questions, list):
        return None
    return sum(1 for item in questions if isinstance(item, dict) and item.get("severity") == "blocking")


def _validate_questions(ask_questions: Any, path: Path, issues: list[dict[str, str]]) -> None:
    if not isinstance(ask_questions, dict):
        add_issue(issues, "ERROR", path, "ask-user-questions.json must parse to a JSON object")
        return
    questions = ask_questions.get("questions")
    if not isinstance(questions, list):
        add_issue(issues, "ERROR", path, "ask-user-questions.json must contain a questions list")
        return
    explicit = ask_questions.get("blocking_count")
    computed = sum(1 for item in questions if isinstance(item, dict) and item.get("severity") == "blocking")
    if isinstance(explicit, int) and explicit != computed:
        add_issue(
            issues,
            "ERROR",
            path,
            f"blocking_count {explicit} does not match blocking questions count {computed}",
        )


def _non_production_statement_present(text: str) -> bool:
    return any(pattern.search(text) for pattern in NON_PRODUCTION_PATTERNS)


def _extract_completion_readiness(text: str) -> str | None:
    for value in READINESS_VALUES:
        if value in text:
            return value
    return None


def validate_review_artifacts(
    repo_root: Path,
    kb_pack_id: str,
) -> tuple[dict[str, Any], list[dict[str, str]]]:
    repo_root = repo_root.resolve()
    kb_dir = repo_root / "kb" / kb_pack_id
    issues: list[dict[str, str]] = []
    result: dict[str, Any] = {
        "review_index": None,
        "ask_user_questions": None,
        "readiness": None,
    }

    for filename in REQUIRED_REVIEW_FILES:
        path = kb_dir / filename
        if not path.exists():
            add_issue(issues, "ERROR", path, "required review artifact is missing")

    review_path = kb_dir / "review" / "review-index.yaml"
    ask_path = kb_dir / "review" / "ask-user-questions.json"
    completion_path = kb_dir / "reports" / "completion-report.md"

    review_index = load_yaml(review_path, issues) if review_path.exists() else None
    ask_questions = load_json(ask_path, issues) if ask_path.exists() else None
    result["review_index"] = review_index
    result["ask_user_questions"] = ask_questions

    if review_index is not None and not isinstance(review_index, dict):
        add_issue(issues, "ERROR", review_path, "review-index.yaml must parse to a mapping")
    if isinstance(review_index, dict):
        summary = _summary_from_review_index(review_index)
        readiness = summary.get("readiness")
        if readiness is not None:
            if readiness not in READINESS_VALUES:
                add_issue(issues, "ERROR", review_path, f"invalid readiness value {readiness!r}")
            result["readiness"] = readiness

        summary_blocking = summary.get("blocking_count")
        finding_blocking = _blocking_count_from_findings(review_index)
        if isinstance(summary_blocking, int) and finding_blocking is not None and summary_blocking != finding_blocking:
            add_issue(
                issues,
                "ERROR",
                review_path,
                f"summary.blocking_count {summary_blocking} does not match blocking findings count {finding_blocking}",
            )

    if ask_questions is not None:
        _validate_questions(ask_questions, ask_path, issues)

    ask_blocking = _question_blocking_count(ask_questions)
    review_summary = _summary_from_review_index(review_index)
    review_blocking = review_summary.get("blocking_count")
    if isinstance(ask_blocking, int) and isinstance(review_blocking, int) and ask_blocking != review_blocking:
        add_issue(
            issues,
            "ERROR",
            ask_path,
            f"ask-user blocking_count {ask_blocking} does not match review summary blocking_count {review_blocking}",
        )

    if completion_path.exists():
        text = completion_path.read_text(encoding="utf-8")
        if not _non_production_statement_present(text):
            add_issue(
                issues,
                "ERROR",
                completion_path,
                "completion report must include an explicit non-production statement",
            )
        completion_readiness = _extract_completion_readiness(text)
        if completion_readiness is None:
            add_issue(issues, "ERROR", completion_path, "completion report must include an allowed readiness value")
        else:
            result["readiness"] = result["readiness"] or completion_readiness

    return result, issues
