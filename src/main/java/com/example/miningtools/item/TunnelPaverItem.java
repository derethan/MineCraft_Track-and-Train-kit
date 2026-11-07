package com.example.miningtools.item;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.TunnelPaverEntity;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class TunnelPaverItem extends Item {
    public TunnelPaverItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null) {
            return InteractionResult.PASS;
        }
        return this.placeTunnelPaver(
                context.getLevel(),
                context.getClickedPos(),
                context.getClickedFace(),
                context.getHorizontalDirection(),
                null,
                context.getItemInHand()
        );
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        InteractionResult result = this.placeTunnelPaver(
                level,
                hitResult.getBlockPos(),
                hitResult.getDirection(),
                player.getDirection(),
                player,
                itemStack
        );
        if (result.consumesAction()) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return result;
    }

    private InteractionResult placeTunnelPaver(
            Level level,
            BlockPos clickedPos,
            Direction clickedFace,
            Direction fallbackFacing,
            @Nullable Player player,
            ItemStack itemStack
    ) {
        BlockPos targetPos = null;
        BlockState railState = null;
        BlockState clickedState = level.getBlockState(clickedPos);

        if (clickedState.is(BlockTags.RAILS)) {
            targetPos = clickedPos;
            railState = clickedState;
        } else {
            BlockPos adjacentPos = clickedPos.relative(clickedFace);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            if (adjacentState.is(BlockTags.RAILS)) {
                targetPos = adjacentPos;
                railState = adjacentState;
            }
        }

        if (targetPos == null || railState == null) {
            return InteractionResult.FAIL;
        }

        RailShape shape = railState.getBlock() instanceof BaseRailBlock baseRail
                ? baseRail.getRailDirection(railState, level, targetPos, null)
                : RailShape.NORTH_SOUTH;

        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
        Vec3i exit0 = exits.getFirst();
        Vec3i exit1 = exits.getSecond();

        Direction d0 = Direction.getNearest(exit0.getX(), 0, exit0.getZ(), Direction.NORTH);
        Direction d1 = Direction.getNearest(exit1.getX(), 0, exit1.getZ(), Direction.SOUTH);

        Direction chosenFacing;
        if (player != null) {
            Vec3 look = player.getLookAngle();
            double dot0 = (d0 != null) ? (look.x * d0.getStepX() + look.z * d0.getStepZ()) : -1.0;
            double dot1 = (d1 != null) ? (look.x * d1.getStepX() + look.z * d1.getStepZ()) : -1.0;
            chosenFacing = (dot0 >= dot1) ? (d0 != null ? d0 : Direction.NORTH) : (d1 != null ? d1 : Direction.SOUTH);
        } else {
            double dot0 = (d0 != null) ? (fallbackFacing.getStepX() * d0.getStepX() + fallbackFacing.getStepZ() * d0.getStepZ()) : -1.0;
            double dot1 = (d1 != null) ? (fallbackFacing.getStepX() * d1.getStepX() + fallbackFacing.getStepZ() * d1.getStepZ()) : -1.0;
            chosenFacing = (dot0 >= dot1) ? (d0 != null ? d0 : Direction.NORTH) : (d1 != null ? d1 : Direction.SOUTH);
        }

        if (level instanceof ServerLevel serverLevel) {
            double offset = shape.isSlope() ? 0.5D : 0.0D;
            Vec3 spawnPos = new Vec3(targetPos.getX() + 0.5D, targetPos.getY() + 0.0625D + offset, targetPos.getZ() + 0.5D);

            TunnelPaverEntity paver = new TunnelPaverEntity(MiningTools.TUNNEL_PAVER_ENTITY.get(), level, spawnPos.x, spawnPos.y, spawnPos.z);
            paver.setFacing(chosenFacing);
            paver.setOnRails(true);

            if (itemStack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
                Component customName = itemStack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
                paver.setCustomName(customName);
                if (customName != null) {
                    paver.setTrainName(customName.getString());
                }
            }

            serverLevel.addFreshEntity(paver);
            serverLevel.gameEvent(GameEvent.ENTITY_PLACE, targetPos, GameEvent.Context.of(player, railState));
            itemStack.consume(1, player);
        }

        return InteractionResult.SUCCESS;
    }
}
