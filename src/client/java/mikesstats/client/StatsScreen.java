package mikesstats.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import mikesstats.StatNames;
import mikesstats.Texts;
import mikesstats.core.Formats;
import mikesstats.core.PivotLayout;
import mikesstats.core.SortState;
import mikesstats.core.StatsSnapshot;
import mikesstats.core.StatsSnapshot.DeathEntry;
import mikesstats.core.StatsSnapshot.PlayerEntry;
import mikesstats.core.Tab;
import mikesstats.core.TableModel;
import mikesstats.core.TableModel.PivotRow;
import mikesstats.core.TableModel.PivotTable;
import mikesstats.core.Tabs;
import mikesstats.server.Network;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ResolvableProfile;

/** "Statistiken alle": all players in tabs, like the approved mockup (dark, 50 % see-through, sortable columns). */
public class StatsScreen extends Screen {
    // colours from the mockup
    static final int PANEL = 0x800E0F12;
    static final int BORDER = 0xFF34363C;
    static final int LINE = 0xFF2C2E33;
    static final int WHITE = 0xFFFFFFFF;
    static final int TEXT = 0xFFE8E8E8;
    static final int MUTED = 0xFF8A8D93;
    static final int SOFT = 0xFFB9BCC2;
    static final int GOLD = 0xFFE8C547;
    static final int GREEN = 0xFF7FE05A;
    static final int RED = 0xFFFF7A7A;
    static final int ORANGE = 0xFFFFAA00;
    static final int CELL = 0xFF9A9DA3;
    static final int BUTTON = 0xFF1F2024;
    static final int BUTTON_BORDER = 0xFF3A3C42;
    static final int ZEBRA = 0x0DFFFFFF;
    static final int OWN_ROW = 0x1FFFFFFF;
    static final int ROW_H = 20;
    /** stat names keep at least this much before player columns scroll sideways */
    static final int NAME_MIN = 110;

    private static final String[] TAB_KEYS = {"overview", "general", "mobs", "blocks", "items", "ways", "deaths"};

    /** Newest stats from the server; set by the network receiver. */
    static volatile StatsSnapshot latest;

    private final Screen parent;
    private Tab tab = Tab.OVERVIEW;
    private int sub;
    private SortState sort = new SortState("name", false);
    private int scroll;
    /** first player column shown in the pivot tabs */
    private int scrollX;
    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, String> causes = new HashMap<>();

    // rebuilt when data, tab, sub, sort or hidden players change
    private StatsSnapshot built;
    private String builtKey = "";
    private List<PlayerEntry> overview = List.of();
    private PivotTable pivot;
    private List<DeathEntry> deaths = List.of();
    // pivot widths, measured once per rebuild instead of every frame
    private int pivotValueW;
    private int pivotTotalW;

    record Hit(int x0, int y0, int x1, int y1, Runnable action) {
        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    public StatsScreen(Screen parent) {
        super(Component.translatable("mikes_stats.title"));
        this.parent = parent;
    }

    static String t(String key, Object... args) {
        return I18n.get("mikes_stats." + key, args);
    }

