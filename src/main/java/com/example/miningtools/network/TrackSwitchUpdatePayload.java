package com.example.miningtools.network;

import java.util.ArrayList;
import java.util.List;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.TrackSwitchBlock;
import com.example.miningtools.block.entity.TrackSwitchBlockEntity;
import com.example.miningtools.block.entity.TrackSwitchBlockEntity.SwitchRule;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TrackSwitchUpdatePayload(
        BlockPos pos,
        boolean currentOpen,
        int detectRange,
        List<SwitchRule> rules
) implements CustomPacketPayload {

    public static final Type<TrackSwitchUpdatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "track_switch_update"));

    public static final StreamCodec<ByteBuf, TrackSwitchUpdatePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TrackSwitchUpdatePayload::pos,
            ByteBufCodecs.BOOL,
            TrackSwitchUpdatePayload::currentOpen,
            ByteBufCodecs.VAR_INT,
            TrackSwitchUpdatePayload::detectRange,
            SwitchRule.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)),
            TrackSwitchUpdatePayload::rules,
            TrackSwitchUpdatePayload::new
    );

    @Override
    public Type<TrackSwitchUpdatePayload> type() {
        return TYPE;
    }

    public static void handle(TrackSwitchUpdatePayload payload, IPayloadContext context) {
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
            if (level.getBlockEntity(pos) instanceof TrackSwitchBlockEntity switchBe) {
                switchBe.setDetectRange(payload.detectRange());
                switchBe.setRules(payload.rules());
                BlockState state = level.getBlockState(pos);
                if (state.is(MiningTools.TRACK_SWITCH.get())) {
                    if (state.getValue(TrackSwitchBlock.OPEN) != payload.currentOpen()) {
                        TrackSwitchBlock.setSwitchState(level, pos, state, payload.currentOpen(), true);
                    } else {
                        TrackSwitchBlock.applySwitchToAdjacentRails(level, pos, payload.currentOpen());
                    }
                }
            }
        });
    }
}
