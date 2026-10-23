package com.example.miningtools.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class AbstractEntityTransferBlockEntity extends BlockEntity {
    public static final int AGE_BOTH = 0;
    public static final int AGE_ADULT_ONLY = 1;
    public static final int AGE_BABY_ONLY = 2;

    public static final int MIN_RANGE = 1;
    public static final int MAX_RANGE = 16;
    public static final int DEFAULT_RANGE = 5;

    protected String whitelist = "";
    protected String blacklist = "";
    protected int ageFilter = AGE_BOTH;
    protected int range = DEFAULT_RANGE;
    protected int cooldown = 0;

    public AbstractEntityTransferBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public String getWhitelist() {
        return this.whitelist;
    }

    public void setWhitelist(String whitelist) {
        this.whitelist = whitelist != null ? whitelist.trim() : "";
        this.setChanged();
    }

    public String getBlacklist() {
        return this.blacklist;
    }

    public void setBlacklist(String blacklist) {
        this.blacklist = blacklist != null ? blacklist.trim() : "";
        this.setChanged();
    }

    public int getAgeFilter() {
        return this.ageFilter;
    }

    public void setAgeFilter(int ageFilter) {
        this.ageFilter = Math.floorMod(ageFilter, 3);
        this.setChanged();
    }

    public int getRange() {
        return this.range;
    }

    public void setRange(int range) {
        this.range = Math.clamp(range, MIN_RANGE, MAX_RANGE);
        this.setChanged();
    }

    public boolean matchesFilters(Entity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            return false;
        }

        // 1. Age filter
        if (!matchesAge(entity, this.ageFilter)) {
            return false;
        }

        // 2. Blacklist check - if matched, immediately reject
        if (matchesFilter(entity, this.blacklist)) {
            return false;
        }

        // 3. Whitelist check - if whitelist is specified, entity MUST match
        if (!this.whitelist.isEmpty()) {
            return matchesFilter(entity, this.whitelist);
        }

        // If whitelist is empty and not blacklisted, allowed
        return true;
    }

    public static boolean matchesAge(Entity entity, int ageFilter) {
        if (ageFilter == AGE_BOTH) {
            return true;
        }
        if (ageFilter == AGE_ADULT_ONLY) {
            if (entity instanceof AgeableMob ageable) {
                return !ageable.isBaby();
            }
            return true; // Non-ageable entities (e.g. Iron Golems) count as adults
        }
        if (ageFilter == AGE_BABY_ONLY) {
            if (entity instanceof AgeableMob ageable) {
                return ageable.isBaby();
            }
            return false; // Non-ageable entities cannot be babies
        }
        return true;
    }

    public static boolean matchesFilter(Entity entity, String listStr) {
        if (listStr == null || listStr.isBlank()) {
            return false;
        }
        String[] tokens = listStr.split("[,;\\n]+");
        String name = entity.getName().getString().trim().toLowerCase();
        String customName = entity.getCustomName() != null ? entity.getCustomName().getString().trim().toLowerCase() : "";
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String typeFull = typeId != null ? typeId.toString().toLowerCase() : "";
        String typePath = typeId != null ? typeId.getPath().toLowerCase() : "";

        for (String token : tokens) {
            String t = token.trim().toLowerCase();
            if (t.isEmpty()) continue;
            if (name.equalsIgnoreCase(t)
                    || customName.equalsIgnoreCase(t)
                    || typeFull.equalsIgnoreCase(t)
                    || typePath.equalsIgnoreCase(t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Whitelist", this.whitelist);
        output.putString("Blacklist", this.blacklist);
        output.putInt("AgeFilter", this.ageFilter);
        output.putInt("Range", this.range);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.whitelist = input.getString("Whitelist").orElse("");
        this.blacklist = input.getString("Blacklist").orElse("");
        this.ageFilter = input.getInt("AgeFilter").orElse(AGE_BOTH);
        this.range = Math.clamp(input.getInt("Range").orElse(DEFAULT_RANGE), MIN_RANGE, MAX_RANGE);
    }
}
