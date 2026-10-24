package mikesstats.core;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Everything a viewer needs, built by the server and sent to clients / shown in /stats. */
public record StatsSnapshot(LocalDate serverStart, long daysSinceStart, long ingameDay,
                            List<PlayerEntry> players, List<DeathEntry> deaths) {

    /**
     * @param lastSeen null while online
     * @param maxLevel -1 = unknown (no mod data yet)
     * @param xpTotal -1 = unknown
     * @param waypointColor 0xRRGGBB
     * @param pvpVictims victim name -> kills since mod install
     * @param stats category id -> stat id -> value (vanilla stats file)
     */
    public record PlayerEntry(UUID uuid, String name, boolean online, LocalDate lastSeen, int level, int maxLevel,
                              long xpTotal, int waypointColor, Map<String, Integer> pvpVictims,
                              Map<String, Map<String, Integer>> stats) {
    }

    /** @param cause ready-made text, e.g. "von Ben getötet" */
    public record DeathEntry(LocalDate date, UUID player, int level, String cause) {
    }
}
