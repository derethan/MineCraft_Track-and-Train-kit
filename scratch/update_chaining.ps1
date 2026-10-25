$path = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\util\TrainChainingUtil.java"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$text = [System.IO.File]::ReadAllText($path, $utf8NoBom)

# 1. Replace determineRoles and linkCarts
$startMarker1 = "    public static @Nullable Pair<AbstractMinecart, AbstractMinecart> determineRoles(AbstractMinecart first, AbstractMinecart second) {"
$endMarker1 = "    public static boolean unlinkCart(AbstractMinecart cart, ServerLevel level) {"

$idx1 = $text.IndexOf($startMarker1)
$idx2 = $text.IndexOf($endMarker1)
if ($idx1 -lt 0 -or $idx2 -lt 0) {
    throw "Markers 1/2 not found: $idx1, $idx2"
}

$replacement1 = @'
    /**
     * Reverses the parent/child coupler polarity of a connected chain of carts so two train segments
     * with matching free couplers (e.g., two tails or two heads) can couple together seamlessly
     * without ever disconnecting existing neighbors.
     */
    public static void reverseChainOrientation(AbstractMinecart startCart, Level level) {
        if (startCart == null || level == null) return;

        List<AbstractMinecart> chain = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        Deque<AbstractMinecart> queue = new ArrayDeque<>();
        queue.add(startCart);
        visited.add(startCart.getUUID());

        while (!queue.isEmpty() && chain.size() < 100) {
            AbstractMinecart curr = queue.poll();
            chain.add(curr);
            UUID pId = getCartParentUUID(curr);
            if (pId != null && visited.add(pId)) {
                Entity pEnt = level.getEntity(pId);
                if (pEnt instanceof AbstractMinecart pCart && !pCart.isRemoved()) {
                    queue.add(pCart);
                }
            }
            UUID cId = getCartChildUUID(curr);
            if (cId != null && visited.add(cId)) {
                Entity cEnt = level.getEntity(cId);
                if (cEnt instanceof AbstractMinecart cCart && !cCart.isRemoved()) {
                    queue.add(cCart);
                }
            }
        }

        Map<UUID, UUID> oldParents = new HashMap<>();
        Map<UUID, UUID> oldChildren = new HashMap<>();
        for (AbstractMinecart c : chain) {
            oldParents.put(c.getUUID(), getCartParentUUID(c));
            oldChildren.put(c.getUUID(), getCartChildUUID(c));
        }

        for (AbstractMinecart c : chain) {
            UUID oldP = oldParents.get(c.getUUID());
            UUID oldC = oldChildren.get(c.getUUID());
            setCartParent(c, oldC, level);
            setCartChild(c, oldP, level);
        }
    }

    public static @Nullable Pair<AbstractMinecart, AbstractMinecart> determineRoles(AbstractMinecart first, AbstractMinecart second) {
        if (first == null || second == null || first.getUUID().equals(second.getUUID())) {
            return null;
        }
        if (isConnectedInSameTrain(first, second)) {
            return null;
        }

        UUID firstParent = getCartParentUUID(first);
        UUID firstChild = getCartChildUUID(first);
        UUID secondParent = getCartParentUUID(second);
        UUID secondChild = getCartChildUUID(second);

        boolean fFrontFree = (firstParent == null);
        boolean fRearFree = (firstChild == null);
        boolean sFrontFree = (secondParent == null);
        boolean sRearFree = (secondChild == null);

        // If a clicked cart has both front and rear couplers already occupied, walk to the nearest free end of its consist
        if (!fFrontFree && !fRearFree) {
            AbstractMinecart tail = getTrainTail(first);
            AbstractMinecart head = getTrainHead(first);
            if (tail != null && getCartChildUUID(tail) == null) {
                first = tail;
                fFrontFree = (getCartParentUUID(first) == null);
                fRearFree = true;
            } else if (head != null && getCartParentUUID(head) == null) {
                first = head;
                fFrontFree = true;
                fRearFree = (getCartChildUUID(first) == null);
            } else {
                return null;
            }
        }

        if (!sFrontFree && !sRearFree) {
            AbstractMinecart head = getTrainHead(second);
            AbstractMinecart tail = getTrainTail(second);
            if (head != null && getCartParentUUID(head) == null) {
                second = head;
                sFrontFree = true;
                sRearFree = (getCartChildUUID(second) == null);
            } else if (tail != null && getCartChildUUID(tail) == null) {
                second = tail;
                sFrontFree = (getCartParentUUID(second) == null);
                sRearFree = true;
            } else {
                return null;
            }
        }

        boolean firstIsLead = isLeadCart(first);
        boolean secondIsLead = isLeadCart(second);

        // If both carts are completely unlinked locomotives, orient by facing
        if (firstIsLead && secondIsLead && fFrontFree && fRearFree && sFrontFree && sRearFree) {
            Direction facing = (first instanceof BasicLocomotiveEntity bl) ? bl.getFacing()
                    : (first instanceof DieselLocomotiveEntity dl) ? dl.getFacing()
                    : (first instanceof ElectricLocomotiveEntity el) ? el.getFacing()
                    : Direction.NORTH;
            double dx = second.getX() - first.getX();
            double dz = second.getZ() - first.getZ();
            double dot = dx * facing.getStepX() + dz * facing.getStepZ();
            if (dot < 0.0) {
                return Pair.of(first, second);
            } else {
                return Pair.of(second, first);
            }
        }

        // If second is an unlinked locomotive and first is unlinked rolling stock, put locomotive in front
        if (secondIsLead && !firstIsLead && sRearFree && fFrontFree) {
            return Pair.of(second, first);
        }

        // Connect directly using available front (parent) and rear (child) connection points without ever overwriting occupied couplers
        if (fRearFree && sFrontFree) {
            return Pair.of(first, second);
        }
        if (sRearFree && fFrontFree) {
            return Pair.of(second, first);
        }

        // Both have only rear couplers free (two train tails): reverse second's chain so its front coupler is free
        if (fRearFree && sRearFree) {
            reverseChainOrientation(second, first.level());
            return Pair.of(first, second);
        }

        // Both have only front couplers free (two train heads): reverse first's chain so its rear coupler is free
        if (fFrontFree && sFrontFree) {
            reverseChainOrientation(first, first.level());
            return Pair.of(first, second);
        }

        return null;
    }

    public static boolean isLeadCart(AbstractMinecart cart) {
        return cart instanceof BasicLocomotiveEntity
                || cart instanceof DieselLocomotiveEntity
                || cart instanceof ElectricLocomotiveEntity
                || cart instanceof TunnelBoreEntity;
    }

    public static boolean isTailCart(AbstractMinecart cart) {
        return false;
    }

    public static boolean linkCarts(AbstractMinecart parent, AbstractMinecart child, ServerLevel level) {
        if (parent == null || child == null) return false;
        if (parent.getUUID().equals(child.getUUID())) return false;

        UUID currentParentChild = getCartChildUUID(parent);
        UUID currentChildParent = getCartParentUUID(child);
        if (child.getUUID().equals(currentParentChild) && parent.getUUID().equals(currentChildParent)) {
            return true;
        }

        if (isConnectedInSameTrain(parent, child)) {
            return false;
        }

        // Never overwrite an already-occupied front or rear coupler on either cart
        if (currentParentChild != null && !currentParentChild.equals(child.getUUID())) {
            Entity existingChild = level.getEntity(currentParentChild);
            if (existingChild instanceof AbstractMinecart existingCart && !existingCart.isRemoved()) {
                return false;
            }
        }

        if (currentChildParent != null && !currentChildParent.equals(parent.getUUID())) {
            Entity existingParent = level.getEntity(currentChildParent);
            if (existingParent instanceof AbstractMinecart existingCart && !existingCart.isRemoved()) {
                return false;
            }
        }

        setCartChild(parent, child.getUUID(), level);
        setCartParent(child, parent.getUUID(), level);

        try {
            Class<?> chainableClass = Class.forName("org.fuseleaf.minecarttrainsfork.chaining.Chainable");
            Method setParentChild = chainableClass.getMethod("setChainedParentChild", chainableClass, chainableClass);
            setParentChild.invoke(null, parent, child);
        } catch (Throwable ignored) {}

        return true;
    }

