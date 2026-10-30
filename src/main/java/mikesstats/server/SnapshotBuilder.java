package mikesstats.server;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import mikesstats.MikesStatsMod;
import mikesstats.core.Config;
import mikesstats.core.Formats;
import mikesstats.core.ModData;
import mikesstats.core.ModDataStore;
import mikesstats.core.StatsSnapshot;
import mikesstats.core.StatsSnapshot.DeathEntry;
import mikesstats.core.StatsSnapshot.PlayerEntry;
import mikesstats.core.VanillaStatsReader;
import mikesstats.mixin.ServerStatsCounterAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ARGB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;
import net.minecraft.world.level.storage.LevelResource;

/** Everyone who ever played: online players live, offline players from their files. Runs on the server thread. */
public final class SnapshotBuilder {
    private static StatsSnapshot cached;
    private static long cachedAt;

    private SnapshotBuilder() {
    }

    /** Everyone asking within a second gets the same snapshot: /stats and the client mod share one disk read. */
    public static StatsSnapshot get(MinecraftServer server) {
        long now = System.currentTimeMillis();
        if (cached == null || now - cachedAt >= 1000) {
            cached = build(server);
            cachedAt = now;
        }
        return cached;
    }

    private static StatsSnapshot build(MinecraftServer server) {
        ModDataStore store = MikesStatsMod.store();
        Path statsDir = server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
        Path dataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);

        Set<UUID> uuids = new LinkedHashSet<>(store.all().keySet());
        uuids.addAll(uuidsIn(statsDir, ".json"));
        server.getPlayerList().getPlayers().forEach(p -> uuids.add(p.getUUID()));

        Map<UUID, String> names = new HashMap<>();
        for (UUID uuid : uuids) {
            ServerPlayer online = server.getPlayerList().getPlayer(uuid);
            String known = online != null ? online.getGameProfile().name() : store.get(uuid).name;
            if (known == null) {
                known = server.services().nameToIdCache().get(uuid).map(NameAndId::name).orElse(null);
            }
            names.put(uuid, VanillaStatsReader.displayName(uuid, known));
        }

        List<PlayerEntry> players = new ArrayList<>();
        List<DeathEntry> deaths = new ArrayList<>();
        for (UUID uuid : uuids) {
            ServerPlayer online = server.getPlayerList().getPlayer(uuid);
            ModData data = store.get(uuid);
            Map<String, Map<String, Integer>> stats;
            int level;
            int color;
            LocalDate lastSeen = null;
            if (online != null) {
                data.seeLevel(online.experienceLevel);
                stats = VanillaStatsReader.parse(((ServerStatsCounterAccessor) online.getStats()).mikesstats$toJson());
                level = online.experienceLevel;
                color = online.waypointIcon().cloneAndAssignStyle(online).color.orElse(defaultColor(uuid));
            } else {
                Path datFile = dataDir.resolve(uuid + ".dat");
                CompoundTag tag = readPlayerData(datFile);
                stats = VanillaStatsReader.read(statsDir.resolve(uuid + ".json"));
                level = tag.getIntOr("XpLevel", 0);
                color = tag.getCompoundOrEmpty("locator_bar_icon").getInt("color")
                        .or(() -> teamColor(server, names.get(uuid)))
                        .orElse(defaultColor(uuid));
                lastSeen = data.lastSeen != null ? data.lastSeen : modified(datFile, statsDir.resolve(uuid + ".json"));
            }
            Map<String, Integer> victims = new HashMap<>();
            data.pvpVictims.forEach((victim, count) ->
                    victims.merge(names.getOrDefault(victim, VanillaStatsReader.displayName(victim, null)), count, Integer::sum));
            players.add(new PlayerEntry(uuid, names.get(uuid), online != null, lastSeen, level, data.maxLevel,
                    data.xpTotal, color & 0xFFFFFF, victims, stats));
            data.deaths.forEach(d -> deaths.add(new DeathEntry(d.date(), uuid, d.level(), d.cause())));
        }

        Config config = MikesStatsMod.config();
        return new StatsSnapshot(config.startDate(), Formats.daysSince(config.startDate(), LocalDate.now()),
                Formats.ingameDay(server.overworld().getGameTime()), players, deaths);
    }

    /** Vanilla LocatorBar: brightness 0.9 of the UUID hash, unless /waypoint or a team colour set one. */
    private static int defaultColor(UUID uuid) {
        return ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F);
    }

    /** Same as Waypoint.Icon.cloneAndAssignStyle for players who are offline. */
    private static Optional<Integer> teamColor(MinecraftServer server, String name) {
        PlayerTeam team = server.getScoreboard().getPlayersTeam(name);
        return team == null ? Optional.empty()
                : team.getColor().map(c -> c == TeamColor.BLACK ? -13619152 : c.rgb());
    }

    private static CompoundTag readPlayerData(Path file) {
        if (!Files.isRegularFile(file)) return new CompoundTag();
        try {
            return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (IOException | RuntimeException e) {
            MikesStatsMod.LOGGER.warn("Mikes Stats: could not read {}", file, e);
            return new CompoundTag();
        }
    }

    /** Fallback for "zuletzt online" before the mod was installed: when the game last wrote the player's files. */
    private static LocalDate modified(Path... files) {
        for (Path f : files) {
            try {
                return LocalDate.ofInstant(Files.getLastModifiedTime(f).toInstant(), ZoneId.systemDefault());
            } catch (IOException ignored) {
                // try the next file
            }
        }
        return null;
    }

    private static List<UUID> uuidsIn(Path dir, String suffix) {
        List<UUID> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) return result;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*" + suffix)) {
            for (Path f : files) {
                String name = f.getFileName().toString();
                try {
                    result.add(UUID.fromString(name.substring(0, name.length() - suffix.length())));
                } catch (IllegalArgumentException ignored) {
                    // old name-based file
                }
            }
        } catch (IOException e) {
            MikesStatsMod.LOGGER.warn("Mikes Stats: could not list {}", dir, e);
        }
        return result;
    }
}
