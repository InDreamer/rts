#!/usr/bin/env python3
"""Validate KB draft artifacts for the source-backed KB draft MVP."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from validate_source_bundle import (
    NON_TRUTH_CLAIM_STATUSES,
    TRUTH_CLAIM_STATUSES,
    add_issue,
    load_yaml,
)


REQUIRED_KB_FILES = [
    "metadata.yaml",
    "README.md",
    "evidence/evidence-index.yaml",
    "review/review-index.yaml",
    "reports/extraction-report.md",
    "reports/review-checklist.md",
    "reports/closure-check.md",
    "reports/completion-report.md",
]
REQUIRED_KB_DIRS = ["rules", "lookups", "helpers", "evidence", "review", "reports"]
REQUIRED_OBJECT_FIELDS = [
    "schema_version",
    "id",
    "object_type",
    "status",
    "signoff_status",
    "revision",
    "scope",
    "claim_refs",
    "claim_status_used",
    "dependencies",
]
OBJECT_DIRS = {
    "rules": "rule",
    "lookups": "lookup",
    "helpers": "helper",
}


def _metadata_source_bundle_id(metadata: Any) -> str | None:
    if not isinstance(metadata, dict):
        return None
    source_bundle = metadata.get("source_bundle")
    if isinstance(source_bundle, dict):
        source_id = source_bundle.get("id")
        if isinstance(source_id, str):
            return source_id
    return None


def _iter_yaml_objects(kb_dir: Path) -> list[tuple[Path, Any, str]]:
    objects: list[tuple[Path, Any, str]] = []
    for directory, expected_type in OBJECT_DIRS.items():
        object_dir = kb_dir / directory
        if not object_dir.exists():
            continue
        for path in sorted(object_dir.glob("*.yaml")):
            objects.append((path, expected_type, directory))
    return objects


def _extract_evidence_ids(evidence_index: Any) -> set[str]:
    ids: set[str] = set()
    if not isinstance(evidence_index, dict):
        return ids
    evidence = evidence_index.get("evidence")
    if not isinstance(evidence, list):
        return ids
    for item in evidence:
        if isinstance(item, dict) and isinstance(item.get("evidence_id"), str):
            ids.add(item["evidence_id"])
    return ids


def _extract_review_decision_ids(review_index: Any) -> set[str]:
    ids: set[str] = set()
    if not isinstance(review_index, dict):
        return ids
    decisions = review_index.get("decisions")
    if isinstance(decisions, list):
        for item in decisions:
            if isinstance(item, dict) and isinstance(item.get("decision_id"), str):
                ids.add(item["decision_id"])
    findings = review_index.get("findings")
    if isinstance(findings, list):
        for item in findings:
            if isinstance(item, dict) and isinstance(item.get("finding_id"), str):
                ids.add(item["finding_id"])
    return ids


def _validate_metadata_paths(metadata: Any, kb_dir: Path, path: Path, issues: list[dict[str, str]]) -> None:
    if not isinstance(metadata, dict):
        add_issue(issues, "ERROR", path, "metadata.yaml must parse to a mapping")
        return

    indexes = metadata.get("indexes")
    if not isinstance(indexes, dict):
        add_issue(issues, "ERROR", path, "metadata.yaml missing indexes mapping")
        return

    for field, rel_path in indexes.items():
        if not isinstance(rel_path, str) or not rel_path:
            add_issue(issues, "ERROR", path, f"metadata indexes.{field} must be a non-empty path")
            continue
        if "{" in rel_path:
            add_issue(issues, "WARN", path, f"metadata indexes.{field} still contains placeholder path {rel_path!r}")
            continue
        if not (kb_dir / rel_path).exists():
            add_issue(issues, "ERROR", path, f"metadata indexes.{field} path does not exist: {rel_path}")


def _validate_object(
    obj: Any,
    path: Path,
    expected_type: str,
    claims_by_id: dict[str, dict[str, Any]],
    evidence_ids: set[str],
    review_ids: set[str],
    issues: list[dict[str, str]],
) -> str | None:
    if not isinstance(obj, dict):
        add_issue(issues, "ERROR", path, "KB object must parse to a mapping")
        return None

    object_id = obj.get("id")
    label = f"object {object_id!r}" if isinstance(object_id, str) else f"object {path.name}"
    for field in REQUIRED_OBJECT_FIELDS:
        if field not in obj:
            add_issue(issues, "ERROR", path, f"{label}: missing required field {field!r}")

    if obj.get("object_type") != expected_type:
        add_issue(issues, "ERROR", path, f"{label}: object_type must be {expected_type!r}, got {obj.get('object_type')!r}")

    if not isinstance(object_id, str) or not object_id:
        add_issue(issues, "ERROR", path, f"{label}: id must be a non-empty string")
        object_id = None

    claim_refs = obj.get("claim_refs")
    if not isinstance(claim_refs, list) or not claim_refs:
        add_issue(issues, "ERROR", path, f"{label}: every nontrivial KB object must have claim_refs")
        claim_refs = []

    used_statuses = obj.get("claim_status_used")
    if not isinstance(used_statuses, list) or not used_statuses:
        add_issue(issues, "ERROR", path, f"{label}: claim_status_used must be a non-empty list")
        used_statuses = []

    for status in used_statuses:
        if status not in TRUTH_CLAIM_STATUSES:
            add_issue(issues, "ERROR", path, f"{label}: disallowed claim_status_used {status!r} enters KB truth")

    for ref in claim_refs:
        if not isinstance(ref, str) or not ref:
            add_issue(issues, "ERROR", path, f"{label}: claim_refs entries must be non-empty strings")
            continue
        claim = claims_by_id.get(ref)
        if claim is None:
            add_issue(issues, "ERROR", path, f"{label}: claim_ref {ref!r} does not resolve to claims.jsonl")
            continue
        status = claim.get("status")
        if status in NON_TRUTH_CLAIM_STATUSES:
            add_issue(issues, "ERROR", path, f"{label}: claim_ref {ref!r} has disallowed status {status!r}")
        elif status not in TRUTH_CLAIM_STATUSES:
            add_issue(issues, "ERROR", path, f"{label}: claim_ref {ref!r} has invalid status {status!r}")

    source_anchors = obj.get("source_anchors")
    evidence_refs = obj.get("evidence_refs")
    if not source_anchors and not evidence_refs:
        add_issue(issues, "ERROR", path, f"{label}: missing source_anchors or evidence_refs")

    if isinstance(source_anchors, list):
        for index, anchor in enumerate(source_anchors, 1):
            if not isinstance(anchor, dict):
                add_issue(issues, "ERROR", path, f"{label}: source_anchors[{index}] must be a mapping")
                continue
            locator_ref = anchor.get("locator_ref")
            if isinstance(locator_ref, str) and ("source-index.yaml" in locator_ref or "workflow-map.yaml" in locator_ref):
                add_issue(
                    issues,
                    "WARN",
                    path,
                    f"{label}: source_anchors[{index}] references inventory file; validator cannot prove source truth from inventory alone",
                )

    if isinstance(evidence_refs, list):
        for ref in evidence_refs:
            if isinstance(ref, str) and ref and evidence_ids and ref not in evidence_ids:
                add_issue(issues, "WARN", path, f"{label}: evidence_ref {ref!r} does not resolve in evidence-index.yaml")

    review_refs = obj.get("review_refs")
    if isinstance(review_refs, list):
        for ref in review_refs:
            if isinstance(ref, str) and ref and review_ids and ref not in review_ids:
                add_issue(issues, "WARN", path, f"{label}: review_ref {ref!r} does not resolve in review-index.yaml")

    dependencies = obj.get("dependencies")
    if dependencies is not None and not isinstance(dependencies, (dict, list)):
        add_issue(issues, "ERROR", path, f"{label}: dependencies must be a mapping or list")

    return object_id


def validate_kb_draft(
    repo_root: Path,
    kb_pack_id: str,
    expected_source_bundle_id: str,
    claims_by_id: dict[str, dict[str, Any]],
) -> tuple[dict[str, Any], list[dict[str, str]]]:
    repo_root = repo_root.resolve()
    kb_dir = repo_root / "kb" / kb_pack_id
    issues: list[dict[str, str]] = []
    result: dict[str, Any] = {
        "kb_dir": kb_dir,
        "metadata": None,
        "objects": {},
        "object_counts": {"rule": 0, "lookup": 0, "helper": 0},
    }

    if not kb_dir.exists():
        add_issue(issues, "ERROR", kb_dir, "KB pack directory does not exist")
        return result, issues

    for dirname in REQUIRED_KB_DIRS:
        path = kb_dir / dirname
        if not path.is_dir():
            add_issue(issues, "ERROR", path, "required KB directory is missing")
    for filename in REQUIRED_KB_FILES:
        path = kb_dir / filename
        if not path.exists():
            add_issue(issues, "ERROR", path, "required KB file is missing")

    metadata_path = kb_dir / "metadata.yaml"
    metadata = load_yaml(metadata_path, issues) if metadata_path.exists() else None
    result["metadata"] = metadata
    if metadata is not None:
        actual_source_bundle_id = _metadata_source_bundle_id(metadata)
        if actual_source_bundle_id != expected_source_bundle_id:
            add_issue(
                issues,
                "ERROR",
                metadata_path,
                f"metadata source_bundle.id must be {expected_source_bundle_id!r}, got {actual_source_bundle_id!r}",
            )
        _validate_metadata_paths(metadata, kb_dir, metadata_path, issues)

    evidence_path = kb_dir / "evidence" / "evidence-index.yaml"
    evidence_index = load_yaml(evidence_path, issues) if evidence_path.exists() else None
    evidence_ids = _extract_evidence_ids(evidence_index)

    review_path = kb_dir / "review" / "review-index.yaml"
    review_index = load_yaml(review_path, issues) if review_path.exists() else None
    review_ids = _extract_review_decision_ids(review_index)

    object_ids: dict[str, Path] = {}
    for path, expected_type, _directory in _iter_yaml_objects(kb_dir):
        obj = load_yaml(path, issues)
        object_id = _validate_object(obj, path, expected_type, claims_by_id, evidence_ids, review_ids, issues)
        if isinstance(obj, dict) and obj.get("object_type") in result["object_counts"]:
            result["object_counts"][obj["object_type"]] += 1
        if object_id:
            if object_id in object_ids:
                add_issue(issues, "ERROR", path, f"duplicate KB object id {object_id!r} also used by {object_ids[object_id]}")
            object_ids[object_id] = path
            result["objects"][object_id] = obj

    return result, issues
