package mikesstats.server;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import mikesstats.MikesStatsMod;
import mikesstats.Texts;
import mikesstats.core.ModData;
import mikesstats.core.ModDataStore;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/** Counts what vanilla doesn't keep: highest level, total XP, deaths with level and cause, PvP victims, last online. */
public final class Tracking {
    private Tracking() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> guard("join", () -> onJoin(handler.player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> guard("leave", () -> onLeave(handler.player)));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) guard("death", () -> onDeath(player, source));
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> guard("stop", () -> {
            ModDataStore store = MikesStatsMod.store();
            if (store != null) store.all().keySet().forEach(Tracking::save);
        }));
    }

    /** These run inside vanilla (death, shutdown, XP): a bug of ours must never crash the server or skip its save. */
    private static void guard(String what, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            MikesStatsMod.LOGGER.error("Mikes Stats: {} tracking failed", what, e);
        }
    }

    /** from PlayerXpMixin */
    public static void onXpGained(ServerPlayer player, int amount) {
        guard("xp", () -> {
            ModData data = data(player);
            if (data != null) data.addXp(amount);
        });
    }

    /** from PlayerXpMixin */
    public static void onLevelChanged(ServerPlayer player) {
        guard("level", () -> {
            ModData data = data(player);
            if (data != null) data.seeLevel(player.experienceLevel);
        });
    }

    private static void onJoin(ServerPlayer player) {
        ModData data = data(player);
        if (data == null) return;
        data.name = player.getGameProfile().name();
        data.seeLevel(player.experienceLevel);
        save(player.getUUID());
    }

    private static void onLeave(ServerPlayer player) {
        ModData data = data(player);
        if (data == null) return;
        data.lastSeen = LocalDate.now();
        data.seeLevel(player.experienceLevel);
        save(player.getUUID());
    }

    private static void onDeath(ServerPlayer victim, DamageSource source) {
        ModData data = data(victim);
        if (data == null) return;
        Component cause;
        if (victim.getKillCredit() instanceof ServerPlayer killer && killer != victim) {
            cause = Component.literal(" von " + killer.getGameProfile().name() + " getötet");
            data(killer).pvpVictims.merge(victim.getUUID(), 1, Integer::sum);
            save(killer.getUUID());
        } else {
            cause = withoutVictimName(source.getLocalizedDeathMessage(victim));
        }
        data.seeLevel(victim.experienceLevel);
        data.addDeath(new ModData.DeathRecord(LocalDate.now(), victim.experienceLevel,
                Texts.toJson(cause, victim.registryAccess())));
        save(victim.getUUID());
    }

    /** "%1$s fell from a high place" -> " fell from a high place": the Tode list already shows who died. */
    private static Component withoutVictimName(Component message) {
        if (!(message.getContents() instanceof TranslatableContents t) || t.getArgs().length == 0) return message;
        Object[] args = Arrays.copyOf(t.getArgs(), t.getArgs().length);
        args[0] = "";
        return Component.translatable(t.getKey(), args);
    }

    private static ModData data(ServerPlayer player) {
        ModDataStore store = MikesStatsMod.store();
        return store == null ? null : store.get(player.getUUID());
    }

    private static void save(UUID uuid) {
        try {
            MikesStatsMod.store().save(uuid);
        } catch (RuntimeException e) {
            MikesStatsMod.LOGGER.error("Mikes Stats: could not save data of {}", uuid, e);
        }
    }
}
