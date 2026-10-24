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
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.CabooseMinecartEntity;
import com.example.miningtools.entity.CargoMinecartEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.entity.LargeLiquidMinecartEntity;
import com.example.miningtools.entity.LiquidMinecartEntity;
import com.example.miningtools.entity.LivestockCartEntity;
import com.example.miningtools.entity.TunnelBoreEntity;
import com.example.miningtools.entity.TurretCartEntity;
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
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
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
        Level level = entity.level();
        if (BaseRailBlock.isRail(level.getBlockState(new BlockPos(i, j, k)))) {
            return new BlockPos(i, j, k);
        }
        if (BaseRailBlock.isRail(level.getBlockState(new BlockPos(i, j - 1, k)))) {
            return new BlockPos(i, j - 1, k);
        }
        if (BaseRailBlock.isRail(level.getBlockState(new BlockPos(i, j - 2, k)))) {
            return new BlockPos(i, j - 2, k);
        }
        if (BaseRailBlock.isRail(level.getBlockState(new BlockPos(i, j - 3, k)))) {
            return new BlockPos(i, j - 3, k);
        }
        if (BaseRailBlock.isRail(level.getBlockState(new BlockPos(i, j + 1, k)))) {
            return new BlockPos(i, j + 1, k);
        }
        return new BlockPos(i, j, k);
    }

    private TrainChainingUtil() {}

    public static final String TAG_PARENT_CART = "miningtools:parent_cart";
    public static final String TAG_CHILD_CART = "miningtools:child_cart";

    public static final double COUPLER_GAP = 0.08D;

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

    public static @Nullable Pair<AbstractMinecart, AbstractMinecart> determineRoles(AbstractMinecart first, AbstractMinecart second) {
        boolean firstIsTail = isTailCart(first);
        boolean secondIsTail = isTailCart(second);

        if (firstIsTail && !secondIsTail) {
            return null;
        }
        if (secondIsTail && !firstIsTail) {
            AbstractMinecart tail = getTrainTail(first);
            return Pair.of(tail != null ? tail : first, second);
        }

        boolean firstIsLead = isLeadCart(first);
        boolean secondIsLead = isLeadCart(second);

        UUID firstChild = getCartChildUUID(first);
        UUID firstParent = getCartParentUUID(first);
        UUID secondChild = getCartChildUUID(second);
        UUID secondParent = getCartParentUUID(second);

        if (firstParent != null && firstChild == null) {
            return Pair.of(first, second);
        }

        if (secondParent != null && secondChild == null && firstParent == null) {
            return Pair.of(second, first);
        }

        if (firstChild != null && secondParent == null && secondChild == null) {
            AbstractMinecart tail = getTrainTail(first);
            if (tail != null && !tail.getUUID().equals(second.getUUID())) {
                return Pair.of(tail, second);
            }
        }

        if (firstParent == null && firstChild == null && secondParent == null && secondChild == null) {
            if (firstIsLead && !secondIsLead) {
                return Pair.of(first, second);
            }
            if (secondIsLead && !firstIsLead) {
                return Pair.of(second, first);
            }
            if (firstIsLead && secondIsLead) {
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
        }

        return Pair.of(first, second);
    }

    public static boolean isLeadCart(AbstractMinecart cart) {
        return cart instanceof BasicLocomotiveEntity
                || cart instanceof DieselLocomotiveEntity
                || cart instanceof ElectricLocomotiveEntity
                || cart instanceof TunnelBoreEntity;
    }

    public static boolean isTailCart(AbstractMinecart cart) {
        return cart instanceof CabooseMinecartEntity;
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

        if (currentParentChild != null && !currentParentChild.equals(child.getUUID())) {
            Entity oldChild = level.getEntity(currentParentChild);
            if (oldChild instanceof AbstractMinecart oldChildCart) {
                setCartParent(oldChildCart, null, level);
            }
        }

        if (currentChildParent != null && !currentChildParent.equals(parent.getUUID())) {
            Entity oldParent = level.getEntity(currentChildParent);
            if (oldParent instanceof AbstractMinecart oldParentCart) {
                setCartChild(oldParentCart, null, level);
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
        if (isConnectedInSameTrain(cart, entity) || entity instanceof LivingEntity || entity instanceof AbstractMinecart) {
            return false;
        }
        return !entity.isPassengerOfSameVehicle(cart);
    }

    public static boolean canBeCollidedWith(AbstractMinecart cart, @Nullable Entity other) {
        if (other instanceof AbstractMinecart || other instanceof LivingEntity) {
            return false;
        }
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

        Vec3 motion = cart.getDeltaMovement();
        double horizSpeed = motion.horizontalDistance();
        boolean isRunningTrain = isPartOfRunningTrain(cart);

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

        double dx = entity.getX() - cart.getX();
        double dz = entity.getZ() - cart.getZ();
        double distSq = dx * dx + dz * dz;
        if (distSq >= 1.0E-4D) {
            double dist = Math.sqrt(distSq);
            dx /= dist;
            dz /= dist;
            double factor = 1.0D / dist;
            if (factor > 1.0D) {
                factor = 1.0D;
            }
            dx *= factor * 0.1D;
            dz *= factor * 0.1D;
            if (entity.isPushable()) {
                entity.push(dx, 0.0D, dz);
            }
        }
    }

    public static @Nullable Vec3 getRailPosForEntity(AbstractMinecart entity, double x, double y, double z) {
        int xt = Mth.floor(x);
        int yt = Mth.floor(y);
        int zt = Mth.floor(z);
        if (BaseRailBlock.isRail(entity.level().getBlockState(new BlockPos(xt, yt - 1, zt)))) {
            yt--;
        } else if (!BaseRailBlock.isRail(entity.level().getBlockState(new BlockPos(xt, yt, zt)))) {
            if (BaseRailBlock.isRail(entity.level().getBlockState(new BlockPos(xt, yt - 2, zt)))) {
                yt -= 2;
            } else if (BaseRailBlock.isRail(entity.level().getBlockState(new BlockPos(xt, yt - 3, zt)))) {
                yt -= 3;
            } else if (BaseRailBlock.isRail(entity.level().getBlockState(new BlockPos(xt, yt + 1, zt)))) {
                yt++;
            }
        }

        BlockPos railPos = new BlockPos(xt, yt, zt);
        BlockState state = entity.level().getBlockState(railPos);
        if (BaseRailBlock.isRail(state)) {
            RailShape shape = ((BaseRailBlock) state.getBlock()).getRailDirection(state, entity.level(), railPos, entity);
            Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
            Vec3i exit0 = exits.getFirst();
            Vec3i exit1 = exits.getSecond();
            boolean isCurve = !shape.isSlope() && shape != RailShape.NORTH_SOUTH && shape != RailShape.EAST_WEST;
            if (isCurve) {
                double circleCenterX = xt + 0.5 + (exit0.getX() + exit1.getX()) * 0.5;
                double circleCenterZ = zt + 0.5 + (exit0.getZ() + exit1.getZ()) * 0.5;
                double vx = x - circleCenterX;
                double vz = z - circleCenterZ;
                double signX = -(exit0.getX() + exit1.getX());
                double signZ = -(exit0.getZ() + exit1.getZ());
                if (vx * signX < 0.0) {
                    vx = 0.0;
                }
                if (vz * signZ < 0.0) {
                    vz = 0.0;
                }
                double dist = Math.sqrt(vx * vx + vz * vz);
                if (dist > 1.0E-5) {
                    x = circleCenterX + (vx / dist) * 0.5;
                    z = circleCenterZ + (vz / dist) * 0.5;
                }
                y = yt + 0.0625;
            } else {
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
            }

            return new Vec3(x, y, z);
        }
        return null;
    }

    public static @Nullable Vec3 getRailPosOffs(AbstractMinecart entity, double x, double y, double z, double offset, Direction facing) {
        int xt = Mth.floor(x);
        int yt = Mth.floor(y);
        int zt = Mth.floor(z);
        if (BaseRailBlock.isRail(entity.level().getBlockState(new BlockPos(xt, yt - 1, zt)))) {
            yt--;
        } else if (!BaseRailBlock.isRail(entity.level().getBlockState(new BlockPos(xt, yt, zt)))) {
            if (BaseRailBlock.isRail(entity.level().getBlockState(new BlockPos(xt, yt - 2, zt)))) {
                yt -= 2;
            } else if (BaseRailBlock.isRail(entity.level().getBlockState(new BlockPos(xt, yt - 3, zt)))) {
                yt -= 3;
            } else if (BaseRailBlock.isRail(entity.level().getBlockState(new BlockPos(xt, yt + 1, zt)))) {
                yt++;
            }
        }

        BlockPos railPos = new BlockPos(xt, yt, zt);
        BlockState state = entity.level().getBlockState(railPos);
        if (BaseRailBlock.isRail(state)) {
            RailShape shape = ((BaseRailBlock) state.getBlock()).getRailDirection(state, entity.level(), railPos, entity);
            y = yt;
            if (shape.isSlope()) {
                y = yt + 1;
            }

            Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
            Vec3i exit0 = exits.getFirst();
            Vec3i exit1 = exits.getSecond();
            boolean isCurve = !shape.isSlope() && shape != RailShape.NORTH_SOUTH && shape != RailShape.EAST_WEST;
            double xD;
            double zD;

            if (isCurve) {
                double circleCenterX = xt + 0.5 + (exit0.getX() + exit1.getX()) * 0.5;
                double circleCenterZ = zt + 0.5 + (exit0.getZ() + exit1.getZ()) * 0.5;
                double vx = x - circleCenterX;
                double vz = z - circleCenterZ;
                double dist = Math.sqrt(vx * vx + vz * vz);
                double ux = dist > 1.0E-5 ? vx / dist : 0.70710678D * -(exit0.getX() + exit1.getX());
                double uz = dist > 1.0E-5 ? vz / dist : 0.70710678D * -(exit0.getZ() + exit1.getZ());
                double tX = -uz;
                double tZ = ux;
                boolean movingExit0ToExit1 = (exit0.getX() == -facing.getStepX() && exit0.getZ() == -facing.getStepZ())
                        || (exit1.getX() == facing.getStepX() && exit1.getZ() == facing.getStepZ());
                double exitVecX = (exit1.getX() - exit0.getX()) * (movingExit0ToExit1 ? 1.0 : -1.0);
                double exitVecZ = (exit1.getZ() - exit0.getZ()) * (movingExit0ToExit1 ? 1.0 : -1.0);
                if (tX * exitVecX + tZ * exitVecZ < 0.0) {
                    tX = -tX;
                    tZ = -tZ;
                }
                xD = tX;
                zD = tZ;
            } else {
                xD = exit1.getX() - exit0.getX();
                zD = exit1.getZ() - exit0.getZ();
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
            }

            x += xD * offset;
            z += zD * offset;
            if (exit0.getY() != 0 && Mth.floor(x) - xt == exit0.getX() && Mth.floor(z) - zt == exit0.getZ()) {
                y += exit0.getY();
            } else if (exit1.getY() != 0 && Mth.floor(x) - xt == exit1.getX() && Mth.floor(z) - zt == exit1.getZ()) {
                y += exit1.getY();
            }

            return getRailPosForEntity(entity, x, y, z);
        } else {
            return null;
        }
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
        double diffZ = nextPos.z - prevPos.z;
        double horizChord = Math.hypot(diffX, diffZ);

        BlockPos prevRail = getCurrentBlockPosOrRailBelow(prev);
        BlockPos nextRail = getCurrentBlockPosOrRailBelow(next);
        BlockState prevState = level.getBlockState(prevRail);
        BlockState nextState = level.getBlockState(nextRail);

        if (!BaseRailBlock.isRail(prevState) || !BaseRailBlock.isRail(nextState)) {
            double dirX = horizChord > 1.0E-4D ? diffX / horizChord : 1.0D;
            double dirZ = horizChord > 1.0E-4D ? diffZ / horizChord : 0.0D;
            double dist = horizChord + 0.41421356D * Math.abs(nextPos.y - prevPos.y);
            return new RailPathLink(dist, dirX, dirZ, false);
        }

        if (prevRail.equals(nextRail)) {
            RailShape shape = ((BaseRailBlock) nextState.getBlock()).getRailDirection(nextState, level, nextRail, next);
            Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
            Vec3i exit0 = exits.getFirst();
            Vec3i exit1 = exits.getSecond();
            boolean isCurve = !shape.isSlope() && shape != RailShape.NORTH_SOUTH && shape != RailShape.EAST_WEST;
            double tX;
            double tZ;
            double dist;

            if (isCurve) {
                double circleCenterX = nextRail.getX() + 0.5 + (exit0.getX() + exit1.getX()) * 0.5;
                double circleCenterZ = nextRail.getZ() + 0.5 + (exit0.getZ() + exit1.getZ()) * 0.5;
                double vx = nextPos.x - circleCenterX;
                double vz = nextPos.z - circleCenterZ;
                double vLen = Math.sqrt(vx * vx + vz * vz);
                double ux = vLen > 1.0E-5 ? vx / vLen : 0.70710678D * -(exit0.getX() + exit1.getX());
                double uz = vLen > 1.0E-5 ? vz / vLen : 0.70710678D * -(exit0.getZ() + exit1.getZ());
                tX = -uz;
                tZ = ux;
                if (tX * diffX + tZ * diffZ < 0.0) {
                    tX = -tX;
                    tZ = -tZ;
                }
                double pvX = prevPos.x - circleCenterX;
                double pvZ = prevPos.z - circleCenterZ;
                double anglePrev = Math.atan2(pvZ, pvX);
                double angleNext = Math.atan2(vz, vx);
                double deltaAngle = Math.abs(Mth.wrapDegrees((float) Math.toDegrees(angleNext - anglePrev)));
                dist = Math.toRadians(deltaAngle) * 0.5D;
            } else {
                double xD = exit1.getX() - exit0.getX();
                double zD = exit1.getZ() - exit0.getZ();
                double length = Math.sqrt(xD * xD + zD * zD);
                tX = length > 0 ? xD / length : (horizChord > 1.0E-4D ? diffX / horizChord : 1.0D);
                tZ = length > 0 ? zD / length : (horizChord > 1.0E-4D ? diffZ / horizChord : 0.0D);
                if (tX * diffX + tZ * diffZ < 0.0) {
                    tX = -tX;
                    tZ = -tZ;
                }
                dist = shape.isSlope() ? horizChord * 1.41421356D : horizChord;
            }
            return new RailPathLink(dist, tX, tZ, true);
        }

        // Trace up to 6 rail blocks from prevRail to nextRail
        List<BlockPos> path = findRailPath(level, prevRail, nextRail, 6);
        if (path != null && path.size() >= 2) {
            // Found contiguous rail path!
            BlockPos lastStep = path.get(path.size() - 2);
            int stepInX = nextRail.getX() - lastStep.getX();
            int stepInZ = nextRail.getZ() - lastStep.getZ();

            RailShape nextShape = ((BaseRailBlock) nextState.getBlock()).getRailDirection(nextState, level, nextRail, next);
            Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(nextShape);
            Vec3i exit0 = exits.getFirst();
            Vec3i exit1 = exits.getSecond();
            boolean isCurve = !nextShape.isSlope() && nextShape != RailShape.NORTH_SOUTH && nextShape != RailShape.EAST_WEST;
            double tX;
            double tZ;

            if (isCurve) {
                double circleCenterX = nextRail.getX() + 0.5 + (exit0.getX() + exit1.getX()) * 0.5;
                double circleCenterZ = nextRail.getZ() + 0.5 + (exit0.getZ() + exit1.getZ()) * 0.5;
                double vx = nextPos.x - circleCenterX;
                double vz = nextPos.z - circleCenterZ;
                double vLen = Math.sqrt(vx * vx + vz * vz);
                double ux = vLen > 1.0E-5 ? vx / vLen : 0.70710678D * -(exit0.getX() + exit1.getX());
                double uz = vLen > 1.0E-5 ? vz / vLen : 0.70710678D * -(exit0.getZ() + exit1.getZ());
                tX = -uz;
                tZ = ux;
                if (tX * stepInX + tZ * stepInZ < 0.0) {
                    tX = -tX;
                    tZ = -tZ;
                }
            } else {
                double xD = exit1.getX() - exit0.getX();
                double zD = exit1.getZ() - exit0.getZ();
                double length = Math.sqrt(xD * xD + zD * zD);
                tX = length > 0 ? xD / length : 1.0;
                tZ = length > 0 ? zD / length : 0.0;
                if (tX * stepInX + tZ * stepInZ < 0.0) {
                    tX = -tX;
                    tZ = -tZ;
                }
            }

            // Path arc length estimation: sum of block arc segments
            double totalArc = 0.0D;
            for (int i = 0; i < path.size(); i++) {
                BlockPos p = path.get(i);
                BlockState st = level.getBlockState(p);
                if (BaseRailBlock.isRail(st)) {
                    RailShape sh = ((BaseRailBlock) st.getBlock()).getRailDirection(st, level, p, null);
                    if (sh.isSlope()) {
                        totalArc += 1.41421356D;
                    } else if (sh != RailShape.NORTH_SOUTH && sh != RailShape.EAST_WEST) {
                        totalArc += 0.78539816D; // pi/4
                    } else {
                        totalArc += 1.0D;
                    }
                }
            }
            // Adjust end offsets
            totalArc -= 0.5D; // start and end centered
            totalArc = Math.max(totalArc, 0.1D);
            return new RailPathLink(totalArc, tX, tZ, true);
        }

        // Corner-aware Manhattan arc fallback
        double minD = Math.min(Math.abs(diffX), Math.abs(diffZ));
        double arcAdj = 0.2146D * Math.min(1.0D, minD / 0.5D);
        double dist = (Math.abs(diffX) + Math.abs(diffZ) - arcAdj) + 0.41421356D * Math.abs(nextPos.y - prevPos.y);
        double dirX = horizChord > 1.0E-4D ? diffX / horizChord : 1.0D;
        double dirZ = horizChord > 1.0E-4D ? diffZ / horizChord : 0.0D;
        return new RailPathLink(dist, dirX, dirZ, false);
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

        cart.setDeltaMovement(dirX * distance3D, 0.0, dirZ * distance3D);

        RailShape shape = ((BaseRailBlock) railState.getBlock()).getRailDirection(railState, cart.level(), railPos, cart);
        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
        Vec3i exit0 = exits.getFirst();
        Vec3i exit1 = exits.getSecond();

        double initialX = cart.getX();
        double initialY = cart.getY();
        double initialZ = cart.getZ();

        Direction cartDir = (Math.abs(dirX) > 1.0E-4D || Math.abs(dirZ) > 1.0E-4D)
                ? Direction.getApproximateNearest(dirX, 0.0, dirZ)
                : cart.getMotionDirection();
        boolean uphillAhead = BaseRailBlock.isRail(cart.level().getBlockState(railPos.relative(cartDir).above()));

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

        boolean isCurve = !shape.isSlope() && shape != RailShape.NORTH_SOUTH && shape != RailShape.EAST_WEST;
        double x;
        double z;
        if (isCurve) {
            Vec3 railPosVec = getRailPosForEntity(cart, cart.getX(), cart.getY(), cart.getZ());
            if (railPosVec != null) {
                x = railPosVec.x;
                z = railPosVec.z;
            } else {
                x = x0 + xD * progress;
                z = z0 + zD * progress;
            }
        } else {
            x = x0 + xD * progress;
            z = z0 + zD * progress;
        }

        double y = railPos.getY();
        if (shape.isSlope() || uphillAhead) {
            y = railPos.getY() + 1.0;
        }
        cart.setPos(x, y, z);

        double horizStep = shape.isSlope() ? distance3D * 0.70710678D : distance3D;
        double alignedVx;
        double alignedVz;

        if (isCurve) {
            double circleCenterX = railPos.getX() + 0.5 + (exit0.getX() + exit1.getX()) * 0.5;
            double circleCenterZ = railPos.getZ() + 0.5 + (exit0.getZ() + exit1.getZ()) * 0.5;
            double vx = x - circleCenterX;
            double vz = z - circleCenterZ;
            double dist = Math.sqrt(vx * vx + vz * vz);
            double ux = dist > 1.0E-5 ? vx / dist : 0.70710678D * -(exit0.getX() + exit1.getX());
            double uz = dist > 1.0E-5 ? vz / dist : 0.70710678D * -(exit0.getZ() + exit1.getZ());
            double tX = -uz;
            double tZ = ux;
            if (tX * dirX + tZ * dirZ < 0.0) {
                tX = -tX;
                tZ = -tZ;
            }
            alignedVx = horizStep * tX;
            alignedVz = horizStep * tZ;
        } else {
            double length = Math.sqrt(xD * xD + zD * zD);
            alignedVx = dirX * horizStep;
            alignedVz = dirZ * horizStep;
            if (length > 0.0) {
                double flip = dirX * xD + dirZ * zD;
                if (flip < 0.0) {
                    xD = -xD;
                    zD = -zD;
                }
                alignedVx = horizStep * xD / length;
                alignedVz = horizStep * zD / length;
            }
        }

        cart.setDeltaMovement(alignedVx, 0.0, alignedVz);

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

        if (shape.isSlope()) {
            Vec3i lowerExit = exit0.getY() == 0 ? exit0 : exit1;
            Vec3i upperExit = exit0.getY() > 0 ? exit0 : exit1;
            if (Mth.floor(cart.getX()) - railPos.getX() == upperExit.getX() && Mth.floor(cart.getZ()) - railPos.getZ() == upperExit.getZ()) {
                cart.setPos(cart.getX(), cart.getY() + 1.0, cart.getZ());
            } else if (Mth.floor(cart.getX()) - railPos.getX() == lowerExit.getX() && Mth.floor(cart.getZ()) - railPos.getZ() == lowerExit.getZ()) {
                BlockPos lowerRail = railPos.offset(lowerExit.getX(), -1, lowerExit.getZ());
                if (BaseRailBlock.isRail(cart.level().getBlockState(lowerRail))) {
                    cart.setPos(cart.getX(), cart.getY() - 1.0, cart.getZ());
                }
            }
        }

        Vec3 newRailPos = getRailPosForEntity(cart, cart.getX(), cart.getY(), cart.getZ());
        if (newRailPos != null) {
            cart.setPos(newRailPos.x, newRailPos.y, newRailPos.z);
        }

        double actualDx = cart.getX() - initialX;
        double actualDy = cart.getY() - initialY;
        double actualDz = cart.getZ() - initialZ;
        double actual3D = Math.sqrt(actualDx * actualDx + actualDy * actualDy + actualDz * actualDz);

        if (actualDx * actualDx + actualDz * actualDz > 1.0E-5D) {
            float yaw = (float) (Mth.atan2(-actualDz, -actualDx) * 180.0 / Math.PI);
            if (cart.isFlipped()) {
                yaw += 180.0F;
            }
            yaw = Mth.wrapDegrees(yaw);
            float rotDiff = Mth.wrapDegrees(yaw - cart.yRotO);
            cart.yRotO = yaw - rotDiff;
            cart.setYRot(yaw);
        }

        cart.setDeltaMovement(Vec3.ZERO);

        boolean offRails = (newRailPos == null);
        boolean blocked = false;
        if (offRails) {
            blocked = true;
        } else {
            boolean madeProgress = actual3D >= distance3D * 0.3D || actual3D > 0.03D;
            if (!madeProgress && BasicLocomotiveEntity.isPathBlockedByObstacle(cart.level(), railPos, cartDir, cart)) {
                blocked = true;
            }
        }
        return !blocked;
    }

    /**
     * Unified consist cart movement handler supporting all locomotive types.
     * Handles center-to-center arc coupling, corner curve alignment, and atomic stall rollback.
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
        double locoSpeed = getLocomotiveSpeed(loco);
        double maxStep = moving ? Math.max(locoSpeed * 1.6D, 0.25D) : 0.15D;

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

            double targetDist = getTargetCouplingDistance(prev, cart, cart.getX() - prev.getX(), cart.getZ() - prev.getZ());
            double minAllowed = moving ? (targetDist - 0.08D) : (targetDist - 0.12D);
            double maxAllowed = moving ? (targetDist + 0.08D) : (targetDist + 0.12D);

            double tangentX = railLink.tangentX;
            double tangentZ = railLink.tangentZ;

            if (trackDist > maxAllowed) {
                // Cart is lagging behind prev -> pull cart along rails toward prev
                double needed = Math.min(trackDist - targetDist, maxStep);
                boolean success = moveConsistCartAlongRails(cart, -tangentX, -tangentZ, needed, consistCarts);
                if (!success && moving && isLocomotiveRunning(loco)) {
                    blocked = true;
                    break;
                }
            } else if (trackDist < minAllowed) {
                // Cart is too close to prev -> push cart along rails away from prev
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
                cart.setDeltaMovement(Vec3.ZERO);
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
            double pushDist = Math.max(penetration + 0.05D, Math.max(locoSpeed, 0.2D));
            moveCartAlongRails(otherCart, bumperDirX, bumperDirZ, pushDist);

            double pushSpeed = Math.max(locoSpeed, 0.35D);
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
            double pushDist = Math.max(penetration + 0.05D, Math.max(locoSpeed, 0.2D));
            boolean allMoved = true;

            for (AbstractMinecart cart : unlinkedCarts) {
                RailPathLink link = traceRailLink(loco.level(), loco, cart);
                double pDirX = link.tangentX;
                double pDirZ = link.tangentZ;
                boolean ok = moveCartAlongRails(cart, pDirX, pDirZ, pushDist);
                if (!ok) {
                    allMoved = false;
                }
                double pushSpeed = Math.max(locoSpeed, 0.35D);
                cart.setDeltaMovement(pDirX * pushSpeed, cart.getDeltaMovement().y, pDirZ * pushSpeed);
                cart.needsSync = true;
            }

            // Re-check distance to closest cart
            double postDx = closestCart.getX() - loco.getX();
            double postDz = closestCart.getZ() - loco.getZ();
            double postDist = postDx * travelDir.getStepX() + postDz * travelDir.getStepZ();
            double postPenetration = contactDist - postDist;

            if (postPenetration > 0.15D && !allMoved) {
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
            }
        }
    }
}
