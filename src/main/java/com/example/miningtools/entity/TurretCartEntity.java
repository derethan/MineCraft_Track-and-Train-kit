package com.example.miningtools.entity;

import net.minecraft.core.BlockPos;

import com.example.miningtools.util.TrainChainingUtil;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.menu.TurretCartMenu;

import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Arrow Turret Cart: an armoured rolling-stock wagon carrying a small rotating turret
 * with two bow mounts. Load bows into the two weapon slots and arrows into the
 * magazine; the turret automatically shoots hostile mobs that come near the train.
 */
public class TurretCartEntity extends AbstractMinecartContainer {
    public static final int BOW_SLOTS = 2;
    public static final int ARROW_SLOTS = 9;
    public static final int FIRST_ARROW_SLOT = BOW_SLOTS;
    public static final int TOTAL_SLOTS = BOW_SLOTS + ARROW_SLOTS;

    public static final int FILTER_MODE_WHITELIST = 0;
    public static final int FILTER_MODE_BLACKLIST = 1;

    /** Detection / engagement radius in blocks. Calibrated 32.0 (2x previous 16.0). */
    public static final double RANGE = 32.0D;
    /** Ticks between shots of the same bow. The two bows are staggered by half this period. */
    public static final int FIRE_INTERVAL = 24;
    /** Ticks between target scans (also drives turret aiming). */
    private static final int SCAN_INTERVAL = 4;
    /** Height above the cart origin where arrows are released (just above the turret roof). */
    private static final double MUZZLE_HEIGHT = 1.15D;

