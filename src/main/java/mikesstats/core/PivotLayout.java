package mikesstats.core;

/** Player columns of the pivot tabs: all the same width, Gesamt pinned right, the rest scrolls sideways. */
public final class PivotLayout {
    public static final int MIN_COLUMN = 40;
    public static final int MAX_COLUMN = 60;

    private PivotLayout() {
    }

    /** Shares the space, at least as wide as the widest value, always between 40 and 60 px. */
    public static int columnWidth(int available, int players, int widestValue) {
        int share = available / Math.max(1, players);
        return Math.max(MIN_COLUMN, Math.min(MAX_COLUMN, Math.max(share, widestValue)));
    }

    /** How many columns fit side by side; at least one. */
    public static int visibleColumns(int available, int columnWidth, int players) {
        return Math.max(1, Math.min(players, available / columnWidth));
    }

    /** Index of the first shown column, kept in range. */
    public static int clampScroll(int first, int players, int visible) {
        return Math.max(0, Math.min(first, players - visible));
    }
}
