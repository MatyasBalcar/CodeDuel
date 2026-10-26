"""Compile ONLY repository-owned reference programs and check every catalog case."""
import json
import argparse
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
resources = ROOT.parent / 'CodeDuelBackend/src/main/resources'
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--functions-only', action='store_true')
options = parser.parse_args()
catalog = [] if options.functions_only else json.loads((resources / 'problems/catalog.json').read_text(encoding='utf-8'))
catalog += json.loads((resources / 'problems/functions.json').read_text(encoding='utf-8'))
template = (resources / 'judge/FunctionRunner.java').read_text(encoding='utf-8')


def same_value(actual, expected):
    if isinstance(expected, list):
        return isinstance(actual, list) and len(actual) == len(expected) and all(same_value(a, e) for a, e in zip(actual, expected))
    if isinstance(expected, bool):
        return type(actual) is bool and actual == expected
    return type(actual) is type(expected) and actual == expected or (
        type(actual) in (int, float) and type(expected) in (int, float) and actual == expected)


total = 0
for problem in catalog:
    function = problem.get('function')
    reference = ROOT / problem['slug'] / ('Solution.java' if function else 'Main.java')
    assert problem['referenceSource'] == reference.read_text(encoding="utf-8"), "Regenerate catalog after reference edits"
    with tempfile.TemporaryDirectory(prefix="codeduel-reference-") as directory:
        main = reference
        if function:
            arguments = ', '.join(f'({p["type"]}) convert(arguments.get("{p["name"]}"), "{p["type"]}")' for p in function['parameters'])
            main = Path(directory) / 'Main.java'
            main.write_text(template.replace('__RETURN_TYPE__', function['returnType']).replace('__ARGUMENTS__', arguments), encoding='utf-8')
            (Path(directory) / 'Solution.java').write_text(problem['referenceSource'], encoding='utf-8')
        subprocess.run(["javac", "--release", "21", "-d", directory, str(main)], cwd=directory, check=True)
        for index, case in enumerate(problem['cases'], start=1):
            data = json.dumps(case['arguments'], ensure_ascii=False) if function else case['input']
            result = subprocess.run(["java", "-cp", directory, "Main"], input=data, capture_output=True, text=True, encoding='utf-8', timeout=5, check=True)
            matches = same_value(json.loads(result.stdout), case['expected']) if function else result.stdout.split() == case['output'].split()
            assert matches, f"{problem['slug']} v{problem['version']}: case {index} reference mismatch"
            total += 1
    print(f"Verified {problem['slug']} v{problem['version']} ({len(problem['cases'])} cases).", flush=True)
print(f"Verified {total} cases against trusted Java reference programs.")
