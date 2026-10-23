package mikesstats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModDataStoreTest {
    static final UUID BEN = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID TIM = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @TempDir
    Path dir;

    @Test
    void saveAndLoad_roundTrip() {
        ModDataStore store = new ModDataStore(dir);
        ModData d = store.get(BEN);
        d.seeLevel(42);
        d.addXp(500);
        d.lastSeen = LocalDate.of(2026, 10, 23);
        d.deaths.add(new ModData.DeathRecord(LocalDate.of(2026, 10, 2), 31, "von Tim getötet"));
        d.pvpVictims.put(TIM, 3);
        d.hidden.add(TIM);
        store.save(BEN);

        ModData loaded = new ModDataStore(dir).get(BEN);
        assertEquals(42, loaded.maxLevel);
        assertEquals(500, loaded.xpTotal);
        assertEquals(LocalDate.of(2026, 10, 23), loaded.lastSeen);
        assertEquals(d.deaths, loaded.deaths);
        assertEquals(3, loaded.pvpVictims.get(TIM));
        assertTrue(loaded.hidden.contains(TIM));
        assertEquals(1, new ModDataStore(dir).all().size());
    }

    @Test
    void corruptFile_isBackedUpAndStartsEmpty() throws IOException {
        ModDataStore first = new ModDataStore(dir);
        first.get(TIM).seeLevel(7);
        first.save(TIM);
        String timBefore = Files.readString(dir.resolve(TIM + ".json"));
        Files.writeString(dir.resolve(BEN + ".json"), "nicht json {{{");

        ModData ben = new ModDataStore(dir).get(BEN);

        assertEquals(-1, ben.maxLevel);
        assertTrue(ben.deaths.isEmpty());
        assertEquals("nicht json {{{", Files.readString(dir.resolve(BEN + ".json.bak")));
        assertEquals(timBefore, Files.readString(dir.resolve(TIM + ".json")));
    }

    @Test
    void addXp_ignoresNegative() {
        ModData d = new ModData();
        d.addXp(-5);
        assertEquals(-1, d.xpTotal);
        d.addXp(10);
        d.addXp(-3);
        assertEquals(10, d.xpTotal);
    }

    @Test
    void seeLevel_keepsMaximum() {
        ModData d = new ModData();
        d.seeLevel(30);
        d.seeLevel(12);
        assertEquals(30, d.maxLevel);
    }
}
