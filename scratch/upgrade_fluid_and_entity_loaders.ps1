$ErrorActionPreference = "Stop"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

# 1. Update FluidLoaderBlock.java (default POWERED = true)
$flBlockPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\block\FluidLoaderBlock.java"
$flBlockContent = [System.IO.File]::ReadAllText($flBlockPath, $utf8NoBom)
$flBlockContent = $flBlockContent.Replace(".setValue(POWERED, false)", ".setValue(POWERED, true)")
[System.IO.File]::WriteAllText($flBlockPath, $flBlockContent, $utf8NoBom)
Write-Host "Updated FluidLoaderBlock.java"

# 2. Update FluidLoaderBlockEntity.java to hold cart (POWERED = false) while loading/unloading and release (POWERED = true) when done
$flBePath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\block\entity\FluidLoaderBlockEntity.java"
$flBeContent = [System.IO.File]::ReadAllText($flBePath, $utf8NoBom)

if (-not $flBeContent.Contains("import com.example.miningtools.util.TrainChainingUtil;")) {
    $flBeContent = $flBeContent.Replace("import com.example.miningtools.menu.FluidLoaderMenu;", "import com.example.miningtools.menu.FluidLoaderMenu;`r`nimport com.example.miningtools.util.TrainChainingUtil;`r`nimport net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;")
}

$oldServerTick = @'
        if (cartFluidHandler != null) {
            blockEntity.cartPresent = true;
            blockEntity.cartFluidAmount = cartFluidHandler.getAmountAsInt(0);
            blockEntity.cartFluidCapacity = cartFluidHandler.getCapacityAsInt(0, cartFluidHandler.getResource(0));
            if (blockEntity.cartFluidCapacity <= 0) {
                blockEntity.cartFluidCapacity = TANK_CAPACITY;
            }

            int mode = blockEntity.transferMode;
            boolean movedAny = false;

            if (mode == MODE_UNLOAD || (mode == MODE_AUTO && blockEntity.cartFluidAmount > 0)) {
                // Drain cart into station buffer
                int moved = ResourceHandlerUtil.move(cartFluidHandler, blockEntity.tank, r -> true, TRANSFER_RATE, null);
                if (moved > 0) {
                    movedAny = true;
                }
            } else if (mode == MODE_LOAD || (mode == MODE_AUTO && blockEntity.cartFluidAmount == 0)) {
                // Fill cart from station buffer
                int moved = ResourceHandlerUtil.move(blockEntity.tank, cartFluidHandler, r -> true, TRANSFER_RATE, null);
                if (moved > 0) {
                    movedAny = true;
                }
            }

            // Update cart amount after move
            int newCartAmount = cartFluidHandler.getAmountAsInt(0);
            blockEntity.cartFluidAmount = newCartAmount;

            // Check redstone completion trigger
            boolean justFinished = false;
            if (blockEntity.redstoneMode == REDSTONE_WHEN_EMPTY) {
                if (newCartAmount == 0 && (movedAny || blockEntity.lastCartWasTransferring)) {
                    justFinished = true;
                }
            } else if (blockEntity.redstoneMode == REDSTONE_WHEN_FULL) {
                if (ResourceHandlerUtil.isFull(cartFluidHandler) && (movedAny || blockEntity.lastCartWasTransferring)) {
                    justFinished = true;
                }
            } else if (blockEntity.redstoneMode == REDSTONE_WHEN_DONE) {
                if (!movedAny && blockEntity.lastCartWasTransferring) {
                    justFinished = true;
                }
            }

            if (justFinished && blockEntity.pulseTicks <= 0) {
                blockEntity.triggerRedstonePulse(level, pos, state);
            }

            blockEntity.lastCartWasTransferring = movedAny;
        } else {
            blockEntity.cartPresent = false;
            blockEntity.cartFluidAmount = 0;
            blockEntity.cartFluidCapacity = 0;
            blockEntity.lastCartWasTransferring = false;
        }

        // 3. Handle redstone pulse countdown
        if (blockEntity.pulseTicks > 0) {
            blockEntity.pulseTicks--;
            if (blockEntity.pulseTicks == 0) {
                // Pulse expired -> turn off redstone power
                setPoweredState(level, pos, state, false);
            }
        }
'@

