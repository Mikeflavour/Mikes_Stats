package mikesstats.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import mikesstats.MikesStatsMod;
import mikesstats.StatNames;
import mikesstats.Texts;
import mikesstats.core.ChestLayout;
import mikesstats.core.Formats;
import mikesstats.core.SortState;
import mikesstats.core.StatsSnapshot;
import mikesstats.core.StatsSnapshot.DeathEntry;
import mikesstats.core.StatsSnapshot.PlayerEntry;
import mikesstats.core.Tab;
import mikesstats.core.TableModel;
import mikesstats.core.Tabs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TooltipDisplay;

/** /stats as chest menus (works for vanilla clients): heads page, detail page per player, Player filter. */
public final class ChestMenus {
    private static final String[] SORT_KEYS = {"name", "level", "deaths", "playTime"};
    private static final String[] SORT_NAMES = {"Name", "Level", "Tode", "Spielzeit"};

    private ChestMenus() {
    }

    private enum View { MAIN, DETAIL, FILTER }

    /** What one viewer is looking at. The snapshot is built once per /stats. */
    private static final class State {
        final StatsSnapshot snapshot;
        final MinecraftServer server;
        View view = View.MAIN;
        int page;
        int sort;
        PlayerEntry detail;

        State(StatsSnapshot snapshot, MinecraftServer server) {
            this.snapshot = snapshot;
            this.server = server;
        }
    }

    public static void open(ServerPlayer viewer, MinecraftServer server) {
        open(viewer, new State(SnapshotBuilder.get(server), server));
    }

    private static void open(ServerPlayer viewer, State state) {
        Component title = Component.literal(switch (state.view) {
            case MAIN -> "Statistiken";
            case DETAIL -> "Statistiken: " + state.detail.name();
            case FILTER -> "Player";
        });
        viewer.openMenu(new SimpleMenuProvider((id, inventory, player) -> new Menu(id, inventory, viewer, state), title));
    }

    private static final class Menu extends ChestMenu {
        private final SimpleContainer items;
        private final ServerPlayer viewer;
        private final State state;
        /** heads on the current page, in slot order from slot 9 */
        private List<PlayerEntry> shown = List.of();

        Menu(int id, Inventory inventory, ServerPlayer viewer, State state) {
            this(id, inventory, new SimpleContainer(54), viewer, state);
        }

        private Menu(int id, Inventory inventory, SimpleContainer items, ServerPlayer viewer, State state) {
            super(MenuType.GENERIC_9x6, id, inventory, items, 6);
            this.items = items;
            this.viewer = viewer;
            this.state = state;
            render();
        }

        /** Every click is caught: nothing can be taken, shift-clicked or dragged. */
        @Override
        public void clicked(int slot, int button, ContainerInput input, Player player) {
            if (input == ContainerInput.PICKUP && slot >= 0 && slot < 54) click(slot);
            if (viewer.containerMenu == this) sendAllDataToRemote(); // undo what the client predicted
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        private void click(int slot) {
            int head = slot - ChestLayout.FIRST_HEAD_SLOT;
            PlayerEntry clicked = head >= 0 && head < shown.size() ? shown.get(head) : null;
            switch (state.view) {
                case MAIN -> {
                    if (clicked != null) {
                        state.detail = clicked;
                        show(View.DETAIL);
                    } else if (slot == 8) {
                        state.page = 0;
                        show(View.FILTER);
                    } else if (slot == 48) {
                        state.sort = (state.sort + 1) % SORT_KEYS.length;
                        state.page = 0;
                        render();
                    } else {
                        pageOrClose(slot);
                    }
                }
                case DETAIL -> {
                    if (slot == 45) show(View.MAIN);
                    else if (slot == 49) viewer.closeContainer();
                }
                case FILTER -> {
                    if (clicked != null) {
                        Set<UUID> hidden = hidden();
                        if (!hidden.remove(clicked.uuid())) hidden.add(clicked.uuid());
                        try {
                            MikesStatsMod.store().save(viewer.getUUID());
                        } catch (RuntimeException e) {
                            MikesStatsMod.LOGGER.error("Mikes Stats: could not save hidden players", e);
                        }
                        render();
                    } else if (slot == 48) {
                        state.page = 0;
                        show(View.MAIN);
                    } else {
                        pageOrClose(slot);
                    }
                }
            }
        }

        private void pageOrClose(int slot) {
            if (slot == 45 && state.page > 0) {
                state.page--;
                render();
            } else if (slot == 53 && items.getItem(53).is(Items.ARROW)) {
                state.page++;
                render();
            } else if (slot == 49) {
                viewer.closeContainer();
            }
        }

        private void show(View view) {
            state.view = view;
            open(viewer, state);
        }

        private void render() {
            items.clearContent();
            ItemStack pane = new ItemStack(Items.STAINED_GLASS_PANE.pick(DyeColor.GRAY));
            pane.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, TooltipDisplay.DEFAULT.hiddenComponents()));
            for (int i = 0; i < 9; i++) {
                items.setItem(i, pane.copy());
                items.setItem(45 + i, pane.copy());
            }
            items.setItem(49, named(Items.BARRIER, "Schließen"));
            switch (state.view) {
                case MAIN -> renderMain();
                case DETAIL -> renderDetail();
                case FILTER -> renderFilter();
            }
        }

