package mikesstats.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** German number/date formats shown in both menus. */
public final class Formats {
    private static final DecimalFormatSymbols DE = DecimalFormatSymbols.getInstance(Locale.GERMANY);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private Formats() {
    }

    public static String date(LocalDate date) {
        return date.format(DATE);
    }

    public static String km(long cm) {
        return new DecimalFormat("#,##0.0", DE).format(cm / 100_000.0) + " km";
    }

    /** 20 ticks per second */
    public static String hours(long ticks) {
        return number(ticks / 72_000) + " h";
    }

    public static String number(long n) {
        return new DecimalFormat("#,##0", DE).format(n);
    }

    public static long daysSince(LocalDate start, LocalDate today) {
        return ChronoUnit.DAYS.between(start, today);
    }

    /** total overworld time, not time of day (that one changes with /time set) */
    public static long ingameDay(long gameTime) {
        return gameTime / 24_000;
    }
}
