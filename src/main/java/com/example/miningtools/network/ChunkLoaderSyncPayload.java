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

public record ChunkLoaderSyncPayload(
        BlockPos pos,
        int radius,
        boolean requiresRedstone,
        boolean active,
        int forcedChunkCount
) implements CustomPacketPayload {

    public static final Type<ChunkLoaderSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "chunk_loader_sync"));

    public static final StreamCodec<ByteBuf, ChunkLoaderSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ChunkLoaderSyncPayload::pos,
            ByteBufCodecs.VAR_INT,
            ChunkLoaderSyncPayload::radius,
            ByteBufCodecs.BOOL,
            ChunkLoaderSyncPayload::requiresRedstone,
            ByteBufCodecs.BOOL,
            ChunkLoaderSyncPayload::active,
            ByteBufCodecs.VAR_INT,
            ChunkLoaderSyncPayload::forcedChunkCount,
            ChunkLoaderSyncPayload::new
    );

    public static @Nullable Consumer<ChunkLoaderSyncPayload> CLIENT_HANDLER = null;

    @Override
    public Type<ChunkLoaderSyncPayload> type() {
        return TYPE;
    }

    public static void handle(ChunkLoaderSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (CLIENT_HANDLER != null) {
                CLIENT_HANDLER.accept(payload);
            }
        });
    }
}
