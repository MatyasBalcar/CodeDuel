# Problem library

The library has 100 Java 21 problems: 36 easy, 47 medium, and 17 hard. They include
familiar interview and contest exercises alongside the original CodeDuel problems.

Every current problem uses a function. For Two Sum, the signature is:

```java
public int[] solve(int[] nums, long target)
```

An example gives `nums = [2,7,11,15]`, `target = 9`, and the expected return value
`[1,2]`. Implement the method in `public class Solution`. You do not need a `main`
method, input parsing, or printing. Arrays and booleans are actual values in the test
data. Array order and whitespace inside returned strings matter. Debug prints do
not affect the answer.

| Difficulty | Examples |
| --- | --- |
| Easy | Two Sum, Valid Anagram, Fizz Buzz, Roman to Integer, Sorted Squares, Plus One, Transpose Matrix |
| Medium | Three Sum, Product Except Self, Word Break, Coin Change Combinations, Word Search, Task Scheduler, Spiral Matrix |
| Hard | Median of Two Sorted Arrays, Minimum Window Substring, Burst Balloons, Count Smaller After Self, Regular Expression Matching |

Most easy problems allow 10-minute duels, medium problems 15 minutes, and hard
problems 20 minutes. Each has two public examples and private edge, random, and
large cases. Together, the current versions have 1,827 cases.

`build_catalog.py` combines the original batch, `extended_catalog.py`,
`common_catalog.py`, and `classic_catalog.py`. `functions_catalog.py` gives every
problem its named parameters and return type, and converts its cases to structured
arguments. Generators use fixed random seeds and independent Python answers where
practical. References and private cases stay in the backend.

With JDK 21 and Python on PATH, run from the project root:

```sh
python problems/build_catalog.py
python problems/verify_catalog.py
```

The verifier compiles only repository-owned references with `--release 21`. Function
solutions use the same driver template as the real judge. To check just the current
function versions, add `--functions-only` to the verifier. On the Windows/WSL setup,
`scripts/check.ps1 -Wsl -Only problems` handles the Java tools for you.

The original stdin versions stay in `catalog.json` and their `Main.java` files;
the function versions are published as v2 in `functions.json`, using `Solution.java`.
The full verifier checks both sets: 3,654 cases. Keeping v1 unchanged preserves
checksums, old submissions, and matches. The library, new practice attempts, and
new matches select the latest published version.

Rebuild and restart the app to apply the migration and seed the current versions:

```sh
docker compose up -d --build api frontend
```

If you use the separate real worker, rebuild and restart it too. The real sandbox
suite checks every version's largest case under its configured execution limits.
