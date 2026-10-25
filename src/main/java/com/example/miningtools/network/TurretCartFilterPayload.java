package com.example.miningtools.network;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.TurretCartEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TurretCartFilterPayload(
        int cartEntityId,
        int filterMode,
        String whitelist,
        String blacklist
) implements CustomPacketPayload {

    public static final Type<TurretCartFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "turret_cart_filter"));

    public static final StreamCodec<ByteBuf, TurretCartFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            TurretCartFilterPayload::cartEntityId,
            ByteBufCodecs.VAR_INT,
            TurretCartFilterPayload::filterMode,
            ByteBufCodecs.stringUtf8(256),
            TurretCartFilterPayload::whitelist,
            ByteBufCodecs.stringUtf8(256),
            TurretCartFilterPayload::blacklist,
            TurretCartFilterPayload::new
    );

    @Override
    public Type<TurretCartFilterPayload> type() {
        return TYPE;
    }

    public static void handle(TurretCartFilterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null || player.level() == null) {
                return;
            }
            Entity entity = player.level().getEntity(payload.cartEntityId());
            if (entity instanceof TurretCartEntity cart && !cart.isRemoved() && cart.distanceToSqr(player) < 64.0D) {
                cart.setFilterMode(payload.filterMode());
                cart.setWhitelist(payload.whitelist());
                cart.setBlacklist(payload.blacklist());
            }
        });
    }
}
