package com.example.miningtools.network;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.FluidLoaderBlockEntity;
import com.example.miningtools.menu.FluidLoaderMenu;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FluidLoaderFilterPayload(
        String fluidWhitelist,
        String fluidBlacklist,
        String cartWhitelist,
        String cartBlacklist
) implements CustomPacketPayload {

    public static final Type<FluidLoaderFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "fluid_loader_filter"));

    public static final StreamCodec<ByteBuf, FluidLoaderFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128),
            FluidLoaderFilterPayload::fluidWhitelist,
            ByteBufCodecs.stringUtf8(128),
            FluidLoaderFilterPayload::fluidBlacklist,
            ByteBufCodecs.stringUtf8(128),
            FluidLoaderFilterPayload::cartWhitelist,
            ByteBufCodecs.stringUtf8(128),
            FluidLoaderFilterPayload::cartBlacklist,
            FluidLoaderFilterPayload::new
    );

    @Override
    public Type<FluidLoaderFilterPayload> type() {
        return TYPE;
    }

    public static void handle(FluidLoaderFilterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) {
                return;
            }
            if (player.containerMenu instanceof FluidLoaderMenu menu
                    && menu.getContainer() instanceof FluidLoaderBlockEntity loaderBe
                    && menu.stillValid(player)) {
                loaderBe.setFluidWhitelist(payload.fluidWhitelist());
                loaderBe.setFluidBlacklist(payload.fluidBlacklist());
                loaderBe.setCartWhitelist(payload.cartWhitelist());
                loaderBe.setCartBlacklist(payload.cartBlacklist());
            }
        });
    }
}
