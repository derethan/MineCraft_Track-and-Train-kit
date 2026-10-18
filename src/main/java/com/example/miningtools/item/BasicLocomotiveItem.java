package com.example.miningtools.item;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.BasicLocomotiveEntity;

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
import net.minecraft.world.phys.Vec3;

public class BasicLocomotiveItem extends Item {
    public BasicLocomotiveItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null) {
            return InteractionResult.PASS;
        }
        return this.placeLocomotive(
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

        InteractionResult result = this.placeLocomotive(
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

    private InteractionResult placeLocomotive(
            Level level,
            BlockPos clickedPos,
            Direction clickedFace,
            Direction facing,
            @Nullable Player player,
            ItemStack itemStack
    ) {
        BlockPos targetPos = null;
        BlockState clickedState = level.getBlockState(clickedPos);

        if (clickedState.is(BlockTags.RAILS)) {
            targetPos = clickedPos;
        } else {
            BlockPos adjacentPos = clickedPos.relative(clickedFace);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            if (adjacentState.is(BlockTags.RAILS)) {
                targetPos = adjacentPos;
            } else if (clickedFace == Direction.UP
                    && clickedState.isFaceSturdy(level, clickedPos, Direction.UP)
                    && (adjacentState.isAir() || adjacentState.canBeReplaced())) {
                targetPos = adjacentPos;
            }
        }

        if (targetPos == null) {
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel serverLevel) {
            BlockState targetState = level.getBlockState(targetPos);
            RailShape shape = targetState.getBlock() instanceof BaseRailBlock baseRail
                    ? baseRail.getRailDirection(targetState, level, targetPos, null)
                    : RailShape.NORTH_SOUTH;
            double offset = shape.isSlope() ? 0.5 : 0.0;
            Vec3 spawnPos = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.0625 + offset, targetPos.getZ() + 0.5);

            BasicLocomotiveEntity loco = new BasicLocomotiveEntity(level, spawnPos.x, spawnPos.y, spawnPos.z);
            loco.setFacing(facing);
            serverLevel.addFreshEntity(loco);
            serverLevel.gameEvent(GameEvent.ENTITY_PLACE, targetPos, GameEvent.Context.of(player, serverLevel.getBlockState(targetPos.below())));
            itemStack.consume(1, player);
        }

        return InteractionResult.SUCCESS;
    }
}
