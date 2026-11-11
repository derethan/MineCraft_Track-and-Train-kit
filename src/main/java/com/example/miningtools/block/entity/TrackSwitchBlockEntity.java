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
import com.example.miningtools.util.TrainChainingUtil;

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
    public static final int MIN_DELAY_SECONDS = 0;
    public static final int MAX_DELAY_SECONDS = 300;
    public static final int DEFAULT_DELAY_SECONDS = 0;

    public enum DirectionFilter {
        ANY(0, "ANY", "gui.miningtools.track_switch.dir.any", "All"),
        NORTH(1, "NORTH", "gui.miningtools.track_switch.dir.north", "N ^"),
        SOUTH(2, "SOUTH", "gui.miningtools.track_switch.dir.south", "S v"),
        EAST(3, "EAST", "gui.miningtools.track_switch.dir.east", "E >"),
        WEST(4, "WEST", "gui.miningtools.track_switch.dir.west", "W <");

        private final int index;
        private final String serializedName;
        private final String translationKey;
        private final String shortLabel;

        DirectionFilter(int index, String serializedName, String translationKey, String shortLabel) {
            this.index = index;
            this.serializedName = serializedName;
            this.translationKey = translationKey;
            this.shortLabel = shortLabel;
        }

        public int getIndex() {
            return this.index;
        }

        public String getSerializedName() {
            return this.serializedName;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }

        public String getShortLabel() {
            return this.shortLabel;
        }

        public static DirectionFilter fromIndex(int idx) {
            for (DirectionFilter f : values()) {
                if (f.index == idx) {
                    return f;
                }
            }
            return ANY;
        }

        public DirectionFilter next() {
            DirectionFilter[] vals = values();
            return vals[(this.ordinal() + 1) % vals.length];
        }

        public DirectionFilter prev() {
            DirectionFilter[] vals = values();
            return vals[(this.ordinal() - 1 + vals.length) % vals.length];
        }

        public boolean matches(@Nullable Direction dir) {
            if (this == ANY) {
                return true;
            }
            if (dir == null) {
                return false;
            }
            return switch (this) {
                case NORTH -> dir == Direction.NORTH;
                case SOUTH -> dir == Direction.SOUTH;
                case EAST -> dir == Direction.EAST;
                case WEST -> dir == Direction.WEST;
                default -> true;
            };
        }
    }

    public enum CargoCondition {
        ANY(0, "ANY", "gui.miningtools.track_switch.cargo.any", "Cargo: Any"),
        LOADED(1, "LOADED", "gui.miningtools.track_switch.cargo.loaded", "Cargo: Loaded"),
        EMPTY(2, "EMPTY", "gui.miningtools.track_switch.cargo.empty", "Cargo: Empty");

        private final int index;
        private final String serializedName;
        private final String translationKey;
        private final String shortLabel;

        CargoCondition(int index, String serializedName, String translationKey, String shortLabel) {
            this.index = index;
            this.serializedName = serializedName;
            this.translationKey = translationKey;
            this.shortLabel = shortLabel;
        }

        public int getIndex() {
            return this.index;
        }

        public String getSerializedName() {
            return this.serializedName;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }

        public String getShortLabel() {
            return this.shortLabel;
        }

        public static CargoCondition fromIndex(int idx) {
            for (CargoCondition c : values()) {
                if (c.index == idx) {
                    return c;
                }
            }
            return ANY;
        }

        public CargoCondition next() {
            CargoCondition[] vals = values();
            return vals[(this.ordinal() + 1) % vals.length];
        }

        public CargoCondition prev() {
            CargoCondition[] vals = values();
            return vals[(this.ordinal() - 1 + vals.length) % vals.length];
        }

        public boolean matches(boolean isLoaded, boolean isEmpty) {
            return switch (this) {
                case ANY -> true;
                case LOADED -> isLoaded;
                case EMPTY -> isEmpty;
            };
        }
    }

    public enum FuelCondition {
        ANY(0, "ANY", "gui.miningtools.track_switch.fuel.any", "Fuel: Any"),
        LESS_THAN_25(1, "LT_25", "gui.miningtools.track_switch.fuel.lt25", "Fuel < 25%"),
        LESS_THAN_50(2, "LT_50", "gui.miningtools.track_switch.fuel.lt50", "Fuel < 50%"),
        GREATER_THAN_25(3, "GT_25", "gui.miningtools.track_switch.fuel.gt25", "Fuel > 25%"),
        GREATER_THAN_50(4, "GT_50", "gui.miningtools.track_switch.fuel.gt50", "Fuel > 50%");

        private final int index;
        private final String serializedName;
        private final String translationKey;
        private final String shortLabel;

        FuelCondition(int index, String serializedName, String translationKey, String shortLabel) {
            this.index = index;
            this.serializedName = serializedName;
            this.translationKey = translationKey;
            this.shortLabel = shortLabel;
        }

        public int getIndex() {
            return this.index;
        }

        public String getSerializedName() {
            return this.serializedName;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }

        public String getShortLabel() {
            return this.shortLabel;
        }

        public static FuelCondition fromIndex(int idx) {
            for (FuelCondition f : values()) {
                if (f.index == idx) {
                    return f;
                }
            }
            return ANY;
        }

        public FuelCondition next() {
            FuelCondition[] vals = values();
            return vals[(this.ordinal() + 1) % vals.length];
        }

        public FuelCondition prev() {
            FuelCondition[] vals = values();
            return vals[(this.ordinal() - 1 + vals.length) % vals.length];
        }

        public boolean matches(int fuelPercent) {
            return switch (this) {
                case ANY -> true;
                case LESS_THAN_25 -> fuelPercent < 25;
                case LESS_THAN_50 -> fuelPercent < 50;
                case GREATER_THAN_25 -> fuelPercent >= 25;
                case GREATER_THAN_50 -> fuelPercent >= 50;
            };
        }
    }

    public record SwitchRule(
            String trainName,
            DirectionFilter direction,
            CargoCondition cargo,
            FuelCondition fuel,
            boolean openState,
            int delaySeconds
    ) {
        public SwitchRule(String trainName, boolean openState) {
            this(trainName, DirectionFilter.ANY, CargoCondition.ANY, FuelCondition.ANY, openState, 0);
        }

        public SwitchRule(String trainName, DirectionFilter direction, CargoCondition cargo, FuelCondition fuel, boolean openState) {
            this(trainName, direction, cargo, fuel, openState, 0);
        }

        public static final StreamCodec<ByteBuf, SwitchRule> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(32),
                SwitchRule::trainName,
                ByteBufCodecs.VAR_INT.map(DirectionFilter::fromIndex, DirectionFilter::getIndex),
                SwitchRule::direction,
                ByteBufCodecs.VAR_INT.map(CargoCondition::fromIndex, CargoCondition::getIndex),
                SwitchRule::cargo,
                ByteBufCodecs.VAR_INT.map(FuelCondition::fromIndex, FuelCondition::getIndex),
                SwitchRule::fuel,
                ByteBufCodecs.BOOL,
                SwitchRule::openState,
                ByteBufCodecs.VAR_INT,
                SwitchRule::delaySeconds,
                SwitchRule::new
        );
    }

    private final List<SwitchRule> rules = new ArrayList<>();
    private int detectRange = DEFAULT_DETECT_RANGE;
    private int lockTicks = 0;
    private boolean forceCheckStationary = false;

    public TrackSwitchBlockEntity(BlockPos pos, BlockState blockState) {
        super(MiningTools.TRACK_SWITCH_BLOCK_ENTITY.get(), pos, blockState);
    }

    public List<SwitchRule> getRules() {
        return this.rules;
    }

    public int getLockTicks() {
        return this.lockTicks;
    }

    public boolean isLocked() {
        return this.lockTicks > 0;
    }

    public void clearLock() {
        if (this.lockTicks > 0) {
            this.lockTicks = 0;
            this.setChanged();
        }
    }

    public void setLockTicks(int ticks) {
        this.lockTicks = Math.max(0, ticks);
        this.setChanged();
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
                DirectionFilter dir = r.direction() == null ? DirectionFilter.ANY : r.direction();
                CargoCondition cargo = r.cargo() == null ? CargoCondition.ANY : r.cargo();
                FuelCondition fuel = r.fuel() == null ? FuelCondition.ANY : r.fuel();
                int delay = Mth.clamp(r.delaySeconds(), MIN_DELAY_SECONDS, MAX_DELAY_SECONDS);
                this.rules.add(new SwitchRule(cleaned, dir, cargo, fuel, r.openState(), delay));
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
        if (level.isClientSide()) {
            return;
        }
        if (blockEntity.lockTicks > 0) {
            blockEntity.lockTicks--;
            if (blockEntity.lockTicks == 0 || blockEntity.lockTicks % 20 == 0) {
                blockEntity.setChanged();
            }
            return;
        }
        if (blockEntity.rules.isEmpty()) {
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

            Direction travelDir = TrainChainingUtil.getConsistTravelDirection(cart);
            if (travelDir == null) {
                Vec3 vel = cart.getDeltaMovement();
                if (vel.horizontalDistanceSqr() > 1.0E-4) {
                    travelDir = Math.abs(vel.x) > Math.abs(vel.z)
                            ? (vel.x > 0 ? Direction.EAST : Direction.WEST)
                            : (vel.z > 0 ? Direction.SOUTH : Direction.NORTH);
                }
            }
            TrainChainingUtil.ConsistCargoStatus cargoStatus = TrainChainingUtil.getConsistCargoStatus(cart);
            int fuelPercent = TrainChainingUtil.getConsistFuelPercent(cart);

            String trainName = BasicLocomotiveEntity.resolveTrainName(cart);
            SwitchRule matched = blockEntity.findMatchingRule(trainName, travelDir, cargoStatus, fuelPercent);
            if (matched != null) {
                boolean currentOpen = state.getValue(TrackSwitchBlock.OPEN);
                if (currentOpen != matched.openState()) {
                    TrackSwitchBlock.setSwitchState(level, pos, state, matched.openState(), true);
                }
                if (matched.delaySeconds() > 0) {
                    blockEntity.lockTicks = matched.delaySeconds() * 20;
                    blockEntity.setChanged();
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
        return this.findMatchingRule(trainName, null, new TrainChainingUtil.ConsistCargoStatus(false, true), 100);
    }

    public @Nullable SwitchRule findMatchingRule(String trainName, @Nullable Direction travelDir, TrainChainingUtil.ConsistCargoStatus cargoStatus, int fuelPercent) {
        String cleanTrain = trainName == null ? "" : trainName.trim();
        String lowerTrain = cleanTrain.toLowerCase(Locale.ROOT);

        SwitchRule bestRule = null;
        int bestScore = -1;

        for (SwitchRule rule : this.rules) {
            if (!rule.direction().matches(travelDir)) {
                continue;
            }
            if (!rule.cargo().matches(cargoStatus.hasCargo(), cargoStatus.isEmpty())) {
                continue;
            }
            if (!rule.fuel().matches(fuelPercent)) {
                continue;
            }

            int nameMatchScore = testTrainNameMatch(rule.trainName(), cleanTrain, lowerTrain);
            if (nameMatchScore < 0) {
                continue;
            }

            int specificityScore = nameMatchScore
                    + (rule.direction() != DirectionFilter.ANY ? 30 : 0)
                    + (rule.cargo() != CargoCondition.ANY ? 20 : 0)
                    + (rule.fuel() != FuelCondition.ANY ? 20 : 0);

            if (specificityScore > bestScore) {
                bestScore = specificityScore;
                bestRule = rule;
            }
        }

        return bestRule;
    }

    private static int testTrainNameMatch(String rulePattern, String cleanTrain, String lowerTrain) {
        String rName = rulePattern == null ? "" : rulePattern.trim();
        if (rName.isEmpty()) {
            return -1;
        }

        // Exact match
        if (!cleanTrain.isEmpty() && rName.equalsIgnoreCase(cleanTrain)) {
            return 100;
        }

        String lowerPattern = rName.toLowerCase(Locale.ROOT);
        // Prefix wildcard match e.g. "Ore*"
        if (rName.endsWith("*") && rName.length() > 1 && !lowerTrain.isEmpty()) {
            String prefix = lowerPattern.substring(0, lowerPattern.length() - 1).trim();
            if (lowerTrain.startsWith(prefix)) {
                return 60;
            }
        }

        // Substring match
        if (!lowerTrain.isEmpty() && lowerTrain.contains(lowerPattern)) {
            return 40;
        }

        // Catch-all wildcard "*" or "any"
        if (rName.equals("*") || rName.equalsIgnoreCase("any")) {
            return 10;
        }

        return -1;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("DetectRange", this.detectRange);
        output.putInt("LockTicks", this.lockTicks);
        output.putInt("RuleCount", this.rules.size());
        for (int i = 0; i < this.rules.size(); i++) {
            SwitchRule rule = this.rules.get(i);
            output.putString("RuleName_" + i, rule.trainName());
            output.putInt("RuleDir_" + i, rule.direction().getIndex());
            output.putInt("RuleCargo_" + i, rule.cargo().getIndex());
            output.putInt("RuleFuel_" + i, rule.fuel().getIndex());
            output.putBoolean("RuleOpen_" + i, rule.openState());
            output.putInt("RuleDelay_" + i, rule.delaySeconds());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.detectRange = Mth.clamp(input.getIntOr("DetectRange", DEFAULT_DETECT_RANGE), MIN_DETECT_RANGE, MAX_DETECT_RANGE);
        this.lockTicks = Math.max(0, input.getIntOr("LockTicks", 0));
        int count = Mth.clamp(input.getIntOr("RuleCount", 0), 0, MAX_RULES);
        this.rules.clear();
        for (int i = 0; i < count; i++) {
            String name = input.getStringOr("RuleName_" + i, "").trim();
            DirectionFilter dir = DirectionFilter.fromIndex(input.getIntOr("RuleDir_" + i, DirectionFilter.ANY.getIndex()));
            CargoCondition cargo = CargoCondition.fromIndex(input.getIntOr("RuleCargo_" + i, CargoCondition.ANY.getIndex()));
            FuelCondition fuel = FuelCondition.fromIndex(input.getIntOr("RuleFuel_" + i, FuelCondition.ANY.getIndex()));
            boolean open = input.getBooleanOr("RuleOpen_" + i, true);
            int delay = Mth.clamp(input.getIntOr("RuleDelay_" + i, 0), MIN_DELAY_SECONDS, MAX_DELAY_SECONDS);
            if (!name.isEmpty()) {
                this.rules.add(new SwitchRule(name, dir, cargo, fuel, open, delay));
            }
        }
    }
}
