package mikesstats.core;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Reads vanilla files (read-only). Missing or broken files give an empty result, never an exception. */
public final class VanillaStatsReader {
    private VanillaStatsReader() {
    }

    /** world/stats/&lt;uuid&gt;.json -> category id -> stat id -> value */
    public static Map<String, Map<String, Integer>> read(Path statsFile) {
        Map<String, Map<String, Integer>> result = new HashMap<>();
        try (Reader r = Files.newBufferedReader(statsFile)) {
            JsonObject stats = JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("stats");
            for (var category : stats.entrySet()) {
                Map<String, Integer> values = new HashMap<>();
                for (var stat : category.getValue().getAsJsonObject().entrySet()) {
                    values.put(stat.getKey(), stat.getValue().getAsInt());
                }
                result.put(category.getKey(), values);
            }
            return result;
        } catch (IOException | RuntimeException e) {
            return Map.of();
        }
    }

    /** usercache.json -> uuid -> last known name */
    public static Map<UUID, String> readUserCache(Path usercacheJson) {
        Map<UUID, String> names = new HashMap<>();
        try (Reader r = Files.newBufferedReader(usercacheJson)) {
            for (JsonElement e : JsonParser.parseReader(r).getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                names.put(UUID.fromString(o.get("uuid").getAsString()), o.get("name").getAsString());
            }
            return names;
        } catch (IOException | RuntimeException e) {
            return Map.of();
        }
    }
}
