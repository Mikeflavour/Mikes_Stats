package mikesstats.client;

import java.util.List;

import mikesstats.core.ButtonPlacement;
import mikesstats.server.Network;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** "Statistiken alle" in the pause menu, right of "Statistiken" (below it when there is no room). */
final class PauseButton {
    private static final int ROW = 24;

    private PauseButton() {
    }

    static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof PauseScreen)) return;
            List<AbstractWidget> widgets = Screens.getWidgets(screen);
            AbstractWidget stats = widgets.stream()
                    .filter(w -> w.getMessage().getContents() instanceof TranslatableContents t && t.getKey().equals("gui.stats"))
                    .findFirst().orElse(null);
            if (stats == null) return; // pause menu without buttons (F3+Esc)

            Button button = Button.builder(Component.translatable("mikes_stats.button"),
                    b -> client.gui.setScreen(new StatsScreen(screen))).width(stats.getWidth()).build();
            if (ButtonPlacement.rightOf(stats.getX(), stats.getWidth(), width, button.getWidth())) {
                button.setX(stats.getX() + stats.getWidth() + ButtonPlacement.GAP);
                button.setY(stats.getY());
            } else {
                for (AbstractWidget w : widgets) {
                    if (w.getY() > stats.getY()) w.setY(w.getY() + ROW);
                }
                button.setX(stats.getX());
                button.setY(stats.getY() + ROW);
            }
            if (!ClientPlayNetworking.canSend(Network.RequestStatsPayload.TYPE)) {
                button.active = false;
                button.setTooltip(Tooltip.create(Component.translatable("mikes_stats.button.no_server")));
            }
            widgets.add(button);
        });
    }
}
