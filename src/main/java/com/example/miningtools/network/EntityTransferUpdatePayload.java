package com.example.miningtools.network;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.AbstractEntityTransferBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EntityTransferUpdatePayload(
        BlockPos pos,
        String whitelist,
        String blacklist,
        int ageFilter,
        int range
) implements CustomPacketPayload {

    public static final Type<EntityTransferUpdatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "entity_transfer_update"));

    public static final StreamCodec<ByteBuf, EntityTransferUpdatePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            EntityTransferUpdatePayload::pos,
            ByteBufCodecs.STRING_UTF8,
            EntityTransferUpdatePayload::whitelist,
            ByteBufCodecs.STRING_UTF8,
            EntityTransferUpdatePayload::blacklist,
            ByteBufCodecs.VAR_INT,
            EntityTransferUpdatePayload::ageFilter,
            ByteBufCodecs.VAR_INT,
            EntityTransferUpdatePayload::range,
            EntityTransferUpdatePayload::new
    );

    @Override
    public Type<EntityTransferUpdatePayload> type() {
        return TYPE;
    }

    public static void handle(EntityTransferUpdatePayload payload, IPayloadContext context) {
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
            if (level.getBlockEntity(pos) instanceof AbstractEntityTransferBlockEntity be) {
                be.setWhitelist(payload.whitelist());
                be.setBlacklist(payload.blacklist());
                be.setAgeFilter(payload.ageFilter());
                be.setRange(payload.range());
            }
        });
    }
}
