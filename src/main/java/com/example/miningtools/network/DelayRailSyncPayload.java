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

public record DelayRailSyncPayload(
        BlockPos pos,
        boolean timerEnabled,
        int delaySeconds,
        int releaseDirection,
        String trainNameFilter,
        int remainingTicks
) implements CustomPacketPayload {

    public static final Type<DelayRailSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "delay_rail_sync"));

    public static final StreamCodec<ByteBuf, DelayRailSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            DelayRailSyncPayload::pos,
            ByteBufCodecs.BOOL,
            DelayRailSyncPayload::timerEnabled,
            ByteBufCodecs.VAR_INT,
            DelayRailSyncPayload::delaySeconds,
            ByteBufCodecs.VAR_INT,
            DelayRailSyncPayload::releaseDirection,
            ByteBufCodecs.stringUtf8(64),
            DelayRailSyncPayload::trainNameFilter,
            ByteBufCodecs.VAR_INT,
            DelayRailSyncPayload::remainingTicks,
            DelayRailSyncPayload::new
    );

    public static @Nullable Consumer<DelayRailSyncPayload> CLIENT_HANDLER = null;

    @Override
    public Type<DelayRailSyncPayload> type() {
        return TYPE;
    }

    public static void handle(DelayRailSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (CLIENT_HANDLER != null) {
                CLIENT_HANDLER.accept(payload);
            }
        });
    }
}
