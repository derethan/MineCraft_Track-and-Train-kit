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

public record SpeedControlRailSyncPayload(
        BlockPos pos,
        int targetGear,
        int directionMode,
        String trainNameFilter
) implements CustomPacketPayload {

    public static final Type<SpeedControlRailSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "speed_control_rail_sync"));

    public static final StreamCodec<ByteBuf, SpeedControlRailSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SpeedControlRailSyncPayload::pos,
            ByteBufCodecs.VAR_INT,
            SpeedControlRailSyncPayload::targetGear,
            ByteBufCodecs.VAR_INT,
            SpeedControlRailSyncPayload::directionMode,
            ByteBufCodecs.stringUtf8(64),
            SpeedControlRailSyncPayload::trainNameFilter,
            SpeedControlRailSyncPayload::new
    );

    public static @Nullable Consumer<SpeedControlRailSyncPayload> CLIENT_HANDLER = null;

    @Override
    public Type<SpeedControlRailSyncPayload> type() {
        return TYPE;
    }

    public static void handle(SpeedControlRailSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (CLIENT_HANDLER != null) {
                CLIENT_HANDLER.accept(payload);
            }
        });
    }
}
