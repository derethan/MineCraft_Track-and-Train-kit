$ErrorActionPreference = "Stop"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

# 1. Update TrainChainingUtil.java
$utilPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\util\TrainChainingUtil.java"
$utilContent = [System.IO.File]::ReadAllText($utilPath, $utf8NoBom)

if (-not $utilContent.Contains("import com.example.miningtools.MiningTools;")) {
    $utilContent = $utilContent.Replace("import com.example.miningtools.Config;", "import com.example.miningtools.Config;`r`nimport com.example.miningtools.MiningTools;")
}
if (-not $utilContent.Contains("import net.minecraft.world.level.block.PoweredRailBlock;")) {
    $utilContent = $utilContent.Replace("import net.minecraft.world.level.block.BaseRailBlock;", "import net.minecraft.world.level.block.BaseRailBlock;`r`nimport net.minecraft.world.level.block.PoweredRailBlock;")
}

$brakingMethods = @'
    public static boolean isCartOnUnpoweredBrakingRail(Entity entity) {
        if (entity == null || entity.level() == null) return false;
        BlockPos railPos = getCurrentBlockPosOrRailBelow(entity);
        BlockState state = entity.level().getBlockState(railPos);
        if (state.is(MiningTools.BRAKING_RAIL.get())) {
            return !state.getValue(PoweredRailBlock.POWERED);
        }
        if (state.is(MiningTools.CONTROL_RAIL.get())
                && (entity instanceof BasicLocomotiveEntity || entity instanceof DieselLocomotiveEntity || entity instanceof ElectricLocomotiveEntity)) {
            return !state.getValue(PoweredRailBlock.POWERED);
        }
        return false;
    }

    public static boolean isConsistHeldByBrakingRail(AbstractMinecart startCart) {
        if (startCart == null || startCart.level() == null) return false;
        if (isCartOnUnpoweredBrakingRail(startCart)) return true;

        Level level = startCart.level();
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> queue = new ArrayDeque<>();
        visited.add(startCart.getUUID());
        UUID p = getCartParentUUID(startCart);
        if (p != null) queue.add(p);
        UUID c = getCartChildUUID(startCart);
        if (c != null) queue.add(c);

        while (!queue.isEmpty() && visited.size() < 100) {
            UUID next = queue.poll();
            if (next != null && visited.add(next)) {
                Entity ent = level.getEntity(next);
                if (ent instanceof AbstractMinecart cart && !cart.isRemoved()) {
                    if (isCartOnUnpoweredBrakingRail(cart)) {
                        return true;
                    }
                    UUID cp = getCartParentUUID(cart);
                    if (cp != null && !visited.contains(cp)) queue.add(cp);
                    UUID cc = getCartChildUUID(cart);
                    if (cc != null && !visited.contains(cc)) queue.add(cc);
                }
            }
        }
        return false;
    }

    public static void freezeConsistFromBrakingRail(AbstractMinecart startCart) {
        if (startCart == null || startCart.level() == null) return;
        Level level = startCart.level();
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> queue = new ArrayDeque<>();
        queue.add(startCart.getUUID());

        while (!queue.isEmpty() && visited.size() < 100) {
            UUID next = queue.poll();
            if (next != null && visited.add(next)) {
                Entity ent = level.getEntity(next);
                if (ent instanceof AbstractMinecart cart && !cart.isRemoved()) {
                    cart.setDeltaMovement(Vec3.ZERO);
                    if (cart instanceof BasicLocomotiveEntity bl) {
                        bl.stopLocomotiveFromRail();
                    } else if (cart instanceof DieselLocomotiveEntity dl) {
                        dl.stopLocomotiveFromRail();
                    } else if (cart instanceof ElectricLocomotiveEntity el) {
                        el.stopLocomotiveFromRail();
                    } else if (cart instanceof TunnelBoreEntity bore && bore.isRunning()) {
                        bore.setRunning(false);
                    }
                    UUID cp = getCartParentUUID(cart);
                    if (cp != null && !visited.contains(cp)) queue.add(cp);
                    UUID cc = getCartChildUUID(cart);
                    if (cc != null && !visited.contains(cc)) queue.add(cc);
                }
            }
        }
    }

    public static void releaseConsistFromBrakingRail(AbstractMinecart startCart) {
        if (startCart == null || startCart.level() == null || startCart.level().isClientSide()) return;
        if (isConsistHeldByBrakingRail(startCart)) {
            return;
        }
        Level level = startCart.level();
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> queue = new ArrayDeque<>();
        queue.add(startCart.getUUID());

        boolean startedLoco = false;
        List<AbstractMinecart> allCarts = new ArrayList<>();

        while (!queue.isEmpty() && visited.size() < 100) {
            UUID next = queue.poll();
            if (next != null && visited.add(next)) {
                Entity ent = level.getEntity(next);
                if (ent instanceof AbstractMinecart cart && !cart.isRemoved()) {
                    allCarts.add(cart);
                    if (cart instanceof BasicLocomotiveEntity bl) {
                        if (!bl.isRunning()) {
                            bl.startLocomotiveFromRail();
                        }
                        if (bl.isRunning()) startedLoco = true;
                    } else if (cart instanceof DieselLocomotiveEntity dl) {
                        if (!dl.isRunning()) {
                            dl.startLocomotiveFromRail();
                        }
                        if (dl.isRunning()) startedLoco = true;
                    } else if (cart instanceof ElectricLocomotiveEntity el) {
                        if (!el.isRunning()) {
                            el.startLocomotiveFromRail();
                        }
                        if (el.isRunning()) startedLoco = true;
                    }
                    UUID cp = getCartParentUUID(cart);
                    if (cp != null && !visited.contains(cp)) queue.add(cp);
                    UUID cc = getCartChildUUID(cart);
                    if (cc != null && !visited.contains(cc)) queue.add(cc);
                }
            }
        }

        if (!startedLoco) {
            // Standalone cart or unpowered consist: give a gentle kick along its current facing if stationary
            for (AbstractMinecart cart : allCarts) {
                if (cart.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D) {
                    Vec3 heading = getCartHeadingVector(cart, 0.0D, 0.0D);
                    cart.setDeltaMovement(heading.x * 0.08D, cart.getDeltaMovement().y, heading.z * 0.08D);
                    cart.needsSync = true;
                }
            }
        }
    }

    public static String getCartIndividualName(Entity cart) {
        if (cart == null) return "";
        if (cart instanceof BasicLocomotiveEntity bl) return bl.getTrainName();
        if (cart instanceof DieselLocomotiveEntity dl) return dl.getTrainName();
        if (cart instanceof ElectricLocomotiveEntity el) return el.getTrainName();
        var custom = cart.getCustomName();
        return custom != null ? custom.getString().trim() : "";
    }

    public static boolean matchesCartNameFilter(Entity cart, String whitelistCsv, String blacklistCsv) {
        if (cart == null) return false;
        String cartName = getCartIndividualName(cart).toLowerCase(java.util.Locale.ROOT);
        String trainName = BasicLocomotiveEntity.resolveTrainName(cart).toLowerCase(java.util.Locale.ROOT);

        if (blacklistCsv != null && !blacklistCsv.isBlank()) {
            for (String token : blacklistCsv.split(",")) {
                String pat = token.trim().toLowerCase(java.util.Locale.ROOT);
                if (!pat.isEmpty()) {
                    if ((!cartName.isEmpty() && cartName.contains(pat))
                            || (!trainName.isEmpty() && trainName.contains(pat))) {
                        return false;
                    }
                }
            }
        }

        if (whitelistCsv != null && !whitelistCsv.isBlank()) {
            boolean anyPattern = false;
            for (String token : whitelistCsv.split(",")) {
                String pat = token.trim().toLowerCase(java.util.Locale.ROOT);
                if (!pat.isEmpty()) {
                    anyPattern = true;
                    if ((!cartName.isEmpty() && cartName.contains(pat))
                            || (!trainName.isEmpty() && trainName.contains(pat))) {
                        return true;
                    }
                }
            }
            if (anyPattern) {
                return false;
            }
        }

        return true;
    }