$newServerTick = @'
        if (blockEntity.pulseTicks > 0) {
            blockEntity.pulseTicks--;
        }

        if (cartFluidHandler != null && blockEntity.pulseTicks <= 0) {
            blockEntity.cartPresent = true;
            blockEntity.cartFluidAmount = cartFluidHandler.getAmountAsInt(0);
            blockEntity.cartFluidCapacity = cartFluidHandler.getCapacityAsInt(0, cartFluidHandler.getResource(0));
            if (blockEntity.cartFluidCapacity <= 0) {
                blockEntity.cartFluidCapacity = TANK_CAPACITY;
            }

            int mode = blockEntity.transferMode;
            boolean movedAny = false;

            if (mode == MODE_UNLOAD || (mode == MODE_AUTO && blockEntity.cartFluidAmount > 0)) {
                int moved = ResourceHandlerUtil.move(cartFluidHandler, blockEntity.tank, r -> true, TRANSFER_RATE, null);
                if (moved > 0) {
                    movedAny = true;
                }
            } else if (mode == MODE_LOAD || (mode == MODE_AUTO && blockEntity.cartFluidAmount == 0)) {
                int moved = ResourceHandlerUtil.move(blockEntity.tank, cartFluidHandler, r -> true, TRANSFER_RATE, null);
                if (moved > 0) {
                    movedAny = true;
                }
            }

            if (targetCart instanceof LiquidMinecartEntity lme && movedAny) {
                lme.syncFluidData();
            }

            int newCartAmount = cartFluidHandler.getAmountAsInt(0);
            blockEntity.cartFluidAmount = newCartAmount;
            boolean cartFull = newCartAmount >= blockEntity.cartFluidCapacity || ResourceHandlerUtil.isFull(cartFluidHandler);

            boolean shouldHold = false;
            if (blockEntity.redstoneMode != REDSTONE_DISABLED) {
                if (blockEntity.redstoneMode == REDSTONE_WHEN_EMPTY) {
                    shouldHold = newCartAmount > 0;
                } else if (blockEntity.redstoneMode == REDSTONE_WHEN_FULL) {
                    shouldHold = !cartFull;
                } else if (blockEntity.redstoneMode == REDSTONE_WHEN_DONE) {
                    shouldHold = movedAny;
                }
            }

            if (shouldHold) {
                setPoweredState(level, pos, state, false);
                if (targetCart instanceof AbstractMinecart mc) {
                    TrainChainingUtil.freezeConsistFromBrakingRail(mc);
                }
                blockEntity.lastCartWasTransferring = true;
            } else {
                boolean wasHolding = blockEntity.lastCartWasTransferring || !state.getValue(FluidLoaderBlock.POWERED);
                setPoweredState(level, pos, state, true);
                if (wasHolding) {
                    blockEntity.pulseTicks = 35; // Departure grace period
                    if (targetCart instanceof AbstractMinecart mc) {
                        TrainChainingUtil.releaseConsistFromBrakingRail(mc);
                    }
                }
                blockEntity.lastCartWasTransferring = false;
            }
        } else if (cartFluidHandler == null) {
            blockEntity.cartPresent = false;
            blockEntity.cartFluidAmount = 0;
            blockEntity.cartFluidCapacity = 0;
            blockEntity.lastCartWasTransferring = false;
            setPoweredState(level, pos, state, blockEntity.redstoneMode != REDSTONE_DISABLED);
        }
'@

$flBeContent = $flBeContent.Replace($oldServerTick, $newServerTick)
[System.IO.File]::WriteAllText($flBePath, $flBeContent, $utf8NoBom)
Write-Host "Updated FluidLoaderBlockEntity.java"

# 3. Update EntityLoaderBlock.java & EntityUnloaderBlock.java to add POWERED property (default true)
$elBlockPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\block\EntityLoaderBlock.java"
$elBlockContent = @'
package com.example.miningtools.block;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.EntityLoaderBlockEntity;
import com.example.miningtools.network.EntityTransferSyncPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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

public class EntityLoaderBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public EntityLoaderBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, true));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, true);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EntityLoaderBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        if (type == MiningTools.ENTITY_LOADER_BLOCK_ENTITY.get()) {
            return (lvl, pos, st, be) -> EntityLoaderBlockEntity.serverTick(lvl, pos, st, (EntityLoaderBlockEntity) be);
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
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof EntityLoaderBlockEntity be) {
                PacketDistributor.sendToPlayer(serverPlayer, new EntityTransferSyncPayload(
                        pos,
                        false,
                        be.getWhitelist(),
                        be.getBlacklist(),
                        be.getAgeFilter(),
                        be.getRange()
                ));
            }
        }
        return InteractionResult.SUCCESS;
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
'@
[System.IO.File]::WriteAllText($elBlockPath, $elBlockContent, $utf8NoBom)
Write-Host "Updated EntityLoaderBlock.java"

$euBlockPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\block\EntityUnloaderBlock.java"
$euBlockContent = @'
package com.example.miningtools.block;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.EntityUnloaderBlockEntity;
import com.example.miningtools.network.EntityTransferSyncPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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

