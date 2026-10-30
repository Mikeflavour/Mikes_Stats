package mikesstats.client;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import mikesstats.MikesStatsMod;
import mikesstats.core.SnapshotCodec;
import net.fabricmc.loader.api.FabricLoader;

/** config/mikes_stats_client.json: who this player hid with the "Player" button. Only on this PC. */
public final class ClientConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mikes_stats_client.json");
    private static ClientConfig instance;

    public Set<UUID> hidden = new HashSet<>();

    public static ClientConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static ClientConfig load() {
        try (Reader r = Files.newBufferedReader(FILE)) {
            ClientConfig config = SnapshotCodec.GSON.fromJson(r, ClientConfig.class);
            if (config != null && config.hidden != null) return config;
        } catch (IOException | RuntimeException ignored) {
            // missing or broken: start with nobody hidden
        }
        return new ClientConfig();
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, SnapshotCodec.GSON.toJson(this));
        } catch (IOException e) {
            MikesStatsMod.LOGGER.warn("Mikes Stats: could not save {}", FILE, e);
        }
    }
}
