package mikesstats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class FormatsTest {
    @Test
    void formats() {
        assertEquals("21.10.2026", Formats.date(LocalDate.of(2026, 10, 21)));
        assertEquals("12,4 km", Formats.km(1_240_000));
        assertEquals("1.234,5 km", Formats.km(123_450_000));
        assertEquals("86 h", Formats.hours(86 * 72000));
        assertEquals("18.240", Formats.number(18_240));
        assertEquals(179, Formats.daysSince(LocalDate.of(2026, 4, 27), LocalDate.of(2026, 10, 23)));
        assertEquals(2140, Formats.ingameDay(51_360_000));
    }
}
