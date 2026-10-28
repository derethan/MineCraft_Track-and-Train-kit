package com.example.miningtools.item;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.block.RailwaySignalBlock;
import com.example.miningtools.block.TrackSwitchBlock;
import com.example.miningtools.block.entity.RailwaySignalBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SignalTunerItem extends Item {

    public static final String TAG_SELECTED_SIGNAL = "SelectedSignal";
    public static final String TAG_SIGNAL_X = "SignalX";
    public static final String TAG_SIGNAL_Y = "SignalY";
    public static final String TAG_SIGNAL_Z = "SignalZ";

    public SignalTunerItem(Properties properties) {
        super(properties);
    }

    public static boolean hasSelectedSignal(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.contains(TAG_SELECTED_SIGNAL);
    }

    public static @Nullable BlockPos getSelectedSignalPos(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains(TAG_SELECTED_SIGNAL)) {
            CompoundTag tag = customData.copyTag();
            int x = tag.getInt(TAG_SIGNAL_X).orElse(0);
            int y = tag.getInt(TAG_SIGNAL_Y).orElse(0);
            int z = tag.getInt(TAG_SIGNAL_Z).orElse(0);
            return new BlockPos(x, y, z);
        }
        return null;
    }

    public static void setSelectedSignal(ItemStack stack, BlockPos pos) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putBoolean(TAG_SELECTED_SIGNAL, true);
        tag.putInt(TAG_SIGNAL_X, pos.getX());
        tag.putInt(TAG_SIGNAL_Y, pos.getY());
        tag.putInt(TAG_SIGNAL_Z, pos.getZ());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void clearSelectedSignal(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            tag.remove(TAG_SELECTED_SIGNAL);
            tag.remove(TAG_SIGNAL_X);
            tag.remove(TAG_SIGNAL_Y);
            tag.remove(TAG_SIGNAL_Z);
            if (tag.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_DATA);
            } else {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockState state = level.getBlockState(clickedPos);

        if (state.getBlock() instanceof RailwaySignalBlock) {
            if (!level.isClientSide() && player != null) {
                BlockEntity be = level.getBlockEntity(clickedPos);
                if (be instanceof RailwaySignalBlockEntity signalBe) {
                    BlockPos storedPos = getSelectedSignalPos(stack);

                    if (player.isShiftKeyDown()) {
                        // Cycle manual test aspect override
                        signalBe.cycleManualOverride();
                        String aspectDesc = (signalBe.getManualOverride() != null)
                                ? signalBe.getManualOverride().name() + " (Manual Override)"
                                : "AUTOMATIC (Track Circuit)";
                        player.sendOverlayMessage(Component.literal("Signal Aspect: " + aspectDesc).withStyle(ChatFormatting.GOLD));
                        level.playSound(null, clickedPos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
                        return InteractionResult.SUCCESS;
                    }

                    if (storedPos == null) {
                        // Store Signal 1
                        setSelectedSignal(stack, clickedPos);
                        player.sendOverlayMessage(Component.literal("Selected Signal at " + clickedPos.toShortString() + ". Right-click another signal to establish Block Section.").withStyle(ChatFormatting.AQUA));
                        level.playSound(null, clickedPos, SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.8F, 1.2F);
                        return InteractionResult.SUCCESS;
                    } else if (storedPos.equals(clickedPos)) {
                        // Clicked same signal without shift -> toggle Passing Siding vs ABS mode
                        boolean nextMode = !signalBe.isPassingSidingMode();
                        signalBe.setPassingSidingMode(nextMode);
                        player.sendOverlayMessage(Component.literal("Signal Mode: " + (nextMode ? "Passing Siding (Bidirectional)" : "Automatic Block Signal (Unidirectional)")).withStyle(ChatFormatting.YELLOW));
                        level.playSound(null, clickedPos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.0F);
                        return InteractionResult.SUCCESS;
                    } else {
                        // Link Signal 1 and Signal 2 mutually
                        BlockEntity be1 = level.getBlockEntity(storedPos);
                        if (be1 instanceof RailwaySignalBlockEntity signal1) {
                            signal1.setLinkedPartnerSignal(clickedPos);
                            signalBe.setLinkedPartnerSignal(storedPos);
                            clearSelectedSignal(stack);
                            int dist = (int) Math.sqrt(clickedPos.distSqr(storedPos));
                            player.sendOverlayMessage(Component.literal("Linked Block Section between " + storedPos.toShortString() + " and " + clickedPos.toShortString() + " (" + dist + "m)").withStyle(ChatFormatting.GREEN));
                            level.playSound(null, clickedPos, SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.8F, 1.5F);
                            return InteractionResult.SUCCESS;
                        } else {
                            player.sendOverlayMessage(Component.literal("Previously selected signal at " + storedPos.toShortString() + " is no longer valid.").withStyle(ChatFormatting.RED));
                            clearSelectedSignal(stack);
                            return InteractionResult.FAIL;
                        }
                    }
                }
            }
            return InteractionResult.SUCCESS;
        } else if (state.getBlock() instanceof TrackSwitchBlock) {
            if (!level.isClientSide() && player != null) {
                BlockPos storedPos = getSelectedSignalPos(stack);
                if (storedPos != null) {
                    BlockEntity be = level.getBlockEntity(storedPos);
                    if (be instanceof RailwaySignalBlockEntity signalBe) {
                        signalBe.setLinkedSwitchPos(clickedPos);
                        clearSelectedSignal(stack);
                        player.sendOverlayMessage(Component.literal("Linked Track Switch at " + clickedPos.toShortString() + " to Signal at " + storedPos.toShortString() + " for automated siding routing.").withStyle(ChatFormatting.GOLD));
                        level.playSound(null, clickedPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    player.sendOverlayMessage(Component.literal("Select a Railway Signal first, then right-click this Track Switch to link it.").withStyle(ChatFormatting.YELLOW));
                    return InteractionResult.CONSUME;
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (hasSelectedSignal(stack)) {
                if (!level.isClientSide()) {
                    clearSelectedSignal(stack);
                    player.sendOverlayMessage(Component.literal("Signal Tuner memory cleared.").withStyle(ChatFormatting.GRAY));
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.6F, 0.8F);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipOutput, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipOutput, tooltipFlag);
        BlockPos stored = getSelectedSignalPos(stack);
        if (stored != null) {
            tooltipOutput.accept(Component.literal("Selected Signal: " + stored.toShortString()).withStyle(ChatFormatting.AQUA));
            tooltipOutput.accept(Component.literal("Right-click another signal to link Block Section.").withStyle(ChatFormatting.GRAY));
            tooltipOutput.accept(Component.literal("Right-click a Track Switch to link passing siding.").withStyle(ChatFormatting.GOLD));
            tooltipOutput.accept(Component.literal("Sneak + Right-click air to clear memory.").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltipOutput.accept(Component.literal("Right-click a Signal to select it.").withStyle(ChatFormatting.GRAY));
            tooltipOutput.accept(Component.literal("Right-click a 2nd Signal to establish Block Section.").withStyle(ChatFormatting.GRAY));
            tooltipOutput.accept(Component.literal("Right-click selected Signal to toggle Siding / ABS mode.").withStyle(ChatFormatting.GRAY));
            tooltipOutput.accept(Component.literal("Sneak + Right-click Signal to cycle manual test aspect.").withStyle(ChatFormatting.GRAY));
        }
    }
}
