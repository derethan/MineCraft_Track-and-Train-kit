package com.example.miningtools.network;

import java.util.ArrayList;
import java.util.List;

import com.example.miningtools.MiningTools;
import com.example.miningtools.compat.journeymap.JourneyMapTrainPlugin;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TrainMapSyncPayload(List<TrainMapEntry> entries) implements CustomPacketPayload {

    public static final Type<TrainMapSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "train_map_sync"));

    public static final StreamCodec<ByteBuf, TrainMapEntry> ENTRY_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            TrainMapEntry::entityId,
            BlockPos.STREAM_CODEC,
            TrainMapEntry::pos,
            ByteBufCodecs.VAR_INT,
            TrainMapEntry::yRot,
            ByteBufCodecs.stringUtf8(64),
            TrainMapEntry::dimension,
            ByteBufCodecs.stringUtf8(64),
            TrainMapEntry::name,
            ByteBufCodecs.stringUtf8(32),
            TrainMapEntry::iconType,
            ByteBufCodecs.BOOL,
            TrainMapEntry::isRemoved,
            TrainMapEntry::new
    );

    public static final StreamCodec<ByteBuf, TrainMapSyncPayload> STREAM_CODEC =
            ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list()).map(TrainMapSyncPayload::new, TrainMapSyncPayload::entries);

    @Override
    public Type<TrainMapSyncPayload> type() {
        return TYPE;
    }

    public static void handle(TrainMapSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                JourneyMapTrainPlugin.handleServerMapSync(payload);
            } catch (Throwable ignored) {
            }
        });
    }

    public record TrainMapEntry(
            int entityId,
            BlockPos pos,
            int yRot,
            String dimension,
            String name,
            String iconType,
            boolean isRemoved
    ) {}
}
