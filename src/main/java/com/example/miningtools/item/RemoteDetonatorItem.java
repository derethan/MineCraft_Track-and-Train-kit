package com.example.miningtools.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.example.miningtools.Config;
import com.example.miningtools.MiningTools;
import com.example.miningtools.block.SurveyorsChargeBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class RemoteDetonatorItem extends Item {

    public RemoteDetonatorItem(Properties properties) {
        super(properties);
    }

    private static double getDetonatorRange() {
        try {
            if (Config.REMOTE_DETONATOR_RANGE != null) {
                return Config.REMOTE_DETONATOR_RANGE.get();
            }
        } catch (Throwable ignored) {}
        return 12.0D;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.LEVER_CLICK,
                SoundSource.PLAYERS,
                0.8F,
                1.5F
        );

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        double maxRange = getDetonatorRange();
        double maxRangeSq = maxRange * maxRange;
        BlockPos playerPos = player.blockPosition();
        int boxRange = (int) Math.ceil(maxRange);

        List<BlockPos> chargesToDetonate = new ArrayList<>();
        for (BlockPos targetPos : BlockPos.betweenClosed(
                playerPos.offset(-boxRange, -boxRange, -boxRange),
                playerPos.offset(boxRange, boxRange, boxRange))) {
            if (targetPos.distSqr(playerPos) <= maxRangeSq) {
                if (level.getBlockState(targetPos).is(MiningTools.SURVEYORS_CHARGE.get())) {
                    chargesToDetonate.add(targetPos.immutable());
                }
            }
        }

        if (chargesToDetonate.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal("§c[Remote Detonator] No Surveyor's Charges found within " + (int) maxRange + " blocks.")
            );
            player.getCooldowns().addCooldown(heldStack, 10);
            return InteractionResult.CONSUME;
        }

        player.sendSystemMessage(
                Component.literal("§a[Remote Detonator] Detonating " + chargesToDetonate.size() + " Surveyor's Charge(s)...")
        );

        for (BlockPos chargePos : chargesToDetonate) {
            SurveyorsChargeBlock.detonate(level, chargePos, player);
        }

        player.getCooldowns().addCooldown(heldStack, 15);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipOutput, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipOutput, tooltipFlag);
        tooltipOutput.accept(Component.literal("Remotely triggers all Surveyor's Charges within 12 blocks.").withStyle(ChatFormatting.GRAY));
        tooltipOutput.accept(Component.literal("Right-click to detonate.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
