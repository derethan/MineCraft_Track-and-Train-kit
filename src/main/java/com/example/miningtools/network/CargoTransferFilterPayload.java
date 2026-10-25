package com.example.miningtools.network;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.CargoTransferBlockEntity;
import com.example.miningtools.menu.CargoTransferMenu;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CargoTransferFilterPayload(
        String itemWhitelist,
        String itemBlacklist,
        String cartWhitelist,
        String cartBlacklist
) implements CustomPacketPayload {

    public static final Type<CargoTransferFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "cargo_transfer_filter"));

    public static final StreamCodec<ByteBuf, CargoTransferFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128),
            CargoTransferFilterPayload::itemWhitelist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferFilterPayload::itemBlacklist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferFilterPayload::cartWhitelist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferFilterPayload::cartBlacklist,
            CargoTransferFilterPayload::new
    );

    @Override
    public Type<CargoTransferFilterPayload> type() {
        return TYPE;
    }

    public static void handle(CargoTransferFilterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) {
                return;
            }
            if (player.containerMenu instanceof CargoTransferMenu menu
                    && menu.getContainer() instanceof CargoTransferBlockEntity transferBe
                    && menu.stillValid(player)) {
                transferBe.setItemWhitelist(payload.itemWhitelist());
                transferBe.setItemBlacklist(payload.itemBlacklist());
                transferBe.setCartWhitelist(payload.cartWhitelist());
                transferBe.setCartBlacklist(payload.cartBlacklist());
            }
        });
    }
}
