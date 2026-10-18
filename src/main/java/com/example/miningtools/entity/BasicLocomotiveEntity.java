package com.example.miningtools.entity;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.Config;
import com.example.miningtools.MiningTools;
import com.example.miningtools.menu.BasicLocomotiveMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BasicLocomotiveEntity extends AbstractMinecartContainer {
    public static final int INVENTORY_SIZE = 3;

    private static final EntityDataAccessor<Direction> DATA_FACING =
            SynchedEntityData.defineId(BasicLocomotiveEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE =
            SynchedEntityData.defineId(BasicLocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LIT =
            SynchedEntityData.defineId(BasicLocomotiveEntity.class, EntityDataSerializers.BOOLEAN);

    private static final StackWalker STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static @Nullable Method GET_PARENT_UUID_METHOD = null;
    private static boolean CHECKED_PARENT_UUID_METHOD = false;

    public static final int MIN_SPEED_LEVEL = 1;
    public static final int MAX_SPEED_LEVEL = 5;
    public static final int DEFAULT_SPEED_LEVEL = 3;

    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    private boolean running = false;
    private int fuel = 0;
    private int fuelTotal = 1600;
    private int speedLevel = DEFAULT_SPEED_LEVEL;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> BasicLocomotiveEntity.this.fuel;
                case 1 -> BasicLocomotiveEntity.this.fuelTotal;
                case 2 -> BasicLocomotiveEntity.this.running ? 1 : 0;
                case 3 -> BasicLocomotiveEntity.this.getFacing().get2DDataValue();
                case 4 -> BasicLocomotiveEntity.this.speedLevel;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> BasicLocomotiveEntity.this.fuel = value;
                case 1 -> BasicLocomotiveEntity.this.fuelTotal = value;
                case 2 -> BasicLocomotiveEntity.this.setRunning(value != 0);
                case 3 -> BasicLocomotiveEntity.this.setFacing(Direction.from2DDataValue(value));
                case 4 -> BasicLocomotiveEntity.this.setSpeedLevel(value);
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public BasicLocomotiveEntity(EntityType<? extends BasicLocomotiveEntity> type, Level level) {
        super(type, level);
    }

    public BasicLocomotiveEntity(Level level, double x, double y, double z) {
        super(MiningTools.BASIC_LOCOMOTIVE_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FACING, Direction.NORTH);
        builder.define(DATA_ACTIVE, false);
        builder.define(DATA_LIT, false);
    }

    public Direction getFacing() {
        return this.entityData.get(DATA_FACING);
    }

    public void setFacing(Direction facing) {
        if (facing.getAxis().isHorizontal()) {
            this.entityData.set(DATA_FACING, facing);
        }
    }

    public boolean isRunning() {
        return this.running;
    }

    public void setRunning(boolean running) {
        this.running = running;
        if (!running) {
            this.setActive(false);
        }
    }

    public boolean isActive() {
        return this.entityData.get(DATA_ACTIVE);
    }

    public void setActive(boolean active) {
        this.entityData.set(DATA_ACTIVE, active);
    }

    public boolean isLit() {
        return this.entityData.get(DATA_LIT);
    }

    public void setLit(boolean lit) {
        this.entityData.set(DATA_LIT, lit);
    }

    public int getSpeedLevel() {
        return this.speedLevel;
    }

    public void setSpeedLevel(int level) {
        this.speedLevel = Mth.clamp(level, MIN_SPEED_LEVEL, MAX_SPEED_LEVEL);
    }

    public double getLocomotiveSpeed() {
        double maxSpeed = Config.LOCOMOTIVE_MAX_SPEED.get();
        return switch (this.speedLevel) {
            case 1 -> Math.max(0.04D, maxSpeed * 0.15D); // Crawl (~0.05 b/t)
            case 2 -> Math.max(0.06D, maxSpeed * 0.30D); // Slow (~0.10 b/t)
            case 3 -> Math.max(0.08D, maxSpeed * 0.50D); // Normal / Half-Speed (~0.175 b/t)
            case 4 -> maxSpeed * 0.75D;                  // Fast (~0.26 b/t)
            case 5 -> maxSpeed;                          // Max (~0.35 b/t)
            default -> Math.max(0.08D, maxSpeed * 0.50D);
        };
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
        return MiningTools.BASIC_LOCOMOTIVE_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.BASIC_LOCOMOTIVE_ITEM.get());
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        if (this.isChainedTo(entity)) {
            return false;
        }
        return !entity.isPassengerOfSameVehicle(this);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        if (other != null && this.isChainedTo(other)) {
            return false;
        }
        return !this.isRemoved();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new BasicLocomotiveMenu(containerId, inventory, this, this.dataAccess);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            // Sneak + empty hand right click: reverse locomotive direction
            if (!this.level().isClientSide()) {
                this.setFacing(this.getFacing().getOpposite());
                this.level().playSound(null, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.8F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand, location);
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

        Direction facing = this.getFacing();
        float targetYRot = facing.toYRot();
        this.setYRot(targetYRot);
        this.yRotO = targetYRot;

        if (this.level().isClientSide()) {
            // Client side particle effects
            if (this.isActive()) {
                // Chimney / smokestack position
                double chimneyForward = 0.55;
                double chimneyX = this.getX() + facing.getStepX() * chimneyForward;
                double chimneyZ = this.getZ() + facing.getStepZ() * chimneyForward;
                double chimneyY = this.getY() + 1.25;

                // Black coal smoke
                if (this.random.nextInt(2) == 0) {
                    this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, chimneyX, chimneyY, chimneyZ,
                            facing.getStepX() * 0.02, 0.08, facing.getStepZ() * 0.02);
                }
                // White steam puffs
                if (this.random.nextInt(3) == 0) {
                    this.level().addParticle(ParticleTypes.POOF, chimneyX, chimneyY, chimneyZ,
                            0.0, 0.04, 0.0);
                }
            }
            return;
        }

        // Server side fuel and rail kinematics
        this.tickFuelAndMovement();

        // Push entities in path
        List<Entity> pushableEntities = this.level().getEntities(
                this,
                this.getBoundingBox().inflate(0.1),
                EntitySelector.pushableBy(this)
        );
        for (Entity pushable : pushableEntities) {
            this.push(pushable);
        }
    }

    private void tickFuelAndMovement() {
        if (this.running) {
            // Consume fuel
            if (this.fuel > 0) {
                this.fuel--;
            } else {
                // Try consuming from the 3 fuel slots
                boolean refueled = false;
                for (int i = 0; i < INVENTORY_SIZE; i++) {
                    ItemStack stack = this.getItem(i);
                    if (!stack.isEmpty() && stack.is(ItemTags.COALS)) {
                        stack.shrink(1);
                        int burnTicks = Config.LOCOMOTIVE_COAL_BURN_TICKS.get();
                        this.fuelTotal = burnTicks;
                        this.fuel = burnTicks;
                        refueled = true;
                        this.level().playSound(null, this.blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.0F);
                        break;
                    }
                }
                if (!refueled) {
                    // Out of fuel -> stop engine
                    this.setRunning(false);
                }
            }
        }

        boolean hasFuel = this.fuel > 0;
        this.setLit(hasFuel);

        BlockPos railPos = this.blockPosition();
        BlockState railState = this.level().getBlockState(railPos);
        boolean onRails = railState.is(BlockTags.RAILS);

        if (this.running && hasFuel && onRails) {
            this.setActive(true);
            Direction facing = this.getFacing();
            double maxSpeed = this.getLocomotiveSpeed();
            double accel = Config.LOCOMOTIVE_ACCELERATION.get();

            Vec3 currentDelta = this.getDeltaMovement();
            double targetVx = facing.getStepX() * maxSpeed;
            double targetVz = facing.getStepZ() * maxSpeed;

            // Accelerate towards target velocity
            double newVx = currentDelta.x + (targetVx - currentDelta.x) * accel;
            double newVz = currentDelta.z + (targetVz - currentDelta.z) * accel;
            Vec3 newMovement = new Vec3(newVx, currentDelta.y, newVz);

            this.setDeltaMovement(newMovement);
            this.move(MoverType.SELF, newMovement);
            this.handleChainedCartsMovement(newMovement, facing);
        } else {
            this.setActive(false);
            // Apply braking
            Vec3 currentDelta = this.getDeltaMovement();
            if (currentDelta.horizontalDistanceSqr() > 1.0E-4D) {
                Vec3 braked = currentDelta.multiply(0.65D, 1.0D, 0.65D);
                this.setDeltaMovement(braked);
                this.move(MoverType.SELF, braked);
                this.brakePushedCarts(braked, this.getFacing());
            } else {
                this.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Minecart Trains Fork / Train Chaining Compatibility
    // -------------------------------------------------------------------------

    private static boolean isCalledByTrainChaining() {
        return STACK_WALKER.walk(frames ->
                frames.limit(6).anyMatch(f -> f.getClassName().startsWith("org.fuseleaf.minecarttrainsfork"))
        );
    }

    private static @Nullable Method GET_CHILD_UUID_METHOD = null;
    private static boolean CHECKED_CHILD_UUID_METHOD = false;

    public static @Nullable UUID getCartParentUUID(Entity entity) {
        if (!CHECKED_PARENT_UUID_METHOD) {
            CHECKED_PARENT_UUID_METHOD = true;
            try {
                GET_PARENT_UUID_METHOD = entity.getClass().getMethod("getParentUUID");
            } catch (Throwable ignored) {
            }
        }
        if (GET_PARENT_UUID_METHOD != null) {
            try {
                return (UUID) GET_PARENT_UUID_METHOD.invoke(entity);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static @Nullable UUID getCartChildUUID(Entity entity) {
        if (!CHECKED_CHILD_UUID_METHOD) {
            CHECKED_CHILD_UUID_METHOD = true;
            try {
                GET_CHILD_UUID_METHOD = entity.getClass().getMethod("getChildUUID");
            } catch (Throwable ignored) {
            }
        }
        if (GET_CHILD_UUID_METHOD != null) {
            try {
                return (UUID) GET_CHILD_UUID_METHOD.invoke(entity);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public List<AbstractMinecart> getConnectedTrainCarts() {
        List<AbstractMinecart> result = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        visited.add(this.getUUID());

        Deque<UUID> toCheck = new ArrayDeque<>();
        UUID p = getCartParentUUID(this);
        if (p != null) toCheck.add(p);
        UUID c = getCartChildUUID(this);
        if (c != null) toCheck.add(c);

        while (!toCheck.isEmpty()) {
            UUID next = toCheck.poll();
            if (next != null && visited.add(next)) {
                Entity ent = this.level().getEntity(next);
                if (ent instanceof AbstractMinecart cart && !cart.isRemoved()) {
                    result.add(cart);
                    UUID cp = getCartParentUUID(cart);
                    if (cp != null && !visited.contains(cp)) toCheck.add(cp);
                    UUID cc = getCartChildUUID(cart);
                    if (cc != null && !visited.contains(cc)) toCheck.add(cc);
                }
            }
        }
        return result;
    }

    public boolean isChainedTo(Entity entity) {
        if (entity instanceof AbstractMinecart cart) {
            UUID id = cart.getUUID();
            UUID p = getCartParentUUID(this);
            UUID c = getCartChildUUID(this);
            if (id.equals(p) || id.equals(c)) return true;
            UUID ep = getCartParentUUID(cart);
            UUID ec = getCartChildUUID(cart);
            if (this.getUUID().equals(ep) || this.getUUID().equals(ec)) return true;
            for (AbstractMinecart tc : this.getConnectedTrainCarts()) {
                if (tc.getUUID().equals(id)) return true;
            }
        }
        return false;
    }

    public Vec3 getAnchorPosFor(Vec3 otherPos) {
        Direction facing = this.getFacing();
        double dx = otherPos.x - super.position().x;
        double dz = otherPos.z - super.position().z;
        double dot = dx * facing.getStepX() + dz * facing.getStepZ();
        // If otherPos is behind locomotive (dot < 0), use rear bumper (-facing * 0.36)
        // If otherPos is in front of locomotive (dot >= 0), use front bumper (+facing * 0.36)
        double sign = dot < 0 ? -1.0 : 1.0;
        return new Vec3(
                super.position().x + facing.getStepX() * (0.36 * sign),
                super.position().y,
                super.position().z + facing.getStepZ() * (0.36 * sign)
        );
    }

    public @Nullable AbstractMinecart getNearestConnectedCart() {
        List<AbstractMinecart> carts = this.getConnectedTrainCarts();
        if (carts.isEmpty()) return null;
        AbstractMinecart nearest = carts.get(0);
        double nearestDistSqr = nearest.distanceToSqr(this);
        for (int i = 1; i < carts.size(); i++) {
            AbstractMinecart cart = carts.get(i);
            double d = cart.distanceToSqr(this);
            if (d < nearestDistSqr) {
                nearest = cart;
                nearestDistSqr = d;
            }
        }
        return nearest;
    }

    @Override
    public Vec3 position() {
        if (isCalledByTrainChaining()) {
            AbstractMinecart nearest = this.getNearestConnectedCart();
            if (nearest != null) {
                return this.getAnchorPosFor(nearest.position());
            }
            Direction facing = this.getFacing();
            return new Vec3(
                    super.position().x - facing.getStepX() * 0.36,
                    super.position().y,
                    super.position().z - facing.getStepZ() * 0.36
            );
        }
        return super.position();
    }

    @Override
    public float distanceTo(Entity entity) {
        if (entity instanceof AbstractMinecart) {
            return (float) this.getAnchorPosFor(entity.position()).distanceTo(entity.position());
        }
        return super.distanceTo(entity);
    }

    @Override
    public double distanceToSqr(Entity entity) {
        if (entity instanceof AbstractMinecart) {
            return this.getAnchorPosFor(entity.position()).distanceToSqr(entity.position());
        }
        return super.distanceToSqr(entity);
    }

    @Override
    public double distanceToSqr(Vec3 vec) {
        if (isCalledByTrainChaining()) {
            return this.getAnchorPosFor(vec).distanceToSqr(vec);
        }
        return super.distanceToSqr(vec);
    }

    @Override
    public double distanceToSqr(double x, double y, double z) {
        Vec3 target = new Vec3(x, y, z);
        if (isCalledByTrainChaining()) {
            return this.getAnchorPosFor(target).distanceToSqr(target);
        }
        return super.distanceToSqr(x, y, z);
    }

    @Override
    public Vec3 getDeltaMovement() {
        if (this.isActive()) {
            Direction facing = this.getFacing();
            double speed = this.getLocomotiveSpeed();
            return new Vec3(facing.getStepX() * speed, 0.0, facing.getStepZ() * speed);
        }
        return super.getDeltaMovement();
    }

    private void handleChainedCartsMovement(Vec3 movement, Direction facing) {
        List<AbstractMinecart> carts = this.getConnectedTrainCarts();
        if (carts.isEmpty()) {
            return;
        }

        // Collect carts ahead of locomotive movement direction (being pushed)
        List<AbstractMinecart> pushedCarts = new ArrayList<>();
        for (AbstractMinecart cart : carts) {
            double dx = cart.getX() - this.getX();
            double dz = cart.getZ() - this.getZ();
            double forwardDot = dx * facing.getStepX() + dz * facing.getStepZ();
            if (forwardDot > 0.05D) {
                pushedCarts.add(cart);
            }
        }

        if (pushedCarts.isEmpty()) {
            return;
        }

        // Sort closest to farthest along the direction of travel
        pushedCarts.sort(Comparator.comparingDouble(c -> {
            double dx = c.getX() - this.getX();
            double dz = c.getZ() - this.getZ();
            return dx * facing.getStepX() + dz * facing.getStepZ();
        }));

        Entity prev = this;
        for (AbstractMinecart cart : pushedCarts) {
            // Propel the pushed cart forward at locomotive travel speed
            cart.setDeltaMovement(new Vec3(movement.x, cart.getDeltaMovement().y, movement.z));

            // Enforce minimum buffer separation so carts don't clip into each other
            double dx = cart.getX() - prev.getX();
            double dz = cart.getZ() - prev.getZ();
            double distAlongFacing = dx * facing.getStepX() + dz * facing.getStepZ();
            double minBuffer = 1.15D;
            if (distAlongFacing < minBuffer) {
                double pushNeeded = minBuffer - distAlongFacing;
                cart.move(MoverType.SELF, new Vec3(facing.getStepX() * pushNeeded, 0.0, facing.getStepZ() * pushNeeded));
            }

            prev = cart;
        }
    }

    private void brakePushedCarts(Vec3 brakedMovement, Direction facing) {
        List<AbstractMinecart> carts = this.getConnectedTrainCarts();
        for (AbstractMinecart cart : carts) {
            double dx = cart.getX() - this.getX();
            double dz = cart.getZ() - this.getZ();
            double forwardDot = dx * facing.getStepX() + dz * facing.getStepZ();
            if (forwardDot > 0.05D) {
                cart.setDeltaMovement(new Vec3(brakedMovement.x, cart.getDeltaMovement().y, brakedMovement.z));
            }
        }
    }

    @Override
    public void push(Entity entity) {
        if (!this.level().isClientSide() && !entity.noPhysics && !this.hasPassenger(entity)) {
            if (this.isChainedTo(entity)) {
                return;
            }
            super.push(entity);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Running", this.running);
        output.putInt("Fuel", this.fuel);
        output.putInt("FuelTotal", this.fuelTotal);
        output.putString("Facing", this.getFacing().getName());
        output.putInt("SpeedLevel", this.speedLevel);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.running = input.getBooleanOr("Running", false);
        this.fuel = input.getIntOr("Fuel", 0);
        this.fuelTotal = input.getIntOr("FuelTotal", Config.LOCOMOTIVE_COAL_BURN_TICKS.get());
        String facingName = input.getStringOr("Facing", Direction.NORTH.getName());
        this.setFacing(Direction.byName(facingName) != null ? Direction.byName(facingName) : Direction.NORTH);
        this.speedLevel = input.getIntOr("SpeedLevel", DEFAULT_SPEED_LEVEL);
    }
}
