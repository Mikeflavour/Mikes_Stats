package mikesstats.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** What the mod tracks itself per player (world/mikes_stats/&lt;uuid&gt;.json). Only counts since install. */
public final class ModData {
    public int maxLevel = -1;
    public long xpTotal = -1;
    public LocalDate lastSeen;
    public List<DeathRecord> deaths = new ArrayList<>();
    public Map<UUID, Integer> pvpVictims = new HashMap<>();
    /** players this viewer hid with the "Player" filter */
    public Set<UUID> hidden = new HashSet<>();

    public record DeathRecord(LocalDate date, int level, String cause) {
    }

    public void addXp(int gained) {
        if (gained <= 0) return;
        xpTotal = Math.max(xpTotal, 0) + gained;
    }

    public void seeLevel(int level) {
        maxLevel = Math.max(maxLevel, level);
    }
}
