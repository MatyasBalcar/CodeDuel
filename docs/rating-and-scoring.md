# Rating and scoring

## Winner

Only official submissions accepted by the server while `starts_at ≤ now < ends_at` qualify. Public example runs never affect the outcome. Receipt time is assigned after the battle lock is obtained; a monotonically increasing receive sequence defines a total, deterministic server-acceptance order even when timestamps coincide.

The first **accepted** official receipt wins. A later accepted program must wait for earlier pending receipts. Before the deadline, later pending receipts cannot defeat an already accepted earlier receipt. At/after the deadline, all on-time receipts receive final feedback before finalization. Disconnects do not pause the game or count as forfeits.

No accepted submission at the deadline yields a draw. An explicit in-time forfeit is an immediate, irrevocable loss and overrides pending own code; a late forfeit cannot replace the expired game’s submission outcome. A relevant unrecoverable judge failure cancels without Elo. A failed later job cannot erase an already finalized earlier win.

`OutcomeResolver` is pure Java and has no Spring/database dependencies. Integration tests verify that the transaction ordering makes its inputs safe under concurrency.

## Elo

Players start at 1200. For player A against B:

```text
expectedA = 1 / (1 + 10^((ratingB - ratingA) / 400))
deltaA = K_A × (scoreA - expectedA)
```

Scores are win `1`, draw `0.5`, loss `0`. K is 40 when pre-match ranked game count is below 10, otherwise 24. Both changes use pre-match ratings. Different K values need not yield a zero-sum change.

Internal ratings/deltas are `numeric(14,6)`; calculated deltas use `HALF_UP` at six places. The browser uses one explicit, symmetric integer `HALF_UP` function for every public rating/delta, including negative values and normalized negative zero. History retains the exact six-decimal values, so displayed rounded before/after differences can differ by one from the rounded stored delta. The product currently applies no rating floor.

Only ranked `FINISHED` battles receive history events. Private/cancelled games do not. The battle row lock and unique history keys prevent double application; finalization is one database transaction and is safe to retry.

Changing a future rating floor is a versioned product decision, not a hidden adjustment to history.