    @Override
    protected void init() {
        ClientPlayNetworking.send(new Network.RequestStatsPayload());
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    /** No blur or darkening: the game stays visible behind the panel. */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (Hit hit : List.copyOf(hits)) {
                if (hit.contains(event.x(), event.y())) {
                    hit.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (dx != 0 || minecraft.hasShiftDown()) {
            scrollX = Math.max(0, scrollX - (int) Math.signum(dx != 0 ? dx : dy));
        } else {
            scroll = Math.max(0, scroll - (int) Math.signum(dy) * 2);
        }
        return true;
    }

    private void selectTab(Tab next) {
        tab = next;
        sub = 0;
        scroll = 0;
        sort = new SortState(switch (next) {
            case OVERVIEW -> "name";
            case DEATHS -> "date";
            default -> "total";
        }, false);
    }

    private void rebuild(StatsSnapshot s, Set<UUID> hidden) {
        String key = tab + "|" + sub + "|" + sort + "|" + hidden.hashCode() + "|" + hidden.size();
        if (s == built && key.equals(builtKey)) return;
        built = s;
        builtKey = key;
        switch (tab) {
            case OVERVIEW -> overview = TableModel.overview(s, hidden, sort);
            case DEATHS -> deaths = TableModel.deaths(s, hidden, sort);
            default -> {
                String category = Tabs.subCategories(tab).get(sub);
                pivot = TableModel.pivot(s, tab, category, hidden, StatNames::isBlockItem, id -> label(category, id), sort);
                measurePivot();
            }
        }
    }

    private void measurePivot() {
        int values = 0;
        int total = font.width(Component.literal(t("col.total") + " ▼").withStyle(ChatFormatting.BOLD));
        for (PivotRow r : pivot.rows()) {
            for (long v : r.values()) if (v > 0) values = Math.max(values, font.width(Formats.value(r.statId(), v)));
            if (r.total() >= 0) total = Math.max(total, font.width(Formats.value(r.statId(), r.total())));
        }
        pivotValueW = values + 6;
        pivotTotalW = total + 8;
    }

    private static String label(String category, String statId) {
        return I18n.get(StatNames.key(category, statId));
    }

    private static final Map<UUID, ResolvableProfile> PROFILES = new HashMap<>();

    /** One profile per player, so the skin cache finds it again every frame. */
    static ResolvableProfile profile(UUID uuid) {
        return PROFILES.computeIfAbsent(uuid, ResolvableProfile::createUnresolved);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        hits.clear();
        int w = Math.min(width - 16, 640);
        int x0 = (width - w) / 2;
        int x1 = x0 + w;
        int y0 = 8;
        int y1 = height - 8;
        g.fill(x0, y0, x1, y1, PANEL);
        g.outline(x0, y0, w, y1 - y0, BORDER);
        int left = x0 + 10;
        int right = x1 - 10;

        StatsSnapshot s = latest;
        Set<UUID> hidden = ClientConfig.get().hidden;

        // header: title left, server info chips right
        g.text(font, title, left, y0 + 10, WHITE);
        if (s != null) {
            int cx = chip(g, t("day", Formats.number(s.ingameDay())), right);
            chip(g, t("since", Formats.date(s.serverStart()), Formats.number(s.daysSinceStart())), cx - 6);
        }

        // tabs, and the Player button on the right
        int ty = y0 + 30;
        int tx = left;
        for (Tab each : Tab.values()) {
            String name = t("tab." + TAB_KEYS[each.ordinal()]);
            int tw = font.width(name);
            boolean on = each == tab;
            g.text(font, name, tx, ty, on ? WHITE : MUTED);
            if (on) g.fill(tx - 2, ty + 11, tx + tw + 2, ty + 13, GOLD);
            hits.add(new Hit(tx - 4, ty - 4, tx + tw + 4, ty + 13, () -> selectTab(each)));
            tx += tw + 12;
        }
        g.fill(left, ty + 13, right, ty + 14, LINE);
        if (s != null) {
            long visible = s.players().stream().filter(p -> !hidden.contains(p.uuid())).count();
            String label = t("players", visible, s.players().size());
            int bw = font.width(label) + 12;
            button(g, label, right - bw, ty - 4, bw, false, () -> minecraft.gui.setScreen(new PlayerFilterScreen(this)));
        }

        int top = ty + 20;
        List<String> subs = Tabs.subCategories(tab);
        if (subs.size() > 1) {
            int px = left;
            for (int i = 0; i < subs.size(); i++) {
                String name = t("sub." + subKeyFor(subs.get(i)));
                int bw = font.width(name) + 12;
                int index = i;
                button(g, name, px, top, bw, i == sub, () -> {
                    sub = index;
                    scroll = 0;
                });
                px += bw + 4;
            }
            top += 18;
        }

        int bottom = y1 - (tab == Tab.OVERVIEW ? 18 : 8);
        if (s == null) {
            g.centeredText(font, t("loading"), (x0 + x1) / 2, (top + bottom) / 2, MUTED);
        } else {
            rebuild(s, hidden);
            g.enableScissor(left - 2, top, right + 2, bottom);
            switch (tab) {
                case OVERVIEW -> drawOverview(g, left, right, top, bottom, mouseX, mouseY);
                case DEATHS -> drawDeaths(g, left, right, top, bottom);
                default -> drawPivot(g, left, right, top, bottom, mouseX, mouseY);
            }
            g.disableScissor();
        }
        if (tab == Tab.OVERVIEW) g.text(font, t("legend"), left, y1 - 14, MUTED);

        super.extractRenderState(g, mouseX, mouseY, a);
    }

    private static String subKey(String category) {
        return category.substring("minecraft:".length());
    }

    /** "used" means placed in Blöcke. */
    private String subKeyFor(String category) {
        return tab == Tab.BLOCKS && category.equals("minecraft:used") ? "placed" : subKey(category);
    }

    private int chip(GuiGraphicsExtractor g, String text, int rightEdge) {
        int cw = font.width(text) + 12;
        int x = rightEdge - cw;
        g.outline(x, 12, cw, 16, BORDER);
        g.text(font, text, x + 6, 16, SOFT);
        return x;
    }

    private void button(GuiGraphicsExtractor g, String text, int x, int y, int w, boolean active, Runnable action) {
        g.fill(x, y, x + w, y + 15, active ? GOLD : BUTTON);
        g.outline(x, y, w, 15, active ? GOLD : BUTTON_BORDER);
        g.text(font, text, x + 6, y + 4, active ? 0xFF141414 : TEXT, false);
        hits.add(new Hit(x, y, x + w, y + 15, action));
    }

    // ---- column headers -------------------------------------------------------------------

    /** Draws a sortable header; active column white, bold, with ▲ (ascending) or ▼. */
    private void header(GuiGraphicsExtractor g, String label, String key, boolean numeric, int x, int colW, boolean alignRight, int y) {
        boolean on = sort.key().equals(key);
        boolean ascending = numeric == sort.flipped();
        Component text = on ? Component.literal(label + (ascending ? " ▲" : " ▼")).withStyle(ChatFormatting.BOLD)
                : Component.literal(label);
        int tw = font.width(text);
        int tx = alignRight ? x + colW - tw : x;
        g.text(font, text, tx, y, on ? WHITE : MUTED);
        hits.add(new Hit(x, y - 3, x + colW, y + 11, () -> {
            sort = sort.click(key);
            scroll = 0;
        }));
    }

    /** Full text as tooltip when it had to be cut to fit. */
    private void cutTooltip(GuiGraphicsExtractor g, String full, String shown, int x0, int y0, int x1, int y1, int mouseX, int mouseY) {
        if (shown.length() < full.length() && mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1) {
            g.setTooltipForNextFrame(font, Component.literal(full), mouseX, mouseY);
        }
    }

    private void arrow(GuiGraphicsExtractor g, String glyph, int x, int y, int step) {
        g.text(font, glyph, x, y, GOLD);
        hits.add(new Hit(x - 2, y - 2, x + 10, y + 10, () -> scrollX += step));
    }

    private int visibleRows(int rowsTop, int bottom, int count) {
        int fit = Math.max(0, (bottom - rowsTop) / ROW_H);
        scroll = Math.max(0, Math.min(scroll, count - fit));
        return fit;
    }

    private void scrollbar(GuiGraphicsExtractor g, int right, int rowsTop, int bottom, int count, int fit) {
        if (count <= fit || fit == 0) return;
        int track = bottom - rowsTop;
        int thumb = Math.max(10, track * fit / count);
        int y = rowsTop + (track - thumb) * scroll / Math.max(1, count - fit);
        g.fill(right + 1, rowsTop, right + 2, bottom, 0x20FFFFFF);
        g.fill(right + 1, y, right + 2, y + thumb, MUTED);
    }

    private void rowBackground(GuiGraphicsExtractor g, int left, int right, int y, int index, UUID player) {
        boolean own = minecraft.player != null && minecraft.player.getUUID().equals(player);
        if (own) g.fill(left - 2, y, right + 2, y + ROW_H, OWN_ROW);
        else if (index % 2 == 0) g.fill(left - 2, y, right + 2, y + ROW_H, ZEBRA);
    }

    private void empty(GuiGraphicsExtractor g, String key, int left, int right, int y) {
        g.centeredText(font, t(key), (left + right) / 2, y, MUTED);
    }

    // ---- Übersicht ------------------------------------------------------------------------

    private void drawOverview(GuiGraphicsExtractor g, int left, int right, int top, int bottom, int mouseX, int mouseY) {
        String[][] cols = {
                {"level", t("col.level")}, {"maxLevel", t("col.max_level")}, {"deaths", t("col.deaths")},
                {"pvpKills", t("col.pvp")}, {"playTime", t("col.play_time")}, {"xpTotal", t("col.xp")}};
        int[] widths = new int[cols.length];
        String[] below = new String[cols.length]; // "(seit Mod)" goes on a second header line
        int fixed = 0;
        for (int i = 0; i < cols.length; i++) {
            int split = cols[i][1].indexOf(" (");
            if (split > 0) {
                below[i] = cols[i][1].substring(split + 1);
                cols[i][1] = cols[i][1].substring(0, split);
            }
            widths[i] = Math.max(font.width(Component.literal(cols[i][1] + " ▼").withStyle(ChatFormatting.BOLD)),
                    below[i] == null ? 0 : font.width(below[i])) + 8;
            fixed += widths[i];
        }
        int nameX = left + 26;
        int nameW = Math.max(40, right - nameX - fixed);

        header(g, t("col.name"), "name", false, nameX, nameW, false, top);
        int cx = nameX + nameW;
        for (int i = 0; i < cols.length; i++) {
            header(g, cols[i][1], cols[i][0], true, cx, widths[i], true, top);
            if (below[i] != null) g.text(font, below[i], cx + widths[i] - font.width(below[i]), top + 10, MUTED);
            cx += widths[i];
        }

        int rowsTop = top + 24;
        if (overview.isEmpty()) {
            empty(g, "empty.hidden", left, right, rowsTop + 10);
            return;
        }
        // status under the name for everyone as soon as one row has no room beside it
        boolean stacked = false;
        for (PlayerEntry p : overview) stacked |= font.width(p.name()) + 6 + font.width(status(p)) > nameW - 4;
        int fit = visibleRows(rowsTop, bottom, overview.size());
        for (int i = 0; i < fit && scroll + i < overview.size(); i++) {
            PlayerEntry p = overview.get(scroll + i);
            int y = rowsTop + i * ROW_H;
            rowBackground(g, left, right, y, scroll + i, p.uuid());
            int ty = y + 6;

            g.fill(left, y + 7, left + 6, y + 13, 0xFF000000 | p.waypointColor());
            if (mouseX >= left && mouseX < left + 6 && mouseY >= y + 7 && mouseY < y + 13) {
                g.setTooltipForNextFrame(font, Component.literal(String.format("#%06X", p.waypointColor())), mouseX, mouseY);
            }
            PlayerFaceExtractor.extractRenderState(g, profile(p.uuid()), left + 10, y + 4, 12);

            String status = status(p);
            int statusColor = p.online() ? GREEN : MUTED;
            if (stacked) {
                g.text(font, font.plainSubstrByWidth(p.name(), nameW - 4), nameX, y + 2, WHITE);
                g.text(font, font.plainSubstrByWidth(status, nameW - 4), nameX, y + 11, statusColor);
            } else {
                g.text(font, p.name(), nameX, ty, WHITE);
                g.text(font, status, nameX + font.width(p.name()) + 6, ty, statusColor);
            }

            String[] values = {
                    Formats.number(p.level()),
                    p.maxLevel() < 0 ? "–" : Formats.number(p.maxLevel()),
                    Formats.number(TableModel.custom(p, "minecraft:deaths")),
                    Formats.number(TableModel.custom(p, "minecraft:player_kills")),
                    Formats.hours(TableModel.custom(p, "minecraft:play_time")),
                    p.xpTotal() < 0 ? "–" : Formats.number(p.xpTotal())};
            int[] colors = {GREEN, SOFT, RED, ORANGE, TEXT, SOFT};
            cx = nameX + nameW;
            for (int c = 0; c < values.length; c++) {
                int vx = cx + widths[c] - font.width(values[c]);
                g.text(font, values[c], vx, ty, colors[c]);
                if (c == 3 && mouseX >= vx && mouseX < cx + widths[c] && mouseY >= y && mouseY < y + ROW_H) {
                    g.setTooltipForNextFrame(font, Component.literal(victims(p)), mouseX, mouseY);
                }
                cx += widths[c];
            }
        }
        scrollbar(g, right, rowsTop, bottom, overview.size(), fit);
    }

    private static String status(PlayerEntry p) {
        return p.online() ? t("online") : t("last_seen", p.lastSeen() == null ? "–" : Formats.date(p.lastSeen()));
    }

    /** "Ben 3×, Tim 1×" – who this player killed since the mod was installed. */
    private static String victims(PlayerEntry p) {
        if (p.pvpVictims().isEmpty()) return t("pvp.none");
        return p.pvpVictims().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(e -> e.getKey() + " " + e.getValue() + "×")
                .collect(Collectors.joining(", "));
    }

    // ---- Allgemein, Mobs, Blöcke, Items, Wege ---------------------------------------------

    private void drawPivot(GuiGraphicsExtractor g, int left, int right, int top, int bottom, int mouseX, int mouseY) {
        String category = Tabs.subCategories(tab).get(sub);
        List<PlayerEntry> players = pivot.columns();
        if (players.isEmpty()) {
            empty(g, "empty.hidden", left, right, top + 20);
            return;
        }
        // Gesamt pinned right, player columns left of it, the stat name gets the rest
        int n = players.size();
        int nameX = left + 20;
        int totalX = right - pivotTotalW;
        int avail = totalX - nameX - NAME_MIN;
        int colW = PivotLayout.columnWidth(avail, n, pivotValueW);
        int shown = PivotLayout.visibleColumns(avail, colW, n);
        scrollX = PivotLayout.clampScroll(scrollX, n, shown);
        int colsX = totalX - shown * colW;
        int nameW = colsX - nameX;

        int labelY = top + 12;
        header(g, t("col." + TAB_KEYS[tab.ordinal()]), "name", false, nameX, nameW, false, labelY);
        for (int c = 0; c < shown; c++) {
            PlayerEntry p = players.get(scrollX + c);
            int cx = colsX + c * colW;
            PlayerFaceExtractor.extractRenderState(g, profile(p.uuid()), cx + colW - 9, top, 8);
            String name = font.plainSubstrByWidth(p.name(), colW - 4);
            header(g, name, p.uuid().toString(), true, cx, colW, true, labelY);
            cutTooltip(g, p.name(), name, cx, top, cx + colW, labelY + 11, mouseX, mouseY);
        }
        if (scrollX > 0) arrow(g, "◀", colsX - 10, top, -1);
        if (scrollX + shown < n) arrow(g, "▶", totalX + 2, top, 1);
        header(g, t("col.total"), "total", true, totalX, pivotTotalW, true, labelY);

        int rowsTop = labelY + 14;
        List<PivotRow> rows = pivot.rows();
        if (rows.isEmpty()) {
            empty(g, "empty.rows", left, right, rowsTop + 10);
            return;
        }
        int fit = visibleRows(rowsTop, bottom, rows.size());
        for (int i = 0; i < fit && scroll + i < rows.size(); i++) {
            PivotRow r = rows.get(scroll + i);
            int y = rowsTop + i * ROW_H;
            rowBackground(g, left, right, y, scroll + i, null);
            int ty = y + 6;
            g.item(StatIcons.icon(category, r.statId()), left, y + 2);
            String label = label(category, r.statId());
            String cut = font.plainSubstrByWidth(label, nameW - 4);
            g.text(font, cut, nameX, ty, WHITE);
            cutTooltip(g, label, cut, nameX, y, colsX, y + ROW_H, mouseX, mouseY);
            long best = 0;
            for (long v : r.values()) best = Math.max(best, v);
            for (int c = 0; c < shown; c++) {
                long v = r.values()[scrollX + c];
                String text = v == 0 ? "–" : Formats.value(r.statId(), v);
                g.text(font, text, colsX + (c + 1) * colW - font.width(text), ty, v > 0 && v == best ? WHITE : CELL);
            }
            String total = r.total() < 0 ? "–" : Formats.value(r.statId(), r.total());
            g.text(font, total, right - font.width(total), ty, GOLD);
        }
        scrollbar(g, right, rowsTop, bottom, rows.size(), fit);
    }

    // ---- Tode -----------------------------------------------------------------------------

    private void drawDeaths(GuiGraphicsExtractor g, int left, int right, int top, int bottom) {
        int dateW = font.width(Component.literal(t("col.date") + " ▼").withStyle(ChatFormatting.BOLD)) + 30;
        int levelW = font.width(Component.literal(t("col.death_level") + " ▼").withStyle(ChatFormatting.BOLD)) + 12;
        int rest = right - left - dateW - levelW;
        int playerW = Math.max(60, rest / 3);
        int playerX = left + dateW;
        int levelX = playerX + playerW;
        int causeX = levelX + levelW + 10;
        int causeW = right - causeX;

        header(g, t("col.date"), "date", true, left, dateW, false, top);
        header(g, t("col.player"), "player", false, playerX, playerW, false, top);
        header(g, t("col.death_level"), "level", true, levelX, levelW, true, top);
        header(g, t("col.cause"), "cause", false, causeX, causeW, false, top);

        int rowsTop = top + 14;
        if (deaths.isEmpty()) {
            empty(g, "empty.deaths", left, right, rowsTop + 10);
            return;
        }
        Map<UUID, String> names = new HashMap<>();
        built.players().forEach(p -> names.put(p.uuid(), p.name()));
        int fit = visibleRows(rowsTop, bottom, deaths.size());
        for (int i = 0; i < fit && scroll + i < deaths.size(); i++) {
            DeathEntry d = deaths.get(scroll + i);
            int y = rowsTop + i * ROW_H;
            rowBackground(g, left, right, y, scroll + i, d.player());
            int ty = y + 6;
            g.text(font, Formats.date(d.date()), left, ty, SOFT);
            PlayerFaceExtractor.extractRenderState(g, profile(d.player()), playerX, y + 4, 12);
            g.text(font, font.plainSubstrByWidth(names.getOrDefault(d.player(), "?"), playerW - 20), playerX + 16, ty, WHITE);
            String level = Formats.number(d.level());
            g.text(font, level, levelX + levelW - font.width(level), ty, GREEN);
            g.text(font, font.plainSubstrByWidth(cause(d.cause()), causeW), causeX, ty, RED);
        }
        scrollbar(g, right, rowsTop, bottom, deaths.size(), fit);
    }

    /** Death messages are component JSON from the server, shown in this player's language. */
    private String cause(String json) {
        return causes.computeIfAbsent(json, j -> minecraft.player == null ? j
                : Texts.fromJson(j, minecraft.player.registryAccess()).getString().trim());
    }
}
