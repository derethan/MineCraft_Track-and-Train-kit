package com.example.miningtools.item;

import com.example.miningtools.entity.LiquidMinecartEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class LiquidMinecartItem extends Item {
    public LiquidMinecartItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);

        BlockPos targetPos = null;
        BlockState railState = null;

        if (clickedState.is(BlockTags.RAILS)) {
            targetPos = clickedPos;
            railState = clickedState;
        } else if (context.getClickedFace() == Direction.UP) {
            BlockPos abovePos = clickedPos.above();
            BlockState aboveState = level.getBlockState(abovePos);
            if (aboveState.is(BlockTags.RAILS)) {
                targetPos = abovePos;
                railState = aboveState;
            }
        }

        if (targetPos == null || railState == null) {
            return InteractionResult.FAIL;
        }

        RailShape shape = railState.getBlock() instanceof BaseRailBlock baseRail
                ? baseRail.getRailDirection(railState, level, targetPos, null)
                : RailShape.NORTH_SOUTH;

        Player player = context.getPlayer();

        if (level instanceof ServerLevel serverLevel) {
            double offset = shape.isSlope() ? 0.5 : 0.0;
            Vec3 spawnPos = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.0625 + offset, targetPos.getZ() + 0.5);

            LiquidMinecartEntity cart = new LiquidMinecartEntity(level, spawnPos.x, spawnPos.y, spawnPos.z);
            serverLevel.addFreshEntity(cart);
            serverLevel.gameEvent(GameEvent.ENTITY_PLACE, targetPos, GameEvent.Context.of(player, serverLevel.getBlockState(targetPos.below())));
            context.getItemInHand().consume(1, player);
        }

        return InteractionResult.SUCCESS;
    }
}
