package com.example.miningtools.block;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.CargoTransferBlockEntity;
import com.example.miningtools.network.CargoTransferSyncPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

/**
 * Cargo Loader, Cargo Unloader, and Locomotive Coaling Station block.
 * Placed underneath a Braking/Holding Rail (or Control/Powered Rail).
 * Emits redstone power (POWERED = true) by default when idle or finished so trains pass freely.
 * Depowers (POWERED = false) when holding a matching cart or locomotive for item/coal transfer.
 */
public class CargoTransferBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public static final int ROLE_LOADER = 0;
    public static final int ROLE_UNLOADER = 1;
    public static final int ROLE_COAL_FUELER = 2;

    private final int stationRole;

    public CargoTransferBlock(boolean unloader, BlockBehaviour.Properties properties) {
        this(unloader ? ROLE_UNLOADER : ROLE_LOADER, properties);
    }

    public CargoTransferBlock(int stationRole, BlockBehaviour.Properties properties) {
        super(properties);
        this.stationRole = stationRole;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, true));
    }

    public int getStationRole() {
        return this.stationRole;
    }

    public boolean isUnloader() {
        return this.stationRole == ROLE_UNLOADER;
    }

    public boolean isCoalFueler() {
        return this.stationRole == ROLE_COAL_FUELER;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, true);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CargoTransferBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        if (type == MiningTools.CARGO_TRANSFER_BLOCK_ENTITY.get()) {
            return (lvl, pos, st, be) -> CargoTransferBlockEntity.serverTick(lvl, pos, st, (CargoTransferBlockEntity) be);
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
            if (be instanceof CargoTransferBlockEntity transferBe && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(transferBe);
                PacketDistributor.sendToPlayer(
                        serverPlayer,
                        new CargoTransferSyncPayload(
                                transferBe.getStationRole(),
                                transferBe.getItemWhitelist(),
                                transferBe.getItemBlacklist(),
                                transferBe.getCartWhitelist(),
                                transferBe.getCartBlacklist()
                        )
                );
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean isMoving) {
        if (!state.is(state.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CargoTransferBlockEntity transferBe) {
                Containers.dropContents(level, pos, transferBe.getItems());
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
