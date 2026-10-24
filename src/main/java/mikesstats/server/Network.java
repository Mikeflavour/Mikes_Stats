package mikesstats.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mikesstats.MikesStatsMod;
import mikesstats.core.SnapshotCodec;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client mod asks, server answers with the whole snapshot (gzip JSON, under 1 MB). */
public final class Network {
    private static final Map<UUID, Long> lastAnswer = new HashMap<>();

    private Network() {
    }

    public record RequestStatsPayload() implements CustomPacketPayload {
        public static final Type<RequestStatsPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(MikesStatsMod.MOD_ID, "request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestStatsPayload> CODEC =
                StreamCodec.unit(new RequestStatsPayload());

        @Override
        public Type<RequestStatsPayload> type() {
            return TYPE;
        }
    }

    public record StatsPayload(byte[] data) implements CustomPacketPayload {
        public static final Type<StatsPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(MikesStatsMod.MOD_ID, "stats"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StatsPayload> CODEC =
                ByteBufCodecs.BYTE_ARRAY.map(StatsPayload::new, StatsPayload::data).cast();

        @Override
        public Type<StatsPayload> type() {
            return TYPE;
        }
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(RequestStatsPayload.TYPE, RequestStatsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StatsPayload.TYPE, StatsPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RequestStatsPayload.TYPE, (payload, context) -> {
            long now = System.currentTimeMillis();
            Long last = lastAnswer.get(context.player().getUUID());
            if (last != null && now - last < 1000) return; // at most one answer per second per player
            lastAnswer.put(context.player().getUUID(), now);
            ServerPlayNetworking.send(context.player(),
                    new StatsPayload(SnapshotCodec.encode(SnapshotBuilder.get(context.server()))));
        });
    }
}
