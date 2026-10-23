package com.example.miningtools.network;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EntityTransferSyncPayload(
        BlockPos pos,
        boolean isUnloader,
        String whitelist,
        String blacklist,
        int ageFilter,
        int range
) implements CustomPacketPayload {

    public static final Type<EntityTransferSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "entity_transfer_sync"));

    public static final StreamCodec<ByteBuf, EntityTransferSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            EntityTransferSyncPayload::pos,
            ByteBufCodecs.BOOL,
            EntityTransferSyncPayload::isUnloader,
            ByteBufCodecs.STRING_UTF8,
            EntityTransferSyncPayload::whitelist,
            ByteBufCodecs.STRING_UTF8,
            EntityTransferSyncPayload::blacklist,
            ByteBufCodecs.VAR_INT,
            EntityTransferSyncPayload::ageFilter,
            ByteBufCodecs.VAR_INT,
            EntityTransferSyncPayload::range,
            EntityTransferSyncPayload::new
    );

    public static @Nullable Consumer<EntityTransferSyncPayload> CLIENT_HANDLER = null;

    @Override
    public Type<EntityTransferSyncPayload> type() {
        return TYPE;
    }

    public static void handle(EntityTransferSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (CLIENT_HANDLER != null) {
                CLIENT_HANDLER.accept(payload);
            }
        });
    }
}
