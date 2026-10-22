"""Compile ONLY repository-owned reference programs and check every catalog case."""
import json
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
catalog = json.loads((ROOT.parent / "CodeDuelBackend/src/main/resources/problems/catalog.json").read_text())
total = 0
for problem in catalog:
    reference = ROOT / problem['slug'] / "Main.java"
    assert problem['referenceSource'] == reference.read_text(), "Regenerate catalog after reference edits"
    with tempfile.TemporaryDirectory(prefix="codeduel-reference-") as directory:
        subprocess.run(["javac", "--release", "21", "-d", directory, str(reference)], check=True)
        for case in problem['cases']:
            result = subprocess.run(["java", "-cp", directory, "Main"], input=case['input'], capture_output=True, text=True, timeout=5, check=True)
            assert result.stdout.split() == case['output'].split(), f"{problem['slug']}: reference mismatch"
            total += 1
print(f"Verified {total} cases against trusted Java reference programs.")
