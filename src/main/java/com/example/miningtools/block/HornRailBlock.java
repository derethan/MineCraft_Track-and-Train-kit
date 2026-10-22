package com.example.miningtools.block;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

public class HornRailBlock extends PoweredRailBlock {

    public HornRailBlock(BlockBehaviour.Properties properties) {
        super(properties, false);
    }

    @Override
    protected boolean isSameRailWithPower(Level level, BlockPos pos, boolean searchForward, int distance, RailShape shape) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this) && !state.is(MiningTools.CONTROL_RAIL.get()) && !state.is(MiningTools.ELECTRIC_RAIL.get())) {
            return false;
        }
        return super.isSameRailWithPower(level, pos, searchForward, distance, shape);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!level.isClientSide()) {
            if (entity instanceof BasicLocomotiveEntity loco) {
                loco.triggerHornRail(pos, state);
            } else if (entity instanceof ElectricLocomotiveEntity elecLoco) {
                elecLoco.triggerHornRail(pos, state);
            }
        }
    }
}

