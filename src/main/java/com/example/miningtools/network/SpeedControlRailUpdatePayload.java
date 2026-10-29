package com.example.miningtools.network;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.SpeedControlRailBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpeedControlRailUpdatePayload(
        BlockPos pos,
        int targetGear,
        int directionMode,
        String trainNameFilter
) implements CustomPacketPayload {

    public static final Type<SpeedControlRailUpdatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "speed_control_rail_update"));

    public static final StreamCodec<ByteBuf, SpeedControlRailUpdatePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SpeedControlRailUpdatePayload::pos,
            ByteBufCodecs.VAR_INT,
            SpeedControlRailUpdatePayload::targetGear,
            ByteBufCodecs.VAR_INT,
            SpeedControlRailUpdatePayload::directionMode,
            ByteBufCodecs.stringUtf8(64),
            SpeedControlRailUpdatePayload::trainNameFilter,
            SpeedControlRailUpdatePayload::new
    );

    @Override
    public Type<SpeedControlRailUpdatePayload> type() {
        return TYPE;
    }

    public static void handle(SpeedControlRailUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;
            Level level = player.level();
            BlockPos pos = payload.pos();
            if (!level.isLoaded(pos) || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                return;
            }
            if (level.getBlockEntity(pos) instanceof SpeedControlRailBlockEntity be) {
                be.setTargetGear(payload.targetGear());
                be.setDirectionMode(payload.directionMode());
                be.setTrainNameFilter(payload.trainNameFilter());
            }
        });
    }
}
