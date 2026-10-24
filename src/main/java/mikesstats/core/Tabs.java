package mikesstats.core;

import java.util.List;
import java.util.Set;

/** Which vanilla stat categories / custom stats show up in which tab (no info twice). */
public final class Tabs {
    /** shown as Übersicht columns, so not repeated in Allgemein */
    private static final Set<String> IN_OVERVIEW = Set.of("minecraft:play_time", "minecraft:deaths",
            "minecraft:player_kills");

    private Tabs() {
    }

    public static List<String> subCategories(Tab tab) {
        return switch (tab) {
            case MOBS -> List.of("minecraft:killed", "minecraft:killed_by");
            case BLOCKS -> List.of("minecraft:mined", "minecraft:used");
            case ITEMS -> List.of("minecraft:crafted", "minecraft:used", "minecraft:broken", "minecraft:picked_up",
                    "minecraft:dropped");
            case GENERAL, WAYS -> List.of("minecraft:custom");
            case OVERVIEW, DEATHS -> List.of();
        };
    }

    /** For stats of the minecraft:custom category. */
    public static boolean belongsTo(Tab tab, String customStatId) {
        boolean way = customStatId.endsWith("_one_cm") || customStatId.equals("minecraft:jump")
                || customStatId.equals("minecraft:sneak_time");
        return switch (tab) {
            case WAYS -> way;
            case GENERAL -> !way && !IN_OVERVIEW.contains(customStatId);
            default -> false;
        };
    }

    /** A sum over players makes no sense for these. */
    public static boolean hasNoTotal(String statId) {
        return statId.equals("minecraft:time_since_death") || statId.equals("minecraft:time_since_rest");
    }
}
