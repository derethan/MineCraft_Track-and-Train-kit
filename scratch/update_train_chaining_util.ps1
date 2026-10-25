$path = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\util\TrainChainingUtil.java"
$content = [System.IO.File]::ReadAllText($path)

# 1. Update pushCartFromLocomotiveBumper
$targetBumper = @"
        if (penetration > -0.05D) {
            double pushDist = Math.max(penetration + 0.05D, Math.max(locoSpeed, 0.2D));
            moveCartAlongRails(otherCart, bumperDirX, bumperDirZ, pushDist);

            double pushSpeed = Math.max(locoSpeed, 0.35D);
            otherCart.setDeltaMovement(
                    bumperDirX * pushSpeed,
                    otherCart.getDeltaMovement().y,
                    bumperDirZ * pushSpeed
            );
            otherCart.needsSync = true;
        }
"@

$replaceBumper = @"
        if (penetration > -0.05D) {
            double pushDist = Math.max(penetration + 0.05D, Math.max(locoSpeed, 0.05D));
            moveCartAlongRails(otherCart, bumperDirX, bumperDirZ, pushDist);

            double pushSpeed = Math.max(locoSpeed, 0.05D);
            otherCart.setDeltaMovement(
                    bumperDirX * pushSpeed,
                    otherCart.getDeltaMovement().y,
                    bumperDirZ * pushSpeed
            );
            otherCart.needsSync = true;
        }
"@

if ($content.Contains($targetBumper)) {
    $content = $content.Replace($targetBumper, $replaceBumper)
    Write-Host "Updated pushCartFromLocomotiveBumper."
} else {
    Write-Warning "targetBumper not matched directly, trying normalized line endings..."
    $content = $content -replace "Math\.max\(penetration \+ 0\.05D, Math\.max\(locoSpeed, 0\.2D\)\);", "Math.max(penetration + 0.05D, Math.max(locoSpeed, 0.05D));"
    $content = $content -replace "double pushSpeed = Math\.max\(locoSpeed, 0\.35D\);", "double pushSpeed = Math.max(locoSpeed, 0.05D);"
}

# 2. Add applyRollingStockSlowdown and update tickConsistMovement and pushMinecarts
$targetConsist = @"
    private static final Map<UUID, Long> CONSIST_TICK_MAP = new HashMap<>();

    public static void tickConsistMovement(AbstractMinecart cart) {
        if (cart.level().isClientSide() || cart.isRemoved()) return;

        if (isPartOfRunningTrain(cart)) {
            return;
        }

        UUID parentUUID = getCartParentUUID(cart);
        UUID childUUID = getCartChildUUID(cart);
        if (parentUUID == null && childUUID == null) {
            return;
        }

        AbstractMinecart head = getTrainHead(cart);
        if (head == null || head.isRemoved()) head = cart;

        long gameTime = cart.level().getGameTime();
        Long lastTick = CONSIST_TICK_MAP.get(head.getUUID());
        if (lastTick != null && lastTick == gameTime) {
            return;
        }
        CONSIST_TICK_MAP.put(head.getUUID(), gameTime);

        if (gameTime % 200 == 0 && CONSIST_TICK_MAP.size() > 50) {
            CONSIST_TICK_MAP.clear();
            CONSIST_TICK_MAP.put(head.getUUID(), gameTime);
        }

        Direction dir = cart.getMotionDirection();
        handleChainedCartsMovement(head, dir);
    }

    public static void pushMinecarts(AbstractMinecart a, AbstractMinecart b) {
        if (a.level().isClientSide() || isConnectedInSameTrain(a, b)) {
            return;
        }

        if (a instanceof BasicLocomotiveEntity || a instanceof DieselLocomotiveEntity || a instanceof ElectricLocomotiveEntity) {
            pushCartFromLocomotiveBumper(a, b);
            return;
        }
        if (b instanceof BasicLocomotiveEntity || b instanceof DieselLocomotiveEntity || b instanceof ElectricLocomotiveEntity) {
            pushCartFromLocomotiveBumper(b, a);
            return;
        }

        double dx = b.getX() - a.getX();
        double dz = b.getZ() - a.getZ();
        double distSq = dx * dx + dz * dz;
        if (distSq < 1.0E-4D) {
            dx = 0.01;
            dz = 0.0;
            distSq = dx * dx;
        }
        double dist = Math.sqrt(distSq);
        double nx = dx / dist;
        double nz = dz / dist;

        double rA = getCartCouplerRadius(a);
        double rB = getCartCouplerRadius(b);
        double minDistance = Math.max(rA + rB - 0.05D, 0.85D);

        if (dist < minDistance) {
            double overlap = minDistance - dist;
            double pushX = nx * (overlap * 0.5D + 0.02D);
            double pushZ = nz * (overlap * 0.5D + 0.02D);

            b.push(pushX, 0.0, pushZ);
            a.push(-pushX, 0.0, -pushZ);

            Vec3 vA = a.getDeltaMovement();
            Vec3 vB = b.getDeltaMovement();
            double dotA = vA.x * nx + vA.z * nz;
            double dotB = vB.x * nx + vB.z * nz;

            if (dotA > dotB) {
                double impulse = (dotA - dotB) * 0.5D + 0.05D;
                b.setDeltaMovement(b.getDeltaMovement().add(nx * impulse, 0.0, nz * impulse));
                b.needsSync = true;
                a.setDeltaMovement(a.getDeltaMovement().subtract(nx * impulse * 0.5D, 0.0, nz * impulse * 0.5D));
                a.needsSync = true;
            }
        }
    }
