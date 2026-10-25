package com.example.miningtools.network;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LocomotiveControlPayload(int action) implements CustomPacketPayload {
    public static final int ACTION_START = 0;
    public static final int ACTION_STOP = 1;
    public static final int ACTION_SPEED_DOWN = 2;
    public static final int ACTION_SPEED_UP = 3;
    public static final int ACTION_REVERSE = 4;
    public static final int ACTION_HORN = 5;
    public static final int ACTION_TOGGLE_LIGHT = 6;
    public static final int ACTION_OPEN_GUI = 7;

    public static final Type<LocomotiveControlPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "locomotive_control"));

    public static final StreamCodec<ByteBuf, LocomotiveControlPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(LocomotiveControlPayload::new, LocomotiveControlPayload::action);

    @Override
    public Type<LocomotiveControlPayload> type() {
        return TYPE;
    }

    public static void handle(LocomotiveControlPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;
            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                if (payload.action() == ACTION_OPEN_GUI) {
                    if (vehicle instanceof net.minecraft.world.MenuProvider menuProvider) {
                        player.openMenu(menuProvider);
                        return;
                    }
                    BasicLocomotiveEntity loco = BasicLocomotiveEntity.getConnectedLocomotive(vehicle);
                    if (loco != null && !loco.isRemoved()) {
                        player.openMenu(loco);
                        return;
                    }
                    ElectricLocomotiveEntity elecLoco = ElectricLocomotiveEntity.getConnectedLocomotive(vehicle);
                    if (elecLoco != null && !elecLoco.isRemoved()) {
                        player.openMenu(elecLoco);
                        return;
                    }
                    DieselLocomotiveEntity dieselLoco = DieselLocomotiveEntity.getConnectedLocomotive(vehicle);
                    if (dieselLoco != null && !dieselLoco.isRemoved()) {
                        player.openMenu(dieselLoco);
                        return;
                    }
                    com.example.miningtools.entity.TunnelBoreEntity bore = com.example.miningtools.entity.TunnelBoreEntity.getConnectedBore(vehicle);
                    if (bore != null && !bore.isRemoved()) {
                        player.openMenu(bore);
                        return;
                    }
                    return;
                }

                if (vehicle instanceof com.example.miningtools.entity.TunnelBoreEntity bore && !bore.isRemoved()) {

                    bore.handleHotkeyAction(player, payload.action());
                    return;
                }
                if (vehicle instanceof com.example.miningtools.entity.TrackPaverEntity paver && !paver.isRemoved()) {
                    paver.handleHotkeyAction(player, payload.action());
                    return;
                }
                BasicLocomotiveEntity loco = BasicLocomotiveEntity.getConnectedLocomotive(vehicle);
                if (loco != null && !loco.isRemoved()) {
                    loco.handleHotkeyAction(player, payload.action());
                    return;
                }
                ElectricLocomotiveEntity elecLoco = ElectricLocomotiveEntity.getConnectedLocomotive(vehicle);
                if (elecLoco != null && !elecLoco.isRemoved()) {
                    elecLoco.handleHotkeyAction(player, payload.action());
                    return;
                }
                DieselLocomotiveEntity dieselLoco = DieselLocomotiveEntity.getConnectedLocomotive(vehicle);
                if (dieselLoco != null && !dieselLoco.isRemoved()) {
                    dieselLoco.handleHotkeyAction(player, payload.action());
                }
            }
        });
    }
}
