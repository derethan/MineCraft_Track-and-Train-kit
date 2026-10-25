package com.example.miningtools.network;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CartRenamePayload(int entityId, String cartName) implements CustomPacketPayload {
    public static final Type<CartRenamePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "cart_rename"));

    public static final StreamCodec<ByteBuf, CartRenamePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CartRenamePayload::entityId,
            ByteBufCodecs.stringUtf8(32),
            CartRenamePayload::cartName,
            CartRenamePayload::new
    );

    @Override
    public Type<CartRenamePayload> type() {
        return TYPE;
    }

    public static void applyCustomNameToCart(AbstractMinecart cart, String rawName) {
        String cleaned = rawName != null ? rawName.trim() : "";
        if (cleaned.length() > 32) {
            cleaned = cleaned.substring(0, 32);
        }
        if (cart instanceof BasicLocomotiveEntity bl) {
            bl.setTrainName(cleaned);
        } else if (cart instanceof DieselLocomotiveEntity dl) {
            dl.setTrainName(cleaned);
        } else if (cart instanceof ElectricLocomotiveEntity el) {
            el.setTrainName(cleaned);
        } else {
            if (cleaned.isEmpty()) {
                cart.setCustomName(null);
                cart.setCustomNameVisible(false);
            } else {
                cart.setCustomName(Component.literal(cleaned));
                cart.setCustomNameVisible(true);
            }
        }
    }

    public static void handle(CartRenamePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) {
                return;
            }
            Entity entity = player.level().getEntity(payload.entityId());
            if (entity instanceof AbstractMinecart cart && !cart.isRemoved() && player.distanceToSqr(cart) <= 100.0D) {
                applyCustomNameToCart(cart, payload.cartName());
            }
        });
    }
}
