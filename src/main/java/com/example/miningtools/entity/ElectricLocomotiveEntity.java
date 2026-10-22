package com.example.miningtools.entity;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.Config;
import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.ElectricRailBlockEntity;
import com.example.miningtools.menu.ElectricLocomotiveMenu;
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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class ElectricLocomotiveEntity extends AbstractMinecartContainer {
    public static final int INVENTORY_SIZE = 1;

    private static final EntityDataAccessor<Direction> DATA_FACING =
            SynchedEntityData.defineId(ElectricLocomotiveEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE =
            SynchedEntityData.defineId(ElectricLocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HEADLIGHT =
            SynchedEntityData.defineId(ElectricLocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_POWERED_FROM_RAIL =
            SynchedEntityData.defineId(ElectricLocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SPEED_LEVEL =
            SynchedEntityData.defineId(ElectricLocomotiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ENERGY_STORED =
            SynchedEntityData.defineId(ElectricLocomotiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ENERGY_CAPACITY =
            SynchedEntityData.defineId(ElectricLocomotiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_TRAIN_NAME =
            SynchedEntityData.defineId(ElectricLocomotiveEntity.class, EntityDataSerializers.STRING);

    private static @Nullable Method GET_PARENT_UUID_METHOD = null;
    private static boolean CHECKED_PARENT_UUID_METHOD = false;
    private static @Nullable Method GET_CHILD_UUID_METHOD = null;
    private static boolean CHECKED_CHILD_UUID_METHOD = false;

    public static final int MIN_SPEED_LEVEL = 1;
    public static final int MAX_SPEED_LEVEL = 5;
    public static final int DEFAULT_SPEED_LEVEL = 1;

    private @Nullable BlockPos currentLightPos = null;
    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    private boolean running = false;
    private int speedLevel = DEFAULT_SPEED_LEVEL;
    private boolean poweredFromRail = false;
    private @Nullable BlockPos lastHornRailPos = null;
    private int hornRailCooldown = 0;

    private final SimpleEnergyHandler battery = new SimpleEnergyHandler(
            Config.ELECTRIC_LOCOMOTIVE_BATTERY_CAPACITY.get(),
            5000,
            5000,
            0
    ) {
        @Override
        protected void onEnergyChanged(int previousEnergy) {
            super.onEnergyChanged(previousEnergy);
            ElectricLocomotiveEntity.this.syncEnergyData();
        }
    };

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> ElectricLocomotiveEntity.this.battery.getAmountAsInt() & 0xFFFF;
                case 1 -> (ElectricLocomotiveEntity.this.battery.getAmountAsInt() >> 16) & 0xFFFF;
                case 2 -> ElectricLocomotiveEntity.this.battery.getCapacityAsInt() & 0xFFFF;
                case 3 -> (ElectricLocomotiveEntity.this.battery.getCapacityAsInt() >> 16) & 0xFFFF;
                case 4 -> ElectricLocomotiveEntity.this.running ? 1 : 0;
                case 5 -> ElectricLocomotiveEntity.this.getFacing().get2DDataValue();
                case 6 -> ElectricLocomotiveEntity.this.speedLevel;
                case 7 -> ElectricLocomotiveEntity.this.isHeadlightEnabled() ? 1 : 0;
                case 8 -> ElectricLocomotiveEntity.this.poweredFromRail ? 1 : 0;
                case 9 -> ElectricLocomotiveEntity.this.getId();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 4 -> ElectricLocomotiveEntity.this.setRunning(value != 0);
                case 5 -> ElectricLocomotiveEntity.this.setFacing(Direction.from2DDataValue(value));
                case 6 -> ElectricLocomotiveEntity.this.setSpeedLevel(value);
                case 7 -> ElectricLocomotiveEntity.this.setHeadlightEnabled(value != 0);
            }
        }

        @Override
        public int getCount() {
            return 10;
        }
    };

    public ElectricLocomotiveEntity(EntityType<? extends ElectricLocomotiveEntity> type, Level level) {
        super(type, level);
    }

    public ElectricLocomotiveEntity(Level level, double x, double y, double z) {
        super(MiningTools.ELECTRIC_LOCOMOTIVE_ENTITY.get(), level);
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
        builder.define(DATA_HEADLIGHT, false);
        builder.define(DATA_POWERED_FROM_RAIL, false);
        builder.define(DATA_SPEED_LEVEL, DEFAULT_SPEED_LEVEL);
        builder.define(DATA_ENERGY_STORED, 0);
        builder.define(DATA_ENERGY_CAPACITY, Config.ELECTRIC_LOCOMOTIVE_BATTERY_CAPACITY.get());
        builder.define(DATA_TRAIN_NAME, "");
    }

    public EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return this.battery;
    }

    public SimpleEnergyHandler getInternalBattery() {
        return this.battery;
    }

    private void syncEnergyData() {
        if (!this.level().isClientSide()) {
            this.entityData.set(DATA_ENERGY_STORED, this.battery.getAmountAsInt());
            this.entityData.set(DATA_ENERGY_CAPACITY, this.battery.getCapacityAsInt());
        }
    }

    public int getEnergyStored() {
        return this.level().isClientSide() ? this.entityData.get(DATA_ENERGY_STORED) : this.battery.getAmountAsInt();
    }

    public int getEnergyCapacity() {
        return this.level().isClientSide() ? this.entityData.get(DATA_ENERGY_CAPACITY) : this.battery.getCapacityAsInt();
    }

    public boolean isPoweredFromRail() {
        return this.level().isClientSide() ? this.entityData.get(DATA_POWERED_FROM_RAIL) : this.poweredFromRail;
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

    public boolean isHeadlightEnabled() {
        return this.entityData.get(DATA_HEADLIGHT);
    }

    public void setHeadlightEnabled(boolean enabled) {
        this.entityData.set(DATA_HEADLIGHT, enabled);
        if (!enabled && !this.level().isClientSide()) {
            this.removeDynamicLight();
        }
    }

    public int getSpeedLevel() {
        return this.level().isClientSide() ? this.entityData.get(DATA_SPEED_LEVEL) : this.speedLevel;
    }

    public void setSpeedLevel(int level) {
        this.speedLevel = Mth.clamp(level, MIN_SPEED_LEVEL, MAX_SPEED_LEVEL);
        this.entityData.set(DATA_SPEED_LEVEL, this.speedLevel);
    }

    public double getLocomotiveSpeed() {
        double maxSpeed = Config.ELECTRIC_LOCOMOTIVE_MAX_SPEED.get(); // Default 1.75 b/t (5x steam 0.35)
        return switch (this.speedLevel) {
            case 1 -> Math.max(0.20D, maxSpeed * 0.20D); // Commuter (~0.35 b/t)
            case 2 -> Math.max(0.35D, maxSpeed * 0.40D); // Express (~0.70 b/t)
            case 3 -> Math.max(0.50D, maxSpeed * 0.60D); // Rapid (~1.05 b/t)
            case 4 -> maxSpeed * 0.80D;                  // Super-Express (~1.40 b/t)
            case 5 -> maxSpeed;                          // Bullet (~1.75 b/t)
            default -> Math.max(0.50D, maxSpeed * 0.60D);
        };
    }

    public static String getSpeedName(int speedLevel) {
        return switch (speedLevel) {
            case 1 -> "Commuter (0.35)";
            case 2 -> "Express (0.70)";
            case 3 -> "Rapid (1.05)";
            case 4 -> "Super-Express (1.40)";
            case 5 -> "Bullet (1.75)";
            default -> "Express";
        };
    }

    public String getTrainName() {
        return this.entityData.get(DATA_TRAIN_NAME);
    }

    public void setTrainName(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.length() > 32) {
            clean = clean.substring(0, 32);
        }
        this.entityData.set(DATA_TRAIN_NAME, clean);
    }

    public int getEnergyCostPerTick() {
        int baseCost = Config.ELECTRIC_LOCOMOTIVE_FE_PER_TICK.get();
        return Math.max(5, (this.speedLevel * baseCost) / 5);
    }

    @Override
    protected Item getDropItem() {
        return MiningTools.ELECTRIC_LOCOMOTIVE_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.ELECTRIC_LOCOMOTIVE_ITEM.get());
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
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
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ElectricLocomotiveMenu(containerId, inventory, this, this.dataAccess);
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
        super.tick();

        float targetYRot = this.getFacing().toYRot();
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
            // High speed electric effects
            if (this.isActive() && this.random.nextInt(6) == 0) {
                double rad = Math.toRadians(targetYRot + 90.0);
                double sparkX = this.getX() - Math.cos(rad) * 0.4;
                double sparkZ = this.getZ() - Math.sin(rad) * 0.4;
                double sparkY = this.getY() + 1.25; // near pantograph collector
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, sparkX, sparkY, sparkZ,
                        (this.random.nextDouble() - 0.5) * 0.05, 0.02, (this.random.nextDouble() - 0.5) * 0.05);
            }
            return;
        }

        // Server tick
        this.tickBatteryChargingItem();
        this.tickMovementAndPower();
        this.updateDynamicLight();

        // Push entities in path
        Direction pushFacing = this.getFacing();
        AABB pushBox = this.getBoundingBox().inflate(0.2D).expandTowards(pushFacing.getStepX() * 0.6D, 0.0D, pushFacing.getStepZ() * 0.6D);
        List<Entity> pushableEntities = this.level().getEntities(
                this,
                pushBox,
                e -> !e.noPhysics && !this.isChainedTo(e) && !this.hasPassenger(e)
        );
        for (Entity pushable : pushableEntities) {
            this.push(pushable);
        }
    }

    private void tickBatteryChargingItem() {
        ItemStack item = this.getItem(0);
        if (!item.isEmpty()) {
            var itemAccess = ItemAccess.forStack(item.copyWithCount(1));
            var itemHandler = itemAccess.getCapability(Capabilities.Energy.ITEM);
            if (itemHandler != null && this.battery.getAmountAsInt() < this.battery.getCapacityAsInt()) {
                int moved = EnergyHandlerUtil.move(itemHandler, this.battery, 2000, null);
                if (moved > 0) {
                    this.setItem(0, itemAccess.getResource().toStack());
                    this.setChanged();
                }
            }
        }
    }

    private void tickMovementAndPower() {
        BlockPos railPos = this.getCurrentBlockPosOrRailBelow();
        BlockState railState = this.level().getBlockState(railPos);
        boolean onRails = BaseRailBlock.isRail(railState);

        // Control rail interaction
        if (railState.is(MiningTools.CONTROL_RAIL.get())) {
            boolean powered = railState.getValue(PoweredRailBlock.POWERED);
            if (powered) {
                if (!this.running) {
                    this.startLocomotiveFromRail();
                }
            } else {
                if (this.running) {
                    this.stopLocomotiveFromRail();
                }
            }
        }

        // Check if on Electric Rail with power
        this.poweredFromRail = false;
        BlockEntity be = this.level().getBlockEntity(railPos);
        if (be instanceof ElectricRailBlockEntity electricRail) {
            int railEnergy = electricRail.getEnergyStored();
            if (railEnergy > 0) {
                this.poweredFromRail = true;
                // Recharging battery from rail while driving or standing
                int needed = this.battery.getCapacityAsInt() - this.battery.getAmountAsInt();
                if (needed > 0) {
                    EnergyHandlerUtil.move(electricRail.getInternalEnergyStorage(), this.battery, Math.min(needed, 2000), null);
                }
            }
        }

        if (this.entityData.get(DATA_POWERED_FROM_RAIL) != this.poweredFromRail) {
            this.entityData.set(DATA_POWERED_FROM_RAIL, this.poweredFromRail);
        }

        // Determine if train has power to run
        boolean hasPower = false;
        if (this.running) {
            int cost = this.getEnergyCostPerTick();
            if (this.poweredFromRail) {
                // Rail supplies the motive power!
                hasPower = true;
            } else if (this.battery.getAmountAsInt() >= cost) {
                // Coasting over unpowered rails/switches/curves using onboard battery!
                try (Transaction tx = Transaction.open(null)) {
                    this.battery.extract(cost, tx);
                    tx.commit();
                }
                hasPower = true;
            } else {
                // Out of power
                hasPower = false;
                this.setRunning(false);
            }
        }

        if (this.running && hasPower && onRails) {
            this.setActive(true);
            Direction facing = this.getFacing();
            double maxSpeed = this.getLocomotiveSpeed();
            double accel = Config.ELECTRIC_LOCOMOTIVE_ACCELERATION.get();

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

            this.move(MoverType.SELF, new Vec3(newVx, 0.0, newVz));

            if (exit0.getY() != 0 && Mth.floor(this.getX()) - railPos.getX() == exit0.getX() && Mth.floor(this.getZ()) - railPos.getZ() == exit0.getZ()) {
                this.setPos(this.getX(), this.getY() + exit0.getY(), this.getZ());
            } else if (exit1.getY() != 0 && Mth.floor(this.getX()) - railPos.getX() == exit1.getX() && Mth.floor(this.getZ()) - railPos.getZ() == exit1.getZ()) {
                this.setPos(this.getX(), this.getY() + exit1.getY(), this.getZ());
            }

            Vec3 newRailPos = this.getRailPos(this.getX(), this.getY(), this.getZ());
            if (newRailPos != null) {
                this.setPos(newRailPos.x, newRailPos.y, newRailPos.z);
            }

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

            double forwardProgress = actualDx * facing.getStepX() + actualDz * facing.getStepZ();
            boolean hitObstacle = (this.horizontalCollision && forwardProgress < 0.005D)
                    && BasicLocomotiveEntity.isPathBlockedByObstacle(this.level(), railPos, moveDir, this);
            if (hitObstacle) {
                this.setRunning(false);
                this.setActive(false);
                this.setDeltaMovement(Vec3.ZERO);
                this.level().playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.BLOCKS, 0.6F, 1.4F);
            } else {
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
            Vec3 currentDelta = super.getDeltaMovement();
            if (currentDelta.horizontalDistanceSqr() > 1.0E-4D) {
                Vec3 braked = currentDelta.multiply(0.70D, 1.0D, 0.70D);
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

    public void startLocomotiveFromRail() {
        if (this.poweredFromRail || this.battery.getAmountAsInt() >= this.getEnergyCostPerTick()) {
            this.setRunning(true);
            this.setActive(true);
            this.level().playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0F, 1.5F);
        }
    }

    public void stopLocomotiveFromRail() {
        if (this.running) {
            this.setRunning(false);
            this.setActive(false);
            this.level().playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.8F, 0.8F);
        }
    }

    public void handleHotkeyAction(Player player, int action) {
        switch (action) {
            case LocomotiveControlPayload.ACTION_START -> {
                if (!this.running) {
                    if (this.poweredFromRail || this.battery.getAmountAsInt() >= this.getEnergyCostPerTick()) {
                        this.setRunning(true);
                        this.setActive(true);
                        this.level().playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0F, 1.5F);
                        sendNotification(player, Component.literal("Electric Bullet Train started").withStyle(net.minecraft.ChatFormatting.AQUA));
                    } else {
                        this.level().playSound(null, this.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8F, 1.2F);
                        sendNotification(player, Component.literal("No electrical power! Need powered electric rail or charged battery").withStyle(net.minecraft.ChatFormatting.RED));
                    }
                }
            }
            case LocomotiveControlPayload.ACTION_STOP -> {
                if (this.running) {
                    this.setRunning(false);
                    this.setActive(false);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.8F, 0.8F);
                    sendNotification(player, Component.literal("Electric Bullet Train stopped").withStyle(net.minecraft.ChatFormatting.GOLD));
                }
            }
            case LocomotiveControlPayload.ACTION_SPEED_DOWN -> {
                if (this.speedLevel > MIN_SPEED_LEVEL) {
                    this.setSpeedLevel(this.speedLevel - 1);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 0.9F);
                    sendNotification(player, Component.literal("Speed: " + getSpeedName(this.speedLevel)).withStyle(net.minecraft.ChatFormatting.BLUE));
                }
            }
            case LocomotiveControlPayload.ACTION_SPEED_UP -> {
                if (this.speedLevel < MAX_SPEED_LEVEL) {
                    this.setSpeedLevel(this.speedLevel + 1);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.1F);
                    sendNotification(player, Component.literal("Speed: " + getSpeedName(this.speedLevel)).withStyle(net.minecraft.ChatFormatting.BLUE));
                }
            }
            case LocomotiveControlPayload.ACTION_REVERSE -> {
                Direction newFacing = this.getFacing().getOpposite();
                this.setFacing(newFacing);
                this.level().playSound(null, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.8F, 1.2F);
                sendNotification(player, Component.literal("Direction: " + newFacing.getName().toUpperCase()).withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
            case LocomotiveControlPayload.ACTION_HORN -> {
                this.blowHorn(player);
            }
            case LocomotiveControlPayload.ACTION_TOGGLE_LIGHT -> {
                boolean newState = !this.isHeadlightEnabled();
                this.setHeadlightEnabled(newState);
                this.level().playSound(null, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.8F, newState ? 1.4F : 0.8F);
                sendNotification(player, Component.literal(newState ? "Headlights: ON" : "Headlights: OFF").withStyle(newState ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.GRAY));
            }
        }
    }

    public void triggerHornRail(BlockPos railPos, BlockState railState) {
        if (this.level().isClientSide()) {
            return;
        }
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
        if (!this.level().isClientSide()) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.RECORDS, 2.5F, 1.4F);
            this.level().playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.RECORDS, 2.0F, 1.2F);
        }
    }

    private void sendNotification(Player player, Component message) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, true);
        } else {
            player.sendSystemMessage(message);
        }
    }

    private void updateDynamicLight() {
        if (!this.isHeadlightEnabled()) {
            this.removeDynamicLight();
            return;
        }

        Direction facing = this.getFacing();
        BlockPos headPos = this.blockPosition().relative(facing, 2).above();
        if (headPos.equals(this.currentLightPos)) {
            return;
        }

        this.removeDynamicLight();
        BlockState targetState = this.level().getBlockState(headPos);
        if (targetState.isAir()) {
            this.level().setBlock(headPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
            this.currentLightPos = headPos;
        }
    }

    private void removeDynamicLight() {
        if (this.currentLightPos != null) {
            BlockState state = this.level().getBlockState(this.currentLightPos);
            if (state.is(Blocks.LIGHT)) {
                this.level().setBlock(this.currentLightPos, Blocks.AIR.defaultBlockState(), 3);
            }
            this.currentLightPos = null;
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide()) {
            this.removeDynamicLight();
        }
        super.remove(reason);
    }

    // Train chaining implementation
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

    public static @Nullable ElectricLocomotiveEntity getConnectedLocomotive(Entity entity) {
        if (entity instanceof ElectricLocomotiveEntity loco && !loco.isRemoved()) {
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
                if (ent instanceof ElectricLocomotiveEntity loco && !loco.isRemoved()) {
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
        if (links.isEmpty()) return false;

        boolean moving = this.isActive() || super.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;
        double maxStep = moving ? Math.max(this.getLocomotiveSpeed() * 1.6D, 0.35D) : 0.15D;

        for (Pair<Entity, AbstractMinecart> link : links) {
            Entity prev = link.getFirst();
            AbstractMinecart c = link.getSecond();

            double targetDist = TrainChainingUtil.getTargetCouplingDistance(prev, c, false);
            double minAllowed = moving ? (targetDist - 0.08D) : (targetDist - 0.12D);
            double maxAllowed = moving ? (targetDist + 0.08D) : (targetDist + 0.12D);

            Vec3 initialPos = initialCartPositions.get(c.getUUID());
            if (initialPos == null) {
                initialPos = new Vec3(c.getX(), c.getY(), c.getZ());
            }

            double pX = prev.getX();
            double pY = prev.getY();
            double pZ = prev.getZ();

            double cX = initialPos.x;
            double cY = initialPos.y;
            double cZ = initialPos.z;

            double dx = pX - cX;
            double dy = pY - cY;
            double dz = pZ - cZ;
            double currentDist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (currentDist > 1.0E-4D && (currentDist > maxAllowed || currentDist < minAllowed)) {
                double diff = currentDist - targetDist;
                double pullDist = Mth.clamp(diff, -maxStep, maxStep);
                double moveRatio = pullDist / currentDist;
                this.moveCartAlongRails(c, dx * moveRatio, dz * moveRatio, Math.abs(pullDist));
            } else {
                c.setDeltaMovement(Vec3.ZERO);
            }
        }
        return true;
    }

    private boolean moveCartAlongRails(AbstractMinecart cart, double dirX, double dirZ, double distance3D) {
        if (distance3D < 1.0E-5D) return true;

        BlockPos currentRail = cart.getCurrentBlockPosOrRailBelow();
        BlockState state = cart.level().getBlockState(currentRail);
        if (!BaseRailBlock.isRail(state)) {
            cart.setPos(cart.getX() + dirX, cart.getY(), cart.getZ() + dirZ);
            return false;
        }

        double initialX = cart.getX();
        double initialZ = cart.getZ();

        double nextX = cart.getX() + dirX;
        double nextZ = cart.getZ() + dirZ;
        double nextY = cart.getY();

        cart.setPos(nextX, nextY, nextZ);
        Vec3 newRailPos = this.getRailPos(cart.getX(), cart.getY(), cart.getZ());
        if (newRailPos != null) {
            cart.setPos(newRailPos.x, newRailPos.y, newRailPos.z);
        }

        double actualDx = cart.getX() - initialX;
        double actualDz = cart.getZ() - initialZ;
        if (actualDx * actualDx + actualDz * actualDz > 1.0E-5D) {
            float yaw = (float) (Mth.atan2(-actualDz, -actualDx) * 180.0 / Math.PI);
            cart.setYRot(yaw % 360.0F);
        }
        return true;
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

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        this.battery.serialize(output);
        output.putBoolean("Running", this.running);
        output.putBoolean("Headlight", this.isHeadlightEnabled());
        output.putInt("SpeedLevel", this.speedLevel);
        output.putInt("Facing", this.getFacing().get2DDataValue());
        output.putString("TrainName", this.getTrainName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.battery.deserialize(input);
        this.running = input.getBooleanOr("Running", false);
        this.setHeadlightEnabled(input.getBooleanOr("Headlight", false));
        this.speedLevel = input.getIntOr("SpeedLevel", DEFAULT_SPEED_LEVEL);
        this.setFacing(Direction.from2DDataValue(input.getIntOr("Facing", Direction.NORTH.get2DDataValue())));
        this.setTrainName(input.getStringOr("TrainName", ""));
        this.syncEnergyData();
    }
}
