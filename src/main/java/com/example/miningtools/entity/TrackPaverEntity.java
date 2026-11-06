package com.example.miningtools.entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.menu.TrackPaverMenu;
import com.example.miningtools.network.LocomotiveControlPayload;
import com.example.miningtools.util.TrainChainingUtil;
import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Heavy Road & Track Paver (inspired by CAT AP1055F Track Paver).
 * - Crawls at a calibrated 1.0 m/s (20 ticks per block step).
 * - Paves a 1-deep by 3-wide strip (left, center, right) with the selected paver block.
 * - When replacing blocks underneath rails, keeps track intact and undisturbed!
 * - Runs freely on solid ground (off-rails) OR snaps to rails when placed on track.
 * - Supports Cardinal Direction Lock (North, South, East, West) or free steering.
 * - Manual operation / riding mode and automated cruise paving mode.
 * - 27-slot bulk material inventory + filter slot + fuel slot.
 */
public class TrackPaverEntity extends AbstractMinecartContainer {
    public static final int INVENTORY_SIZE = 31;
    public static final int SLOT_PATTERN_LEFT = 0;
    public static final int SLOT_PATTERN_CENTER = 1;
    public static final int SLOT_PATTERN_RIGHT = 2;
    public static final int SLOT_PAVER_BLOCK = SLOT_PATTERN_CENTER; // Backward compatibility alias
    public static final int SLOT_FUEL = 3;
    public static final int SLOT_MATERIAL_START = 4;
    public static final int SLOT_MATERIAL_COUNT = 27;

    public static final int MODE_STOPPED = 0;
    public static final int MODE_FORWARD = 1;
    public static final int MODE_REVERSE = 2;

    public static final int LOCK_OFF = 0;
    public static final int LOCK_NORTH = 1;
    public static final int LOCK_SOUTH = 2;
    public static final int LOCK_WEST = 3;
    public static final int LOCK_EAST = 4;

    // 20 ticks = 1.0 second per block step = exact 1.0 m/s travel speed in Work Mode
    public static final int MOVE_TICKS_PER_BLOCK = 20;
    // 30 ticks = 1.5 seconds per block step for safe reversing in Work Mode
    public static final int REVERSE_TICKS_PER_BLOCK = 30;
    // 10 ticks = 2.0 blocks per second = 2.0 m/s travel speed when Work Mode is OFF (Transport Mode)
    public static final int FAST_MOVE_TICKS_PER_BLOCK = 10;
    public static final int FAST_REVERSE_TICKS_PER_BLOCK = 10;

