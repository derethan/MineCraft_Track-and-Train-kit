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

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
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
    public static final int INVENTORY_SIZE = 29;
    public static final int SLOT_PAVER_BLOCK = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_MATERIAL_START = 2;
    public static final int SLOT_MATERIAL_COUNT = 27;

    public static final int MODE_STOPPED = 0;
    public static final int MODE_FORWARD = 1;
    public static final int MODE_REVERSE = 2;

    public static final int LOCK_OFF = 0;
    public static final int LOCK_NORTH = 1;
    public static final int LOCK_SOUTH = 2;
    public static final int LOCK_WEST = 3;
    public static final int LOCK_EAST = 4;

    // 20 ticks = 1.0 second per block step = exact 1.0 m/s travel speed
    public static final int MOVE_TICKS_PER_BLOCK = 20;
    // 30 ticks = 1.5 seconds per block step for safe reversing
    public static final int REVERSE_TICKS_PER_BLOCK = 30;

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

    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    private int fuel = 0;
    private int fuelTotal = 1600;

    // Movement step interpolation
    private boolean isMoving = false;
    private int moveTicks = 0;
    private @Nullable BlockPos currentStepPos = null;
    private @Nullable BlockPos targetStepPos = null;

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
                case 4 -> TrackPaverEntity.this.isPaving() ? 1 : 0;
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
                case 4 -> TrackPaverEntity.this.setPaving(value != 0);
            }
        }

        @Override
        public int getCount() {
            return 5;
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
    }

    public int getRunMode() {
        return this.entityData.get(DATA_RUN_MODE);
    }

    public void setRunMode(int mode) {
        this.entityData.set(DATA_RUN_MODE, mode);
        if (mode == MODE_STOPPED) {
            this.setActive(false);
            this.isMoving = false;
            this.moveTicks = 0;
            this.targetStepPos = null;
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
    public void tick() {
        // Update client-side animations
        if (this.level().isClientSide()) {
            if (this.isActive()) {
                this.treadRotation += 0.15F;
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

        // Server tick
        if (this.currentStepPos == null) {
            this.currentStepPos = BlockPos.containing(this.position());
        }

        // Detect if rail is directly beneath tracks
        BlockPos belowPos = this.blockPosition();
        BlockState belowState = this.level().getBlockState(belowPos);
        BlockState underState = this.level().getBlockState(belowPos.below());
        boolean hasRail = BaseRailBlock.isRail(belowState) || BaseRailBlock.isRail(underState);
        this.setOnRails(hasRail);

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
        }
    }

    private void handleRiderControls(LivingEntity rider) {
        float forwardInput = rider.zza;
        float strafeInput = rider.xxa;

        if (this.getCardinalLock() == LOCK_OFF && Math.abs(strafeInput) > 0.05F) {
            float newYaw = this.getYRot() + strafeInput * 2.0F;
            this.setYRot(newYaw);
            this.setFacing(Direction.fromYRot(newYaw));
        }

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

    private void tickMoveStep(Direction travelDir, boolean isReversing) {
        int ticksPerBlock = isReversing ? REVERSE_TICKS_PER_BLOCK : MOVE_TICKS_PER_BLOCK;

        if (!this.isMoving) {
            // Determine next target block
            BlockPos nextPos = this.currentStepPos.relative(travelDir);

            // Terrain step-up: check if next position is obstructed by a 1-block rise
            BlockState nextFeetState = this.level().getBlockState(nextPos);
            BlockState nextHeadState = this.level().getBlockState(nextPos.above());
            if (nextFeetState.isSolid() && !nextHeadState.isSolid()) {
                // Ground steps up by 1 block
                nextPos = nextPos.above();
            } else if (!nextFeetState.isSolid() && !this.level().getBlockState(nextPos.below()).isSolid()) {
                // Ground steps down by 1 block
                BlockPos downPos = nextPos.below();
                if (this.level().getBlockState(downPos).isSolid() || this.level().getBlockState(downPos.below()).isSolid()) {
                    nextPos = downPos;
                }
            }

            // Impassable wall check (bedrock or unbreakable)
            if (this.level().getBlockState(nextPos).getDestroySpeed(this.level(), nextPos) < 0.0F
                    || this.level().getBlockState(nextPos.above()).getDestroySpeed(this.level(), nextPos.above()) < 0.0F) {
                this.setRunMode(MODE_STOPPED);
                return;
            }

            this.isMoving = true;
            this.moveTicks = 0;
            this.targetStepPos = nextPos;
        }

        // Consume fuel tick
        if (!this.consumeFuelTick()) {
            this.setRunMode(MODE_STOPPED);
            return;
        }

        this.setActive(true);
        this.moveTicks++;

        double frac = (double) this.moveTicks / (double) ticksPerBlock;
        Vec3 start = Vec3.atBottomCenterOf(this.currentStepPos);
        Vec3 end = Vec3.atBottomCenterOf(this.targetStepPos);
        Vec3 target = start.lerp(end, frac);

        this.setPos(target.x, target.y, target.z);
        this.pullTrailingCartsOnMove();

        // When halfway through the step or completed, execute paving under the screed
        if (this.moveTicks == ticksPerBlock / 2 && this.isPaving()) {
            this.executePavingStrip(this.targetStepPos, travelDir);
        }

        if (this.moveTicks >= ticksPerBlock) {
            this.currentStepPos = this.targetStepPos;
            this.targetStepPos = null;
            this.isMoving = false;
            this.moveTicks = 0;
            this.setPos(end.x, end.y, end.z);
            this.pullTrailingCartsOnMove();

            if (this.isPaving()) {
                this.executePavingStrip(this.currentStepPos, travelDir);
            }
        }
    }

    /**
     * Paves a 1-deep x 3-wide strip (left, center, right) across the rear screed.
     * Preserves tracks intact when replacing blocks underneath railway rails!
     */
    public void executePavingStrip(BlockPos centerPos, Direction forwardDir) {
        BlockState targetPaverState = this.getTargetPaverBlockState();
        if (targetPaverState == null || targetPaverState.isAir()) {
            return;
        }

        Direction leftDir = forwardDir.getCounterClockWise();
        Direction rightDir = forwardDir.getClockWise();

        // Paving occurs at the ground foundation layer (Y - 1 beneath vehicle position)
        BlockPos groundCenter = centerPos.below();
        BlockPos[] strip = new BlockPos[] {
                groundCenter.relative(leftDir),
                groundCenter,
                groundCenter.relative(rightDir)
        };

        boolean placedAny = false;
        for (BlockPos pos : strip) {
            if (this.paveSingleBlock(pos, targetPaverState)) {
                placedAny = true;
            }
        }

        if (placedAny) {
            SoundType soundType = targetPaverState.getSoundType();
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
        // 1. Check Slot 0 (Filter / Target Block Slot)
        ItemStack filterStack = this.getItem(SLOT_PAVER_BLOCK);
        if (!filterStack.isEmpty() && filterStack.getItem() instanceof BlockItem bi) {
            return bi.getBlock().defaultBlockState();
        }

        // 2. If Slot 0 is empty, take the first valid full block found in the material inventory
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
                return true;
            }
        }
        return false;
    }

    private boolean consumeFuelTick() {
        if (this.fuel <= 0) {
            ItemStack fuelStack = this.getItem(SLOT_FUEL);
            if (!fuelStack.isEmpty()) {
                int duration = getFuelBurnDuration(fuelStack);
                if (duration > 0) {
                    fuelStack.shrink(1);
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
        if (stack.isEmpty()) return 0;
        if (stack.is(Items.COAL_BLOCK)) return 14400;
        if (stack.is(ItemTags.COALS)) return 1600;
        if (stack.is(Items.BLAZE_ROD)) return 2400;
        if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS)) return 300;
        return 0;
    }

    public void handleHotkeyAction(Player player, int action) {
        switch (action) {
            case LocomotiveControlPayload.ACTION_START -> {
                this.setRunMode(MODE_FORWARD);
                player.sendOverlayMessage(Component.literal("Track Paver: Paving Forward (1.0 m/s)").withStyle(ChatFormatting.GREEN));
            }
            case LocomotiveControlPayload.ACTION_STOP -> {
                this.setRunMode(MODE_STOPPED);
                player.sendOverlayMessage(Component.literal("Track Paver: Stopped").withStyle(ChatFormatting.RED));
            }
            case LocomotiveControlPayload.ACTION_SPEED_DOWN -> {
                // A key: Turn Left (counter-clockwise)
                Direction newFacing = this.getFacing().getCounterClockWise();
                this.setFacing(newFacing);
                if (this.getCardinalLock() != LOCK_OFF) {
                    this.setCardinalLock(getLockForDirection(newFacing));
                }
                player.sendOverlayMessage(Component.literal("Track Paver: Turned Left to " + newFacing.getName().toUpperCase()).withStyle(ChatFormatting.YELLOW));
            }
            case LocomotiveControlPayload.ACTION_SPEED_UP -> {
                // D key: Turn Right (clockwise)
                Direction newFacing = this.getFacing().getClockWise();
                this.setFacing(newFacing);
                if (this.getCardinalLock() != LOCK_OFF) {
                    this.setCardinalLock(getLockForDirection(newFacing));
                }
                player.sendOverlayMessage(Component.literal("Track Paver: Turned Right to " + newFacing.getName().toUpperCase()).withStyle(ChatFormatting.YELLOW));
            }
            case LocomotiveControlPayload.ACTION_REVERSE -> {
                int next = (this.getRunMode() == MODE_REVERSE) ? MODE_FORWARD : MODE_REVERSE;
                this.setRunMode(next);
                if (next == MODE_REVERSE) {
                    player.sendOverlayMessage(Component.literal("Track Paver: Reversing").withStyle(ChatFormatting.GOLD));
                } else {
                    player.sendOverlayMessage(Component.literal("Track Paver: Paving Forward (1.0 m/s)").withStyle(ChatFormatting.GREEN));
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
        output.putInt("Facing", this.getFacing().get3DDataValue());
        output.putInt("Fuel", this.fuel);
        output.putInt("FuelTotal", this.fuelTotal);
        output.putInt("RunMode", this.getRunMode());
        output.putInt("CardinalLock", this.getCardinalLock());
        output.putBoolean("Paving", this.isPaving());
        output.putBoolean("OnRails", this.isOnRails());
        output.putString("TrainName", this.getTrainName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int facingVal = input.getIntOr("Facing", Direction.NORTH.get3DDataValue());
        this.setFacing(Direction.from3DDataValue(facingVal));
        this.fuel = input.getIntOr("Fuel", 0);
        this.fuelTotal = input.getIntOr("FuelTotal", 1600);
        this.setRunMode(input.getIntOr("RunMode", MODE_STOPPED));
        this.setCardinalLock(input.getIntOr("CardinalLock", LOCK_OFF));
        this.setPaving(input.getBooleanOr("Paving", true));
        this.setOnRails(input.getBooleanOr("OnRails", false));
        this.setTrainName(input.getStringOr("TrainName", ""));
    }
}
