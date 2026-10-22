package dev.codeduel.battles;

public enum BattleStatus {
  WAITING,
  READY_CHECK,
  IN_PROGRESS,
  JUDGING,
  FINISHED,
  CANCELLED;

  public boolean terminal() {
    return this == FINISHED || this == CANCELLED;
  }
}
