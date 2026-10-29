package com.example.miningtools.entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.menu.TrackLayerCartMenu;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.phys.Vec3;

/**
 * Compact Monolithic Track Laying & Removing Cart.
 * - Sits cleanly on rails as a compact monolithic 1-block machine.
 * - In Laying Mode: Places 1 piece of track from its inventory directly in front of itself.
 * - In Removing Mode: Picks up the track behind it after traveling over it and collects it into inventory.
 * - In Work Modes (Laying or Removing): Operates at an exact, calibrated speed of 1.5 blocks/s.
 * - In Normal Mode (Work Mode OFF): Behaves as a standard minecart (free rolling, pushable, standard speeds).
 * - Fully driveable by player with W/S and hotkeys, or runnable via GUI controls.
 * - Seamlessly shares materials (tracks) across linked consist carts (pulls rails when needed, pushes collected rails).
 */
public class TrackLayerCartEntity extends AbstractMinecartContainer {
    public static final int INVENTORY_SIZE = 28;
    public static final int SLOT_PATTERN = 0;
    public static final int SLOT_STORAGE_START = 1;
    public static final int SLOT_STORAGE_COUNT = 27;

    public static final int WORK_MODE_LAY = 0;
    public static final int WORK_MODE_REMOVE = 1;
    public static final int WORK_MODE_OFF = 2;

    public static final int RUN_MODE_STOPPED = 0;
    public static final int RUN_MODE_FORWARD = 1;
    public static final int RUN_MODE_REVERSE = 2;

    // 1.5 blocks/second = 20 ticks / 1.5 blocks ~ 13.33 ticks per block step
    public static final int WORK_MOVE_TICKS_PER_BLOCK = 13;

