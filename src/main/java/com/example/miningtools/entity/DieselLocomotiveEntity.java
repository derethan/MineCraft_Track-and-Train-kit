package com.example.miningtools.entity;

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
import com.example.miningtools.menu.DieselLocomotiveMenu;
import com.example.miningtools.network.LocomotiveControlPayload;
import com.example.miningtools.util.TrainChainingUtil;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

public class DieselLocomotiveEntity extends AbstractMinecartContainer {
    public static final int INVENTORY_SIZE = 2; // Slot 0: fuel container input, Slot 1: empty container return
    public static final int DEFAULT_SPEED_LEVEL = 3;

    private static final EntityDataAccessor<Direction> DATA_FACING =
            SynchedEntityData.defineId(DieselLocomotiveEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE =
            SynchedEntityData.defineId(DieselLocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RUNNING =
            SynchedEntityData.defineId(DieselLocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SPEED_LEVEL =
            SynchedEntityData.defineId(DieselLocomotiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HEADLIGHT =
            SynchedEntityData.defineId(DieselLocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_TRAIN_NAME =
            SynchedEntityData.defineId(DieselLocomotiveEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_REVERSED =
            SynchedEntityData.defineId(DieselLocomotiveEntity.class, EntityDataSerializers.BOOLEAN);

    // Synced fluid tank fields
    private static final EntityDataAccessor<String> DATA_FLUID_NAME =
            SynchedEntityData.defineId(DieselLocomotiveEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_FLUID_AMOUNT =
            SynchedEntityData.defineId(DieselLocomotiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FLUID_CAPACITY =
            SynchedEntityData.defineId(DieselLocomotiveEntity.class, EntityDataSerializers.INT);

    private boolean running = false;
    private int speedLevel = DEFAULT_SPEED_LEVEL;
    private @Nullable BlockPos currentLightPos = null;
    private int hornRailCooldown = 0;
    private @Nullable BlockPos lastHornRailPos = null;

    protected final FluidStacksResourceHandler tank;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> DieselLocomotiveEntity.this.getFluidAmount() & 0xFFFF;
                case 1 -> (DieselLocomotiveEntity.this.getFluidAmount() >> 16) & 0xFFFF;
                case 2 -> DieselLocomotiveEntity.this.running ? 1 : 0;
                case 3 -> DieselLocomotiveEntity.this.getFacing().get2DDataValue();
                case 4 -> DieselLocomotiveEntity.this.speedLevel;
                case 5 -> DieselLocomotiveEntity.this.isHeadlightEnabled() ? 1 : 0;
                case 6 -> DieselLocomotiveEntity.this.getId();
                case 7 -> DieselLocomotiveEntity.this.isReversed() ? 1 : 0;
                case 8 -> DieselLocomotiveEntity.this.getCapacity() & 0xFFFF;
                case 9 -> (DieselLocomotiveEntity.this.getCapacity() >> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 2 -> DieselLocomotiveEntity.this.setRunning(value != 0);
                case 3 -> DieselLocomotiveEntity.this.setFacing(Direction.from2DDataValue(value));
                case 4 -> DieselLocomotiveEntity.this.setSpeedLevel(value);
                case 5 -> DieselLocomotiveEntity.this.setHeadlightEnabled(value != 0);
                case 7 -> DieselLocomotiveEntity.this.setReversed(value != 0);
            }
        }

        @Override
        public int getCount() {
            return 10;
        }
    };

    public DieselLocomotiveEntity(EntityType<? extends DieselLocomotiveEntity> type, Level level) {
        super(type, level);
        this.tank = new FluidStacksResourceHandler(1, Config.DIESEL_LOCOMOTIVE_TANK_CAPACITY.get()) {
            @Override
            public boolean isValid(int slot, FluidResource resource) {
                return isDieselFuel(resource.toStack(1));
            }

            @Override
            protected void onContentsChanged(int index, FluidStack previousStack) {
                super.onContentsChanged(index, previousStack);
                DieselLocomotiveEntity.this.syncFluidData();
            }
        };
    }

    public DieselLocomotiveEntity(Level level, double x, double y, double z) {
        super(MiningTools.DIESEL_LOCOMOTIVE_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.tank = new FluidStacksResourceHandler(1, Config.DIESEL_LOCOMOTIVE_TANK_CAPACITY.get()) {
            @Override
            public boolean isValid(int slot, FluidResource resource) {
                return isDieselFuel(resource.toStack(1));
            }

            @Override
            protected void onContentsChanged(int index, FluidStack previousStack) {
                super.onContentsChanged(index, previousStack);
                DieselLocomotiveEntity.this.syncFluidData();
            }
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FACING, Direction.NORTH);
        builder.define(DATA_ACTIVE, false);
        builder.define(DATA_RUNNING, false);
        builder.define(DATA_SPEED_LEVEL, DEFAULT_SPEED_LEVEL);
        builder.define(DATA_HEADLIGHT, true);
        builder.define(DATA_TRAIN_NAME, "");
        builder.define(DATA_REVERSED, false);
        builder.define(DATA_FLUID_NAME, "");
        builder.define(DATA_FLUID_AMOUNT, 0);
        builder.define(DATA_FLUID_CAPACITY, Config.DIESEL_LOCOMOTIVE_TANK_CAPACITY.get());
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.tank;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeItem(Container target, int index, ItemStack stack) {
        return false;
    }

    public FluidStacksResourceHandler getTank() {
        return this.tank;
    }

    public int getCapacity() {
        return this.level().isClientSide()
                ? this.entityData.get(DATA_FLUID_CAPACITY)
                : Config.DIESEL_LOCOMOTIVE_TANK_CAPACITY.get();
    }

    public FluidStack getStoredFluid() {
        return FluidUtil.getStack(this.tank, 0);
    }

    public String getFluidName() {
        return this.entityData.get(DATA_FLUID_NAME);
    }

    public int getFluidAmount() {
        return this.entityData.get(DATA_FLUID_AMOUNT);
    }

    public float getFluidFillRatio() {
        int cap = getCapacity();
        return cap > 0 ? (float) getFluidAmount() / (float) cap : 0.0F;
    }

    public void syncFluidData() {
        if (!this.level().isClientSide()) {
            FluidStack stack = FluidUtil.getStack(this.tank, 0);
            if (stack.isEmpty()) {
                this.entityData.set(DATA_FLUID_NAME, "");
                this.entityData.set(DATA_FLUID_AMOUNT, 0);
            } else {
                Identifier id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
                this.entityData.set(DATA_FLUID_NAME, id != null ? id.toString() : "");
                this.entityData.set(DATA_FLUID_AMOUNT, stack.getAmount());
            }
            this.entityData.set(DATA_FLUID_CAPACITY, this.getCapacity());
        }
    }

    public static boolean isDieselFuel(FluidStack stack) {
        if (stack.isEmpty()) return false;
        Fluid fluid = stack.getFluid();
        if (fluid == Fluids.LAVA) return true; // Emergency fallback

        Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
        if (id != null) {
            String path = id.getPath().toLowerCase();
            String namespace = id.getNamespace().toLowerCase();
            if (path.contains("diesel") || path.contains("fuel") || path.contains("oil")
                    || path.contains("bio") || path.contains("petrol") || path.contains("gasoline")
                    || path.contains("kerosene") || path.contains("methanol") || path.contains("ethanol")
                    || namespace.contains("refinery") || namespace.contains("car")) {
                return true;
            }
        }
        try {
            return fluid.builtInRegistryHolder().tags().anyMatch(tag -> {
                String tagPath = tag.location().getPath().toLowerCase();
                return tagPath.contains("diesel") || tagPath.contains("fuel") || tagPath.contains("bio") || tagPath.contains("oil");
            });
        } catch (Throwable ignored) {}
        return false;
    }

    public boolean hasFuel() {
        return this.getFluidAmount() > 0;
    }

        @Override
    public BlockPos getCurrentBlockPosOrRailBelow() {
        return TrainChainingUtil.getCurrentBlockPosOrRailBelow(this);
    }

    public Direction getFacing() {
        return this.entityData.get(DATA_FACING);
    }

    public void setFacing(Direction facing) {
        if (facing.getAxis().isHorizontal()) {
            this.entityData.set(DATA_FACING, facing);
        }
    }

    public boolean isReversed() {
        return this.entityData.get(DATA_REVERSED);
    }

    public void setReversed(boolean reversed) {
        this.entityData.set(DATA_REVERSED, reversed);
    }

    public Direction getTravelDirection() {
        return this.isReversed() ? this.getFacing().getOpposite() : this.getFacing();
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

    public int getSpeedLevel() {
        return this.entityData.get(DATA_SPEED_LEVEL);
    }

    public void setSpeedLevel(int level) {
        int clamped = Mth.clamp(level, 1, 5);
        this.speedLevel = clamped;
        this.entityData.set(DATA_SPEED_LEVEL, clamped);
    }

    public static String getSpeedName(int level) {
        return switch (level) {
            case 1 -> "1 (Crawl 1 m/s)";
            case 2 -> "2 (Slow 2.25 m/s)";
            case 3 -> "3 (Cruise)";
            case 4 -> "4 (Express)";
            case 5 -> "5 (High-Ball)";
            default -> String.valueOf(level);
        };
    }

    public double getLocomotiveSpeed() {
        double maxSpeed = Config.DIESEL_LOCOMOTIVE_MAX_SPEED.get();
        return switch (this.speedLevel) {
            case 1 -> 0.05D;   // Crawl (1.0 m/s = 0.05 b/t)
            case 2 -> 0.1125D; // Slow (2.25 m/s = 0.1125 b/t)
            case 3 -> maxSpeed * 0.50D;
            case 4 -> maxSpeed * 0.80D;
            case 5 -> maxSpeed;
            default -> maxSpeed * 0.50D;
        };
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
        return this.isHeadlightEnabled() && (this.hasFuel() || this.running || !this.getPassengers().isEmpty());
    }

    public String getTrainName() {
        return this.entityData.get(DATA_TRAIN_NAME);
    }

    public void setTrainName(String name) {
        this.entityData.set(DATA_TRAIN_NAME, name.trim());
    }

    @Override
    public Component getDisplayName() {
        String custom = this.getTrainName();
        if (!custom.isBlank()) {
            return Component.literal(custom);
        }
        return super.getDisplayName();
    }

    public static @Nullable DieselLocomotiveEntity getConnectedLocomotive(Entity entity) {
        if (entity instanceof DieselLocomotiveEntity loco) {
            return loco;
        }
        if (!(entity instanceof AbstractMinecart cart)) {
            return null;
        }
        UUID parentUUID = TrainChainingUtil.getCartParentUUID(cart);
        Set<UUID> visited = new HashSet<>();
        visited.add(cart.getUUID());
        while (parentUUID != null && visited.add(parentUUID)) {
            Entity p = entity.level().getEntity(parentUUID);
            if (p instanceof DieselLocomotiveEntity loco) {
                return loco;
            }
            if (p instanceof AbstractMinecart pc) {
                parentUUID = TrainChainingUtil.getCartParentUUID(pc);
            } else {
                break;
            }
        }
        return null;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.getItemInHand(hand).is(MiningTools.CROWBAR_ITEM.get())) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        // Fluid container interaction (buckets, cans)
        if (!held.isEmpty()) {
            boolean success = FluidUtil.interactWithFluidHandler(player, hand, this.blockPosition(), this.tank, null);
            if (success) {
                this.syncFluidData();
                return InteractionResult.SUCCESS;
            }
        }

        if (player.isShiftKeyDown()) {
            if (!this.level().isClientSide()) {
                player.openMenu(this);
            }
            return InteractionResult.SUCCESS;
        }

        if (held.isEmpty()) {
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
                    double dot = diff.x * facing.getStepX() + diff.z * facing.getStepZ();
                    Vec3 forwardDir = (dot >= -0.2D ? diff : diff.reverse()).normalize();
                    float pitch = (float) (Math.atan(forwardDir.y) * 73.0);
                    this.setXRot(pitch);
                    this.xRotO = pitch;
                }
            }
        }
        targetYRot = Mth.wrapDegrees(targetYRot);
        float rotDiff = Mth.wrapDegrees(targetYRot - this.yRotO);
        this.yRotO = targetYRot - rotDiff;
        this.setYRot(targetYRot);

        if (this.level().isClientSide()) {
            if (this.running && this.random.nextInt(3) == 0) {
                // Diesel exhaust smoke puff from roof stack
                double rad = Math.toRadians(facing.toYRot() + 90.0);
                double stackX = this.getX() + Math.cos(rad) * 0.25;
                double stackZ = this.getZ() + Math.sin(rad) * 0.25;
                double stackY = this.getY() + 1.25;
                this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, stackX, stackY, stackZ, 0.0, 0.08, 0.0);
            }
            return;
        }

        double preTickX = this.getX();
        double preTickY = this.getY();
        double preTickZ = this.getZ();

        // Server-side tank drainage from input inventory slot 0
        this.drainFuelItemSlot();
        this.siphonFuelFromTenderCarts();

        this.tickFuelAndMovement();
        this.updateDynamicLight();

        // Push entities in path from connection points (bumpers)
        Direction travelDir = this.getTravelDirection();
        double speed = this.isActive() ? this.getLocomotiveSpeed() : this.getDeltaMovement().horizontalDistance();
        TrainChainingUtil.pushEntitiesInLocomotivePath(this, travelDir, speed, preTickX, preTickY, preTickZ);
    }

    /**
     * Siphons compatible diesel fuel from any directly coupled Liquid Minecart (tender)
     * connected directly behind or ahead of the locomotive.
     */
    private void siphonFuelFromTenderCarts() {
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int space = this.getCapacity() - this.getFluidAmount();
        if (space <= 0) {
            return;
        }

        List<Entity> tenders = new ArrayList<>();
        UUID parentUUID = TrainChainingUtil.getCartParentUUID(this);
        if (parentUUID != null) {
            Entity p = serverLevel.getEntity(parentUUID);
            if (p != null) tenders.add(p);
        }
        UUID childUUID = TrainChainingUtil.getCartChildUUID(this);
        if (childUUID != null) {
            Entity c = serverLevel.getEntity(childUUID);
            if (c != null) tenders.add(c);
        }

        if (tenders.isEmpty()) {
            List<AbstractMinecart> nearby = this.level().getEntitiesOfClass(
                    AbstractMinecart.class,
                    this.getBoundingBox().inflate(2.6D),
                    e -> e != this && (e instanceof LiquidMinecartEntity || e instanceof LargeLiquidMinecartEntity)
            );
            tenders.addAll(nearby);
        }

        int maxTransfer = Math.min(space, 100);
        for (Entity tender : tenders) {
            ResourceHandler<FluidResource> sourceHandler = null;
            if (tender instanceof LiquidMinecartEntity lme) {
                sourceHandler = lme.getFluidHandler();
            } else if (tender instanceof LargeLiquidMinecartEntity llme) {
                sourceHandler = llme.getFluidHandler();
            } else {
                sourceHandler = tender.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.ENTITY, null);
            }

            if (sourceHandler != null) {
                try {
                    int moved = ResourceHandlerUtil.move(
                            sourceHandler,
                            this.tank,
                            r -> isDieselFuel(r.toStack(1)),
                            maxTransfer,
                            null
                    );
                    if (moved > 0) {
                        this.syncFluidData();
                        if (tender instanceof LiquidMinecartEntity lme) {
                            lme.syncFluidData();
                        }
                        space -= moved;
                        if (space <= 0) break;
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    private void drainFuelItemSlot() {
        ItemStack input = this.getItem(0);
        if (input.isEmpty()) return;

        FluidStack contained = FluidUtil.getFirstStackContained(input);
        if ((contained == null || contained.isEmpty()) && input.getItem() instanceof net.minecraft.world.item.BucketItem bucket) {
            Fluid bucketFluid = bucket.getContent();
            if (bucketFluid != Fluids.EMPTY) {
                contained = new FluidStack(bucketFluid, 1000);
            }
        }
        if (contained != null && !contained.isEmpty() && isDieselFuel(contained) && this.getFluidAmount() < this.getCapacity()) {
            int toInsert = Math.min(contained.getAmount(), this.getCapacity() - this.getFluidAmount());
            try (net.neoforged.neoforge.transfer.transaction.Transaction tx = net.neoforged.neoforge.transfer.transaction.Transaction.open(null)) {
                int inserted = this.tank.insert(FluidResource.of(contained), toInsert, tx);
                if (inserted > 0) {
                    tx.commit();
                    this.syncFluidData();
                    net.minecraft.world.item.ItemStackTemplate remTemplate = input.getItem().getCraftingRemainder();
                    ItemStack remainder = remTemplate != null ? remTemplate.create() : (input.getItem() instanceof net.minecraft.world.item.BucketItem ? new ItemStack(Items.BUCKET) : ItemStack.EMPTY);
                    input.shrink(1);
                    if (!remainder.isEmpty()) {
                        ItemStack out = this.getItem(1);
                        if (out.isEmpty()) {
                            this.setItem(1, remainder);
                        } else if (ItemStack.isSameItemSameComponents(out, remainder) && out.getCount() < out.getMaxStackSize()) {
                            out.grow(1);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    private void tickFuelAndMovement() {
        BlockPos railPos = this.getCurrentBlockPosOrRailBelow();
        BlockState railState = this.level().getBlockState(railPos);
        boolean onRails = BaseRailBlock.isRail(railState);

        if (railState.is(MiningTools.CONTROL_RAIL.get()) || railState.is(MiningTools.BRAKING_RAIL.get())) {
            boolean powered = railState.getValue(PoweredRailBlock.POWERED);
            if (powered) {
                if (!this.running && !TrainChainingUtil.isConsistHeldByBrakingRail(this)) {
                    this.startLocomotiveFromRail();
                }
            } else {
                if (this.running) {
                    this.stopLocomotiveFromRail();
                }
            }
        } else if (this.running && TrainChainingUtil.isConsistHeldByBrakingRail(this)) {
            this.stopLocomotiveFromRail();
        }

        if (this.running) {
            if (this.hasFuel()) {
                // Consume diesel fuel per tick
                int cost = Math.max(1, Config.DIESEL_LOCOMOTIVE_FUEL_PER_TICK.get());
                try (net.neoforged.neoforge.transfer.transaction.Transaction tx = net.neoforged.neoforge.transfer.transaction.Transaction.open(null)) {
                    var extracted = net.neoforged.neoforge.transfer.ResourceHandlerUtil.extractFirst(this.tank, r -> true, cost, tx);
                    if (extracted != null && !extracted.isEmpty()) {
                        tx.commit();
                    }
                } catch (Throwable ignored) {}
                this.syncFluidData();

                if (!this.hasFuel()) {
                    this.setRunning(false);
                    this.setActive(false);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.2F);
                }
            } else {
                this.setRunning(false);
                this.setActive(false);
            }
        }

        boolean activeFuel = this.hasFuel();

        if (this.running && activeFuel && onRails) {
            this.setActive(true);
            if (TrainChainingUtil.hasActiveLeadLocomotiveAhead(this)) {
                return;
            }

            TrainChainingUtil.TrainConsistStats stats = TrainChainingUtil.calculateConsistStats(this);
            double baseSpeed = this.getLocomotiveSpeed();
            double maxSpeed = baseSpeed * stats.speedFactor;
            double accel = Config.DIESEL_LOCOMOTIVE_ACCELERATION.get();

            if (stats.speedFactor <= 0.0D) {
                // Overloaded stall!
                if (this.random.nextInt(20) == 0) {
                    this.level().playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.BLOCKS, 0.7F, 0.8F);
                    sendNotification(null, Component.literal("Train overloaded! (" + stats.totalLoad + " Carts / " + stats.totalCapacity + " Capacity) - Add more locomotives!").withStyle(net.minecraft.ChatFormatting.RED));
                }
                maxSpeed = 0.0D;
            }

            Direction travelDir = this.getTravelDirection();
            Vec3 currentDelta = super.getDeltaMovement();
            double currentSpeed = currentDelta.length();
            double nextSpeed = currentSpeed + (maxSpeed - currentSpeed) * accel;
            double newVx = travelDir.getStepX() * nextSpeed;
            double newVz = travelDir.getStepZ() * nextSpeed;
            if (currentSpeed < 1.0E-4D) {
                this.setDeltaMovement(newVx, currentDelta.y, newVz);
            }

            RailShape shape = ((BaseRailBlock) railState.getBlock()).getRailDirection(railState, this.level(), railPos, this);

            double initialX = this.getX();
            double initialY = this.getY();
            double initialZ = this.getZ();

            List<Pair<Entity, AbstractMinecart>> trainLinks = this.getOrderedTrainLinks();
            Map<UUID, Vec3> initialCartPositions = new HashMap<>();
            for (Pair<Entity, AbstractMinecart> link : trainLinks) {
                AbstractMinecart c = link.getSecond();
                initialCartPositions.put(c.getUUID(), new Vec3(c.getX(), c.getY(), c.getZ()));
            }

            Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
            Vec3i exit0 = exits.getFirst();
            Vec3i exit1 = exits.getSecond();

            int entryX = -travelDir.getStepX();
            int entryZ = -travelDir.getStepZ();
            boolean entryMatches = (exit0.getX() == entryX && exit0.getZ() == entryZ) || (exit1.getX() == entryX && exit1.getZ() == entryZ);
            boolean exitMatches = (exit0.getX() == travelDir.getStepX() && exit0.getZ() == travelDir.getStepZ()) || (exit1.getX() == travelDir.getStepX() && exit1.getZ() == travelDir.getStepZ());
            if (!shape.isSlope() && !entryMatches && !exitMatches) {
                this.setPos(initialX, initialY, initialZ);
                this.stopLocomotiveFromRail();
                return;
            }

            double x = initialX;
            double y = initialY;
            double z = initialZ;

            Vec3 railSnap = this.getRailPos(x, y, z);
            if (railSnap != null) {
                x = railSnap.x;
                y = railSnap.y;
                z = railSnap.z;
            }

            boolean isCurve = !shape.isSlope() && shape != RailShape.NORTH_SOUTH && shape != RailShape.EAST_WEST;
            boolean pathBlocked = BasicLocomotiveEntity.isPathBlockedByObstacle(this.level(), railPos, travelDir, this);

            if (isCurve) {
                double circleCenterX = (double) railPos.getX() + 0.5D + (double) (exit0.getX() + exit1.getX()) * 0.5D;
                double circleCenterZ = (double) railPos.getZ() + 0.5D + (double) (exit0.getZ() + exit1.getZ()) * 0.5D;
                double vx = x - circleCenterX;
                double vz = z - circleCenterZ;
                double dist = Math.sqrt(vx * vx + vz * vz);
                double ux = dist > 1.0E-5D ? vx / dist : 0.70710678D * -(exit0.getX() + exit1.getX());
                double uz = dist > 1.0E-5D ? vz / dist : 0.70710678D * -(exit0.getZ() + exit1.getZ());
                double tX = -uz;
                double tZ = ux;
                boolean movingExit0ToExit1 = (exit0.getX() == -travelDir.getStepX() && exit0.getZ() == -travelDir.getStepZ())
                        || (exit1.getX() == travelDir.getStepX() && exit1.getZ() == travelDir.getStepZ());
                double exitVecX = (exit1.getX() - exit0.getX()) * (movingExit0ToExit1 ? 1.0D : -1.0D);
                double exitVecZ = (exit1.getZ() - exit0.getZ()) * (movingExit0ToExit1 ? 1.0D : -1.0D);
                if (tX * exitVecX + tZ * exitVecZ < 0.0D) {
                    tX = -tX;
                    tZ = -tZ;
                }
                double pow = Math.sqrt(newVx * newVx + newVz * newVz);
                newVx = pow * tX;
                newVz = pow * tZ;
            } else {
                double xD = (double) (exit1.getX() - exit0.getX());
                double zD = (double) (exit1.getZ() - exit0.getZ());
                double length = Math.sqrt(xD * xD + zD * zD);
                if (length > 0.0D) {
                    double flip = newVx * xD + newVz * zD;
                    if (flip < 0.0D) {
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
            }

            // On slopes, set Y to base rail elevation + 1.0 (matching vanilla OldMinecartBehavior) before horizontal move()
            double moveY = (double) railPos.getY();
            if (shape.isSlope()) {
                moveY += 1.0D;
            }
            this.setPos(x, moveY, z);
            this.setDeltaMovement(newVx, 0.0, newVz);
            this.move(MoverType.SELF, new Vec3(newVx, 0.0, newVz));

            // Vanilla slope step-up / step-down check
            if (exit0.getY() != 0 && Mth.floor(this.getX()) - railPos.getX() == exit0.getX() && Mth.floor(this.getZ()) - railPos.getZ() == exit0.getZ()) {
                this.setPos(this.getX(), this.getY() + (double) exit0.getY(), this.getZ());
            } else if (exit1.getY() != 0 && Mth.floor(this.getX()) - railPos.getX() == exit1.getX() && Mth.floor(this.getZ()) - railPos.getZ() == exit1.getZ()) {
                this.setPos(this.getX(), this.getY() + (double) exit1.getY(), this.getZ());
            }

            // Snap Y precisely to the rail surface, and center lateral axis on straight rails
            Vec3 newRailPos = this.getRailPos(this.getX(), this.getY(), this.getZ());
            if (newRailPos != null) {
                if (isCurve) {
                    this.setPos(newRailPos.x, newRailPos.y, newRailPos.z);
                } else {
                    double finalX = (shape == RailShape.NORTH_SOUTH || shape == RailShape.ASCENDING_NORTH || shape == RailShape.ASCENDING_SOUTH) ? newRailPos.x : this.getX();
                    double finalZ = (shape == RailShape.EAST_WEST || shape == RailShape.ASCENDING_EAST || shape == RailShape.ASCENDING_WEST) ? newRailPos.z : this.getZ();
                    this.setPos(finalX, newRailPos.y, finalZ);
                }
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

            boolean hitObstacle = pathBlocked;
            if (hitObstacle) {
                this.setRunning(false);
                this.setActive(false);
                this.setDeltaMovement(Vec3.ZERO);
                this.level().playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.BLOCKS, 0.8F, 1.0F);
            } else {
                Direction currentMoveDir = Direction.getApproximateNearest(actualDx, 0.0, actualDz);
                Direction targetFacing = this.isReversed() ? currentMoveDir.getOpposite() : currentMoveDir;
                if (targetFacing.getAxis().isHorizontal()
                        && actualMovement.horizontalDistanceSqr() > 1.0E-4D
                        && targetFacing.getAxis() != this.getFacing().getAxis()) {
                    this.setFacing(targetFacing);
                }
            }

            BlockPos currentRail = this.getCurrentBlockPosOrRailBelow();
            BlockState currentRailState = this.level().getBlockState(currentRail);
            if ((currentRailState.is(MiningTools.CONTROL_RAIL.get()) || currentRailState.is(MiningTools.BRAKING_RAIL.get()))
                    && !currentRailState.getValue(PoweredRailBlock.POWERED)) {
                this.stopLocomotiveFromRail();
            }
            if (currentRailState.is(MiningTools.HORN_RAIL.get())) {
                this.triggerHornRail(currentRail, currentRailState);
            }

            TrainChainingUtil.handleChainedCartsMovement(this, this.getFacing(), trainLinks, initialCartPositions, initialX, initialY, initialZ);
        } else {
            this.setActive(false);
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
                TrainChainingUtil.handleChainedCartsMovement(this, this.getFacing(), this.getOrderedTrainLinks(), new HashMap<>(), this.getX(), this.getY(), this.getZ());
            }
        }
    }

    public void triggerHornRail(BlockPos railPos, BlockState state) {
        if (!railPos.equals(this.lastHornRailPos)) {
            this.blowHorn(null);
            this.lastHornRailPos = railPos;
            this.hornRailCooldown = 20;
        }
    }

    public void blowHorn(@Nullable Player player) {
        // Deep authentic Nathan K5HL/K3LA diesel air horn (goat horn at low pitch 0.65F)
        this.level().playSound(null, this.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.RECORDS, 2.5F, 0.65F);
        if (player != null) {
            sendNotification(player, Component.literal("HOOOOONK!").withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }

    public void startLocomotiveFromRail() {
        if (this.hasFuel()) {
            this.setRunning(true);
            this.setActive(true);
            this.level().playSound(null, this.blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 0.8F);
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

    public void handleHotkeyAction(Player player, int action) {
        switch (action) {
            case LocomotiveControlPayload.ACTION_START -> {
                if (this.hasFuel()) {
                    this.setRunning(true);
                    this.setActive(true);
                    TrainChainingUtil.syncTrainConsist(this, true, this.speedLevel, this.isReversed());
                    this.level().playSound(null, this.blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.PLAYERS, 1.0F, 0.8F);
                    sendNotification(player, Component.literal("CN Diesel Locomotive: Started"));
                } else {
                    sendNotification(player, Component.literal("No Diesel Fuel! Fill tank with diesel/fuel").withStyle(net.minecraft.ChatFormatting.RED));
                }
            }
            case LocomotiveControlPayload.ACTION_STOP -> {
                this.setRunning(false);
                this.setActive(false);
                this.setDeltaMovement(Vec3.ZERO);
                TrainChainingUtil.syncTrainConsist(this, false, this.speedLevel, this.isReversed());
                this.level().playSound(null, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.2F);
                sendNotification(player, Component.literal("CN Diesel Locomotive: Stopped"));
            }
            case LocomotiveControlPayload.ACTION_SPEED_DOWN -> {
                if (this.speedLevel > 1) {
                    this.setSpeedLevel(this.speedLevel - 1);
                    TrainChainingUtil.syncTrainConsist(this, this.running, this.speedLevel, this.isReversed());
                    this.level().playSound(null, this.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
                    sendNotification(player, Component.literal("Speed: " + getSpeedName(this.speedLevel)));
                }
            }
            case LocomotiveControlPayload.ACTION_SPEED_UP -> {
                if (this.speedLevel < 5) {
                    this.setSpeedLevel(this.speedLevel + 1);
                    TrainChainingUtil.syncTrainConsist(this, this.running, this.speedLevel, this.isReversed());
                    this.level().playSound(null, this.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
                    sendNotification(player, Component.literal("Speed: " + getSpeedName(this.speedLevel)));
                }
            }
            case LocomotiveControlPayload.ACTION_REVERSE -> {
                boolean newReversed = !this.isReversed();
                this.setReversed(newReversed);
                TrainChainingUtil.syncTrainConsist(this, this.running, this.speedLevel, newReversed);
                this.level().playSound(null, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.8F, 1.2F);
                sendNotification(player, Component.literal("Travel Direction: " + (newReversed ? "REVERSE" : "FORWARD")));
            }
            case LocomotiveControlPayload.ACTION_HORN -> {
                this.blowHorn(player);
            }
            case LocomotiveControlPayload.ACTION_TOGGLE_LIGHT -> {
                boolean newState = !this.isHeadlightEnabled();
                this.setHeadlightEnabled(newState);
                this.level().playSound(null, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.8F, newState ? 1.4F : 0.8F);
                sendNotification(player, Component.literal(newState ? "Headlight: ON" : "Headlight: OFF"));
            }
        }
    }

    private static void sendNotification(@Nullable Player player, Component message) {
        if (player != null) {
            player.sendSystemMessage(message);
        }
    }

    private void updateDynamicLight() {
        if (!this.isHeadlightActive()) {
            this.clearDynamicLight();
            return;
        }

        Direction travel = this.getTravelDirection();
        BlockPos headPos = this.blockPosition().relative(travel, 2).above();
        if (!this.level().hasChunkAt(headPos)) {
            return;
        }
        if (headPos.equals(this.currentLightPos)) {
            return;
        }

        this.clearDynamicLight();
        BlockState targetState = this.level().getBlockState(headPos);
        if (targetState.isAir()) {
            this.level().setBlock(headPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
            this.currentLightPos = headPos;
        }
    }

    public void clearDynamicLight() {
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
            this.clearDynamicLight();
        }
        super.remove(reason);
    }

    public List<Pair<Entity, AbstractMinecart>> getOrderedTrainLinks() {
        List<Pair<Entity, AbstractMinecart>> result = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        visited.add(this.getUUID());

        Deque<Pair<Entity, UUID>> toCheck = new ArrayDeque<>();
        UUID p = TrainChainingUtil.getCartParentUUID(this);
        if (p != null) toCheck.add(Pair.of(this, p));
        UUID c = TrainChainingUtil.getCartChildUUID(this);
        if (c != null) toCheck.add(Pair.of(this, c));

        while (!toCheck.isEmpty()) {
            Pair<Entity, UUID> next = toCheck.poll();
            UUID nextId = next.getSecond();
            if (nextId != null && visited.add(nextId)) {
                Entity ent = this.level().getEntity(nextId);
                if (ent instanceof AbstractMinecart cart && !cart.isRemoved()) {
                    result.add(Pair.of(next.getFirst(), cart));
                    UUID cp = TrainChainingUtil.getCartParentUUID(cart);
                    if (cp != null && !visited.contains(cp)) toCheck.add(Pair.of(cart, cp));
                    UUID cc = TrainChainingUtil.getCartChildUUID(cart);
                    if (cc != null && !visited.contains(cc)) toCheck.add(Pair.of(cart, cc));
                }
            }
        }
        return result;
    }

    public boolean isChainedTo(Entity entity) {
        if (entity instanceof AbstractMinecart cart) {
            UUID id = cart.getUUID();
            UUID p = TrainChainingUtil.getCartParentUUID(this);
            UUID c = TrainChainingUtil.getCartChildUUID(this);
            if (id.equals(p) || id.equals(c)) return true;
            UUID ep = TrainChainingUtil.getCartParentUUID(cart);
            UUID ec = TrainChainingUtil.getCartChildUUID(cart);
            if (this.getUUID().equals(ep) || this.getUUID().equals(ec)) return true;
            for (AbstractMinecart tc : this.getConnectedTrainCarts()) {
                if (tc.getUUID().equals(id)) return true;
            }
        }
        return false;
    }

    public List<AbstractMinecart> getConnectedTrainCarts() {
        List<AbstractMinecart> carts = new ArrayList<>();
        for (Pair<Entity, AbstractMinecart> link : this.getOrderedTrainLinks()) {
            carts.add(link.getSecond());
        }
        return carts;
    }

    @Override
    public boolean isPushable() {
        return !this.running;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return TrainChainingUtil.canCollideWith(this, entity);
    }

    @Override
    public boolean canBeCollidedWith(@org.jspecify.annotations.Nullable Entity other) {
        return TrainChainingUtil.canBeCollidedWith(this, other);
    }

    @Override
    public void push(Entity entity) {
        TrainChainingUtil.pushCollidingEntity(this, entity);
    }

    public Vec3 getAnchorPosFor(Vec3 otherPos) {
        Direction facing = this.getFacing();
        double baseX = super.position().x;
        double baseY = super.position().y;
        double baseZ = super.position().z;
        double dx = otherPos.x - baseX;
        double dz = otherPos.z - baseZ;
        double dot = dx * facing.getStepX() + dz * facing.getStepZ();
        double radius = TrainChainingUtil.getDirectionalCouplerRadius(this, facing, dot >= 0.0D);
        double anchorX = baseX + facing.getStepX() * (dot < 0 ? -radius : radius);
        double anchorZ = baseZ + facing.getStepZ() * (dot < 0 ? -radius : radius);
        Vec3 railAtAnchor = this.getRailPos(anchorX, baseY, anchorZ);
        double anchorY = railAtAnchor != null ? railAtAnchor.y : baseY;
        return new Vec3(anchorX, anchorY, anchorZ);
    }

    private boolean handleChainedCartsMovement(Direction facing,
                                               List<Pair<Entity, AbstractMinecart>> links,
                                               Map<UUID, Vec3> initialCartPositions,
                                               double initialLocoX, double initialLocoY, double initialLocoZ) {
        return TrainChainingUtil.handleChainedCartsMovement(this, facing, links, initialCartPositions, initialLocoX, initialLocoY, initialLocoZ);
    }

    private boolean moveCartAlongRails(AbstractMinecart cart, double dirX, double dirZ, double distance3D) {
        if (distance3D < 1.0E-5D) return true;

        BlockPos railPos = TrainChainingUtil.getCurrentBlockPosOrRailBelow(cart);
        BlockState railState = this.level().getBlockState(railPos);
        if (!BaseRailBlock.isRail(railState)) {
            cart.move(MoverType.SELF, new Vec3(dirX * distance3D, 0.0, dirZ * distance3D));
            return !cart.horizontalCollision;
        }

        cart.setDeltaMovement(dirX * distance3D, 0.0, dirZ * distance3D);

        RailShape shape = ((BaseRailBlock) railState.getBlock()).getRailDirection(railState, this.level(), railPos, cart);
        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
        Vec3i exit0 = exits.getFirst();
        Vec3i exit1 = exits.getSecond();

        double x0 = railPos.getX() + 0.5 + exit0.getX() * 0.5;
        double z0 = railPos.getZ() + 0.5 + exit0.getZ() * 0.5;
        double x1 = railPos.getX() + 0.5 + exit1.getX() * 0.5;
        double z1 = railPos.getZ() + 0.5 + exit1.getZ() * 0.5;
        double xD = x1 - x0;
        double zD = z1 - z0;
        double progress;
        if (xD == 0.0) {
            progress = cart.getZ() - railPos.getZ();
        } else if (zD == 0.0) {
            progress = cart.getX() - railPos.getX();
        } else {
            double xx = cart.getX() - x0;
            double zz = cart.getZ() - z0;
            progress = (xx * xD + zz * zD) * 2.0;
        }

        Direction cartDir = (Math.abs(dirX) > 1.0E-4D || Math.abs(dirZ) > 1.0E-4D)
                ? Direction.getApproximateNearest(dirX, 0.0, dirZ)
                : cart.getMotionDirection();
        boolean uphillAhead = BaseRailBlock.isRail(this.level().getBlockState(railPos.relative(cartDir).above()));

        double x = x0 + xD * progress;
        double z = z0 + zD * progress;
        double y = railPos.getY();
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

        cart.setDeltaMovement(alignedVx, 0.0, alignedVz);
        cart.move(MoverType.SELF, new Vec3(alignedVx, 0.0, alignedVz));

        if (shape.isSlope()) {
            Vec3i lowerExit = exit0.getY() == 0 ? exit0 : exit1;
            Vec3i upperExit = exit0.getY() > 0 ? exit0 : exit1;
            if (Mth.floor(cart.getX()) - railPos.getX() == upperExit.getX() && Mth.floor(cart.getZ()) - railPos.getZ() == upperExit.getZ()) {
                cart.setPos(cart.getX(), cart.getY() + 1.0, cart.getZ());
            } else if (Mth.floor(cart.getX()) - railPos.getX() == lowerExit.getX() && Mth.floor(cart.getZ()) - railPos.getZ() == lowerExit.getZ()) {
                BlockPos lowerRail = railPos.offset(lowerExit.getX(), -1, lowerExit.getZ());
                if (BaseRailBlock.isRail(this.level().getBlockState(lowerRail))) {
                    cart.setPos(cart.getX(), cart.getY() - 1.0, cart.getZ());
                }
            }
        }

        Vec3 newRailPos = this.getRailPosForEntity(cart, cart.getX(), cart.getY(), cart.getZ());
        if (newRailPos != null) {
            cart.setPos(newRailPos.x, newRailPos.y, newRailPos.z);
        }
        return true;
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    protected Item getDropItem() {
        return MiningTools.DIESEL_LOCOMOTIVE_ITEM.get();
    }

    public @Nullable Vec3 getRailPosOffs(double x, double y, double z, double offset) {
        return TrainChainingUtil.getRailPosOffs(this, x, y, z, offset, this.getFacing());
    }

    public @Nullable Vec3 getRailPos(double x, double y, double z) {
        return this.getRailPosForEntity(this, x, y, z);
    }

    public @Nullable Vec3 getRailPosForEntity(AbstractMinecart entity, double x, double y, double z) {
        return TrainChainingUtil.getRailPosForEntity(entity, x, y, z);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.DIESEL_LOCOMOTIVE_ITEM.get());
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new DieselLocomotiveMenu(id, inv, this, this.dataAccess);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Running", this.running);
        output.putString("Facing", this.getFacing().getName());
        output.putInt("SpeedLevel", this.speedLevel);
        output.putBoolean("Headlight", this.isHeadlightEnabled());
        output.putString("TrainName", this.getTrainName());
        output.putBoolean("Reversed", this.isReversed());
        this.tank.serialize(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.running = input.getBooleanOr("Running", false);
        String facingName = input.getStringOr("Facing", Direction.NORTH.getName());
        this.setFacing(Direction.byName(facingName) != null ? Direction.byName(facingName) : Direction.NORTH);
        this.setSpeedLevel(input.getIntOr("SpeedLevel", DEFAULT_SPEED_LEVEL));
        this.setHeadlightEnabled(input.getBooleanOr("Headlight", true));
        this.setTrainName(input.getStringOr("TrainName", ""));
        this.setReversed(input.getBooleanOr("Reversed", false));
        this.tank.deserialize(input);
        this.syncFluidData();
    }
}
