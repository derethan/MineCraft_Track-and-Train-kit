package com.example.miningtools.entity;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.menu.TunnelBoreMenu;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TunnelBoreEntity extends AbstractMinecartContainer {
    public static final int INVENTORY_SIZE = 10;
    public static final int FUEL_SLOT = 0;

    public static final int MODE_STOPPED = 0;
    public static final int MODE_FORWARD = 1;
    public static final int MODE_REVERSE = 2;

    // 15 ticks = 0.75 seconds per 1-block track step when moving without mining
    public static final int MOVE_TICKS_PER_BLOCK = 15;
    // 25 ticks = 1.25 seconds per 1-block track step for very slow, safe reversing
    public static final int REVERSE_TICKS_PER_BLOCK = 25;
    // Standard Iron (6.0) / Diamond (8.0) tool speed
    private static final float BORE_TOOL_SPEED = 7.5F;

    private static final EntityDataAccessor<Direction> DATA_FACING =
            SynchedEntityData.defineId(TunnelBoreEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE =
            SynchedEntityData.defineId(TunnelBoreEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DRILLING =
            SynchedEntityData.defineId(TunnelBoreEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_RUN_MODE =
            SynchedEntityData.defineId(TunnelBoreEntity.class, EntityDataSerializers.INT);

    // Stack walker for soft-detecting caller mod interactions (such as minecart_trains_fork)
    private static final StackWalker STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static @Nullable Method GET_PARENT_UUID_METHOD = null;
    private static boolean CHECKED_PARENT_UUID_METHOD = false;

    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    private boolean running = false;
    private int fuel = 0;
    private int fuelTotal = 1600;

    // Fixed block-by-block movement state
    private @Nullable BlockPos currentRailPos = null;
    private boolean isMoving = false;
    private int moveTicks = 0;
    private @Nullable BlockPos moveTargetRailPos = null;

    // Per-block mining state
    private @Nullable BlockPos miningTargetPos = null;
    private int currentBlockMineProgress = 0;
    private int currentBlockTotalTicks = 10;

    public float boreHeadRotation = 0.0F;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> TunnelBoreEntity.this.fuel;
                case 1 -> TunnelBoreEntity.this.fuelTotal;
                case 2 -> TunnelBoreEntity.this.getRunMode();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> TunnelBoreEntity.this.fuel = value;
                case 1 -> TunnelBoreEntity.this.fuelTotal = value;
                case 2 -> TunnelBoreEntity.this.setRunMode(value);
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public TunnelBoreEntity(EntityType<? extends TunnelBoreEntity> type, Level level) {
        super(type, level);
        this.clearItemStacks();
    }

    public TunnelBoreEntity(EntityType<? extends TunnelBoreEntity> type, Level level, double x, double y, double z) {
        super(type, level);
        this.setInitialPos(x, y, z);
        this.currentRailPos = BlockPos.containing(x, y, z);
        this.clearItemStacks();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_FACING, Direction.NORTH);
        entityData.define(DATA_ACTIVE, false);
        entityData.define(DATA_DRILLING, false);
        entityData.define(DATA_RUN_MODE, MODE_STOPPED);
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

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_FACING.equals(accessor)) {
            this.reapplyPosition();
        }
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        Direction facing = this.entityData != null ? this.getFacing() : Direction.NORTH;
        // Full 3D model bounds: +1.55 blocks forward (cutter head) to -4.60 blocks backward (rear compartment),
        // 2.90 blocks wide (+/- 1.45), and 2.70 blocks tall.
        double forward = 1.55;
        double backward = 4.60;
        double halfWidth = 1.45;
        double height = 2.70;

        double centerOffset = (forward - backward) * 0.5;
        double halfLength = (forward + backward) * 0.5;

        double cx = position.x + facing.getStepX() * centerOffset;
        double cz = position.z + facing.getStepZ() * centerOffset;

        double halfX = facing.getAxis() == Direction.Axis.X ? halfLength : halfWidth;
        double halfZ = facing.getAxis() == Direction.Axis.Z ? halfLength : halfWidth;

        return new AABB(
                cx - halfX,
                position.y,
                cz - halfZ,
                cx + halfX,
                position.y + height,
                cz + halfZ
        );
    }

    public int getRunMode() {
        return this.entityData.get(DATA_RUN_MODE);
    }

    public void setRunMode(int mode) {
        this.entityData.set(DATA_RUN_MODE, mode);
        this.running = (mode != MODE_STOPPED);
        if (mode == MODE_STOPPED) {
            this.clearMiningTarget();
            this.isMoving = false;
            this.moveTicks = 0;
            this.setDrilling(false);
            this.setActive(false);
        } else {
            this.setActive(true);
        }
    }

    public boolean isRunning() {
        return this.getRunMode() != MODE_STOPPED;
    }

    public void setRunning(boolean running) {
        this.setRunMode(running ? MODE_FORWARD : MODE_STOPPED);
    }

    public boolean isReversing() {
        return this.getRunMode() == MODE_REVERSE;
    }

    public boolean isActive() {
        return this.entityData.get(DATA_ACTIVE);
    }

    public void setActive(boolean active) {
        this.entityData.set(DATA_ACTIVE, active);
    }

    public boolean isDrilling() {
        return this.entityData.get(DATA_DRILLING);
    }

    public void setDrilling(boolean drilling) {
        this.entityData.set(DATA_DRILLING, drilling);
    }

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
        this.itemStacks = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
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
    protected Item getDropItem() {
        return MiningTools.TUNNEL_BORE_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.TUNNEL_BORE_ITEM.get());
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && this.getPassengers().isEmpty();
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (this.hasPassenger(passenger)) {
            Direction facing = this.getFacing();
            // Driver cabin seat is located behind the boiler/furnace inside the cab platform
            double offsetX = -facing.getStepX() * 1.8;
            double offsetZ = -facing.getStepZ() * 1.8;
            double offsetY = 1.05;
            moveFunction.accept(passenger, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ);
        }
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

    public void handleHotkeyAction(Player player, int action) {
        switch (action) {
            case LocomotiveControlPayload.ACTION_START -> {
                if (this.getRunMode() != MODE_FORWARD) {
                    this.setRunMode(MODE_FORWARD);
                    player.sendOverlayMessage(Component.literal("Tunnel Bore: Digging Forward").withStyle(ChatFormatting.GREEN));
                }
            }
            case LocomotiveControlPayload.ACTION_STOP -> {
                if (this.getRunMode() == MODE_FORWARD) {
                    this.setRunMode(MODE_STOPPED);
                    player.sendOverlayMessage(Component.literal("Tunnel Bore: Stopped").withStyle(ChatFormatting.RED));
                } else if (this.getRunMode() == MODE_STOPPED) {
                    this.setRunMode(MODE_REVERSE);
                    player.sendOverlayMessage(Component.literal("Tunnel Bore: Reversing Slowly").withStyle(ChatFormatting.YELLOW));
                } else if (this.getRunMode() == MODE_REVERSE) {
                    this.setRunMode(MODE_STOPPED);
                    player.sendOverlayMessage(Component.literal("Tunnel Bore: Stopped").withStyle(ChatFormatting.RED));
                }
            }
            case LocomotiveControlPayload.ACTION_REVERSE -> {
                if (this.getRunMode() == MODE_REVERSE) {
                    this.setRunMode(MODE_STOPPED);
                    player.sendOverlayMessage(Component.literal("Tunnel Bore: Stopped").withStyle(ChatFormatting.RED));
                } else {
                    this.setRunMode(MODE_REVERSE);
                    player.sendOverlayMessage(Component.literal("Tunnel Bore: Reversing Slowly").withStyle(ChatFormatting.YELLOW));
                }
            }
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return !entity.isPassengerOfSameVehicle(this);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return !this.isRemoved();
    }

    /**
     * Checks whether the current call originates from the minecart_trains_fork mod.
     */
    private static boolean isCalledByTrainChaining() {
        return STACK_WALKER.walk(frames ->
                frames.limit(6).anyMatch(f -> f.getClassName().startsWith("org.fuseleaf.minecarttrainsfork"))
        );
    }

    /**
     * Checks if a world position is located behind the tunnel bore (opposite to facing direction).
     */
    public boolean isBehindBore(Vec3 pos) {
        Direction facing = this.getFacing();
        double dx = pos.x - super.position().x;
        double dz = pos.z - super.position().z;
        return (dx * facing.getStepX() + dz * facing.getStepZ()) < 0;
    }

    /**
     * Calculates the rear hitch / anchor point for cart linking (e.g. minecart_trains_fork).
     * The model rear bumper sits at -4.65 blocks from the front position.
     * Subtracting the standard minecart half-width (0.49 blocks) places the virtual coupler center
     * at -4.16 blocks so that chains attach precisely to the rear bumper and trailing carts follow with
     * a clean half-block gap.
     */
    public Vec3 getRearAnchorPos() {
        Direction facing = this.getFacing();
        double offset = 4.16;
        return new Vec3(
                super.position().x - facing.getStepX() * offset,
                super.position().y,
                super.position().z - facing.getStepZ() * offset
        );
    }

    /**
     * Calculates the front anchor point (+1.06 blocks forward).
     */
    public Vec3 getFrontAnchorPos() {
        Direction facing = this.getFacing();
        double offset = 1.06;
        return new Vec3(
                super.position().x + facing.getStepX() * offset,
                super.position().y,
                super.position().z + facing.getStepZ() * offset
        );
    }

    @Override
    public Vec3 position() {
        if (isCalledByTrainChaining()) {
            return this.getRearAnchorPos();
        }
        return super.position();
    }

    @Override
    public float distanceTo(Entity entity) {
        if (isCalledByTrainChaining() || (entity instanceof AbstractMinecart && this.isBehindBore(entity.position()))) {
            return (float) this.getRearAnchorPos().distanceTo(entity.position());
        }
        return super.distanceTo(entity);
    }

    @Override
    public double distanceToSqr(Entity entity) {
        if (isCalledByTrainChaining() || (entity instanceof AbstractMinecart && this.isBehindBore(entity.position()))) {
            return this.getRearAnchorPos().distanceToSqr(entity.position());
        }
        return super.distanceToSqr(entity);
    }

    @Override
    public double distanceToSqr(Vec3 vec) {
        if (isCalledByTrainChaining() || this.isBehindBore(vec)) {
            return this.getRearAnchorPos().distanceToSqr(vec);
        }
        return super.distanceToSqr(vec);
    }

    @Override
    public double distanceToSqr(double x, double y, double z) {
        Vec3 target = new Vec3(x, y, z);
        if (isCalledByTrainChaining() || this.isBehindBore(target)) {
            return this.getRearAnchorPos().distanceToSqr(target);
        }
        return super.distanceToSqr(x, y, z);
    }

    @Override
    public Vec3 getDeltaMovement() {
        if (this.isMoving) {
            Direction dir = this.getRunMode() == MODE_REVERSE ? this.getFacing().getOpposite() : this.getFacing();
            double speed = this.getRunMode() == MODE_REVERSE ? 0.05 : 0.12;
            return new Vec3(dir.getStepX() * speed, 0.0, dir.getStepZ() * speed);
        }
        return Vec3.ZERO;
    }

    private boolean isChainedTo(Entity entity) {
        return TrainChainingUtil.isChainedTo(this, entity);
    }

    public static @Nullable TunnelBoreEntity getConnectedBore(Entity entity) {
        if (entity instanceof TunnelBoreEntity bore && !bore.isRemoved()) {
            return bore;
        }
        if (!(entity instanceof AbstractMinecart cart) || cart.isRemoved()) {
            return null;
        }

        Set<UUID> visited = new HashSet<>();
        visited.add(cart.getUUID());
        Deque<UUID> toCheck = new ArrayDeque<>();
        UUID p = TrainChainingUtil.getCartParentUUID(cart);
        if (p != null) toCheck.add(p);
        UUID c = TrainChainingUtil.getCartChildUUID(cart);
        if (c != null) toCheck.add(c);

        while (!toCheck.isEmpty()) {
            UUID nextId = toCheck.poll();
            if (nextId != null && visited.add(nextId)) {
                Entity ent = cart.level().getEntity(nextId);
                if (ent instanceof TunnelBoreEntity bore && !bore.isRemoved()) {
                    return bore;
                }
                if (ent instanceof AbstractMinecart otherCart && !otherCart.isRemoved()) {
                    UUID cp = TrainChainingUtil.getCartParentUUID(otherCart);
                    if (cp != null && !visited.contains(cp)) toCheck.add(cp);
                    UUID cc = TrainChainingUtil.getCartChildUUID(otherCart);
                    if (cc != null && !visited.contains(cc)) toCheck.add(cc);
                }
            }
        }
        return null;
    }


    @Override
    public void push(Entity entity) {
        if (!this.level().isClientSide() && !entity.noPhysics && !this.hasPassenger(entity)) {
            if (this.isChainedTo(entity)) {
                return;
            }
            Direction facing = this.getFacing();
            double cx = this.getX() + facing.getStepX() * -1.525;
            double cz = this.getZ() + facing.getStepZ() * -1.525;
            double xa = entity.getX() - cx;
            double za = entity.getZ() - cz;
            double dd = xa * xa + za * za;
            if (dd >= 1.0E-4F) {
                dd = Math.sqrt(dd);
                xa /= dd;
                za /= dd;
                entity.push(xa * 0.08, 0.0, za * 0.08);
            }
        }
    }

    @Override
    public void push(double xa, double ya, double za) {
        // Prevent external forces from displacing the tunnel bore off its track
    }

    @Override
    public float getPickRadius() {
        return 0.2F;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new TunnelBoreMenu(containerId, inventory, this, this.dataAccess);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Running", this.isRunning());
        output.putInt("RunMode", this.getRunMode());
        output.putInt("Fuel", this.fuel);
        output.putInt("FuelTotal", this.fuelTotal);
        output.putByte("Facing", (byte) this.getFacing().get2DDataValue());
        if (this.currentRailPos != null) {
            output.putInt("RailX", this.currentRailPos.getX());
            output.putInt("RailY", this.currentRailPos.getY());
            output.putInt("RailZ", this.currentRailPos.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int savedMode = input.getIntOr("RunMode", input.getBooleanOr("Running", false) ? MODE_FORWARD : MODE_STOPPED);
        this.setRunMode(savedMode);
        this.fuel = input.getIntOr("Fuel", 0);
        this.fuelTotal = input.getIntOr("FuelTotal", 1600);
        this.setFacing(Direction.from2DDataValue(input.getByteOr("Facing", (byte) 0)));
        int rx = input.getIntOr("RailX", Integer.MIN_VALUE);
        int ry = input.getIntOr("RailY", Integer.MIN_VALUE);
        int rz = input.getIntOr("RailZ", Integer.MIN_VALUE);
        if (rx != Integer.MIN_VALUE && ry != Integer.MIN_VALUE && rz != Integer.MIN_VALUE) {
            this.currentRailPos = new BlockPos(rx, ry, rz);
        }
    }

    private void clearMiningTarget() {
        if (this.miningTargetPos != null && !this.level().isClientSide()) {
            this.level().destroyBlockProgress(this.getId(), this.miningTargetPos, -1);
        }
        this.miningTargetPos = null;
        this.currentBlockMineProgress = 0;
    }

    private boolean hasOrCanRefuel() {
        if (this.fuel > 0) {
            return true;
        }
        ItemStack fuelStack = this.getItem(FUEL_SLOT);
        return !fuelStack.isEmpty() && fuelStack.is(ItemTags.COALS);
    }

    private boolean consumeFuelTick() {
        if (this.fuel <= 0) {
            ItemStack fuelStack = this.getItem(FUEL_SLOT);
            if (!fuelStack.isEmpty() && fuelStack.is(ItemTags.COALS)) {
                fuelStack.shrink(1);
                this.fuel = 1600;
                this.fuelTotal = 1600;
            }
        }
        if (this.fuel > 0) {
            this.fuel--;
            return true;
        }
        return false;
    }

    private boolean shouldMineBlock(BlockPos pos, BlockState state, int dx, int dy) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
            return false;
        }
        // Do not destroy the center track along the floor path
        if (dx == 0 && dy == 0 && state.is(BlockTags.RAILS)) {
            return false;
        }
        return true;
    }

    private @Nullable BlockPos findNextBlockToMine(BlockPos baseRailPos, Direction facing, Direction right) {
        // Check dist 1 (shroud) and dist 2 (right in front of the cutter head)
        for (int dist = 1; dist <= 2; dist++) {
            BlockPos layerCenter = baseRailPos.relative(facing, dist);
            // Mine top-to-bottom (dy = 2 down to 0) so gravity blocks like gravel/sand are handled cleanly
            for (int dy = 2; dy >= 0; dy--) {
                for (int dx = -1; dx <= 1; dx++) {
                    BlockPos targetPos = layerCenter.relative(right, dx).above(dy);
                    BlockState state = this.level().getBlockState(targetPos);
                    if (this.shouldMineBlock(targetPos, state, dx, dy)) {
                        return targetPos;
                    }
                }
            }
        }
        return null;
    }

    private int calculateMineTicks(BlockState state, BlockPos pos) {
        float destroySpeed = state.getDestroySpeed(this.level(), pos);
        if (destroySpeed <= 0.0F) {
            return 2;
        }
        // Standard Iron/Diamond tool mining time: ceil((hardness * 30) / 7.5)
        return Math.max(2, Mth.ceil((destroySpeed * 30.0F) / BORE_TOOL_SPEED));
    }

    @Override
    public void tick() {
        // Handle hurt wobble timer without calling AbstractMinecart's vanilla rail physics
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }
        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }
        this.checkBelowWorld();

        Direction facing = this.getFacing();
        float targetYRot = facing.toYRot();
        this.setYRot(targetYRot);
        this.yRotO = targetYRot;

        if (this.level().isClientSide()) {
            if (this.isDrilling()) {
                this.boreHeadRotation += 0.25F;
            }

            if (this.isActive()) {
                if (this.random.nextInt(3) == 0) {
                    Direction right = facing.getClockWise();
                    double stack1X = this.getX() + right.getStepX() * 0.9 - facing.getStepX() * 1.15;
                    double stack1Z = this.getZ() + right.getStepZ() * 0.9 - facing.getStepZ() * 1.15;
                    double stack2X = this.getX() - right.getStepX() * 0.9 - facing.getStepX() * 1.15;
                    double stack2Z = this.getZ() - right.getStepZ() * 0.9 - facing.getStepZ() * 1.15;
                    double stackY = this.getY() + 1.25;

                    this.level().addParticle(ParticleTypes.SMOKE, stack1X, stackY, stack1Z, 0.0, 0.03, 0.0);
                    this.level().addParticle(ParticleTypes.SMOKE, stack2X, stackY, stack2Z, 0.0, 0.03, 0.0);
                }

                if (this.isDrilling() && this.random.nextInt(2) == 0) {
                    double frontX = this.getX() + facing.getStepX() * 1.55;
                    double frontZ = this.getZ() + facing.getStepZ() * 1.55;
                    double frontY = this.getY() + 0.5 + this.random.nextDouble() * 1.2;
                    this.level().addParticle(ParticleTypes.CRIT, frontX, frontY, frontZ, 0.0, 0.1, 0.0);
                }
            }
            return;
        }

        // Server-side state machine
        this.setDeltaMovement(Vec3.ZERO);
        if (this.currentRailPos == null) {
            this.currentRailPos = this.blockPosition();
        }

        // Push overlapping entities outward so they don't clip inside
        List<Entity> pushableEntities = this.level().getEntities(
                this,
                this.getBoundingBox().inflate(0.05),
                EntitySelector.pushableBy(this)
        );
        for (Entity pushable : pushableEntities) {
            this.push(pushable);
        }

        int runMode = this.getRunMode();
        if (runMode == MODE_STOPPED) {
            this.clearMiningTarget();
            this.isMoving = false;
            this.moveTicks = 0;
            this.setDrilling(false);
            this.setActive(false);
            Vec3 restPos = Vec3.atBottomCenterOf(this.currentRailPos).add(0.0, 0.0625, 0.0);
            this.setPos(restPos.x, restPos.y, restPos.z);
            return;
        }

        // Handle REVERSE Mode
        if (runMode == MODE_REVERSE) {
            this.clearMiningTarget();
            this.setDrilling(false);

            if (!this.consumeFuelTick()) {
                this.setRunMode(MODE_STOPPED);
                return;
            }

            this.setActive(true);

            if (!this.isMoving) {
                Direction backwardDir = facing.getOpposite();
                BlockPos prevRailPos = this.currentRailPos.relative(backwardDir);
                BlockPos targetRail = null;

                if (this.level().getBlockState(prevRailPos).is(BlockTags.RAILS)) {
                    targetRail = prevRailPos;
                } else if (this.level().getBlockState(prevRailPos.below()).is(BlockTags.RAILS)) {
                    targetRail = prevRailPos.below();
                } else if (this.level().getBlockState(prevRailPos.above()).is(BlockTags.RAILS)) {
                    targetRail = prevRailPos.above();
                }

                if (targetRail == null) {
                    this.setRunMode(MODE_STOPPED);
                    for (Entity p : this.getPassengers()) {
                        if (p instanceof Player player) {
                            player.sendOverlayMessage(Component.literal("End of rails behind bore.").withStyle(ChatFormatting.YELLOW));
                        }
                    }
                    return;
                }

                this.isMoving = true;
                this.moveTicks = 0;
                this.moveTargetRailPos = targetRail;
            }

            this.moveTicks++;
            double frac = (double) this.moveTicks / (double) REVERSE_TICKS_PER_BLOCK;
            Vec3 start = Vec3.atBottomCenterOf(this.currentRailPos).add(0.0, 0.0625, 0.0);
            Vec3 end = Vec3.atBottomCenterOf(this.moveTargetRailPos).add(0.0, 0.0625, 0.0);
            Vec3 target = start.lerp(end, frac);
            this.setPos(target.x, target.y, target.z);

            this.pushTrailingCartsOnReverse(facing.getOpposite());

            if (this.moveTicks >= REVERSE_TICKS_PER_BLOCK) {
                this.currentRailPos = this.moveTargetRailPos;
                this.moveTargetRailPos = null;
                this.isMoving = false;
                this.moveTicks = 0;
                this.setPos(end.x, end.y, end.z);
            }
            return;
        }

        Direction right = facing.getClockWise();
        BlockPos blockToMine = this.findNextBlockToMine(this.currentRailPos, facing, right);

        // 1. If any block is in the 3x3 path ahead, HALT movement immediately and mine each block one by one
        if (blockToMine != null) {
            if (this.isMoving) {
                this.isMoving = false;
                this.moveTicks = 0;
                this.moveTargetRailPos = null;
                Vec3 restPos = Vec3.atBottomCenterOf(this.currentRailPos).add(0.0, 0.0625, 0.0);
                this.setPos(restPos.x, restPos.y, restPos.z);
            }

            BlockState targetState = this.level().getBlockState(blockToMine);
            float destroySpeed = targetState.getDestroySpeed(this.level(), blockToMine);
            if (destroySpeed < 0.0F) {
                // Unbreakable block (e.g. Bedrock) - halt completely
                this.clearMiningTarget();
                this.setDrilling(false);
                this.setActive(false);
                return;
            }

            if (!this.consumeFuelTick()) {
                this.clearMiningTarget();
                this.setDrilling(false);
                this.setActive(false);
                return;
            }

            this.setActive(true);
            this.setDrilling(true);

            if (!blockToMine.equals(this.miningTargetPos)) {
                this.clearMiningTarget();
                this.miningTargetPos = blockToMine;
                this.currentBlockMineProgress = 0;
                this.currentBlockTotalTicks = this.calculateMineTicks(targetState, blockToMine);
            }

            this.currentBlockMineProgress++;
            int crackStage = Mth.clamp(
                    (int) ((float) this.currentBlockMineProgress / (float) this.currentBlockTotalTicks * 10.0F),
                    0,
                    9
            );
            this.level().destroyBlockProgress(this.getId(), this.miningTargetPos, crackStage);

            if (this.currentBlockMineProgress % 4 == 1) {
                SoundType soundType = targetState.getSoundType(this.level(), this.miningTargetPos, this);
                this.level().playSound(
                        null,
                        this.miningTargetPos,
                        soundType.getHitSound(),
                        SoundSource.BLOCKS,
                        (soundType.getVolume() + 1.0F) / 4.0F,
                        soundType.getPitch() * 0.8F
                );
            }

            if (this.currentBlockMineProgress >= this.currentBlockTotalTicks) {
                this.level().destroyBlockProgress(this.getId(), this.miningTargetPos, -1);
                this.level().destroyBlock(this.miningTargetPos, true, this);
                this.miningTargetPos = null;
                this.currentBlockMineProgress = 0;
            }
            return;
        }

        // 2. All blocks in the 3x3 radius ahead are clear -> stop drilling and step 1 block forward
        this.clearMiningTarget();
        this.setDrilling(false);

        if (!this.isMoving) {
            BlockPos nextRailPos = this.currentRailPos.relative(facing);
            BlockPos floorPos = nextRailPos.below();

            // Safety check: ensure solid floor beneath the next rail
            if (!this.level().getBlockState(floorPos).isFaceSturdy(this.level(), floorPos, Direction.UP)) {
                this.setActive(false);
                return;
            }

            if (!this.hasOrCanRefuel()) {
                this.setActive(false);
                return;
            }

            // Lay track if not already present at nextRailPos
            BlockState nextState = this.level().getBlockState(nextRailPos);
            if (!nextState.is(BlockTags.RAILS)) {
                int railSlot = -1;
                for (int i = 1; i < INVENTORY_SIZE; i++) {
                    ItemStack s = this.getItem(i);
                    if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof BaseRailBlock) {
                        railSlot = i;
                        break;
                    }
                }

                if (railSlot == -1) {
                    // Out of track -> stop
                    this.setActive(false);
                    return;
                }

                ItemStack railStack = this.getItem(railSlot);
                BlockItem bi = (BlockItem) railStack.getItem();
                Block railBlock = bi.getBlock();
                railStack.shrink(1);
                this.level().setBlock(nextRailPos, railBlock.defaultBlockState(), 3);
                this.level().playSound(null, nextRailPos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            this.isMoving = true;
            this.moveTicks = 0;
            this.moveTargetRailPos = nextRailPos;
        }

        // Advance 1-block movement step at moderate speed (15 ticks = 0.75s per block)
        if (!this.consumeFuelTick()) {
            this.setActive(false);
            return;
        }

        this.setActive(true);
        this.moveTicks++;

        double frac = (double) this.moveTicks / (double) MOVE_TICKS_PER_BLOCK;
        Vec3 start = Vec3.atBottomCenterOf(this.currentRailPos).add(0.0, 0.0625, 0.0);
        Vec3 end = Vec3.atBottomCenterOf(this.moveTargetRailPos).add(0.0, 0.0625, 0.0);
        Vec3 target = start.lerp(end, frac);
        this.setPos(target.x, target.y, target.z);
        this.pullTrailingCartsOnMove();

        if (this.moveTicks >= MOVE_TICKS_PER_BLOCK) {
            this.currentRailPos = this.moveTargetRailPos;
            this.moveTargetRailPos = null;
            this.isMoving = false;
            this.moveTicks = 0;
            this.setPos(end.x, end.y, end.z);
            this.pullTrailingCartsOnMove();
        }
    }

    private void pullTrailingCartsOnMove() {
        UUID nextId = TrainChainingUtil.getCartChildUUID(this);
        Vec3 prevAnchor = this.getRearAnchorPos();
        Set<UUID> visited = new HashSet<>();
        visited.add(this.getUUID());

        while (nextId != null && visited.add(nextId)) {
            Entity ent = this.level().getEntity(nextId);
            if (!(ent instanceof AbstractMinecart cart) || cart.isRemoved()) {
                break;
            }
            Vec3 cartPos = cart.position();
            Vec3 diff = prevAnchor.subtract(cartPos);
            double dist = diff.horizontalDistance();
            if (dist > 1.42) {
                double pull = Math.min(dist - 1.38, 0.25);
                double nx = cart.getX() + (diff.x / dist) * pull;
                double nz = cart.getZ() + (diff.z / dist) * pull;
                cart.setPos(nx, cart.getY(), nz);
            }
            prevAnchor = cart.position();
            nextId = TrainChainingUtil.getCartChildUUID(cart);
        }
    }


    private void pushTrailingCartsOnReverse(Direction backwardDir) {
        UUID childId = TrainChainingUtil.getCartChildUUID(this);
        if (childId != null) {
            Entity ent = this.level().getEntity(childId);
            if (ent instanceof AbstractMinecart childCart && !childCart.isRemoved()) {
                Vec3 rearAnchor = this.getRearAnchorPos();
                double anchorDist = rearAnchor.distanceTo(childCart.position());
                if (anchorDist < 1.35) {
                    double pushDist = 0.05;
                    childCart.setPos(childCart.getX() + backwardDir.getStepX() * pushDist, childCart.getY(), childCart.getZ() + backwardDir.getStepZ() * pushDist);
                }
            }
        }
    }
}
