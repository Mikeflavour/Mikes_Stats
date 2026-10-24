package mikesstats.client;

import static mikesstats.client.StatsScreen.t;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import mikesstats.core.SortState;
import mikesstats.core.StatsSnapshot;
import mikesstats.core.StatsSnapshot.PlayerEntry;
import mikesstats.core.TableModel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** "Player X/Y": tick who shows up in Statistiken alle. Saved only on this PC. */
public class PlayerFilterScreen extends Screen {
    private final Screen parent;
    private final List<StatsScreen.Hit> hits = new ArrayList<>();
    private int scroll;

    public PlayerFilterScreen(Screen parent) {
        super(Component.translatable("mikes_stats.filter.title"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (StatsScreen.Hit hit : List.copyOf(hits)) {
                if (hit.contains(event.x(), event.y())) {
                    hit.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * 2);
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        hits.clear();
        StatsSnapshot s = StatsScreen.latest;
        Set<UUID> hidden = ClientConfig.get().hidden;
        List<PlayerEntry> players = s == null ? List.of() : TableModel.overview(s, Set.of(), new SortState("name", false));

        int w = Math.min(width - 16, 220);
        int x0 = (width - w) / 2;
        int x1 = x0 + w;
        int y0 = 8;
        int y1 = height - 8;
        g.fill(x0, y0, x1, y1, StatsScreen.PANEL);
        g.outline(x0, y0, w, y1 - y0, StatsScreen.BORDER);
        int left = x0 + 10;
        int right = x1 - 10;

        long visible = players.stream().filter(p -> !hidden.contains(p.uuid())).count();
        g.text(font, t("players", visible, players.size()), left, y0 + 10, StatsScreen.WHITE);
        g.text(font, t("filter.hint"), left, y0 + 22, StatsScreen.MUTED);

        int rowsTop = y0 + 38;
        int bottom = y1 - 26;
        int fit = Math.max(0, (bottom - rowsTop) / StatsScreen.ROW_H);
        scroll = Math.max(0, Math.min(scroll, players.size() - fit));
        for (int i = 0; i < fit && scroll + i < players.size(); i++) {
            PlayerEntry p = players.get(scroll + i);
            int y = rowsTop + i * StatsScreen.ROW_H;
            boolean shown = !hidden.contains(p.uuid());
            if ((scroll + i) % 2 == 0) g.fill(left - 2, y, right + 2, y + StatsScreen.ROW_H, StatsScreen.ZEBRA);
            g.outline(left, y + 4, 10, 10, shown ? StatsScreen.GOLD : StatsScreen.BUTTON_BORDER);
            if (shown) g.fill(left + 2, y + 6, left + 8, y + 12, StatsScreen.GOLD);
            PlayerFaceExtractor.extractRenderState(g, StatsScreen.profile(p.uuid()), left + 16, y + 3, 12);
            g.text(font, p.name(), left + 32, y + 5, shown ? StatsScreen.WHITE : StatsScreen.MUTED);
            hits.add(new StatsScreen.Hit(left - 2, y, right + 2, y + StatsScreen.ROW_H, () -> {
                if (!hidden.remove(p.uuid())) hidden.add(p.uuid());
                ClientConfig.get().save();
            }));
        }

        String done = t("done");
        int bw = font.width(done) + 16;
        int bx = (x0 + x1 - bw) / 2;
        int by = y1 - 20;
        g.fill(bx, by, bx + bw, by + 15, StatsScreen.BUTTON);
        g.outline(bx, by, bw, 15, StatsScreen.BUTTON_BORDER);
        g.text(font, done, bx + 8, by + 4, StatsScreen.TEXT, false);
        hits.add(new StatsScreen.Hit(bx, by, bx + bw, by + 15, this::onClose));

        super.extractRenderState(g, mouseX, mouseY, a);
    }
}
