package com.example.miningtools.entity;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.Config;
import com.example.miningtools.MiningTools;
import com.example.miningtools.menu.BasicLocomotiveMenu;
import com.example.miningtools.network.LocomotiveControlPayload;
import com.example.miningtools.util.TrainChainingUtil;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
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
    private static final EntityDataAccessor<Boolean> DATA_HEADLIGHT =
            SynchedEntityData.defineId(BasicLocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SPEED_LEVEL =
            SynchedEntityData.defineId(BasicLocomotiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FUEL_PCT =
            SynchedEntityData.defineId(BasicLocomotiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COAL_COUNT =
            SynchedEntityData.defineId(BasicLocomotiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_TRAIN_NAME =
            SynchedEntityData.defineId(BasicLocomotiveEntity.class, EntityDataSerializers.STRING);

    private static final StackWalker STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static @Nullable Method GET_PARENT_UUID_METHOD = null;
    private static boolean CHECKED_PARENT_UUID_METHOD = false;

    public static final int MIN_SPEED_LEVEL = 1;
    public static final int MAX_SPEED_LEVEL = 5;
    public static final int DEFAULT_SPEED_LEVEL = 1;

    private @Nullable BlockPos currentLightPos = null;
    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    private boolean running = false;
    private int fuel = 0;
    private int fuelTotal = 1600;
    private int speedLevel = DEFAULT_SPEED_LEVEL;
    private @Nullable BlockPos lastHornRailPos = null;
    private int hornRailCooldown = 0;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> BasicLocomotiveEntity.this.fuel;
                case 1 -> BasicLocomotiveEntity.this.fuelTotal;
                case 2 -> BasicLocomotiveEntity.this.running ? 1 : 0;
                case 3 -> BasicLocomotiveEntity.this.getFacing().get2DDataValue();
                case 4 -> BasicLocomotiveEntity.this.speedLevel;
                case 5 -> BasicLocomotiveEntity.this.isHeadlightEnabled() ? 1 : 0;
                case 6 -> BasicLocomotiveEntity.this.getId();
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
                case 5 -> BasicLocomotiveEntity.this.setHeadlightEnabled(value != 0);
            }
        }

        @Override
        public int getCount() {
            return 7;
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
        builder.define(DATA_HEADLIGHT, true);
        builder.define(DATA_SPEED_LEVEL, DEFAULT_SPEED_LEVEL);
        builder.define(DATA_FUEL_PCT, 0);
        builder.define(DATA_COAL_COUNT, 0);
        builder.define(DATA_TRAIN_NAME, "");
    }

    public String getTrainName() {
        String synced = this.entityData.get(DATA_TRAIN_NAME);
        if (synced != null && !synced.isBlank()) {
            return synced;
        }
        Component custom = this.getCustomName();
        return custom != null ? custom.getString().trim() : "";
    }

    public void setTrainName(String name) {
        String cleaned = name == null ? "" : name.trim();
        if (cleaned.length() > 32) {
            cleaned = cleaned.substring(0, 32);
        }
        this.entityData.set(DATA_TRAIN_NAME, cleaned);
        super.setCustomName(cleaned.isEmpty() ? null : Component.literal(cleaned));
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        String cleaned = name != null ? name.getString().trim() : "";
        if (cleaned.length() > 32) {
            cleaned = cleaned.substring(0, 32);
        }
        if (!Objects.equals(this.entityData.get(DATA_TRAIN_NAME), cleaned)) {
            this.entityData.set(DATA_TRAIN_NAME, cleaned);
        }
    }

    public static String resolveTrainName(Entity entity) {
        if (entity instanceof BasicLocomotiveEntity loco) {
            return loco.getTrainName();
        }
        if (entity instanceof ElectricLocomotiveEntity eloco) {
            return eloco.getTrainName();
        }
        BasicLocomotiveEntity connectedLoco = getConnectedLocomotive(entity);
        if (connectedLoco != null) {
            String locoName = connectedLoco.getTrainName();
            if (!locoName.isBlank()) {
                return locoName;
            }
        }
        ElectricLocomotiveEntity connectedElec = ElectricLocomotiveEntity.getConnectedLocomotive(entity);
        if (connectedElec != null) {
            String locoName = connectedElec.getTrainName();
            if (!locoName.isBlank()) {
                return locoName;
            }
        }
        Component custom = entity.getCustomName();
        return custom != null ? custom.getString().trim() : "";
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

    public boolean isHeadlightEnabled() {
        return this.entityData.get(DATA_HEADLIGHT);
    }

    public void setHeadlightEnabled(boolean enabled) {
        this.entityData.set(DATA_HEADLIGHT, enabled);
        if (!enabled) {
            this.clearDynamicLight();
        }
    }

    public boolean isHeadlightActive() {
        return this.isHeadlightEnabled() && (this.fuel > 0 || this.running || !this.getPassengers().isEmpty());
    }

    public int getSpeedLevel() {
        return this.level().isClientSide() ? this.entityData.get(DATA_SPEED_LEVEL) : this.speedLevel;
    }

    public void setSpeedLevel(int level) {
        this.speedLevel = Mth.clamp(level, MIN_SPEED_LEVEL, MAX_SPEED_LEVEL);
        this.entityData.set(DATA_SPEED_LEVEL, this.speedLevel);
    }

    public int getFuelPercent() {
        return this.entityData.get(DATA_FUEL_PCT);
    }

    public int getCoalCount() {
        return this.entityData.get(DATA_COAL_COUNT);
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
        return this.getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (this.hasPassenger(passenger)) {
            Direction facing = this.getFacing();
            // Driver cabin is situated at the rear of the locomotive body
            double offsetX = -facing.getStepX() * 0.35;
            double offsetZ = -facing.getStepZ() * 0.35;
            double offsetY = 0.35;
            moveFunction.accept(passenger, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ);
        }
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    public boolean isPushable() {
        return !this.running;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        if (this.isChainedTo(entity) || entity instanceof LivingEntity) {
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
        if (player.getItemInHand(hand).is(MiningTools.CROWBAR_ITEM.get())) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (!this.level().isClientSide()) {
                player.openMenu(this);
            }
            return InteractionResult.SUCCESS;
        }

        if (player.getItemInHand(hand).isEmpty()) {
            if (this.getPassengers().isEmpty()) {
                if (!this.level().isClientSide()) {
                    player.startRiding(this);
                }
                return InteractionResult.SUCCESS;
            }
        }

        if (!this.level().isClientSide()) {
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        if (this.hornRailCooldown > 0) {
            this.hornRailCooldown--;
            if (this.hornRailCooldown == 0) {
                this.lastHornRailPos = null;
            }
        }
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }
        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }
        this.checkBelowWorld();

        Direction facing = this.getFacing();
        float targetYRot = facing.toYRot();
        Vec3 pos = this.getRailPos(this.getX(), this.getY(), this.getZ());
        if (pos != null) {
            Vec3 p0 = this.getRailPosOffs(this.getX(), this.getY(), this.getZ(), 0.35F);
            Vec3 p1 = this.getRailPosOffs(this.getX(), this.getY(), this.getZ(), -0.35F);
            if (p0 != null && p1 != null) {
                Vec3 diff = p0.subtract(p1);
                if (diff.lengthSqr() > 1.0E-6D) {
                    targetYRot = (float) (Mth.atan2(diff.z, diff.x) * 180.0 / Math.PI) - 90.0F;
                }
            }
        }
        this.setYRot(targetYRot);

        if (this.level().isClientSide()) {
            // Client side particle effects
            if (this.isActive()) {
                // Chimney / smokestack position
                double chimneyForward = 0.55;
                double rad = Math.toRadians(targetYRot + 90.0);
                double chimneyX = this.getX() + Math.cos(rad) * chimneyForward;
                double chimneyZ = this.getZ() + Math.sin(rad) * chimneyForward;
                double chimneyY = this.getY() + 1.25;

                // Transparent coal smoke
                if (this.random.nextInt(3) == 0) {
                    this.level().addParticle(ParticleTypes.SMOKE, chimneyX, chimneyY, chimneyZ,
                            Math.cos(rad) * 0.02, 0.06, Math.sin(rad) * 0.02);
                }
                // Light steam puffs
                if (this.random.nextInt(4) == 0) {
                    this.level().addParticle(ParticleTypes.POOF, chimneyX, chimneyY, chimneyZ,
                            0.0, 0.03, 0.0);
                }
            }
            if (this.isHeadlightActive() && this.random.nextInt(8) == 0) {
                double headlightForward = 0.95;
                double rad = Math.toRadians(targetYRot + 90.0);
                double hlX = this.getX() + Math.cos(rad) * headlightForward;
                double hlZ = this.getZ() + Math.sin(rad) * headlightForward;
                double hlY = this.getY() + 0.65;
                this.level().addParticle(ParticleTypes.SMALL_FLAME, hlX, hlY, hlZ, 0.0, 0.005, 0.0);
            }
            return;
        }

        // Server side fuel, rail kinematics, and dynamic light
        this.tickFuelAndMovement();
        this.updateDynamicLight();

        // Push entities in path
        Direction pushFacing = this.getFacing();
        AABB pushBox = this.getBoundingBox().inflate(0.2D).expandTowards(pushFacing.getStepX() * 0.5D, 0.0D, pushFacing.getStepZ() * 0.5D);
        List<Entity> pushableEntities = this.level().getEntities(
                this,
                pushBox,
                e -> !e.noPhysics && !this.isChainedTo(e) && !this.hasPassenger(e)
        );
        for (Entity pushable : pushableEntities) {
            this.push(pushable);
        }
    }

    public boolean tryRefuel() {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = this.getItem(i);
            if (!stack.isEmpty() && stack.is(ItemTags.COALS)) {
                stack.shrink(1);
                int burnTicks = Config.LOCOMOTIVE_COAL_BURN_TICKS.get();
                this.fuelTotal = burnTicks;
                this.fuel = burnTicks;
                this.level().playSound(null, this.blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.0F);
                return true;
            }
        }
        return false;
    }

    public void startLocomotiveFromRail() {
        if (this.fuel <= 0) {
            this.tryRefuel();
        }
        if (this.fuel > 0) {
            this.setRunning(true);
            this.setActive(true);
            this.level().playSound(null, this.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public void stopLocomotiveFromRail() {
        if (this.running || this.getDeltaMovement().lengthSqr() > 1.0E-4D) {
            this.setRunning(false);
            this.setActive(false);
            this.setDeltaMovement(Vec3.ZERO);
            this.level().playSound(null, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
    }

    private void tickFuelAndMovement() {
        if (this.running) {
            // Consume fuel
            if (this.fuel > 0) {
                this.fuel--;
            } else {
                // Try consuming from the 3 fuel slots
                boolean refueled = this.tryRefuel();
                if (!refueled) {
                    // Out of fuel -> stop engine
                    this.setRunning(false);
                }
            }
        }

        boolean hasFuel = this.fuel > 0;
        this.setLit(hasFuel);

        int fuelPct = (this.fuel > 0 && this.fuelTotal > 0)
                ? Mth.clamp(Mth.ceil((this.fuel * 100.0F) / this.fuelTotal), 1, 100)
                : 0;
        if (this.entityData.get(DATA_FUEL_PCT) != fuelPct) {
            this.entityData.set(DATA_FUEL_PCT, fuelPct);
        }
        int coalTotal = 0;
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack st = this.getItem(i);
            if (!st.isEmpty() && st.is(ItemTags.COALS)) {
                coalTotal += st.getCount();
            }
        }
        if (this.entityData.get(DATA_COAL_COUNT) != coalTotal) {
            this.entityData.set(DATA_COAL_COUNT, coalTotal);
        }
        if (this.entityData.get(DATA_SPEED_LEVEL) != this.speedLevel) {
            this.entityData.set(DATA_SPEED_LEVEL, this.speedLevel);
        }

        BlockPos railPos = this.getCurrentBlockPosOrRailBelow();
        BlockState railState = this.level().getBlockState(railPos);
        boolean onRails = BaseRailBlock.isRail(railState);

        if (railState.is(MiningTools.CONTROL_RAIL.get())) {
            boolean powered = railState.getValue(PoweredRailBlock.POWERED);
            if (powered) {
                if (!this.running) {
                    this.startLocomotiveFromRail();
                    hasFuel = this.fuel > 0;
                    this.setLit(hasFuel);
                }
            } else {
                if (this.running) {
                    this.stopLocomotiveFromRail();
                }
            }
        }

        if (this.running && hasFuel && onRails) {
            this.setActive(true);
            Direction facing = this.getFacing();
            double maxSpeed = this.getLocomotiveSpeed();
            double accel = Config.LOCOMOTIVE_ACCELERATION.get();

            Vec3 currentDelta = super.getDeltaMovement();
            double currentSpeed = currentDelta.length();
            double nextSpeed = currentSpeed + (maxSpeed - currentSpeed) * accel;
            double newVx = facing.getStepX() * nextSpeed;
            double newVz = facing.getStepZ() * nextSpeed;

            RailShape shape = ((BaseRailBlock) railState.getBlock()).getRailDirection(railState, this.level(), railPos, this);

            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();
            double initialX = x;
            double initialY = y;
            double initialZ = z;

            List<Pair<Entity, AbstractMinecart>> trainLinks = this.getOrderedTrainLinks();
            Map<UUID, Vec3> initialCartPositions = new HashMap<>();
            for (Pair<Entity, AbstractMinecart> link : trainLinks) {
                AbstractMinecart c = link.getSecond();
                initialCartPositions.put(c.getUUID(), new Vec3(c.getX(), c.getY(), c.getZ()));
            }

            Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
            Vec3i exit0 = exits.getFirst();
            Vec3i exit1 = exits.getSecond();

            int entryX = -facing.getStepX();
            int entryZ = -facing.getStepZ();
            boolean entryMatches = (exit0.getX() == entryX && exit0.getZ() == entryZ) || (exit1.getX() == entryX && exit1.getZ() == entryZ);
            boolean exitMatches = (exit0.getX() == facing.getStepX() && exit0.getZ() == facing.getStepZ()) || (exit1.getX() == facing.getStepX() && exit1.getZ() == facing.getStepZ());
            if (!shape.isSlope() && !entryMatches && !exitMatches) {
                this.setPos(initialX, initialY, initialZ);
                this.stopLocomotiveFromRail();
                return;
            }

            // Center X/Z on rail and pre-lift Y on slopes to avoid wall collision with upper step
            double x0 = railPos.getX() + 0.5 + exit0.getX() * 0.5;
            double z0 = railPos.getZ() + 0.5 + exit0.getZ() * 0.5;
            double x1 = railPos.getX() + 0.5 + exit1.getX() * 0.5;
            double z1 = railPos.getZ() + 0.5 + exit1.getZ() * 0.5;
            double xD = x1 - x0;
            double zD = z1 - z0;
            double progress;
            if (xD == 0.0) {
                progress = z - railPos.getZ();
            } else if (zD == 0.0) {
                progress = x - railPos.getX();
            } else {
                double xx = x - x0;
                double zz = z - z0;
                progress = (xx * xD + zz * zD) * 2.0;
            }

            Direction moveDir = (Math.abs(newVx) > 1.0E-4D || Math.abs(newVz) > 1.0E-4D)
                    ? Direction.getApproximateNearest(newVx, 0.0, newVz)
                    : facing;
            boolean uphillAhead = BaseRailBlock.isRail(this.level().getBlockState(railPos.relative(moveDir).above()));

            x = x0 + xD * progress;
            z = z0 + zD * progress;
            y = railPos.getY();
            if (shape.isSlope() || uphillAhead) {
                y = railPos.getY() + 1.0;
            }
            this.setPos(x, y, z);

            // Align velocity with the track direction (handles curves and slopes)
            double length = Math.sqrt(xD * xD + zD * zD);
            if (length > 0.0) {
                double flip = newVx * xD + newVz * zD;
                if (flip < 0.0) {
                    xD = -xD;
                    zD = -zD;
                }
                double pow = Math.sqrt(newVx * newVx + newVz * newVz);
                if (shape.isSlope()) {
                    pow *= 0.70710678D;
                }
                newVx = pow * xD / length;
                newVz = pow * zD / length;
            }

            // Move horizontally along track
            this.move(MoverType.SELF, new Vec3(newVx, 0.0, newVz));

            // If exited down the slope via lower exit, step down Y by 1
            if (exit0.getY() != 0 && Mth.floor(this.getX()) - railPos.getX() == exit0.getX() && Mth.floor(this.getZ()) - railPos.getZ() == exit0.getZ()) {
                this.setPos(this.getX(), this.getY() + exit0.getY(), this.getZ());
            } else if (exit1.getY() != 0 && Mth.floor(this.getX()) - railPos.getX() == exit1.getX() && Mth.floor(this.getZ()) - railPos.getZ() == exit1.getZ()) {
                this.setPos(this.getX(), this.getY() + exit1.getY(), this.getZ());
            }

            // Snap X, Y, Z precisely to the rail surface
            Vec3 newRailPos = this.getRailPos(this.getX(), this.getY(), this.getZ());
            if (newRailPos != null) {
                this.setPos(newRailPos.x, newRailPos.y, newRailPos.z);
            }

            // Calculate actual 3D displacement
            double actualDx = this.getX() - initialX;
            double actualDy = this.getY() - initialY;
            double actualDz = this.getZ() - initialZ;
            double actualHoriz = Math.sqrt(actualDx * actualDx + actualDz * actualDz);
            if (actualHoriz > maxSpeed * 1.05D && maxSpeed > 0.0D) {
                double scale = (maxSpeed * 1.05D) / actualHoriz;
                actualDx *= scale;
                actualDz *= scale;
            }
            Vec3 actualMovement = new Vec3(actualDx, actualDy, actualDz);
            this.setDeltaMovement(actualMovement);

            // Check if locomotive collided with a solid block/obstacle in its path
            double forwardProgress = actualDx * facing.getStepX() + actualDz * facing.getStepZ();
            boolean hitObstacle = (this.horizontalCollision && forwardProgress < 0.005D)
                    && isPathBlockedByObstacle(this.level(), railPos, moveDir, this);
            if (hitObstacle) {
                // Stopped by hitting an obstacle (block/buffer) - stop engine and halt without reversing
                this.setRunning(false);
                this.setActive(false);
                this.setDeltaMovement(Vec3.ZERO);
                this.level().playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.BLOCKS, 0.6F, 1.4F);
            } else {
                // Update facing only if track turned around a curve (orthogonal axis change)
                Direction currentMoveDir = Direction.getApproximateNearest(actualDx, 0.0, actualDz);
                if (currentMoveDir.getAxis().isHorizontal()
                        && actualMovement.horizontalDistanceSqr() > 1.0E-4D
                        && currentMoveDir.getAxis() != this.getFacing().getAxis()) {
                    this.setFacing(currentMoveDir);
                }
            }

            BlockPos currentRail = this.getCurrentBlockPosOrRailBelow();
            BlockState currentRailState = this.level().getBlockState(currentRail);
            if (currentRailState.is(MiningTools.CONTROL_RAIL.get()) && !currentRailState.getValue(PoweredRailBlock.POWERED)) {
                this.stopLocomotiveFromRail();
            }
            if (currentRailState.is(MiningTools.HORN_RAIL.get())) {
                this.triggerHornRail(currentRail, currentRailState);
            }


            if (this.running) {
                this.handleChainedCartsMovement(this.getFacing(), trainLinks, initialCartPositions, initialX, initialY, initialZ);
            } else {
                this.handleChainedCartsMovement(this.getFacing(), trainLinks, initialCartPositions, this.getX(), this.getY(), this.getZ());
            }
        } else {
            this.setActive(false);
            // Apply braking
            Vec3 currentDelta = super.getDeltaMovement();
            if (currentDelta.horizontalDistanceSqr() > 1.0E-4D) {
                Vec3 braked = currentDelta.multiply(0.65D, 1.0D, 0.65D);
                this.setDeltaMovement(braked);
                this.move(MoverType.SELF, new Vec3(braked.x, 0.0, braked.z));
                Vec3 newRailPos = this.getRailPos(this.getX(), this.getY(), this.getZ());
                if (newRailPos != null) {
                    this.setPos(newRailPos.x, newRailPos.y, newRailPos.z);
                }
            } else {
                this.setDeltaMovement(Vec3.ZERO);
            }
            if (onRails) {
                this.handleChainedCartsMovement(this.getFacing());
            }
        }
    }

    public static boolean isPathBlockedByObstacle(Level level, BlockPos currentRailPos, Direction moveDir, Entity entity) {
        BlockState currentState = level.getBlockState(currentRailPos);
        if (BaseRailBlock.isRail(currentState)) {
            RailShape currentShape = ((BaseRailBlock) currentState.getBlock()).getRailDirection(currentState, level, currentRailPos, null);
            boolean isCurve = !currentShape.isSlope() && currentShape != RailShape.NORTH_SOUTH && currentShape != RailShape.EAST_WEST;
            if (isCurve) {
                // If on a curve, the track bends into the curve exits
                Pair<Vec3i, Vec3i> curveExits = AbstractMinecart.exits(currentShape);
                Vec3i ce0 = curveExits.getFirst();
                Vec3i ce1 = curveExits.getSecond();
                BlockPos p0 = currentRailPos.offset(ce0);
                BlockPos p1 = currentRailPos.offset(ce1);
                boolean hasCurveExitRail = BaseRailBlock.isRail(level.getBlockState(p0)) || BaseRailBlock.isRail(level.getBlockState(p0.above())) || BaseRailBlock.isRail(level.getBlockState(p0.below()))
                        || BaseRailBlock.isRail(level.getBlockState(p1)) || BaseRailBlock.isRail(level.getBlockState(p1.above())) || BaseRailBlock.isRail(level.getBlockState(p1.below()));
                if (hasCurveExitRail) {
                    BlockPos nextP = (moveDir.getStepX() * ce0.getX() + moveDir.getStepZ() * ce0.getZ() > 0) ? p0 : p1;
                    BlockPos clearance = nextP.above();
                    BlockState clearanceState = level.getBlockState(clearance);
                    if (!clearanceState.isAir() && !BaseRailBlock.isRail(clearanceState) && !clearanceState.getCollisionShape(level, clearance).isEmpty()) {
                        return true;
                    }
                    return false;
                }
            }
        }

        BlockPos nextFlat = currentRailPos.relative(moveDir);
        BlockPos nextUp = nextFlat.above();
        BlockPos nextDown = nextFlat.below();

        BlockState flatState = level.getBlockState(nextFlat);
        BlockState upState = level.getBlockState(nextUp);
        BlockState downState = level.getBlockState(nextDown);

        boolean hasFlatRail = BaseRailBlock.isRail(flatState);
        boolean hasUpRail = BaseRailBlock.isRail(upState);
        boolean hasDownRail = BaseRailBlock.isRail(downState);

        // If none of the 3 positions has a rail, the track has ended (buffer stop, stone wall at end of line, dead end)
        if (!hasFlatRail && !hasUpRail && !hasDownRail) {
            return true;
        }

        // Check if an obstruction block was placed in the clearance path
        BlockPos nextRailPos = hasFlatRail ? nextFlat : (hasUpRail ? nextUp : nextDown);
        BlockPos clearancePos = nextRailPos.above();
        BlockState clearanceState = level.getBlockState(clearancePos);
        if (!clearanceState.isAir() && !BaseRailBlock.isRail(clearanceState)) {
            if (!clearanceState.getCollisionShape(level, clearancePos).isEmpty()) {
                return true; // Solid block obstructing cart clearance
            }
        }

        // For locomotive (which is 1.25m tall), also check clearancePos.above()
        if (entity instanceof BasicLocomotiveEntity) {
            BlockPos clearancePos2 = clearancePos.above();
            BlockState clearanceState2 = level.getBlockState(clearancePos2);
            if (!clearanceState2.isAir() && !clearanceState2.getCollisionShape(level, clearancePos2).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public @Nullable Vec3 getRailPosOffs(double x, double y, double z, double offset) {
        int xt = Mth.floor(x);
        int yt = Mth.floor(y);
        int zt = Mth.floor(z);
        if (BaseRailBlock.isRail(this.level().getBlockState(new BlockPos(xt, yt - 1, zt)))) {
            yt--;
        } else if (!BaseRailBlock.isRail(this.level().getBlockState(new BlockPos(xt, yt, zt)))
                && BaseRailBlock.isRail(this.level().getBlockState(new BlockPos(xt, yt + 1, zt)))) {
            yt++;
        }

        BlockPos railPos = new BlockPos(xt, yt, zt);
        BlockState state = this.level().getBlockState(railPos);
        if (BaseRailBlock.isRail(state)) {
            RailShape shape = ((BaseRailBlock) state.getBlock()).getRailDirection(state, this.level(), railPos, this);
            y = yt;
            if (shape.isSlope()) {
                y = yt + 1;
            }

            Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
            Vec3i exit0 = exits.getFirst();
            Vec3i exit1 = exits.getSecond();
            double xD = exit1.getX() - exit0.getX();
            double zD = exit1.getZ() - exit0.getZ();
            Direction facing = this.getFacing();
            double dirDot = xD * facing.getStepX() + zD * facing.getStepZ();
            if (dirDot < 0.0) {
                xD = -xD;
                zD = -zD;
            }
            double dd = Math.sqrt(xD * xD + zD * zD);
            if (dd > 0.0) {
                xD /= dd;
                zD /= dd;
            }
            x += xD * offset;
            z += zD * offset;
            if (exit0.getY() != 0 && Mth.floor(x) - xt == exit0.getX() && Mth.floor(z) - zt == exit0.getZ()) {
                y += exit0.getY();
            } else if (exit1.getY() != 0 && Mth.floor(x) - xt == exit1.getX() && Mth.floor(z) - zt == exit1.getZ()) {
                y += exit1.getY();
            }

            return this.getRailPos(x, y, z);
        } else {
            return null;
        }
    }

    public @Nullable Vec3 getRailPos(double x, double y, double z) {
        return this.getRailPosForEntity(this, x, y, z);
    }

    public @Nullable Vec3 getRailPosForEntity(AbstractMinecart entity, double x, double y, double z) {
        int xt = Mth.floor(x);
        int yt = Mth.floor(y);
        int zt = Mth.floor(z);
        if (BaseRailBlock.isRail(this.level().getBlockState(new BlockPos(xt, yt - 1, zt)))) {
            yt--;
        } else if (!BaseRailBlock.isRail(this.level().getBlockState(new BlockPos(xt, yt, zt)))
                && BaseRailBlock.isRail(this.level().getBlockState(new BlockPos(xt, yt + 1, zt)))) {
            yt++;
        }

        BlockPos railPos = new BlockPos(xt, yt, zt);
        BlockState state = this.level().getBlockState(railPos);
        if (BaseRailBlock.isRail(state)) {
            RailShape shape = ((BaseRailBlock) state.getBlock()).getRailDirection(state, this.level(), railPos, entity);
            Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
            Vec3i exit0 = exits.getFirst();
            Vec3i exit1 = exits.getSecond();
            double x0 = xt + 0.5 + exit0.getX() * 0.5;
            double y0 = yt + 0.0625 + exit0.getY() * 0.5;
            double z0 = zt + 0.5 + exit0.getZ() * 0.5;
            double x1 = xt + 0.5 + exit1.getX() * 0.5;
            double y1 = yt + 0.0625 + exit1.getY() * 0.5;
            double z1 = zt + 0.5 + exit1.getZ() * 0.5;
            double xD = x1 - x0;
            double yD = (y1 - y0) * 2.0;
            double zD = z1 - z0;
            double progress;
            if (xD == 0.0) {
                progress = z - zt;
            } else if (zD == 0.0) {
                progress = x - xt;
            } else {
                double xx = x - x0;
                double zz = z - z0;
                progress = (xx * xD + zz * zD) * 2.0;
            }

            x = x0 + xD * progress;
            y = y0 + yD * progress;
            z = z0 + zD * progress;
            if (yD < 0.0) {
                y++;
            } else if (yD > 0.0) {
                y += 0.5;
            }

            return new Vec3(x, y, z);
        } else {
            return null;
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
        return TrainChainingUtil.getCartParentUUID(entity);
    }

    public static @Nullable UUID getCartChildUUID(Entity entity) {
        return TrainChainingUtil.getCartChildUUID(entity);
    }


    public List<AbstractMinecart> getConnectedTrainCarts() {
        List<AbstractMinecart> result = new ArrayList<>();
        for (Pair<Entity, AbstractMinecart> link : this.getOrderedTrainLinks()) {
            result.add(link.getSecond());
        }
        return result;
    }

    public List<Pair<Entity, AbstractMinecart>> getOrderedTrainLinks() {
        List<Pair<Entity, AbstractMinecart>> result = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        visited.add(this.getUUID());

        Deque<Pair<Entity, UUID>> toCheck = new ArrayDeque<>();
        UUID p = getCartParentUUID(this);
        if (p != null) toCheck.add(Pair.of(this, p));
        UUID c = getCartChildUUID(this);
        if (c != null) toCheck.add(Pair.of(this, c));

        while (!toCheck.isEmpty()) {
            Pair<Entity, UUID> next = toCheck.poll();
            UUID nextId = next.getSecond();
            if (nextId != null && visited.add(nextId)) {
                Entity ent = this.level().getEntity(nextId);
                if (ent instanceof AbstractMinecart cart && !cart.isRemoved()) {
                    result.add(Pair.of(next.getFirst(), cart));
                    UUID cp = getCartParentUUID(cart);
                    if (cp != null && !visited.contains(cp)) toCheck.add(Pair.of(cart, cp));
                    UUID cc = getCartChildUUID(cart);
                    if (cc != null && !visited.contains(cc)) toCheck.add(Pair.of(cart, cc));
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
        double baseX = super.position().x;
        double baseY = super.position().y;
        double baseZ = super.position().z;
        double dx = otherPos.x - baseX;
        double dz = otherPos.z - baseZ;
        double dot = dx * facing.getStepX() + dz * facing.getStepZ();
        // If otherPos is behind locomotive (dot < 0), use rear bumper (-facing * 0.36)
        // If otherPos is in front of locomotive (dot >= 0), use front bumper (+facing * 0.36)
        double sign = dot < 0 ? -1.0 : 1.0;
        double anchorX = baseX + facing.getStepX() * (0.36 * sign);
        double anchorZ = baseZ + facing.getStepZ() * (0.36 * sign);
        Vec3 railAtAnchor = this.getRailPos(anchorX, baseY, anchorZ);
        double anchorY = railAtAnchor != null ? railAtAnchor.y : baseY;
        return new Vec3(anchorX, anchorY, anchorZ);
    }

    public @Nullable AbstractMinecart getNearestConnectedCart() {
        List<AbstractMinecart> carts = this.getConnectedTrainCarts();
        if (carts.isEmpty()) return null;
        AbstractMinecart nearest = carts.get(0);
        double nearestDistSqr = super.distanceToSqr(nearest.getX(), nearest.getY(), nearest.getZ());
        for (int i = 1; i < carts.size(); i++) {
            AbstractMinecart cart = carts.get(i);
            double d = super.distanceToSqr(cart.getX(), cart.getY(), cart.getZ());
            if (d < nearestDistSqr) {
                nearest = cart;
                nearestDistSqr = d;
            }
        }
        return nearest;
    }

    public static String getSpeedName(int speedLevel) {
        return switch (speedLevel) {
            case 1 -> "Crawl";
            case 2 -> "Slow";
            case 3 -> "Normal";
            case 4 -> "Fast";
            case 5 -> "Max";
            default -> "Normal";
        };
    }

    public static @Nullable BasicLocomotiveEntity getConnectedLocomotive(Entity entity) {
        if (entity instanceof BasicLocomotiveEntity loco && !loco.isRemoved()) {
            return loco;
        }
        if (!(entity instanceof AbstractMinecart cart) || cart.isRemoved()) {
            return null;
        }

        Set<UUID> visited = new HashSet<>();
        visited.add(cart.getUUID());
        Deque<UUID> toCheck = new ArrayDeque<>();
        UUID p = getCartParentUUID(cart);
        if (p != null) toCheck.add(p);
        UUID c = getCartChildUUID(cart);
        if (c != null) toCheck.add(c);

        while (!toCheck.isEmpty()) {
            UUID nextId = toCheck.poll();
            if (nextId != null && visited.add(nextId)) {
                Entity ent = cart.level().getEntity(nextId);
                if (ent instanceof BasicLocomotiveEntity loco && !loco.isRemoved()) {
                    return loco;
                }
                if (ent instanceof AbstractMinecart otherCart && !otherCart.isRemoved()) {
                    UUID cp = getCartParentUUID(otherCart);
                    if (cp != null && !visited.contains(cp)) toCheck.add(cp);
                    UUID cc = getCartChildUUID(otherCart);
                    if (cc != null && !visited.contains(cc)) toCheck.add(cc);
                }
            }
        }
        return null;
    }

    private void sendNotification(Player player, Component message) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, true);
        } else {
            player.sendSystemMessage(message);
        }
    }

    public void handleHotkeyAction(Player player, int action) {
        switch (action) {
            case LocomotiveControlPayload.ACTION_START -> {
                if (!this.running) {
                    if (this.fuel <= 0) {
                        this.tryRefuel();
                    }
                    if (this.fuel > 0) {
                        this.setRunning(true);
                        this.setActive(true);
                        this.level().playSound(null, this.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                        sendNotification(player, Component.translatable("message.miningtools.locomotive.started"));
                    } else {
                        this.level().playSound(null, this.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8F, 1.2F);
                        sendNotification(player, Component.translatable("message.miningtools.locomotive.no_fuel"));
                    }
                }
            }
            case LocomotiveControlPayload.ACTION_STOP -> {
                if (this.running) {
                    this.setRunning(false);
                    this.setActive(false);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.2F);
                    sendNotification(player, Component.translatable("message.miningtools.locomotive.stopped"));
                }
            }
            case LocomotiveControlPayload.ACTION_SPEED_DOWN -> {
                if (this.speedLevel > MIN_SPEED_LEVEL) {
                    this.setSpeedLevel(this.speedLevel - 1);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 0.9F);
                    sendNotification(player, Component.translatable("message.miningtools.locomotive.speed", getSpeedName(this.speedLevel)));
                } else {
                    sendNotification(player, Component.translatable("message.miningtools.locomotive.speed_min"));
                }
            }
            case LocomotiveControlPayload.ACTION_SPEED_UP -> {
                if (this.speedLevel < MAX_SPEED_LEVEL) {
                    this.setSpeedLevel(this.speedLevel + 1);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.1F);
                    sendNotification(player, Component.translatable("message.miningtools.locomotive.speed", getSpeedName(this.speedLevel)));
                } else {
                    sendNotification(player, Component.translatable("message.miningtools.locomotive.speed_max"));
                }
            }
            case LocomotiveControlPayload.ACTION_REVERSE -> {
                Direction newFacing = this.getFacing().getOpposite();
                this.setFacing(newFacing);
                this.level().playSound(null, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.8F, 1.2F);
                sendNotification(player, Component.translatable("message.miningtools.locomotive.direction", newFacing.getName().toUpperCase()));
            }
            case LocomotiveControlPayload.ACTION_HORN -> {
                this.blowHorn(player);
            }
            case LocomotiveControlPayload.ACTION_TOGGLE_LIGHT -> {
                boolean newState = !this.isHeadlightEnabled();
                this.setHeadlightEnabled(newState);
                this.level().playSound(null, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.8F, newState ? 1.4F : 0.8F);
                sendNotification(player, Component.translatable(newState ? "message.miningtools.locomotive.headlight_on" : "message.miningtools.locomotive.headlight_off"));
            }
        }
    }

    public void triggerHornRail(BlockPos railPos, BlockState railState) {
        if (this.level().isClientSide()) {
            return;
        }
        // Option C: Muted by redstone
        if (railState.getValue(PoweredRailBlock.POWERED)) {
            return;
        }
        if (this.hornRailCooldown <= 0 && !railPos.equals(this.lastHornRailPos)) {
            this.blowHorn(null);
            this.lastHornRailPos = railPos;
            this.hornRailCooldown = 25;
        }
    }

    public void blowHorn(@Nullable Player player) {
        if (this.level().isClientSide()) {
            return;
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                MiningTools.LOCOMOTIVE_WHISTLE.get(), SoundSource.NEUTRAL, 3.0F, 0.95F + this.random.nextFloat() * 0.1F);

        float targetYRot = this.getYRot();
        double rad = Math.toRadians(targetYRot + 90.0);
        double whistleForward = -0.25;
        double whistleX = this.getX() + Math.cos(rad) * whistleForward;
        double whistleZ = this.getZ() + Math.sin(rad) * whistleForward;
        double whistleY = this.getY() + 1.45;

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, whistleX, whistleY, whistleZ, 6, 0.05, 0.15, 0.05, 0.02);
            serverLevel.sendParticles(ParticleTypes.CLOUD, whistleX, whistleY, whistleZ, 4, 0.04, 0.08, 0.04, 0.02);
        }

        AABB scareBox = this.getBoundingBox().inflate(12.0, 4.0, 12.0);
        List<PathfinderMob> mobs = this.level().getEntitiesOfClass(PathfinderMob.class, scareBox);
        for (PathfinderMob mob : mobs) {
            Vec3 away = mob.position().subtract(this.position()).normalize();
            mob.getNavigation().moveTo(mob.getX() + away.x * 7.0, mob.getY(), mob.getZ() + away.z * 7.0, 1.4);
        }
    }

    public void updateDynamicLight() {
        if (this.level().isClientSide()) {
            return;
        }
        if (!this.isHeadlightActive() || this.isRemoved()) {
            this.clearDynamicLight();
            return;
        }

        Direction facing = this.getFacing();
        BlockPos frontPos = BlockPos.containing(
                this.getX() + facing.getStepX() * 1.25,
                this.getY() + 0.8,
                this.getZ() + facing.getStepZ() * 1.25
        );

        BlockPos targetPos = frontPos;
        BlockState targetState = this.level().getBlockState(targetPos);
        if (!targetState.isAir() && !targetState.is(Blocks.LIGHT)) {
            targetPos = this.blockPosition().above();
            targetState = this.level().getBlockState(targetPos);
            if (!targetState.isAir() && !targetState.is(Blocks.LIGHT)) {
                targetPos = this.blockPosition();
                targetState = this.level().getBlockState(targetPos);
            }
        }

        if (!Objects.equals(this.currentLightPos, targetPos)) {
            this.clearDynamicLight();
            if (targetState.isAir() || targetState.is(Blocks.LIGHT)) {
                this.level().setBlock(targetPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
                this.currentLightPos = targetPos;
            }
        } else if (targetState.isAir()) {
            this.level().setBlock(targetPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
        }
    }

    public void clearDynamicLight() {
        if (this.currentLightPos != null && !this.level().isClientSide()) {
            BlockState state = this.level().getBlockState(this.currentLightPos);
            if (state.is(Blocks.LIGHT)) {
                this.level().setBlock(this.currentLightPos, Blocks.AIR.defaultBlockState(), 3);
            }
            this.currentLightPos = null;
        }
    }

    @Override
    public void activateMinecart(ServerLevel level, int x, int y, int z, boolean receivingPower) {
        if (receivingPower) {
            this.blowHorn(null);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        this.clearDynamicLight();
        super.remove(reason);
    }

    @Override
    public void onRemovedFromLevel() {
        this.clearDynamicLight();
        super.onRemovedFromLevel();
    }

    @Override
    public Vec3 position() {
        if (isCalledByTrainChaining()) {
            AbstractMinecart nearest = this.getNearestConnectedCart();
            if (nearest != null) {
                return this.getAnchorPosFor(nearest.position());
            }
            Direction facing = this.getFacing();
            double anchorX = super.position().x - facing.getStepX() * 0.36;
            double anchorZ = super.position().z - facing.getStepZ() * 0.36;
            Vec3 railAtAnchor = this.getRailPos(anchorX, super.position().y, anchorZ);
            double anchorY = railAtAnchor != null ? railAtAnchor.y : super.position().y;
            return new Vec3(anchorX, anchorY, anchorZ);
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

    private void handleChainedCartsMovement(Direction facing) {
        List<Pair<Entity, AbstractMinecart>> trainLinks = this.getOrderedTrainLinks();
        Map<UUID, Vec3> initialCartPositions = new HashMap<>();
        for (Pair<Entity, AbstractMinecart> link : trainLinks) {
            AbstractMinecart c = link.getSecond();
            initialCartPositions.put(c.getUUID(), new Vec3(c.getX(), c.getY(), c.getZ()));
        }
        this.handleChainedCartsMovement(facing, trainLinks, initialCartPositions, this.getX(), this.getY(), this.getZ());
    }

    private boolean handleChainedCartsMovement(Direction facing,
                                               List<Pair<Entity, AbstractMinecart>> links,
                                               Map<UUID, Vec3> initialCartPositions,
                                               double initialLocoX, double initialLocoY, double initialLocoZ) {
        if (links.isEmpty()) {
            return false;
        }

        boolean moving = this.isActive() || super.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;
        double maxStep = moving ? Math.max(this.getLocomotiveSpeed() * 1.6D, 0.25D) : 0.15D;

        boolean blocked = false;
        for (Pair<Entity, AbstractMinecart> link : links) {
            Entity prev = link.getFirst();
            AbstractMinecart cart = link.getSecond();

            Vec3 anchorPos = (prev == this)
                    ? this.getAnchorPosFor(new Vec3(cart.getX(), cart.getY(), cart.getZ()))
                    : new Vec3(prev.getX(), prev.getY(), prev.getZ());

            double targetDist = TrainChainingUtil.getTargetCouplingDistance(prev, cart, prev == this);
            double minAllowed = moving ? (targetDist - 0.08D) : (targetDist - 0.12D);
            double maxAllowed = moving ? (targetDist + 0.08D) : (targetDist + 0.12D);

            Vec3 cartPos = new Vec3(cart.getX(), cart.getY(), cart.getZ());
            Vec3 diff = cartPos.subtract(anchorPos);
            double horizDist = diff.horizontalDistance();
            double trackDist = horizDist + 0.41421356D * Math.abs(diff.y);

            double dirX;
            double dirZ;
            if (horizDist > 1.0E-3D) {
                dirX = diff.x / horizDist;
                dirZ = diff.z / horizDist;
            } else {
                dirX = facing.getStepX();
                dirZ = facing.getStepZ();
            }

            if (trackDist > maxAllowed) {
                // Cart is lagging behind anchor -> pull cart along its rails toward anchor
                double needed = Math.min(trackDist - targetDist, maxStep);
                boolean success = this.moveCartAlongRails(cart, -dirX, -dirZ, needed);
                if (!success && moving && this.running) {
                    blocked = true;
                    break;
                }
            } else if (trackDist < minAllowed) {
                // Cart is ahead of or too close to anchor -> push cart along its rails away from anchor
                double needed = Math.min(targetDist - trackDist, maxStep);
                boolean success = this.moveCartAlongRails(cart, dirX, dirZ, needed);
                if (!success && moving && this.running) {
                    blocked = true;
                    break;
                }
            } else {
                Vec3 snapped = this.getRailPosForEntity(cart, cart.getX(), cart.getY(), cart.getZ());
                if (snapped != null) {
                    cart.setPos(snapped.x, snapped.y, snapped.z);
                }
                cart.setDeltaMovement(Vec3.ZERO);
            }
        }

        if (blocked) {
            // Revert locomotive position to before this tick's move so it does NOT drive through the cart
            this.setPos(initialLocoX, initialLocoY, initialLocoZ);
            Vec3 railPos = this.getRailPos(initialLocoX, initialLocoY, initialLocoZ);
            if (railPos != null) {
                this.setPos(railPos.x, railPos.y, railPos.z);
            }
            this.setDeltaMovement(Vec3.ZERO);
            this.setRunning(false);
            this.setActive(false);
            this.level().playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.BLOCKS, 0.6F, 1.4F);

            // Revert any carts that moved forward this tick
            for (Pair<Entity, AbstractMinecart> link : links) {
                AbstractMinecart c = link.getSecond();
                Vec3 initPos = initialCartPositions.get(c.getUUID());
                if (initPos != null) {
                    c.setPos(initPos.x, initPos.y, initPos.z);
                    Vec3 cRail = this.getRailPosForEntity(c, initPos.x, initPos.y, initPos.z);
                    if (cRail != null) {
                        c.setPos(cRail.x, cRail.y, cRail.z);
                    }
                    c.setDeltaMovement(Vec3.ZERO);
                }
            }
            return true;
        }

        return false;
    }

    private boolean moveCartAlongRails(AbstractMinecart cart, double dirX, double dirZ, double distance3D) {
        if (distance3D < 1.0E-5D) {
            return true;
        }

        // Shove any living entities in the path of the cart out of the way so they do not stop the train
        AABB pushBox = cart.getBoundingBox().inflate(0.2D).expandTowards(dirX * 0.5D, 0.0D, dirZ * 0.5D);
        List<Entity> pushables = this.level().getEntities(
                cart,
                pushBox,
                e -> !e.noPhysics && !this.isChainedTo(e) && !cart.hasPassenger(e)
        );
        for (Entity pushable : pushables) {
            if (pushable instanceof LivingEntity living) {
                double lateralSign = (living.getX() - cart.getX()) * (-dirZ) + (living.getZ() - cart.getZ()) * dirX >= 0 ? 1.0 : -1.0;
                double pushStrength = Math.max(this.getLocomotiveSpeed() * 2.2D, 0.5D);
                double pushX = dirX * pushStrength + (-dirZ) * 0.4D * lateralSign;
                double pushZ = dirZ * pushStrength + dirX * 0.4D * lateralSign;
                living.setDeltaMovement(pushX, 0.25D, pushZ);
                living.needsSync = true;
            }
        }

        BlockPos railPos = cart.getCurrentBlockPosOrRailBelow();
        BlockState railState = this.level().getBlockState(railPos);
        if (!BaseRailBlock.isRail(railState)) {
            cart.move(MoverType.SELF, new Vec3(dirX * distance3D, 0.0, dirZ * distance3D));
            return !cart.horizontalCollision;
        }

        RailShape shape = ((BaseRailBlock) railState.getBlock()).getRailDirection(railState, this.level(), railPos, cart);
        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
        Vec3i exit0 = exits.getFirst();
        Vec3i exit1 = exits.getSecond();

        double x = cart.getX();
        double y = cart.getY();
        double z = cart.getZ();
        double initialX = x;
        double initialZ = z;

        Direction cartDir = (Math.abs(dirX) > 1.0E-4D || Math.abs(dirZ) > 1.0E-4D)
                ? Direction.getApproximateNearest(dirX, 0.0, dirZ)
                : cart.getMotionDirection();
        boolean uphillAhead = BaseRailBlock.isRail(this.level().getBlockState(railPos.relative(cartDir).above()));

        // Center X/Z on rail and pre-lift Y on slopes so move() clears the upper step block
        double x0 = railPos.getX() + 0.5 + exit0.getX() * 0.5;
        double z0 = railPos.getZ() + 0.5 + exit0.getZ() * 0.5;
        double x1 = railPos.getX() + 0.5 + exit1.getX() * 0.5;
        double z1 = railPos.getZ() + 0.5 + exit1.getZ() * 0.5;
        double xD = x1 - x0;
        double zD = z1 - z0;
        double progress;
        if (xD == 0.0) {
            progress = z - railPos.getZ();
        } else if (zD == 0.0) {
            progress = x - railPos.getX();
        } else {
            double xx = x - x0;
            double zz = z - z0;
            progress = (xx * xD + zz * zD) * 2.0;
        }

        x = x0 + xD * progress;
        z = z0 + zD * progress;
        y = railPos.getY();
        if (shape.isSlope() || uphillAhead) {
            y = railPos.getY() + 1.0;
        }
        cart.setPos(x, y, z);

        double horizStep = shape.isSlope() ? distance3D * 0.70710678D : distance3D;
        double length = Math.sqrt(xD * xD + zD * zD);
        double alignedVx = dirX * horizStep;
        double alignedVz = dirZ * horizStep;
        if (length > 0.0) {
            double flip = dirX * xD + dirZ * zD;
            if (flip < 0.0) {
                xD = -xD;
                zD = -zD;
            }
            alignedVx = horizStep * xD / length;
            alignedVz = horizStep * zD / length;
        }

        cart.move(MoverType.SELF, new Vec3(alignedVx, 0.0, alignedVz));

        // Step down Y if exiting slope through the lower exit
        if (exit0.getY() != 0 && Mth.floor(cart.getX()) - railPos.getX() == exit0.getX() && Mth.floor(cart.getZ()) - railPos.getZ() == exit0.getZ()) {
            cart.setPos(cart.getX(), cart.getY() + exit0.getY(), cart.getZ());
        } else if (exit1.getY() != 0 && Mth.floor(cart.getX()) - railPos.getX() == exit1.getX() && Mth.floor(cart.getZ()) - railPos.getZ() == exit1.getZ()) {
            cart.setPos(cart.getX(), cart.getY() + exit1.getY(), cart.getZ());
        }

        // Snap precisely onto the new rail position
        Vec3 newRailPos = this.getRailPosForEntity(cart, cart.getX(), cart.getY(), cart.getZ());
        if (newRailPos != null) {
            cart.setPos(newRailPos.x, newRailPos.y, newRailPos.z);
        }

        double actualDx = cart.getX() - initialX;
        double actualDz = cart.getZ() - initialZ;
        double actualHoriz = Math.sqrt(actualDx * actualDx + actualDz * actualDz);

        if (actualDx * actualDx + actualDz * actualDz > 1.0E-5D) {
            float yaw = (float) (Mth.atan2(-actualDz, -actualDx) * 180.0 / Math.PI);
            if (cart.isFlipped()) {
                yaw += 180.0F;
            }
            cart.setYRot(yaw % 360.0F);
        }

        cart.setDeltaMovement(Vec3.ZERO);

        boolean offRails = (newRailPos == null);
        boolean blocked = false;
        if (offRails) {
            blocked = true;
        } else if ((cart.horizontalCollision && actualHoriz < horizStep * 0.3D) || (horizStep > 0.02D && actualHoriz < 0.005D)) {
            if (isPathBlockedByObstacle(this.level(), railPos, cartDir, cart)) {
                blocked = true;
            }
        }
        return !blocked;
    }

    @Override
    public void push(Entity entity) {
        if (!this.level().isClientSide() && !entity.noPhysics && !this.hasPassenger(entity)) {
            if (this.isChainedTo(entity)) {
                return;
            }
            if (entity instanceof LivingEntity) {
                // When locomotive hits a player, animal, or mob: shove them out of the way without stopping the locomotive
                Direction facing = this.getFacing();
                Direction lateral = facing.getClockWise();
                double lateralDist = (entity.getX() - this.getX()) * lateral.getStepX() + (entity.getZ() - this.getZ()) * lateral.getStepZ();
                double lateralSign = lateralDist >= 0 ? 1.0 : -1.0;
                double speed = this.isActive() ? this.getLocomotiveSpeed() : Math.max(super.getDeltaMovement().horizontalDistance(), 0.1D);
                double pushStrength = Math.max(speed * 2.2D, 0.5D);
                double pushX = facing.getStepX() * pushStrength + lateral.getStepX() * 0.4D * lateralSign;
                double pushZ = facing.getStepZ() * pushStrength + lateral.getStepZ() * 0.4D * lateralSign;
                double pushY = 0.25D;
                entity.setDeltaMovement(pushX, pushY, pushZ);
                entity.needsSync = true;
                return;
            }
            if (this.running) {
                // When running, the locomotive is not pushed by other entities; it shoves other pushable entities forward
                if (entity.isPushable()) {
                    Direction facing = this.getFacing();
                    double pushStrength = Math.max(this.getLocomotiveSpeed(), 0.3D);
                    entity.setDeltaMovement(facing.getStepX() * pushStrength, entity.getDeltaMovement().y, facing.getStepZ() * pushStrength);
                    entity.needsSync = true;
                }
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
        output.putBoolean("Headlight", this.isHeadlightEnabled());
        output.putString("TrainName", this.getTrainName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.running = input.getBooleanOr("Running", false);
        this.fuel = input.getIntOr("Fuel", 0);
        this.fuelTotal = input.getIntOr("FuelTotal", Config.LOCOMOTIVE_COAL_BURN_TICKS.get());
        String facingName = input.getStringOr("Facing", Direction.NORTH.getName());
        this.setFacing(Direction.byName(facingName) != null ? Direction.byName(facingName) : Direction.NORTH);
        this.setSpeedLevel(input.getIntOr("SpeedLevel", DEFAULT_SPEED_LEVEL));
        this.setHeadlightEnabled(input.getBooleanOr("Headlight", true));
        String savedTrainName = input.getStringOr("TrainName", "").trim();
        if (!savedTrainName.isEmpty()) {
            this.setTrainName(savedTrainName);
        } else if (this.getCustomName() != null) {
            this.entityData.set(DATA_TRAIN_NAME, this.getCustomName().getString().trim());
        }
    }
}