'@

$text = $text.Substring(0, $idx1) + $replacement1 + $text.Substring($idx2)

# 2. Replace tickConsistMovement
$startMarker2 = "    public static void tickConsistMovement(AbstractMinecart cart) {"
$endMarker2 = "    public static void pushMinecarts(AbstractMinecart a, AbstractMinecart b) {"
$idx3 = $text.IndexOf($startMarker2)
$idx4 = $text.IndexOf($endMarker2)
if ($idx3 -lt 0 -or $idx4 -lt 0) {
    throw "Markers 3/4 not found: $idx3, $idx4"
}

$replacement2 = @'
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

        // Find the cart in the consist with the highest active horizontal speed to act as the force driver
        AbstractMinecart driver = head;
        double maxSpeedSq = head.getDeltaMovement().horizontalDistanceSqr();
        Set<UUID> visited = new HashSet<>();
        AbstractMinecart curr = head;
        while (curr != null && visited.add(curr.getUUID())) {
            double spSq = curr.getDeltaMovement().horizontalDistanceSqr();
            if (spSq > maxSpeedSq) {
                maxSpeedSq = spSq;
                driver = curr;
            }
            UUID cId = getCartChildUUID(curr);
            if (cId == null) break;
            Entity cEnt = cart.level().getEntity(cId);
            curr = (cEnt instanceof AbstractMinecart mc && !mc.isRemoved()) ? mc : null;
        }

        Direction dir = driver.getMotionDirection();
        handleChainedCartsMovement(driver, dir);
    }

