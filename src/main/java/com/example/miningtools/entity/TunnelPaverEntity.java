package com.example.miningtools.entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.menu.TunnelPaverMenu;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
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
 * Heavy Industrial Tunnel Paver (Wall & Ceiling Arch Liner).
 * - Built on the full-scale Tunnel Bore chassis (~2.8m wide, ~2.75m tall, ~6.15m long).
 * - Operates like the Under Track Paver along rails, but does NOT replace blocks beneath itself.
 * - Replaces the 9-block tunnel arch surrounding the 3x3 bore clearance as it advances:
 *   - 3 blocks on its left wall (bottom, middle, top at dx = -2, dy = 0, 1, 2)
 *   - 3 blocks in a row above it (ceiling left, center, right at dx = -1, 0, +1, dy = 3)
 *   - 3 blocks on its right wall (top, middle, bottom at dx = +2, dy = 2, 1, 0)
 * - Features 9 dedicated pattern filter slots (one for each arch block) + 1 fuel slot + 27 material slots.
 */
public class TunnelPaverEntity extends AbstractMinecartContainer {
    public static final int PATTERN_SLOTS_COUNT = 9;
    public static final int SLOT_PATTERN_LEFT_BOTTOM = 0;
    public static final int SLOT_PATTERN_LEFT_MID = 1;
    public static final int SLOT_PATTERN_LEFT_TOP = 2;
    public static final int SLOT_PATTERN_TOP_LEFT = 3;
    public static final int SLOT_PATTERN_TOP_CENTER = 4;
    public static final int SLOT_PATTERN_TOP_RIGHT = 5;
    public static final int SLOT_PATTERN_RIGHT_TOP = 6;
    public static final int SLOT_PATTERN_RIGHT_MID = 7;
    public static final int SLOT_PATTERN_RIGHT_BOTTOM = 8;

    public static final int SLOT_FUEL = 9;
    public static final int SLOT_MATERIAL_START = 10;
    public static final int SLOT_MATERIAL_COUNT = 27;
    public static final int INVENTORY_SIZE = 37;

    public static final int MODE_STOPPED = 0;
    public static final int MODE_FORWARD = 1;
    public static final int MODE_REVERSE = 2;

    // 20 ticks = 1.0 second per block step = 1.0 m/s travel speed in Work Mode
    public static final int MOVE_TICKS_PER_BLOCK = 20;
    // 30 ticks = 1.5 seconds per block step for safe reversing in Work Mode
    public static final int REVERSE_TICKS_PER_BLOCK = 30;
    // 10 ticks = 2.0 blocks per second = 2.0 m/s travel speed when Work Mode is OFF (Transport Mode)
    public static final int FAST_MOVE_TICKS_PER_BLOCK = 10;
    public static final int FAST_REVERSE_TICKS_PER_BLOCK = 10;