    private static final EntityDataAccessor<Direction> DATA_FACING =
            SynchedEntityData.defineId(TrackPaverEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE =
            SynchedEntityData.defineId(TrackPaverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PAVING =
            SynchedEntityData.defineId(TrackPaverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_RUN_MODE =
            SynchedEntityData.defineId(TrackPaverEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CARDINAL_LOCK =
            SynchedEntityData.defineId(TrackPaverEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ON_RAILS =
            SynchedEntityData.defineId(TrackPaverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_TRAIN_NAME =
            SynchedEntityData.defineId(TrackPaverEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_WORK_LIGHT =
            SynchedEntityData.defineId(TrackPaverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HOLOGRAM =
            SynchedEntityData.defineId(TrackPaverEntity.class, EntityDataSerializers.BOOLEAN);

    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    private int fuel = 0;
    private int fuelTotal = 1600;

    // Movement step interpolation
    private boolean isMoving = false;
    private int moveTicks = 0;
    private @Nullable BlockPos currentStepPos = null;
    private @Nullable BlockPos targetStepPos = null;
    private int outOfMaterialNotifyCooldown = 0;
    private @Nullable BlockPos currentLightPos = null;

    // Treads animation rotation for client
    public float treadRotation = 0.0F;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> TrackPaverEntity.this.fuel;
                case 1 -> TrackPaverEntity.this.fuelTotal;
                case 2 -> TrackPaverEntity.this.getRunMode();
                case 3 -> TrackPaverEntity.this.getCardinalLock();
                case 4 -> TrackPaverEntity.this.isWorkMode() ? 1 : 0;
                case 5 -> TrackPaverEntity.this.isWorkLightEnabled() ? 1 : 0;
                case 6 -> TrackPaverEntity.this.isHologramEnabled() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> TrackPaverEntity.this.fuel = value;
                case 1 -> TrackPaverEntity.this.fuelTotal = value;
                case 2 -> TrackPaverEntity.this.setRunMode(value);
                case 3 -> TrackPaverEntity.this.setCardinalLock(value);
                case 4 -> TrackPaverEntity.this.setWorkMode(value != 0);
                case 5 -> TrackPaverEntity.this.setWorkLightEnabled(value != 0);
                case 6 -> TrackPaverEntity.this.setHologramEnabled(value != 0);
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public TrackPaverEntity(EntityType<? extends TrackPaverEntity> type, Level level) {
        super(type, level);
        this.clearItemStacks();
    }

    public TrackPaverEntity(EntityType<? extends TrackPaverEntity> type, Level level, double x, double y, double z) {
        super(type, level);
        this.setInitialPos(x, y, z);
        this.currentStepPos = BlockPos.containing(x, y, z);
        this.clearItemStacks();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_FACING, Direction.NORTH);
        entityData.define(DATA_ACTIVE, false);
        entityData.define(DATA_PAVING, true);
        entityData.define(DATA_RUN_MODE, MODE_STOPPED);
        entityData.define(DATA_CARDINAL_LOCK, LOCK_OFF);
        entityData.define(DATA_ON_RAILS, false);
        entityData.define(DATA_TRAIN_NAME, "");
        entityData.define(DATA_WORK_LIGHT, false);
        entityData.define(DATA_HOLOGRAM, false);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return TrainChainingUtil.canBeCollidedWith(this, entity);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return TrainChainingUtil.canCollideWith(this, entity);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public boolean isWorkMode() {
        return this.entityData.get(DATA_PAVING);
    }

    public void setWorkMode(boolean workMode) {
        this.entityData.set(DATA_PAVING, workMode);
    }

    public boolean isWorkLightEnabled() {
        return this.entityData.get(DATA_WORK_LIGHT);
    }

    public void setWorkLightEnabled(boolean enabled) {
        this.entityData.set(DATA_WORK_LIGHT, enabled);
        if (!enabled && !this.level().isClientSide()) {
            this.clearWorkLight();
        }
    }

    public boolean isHologramEnabled() {
        return this.entityData.get(DATA_HOLOGRAM);
    }

    public void setHologramEnabled(boolean enabled) {
        this.entityData.set(DATA_HOLOGRAM, enabled);
    }

    public int getTicksPerBlock(boolean isReversing) {
        if (this.isWorkMode()) {
            return isReversing ? REVERSE_TICKS_PER_BLOCK : MOVE_TICKS_PER_BLOCK;
        } else {
            return isReversing ? FAST_REVERSE_TICKS_PER_BLOCK : FAST_MOVE_TICKS_PER_BLOCK;
        }
    }

    public Direction getFacing() {
        return this.entityData.get(DATA_FACING);
    }

    public void setFacing(Direction facing) {
        if (facing.getAxis().isHorizontal()) {
            this.entityData.set(DATA_FACING, facing);
            float yRot = facing.toYRot();
            this.setYRot(yRot);
            this.yRotO = yRot;
            this.reapplyPosition();
        }
    }

    public boolean isActive() {
        return this.entityData.get(DATA_ACTIVE);
    }

    public void setActive(boolean active) {
        this.entityData.set(DATA_ACTIVE, active);
    }

    public boolean isPaving() {
        return this.entityData.get(DATA_PAVING);
    }

    public void setPaving(boolean paving) {
        this.entityData.set(DATA_PAVING, paving);
        if (paving && this.getRunMode() != MODE_STOPPED && !this.hasPaverMaterial()) {
            this.setRunMode(MODE_STOPPED);
            this.notifyOutOfMaterial(null);
        }
    }

    public int getRunMode() {
        return this.entityData.get(DATA_RUN_MODE);
    }

    public void setRunMode(int mode) {
        if (mode != MODE_STOPPED && this.isPaving() && !this.hasPaverMaterial()) {
            this.entityData.set(DATA_RUN_MODE, MODE_STOPPED);
            this.setActive(false);
            this.isMoving = false;
            this.moveTicks = 0;
            this.targetStepPos = null;
            this.setDeltaMovement(Vec3.ZERO);
            this.notifyOutOfMaterial(null);
            return;
        }
        this.entityData.set(DATA_RUN_MODE, mode);
        if (mode == MODE_STOPPED) {
            this.setActive(false);
            this.isMoving = false;
            this.moveTicks = 0;
            this.targetStepPos = null;
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    public int getCardinalLock() {
        return this.entityData.get(DATA_CARDINAL_LOCK);
    }

    public void setCardinalLock(int lock) {
        this.entityData.set(DATA_CARDINAL_LOCK, Mth.clamp(lock, 0, 4));
        Direction lockedDir = getLockedDirection(lock);
        if (lockedDir != null) {
            this.setFacing(lockedDir);
        }
    }

    public @Nullable Direction getLockedDirection(int lock) {
        return switch (lock) {
            case LOCK_NORTH -> Direction.NORTH;
            case LOCK_SOUTH -> Direction.SOUTH;
            case LOCK_WEST -> Direction.WEST;
            case LOCK_EAST -> Direction.EAST;
            default -> null;
        };
    }

    public static int getLockForDirection(Direction dir) {
        return switch (dir) {
            case NORTH -> LOCK_NORTH;
            case SOUTH -> LOCK_SOUTH;
            case WEST -> LOCK_WEST;
            case EAST -> LOCK_EAST;
            default -> LOCK_OFF;
        };
    }

    public boolean isOnRails() {
        return this.entityData.get(DATA_ON_RAILS);
    }

    public void setOnRails(boolean onRails) {
        this.entityData.set(DATA_ON_RAILS, onRails);
    }

    public String getTrainName() {
        return this.entityData.get(DATA_TRAIN_NAME);
    }

    public void setTrainName(String name) {
        this.entityData.set(DATA_TRAIN_NAME, name != null ? name.trim() : "");
    }

    @Override
    public Component getName() {
        String customName = this.getTrainName();
        if (customName != null && !customName.isEmpty()) {
            return Component.literal(customName);
        }
        return super.getName();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.TRACK_PAVER_ITEM.get());
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        Direction facing = this.entityData != null ? this.getFacing() : Direction.NORTH;
        // Bounding box: just under 3 blocks wide (2.80m), 2.0m tall, 3.0m long
        double forward = 1.50;
        double backward = 1.50;
        double halfWidth = 1.40;
        double height = 2.00;

        double centerOffset = (forward - backward) * 0.5;
        double halfLength = (forward + backward) * 0.5;

        double cx = position.x + facing.getStepX() * centerOffset;
        double cz = position.z + facing.getStepZ() * centerOffset;

        double halfX = facing.getAxis() == Direction.Axis.X ? halfLength : halfWidth;
        double halfZ = facing.getAxis() == Direction.Axis.Z ? halfLength : halfWidth;

        return new AABB(
                cx - halfX, position.y, cz - halfZ,
                cx + halfX, position.y + height, cz + halfZ
        );
    }

    @Override
    public Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float scale) {
        // Driver operator station: elevated right-side platform
        Direction facing = this.getFacing();
        Direction right = facing.getClockWise();
        double forwardOffset = 0.15D;
        double sideOffset = 0.55D;
        double seatY = 1.35D;

        double x = facing.getStepX() * forwardOffset + right.getStepX() * sideOffset;
        double z = facing.getStepZ() * forwardOffset + right.getStepZ() * sideOffset;

        return new Vec3(x, seatY, z);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.getItemInHand(hand).is(MiningTools.CROWBAR_ITEM.get())) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (!this.level().isClientSide()) {
                player.openMenu(this);
            }
            return InteractionResult.SUCCESS;
        }

        // Mount player into the operator station
        if (this.getPassengers().isEmpty() && player.getItemInHand(hand).isEmpty()) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }

        if (!this.level().isClientSide()) {
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Vec3 getDeltaMovement() {
        if (this.isActive() && this.isMoving && this.getRunMode() != MODE_STOPPED) {
            Direction dir = this.getRunMode() == MODE_REVERSE ? this.getFacing().getOpposite() : this.getFacing();
            int ticks = this.getTicksPerBlock(this.getRunMode() == MODE_REVERSE);
            double speed = 1.0D / (double) ticks;
            return new Vec3(dir.getStepX() * speed, 0.0, dir.getStepZ() * speed);
        }
        return Vec3.ZERO;
    }

    @Override
    public boolean isSilent() {
        return super.isSilent() || !this.isActive();
    }

    @Override
    public void tick() {
        if (!this.isActive() || this.getRunMode() == MODE_STOPPED || !this.isMoving) {
            this.setDeltaMovement(Vec3.ZERO);
        }

        // Update client-side animations
        if (this.level().isClientSide()) {
            if (this.isActive()) {
                float speedMultiplier = this.isWorkMode() ? 1.0F : 5.0F;
                float treadSpeed = (this.getRunMode() == MODE_REVERSE) ? -0.15F * speedMultiplier : 0.15F * speedMultiplier;
                this.treadRotation += treadSpeed;

                // Emit diesel exhaust from vertical smokestack
                Direction facing = this.getFacing();
                double stackForward = 0.20D;
                double stackSide = -0.30D;
                Direction right = facing.getClockWise();
                double stackX = this.getX() + facing.getStepX() * stackForward + right.getStepX() * stackSide;
                double stackZ = this.getZ() + facing.getStepZ() * stackForward + right.getStepZ() * stackSide;
                double stackY = this.getY() + 2.15D;

                this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        stackX, stackY, stackZ,
                        0.0, 0.08, 0.0);
            }
            return;
        }

        // Server-side work light
        if (this.isWorkLightEnabled() && !this.isRemoved()) {
            this.updateWorkLight();
        } else {
            this.clearWorkLight();
        }

        // Server tick
        if (this.outOfMaterialNotifyCooldown > 0) {
            this.outOfMaterialNotifyCooldown--;
        }

        // Verify rail beneath paver
        BlockPos actualRail = TrainChainingUtil.getCurrentBlockPosOrRailBelow(this);
        boolean hasRail = isRailAt(actualRail);
        this.setOnRails(hasRail);

        if (!hasRail) {
            this.setRunMode(MODE_STOPPED);
            this.setActive(false);
            return;
        }

        if (this.currentStepPos == null || !isRailAt(this.currentStepPos)) {
            this.currentStepPos = actualRail;
        }

        // Check if controlled by a mounted player
        Entity controllingPassenger = this.getFirstPassenger();
        if (controllingPassenger instanceof LivingEntity rider) {
            this.handleRiderControls(rider);
        }

        // Automated execution if running
        int mode = this.getRunMode();
        if (mode == MODE_FORWARD) {
            this.tickMoveStep(this.getFacing(), false);
        } else if (mode == MODE_REVERSE) {
            this.tickMoveStep(this.getFacing().getOpposite(), true);
        } else {
            this.setActive(false);
            this.isMoving = false;
            this.moveTicks = 0;
            this.targetStepPos = null;
            Vec3 restPos = getRailCenter(this.currentStepPos);
            this.setPos(restPos.x, restPos.y, restPos.z);
        }
    }

    public void updateWorkLight() {
        if (this.level().isClientSide() || this.isRemoved()) {
            return;
        }
        BlockPos center = this.blockPosition().above(2);
        if (!this.level().hasChunkAt(center)) {
            return;
        }
        BlockState state = this.level().getBlockState(center);
        if (!state.isAir() && !state.is(Blocks.LIGHT)) {
            center = this.blockPosition().above();
            if (!this.level().hasChunkAt(center)) {
                return;
            }
            state = this.level().getBlockState(center);
        }

        if (!java.util.Objects.equals(this.currentLightPos, center)) {
            this.clearWorkLight();
            if (state.isAir() || state.is(Blocks.LIGHT)) {
                this.level().setBlock(center, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
                this.currentLightPos = center;
            }
        } else if (state.isAir()) {
            this.level().setBlock(center, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
        }
    }

    public void clearWorkLight() {
        if (this.currentLightPos != null && !this.level().isClientSide()) {
            if (this.level().hasChunkAt(this.currentLightPos)) {
                BlockState state = this.level().getBlockState(this.currentLightPos);
                if (state.is(Blocks.LIGHT)) {
                    this.level().setBlock(this.currentLightPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            this.currentLightPos = null;
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (reason.shouldDestroy() && !this.level().isClientSide()) {
            this.clearWorkLight();
        }
        super.remove(reason);
    }

    private void handleRiderControls(LivingEntity rider) {
        float forwardInput = rider.zza;

        if (forwardInput > 0.1F) {
            if (this.getRunMode() == MODE_STOPPED) {
                this.setRunMode(MODE_FORWARD);
            }
        } else if (forwardInput < -0.1F) {
            if (this.getRunMode() == MODE_STOPPED) {
                this.setRunMode(MODE_REVERSE);
            }
        }
    }

    public boolean isRailAt(BlockPos pos) {
        return BaseRailBlock.isRail(this.level(), pos) || this.level().getBlockState(pos).is(BlockTags.RAILS);
    }

    public Vec3 getRailCenter(BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.0625D;
        double z = pos.getZ() + 0.5D;
        Vec3 railSnap = TrainChainingUtil.getRailPosForEntity(this, x, y, z);
        if (railSnap != null) {
            return railSnap;
        }
        return new Vec3(x, y, z);
    }

    public boolean isPassable(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        return state.isAir() || state.canBeReplaced() || state.getCollisionShape(this.level(), pos).isEmpty() || BaseRailBlock.isRail(state) || state.is(BlockTags.RAILS);
    }

    public @Nullable BlockPos findNextRailAlongTrack(BlockPos curPos, Direction travelDir) {
        BlockPos checkPos = curPos;
        BlockState curState = this.level().getBlockState(checkPos);
        if (!isRailAt(checkPos)) {
            if (isRailAt(checkPos.below())) {
                checkPos = checkPos.below();
                curState = this.level().getBlockState(checkPos);
            } else if (isRailAt(checkPos.above())) {
                checkPos = checkPos.above();
                curState = this.level().getBlockState(checkPos);
            } else {
                return null;
            }
        }

        if (curState.getBlock() instanceof BaseRailBlock railBlock) {
            RailShape shape = railBlock.getRailDirection(curState, this.level(), checkPos, this);
            Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
            Vec3i exit0 = exits.getFirst();
            Vec3i exit1 = exits.getSecond();

            int entryX = -travelDir.getStepX();
            int entryZ = -travelDir.getStepZ();

            Vec3i chosenExit;
            if (exit0.getX() == entryX && exit0.getZ() == entryZ) {
                chosenExit = exit1;
            } else if (exit1.getX() == entryX && exit1.getZ() == entryZ) {
                chosenExit = exit0;
            } else {
                int dot0 = exit0.getX() * travelDir.getStepX() + exit0.getZ() * travelDir.getStepZ();
                int dot1 = exit1.getX() * travelDir.getStepX() + exit1.getZ() * travelDir.getStepZ();
                chosenExit = (dot0 >= dot1) ? exit0 : exit1;
            }

            Direction moveDir = Direction.getNearest(chosenExit.getX(), 0, chosenExit.getZ(), travelDir);
            if (moveDir == null) {
                moveDir = travelDir;
            }

            if (chosenExit.getY() > 0) {
                BlockPos candUp = checkPos.relative(moveDir).above();
                if (isRailAt(candUp)) {
                    return candUp;
                }
            } else if (chosenExit.getY() < 0) {
                BlockPos candDown = checkPos.relative(moveDir).below();
                if (isRailAt(candDown)) {
                    return candDown;
                }
            }

            BlockPos candFlat = checkPos.relative(moveDir);
            BlockPos candUp = candFlat.above();
            BlockPos candDown = candFlat.below();

            if (isRailAt(candFlat)) {
                return candFlat;
            } else if (isRailAt(candUp)) {
                return candUp;
            } else if (isRailAt(candDown)) {
                return candDown;
            }
        }

        return null;
    }

    private void notifyDriver(Component message) {
        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof Player player) {
            player.sendOverlayMessage(message);
        } else {
            for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(8.0D))) {
                player.sendOverlayMessage(message);
            }
        }
    }

    private void tickMoveStep(Direction travelDir, boolean isReversing) {
        if (this.isWorkMode() && !this.hasPaverMaterial()) {
            this.setRunMode(MODE_STOPPED);
            this.notifyOutOfMaterial(null);
            return;
        }

        int ticksPerBlock = this.getTicksPerBlock(isReversing);

        if (!this.isMoving) {
            BlockPos actualRail = TrainChainingUtil.getCurrentBlockPosOrRailBelow(this);
            if (this.currentStepPos == null || !isRailAt(this.currentStepPos)) {
                this.currentStepPos = actualRail;
            }

            if (!isRailAt(this.currentStepPos)) {
                this.setRunMode(MODE_STOPPED);
                return;
            }

            BlockPos nextRail = this.findNextRailAlongTrack(this.currentStepPos, travelDir);
            if (nextRail == null) {
                this.setRunMode(MODE_STOPPED);
                this.notifyDriver(Component.literal("Under Track Paver: End of track!").withStyle(ChatFormatting.YELLOW));
                return;
            }

            // Impassable obstruction check
            if (!isPassable(nextRail.above()) || !isPassable(nextRail.above(2))) {
                this.setRunMode(MODE_STOPPED);
                this.notifyDriver(Component.literal("Under Track Paver: Track path obstructed!").withStyle(ChatFormatting.RED));
                return;
            }

            this.isMoving = true;
            this.moveTicks = 0;
            this.targetStepPos = nextRail;
        }

        // Consume fuel tick
        if (!this.consumeFuelTick()) {
            this.setRunMode(MODE_STOPPED);
            return;
        }

        this.setActive(true);
        this.moveTicks++;

        double frac = (double) this.moveTicks / (double) ticksPerBlock;
        Vec3 start = getRailCenter(this.currentStepPos);
        Vec3 end = getRailCenter(this.targetStepPos);
        Vec3 target = start.lerp(end, frac);

        Vec3 snap = TrainChainingUtil.getRailPosForEntity(this, target.x, target.y, target.z);
        if (snap != null) {
            target = snap;
        }

        this.setPos(target.x, target.y, target.z);

        double dx = end.x - start.x;
        double dz = end.z - start.z;
        if (Math.abs(dx) > 1.0E-4 || Math.abs(dz) > 1.0E-4) {
            float targetYaw = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
            if (isReversing) {
                targetYaw += 180.0F;
            }
            this.setYRot(targetYaw);
            this.yRotO = targetYaw;
        }

        if (isReversing) {
            this.pushTrailingCartsOnReverse(travelDir);
        } else {
            this.pullTrailingCartsOnMove();
        }

        // When halfway through the step or completed, execute paving under the screed ONLY IF in work mode!
        if (this.moveTicks == ticksPerBlock / 2 && this.isWorkMode()) {
            Direction paveDir = isReversing ? this.getFacing() : travelDir;
            this.executePavingStrip(this.targetStepPos, paveDir);
            if (this.getRunMode() == MODE_STOPPED) {
                return;
            }
        }

        if (this.moveTicks >= ticksPerBlock) {
            this.currentStepPos = this.targetStepPos;
            this.targetStepPos = null;
            this.isMoving = false;
            this.moveTicks = 0;
            this.setPos(end.x, end.y, end.z);

            if (isReversing) {
                this.pushTrailingCartsOnReverse(travelDir);
            } else {
                this.pullTrailingCartsOnMove();
            }

            Direction stepDir = Direction.getApproximateNearest(dx, 0.0, dz);
            if (stepDir != null) {
                Direction newFacing = isReversing ? stepDir.getOpposite() : stepDir;
                this.setFacing(newFacing);
            }

            if (this.isWorkMode()) {
                this.executePavingStrip(this.currentStepPos, this.getFacing());
                if (this.getRunMode() == MODE_STOPPED) {
                    return;
                }
            }
        }
    }

    /**
     * Paves a 1-deep x 3-wide strip (left, center, right) across the rear screed.
     * Preserves tracks intact when replacing blocks underneath railway rails!
     * Each of the 3 tiles is configured by its corresponding pattern slot:
     * - Tile 0 (Left): SLOT_PATTERN_LEFT
     * - Tile 1 (Center): SLOT_PATTERN_CENTER
     * - Tile 2 (Right): SLOT_PATTERN_RIGHT
     */
    public void executePavingStrip(BlockPos centerPos, Direction forwardDir) {
        if (!this.hasPaverMaterial()) {
            this.setRunMode(MODE_STOPPED);
            this.notifyOutOfMaterial(null);
            return;
        }

        Direction leftDir = forwardDir.getCounterClockWise();
        Direction rightDir = forwardDir.getClockWise();

        // Paving occurs at the ground foundation layer (Y - 1 beneath vehicle position)
        BlockPos groundCenter = centerPos.below();
        BlockPos[] strip = new BlockPos[] {
                groundCenter.relative(leftDir), // 0: Left
                groundCenter,                   // 1: Center
                groundCenter.relative(rightDir) // 2: Right
        };

        boolean placedAny = false;
        SoundType soundType = null;

        for (int i = 0; i < 3; i++) {
            BlockState targetState = this.getTargetPaverBlockState(i);
            if (targetState == null || targetState.isAir()) {
                continue; // Skip paving this tile if no pattern or unpaved
            }

            if (this.paveSingleBlock(strip[i], targetState)) {
                placedAny = true;
                soundType = targetState.getSoundType();
            } else if (this.getRunMode() == MODE_STOPPED) {
                break;
            }
        }

        if (placedAny && soundType != null) {
            this.level().playSound(null, centerPos, soundType.getPlaceSound(), SoundSource.BLOCKS, 0.8F, soundType.getPitch() * 0.9F);
        }
    }

    /**
     * Safely paves a single block position.
     * If there is a rail above pos, preserves and protects the rail from being disturbed or popped!
     */
    private boolean paveSingleBlock(BlockPos pos, BlockState targetState) {
        BlockState currentState = this.level().getBlockState(pos);
        if (currentState.equals(targetState)) {
            return false;
        }

        // Never replace indestructible blocks
        if (currentState.getDestroySpeed(this.level(), pos) < 0.0F) {
            return false;
        }

        // Check if there is a railway track on top of this block
        BlockPos abovePos = pos.above();
        BlockState aboveState = this.level().getBlockState(abovePos);
        boolean hasRailAbove = BaseRailBlock.isRail(aboveState) || aboveState.is(BlockTags.RAILS);

        // Check material availability
        if (!this.consumePaverMaterial(targetState.getBlock())) {
            this.setRunMode(MODE_STOPPED);
            this.notifyOutOfMaterial(null);
            return false;
        }

        // Collect drops of replaced block if not air or liquid
        if (!currentState.isAir() && !currentState.liquid()) {
            this.harvestReplacedBlock(pos, currentState);
        }

        if (hasRailAbove) {
            // CRITICAL: Replace block beneath rail without popping the track!
            BlockState preservedRailState = aboveState;

            // Set block with flag to avoid neighbor-triggered rail detachment
            this.level().setBlock(pos, targetState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);

            // Re-verify the rail above is still securely in place
            BlockState stateAfter = this.level().getBlockState(abovePos);
            if (!stateAfter.is(preservedRailState.getBlock())) {
                this.level().setBlock(abovePos, preservedRailState, Block.UPDATE_ALL);
            }

            // Remove any dropped rail item entities within 1.2 blocks
            this.purgeStrayRailDrops(abovePos);
        } else {
            // Standard placement: replace block and clear loose vegetation above
            this.level().setBlock(pos, targetState, Block.UPDATE_ALL);
            if (aboveState.canBeReplaced() && !aboveState.isAir()) {
                this.level().destroyBlock(abovePos, false);
            }
        }

        return true;
    }

    private void purgeStrayRailDrops(BlockPos pos) {
        AABB box = new AABB(pos).inflate(1.2);
        List<net.minecraft.world.entity.item.ItemEntity> items =
                this.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box);
        for (var item : items) {
            ItemStack stack = item.getItem();
            if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof BaseRailBlock) {
                item.discard();
            }
        }
    }

    private void harvestReplacedBlock(BlockPos pos, BlockState state) {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, null);
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            // Attempt to store in the paver's 27 material slots
            ItemStack remaining = this.insertIntoMaterialInventory(drop);
            if (!remaining.isEmpty()) {
                Containers.dropItemStack(this.level(), pos.getX(), pos.getY() + 0.5, pos.getZ(), remaining);
            }
        }
    }

    private ItemStack insertIntoMaterialInventory(ItemStack stack) {
        ItemStack toInsert = stack.copy();
        for (int i = SLOT_MATERIAL_START; i < SLOT_MATERIAL_START + SLOT_MATERIAL_COUNT; i++) {
            ItemStack slotStack = this.getItem(i);
            if (slotStack.isEmpty()) {
                this.setItem(i, toInsert);
                return ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(slotStack, toInsert)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                int transfer = Math.min(space, toInsert.getCount());
                if (transfer > 0) {
                    slotStack.grow(transfer);
                    toInsert.shrink(transfer);
                    if (toInsert.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        return toInsert;
    }

    public @Nullable BlockState getTargetPaverBlockState() {
        return getTargetPaverBlockState(1); // Default to Center tile
    }

    /**
     * Gets the target blockstate for a specific tile across the 3-wide screed:
     * - tileIndex 0: Left tile (SLOT_PATTERN_LEFT)
     * - tileIndex 1: Center tile (SLOT_PATTERN_CENTER)
     * - tileIndex 2: Right tile (SLOT_PATTERN_RIGHT)
     */
    public @Nullable BlockState getTargetPaverBlockState(int tileIndex) {
        int slot = switch (tileIndex) {
            case 0 -> SLOT_PATTERN_LEFT;
            case 1 -> SLOT_PATTERN_CENTER;
            case 2 -> SLOT_PATTERN_RIGHT;
            default -> SLOT_PATTERN_CENTER;
        };

        ItemStack filterStack = this.getItem(slot);
        if (!filterStack.isEmpty() && filterStack.getItem() instanceof BlockItem bi) {
            Block b = bi.getBlock();
            if (!(b instanceof BaseRailBlock)) {
                return b.defaultBlockState();
            }
        }

        // If all 3 pattern slots are empty, fall back to first valid full block in bulk inventory
        if (this.areAllPatternSlotsEmpty()) {
            return this.getFirstValidBulkMaterialState();
        }

        // Specific pattern is active, but this specific tile slot is empty -> skip paving this tile
        return null;
    }

    public boolean areAllPatternSlotsEmpty() {
        for (int s = SLOT_PATTERN_LEFT; s <= SLOT_PATTERN_RIGHT; s++) {
            ItemStack stack = this.getItem(s);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof BaseRailBlock)) {
                return false;
            }
        }
        return true;
    }

    public @Nullable BlockState getFirstValidBulkMaterialState() {
        for (int i = SLOT_MATERIAL_START; i < SLOT_MATERIAL_START + SLOT_MATERIAL_COUNT; i++) {
            ItemStack stack = this.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
                Block b = bi.getBlock();
                if (!(b instanceof BaseRailBlock)) {
                    return b.defaultBlockState();
                }
            }
        }
        return null;
    }

    public boolean hasPaverMaterial() {
        Entity driver = this.getFirstPassenger();
        if (driver instanceof Player player && player.isCreative()) {
            return true;
        }

        if (this.areAllPatternSlotsEmpty()) {
            // Check if ANY valid block exists in material inventory
            for (int i = SLOT_MATERIAL_START; i < SLOT_MATERIAL_START + SLOT_MATERIAL_COUNT; i++) {
                ItemStack stack = this.getItem(i);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof BaseRailBlock)) {
                    return true;
                }
            }
            // Try pulling any valid block from linked consist carts
            int pulled = TrainChainingUtil.pullItemsFromConsist(this, s -> s.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof BaseRailBlock), 16);
            return pulled > 0;
        }

        // If specific pattern slots are set, check if material exists for at least one configured block
        for (int s = SLOT_PATTERN_LEFT; s <= SLOT_PATTERN_RIGHT; s++) {
            ItemStack filterStack = this.getItem(s);
            if (!filterStack.isEmpty() && filterStack.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof BaseRailBlock)) {
                Block filterBlock = bi.getBlock();
                for (int i = SLOT_MATERIAL_START; i < SLOT_MATERIAL_START + SLOT_MATERIAL_COUNT; i++) {
                    ItemStack stack = this.getItem(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof BlockItem matBi && matBi.getBlock() == filterBlock) {
                        return true;
                    }
                }
                int pulled = TrainChainingUtil.pullItemsFromConsist(this, cStack -> cStack.getItem() instanceof BlockItem bi2 && bi2.getBlock() == filterBlock, 16);
                if (pulled > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    public void notifyOutOfMaterial(@Nullable Player targetPlayer) {
        if (targetPlayer != null) {
            targetPlayer.sendOverlayMessage(Component.literal("Under Track Paver: Out of paving material!").withStyle(ChatFormatting.RED));
            return;
        }
        if (this.outOfMaterialNotifyCooldown > 0) {
            return;
        }
        this.outOfMaterialNotifyCooldown = 40;

        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof Player rider) {
            rider.sendOverlayMessage(Component.literal("Under Track Paver: Out of paving material!").withStyle(ChatFormatting.RED));
        } else {
            for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(8.0D))) {
                player.sendOverlayMessage(Component.literal("Under Track Paver: Out of paving material!").withStyle(ChatFormatting.RED));
            }
        }
    }

    private boolean consumePaverMaterial(Block targetBlock) {
        // If driver is creative player, don't consume material
        Entity driver = this.getFirstPassenger();
        if (driver instanceof Player player && player.isCreative()) {
            return true;
        }

        for (int i = SLOT_MATERIAL_START; i < SLOT_MATERIAL_START + SLOT_MATERIAL_COUNT; i++) {
            ItemStack stack = this.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi && bi.getBlock() == targetBlock) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    this.setItem(i, ItemStack.EMPTY);
                }
                this.setChanged();
                return true;
            }
        }
        return false;
    }

