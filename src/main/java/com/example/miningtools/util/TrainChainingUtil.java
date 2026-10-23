package com.example.miningtools.util;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
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
import com.example.miningtools.network.TrainLinkSyncPayload;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TrainChainingUtil {
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
}
