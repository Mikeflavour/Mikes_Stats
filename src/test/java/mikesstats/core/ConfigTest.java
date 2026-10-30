package mikesstats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {
    @TempDir
    Path dir;

    @Test
    void missingFile_writesDefaultStartDate() throws IOException {
        Path file = dir.resolve("mikes_stats.json");
        assertEquals(LocalDate.of(2026, 4, 27), Config.load(file).startDate());
        assertTrue(Files.readString(file).contains("2026-04-27"));
    }

    @Test
    void existingFile_isRead() throws IOException {
        Path file = Files.writeString(dir.resolve("mikes_stats.json"), "{\"startDate\":\"2026-05-01\"}");
        assertEquals(LocalDate.of(2026, 5, 1), Config.load(file).startDate());
    }

    @Test
    void brokenFile_usesDefaultAndKeepsFile() throws IOException {
        Path file = Files.writeString(dir.resolve("mikes_stats.json"), "{kaputt");
        assertEquals(LocalDate.of(2026, 4, 27), Config.load(file).startDate());
        assertEquals("{kaputt", Files.readString(file));
    }
}
