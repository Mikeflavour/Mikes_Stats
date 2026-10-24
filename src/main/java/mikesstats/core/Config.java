package mikesstats.core;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/** config/mikes_stats.json. startDate = first day of the server, shown as "Seit TT.MM.JJJJ". */
public record Config(LocalDate startDate) {
    public static final Config DEFAULT = new Config(LocalDate.of(2026, 4, 27));

    /** Missing file: write the default. Broken file: use the default and leave the file for the admin to fix. */
    public static Config load(Path file) {
        if (!Files.exists(file)) {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, SnapshotCodec.GSON.toJson(DEFAULT));
            } catch (IOException ignored) {
                // read-only config dir: the default still works
            }
            return DEFAULT;
        }
        try (Reader r = Files.newBufferedReader(file)) {
            Config config = SnapshotCodec.GSON.fromJson(r, Config.class);
            return config == null || config.startDate() == null ? DEFAULT : config;
        } catch (IOException | RuntimeException e) {
            return DEFAULT;
        }
    }
}
