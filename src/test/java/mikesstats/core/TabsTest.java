package mikesstats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TabsTest {
    @Test
    void general_excludesOverviewAndWays() {
        for (String id : List.of("minecraft:play_time", "minecraft:deaths", "minecraft:player_kills",
                "minecraft:walk_one_cm", "minecraft:jump")) {
            assertFalse(Tabs.belongsTo(Tab.GENERAL, id), id);
        }
        assertTrue(Tabs.belongsTo(Tab.GENERAL, "minecraft:damage_dealt"));
        assertTrue(Tabs.belongsTo(Tab.WAYS, "minecraft:walk_one_cm"));
        assertTrue(Tabs.belongsTo(Tab.WAYS, "minecraft:jump"));
        assertTrue(Tabs.belongsTo(Tab.WAYS, "minecraft:sneak_time"));
        assertFalse(Tabs.belongsTo(Tab.WAYS, "minecraft:play_time"));
    }

    @Test
    void subCategories() {
        assertEquals(List.of("minecraft:killed", "minecraft:killed_by"), Tabs.subCategories(Tab.MOBS));
        assertEquals(List.of("minecraft:mined", "minecraft:used"), Tabs.subCategories(Tab.BLOCKS));
        assertEquals(List.of("minecraft:crafted", "minecraft:used", "minecraft:broken", "minecraft:picked_up",
                "minecraft:dropped"), Tabs.subCategories(Tab.ITEMS));
        assertEquals(List.of("minecraft:custom"), Tabs.subCategories(Tab.WAYS));
        assertTrue(Tabs.hasNoTotal("minecraft:time_since_death"));
        assertFalse(Tabs.hasNoTotal("minecraft:jump"));
    }

    @Test
    void sortState_click() {
        SortState s = new SortState("name", false);
        assertEquals(new SortState("name", true), s.click("name"));
        assertEquals(new SortState("level", false), s.click("name").click("level"));
    }
}
