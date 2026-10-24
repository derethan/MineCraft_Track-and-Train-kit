package com.example.miningtools.network;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.ChunkLoaderBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ChunkLoaderUpdatePayload(
        BlockPos pos,
        int radius,
        boolean requiresRedstone
) implements CustomPacketPayload {

    public static final Type<ChunkLoaderUpdatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "chunk_loader_update"));

    public static final StreamCodec<ByteBuf, ChunkLoaderUpdatePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ChunkLoaderUpdatePayload::pos,
            ByteBufCodecs.VAR_INT,
            ChunkLoaderUpdatePayload::radius,
            ByteBufCodecs.BOOL,
            ChunkLoaderUpdatePayload::requiresRedstone,
            ChunkLoaderUpdatePayload::new
    );

    @Override
    public Type<ChunkLoaderUpdatePayload> type() {
        return TYPE;
    }

    public static void handle(ChunkLoaderUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) {
                return;
            }
            Level level = player.level();
            BlockPos pos = payload.pos();
            if (!level.isLoaded(pos) || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                return;
            }
            if (level.getBlockEntity(pos) instanceof ChunkLoaderBlockEntity be) {
                be.setRadius(payload.radius());
                be.setRequiresRedstone(payload.requiresRedstone());
            }
        });
    }
}