    private static final EntityDataAccessor<Direction> DATA_FACING =
            SynchedEntityData.defineId(TunnelPaverEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE =
            SynchedEntityData.defineId(TunnelPaverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PAVING =
            SynchedEntityData.defineId(TunnelPaverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_RUN_MODE =
            SynchedEntityData.defineId(TunnelPaverEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ON_RAILS =
            SynchedEntityData.defineId(TunnelPaverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_TRAIN_NAME =
            SynchedEntityData.defineId(TunnelPaverEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_WORK_LIGHT =
            SynchedEntityData.defineId(TunnelPaverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HOLOGRAM =
            SynchedEntityData.defineId(TunnelPaverEntity.class, EntityDataSerializers.BOOLEAN);

    private static final StackWalker STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

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

    // Client-side roller / erector animation
    public float rollerRotation = 0.0F;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> TunnelPaverEntity.this.fuel;
                case 1 -> TunnelPaverEntity.this.fuelTotal;
                case 2 -> TunnelPaverEntity.this.getRunMode();
                case 3 -> TunnelPaverEntity.this.isWorkMode() ? 1 : 0;
                case 4 -> TunnelPaverEntity.this.isWorkLightEnabled() ? 1 : 0;
                case 5 -> TunnelPaverEntity.this.isHologramEnabled() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> TunnelPaverEntity.this.fuel = value;
                case 1 -> TunnelPaverEntity.this.fuelTotal = value;
                case 2 -> TunnelPaverEntity.this.setRunMode(value);
                case 3 -> TunnelPaverEntity.this.setWorkMode(value != 0);
                case 4 -> TunnelPaverEntity.this.setWorkLightEnabled(value != 0);
                case 5 -> TunnelPaverEntity.this.setHologramEnabled(value != 0);
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public TunnelPaverEntity(EntityType<? extends TunnelPaverEntity> type, Level level) {
        super(type, level);
        this.clearItemStacks();
    }

    public TunnelPaverEntity(EntityType<? extends TunnelPaverEntity> type, Level level, double x, double y, double z) {
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
        entityData.define(DATA_ON_RAILS, false);
        entityData.define(DATA_TRAIN_NAME, "");
        entityData.define(DATA_WORK_LIGHT, false);
        entityData.define(DATA_HOLOGRAM, false);
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
        // Full Tunnel Bore-scale 3D model bounds: +1.55 blocks forward to -4.60 blocks backward,
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

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && this.getPassengers().isEmpty();
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (this.hasPassenger(passenger)) {
            Direction facing = this.getFacing();
            // Elevated driver observation cockpit in the rear gantry (matching Tunnel Bore)
            double offsetX = -facing.getStepX() * 0.8;
            double offsetZ = -facing.getStepZ() * 0.8;
            double offsetY = 2.05;
            moveFunction.accept(passenger, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ);
        }
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

    private static boolean isCalledByTrainChaining() {
        return STACK_WALKER.walk(frames ->
                frames.limit(6).anyMatch(f -> f.getClassName().startsWith("org.fuseleaf.minecarttrainsfork"))
        );
    }

    public boolean isBehindMachine(Vec3 pos) {
        Direction facing = this.getFacing();
        double dx = pos.x - super.position().x;
        double dz = pos.z - super.position().z;
        return (dx * facing.getStepX() + dz * facing.getStepZ()) < 0;
    }

    public Vec3 getRearAnchorPos() {
        Direction facing = this.getFacing();
        double offset = 4.16;
        return new Vec3(
                super.position().x - facing.getStepX() * offset,
                super.position().y,
                super.position().z - facing.getStepZ() * offset
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
        if (isCalledByTrainChaining() || (entity instanceof AbstractMinecart && this.isBehindMachine(entity.position()))) {
            return (float) this.getRearAnchorPos().distanceTo(entity.position());
        }
        return super.distanceTo(entity);
    }

    @Override
    public double distanceToSqr(Entity entity) {
        if (isCalledByTrainChaining() || (entity instanceof AbstractMinecart && this.isBehindMachine(entity.position()))) {
            return this.getRearAnchorPos().distanceToSqr(entity.position());
        }
        return super.distanceToSqr(entity);
    }

    @Override
    public double distanceToSqr(Vec3 vec) {
        if (isCalledByTrainChaining() || this.isBehindMachine(vec)) {
            return this.getRearAnchorPos().distanceToSqr(vec);
        }
        return super.distanceToSqr(vec);
    }

    @Override
    public double distanceToSqr(double x, double y, double z) {
        Vec3 target = new Vec3(x, y, z);
        if (isCalledByTrainChaining() || this.isBehindMachine(target)) {
            return this.getRearAnchorPos().distanceToSqr(target);
        }
        return super.distanceToSqr(x, y, z);
    }

    public static @Nullable TunnelPaverEntity getConnectedTunnelPaver(Entity entity) {
        if (entity instanceof TunnelPaverEntity paver && !paver.isRemoved()) {
            return paver;
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
                if (ent instanceof TunnelPaverEntity paver && !paver.isRemoved()) {
                    return paver;
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
            if (TrainChainingUtil.isChainedTo(this, entity)) {
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
        // Prevent external forces from displacing the tunnel paver off its track
    }

    @Override
    public float getPickRadius() {
        return 0.2F;
    }

    public boolean isWorkMode() {
        return this.entityData.get(DATA_PAVING);
    }

    public void setWorkMode(boolean workMode) {
        this.entityData.set(DATA_PAVING, workMode);
        if (workMode && this.getRunMode() != MODE_STOPPED && !this.hasPaverMaterial()) {
            this.setRunMode(MODE_STOPPED);
            this.notifyOutOfMaterial(null);
        }
    }

    public boolean isPaving() {
        return this.isWorkMode();
    }

    public void setPaving(boolean paving) {
        this.setWorkMode(paving);
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

    public int getRunMode() {
        return this.entityData.get(DATA_RUN_MODE);
    }

    public boolean isRunning() {
        return this.getRunMode() != MODE_STOPPED;
    }

    public void setRunning(boolean running) {
        this.setRunMode(running ? MODE_FORWARD : MODE_STOPPED);
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
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 0;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.TUNNEL_PAVER_ITEM.get());
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
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }
        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }
        this.checkBelowWorld();

        if (!this.isActive() || this.getRunMode() == MODE_STOPPED || !this.isMoving) {
            this.setDeltaMovement(Vec3.ZERO);
        }

        // Update client-side animations & exhaust smoke
        if (this.level().isClientSide()) {
            if (this.isActive()) {
                float speedMultiplier = this.isWorkMode() ? 1.0F : 2.5F;
                float dirSign = (this.getRunMode() == MODE_REVERSE) ? -1.0F : 1.0F;
                this.rollerRotation += 0.22F * speedMultiplier * dirSign;

                if (this.random.nextInt(3) == 0) {
                    Direction facing = this.getFacing();
                    Direction right = facing.getClockWise();
                    double stack1X = this.getX() + right.getStepX() * 0.9 - facing.getStepX() * 1.15;
                    double stack1Z = this.getZ() + right.getStepZ() * 0.9 - facing.getStepZ() * 1.15;
                    double stack2X = this.getX() - right.getStepX() * 0.9 - facing.getStepX() * 1.15;
                    double stack2Z = this.getZ() - right.getStepZ() * 0.9 - facing.getStepZ() * 1.15;
                    double stackY = this.getY() + 1.25;

                    this.level().addParticle(ParticleTypes.SMOKE, stack1X, stackY, stack1Z, 0.0, 0.03, 0.0);
                    this.level().addParticle(ParticleTypes.SMOKE, stack2X, stackY, stack2Z, 0.0, 0.03, 0.0);
                }
            }
            return;
        }

        // Server-side work light
        if (this.isWorkLightEnabled() && !this.isRemoved()) {
            this.updateWorkLight();
        } else {
            this.clearWorkLight();
        }

        if (this.outOfMaterialNotifyCooldown > 0) {
            this.outOfMaterialNotifyCooldown--;
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

        // Verify rail beneath machine
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
                this.notifyDriver(Component.literal("Tunnel Paver: End of track!").withStyle(ChatFormatting.YELLOW));
                return;
            }

            // Impassable obstruction check along center rail clearance
            if (!isPassable(nextRail.above()) || !isPassable(nextRail.above(2))) {
                this.setRunMode(MODE_STOPPED);
                this.notifyDriver(Component.literal("Tunnel Paver: Track path obstructed!").withStyle(ChatFormatting.RED));
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

        if (this.moveTicks == 1 && this.isWorkMode()) {
            Direction paveDir = isReversing ? this.getFacing() : travelDir;
            this.executeLiningArch(this.currentStepPos, paveDir);
            if (this.getRunMode() == MODE_STOPPED) {
                return;
            }
        }

        if (this.moveTicks == ticksPerBlock / 2 && this.isWorkMode()) {
            Direction paveDir = isReversing ? this.getFacing() : travelDir;
            this.executeLiningArch(this.targetStepPos, paveDir);
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
                this.executeLiningArch(this.currentStepPos, this.getFacing());
                if (this.getRunMode() == MODE_STOPPED) {
                    return;
                }
            }
        }
    }

    /**
     * Returns the 9 world BlockPos locations for the tunnel arch around a given rail center position:
     * - 0: Left Wall Bottom (left 2, dy 0)
     * - 1: Left Wall Middle (left 2, dy 1)
     * - 2: Left Wall Top    (left 2, dy 2)
     * - 3: Ceiling Left     (left 1, dy 3)
     * - 4: Ceiling Center   (center, dy 3)
     * - 5: Ceiling Right    (right 1, dy 3)
     * - 6: Right Wall Top   (right 2, dy 2)
     * - 7: Right Wall Mid   (right 2, dy 1)
     * - 8: Right Wall Bot   (right 2, dy 0)
     */
    public static BlockPos[] getArchPositions(BlockPos centerPos, Direction forwardDir) {
        Direction leftDir = forwardDir.getCounterClockWise();
        Direction rightDir = forwardDir.getClockWise();
        BlockPos leftWallBase = centerPos.relative(leftDir, 2);
        BlockPos rightWallBase = centerPos.relative(rightDir, 2);
        BlockPos ceilingCenter = centerPos.above(3);

        return new BlockPos[] {
                leftWallBase,                           // 0: Left Wall Bottom
                leftWallBase.above(1),                  // 1: Left Wall Middle
                leftWallBase.above(2),                  // 2: Left Wall Top
                ceilingCenter.relative(leftDir, 1),     // 3: Top Left
                ceilingCenter,                          // 4: Top Center
                ceilingCenter.relative(rightDir, 1),    // 5: Top Right
                rightWallBase.above(2),                 // 6: Right Wall Top
                rightWallBase.above(1),                 // 7: Right Wall Middle
                rightWallBase                           // 8: Right Wall Bottom
        };
    }

    /**
     * Lines/replaces the 9-block tunnel arch (3 left wall, 3 ceiling above, 3 right wall)
     * without touching the floor blocks beneath the machine.
     */
    public void executeLiningArch(BlockPos centerPos, Direction forwardDir) {
        if (!this.hasPaverMaterial()) {
            this.setRunMode(MODE_STOPPED);
            this.notifyOutOfMaterial(null);
            return;
        }

        BlockPos[] arch = getArchPositions(centerPos, forwardDir);
        boolean placedAny = false;
        SoundType soundType = null;

        for (int i = 0; i < PATTERN_SLOTS_COUNT; i++) {
            BlockState targetState = this.getTargetArchBlockState(i);
            if (targetState == null || targetState.isAir()) {
                continue; // Skip this tile if pattern is active and this specific filter slot is empty
            }

            if (this.lineSingleBlock(arch[i], targetState)) {
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
     * Replaces a single wall or ceiling block with the configured target block state,
     * harvesting the replaced block's drops into the machine's material inventory.
     */
    private boolean lineSingleBlock(BlockPos pos, BlockState targetState) {
        BlockState currentState = this.level().getBlockState(pos);
        if (currentState.equals(targetState)) {
            return false;
        }

        // Never replace indestructible blocks or existing railway tracks
        if (currentState.getDestroySpeed(this.level(), pos) < 0.0F) {
            return false;
        }
        if (BaseRailBlock.isRail(currentState) || currentState.is(BlockTags.RAILS)) {
            return false;
        }

        if (!this.consumePaverMaterial(targetState.getBlock())) {
            this.setRunMode(MODE_STOPPED);
            this.notifyOutOfMaterial(null);
            return false;
        }

        // Harvest drops of replaced block if not air or liquid
        if (!currentState.isAir() && !currentState.liquid()) {
            this.harvestReplacedBlock(pos, currentState);
        }

        this.level().setBlock(pos, targetState, Block.UPDATE_ALL);
        return true;
    }

    private void harvestReplacedBlock(BlockPos pos, BlockState state) {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, null);
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
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

    /**
     * Gets the target blockstate for a specific arch tile index (0..8).
     */
    public @Nullable BlockState getTargetArchBlockState(int tileIndex) {
        if (tileIndex < 0 || tileIndex >= PATTERN_SLOTS_COUNT) {
            return null;
        }

        ItemStack filterStack = this.getItem(tileIndex);
        if (!filterStack.isEmpty() && filterStack.getItem() instanceof BlockItem bi) {
            Block b = bi.getBlock();
            if (!(b instanceof BaseRailBlock)) {
                return b.defaultBlockState();
            }
        }

        // If all 9 pattern slots are empty, fall back to first valid full block in bulk inventory
        if (this.areAllPatternSlotsEmpty()) {
            return this.getFirstValidBulkMaterialState();
        }

        // Specific pattern is active, but this specific tile slot is empty -> skip lining this tile
        return null;
    }

    public boolean areAllPatternSlotsEmpty() {
        for (int s = 0; s < PATTERN_SLOTS_COUNT; s++) {
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
            for (int i = SLOT_MATERIAL_START; i < SLOT_MATERIAL_START + SLOT_MATERIAL_COUNT; i++) {
                ItemStack stack = this.getItem(i);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof BaseRailBlock)) {
                    return true;
                }
            }
            int pulled = TrainChainingUtil.pullItemsFromConsist(this, s -> s.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof BaseRailBlock), 16);
            return pulled > 0;
        }

        for (int s = 0; s < PATTERN_SLOTS_COUNT; s++) {
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
            targetPlayer.sendOverlayMessage(Component.literal("Tunnel Paver: Out of paving material!").withStyle(ChatFormatting.RED));
            return;
        }
        if (this.outOfMaterialNotifyCooldown > 0) {
            return;
        }
        this.outOfMaterialNotifyCooldown = 40;

        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof Player rider) {
            rider.sendOverlayMessage(Component.literal("Tunnel Paver: Out of paving material!").withStyle(ChatFormatting.RED));
        } else {
            for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(8.0D))) {
                player.sendOverlayMessage(Component.literal("Tunnel Paver: Out of paving material!").withStyle(ChatFormatting.RED));
            }
        }
    }

    private boolean consumePaverMaterial(Block targetBlock) {
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

        // Try pulling matching block from linked consist carts
        int pulled = TrainChainingUtil.pullItemsFromConsist(this, s -> s.getItem() instanceof BlockItem bi && bi.getBlock() == targetBlock, 16);
        if (pulled > 0) {
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

    public void handleHotkeyAction(Player player, int action) {
        switch (action) {
            case LocomotiveControlPayload.ACTION_START -> {
                if (this.isPaving() && !this.hasPaverMaterial()) {
                    this.setRunMode(MODE_STOPPED);
                    player.sendOverlayMessage(Component.literal("Tunnel Paver: Out of paving material!").withStyle(ChatFormatting.RED));
                    return;
                }
                this.setRunMode(MODE_FORWARD);
                String modeDesc = this.isWorkMode() ? "Work Mode (1.0 m/s)" : "Transport Mode (2.0 m/s)";
                player.sendOverlayMessage(Component.literal("Tunnel Paver: Forward - " + modeDesc).withStyle(ChatFormatting.GREEN));
            }
            case LocomotiveControlPayload.ACTION_STOP -> {
                this.setRunMode(MODE_STOPPED);
                player.sendOverlayMessage(Component.literal("Tunnel Paver: Stopped").withStyle(ChatFormatting.RED));
            }
            case LocomotiveControlPayload.ACTION_SPEED_DOWN, LocomotiveControlPayload.ACTION_SPEED_UP -> {
                if (this.getRunMode() == MODE_STOPPED) {
                    Direction newFacing = this.getFacing().getOpposite();
                    this.setFacing(newFacing);
                    player.sendOverlayMessage(Component.literal("Tunnel Paver: Flipped orientation to " + newFacing.getName().toUpperCase()).withStyle(ChatFormatting.YELLOW));
                }
            }
            case LocomotiveControlPayload.ACTION_REVERSE -> {
                int next = (this.getRunMode() == MODE_REVERSE) ? MODE_FORWARD : MODE_REVERSE;
                if (next != MODE_STOPPED && this.isPaving() && !this.hasPaverMaterial()) {
                    this.setRunMode(MODE_STOPPED);
                    player.sendOverlayMessage(Component.literal("Tunnel Paver: Out of paving material!").withStyle(ChatFormatting.RED));
                    return;
                }
                this.setRunMode(next);
                if (next == MODE_REVERSE) {
                    String modeDesc = this.isWorkMode() ? "Work Reverse (0.67 m/s)" : "Transport Reverse (2.0 m/s)";
                    player.sendOverlayMessage(Component.literal("Tunnel Paver: " + modeDesc).withStyle(ChatFormatting.GOLD));
                } else {
                    String modeDesc = this.isWorkMode() ? "Work Mode (1.0 m/s)" : "Transport Mode (2.0 m/s)";
                    player.sendOverlayMessage(Component.literal("Tunnel Paver: Forward - " + modeDesc).withStyle(ChatFormatting.GREEN));
                }
            }
            case LocomotiveControlPayload.ACTION_TOGGLE_LIGHT -> {
                boolean nextLight = !this.isWorkLightEnabled();
                this.setWorkLightEnabled(nextLight);
                player.sendOverlayMessage(Component.literal("Tunnel Paver: Work Light " + (nextLight ? "ON" : "OFF")).withStyle(nextLight ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            }
            case LocomotiveControlPayload.ACTION_HORN -> {
                if (player.isShiftKeyDown()) {
                    boolean nextHolo = !this.isHologramEnabled();
                    this.setHologramEnabled(nextHolo);
                    player.sendOverlayMessage(Component.literal("Tunnel Paver: Alignment Hologram " + (nextHolo ? "ON" : "OFF")).withStyle(nextHolo ? ChatFormatting.AQUA : ChatFormatting.GRAY));
                } else {
                    boolean nextWork = !this.isWorkMode();
                    this.setWorkMode(nextWork);
                    player.sendOverlayMessage(Component.literal("Tunnel Paver: " + (nextWork ? "Work Mode (Lining 1.0 m/s)" : "Transport Mode (No Line 2.0 m/s)")).withStyle(nextWork ? ChatFormatting.GREEN : ChatFormatting.GOLD));
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
                    ? (TrainChainingUtil.getCartCouplerRadius(cart) + TrainChainingUtil.COUPLER_GAP)
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
                    ? (TrainChainingUtil.getCartCouplerRadius(cart) + TrainChainingUtil.COUPLER_GAP)
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
        return new TunnelPaverMenu(id, playerInventory, this, this.dataAccess);
    }

    @Override
    protected Item getDropItem() {
        return MiningTools.TUNNEL_PAVER_ITEM.get();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Facing", this.getFacing().get3DDataValue());
        output.putInt("Fuel", this.fuel);
        output.putInt("FuelTotal", this.fuelTotal);
        output.putInt("RunMode", this.getRunMode());
        output.putBoolean("Paving", this.isPaving());
        output.putBoolean("OnRails", this.isOnRails());
        output.putString("TrainName", this.getTrainName());
        output.putBoolean("WorkLight", this.isWorkLightEnabled());
        output.putBoolean("Hologram", this.isHologramEnabled());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int facingVal = input.getIntOr("Facing", Direction.NORTH.get3DDataValue());
        this.setFacing(Direction.from3DDataValue(facingVal));
        this.fuel = input.getIntOr("Fuel", 0);
        this.fuelTotal = input.getIntOr("FuelTotal", 1600);
        this.setRunMode(input.getIntOr("RunMode", MODE_STOPPED));
        this.setPaving(input.getBooleanOr("Paving", true));
        this.setOnRails(input.getBooleanOr("OnRails", false));
        this.setTrainName(input.getStringOr("TrainName", ""));
        this.setWorkLightEnabled(input.getBooleanOr("WorkLight", false));
        this.setHologramEnabled(input.getBooleanOr("Hologram", false));
    }
}