    private static final EntityDataAccessor<Float> DATA_TURRET_YAW =
            SynchedEntityData.defineId(TurretCartEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_BOW_MASK =
            SynchedEntityData.defineId(TurretCartEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_FILTER_MODE =
            SynchedEntityData.defineId(TurretCartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_WHITELIST =
            SynchedEntityData.defineId(TurretCartEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_BLACKLIST =
            SynchedEntityData.defineId(TurretCartEntity.class, EntityDataSerializers.STRING);

    private @Nullable LivingEntity currentTarget;

    // Client-side smoothed turret yaw (world-space degrees, atan2(dz, dx) convention)
    public float clientTurretYaw = 0.0F;
    public float clientTurretYawO = 0.0F;
    private boolean clientYawInitialised = false;

    public TurretCartEntity(EntityType<? extends TurretCartEntity> type, Level level) {
        super(type, level);
    }

    public TurretCartEntity(Level level, double x, double y, double z) {
        super(MiningTools.TURRET_CART_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TURRET_YAW, 0.0F);
        builder.define(DATA_BOW_MASK, (byte) 0);
        builder.define(DATA_FILTER_MODE, FILTER_MODE_WHITELIST);
        builder.define(DATA_WHITELIST, "");
        builder.define(DATA_BLACKLIST, "");
    }

    public int getFilterMode() {
        return this.entityData.get(DATA_FILTER_MODE);
    }

    public void setFilterMode(int mode) {
        this.entityData.set(DATA_FILTER_MODE, mode == FILTER_MODE_BLACKLIST ? FILTER_MODE_BLACKLIST : FILTER_MODE_WHITELIST);
    }

    public String getWhitelist() {
        return this.entityData.get(DATA_WHITELIST);
    }

    public void setWhitelist(String whitelist) {
        this.entityData.set(DATA_WHITELIST, whitelist != null ? whitelist.trim() : "");
    }

    public String getBlacklist() {
        return this.entityData.get(DATA_BLACKLIST);
    }

    public void setBlacklist(String blacklist) {
        this.entityData.set(DATA_BLACKLIST, blacklist != null ? blacklist.trim() : "");
    }

    // ---------------------------------------------------------------------
    // Item rules
    // ---------------------------------------------------------------------

    public static boolean isBow(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BowItem;
    }

    public static boolean isArrow(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ArrowItem;
    }

    public static boolean isBowSlot(int slot) {
        return slot >= 0 && slot < BOW_SLOTS;
    }

    public static boolean isArrowSlot(int slot) {
        return slot >= FIRST_ARROW_SLOT && slot < TOTAL_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (isBowSlot(slot)) {
            return isBow(stack);
        }
        if (isArrowSlot(slot)) {
            return isArrow(stack);
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    // ---------------------------------------------------------------------
    // Synced visuals
    // ---------------------------------------------------------------------

    /** World-space yaw (degrees, atan2(dz, dx)) that the turret is aimed at. */
    public float getTurretYaw() {
        return this.entityData.get(DATA_TURRET_YAW);
    }

    public boolean hasBowInSlot(int slot) {
        return (this.entityData.get(DATA_BOW_MASK) & (1 << slot)) != 0;
    }

    private void updateBowMask() {
        byte mask = 0;
        for (int i = 0; i < BOW_SLOTS; i++) {
            if (isBow(this.getItem(i))) {
                mask |= (byte) (1 << i);
            }
        }
        if (this.entityData.get(DATA_BOW_MASK) != mask) {
            this.entityData.set(DATA_BOW_MASK, mask);
        }
    }

    // ---------------------------------------------------------------------
    // Ticking
    // ---------------------------------------------------------------------

    @Override
    public BlockPos getCurrentBlockPosOrRailBelow() {
        return TrainChainingUtil.getCurrentBlockPosOrRailBelow(this);
    }

    @Override
    public void tick() {
        if (!TrainChainingUtil.shouldSkipVanillaServerTick(this)) {
            super.tick();
        }
        TrainChainingUtil.tickConsistMovement(this);

        if (this.level().isClientSide()) {
            float target = this.getTurretYaw();
            if (!this.clientYawInitialised) {
                this.clientTurretYaw = target;
                this.clientTurretYawO = target;
                this.clientYawInitialised = true;
            }
            this.clientTurretYawO = this.clientTurretYaw;
            this.clientTurretYaw = Mth.approachDegrees(this.clientTurretYaw, target, 12.0F);
            return;
        }

        if (this.isRemoved() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.tickCount % SCAN_INTERVAL == 0) {
            this.updateBowMask();
            if (this.hasAnyBow() && this.hasAnyArrow()) {
                this.currentTarget = this.findTarget();
                if (this.currentTarget != null) {
                    double dx = this.currentTarget.getX() - this.getX();
                    double dz = this.currentTarget.getZ() - this.getZ();
                    this.entityData.set(DATA_TURRET_YAW, (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG));
                }
            } else {
                this.currentTarget = null;
            }
        }

        int phase = this.tickCount % FIRE_INTERVAL;
        int bowSlot = -1;
        if (phase == 0) {
            bowSlot = 0;
        } else if (phase == FIRE_INTERVAL / 2) {
            bowSlot = 1;
        }
        if (bowSlot >= 0) {
            this.tryFire(serverLevel, bowSlot);
        }
    }

    private boolean hasAnyBow() {
        for (int i = 0; i < BOW_SLOTS; i++) {
            if (isBow(this.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyArrow() {
        return this.findArrowSlot() >= 0;
    }

    private int findArrowSlot() {
        for (int i = FIRST_ARROW_SLOT; i < TOTAL_SLOTS; i++) {
            if (isArrow(this.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private Vec3 getMuzzleBase() {
        return new Vec3(this.getX(), this.getY() + MUZZLE_HEIGHT, this.getZ());
    }

    public boolean isPlayerWhitelisted(Player player) {
        if (player == null) return false;
        String pName = player.getName().getString();
        String wl = this.getWhitelist();
        if (wl == null || wl.isBlank()) return false;
        for (String entry : wl.split(",")) {
            if (entry.trim().equalsIgnoreCase(pName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPlayerBlacklisted(Player player) {
        if (player == null) return false;
        String pName = player.getName().getString();
        String bl = this.getBlacklist();
        if (bl == null || bl.isBlank()) return false;
        for (String entry : bl.split(",")) {
            if (entry.trim().equalsIgnoreCase(pName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNameInList(String name, String list) {
        if (name == null || list == null || list.isBlank()) return false;
        for (String entry : list.split(",")) {
            if (entry.trim().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Valid targets:
     * - In Whitelist mode: any hostile Enemy mob (unless whitelisted), plus any Player NOT on the whitelist
     *   (when a whitelist is configured). Whitelisted players/allies are never targeted.
     * - In Blacklist mode: any hostile Enemy mob, plus any Player explicitly on the blacklist.
     * Never mobs riding in a minecart (train cargo/passengers).
     */
    private boolean isValidTarget(LivingEntity entity) {
        if (!entity.isAlive() || entity.isDeadOrDying() || entity.isRemoved() || entity.isSpectator()) {
            return false;
        }
        if (entity.getVehicle() instanceof AbstractMinecart) {
            return false;
        }
        if (entity.distanceToSqr(this) > RANGE * RANGE) {
            return false;
        }

        if (entity instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
            if (this.getFilterMode() == FILTER_MODE_WHITELIST) {
                // In whitelist mode: if whitelist is specified, non-whitelisted players are targeted!
                return !this.getWhitelist().isBlank() && !this.isPlayerWhitelisted(player);
            } else {
                // In blacklist mode: target players explicitly on the blacklist!
                return this.isPlayerBlacklisted(player);
            }
        }

        if (entity instanceof Enemy) {
            if (entity instanceof Mob mob && !mob.canBeSeenAsEnemy()) {
                return false;
            }
            if (entity instanceof OwnableEntity ownable && ownable.getOwnerReference() != null) {
                return false;
            }
            String typeName = entity.getType().getDescription().getString();
            if (this.getFilterMode() == FILTER_MODE_WHITELIST && this.isNameInList(typeName, this.getWhitelist())) {
                return false;
            }
            return true;
        }

        if (entity instanceof NeutralMob neutral && neutral.isAngry()) {
            return true;
        }

        String typeName = entity.getType().getDescription().getString();
        return this.getFilterMode() == FILTER_MODE_BLACKLIST && this.isNameInList(typeName, this.getBlacklist());
    }

    private @Nullable LivingEntity findTarget() {
        AABB area = this.getBoundingBox().inflate(RANGE, RANGE * 0.5D, RANGE);
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(LivingEntity.class, area, this::isValidTarget);
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator.comparingDouble(m -> m.distanceToSqr(this)));
        Vec3 muzzle = this.getMuzzleBase();
        for (LivingEntity target : candidates) {
            Vec3 aim = new Vec3(target.getX(), target.getY(0.5D), target.getZ());
            if (this.hasClearShot(muzzle, aim, target)) {
                return target;
            }
        }
        return null;
    }

    /**
     * Line-of-sight check against blocks, plus friendly-fire safety: refuse shot if a
     * whitelisted player, train cart, or neutral/friendly entity is in the way.
     */
    private boolean hasClearShot(Vec3 from, Vec3 to, Entity target) {
        HitResult blockHit = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            return false;
        }
        AABB sweep = new AABB(from, to).inflate(1.0D);
        List<Entity> blockers = this.level().getEntities(this, sweep, e -> e != target
                && !e.isSpectator()
                && (e instanceof AbstractMinecart || (e instanceof Player p && this.isPlayerWhitelisted(p)) || (e instanceof LivingEntity le && !this.isValidTarget(le))));
        for (Entity e : blockers) {
            if (e.getBoundingBox().inflate(0.3D).clip(from, to).isPresent()) {
                return false;
            }
        }
        return true;
    }

    private void tryFire(ServerLevel level, int bowSlot) {
        ItemStack bow = this.getItem(bowSlot);
        if (!isBow(bow)) {
            return;
        }
        LivingEntity target = this.currentTarget;
        if (target == null || !this.isValidTarget(target)) {
            return;
        }
        int arrowSlot = this.findArrowSlot();
        if (arrowSlot < 0) {
            return;
        }
        ItemStack ammo = this.getItem(arrowSlot);
        if (!(ammo.getItem() instanceof ArrowItem arrowItem)) {
            return;
        }

        // Release point: above the turret, nudged toward the target so the arrow clears the cart.
        double dx0 = target.getX() - this.getX();
        double dz0 = target.getZ() - this.getZ();
        double horiz0 = Math.sqrt(dx0 * dx0 + dz0 * dz0);
        double nx = horiz0 > 1.0E-4D ? dx0 / horiz0 : 0.0D;
        double nz = horiz0 > 1.0E-4D ? dz0 / horiz0 : 0.0D;
        Vec3 muzzle = this.getMuzzleBase().add(nx * 0.6D, 0.0D, nz * 0.6D);

        Vec3 aim = new Vec3(target.getX(), target.getY(0.5D), target.getZ());
        if (!this.hasClearShot(muzzle, aim, target)) {
            return;
        }

        Projectile projectile = arrowItem.asProjectile(level, muzzle, ammo.copyWithCount(1), Direction.UP);
        if (!(projectile instanceof AbstractArrow arrow)) {
            return;
        }
        arrow.setOwner(this);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;

        // Same ballistic approximation as AbstractSkeleton#performRangedAttack
        double xd = target.getX() - arrow.getX();
        double yd = target.getY(0.3333333333333333D) - arrow.getY();
        double zd = target.getZ() - arrow.getZ();
        double distance = Math.sqrt(xd * xd + zd * zd);
        Projectile.spawnProjectileUsingShoot(arrow, level, ammo, xd, yd + distance * 0.2D, zd, 1.6F, 2.0F);

        ammo.shrink(1);
        if (ammo.isEmpty()) {
            this.setItem(arrowSlot, ItemStack.EMPTY);
        }

        bow.hurtAndBreak(1, level, (LivingEntity) null, broken -> {});
        if (bow.isEmpty()) {
            this.setItem(bowSlot, ItemStack.EMPTY);
            level.playSound(null, this.getX(), this.getY() + MUZZLE_HEIGHT, this.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 0.8F, 0.8F + this.random.nextFloat() * 0.4F);
            this.updateBowMask();
        }
        this.setChanged();

        level.playSound(null, this.getX(), this.getY() + MUZZLE_HEIGHT, this.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
    }

    // ---------------------------------------------------------------------
    // Minecart / container plumbing
    // ---------------------------------------------------------------------

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (!this.level().isClientSide() && isBowSlot(slot)) {
            this.updateBowMask();
        }
    }

    @Override
    protected Item getDropItem() {
        return MiningTools.TURRET_CART_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.TURRET_CART_ITEM.get());
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
        return new TurretCartMenu(containerId, inventory, this);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.getItemInHand(hand).is(MiningTools.CROWBAR_ITEM.get())) {
            return InteractionResult.PASS;
        }
        return this.interactWithContainerVehicle(player);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("TurretYaw", this.getTurretYaw());
        output.putInt("FilterMode", this.getFilterMode());
        output.putString("Whitelist", this.getWhitelist());
        output.putString("Blacklist", this.getBlacklist());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_TURRET_YAW, input.getFloatOr("TurretYaw", 0.0F));
        this.setFilterMode(input.getIntOr("FilterMode", FILTER_MODE_WHITELIST));
        this.setWhitelist(input.getStringOr("Whitelist", ""));
        this.setBlacklist(input.getStringOr("Blacklist", ""));
        this.updateBowMask();
    }




    @Override
    public boolean canCollideWith(Entity entity) {
        if (TrainChainingUtil.isConnectedInSameTrain(this, entity)) {
            return false;
        }
        return super.canCollideWith(entity);
    }

    @Override
    public boolean canBeCollidedWith(@org.jspecify.annotations.Nullable Entity other) {
        if (other != null && TrainChainingUtil.isConnectedInSameTrain(this, other)) {
            return false;
        }
        return super.canBeCollidedWith(other);
    }

    @Override
    public void push(Entity entity) {
        TrainChainingUtil.pushCollidingEntity(this, entity);
    }

    @Override
    protected Vec3 applyNaturalSlowdown(Vec3 deltaMovement) {
        return TrainChainingUtil.applyRollingStockSlowdown(this, deltaMovement);
    }
}