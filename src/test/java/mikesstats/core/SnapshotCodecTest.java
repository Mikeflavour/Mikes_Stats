package mikesstats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

import mikesstats.core.StatsSnapshot.DeathEntry;
import mikesstats.core.StatsSnapshot.PlayerEntry;
import org.junit.jupiter.api.Test;

class SnapshotCodecTest {
    @Test
    void roundTrip_keepsAllFields() {
        UUID ben = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID juergen = UUID.fromString("00000000-0000-0000-0000-000000000002");
        PlayerEntry online = new PlayerEntry(ben, "Ben", true, null, 31, 42, 12_345, 0x3FA9F5,
                Map.of("Jürgen", 3), Map.of("minecraft:custom", Map.of("minecraft:deaths", 4)));
        PlayerEntry offline = new PlayerEntry(juergen, "Jürgen", false, LocalDate.of(2026, 10, 1), 7, -1, -1, 0xFF0000,
                Map.of(), Map.of("minecraft:mined", Map.of("minecraft:stone", 900)));
        StatsSnapshot s = new StatsSnapshot(LocalDate.of(2026, 4, 27), 179, 2140, List.of(online, offline),
                List.of(new DeathEntry(LocalDate.of(2026, 10, 2), juergen, 7, "von Ben getötet")));

        assertEquals(s, SnapshotCodec.decode(SnapshotCodec.encode(s)));
    }

    @Test
    void bigServer_staysUnderOneMegabyte() {
        List<PlayerEntry> players = new ArrayList<>();
        for (int p = 0; p < 30; p++) {
            Map<String, Integer> mined = new HashMap<>();
            for (int i = 0; i < 2000; i++) mined.put("minecraft:block_" + i, p * 1000 + i);
            players.add(new PlayerEntry(UUID.randomUUID(), "Spieler" + p, false, LocalDate.of(2026, 5, 1), 30, 30, 1000,
                    0x123456, Map.of(), Map.of("minecraft:mined", mined)));
        }
        StatsSnapshot s = new StatsSnapshot(LocalDate.of(2026, 4, 27), 179, 2140, players, List.of());

        assertTrue(SnapshotCodec.encode(s).length < 1_000_000);
    }
}
