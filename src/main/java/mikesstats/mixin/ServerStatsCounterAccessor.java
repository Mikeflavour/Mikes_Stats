package mikesstats.mixin;

import com.google.gson.JsonElement;
import net.minecraft.stats.ServerStatsCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Live stats of online players in the same JSON format as their stats file. */
@Mixin(ServerStatsCounter.class)
public interface ServerStatsCounterAccessor {
    @Invoker("toJson")
    JsonElement mikesstats$toJson();
}
