#!/usr/bin/env python3
"""Fixture tests for the source-backed KB draft MVP validator."""

from __future__ import annotations

import json
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
VALIDATOR = REPO_ROOT / "scripts" / "source_to_kb" / "validate_mvp.py"
NON_PRODUCTION = (
    "This report covers MVP KB draft readiness only. It does not claim production signoff, "
    "production snapshot readiness, or production runtime projection readiness."
)


class ValidateMvpTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.source_bundle = "src-demo"
        self.kb_pack = "pack-demo"
        self.create_happy_fixture()

    def tearDown(self) -> None:
        self.tmp.cleanup()

    def run_validator(self) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [
                "python3",
                str(VALIDATOR),
                "--repo-root",
                str(self.root),
                "--source-bundle",
                self.source_bundle,
                "--kb-pack",
                self.kb_pack,
            ],
            text=True,
            capture_output=True,
            check=False,
        )

    def write(self, rel: str, text: str) -> None:
        path = self.root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")

    def create_happy_fixture(self) -> None:
        source_dir = self.root / "sources" / self.source_bundle
        kb_dir = self.root / "kb" / self.kb_pack
        (source_dir).mkdir(parents=True)
        (kb_dir / "rules").mkdir(parents=True)
        (kb_dir / "lookups").mkdir()
        (kb_dir / "helpers").mkdir()
        (kb_dir / "evidence").mkdir()
        (kb_dir / "review").mkdir()
        (kb_dir / "reports").mkdir()

        self.write(
            f"sources/{self.source_bundle}/source-manifest.yaml",
            f"""schema_version: source-bundle-v1
source_bundle:
  id: {self.source_bundle}
artifacts:
  claims: claims.jsonl
""",
        )
        self.write(
            f"sources/{self.source_bundle}/source-index.yaml",
            """schema_version: source-index-v1
items:
  - source_id: src-java
    source_type: java
    status: found
    locator:
      path: src/main/java/Demo.java
      line_range: [1, 4]
    permission_state: allowed
""",
        )
        self.write(
            f"sources/{self.source_bundle}/workflow-map.yaml",
            """schema_version: workflow-map-v1
entrypoints:
  grpc:
    status: found
classification:
  status: found
transformation_flow:
  - step_id: receive_grpc
    status: found
    source_refs: [src-java]
  - step_id: publish_solace
    status: found
    source_refs: [src-java]
""",
        )
        claim = {
            "schema_version": "source-claim-v1",
            "claim_id": "claim-supported",
            "claim_type": "field_mapping",
            "status": "supported",
            "subject": "target.demo",
            "assertion": "Demo claim.",
            "source_anchors": [
                {
                    "source_id": "src-java",
                    "path": "src/main/java/Demo.java",
                    "line_range": [1, 4],
                    "anchor_type": "code_path",
                }
            ],
            "evidence_type": "code_path",
            "extraction_method": "manual_source_read",
            "confidence": "high",
            "limits": [],
        }
        self.write(f"sources/{self.source_bundle}/claims.jsonl", json.dumps(claim) + "\n")
        self.write(
            f"sources/{self.source_bundle}/unresolved-questions.yaml",
            """schema_version: unresolved-questions-v1
questions: []
""",
        )

        self.write(
            f"kb/{self.kb_pack}/metadata.yaml",
            f"""schema_version: kb-pack-v1
pack:
  id: {self.kb_pack}
source_bundle:
  id: {self.source_bundle}
indexes:
  evidence_index: evidence/evidence-index.yaml
  review_index: review/review-index.yaml
  extraction_report: reports/extraction-report.md
  review_checklist: reports/review-checklist.md
  closure_check: reports/closure-check.md
  completion_report: reports/completion-report.md
""",
        )
        self.write(
            f"kb/{self.kb_pack}/README.md",
            "KB draft fixture.\n",
        )
        self.write(
            f"kb/{self.kb_pack}/rules/rule-demo.yaml",
            """schema_version: kb-object-v1
id: rule-demo
object_type: rule
status: candidate
signoff_status: unsigned
revision: 1
scope: {}
claim_refs:
  - claim-supported
claim_status_used:
  - supported
source_anchors:
  - source_id: src-java
evidence_refs:
  - ev-supported
dependencies:
  lookups: []
  helpers: []
  rules: []
warnings: []
""",
        )
        self.write(
            f"kb/{self.kb_pack}/evidence/evidence-index.yaml",
            """schema_version: evidence-index-v1
evidence:
  - evidence_id: ev-supported
    claim_refs: [claim-supported]
""",
        )
        self.write(
            f"kb/{self.kb_pack}/review/review-index.yaml",
            """schema_version: review-index-v1
decisions: []
findings: []
summary:
  blocking_count: 0
  readiness: ready_for_kb_draft_mvp_completion
""",
        )
        self.write(
            f"kb/{self.kb_pack}/review/ask-user-questions.json",
            json.dumps({"schema_version": "ask-user-questions-v1", "blocking_count": 0, "questions": []}) + "\n",
        )
        for report in ["extraction-report.md", "review-checklist.md", "closure-check.md"]:
            self.write(f"kb/{self.kb_pack}/reports/{report}", f"# {report}\n")
        self.write(
            f"kb/{self.kb_pack}/reports/completion-report.md",
            f"""# Completion

- Readiness: ready_for_kb_draft_mvp_completion
- Non-production statement: {NON_PRODUCTION}
""",
        )

    def test_happy_path_passes(self) -> None:
        result = self.run_validator()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("Result: PASS", result.stdout)

    def test_missing_claims_fails(self) -> None:
        (self.root / "sources" / self.source_bundle / "claims.jsonl").unlink()
        result = self.run_validator()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("claims.jsonl", result.stdout)

    def test_bad_jsonl_fails(self) -> None:
        self.write(f"sources/{self.source_bundle}/claims.jsonl", "{bad json}\n")
        result = self.run_validator()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("invalid JSONL", result.stdout)

    def test_kb_object_with_inferred_claim_ref_fails(self) -> None:
        claim = {
            "schema_version": "source-claim-v1",
            "claim_id": "claim-inferred",
            "claim_type": "field_mapping",
            "status": "inferred",
            "subject": "target.demo",
            "assertion": "Inferred claim.",
            "source_anchors": [{"source_id": "src-java"}],
            "evidence_type": "code_path",
            "extraction_method": "manual_source_read",
            "confidence": "low",
            "limits": [],
        }
        self.write(f"sources/{self.source_bundle}/claims.jsonl", json.dumps(claim) + "\n")
        rule_path = self.root / "kb" / self.kb_pack / "rules" / "rule-demo.yaml"
        rule_path.write_text(
            rule_path.read_text(encoding="utf-8")
            .replace("claim-supported", "claim-inferred")
            .replace("- supported", "- inferred"),
            encoding="utf-8",
        )
        result = self.run_validator()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("disallowed", result.stdout)

    def test_missing_claim_ref_fails(self) -> None:
        rule_path = self.root / "kb" / self.kb_pack / "rules" / "rule-demo.yaml"
        text = rule_path.read_text(encoding="utf-8").replace("claim_refs:\n  - claim-supported\n", "claim_refs: []\n")
        rule_path.write_text(text, encoding="utf-8")
        result = self.run_validator()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("claim_refs", result.stdout)

    def test_unresolved_source_id_fails(self) -> None:
        claim_path = self.root / "sources" / self.source_bundle / "claims.jsonl"
        claim = json.loads(claim_path.read_text(encoding="utf-8"))
        claim["source_anchors"][0]["source_id"] = "missing-source"
        claim_path.write_text(json.dumps(claim) + "\n", encoding="utf-8")
        result = self.run_validator()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("does not resolve", result.stdout)

    def test_completion_report_without_non_production_statement_fails(self) -> None:
        self.write(
            f"kb/{self.kb_pack}/reports/completion-report.md",
            "# Completion\n\n- Readiness: ready_for_kb_draft_mvp_completion\n",
        )
        result = self.run_validator()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("non-production", result.stdout)


if __name__ == "__main__":
    unittest.main()
