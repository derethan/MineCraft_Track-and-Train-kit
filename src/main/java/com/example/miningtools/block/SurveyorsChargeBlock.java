package com.example.miningtools.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;

public class SurveyorsChargeBlock extends Block {

    private static final Set<String> DEFAULT_IGNORED = Set.of(
            "minecraft:air",
            "minecraft:cave_air",
            "minecraft:void_air",
            "minecraft:stone",
            "minecraft:cobblestone",
            "minecraft:smooth_stone",
            "minecraft:deepslate",
            "minecraft:cobbled_deepslate",
            "minecraft:dirt",
            "minecraft:coarse_dirt",
            "minecraft:grass_block",
            "minecraft:podzol",
            "minecraft:sand",
            "minecraft:red_sand",
            "minecraft:sandstone",
            "minecraft:red_sandstone",
            "minecraft:gravel",
            "minecraft:water",
            "minecraft:bedrock",
            "minecraft:netherrack",
            "minecraft:basalt",
            "minecraft:blackstone",
            "minecraft:andesite",
            "minecraft:diorite",
            "minecraft:granite",
            "minecraft:tuff",
            "minecraft:calcite",
            "minecraft:dripstone_block",
            "miningtools:surveyors_charge"
    );

    public SurveyorsChargeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving);
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            detonate(level, pos, null);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(state.getBlock()) && level.hasNeighborSignal(pos)) {
            detonate(level, pos, null);
        }
    }

    private static int getScanRadius() {
        try {
            if (Config.SURVEYOR_CHARGE_RADIUS != null) {
                return Config.SURVEYOR_CHARGE_RADIUS.get();
            }
        } catch (Throwable ignored) {}
        return 10;
    }

    private static boolean shouldDropBoreholeBlocks() {
        try {
            if (Config.SURVEYOR_CHARGE_DROP_BOREHOLE_BLOCKS != null) {
                return Config.SURVEYOR_CHARGE_DROP_BOREHOLE_BLOCKS.get();
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static Set<String> getIgnoredBlocks() {
        try {
            if (Config.SURVEYOR_CHARGE_IGNORED_BLOCKS != null && Config.SURVEYOR_CHARGE_IGNORED_BLOCKS.get() != null) {
                return new HashSet<>(Config.SURVEYOR_CHARGE_IGNORED_BLOCKS.get());
            }
        } catch (Throwable ignored) {}
        return DEFAULT_IGNORED;
    }

    private static Set<String> getExtraReportBlocks() {
        try {
            if (Config.SURVEYOR_CHARGE_EXTRA_REPORT_BLOCKS != null && Config.SURVEYOR_CHARGE_EXTRA_REPORT_BLOCKS.get() != null) {
                return new HashSet<>(Config.SURVEYOR_CHARGE_EXTRA_REPORT_BLOCKS.get());
            }
        } catch (Throwable ignored) {}
        return Collections.emptySet();
    }

    /**
     * Detonates the Surveyor's Charge:
     * 1. Self-destructs with a loud explosion sound and smoke particles.
     * 2. Blasts a 1x1 vertical borehole downwards until reaching bedrock or indestructible blocks.
     * 3. Scans a spherical volume of the configured radius around the charge for blocks and oil deposits.
     * 4. Compiles and delivers a formatted Geological Survey Report to the triggering player or nearby players.
     */
    public static void detonate(Level level, BlockPos pos, @Nullable Player triggeringPlayer) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Remove the block immediately to prevent re-triggering
        level.removeBlock(pos, false);

        // Sound & Particle effects (loud bang)
        try {
            serverLevel.playSound(
                    null,
                    pos,
                    SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.BLOCKS,
                    4.0F,
                    (1.0F + (serverLevel.getRandom().nextFloat() - serverLevel.getRandom().nextFloat()) * 0.2F) * 0.7F
            );
            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    1,
                    0,
                    0,
                    0,
                    0
            );
            serverLevel.sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    30,
                    0.35D,
                    0.35D,
                    0.35D,
                    0.05D
            );
        } catch (Throwable ignored) {}

        // 1. Blast 1x1 borehole straight down until bedrock / indestructible block
        int startY = pos.getY() - 1;
        int minY = level.getMinY();
        int lowestBlastedY = startY;
        boolean dropBlocks = shouldDropBoreholeBlocks();

        for (int y = startY; y >= minY; y--) {
            BlockPos shaftPos = new BlockPos(pos.getX(), y, pos.getZ());
            BlockState shaftState = level.getBlockState(shaftPos);

            // Stop when hitting bedrock or any indestructible block (destroySpeed < 0)
            if (shaftState.getDestroySpeed(level, shaftPos) < 0.0F) {
                lowestBlastedY = y + 1;
                break;
            }

            if (!shaftState.isAir()) {
                if (dropBlocks && !shaftState.liquid()) {
                    level.destroyBlock(shaftPos, true);
                } else {
                    level.setBlock(shaftPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            lowestBlastedY = y;
        }

        // 2. Perform geological scan in configured radius around charge
        int radius = getScanRadius();
        Set<String> ignored = getIgnoredBlocks();
        Set<String> extraReport = getExtraReportBlocks();

        Map<Block, Integer> blockCounts = new HashMap<>();
        int totalOilBlocks = 0;
        int oilSourceBlocks = 0;
        int rSq = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > rSq) {
                        continue;
                    }
                    BlockPos scanPos = pos.offset(dx, dy, dz);
                    BlockState scanState = level.getBlockState(scanPos);
                    FluidState fluidState = level.getFluidState(scanPos);

                    boolean isOil = false;
                    if (isOilFluid(fluidState)) {
                        isOil = true;
                        totalOilBlocks++;
                        if (fluidState.isSource()) {
                            oilSourceBlocks++;
                        }
                    } else if (isOilBlock(scanState)) {
                        isOil = true;
                        totalOilBlocks++;
                    }

                    if (scanState.isAir()) {
                        continue;
                    }

                    Identifier id = BuiltInRegistries.BLOCK.getKey(scanState.getBlock());
                    String idStr = id != null ? id.toString() : "";

                    if (extraReport.contains(idStr) || (!ignored.contains(idStr) && !isOil)) {
                        blockCounts.merge(scanState.getBlock(), 1, Integer::sum);
                    }
                }
            }
        }

        // 3. Format Geological Survey Report
        List<Component> report = new ArrayList<>();
        report.add(Component.literal("§6§m============================================="));
        report.add(Component.literal("§6§l[Surveyor's Charge] §eGeological Survey Report"));
        report.add(Component.literal(String.format("§7Location: §f[%d, %d, %d] §8| §7Scan Radius: §f%d blocks",
                pos.getX(), pos.getY(), pos.getZ(), radius)));
        int depth = Math.max(0, startY - lowestBlastedY + 1);
        report.add(Component.literal(String.format("§7Borehole blasted from Y=§f%d §7down to Y=§f%d §8(%d blocks deep)",
                startY, lowestBlastedY, depth)));
        report.add(Component.literal("§6§m---------------------------------------------"));

        // Oil findings
        if (totalOilBlocks > 0) {
            report.add(Component.literal(String.format("§6★ §e§lOil Deposits Detected: §a%d block(s) §r§7(%d source reservoir(s))",
                    totalOilBlocks, oilSourceBlocks)));
        } else {
            report.add(Component.literal("§7▶ Oil Deposits: §8None detected within survey radius"));
        }

        // Mineral & block findings
        if (blockCounts.isEmpty()) {
            report.add(Component.literal("§7▶ Mineral Resources: §8No rare ores or materials detected (common blocks filtered)"));
        } else {
            report.add(Component.literal("§e▶ Detected Mineral & Resource Deposits:"));
            List<Map.Entry<Block, Integer>> entries = new ArrayList<>(blockCounts.entrySet());
            entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            for (Map.Entry<Block, Integer> entry : entries) {
                report.add(Component.literal(String.format("  §f• %s§7: §e%d",
                        entry.getKey().getName().getString(), entry.getValue())));
            }
        }
        report.add(Component.literal("§6§m============================================="));

        // 4. Send report to triggering player or nearest player
        Player target = triggeringPlayer;
        if (target == null) {
            target = serverLevel.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 48.0D, false);
        }
        if (target != null) {
            for (Component line : report) {
                target.sendSystemMessage(line);
            }
        }

        // Also deliver to any players in immediate vicinity (within 16 blocks)
        List<ServerPlayer> nearby = serverLevel.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(16.0D));
        for (ServerPlayer player : nearby) {
            if (player != target) {
                for (Component line : report) {
                    player.sendSystemMessage(line);
                }
            }
        }
    }

    public static boolean isOilFluid(FluidState fluidState) {
        if (fluidState.isEmpty()) {
            return false;
        }
        Fluid fluid = fluidState.getType();
        try {
            Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
            if (id != null) {
                String path = id.getPath().toLowerCase();
                String ns = id.getNamespace().toLowerCase();
                if (path.contains("oil") || path.contains("crude") || path.contains("petroleum")
                        || path.contains("fuel") || path.contains("diesel") || ns.contains("oil")) {
                    return true;
                }
            }
            return fluid.builtInRegistryHolder().tags().anyMatch(tag -> {
                String tagPath = tag.location().getPath().toLowerCase();
                return tagPath.contains("oil") || tagPath.contains("crude") || tagPath.contains("fuel");
            });
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isOilBlock(BlockState state) {
        Block block = state.getBlock();
        try {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id != null) {
                String path = id.getPath().toLowerCase();
                String ns = id.getNamespace().toLowerCase();
                if (path.contains("oil") || path.contains("crude") || path.contains("petroleum") || ns.contains("oil")) {
                    return true;
                }
            }
            return block.builtInRegistryHolder().tags().anyMatch(tag -> {
                String tagPath = tag.location().getPath().toLowerCase();
                return tagPath.contains("oil") || tagPath.contains("crude");
            });
        } catch (Throwable ignored) {
            return false;
        }
    }
}
