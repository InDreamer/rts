#!/usr/bin/env python3
"""Validate source-backed KB draft MVP artifacts.

This validator checks artifact contracts only. It does not validate business
correctness, source accuracy, production readiness, or snapshot/signoff/runtime
projection readiness.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path
from typing import Any

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from validate_kb_draft import validate_kb_draft
from validate_review_artifacts import validate_review_artifacts
from validate_source_bundle import add_issue, validate_source_bundle


SCOPE_STATEMENT = (
    "Scope: artifact-contract validation only; this does not validate business truth, "
    "source accuracy, production readiness, or snapshot/signoff/runtime projection readiness."
)


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Validate generated source-backed KB draft MVP artifacts.",
        epilog=SCOPE_STATEMENT,
    )
    parser.add_argument("--repo-root", required=True, help="Repository root containing sources/ and kb/.")
    parser.add_argument("--source-bundle", required=True, help="Source bundle id under sources/.")
    parser.add_argument("--kb-pack", required=True, help="KB pack id under kb/.")
    parser.add_argument(
        "--source-root",
        help="Optional company source root. When provided, allowed source anchor paths are checked for existence.",
    )
    return parser.parse_args(argv)


def _relativize(path: str, repo_root: Path) -> str:
    try:
        return Path(path).resolve().relative_to(repo_root).as_posix()
    except Exception:
        return path


def _print_issues(level: str, issues: list[dict[str, str]], repo_root: Path) -> None:
    filtered = [issue for issue in issues if issue["level"] == level]
    if not filtered:
        print(f"{level}: none")
        return
    print(f"{level}: {len(filtered)}")
    for issue in filtered:
        path = _relativize(issue["path"], repo_root)
        print(f"- {level}: {path}: {issue['message']}")


def _count_by_level(issues: list[dict[str, str]], level: str) -> int:
    return sum(1 for issue in issues if issue["level"] == level)


def validate_mvp(
    repo_root: Path,
    source_bundle_id: str,
    kb_pack_id: str,
    source_root: Path | None = None,
) -> tuple[dict[str, Any], list[dict[str, str]]]:
    issues: list[dict[str, str]] = []
    result: dict[str, Any] = {
        "source": None,
        "kb": None,
        "review": None,
    }

    source_result, source_issues = validate_source_bundle(repo_root, source_bundle_id, source_root)
    issues.extend(source_issues)
    result["source"] = source_result

    kb_result, kb_issues = validate_kb_draft(
        repo_root,
        kb_pack_id,
        source_bundle_id,
        source_result.get("claims_by_id", {}),
    )
    issues.extend(kb_issues)
    result["kb"] = kb_result

    review_result, review_issues = validate_review_artifacts(repo_root, kb_pack_id)
    issues.extend(review_issues)
    result["review"] = review_result

    # Cross-check that the requested ids agree with artifact locations where possible.
    metadata = kb_result.get("metadata")
    if isinstance(metadata, dict):
        pack = metadata.get("pack")
        if isinstance(pack, dict):
            actual_pack_id = pack.get("id")
            if isinstance(actual_pack_id, str) and actual_pack_id != kb_pack_id:
                add_issue(
                    issues,
                    "ERROR",
                    repo_root / "kb" / kb_pack_id / "metadata.yaml",
                    f"metadata pack.id must be {kb_pack_id!r}, got {actual_pack_id!r}",
                )

    return result, issues


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    repo_root = Path(args.repo_root).resolve()
    source_root = Path(args.source_root).resolve() if args.source_root else None

    result, issues = validate_mvp(repo_root, args.source_bundle, args.kb_pack, source_root)
    error_count = _count_by_level(issues, "ERROR")
    warn_count = _count_by_level(issues, "WARN")

    print("RTS Source-to-KB MVP Artifact Validator")
    print(SCOPE_STATEMENT)
    print(f"Repo root: {repo_root}")
    print(f"Source bundle: {args.source_bundle}")
    print(f"KB pack: {args.kb_pack}")
    if source_root:
        print(f"Source root: {source_root}")

    source = result.get("source") or {}
    kb = result.get("kb") or {}
    review = result.get("review") or {}
    claims = source.get("claims_by_id") or {}
    object_counts = kb.get("object_counts") or {}
    print("")
    print("Summary")
    print(f"- Claims: {len(claims)}")
    print(
        "- KB objects: "
        f"rules={object_counts.get('rule', 0)}, "
        f"lookups={object_counts.get('lookup', 0)}, "
        f"helpers={object_counts.get('helper', 0)}"
    )
    print(f"- Readiness: {review.get('readiness') or 'unknown'}")
    print(f"- Errors: {error_count}")
    print(f"- Warnings: {warn_count}")
    print("")
    _print_issues("ERROR", issues, repo_root)
    print("")
    _print_issues("WARN", issues, repo_root)

    if error_count:
        print("")
        print("Result: FAIL")
        return 1
    print("")
    print("Result: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
