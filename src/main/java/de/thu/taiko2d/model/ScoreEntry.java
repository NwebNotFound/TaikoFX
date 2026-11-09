package de.thu.taiko2d.model;

/**
 * One finished play on the local leaderboard. AI-generated 2026-11-09; see docs/AI_USAGE.md.
 *
 * @param score        total score including the fever bonus
 * @param accuracy     accuracy in percent
 * @param playedAtMs   when the play finished, epoch milliseconds
 */
public record ScoreEntry(long score, double accuracy, int maxCombo, int perfect, int good, int miss,
                         String grade, long playedAtMs) {
    public boolean fullCombo() { return miss == 0; }
}
