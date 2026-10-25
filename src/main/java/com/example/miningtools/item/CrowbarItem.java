package com.example.miningtools.item;

import java.util.UUID;
import java.util.function.Consumer;

import com.example.miningtools.util.TrainChainingUtil;
import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class CrowbarItem extends Item {
    public static final String TAG_SELECTED_CART = "SelectedCartUUID";
    public static final String TAG_SELECTED_CART_NAME = "SelectedCartName";

    public CrowbarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hasSelectedCart(stack)) {
            if (!level.isClientSide()) {
                clearSelectedCart(stack);
                player.sendOverlayMessage(
                        Component.literal("Coupling selection cleared.").withStyle(ChatFormatting.YELLOW)
                );
            }
            player.playSound(SoundEvents.DISPENSER_FAIL, 0.6F, 1.4F);
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }

    public static InteractionResult handleCartInteraction(Player player, InteractionHand hand, ItemStack stack, AbstractMinecart cart, Level level) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        // Sneak + Right-Click: Disconnect cart
        if (player.isShiftKeyDown()) {
            clearSelectedCart(stack);
            boolean unlinked = TrainChainingUtil.unlinkCart(cart, serverLevel);

            serverLevel.playSound(null, cart.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 0.9F, 1.1F);
            serverLevel.sendParticles(ParticleTypes.CRIT, cart.getX(), cart.getY() + 0.5, cart.getZ(), 8, 0.2, 0.2, 0.2, 0.1);

            String cartName = cart.getDisplayName().getString();
            player.sendOverlayMessage(
                    Component.literal("Uncoupled train connections for " + cartName).withStyle(ChatFormatting.YELLOW)
            );
            return InteractionResult.SUCCESS;
        }

        // Normal Right-Click: Couple selection logic
        if (!hasSelectedCart(stack)) {
            // First cart selected
            setSelectedCart(stack, cart.getUUID(), cart.getDisplayName().getString());
            serverLevel.playSound(null, cart.blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.8F, 1.3F);
            serverLevel.playSound(null, cart.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.4F, 1.6F);

            player.sendOverlayMessage(
                    Component.literal("Selected " + cart.getDisplayName().getString() + ". Right-click second cart to couple.")
                            .withStyle(ChatFormatting.AQUA)
            );
            return InteractionResult.SUCCESS;
        }

        // Second cart selected
        UUID firstUUID = getSelectedCartUUID(stack);
        if (firstUUID == null || firstUUID.equals(cart.getUUID())) {
            // Same cart clicked twice -> clear selection
            clearSelectedCart(stack);
            serverLevel.playSound(null, cart.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.3F);
            player.sendOverlayMessage(
                    Component.literal("Coupling selection cancelled.").withStyle(ChatFormatting.GRAY)
            );
            return InteractionResult.SUCCESS;
        }

        Entity firstEntity = serverLevel.getEntity(firstUUID);
        if (!(firstEntity instanceof AbstractMinecart firstCart) || firstCart.isRemoved()) {
            clearSelectedCart(stack);
            player.sendOverlayMessage(
                    Component.literal("First cart is no longer valid. Selection cleared.").withStyle(ChatFormatting.RED)
            );
            return InteractionResult.SUCCESS;
        }

        if (firstCart.distanceTo(cart) > 16.0F) {
            player.sendOverlayMessage(
                    Component.literal("Carts are too far apart to couple (maximum 16 blocks).").withStyle(ChatFormatting.RED)
            );
            return InteractionResult.SUCCESS;
        }

        // Determine parent/child roles and link them
        Pair<AbstractMinecart, AbstractMinecart> roles = TrainChainingUtil.determineRoles(firstCart, cart);
        if (roles == null) {
            clearSelectedCart(stack);
            serverLevel.playSound(null, cart.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.7F, 1.2F);
            player.sendOverlayMessage(
                    Component.literal("Could not couple these carts.")
                            .withStyle(ChatFormatting.RED)
            );
            return InteractionResult.SUCCESS;
        }
        AbstractMinecart parent = roles.getFirst();
        AbstractMinecart child = roles.getSecond();

        boolean success = TrainChainingUtil.linkCarts(parent, child, serverLevel);
        clearSelectedCart(stack);

        if (success) {
            serverLevel.playSound(null, cart.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.9F);
            serverLevel.playSound(null, cart.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4F, 1.7F);
            serverLevel.sendParticles(ParticleTypes.CRIT, (parent.getX() + child.getX()) * 0.5, (parent.getY() + child.getY()) * 0.5 + 0.4, (parent.getZ() + child.getZ()) * 0.5, 12, 0.25, 0.25, 0.25, 0.1);

            player.sendOverlayMessage(
                    Component.literal("Coupled " + parent.getDisplayName().getString() + " -> " + child.getDisplayName().getString() + "!")
                            .withStyle(ChatFormatting.GREEN)
            );
        } else {
            player.sendOverlayMessage(
                    Component.literal("Failed to couple carts.").withStyle(ChatFormatting.RED)
            );
        }

        return InteractionResult.SUCCESS;
    }

    public static boolean hasSelectedCart(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.contains(TAG_SELECTED_CART);
    }

    public static @org.jspecify.annotations.Nullable UUID getSelectedCartUUID(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains(TAG_SELECTED_CART)) {
            String uuidStr = customData.copyTag().getString(TAG_SELECTED_CART).orElse(null);
            if (uuidStr != null) {
                try {
                    return UUID.fromString(uuidStr);
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    public static @org.jspecify.annotations.Nullable String getSelectedCartName(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains(TAG_SELECTED_CART_NAME)) {
            return customData.copyTag().getString(TAG_SELECTED_CART_NAME).orElse(null);
        }
        return null;
    }

    public static void setSelectedCart(ItemStack stack, UUID cartUUID, String cartName) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putString(TAG_SELECTED_CART, cartUUID.toString());
        tag.putString(TAG_SELECTED_CART_NAME, cartName);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void clearSelectedCart(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            tag.remove(TAG_SELECTED_CART);
            tag.remove(TAG_SELECTED_CART_NAME);
            if (tag.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_DATA);
            } else {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipOutput, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipOutput, tooltipFlag);
        String selectedName = getSelectedCartName(stack);
        if (selectedName != null && !selectedName.isBlank()) {
            tooltipOutput.accept(
                    Component.literal("Selected: " + selectedName).withStyle(ChatFormatting.AQUA)
            );
            tooltipOutput.accept(
                    Component.literal("Right-click another cart to connect, or right-click air to cancel.").withStyle(ChatFormatting.GRAY)
            );
        } else {
            tooltipOutput.accept(
                    Component.literal("Right-click one cart, then another to couple them.").withStyle(ChatFormatting.GRAY)
            );
            tooltipOutput.accept(
                    Component.literal("Sneak + Right-click a cart to disconnect.").withStyle(ChatFormatting.GRAY)
            );
            tooltipOutput.accept(
                    Component.literal("No iron chains required.").withStyle(ChatFormatting.DARK_GREEN)
            );
        }
    }
}
