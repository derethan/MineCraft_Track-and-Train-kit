package com.example.miningtools.network;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FluidLoaderSyncPayload(
        int stationRole,
        String fluidWhitelist,
        String fluidBlacklist,
        String cartWhitelist,
        String cartBlacklist
) implements CustomPacketPayload {

    public static final Type<FluidLoaderSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "fluid_loader_sync"));

    public static final StreamCodec<ByteBuf, FluidLoaderSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            FluidLoaderSyncPayload::stationRole,
            ByteBufCodecs.stringUtf8(128),
            FluidLoaderSyncPayload::fluidWhitelist,
            ByteBufCodecs.stringUtf8(128),
            FluidLoaderSyncPayload::fluidBlacklist,
            ByteBufCodecs.stringUtf8(128),
            FluidLoaderSyncPayload::cartWhitelist,
            ByteBufCodecs.stringUtf8(128),
            FluidLoaderSyncPayload::cartBlacklist,
            FluidLoaderSyncPayload::new
    );

    public static @Nullable Consumer<FluidLoaderSyncPayload> CLIENT_HANDLER = null;

    @Override
    public Type<FluidLoaderSyncPayload> type() {
        return TYPE;
    }

    public static void handle(FluidLoaderSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (CLIENT_HANDLER != null) {
                CLIENT_HANDLER.accept(payload);
            }
        });
    }
}