public class EntityUnloaderBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public EntityUnloaderBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, true));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, true);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EntityUnloaderBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        if (type == MiningTools.ENTITY_UNLOADER_BLOCK_ENTITY.get()) {
            return (lvl, pos, st, be) -> EntityUnloaderBlockEntity.serverTick(lvl, pos, st, (EntityUnloaderBlockEntity) be);
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
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof EntityUnloaderBlockEntity be) {
                PacketDistributor.sendToPlayer(serverPlayer, new EntityTransferSyncPayload(
                        pos,
                        true,
                        be.getWhitelist(),
                        be.getBlacklist(),
                        be.getAgeFilter(),
                        be.getRange()
                ));
            }
        }
        return InteractionResult.SUCCESS;
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
'@
[System.IO.File]::WriteAllText($euBlockPath, $euBlockContent, $utf8NoBom)
Write-Host "Updated EntityUnloaderBlock.java"

# 4. Update EntityLoaderBlockEntity.java and EntityUnloaderBlockEntity.java for rail holding
$elBePath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\block\entity\EntityLoaderBlockEntity.java"
$elBeContent = @'
package com.example.miningtools.block.entity;

import java.util.Comparator;
import java.util.List;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.EntityLoaderBlock;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.entity.LivestockCartEntity;
import com.example.miningtools.util.TrainChainingUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class EntityLoaderBlockEntity extends AbstractEntityTransferBlockEntity {

    public EntityLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(MiningTools.ENTITY_LOADER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EntityLoaderBlockEntity be) {
        if (be.cooldown > 0) {
            be.cooldown--;
        }

        BlockPos trackPos = pos.above();
        AABB trackBox = new AABB(trackPos).inflate(0.85, 0.5, 0.85);
        List<AbstractMinecart> carts = level.getEntitiesOfClass(
                AbstractMinecart.class,
                trackBox,
                c -> c.isAlive()
                        && !c.isRemoved()
                        && !(c instanceof BasicLocomotiveEntity)
                        && !(c instanceof DieselLocomotiveEntity)
                        && !(c instanceof ElectricLocomotiveEntity)
                        && (c instanceof LivestockCartEntity lc ? lc.canFitMorePassengers() : c.getPassengers().isEmpty())
        );

        if (carts.isEmpty()) {
            setRailPower(level, pos, state, true);
            return;
        }

        AbstractMinecart targetCart = carts.get(0);

        AABB searchBox = new AABB(pos).inflate(be.getRange());
        List<Entity> candidates = level.getEntities(
                (Entity) null,
                searchBox,
                e -> e.isAlive()
                        && !e.isRemoved()
                        && e != targetCart
                        && !e.isPassenger()
                        && !e.isVehicle()
                        && !(e instanceof AbstractMinecart)
                        && be.matchesFilters(e)
        );

        if (candidates.isEmpty()) {
            boolean wasHolding = !state.getValue(EntityLoaderBlock.POWERED);
            setRailPower(level, pos, state, true);
            if (wasHolding) {
                TrainChainingUtil.releaseConsistFromBrakingRail(targetCart);
            }
            return;
        }

        // Hold cart on rail while loading candidates
        setRailPower(level, pos, state, false);
        TrainChainingUtil.freezeConsistFromBrakingRail(targetCart);

        if (be.cooldown > 0) {
            return;
        }

        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));
        Entity chosen = candidates.get(0);

        chosen.teleportTo(targetCart.getX(), targetCart.getY(), targetCart.getZ());
        boolean mounted = chosen.startRiding(targetCart);

        if (mounted) {
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.2F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetCart.getX(), targetCart.getY() + 0.5, targetCart.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
            }
            be.cooldown = 15;

            boolean stillHasRoom = (targetCart instanceof LivestockCartEntity lc)
                    ? lc.canFitMorePassengers()
                    : targetCart.getPassengers().isEmpty();
            if (!stillHasRoom || candidates.size() <= 1) {
                setRailPower(level, pos, state, true);
                TrainChainingUtil.releaseConsistFromBrakingRail(targetCart);
            }
        }
    }

    private static void setRailPower(Level level, BlockPos pos, BlockState state, boolean powered) {
        if (state.hasProperty(EntityLoaderBlock.POWERED) && state.getValue(EntityLoaderBlock.POWERED) != powered) {
            level.setBlock(pos, state.setValue(EntityLoaderBlock.POWERED, powered), 3);
            level.updateNeighborsAt(pos, state.getBlock());
            level.updateNeighborsAt(pos.above(), state.getBlock());
        }
    }
}
'@
[System.IO.File]::WriteAllText($elBePath, $elBeContent, $utf8NoBom)
Write-Host "Updated EntityLoaderBlockEntity.java"

