package mikesstats.core;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonParseException;

/** Loads ModData lazily, one file per player. A broken file is kept as .bak and the player starts empty. */
public final class ModDataStore {
    private final Path dir;
    private final Map<UUID, ModData> cache = new HashMap<>();

    public ModDataStore(Path dir) {
        this.dir = dir;
    }

    public ModData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    public void save(UUID uuid) {
        ModData data = cache.get(uuid);
        if (data == null) return;
        try {
            Files.createDirectories(dir);
            Path tmp = dir.resolve(uuid + ".json.tmp");
            Files.writeString(tmp, SnapshotCodec.GSON.toJson(data));
            try {
                Files.move(tmp, file(uuid), REPLACE_EXISTING, ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file(uuid), REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Every player that has a file, plus anything already loaded. */
    public Map<UUID, ModData> all() {
        if (Files.isDirectory(dir)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.json")) {
                for (Path f : files) {
                    String name = f.getFileName().toString();
                    try {
                        get(UUID.fromString(name.substring(0, name.length() - ".json".length())));
                    } catch (IllegalArgumentException ignored) {
                        // not a player file
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return cache;
    }

    private Path file(UUID uuid) {
        return dir.resolve(uuid + ".json");
    }

    private ModData load(UUID uuid) {
        Path f = file(uuid);
        if (!Files.exists(f)) return new ModData();
        try (Reader r = Files.newBufferedReader(f)) {
            ModData data = SnapshotCodec.GSON.fromJson(r, ModData.class);
            if (data == null) throw new JsonParseException("empty file");
            // a hand-edited "deaths": null overwrites the field default
            if (data.deaths == null) data.deaths = new ArrayList<>();
            if (data.pvpVictims == null) data.pvpVictims = new HashMap<>();
            if (data.hidden == null) data.hidden = new HashSet<>();
            return data;
        } catch (IOException | RuntimeException e) {
            try {
                Files.move(f, f.resolveSibling(f.getFileName() + ".bak"), REPLACE_EXISTING);
            } catch (IOException ignored) {
                // keep going with empty data; the broken file stays and is retried next start
            }
            return new ModData();
        }
    }
}
