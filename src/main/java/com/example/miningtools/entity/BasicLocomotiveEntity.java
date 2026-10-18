package com.example.miningtools.entity;

import java.lang.reflect.Method;
import java.util.List;

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

    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    private boolean running = false;
    private int fuel = 0;
    private int fuelTotal = 1600;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> BasicLocomotiveEntity.this.fuel;
                case 1 -> BasicLocomotiveEntity.this.fuelTotal;
                case 2 -> BasicLocomotiveEntity.this.running ? 1 : 0;
                case 3 -> BasicLocomotiveEntity.this.getFacing().get2DDataValue();
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
            }
        }

        @Override
        public int getCount() {
            return 4;
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
        return !entity.isPassengerOfSameVehicle(this);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
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
            double maxSpeed = Config.LOCOMOTIVE_MAX_SPEED.get();
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
        } else {
            this.setActive(false);
            // Apply braking
            Vec3 currentDelta = this.getDeltaMovement();
            if (currentDelta.horizontalDistanceSqr() > 1.0E-4D) {
                Vec3 braked = currentDelta.multiply(0.65D, 1.0D, 0.65D);
                this.setDeltaMovement(braked);
                this.move(MoverType.SELF, braked);
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

    public boolean isBehindLocomotive(Vec3 pos) {
        Direction facing = this.getFacing();
        double dx = pos.x - super.position().x;
        double dz = pos.z - super.position().z;
        return (dx * facing.getStepX() + dz * facing.getStepZ()) < 0;
    }

    /**
     * Rear hitch / coupler position for trailing carts.
     * Locomotive cab ends at ~0.85 blocks behind center.
     * With 0.49 minecart half-width subtracted by ChainingRender.linkLine,
     * offset of 0.36 places the chain hook exactly on the rear bumper.
     */
    public Vec3 getRearAnchorPos() {
        Direction facing = this.getFacing();
        double offset = 0.36;
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
        if (isCalledByTrainChaining() || (entity instanceof AbstractMinecart && this.isBehindLocomotive(entity.position()))) {
            return (float) this.getRearAnchorPos().distanceTo(entity.position());
        }
        return super.distanceTo(entity);
    }

    @Override
    public double distanceToSqr(Entity entity) {
        if (isCalledByTrainChaining() || (entity instanceof AbstractMinecart && this.isBehindLocomotive(entity.position()))) {
            return this.getRearAnchorPos().distanceToSqr(entity.position());
        }
        return super.distanceToSqr(entity);
    }

    @Override
    public double distanceToSqr(Vec3 vec) {
        if (isCalledByTrainChaining() || this.isBehindLocomotive(vec)) {
            return this.getRearAnchorPos().distanceToSqr(vec);
        }
        return super.distanceToSqr(vec);
    }

    @Override
    public double distanceToSqr(double x, double y, double z) {
        Vec3 target = new Vec3(x, y, z);
        if (isCalledByTrainChaining() || this.isBehindLocomotive(target)) {
            return this.getRearAnchorPos().distanceToSqr(target);
        }
        return super.distanceToSqr(x, y, z);
    }

    @Override
    public Vec3 getDeltaMovement() {
        if (this.isActive()) {
            Direction facing = this.getFacing();
            double speed = Config.LOCOMOTIVE_MAX_SPEED.get();
            return new Vec3(facing.getStepX() * speed, 0.0, facing.getStepZ() * speed);
        }
        return super.getDeltaMovement();
    }

    private boolean isChainedTo(Entity entity) {
        if (entity instanceof AbstractMinecart) {
            if (!CHECKED_PARENT_UUID_METHOD) {
                CHECKED_PARENT_UUID_METHOD = true;
                try {
                    GET_PARENT_UUID_METHOD = entity.getClass().getMethod("getParentUUID");
                } catch (Throwable ignored) {
                }
            }
            if (GET_PARENT_UUID_METHOD != null) {
                try {
                    Object uuid = GET_PARENT_UUID_METHOD.invoke(entity);
                    return this.getUUID().equals(uuid);
                } catch (Throwable ignored) {
                }
            }
        }
        return false;
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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.running = input.getBooleanOr("Running", false);
        this.fuel = input.getIntOr("Fuel", 0);
        this.fuelTotal = input.getIntOr("FuelTotal", Config.LOCOMOTIVE_COAL_BURN_TICKS.get());
        String facingName = input.getStringOr("Facing", Direction.NORTH.getName());
        this.setFacing(Direction.byName(facingName) != null ? Direction.byName(facingName) : Direction.NORTH);
    }
}
