package com.example.miningtools.block;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.FluidLoaderBlockEntity;
import com.example.miningtools.network.FluidLoaderSyncPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

public class FluidLoaderBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public static final int ROLE_LOADER = 0;
    public static final int ROLE_UNLOADER = 1;
    public static final int ROLE_DIESEL_FUELER = 2;

    private final int stationRole;

    public FluidLoaderBlock(BlockBehaviour.Properties properties) {
        this(ROLE_LOADER, properties);
    }

    public FluidLoaderBlock(int stationRole, BlockBehaviour.Properties properties) {
        super(properties);
        this.stationRole = stationRole;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, true));
    }

    public int getStationRole() {
        return this.stationRole;
    }

    public boolean isDieselFueler() {
        return this.stationRole == ROLE_DIESEL_FUELER;
    }

    public boolean isDefaultUnloader() {
        return this.stationRole == ROLE_UNLOADER;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, true);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidLoaderBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        if (type == MiningTools.FLUID_LOADER_BLOCK_ENTITY.get()) {
            return (lvl, pos, st, be) -> FluidLoaderBlockEntity.serverTick(lvl, pos, st, (FluidLoaderBlockEntity) be);
        }
        return null;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FluidLoaderBlockEntity loader && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(loader, pos);
                PacketDistributor.sendToPlayer(
                        serverPlayer,
                        new FluidLoaderSyncPayload(
                                loader.getStationRole(),
                                loader.getFluidWhitelist(),
                                loader.getFluidBlacklist(),
                                loader.getCartWhitelist(),
                                loader.getCartBlacklist()
                        )
                );
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.isEmpty()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FluidLoaderBlockEntity loader) {
                boolean success = FluidUtil.interactWithFluidHandler(player, hand, pos, loader.getInternalTank(), null);
                if (success) {
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return this.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean isMoving) {
        if (!state.is(state.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FluidLoaderBlockEntity loader) {
                Containers.dropContents(level, pos, loader.getItems());
                level.updateNeighborsAt(pos, this);
                level.updateNeighborsAt(pos.above(), this);
            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }
}
