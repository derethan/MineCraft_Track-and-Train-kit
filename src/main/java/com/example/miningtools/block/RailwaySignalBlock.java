package com.example.miningtools.block;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.RailwaySignalBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RailwaySignalBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<SignalAspect> ASPECT = EnumProperty.create("aspect", SignalAspect.class);

    private static final VoxelShape BASE = Block.box(3.0, 0.0, 3.0, 13.0, 2.0, 13.0);
    private static final VoxelShape MAST = Block.box(6.0, 2.0, 6.0, 10.0, 20.0, 10.0);

    private static final VoxelShape SHAPE_NORTH = Shapes.or(BASE, MAST, Block.box(4.0, 14.0, 2.0, 12.0, 28.0, 8.0));
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(BASE, MAST, Block.box(4.0, 14.0, 8.0, 12.0, 28.0, 14.0));
    private static final VoxelShape SHAPE_WEST  = Shapes.or(BASE, MAST, Block.box(2.0, 14.0, 4.0, 8.0, 28.0, 12.0));
    private static final VoxelShape SHAPE_EAST  = Shapes.or(BASE, MAST, Block.box(8.0, 14.0, 4.0, 14.0, 28.0, 12.0));

    public RailwaySignalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ASPECT, SignalAspect.GREEN));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(ASPECT, SignalAspect.GREEN);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ASPECT);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RailwaySignalBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;
        if (blockEntityType == MiningTools.RAILWAY_SIGNAL_BLOCK_ENTITY.get()) {
            return (lvl, pos, st, be) -> RailwaySignalBlockEntity.serverTick(lvl, pos, st, (RailwaySignalBlockEntity) be);
        }
        return null;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RailwaySignalBlockEntity signal) {
                SignalAspect aspect = state.getValue(ASPECT);
                String partnerStr = (signal.getLinkedPartnerSignal() != null) ? signal.getLinkedPartnerSignal().toShortString() : "None";
                String switchStr = (signal.getLinkedSwitchPos() != null) ? signal.getLinkedSwitchPos().toShortString() : "None";
                String modeStr = signal.isPassingSidingMode() ? "Passing Siding (Bidirectional)" : "Automatic Block Signal (Unidirectional)";

                ChatFormatting aspectColor = switch (aspect) {
                    case GREEN -> ChatFormatting.GREEN;
                    case YELLOW -> ChatFormatting.YELLOW;
                    case SHUNT -> ChatFormatting.AQUA;
                    case RED -> ChatFormatting.RED;
                };

                player.sendOverlayMessage(Component.literal("Signal: " + aspect.name() + " | Mode: " + (signal.isPassingSidingMode() ? "Siding" : "ABS") + " | Linked: " + partnerStr).withStyle(aspectColor));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return switch (state.getValue(ASPECT)) {
            case GREEN -> 15;
            case YELLOW -> 8;
            case SHUNT -> 4;
            case RED -> 0;
        };
    }
}