    private boolean consumeFuelTick() {
        if (this.fuel <= 0) {
            ItemStack fuelStack = this.getItem(SLOT_FUEL);
            if (fuelStack.isEmpty() || !BasicLocomotiveEntity.isBurnableFuel(fuelStack)) {
                TrainChainingUtil.pullItemsFromConsist(this, BasicLocomotiveEntity::isBurnableFuel, 8);
                fuelStack = this.getItem(SLOT_FUEL);
            }
            if (!fuelStack.isEmpty() && BasicLocomotiveEntity.isBurnableFuel(fuelStack)) {
                int duration = BasicLocomotiveEntity.getItemBurnTicks(this.level(), fuelStack);
                if (duration > 0) {
                    var remainderTemplate = fuelStack.getItem().getCraftingRemainder();
                    ItemStack remainder = remainderTemplate != null ? remainderTemplate.create() : ItemStack.EMPTY;
                    fuelStack.shrink(1);
                    if (fuelStack.isEmpty()) {
                        this.setItem(SLOT_FUEL, remainder);
                    }
                    this.fuel = duration;
                    this.fuelTotal = duration;
                }
            }
        }
        if (this.fuel > 0) {
            this.fuel--;
            return true;
        }
        return false;
    }

    public static int getFuelBurnDuration(ItemStack stack) {
        return BasicLocomotiveEntity.isBurnableFuel(stack) ? 1600 : 0;
    }

