package com.example.miningtools.network;

import java.util.Optional;
import java.util.UUID;

import com.example.miningtools.MiningTools;
import com.example.miningtools.util.TrainChainingUtil;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TrainLinkSyncPayload(
        UUID cartUUID,
        Optional<UUID> parentUUID,
        Optional<UUID> childUUID
) implements CustomPacketPayload {

    public static final StreamCodec<ByteBuf, UUID> UUID_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public UUID decode(ByteBuf buffer) {
            return new UUID(buffer.readLong(), buffer.readLong());
        }

        @Override
        public void encode(ByteBuf buffer, UUID value) {
            buffer.writeLong(value.getMostSignificantBits());
            buffer.writeLong(value.getLeastSignificantBits());
        }
    };

    public static final Type<TrainLinkSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "train_link_sync"));

    public static final StreamCodec<ByteBuf, TrainLinkSyncPayload> STREAM_CODEC = StreamCodec.composite(
            UUID_STREAM_CODEC,
            TrainLinkSyncPayload::cartUUID,
            ByteBufCodecs.optional(UUID_STREAM_CODEC),
            TrainLinkSyncPayload::parentUUID,
            ByteBufCodecs.optional(UUID_STREAM_CODEC),
            TrainLinkSyncPayload::childUUID,
            TrainLinkSyncPayload::new
    );

    @Override
    public Type<TrainLinkSyncPayload> type() {
        return TYPE;
    }

    public static void handle(TrainLinkSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            TrainChainingUtil.updateClientLink(
                    payload.cartUUID(),
                    payload.parentUUID().orElse(null),
                    payload.childUUID().orElse(null)
            );
        });
    }
}
