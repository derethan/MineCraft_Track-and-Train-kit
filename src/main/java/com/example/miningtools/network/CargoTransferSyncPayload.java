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

public record CargoTransferSyncPayload(
        int stationRole,
        String itemWhitelist,
        String itemBlacklist,
        String cartWhitelist,
        String cartBlacklist
) implements CustomPacketPayload {

    public static final Type<CargoTransferSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "cargo_transfer_sync"));

    public static final StreamCodec<ByteBuf, CargoTransferSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CargoTransferSyncPayload::stationRole,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferSyncPayload::itemWhitelist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferSyncPayload::itemBlacklist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferSyncPayload::cartWhitelist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferSyncPayload::cartBlacklist,
            CargoTransferSyncPayload::new
    );

    public static @Nullable Consumer<CargoTransferSyncPayload> CLIENT_HANDLER = null;

    @Override
    public Type<CargoTransferSyncPayload> type() {
        return TYPE;
    }

    public static void handle(CargoTransferSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (CLIENT_HANDLER != null) {
                CLIENT_HANDLER.accept(payload);
            }
        });
    }
}
