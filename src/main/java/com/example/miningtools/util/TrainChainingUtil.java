package com.example.miningtools.util;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import java.util.Deque;
import java.util.HashSet;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
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
            // Heavy CN Road diesel locomotive frame (1.18m radius)
            return 1.18D;
        }
        if (entity instanceof ElectricLocomotiveEntity) {
            // Bullet train rear is 14px (0.88m)
            return 0.88D;
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
     * Calculates the target distance between two linked vehicles on the rail.
     * @param prev The forward entity (parent)
     * @param next The trailing cart (child)
     * @param isBumperAnchor True if the parent uses an anchor point already offset toward the bumper (e.g. BasicLocomotiveEntity.getAnchorPosFor)
     * @return The target distance in blocks.
     */
    public static double getTargetCouplingDistance(Entity prev, Entity next, boolean isBumperAnchor) {
        double nextRadius = getCartCouplerRadius(next);
        if (isBumperAnchor) {
            if (prev instanceof DieselLocomotiveEntity) {
                return 0.0D + nextRadius + COUPLER_GAP;
            }
            // The locomotive anchor is already offset by 0.36m toward the bumper.
            // Locomotive total coupler radius is 0.78m, so remaining distance is 0.78 - 0.36 = 0.42m.
            // 0.42m + 0.88m (Cargo Cart) + 0.08m = 1.38m (the exact distance between steam locomotive and first cargo cart).
            return 0.42D + nextRadius + COUPLER_GAP;
        }
        double prevRadius = getCartCouplerRadius(prev);
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

        // Caboose rules:
        // A Caboose can NEVER have carts coupled behind it! It must remain the end of the train.
        if (firstIsTail && !secondIsTail) {
            // Player clicked Caboose then another cart -> invalid (cannot attach to the back of a Caboose)
            return null;
        }
        if (secondIsTail && !firstIsTail) {
            // Player clicked train cart then Caboose -> append Caboose to the end of first's train
            AbstractMinecart tail = getTrainTail(first);
            return Pair.of(tail != null ? tail : first, second);
        }

        boolean firstIsLead = isLeadCart(first);
        boolean secondIsLead = isLeadCart(second);

        UUID firstChild = getCartChildUUID(first);
        UUID firstParent = getCartParentUUID(first);
        UUID secondChild = getCartChildUUID(second);
        UUID secondParent = getCartParentUUID(second);

        // If first cart is in a train and has no child (tail of its consist), attach second behind it,
        // even if second is a locomotive (mid-train helper or rear pusher locomotive).
        if (firstParent != null && firstChild == null) {
            return Pair.of(first, second);
        }

        // If second cart is in a train and has no child, but first has no parent:
        if (secondParent != null && secondChild == null && firstParent == null) {
            return Pair.of(second, first);
        }

        // If first cart has a child and second is unlinked, append second to the end of first's train
        if (firstChild != null && secondParent == null && secondChild == null) {
            AbstractMinecart tail = getTrainTail(first);
            if (tail != null && !tail.getUUID().equals(second.getUUID())) {
                return Pair.of(tail, second);
            }
        }

        // Two standalone carts where one is a lead locomotive
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

        // Default: respect the player's coupling selection order
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

        // Check if already connected in same train
        UUID currentParentChild = getCartChildUUID(parent);
        UUID currentChildParent = getCartParentUUID(child);
        if (child.getUUID().equals(currentParentChild) && parent.getUUID().equals(currentChildParent)) {
            return true; // Already coupled
        }

        // Prevent circular loops
        if (isConnectedInSameTrain(parent, child)) {
            return false;
        }

        // Disconnect old child of parent if any
        if (currentParentChild != null && !currentParentChild.equals(child.getUUID())) {
            Entity oldChild = level.getEntity(currentParentChild);
            if (oldChild instanceof AbstractMinecart oldChildCart) {
                setCartParent(oldChildCart, null, level);
            }
        }

        // Disconnect old parent of child if any
        if (currentChildParent != null && !currentChildParent.equals(parent.getUUID())) {
            Entity oldParent = level.getEntity(currentChildParent);
            if (oldParent instanceof AbstractMinecart oldParentCart) {
                setCartChild(oldParentCart, null, level);
            }
        }

        setCartChild(parent, child.getUUID(), level);
        setCartParent(child, parent.getUUID(), level);

        // Attempt external mod linkage if present
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

        // Attempt external mod unlink if present
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
        if (isChainedTo(cart, entity) || entity instanceof LivingEntity || entity instanceof AbstractMinecart) {
            return false;
        }
        return !entity.isPassengerOfSameVehicle(cart);
    }

    public static boolean canBeCollidedWith(AbstractMinecart cart, @Nullable Entity other) {
        if (other instanceof AbstractMinecart || other instanceof LivingEntity) {
            return false;
        }
        if (other != null && isChainedTo(cart, other)) {
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

    public static void pushCollidingEntity(AbstractMinecart cart, Entity entity) {
        if (cart.level().isClientSide() || entity.noPhysics || cart.hasPassenger(entity)) {
            return;
        }
        if (isChainedTo(cart, entity)) {
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
                dirX = loco.getFacing().getStepX();
                dirZ = loco.getFacing().getStepZ();
                speed = loco.getLocomotiveSpeed();
            } else if (cart instanceof DieselLocomotiveEntity loco && loco.isActive()) {
                dirX = loco.getFacing().getStepX();
                dirZ = loco.getFacing().getStepZ();
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
                    dirX = loco.getFacing().getStepX();
                    dirZ = loco.getFacing().getStepZ();
                } else if (cart instanceof DieselLocomotiveEntity loco) {
                    dirX = loco.getFacing().getStepX();
                    dirZ = loco.getFacing().getStepZ();
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
            if (shape.isSlope()) {
                y = railPos.getY() + 1.0;
            }
            return new Vec3(x, y, z);
        }
        return null;
    }

    public static boolean moveCartAlongRails(AbstractMinecart cart, double dirX, double dirZ, double distance3D) {
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
        boolean uphillAhead = BaseRailBlock.isRail(cart.level().getBlockState(railPos.relative(cartDir).above()));

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
                if (BaseRailBlock.isRail(cart.level().getBlockState(lowerRail))) {
                    cart.setPos(cart.getX(), cart.getY() - 1.0, cart.getZ());
                }
            }
        }

        Vec3 newRailPos = getRailPosForEntity(cart, cart.getX(), cart.getY(), cart.getZ());
        if (newRailPos != null) {
            cart.setPos(newRailPos.x, newRailPos.y, newRailPos.z);
        }
        return !cart.horizontalCollision;
    }

    public static void pushCartFromLocomotiveBumper(AbstractMinecart loco, AbstractMinecart otherCart) {
        if (loco.level().isClientSide() || isChainedTo(loco, otherCart) || isConnectedInSameTrain(loco, otherCart)) {
            return;
        }

        Direction facing;
        if (loco instanceof BasicLocomotiveEntity basic) {
            facing = basic.getFacing();
        } else if (loco instanceof DieselLocomotiveEntity diesel) {
            facing = diesel.getFacing();
        } else if (loco instanceof ElectricLocomotiveEntity elec) {
            facing = elec.getTravelDirection();
        } else {
            facing = loco.getMotionDirection();
        }

        double dx = otherCart.getX() - loco.getX();
        double dz = otherCart.getZ() - loco.getZ();
        double dot = dx * facing.getStepX() + dz * facing.getStepZ();
        Direction bumperDir = dot >= 0 ? facing : facing.getOpposite();

        double locoRadius = getCartCouplerRadius(loco);
        double cartRadius = getCartCouplerRadius(otherCart);
        double contactDist = locoRadius + cartRadius;

        double dist = dx * bumperDir.getStepX() + dz * bumperDir.getStepZ();
        double penetration = contactDist - dist;

        double locoSpeed = loco.getDeltaMovement().horizontalDistance();
        if (loco instanceof BasicLocomotiveEntity basic && basic.isActive()) {
            locoSpeed = Math.max(locoSpeed, basic.getLocomotiveSpeed());
        } else if (loco instanceof DieselLocomotiveEntity diesel && diesel.isActive()) {
            locoSpeed = Math.max(locoSpeed, diesel.getLocomotiveSpeed());
        } else if (loco instanceof ElectricLocomotiveEntity elec && elec.isActive()) {
            locoSpeed = Math.max(locoSpeed, elec.getLocomotiveSpeed());
        }

        if (penetration > -0.05D) {
            double pushDist = Math.max(penetration + 0.05D, Math.max(locoSpeed, 0.2D));
            moveCartAlongRails(otherCart, bumperDir.getStepX(), bumperDir.getStepZ(), pushDist);
        }

        double pushSpeed = Math.max(locoSpeed, 0.35D);
        otherCart.setDeltaMovement(
                bumperDir.getStepX() * pushSpeed,
                otherCart.getDeltaMovement().y,
                bumperDir.getStepZ() * pushSpeed
        );
        otherCart.needsSync = true;
    }

    public static void pushEntitiesInLocomotivePath(AbstractMinecart loco, Direction travelDir, double locoSpeed) {
        if (loco.level().isClientSide()) return;

        double locoRadius = getCartCouplerRadius(loco);
        double scanReach = locoRadius + 2.5D + Math.max(locoSpeed, 0.5D);

        double minX = Math.min(loco.getX(), loco.getX() + travelDir.getStepX() * scanReach) - 0.75D;
        double maxX = Math.max(loco.getX(), loco.getX() + travelDir.getStepX() * scanReach) + 0.75D;
        double minZ = Math.min(loco.getZ(), loco.getZ() + travelDir.getStepZ() * scanReach) - 0.75D;
        double maxZ = Math.max(loco.getZ(), loco.getZ() + travelDir.getStepZ() * scanReach) + 0.75D;
        double minY = loco.getY() - 0.5D;
        double maxY = loco.getY() + 1.5D;
        AABB scanBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        List<Entity> pushableEntities = loco.level().getEntities(
                loco,
                scanBox,
                e -> !e.noPhysics && !loco.hasPassenger(e) && !isChainedTo(loco, e)
        );

        pushableEntities.sort(Comparator.comparingDouble(e -> {
            double dx = e.getX() - loco.getX();
            double dz = e.getZ() - loco.getZ();
            return dx * travelDir.getStepX() + dz * travelDir.getStepZ();
        }));

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
            } else if (entity instanceof AbstractMinecart cart) {
                if (isConnectedInSameTrain(loco, cart)) {
                    continue;
                }
                pushCartFromLocomotiveBumper(loco, cart);
            }
        }

        // Check if any unchained cart is penetrating the REAR bumper
        Direction oppositeDir = travelDir.getOpposite();
        double rearReach = locoRadius + 1.5D;
        double rMinX = Math.min(loco.getX(), loco.getX() + oppositeDir.getStepX() * rearReach) - 0.75D;
        double rMaxX = Math.max(loco.getX(), loco.getX() + oppositeDir.getStepX() * rearReach) + 0.75D;
        double rMinZ = Math.min(loco.getZ(), loco.getZ() + oppositeDir.getStepZ() * rearReach) - 0.75D;
        double rMaxZ = Math.max(loco.getZ(), loco.getZ() + oppositeDir.getStepZ() * rearReach) + 0.75D;
        AABB rearBox = new AABB(rMinX, minY, rMinZ, rMaxX, maxY, rMaxZ);
        List<AbstractMinecart> rearCarts = loco.level().getEntitiesOfClass(
                AbstractMinecart.class,
                rearBox,
                c -> !c.noPhysics && !loco.hasPassenger(c) && !isChainedTo(loco, c) && !isConnectedInSameTrain(loco, c)
        );
        for (AbstractMinecart rearCart : rearCarts) {
            double dx = rearCart.getX() - loco.getX();
            double dz = rearCart.getZ() - loco.getZ();
            double dist = dx * oppositeDir.getStepX() + dz * oppositeDir.getStepZ();
            double cartRadius = getCartCouplerRadius(rearCart);
            double contactDist = locoRadius + cartRadius;
            double penetration = contactDist - dist;
            if (penetration > 0.0D) {
                moveCartAlongRails(rearCart, oppositeDir.getStepX(), oppositeDir.getStepZ(), penetration + 0.05D);
                rearCart.setDeltaMovement(0.0, rearCart.getDeltaMovement().y, 0.0);
                rearCart.needsSync = true;
            }
        }
    }
}