package mikesstats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VanillaStatsReaderTest {
    @TempDir
    Path dir;

    @Test
    void read_sampleFile() throws URISyntaxException {
        Path sample = Path.of(getClass().getResource("/stats/sample.json").toURI());
        Map<String, Map<String, Integer>> result = VanillaStatsReader.read(sample);
        assertEquals(12, result.get("minecraft:mined").get("minecraft:stone"));
        assertEquals(3, result.get("minecraft:custom").get("minecraft:deaths"));
    }

    @Test
    void read_missingFile_returnsEmpty() {
        assertTrue(VanillaStatsReader.read(dir.resolve("nope.json")).isEmpty());
    }

    @Test
    void read_corruptFile_returnsEmpty() throws IOException {
        Path f = Files.writeString(dir.resolve("bad.json"), "{\"stats\":");
        assertTrue(VanillaStatsReader.read(f).isEmpty());
    }

    @Test
    void displayName_missingName_isShortUuid() {
        UUID ben = UUID.fromString("0f0e0d0c-0b0a-0908-0706-050403020100");
        assertEquals("0f0e0d0c", VanillaStatsReader.displayName(ben, null));
        assertEquals("0f0e0d0c", VanillaStatsReader.displayName(ben, " "));
        assertEquals("Ben", VanillaStatsReader.displayName(ben, "Ben"));
    }
}
