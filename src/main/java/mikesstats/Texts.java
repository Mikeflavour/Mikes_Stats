package mikesstats;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/** Death causes are stored as component JSON so every viewer sees them in their own language. */
public final class Texts {
    private Texts() {
    }

    public static String toJson(Component text, HolderLookup.Provider registries) {
        return ComponentSerialization.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), text)
                .getOrThrow().toString();
    }

    /** Anything that isn't component JSON is shown as plain text. */
    public static Component fromJson(String json, HolderLookup.Provider registries) {
        try {
            return ComponentSerialization.CODEC
                    .parse(registries.createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json))
                    .result().orElseGet(() -> Component.literal(json));
        } catch (RuntimeException e) {
            return Component.literal(json);
        }
    }
}
