package dev.birinder.ac.entity.ai;

import dev.birinder.ac.block.TavernBenchBlock;
import dev.birinder.ac.block.TavernTableBlock;
import dev.birinder.ac.entity.custom.SeatEntity;
import dev.birinder.ac.entity.custom.TavernVillagerEntity;
import dev.birinder.ac.util.SitUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class TavernVillagerSeekBenchGoal extends Goal {

    private final TavernVillagerEntity villager;
    private BlockPos targetSeatPos = null;
    private BlockPos approachPos = null;
    private int searchCooldown = 0;
    private int sitDuration = 0;
    private int maxSitDuration = 0;
    private int navigationStuckTicks = 0;
    private Vec3d lastPosition = Vec3d.ZERO;

    public TavernVillagerSeekBenchGoal(TavernVillagerEntity villager) {
        this.villager = villager;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.villager.hasVehicle()) {
            return false;
        }

        if (this.searchCooldown-- > 0) {
            return false;
        }
        this.searchCooldown = 30; // Search every 1.5 seconds

        this.targetSeatPos = findEligibleSeat();
        if (this.targetSeatPos != null) {
            this.approachPos = findWalkableApproach(this.villager.getWorld(), this.targetSeatPos);
            // Claim this seat position and adjacent spots
            SitUtil.claimSeat(this.villager.getWorld(), this.targetSeatPos, this.villager.getUuid(), 300);
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        // If seated, continue until sit duration completes
        if (this.villager.hasVehicle()) {
            return this.sitDuration < this.maxSitDuration;
        }

        if (this.targetSeatPos == null || this.navigationStuckTicks > 80) {
            return false;
        }

        World world = this.villager.getWorld();
        return !SitUtil.isSeatOccupied(world, this.targetSeatPos)
                && !SitUtil.isSeatReservedForPlayer(world, this.targetSeatPos)
                && !SitUtil.isSeatClaimedByOther(world, this.targetSeatPos, this.villager.getUuid());
    }

    @Override
    public void start() {
        this.sitDuration = 0;
        this.navigationStuckTicks = 0;
        this.lastPosition = this.villager.getPos();

        if (this.targetSeatPos != null) {
            startNavigationTowardsSeat();
        }
    }

    private void startNavigationTowardsSeat() {
        World world = this.villager.getWorld();
        if (this.approachPos == null || !canStandAt(world, this.approachPos)) {
            this.approachPos = findWalkableApproach(world, this.targetSeatPos);
        }

        boolean started = false;
        if (this.approachPos != null) {
            started = this.villager.getNavigation().startMovingTo(
                    this.approachPos.getX() + 0.5,
                    this.approachPos.getY(),
                    this.approachPos.getZ() + 0.5,
                    0.75
            );
        }

        if (!started) {
            // Fallback: pathfind with distance 1 to allow pathing beside the solid bench
            Path path = this.villager.getNavigation().findPathTo(this.targetSeatPos, 1);
            if (path != null) {
                this.villager.getNavigation().startMovingAlong(path, 0.75);
            }
        }
    }

    @Override
    public void tick() {
        World world = this.villager.getWorld();

        // 1. SEATED BEHAVIOR
        if (this.villager.hasVehicle()) {
            this.sitDuration++;
            Entity vehicle = this.villager.getVehicle();
            if (vehicle != null) {
                float vehicleYaw = vehicle.getYaw();
                this.villager.setYaw(vehicleYaw);
                this.villager.setBodyYaw(vehicleYaw);
                this.villager.setHeadYaw(vehicleYaw);

                // Look straight forward across the table or room
                double lookX = this.villager.getX() - Math.sin(Math.toRadians(vehicleYaw)) * 2.0;
                double lookZ = this.villager.getZ() + Math.cos(Math.toRadians(vehicleYaw)) * 2.0;
                this.villager.getLookControl().lookAt(lookX, this.villager.getEyeY(), lookZ);

                // Refresh seat claim periodically while seated
                if (this.sitDuration % 40 == 0) {
                    BlockPos seatPos = (vehicle instanceof SeatEntity seat) ? seat.getSeatSourcePos() : vehicle.getBlockPos();
                    SitUtil.claimSeat(world, seatPos, this.villager.getUuid(), 100);
                }
            }
            return;
        }

        if (this.targetSeatPos == null) {
            return;
        }

        // 2. CHECK IF SEAT BECAME OCCUPIED
        if (SitUtil.isSeatOccupied(world, this.targetSeatPos) || SitUtil.isSeatReservedForPlayer(world, this.targetSeatPos)) {
            SitUtil.releaseSeatClaim(world, this.targetSeatPos, this.villager.getUuid());
            this.targetSeatPos = null;
            return;
        }

        // Refresh claim while walking
        SitUtil.claimSeat(world, this.targetSeatPos, this.villager.getUuid(), 100);

        this.villager.getLookControl().lookAt(
                this.targetSeatPos.getX() + 0.5,
                this.targetSeatPos.getY() + 0.5,
                this.targetSeatPos.getZ() + 0.5
        );

        // 3. ARRIVAL / PROXIMITY CHECK
        if (isBesideSeat(this.targetSeatPos, this.villager.getPos())) {
            this.villager.getNavigation().stop();
            BlockState seatBlockState = world.getBlockState(this.targetSeatPos);
            double yOffset = (seatBlockState.getBlock() instanceof TavernBenchBlock) ? SitUtil.BENCH_OFFSET : SitUtil.STOOL_OFFSET;
            if (SitUtil.sitEntity(world, this.targetSeatPos, this.villager, yOffset)) {
                this.sitDuration = 0;
                this.maxSitDuration = 600 + this.villager.getRandom().nextInt(800); // 30 - 70 seconds
                SitUtil.claimSeat(world, this.targetSeatPos, this.villager.getUuid(), this.maxSitDuration + 100);
                this.targetSeatPos = null;
                this.approachPos = null;
            } else {
                SitUtil.releaseSeatClaim(world, this.targetSeatPos, this.villager.getUuid());
                this.targetSeatPos = null;
                this.approachPos = null;
            }
            return;
        }

        // 4. NAVIGATION & STUCK DETECTION
        Vec3d currentPos = this.villager.getPos();
        if (currentPos.squaredDistanceTo(this.lastPosition) < 0.005) {
            this.navigationStuckTicks++;
        } else {
            this.navigationStuckTicks = 0;
            this.lastPosition = currentPos;
        }

        // Re-issue pathfinding if idle or periodically
        if (this.villager.getNavigation().isIdle() || world.getTime() % 30 == 0) {
            startNavigationTowardsSeat();
        }
    }

    @Override
    public void stop() {
        World world = this.villager.getWorld();

        if (this.targetSeatPos != null) {
            SitUtil.releaseSeatClaim(world, this.targetSeatPos, this.villager.getUuid());
            this.targetSeatPos = null;
            this.approachPos = null;
        }

        if (this.villager.hasVehicle()) {
            Entity vehicle = this.villager.getVehicle();
            if (vehicle instanceof SeatEntity seat) {
                SitUtil.releaseSeatClaim(world, seat.getSeatSourcePos(), this.villager.getUuid());
            }
            this.villager.dismountVehicle();
        }

        // 15-30s delay before seeking seat again so villagers take natural breaks, stroll, and look at market stalls
        this.searchCooldown = 300 + this.villager.getRandom().nextInt(300);
        this.sitDuration = 0;
        this.navigationStuckTicks = 0;
    }

    private BlockPos findEligibleSeat() {
        World world = this.villager.getWorld();
        BlockPos origin = this.villager.getBlockPos();
        int radius = 24; // Full 24-block tavern detection range

        int totalSeats = 0;
        int occupiedSeats = 0;
        List<BlockPos> availableBenches = new ArrayList<>();
        List<BlockPos> availableStools = new ArrayList<>();

        for (BlockPos pos : BlockPos.iterate(origin.add(-radius, -3, -radius), origin.add(radius, 3, radius))) {
            BlockState state = world.getBlockState(pos);
            boolean isBench = state.getBlock() instanceof TavernBenchBlock;
            boolean isStool = state.getBlock() instanceof dev.birinder.ac.block.TavernStoolBlock;
            if (isBench || isStool) {
                totalSeats++;
                boolean occupied = SitUtil.isSeatOccupied(world, pos)
                        || SitUtil.isSeatReservedForPlayer(world, pos)
                        || SitUtil.isSeatClaimedByOther(world, pos, this.villager.getUuid());
                if (occupied) {
                    occupiedSeats++;
                } else {
                    BlockPos approach = findWalkableApproach(world, pos);
                    if (approach != null) {
                        if (isBench) {
                            availableBenches.add(pos.toImmutable());
                        } else {
                            availableStools.add(pos.toImmutable());
                        }
                    }
                }
            }
        }

        if (totalSeats == 0 || (availableBenches.isEmpty() && availableStools.isEmpty())) {
            return null;
        }

        // =========================================================================
        // OCCUPANCY SYSTEM: Ensure villagers do NOT take every bench!
        // =========================================================================
        // 1. In any area with 2+ seats, ALWAYS leave at least 1 open seat for the player!
        if (totalSeats > 1 && occupiedSeats >= totalSeats - 1) {
            return null; // Reserved for player seating
        }

        // 2. Maximum occupancy limit (max ~55% of all tavern seating)
        int maxOccupancy = Math.max(1, (int) Math.floor(totalSeats * 0.55));
        if (totalSeats > 2 && occupiedSeats >= maxOccupancy) {
            return null; // Keep tavern spacious with walking patrons
        }

        // 3. For a single lone bench, villager only sits with a 40% chance so it isn't monopolized
        if (totalSeats == 1 && this.villager.getRandom().nextFloat() > 0.40F) {
            return null;
        }

        // Prefer benches over stools, and prefer seats with personal space (not immediately beside another occupied seat)
        List<BlockPos> candidates = !availableBenches.isEmpty() ? availableBenches : availableStools;

        BlockPos bestSeat = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BlockPos pos : candidates) {
            boolean hasOccupiedNeighbor = false;
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos neighbor = pos.offset(dir);
                if (SitUtil.isSeatOccupied(world, neighbor)) {
                    hasOccupiedNeighbor = true;
                    break;
                }
            }

            double distSq = origin.getSquaredDistance(pos);
            // If another seated entity is on the immediate adjacent block of this row, penalize distance
            // so villagers distribute themselves across the tavern rather than bunching together
            if (hasOccupiedNeighbor) {
                distSq += 25.0;
            }

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestSeat = pos;
            }
        }

        return bestSeat;
    }

    /**
     * Computes the nearest walkable floor block adjacent to the bench (front, back, left, right)
     * so mob navigation paths to a standing position rather than into the solid bench collision box.
     */
    private BlockPos findWalkableApproach(World world, BlockPos benchPos) {
        BlockState state = world.getBlockState(benchPos);
        Direction facing = state.contains(TavernBenchBlock.FACING) ? state.get(TavernBenchBlock.FACING) : null;

        List<BlockPos> candidates = new ArrayList<>();
        if (facing != null) {
            // Priority 1: In front of the bench (between bench and table/street)
            candidates.add(benchPos.offset(facing));
            // Priority 2: Behind the bench
            candidates.add(benchPos.offset(facing.getOpposite()));
            // Priority 3: Sides of the bench
            candidates.add(benchPos.offset(facing.rotateYClockwise()));
            candidates.add(benchPos.offset(facing.rotateYCounterclockwise()));
        } else {
            for (Direction dir : Direction.Type.HORIZONTAL) {
                candidates.add(benchPos.offset(dir));
            }
        }

        // Return the first candidate where the mob can physically stand
        for (BlockPos candidate : candidates) {
            if (canStandAt(world, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean canStandAt(World world, BlockPos pos) {
        BlockState floor = world.getBlockState(pos.down());
        BlockState atFeet = world.getBlockState(pos);
        BlockState atHead = world.getBlockState(pos.up());

        return !floor.isAir()
                && floor.isSolidBlock(world, pos.down())
                && !atFeet.blocksMovement()
                && !atHead.blocksMovement()
                && !(atFeet.getBlock() instanceof TavernBenchBlock)
                && !(atFeet.getBlock() instanceof TavernTableBlock);
    }

    private boolean isBesideSeat(BlockPos seatPos, Vec3d villagerPos) {
        double dx = villagerPos.x - (seatPos.getX() + 0.5);
        double dz = villagerPos.z - (seatPos.getZ() + 0.5);
        double dy = Math.abs(villagerPos.y - seatPos.getY());

        return (dx * dx + dz * dz) <= 3.5 && dy <= 1.25;
    }
}
