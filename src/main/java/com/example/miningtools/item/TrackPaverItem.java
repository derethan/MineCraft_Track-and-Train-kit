package com.example.miningtools.item;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.TrackPaverEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

public class TrackPaverItem extends Item {
    public TrackPaverItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null) {
            return InteractionResult.PASS;
        }
        return this.placeTrackPaver(
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

        InteractionResult result = this.placeTrackPaver(
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

    private InteractionResult placeTrackPaver(
            Level level,
            BlockPos clickedPos,
            Direction clickedFace,
            Direction facing,
            @Nullable Player player,
            ItemStack itemStack
    ) {
        BlockPos targetPos = null;
        BlockState clickedState = level.getBlockState(clickedPos);
        boolean isRail = false;

        if (clickedState.is(BlockTags.RAILS)) {
            targetPos = clickedPos;
            isRail = true;
        } else {
            BlockPos adjacentPos = clickedPos.relative(clickedFace);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            if (adjacentState.is(BlockTags.RAILS)) {
                targetPos = adjacentPos;
                isRail = true;
            } else if (clickedFace == Direction.UP
                    && clickedState.isFaceSturdy(level, clickedPos, Direction.UP)
                    && (adjacentState.isAir() || adjacentState.canBeReplaced())) {
                targetPos = adjacentPos;
                isRail = false;
            }
        }

        if (targetPos == null) {
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel serverLevel) {
            double spawnX = targetPos.getX() + 0.5D;
            double spawnY = isRail ? (targetPos.getY() + 0.0625D) : (targetPos.getY());
            double spawnZ = targetPos.getZ() + 0.5D;

            Direction initialFacing = facing;
            if (isRail) {
                BlockState railState = level.getBlockState(targetPos);
                if (railState.getBlock() instanceof BaseRailBlock railBlock) {
                    RailShape shape = railState.getValue(railBlock.getShapeProperty());
                    if (shape == RailShape.EAST_WEST || shape == RailShape.ASCENDING_EAST || shape == RailShape.ASCENDING_WEST) {
                        initialFacing = (facing.getAxis() == Direction.Axis.X) ? facing : Direction.EAST;
                    } else if (shape == RailShape.NORTH_SOUTH || shape == RailShape.ASCENDING_NORTH || shape == RailShape.ASCENDING_SOUTH) {
                        initialFacing = (facing.getAxis() == Direction.Axis.Z) ? facing : Direction.NORTH;
                    }
                }
            }

            TrackPaverEntity paver = new TrackPaverEntity(MiningTools.TRACK_PAVER_ENTITY.get(), level, spawnX, spawnY, spawnZ);
            paver.setFacing(initialFacing);
            paver.setOnRails(isRail);

            serverLevel.addFreshEntity(paver);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, targetPos);
        }

        itemStack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
