package dev.codeduel.battles;

import static org.assertj.core.api.Assertions.assertThat;

import dev.codeduel.submissions.Verdict;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutcomeResolverTest {
  private static final UUID FIRST = UUID.randomUUID();
  private static final UUID SECOND = UUID.randomUUID();
  private static final Instant DEADLINE = Instant.parse("2026-10-20T20:00:00Z");

  private static OutcomeResolver.Candidate submission(long sequence, UUID user, Verdict verdict) {
    return new OutcomeResolver.Candidate(sequence, user, verdict);
  }

  private OutcomeResolver.Decision resolve(Instant now, OutcomeResolver.Candidate... candidates) {
    return OutcomeResolver.resolve(now, DEADLINE, List.of(candidates), null, null);
  }

  @Test
  void laterAcceptedMustWaitForEarlierPendingSubmission() {
    assertThat(
            resolve(
                    DEADLINE.minusSeconds(1),
                    submission(1, FIRST, Verdict.PENDING),
                    submission(2, SECOND, Verdict.ACCEPTED))
                .isPending())
        .isTrue();
  }

  @Test
  void receiptOrderWinsEvenWhenCandidatesAreReturnedInReverseOrder() {
    assertThat(
            resolve(
                    DEADLINE,
                    submission(2, SECOND, Verdict.ACCEPTED),
                    submission(1, FIRST, Verdict.ACCEPTED))
                .winnerId())
        .isEqualTo(FIRST);
  }

  @Test
  void earlierWrongAnswerDoesNotBlockLaterAccepted() {
    assertThat(
            resolve(
                    DEADLINE.minusSeconds(1),
                    submission(1, FIRST, Verdict.WRONG_ANSWER),
                    submission(2, SECOND, Verdict.ACCEPTED))
                .winnerId())
        .isEqualTo(SECOND);
  }

  @Test
  void laterPendingCannotChangeWinnerBeforeDeadline() {
    assertThat(
            resolve(
                    DEADLINE.minusSeconds(1),
                    submission(1, FIRST, Verdict.ACCEPTED),
                    submission(2, SECOND, Verdict.PENDING))
                .winnerId())
        .isEqualTo(FIRST);
  }

  @Test
  void deadlineRequiresEveryInTimeSubmissionToHaveFinalFeedback() {
    assertThat(
            resolve(
                    DEADLINE,
                    submission(1, FIRST, Verdict.ACCEPTED),
                    submission(2, SECOND, Verdict.PENDING))
                .isPending())
        .isTrue();
  }

  @Test
  void pendingAtDeadlineCanStillWinAfterDeadline() {
    assertThat(resolve(DEADLINE.plusSeconds(20), submission(1, FIRST, Verdict.ACCEPTED)).winnerId())
        .isEqualTo(FIRST);
  }

  @Test
  void noSolveAtExactDeadlineIsDraw() {
    var decision = resolve(DEADLINE, submission(1, FIRST, Verdict.WRONG_ANSWER));
    assertThat(decision.status()).isEqualTo(BattleStatus.FINISHED);
    assertThat(decision.winnerId()).isNull();
    assertThat(decision.reason()).isEqualTo("DEADLINE_DRAW");
  }

  @Test
  void noSolveBeforeDeadlineKeepsGameOpen() {
    assertThat(resolve(DEADLINE.minusNanos(1)).isPending()).isTrue();
  }

  @Test
  void relevantInfrastructureFailureCancelsInsteadOfAwardingOpponentWin() {
    assertThat(
            resolve(
                    DEADLINE.minusSeconds(1),
                    submission(1, FIRST, Verdict.JUDGE_ERROR),
                    submission(2, SECOND, Verdict.ACCEPTED))
                .status())
        .isEqualTo(BattleStatus.CANCELLED);
  }

  @Test
  void irrelevantLaterFailureDoesNotEraseAnEstablishedWinner() {
    assertThat(
            resolve(
                    DEADLINE.minusSeconds(1),
                    submission(1, FIRST, Verdict.ACCEPTED),
                    submission(2, SECOND, Verdict.JUDGE_ERROR))
                .winnerId())
        .isEqualTo(FIRST);
  }

  @Test
  void explicitForfeitIsIrrevocableEvenWhenOwnEarlierCodeIsPending() {
    var decision =
        OutcomeResolver.resolve(
            DEADLINE.minusSeconds(1),
            DEADLINE,
            List.of(submission(1, FIRST, Verdict.PENDING)),
            FIRST,
            SECOND);
    assertThat(decision.winnerId()).isEqualTo(SECOND);
    assertThat(decision.reason()).isEqualTo("FORFEIT");
  }

  @Test
  void laterInfrastructureFailureCannotEraseProvenWinnerAfterDeadline() {
    assertThat(
            resolve(
                    DEADLINE,
                    submission(1, FIRST, Verdict.ACCEPTED),
                    submission(2, SECOND, Verdict.JUDGE_ERROR))
                .winnerId())
        .isEqualTo(FIRST);
  }
}
