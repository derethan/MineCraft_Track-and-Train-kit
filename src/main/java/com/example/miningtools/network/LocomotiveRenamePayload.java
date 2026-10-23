package com.example.miningtools.network;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.menu.BasicLocomotiveMenu;
import com.example.miningtools.menu.DieselLocomotiveMenu;
import com.example.miningtools.menu.ElectricLocomotiveMenu;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LocomotiveRenamePayload(String trainName) implements CustomPacketPayload {
    public static final Type<LocomotiveRenamePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "locomotive_rename"));

    public static final StreamCodec<ByteBuf, LocomotiveRenamePayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(32).map(LocomotiveRenamePayload::new, LocomotiveRenamePayload::trainName);

    @Override
    public Type<LocomotiveRenamePayload> type() {
        return TYPE;
    }

    public static void handle(LocomotiveRenamePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) {
                return;
            }
            if (player.containerMenu instanceof BasicLocomotiveMenu menu
                    && menu.getContainer() instanceof BasicLocomotiveEntity loco
                    && !loco.isRemoved()
                    && loco.stillValid(player)) {
                loco.setTrainName(payload.trainName());
                return;
            }
            if (player.containerMenu instanceof ElectricLocomotiveMenu menu
                    && menu.getContainer() instanceof ElectricLocomotiveEntity loco
                    && !loco.isRemoved()
                    && loco.stillValid(player)) {
                loco.setTrainName(payload.trainName());
                return;
            }
            if (player.containerMenu instanceof DieselLocomotiveMenu menu
                    && menu.getContainer() instanceof DieselLocomotiveEntity loco
                    && !loco.isRemoved()
                    && loco.stillValid(player)) {
                loco.setTrainName(payload.trainName());
                return;
            }
            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                BasicLocomotiveEntity loco = BasicLocomotiveEntity.getConnectedLocomotive(vehicle);
                if (loco != null && !loco.isRemoved()) {
                    loco.setTrainName(payload.trainName());
                    return;
                }
                ElectricLocomotiveEntity elecLoco = ElectricLocomotiveEntity.getConnectedLocomotive(vehicle);
                if (elecLoco != null && !elecLoco.isRemoved()) {
                    elecLoco.setTrainName(payload.trainName());
                    return;
                }
                DieselLocomotiveEntity dieselLoco = DieselLocomotiveEntity.getConnectedLocomotive(vehicle);
                if (dieselLoco != null && !dieselLoco.isRemoved()) {
                    dieselLoco.setTrainName(payload.trainName());
                }
            }
        });
    }
}