$euBePath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\block\entity\EntityUnloaderBlockEntity.java"
$euBeContent = @'
package com.example.miningtools.block.entity;

import java.util.List;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.EntityUnloaderBlock;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.util.TrainChainingUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class EntityUnloaderBlockEntity extends AbstractEntityTransferBlockEntity {

    public EntityUnloaderBlockEntity(BlockPos pos, BlockState state) {
        super(MiningTools.ENTITY_UNLOADER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EntityUnloaderBlockEntity be) {
        if (be.cooldown > 0) {
            be.cooldown--;
        }

        BlockPos trackPos = pos.above();
        AABB trackBox = new AABB(trackPos).inflate(0.85, 0.5, 0.85);
        List<AbstractMinecart> carts = level.getEntitiesOfClass(
                AbstractMinecart.class,
                trackBox,
                c -> c.isAlive()
                        && !c.isRemoved()
                        && !(c instanceof BasicLocomotiveEntity)
                        && !(c instanceof DieselLocomotiveEntity)
                        && !(c instanceof ElectricLocomotiveEntity)
                        && !c.getPassengers().isEmpty()
        );

        if (carts.isEmpty()) {
            setRailPower(level, pos, state, true);
            return;
        }

        AbstractMinecart cart = carts.get(0);
        List<Entity> passengers = List.copyOf(cart.getPassengers());
        Entity matchingPassenger = null;
        int matchingCount = 0;

        for (Entity passenger : passengers) {
            if (be.matchesFilters(passenger)) {
                if (matchingPassenger == null) {
                    matchingPassenger = passenger;
                }
                matchingCount++;
            }
        }

        if (matchingPassenger == null) {
            boolean wasHolding = !state.getValue(EntityUnloaderBlock.POWERED);
            setRailPower(level, pos, state, true);
            if (wasHolding) {
                TrainChainingUtil.releaseConsistFromBrakingRail(cart);
            }
            return;
        }

        // Depower rail above and hold consist until all matching passengers are unloaded
        setRailPower(level, pos, state, false);
        TrainChainingUtil.freezeConsistFromBrakingRail(cart);

        if (be.cooldown > 0) {
            return;
        }

        matchingPassenger.stopRiding();
        BlockPos safePos = findSafeUnloadPosition(level, trackPos, be.getRange());
        matchingPassenger.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);

        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.2F);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, safePos.getX() + 0.5, safePos.getY() + 0.5, safePos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.05);
        }
        be.cooldown = 15;

        if (matchingCount <= 1) {
            setRailPower(level, pos, state, true);
            TrainChainingUtil.releaseConsistFromBrakingRail(cart);
        }
    }

    private static void setRailPower(Level level, BlockPos pos, BlockState state, boolean powered) {
        if (state.hasProperty(EntityUnloaderBlock.POWERED) && state.getValue(EntityUnloaderBlock.POWERED) != powered) {
            level.setBlock(pos, state.setValue(EntityUnloaderBlock.POWERED, powered), 3);
            level.updateNeighborsAt(pos, state.getBlock());
            level.updateNeighborsAt(pos.above(), state.getBlock());
        }
    }

    public static BlockPos findSafeUnloadPosition(Level level, BlockPos trackPos, int range) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos target = trackPos.relative(dir);
            if (isSafeLanding(level, target)) {
                return target;
            }
        }

        int maxR = Math.min(range, 8);
        for (int r = 2; r <= maxR; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) == r || Math.abs(dz) == r) {
                        for (int dy = -1; dy <= 2; dy++) {
                            BlockPos target = trackPos.offset(dx, dy, dz);
                            if (isSafeLanding(level, target)) {
                                return target;
                            }
                        }
                    }
                }
            }
        }

        return trackPos.relative(Direction.EAST);
    }

    private static boolean isSafeLanding(Level level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState ground = level.getBlockState(pos.below());

        if (BaseRailBlock.isRail(feet) || BaseRailBlock.isRail(ground)) {
            return false;
        }
        boolean feetPassable = feet.isAir() || feet.canBeReplaced();
        boolean headPassable = head.isAir() || head.canBeReplaced();
        boolean groundSolid = ground.isFaceSturdy(level, pos.below(), Direction.UP);

        return feetPassable && headPassable && groundSolid;
    }
}
'@
[System.IO.File]::WriteAllText($euBePath, $euBeContent, $utf8NoBom)
Write-Host "Updated EntityUnloaderBlockEntity.java"