'@

$text = $text.Substring(0, $idx3) + $replacement2 + $text.Substring($idx4)

# 3. Replace traceRailLink, moveConsistCartAlongRails, and handleChainedCartsMovement
$startMarker3 = "    public static RailPathLink traceRailLink(Level level, Entity prev, AbstractMinecart next) {"
$endMarker3 = "    public static void pushCartFromLocomotiveBumper(AbstractMinecart loco, AbstractMinecart otherCart) {"
$idx5 = $text.IndexOf($startMarker3)
$idx6 = $text.IndexOf($endMarker3)
if ($idx5 -lt 0 -or $idx6 -lt 0) {
    throw "Markers 5/6 not found: $idx5, $idx6"
}

$replacement3 = @'
    public static RailPathLink traceRailLink(Level level, Entity prev, AbstractMinecart next) {
        Vec3 prevPos = prev.position();
        Vec3 nextPos = next.position();
        double diffX = nextPos.x - prevPos.x;
        double diffY = nextPos.y - prevPos.y;
        double diffZ = nextPos.z - prevPos.z;
        double horizChord = Math.hypot(diffX, diffZ);

        BlockPos prevRail = getCurrentBlockPosOrRailBelow(prev);
        BlockPos nextRail = getCurrentBlockPosOrRailBelow(next);
        BlockState prevState = level.getBlockState(prevRail);
        BlockState nextState = level.getBlockState(nextRail);

        if (!BaseRailBlock.isRail(prevState) || !BaseRailBlock.isRail(nextState)) {
            double dirX = horizChord > 1.0E-4D ? diffX / horizChord : 1.0D;
            double dirZ = horizChord > 1.0E-4D ? diffZ / horizChord : 0.0D;
            double dist = Math.sqrt(horizChord * horizChord + diffY * diffY);
            return new RailPathLink(dist, dirX, dirZ, false);
        }

        RailShape prevShape = ((BaseRailBlock) prevState.getBlock()).getRailDirection(prevState, level, prevRail, prev instanceof AbstractMinecart pm ? pm : null);
        RailShape nextShape = ((BaseRailBlock) nextState.getBlock()).getRailDirection(nextState, level, nextRail, next);
        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(nextShape);
        Vec3i exit0 = exits.getFirst();
        Vec3i exit1 = exits.getSecond();
        boolean nextIsCurve = !nextShape.isSlope() && nextShape != RailShape.NORTH_SOUTH && nextShape != RailShape.EAST_WEST;
        boolean prevIsCurve = !prevShape.isSlope() && prevShape != RailShape.NORTH_SOUTH && prevShape != RailShape.EAST_WEST;

        double tX;
        double tZ;
        if (nextIsCurve) {
            double circleCenterX = nextRail.getX() + 0.5 + (exit0.getX() + exit1.getX()) * 0.5;
            double circleCenterZ = nextRail.getZ() + 0.5 + (exit0.getZ() + exit1.getZ()) * 0.5;
            double vx = nextPos.x - circleCenterX;
            double vz = nextPos.z - circleCenterZ;
            double vLen = Math.sqrt(vx * vx + vz * vz);
            double ux = vLen > 1.0E-5 ? vx / vLen : 0.70710678D * -(exit0.getX() + exit1.getX());
            double uz = vLen > 1.0E-5 ? vz / vLen : 0.70710678D * -(exit0.getZ() + exit1.getZ());
            tX = -uz;
            tZ = ux;
        } else {
            double xD = exit1.getX() - exit0.getX();
            double zD = exit1.getZ() - exit0.getZ();
            double length = Math.sqrt(xD * xD + zD * zD);
            tX = length > 0 ? xD / length : (horizChord > 1.0E-4D ? diffX / horizChord : 1.0D);
            tZ = length > 0 ? zD / length : (horizChord > 1.0E-4D ? diffZ / horizChord : 0.0D);
        }

        // Orient tangent at `next` so it points away from `prev` along the rail path
        if (tX * diffX + tZ * diffZ < 0.0) {
            tX = -tX;
            tZ = -tZ;
        }

        // Continuous 3D chord distance between carts (never jumps discontinuously at block boundaries)
        double dist = Math.sqrt(horizChord * horizChord + diffY * diffY);
        if (nextIsCurve || prevIsCurve) {
            // Smooth arc-to-chord compensation around 90-degree corners
            double curveBlend = Math.min(1.0D, Math.abs(diffX) * Math.abs(diffZ) * 1.5D);
            dist *= (1.0D + 0.045D * curveBlend);
        }

        return new RailPathLink(dist, tX, tZ, true);
    }

    private static @Nullable List<BlockPos> findRailPath(Level level, BlockPos start, BlockPos target, int maxDepth) {
        if (start.equals(target)) {
            List<BlockPos> single = new ArrayList<>();
            single.add(start);
            return single;
        }

        Map<BlockPos, BlockPos> parentMap = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        int depth = 0;
        while (!queue.isEmpty() && depth < maxDepth) {
            int levelSize = queue.size();
            for (int s = 0; s < levelSize; s++) {
                BlockPos current = queue.poll();
                if (current == null) continue;
                if (current.equals(target)) {
                    List<BlockPos> path = new ArrayList<>();
                    BlockPos curr = target;
                    while (curr != null) {
                        path.add(curr);
                        curr = parentMap.get(curr);
                    }
                    Collections.reverse(path);
                    return path;
                }

                BlockState st = level.getBlockState(current);
                if (BaseRailBlock.isRail(st)) {
                    RailShape shape = ((BaseRailBlock) st.getBlock()).getRailDirection(st, level, current, null);
                    Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
                    Vec3i[] exitArray = new Vec3i[]{exits.getFirst(), exits.getSecond()};

                    for (Vec3i ex : exitArray) {
                        BlockPos nFlat = current.offset(ex.getX(), ex.getY(), ex.getZ());
                        BlockPos[] candidates;
                        if (ex.getY() == 0) {
                            candidates = new BlockPos[]{nFlat, nFlat.above(), nFlat.below()};
                        } else {
                            candidates = new BlockPos[]{nFlat};
                        }

                        for (BlockPos cand : candidates) {
                            if (visited.add(cand) && BaseRailBlock.isRail(level.getBlockState(cand))) {
                                parentMap.put(cand, current);
                                queue.add(cand);
                            }
                        }
                    }
                }
            }
            depth++;
        }
        return null;
    }

    public static boolean moveCartAlongRails(AbstractMinecart cart, double dirX, double dirZ, double distance3D) {
        return moveConsistCartAlongRails(cart, dirX, dirZ, distance3D, null);
    }

    public static boolean moveConsistCartAlongRails(AbstractMinecart cart, double dirX, double dirZ, double distance3D, @Nullable List<AbstractMinecart> consistCarts) {
        if (distance3D < 1.0E-5D) return true;

        BlockPos railPos = getCurrentBlockPosOrRailBelow(cart);
        BlockState railState = cart.level().getBlockState(railPos);
        if (!BaseRailBlock.isRail(railState)) {
            cart.move(MoverType.SELF, new Vec3(dirX * distance3D, 0.0, dirZ * distance3D));
            return !cart.horizontalCollision;
        }

        RailShape shape = ((BaseRailBlock) railState.getBlock()).getRailDirection(railState, cart.level(), railPos, cart);
        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
        Vec3i exit0 = exits.getFirst();
        Vec3i exit1 = exits.getSecond();

        double initialX = cart.getX();
        double initialY = cart.getY();
        double initialZ = cart.getZ();

        double x0 = railPos.getX() + 0.5 + exit0.getX() * 0.5;
        double z0 = railPos.getZ() + 0.5 + exit0.getZ() * 0.5;
        double x1 = railPos.getX() + 0.5 + exit1.getX() * 0.5;
        double z1 = railPos.getZ() + 0.5 + exit1.getZ() * 0.5;
        double xD = x1 - x0;
        double zD = z1 - z0;
        double progress;
        if (xD == 0.0) {
            progress = cart.getZ() - railPos.getZ();
        } else if (zD == 0.0) {
            progress = cart.getX() - railPos.getX();
        } else {
            double xx = cart.getX() - x0;
            double zz = cart.getZ() - z0;
            progress = (xx * xD + zz * zD) * 2.0;
        }

        boolean isCurve = !shape.isSlope() && shape != RailShape.NORTH_SOUTH && shape != RailShape.EAST_WEST;
        double x;
        double z;
        if (isCurve) {
            Vec3 railPosVec = getRailPosForEntity(cart, cart.getX(), cart.getY(), cart.getZ());
            if (railPosVec != null) {
                x = railPosVec.x;
                z = railPosVec.z;
            } else {
                x = x0 + xD * progress;
                z = z0 + zD * progress;
            }
        } else {
            x = x0 + xD * progress;
            z = z0 + zD * progress;
        }

        cart.setPos(x, cart.getY(), z);

        double horizStep = shape.isSlope() ? distance3D * 0.70710678D : distance3D;
        double alignedVx;
        double alignedVz;

        if (isCurve) {
            double circleCenterX = railPos.getX() + 0.5 + (exit0.getX() + exit1.getX()) * 0.5;
            double circleCenterZ = railPos.getZ() + 0.5 + (exit0.getZ() + exit1.getZ()) * 0.5;
            double vx = x - circleCenterX;
            double vz = z - circleCenterZ;
            double dist = Math.sqrt(vx * vx + vz * vz);
            double ux = dist > 1.0E-5 ? vx / dist : 0.70710678D * -(exit0.getX() + exit1.getX());
            double uz = dist > 1.0E-5 ? vz / dist : 0.70710678D * -(exit0.getZ() + exit1.getZ());
            double tX = -uz;
            double tZ = ux;
            if (tX * dirX + tZ * dirZ < 0.0) {
                tX = -tX;
                tZ = -tZ;
            }
            alignedVx = horizStep * tX;
            alignedVz = horizStep * tZ;
        } else {
            double length = Math.sqrt(xD * xD + zD * zD);
            alignedVx = dirX * horizStep;
            alignedVz = dirZ * horizStep;
            if (length > 0.0) {
                double flip = dirX * xD + dirZ * zD;
                if (flip < 0.0) {
                    xD = -xD;
                    zD = -zD;
                }
                alignedVx = horizStep * xD / length;
                alignedVz = horizStep * zD / length;
            }
        }

        // Suppress same-consist carts' bounding boxes temporarily during cart.move() so they never bind on corners
        List<Pair<AbstractMinecart, AABB>> savedBoxes = new ArrayList<>();
        if (consistCarts != null) {
            for (AbstractMinecart other : consistCarts) {
                if (other != cart && !other.isRemoved()) {
                    savedBoxes.add(Pair.of(other, other.getBoundingBox()));
                    other.setBoundingBox(new AABB(0, -1000, 0, 0, -1000, 0));
                }
            }
        }
        try {
            cart.move(MoverType.SELF, new Vec3(alignedVx, 0.0, alignedVz));
        } finally {
            for (Pair<AbstractMinecart, AABB> entry : savedBoxes) {
                entry.getFirst().setBoundingBox(entry.getSecond());
            }
        }

        Vec3 newRailPos = getRailPosForEntity(cart, cart.getX(), cart.getY(), cart.getZ());
        if (newRailPos != null) {
            cart.setPos(newRailPos.x, newRailPos.y, newRailPos.z);
        }

        double actualDx = cart.getX() - initialX;
        double actualDy = cart.getY() - initialY;
        double actualDz = cart.getZ() - initialZ;

        // Pivot cart yaw around corners using its Front and Rear Connection Points on the rail
        double moveDistSq = actualDx * actualDx + actualDz * actualDz;
        if (moveDistSq > 1.0E-5D) {
            double moveLen = Math.sqrt(moveDistSq);
            double mDirX = actualDx / moveLen;
            double mDirZ = actualDz / moveLen;
            double pivotRadius = getCartCouplerRadius(cart);
            Direction moveFacing = Direction.getApproximateNearest(mDirX, 0.0, mDirZ);

            Vec3 frontPivot = getRailPosOffs(cart, cart.getX(), cart.getY(), cart.getZ(), pivotRadius, moveFacing);
            Vec3 rearPivot = getRailPosOffs(cart, cart.getX(), cart.getY(), cart.getZ(), -pivotRadius, moveFacing);

            double chordDx = actualDx;
            double chordDz = actualDz;
            if (frontPivot != null && rearPivot != null) {
                double pDx = frontPivot.x - rearPivot.x;
                double pDz = frontPivot.z - rearPivot.z;
                if (pDx * pDx + pDz * pDz > 1.0E-4D) {
                    chordDx = pDx;
                    chordDz = pDz;
                }
            }

            float yaw = (float) (Mth.atan2(-chordDz, -chordDx) * 180.0 / Math.PI);
            if (cart.isFlipped()) {
                yaw += 180.0F;
            }
            yaw = Mth.wrapDegrees(yaw);
            float rotDiff = Mth.wrapDegrees(yaw - cart.yRotO);
            cart.yRotO = yaw - rotDiff;
            cart.setYRot(yaw);
        }

        // Preserve smooth client velocity vector for interpolation without causing runaway physics
        cart.setDeltaMovement(actualDx, actualDy, actualDz);
        cart.needsSync = true;

        boolean offRails = (newRailPos == null);
        return !offRails && !cart.horizontalCollision;
    }

    /**
     * Unified consist cart movement handler supporting all locomotive types and unpowered rolling stock.
     * Propagates push/pull forces neighbor-to-neighbor through each cart's Front and Rear Connection Points.
     */
    public static List<Pair<Entity, AbstractMinecart>> getOrderedTrainLinks(AbstractMinecart root) {
        List<Pair<Entity, AbstractMinecart>> result = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        visited.add(root.getUUID());

        Deque<Pair<Entity, UUID>> toCheck = new ArrayDeque<>();
        UUID p = getCartParentUUID(root);
        if (p != null) toCheck.add(Pair.of(root, p));
        UUID c = getCartChildUUID(root);
        if (c != null) toCheck.add(Pair.of(root, c));

        while (!toCheck.isEmpty()) {
            Pair<Entity, UUID> next = toCheck.poll();
            UUID nextId = next.getSecond();
            if (nextId != null && visited.add(nextId)) {
                Entity ent = root.level().getEntity(nextId);
                if (ent instanceof AbstractMinecart cart && !cart.isRemoved()) {
                    result.add(Pair.of(next.getFirst(), cart));
                    UUID cp = getCartParentUUID(cart);
                    if (cp != null && !visited.contains(cp)) toCheck.add(Pair.of(cart, cp));
                    UUID cc = getCartChildUUID(cart);
                    if (cc != null && !visited.contains(cc)) toCheck.add(Pair.of(cart, cc));
                }
            }
        }
        return result;
    }

    public static boolean handleChainedCartsMovement(AbstractMinecart loco, Direction facing) {
        List<Pair<Entity, AbstractMinecart>> links = getOrderedTrainLinks(loco);
        Map<UUID, Vec3> initialCartPositions = new HashMap<>();
        for (Pair<Entity, AbstractMinecart> link : links) {
            AbstractMinecart c = link.getSecond();
            initialCartPositions.put(c.getUUID(), new Vec3(c.getX(), c.getY(), c.getZ()));
        }
        return handleChainedCartsMovement(loco, facing, links, initialCartPositions, loco.getX(), loco.getY(), loco.getZ());
    }

    public static double getDirectionalCouplerRadius(Entity entity, Direction facing, boolean isFront) {
        double sign = isFront ? 1.0D : -1.0D;
        return getDirectionalCouplerRadius(entity, facing.getStepX() * sign, facing.getStepZ() * sign);
    }

    public static boolean handleChainedCartsMovement(AbstractMinecart loco,
                                                     Direction facing,
                                                     List<Pair<Entity, AbstractMinecart>> links,
                                                     Map<UUID, Vec3> initialCartPositions,
                                                     double initialLocoX, double initialLocoY, double initialLocoZ) {
        if (links.isEmpty()) {
            return false;
        }

        boolean moving = isLocomotiveActive(loco) || loco.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;
        double locoSpeed = isLocomotiveActive(loco) ? getLocomotiveSpeed(loco) : loco.getDeltaMovement().horizontalDistance();
        double maxStep = moving ? Math.max(locoSpeed * 1.6D, 0.35D) : 0.20D;

        List<AbstractMinecart> consistCarts = new ArrayList<>();
        consistCarts.add(loco);
        for (Pair<Entity, AbstractMinecart> link : links) {
            consistCarts.add(link.getSecond());
        }

        boolean blocked = false;
        for (Pair<Entity, AbstractMinecart> link : links) {
            Entity prev = link.getFirst();
            AbstractMinecart cart = link.getSecond();

            RailPathLink railLink = traceRailLink(loco.level(), prev, cart);
            double trackDist = railLink.distance;

            // Target distance between cart centers = prev's connection point radius + cart's connection point radius + COUPLER_GAP
            double targetDist = getTargetCouplingDistance(prev, cart, cart.getX() - prev.getX(), cart.getZ() - prev.getZ());
            double deadband = 0.025D;
            double minAllowed = targetDist - deadband;
            double maxAllowed = targetDist + deadband;

            double tangentX = railLink.tangentX;
            double tangentZ = railLink.tangentZ;

            if (trackDist > maxAllowed) {
                // Pull force transmitted through connection points: pull cart along rails toward prev
                double needed = Math.min(trackDist - targetDist, maxStep);
                boolean success = moveConsistCartAlongRails(cart, -tangentX, -tangentZ, needed, consistCarts);
                if (!success && moving && isLocomotiveRunning(loco)) {
                    blocked = true;
                    break;
                }
            } else if (trackDist < minAllowed) {
                // Push force transmitted through connection points: push cart along rails away from prev
                double needed = Math.min(targetDist - trackDist, maxStep);
                boolean success = moveConsistCartAlongRails(cart, tangentX, tangentZ, needed, consistCarts);
                if (!success && moving && isLocomotiveRunning(loco)) {
                    blocked = true;
                    break;
                }
            } else {
                Vec3 snapped = getRailPosForEntity(cart, cart.getX(), cart.getY(), cart.getZ());
                if (snapped != null) {
                    cart.setPos(snapped.x, snapped.y, snapped.z);
                }
                if (!moving) {
                    cart.setDeltaMovement(Vec3.ZERO);
                }
            }
        }

        if (blocked) {
            // Revert locomotive position to before this tick's move so it does NOT drive through carts
            loco.setPos(initialLocoX, initialLocoY, initialLocoZ);
            Vec3 railPos = getRailPosForEntity(loco, initialLocoX, initialLocoY, initialLocoZ);
            if (railPos != null) {
                loco.setPos(railPos.x, railPos.y, railPos.z);
            }
            loco.setDeltaMovement(Vec3.ZERO);
            stopLocomotive(loco);
            syncTrainConsist(loco, false, getLocomotiveSpeedLevel(loco), isLocomotiveReversed(loco));
            loco.level().playSound(null, loco.blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.BLOCKS, 0.6F, 1.4F);

            // Revert all carts that moved forward this tick
            for (Pair<Entity, AbstractMinecart> link : links) {
                AbstractMinecart c = link.getSecond();
                Vec3 initPos = initialCartPositions.get(c.getUUID());
                if (initPos != null) {
                    c.setPos(initPos.x, initPos.y, initPos.z);
                    Vec3 cRail = getRailPosForEntity(c, initPos.x, initPos.y, initPos.z);
                    if (cRail != null) {
                        c.setPos(cRail.x, cRail.y, cRail.z);
                    }
                    c.setDeltaMovement(Vec3.ZERO);
                }
            }
            return true;
        }

        return false;
    }

'@

$text = $text.Substring(0, $idx5) + $replacement3 + $text.Substring($idx6)

[System.IO.File]::WriteAllText($path, $text, $utf8NoBom)
Write-Host "Successfully updated TrainChainingUtil.java"
