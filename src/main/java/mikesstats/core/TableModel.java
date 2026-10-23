package mikesstats.core;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

import mikesstats.core.StatsSnapshot.DeathEntry;
import mikesstats.core.StatsSnapshot.PlayerEntry;

/** Builds the rows of every tab from a snapshot, without hidden players, sorted like the spec says. */
public final class TableModel {
    /** German A–Z, umlauts next to their base letter */
    private static final Collator TEXT = Collator.getInstance(Locale.GERMAN);
    private static final Comparator<PlayerEntry> BY_NAME = Comparator.comparing(PlayerEntry::name, TEXT);

    public record PivotTable(List<PlayerEntry> columns, List<PivotRow> rows) {
    }

    /** @param total -1 = no total for this row */
    public record PivotRow(String statId, long[] values, long total) {
    }

    private TableModel() {
    }

    public static int custom(PlayerEntry player, String statId) {
        return player.stats().getOrDefault("minecraft:custom", Map.of()).getOrDefault(statId, 0);
    }

    /** Sort keys: name, level, maxLevel, deaths, pvpKills, playTime, xpTotal. */
    public static List<PlayerEntry> overview(StatsSnapshot snapshot, Set<UUID> hidden, SortState sort) {
        Comparator<PlayerEntry> c = switch (sort.key()) {
            case "level" -> highestFirst(PlayerEntry::level);
            case "maxLevel" -> highestFirst(PlayerEntry::maxLevel);
            case "deaths" -> highestFirst(p -> custom(p, "minecraft:deaths"));
            case "pvpKills" -> highestFirst(p -> custom(p, "minecraft:player_kills"));
            case "playTime" -> highestFirst(p -> custom(p, "minecraft:play_time"));
            case "xpTotal" -> highestFirst(PlayerEntry::xpTotal);
            default -> BY_NAME;
        };
        return visible(snapshot, hidden).sorted(flip(c, sort).thenComparing(BY_NAME)).toList();
    }

    /**
     * One row per stat, one column per visible player (A–Z). Sort keys: "name" (by label), "total",
     * or a player's uuid string.
     */
    public static PivotTable pivot(StatsSnapshot snapshot, Tab tab, String category, Set<UUID> hidden,
                                   Predicate<String> isBlock, Function<String, String> label, SortState sort) {
        List<PlayerEntry> columns = visible(snapshot, hidden).sorted(BY_NAME).toList();
        Set<String> ids = new LinkedHashSet<>();
        columns.forEach(p -> ids.addAll(p.stats().getOrDefault(category, Map.of()).keySet()));

        List<PivotRow> rows = new ArrayList<>();
        for (String id : ids) {
            if (!fits(tab, category, id, isBlock)) continue;
            long[] values = new long[columns.size()];
            long total = 0;
            boolean any = false;
            for (int i = 0; i < values.length; i++) {
                values[i] = columns.get(i).stats().getOrDefault(category, Map.of()).getOrDefault(id, 0);
                total += values[i];
                any |= values[i] != 0;
            }
            if (any) rows.add(new PivotRow(id, values, Tabs.hasNoTotal(id) ? -1 : total));
        }

        Comparator<PivotRow> byLabel = Comparator.comparing(r -> label.apply(r.statId()), TEXT);
        Comparator<PivotRow> c = byLabel;
        if (!sort.key().equals("name")) {
            int col = indexOf(columns, sort.key());
            c = highestFirst(r -> col < 0 ? r.total() : r.values()[col]);
        }
        rows.sort(flip(c, sort).thenComparing(byLabel));
        return new PivotTable(columns, rows);
    }

    /** Sort keys: date (default, newest first), player, level, cause. */
    public static List<DeathEntry> deaths(StatsSnapshot snapshot, Set<UUID> hidden, SortState sort) {
        Map<UUID, String> names = new HashMap<>();
        snapshot.players().forEach(p -> names.put(p.uuid(), p.name()));
        boolean numeric = !sort.key().equals("player") && !sort.key().equals("cause");
        Comparator<DeathEntry> ascending = switch (sort.key()) {
            case "player" -> Comparator.comparing(d -> names.getOrDefault(d.player(), ""), TEXT);
            case "cause" -> Comparator.comparing(DeathEntry::cause, TEXT);
            case "level" -> Comparator.comparingInt(DeathEntry::level);
            default -> Comparator.comparing(DeathEntry::date);
        };
        // stable sort on the chronological list, then reverse: same-day deaths keep their real order
        List<DeathEntry> list = new ArrayList<>(snapshot.deaths().stream()
                .filter(d -> !hidden.contains(d.player())).sorted(ascending).toList());
        if (numeric != sort.flipped()) Collections.reverse(list);
        return list;
    }

    private static boolean fits(Tab tab, String category, String id, Predicate<String> isBlock) {
        if (category.equals("minecraft:custom")) return Tabs.belongsTo(tab, id);
        // "used" of a block item = placed (Blöcke); Items/Benutzt shows only the rest, so nothing appears twice
        if (category.equals("minecraft:used") && tab == Tab.BLOCKS) return isBlock.test(id);
        if (category.equals("minecraft:used") && tab == Tab.ITEMS) return !isBlock.test(id);
        return true;
    }

    private static Stream<PlayerEntry> visible(StatsSnapshot snapshot, Set<UUID> hidden) {
        return snapshot.players().stream().filter(p -> !hidden.contains(p.uuid()));
    }

    private static int indexOf(List<PlayerEntry> columns, String uuid) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).uuid().toString().equals(uuid)) return i;
        }
        return -1;
    }

    private static <T> Comparator<T> highestFirst(ToLongFunction<T> value) {
        return Comparator.comparingLong(value).reversed();
    }

    private static <T> Comparator<T> flip(Comparator<T> c, SortState sort) {
        return sort.flipped() ? c.reversed() : c;
    }
}
