# Original problem library

Fifteen original Java 21 stdin/stdout challenges cover brackets, strings, hash maps, pairs, intervals, heaps, BFS, prefix sums, sorting, dynamic programming, LIS, binary search, connected components and counting paths.

`build_catalog.py` combines the original batch and `extended_catalog.py` into the private backend resource. It uses a deterministic random seed and independent Python oracles, including brute-force checks on small cases plus adversarial/maximum cases. Each problem has public examples, private tests, a trusted Java reference and explicit limits. No reference/private data is copied into frontend assets.

From the shared repository root:

```sh
python3 problems/build_catalog.py
python3 problems/verify_catalog.py
```

The verifier compiles only repository-owned references with Java 21 and compares **362 cases**. Real sandbox tests separately run each problem's largest input under its configured execution limits.

Published content is immutable, including source checksums. Preserve an already-deployed version and author a new version for changes. The admin workflow clones drafts, freezes review content and requires an accepted real-executor reference check before publication. Avoid reformatting existing deployed v1 reference source as an incidental change; it is part of that version's checksum.
