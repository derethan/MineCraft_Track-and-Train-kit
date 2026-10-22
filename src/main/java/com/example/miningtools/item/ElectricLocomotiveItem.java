package com.example.miningtools.item;

import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class ElectricLocomotiveItem extends Item {
    public ElectricLocomotiveItem(Item.Properties properties) {
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

        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
        Vec3i exit0 = exits.getFirst();
        Vec3i exit1 = exits.getSecond();

        Direction d0 = Direction.getNearest(exit0.getX(), 0, exit0.getZ(), Direction.NORTH);
        Direction d1 = Direction.getNearest(exit1.getX(), 0, exit1.getZ(), Direction.SOUTH);

        Player player = context.getPlayer();
        Direction chosenFacing;
        if (player != null) {
            Vec3 look = player.getLookAngle();
            double dot0 = (d0 != null) ? (look.x * d0.getStepX() + look.z * d0.getStepZ()) : -1.0;
            double dot1 = (d1 != null) ? (look.x * d1.getStepX() + look.z * d1.getStepZ()) : -1.0;
            chosenFacing = (dot0 >= dot1) ? (d0 != null ? d0 : Direction.NORTH) : (d1 != null ? d1 : Direction.SOUTH);
        } else {
            Direction hDir = context.getHorizontalDirection();
            double dot0 = (d0 != null) ? (hDir.getStepX() * d0.getStepX() + hDir.getStepZ() * d0.getStepZ()) : -1.0;
            double dot1 = (d1 != null) ? (hDir.getStepX() * d1.getStepX() + hDir.getStepZ() * d1.getStepZ()) : -1.0;
            chosenFacing = (dot0 >= dot1) ? (d0 != null ? d0 : Direction.NORTH) : (d1 != null ? d1 : Direction.SOUTH);
        }

        if (level instanceof ServerLevel serverLevel) {
            double offset = shape.isSlope() ? 0.5 : 0.0;
            Vec3 spawnPos = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.0625 + offset, targetPos.getZ() + 0.5);

            ElectricLocomotiveEntity loco = new ElectricLocomotiveEntity(level, spawnPos.x, spawnPos.y, spawnPos.z);
            loco.setFacing(chosenFacing);
            serverLevel.addFreshEntity(loco);
            serverLevel.gameEvent(GameEvent.ENTITY_PLACE, targetPos, GameEvent.Context.of(player, serverLevel.getBlockState(targetPos.below())));
            context.getItemInHand().consume(1, player);
        }

        return InteractionResult.SUCCESS;
    }
}
