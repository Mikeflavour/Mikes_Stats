package mikesstats.core;

/** Where the "Statistiken alle" button goes in the pause menu. */
public final class ButtonPlacement {
    public static final int GAP = 4;

    private ButtonPlacement() {
    }

    /** Right of "Statistiken" when it fits with a 4 px gap and 4 px to the screen edge, otherwise below it. */
    public static boolean rightOf(int statsX, int statsW, int screenW, int btnW) {
        return statsX + statsW + GAP + btnW <= screenW - GAP;
    }
}
