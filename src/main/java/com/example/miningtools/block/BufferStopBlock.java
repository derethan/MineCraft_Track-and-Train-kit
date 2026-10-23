package com.example.miningtools.block;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.entity.TunnelBoreEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BufferStopBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D);

    public BufferStopBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, false);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof AbstractMinecart cart) {
            handleCartCollision(state, level, pos, cart);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity instanceof AbstractMinecart cart) {
            handleCartCollision(state, level, pos, cart);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving);
        if (!level.isClientSide()) {
            checkAdjacentCarts(state, level, pos);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(MiningTools.CROWBAR_ITEM.get())) {
            if (!level.isClientSide()) {
                Direction next = state.getValue(FACING).getClockWise();
                level.setBlock(pos, state.setValue(FACING, next), 3);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.8F, 1.2F);
                player.sendOverlayMessage(
                        Component.literal("Rotated Buffer Stop to face " + next.getName().toUpperCase()).withStyle(ChatFormatting.YELLOW)
                );
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                Direction next = state.getValue(FACING).getClockWise();
                level.setBlock(pos, state.setValue(FACING, next), 3);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.8F, 1.2F);
                player.sendOverlayMessage(
                        Component.literal("Rotated Buffer Stop to face " + next.getName().toUpperCase()).withStyle(ChatFormatting.YELLOW)
                );
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    public static void handleCartCollision(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        Vec3 motion = cart.getDeltaMovement();
        boolean moving = motion.horizontalDistanceSqr() > 1.0E-4D;

        // Halt cart immediately
        cart.setDeltaMovement(0.0D, motion.y, 0.0D);

        // If locomotive or tunnel bore, shut down the engine
        if (cart instanceof BasicLocomotiveEntity loco) {
            loco.stopLocomotiveFromRail();
        } else if (cart instanceof DieselLocomotiveEntity loco) {
            loco.stopLocomotiveFromRail();
        } else if (cart instanceof ElectricLocomotiveEntity loco) {
            loco.stopLocomotiveFromRail();
        } else if (cart instanceof TunnelBoreEntity bore) {
            bore.setRunMode(TunnelBoreEntity.MODE_STOPPED);
            bore.setActive(false);
        }

        if (moving) {
            level.playSound(null, pos, SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.BLOCKS, 0.8F, 1.2F);
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.5F);

            if (level instanceof ServerLevel serverLevel) {
                Direction facing = state.getValue(FACING);
                double px = pos.getX() + 0.5D + facing.getStepX() * 0.45D;
                double py = pos.getY() + 0.5D;
                double pz = pos.getZ() + 0.5D + facing.getStepZ() * 0.45D;
                serverLevel.sendParticles(ParticleTypes.CRIT, px, py, pz, 10, 0.25D, 0.2D, 0.25D, 0.1D);
            }
        }

        if (!state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, true), 3);
        }
        level.scheduleTick(pos, state.getBlock(), 10);
    }

    public void checkAdjacentCarts(BlockState state, Level level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        AABB searchBox = new AABB(pos)
                .inflate(0.3D)
                .expandTowards(facing.getStepX() * 0.85D, 0.2D, facing.getStepZ() * 0.85D);

        List<AbstractMinecart> carts = level.getEntitiesOfClass(AbstractMinecart.class, searchBox);
        if (!carts.isEmpty()) {
            for (AbstractMinecart cart : carts) {
                handleCartCollision(state, level, pos, cart);
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);
        AABB searchBox = new AABB(pos)
                .inflate(0.3D)
                .expandTowards(facing.getStepX() * 0.85D, 0.2D, facing.getStepZ() * 0.85D);

        List<AbstractMinecart> carts = level.getEntitiesOfClass(AbstractMinecart.class, searchBox);
        if (!carts.isEmpty()) {
            for (AbstractMinecart cart : carts) {
                Vec3 motion = cart.getDeltaMovement();
                if (motion.horizontalDistanceSqr() > 1.0E-4D) {
                    cart.setDeltaMovement(0.0D, motion.y, 0.0D);
                }
            }
            if (!state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, true), 3);
            }
            level.scheduleTick(pos, this, 10);
        } else {
            if (state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, false), 3);
            }
        }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return side == state.getValue(FACING).getOpposite() ? (state.getValue(POWERED) ? 15 : 0) : 0;
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
