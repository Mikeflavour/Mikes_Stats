package mikesstats;

import com.mojang.logging.LogUtils;
import mikesstats.core.Config;
import mikesstats.core.ModDataStore;
import mikesstats.server.Tracking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

public class MikesStatsMod implements ModInitializer {
    public static final String MOD_ID = "mikes_stats";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static Config config = Config.DEFAULT;
    private static ModDataStore store;

    public static Config config() {
        return config;
    }

    /** world/mikes_stats, set while a server runs */
    public static ModDataStore store() {
        return store;
    }

    @Override
    public void onInitialize() {
        config = Config.load(FabricLoader.getInstance().getConfigDir().resolve("mikes_stats.json"));
        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                store = new ModDataStore(server.getWorldPath(LevelResource.ROOT).resolve("mikes_stats")));
        Tracking.register();
    }
}
