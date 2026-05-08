#!/usr/bin/env python3
"""Validate the PD-3 documentation contract for a repository.

This script checks:
- hidden `docmeta` blocks in governed markdown files
- allowed roles and layer bounds
- parent/child consistency and max depth of three layers
- presence and structure of docs/catalog.yaml
- optional warnings about oversized routing docs and missing source paths

Usage:
    python validate_doc_contract.py <repo-root>
    python validate_doc_contract.py <repo-root> --strict
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import yaml

DOCMETA_RE = re.compile(r"<!--\s*docmeta\n(.*?)\n-->", re.DOTALL)
ALLOWED_ROLES = {"entry", "domain", "leaf", "agent"}
EXCLUDED_DIRS = {
    ".git",
    ".hg",
    ".svn",
    ".venv",
    "venv",
    "node_modules",
    "dist",
    "build",
    "coverage",
    "__pycache__",
}


@dataclass
class Issue:
    level: str
    message: str


@dataclass
class DocRecord:
    path: str
    role: str
    layer: int
    parent: str | None
    children: list[str]
    summary: str
    read_when: list[str]
    skip_when: list[str]
    source_of_truth: list[str]
    body_line_count: int


class Validator:
    def __init__(self, root: Path, strict: bool = False) -> None:
        self.root = root.resolve()
        self.strict = strict
        self.issues: list[Issue] = []
        self.docs: dict[str, DocRecord] = {}

    def error(self, message: str) -> None:
        self.issues.append(Issue("ERROR", message))

    def warn(self, message: str) -> None:
        self.issues.append(Issue("WARN", message))

    def discover_candidates(self) -> list[Path]:
        candidates: set[Path] = set()

        readme = self.root / "README.md"
        if readme.exists():
            candidates.add(readme)

        docs_dir = self.root / "docs"
        if docs_dir.exists():
            for path in docs_dir.rglob("*.md"):
                if not self._is_excluded(path):
                    candidates.add(path)

        for path in self.root.rglob("AGENTS.md"):
            if not self._is_excluded(path):
                candidates.add(path)

        return sorted(candidates)

    def _is_excluded(self, path: Path) -> bool:
        if any(part in EXCLUDED_DIRS for part in path.parts):
            return True
        rel = path.relative_to(self.root).as_posix()
        if rel.startswith("docs/archive/reference-proposals/"):
            return True
        if rel.startswith("docs/archive/generated-artifacts/") and path.name != "README.md":
            return True
        return False

    def relative(self, path: Path) -> str:
        return path.relative_to(self.root).as_posix()

    def parse_docmeta(self, path: Path) -> dict[str, Any] | None:
        text = path.read_text(encoding="utf-8")
        match = DOCMETA_RE.search(text[:4000])
        if not match:
            return None
        try:
            data = yaml.safe_load(match.group(1))
        except yaml.YAMLError as exc:
            self.error(f"{self.relative(path)}: invalid YAML in docmeta: {exc}")
            return None
        if not isinstance(data, dict):
            self.error(f"{self.relative(path)}: docmeta must parse to a mapping")
            return None
        return data

    def build_record(self, path: Path, data: dict[str, Any]) -> DocRecord | None:
        rel = self.relative(path)
        required = [
            "role",
            "layer",
            "parent",
            "children",
            "summary",
            "read_when",
            "skip_when",
            "source_of_truth",
        ]
        missing = [key for key in required if key not in data]
        if missing:
            self.error(f"{rel}: missing docmeta fields: {', '.join(missing)}")
            return None

        role = data["role"]
        if role not in ALLOWED_ROLES:
            self.error(f"{rel}: role must be one of {sorted(ALLOWED_ROLES)}, got {role!r}")
            return None

        layer = data["layer"]
        if not isinstance(layer, int) or layer not in {1, 2, 3}:
            self.error(f"{rel}: layer must be integer 1, 2, or 3")
            return None

        if role == "entry" and layer != 1:
            self.error(f"{rel}: entry docs must use layer 1")
        if role == "domain" and layer != 2:
            self.error(f"{rel}: domain docs must use layer 2")
        if role == "leaf" and layer not in {2, 3}:
            self.error(f"{rel}: leaf docs must use layer 2 or 3")

        parent = data["parent"]
        if parent in {"", "null", "None"}:
            parent = None
        if parent is not None and not isinstance(parent, str):
            self.error(f"{rel}: parent must be a relative path string or null")
            return None
        if layer == 1 and parent is not None:
            self.error(f"{rel}: layer-1 docs must use parent: null")
        if layer > 1 and not parent:
            self.error(f"{rel}: layer-{layer} docs must declare a parent")

        children = data["children"]
        if not isinstance(children, list) or not all(isinstance(item, str) for item in children):
            self.error(f"{rel}: children must be a list of relative paths")
            return None
        if role == "leaf" and children:
            self.error(f"{rel}: leaf docs must not declare children")

        summary = data["summary"]
        if not isinstance(summary, str) or not summary.strip():
            self.error(f"{rel}: summary must be a non-empty string")
            return None

        read_when = data["read_when"]
        if not isinstance(read_when, list) or not read_when or not all(isinstance(item, str) and item.strip() for item in read_when):
            self.error(f"{rel}: read_when must be a non-empty list of strings")
            return None

        skip_when = data["skip_when"]
        if not isinstance(skip_when, list) or not skip_when or not all(isinstance(item, str) and item.strip() for item in skip_when):
            self.error(f"{rel}: skip_when must be a non-empty list of strings")
            return None

        source_of_truth = data["source_of_truth"]
        if not isinstance(source_of_truth, list) or not source_of_truth or not all(isinstance(item, str) and item.strip() for item in source_of_truth):
            self.error(f"{rel}: source_of_truth must be a non-empty list of strings")
            return None

        body_text = DOCMETA_RE.sub("", path.read_text(encoding="utf-8"), count=1).strip()
        body_line_count = len(body_text.splitlines()) if body_text else 0

        return DocRecord(
            path=rel,
            role=role,
            layer=layer,
            parent=parent,
            children=children,
            summary=summary.strip(),
            read_when=[item.strip() for item in read_when],
            skip_when=[item.strip() for item in skip_when],
            source_of_truth=[item.strip() for item in source_of_truth],
            body_line_count=body_line_count,
        )

    def validate_candidates(self) -> None:
        candidates = self.discover_candidates()
        docs_dir = self.root / "docs"

        for path in candidates:
            rel = self.relative(path)
            meta = self.parse_docmeta(path)
            required = (
                rel == "README.md"
                or rel == "docs/INDEX.md"
                or path.name == "AGENTS.md"
                or self.strict and rel.startswith("docs/")
            )
            if meta is None:
                if required:
                    self.error(f"{rel}: missing required docmeta block")
                elif rel.startswith("docs/"):
                    self.warn(f"{rel}: docs file is outside the governed contract because it has no docmeta block")
                continue

            record = self.build_record(path, meta)
            if record is not None:
                self.docs[record.path] = record

        if docs_dir.exists() and "docs/INDEX.md" not in self.docs:
            self.error("docs/INDEX.md: required as the primary docs entry point once docs/ exists")

    def validate_relationships(self) -> None:
        if not self.docs:
            self.error("no governed documents with docmeta were found")
            return

        for record in self.docs.values():
            if record.parent:
                parent = self.docs.get(record.parent)
                if parent is None:
                    self.error(f"{record.path}: parent {record.parent!r} is missing or has no valid docmeta")
                else:
                    if record.path not in parent.children:
                        self.error(f"{record.path}: parent {record.parent!r} does not list this child")
                    if record.layer != parent.layer + 1:
                        self.error(
                            f"{record.path}: layer {record.layer} must be exactly parent layer + 1 (parent {record.parent!r} is layer {parent.layer})"
                        )
            for child_path in record.children:
                child = self.docs.get(child_path)
                if child is None:
                    self.error(f"{record.path}: child {child_path!r} is missing or has no valid docmeta")
                    continue
                if child.parent != record.path:
                    self.error(f"{record.path}: child {child_path!r} points to parent {child.parent!r} instead of {record.path!r}")
                if child.layer != record.layer + 1:
                    self.error(
                        f"{record.path}: child {child_path!r} must be exactly one layer deeper (parent layer {record.layer}, child layer {child.layer})"
                    )

        for record in self.docs.values():
            for source in record.source_of_truth:
                source_path = self.root / source
                if not source_path.exists():
                    self.warn(f"{record.path}: source_of_truth path does not exist: {source}")

        for record in self.docs.values():
            if record.role in {"entry", "domain", "agent"} and record.body_line_count > 180:
                self.warn(f"{record.path}: routing document is long ({record.body_line_count} body lines); consider splitting or trimming")
            if record.role == "leaf" and record.body_line_count > 260:
                self.warn(f"{record.path}: leaf document is long ({record.body_line_count} body lines); confirm it still covers one decision surface")

    def validate_catalog(self) -> None:
        docs_dir = self.root / "docs"
        if not docs_dir.exists():
            return

        catalog_path = docs_dir / "catalog.yaml"
        if not catalog_path.exists():
            if self.strict:
                self.error("docs/catalog.yaml: missing required catalog in strict mode")
            else:
                self.warn("docs/catalog.yaml: missing catalog; add one for machine-readable first-pass routing")
            return

        try:
            catalog = yaml.safe_load(catalog_path.read_text(encoding="utf-8"))
        except yaml.YAMLError as exc:
            self.error(f"docs/catalog.yaml: invalid YAML: {exc}")
            return

        if not isinstance(catalog, dict):
            self.error("docs/catalog.yaml: root must be a mapping")
            return

        documents = catalog.get("documents")
        if not isinstance(documents, list):
            self.error("docs/catalog.yaml: documents must be a list")
            return

        catalog_map: dict[str, dict[str, Any]] = {}
        for index, item in enumerate(documents, start=1):
            if not isinstance(item, dict):
                self.error(f"docs/catalog.yaml: entry {index} must be a mapping")
                continue
            path = item.get("path")
            role = item.get("role")
            layer = item.get("layer")
            parent = item.get("parent")
            summary = item.get("summary")
            missing = [key for key, value in [("path", path), ("role", role), ("layer", layer), ("summary", summary)] if value is None]
            if missing:
                self.error(f"docs/catalog.yaml: entry {index} missing fields: {', '.join(missing)}")
                continue
            if not isinstance(path, str):
                self.error(f"docs/catalog.yaml: entry {index} path must be a string")
                continue
            catalog_map[path] = item
            if path in self.docs:
                record = self.docs[path]
                if role != record.role:
                    self.error(f"docs/catalog.yaml: {path} role {role!r} does not match docmeta {record.role!r}")
                if layer != record.layer:
                    self.error(f"docs/catalog.yaml: {path} layer {layer!r} does not match docmeta {record.layer!r}")
                normalized_parent = None if parent in {None, "", "null", "None"} else parent
                if normalized_parent != record.parent:
                    self.error(f"docs/catalog.yaml: {path} parent {parent!r} does not match docmeta {record.parent!r}")
            else:
                self.warn(f"docs/catalog.yaml: {path} is listed but the file has no valid governed docmeta")

        for path, record in sorted(self.docs.items()):
            if path not in catalog_map:
                message = f"docs/catalog.yaml: missing entry for governed document {path}"
                if self.strict:
                    self.error(message)
                else:
                    self.warn(message)

    def run(self) -> int:
        self.validate_candidates()
        self.validate_relationships()
        self.validate_catalog()

        errors = [issue for issue in self.issues if issue.level == "ERROR"]
        warnings = [issue for issue in self.issues if issue.level == "WARN"]

        if errors or warnings:
            for issue in self.issues:
                print(f"[{issue.level}] {issue.message}")
        else:
            print("[OK] PD-3 contract is valid")

        print(f"\nSummary: {len(errors)} error(s), {len(warnings)} warning(s)")
        return 1 if errors else 0


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate the PD-3 documentation contract")
    parser.add_argument("repo_root", help="Path to the repository root")
    parser.add_argument("--strict", action="store_true", help="Require catalog coverage for all governed docs and docmeta for all docs/* markdown files")
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    root = Path(args.repo_root)
    if not root.exists() or not root.is_dir():
        print(f"[ERROR] repo root does not exist or is not a directory: {root}")
        return 1
    validator = Validator(root, strict=args.strict)
    return validator.run()


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
