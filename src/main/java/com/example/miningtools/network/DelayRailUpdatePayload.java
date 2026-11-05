package com.example.miningtools.network;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.DelayRailBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DelayRailUpdatePayload(
        BlockPos pos,
        boolean timerEnabled,
        int delaySeconds,
        int releaseDirection,
        String trainNameFilter
) implements CustomPacketPayload {

    public static final Type<DelayRailUpdatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "delay_rail_update"));

    public static final StreamCodec<ByteBuf, DelayRailUpdatePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            DelayRailUpdatePayload::pos,
            ByteBufCodecs.BOOL,
            DelayRailUpdatePayload::timerEnabled,
            ByteBufCodecs.VAR_INT,
            DelayRailUpdatePayload::delaySeconds,
            ByteBufCodecs.VAR_INT,
            DelayRailUpdatePayload::releaseDirection,
            ByteBufCodecs.stringUtf8(64),
            DelayRailUpdatePayload::trainNameFilter,
            DelayRailUpdatePayload::new
    );

    @Override
    public Type<DelayRailUpdatePayload> type() {
        return TYPE;
    }

    public static void handle(DelayRailUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;
            Level level = player.level();
            BlockPos pos = payload.pos();
            if (!level.isLoaded(pos) || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                return;
            }
            if (level.getBlockEntity(pos) instanceof DelayRailBlockEntity be) {
                be.setTimerEnabled(payload.timerEnabled());
                be.setDelaySeconds(payload.delaySeconds());
                be.setReleaseDirection(payload.releaseDirection());
                be.setTrainNameFilter(payload.trainNameFilter());
            }
        });
    }
}
