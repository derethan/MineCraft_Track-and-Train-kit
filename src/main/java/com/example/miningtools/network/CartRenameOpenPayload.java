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

public record CartRenameOpenPayload(int entityId, String currentName, String typeTitle, String statusLine) implements CustomPacketPayload {
    public static final Type<CartRenameOpenPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "cart_rename_open"));

    public static final StreamCodec<ByteBuf, CartRenameOpenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CartRenameOpenPayload::entityId,
            ByteBufCodecs.stringUtf8(32),
            CartRenameOpenPayload::currentName,
            ByteBufCodecs.stringUtf8(64),
            CartRenameOpenPayload::typeTitle,
            ByteBufCodecs.stringUtf8(128),
            CartRenameOpenPayload::statusLine,
            CartRenameOpenPayload::new
    );

    public static @Nullable Consumer<CartRenameOpenPayload> CLIENT_HANDLER = null;

    @Override
    public Type<CartRenameOpenPayload> type() {
        return TYPE;
    }

    public static void handle(CartRenameOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (CLIENT_HANDLER != null) {
                CLIENT_HANDLER.accept(payload);
            }
        });
    }
}