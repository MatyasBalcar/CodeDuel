# Problem library

The library has 45 Java 21 stdin/stdout challenges: 16 easy, 22 medium, and 7 hard.
It includes the original 15 problems and 30 familiar algorithm exercises with statements,
examples, and tests written for CodeDuel.

The new problems cover:

| Difficulty | Problems |
| --- | --- |
| Easy | Two Sum, Palindrome Check, Valid Anagram, Best Time to Buy and Sell Stock, Contains Duplicate, Missing Number, Move Zeroes, Single Number, Majority Element, Range Sum Query, GCD and LCM, Merge Sorted Arrays |
| Medium | Maximum Subarray, Next Permutation, Container With Most Water, Daily Temperatures, Search in Rotated Sorted Array, Jump Game, House Robber, Word Break, Edit Distance, Longest Common Subsequence, Shortest Path, Course Schedule |
| Hard | Trapping Rain Water, Sliding Window Maximum, Largest Rectangle in Histogram, Longest Valid Parentheses, Minimum Spanning Tree, 0/1 Knapsack |

New easy problems have 10-minute duels, medium problems have 15 minutes, and hard problems
have 20 minutes. Each has two public examples, private edge and random cases, and a large
input case. Tests cover empty inputs where allowed, duplicates, negative values,
unreachable graphs, cycles, and answers that need 64-bit integers.

`build_catalog.py` combines the original batch, `extended_catalog.py`, and
`common_catalog.py` into the private backend resource. Each batch uses a fixed random
seed. The Python answers use brute force or a separate algorithm from the Java reference
where practical. Reference solutions and private cases stay in the backend.

With JDK 21 or newer on PATH, run from the shared project root:

```sh
python problems/build_catalog.py
python problems/verify_catalog.py
```

The verifier compiles only repository-owned references with `--release 21` and compares
all 930 cases. The real sandbox suite separately checks each problem's largest input
under its configured execution limits.

Published content is immutable, including source checksums. Preserve an already-deployed version and author a new version for changes. The admin workflow clones drafts, freezes review content and requires an accepted real-executor reference check before publication. Avoid reformatting existing deployed v1 reference source as an incidental change; it is part of that version's checksum.

The added problems use new slugs. Existing problem versions, submissions, and battle
history are preserved. Rebuild and restart the app to seed them into an existing database:

```sh
docker compose up -d --build api frontend
```
