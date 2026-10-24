package mikesstats.core;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/** Reads vanilla files (read-only). Missing or broken files give an empty result, never an exception. */
public final class VanillaStatsReader {
    private VanillaStatsReader() {
    }

    /** players/stats/&lt;uuid&gt;.json -> category id -> stat id -> value */
    public static Map<String, Map<String, Integer>> read(Path statsFile) {
        try (Reader r = Files.newBufferedReader(statsFile)) {
            return parse(JsonParser.parseReader(r));
        } catch (IOException | RuntimeException e) {
            return Map.of();
        }
    }

    /** Same format as the file: {"stats":{category:{stat:value}}}. */
    public static Map<String, Map<String, Integer>> parse(JsonElement root) {
        Map<String, Map<String, Integer>> result = new HashMap<>();
        try {
            for (var category : root.getAsJsonObject().getAsJsonObject("stats").entrySet()) {
                Map<String, Integer> values = new HashMap<>();
                for (var stat : category.getValue().getAsJsonObject().entrySet()) {
                    values.put(stat.getKey(), stat.getValue().getAsInt());
                }
                result.put(category.getKey(), values);
            }
            return result;
        } catch (RuntimeException e) {
            return Map.of();
        }
    }

    /** Known name, or the first 8 characters of the UUID when nobody knows the name. */
    public static String displayName(UUID uuid, String knownName) {
        return knownName == null || knownName.isBlank() ? uuid.toString().substring(0, 8) : knownName;
    }
}
