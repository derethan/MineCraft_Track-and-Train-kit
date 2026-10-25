package com.example.miningtools.util;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.Config;
import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.CabooseMinecartEntity;
import com.example.miningtools.entity.AggregateHopperCartEntity;
import com.example.miningtools.entity.CargoMinecartEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.entity.LargeLiquidMinecartEntity;
import com.example.miningtools.entity.LiquidMinecartEntity;
import com.example.miningtools.entity.LivestockCartEntity;
import com.example.miningtools.entity.TunnelBoreEntity;
import com.example.miningtools.entity.TurretCartEntity;
import com.example.miningtools.entity.GeneratorMinecartEntity;
import com.example.miningtools.network.TrainLinkSyncPayload;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TrainChainingUtil {
    /**
     * Finds the rail BlockPos at, below, or above an entity, supporting steep multi-block descents.
     * Checks entity level, 1 block below, 2 blocks below, 3 blocks below, and 1 block above.
     */
    public static BlockPos getCurrentBlockPosOrRailBelow(Entity entity) {
        int i = Mth.floor(entity.getX());
        int j = Mth.floor(entity.getY());
        int k = Mth.floor(entity.getZ());
        BlockPos atPos = new BlockPos(i, j, k);
        if (BaseRailBlock.isRail(entity.level(), atPos)) {
            return atPos;
        }
        BlockPos belowPos = new BlockPos(i, j - 1, k);
        if (BaseRailBlock.isRail(entity.level(), belowPos)) {
            return belowPos;
        }
        BlockPos abovePos = new BlockPos(i, j + 1, k);
        if (BaseRailBlock.isRail(entity.level(), abovePos)) {
            return abovePos;
        }
        return atPos;
    }

    private TrainChainingUtil() {}

    public static final String TAG_PARENT_CART = "miningtools:parent_cart";
    public static final String TAG_CHILD_CART = "miningtools:child_cart";

    public static final double COUPLER_GAP = 0.28D;

    /**
     * Returns the normalized horizontal heading vector (X, 0, Z) for a cart along its track or yaw orientation.
     */
    public static Vec3 getCartHeadingVector(Entity entity, double fallbackDirX, double fallbackDirZ) {
        if (entity instanceof BasicLocomotiveEntity bl) {
            Direction f = bl.getFacing();
            return new Vec3(f.getStepX(), 0.0D, f.getStepZ());
        }
        if (entity instanceof DieselLocomotiveEntity dl) {
            Direction f = dl.getFacing();
            return new Vec3(f.getStepX(), 0.0D, f.getStepZ());
        }
        if (entity instanceof ElectricLocomotiveEntity el) {
            Direction f = el.getFacing();
            return new Vec3(f.getStepX(), 0.0D, f.getStepZ());
        }
        double fLen = Math.hypot(fallbackDirX, fallbackDirZ);
        if (fLen > 1.0E-4D) {
            return new Vec3(fallbackDirX / fLen, 0.0D, fallbackDirZ / fLen);
        }
        float yawRad = (float) Math.toRadians(entity.getYRot());
        double hx = -Math.cos(yawRad);
        double hz = -Math.sin(yawRad);
        double hLen = Math.hypot(hx, hz);
        if (hLen > 1.0E-4D) {
            return new Vec3(hx / hLen, 0.0D, hz / hLen);
        }
        return new Vec3(1.0D, 0.0D, 0.0D);
    }

    /**
     * Returns the 3D world position of the cart's Front Connection Point (coupler/pivot).
     */
    public static Vec3 getFrontConnectionPoint(Entity entity, double dirX, double dirZ) {
        Vec3 heading = getCartHeadingVector(entity, dirX, dirZ);
        double r = getDirectionalCouplerRadius(entity, heading.x, heading.z);
        return new Vec3(entity.getX() + heading.x * r, entity.getY() + 0.25D, entity.getZ() + heading.z * r);
    }

    /**
     * Returns the 3D world position of the cart's Rear Connection Point (coupler/pivot).
     */
    public static Vec3 getRearConnectionPoint(Entity entity, double dirX, double dirZ) {
        Vec3 heading = getCartHeadingVector(entity, dirX, dirZ);
        double r = getDirectionalCouplerRadius(entity, -heading.x, -heading.z);
        return new Vec3(entity.getX() - heading.x * r, entity.getY() + 0.25D, entity.getZ() - heading.z * r);
    }

    /**
     * Returns the half-length radius (center to coupler bumper) of a train vehicle in blocks.
     */
    public static double getCartCouplerRadius(Entity entity) {
        if (entity instanceof BasicLocomotiveEntity) {
            // Steam locomotive frame is 25px long (1.56m) -> 0.78m radius
            return 0.78D;
        }
        if (entity instanceof DieselLocomotiveEntity) {
            // Heavy CN Road diesel locomotive frame (38px + plow = 2.41m -> 1.20m radius)
            return 1.20D;
        }
        if (entity instanceof ElectricLocomotiveEntity) {
            // Bullet train chassis (28px - 37px -> 1.17m average radius)
            return 1.17D;
        }
        if (entity instanceof LargeLiquidMinecartEntity) {
            // Heavy DOT-111 tank car couplers extend to 128px * 0.14 = 17.92px = 1.12m
            return 1.12D;
        }
        if (entity instanceof CabooseMinecartEntity) {
            // Caboose porches and couplers extend to 108px * 0.14 = 15.12px = 0.95m
            return 0.95D;
        }
        if (entity instanceof LivestockCartEntity) {
            // Standard railroad stock car frame (24px long -> 0.88m radius)
            return 0.88D;
        }
        if (entity instanceof TurretCartEntity) {
            // Standard armored turret wagon frame (24px long -> 0.88m radius)
            return 0.88D;
        }
        if (entity instanceof GeneratorMinecartEntity) {
            // Heavy generator / dynamo cart frame (24px long -> 0.88m radius)
            return 0.88D;
        }
        if (entity instanceof AggregateHopperCartEntity) {
            // High-sided aggregate hopper car (26px frame -> 0.92m radius)
            return 0.92D;
        }
        if (entity instanceof CargoMinecartEntity) {
            // Flatbed logging / cargo cart couplers extend to 14px = 0.88m
            return 0.88D;
        }
        if (entity instanceof LiquidMinecartEntity) {
            // Compact short tank car couplers extend to ~86px * 0.14 = 12px = 0.75m
            return 0.75D;
        }
        if (entity instanceof TunnelBoreEntity) {
            // Tunnel bore rear bumper anchor
            return 4.16D;
        }
        // Vanilla minecarts are 0.98m long -> 0.49m (~0.50m) radius
        if (entity instanceof AbstractMinecart) {
            return 0.50D;
        }
        return 0.60D;
    }

    /**
     * Returns the exact directional bumper radius (center to bumper) of a train vehicle toward a given offset vector.
     */
    public static double getDirectionalCouplerRadius(Entity entity, double dirX, double dirZ) {
        if (entity instanceof DieselLocomotiveEntity diesel) {
            Direction facing = diesel.getFacing();
            double dot = dirX * facing.getStepX() + dirZ * facing.getStepZ();
            // Front snow plow extends to -19.5px (1.22m), rear walkway deck to +19.0px (1.19m)
            return dot >= 0 ? 1.22D : 1.19D;
        }
        if (entity instanceof ElectricLocomotiveEntity elec) {
            Direction facing = elec.getFacing();
            double dot = dirX * facing.getStepX() + dirZ * facing.getStepZ();
            // Aerodynamic nose cone extends to -23.3px (1.46m), rear chassis extends to +14px (0.88m)
            return dot >= 0 ? 1.46D : 0.88D;
        }
        return getCartCouplerRadius(entity);
    }

    /**
     * Calculates the target distance between two linked vehicles on the rail.
     */
    public static double getTargetCouplingDistance(Entity prev, Entity next, boolean isBumperAnchor) {
        double dx = next.getX() - prev.getX();
        double dz = next.getZ() - prev.getZ();
        return getTargetCouplingDistance(prev, next, dx, dz);
    }

    public static double getTargetCouplingDistance(Entity prev, Entity next, double prevToNextX, double prevToNextZ) {
        double prevRadius = getDirectionalCouplerRadius(prev, prevToNextX, prevToNextZ);
        double nextRadius = getDirectionalCouplerRadius(next, -prevToNextX, -prevToNextZ);
        return prevRadius + nextRadius + COUPLER_GAP;
    }

    private static final Map<UUID, UUID> CLIENT_PARENT_MAP = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> CLIENT_CHILD_MAP = new ConcurrentHashMap<>();

    private static @Nullable Method EXTERNAL_GET_PARENT = null;
    private static boolean CHECKED_EXTERNAL_GET_PARENT = false;
    private static @Nullable Method EXTERNAL_GET_CHILD = null;
    private static boolean CHECKED_EXTERNAL_GET_CHILD = false;

    public static @Nullable UUID getCartParentUUID(Entity entity) {
        if (entity == null) return null;

        var nbt = entity.getPersistentData();
        if (nbt.contains(TAG_PARENT_CART)) {
            try {
                return nbt.getString(TAG_PARENT_CART).map(UUID::fromString).orElse(null);
            } catch (Throwable ignored) {}
        }

        UUID mapped = CLIENT_PARENT_MAP.get(entity.getUUID());
        if (mapped != null) {
            return mapped;
        }

        if (!CHECKED_EXTERNAL_GET_PARENT) {
            CHECKED_EXTERNAL_GET_PARENT = true;
            try {
                EXTERNAL_GET_PARENT = entity.getClass().getMethod("getParentUUID");
            } catch (Throwable ignored) {}
        }
        if (EXTERNAL_GET_PARENT != null) {
            try {
                return (UUID) EXTERNAL_GET_PARENT.invoke(entity);
            } catch (Throwable ignored) {}
        }

        return null;
    }

    public static @Nullable UUID getCartChildUUID(Entity entity) {
        if (entity == null) return null;

        var nbt = entity.getPersistentData();
        if (nbt.contains(TAG_CHILD_CART)) {
            try {
                return nbt.getString(TAG_CHILD_CART).map(UUID::fromString).orElse(null);
            } catch (Throwable ignored) {}
        }

        UUID mapped = CLIENT_CHILD_MAP.get(entity.getUUID());
        if (mapped != null) {
            return mapped;
        }

        if (!CHECKED_EXTERNAL_GET_CHILD) {
            CHECKED_EXTERNAL_GET_CHILD = true;
            try {
                EXTERNAL_GET_CHILD = entity.getClass().getMethod("getChildUUID");
            } catch (Throwable ignored) {}
        }
        if (EXTERNAL_GET_CHILD != null) {
            try {
                return (UUID) EXTERNAL_GET_CHILD.invoke(entity);
            } catch (Throwable ignored) {}
        }

        return null;
    }

    public static void setCartParent(AbstractMinecart cart, @Nullable UUID parentUUID, Level level) {
        var nbt = cart.getPersistentData();
        if (parentUUID != null) {
            nbt.putString(TAG_PARENT_CART, parentUUID.toString());
        } else {
            nbt.remove(TAG_PARENT_CART);
        }

        try {
            Method m = cart.getClass().getMethod("setParentUUID", UUID.class);
            m.invoke(cart, parentUUID);
        } catch (Throwable ignored) {}

        if (!level.isClientSide()) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    cart,
                    new TrainLinkSyncPayload(
                            cart.getUUID(),
                            Optional.ofNullable(parentUUID),
                            Optional.ofNullable(getCartChildUUID(cart))
                    )
            );
        } else {
            if (parentUUID != null) {
                CLIENT_PARENT_MAP.put(cart.getUUID(), parentUUID);
            } else {
                CLIENT_PARENT_MAP.remove(cart.getUUID());
            }
        }
    }

    public static void setCartChild(AbstractMinecart cart, @Nullable UUID childUUID, Level level) {
        var nbt = cart.getPersistentData();
        if (childUUID != null) {
            nbt.putString(TAG_CHILD_CART, childUUID.toString());
        } else {
            nbt.remove(TAG_CHILD_CART);
        }

        try {
            Method m = cart.getClass().getMethod("setChildUUID", UUID.class);
            m.invoke(cart, childUUID);
        } catch (Throwable ignored) {}

        if (!level.isClientSide()) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    cart,
                    new TrainLinkSyncPayload(
                            cart.getUUID(),
                            Optional.ofNullable(getCartParentUUID(cart)),
                            Optional.ofNullable(childUUID)
                    )
            );
        } else {
            if (childUUID != null) {
                CLIENT_CHILD_MAP.put(cart.getUUID(), childUUID);
            } else {
                CLIENT_CHILD_MAP.remove(cart.getUUID());
            }
        }
    }

    public static void updateClientLink(UUID cartUUID, @Nullable UUID parentUUID, @Nullable UUID childUUID) {
        if (parentUUID != null) {
            CLIENT_PARENT_MAP.put(cartUUID, parentUUID);
        } else {
            CLIENT_PARENT_MAP.remove(cartUUID);
        }

        if (childUUID != null) {
            CLIENT_CHILD_MAP.put(cartUUID, childUUID);
        } else {
            CLIENT_CHILD_MAP.remove(cartUUID);
        }

        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null) {
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity.getUUID().equals(cartUUID)) {
                        var nbt = entity.getPersistentData();
                        if (parentUUID != null) {
                            nbt.putString(TAG_PARENT_CART, parentUUID.toString());
                        } else {
                            nbt.remove(TAG_PARENT_CART);
                        }
                        if (childUUID != null) {
                            nbt.putString(TAG_CHILD_CART, childUUID.toString());
                        } else {
                            nbt.remove(TAG_CHILD_CART);
                        }
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isChainedTo(Entity a, Entity b) {
        if (!(a instanceof AbstractMinecart) || !(b instanceof AbstractMinecart)) {
            return false;
        }
        UUID aId = a.getUUID();
        UUID bId = b.getUUID();
        if (aId.equals(bId)) return false;

        UUID aP = getCartParentUUID(a);
        UUID aC = getCartChildUUID(a);
        if (bId.equals(aP) || bId.equals(aC)) return true;

        UUID bP = getCartParentUUID(b);
        UUID bC = getCartChildUUID(b);
        return aId.equals(bP) || aId.equals(bC);
    }

    public static boolean isConnectedInSameTrain(Entity a, Entity b) {
        if (!(a instanceof AbstractMinecart) || !(b instanceof AbstractMinecart)) return false;
        if (a.getUUID().equals(b.getUUID())) return true;

        Set<UUID> visited = new HashSet<>();
        Deque<UUID> queue = new ArrayDeque<>();
        visited.add(a.getUUID());

        UUID ap = getCartParentUUID(a);
        if (ap != null) queue.add(ap);
        UUID ac = getCartChildUUID(a);
        if (ac != null) queue.add(ac);

        while (!queue.isEmpty() && visited.size() < 100) {
            UUID next = queue.poll();
            if (next == null) continue;
            if (next.equals(b.getUUID())) return true;
            if (visited.add(next)) {
                Entity ent = a.level().getEntity(next);
                if (ent instanceof AbstractMinecart cart && !cart.isRemoved()) {
                    UUID p = getCartParentUUID(cart);
                    if (p != null && !visited.contains(p)) queue.add(p);
                    UUID c = getCartChildUUID(cart);
                    if (c != null && !visited.contains(c)) queue.add(c);
                }
            }
        }
        return false;
    }

    public static @Nullable AbstractMinecart getTrainTail(AbstractMinecart cart) {
        if (cart == null) return null;
        AbstractMinecart current = cart;
        Set<UUID> visited = new HashSet<>();
        visited.add(current.getUUID());
        Level level = current.level();

        while (true) {
            UUID childUUID = getCartChildUUID(current);
            if (childUUID == null || !visited.add(childUUID)) break;
            Entity childEntity = level.getEntity(childUUID);
            if (childEntity instanceof AbstractMinecart childCart && !childCart.isRemoved()) {
                current = childCart;
            } else {
                break;
            }
        }
        return current;
    }

    public static @Nullable AbstractMinecart getTrainHead(AbstractMinecart cart) {
        if (cart == null) return null;
        AbstractMinecart current = cart;
        Set<UUID> visited = new HashSet<>();
        visited.add(current.getUUID());
        Level level = current.level();

        while (true) {
            UUID parentUUID = getCartParentUUID(current);
            if (parentUUID == null || !visited.add(parentUUID)) break;
            Entity parentEntity = level.getEntity(parentUUID);
            if (parentEntity instanceof AbstractMinecart parentCart && !parentCart.isRemoved()) {
                current = parentCart;
            } else {
                break;
            }
        }
        return current;
    }

    /**
     * Reverses the parent/child coupler polarity of a connected chain of carts so two train segments
     * with matching free couplers (e.g., two tails or two heads) can couple together seamlessly
     * without ever disconnecting existing neighbors.
     */
    public static void reverseChainOrientation(AbstractMinecart startCart, Level level) {
        if (startCart == null || level == null) return;

        List<AbstractMinecart> chain = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        Deque<AbstractMinecart> queue = new ArrayDeque<>();
        queue.add(startCart);
        visited.add(startCart.getUUID());

        while (!queue.isEmpty() && chain.size() < 100) {
            AbstractMinecart curr = queue.poll();
            chain.add(curr);
            UUID pId = getCartParentUUID(curr);
            if (pId != null && visited.add(pId)) {
                Entity pEnt = level.getEntity(pId);
                if (pEnt instanceof AbstractMinecart pCart && !pCart.isRemoved()) {
                    queue.add(pCart);
                }
            }
            UUID cId = getCartChildUUID(curr);
            if (cId != null && visited.add(cId)) {
                Entity cEnt = level.getEntity(cId);
                if (cEnt instanceof AbstractMinecart cCart && !cCart.isRemoved()) {
                    queue.add(cCart);
                }
            }
        }

        Map<UUID, UUID> oldParents = new HashMap<>();
        Map<UUID, UUID> oldChildren = new HashMap<>();
        for (AbstractMinecart c : chain) {
            oldParents.put(c.getUUID(), getCartParentUUID(c));
            oldChildren.put(c.getUUID(), getCartChildUUID(c));
        }

        for (AbstractMinecart c : chain) {
            UUID oldP = oldParents.get(c.getUUID());
            UUID oldC = oldChildren.get(c.getUUID());
            setCartParent(c, oldC, level);
            setCartChild(c, oldP, level);
        }
    }

    public static @Nullable Pair<AbstractMinecart, AbstractMinecart> determineRoles(AbstractMinecart first, AbstractMinecart second) {
        if (first == null || second == null || first.getUUID().equals(second.getUUID())) {
            return null;
        }
        if (isConnectedInSameTrain(first, second)) {
            return null;
        }

        UUID firstParent = getCartParentUUID(first);
        UUID firstChild = getCartChildUUID(first);
        UUID secondParent = getCartParentUUID(second);
        UUID secondChild = getCartChildUUID(second);

        boolean fFrontFree = (firstParent == null);
        boolean fRearFree = (firstChild == null);
        boolean sFrontFree = (secondParent == null);
        boolean sRearFree = (secondChild == null);

        // If a clicked cart has both front and rear couplers already occupied, walk to the nearest free end of its consist
        if (!fFrontFree && !fRearFree) {
            AbstractMinecart tail = getTrainTail(first);
            AbstractMinecart head = getTrainHead(first);
            if (tail != null && getCartChildUUID(tail) == null) {
                first = tail;
                fFrontFree = (getCartParentUUID(first) == null);
                fRearFree = true;
            } else if (head != null && getCartParentUUID(head) == null) {
                first = head;
                fFrontFree = true;
                fRearFree = (getCartChildUUID(first) == null);
            } else {
                return null;
            }
        }

        if (!sFrontFree && !sRearFree) {
            AbstractMinecart head = getTrainHead(second);
            AbstractMinecart tail = getTrainTail(second);
            if (head != null && getCartParentUUID(head) == null) {
                second = head;
                sFrontFree = true;
                sRearFree = (getCartChildUUID(second) == null);
            } else if (tail != null && getCartChildUUID(tail) == null) {
                second = tail;
                sFrontFree = (getCartParentUUID(second) == null);
                sRearFree = true;
            } else {
                return null;
            }
        }

        boolean firstIsLead = isLeadCart(first);
        boolean secondIsLead = isLeadCart(second);

        // If both carts are completely unlinked locomotives, orient by facing
        if (firstIsLead && secondIsLead && fFrontFree && fRearFree && sFrontFree && sRearFree) {
            Direction facing = (first instanceof BasicLocomotiveEntity bl) ? bl.getFacing()
                    : (first instanceof DieselLocomotiveEntity dl) ? dl.getFacing()
                    : (first instanceof ElectricLocomotiveEntity el) ? el.getFacing()
                    : Direction.NORTH;
            double dx = second.getX() - first.getX();
            double dz = second.getZ() - first.getZ();
            double dot = dx * facing.getStepX() + dz * facing.getStepZ();
            if (dot < 0.0) {
                return Pair.of(first, second);
            } else {
                return Pair.of(second, first);
            }
        }

        // If second is an unlinked locomotive and first is unlinked rolling stock, put locomotive in front
        if (secondIsLead && !firstIsLead && sRearFree && fFrontFree) {
            return Pair.of(second, first);
        }

        // Connect directly using available front (parent) and rear (child) connection points without ever overwriting occupied couplers
        if (fRearFree && sFrontFree) {
            return Pair.of(first, second);
        }
        if (sRearFree && fFrontFree) {
            return Pair.of(second, first);
        }

        // Both have only rear couplers free (two train tails): reverse second's chain so its front coupler is free
        if (fRearFree && sRearFree) {
            reverseChainOrientation(second, first.level());
            return Pair.of(first, second);
        }

        // Both have only front couplers free (two train heads): reverse first's chain so its rear coupler is free
        if (fFrontFree && sFrontFree) {
            reverseChainOrientation(first, first.level());
            return Pair.of(first, second);
        }

        return null;
    }

    public static boolean isLeadCart(AbstractMinecart cart) {
        return cart instanceof BasicLocomotiveEntity
                || cart instanceof DieselLocomotiveEntity
                || cart instanceof ElectricLocomotiveEntity
                || cart instanceof TunnelBoreEntity;
    }

    public static boolean isTailCart(AbstractMinecart cart) {
        return false;
    }

    public static boolean linkCarts(AbstractMinecart parent, AbstractMinecart child, ServerLevel level) {
        if (parent == null || child == null) return false;
        if (parent.getUUID().equals(child.getUUID())) return false;

        UUID currentParentChild = getCartChildUUID(parent);
        UUID currentChildParent = getCartParentUUID(child);
        if (child.getUUID().equals(currentParentChild) && parent.getUUID().equals(currentChildParent)) {
            return true;
        }

        if (isConnectedInSameTrain(parent, child)) {
            return false;
        }

        // Never overwrite an already-occupied front or rear coupler on either cart
        if (currentParentChild != null && !currentParentChild.equals(child.getUUID())) {
            Entity existingChild = level.getEntity(currentParentChild);
            if (existingChild instanceof AbstractMinecart existingCart && !existingCart.isRemoved()) {
                return false;
            }
        }

        if (currentChildParent != null && !currentChildParent.equals(parent.getUUID())) {
            Entity existingParent = level.getEntity(currentChildParent);
            if (existingParent instanceof AbstractMinecart existingCart && !existingCart.isRemoved()) {
                return false;
            }
        }

        setCartChild(parent, child.getUUID(), level);
        setCartParent(child, parent.getUUID(), level);

        try {
            Class<?> chainableClass = Class.forName("org.fuseleaf.minecarttrainsfork.chaining.Chainable");
            Method setParentChild = chainableClass.getMethod("setChainedParentChild", chainableClass, chainableClass);
            setParentChild.invoke(null, parent, child);
        } catch (Throwable ignored) {}

        return true;
    }
    public static boolean unlinkCart(AbstractMinecart cart, ServerLevel level) {
        if (cart == null) return false;

        UUID parentId = getCartParentUUID(cart);
        UUID childId = getCartChildUUID(cart);
        boolean hadLinks = (parentId != null || childId != null);

        if (parentId != null) {
            Entity pEnt = level.getEntity(parentId);
            if (pEnt instanceof AbstractMinecart pCart) {
                setCartChild(pCart, null, level);
            }
        }

        if (childId != null) {
            Entity cEnt = level.getEntity(childId);
            if (cEnt instanceof AbstractMinecart cCart) {
                setCartParent(cCart, null, level);
            }
        }

        setCartParent(cart, null, level);
        setCartChild(cart, null, level);

        try {
            Class<?> chainableClass = Class.forName("org.fuseleaf.minecarttrainsfork.chaining.Chainable");
            Class<?> connClass = Class.forName("org.fuseleaf.minecarttrainsfork.chaining.Connection");
            Method unlinkMethod = connClass.getMethod("unlink", chainableClass, ServerLevel.class);
            unlinkMethod.invoke(null, cart, level);
            hadLinks = true;
        } catch (Throwable ignored) {}

        return hadLinks;
    }

    public static class TrainConsistStats {
        public final int totalCapacity;
        public final int totalLoad;
        public final int activeLocomotives;
        public final double speedFactor;

        public TrainConsistStats(int totalCapacity, int totalLoad, int activeLocomotives, double speedFactor) {
            this.totalCapacity = totalCapacity;
            this.totalLoad = totalLoad;
            this.activeLocomotives = activeLocomotives;
            this.speedFactor = speedFactor;
        }
    }

    public static TrainConsistStats calculateConsistStats(AbstractMinecart startCart) {
        if (startCart == null || startCart.level() == null) {
            return new TrainConsistStats(10, 0, 1, 1.0D);
        }
        Level level = startCart.level();
        AbstractMinecart head = startCart;
        Set<UUID> visited = new HashSet<>();
        visited.add(head.getUUID());

        while (true) {
            UUID parentUUID = getCartParentUUID(head);
            if (parentUUID == null || !visited.add(parentUUID)) break;
            Entity parentEntity = level.getEntity(parentUUID);
            if (parentEntity instanceof AbstractMinecart parentCart) {
                head = parentCart;
            } else {
                break;
            }
        }

        int capacity = 0;
        int load = 0;
        int activeLocos = 0;
        visited.clear();

        AbstractMinecart current = head;
        while (current != null && visited.add(current.getUUID())) {
            if (current instanceof BasicLocomotiveEntity loco) {
                if (loco.isRunning() && loco.hasFuel()) {
                    capacity += Config.STEAM_LOCOMOTIVE_PULL_CAPACITY.get();
                    activeLocos++;
                } else {
                    load++;
                }
            } else if (current instanceof DieselLocomotiveEntity loco) {
                if (loco.isRunning() && loco.hasFuel()) {
                    capacity += Config.DIESEL_LOCOMOTIVE_PULL_CAPACITY.get();
                    activeLocos++;
                } else {
                    load++;
                }
            } else if (current instanceof ElectricLocomotiveEntity loco) {
                if (loco.isRunning()) {
                    capacity += Config.ELECTRIC_LOCOMOTIVE_PULL_CAPACITY.get();
                    activeLocos++;
                } else {
                    load++;
                }
            } else {
                load++;
            }

            UUID childUUID = getCartChildUUID(current);
            if (childUUID == null || visited.contains(childUUID)) break;
            Entity childEntity = level.getEntity(childUUID);
            if (childEntity instanceof AbstractMinecart childCart) {
                current = childCart;
            } else {
                break;
            }
        }

        double speedFactor;
        if (load <= capacity) {
            speedFactor = 1.0D;
        } else if (capacity <= 0) {
            speedFactor = 0.0D;
        } else {
            double ratio = (double) capacity / (double) load;
            if (ratio < 0.25D) {
                speedFactor = 0.0D; // Overload stall
            } else {
                speedFactor = ratio;
            }
        }

        return new TrainConsistStats(capacity, load, activeLocos, speedFactor);
    }

    public static void syncTrainConsist(AbstractMinecart sourceLoco, boolean running, int speedLevel, boolean reversed) {
        if (sourceLoco == null || sourceLoco.level() == null || sourceLoco.level().isClientSide()) return;
        Level level = sourceLoco.level();

        Set<UUID> visited = new HashSet<>();
        Deque<UUID> queue = new ArrayDeque<>();
        queue.add(sourceLoco.getUUID());
        visited.add(sourceLoco.getUUID());

        while (!queue.isEmpty() && visited.size() < 100) {
            UUID next = queue.poll();
            Entity ent = level.getEntity(next);
            if (ent instanceof AbstractMinecart cart) {
                if (cart != sourceLoco) {
                    if (cart instanceof BasicLocomotiveEntity loco) {
                        loco.setRunning(running);
                        loco.setSpeedLevel(speedLevel);
                        loco.setReversed(reversed);
                    } else if (cart instanceof DieselLocomotiveEntity loco) {
                        loco.setRunning(running);
                        loco.setSpeedLevel(speedLevel);
                        loco.setReversed(reversed);
                    } else if (cart instanceof ElectricLocomotiveEntity loco) {
                        loco.setRunning(running);
                        loco.setSpeedLevel(speedLevel);
                        loco.setReversed(reversed);
                    }
                }

                UUID p = getCartParentUUID(cart);
                if (p != null && visited.add(p)) queue.add(p);
                UUID c = getCartChildUUID(cart);
                if (c != null && visited.add(c)) queue.add(c);
            }
        }
    }

    public static boolean hasActiveLeadLocomotiveAhead(AbstractMinecart startCart) {
        if (startCart == null || startCart.level() == null) return false;
        Level level = startCart.level();
        Set<UUID> visited = new HashSet<>();
        visited.add(startCart.getUUID());
        UUID p = getCartParentUUID(startCart);
        while (p != null && visited.add(p)) {
            Entity ent = level.getEntity(p);
            if (ent instanceof BasicLocomotiveEntity loco && loco.isRunning() && loco.hasFuel()) {
                return true;
            }
            if (ent instanceof DieselLocomotiveEntity loco && loco.isRunning() && loco.hasFuel()) {
                return true;
            }
            if (ent instanceof ElectricLocomotiveEntity loco && loco.isRunning()) {
                return true;
            }
            if (ent instanceof AbstractMinecart cart) {
                p = getCartParentUUID(cart);
            } else {
                break;
            }
        }
        return false;
    }

    public static boolean isConsistWithLocomotive(AbstractMinecart cart) {
        if (cart == null || cart.level() == null) return false;
        if (cart instanceof BasicLocomotiveEntity || cart instanceof DieselLocomotiveEntity
                || cart instanceof ElectricLocomotiveEntity || cart instanceof TunnelBoreEntity) {
            return true;
        }
        return BasicLocomotiveEntity.getConnectedLocomotive(cart) != null
                || DieselLocomotiveEntity.getConnectedLocomotive(cart) != null
                || ElectricLocomotiveEntity.getConnectedLocomotive(cart) != null;
    }

    public static boolean isConnectedToAnyTrain(AbstractMinecart cart) {
        if (cart == null || cart.level() == null) return false;
        if (cart instanceof BasicLocomotiveEntity || cart instanceof DieselLocomotiveEntity
                || cart instanceof ElectricLocomotiveEntity || cart instanceof TunnelBoreEntity) {
            return true;
        }
        return getCartParentUUID(cart) != null || getCartChildUUID(cart) != null;
    }

    public static boolean isPartOfRunningTrain(AbstractMinecart cart) {
        if (cart == null || cart.level() == null) return false;
        if (cart instanceof BasicLocomotiveEntity loco && loco.isRunning()) return true;
        if (cart instanceof DieselLocomotiveEntity loco && loco.isRunning()) return true;
        if (cart instanceof ElectricLocomotiveEntity loco && loco.isRunning()) return true;
        if (cart instanceof TunnelBoreEntity bore && bore.isRunning()) return true;

        BasicLocomotiveEntity steam = BasicLocomotiveEntity.getConnectedLocomotive(cart);
        if (steam != null && steam.isRunning()) return true;
        DieselLocomotiveEntity diesel = DieselLocomotiveEntity.getConnectedLocomotive(cart);
        if (diesel != null && diesel.isRunning()) return true;
        ElectricLocomotiveEntity elec = ElectricLocomotiveEntity.getConnectedLocomotive(cart);
        if (elec != null && elec.isRunning()) return true;
        return false;
    }

    public static boolean canCollideWith(AbstractMinecart cart, Entity entity) {
        if (isConnectedInSameTrain(cart, entity)) {
            return false;
        }
        return !entity.isPassengerOfSameVehicle(cart);
    }

    public static boolean canBeCollidedWith(AbstractMinecart cart, @Nullable Entity other) {
        if (other != null && isConnectedInSameTrain(cart, other)) {
            return false;
        }
        return !cart.isRemoved();
    }

    public static boolean isCartPushable(AbstractMinecart cart) {
        if (cart instanceof BasicLocomotiveEntity loco) return !loco.isRunning();
        if (cart instanceof DieselLocomotiveEntity loco) return !loco.isRunning();
        if (cart instanceof ElectricLocomotiveEntity loco) return !loco.isRunning();
        if (cart instanceof TunnelBoreEntity bore) return !bore.isRunning();
        return !isPartOfRunningTrain(cart);
    }

    public static double getLocomotiveSpeed(AbstractMinecart loco) {
        if (loco instanceof BasicLocomotiveEntity bl) return bl.getLocomotiveSpeed();
        if (loco instanceof DieselLocomotiveEntity dl) return dl.getLocomotiveSpeed();
        if (loco instanceof ElectricLocomotiveEntity el) return el.getLocomotiveSpeed();
        return 0.4D;
    }

    public static boolean isLocomotiveActive(AbstractMinecart loco) {
        if (loco instanceof BasicLocomotiveEntity bl) return bl.isActive();
        if (loco instanceof DieselLocomotiveEntity dl) return dl.isActive();
        if (loco instanceof ElectricLocomotiveEntity el) return el.isActive();
        return false;
    }

    public static boolean isLocomotiveRunning(AbstractMinecart loco) {
        if (loco instanceof BasicLocomotiveEntity bl) return bl.isRunning();
        if (loco instanceof DieselLocomotiveEntity dl) return dl.isRunning();
        if (loco instanceof ElectricLocomotiveEntity el) return el.isRunning();
        return false;
    }

    public static boolean isLocomotiveReversed(AbstractMinecart loco) {
        if (loco instanceof BasicLocomotiveEntity bl) return bl.isReversed();
        if (loco instanceof DieselLocomotiveEntity dl) return dl.isReversed();
        if (loco instanceof ElectricLocomotiveEntity el) return el.isReversed();
        return false;
    }

    public static int getLocomotiveSpeedLevel(AbstractMinecart loco) {
        if (loco instanceof BasicLocomotiveEntity bl) return bl.getSpeedLevel();
        if (loco instanceof DieselLocomotiveEntity dl) return dl.getSpeedLevel();
        if (loco instanceof ElectricLocomotiveEntity el) return el.getSpeedLevel();
        return 3;
    }

    public static Direction getLocomotiveTravelDirection(AbstractMinecart loco) {
        if (loco instanceof BasicLocomotiveEntity bl) return bl.getTravelDirection();
        if (loco instanceof DieselLocomotiveEntity dl) return dl.getTravelDirection();
        if (loco instanceof ElectricLocomotiveEntity el) return el.getTravelDirection();
        return loco.getMotionDirection();
    }

    public static void stopLocomotive(AbstractMinecart loco) {
        if (loco instanceof BasicLocomotiveEntity bl) bl.stopLocomotiveFromRail();
        else if (loco instanceof DieselLocomotiveEntity dl) dl.stopLocomotiveFromRail();
        else if (loco instanceof ElectricLocomotiveEntity el) el.stopLocomotiveFromRail();
        else loco.setDeltaMovement(Vec3.ZERO);
    }

    private static final Map<UUID, Long> CONSIST_TICK_MAP = new HashMap<>();
    private static final Map<UUID, Long> POST_BUMP_BRAKE_UNTIL = new HashMap<>();

    public static boolean isCartOnUnpoweredBrakingRail(Entity entity) {
        if (entity == null || entity.level() == null) return false;
        BlockPos railPos = getCurrentBlockPosOrRailBelow(entity);
        BlockState state = entity.level().getBlockState(railPos);
        if (state.is(MiningTools.BRAKING_RAIL.get())) {
            return !state.getValue(PoweredRailBlock.POWERED);
        }
        if (state.is(MiningTools.CONTROL_RAIL.get())
                && (entity instanceof BasicLocomotiveEntity || entity instanceof DieselLocomotiveEntity || entity instanceof ElectricLocomotiveEntity)) {
            return !state.getValue(PoweredRailBlock.POWERED);
        }
        return false;
    }

    public static boolean isConsistHeldByBrakingRail(AbstractMinecart startCart) {
        if (startCart == null || startCart.level() == null) return false;
        if (isCartOnUnpoweredBrakingRail(startCart)) return true;

        Level level = startCart.level();
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> queue = new ArrayDeque<>();
        visited.add(startCart.getUUID());
        UUID p = getCartParentUUID(startCart);
        if (p != null) queue.add(p);
        UUID c = getCartChildUUID(startCart);
        if (c != null) queue.add(c);

        while (!queue.isEmpty() && visited.size() < 100) {
            UUID next = queue.poll();
            if (next != null && visited.add(next)) {
                Entity ent = level.getEntity(next);
                if (ent instanceof AbstractMinecart cart && !cart.isRemoved()) {
                    if (isCartOnUnpoweredBrakingRail(cart)) {
                        return true;
                    }
                    UUID cp = getCartParentUUID(cart);
                    if (cp != null && !visited.contains(cp)) queue.add(cp);
                    UUID cc = getCartChildUUID(cart);
                    if (cc != null && !visited.contains(cc)) queue.add(cc);
                }
            }
        }
        return false;
    }

    public static void freezeConsistFromBrakingRail(AbstractMinecart startCart) {
        if (startCart == null || startCart.level() == null) return;
        Level level = startCart.level();
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> queue = new ArrayDeque<>();
        queue.add(startCart.getUUID());

        while (!queue.isEmpty() && visited.size() < 100) {
            UUID next = queue.poll();
            if (next != null && visited.add(next)) {
                Entity ent = level.getEntity(next);
                if (ent instanceof AbstractMinecart cart && !cart.isRemoved()) {
                    cart.setDeltaMovement(Vec3.ZERO);
                    if (cart instanceof BasicLocomotiveEntity bl) {
                        bl.stopLocomotiveFromRail();
                    } else if (cart instanceof DieselLocomotiveEntity dl) {
                        dl.stopLocomotiveFromRail();
                    } else if (cart instanceof ElectricLocomotiveEntity el) {
                        el.stopLocomotiveFromRail();
                    } else if (cart instanceof TunnelBoreEntity bore && bore.isRunning()) {
                        bore.setRunning(false);
                    }
                    UUID cp = getCartParentUUID(cart);
                    if (cp != null && !visited.contains(cp)) queue.add(cp);
                    UUID cc = getCartChildUUID(cart);
                    if (cc != null && !visited.contains(cc)) queue.add(cc);
                }
            }
        }
    }

    public static void releaseConsistFromBrakingRail(AbstractMinecart startCart) {
        if (startCart == null || startCart.level() == null || startCart.level().isClientSide()) return;
        if (isConsistHeldByBrakingRail(startCart)) {
            return;
        }
        Level level = startCart.level();
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> queue = new ArrayDeque<>();
        queue.add(startCart.getUUID());

        boolean startedLoco = false;
        List<AbstractMinecart> allCarts = new ArrayList<>();

        while (!queue.isEmpty() && visited.size() < 100) {
            UUID next = queue.poll();
            if (next != null && visited.add(next)) {
                Entity ent = level.getEntity(next);
                if (ent instanceof AbstractMinecart cart && !cart.isRemoved()) {
                    allCarts.add(cart);
                    if (cart instanceof BasicLocomotiveEntity bl) {
                        if (!bl.isRunning()) {
                            bl.startLocomotiveFromRail();
                        }
                        if (bl.isRunning()) startedLoco = true;
                    } else if (cart instanceof DieselLocomotiveEntity dl) {
                        if (!dl.isRunning()) {
                            dl.startLocomotiveFromRail();
                        }
                        if (dl.isRunning()) startedLoco = true;
                    } else if (cart instanceof ElectricLocomotiveEntity el) {
                        if (!el.isRunning()) {
                            el.startLocomotiveFromRail();
                        }
                        if (el.isRunning()) startedLoco = true;
                    }
                    UUID cp = getCartParentUUID(cart);
                    if (cp != null && !visited.contains(cp)) queue.add(cp);
                    UUID cc = getCartChildUUID(cart);
                    if (cc != null && !visited.contains(cc)) queue.add(cc);
                }
            }
        }

        if (!startedLoco) {
            // Standalone cart or unpowered consist: give a gentle kick along its current facing if stationary
            for (AbstractMinecart cart : allCarts) {
                if (cart.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D) {
                    Vec3 heading = getCartHeadingVector(cart, 0.0D, 0.0D);
                    cart.setDeltaMovement(heading.x * 0.08D, cart.getDeltaMovement().y, heading.z * 0.08D);
                    cart.needsSync = true;
                }
            }
        }
    }

    public static String getCartIndividualName(Entity cart) {
        if (cart == null) return "";
        if (cart instanceof BasicLocomotiveEntity bl) return bl.getTrainName();
        if (cart instanceof DieselLocomotiveEntity dl) return dl.getTrainName();
        if (cart instanceof ElectricLocomotiveEntity el) return el.getTrainName();
        var custom = cart.getCustomName();
        return custom != null ? custom.getString().trim() : "";
    }

    public static boolean matchesCartNameFilter(Entity cart, String whitelistCsv, String blacklistCsv) {
        if (cart == null) return false;
        String cartName = getCartIndividualName(cart).toLowerCase(java.util.Locale.ROOT);
        String trainName = BasicLocomotiveEntity.resolveTrainName(cart).toLowerCase(java.util.Locale.ROOT);

        if (blacklistCsv != null && !blacklistCsv.isBlank()) {
            for (String token : blacklistCsv.split(",")) {
                String pat = token.trim().toLowerCase(java.util.Locale.ROOT);
                if (!pat.isEmpty()) {
                    if ((!cartName.isEmpty() && cartName.contains(pat))
                            || (!trainName.isEmpty() && trainName.contains(pat))) {
                        return false;
                    }
                }
            }
        }

        if (whitelistCsv != null && !whitelistCsv.isBlank()) {
            boolean anyPattern = false;
            for (String token : whitelistCsv.split(",")) {
                String pat = token.trim().toLowerCase(java.util.Locale.ROOT);
                if (!pat.isEmpty()) {
                    anyPattern = true;
                    if ((!cartName.isEmpty() && cartName.contains(pat))
                            || (!trainName.isEmpty() && trainName.contains(pat))) {
                        return true;
                    }
                }
            }
            if (anyPattern) {
                return false;
            }
        }

        return true;
    }

    public static Vec3 applyRollingStockSlowdown(AbstractMinecart cart, Vec3 deltaMovement) {
        if (isCartOnUnpoweredBrakingRail(cart) || isConsistHeldByBrakingRail(cart)) {
            cart.setDeltaMovement(Vec3.ZERO);
            return Vec3.ZERO;
        }
        if (isPartOfRunningTrain(cart) || isConsistWithLocomotive(cart)) {
            cart.setDeltaMovement(Vec3.ZERO);
            return Vec3.ZERO;
        }

        if (cart.level() != null && !cart.level().isClientSide()) {
            long gameTime = cart.level().getGameTime();
            Long brakeUntil = POST_BUMP_BRAKE_UNTIL.get(cart.getUUID());
            if (brakeUntil != null) {
                if (gameTime <= brakeUntil) {
                    Vec3 braked = deltaMovement.multiply(0.50D, 0.0D, 0.50D);
                    if (braked.horizontalDistanceSqr() < 4.0E-4D) {
                        POST_BUMP_BRAKE_UNTIL.remove(cart.getUUID());
                        cart.setDeltaMovement(Vec3.ZERO);
                        return Vec3.ZERO;
                    }
                    return braked;
                } else {
                    POST_BUMP_BRAKE_UNTIL.remove(cart.getUUID());
                }
            }
        }

        double drag = Math.min(Config.ROLLING_STOCK_DRAG_COEFFICIENT.get(), 0.88D);
        if (cart.isInWater()) {
            drag *= 0.80D;
        }

        Vec3 slowed = deltaMovement.multiply(drag, 0.0D, drag);
        if (slowed.horizontalDistanceSqr() < 2.5E-4D) {
            return Vec3.ZERO;
        }
        return slowed;
    }

    public static void markConsistPostBumpBrake(AbstractMinecart cart, long durationTicks) {
        if (cart == null || cart.level() == null || cart.level().isClientSide()) return;
        long until = cart.level().getGameTime() + durationTicks;
        if (POST_BUMP_BRAKE_UNTIL.size() > 100) {
            POST_BUMP_BRAKE_UNTIL.clear();
        }
        POST_BUMP_BRAKE_UNTIL.put(cart.getUUID(), until);
        for (Pair<Entity, AbstractMinecart> link : getOrderedTrainLinks(cart)) {
            POST_BUMP_BRAKE_UNTIL.put(link.getSecond().getUUID(), until);
        }
    }

    public static boolean shouldSkipVanillaServerTick(AbstractMinecart cart) {
        if (cart.level().isClientSide()) {
            return false;
        }
        if (isConsistWithLocomotive(cart) || isPartOfRunningTrain(cart) || isConsistHeldByBrakingRail(cart)) {
            if (cart.getHurtTime() > 0) {
                cart.setHurtTime(cart.getHurtTime() - 1);
            }
            if (cart.getDamage() > 0.0F) {
                cart.setDamage(cart.getDamage() - 1.0F);
            }
            cart.setDeltaMovement(Vec3.ZERO);
            cart.setOnRails(true);
            Vec3 snap = getRailPosForEntity(cart, cart.getX(), cart.getY(), cart.getZ());
            if (snap != null) {
                cart.setPos(snap.x, snap.y, snap.z);
            }
            return true;
        }
        return false;
    }

    public static void tickConsistMovement(AbstractMinecart cart) {
        if (cart.level().isClientSide() || cart.isRemoved()) return;

        if (isConsistHeldByBrakingRail(cart)) {
            freezeConsistFromBrakingRail(cart);
            return;
        }

        if (isPartOfRunningTrain(cart) || isConsistWithLocomotive(cart)) {
            cart.setDeltaMovement(Vec3.ZERO);
            cart.setOnRails(true);
            Vec3 snap = getRailPosForEntity(cart, cart.getX(), cart.getY(), cart.getZ());
            if (snap != null) {
                cart.setPos(snap.x, snap.y, snap.z);
            }
            return;
        }

        UUID parentUUID = getCartParentUUID(cart);
        UUID childUUID = getCartChildUUID(cart);
        if (parentUUID == null && childUUID == null) {
            return;
        }

        AbstractMinecart head = getTrainHead(cart);
        if (head == null || head.isRemoved()) head = cart;

        long gameTime = cart.level().getGameTime();
        Long lastTick = CONSIST_TICK_MAP.get(head.getUUID());
        if (lastTick != null && lastTick == gameTime) {
            return;
        }
        CONSIST_TICK_MAP.put(head.getUUID(), gameTime);

        if (gameTime % 200 == 0 && CONSIST_TICK_MAP.size() > 50) {
            CONSIST_TICK_MAP.clear();
            CONSIST_TICK_MAP.put(head.getUUID(), gameTime);
        }

        // Find the cart in the consist with the highest active horizontal speed to act as the force driver
        AbstractMinecart driver = head;
        double maxSpeedSq = head.getDeltaMovement().horizontalDistanceSqr();
        Set<UUID> visited = new HashSet<>();
        AbstractMinecart curr = head;
        while (curr != null && visited.add(curr.getUUID())) {
            double spSq = curr.getDeltaMovement().horizontalDistanceSqr();
            if (spSq > maxSpeedSq) {
                maxSpeedSq = spSq;
                driver = curr;
            }
            UUID cId = getCartChildUUID(curr);
            if (cId == null) break;
            Entity cEnt = cart.level().getEntity(cId);
            curr = (cEnt instanceof AbstractMinecart mc && !mc.isRemoved()) ? mc : null;
        }

        Direction dir = driver.getMotionDirection();
        handleChainedCartsMovement(driver, dir);
    }

    public static void pushMinecarts(AbstractMinecart a, AbstractMinecart b) {
        if (a.level().isClientSide() || isConnectedInSameTrain(a, b)) {
            return;
        }

        if (a instanceof BasicLocomotiveEntity || a instanceof DieselLocomotiveEntity || a instanceof ElectricLocomotiveEntity) {
            pushCartFromLocomotiveBumper(a, b);
            return;
        }
        if (b instanceof BasicLocomotiveEntity || b instanceof DieselLocomotiveEntity || b instanceof ElectricLocomotiveEntity) {
            pushCartFromLocomotiveBumper(b, a);
            return;
        }

        double dx = b.getX() - a.getX();
        double dz = b.getZ() - a.getZ();
        double distSq = dx * dx + dz * dz;
        if (distSq < 1.0E-4D) {
            dx = 0.01;
            dz = 0.0;
            distSq = dx * dx;
        }
        double dist = Math.sqrt(distSq);
        double nx = dx / dist;
        double nz = dz / dist;

        double rA = getCartCouplerRadius(a);
        double rB = getCartCouplerRadius(b);
        double minDistance = Math.max(rA + rB - 0.05D, 0.85D);

        if (dist < minDistance) {
            double overlap = minDistance - dist;

            // Physical bumper separation along track without adding artificial velocity via push()
            double sep = overlap * 0.45D;
            a.setPos(a.getX() - nx * sep, a.getY(), a.getZ() - nz * sep);
            b.setPos(b.getX() + nx * sep, b.getY(), b.getZ() + nz * sep);
            Vec3 snapA = getRailPosForEntity(a, a.getX(), a.getY(), a.getZ());
            if (snapA != null) {
                a.setPos(snapA.x, snapA.y, snapA.z);
            }
            Vec3 snapB = getRailPosForEntity(b, b.getX(), b.getY(), b.getZ());
            if (snapB != null) {
                b.setPos(snapB.x, snapB.y, snapB.z);
            }

            Vec3 vA = a.getDeltaMovement();
            Vec3 vB = b.getDeltaMovement();
            double dotA = vA.x * nx + vA.z * nz;
            double dotB = vB.x * nx + vB.z * nz;
            double relSpeed = dotA - dotB; // Closing speed along collision normal

            // Apply post-bump rapid braking to both carts so they come to a stop very shortly after impact
            markConsistPostBumpBrake(a, 25L);
            markConsistPostBumpBrake(b, 25L);

            if (relSpeed > 0.002D) {
                double transferRatio = Math.min(Config.ROLLING_STOCK_BUMP_TRANSFER.get(), 0.25D);
                double damping = Math.min(Config.ROLLING_STOCK_BUMP_DAMPING.get(), 0.30D);

                double impulse = Math.min(relSpeed * transferRatio, 0.04D);

                Vec3 newVA = vA.subtract(nx * impulse, 0.0, nz * impulse).scale(damping);
                Vec3 newVB = vB.add(nx * impulse, 0.0, nz * impulse).scale(damping);

                a.setDeltaMovement(newVA);
                b.setDeltaMovement(newVB);
                a.needsSync = true;
                b.needsSync = true;
            } else {
                a.setDeltaMovement(vA.scale(0.30D));
                b.setDeltaMovement(vB.scale(0.30D));
                a.needsSync = true;
                b.needsSync = true;
            }
        }
    }

    public static void pushCollidingEntity(AbstractMinecart cart, Entity entity) {
        if (cart.level().isClientSide() || entity.noPhysics || cart.hasPassenger(entity)) {
            return;
        }
        if (isConnectedInSameTrain(cart, entity)) {
            return;
        }

        if ((cart instanceof BasicLocomotiveEntity || cart instanceof DieselLocomotiveEntity || cart instanceof ElectricLocomotiveEntity)
                && entity instanceof AbstractMinecart otherCart) {
            pushCartFromLocomotiveBumper(cart, otherCart);
            return;
        }
        if (entity instanceof BasicLocomotiveEntity || entity instanceof DieselLocomotiveEntity || entity instanceof ElectricLocomotiveEntity) {
            pushCartFromLocomotiveBumper((AbstractMinecart) entity, cart);
            return;
        }
        if (entity instanceof AbstractMinecart otherCart) {
            pushMinecarts(cart, otherCart);
            return;
        }

        Vec3 motion = cart.getDeltaMovement();
        double horizSpeed = motion.horizontalDistance();
        boolean isRunningTrain = isPartOfRunningTrain(cart);

        // Loose / unpowered carts: only roll from player interaction when the player is running (sprinting or holding shift)
        if (!isRunningTrain && horizSpeed <= 0.08D) {
            if (entity instanceof Player player) {
                boolean holdingCrowbar = player.getMainHandItem().is(MiningTools.CROWBAR_ITEM.get())
                        || player.getOffhandItem().is(MiningTools.CROWBAR_ITEM.get());
                boolean canPlayerPush = player.isSprinting() || (player.isShiftKeyDown() && !holdingCrowbar);
                if (!canPlayerPush) {
                    return;
                }
                if (isCartOnUnpoweredBrakingRail(cart) || isConsistHeldByBrakingRail(cart)) {
                    return;
                }
                double dx = cart.getX() - player.getX();
                double dz = cart.getZ() - player.getZ();
                double dist = Math.hypot(dx, dz);
                if (dist > 1.0E-4D) {
                    double nx = dx / dist;
                    double nz = dz / dist;
                    double pushSpeed = 0.14D;
                    double targetVx = motion.x * 0.5D + nx * pushSpeed;
                    double targetVz = motion.z * 0.5D + nz * pushSpeed;
                    double maxPush = 0.16D;
                    double mag = Math.hypot(targetVx, targetVz);
                    if (mag > maxPush) {
                        targetVx = (targetVx / mag) * maxPush;
                        targetVz = (targetVz / mag) * maxPush;
                    }
                    cart.setDeltaMovement(targetVx, motion.y, targetVz);
                    cart.needsSync = true;
                }
            }
            return;
        }

        if (entity instanceof LivingEntity living) {
            double dirX;
            double dirZ;
            double speed;
            if (horizSpeed > 0.01D) {
                dirX = motion.x / horizSpeed;
                dirZ = motion.z / horizSpeed;
                speed = horizSpeed;
            } else if (cart instanceof BasicLocomotiveEntity loco && loco.isActive()) {
                dirX = loco.getTravelDirection().getStepX();
                dirZ = loco.getTravelDirection().getStepZ();
                speed = loco.getLocomotiveSpeed();
            } else if (cart instanceof DieselLocomotiveEntity loco && loco.isActive()) {
                dirX = loco.getTravelDirection().getStepX();
                dirZ = loco.getTravelDirection().getStepZ();
                speed = loco.getLocomotiveSpeed();
            } else if (cart instanceof ElectricLocomotiveEntity loco && loco.isActive()) {
                dirX = loco.getTravelDirection().getStepX();
                dirZ = loco.getTravelDirection().getStepZ();
                speed = loco.getLocomotiveSpeed();
            } else {
                double diffX = living.getX() - cart.getX();
                double diffZ = living.getZ() - cart.getZ();
                double dLen = Math.hypot(diffX, diffZ);
                if (dLen > 1.0E-4D) {
                    dirX = diffX / dLen;
                    dirZ = diffZ / dLen;
                } else {
                    dirX = 1.0;
                    dirZ = 0.0;
                }
                speed = 0.1D;
            }

            double lateralX = -dirZ;
            double lateralZ = dirX;
            double lateralDist = (living.getX() - cart.getX()) * lateralX + (living.getZ() - cart.getZ()) * lateralZ;
            double lateralSign = lateralDist >= 0 ? 1.0 : -1.0;

            double pushStrength = Math.max(speed * 2.2D, 0.5D);
            double pushX = dirX * pushStrength + lateralX * 0.4D * lateralSign;
            double pushZ = dirZ * pushStrength + lateralZ * 0.4D * lateralSign;
            double pushY = 0.25D;

            living.setDeltaMovement(pushX, pushY, pushZ);
            living.needsSync = true;
            return;
        }

        if (isRunningTrain || horizSpeed > 0.05D) {
            if (entity.isPushable()) {
                double dirX = horizSpeed > 0.01D ? (motion.x / horizSpeed) : 0.0;
                double dirZ = horizSpeed > 0.01D ? (motion.z / horizSpeed) : 0.0;
                if (cart instanceof BasicLocomotiveEntity loco) {
                    dirX = loco.getTravelDirection().getStepX();
                    dirZ = loco.getTravelDirection().getStepZ();
                } else if (cart instanceof DieselLocomotiveEntity loco) {
                    dirX = loco.getTravelDirection().getStepX();
                    dirZ = loco.getTravelDirection().getStepZ();
                } else if (cart instanceof ElectricLocomotiveEntity loco) {
                    dirX = loco.getTravelDirection().getStepX();
                    dirZ = loco.getTravelDirection().getStepZ();
                }
                double pushStrength = Math.max(horizSpeed, 0.3D);
                entity.setDeltaMovement(dirX * pushStrength, entity.getDeltaMovement().y, dirZ * pushStrength);
                entity.needsSync = true;
            }
            return;
        }
    }

    public static @Nullable BlockPos findRailBlock(Level level, double x, double y, double z) {
        int xt = Mth.floor(x);
        int yt = Mth.floor(y);
        int zt = Mth.floor(z);

        // 1. Vanilla check: if yt - 1 has a rail, cart is on an ascending slope from yt - 1
        BlockPos below = new BlockPos(xt, yt - 1, zt);
        if (BaseRailBlock.isRail(level.getBlockState(below))) {
            return below;
        }

        // 2. Direct block: cart is on rail at yt
        BlockPos current = new BlockPos(xt, yt, zt);
        if (BaseRailBlock.isRail(level.getBlockState(current))) {
            return current;
        }

        // 3. One block above (descending slope or step)
        BlockPos above = new BlockPos(xt, yt + 1, zt);
        if (BaseRailBlock.isRail(level.getBlockState(above))) {
            return above;
        }

        // 4. Multi-block tolerances (+/- 2)
        BlockPos below2 = new BlockPos(xt, yt - 2, zt);
        if (BaseRailBlock.isRail(level.getBlockState(below2))) {
            return below2;
        }
        BlockPos above2 = new BlockPos(xt, yt + 2, zt);
        if (BaseRailBlock.isRail(level.getBlockState(above2))) {
            return above2;
        }

        return null;
    }

    public static @Nullable Vec3 getRailPosForEntity(AbstractMinecart entity, double x, double y, double z) {
        BlockPos railPos = findRailBlock(entity.level(), x, y, z);
        if (railPos == null) {
            return null;
        }

        BlockState state = entity.level().getBlockState(railPos);
        if (!BaseRailBlock.isRail(state)) {
            return null;
        }

        RailShape shape = ((BaseRailBlock) state.getBlock()).getRailDirection(state, entity.level(), railPos, entity);
        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
        Vec3i exit0 = exits.getFirst();
        Vec3i exit1 = exits.getSecond();

        int xt = railPos.getX();
        int yt = railPos.getY();
        int zt = railPos.getZ();

        boolean isCurve = !shape.isSlope() && shape != RailShape.NORTH_SOUTH && shape != RailShape.EAST_WEST;
        if (isCurve) {
            double circleCenterX = (double) xt + 0.5D + (double) (exit0.getX() + exit1.getX()) * 0.5D;
            double circleCenterZ = (double) zt + 0.5D + (double) (exit0.getZ() + exit1.getZ()) * 0.5D;
            double vx = x - circleCenterX;
            double vz = z - circleCenterZ;
            double signX = -(exit0.getX() + exit1.getX());
            double signZ = -(exit0.getZ() + exit1.getZ());
            if (vx * signX < 0.0D) {
                vx = 0.0D;
            }
            if (vz * signZ < 0.0D) {
                vz = 0.0D;
            }
            double dist = Math.sqrt(vx * vx + vz * vz);
            if (dist > 1.0E-5D) {
                x = circleCenterX + (vx / dist) * 0.5D;
                z = circleCenterZ + (vz / dist) * 0.5D;
            }
            y = (double) yt + 0.0625D;
            return new Vec3(x, y, z);
        }

        double x0 = (double) xt + 0.5D + (double) exit0.getX() * 0.5D;
        double y0 = (double) yt + 0.0625D + (double) exit0.getY() * 0.5D;
        double z0 = (double) zt + 0.5D + (double) exit0.getZ() * 0.5D;

        double x1 = (double) xt + 0.5D + (double) exit1.getX() * 0.5D;
        double y1 = (double) yt + 0.0625D + (double) exit1.getY() * 0.5D;
        double z1 = (double) zt + 0.5D + (double) exit1.getZ() * 0.5D;

        double xD = x1 - x0;
        double yD = (y1 - y0) * 2.0D;
        double zD = z1 - z0;

        double progress;
        if (xD == 0.0D) {
            progress = z - (double) zt;
        } else if (zD == 0.0D) {
            progress = x - (double) xt;
        } else {
            double dX = x - x0;
            double dZ = z - z0;
            progress = (dX * xD + dZ * zD) * 2.0D;
        }
        // Do not clamp progress to [0, 1] - clamping snaps carts backward and freezes them at block boundaries
        double resX = x0 + xD * progress;
        double resY = y0 + yD * progress;
        double resZ = z0 + zD * progress;

        if (yD < 0.0D) {
            resY += 1.0D;
        } else if (yD > 0.0D) {
            resY += 0.5D;
        }

        return new Vec3(resX, resY, resZ);
    }

    public static @Nullable Vec3 getRailPosOffs(AbstractMinecart entity, double x, double y, double z, double offset, Direction facing) {
        BlockPos railPos = findRailBlock(entity.level(), x, y, z);
        if (railPos == null) {
            return null;
        }

        BlockState state = entity.level().getBlockState(railPos);
        if (!BaseRailBlock.isRail(state)) {
            return null;
        }

        RailShape shape = ((BaseRailBlock) state.getBlock()).getRailDirection(state, entity.level(), railPos, entity);
        int xt = railPos.getX();
        int yt = railPos.getY();
        int zt = railPos.getZ();

        double currentY = yt;
        if (shape.isSlope()) {
            currentY = yt + 1.0D;
        }

        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
        Vec3i exit0 = exits.getFirst();
        Vec3i exit1 = exits.getSecond();
        boolean isCurve = !shape.isSlope() && shape != RailShape.NORTH_SOUTH && shape != RailShape.EAST_WEST;

        double xD;
        double zD;

        if (isCurve) {
            double circleCenterX = (double) xt + 0.5D + (double) (exit0.getX() + exit1.getX()) * 0.5D;
            double circleCenterZ = (double) zt + 0.5D + (double) (exit0.getZ() + exit1.getZ()) * 0.5D;
            double vx = x - circleCenterX;
            double vz = z - circleCenterZ;
            double dist = Math.sqrt(vx * vx + vz * vz);
            double ux = dist > 1.0E-5D ? vx / dist : 0.70710678D * -(exit0.getX() + exit1.getX());
            double uz = dist > 1.0E-5D ? vz / dist : 0.70710678D * -(exit0.getZ() + exit1.getZ());
            double tX = -uz;
            double tZ = ux;
            boolean movingExit0ToExit1 = (exit0.getX() == -facing.getStepX() && exit0.getZ() == -facing.getStepZ())
                    || (exit1.getX() == facing.getStepX() && exit1.getZ() == facing.getStepZ());
            double exitVecX = (exit1.getX() - exit0.getX()) * (movingExit0ToExit1 ? 1.0D : -1.0D);
            double exitVecZ = (exit1.getZ() - exit0.getZ()) * (movingExit0ToExit1 ? 1.0D : -1.0D);
            if (tX * exitVecX + tZ * exitVecZ < 0.0D) {
                tX = -tX;
                tZ = -tZ;
            }
            xD = tX;
            zD = tZ;
        } else {
            xD = (double) (exit1.getX() - exit0.getX());
            zD = (double) (exit1.getZ() - exit0.getZ());
            double dirDot = xD * facing.getStepX() + zD * facing.getStepZ();
            if (dirDot < 0.0D) {
                xD = -xD;
                zD = -zD;
            }
            double dd = Math.sqrt(xD * xD + zD * zD);
            if (dd > 0.0D) {
                xD /= dd;
                zD /= dd;
            }
        }

        x += xD * offset;
        z += zD * offset;

        if (exit0.getY() != 0 && Mth.floor(x) - xt == exit0.getX() && Mth.floor(z) - zt == exit0.getZ()) {
            currentY += exit0.getY();
        } else if (exit1.getY() != 0 && Mth.floor(x) - xt == exit1.getX() && Mth.floor(z) - zt == exit1.getZ()) {
            currentY += exit1.getY();
        }

        return getRailPosForEntity(entity, x, currentY, z);
    }

    public static class RailPathLink {
        public final double distance;
        public final double tangentX;
        public final double tangentZ;
        public final boolean onRails;

        public RailPathLink(double distance, double tangentX, double tangentZ, boolean onRails) {
            this.distance = distance;
            this.tangentX = tangentX;
            this.tangentZ = tangentZ;
            this.onRails = onRails;
        }
    }

    /**
     * Traces the along-track distance and the normalized tangent vector at `next` pointing away from `prev`.
     */
    public static RailPathLink traceRailLink(Level level, Entity prev, AbstractMinecart next) {
        Vec3 prevPos = prev.position();
        Vec3 nextPos = next.position();
        double diffX = nextPos.x - prevPos.x;
        double diffY = nextPos.y - prevPos.y;
        double diffZ = nextPos.z - prevPos.z;
        double horizChord = Math.hypot(diffX, diffZ);

        BlockPos prevRail = getCurrentBlockPosOrRailBelow(prev);
        BlockPos nextRail = getCurrentBlockPosOrRailBelow(next);
        BlockState prevState = level.getBlockState(prevRail);
        BlockState nextState = level.getBlockState(nextRail);

        if (!BaseRailBlock.isRail(prevState) || !BaseRailBlock.isRail(nextState)) {
            double dirX = horizChord > 1.0E-4D ? diffX / horizChord : 1.0D;
            double dirZ = horizChord > 1.0E-4D ? diffZ / horizChord : 0.0D;
            double dist = Math.sqrt(horizChord * horizChord + diffY * diffY);
            return new RailPathLink(dist, dirX, dirZ, false);
        }

        RailShape prevShape = ((BaseRailBlock) prevState.getBlock()).getRailDirection(prevState, level, prevRail, prev instanceof AbstractMinecart pm ? pm : null);
        RailShape nextShape = ((BaseRailBlock) nextState.getBlock()).getRailDirection(nextState, level, nextRail, next);
        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(nextShape);
        Vec3i exit0 = exits.getFirst();
        Vec3i exit1 = exits.getSecond();
        boolean nextIsCurve = !nextShape.isSlope() && nextShape != RailShape.NORTH_SOUTH && nextShape != RailShape.EAST_WEST;
        boolean prevIsCurve = !prevShape.isSlope() && prevShape != RailShape.NORTH_SOUTH && prevShape != RailShape.EAST_WEST;

        double tX;
        double tZ;
        if (nextIsCurve) {
            double circleCenterX = nextRail.getX() + 0.5 + (exit0.getX() + exit1.getX()) * 0.5;
            double circleCenterZ = nextRail.getZ() + 0.5 + (exit0.getZ() + exit1.getZ()) * 0.5;
            double vx = nextPos.x - circleCenterX;
            double vz = nextPos.z - circleCenterZ;
            double vLen = Math.sqrt(vx * vx + vz * vz);
            double ux = vLen > 1.0E-5 ? vx / vLen : 0.70710678D * -(exit0.getX() + exit1.getX());
            double uz = vLen > 1.0E-5 ? vz / vLen : 0.70710678D * -(exit0.getZ() + exit1.getZ());
            tX = -uz;
            tZ = ux;
        } else {
            double xD = exit1.getX() - exit0.getX();
            double zD = exit1.getZ() - exit0.getZ();
            double length = Math.sqrt(xD * xD + zD * zD);
            tX = length > 0 ? xD / length : (horizChord > 1.0E-4D ? diffX / horizChord : 1.0D);
            tZ = length > 0 ? zD / length : (horizChord > 1.0E-4D ? diffZ / horizChord : 0.0D);
        }

        // Orient tangent at `next` so it points away from `prev` along the rail path
        if (tX * diffX + tZ * diffZ < 0.0) {
            tX = -tX;
            tZ = -tZ;
        }

        // Continuous 3D chord distance between carts (never jumps discontinuously at block boundaries)
        double dist = Math.sqrt(horizChord * horizChord + diffY * diffY);
        if (nextIsCurve || prevIsCurve) {
            // Smooth arc-to-chord compensation around 90-degree corners
            double curveBlend = Math.min(1.0D, Math.abs(diffX) * Math.abs(diffZ) * 1.5D);
            dist *= (1.0D + 0.045D * curveBlend);
        }

        return new RailPathLink(dist, tX, tZ, true);
    }

    private static @Nullable List<BlockPos> findRailPath(Level level, BlockPos start, BlockPos target, int maxDepth) {
        if (start.equals(target)) {
            List<BlockPos> single = new ArrayList<>();
            single.add(start);
            return single;
        }

        Map<BlockPos, BlockPos> parentMap = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        int depth = 0;
        while (!queue.isEmpty() && depth < maxDepth) {
            int levelSize = queue.size();
            for (int s = 0; s < levelSize; s++) {
                BlockPos current = queue.poll();
                if (current == null) continue;
                if (current.equals(target)) {
                    List<BlockPos> path = new ArrayList<>();
                    BlockPos curr = target;
                    while (curr != null) {
                        path.add(curr);
                        curr = parentMap.get(curr);
                    }
                    Collections.reverse(path);
                    return path;
                }

                BlockState st = level.getBlockState(current);
                if (BaseRailBlock.isRail(st)) {
                    RailShape shape = ((BaseRailBlock) st.getBlock()).getRailDirection(st, level, current, null);
                    Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
                    Vec3i[] exitArray = new Vec3i[]{exits.getFirst(), exits.getSecond()};

                    for (Vec3i ex : exitArray) {
                        BlockPos nFlat = current.offset(ex.getX(), ex.getY(), ex.getZ());
                        BlockPos[] candidates;
                        if (ex.getY() == 0) {
                            candidates = new BlockPos[]{nFlat, nFlat.above(), nFlat.below()};
                        } else {
                            candidates = new BlockPos[]{nFlat};
                        }

                        for (BlockPos cand : candidates) {
                            if (visited.add(cand) && BaseRailBlock.isRail(level.getBlockState(cand))) {
                                parentMap.put(cand, current);
                                queue.add(cand);
                            }
                        }
                    }
                }
            }
            depth++;
        }
        return null;
    }

    public static boolean moveCartAlongRails(AbstractMinecart cart, double dirX, double dirZ, double distance3D) {
        return moveConsistCartAlongRails(cart, dirX, dirZ, distance3D, null);
    }

    public static boolean moveConsistCartAlongRails(AbstractMinecart cart, double dirX, double dirZ, double distance3D, @Nullable List<AbstractMinecart> consistCarts) {
        if (distance3D < 1.0E-5D) return true;

        BlockPos railPos = getCurrentBlockPosOrRailBelow(cart);
        BlockState railState = cart.level().getBlockState(railPos);
        if (!BaseRailBlock.isRail(railState)) {
            cart.move(MoverType.SELF, new Vec3(dirX * distance3D, 0.0, dirZ * distance3D));
            return !cart.horizontalCollision;
        }

        RailShape shape = ((BaseRailBlock) railState.getBlock()).getRailDirection(railState, cart.level(), railPos, cart);
        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
        Vec3i exit0 = exits.getFirst();
        Vec3i exit1 = exits.getSecond();

        double initialX = cart.getX();
        double initialY = cart.getY();
        double initialZ = cart.getZ();

        double x = initialX;
        double y = initialY;
        double z = initialZ;

        // Ensure cart is precisely on rail before computing velocity
        Vec3 railSnap = getRailPosForEntity(cart, x, y, z);
        if (railSnap != null) {
            x = railSnap.x;
            y = railSnap.y;
            z = railSnap.z;
        }

        boolean isCurve = !shape.isSlope() && shape != RailShape.NORTH_SOUTH && shape != RailShape.EAST_WEST;
        double horizStep = shape.isSlope() ? distance3D * 0.70710678D : distance3D;
        double alignedVx;
        double alignedVz;

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
            if (tX * dirX + tZ * dirZ < 0.0D) {
                tX = -tX;
                tZ = -tZ;
            }
            alignedVx = horizStep * tX;
            alignedVz = horizStep * tZ;
        } else {
            double xD = (double) (exit1.getX() - exit0.getX());
            double zD = (double) (exit1.getZ() - exit0.getZ());
            double length = Math.sqrt(xD * xD + zD * zD);
            alignedVx = dirX * horizStep;
            alignedVz = dirZ * horizStep;
            if (length > 0.0D) {
                double flip = dirX * xD + dirZ * zD;
                if (flip < 0.0D) {
                    xD = -xD;
                    zD = -zD;
                }
                alignedVx = horizStep * xD / length;
                alignedVz = horizStep * zD / length;
            }
        }

        // On slopes, set Y to base rail elevation + 1.0 (matching vanilla OldMinecartBehavior) before horizontal move()
        double moveY = (double) railPos.getY();
        if (shape.isSlope()) {
            moveY += 1.0D;
        }
        cart.setPos(x, moveY, z);

        // Suppress same-consist carts' bounding boxes temporarily during cart.move() so they never bind on corners
        List<Pair<AbstractMinecart, AABB>> savedBoxes = new ArrayList<>();
        if (consistCarts != null) {
            for (AbstractMinecart other : consistCarts) {
                if (other != cart && !other.isRemoved()) {
                    savedBoxes.add(Pair.of(other, other.getBoundingBox()));
                    other.setBoundingBox(new AABB(0, -1000, 0, 0, -1000, 0));
                }
            }
        }
        try {
            cart.move(MoverType.SELF, new Vec3(alignedVx, 0.0, alignedVz));
        } finally {
            for (Pair<AbstractMinecart, AABB> entry : savedBoxes) {
                entry.getFirst().setBoundingBox(entry.getSecond());
            }
        }

        // Vanilla slope step-up / step-down check
        if (exit0.getY() != 0 && Mth.floor(cart.getX()) - railPos.getX() == exit0.getX() && Mth.floor(cart.getZ()) - railPos.getZ() == exit0.getZ()) {
            cart.setPos(cart.getX(), cart.getY() + (double) exit0.getY(), cart.getZ());
        } else if (exit1.getY() != 0 && Mth.floor(cart.getX()) - railPos.getX() == exit1.getX() && Mth.floor(cart.getZ()) - railPos.getZ() == exit1.getZ()) {
            cart.setPos(cart.getX(), cart.getY() + (double) exit1.getY(), cart.getZ());
        }

        // Snap precisely onto the rail surface vertically, while preserving forward progress along travel axis
        Vec3 newRailPos = getRailPosForEntity(cart, cart.getX(), cart.getY(), cart.getZ());
        if (newRailPos != null) {
            if (isCurve) {
                cart.setPos(newRailPos.x, newRailPos.y, newRailPos.z);
            } else {
                double finalX = (shape == RailShape.NORTH_SOUTH || shape == RailShape.ASCENDING_NORTH || shape == RailShape.ASCENDING_SOUTH) ? newRailPos.x : cart.getX();
                double finalZ = (shape == RailShape.EAST_WEST || shape == RailShape.ASCENDING_EAST || shape == RailShape.ASCENDING_WEST) ? newRailPos.z : cart.getZ();
                cart.setPos(finalX, newRailPos.y, finalZ);
            }
        }

        double actualDx = cart.getX() - initialX;
        double actualDy = cart.getY() - initialY;
        double actualDz = cart.getZ() - initialZ;

        // Pivot cart yaw around corners using its Front and Rear Connection Points on the rail
        double moveDistSq = actualDx * actualDx + actualDz * actualDz;
        if (moveDistSq > 1.0E-5D) {
            double moveLen = Math.sqrt(moveDistSq);
            double mDirX = actualDx / moveLen;
            double mDirZ = actualDz / moveLen;
            double pivotRadius = getCartCouplerRadius(cart);
            Direction moveFacing = Direction.getApproximateNearest(mDirX, 0.0, mDirZ);

            float yaw;
            if (isCurve) {
                Vec3 frontPivot = getRailPosOffs(cart, cart.getX(), cart.getY(), cart.getZ(), pivotRadius, moveFacing);
                Vec3 rearPivot = getRailPosOffs(cart, cart.getX(), cart.getY(), cart.getZ(), -pivotRadius, moveFacing);

                double chordDx = actualDx;
                double chordDz = actualDz;
                if (frontPivot != null && rearPivot != null) {
                    double pDx = frontPivot.x - rearPivot.x;
                    double pDz = frontPivot.z - rearPivot.z;
                    if (pDx * pDx + pDz * pDz > 1.0E-4D) {
                        chordDx = pDx;
                        chordDz = pDz;
                    }
                }
                yaw = (float) (Mth.atan2(chordDz, chordDx) * 180.0 / Math.PI) - 90.0F;
            } else {
                // Slopes and straight rails are strictly aligned to the track axis direction
                yaw = moveFacing.toYRot();
            }

            if (cart.isFlipped()) {
                yaw += 180.0F;
            }
            yaw = Mth.wrapDegrees(yaw);
            float rotDiff = Mth.wrapDegrees(yaw - cart.yRotO);
            cart.yRotO = yaw - rotDiff;
            cart.setYRot(yaw);
        }

        // Set cart pitch on slope rails
        float targetPitch = 0.0F;
        if (shape.isSlope()) {
            Vec3i upper = exit0.getY() > 0 ? exit0 : exit1;
            Direction moveFacing = (moveDistSq > 1.0E-5D)
                    ? Direction.getApproximateNearest(actualDx, 0.0, actualDz)
                    : cart.getMotionDirection();
            boolean movingUphill = (upper.getX() == moveFacing.getStepX() && upper.getZ() == moveFacing.getStepZ());
            targetPitch = movingUphill ? -45.0F : 45.0F;
        }
        cart.setXRot(targetPitch);
        cart.xRotO = targetPitch;

        if (isPartOfRunningTrain(cart) || isConsistWithLocomotive(cart)) {
            cart.setDeltaMovement(Vec3.ZERO);
        } else {
            cart.setDeltaMovement(actualDx, actualDy, actualDz);
        }
        cart.needsSync = true;

        boolean offRails = (newRailPos == null && findRailBlock(cart.level(), cart.getX(), cart.getY(), cart.getZ()) == null);
        return !offRails;
    }

    /**
     * Unified consist cart movement handler supporting all locomotive types and unpowered rolling stock.
     * Propagates push/pull forces neighbor-to-neighbor through each cart's Front and Rear Connection Points.
     */
    public static List<Pair<Entity, AbstractMinecart>> getOrderedTrainLinks(AbstractMinecart root) {
        List<Pair<Entity, AbstractMinecart>> result = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        visited.add(root.getUUID());

        Deque<Pair<Entity, UUID>> toCheck = new ArrayDeque<>();
        UUID p = getCartParentUUID(root);
        if (p != null) toCheck.add(Pair.of(root, p));
        UUID c = getCartChildUUID(root);
        if (c != null) toCheck.add(Pair.of(root, c));

        while (!toCheck.isEmpty()) {
            Pair<Entity, UUID> next = toCheck.poll();
            UUID nextId = next.getSecond();
            if (nextId != null && visited.add(nextId)) {
                Entity ent = root.level().getEntity(nextId);
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

    public static boolean handleChainedCartsMovement(AbstractMinecart loco, Direction facing) {
        List<Pair<Entity, AbstractMinecart>> links = getOrderedTrainLinks(loco);
        Map<UUID, Vec3> initialCartPositions = new HashMap<>();
        for (Pair<Entity, AbstractMinecart> link : links) {
            AbstractMinecart c = link.getSecond();
            initialCartPositions.put(c.getUUID(), new Vec3(c.getX(), c.getY(), c.getZ()));
        }
        return handleChainedCartsMovement(loco, facing, links, initialCartPositions, loco.getX(), loco.getY(), loco.getZ());
    }

    public static double getDirectionalCouplerRadius(Entity entity, Direction facing, boolean isFront) {
        double sign = isFront ? 1.0D : -1.0D;
        return getDirectionalCouplerRadius(entity, facing.getStepX() * sign, facing.getStepZ() * sign);
    }

    public static boolean handleChainedCartsMovement(AbstractMinecart loco,
                                                     Direction facing,
                                                     List<Pair<Entity, AbstractMinecart>> links,
                                                     Map<UUID, Vec3> initialCartPositions,
                                                     double initialLocoX, double initialLocoY, double initialLocoZ) {
        if (links.isEmpty()) {
            return false;
        }

        boolean moving = isLocomotiveActive(loco) || loco.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;
        double locoSpeed = isLocomotiveActive(loco) ? getLocomotiveSpeed(loco) : loco.getDeltaMovement().horizontalDistance();
        double maxStep = moving ? Math.max(locoSpeed * 3.0D, 1.20D) : 0.50D;

        List<AbstractMinecart> consistCarts = new ArrayList<>();
        consistCarts.add(loco);
        for (Pair<Entity, AbstractMinecart> link : links) {
            consistCarts.add(link.getSecond());
        }

        boolean blocked = false;
        for (Pair<Entity, AbstractMinecart> link : links) {
            Entity prev = link.getFirst();
            AbstractMinecart cart = link.getSecond();

            RailPathLink railLink = traceRailLink(loco.level(), prev, cart);
            double trackDist = railLink.distance;

            // Target distance between cart centers = prev's connection point radius + cart's connection point radius + COUPLER_GAP
            double targetDist = getTargetCouplingDistance(prev, cart, cart.getX() - prev.getX(), cart.getZ() - prev.getZ());
            double deadband = 0.025D;
            double minAllowed = targetDist - deadband;
            double maxAllowed = targetDist + deadband;

            double tangentX = railLink.tangentX;
            double tangentZ = railLink.tangentZ;

            if (trackDist > maxAllowed) {
                // Pull force transmitted through connection points: pull cart along rails toward prev
                double needed = Math.min(trackDist - targetDist, maxStep);
                boolean success = moveConsistCartAlongRails(cart, -tangentX, -tangentZ, needed, consistCarts);
                if (!success && moving && isLocomotiveRunning(loco)) {
                    blocked = true;
                    break;
                }
            } else if (trackDist < minAllowed) {
                // Push force transmitted through connection points: push cart along rails away from prev
                double needed = Math.min(targetDist - trackDist, maxStep);
                boolean success = moveConsistCartAlongRails(cart, tangentX, tangentZ, needed, consistCarts);
                if (!success && moving && isLocomotiveRunning(loco)) {
                    blocked = true;
                    break;
                }
            } else {
                Vec3 snapped = getRailPosForEntity(cart, cart.getX(), cart.getY(), cart.getZ());
                if (snapped != null) {
                    cart.setPos(snapped.x, snapped.y, snapped.z);
                }
                if (!moving) {
                    cart.setDeltaMovement(Vec3.ZERO);
                }
            }
        }

        if (blocked) {
            // Revert locomotive position to before this tick's move so it does NOT drive through carts
            loco.setPos(initialLocoX, initialLocoY, initialLocoZ);
            Vec3 railPos = getRailPosForEntity(loco, initialLocoX, initialLocoY, initialLocoZ);
            if (railPos != null) {
                loco.setPos(railPos.x, railPos.y, railPos.z);
            }
            loco.setDeltaMovement(Vec3.ZERO);
            stopLocomotive(loco);
            syncTrainConsist(loco, false, getLocomotiveSpeedLevel(loco), isLocomotiveReversed(loco));
            loco.level().playSound(null, loco.blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.BLOCKS, 0.6F, 1.4F);

            // Revert all carts that moved forward this tick
            for (Pair<Entity, AbstractMinecart> link : links) {
                AbstractMinecart c = link.getSecond();
                Vec3 initPos = initialCartPositions.get(c.getUUID());
                if (initPos != null) {
                    c.setPos(initPos.x, initPos.y, initPos.z);
                    Vec3 cRail = getRailPosForEntity(c, initPos.x, initPos.y, initPos.z);
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
    public static void pushCartFromLocomotiveBumper(AbstractMinecart loco, AbstractMinecart otherCart) {
        if (loco.level().isClientSide() || isConnectedInSameTrain(loco, otherCart)) {
            return;
        }

        Direction travelDir = getLocomotiveTravelDirection(loco);
        RailPathLink link = traceRailLink(loco.level(), loco, otherCart);
        double bumperDirX = link.tangentX;
        double bumperDirZ = link.tangentZ;

        double locoRadius = getDirectionalCouplerRadius(loco, travelDir.getStepX(), travelDir.getStepZ());
        double cartRadius = getCartCouplerRadius(otherCart);
        double contactDist = locoRadius + cartRadius;

        double dist = link.distance;
        double penetration = contactDist - dist;

        double locoSpeed = getLocomotiveSpeed(loco);

        if (penetration > -0.05D) {
            double pushDist = Math.max(penetration + 0.05D, Math.max(locoSpeed, 0.05D));
            moveCartAlongRails(otherCart, bumperDirX, bumperDirZ, pushDist);

            double pushSpeed = Math.max(locoSpeed, 0.05D);
            otherCart.setDeltaMovement(
                    bumperDirX * pushSpeed,
                    otherCart.getDeltaMovement().y,
                    bumperDirZ * pushSpeed
            );
            otherCart.needsSync = true;
        }
    }

    public static void pushEntitiesInLocomotivePath(AbstractMinecart loco, Direction travelDir, double locoSpeed) {
        pushEntitiesInLocomotivePath(loco, travelDir, locoSpeed, loco.getX(), loco.getY(), loco.getZ());
    }

    public static void pushEntitiesInLocomotivePath(AbstractMinecart loco,
                                                    Direction travelDir,
                                                    double locoSpeed,
                                                    double initialLocoX, double initialLocoY, double initialLocoZ) {
        if (loco.level().isClientSide()) return;

        double frontRadius = getDirectionalCouplerRadius(loco, travelDir.getStepX(), travelDir.getStepZ());
        double scanReach = frontRadius + 2.5D + Math.max(locoSpeed, 0.5D);

        double minX = Math.min(loco.getX(), loco.getX() + travelDir.getStepX() * scanReach) - 1.25D;
        double maxX = Math.max(loco.getX(), loco.getX() + travelDir.getStepX() * scanReach) + 1.25D;
        double minZ = Math.min(loco.getZ(), loco.getZ() + travelDir.getStepZ() * scanReach) - 1.25D;
        double maxZ = Math.max(loco.getZ(), loco.getZ() + travelDir.getStepZ() * scanReach) + 1.25D;
        double minY = loco.getY() - 1.0D;
        double maxY = loco.getY() + 2.0D;
        AABB scanBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        List<Entity> pushableEntities = loco.level().getEntities(
                loco,
                scanBox,
                e -> !e.noPhysics && !loco.hasPassenger(e) && !isConnectedInSameTrain(loco, e)
        );

        // 1. Deflect living entities (cowcatcher)
        for (Entity entity : pushableEntities) {
            if (entity instanceof LivingEntity living) {
                double lateralX = -travelDir.getStepZ();
                double lateralZ = travelDir.getStepX();
                double lateralDist = (living.getX() - loco.getX()) * lateralX + (living.getZ() - loco.getZ()) * lateralZ;
                double lateralSign = lateralDist >= 0 ? 1.0 : -1.0;
                double pushStrength = Math.max(locoSpeed * 2.2D, 0.5D);
                double pushX = travelDir.getStepX() * pushStrength + lateralX * 0.4D * lateralSign;
                double pushZ = travelDir.getStepZ() * pushStrength + lateralZ * 0.4D * lateralSign;
                living.setDeltaMovement(pushX, 0.25D, pushZ);
                living.needsSync = true;
            }
        }

        // 2. Collect and sort unlinked carts ahead of the locomotive
        List<AbstractMinecart> unlinkedCarts = new ArrayList<>();
        for (Entity entity : pushableEntities) {
            if (entity instanceof AbstractMinecart cart) {
                unlinkedCarts.add(cart);
            }
        }

        if (unlinkedCarts.isEmpty()) return;

        // Sort descending by along-track distance from locomotive (furthest cart first)
        unlinkedCarts.sort((a, b) -> {
            double da = (a.getX() - loco.getX()) * travelDir.getStepX() + (a.getZ() - loco.getZ()) * travelDir.getStepZ();
            double db = (b.getX() - loco.getX()) * travelDir.getStepX() + (b.getZ() - loco.getZ()) * travelDir.getStepZ();
            return Double.compare(db, da);
        });

        AbstractMinecart closestCart = unlinkedCarts.get(unlinkedCarts.size() - 1);
        double dx = closestCart.getX() - loco.getX();
        double dz = closestCart.getZ() - loco.getZ();
        double distAlongTravel = dx * travelDir.getStepX() + dz * travelDir.getStepZ();
        double cartRadius = getCartCouplerRadius(closestCart);
        double contactDist = frontRadius + cartRadius;
        double penetration = contactDist - distAlongTravel;

        if (penetration > -0.05D) {
            double pushDist = Math.max(penetration + 0.05D, Math.max(locoSpeed, 0.05D));
            boolean allMoved = true;

            for (AbstractMinecart cart : unlinkedCarts) {
                RailPathLink link = traceRailLink(loco.level(), loco, cart);
                double pDirX = link.tangentX;
                double pDirZ = link.tangentZ;
                boolean ok = moveCartAlongRails(cart, pDirX, pDirZ, pushDist);
                if (!ok) {
                    allMoved = false;
                }
                double pushSpeed = Math.max(locoSpeed, 0.05D);
                cart.setDeltaMovement(pDirX * pushSpeed, cart.getDeltaMovement().y, pDirZ * pushSpeed);
                cart.needsSync = true;
            }

            // Re-check distance to closest cart
            double postDx = closestCart.getX() - loco.getX();
            double postDz = closestCart.getZ() - loco.getZ();
            double postDist = postDx * travelDir.getStepX() + postDz * travelDir.getStepZ();
            double postPenetration = contactDist - postDist;

            if (postPenetration > 0.01D) {
                if (!allMoved || postPenetration > 0.25D) {
                    // Cart chain blocked by obstacle/buffer stop: revert locomotive to prevent merging and halt
                    loco.setPos(initialLocoX, initialLocoY, initialLocoZ);
                    Vec3 railPos = getRailPosForEntity(loco, initialLocoX, initialLocoY, initialLocoZ);
                    if (railPos != null) {
                        loco.setPos(railPos.x, railPos.y, railPos.z);
                    }
                    loco.setDeltaMovement(Vec3.ZERO);
                    stopLocomotive(loco);
                    syncTrainConsist(loco, false, getLocomotiveSpeedLevel(loco), isLocomotiveReversed(loco));
                    loco.level().playSound(null, loco.blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.BLOCKS, 0.7F, 1.2F);
                } else {
                    // Prevent phasing: clamp locomotive position so it pushes exactly from connection bumper
                    double clampX = closestCart.getX() - travelDir.getStepX() * contactDist;
                    double clampZ = closestCart.getZ() - travelDir.getStepZ() * contactDist;
                    Vec3 clampedRail = getRailPosForEntity(loco, clampX, loco.getY(), clampZ);
                    if (clampedRail != null) {
                        loco.setPos(clampedRail.x, clampedRail.y, clampedRail.z);
                    }
                }
            }
        }
    }
}
