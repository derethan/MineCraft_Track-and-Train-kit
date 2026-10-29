package com.example.miningtools.block.entity;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.CouplerRailBlock;
import com.example.miningtools.util.TrainChainingUtil;
import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Coupler Rail Block Entity:
 * Handles automated locomotive coupling and uncoupling on track arrival.
 * Features trip tracking to ensure that a parked locomotive only connects
 * or disconnects ONCE per trip until it completely leaves the track piece.
 */
public class CouplerRailBlockEntity extends BlockEntity {

    private @Nullable UUID lastTriggeredLocoUUID = null;
    private int cooldown = 0;

    public CouplerRailBlockEntity(BlockPos pos, BlockState state) {
        super(MiningTools.COUPLER_RAIL_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CouplerRailBlockEntity be) {
        if (be.cooldown > 0) {
            be.cooldown--;
        }

        AABB trackBox = new AABB(pos).inflate(0.35, 0.8, 0.35);
        List<AbstractMinecart> locos = level.getEntitiesOfClass(
                AbstractMinecart.class,
                trackBox,
                c -> c.isAlive() && !c.isRemoved() && TrainChainingUtil.isLocomotive(c)
        );

        if (locos.isEmpty()) {
            // Locomotive has departed! Reset trip state for next arrival
            be.lastTriggeredLocoUUID = null;
            return;
        }

        AbstractMinecart loco = locos.get(0);

        // If this locomotive already triggered on this trip and remains on the rail, do not trigger again!
        if (loco.getUUID().equals(be.lastTriggeredLocoUUID)) {
            return;
        }

        if (be.cooldown > 0) {
            return;
        }

        // Register this locomotive for this trip
        be.lastTriggeredLocoUUID = loco.getUUID();
        be.cooldown = 30; // 1.5s debounce

        boolean redstone = level.hasNeighborSignal(pos);
        boolean manualCouple = state.getValue(CouplerRailBlock.COUPLE_MODE);
        // Redstone signal toggles/inverts the manual mode
        boolean effectiveCouple = redstone ? !manualCouple : manualCouple;

        if (effectiveCouple) {
            be.coupleNearestCart(level, pos, loco);
        } else {
            be.decoupleLocomotive(level, pos, loco);
        }
    }

    private void coupleNearestCart(Level level, BlockPos pos, AbstractMinecart loco) {
        if (!(level instanceof ServerLevel sl)) return;

        AABB searchBox = loco.getBoundingBox().inflate(16.0D, 4.0D, 16.0D);
        List<AbstractMinecart> candidates = level.getEntitiesOfClass(
                AbstractMinecart.class,
                searchBox,
                c -> c.isAlive()
                        && !c.isRemoved()
                        && !c.getUUID().equals(loco.getUUID())
                        && !TrainChainingUtil.isConnectedInSameTrain(loco, c)
        );

        if (candidates.isEmpty()) {
            sl.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.6F, 1.2F);
            notifyLocoRider(loco, Component.literal("Coupler Rail: No unlinked cart found within 16 blocks.").withStyle(ChatFormatting.RED));
            return;
        }

        candidates.sort(Comparator.comparingDouble(c -> c.distanceToSqr(loco)));

        boolean coupled = false;
        for (AbstractMinecart target : candidates) {
            Pair<AbstractMinecart, AbstractMinecart> roles = TrainChainingUtil.determineRoles(loco, target);
            if (roles != null) {
                boolean success = TrainChainingUtil.linkCarts(roles.getFirst(), roles.getSecond(), sl);
                if (success) {
                    coupled = true;
                    sl.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
                    sl.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4F, 1.7F);
                    sl.sendParticles(ParticleTypes.CRIT,
                            (loco.getX() + target.getX()) * 0.5,
                            (loco.getY() + target.getY()) * 0.5 + 0.5,
                            (loco.getZ() + target.getZ()) * 0.5,
                            12, 0.25, 0.25, 0.25, 0.1);

                    String locoName = loco.getDisplayName().getString();
                    String targetName = target.getDisplayName().getString();
                    notifyLocoRider(loco, Component.literal("Coupled " + targetName + " to " + locoName + "!").withStyle(ChatFormatting.GREEN));
                    break;
                }
            }
        }

        if (!coupled) {
            sl.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.6F, 1.2F);
            notifyLocoRider(loco, Component.literal("Coupler Rail: Could not couple nearest cart (couplers busy or misaligned).").withStyle(ChatFormatting.RED));
        }
    }

    private void decoupleLocomotive(Level level, BlockPos pos, AbstractMinecart loco) {
        if (!(level instanceof ServerLevel sl)) return;

        UUID childUUID = TrainChainingUtil.getCartChildUUID(loco);
        UUID parentUUID = TrainChainingUtil.getCartParentUUID(loco);

        if (childUUID == null && parentUUID == null) {
            sl.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.6F, 1.2F);
            notifyLocoRider(loco, Component.literal("Coupler Rail: Locomotive has no connected carts to uncouple.").withStyle(ChatFormatting.YELLOW));
            return;
        }

        AbstractMinecart uncoupledCart = null;

        // Disconnect trailing cart (child) if present
        if (childUUID != null) {
            Entity childEnt = sl.getEntity(childUUID);
            if (childEnt instanceof AbstractMinecart childCart) {
                TrainChainingUtil.setCartChild(loco, null, sl);
                TrainChainingUtil.setCartParent(childCart, null, sl);
                uncoupledCart = childCart;
            } else {
                TrainChainingUtil.setCartChild(loco, null, sl);
            }
        } else if (parentUUID != null) {
            // Disconnect leading / pushed cart (parent)
            Entity parentEnt = sl.getEntity(parentUUID);
            if (parentEnt instanceof AbstractMinecart parentCart) {
                TrainChainingUtil.setCartChild(parentCart, null, sl);
                TrainChainingUtil.setCartParent(loco, null, sl);
                uncoupledCart = parentCart;
            } else {
                TrainChainingUtil.setCartParent(loco, null, sl);
            }
        }

        sl.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        sl.sendParticles(ParticleTypes.CRIT, loco.getX(), loco.getY() + 0.5, loco.getZ(), 10, 0.25, 0.25, 0.25, 0.1);

        String locoName = loco.getDisplayName().getString();
        String cartName = (uncoupledCart != null) ? uncoupledCart.getDisplayName().getString() : "consist";
        notifyLocoRider(loco, Component.literal("Uncoupled " + cartName + " from " + locoName + "!").withStyle(ChatFormatting.YELLOW));
    }

    private static void notifyLocoRider(AbstractMinecart loco, Component msg) {
        for (Entity passenger : loco.getPassengers()) {
            if (passenger instanceof ServerPlayer sp) {
                sp.sendSystemMessage(msg, true);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.lastTriggeredLocoUUID != null) {
            output.putString("LastTriggeredLoco", this.lastTriggeredLocoUUID.toString());
        }
        output.putInt("Cooldown", this.cooldown);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String uuidStr = input.getString("LastTriggeredLoco").orElse(null);
        if (uuidStr != null && !uuidStr.isBlank()) {
            try {
                this.lastTriggeredLocoUUID = UUID.fromString(uuidStr);
            } catch (Throwable ignored) {
                this.lastTriggeredLocoUUID = null;
            }
        } else {
            this.lastTriggeredLocoUUID = null;
        }
        this.cooldown = input.getInt("Cooldown").orElse(0);
    }
}