    private static final EntityDataAccessor<Direction> DATA_FACING =
            SynchedEntityData.defineId(TrackLayerCartEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Integer> DATA_WORK_MODE =
            SynchedEntityData.defineId(TrackLayerCartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RUN_MODE =
            SynchedEntityData.defineId(TrackLayerCartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE =
            SynchedEntityData.defineId(TrackLayerCartEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_TRAIN_NAME =
            SynchedEntityData.defineId(TrackLayerCartEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_STORED_RAILS =
            SynchedEntityData.defineId(TrackLayerCartEntity.class, EntityDataSerializers.INT);

    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    // Movement step state for calibrated 1.5 blocks/s work mode
    private boolean isStepMoving = false;
    private int stepTicks = 0;
    private @Nullable BlockPos currentRailPos = null;
    private @Nullable BlockPos targetRailPos = null;
    private @Nullable BlockPos previousRailPos = null;
    private int notifyCooldown = 0;

    public float rollerAnimationRotation = 0.0F;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> TrackLayerCartEntity.this.getWorkMode();
                case 1 -> TrackLayerCartEntity.this.getRunMode();
                case 2 -> TrackLayerCartEntity.this.isActive() ? 1 : 0;
                case 3 -> TrackLayerCartEntity.this.getStoredRailCount();
                case 4 -> TrackLayerCartEntity.this.getFacing().get2DDataValue();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> TrackLayerCartEntity.this.setWorkMode(value);
                case 1 -> TrackLayerCartEntity.this.setRunMode(value);
                case 2 -> TrackLayerCartEntity.this.setActive(value != 0);
                case 4 -> TrackLayerCartEntity.this.setFacing(Direction.from2DDataValue(value));
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public TrackLayerCartEntity(EntityType<? extends TrackLayerCartEntity> type, Level level) {
        super(type, level);
    }

    public TrackLayerCartEntity(Level level, double x, double y, double z) {
        super(MiningTools.TRACK_LAYER_CART_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FACING, Direction.NORTH);
        builder.define(DATA_WORK_MODE, WORK_MODE_LAY);
        builder.define(DATA_RUN_MODE, RUN_MODE_STOPPED);
        builder.define(DATA_ACTIVE, false);
        builder.define(DATA_TRAIN_NAME, "");
        builder.define(DATA_STORED_RAILS, 0);
    }

    public Direction getFacing() {
        return this.entityData.get(DATA_FACING);
    }

    public void setFacing(Direction facing) {
        this.entityData.set(DATA_FACING, facing);
    }

    public int getWorkMode() {
        return this.entityData.get(DATA_WORK_MODE);
    }

    public void setWorkMode(int mode) {
        this.entityData.set(DATA_WORK_MODE, mode);
    }

    public boolean isWorkMode() {
        return this.getWorkMode() != WORK_MODE_OFF;
    }

    public int getRunMode() {
        return this.entityData.get(DATA_RUN_MODE);
    }

    public void setRunMode(int mode) {
        this.entityData.set(DATA_RUN_MODE, mode);
        this.setActive(mode != RUN_MODE_STOPPED);
    }

    public boolean isActive() {
        return this.entityData.get(DATA_ACTIVE);
    }

    public void setActive(boolean active) {
        this.entityData.set(DATA_ACTIVE, active);
    }

    public String getTrainName() {
        return this.entityData.get(DATA_TRAIN_NAME);
    }

    public void setTrainName(String name) {
        this.entityData.set(DATA_TRAIN_NAME, name);
    }

    public int getStoredRailCount() {
        return this.entityData.get(DATA_STORED_RAILS);
    }

    public void updateStoredRailCount() {
        int count = 0;
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = this.getItem(i);
            if (!stack.isEmpty() && isRailItem(stack)) {
                count += stack.getCount();
            }
        }
        this.entityData.set(DATA_STORED_RAILS, count);
    }

    public static boolean isRailItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(ItemTags.RAILS)) return true;
        if (stack.getItem() instanceof BlockItem bi) {
            return bi.getBlock() instanceof BaseRailBlock;
        }
        return false;
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float scale) {
        // Driver operator station recessed cleanly within monolithic chassis
        Direction facing = this.getFacing();
        double forwardOffset = -0.05D;
        double seatY = 0.22D;
        double x = facing.getStepX() * forwardOffset;
        double z = facing.getStepZ() * forwardOffset;
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
        if (this.notifyCooldown > 0) {
            this.notifyCooldown--;
        }

        if (this.level().isClientSide()) {
            if (this.isActive()) {
                this.rollerAnimationRotation += 0.15F;
            }
            super.tick();
            return;
        }

        updateStoredRailCount();

        // When Work Mode is OFF: act as normal minecart
        if (!this.isWorkMode()) {
            if (!TrainChainingUtil.shouldSkipVanillaServerTick(this)) {
                super.tick();
            }
            TrainChainingUtil.tickConsistMovement(this);
            return;
        }

        // Work Mode is ON (Laying or Removing at 1.5 blocks/s)
        this.tickWorkModeMovement();
    }

    private void tickWorkModeMovement() {
        if (this.getRunMode() == RUN_MODE_STOPPED) {
            this.isStepMoving = false;
            this.setDeltaMovement(Vec3.ZERO);
            TrainChainingUtil.tickConsistMovement(this);
            return;
        }

        boolean isReversing = (this.getRunMode() == RUN_MODE_REVERSE);
        Direction travelDir = isReversing ? this.getFacing().getOpposite() : this.getFacing();

        if (!this.isStepMoving) {
            if (this.currentRailPos == null) {
                this.currentRailPos = TrainChainingUtil.getCurrentBlockPosOrRailBelow(this);
            }

            BlockPos nextPos = this.currentRailPos.relative(travelDir);

            // Laying Mode: Check if track is needed in front and lay it
            if (this.getWorkMode() == WORK_MODE_LAY) {
                BlockState nextState = this.level().getBlockState(nextPos);
                if (!nextState.is(BlockTags.RAILS) && !(nextState.getBlock() instanceof BaseRailBlock)) {
                    // Check ground support
                    BlockPos groundPos = nextPos.below();
                    BlockState groundState = this.level().getBlockState(groundPos);
                    if (!groundState.isFaceSturdy(this.level(), groundPos, Direction.UP)) {
                        this.setRunMode(RUN_MODE_STOPPED);
                        notifyDriver(Component.literal("Track Layer: Ground ahead is not sturdy!").withStyle(ChatFormatting.RED));
                        return;
                    }

                    // Check clearance at track position
                    if (!nextState.isAir() && !nextState.canBeReplaced()) {
                        this.setRunMode(RUN_MODE_STOPPED);
                        notifyDriver(Component.literal("Track Layer: Track path blocked!").withStyle(ChatFormatting.RED));
                        return;
                    }

                    // Find track in inventory or pull from linked consist carts
                    int railSlot = findRailSlot();
                    if (railSlot == -1) {
                        TrainChainingUtil.pullItemsFromConsist(this, TrackLayerCartEntity::isRailItem, 16);
                        railSlot = findRailSlot();
                    }

                    if (railSlot == -1) {
                        this.setRunMode(RUN_MODE_STOPPED);
                        notifyDriver(Component.literal("Track Layer: Out of track!").withStyle(ChatFormatting.RED));
                        return;
                    }

                    ItemStack railStack = this.getItem(railSlot);
                    if (railStack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof BaseRailBlock railBlock) {
                        railStack.shrink(1);
                        this.level().setBlock(nextPos, railBlock.defaultBlockState(), 3);
                        SoundType st = railBlock.defaultBlockState().getSoundType();
                        this.level().playSound(null, nextPos, st.getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                        if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.CRIT, nextPos.getX() + 0.5D, nextPos.getY() + 0.2D, nextPos.getZ() + 0.5D, 5, 0.2D, 0.1D, 0.2D, 0.05D);
                        }
                    } else {
                        this.setRunMode(RUN_MODE_STOPPED);
                        return;
                    }
                }
            } else if (this.getWorkMode() == WORK_MODE_REMOVE) {
                // In Remove Mode, ensure rail exists ahead to drive on
                BlockState nextState = this.level().getBlockState(nextPos);
                if (!nextState.is(BlockTags.RAILS) && !(nextState.getBlock() instanceof BaseRailBlock)) {
                    this.setRunMode(RUN_MODE_STOPPED);
                    notifyDriver(Component.literal("Track Layer: Reached end of track line!").withStyle(ChatFormatting.GOLD));
                    return;
                }
            }

            this.isStepMoving = true;
            this.stepTicks = 0;
            this.targetRailPos = nextPos;
        }

        // Calibrated 1.5 blocks/s travel speed: 13 ticks per block step
        this.stepTicks++;
        double frac = (double) this.stepTicks / (double) WORK_MOVE_TICKS_PER_BLOCK;
        Vec3 start = Vec3.atBottomCenterOf(this.currentRailPos).add(0.0, 0.0625, 0.0);
        Vec3 end = Vec3.atBottomCenterOf(this.targetRailPos).add(0.0, 0.0625, 0.0);
        Vec3 currentPos = start.lerp(end, Math.min(frac, 1.0D));
        this.setPos(currentPos.x, currentPos.y, currentPos.z);
        this.pullTrailingCartsOnMove();

        if (this.stepTicks >= WORK_MOVE_TICKS_PER_BLOCK) {
            this.previousRailPos = this.currentRailPos;
            this.currentRailPos = this.targetRailPos;
            this.targetRailPos = null;
            this.isStepMoving = false;

            // In Removing Mode: Pick up track behind the cart now that it has traveled over it
            if (this.getWorkMode() == WORK_MODE_REMOVE && this.previousRailPos != null && !this.previousRailPos.equals(this.currentRailPos)) {
                harvestRailBehind(this.previousRailPos);
            }
        }
    }

    private void harvestRailBehind(BlockPos railPos) {
        BlockState state = this.level().getBlockState(railPos);
        if (state.is(BlockTags.RAILS) || state.getBlock() instanceof BaseRailBlock) {
            Item dropItem = state.getBlock().asItem();
            ItemStack harvested = dropItem != Items.AIR ? new ItemStack(dropItem) : new ItemStack(Items.RAIL);

            this.level().destroyBlock(railPos, false, this);
            SoundType st = state.getSoundType();
            this.level().playSound(null, railPos, st.getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

            // Place harvested rail into local inventory
            int inserted = TrainChainingUtil.insertItemIntoContainer(this, harvested);
            if (inserted < harvested.getCount()) {
                ItemStack remainder = harvested.copyWithCount(harvested.getCount() - inserted);
                // Push excess into linked consist carts (like cargo carts)
                int pushed = TrainChainingUtil.pushItemsToConsist(this, TrackLayerCartEntity::isRailItem, remainder.getCount());
                if (pushed < remainder.getCount()) {
                    remainder.shrink(pushed);
                    if (!this.level().isClientSide() && this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                        this.spawnAtLocation(sl, remainder);
                    }
                }
            }
        }
    }

    private int findRailSlot() {
        // First check pattern slot if specified
        ItemStack pattern = this.getItem(SLOT_PATTERN);
        if (!pattern.isEmpty() && isRailItem(pattern)) {
            for (int i = SLOT_STORAGE_START; i < SLOT_STORAGE_START + SLOT_STORAGE_COUNT; i++) {
                ItemStack st = this.getItem(i);
                if (!st.isEmpty() && ItemStack.isSameItemSameComponents(st, pattern)) {
                    return i;
                }
            }
        }
        // Fallback: check storage slots for any rail
        for (int i = SLOT_STORAGE_START; i < SLOT_STORAGE_START + SLOT_STORAGE_COUNT; i++) {
            ItemStack st = this.getItem(i);
            if (!st.isEmpty() && isRailItem(st)) {
                return i;
            }
        }
        return -1;
    }

    public void handleHotkeyAction(Player player, int action) {
        switch (action) {
            case LocomotiveControlPayload.ACTION_START -> {
                // W key: Forward
                this.setRunMode(RUN_MODE_FORWARD);
                String modeName = switch (this.getWorkMode()) {
                    case WORK_MODE_LAY -> "Laying Mode (1.5 m/s)";
                    case WORK_MODE_REMOVE -> "Removing Mode (1.5 m/s)";
                    default -> "Transport Mode (Normal Minecart)";
                };
                player.sendOverlayMessage(Component.literal("Track Layer: Forward - " + modeName).withStyle(ChatFormatting.GREEN));
            }
            case LocomotiveControlPayload.ACTION_STOP -> {
                // S key: Stop
                this.setRunMode(RUN_MODE_STOPPED);
                player.sendOverlayMessage(Component.literal("Track Layer: Stopped").withStyle(ChatFormatting.RED));
            }
            case LocomotiveControlPayload.ACTION_REVERSE -> {
                // R key: Toggle Reverse
                int next = (this.getRunMode() == RUN_MODE_REVERSE) ? RUN_MODE_FORWARD : RUN_MODE_REVERSE;
                this.setRunMode(next);
                String dirName = (next == RUN_MODE_REVERSE) ? "Reverse" : "Forward";
                player.sendOverlayMessage(Component.literal("Track Layer: " + dirName).withStyle(ChatFormatting.GOLD));
            }
            case LocomotiveControlPayload.ACTION_HORN -> {
                // H key: Cycle Work Mode (LAY -> REMOVE -> OFF)
                int nextMode = (this.getWorkMode() + 1) % 3;
                this.setWorkMode(nextMode);
                String modeName = switch (nextMode) {
                    case WORK_MODE_LAY -> "Laying Mode (1.5 m/s)";
                    case WORK_MODE_REMOVE -> "Removing Mode (1.5 m/s)";
                    default -> "Transport Mode (Work Mode OFF)";
                };
                player.sendOverlayMessage(Component.literal("Track Layer Mode: " + modeName).withStyle(ChatFormatting.AQUA));
                this.level().playSound(null, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 1.2F);
            }
            case LocomotiveControlPayload.ACTION_OPEN_GUI -> player.openMenu(this);
            default -> {}
        }
    }

    private void notifyDriver(Component message) {
        if (this.notifyCooldown > 0) return;
        this.notifyCooldown = 40;
        Entity driver = this.getFirstPassenger();
        if (driver instanceof Player player) {
            player.sendOverlayMessage(message);
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
                    ? (TrainChainingUtil.getCartCouplerRadius(cart) + TrainChainingUtil.COUPLER_GAP + 0.35D)
                    : TrainChainingUtil.getTargetCouplingDistance(prevEnt, cart, false);

            Vec3 cartPos = cart.position();
            Vec3 diff = prevAnchor.subtract(cartPos);
            double dist = diff.horizontalDistance();
            if (dist > targetDist + 0.05D) {
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
        double offset = 0.65D;
        return new Vec3(
                this.getX() + backward.getStepX() * offset,
                this.getY(),
                this.getZ() + backward.getStepZ() * offset
        );
    }

    // AbstractMinecartContainer implementation
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
        return new TrackLayerCartMenu(id, playerInventory, this, this.dataAccess);
    }

    @Override
    protected Item getDropItem() {
        return MiningTools.TRACK_LAYER_CART_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.TRACK_LAYER_CART_ITEM.get());
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 0;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Facing", this.getFacing().get2DDataValue());
        output.putInt("WorkMode", this.getWorkMode());
        output.putInt("RunMode", this.getRunMode());
        output.putBoolean("Active", this.isActive());
        output.putString("TrainName", this.getTrainName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setFacing(Direction.from2DDataValue(input.getIntOr("Facing", Direction.NORTH.get2DDataValue())));
        this.setWorkMode(input.getIntOr("WorkMode", WORK_MODE_LAY));
        this.setRunMode(input.getIntOr("RunMode", RUN_MODE_STOPPED));
        this.setActive(input.getBooleanOr("Active", false));
        this.setTrainName(input.getStringOr("TrainName", ""));
    }
}
