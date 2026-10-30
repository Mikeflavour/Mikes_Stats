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

    /** Under an hour in minutes, otherwise hours. */
    public static String duration(long ticks) {
        return ticks < 72_000 ? number(ticks / 1_200) + " min" : hours(ticks);
    }

    /** A stat value in the unit vanilla uses for it (Stats.makeCustomStat formatters). */
    public static String value(String statId, long value) {
        if (statId.endsWith("_one_cm")) return km(value);
        if (statId.endsWith("_time") || statId.startsWith("minecraft:time_since_")) return duration(value);
        if (statId.startsWith("minecraft:damage_")) return number(value / 10); // stored as tenths of a heart point
        return number(value);
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
