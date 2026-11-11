package com.example.miningtools.network;

import java.util.ArrayList;
import java.util.List;

import com.example.miningtools.MiningTools;
import com.example.miningtools.compat.journeymap.JourneyMapTrainPlugin;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Synchronizes active force-loaded chunk anchors (stationary Industrial Chunk Anchors
 * and mobile Rail Chunk Anchors / Anchor Cabooses / Active Consists) to clients for
 * rendering color-coded chunk overlays on JourneyMap.
 */
public record ChunkMapSyncPayload(List<ChunkAnchorEntry> entries) implements CustomPacketPayload {

    public static final int TYPE_STATIC_ANCHOR = 0;
    public static final int TYPE_RAIL_ANCHOR = 1;

    public static final Type<ChunkMapSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "chunk_map_sync"));

    public static final StreamCodec<ByteBuf, ChunkAnchorEntry> ENTRY_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ChunkAnchorEntry decode(ByteBuf buf) {
            String ownerKey = ByteBufCodecs.STRING_UTF8.decode(buf);
            String dimension = ByteBufCodecs.STRING_UTF8.decode(buf);
            int anchorType = ByteBufCodecs.VAR_INT.decode(buf);
            BlockPos centerPos = BlockPos.STREAM_CODEC.decode(buf);
            String title = ByteBufCodecs.STRING_UTF8.decode(buf);
            int count = ByteBufCodecs.VAR_INT.decode(buf);
            List<Long> packedChunks = new ArrayList<>(Math.min(count, 256));
            for (int i = 0; i < count; i++) {
                packedChunks.add(buf.readLong());
            }
            return new ChunkAnchorEntry(ownerKey, dimension, anchorType, centerPos, title, packedChunks);
        }

        @Override
        public void encode(ByteBuf buf, ChunkAnchorEntry value) {
            ByteBufCodecs.STRING_UTF8.encode(buf, value.ownerKey());
            ByteBufCodecs.STRING_UTF8.encode(buf, value.dimension());
            ByteBufCodecs.VAR_INT.encode(buf, value.anchorType());
            BlockPos.STREAM_CODEC.encode(buf, value.centerPos());
            ByteBufCodecs.STRING_UTF8.encode(buf, value.title());
            List<Long> chunks = value.packedChunks();
            ByteBufCodecs.VAR_INT.encode(buf, chunks.size());
            for (long packed : chunks) {
                buf.writeLong(packed);
            }
        }
    };

    public static final StreamCodec<ByteBuf, ChunkMapSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list()),
            ChunkMapSyncPayload::entries,
            ChunkMapSyncPayload::new
    );

    @Override
    public Type<ChunkMapSyncPayload> type() {
        return TYPE;
    }

    public static void handle(ChunkMapSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                JourneyMapTrainPlugin.handleChunkMapSync(payload);
            } catch (Throwable ignored) {
            }
        });
    }

    public record ChunkAnchorEntry(
            String ownerKey,
            String dimension,
            int anchorType,
            BlockPos centerPos,
            String title,
            List<Long> packedChunks
    ) {}
}