    public void handleHotkeyAction(Player player, int action) {
        switch (action) {
            case LocomotiveControlPayload.ACTION_START -> {
                if (this.isPaving() && !this.hasPaverMaterial()) {
                    this.setRunMode(MODE_STOPPED);
                    player.sendOverlayMessage(Component.literal("Under Track Paver: Out of paving material!").withStyle(ChatFormatting.RED));
                    return;
                }
                this.setRunMode(MODE_FORWARD);
                String modeDesc = this.isWorkMode() ? "Work Mode (1.0 m/s)" : "Transport Mode (2.0 m/s)";
                player.sendOverlayMessage(Component.literal("Under Track Paver: Forward - " + modeDesc).withStyle(ChatFormatting.GREEN));
            }
            case LocomotiveControlPayload.ACTION_STOP -> {
                this.setRunMode(MODE_STOPPED);
                player.sendOverlayMessage(Component.literal("Under Track Paver: Stopped").withStyle(ChatFormatting.RED));
            }
            case LocomotiveControlPayload.ACTION_SPEED_DOWN, LocomotiveControlPayload.ACTION_SPEED_UP -> {
                if (this.getRunMode() == MODE_STOPPED) {
                    Direction newFacing = this.getFacing().getOpposite();
                    this.setFacing(newFacing);
                    player.sendOverlayMessage(Component.literal("Under Track Paver: Flipped orientation to " + newFacing.getName().toUpperCase()).withStyle(ChatFormatting.YELLOW));
                }
            }
            case LocomotiveControlPayload.ACTION_REVERSE -> {
                int next = (this.getRunMode() == MODE_REVERSE) ? MODE_FORWARD : MODE_REVERSE;
                if (next != MODE_STOPPED && this.isPaving() && !this.hasPaverMaterial()) {
                    this.setRunMode(MODE_STOPPED);
                    player.sendOverlayMessage(Component.literal("Under Track Paver: Out of paving material!").withStyle(ChatFormatting.RED));
                    return;
                }
                this.setRunMode(next);
                if (next == MODE_REVERSE) {
                    String modeDesc = this.isWorkMode() ? "Work Reverse (0.67 m/s)" : "Transport Reverse (2.0 m/s)";
                    player.sendOverlayMessage(Component.literal("Under Track Paver: " + modeDesc).withStyle(ChatFormatting.GOLD));
                } else {
                    String modeDesc = this.isWorkMode() ? "Work Mode (1.0 m/s)" : "Transport Mode (2.0 m/s)";
                    player.sendOverlayMessage(Component.literal("Under Track Paver: Forward - " + modeDesc).withStyle(ChatFormatting.GREEN));
                }
            }
            case LocomotiveControlPayload.ACTION_TOGGLE_LIGHT -> {
                boolean nextLight = !this.isWorkLightEnabled();
                this.setWorkLightEnabled(nextLight);
                player.sendOverlayMessage(Component.literal("Under Track Paver: Work Light " + (nextLight ? "ON" : "OFF")).withStyle(nextLight ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            }
            case LocomotiveControlPayload.ACTION_HORN -> {
                if (player.isShiftKeyDown()) {
                    boolean nextHolo = !this.isHologramEnabled();
                    this.setHologramEnabled(nextHolo);
                    player.sendOverlayMessage(Component.literal("Under Track Paver: Alignment Hologram " + (nextHolo ? "ON" : "OFF")).withStyle(nextHolo ? ChatFormatting.AQUA : ChatFormatting.GRAY));
                } else {
                    boolean nextWork = !this.isWorkMode();
                    this.setWorkMode(nextWork);
                    player.sendOverlayMessage(Component.literal("Under Track Paver: " + (nextWork ? "Work Mode (Paving 1.0 m/s)" : "Transport Mode (No Pave 2.0 m/s)")).withStyle(nextWork ? ChatFormatting.GREEN : ChatFormatting.GOLD));
                }
            }
            case LocomotiveControlPayload.ACTION_OPEN_GUI -> player.openMenu(this);
            default -> {}
        }
    }

    private void pullTrailingCartsOnMove() {
        UUID nextId = TrainChainingUtil.getCartChildUUID(this);
        Entity prevEnt = this;
        Vec3 prevAnchor = this.getRearAnchorPos();
        Set<UUID> visited = new HashSet<>();
        visited.add(this.getUUID());

        while (nextId != null && visited.add(nextId)) {
            Entity ent = this.level().getEntity(nextId);
            if (!(ent instanceof AbstractMinecart cart) || cart.isRemoved()) {
                break;
            }
            boolean isFirst = (prevEnt == this);
            double targetDist = isFirst
                    ? (TrainChainingUtil.getCartCouplerRadius(cart) + TrainChainingUtil.COUPLER_GAP + 0.6D)
                    : TrainChainingUtil.getTargetCouplingDistance(prevEnt, cart, false);

            Vec3 cartPos = cart.position();
            Vec3 diff = prevAnchor.subtract(cartPos);
            double dist = diff.horizontalDistance();
            if (dist > targetDist + 0.06D) {
                double pull = Math.min(dist - targetDist, 0.25D);
                double nx = cart.getX() + (diff.x / dist) * pull;
                double nz = cart.getZ() + (diff.z / dist) * pull;
                cart.setPos(nx, cart.getY(), nz);
            }
            prevEnt = cart;
            prevAnchor = cart.position();
            nextId = TrainChainingUtil.getCartChildUUID(cart);
        }
    }

    private void pushTrailingCartsOnReverse(Direction backwardDir) {
        UUID nextId = TrainChainingUtil.getCartChildUUID(this);
        Entity prevEnt = this;
        Vec3 prevAnchor = this.getRearAnchorPos();
        Set<UUID> visited = new HashSet<>();
        visited.add(this.getUUID());

        while (nextId != null && visited.add(nextId)) {
            Entity ent = this.level().getEntity(nextId);
            if (!(ent instanceof AbstractMinecart cart) || cart.isRemoved()) {
                break;
            }
            boolean isFirst = (prevEnt == this);
            double targetDist = isFirst
                    ? (TrainChainingUtil.getCartCouplerRadius(cart) + TrainChainingUtil.COUPLER_GAP + 0.6D)
                    : TrainChainingUtil.getTargetCouplingDistance(prevEnt, cart, false);

            Vec3 cartPos = cart.position();
            Vec3 diff = prevAnchor.subtract(cartPos);
            double dist = diff.horizontalDistance();
            if (dist < targetDist - 0.05D && dist > 1.0E-4D) {
                double push = Math.min(targetDist - dist, 0.25D);
                double nx = cart.getX() - (diff.x / dist) * push;
                double nz = cart.getZ() - (diff.z / dist) * push;
                cart.setPos(nx, cart.getY(), nz);
            }
            prevEnt = cart;
            prevAnchor = cart.position();
            nextId = TrainChainingUtil.getCartChildUUID(cart);
        }
    }

    public Vec3 getRearAnchorPos() {
        Direction backward = this.getFacing().getOpposite();
        double offset = 1.45D;
        return new Vec3(
                this.getX() + backward.getStepX() * offset,
                this.getY(),
                this.getZ() + backward.getStepZ() * offset
        );
    }

    // AbstractMinecartContainer methods
    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return this.itemStacks;
    }

    @Override
    public void clearItemStacks() {
        this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
        return new TrackPaverMenu(id, playerInventory, this, this.dataAccess);
    }

    @Override
    protected Item getDropItem() {
        return MiningTools.TRACK_PAVER_ITEM.get();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("PaverVersion", 1);
        output.putInt("Facing", this.getFacing().get3DDataValue());
        output.putInt("Fuel", this.fuel);
        output.putInt("FuelTotal", this.fuelTotal);
        output.putInt("RunMode", this.getRunMode());
        output.putInt("CardinalLock", this.getCardinalLock());
        output.putBoolean("Paving", this.isPaving());
        output.putBoolean("OnRails", this.isOnRails());
        output.putString("TrainName", this.getTrainName());
        output.putBoolean("WorkLight", this.isWorkLightEnabled());
        output.putBoolean("Hologram", this.isHologramEnabled());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        int version = input.getIntOr("PaverVersion", 0);
        if (version == 0 && this.itemStacks.size() >= INVENTORY_SIZE) {
            // Version 0 migration:
            // Old Slot 0: filter
            // Old Slot 1: fuel
            // Old Slots 2..28: 27 materials
            ItemStack oldPattern = this.itemStacks.get(0).copy();
            ItemStack oldFuel = this.itemStacks.get(1).copy();
            NonNullList<ItemStack> oldMaterials = NonNullList.withSize(SLOT_MATERIAL_COUNT, ItemStack.EMPTY);
            for (int i = 0; i < SLOT_MATERIAL_COUNT && (2 + i) < this.itemStacks.size(); i++) {
                oldMaterials.set(i, this.itemStacks.get(2 + i).copy());
            }

            this.itemStacks.set(SLOT_PATTERN_LEFT, oldPattern.copy());
            this.itemStacks.set(SLOT_PATTERN_CENTER, oldPattern.copy());
            this.itemStacks.set(SLOT_PATTERN_RIGHT, oldPattern.copy());
            this.itemStacks.set(SLOT_FUEL, oldFuel);
            for (int i = 0; i < SLOT_MATERIAL_COUNT; i++) {
                this.itemStacks.set(SLOT_MATERIAL_START + i, oldMaterials.get(i));
            }
        }

        int facingVal = input.getIntOr("Facing", Direction.NORTH.get3DDataValue());
        this.setFacing(Direction.from3DDataValue(facingVal));
        this.fuel = input.getIntOr("Fuel", 0);
        this.fuelTotal = input.getIntOr("FuelTotal", 1600);
        this.setRunMode(input.getIntOr("RunMode", MODE_STOPPED));
        this.setCardinalLock(input.getIntOr("CardinalLock", LOCK_OFF));
        this.setPaving(input.getBooleanOr("Paving", true));
        this.setOnRails(input.getBooleanOr("OnRails", false));
        this.setTrainName(input.getStringOr("TrainName", ""));
        this.setWorkLightEnabled(input.getBooleanOr("WorkLight", false));
        this.setHologramEnabled(input.getBooleanOr("Hologram", false));
    }
}
