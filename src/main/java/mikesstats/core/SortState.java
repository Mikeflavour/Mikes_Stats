package mikesstats.core;

/** Active sort column. Not flipped: text A–Z, numbers highest first. */
public record SortState(String key, boolean flipped) {
    public SortState click(String clicked) {
        return clicked.equals(key) ? new SortState(key, !flipped) : new SortState(clicked, false);
    }
}
