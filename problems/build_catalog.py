"""Build versioned seed content deterministically. Never pass user programs to this script."""
import json
import random
from pathlib import Path

ROOT = Path(__file__).resolve().parent
DESTINATION = ROOT.parent / "CodeDuelBackend/src/main/resources/problems/catalog.json"
RANDOM = random.Random(20261020)

STARTER = '''import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        // Read from standard input and write your answer to standard output.
    }
}
'''


def bracket_oracle(text):
    stack = []
    for char in text:
        if char in "([{":
            stack.append(char)
        elif not stack or stack.pop() != {')': '(', ']': '[', '}': '{'}[char]:
            return "BROKEN"
    return "BROKEN" if stack else "BALANCED"


def window_oracle(text):
    best = 0
    for start in range(len(text)):
        seen = set()
        for char in text[start:start + 37]:
            if char in seen:
                break
            seen.add(char)
        best = max(best, len(seen))
    return str(best)


def case(stdin, stdout, public=False):
    return dict(input=stdin, output=stdout + "\n", public=public)


def problem(slug, title, difficulty, tags, statement, constraints, input_format, output_format, cases):
    return dict(slug=slug, title=title, difficulty=difficulty, tags=tags, version=1,
                statement=statement, constraints=constraints, inputFormat=input_format,
                outputFormat=output_format, starterCode=STARTER,
                referenceSource=(ROOT / slug / "Main.java").read_text(encoding="utf-8"),
                timeLimitMs=2000, memoryLimitMb=256, durationSeconds=900, cases=cases)


brackets = ["{[()]}()", "([)]", "", ")", "(", "()[]{}", "{{[[]]}}", "{" * 100000 + "}" * 100000]
brackets += [''.join(RANDOM.choices('()[]{}', k=RANDOM.randint(1, 200))) for _ in range(12)]
signals = ["abcaefb", "zzzz", "", "a", "abba", "abcabcbb", "0123456789abcdefghijklmnopqrstuvwxyz", "a" * 200000]
signals += [''.join(RANDOM.choices('abcdef012345', k=RANDOM.randint(1, 120))) for _ in range(12)]
cargo = [([2, 4, 2, 6, 0], 6), ([3, 3, 3, 3], 6), ([], 0), ([-2, 0, 2, 4], 2), ([1000000000, 1000000000], 2000000000)]
cargo += [([RANDOM.randint(-30, 30) for _ in range(RANDOM.randint(1, 100))], RANDOM.randint(-20, 20)) for _ in range(12)]
cargo_cases = []
for index, (weights, target) in enumerate(cargo):
    expected = sum(weights[a] + weights[b] == target for a in range(len(weights)) for b in range(a + 1, len(weights)))
    cargo_cases.append(case(f"{len(weights)} {target}\n{' '.join(map(str, weights))}\n", str(expected), index < 2))
cargo_cases.append(case("200000 0\n" + "0 " * 200000 + "\n", "19999900000"))

catalog = [
    problem("balanced-relay", "Balanced Relay", "EASY", ["Stacks", "Strings"],
            "A relay station receives a signal made entirely of bracket symbols. Every opening bracket must be closed by the same bracket type, and inner groups must close before outer groups. Decide whether the entire signal is balanced. An empty signal is balanced.",
            "0 ≤ signal length ≤ 200,000. Every symbol is one of ( ) [ ] { }.",
            "One line containing the signal. The line may be empty.",
            "Print BALANCED if all groups close correctly; otherwise print BROKEN.",
            [case(s + "\n", bracket_oracle(s), i < 2) for i, s in enumerate(brackets)]),
    problem("signal-window", "Signal Window", "MEDIUM", ["Sliding window", "Strings"],
            "Your receiver keeps a contiguous window of an incoming signal. A window is clear when no symbol occurs twice inside it. Find the greatest length of any clear window. You may discard symbols only from the ends of a window; you cannot rearrange the signal.",
            "0 ≤ signal length ≤ 200,000. Symbols are lowercase English letters a–z or digits 0–9.",
            "One line containing the signal. The line may be empty.",
            "Print the maximum clear-window length as one integer. For an empty signal, print 0.",
            [case(s + "\n", window_oracle(s), i < 2) for i, s in enumerate(signals)]),
    problem("cargo-pairs", "Cargo Pairs", "MEDIUM", ["Hash maps", "Arrays"],
            "A cargo dispatcher has n numbered crates, each with an integer balance weight. Two different crates may share a carrier when their weights add to target. Count every unordered pair of crate positions that can share a carrier. A crate can appear in several candidate pairs; you are counting possibilities, not assigning carriers. Equal weights at different positions still represent different crates.",
            "0 ≤ n ≤ 200,000. −1,000,000,000 ≤ weight ≤ 1,000,000,000. −2,000,000,000 ≤ target ≤ 2,000,000,000. The answer may require a 64-bit integer.",
            "First line: n and target. Second line: n integer weights separated by spaces. When n is 0 the second line is empty.",
            "Print the number of unordered pairs (i, j) with i < j and weight[i] + weight[j] = target.",
            cargo_cases),
]

from extended_catalog import build_extra
from common_catalog import build_common
from classic_catalog import build_classics
from functions_catalog import build_functions

catalog.extend(build_extra(problem, case))
catalog.extend(build_common(problem, case))
catalog.extend(build_classics(problem, case))

DESTINATION.parent.mkdir(parents=True, exist_ok=True)
DESTINATION.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
functions = build_functions(catalog)
DESTINATION.with_name('functions.json').write_text(
    '[\n' + ',\n'.join(json.dumps(problem, ensure_ascii=False, separators=(',', ':')) for problem in functions) + '\n]\n',
    encoding='utf-8', newline='\n')
print(f"Built {len(functions)} function problems with {sum(len(p['cases']) for p in functions)} cases; kept the original versions.")
