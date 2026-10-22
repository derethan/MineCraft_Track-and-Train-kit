package com.example.miningtools.block.entity;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.Config;
import com.example.miningtools.MiningTools;
import com.example.miningtools.block.ElectricRailBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

public class ElectricRailBlockEntity extends BlockEntity {

    private final SimpleEnergyHandler energyStorage = new SimpleEnergyHandler(
            Config.ELECTRIC_RAIL_CAPACITY.get(),
            Config.ELECTRIC_RAIL_TRANSFER_RATE.get(),
            Config.ELECTRIC_RAIL_TRANSFER_RATE.get(),
            0
    ) {
        @Override
        protected void onEnergyChanged(int previousEnergy) {
            super.onEnergyChanged(previousEnergy);
            ElectricRailBlockEntity.this.setChanged();
        }
    };

    public ElectricRailBlockEntity(BlockPos pos, BlockState blockState) {
        super(MiningTools.ELECTRIC_RAIL_BLOCK_ENTITY.get(), pos, blockState);
    }

    public EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return this.energyStorage;
    }

    public SimpleEnergyHandler getInternalEnergyStorage() {
        return this.energyStorage;
    }

    public int getEnergyStored() {
        return this.energyStorage.getAmountAsInt();
    }

    public int getCapacity() {
        return this.energyStorage.getCapacityAsInt();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ElectricRailBlockEntity blockEntity) {
        int currentEnergy = blockEntity.energyStorage.getAmountAsInt();

        // Visual state update: rail conducts light/glow when powered
        boolean hasPower = currentEnergy > 0;
        if (state.hasProperty(ElectricRailBlock.POWERED) && state.getValue(ElectricRailBlock.POWERED) != hasPower) {
            level.setBlock(pos, state.setValue(ElectricRailBlock.POWERED, hasPower), 3);
        }

        // Propagate power to adjacent connected electric rails along the track
        if (currentEnergy > 10 && state.hasProperty(ElectricRailBlock.SHAPE)) {
            RailShape shape = state.getValue(ElectricRailBlock.SHAPE);
            Direction[] dirs = getTrackDirections(shape);

            int maxTransfer = Config.ELECTRIC_RAIL_TRANSFER_RATE.get();
            for (Direction dir : dirs) {
                // Check direct neighbor, upper neighbor (for slopes), and lower neighbor
                BlockPos[] targets = new BlockPos[]{
                        pos.relative(dir),
                        pos.relative(dir).above(),
                        pos.relative(dir).below()
                };

                for (BlockPos targetPos : targets) {
                    BlockEntity neighborBe = level.getBlockEntity(targetPos);
                    if (neighborBe instanceof ElectricRailBlockEntity neighborRail) {
                        int neighborEnergy = neighborRail.energyStorage.getAmountAsInt();
                        if (neighborEnergy < blockEntity.energyStorage.getAmountAsInt()) {
                            int diff = (blockEntity.energyStorage.getAmountAsInt() - neighborEnergy) / 2;
                            int toMove = Math.min(diff, maxTransfer / 2);
                            if (toMove > 0) {
                                EnergyHandlerUtil.move(blockEntity.energyStorage, neighborRail.energyStorage, toMove, null);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Direction[] getTrackDirections(RailShape shape) {
        return switch (shape) {
            case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> new Direction[]{Direction.NORTH, Direction.SOUTH};
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> new Direction[]{Direction.EAST, Direction.WEST};
            default -> new Direction[]{Direction.NORTH, Direction.SOUTH};
        };
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energyStorage.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energyStorage.deserialize(input);
    }
}