'@

$oldSlowdown = @'
    public static Vec3 applyRollingStockSlowdown(AbstractMinecart cart, Vec3 deltaMovement) {
        if (isPartOfRunningTrain(cart)) {
            return deltaMovement;
        }
'@

$newSlowdown = $brakingMethods + "`r`n`r`n" + @'
    public static Vec3 applyRollingStockSlowdown(AbstractMinecart cart, Vec3 deltaMovement) {
        if (isCartOnUnpoweredBrakingRail(cart) || isConsistHeldByBrakingRail(cart)) {
            cart.setDeltaMovement(Vec3.ZERO);
            return Vec3.ZERO;
        }
        if (isPartOfRunningTrain(cart)) {
            return deltaMovement;
        }
'@

if (-not $utilContent.Contains("isCartOnUnpoweredBrakingRail")) {
    $utilContent = $utilContent.Replace($oldSlowdown, $newSlowdown)
}

$oldTickConsist = @'
    public static void tickConsistMovement(AbstractMinecart cart) {
        if (cart.level().isClientSide() || cart.isRemoved()) return;

        if (isPartOfRunningTrain(cart)) {
            return;
        }
'@

$newTickConsist = @'
    public static void tickConsistMovement(AbstractMinecart cart) {
        if (cart.level().isClientSide() || cart.isRemoved()) return;

        if (isConsistHeldByBrakingRail(cart)) {
            freezeConsistFromBrakingRail(cart);
            return;
        }

        if (isPartOfRunningTrain(cart)) {
            return;
        }
'@

$utilContent = $utilContent.Replace($oldTickConsist, $newTickConsist)
[System.IO.File]::WriteAllText($utilPath, $utilContent, $utf8NoBom)
Write-Host "Updated TrainChainingUtil.java"

# 2. Update BasicLocomotiveEntity.java
$basicPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\BasicLocomotiveEntity.java"
$basicContent = [System.IO.File]::ReadAllText($basicPath, $utf8NoBom)

$oldResolve = @'
        if (entity instanceof ElectricLocomotiveEntity eloco) {
            return eloco.getTrainName();
        }
        BasicLocomotiveEntity connectedLoco = getConnectedLocomotive(entity);
        if (connectedLoco != null) {
            String locoName = connectedLoco.getTrainName();
            if (!locoName.isBlank()) {
                return locoName;
            }
        }
        ElectricLocomotiveEntity connectedElec = ElectricLocomotiveEntity.getConnectedLocomotive(entity);
        if (connectedElec != null) {
            String locoName = connectedElec.getTrainName();
            if (!locoName.isBlank()) {
                return locoName;
            }
        }
'@

$newResolve = @'
        if (entity instanceof DieselLocomotiveEntity dloco) {
            return dloco.getTrainName();
        }
        if (entity instanceof ElectricLocomotiveEntity eloco) {
            return eloco.getTrainName();
        }
        BasicLocomotiveEntity connectedLoco = getConnectedLocomotive(entity);
        if (connectedLoco != null) {
            String locoName = connectedLoco.getTrainName();
            if (!locoName.isBlank()) {
                return locoName;
            }
        }
        DieselLocomotiveEntity connectedDiesel = DieselLocomotiveEntity.getConnectedLocomotive(entity);
        if (connectedDiesel != null) {
            String locoName = connectedDiesel.getTrainName();
            if (!locoName.isBlank()) {
                return locoName;
            }
        }
        ElectricLocomotiveEntity connectedElec = ElectricLocomotiveEntity.getConnectedLocomotive(entity);
        if (connectedElec != null) {
            String locoName = connectedElec.getTrainName();
            if (!locoName.isBlank()) {
                return locoName;
            }
        }
'@

$basicContent = $basicContent.Replace($oldResolve, $newResolve)

$oldBasicControl = @'
        if (railState.is(MiningTools.CONTROL_RAIL.get())) {
            boolean powered = railState.getValue(PoweredRailBlock.POWERED);
            if (powered) {
                if (!this.running) {
                    this.startLocomotiveFromRail();
                    hasFuel = this.fuel > 0;
                    this.setLit(hasFuel);
                }
            } else {
                if (this.running) {
                    this.stopLocomotiveFromRail();
                }
            }
        }
'@

$newBasicControl = @'
        if (railState.is(MiningTools.CONTROL_RAIL.get()) || railState.is(MiningTools.BRAKING_RAIL.get())) {
            boolean powered = railState.getValue(PoweredRailBlock.POWERED);
            if (powered) {
                if (!this.running && !TrainChainingUtil.isConsistHeldByBrakingRail(this)) {
                    this.startLocomotiveFromRail();
                    hasFuel = this.fuel > 0;
                    this.setLit(hasFuel);
                }
            } else {
                if (this.running) {
                    this.stopLocomotiveFromRail();
                }
            }
        } else if (this.running && TrainChainingUtil.isConsistHeldByBrakingRail(this)) {
            this.stopLocomotiveFromRail();
        }
'@

$basicContent = $basicContent.Replace($oldBasicControl, $newBasicControl)

$oldBasicPost = @'
            if (currentRailState.is(MiningTools.CONTROL_RAIL.get()) && !currentRailState.getValue(PoweredRailBlock.POWERED)) {
                this.stopLocomotiveFromRail();
            }
'@

$newBasicPost = @'
            if ((currentRailState.is(MiningTools.CONTROL_RAIL.get()) || currentRailState.is(MiningTools.BRAKING_RAIL.get()))
                    && !currentRailState.getValue(PoweredRailBlock.POWERED)) {
                this.stopLocomotiveFromRail();
            }
'@

$basicContent = $basicContent.Replace($oldBasicPost, $newBasicPost)
[System.IO.File]::WriteAllText($basicPath, $basicContent, $utf8NoBom)
Write-Host "Updated BasicLocomotiveEntity.java"

# 3. Update DieselLocomotiveEntity.java
$dieselPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\DieselLocomotiveEntity.java"
$dieselContent = [System.IO.File]::ReadAllText($dieselPath, $utf8NoBom)

$oldDieselPre = @'
        BlockPos railPos = this.getCurrentBlockPosOrRailBelow();
        BlockState railState = this.level().getBlockState(railPos);
        boolean onRails = BaseRailBlock.isRail(railState);

        if (this.running) {
'@

$newDieselPre = @'
        BlockPos railPos = this.getCurrentBlockPosOrRailBelow();
        BlockState railState = this.level().getBlockState(railPos);
        boolean onRails = BaseRailBlock.isRail(railState);

        if (railState.is(MiningTools.CONTROL_RAIL.get()) || railState.is(MiningTools.BRAKING_RAIL.get())) {
            boolean powered = railState.getValue(PoweredRailBlock.POWERED);
            if (powered) {
                if (!this.running && !TrainChainingUtil.isConsistHeldByBrakingRail(this)) {
                    this.startLocomotiveFromRail();
                }
            } else {
                if (this.running) {
                    this.stopLocomotiveFromRail();
                }
            }
        } else if (this.running && TrainChainingUtil.isConsistHeldByBrakingRail(this)) {
            this.stopLocomotiveFromRail();
        }

        if (this.running) {
'@

$dieselContent = $dieselContent.Replace($oldDieselPre, $newDieselPre)

$oldDieselPost = @'
            if (currentRailState.is(MiningTools.CONTROL_RAIL.get()) && !currentRailState.getValue(PoweredRailBlock.POWERED)) {
                this.stopLocomotiveFromRail();
            }
'@

$newDieselPost = @'
            if ((currentRailState.is(MiningTools.CONTROL_RAIL.get()) || currentRailState.is(MiningTools.BRAKING_RAIL.get()))
                    && !currentRailState.getValue(PoweredRailBlock.POWERED)) {
                this.stopLocomotiveFromRail();
            }
'@

$dieselContent = $dieselContent.Replace($oldDieselPost, $newDieselPost)
[System.IO.File]::WriteAllText($dieselPath, $dieselContent, $utf8NoBom)
Write-Host "Updated DieselLocomotiveEntity.java"

# 4. Update ElectricLocomotiveEntity.java
$elecPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\ElectricLocomotiveEntity.java"
$elecContent = [System.IO.File]::ReadAllText($elecPath, $utf8NoBom)

$oldElecPre = @'
        // Control rail interaction
        if (railState.is(MiningTools.CONTROL_RAIL.get())) {
            boolean powered = railState.getValue(PoweredRailBlock.POWERED);
            if (powered) {
                if (!this.running) {
                    this.startLocomotiveFromRail();
                }
            } else {
                if (this.running) {
                    this.stopLocomotiveFromRail();
                }
            }
        }
'@

$newElecPre = @'
        // Control & Braking rail interaction
        if (railState.is(MiningTools.CONTROL_RAIL.get()) || railState.is(MiningTools.BRAKING_RAIL.get())) {
            boolean powered = railState.getValue(PoweredRailBlock.POWERED);
            if (powered) {
                if (!this.running && !TrainChainingUtil.isConsistHeldByBrakingRail(this)) {
                    this.startLocomotiveFromRail();
                }
            } else {
                if (this.running) {
                    this.stopLocomotiveFromRail();
                }
            }
        } else if (this.running && TrainChainingUtil.isConsistHeldByBrakingRail(this)) {
            this.stopLocomotiveFromRail();
        }
'@

$elecContent = $elecContent.Replace($oldElecPre, $newElecPre)
$elecContent = $elecContent.Replace($oldDieselPost, $newDieselPost)
[System.IO.File]::WriteAllText($elecPath, $elecContent, $utf8NoBom)
Write-Host "Updated ElectricLocomotiveEntity.java"
