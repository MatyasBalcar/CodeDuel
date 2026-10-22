package dev.codeduel.battles;

import dev.codeduel.submissions.Verdict;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Pure receipt-order policy. Sequence is assigned under the battle lock, so ties cannot exist. */
public final class OutcomeResolver {
  private OutcomeResolver() {}

  public static Decision resolve(
      Instant now, Instant deadline, List<Candidate> official, UUID forfeiting, UUID opponent) {
    if (forfeiting != null) return new Decision(BattleStatus.FINISHED, opponent, "FORFEIT");
    var accepted =
        official.stream()
            .filter(s -> s.verdict() == Verdict.ACCEPTED)
            .min(Comparator.comparingLong(Candidate::sequence));
    boolean expired = !now.isBefore(deadline);
    // Later failures cannot change an already proven earliest accepted receipt.
    var relevant =
        official.stream()
            .filter(s -> accepted.isEmpty() || s.sequence() < accepted.get().sequence())
            .toList();
    // After the deadline, wait for accurate feedback on every in-time receipt too.
    var feedbackBarrier = expired ? official : relevant;
    if (feedbackBarrier.stream().anyMatch(s -> !s.verdict().terminal())) return Decision.pending();
    if (relevant.stream().anyMatch(s -> s.verdict() == Verdict.JUDGE_ERROR)) {
      return new Decision(BattleStatus.CANCELLED, null, "JUDGE_UNAVAILABLE");
    }
    if (accepted.isPresent())
      return new Decision(BattleStatus.FINISHED, accepted.get().userId(), "FIRST_ACCEPTED");
    if (!expired) return Decision.pending();
    return new Decision(BattleStatus.FINISHED, null, "DEADLINE_DRAW");
  }

  public record Candidate(long sequence, UUID userId, Verdict verdict) {}

  public record Decision(BattleStatus status, UUID winnerId, String reason) {
    static Decision pending() {
      return new Decision(null, null, null);
    }

    public boolean isPending() {
      return status == null;
    }
  }
}
