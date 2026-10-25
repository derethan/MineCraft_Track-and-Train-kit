package com.example.miningtools.entity;

import com.example.miningtools.Config;
import com.example.miningtools.MiningTools;
import com.example.miningtools.menu.GeneratorMinecartMenu;
import com.example.miningtools.util.TrainChainingUtil;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class GeneratorMinecartEntity extends AbstractMinecartContainer implements WorldlyContainer {
    public static final int TOTAL_SLOTS = 4;
    public static final int FUEL_SLOT = 0;
    public static final int WATER_IN_SLOT = 1;
    public static final int BUCKET_OUT_SLOT = 2;
    public static final int ENERGY_ITEM_SLOT = 3;

    private static final int[] SLOTS_TOP = new int[]{FUEL_SLOT, WATER_IN_SLOT};
    private static final int[] SLOTS_BOTTOM = new int[]{BUCKET_OUT_SLOT};
    private static final int[] SLOTS_SIDES = new int[]{ENERGY_ITEM_SLOT, FUEL_SLOT, WATER_IN_SLOT};

    private static final EntityDataAccessor<Integer> DATA_WATER_AMOUNT =
            SynchedEntityData.defineId(GeneratorMinecartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WATER_CAPACITY =
            SynchedEntityData.defineId(GeneratorMinecartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ENERGY_STORED =
            SynchedEntityData.defineId(GeneratorMinecartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ENERGY_CAPACITY =
            SynchedEntityData.defineId(GeneratorMinecartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_GENERATING =
            SynchedEntityData.defineId(GeneratorMinecartEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BURN_TIME =
            SynchedEntityData.defineId(GeneratorMinecartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BURN_TOTAL =
            SynchedEntityData.defineId(GeneratorMinecartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_TRAIN_NAME =
            SynchedEntityData.defineId(GeneratorMinecartEntity.class, EntityDataSerializers.STRING);

    public int burnTime = 0;
    public int burnTotal = 0;

    private final SimpleEnergyHandler energyStorage = new SimpleEnergyHandler(
            Config.GENERATOR_CART_ENERGY_CAPACITY.get(),
            Config.GENERATOR_CART_TRANSFER_RATE.get(),
            Config.GENERATOR_CART_TRANSFER_RATE.get()
    ) {
        @Override
        protected void onEnergyChanged(int previousEnergy) {
            super.onEnergyChanged(previousEnergy);
            GeneratorMinecartEntity.this.syncEnergyData();
        }
    };

    private final FluidStacksResourceHandler tank = new FluidStacksResourceHandler(1, Config.GENERATOR_CART_WATER_CAPACITY.get()) {
        @Override
        protected void onContentsChanged(int index, FluidStack previousStack) {
            super.onContentsChanged(index, previousStack);
            GeneratorMinecartEntity.this.syncFluidData();
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.getFluid() == Fluids.WATER;
        }
    };

    protected final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> GeneratorMinecartEntity.this.getFluidAmount() & 0xFFFF;
                case 1 -> (GeneratorMinecartEntity.this.getFluidAmount() >> 16) & 0xFFFF;
                case 2 -> GeneratorMinecartEntity.this.getWaterCapacity() & 0xFFFF;
                case 3 -> (GeneratorMinecartEntity.this.getWaterCapacity() >> 16) & 0xFFFF;
                case 4 -> GeneratorMinecartEntity.this.getEnergyStored() & 0xFFFF;
                case 5 -> (GeneratorMinecartEntity.this.getEnergyStored() >> 16) & 0xFFFF;
                case 6 -> GeneratorMinecartEntity.this.getEnergyCapacity() & 0xFFFF;
                case 7 -> (GeneratorMinecartEntity.this.getEnergyCapacity() >> 16) & 0xFFFF;
                case 8 -> GeneratorMinecartEntity.this.burnTime;
                case 9 -> GeneratorMinecartEntity.this.burnTotal;
                case 10 -> GeneratorMinecartEntity.this.isGenerating() ? Config.GENERATOR_CART_ENERGY_RATE.get() : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 8 -> GeneratorMinecartEntity.this.burnTime = value;
                case 9 -> GeneratorMinecartEntity.this.burnTotal = value;
            }
        }

        @Override
        public int getCount() {
            return 11;
        }
    };

    public GeneratorMinecartEntity(EntityType<? extends AbstractMinecart> type, Level level) {
        super(type, level);
    }

    public GeneratorMinecartEntity(Level level, double x, double y, double z) {
        super(MiningTools.GENERATOR_MINECART_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected Item getDropItem() {
        return MiningTools.GENERATOR_MINECART_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.GENERATOR_MINECART_ITEM.get());
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    public int getWaterCapacity() {
        return Config.GENERATOR_CART_WATER_CAPACITY.get();
    }

    public int getEnergyCapacity() {
        return Config.GENERATOR_CART_ENERGY_CAPACITY.get();
    }

    public int getFluidAmount() {
        return this.entityData.get(DATA_WATER_AMOUNT);
    }

    public int getEnergyStored() {
        return this.entityData.get(DATA_ENERGY_STORED);
    }

    public boolean isGenerating() {
        return this.entityData.get(DATA_IS_GENERATING);
    }

    public String getTrainName() {
        return this.entityData.get(DATA_TRAIN_NAME);
    }

    public void setTrainName(String name) {
        this.entityData.set(DATA_TRAIN_NAME, name != null ? name : "");
        if (name != null && !name.isBlank()) {
            this.setCustomName(Component.literal(name));
        } else {
            this.setCustomName(null);
        }
    }

    @Override
    public Component getDisplayName() {
        String custom = this.getTrainName();
        if (custom != null && !custom.isBlank()) {
            return Component.literal(custom);
        }
        return Component.translatable("entity.miningtools.generator_cart");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WATER_AMOUNT, 0);
        builder.define(DATA_WATER_CAPACITY, Config.GENERATOR_CART_WATER_CAPACITY.get());
        builder.define(DATA_ENERGY_STORED, 0);
        builder.define(DATA_ENERGY_CAPACITY, Config.GENERATOR_CART_ENERGY_CAPACITY.get());
        builder.define(DATA_IS_GENERATING, false);
        builder.define(DATA_BURN_TIME, 0);
        builder.define(DATA_BURN_TOTAL, 0);
        builder.define(DATA_TRAIN_NAME, "");
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.tank;
    }

    public EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return this.energyStorage;
    }

    public SimpleEnergyHandler getInternalEnergyStorage() {
        return this.energyStorage;
    }

    public void syncFluidData() {
        if (!this.level().isClientSide()) {
            FluidStack stack = FluidUtil.getStack(this.tank, 0);
            int amt = stack.isEmpty() ? 0 : stack.getAmount();
            this.entityData.set(DATA_WATER_AMOUNT, amt);
            this.entityData.set(DATA_WATER_CAPACITY, this.getWaterCapacity());
        }
    }

    public void syncEnergyData() {
        if (!this.level().isClientSide()) {
            this.entityData.set(DATA_ENERGY_STORED, this.energyStorage.getAmountAsInt());
            this.entityData.set(DATA_ENERGY_CAPACITY, this.energyStorage.getCapacityAsInt());
        }
    }

    @Override
    public void tick() {
        if (!TrainChainingUtil.shouldSkipVanillaServerTick(this)) {
            super.tick();
        }
        TrainChainingUtil.tickConsistMovement(this);

        if (this.level().isClientSide()) {
            this.tickClientEffects();
            return;
        }

        this.tickWaterInputContainer();
        this.siphonWaterFromTenders();
        this.tickGeneration();
        this.tickEnergyItemSlot();
        this.distributeEnergy();

        this.syncFluidData();
        this.syncEnergyData();
    }

    private void tickClientEffects() {
        if (!this.isGenerating()) return;

        double forward = 0.45D;
        double rad = Math.toRadians(this.getYRot() + 90.0D);
        double stackX = this.getX() + Math.cos(rad) * forward;
        double stackZ = this.getZ() + Math.sin(rad) * forward;
        double stackY = this.getY() + 1.20D;

        if (this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, stackX, stackY, stackZ,
                    0.0D, 0.06D, 0.0D);
        }
        if (this.random.nextInt(4) == 0) {
            this.level().addParticle(ParticleTypes.POOF, stackX, stackY, stackZ,
                    0.0D, 0.03D, 0.0D);
        }

        // Electrical discharge sparks around the dynamo insulators
        if (this.random.nextInt(6) == 0) {
            double sparkX = this.getX() - Math.cos(rad) * 0.35D + (this.random.nextDouble() - 0.5D) * 0.4D;
            double sparkZ = this.getZ() - Math.sin(rad) * 0.35D + (this.random.nextDouble() - 0.5D) * 0.4D;
            double sparkY = this.getY() + 1.05D;
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, sparkX, sparkY, sparkZ,
                    (this.random.nextDouble() - 0.5D) * 0.02D, 0.01D, (this.random.nextDouble() - 0.5D) * 0.02D);
        }
    }

    private void tickWaterInputContainer() {
        ItemStack in = this.getItem(WATER_IN_SLOT);
        if (in.isEmpty()) return;

        FluidStack contained = FluidUtil.getFirstStackContained(in);
        if ((contained == null || contained.isEmpty()) && in.getItem() instanceof net.minecraft.world.item.BucketItem bucket) {
            net.minecraft.world.level.material.Fluid bucketFluid = bucket.getContent();
            if (bucketFluid == Fluids.WATER) {
                contained = new FluidStack(Fluids.WATER, 1000);
            }
        }
        if (contained != null && !contained.isEmpty() && contained.getFluid() == Fluids.WATER && this.getFluidAmount() < this.getWaterCapacity()) {
            int toInsert = Math.min(contained.getAmount(), this.getWaterCapacity() - this.getFluidAmount());
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.open(null)) {
                int inserted = this.tank.insert(FluidResource.of(contained), toInsert, tx);
                if (inserted > 0) {
                    tx.commit();
                    this.syncFluidData();
                    ItemStack remainder = new ItemStack(Items.BUCKET);
                    in.shrink(1);
                    ItemStack out = this.getItem(BUCKET_OUT_SLOT);
                    if (out.isEmpty()) {
                        this.setItem(BUCKET_OUT_SLOT, remainder);
                    } else if (ItemStack.isSameItemSameComponents(out, remainder) && out.getCount() < out.getMaxStackSize()) {
                        out.grow(1);
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    private void siphonWaterFromTenders() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        FluidStack current = FluidUtil.getStack(this.tank, 0);
        int currentAmt = current.isEmpty() ? 0 : current.getAmount();
        int space = this.getWaterCapacity() - currentAmt;
        if (space <= 0) return;

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

        for (Entity tender : tenders) {
            if (space <= 0) break;
            var sourceHandler = tender.getCapability(Capabilities.Fluid.ENTITY, null);
            if (sourceHandler != null) {
                try {
                    int moved = net.neoforged.neoforge.transfer.ResourceHandlerUtil.move(
                            sourceHandler,
                            this.tank,
                            r -> r.toStack(1).getFluid() == Fluids.WATER,
                            Math.min(space, 200),
                            null
                    );
                    if (moved > 0) {
                        this.syncFluidData();
                        if (tender instanceof LiquidMinecartEntity lme) {
                            lme.syncFluidData();
                        } else if (tender instanceof LargeLiquidMinecartEntity llme) {
                            llme.syncFluidData();
                        }
                        space -= moved;
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    private void tickGeneration() {
        FluidStack water = FluidUtil.getStack(this.tank, 0);
        int waterAmt = water.isEmpty() ? 0 : water.getAmount();
        boolean hasWater = waterAmt > 0;
        boolean needsEnergy = this.energyStorage.getAmountAsInt() < this.energyStorage.getCapacityAsInt();

        if (this.burnTime > 0) {
            if (hasWater && needsEnergy) {
                this.burnTime--;
                // Consumes 1 mB of water every 2 ticks (10 mB/sec)
                if (this.tickCount % 2 == 0) {
                    try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.open(null)) {
                        var extracted = net.neoforged.neoforge.transfer.ResourceHandlerUtil.extractFirst(this.tank, r -> true, 1, tx);
                        if (extracted != null && !extracted.isEmpty()) {
                            tx.commit();
                        }
                    } catch (Throwable ignored) {}
                }
                int rate = Config.GENERATOR_CART_ENERGY_RATE.get();
                this.energyStorage.insert(rate, null);
                this.setGenerating(true);
            } else {
                // Throttled: preserve fuel when battery is full or water is dry
                this.setGenerating(false);
            }
        } else {
            this.setGenerating(false);
            if (hasWater && needsEnergy) {
                ItemStack fuel = this.getItem(FUEL_SLOT);
                int duration = getFuelBurnDuration(fuel);
                if (duration > 0) {
                    fuel.shrink(1);
                    this.burnTime = duration;
                    this.burnTotal = duration;
                    this.setGenerating(true);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.5F, 1.0F);
                }
            }
        }

        this.entityData.set(DATA_BURN_TIME, this.burnTime);
        this.entityData.set(DATA_BURN_TOTAL, this.burnTotal);
    }

    private void setGenerating(boolean generating) {
        if (this.entityData.get(DATA_IS_GENERATING) != generating) {
            this.entityData.set(DATA_IS_GENERATING, generating);
        }
    }

    public static int getFuelBurnDuration(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(Items.COAL_BLOCK)) return 14400;
        if (stack.is(ItemTags.COALS)) return 1600;
        if (stack.is(Items.BLAZE_ROD)) return 2400;
        if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS)) return 300;
        if (stack.is(ItemTags.WOODEN_SLABS)) return 150;
        return 0;
    }

    private void tickEnergyItemSlot() {
        ItemStack item = this.getItem(ENERGY_ITEM_SLOT);
        if (!item.isEmpty() && this.energyStorage.getAmountAsInt() > 0) {
            var itemAccess = ItemAccess.forStack(item.copyWithCount(1));
            var itemHandler = itemAccess.getCapability(Capabilities.Energy.ITEM);
            if (itemHandler != null) {
                int rate = Config.GENERATOR_CART_TRANSFER_RATE.get();
                int moved = EnergyHandlerUtil.move(this.energyStorage, itemHandler, rate, null);
                if (moved > 0) {
                    this.setItem(ENERGY_ITEM_SLOT, itemAccess.getResource().toStack());
                    this.setChanged();
                }
            }
        }
    }

    private void distributeEnergy() {
        if (this.energyStorage.getAmountAsInt() <= 0) return;

        Level level = this.level();
        int maxTransfer = Config.GENERATOR_CART_TRANSFER_RATE.get();

        // 1. Direct train consist locomotive support: power coupled Electric Locomotives
        for (Pair<Entity, AbstractMinecart> link : TrainChainingUtil.getOrderedTrainLinks(this)) {
            if (this.energyStorage.getAmountAsInt() <= 0) break;
            AbstractMinecart linked = link.getSecond();
            if (linked instanceof ElectricLocomotiveEntity elec && !elec.isRemoved()) {
                var elecEnergy = elec.getEnergyHandler(null);
                if (elecEnergy != null) {
                    EnergyHandlerUtil.move(this.energyStorage, elecEnergy, maxTransfer, null);
                }
            }
        }

        if (this.energyStorage.getAmountAsInt() <= 0) return;

        // 2. Adjacent block transfer (push to connected cables, conduits, and machines)
        BlockPos currentPos = this.blockPosition();
        for (Direction dir : Direction.values()) {
            if (this.energyStorage.getAmountAsInt() <= 0) break;
            BlockPos targetPos = currentPos.relative(dir);
            var blockCap = level.getCapability(Capabilities.Energy.BLOCK, targetPos, dir.getOpposite());
            if (blockCap != null) {
                EnergyHandlerUtil.move(this.energyStorage, blockCap, maxTransfer, null);
            }
        }

        if (this.energyStorage.getAmountAsInt() <= 0) return;

        // 3. Wireless radius area broadcast to nearby energy blocks/cables
        int radius = Config.GENERATOR_CART_BROADCAST_RADIUS.get();
        if (radius > 1) {
            for (BlockPos targetPos : BlockPos.betweenClosed(
                    currentPos.offset(-radius, -1, -radius),
                    currentPos.offset(radius, 2, radius))) {
                if (this.energyStorage.getAmountAsInt() <= 0) break;
                if (targetPos.equals(currentPos)) continue;
                if (Math.abs(targetPos.getX() - currentPos.getX()) <= 1
                        && Math.abs(targetPos.getY() - currentPos.getY()) <= 1
                        && Math.abs(targetPos.getZ() - currentPos.getZ()) <= 1) {
                    continue;
                }
                var blockCap = level.getCapability(Capabilities.Energy.BLOCK, targetPos, null);
                if (blockCap != null) {
                    EnergyHandlerUtil.move(this.energyStorage, blockCap, maxTransfer / 2, null);
                }
            }
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) {
            return SLOTS_TOP;
        } else if (side == Direction.DOWN) {
            return SLOTS_BOTTOM;
        } else {
            return SLOTS_SIDES;
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == FUEL_SLOT) {
            return getFuelBurnDuration(stack) > 0;
        }
        if (slot == WATER_IN_SLOT) {
            return stack.is(Items.WATER_BUCKET);
        }
        if (slot == ENERGY_ITEM_SLOT) {
            return ItemAccess.forStack(stack.copyWithCount(1)).getCapability(Capabilities.Energy.ITEM) != null;
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == BUCKET_OUT_SLOT || (slot == ENERGY_ITEM_SLOT && side != Direction.UP);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new GeneratorMinecartMenu(containerId, inventory, this, this.containerData);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.getItemInHand(hand).is(MiningTools.CROWBAR_ITEM.get())) {
            return InteractionResult.PASS;
        }
        return this.interactWithContainerVehicle(player);
    }

    @Override
    protected Vec3 applyNaturalSlowdown(Vec3 deltaMovement) {
        return TrainChainingUtil.applyRollingStockSlowdown(this, deltaMovement);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("EnergyStored", this.energyStorage.getAmountAsInt());
        output.putInt("WaterAmount", this.getFluidAmount());
        output.putInt("BurnTime", this.burnTime);
        output.putInt("BurnTotal", this.burnTotal);
        output.putString("TrainName", this.getTrainName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int energy = input.getIntOr("EnergyStored", 0);
        int water = input.getIntOr("WaterAmount", 0);
        this.burnTime = input.getIntOr("BurnTime", 0);
        this.burnTotal = input.getIntOr("BurnTotal", 0);
        this.setTrainName(input.getStringOr("TrainName", ""));

        if (energy > 0) {
            this.energyStorage.insert(energy, null);
        }
        if (water > 0) {
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.open(null)) {
                this.tank.insert(FluidResource.of(Fluids.WATER), water, tx);
                tx.commit();
            } catch (Throwable ignored) {}
        }
        this.syncFluidData();
        this.syncEnergyData();
    }
}
