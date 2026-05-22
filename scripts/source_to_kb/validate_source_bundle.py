#!/usr/bin/env python3
"""Validate source inventory artifacts for the source-backed KB draft MVP."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

try:
    import yaml
except ImportError:  # pragma: no cover - exercised only in missing dependency envs
    yaml = None


ALLOWED_CLAIM_STATUSES = {
    "supported",
    "user_confirmed",
    "runtime_observed",
    "blocked",
    "unsupported",
    "inferred",
    "contradicted",
    "not_accessible",
}
TRUTH_CLAIM_STATUSES = {"supported", "user_confirmed", "runtime_observed"}
NON_TRUTH_CLAIM_STATUSES = ALLOWED_CLAIM_STATUSES - TRUTH_CLAIM_STATUSES
WORKFLOW_STATUSES = {
    "found",
    "not_found",
    "not_applicable",
    "not_accessible",
    "needs_user_confirmation",
}
REQUIRED_SOURCE_FILES = [
    "source-manifest.yaml",
    "source-index.yaml",
    "workflow-map.yaml",
    "claims.jsonl",
    "unresolved-questions.yaml",
]
REQUIRED_CLAIM_FIELDS = [
    "schema_version",
    "claim_id",
    "claim_type",
    "status",
    "subject",
    "assertion",
    "source_anchors",
    "evidence_type",
    "extraction_method",
    "confidence",
    "limits",
]
PERMISSION_ALLOW_VALUES = {
    "allow",
    "allowed",
    "read",
    "readable",
    "available",
    "not_required",
}
PERMISSION_DENY_VALUES = {
    "deny",
    "denied",
    "forbidden",
    "not_accessible",
    "restricted",
}


def add_issue(issues: list[dict[str, str]], level: str, path: str | Path, message: str) -> None:
    issues.append({"level": level, "path": str(path), "message": message})


def load_yaml(path: Path, issues: list[dict[str, str]]) -> Any:
    if yaml is None:
        add_issue(issues, "ERROR", path, "PyYAML is required to parse YAML artifacts")
        return None
    try:
        with path.open("r", encoding="utf-8") as handle:
            return yaml.safe_load(handle)
    except yaml.YAMLError as exc:
        add_issue(issues, "ERROR", path, f"invalid YAML: {exc}")
    except OSError as exc:
        add_issue(issues, "ERROR", path, f"cannot read file: {exc}")
    return None


def load_json(path: Path, issues: list[dict[str, str]]) -> Any:
    try:
        with path.open("r", encoding="utf-8") as handle:
            return json.load(handle)
    except json.JSONDecodeError as exc:
        add_issue(issues, "ERROR", path, f"invalid JSON: {exc}")
    except OSError as exc:
        add_issue(issues, "ERROR", path, f"cannot read file: {exc}")
    return None


def load_claims_jsonl(path: Path, issues: list[dict[str, str]]) -> list[dict[str, Any]]:
    claims: list[dict[str, Any]] = []
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except OSError as exc:
        add_issue(issues, "ERROR", path, f"cannot read file: {exc}")
        return claims

    for line_number, line in enumerate(lines, 1):
        if not line.strip():
            continue
        try:
            value = json.loads(line)
        except json.JSONDecodeError as exc:
            add_issue(issues, "ERROR", path, f"line {line_number}: invalid JSONL: {exc}")
            continue
        if not isinstance(value, dict):
            add_issue(issues, "ERROR", path, f"line {line_number}: claim record must be a JSON object")
            continue
        value["_line_number"] = line_number
        claims.append(value)
    return claims


def find_duplicate_ids(records: list[dict[str, Any]], id_field: str) -> set[str]:
    seen: set[str] = set()
    duplicates: set[str] = set()
    for record in records:
        value = record.get(id_field)
        if not isinstance(value, str):
            continue
        if value in seen:
            duplicates.add(value)
        seen.add(value)
    return duplicates


def validate_required_mapping_fields(
    record: dict[str, Any],
    required_fields: list[str],
    path: Path,
    issues: list[dict[str, str]],
    label: str,
) -> None:
    for field in required_fields:
        if field not in record:
            add_issue(issues, "ERROR", path, f"{label}: missing required field {field!r}")


def get_source_items(source_index: Any, path: Path, issues: list[dict[str, str]]) -> dict[str, dict[str, Any]]:
    if not isinstance(source_index, dict):
        add_issue(issues, "ERROR", path, "source-index.yaml must parse to a mapping")
        return {}
    items = source_index.get("items")
    if not isinstance(items, list):
        add_issue(issues, "ERROR", path, "source-index.yaml must contain an items list")
        return {}

    by_id: dict[str, dict[str, Any]] = {}
    for index, item in enumerate(items, 1):
        if not isinstance(item, dict):
            add_issue(issues, "ERROR", path, f"items[{index}] must be a mapping")
            continue
        source_id = item.get("source_id")
        if not isinstance(source_id, str) or not source_id:
            add_issue(issues, "ERROR", path, f"items[{index}] missing non-empty source_id")
            continue
        if source_id in by_id:
            add_issue(issues, "ERROR", path, f"duplicate source_id {source_id!r}")
        by_id[source_id] = item
    return by_id


def _permission_allows(item: dict[str, Any]) -> bool:
    permission = str(item.get("permission_state", "")).strip().lower()
    if not permission:
        return True
    if permission in PERMISSION_DENY_VALUES:
        return False
    if permission in PERMISSION_ALLOW_VALUES:
        return True
    return True


def _anchor_path(anchor: dict[str, Any], item: dict[str, Any] | None) -> str | None:
    path = anchor.get("path")
    if isinstance(path, str) and path:
        return path
    if isinstance(item, dict):
        locator = item.get("locator")
        if isinstance(locator, dict):
            item_path = locator.get("path")
            if isinstance(item_path, str) and item_path:
                return item_path
    return None


def _resolve_source_path(source_root: Path, source_path: str) -> Path:
    candidate = Path(source_path)
    if candidate.is_absolute():
        return candidate
    return source_root / candidate


def validate_source_anchor_paths(
    source_root: Path,
    claims: list[dict[str, Any]],
    source_items: dict[str, dict[str, Any]],
    issues: list[dict[str, str]],
    claims_path: Path,
) -> None:
    for claim in claims:
        anchors = claim.get("source_anchors")
        if not isinstance(anchors, list):
            continue
        for index, anchor in enumerate(anchors, 1):
            if not isinstance(anchor, dict):
                continue
            source_id = anchor.get("source_id")
            item = source_items.get(source_id) if isinstance(source_id, str) else None
            if item is not None and not _permission_allows(item):
                continue
            source_path = _anchor_path(anchor, item)
            if not source_path:
                continue
            if "{" in source_path or "..." in source_path:
                add_issue(
                    issues,
                    "WARN",
                    claims_path,
                    f"claim {claim.get('claim_id')!r} anchor {index} uses placeholder-like path {source_path!r}",
                )
                continue
            resolved = _resolve_source_path(source_root, source_path)
            if not resolved.exists():
                add_issue(
                    issues,
                    "ERROR",
                    claims_path,
                    f"claim {claim.get('claim_id')!r} anchor path does not exist under --source-root: {source_path}",
                )


def validate_claims(
    claims: list[dict[str, Any]],
    claims_path: Path,
    source_items: dict[str, dict[str, Any]],
    issues: list[dict[str, str]],
) -> dict[str, dict[str, Any]]:
    for claim in claims:
        claim_label = f"claim line {claim.get('_line_number')}"
        validate_required_mapping_fields(claim, REQUIRED_CLAIM_FIELDS, claims_path, issues, claim_label)

        claim_id = claim.get("claim_id")
        if not isinstance(claim_id, str) or not claim_id:
            add_issue(issues, "ERROR", claims_path, f"{claim_label}: claim_id must be a non-empty string")

        status = claim.get("status")
        if status not in ALLOWED_CLAIM_STATUSES:
            add_issue(issues, "ERROR", claims_path, f"{claim_label}: invalid claim status {status!r}")

        anchors = claim.get("source_anchors")
        if not isinstance(anchors, list):
            add_issue(issues, "ERROR", claims_path, f"{claim_label}: source_anchors must be a list")
            anchors = []

        if status in TRUTH_CLAIM_STATUSES and not anchors:
            add_issue(
                issues,
                "ERROR",
                claims_path,
                f"{claim_label}: {status} claim must include at least one source anchor",
            )

        limits = claim.get("limits")
        if "limits" in claim and not isinstance(limits, list):
            add_issue(issues, "ERROR", claims_path, f"{claim_label}: limits must be a list")

        for index, anchor in enumerate(anchors, 1):
            if not isinstance(anchor, dict):
                add_issue(issues, "ERROR", claims_path, f"{claim_label}: source_anchors[{index}] must be a mapping")
                continue
            source_id = anchor.get("source_id")
            if not isinstance(source_id, str) or not source_id:
                add_issue(issues, "ERROR", claims_path, f"{claim_label}: source_anchors[{index}] missing source_id")
                continue
            if source_id not in source_items:
                add_issue(
                    issues,
                    "ERROR",
                    claims_path,
                    f"{claim_label}: source_anchors[{index}].source_id {source_id!r} does not resolve to source-index items",
                )

    for duplicate in sorted(find_duplicate_ids(claims, "claim_id")):
        add_issue(issues, "ERROR", claims_path, f"duplicate claim_id {duplicate!r}")

    return {
        claim["claim_id"]: claim
        for claim in claims
        if isinstance(claim.get("claim_id"), str) and claim.get("claim_id")
    }


def _status_at(path: Path, label: str, value: Any, issues: list[dict[str, str]]) -> None:
    if value not in WORKFLOW_STATUSES:
        add_issue(issues, "ERROR", path, f"{label} must have explicit status in {sorted(WORKFLOW_STATUSES)}, got {value!r}")


def validate_workflow_map(workflow_map: Any, path: Path, unresolved: Any, issues: list[dict[str, str]]) -> None:
    if not isinstance(workflow_map, dict):
        add_issue(issues, "ERROR", path, "workflow-map.yaml must parse to a mapping")
        return

    entrypoints = workflow_map.get("entrypoints")
    if isinstance(entrypoints, dict):
        for name, entrypoint in entrypoints.items():
            if isinstance(entrypoint, dict):
                _status_at(path, f"entrypoints.{name}.status", entrypoint.get("status"), issues)
    else:
        add_issue(issues, "ERROR", path, "workflow-map.yaml missing entrypoints mapping")

    classification = workflow_map.get("classification")
    if isinstance(classification, dict):
        _status_at(path, "classification.status", classification.get("status"), issues)
    else:
        add_issue(issues, "ERROR", path, "workflow-map.yaml missing classification mapping")

    transformation_flow = workflow_map.get("transformation_flow")
    if not isinstance(transformation_flow, list) or not transformation_flow:
        add_issue(issues, "ERROR", path, "workflow-map.yaml must contain a non-empty transformation_flow list")
        return

    unresolved_count = 0
    if isinstance(unresolved, dict) and isinstance(unresolved.get("questions"), list):
        unresolved_count = len(unresolved["questions"])

    for index, step in enumerate(transformation_flow, 1):
        if not isinstance(step, dict):
            add_issue(issues, "ERROR", path, f"transformation_flow[{index}] must be a mapping")
            continue
        label = step.get("step_id", f"step-{index}")
        _status_at(path, f"transformation_flow[{label}].status", step.get("status"), issues)
        source_refs = step.get("source_refs")
        if source_refs is None:
            add_issue(issues, "ERROR", path, f"transformation_flow[{label}] missing source_refs list")
            source_refs = []
        if not isinstance(source_refs, list):
            add_issue(issues, "ERROR", path, f"transformation_flow[{label}].source_refs must be a list")
            source_refs = []
        if step.get("status") == "found" and not source_refs:
            add_issue(issues, "ERROR", path, f"transformation_flow[{label}] is found but has no source_refs")
        if step.get("status") in {"not_found", "not_accessible", "needs_user_confirmation"} and not source_refs and unresolved_count == 0:
            add_issue(
                issues,
                "WARN",
                path,
                f"transformation_flow[{label}] has no source_refs and no unresolved questions are recorded",
            )


def validate_source_bundle(
    repo_root: Path,
    source_bundle_id: str,
    source_root: Path | None = None,
) -> tuple[dict[str, Any], list[dict[str, str]]]:
    repo_root = repo_root.resolve()
    source_dir = repo_root / "sources" / source_bundle_id
    issues: list[dict[str, str]] = []
    result: dict[str, Any] = {
        "source_dir": source_dir,
        "claims": [],
        "claims_by_id": {},
        "source_items_by_id": {},
        "manifest": None,
        "source_index": None,
        "workflow_map": None,
        "unresolved_questions": None,
    }

    if not source_dir.exists():
        add_issue(issues, "ERROR", source_dir, "source bundle directory does not exist")
        return result, issues

    for filename in REQUIRED_SOURCE_FILES:
        path = source_dir / filename
        if not path.exists():
            add_issue(issues, "ERROR", path, "required source bundle file is missing")

    manifest_path = source_dir / "source-manifest.yaml"
    source_index_path = source_dir / "source-index.yaml"
    workflow_map_path = source_dir / "workflow-map.yaml"
    claims_path = source_dir / "claims.jsonl"
    unresolved_path = source_dir / "unresolved-questions.yaml"

    if manifest_path.exists():
        result["manifest"] = load_yaml(manifest_path, issues)
    if source_index_path.exists():
        result["source_index"] = load_yaml(source_index_path, issues)
    if unresolved_path.exists():
        result["unresolved_questions"] = load_yaml(unresolved_path, issues)
    if workflow_map_path.exists():
        result["workflow_map"] = load_yaml(workflow_map_path, issues)
    if claims_path.exists():
        result["claims"] = load_claims_jsonl(claims_path, issues)

    source_items = get_source_items(result["source_index"], source_index_path, issues) if result["source_index"] is not None else {}
    result["source_items_by_id"] = source_items
    claims_by_id = validate_claims(result["claims"], claims_path, source_items, issues) if claims_path.exists() else {}
    result["claims_by_id"] = claims_by_id

    if result["workflow_map"] is not None:
        validate_workflow_map(result["workflow_map"], workflow_map_path, result["unresolved_questions"], issues)

    if source_root is not None and claims_path.exists():
        validate_source_anchor_paths(source_root.resolve(), result["claims"], source_items, issues, claims_path)

    return result, issues