        private void renderMain() {
            StatsSnapshot s = state.snapshot;
            List<PlayerEntry> rows = TableModel.overview(s, hidden(), new SortState(SORT_KEYS[state.sort], false));
            items.setItem(0, named(Items.BOOK, "Server-Infos",
                    text("Seit " + Formats.date(s.serverStart()) + " · " + s.daysSinceStart() + " Tage", ChatFormatting.GRAY),
                    text("Ingame-Tag " + Formats.number(s.ingameDay()), ChatFormatting.GRAY)));
            items.setItem(8, named(Items.NAME_TAG, "Player " + rows.size() + "/" + s.players().size(),
                    text("Klick: Spieler ein- oder ausblenden", ChatFormatting.GRAY)));
            placeHeads(rows, p -> head(p, "Klick: Details"));
            List<Component> sortLines = new ArrayList<>();
            for (int i = 0; i < SORT_NAMES.length; i++) {
                sortLines.add(i == state.sort ? text("▶ " + SORT_NAMES[i], ChatFormatting.WHITE)
                        : text("  " + SORT_NAMES[i], ChatFormatting.GRAY));
            }
            sortLines.add(text("Klick: wechseln", ChatFormatting.DARK_GRAY));
            items.setItem(48, named(Items.HOPPER, "Sortierung: " + SORT_NAMES[state.sort], sortLines.toArray(Component[]::new)));
        }

        private void renderDetail() {
            PlayerEntry p = state.detail;
            items.setItem(4, head(p, null));
            items.setItem(19, top(Items.CLOCK, "Allgemein", p, Tab.GENERAL, "minecraft:custom", "Top 10"));
            items.setItem(20, top(Items.ZOMBIE_HEAD, "Mobs", p, Tab.MOBS, "minecraft:killed", "Getötet – Top 10"));
            items.setItem(21, top(Items.GRASS_BLOCK, "Blöcke", p, Tab.BLOCKS, "minecraft:mined", "Abgebaut – Top 10"));
            items.setItem(22, top(Items.CRAFTING_TABLE, "Items", p, Tab.ITEMS, "minecraft:crafted", "Hergestellt – Top 10"));
            items.setItem(23, top(Items.COMPASS, "Wege", p, Tab.WAYS, "minecraft:custom", "Top 10"));
            items.setItem(24, deaths(p));
            items.setItem(45, named(Items.ARROW, "Zurück"));
        }

        private void renderFilter() {
            List<PlayerEntry> all = TableModel.overview(state.snapshot, Set.of(), new SortState("name", false));
            Set<UUID> hidden = hidden();
            long visible = all.stream().filter(p -> !hidden.contains(p.uuid())).count();
            items.setItem(4, named(Items.NAME_TAG, "Player " + visible + "/" + all.size(),
                    text("Klick auf einen Kopf: ein- oder ausblenden", ChatFormatting.GRAY)));
            placeHeads(all, p -> hidden.contains(p.uuid())
                    ? named(Items.DYE.pick(DyeColor.GRAY), p.name(), text("ausgeblendet", ChatFormatting.DARK_GRAY))
                    : head(p, "sichtbar"));
            items.setItem(48, named(Items.ARROW, "Zurück"));
        }

        private void placeHeads(List<PlayerEntry> players, Function<PlayerEntry, ItemStack> item) {
            int pages = ChestLayout.pages(players.size());
            state.page = Math.min(state.page, pages - 1);
            List<Integer> slots = ChestLayout.headSlots(state.page, players.size());
            shown = new ArrayList<>();
            for (int i = 0; i < slots.size(); i++) {
                PlayerEntry p = players.get(state.page * ChestLayout.HEADS_PER_PAGE + i);
                shown.add(p);
                items.setItem(slots.get(i), item.apply(p));
            }
            if (state.page > 0) items.setItem(45, named(Items.ARROW, "Vorherige Seite"));
            if (state.page + 1 < pages) items.setItem(53, named(Items.ARROW, "Nächste Seite"));
        }

