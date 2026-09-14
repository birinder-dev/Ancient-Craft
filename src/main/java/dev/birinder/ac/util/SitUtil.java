package dev.birinder.ac.util;

import dev.birinder.ac.block.GamblingTableBlock;
import dev.birinder.ac.block.TavernBenchBlock;
import dev.birinder.ac.block.TavernTableBlock;
import dev.birinder.ac.entity.custom.SeatEntity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Dismounting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SitUtil {

    // Calibrated sweet spots: sits firmly and naturally for both Player and NPC on stools and benches
    public static final double STOOL_OFFSET = 0.65; // Cushioned stool surface
    public static final double BENCH_OFFSET = 0.45; // Medieval bench surface

    // Seat reservation map: BlockPos -> expiration game tick (60s player reservation window)
    private static final Map<BlockPos, Long> RESERVED_SEATS = new HashMap<>();
    // Seat claim map for walking AI: BlockPos -> ClaimInfo (prevents multiple villagers walking to same seat)
    private static final Map<BlockPos, SeatClaim> CLAIMED_SEATS = new HashMap<>();
    // Active occupied seats map: BlockPos -> Seated Entity UUID (immediate in-memory tracking)
    private static final Map<BlockPos, java.util.UUID> ACTIVE_SEATS = new java.util.concurrent.ConcurrentHashMap<>();

    public static void vacateSeat(BlockPos pos) {
        if (pos != null) {
            ACTIVE_SEATS.remove(pos.toImmutable());
        }
    }

    public record SeatClaim(java.util.UUID entityUuid, long expiryTick) {
    }

    public static boolean sitPlayer(World world, BlockPos pos, LivingEntity player, double yOffset) {
        return sitEntity(world, pos, player, yOffset);
    }

    public static boolean sitEntity(World world, BlockPos pos, LivingEntity entity, double yOffset) {
        if (!world.isClient()) {
            // Check if seat is already occupied
            if (isSeatOccupied(world, pos)) {
                LivingEntity seated = getSeatedEntity(world, pos);
                if (entity instanceof PlayerEntity) {
                    if (seated instanceof PlayerEntity) {
                        return false; // Another human player is sitting here — cannot displace
                    } else if (seated != null) {
                        // NPC is sitting here (Tavern Villager, Gambler, etc.) -> make them stand up!
                        seated.dismountVehicle();
                        reserveSeat(world, pos, 1200); // Reserve for 60 seconds
                        return true; // NPC stood up! Player can click again to sit down.
                    }
                } else {
                    return false; // NPCs cannot displace anyone
                }
            }

            // If a player is sitting down, reserve this seat position for 60s
            if (entity instanceof PlayerEntity) {
                reserveSeat(world, pos, 1200);
            }

            // SEAT SWITCHING: If hopping from another seat, dismount cleanly
            if (entity.hasVehicle()) {
                Entity currentVehicle = entity.getVehicle();
                entity.dismountVehicle();
                if (currentVehicle instanceof SeatEntity) {
                    currentVehicle.discard();
                }
            }

            // Calculate intelligent seated orientation
            float yaw = calculateOptimalSeatedYaw(world, pos, entity);

            // Spawn dedicated SeatEntity at the exact seat surface height
            SeatEntity seat = new SeatEntity(world, pos, yOffset);
            seat.setYaw(yaw);
            seat.setBodyYaw(yaw);
            seat.setHeadYaw(yaw);

            world.spawnEntity(seat);

            boolean mounted = entity.startRiding(seat, true);
            if (!mounted) {
                seat.discard();
                return false;
            }

            entity.setYaw(yaw);
            entity.setBodyYaw(yaw);
            entity.setHeadYaw(yaw);

            // Record this seat as immediately occupied
            ACTIVE_SEATS.put(pos.toImmutable(), entity.getUuid());

            // Stop active navigation immediately
            if (entity instanceof net.minecraft.entity.mob.MobEntity mob) {
                mob.getNavigation().stop();
            }

            // Release any temporary pathing claim now that entity is physically seated
            releaseSeatClaim(world, pos, entity.getUuid());

            return true;
        }
        return false;
    }

    /**
     * Intelligent Seated Orientation:
     * 1. Direct adjacent table (North, South, East, West)
     * 2. Nearby seated conversation partner across the table
     * 3. Nearest table within 20 blocks
     * 4. Bench default facing or dominant EAST direction (90°)
     */
    public static float calculateOptimalSeatedYaw(World world, BlockPos seatPos, LivingEntity entity) {
        // Priority 1: Directly adjacent table
        if (isTableBlock(world, seatPos.north())) {
            return Direction.NORTH.asRotation(); // 180.0F
        } else if (isTableBlock(world, seatPos.south())) {
            return Direction.SOUTH.asRotation(); // 0.0F
        } else if (isTableBlock(world, seatPos.east())) {
            return Direction.EAST.asRotation(); // 270.0F
        } else if (isTableBlock(world, seatPos.west())) {
            return Direction.WEST.asRotation(); // 90.0F
        }

        // Priority 2: Other seated villager / player across the table within 4 blocks
        Box nearbySeatBox = new Box(seatPos).expand(4.0);
        List<Entity> occupants = world.getOtherEntities(entity, nearbySeatBox,
                e -> e instanceof LivingEntity && e.hasVehicle()
                        && e.getVehicle() instanceof SeatEntity);

        if (!occupants.isEmpty()) {
            Entity partner = occupants.get(0);
            double dx = partner.getX() - (seatPos.getX() + 0.5);
            double dz = partner.getZ() - (seatPos.getZ() + 0.5);
            return (float) Math.toDegrees(Math.atan2(-dx, dz));
        }

        // Priority 3: Nearest table anywhere in a 20-block radius
        BlockPos nearestTable = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (BlockPos p : BlockPos.iterate(seatPos.add(-20, -4, -20), seatPos.add(20, 4, 20))) {
            if (isTableBlock(world, p)) {
                double distSq = seatPos.getSquaredDistance(p);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearestTable = p.toImmutable();
                }
            }
        }

        if (nearestTable != null) {
            double dx = (nearestTable.getX() + 0.5) - (seatPos.getX() + 0.5);
            double dz = (nearestTable.getZ() + 0.5) - (seatPos.getZ() + 0.5);
            return (float) Math.toDegrees(Math.atan2(-dx, dz));
        }

        // Priority 4: Bench placement orientation or dominant EAST direction
        BlockState seatState = world.getBlockState(seatPos);
        if (seatState.getBlock() instanceof TavernBenchBlock && seatState.contains(TavernBenchBlock.FACING)) {
            return seatState.get(TavernBenchBlock.FACING).asRotation();
        }

        return 90.0F; // Dominant East direction
    }

    private static boolean isTableBlock(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof TavernTableBlock
                || world.getBlockState(pos).getBlock() instanceof GamblingTableBlock;
    }

    public static void reserveSeat(World world, BlockPos pos, long durationTicks) {
        RESERVED_SEATS.put(pos.toImmutable(), world.getTime() + durationTicks);
    }

    public static boolean isSeatReservedForPlayer(World world, BlockPos pos) {
        Long expiry = RESERVED_SEATS.get(pos);
        if (expiry == null) {
            return false;
        }
        long now = world.getTime();
        if (now > expiry || (expiry - now) > 6000) {
            RESERVED_SEATS.remove(pos);
            return false;
        }
        return true;
    }

    public static boolean isSeatOccupied(World world, BlockPos pos) {
        BlockPos immutable = pos.toImmutable();

        // 1. Direct query for any SeatEntity at this block that has passengers
        Box searchBox = new Box(immutable).expand(0.2);
        List<SeatEntity> seats = world.getEntitiesByClass(SeatEntity.class, searchBox, SeatEntity::hasPassengers);
        if (!seats.isEmpty()) {
            Entity passenger = seats.get(0).getFirstPassenger();
            if (passenger != null) {
                ACTIVE_SEATS.put(immutable, passenger.getUuid());
                return true;
            }
        }

        // 2. Fast in-memory check (verifies entity is alive and still riding)
        java.util.UUID seatedUuid = ACTIVE_SEATS.get(immutable);
        if (seatedUuid != null) {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                Entity seated = serverWorld.getEntity(seatedUuid);
                if (seated != null && seated.isAlive() && seated.hasVehicle()) {
                    return true;
                } else {
                    ACTIVE_SEATS.remove(immutable);
                }
            } else {
                return true;
            }
        }

        // 3. Check for any living entity seated directly on top of this block
        Box entityBox = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.5, pos.getZ() + 1.0);
        List<LivingEntity> livingOnBlock = world.getEntitiesByClass(
                LivingEntity.class, entityBox,
                LivingEntity::hasVehicle);
        if (!livingOnBlock.isEmpty()) {
            ACTIVE_SEATS.put(immutable, livingOnBlock.get(0).getUuid());
            return true;
        }

        return false;
    }

    public static LivingEntity getSeatedEntity(World world, BlockPos pos) {
        Box searchBox = new Box(pos.toImmutable()).expand(0.2);
        List<SeatEntity> seats = world.getEntitiesByClass(SeatEntity.class, searchBox, SeatEntity::hasPassengers);
        if (!seats.isEmpty()) {
            Entity passenger = seats.get(0).getFirstPassenger();
            if (passenger instanceof LivingEntity living) {
                return living;
            }
        }

        Box entityBox = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.5, pos.getZ() + 1.0);
        List<LivingEntity> livingOnBlock = world.getEntitiesByClass(
                LivingEntity.class, entityBox,
                LivingEntity::hasVehicle);
        if (!livingOnBlock.isEmpty()) {
            return livingOnBlock.get(0);
        }

        return null;
    }

    public static boolean claimSeat(World world, BlockPos pos, java.util.UUID entityUuid, long durationTicks) {
        BlockPos immutable = pos.toImmutable();
        SeatClaim existing = CLAIMED_SEATS.get(immutable);
        long now = world.getTime();
        // If expired or same entity, allow claim
        if (existing != null && now >= 0 && now < existing.expiryTick() && !existing.entityUuid().equals(entityUuid)) {
            // Also verify claim is not stale across world reloads (max 5 minutes)
            if ((existing.expiryTick() - now) <= 6000) {
                return false; // Claimed by someone else
            }
        }
        CLAIMED_SEATS.put(immutable, new SeatClaim(entityUuid, now + durationTicks));
        return true;
    }

    public static void releaseSeatClaim(World world, BlockPos pos, java.util.UUID entityUuid) {
        if (pos == null || entityUuid == null)
            return;
        BlockPos immutable = pos.toImmutable();
        SeatClaim existing = CLAIMED_SEATS.get(immutable);
        if (existing != null && existing.entityUuid().equals(entityUuid)) {
            CLAIMED_SEATS.remove(immutable);
        }
    }

    public static boolean isSeatClaimedByOther(World world, BlockPos pos, java.util.UUID entityUuid) {
        BlockPos immutable = pos.toImmutable();
        SeatClaim existing = CLAIMED_SEATS.get(immutable);
        if (existing == null) {
            return false;
        }
        long now = world.getTime();
        // If expired or if time reset across world reload:
        if (now < 0 || now > existing.expiryTick() || (existing.expiryTick() - now) > 6000) {
            CLAIMED_SEATS.remove(immutable);
            return false;
        }
        return !existing.entityUuid().equals(entityUuid);
    }

    /**
     * Finds a safe floor position next to the bench/stool so the player or mob
     * dismounts standing cleanly beside the furniture, instead of inside or on top of it.
     */
    public static Vec3d findSafeDismountPosition(World world, BlockPos seatPos, LivingEntity entity) {
        BlockState seatState = world.getBlockState(seatPos);
        Direction facing = null;
        if (seatState.contains(TavernBenchBlock.FACING)) {
            facing = seatState.get(TavernBenchBlock.FACING);
        }

        List<BlockPos> candidates = new ArrayList<>();

        if (facing != null) {
            candidates.add(seatPos.offset(facing.getOpposite()));
            candidates.add(seatPos.offset(facing.rotateYClockwise()));
            candidates.add(seatPos.offset(facing.rotateYCounterclockwise()));
            candidates.add(seatPos.offset(facing.rotateYClockwise(), 2));
            candidates.add(seatPos.offset(facing.rotateYCounterclockwise(), 2));
            candidates.add(seatPos.offset(facing.getOpposite()).offset(facing.rotateYClockwise()));
            candidates.add(seatPos.offset(facing.getOpposite()).offset(facing.rotateYCounterclockwise()));
            candidates.add(seatPos.offset(facing));
            candidates.add(seatPos.offset(facing).offset(facing.rotateYClockwise()));
            candidates.add(seatPos.offset(facing).offset(facing.rotateYCounterclockwise()));
        } else {
            Direction entityFacing = entity.getHorizontalFacing();
            candidates.add(seatPos.offset(entityFacing.getOpposite()));
            candidates.add(seatPos.offset(entityFacing.rotateYClockwise()));
            candidates.add(seatPos.offset(entityFacing.rotateYCounterclockwise()));
            candidates.add(seatPos.offset(entityFacing));
            for (int dx = -1; dx <= 1; dx += 2) {
                for (int dz = -1; dz <= 1; dz += 2) {
                    candidates.add(seatPos.add(dx, 0, dz));
                }
            }
        }

        for (BlockPos candidate : candidates) {
            if (!isFurnitureBlock(world, candidate)) {
                Vec3d respawn = Dismounting.findRespawnPos(entity.getType(), world, candidate, false);
                if (respawn != null) {
                    return respawn;
                }
            }
            BlockPos candidateDown = candidate.down();
            if (!isFurnitureBlock(world, candidateDown)) {
                Vec3d respawn = Dismounting.findRespawnPos(entity.getType(), world, candidateDown, false);
                if (respawn != null) {
                    return respawn;
                }
            }
        }

        BlockPos candidateUp = seatPos.up();
        Vec3d respawnUp = Dismounting.findRespawnPos(entity.getType(), world, candidateUp, false);
        if (respawnUp != null) {
            return respawnUp;
        }

        return new Vec3d(seatPos.getX() + 0.5, seatPos.getY() + 1.0, seatPos.getZ() + 0.5);
    }

    public static boolean isFurnitureBlock(World world, BlockPos pos) {
        return isFurnitureBlock(world.getBlockState(pos));
    }

    public static boolean isFurnitureBlock(BlockState state) {
        return state.getBlock() instanceof TavernBenchBlock
                || state.getBlock() instanceof TavernTableBlock
                || state.getBlock() instanceof dev.birinder.ac.block.TavernStoolBlock
                || state.getBlock() instanceof GamblingTableBlock
                || state.getBlock() instanceof dev.birinder.ac.block.GamblingStoolBlock;
    }
}