package mikesstats.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ButtonPlacementTest {
    @Test
    void rightOf_onlyWhenItFitsWithMargins() {
        // pause menu at width 480: "Statistiken" at x=242, 98 wide -> needs 242+98+4+98 = 442 <= 476
        assertTrue(ButtonPlacement.rightOf(242, 98, 480, 98));
        // small window, width 320: 162+98+4+98 = 362 > 316
        assertFalse(ButtonPlacement.rightOf(162, 98, 320, 98));
        // exactly at the edge still fits
        assertTrue(ButtonPlacement.rightOf(100, 98, 304, 98));
        assertFalse(ButtonPlacement.rightOf(101, 98, 304, 98));
    }
}
