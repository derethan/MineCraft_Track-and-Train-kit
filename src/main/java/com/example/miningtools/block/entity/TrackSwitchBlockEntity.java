package com.example.miningtools.block.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.TrackSwitchBlock;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TrackSwitchBlockEntity extends BlockEntity {
    public static final int MIN_DETECT_RANGE = 3;
    public static final int MAX_DETECT_RANGE = 16;
    public static final int DEFAULT_DETECT_RANGE = 8;
    public static final int MAX_RULES = 32;

    public record SwitchRule(String trainName, boolean openState) {
        public static final StreamCodec<ByteBuf, SwitchRule> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(32),
                SwitchRule::trainName,
                ByteBufCodecs.BOOL,
                SwitchRule::openState,
                SwitchRule::new
        );
    }

    private final List<SwitchRule> rules = new ArrayList<>();
    private int detectRange = DEFAULT_DETECT_RANGE;
    private boolean forceCheckStationary = false;

    public TrackSwitchBlockEntity(BlockPos pos, BlockState blockState) {
        super(MiningTools.TRACK_SWITCH_BLOCK_ENTITY.get(), pos, blockState);
    }

    public List<SwitchRule> getRules() {
        return this.rules;
    }

    public void setRules(List<SwitchRule> newRules) {
        this.rules.clear();
        for (SwitchRule r : newRules) {
            if (this.rules.size() >= MAX_RULES) {
                break;
            }
            String cleaned = r.trainName() == null ? "" : r.trainName().trim();
            if (!cleaned.isEmpty()) {
                if (cleaned.length() > 32) {
                    cleaned = cleaned.substring(0, 32);
                }
                this.rules.add(new SwitchRule(cleaned, r.openState()));
            }
        }
        this.forceCheckStationary = true;
        this.setChanged();
    }

    public int getDetectRange() {
        return this.detectRange;
    }

    public void setDetectRange(int range) {
        this.detectRange = Mth.clamp(range, MIN_DETECT_RANGE, MAX_DETECT_RANGE);
        this.forceCheckStationary = true;
        this.setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TrackSwitchBlockEntity blockEntity) {
        if (level.isClientSide() || blockEntity.rules.isEmpty()) {
            return;
        }
        boolean checkStationaryNow = blockEntity.forceCheckStationary;
        if (!checkStationaryNow && (level.getGameTime() % 2L != 0L)) {
            return;
        }
        blockEntity.forceCheckStationary = false;

        int range = blockEntity.detectRange;
        AABB searchBox = new AABB(pos).inflate(range, 3.5, range);
        List<AbstractMinecart> carts = level.getEntitiesOfClass(AbstractMinecart.class, searchBox, c -> !c.isRemoved());
        if (carts.isEmpty()) {
            return;
        }

        Vec3 switchCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        carts.sort(Comparator.comparingDouble(c -> c.position().distanceToSqr(switchCenter)));

        for (AbstractMinecart cart : carts) {
            double dx = switchCenter.x - cart.getX();
            double dz = switchCenter.z - cart.getZ();
            double horizDist = Math.hypot(dx, dz);
            if (horizDist > range + 0.75) {
                continue;
            }

            if (!isApproachingOrAtSwitch(cart, dx, dz, horizDist, checkStationaryNow)) {
                continue;
            }

            String trainName = BasicLocomotiveEntity.resolveTrainName(cart);
            SwitchRule matched = blockEntity.findMatchingRule(trainName);
            if (matched != null) {
                boolean currentOpen = state.getValue(TrackSwitchBlock.OPEN);
                if (currentOpen != matched.openState()) {
                    TrackSwitchBlock.setSwitchState(level, pos, state, matched.openState(), true);
                }
                return;
            }
        }
    }

    private static boolean isApproachingOrAtSwitch(AbstractMinecart cart, double dxToSwitch, double dzToSwitch, double horizDist, boolean allowStationary) {
        // If right next to the switch/rail (within 2.5 blocks), always allow
        if (horizDist <= 2.5) {
            return true;
        }

        Vec3 vel = cart.getDeltaMovement();
        double vx = vel.x;
        double vz = vel.z;

        BasicLocomotiveEntity loco = BasicLocomotiveEntity.getConnectedLocomotive(cart);
        if (loco != null && loco.isRunning()) {
            Direction facing = loco.getFacing();
            if (vx * vx + vz * vz < 1.0E-4) {
                vx = facing.getStepX() * 0.1;
                vz = facing.getStepZ() * 0.1;
            }
        } else {
            ElectricLocomotiveEntity eloco = ElectricLocomotiveEntity.getConnectedLocomotive(cart);
            if (eloco != null && eloco.isRunning()) {
                Direction facing = eloco.getFacing();
                if (vx * vx + vz * vz < 1.0E-4) {
                    vx = facing.getStepX() * 0.1;
                    vz = facing.getStepZ() * 0.1;
                }
            }
        }

        double speed = Math.hypot(vx, vz);
        if (speed < 1.0E-3) {
            return allowStationary;
        }

        // Dot product between velocity vector and vector toward the switch
        double dot = (vx * dxToSwitch + vz * dzToSwitch) / (speed * Math.max(0.001, horizDist));
        return dot >= -0.15;
    }

    public @Nullable SwitchRule findMatchingRule(String trainName) {
        String cleanTrain = trainName == null ? "" : trainName.trim();

        // 1. Exact case-insensitive match
        if (!cleanTrain.isEmpty()) {
            for (SwitchRule rule : this.rules) {
                if (rule.trainName().equalsIgnoreCase(cleanTrain)) {
                    return rule;
                }
            }
        }

        // 2. Substring or wildcard match
        String lowerTrain = cleanTrain.toLowerCase(Locale.ROOT);
        for (SwitchRule rule : this.rules) {
            String rName = rule.trainName().trim();
            if (rName.equals("*") || rName.equalsIgnoreCase("any")) {
                return rule;
            }
            if (rName.endsWith("*") && rName.length() > 1 && !lowerTrain.isEmpty()) {
                String prefix = rName.substring(0, rName.length() - 1).trim().toLowerCase(Locale.ROOT);
                if (lowerTrain.startsWith(prefix)) {
                    return rule;
                }
            }
            if (!lowerTrain.isEmpty() && lowerTrain.contains(rName.toLowerCase(Locale.ROOT))) {
                return rule;
            }
        }

        return null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("DetectRange", this.detectRange);
        output.putInt("RuleCount", this.rules.size());
        for (int i = 0; i < this.rules.size(); i++) {
            SwitchRule rule = this.rules.get(i);
            output.putString("RuleName_" + i, rule.trainName());
            output.putBoolean("RuleOpen_" + i, rule.openState());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.detectRange = Mth.clamp(input.getIntOr("DetectRange", DEFAULT_DETECT_RANGE), MIN_DETECT_RANGE, MAX_DETECT_RANGE);
        int count = Mth.clamp(input.getIntOr("RuleCount", 0), 0, MAX_RULES);
        this.rules.clear();
        for (int i = 0; i < count; i++) {
            String name = input.getStringOr("RuleName_" + i, "").trim();
            boolean open = input.getBooleanOr("RuleOpen_" + i, true);
            if (!name.isEmpty()) {
                this.rules.add(new SwitchRule(name, open));
            }
        }
    }
}
