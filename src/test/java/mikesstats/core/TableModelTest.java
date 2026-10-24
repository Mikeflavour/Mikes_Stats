package mikesstats.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import mikesstats.core.StatsSnapshot.DeathEntry;
import mikesstats.core.StatsSnapshot.PlayerEntry;
import mikesstats.core.TableModel.PivotRow;
import mikesstats.core.TableModel.PivotTable;
import org.junit.jupiter.api.Test;

class TableModelTest {
    static final Function<String, String> ID = Function.identity();

    static PlayerEntry player(String name, int level, Map<String, Map<String, Integer>> stats) {
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes());
        return new PlayerEntry(uuid, name, false, LocalDate.of(2026, 10, 1), level, -1, -1, 0, Map.of(), stats);
    }

    static StatsSnapshot snapshot(PlayerEntry... players) {
        return new StatsSnapshot(LocalDate.of(2026, 4, 27), 179, 2140, List.of(players), List.of());
    }

    static List<String> names(List<PlayerEntry> players) {
        return players.stream().map(PlayerEntry::name).toList();
    }

    static List<String> ids(PivotTable t) {
        return t.rows().stream().map(PivotRow::statId).toList();
    }

    @Test
    void sort_textAscendingFirst_thenFlips() {
        StatsSnapshot s = snapshot(player("Tim", 1, Map.of()), player("Anna", 2, Map.of()), player("Ben", 3, Map.of()));
        SortState sort = new SortState("name", false);
        assertEquals(List.of("Anna", "Ben", "Tim"), names(TableModel.overview(s, Set.of(), sort)));
        assertEquals(List.of("Tim", "Ben", "Anna"), names(TableModel.overview(s, Set.of(), sort.click("name"))));
    }

    @Test
    void sort_numbersHighestFirst() {
        StatsSnapshot s = snapshot(player("Tim", 5, Map.of()), player("Anna", 30, Map.of()), player("Ben", 12, Map.of()));
        SortState sort = new SortState("level", false);
        assertEquals(List.of("Anna", "Ben", "Tim"), names(TableModel.overview(s, Set.of(), sort)));
        assertEquals(List.of("Tim", "Ben", "Anna"), names(TableModel.overview(s, Set.of(), sort.click("level"))));
    }

    @Test
    void overview_playerWithoutStatsFile_hasZeros() {
        PlayerEntry noFile = player("Neu", 0, Map.of());
        PlayerEntry old = player("Alt", 9, Map.of("minecraft:custom", Map.of("minecraft:deaths", 4)));
        List<PlayerEntry> rows = TableModel.overview(snapshot(noFile, old), Set.of(), new SortState("deaths", false));
        assertEquals(List.of("Alt", "Neu"), names(rows));
        assertEquals(0, TableModel.custom(noFile, "minecraft:deaths"));
    }

    @Test
    void pivot_dropsRowsWhereAllVisibleAreZero() {
        StatsSnapshot s = snapshot(
                player("Anna", 1, Map.of("minecraft:mined", Map.of("minecraft:stone", 5, "minecraft:dirt", 0))),
                player("Ben", 1, Map.of("minecraft:mined", Map.of("minecraft:dirt", 0))));
        PivotTable t = TableModel.pivot(s, Tab.BLOCKS, "minecraft:mined", Set.of(), id -> true, ID,
                new SortState("total", false));
        assertEquals(List.of("minecraft:stone"), ids(t));
    }

    @Test
    void pivot_hiddenPlayersExcludedFromColumnsAndTotal() {
        PlayerEntry anna = player("Anna", 1, Map.of("minecraft:mined", Map.of("minecraft:stone", 5, "minecraft:dirt", 1)));
        PlayerEntry ben = player("Ben", 1, Map.of("minecraft:mined", Map.of("minecraft:stone", 7, "minecraft:sand", 3)));
        PivotTable t = TableModel.pivot(snapshot(anna, ben), Tab.BLOCKS, "minecraft:mined", Set.of(ben.uuid()),
                id -> true, ID, new SortState("total", false));
        assertEquals(List.of("Anna"), names(t.columns()));
        assertEquals(List.of("minecraft:stone", "minecraft:dirt"), ids(t));
        assertEquals(5, t.rows().get(0).total());
        assertArrayEquals(new long[] {5}, t.rows().get(0).values());
    }

    @Test
    void pivot_allHidden_returnsEmptyTable() {
        PlayerEntry anna = player("Anna", 1, Map.of("minecraft:mined", Map.of("minecraft:stone", 5)));
        PivotTable t = TableModel.pivot(snapshot(anna), Tab.BLOCKS, "minecraft:mined", Set.of(anna.uuid()),
                id -> true, ID, new SortState("total", false));
        assertTrue(t.columns().isEmpty());
        assertTrue(t.rows().isEmpty());
    }

    @Test
    void pivot_timeSinceDeath_hasNoTotal() {
        PlayerEntry anna = player("Anna", 1, Map.of("minecraft:custom", Map.of("minecraft:time_since_death", 900)));
        PivotTable t = TableModel.pivot(snapshot(anna), Tab.GENERAL, "minecraft:custom", Set.of(), id -> true, ID,
                new SortState("total", false));
        assertEquals(-1, t.rows().get(0).total());
    }

    @Test
    void pivot_sortsByLabelAndPlayerColumn() {
        PlayerEntry anna = player("Anna", 1, Map.of("minecraft:mined", Map.of("minecraft:stone", 5, "minecraft:dirt", 9)));
        PlayerEntry ben = player("Ben", 1, Map.of("minecraft:mined", Map.of("minecraft:stone", 7)));
        Function<String, String> german = id -> id.equals("minecraft:stone") ? "Stein" : "Erde";
        StatsSnapshot s = snapshot(ben, anna);
        assertEquals(List.of("minecraft:dirt", "minecraft:stone"), ids(TableModel.pivot(s, Tab.BLOCKS,
                "minecraft:mined", Set.of(), id -> true, german, new SortState("name", false))));
        assertEquals(List.of("minecraft:stone", "minecraft:dirt"), ids(TableModel.pivot(s, Tab.BLOCKS,
                "minecraft:mined", Set.of(), id -> true, german, new SortState(ben.uuid().toString(), false))));
        assertEquals(List.of("Anna", "Ben"), names(TableModel.pivot(s, Tab.BLOCKS, "minecraft:mined", Set.of(),
                id -> true, german, new SortState("total", false)).columns()));
    }

    @Test
    void blocks_used_onlyBlockIds() {
        PlayerEntry anna = player("Anna", 1, Map.of("minecraft:used", Map.of("minecraft:dirt", 5, "minecraft:bread", 3)));
        PivotTable blocks = TableModel.pivot(snapshot(anna), Tab.BLOCKS, "minecraft:used", Set.of(),
                "minecraft:dirt"::equals, ID, new SortState("total", false));
        assertEquals(List.of("minecraft:dirt"), ids(blocks));
        PivotTable items = TableModel.pivot(snapshot(anna), Tab.ITEMS, "minecraft:used", Set.of(),
                "minecraft:dirt"::equals, ID, new SortState("total", false));
        assertEquals(List.of("minecraft:bread"), ids(items));
    }

    @Test
    void deaths_newestFirst_hiddenLeftOut() {
        PlayerEntry anna = player("Anna", 1, Map.of());
        PlayerEntry ben = player("Ben", 1, Map.of());
        DeathEntry old = new DeathEntry(LocalDate.of(2026, 5, 1), anna.uuid(), 3, "ertrunken");
        DeathEntry recent = new DeathEntry(LocalDate.of(2026, 10, 1), anna.uuid(), 9, "von Ben getötet");
        DeathEntry hidden = new DeathEntry(LocalDate.of(2026, 10, 2), ben.uuid(), 9, "gefallen");
        StatsSnapshot s = new StatsSnapshot(LocalDate.of(2026, 4, 27), 179, 2140, List.of(anna, ben),
                List.of(old, recent, hidden));
        SortState sort = new SortState("date", false);
        assertEquals(List.of(recent, old), TableModel.deaths(s, Set.of(ben.uuid()), sort));
        assertEquals(List.of(old, recent), TableModel.deaths(s, Set.of(ben.uuid()), sort.click("date")));
    }
}