        private Set<UUID> hidden() {
            return MikesStatsMod.store().get(viewer.getUUID()).hidden;
        }

        private ItemStack head(PlayerEntry p, String hint) {
            ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
            ServerPlayer online = state.server.getPlayerList().getPlayer(p.uuid());
            // offline: the client loads the skin by UUID
            stack.set(DataComponents.PROFILE, online != null ? ResolvableProfile.createResolved(online.getGameProfile())
                    : ResolvableProfile.createUnresolved(p.uuid()));
            List<Component> lore = new ArrayList<>();
            lore.add(p.online() ? text("online", ChatFormatting.GREEN)
                    : text("zuletzt online am: " + (p.lastSeen() == null ? "–" : Formats.date(p.lastSeen())), ChatFormatting.GRAY));
            lore.add(value("Level", Formats.number(p.level())));
            lore.add(value("Höchstes (seit Mod)", p.maxLevel() < 0 ? "–" : Formats.number(p.maxLevel())));
            lore.add(value("Tode", Formats.number(TableModel.custom(p, "minecraft:deaths"))));
            lore.add(value("PvP-Kills", Formats.number(TableModel.custom(p, "minecraft:player_kills"))));
            lore.add(value("Spielzeit", Formats.hours(TableModel.custom(p, "minecraft:play_time"))));
            lore.add(value("XP gesamt (seit Mod)", p.xpTotal() < 0 ? "–" : Formats.number(p.xpTotal())));
            if (hint != null) lore.add(text(hint, ChatFormatting.DARK_GRAY));
            stack.set(DataComponents.CUSTOM_NAME, text(p.name(), ChatFormatting.WHITE));
            stack.set(DataComponents.LORE, new ItemLore(lore));
            return stack;
        }

        /** The player's 10 highest values of one tab (first sub-category). */
        private ItemStack top(Item icon, String name, PlayerEntry p, Tab tab, String category, String heading) {
            List<Map.Entry<String, Integer>> best = p.stats().getOrDefault(category, Map.of()).entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .filter(e -> !category.equals("minecraft:custom") || Tabs.belongsTo(tab, e.getKey()))
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .toList();
            List<Component> lore = new ArrayList<>();
            lore.add(text(heading, ChatFormatting.DARK_GRAY));
            for (Map.Entry<String, Integer> e : best) {
                lore.add(Component.translatable(StatNames.key(category, e.getKey()))
                        .withStyle(st -> st.withItalic(false).withColor(ChatFormatting.GRAY))
                        .append(Component.literal(": " + Formats.value(e.getKey(), e.getValue())).withStyle(ChatFormatting.WHITE)));
            }
            if (best.isEmpty()) lore.add(text("Noch nichts", ChatFormatting.GRAY));
            return named(icon, name, lore.toArray(Component[]::new));
        }

        private ItemStack deaths(PlayerEntry p) {
            List<DeathEntry> last = TableModel.deaths(state.snapshot, Set.of(), new SortState("date", false)).stream()
                    .filter(d -> d.player().equals(p.uuid()))
                    .limit(10)
                    .toList();
            List<Component> lore = new ArrayList<>();
            lore.add(value("Tode gesamt", Formats.number(TableModel.custom(p, "minecraft:deaths"))));
            lore.add(text("Letzte Tode (seit Mod)", ChatFormatting.DARK_GRAY));
            for (DeathEntry d : last) {
                // causes start with a space: " fell from a high place", " von Ben getötet"
                lore.add(text(Formats.date(d.date()) + " · Level " + d.level() + " ·", ChatFormatting.GRAY)
                        .append(Texts.fromJson(d.cause(), state.server.registryAccess()).copy().withStyle(ChatFormatting.WHITE)));
            }
            if (last.isEmpty()) lore.add(text("Keine seit Installation", ChatFormatting.GRAY));
            return named(Items.SKELETON_SKULL, "Tode", lore.toArray(Component[]::new));
        }
    }

    private static ItemStack named(Item item, String name, Component... lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, text(name, ChatFormatting.WHITE));
        if (lore.length > 0) stack.set(DataComponents.LORE, new ItemLore(List.of(lore)));
        return stack;
    }

    /** Lore and names are italic purple by default. */
    private static MutableComponent text(String s, ChatFormatting color) {
        return Component.literal(s).withStyle(st -> st.withItalic(false).withColor(color));
    }

    private static MutableComponent value(String label, String value) {
        return text(label + ": ", ChatFormatting.GRAY).append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }
}