"@

$replaceConsist = @"
    private static final Map<UUID, Long> CONSIST_TICK_MAP = new HashMap<>();

    public static Vec3 applyRollingStockSlowdown(AbstractMinecart cart, Vec3 deltaMovement) {
        if (isPartOfRunningTrain(cart)) {
            return deltaMovement;
        }

        double drag = Config.ROLLING_STOCK_DRAG_COEFFICIENT.get();
        if (cart.isInWater()) {
            drag *= 0.85D;
        }

        Vec3 slowed = deltaMovement.multiply(drag, 0.0D, drag);
        if (slowed.horizontalDistanceSqr() < 1.0E-4D) {
            return Vec3.ZERO;
        }
        return slowed;
    }

    public static void tickConsistMovement(AbstractMinecart cart) {
        if (cart.level().isClientSide() || cart.isRemoved()) return;

        if (isPartOfRunningTrain(cart)) {
            return;
        }

        UUID parentUUID = getCartParentUUID(cart);
        UUID childUUID = getCartChildUUID(cart);
        if (parentUUID == null && childUUID == null) {
            return;
        }

        AbstractMinecart head = getTrainHead(cart);
        if (head == null || head.isRemoved()) head = cart;

        long gameTime = cart.level().getGameTime();
        Long lastTick = CONSIST_TICK_MAP.get(head.getUUID());
        if (lastTick != null && lastTick == gameTime) {
            return;
        }
        CONSIST_TICK_MAP.put(head.getUUID(), gameTime);

        if (gameTime % 200 == 0 && CONSIST_TICK_MAP.size() > 50) {
            CONSIST_TICK_MAP.clear();
            CONSIST_TICK_MAP.put(head.getUUID(), gameTime);
        }

        if (cart != head && cart.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D) {
            if (head.getDeltaMovement().horizontalDistanceSqr() < cart.getDeltaMovement().horizontalDistanceSqr()) {
                head.setDeltaMovement(cart.getDeltaMovement());
                head.needsSync = true;
            }
        }

        Direction dir = cart.getMotionDirection();
        handleChainedCartsMovement(head, dir);
    }

    public static void pushMinecarts(AbstractMinecart a, AbstractMinecart b) {
        if (a.level().isClientSide() || isConnectedInSameTrain(a, b)) {
            return;
        }

        if (a instanceof BasicLocomotiveEntity || a instanceof DieselLocomotiveEntity || a instanceof ElectricLocomotiveEntity) {
            pushCartFromLocomotiveBumper(a, b);
            return;
        }
        if (b instanceof BasicLocomotiveEntity || b instanceof DieselLocomotiveEntity || b instanceof ElectricLocomotiveEntity) {
            pushCartFromLocomotiveBumper(b, a);
            return;
        }

        double dx = b.getX() - a.getX();
        double dz = b.getZ() - a.getZ();
        double distSq = dx * dx + dz * dz;
        if (distSq < 1.0E-4D) {
            dx = 0.01;
            dz = 0.0;
            distSq = dx * dx;
        }
        double dist = Math.sqrt(distSq);
        double nx = dx / dist;
        double nz = dz / dist;

        double rA = getCartCouplerRadius(a);
        double rB = getCartCouplerRadius(b);
        double minDistance = Math.max(rA + rB - 0.05D, 0.85D);

        if (dist < minDistance) {
            double overlap = minDistance - dist;

            // Physical bumper separation along the track without adding artificial velocity via push()
            double sep = overlap * 0.4D;
            a.setPos(a.getX() - nx * sep, a.getY(), a.getZ() - nz * sep);
            b.setPos(b.getX() + nx * sep, b.getY(), b.getZ() + nz * sep);

            Vec3 vA = a.getDeltaMovement();
            Vec3 vB = b.getDeltaMovement();
            double dotA = vA.x * nx + vA.z * nz;
            double dotB = vB.x * nx + vB.z * nz;
            double relSpeed = dotA - dotB; // Closing speed along collision normal

            if (relSpeed > 0.005D) {
                double transferRatio = Config.ROLLING_STOCK_BUMP_TRANSFER.get();
                double damping = Config.ROLLING_STOCK_BUMP_DAMPING.get();

                double impulse = Math.min(relSpeed * transferRatio, 0.12D);

                Vec3 newVA = vA.subtract(nx * impulse, 0.0, nz * impulse).scale(damping);
                Vec3 newVB = vB.add(nx * impulse, 0.0, nz * impulse).scale(damping);

                a.setDeltaMovement(newVA);
                b.setDeltaMovement(newVB);
                a.needsSync = true;
                b.needsSync = true;
            }
        }
    }
"@

if ($content.Contains($targetConsist)) {
    $content = $content.Replace($targetConsist, $replaceConsist)
    Write-Host "Updated tickConsistMovement and pushMinecarts."
} else {
    Write-Error "targetConsist not found in TrainChainingUtil.java"
}

[System.IO.File]::WriteAllText($path, $content, [System.Text.UTF8Encoding]::new($false))
Write-Host "TrainChainingUtil.java saved."
