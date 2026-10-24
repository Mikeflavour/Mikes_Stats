package mikesstats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ChestLayoutTest {
    @Test
    void pages() {
        assertEquals(1, ChestLayout.pages(0));
        assertEquals(1, ChestLayout.pages(36));
        assertEquals(2, ChestLayout.pages(37));
    }

    @Test
    void headSlots_secondPageOfForty() {
        assertEquals(List.of(9, 10, 11, 12), ChestLayout.headSlots(1, 40));
        assertEquals(36, ChestLayout.headSlots(0, 40).size());
        assertEquals(List.of(), ChestLayout.headSlots(0, 0));
    }
}
