package mikesstats.client;

import mikesstats.MikesStatsMod;
import mikesstats.core.SnapshotCodec;
import mikesstats.server.Network;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class MikesStatsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Network.StatsPayload.TYPE, (payload, context) -> {
            try {
                StatsScreen.latest = SnapshotCodec.decode(payload.data());
            } catch (RuntimeException e) {
                MikesStatsMod.LOGGER.warn("Mikes Stats: could not read stats from server", e);
            }
        });
        PauseButton.register();
    }
}
