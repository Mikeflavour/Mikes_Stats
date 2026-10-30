package mikesstats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PivotLayoutTest {
    @Test
    void columnWidth_fewPlayersGetMax() {
        assertEquals(60, PivotLayout.columnWidth(420, 3, 30));
    }

    @Test
    void columnWidth_manyPlayersNeverBelowMin() {
        assertEquals(40, PivotLayout.columnWidth(420, 15, 30));
    }

    @Test
    void columnWidth_wideValuesWidenUpToMax() {
        assertEquals(50, PivotLayout.columnWidth(420, 15, 50));
        assertEquals(60, PivotLayout.columnWidth(420, 15, 90));
    }

    @Test
    void visibleColumns_fitTheSpaceAtLeastOne() {
        assertEquals(10, PivotLayout.visibleColumns(420, 40, 15));
        assertEquals(3, PivotLayout.visibleColumns(420, 60, 3));
        assertEquals(1, PivotLayout.visibleColumns(10, 40, 5));
    }

    @Test
    void clampScroll_staysInRange() {
        assertEquals(5, PivotLayout.clampScroll(9, 15, 10));
        assertEquals(0, PivotLayout.clampScroll(-1, 15, 10));
        assertEquals(0, PivotLayout.clampScroll(3, 3, 3));
    }
}
