package mikesstats.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/** StatsSnapshot <-> gzip JSON bytes for the network payload. */
public final class SnapshotCodec {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
                @Override
                public void write(JsonWriter out, LocalDate value) throws IOException {
                    if (value == null) out.nullValue();
                    else out.value(value.toString());
                }

                @Override
                public LocalDate read(JsonReader in) throws IOException {
                    if (in.peek() == JsonToken.NULL) {
                        in.nextNull();
                        return null;
                    }
                    return LocalDate.parse(in.nextString());
                }
            })
            .create();

    private SnapshotCodec() {
    }

    public static byte[] encode(StatsSnapshot snapshot) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Writer w = new OutputStreamWriter(new GZIPOutputStream(bytes), StandardCharsets.UTF_8)) {
            GSON.toJson(snapshot, w);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static StatsSnapshot decode(byte[] data) {
        try (var r = new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(data)), StandardCharsets.UTF_8)) {
            return GSON.fromJson(r, StatsSnapshot.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
