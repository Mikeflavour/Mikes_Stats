package mikesstats.core;

import java.util.ArrayList;
import java.util.List;

/** /stats chest: 6 rows, heads in rows 2–5 (slots 9–44), 36 per page. */
public final class ChestLayout {
    public static final int FIRST_HEAD_SLOT = 9;
    public static final int HEADS_PER_PAGE = 36;

    private ChestLayout() {
    }

    public static int pages(int count) {
        return Math.max(1, (count + HEADS_PER_PAGE - 1) / HEADS_PER_PAGE);
    }

    /** Slots used on this page (0-based); the i-th slot shows entry page * 36 + i. */
    public static List<Integer> headSlots(int page, int count) {
        int onPage = Math.max(0, Math.min(HEADS_PER_PAGE, count - page * HEADS_PER_PAGE));
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < onPage; i++) slots.add(FIRST_HEAD_SLOT + i);
        return slots;
    }
}
