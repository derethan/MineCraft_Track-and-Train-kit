package com.example.miningtools.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.TrackSwitchBlockEntity.SwitchRule;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TrackSwitchSyncPayload(
        BlockPos pos,
        boolean currentOpen,
        int detectRange,
        List<SwitchRule> rules
) implements CustomPacketPayload {

    public static final Type<TrackSwitchSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "track_switch_sync"));

    public static final StreamCodec<ByteBuf, TrackSwitchSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TrackSwitchSyncPayload::pos,
            ByteBufCodecs.BOOL,
            TrackSwitchSyncPayload::currentOpen,
            ByteBufCodecs.VAR_INT,
            TrackSwitchSyncPayload::detectRange,
            SwitchRule.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)),
            TrackSwitchSyncPayload::rules,
            TrackSwitchSyncPayload::new
    );

    public static @Nullable Consumer<TrackSwitchSyncPayload> CLIENT_HANDLER = null;

    @Override
    public Type<TrackSwitchSyncPayload> type() {
        return TYPE;
    }

    public static void handle(TrackSwitchSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (CLIENT_HANDLER != null) {
                CLIENT_HANDLER.accept(payload);
            }
        });
    }
}
