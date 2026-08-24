package dev.birinder.ac.util;

import dev.birinder.ac.block.GamblingTableBlock;
import dev.birinder.ac.block.TavernBenchBlock;
import dev.birinder.ac.block.TavernTableBlock;
import dev.birinder.ac.entity.ModEntities;
import dev.birinder.ac.entity.custom.SeatEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SitUtil {

    // Calibrated sweet spot: buttocks nestle firmly inside the bench wood with clean leg drape!
    public static final double STOOL_OFFSET = 0.59;  // 10-pixel cushioned stool
    public static final double BENCH_OFFSET = 0.40;  // Thick KCD medieval bench (deep firm seat, slightly inside bench)

    // Seat reservation map: BlockPos -> expiration game tick (60s player reservation window)
    private static final Map<BlockPos, Long> RESERVED_SEATS = new HashMap<>();

    public static boolean sitPlayer(World world, BlockPos pos, LivingEntity player, double yOffset) {
        return sitEntity(world, pos, player, yOffset);
    }

    public static boolean sitEntity(World world, BlockPos pos, LivingEntity entity, double yOffset) {
        if (!world.isClient()) {
            // Check if seat is already occupied
            if (isSeatOccupied(world, pos)) {
                LivingEntity seated = getSeatedEntity(world, pos);
                // PLAYER DISPLACEMENT: If a human player right-clicks an NPC villager's seat, the villager stands up!
                if (entity instanceof PlayerEntity && seated instanceof VillagerEntity villager) {
                    villager.dismountVehicle();
                    // Reserve seat for 60 seconds (1200 ticks) so villagers don't rush back
                    reserveSeat(world, pos, 1200);
                } else {
                    return false; // Another player is sitting here
                }
            }

            // If a player is sitting down, reserve this seat position for 60s
            if (entity instanceof PlayerEntity) {
                reserveSeat(world, pos, 1200);
            }

            // SEAT SWITCHING: If player is already seated, smoothly hop to new seat!
            if (entity.hasVehicle()) {
                Entity currentVehicle = entity.getVehicle();
                if (currentVehicle instanceof SeatEntity) {
                    entity.dismountVehicle();
                    currentVehicle.discard();
                } else {
                    entity.dismountVehicle();
                }
            }

            // Calculate intelligent seated orientation
            float yaw = calculateOptimalSeatedYaw(world, pos, entity);

            SeatEntity seat = new SeatEntity(ModEntities.SEAT, world, pos, yOffset, yaw);
            world.spawnEntity(seat);

            entity.startRiding(seat, true);
            entity.setYaw(yaw);
            entity.setBodyYaw(yaw);
            entity.setHeadYaw(yaw);
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
            return 0.0F;   // Face North
        } else if (isTableBlock(world, seatPos.south())) {
            return 180.0F; // Face South
        } else if (isTableBlock(world, seatPos.east())) {
            return 90.0F;  // Face East
        } else if (isTableBlock(world, seatPos.west())) {
            return 270.0F; // Face West
        }

        // Priority 2: Other seated villager / player across the table within 4 blocks
        Box nearbySeatBox = new Box(seatPos).expand(4.0);
        List<LivingEntity> otherSeated = world.getEntitiesByClass(
                LivingEntity.class,
                nearbySeatBox,
                e -> e != entity && e.hasVehicle() && e.getVehicle() instanceof SeatEntity
        );

        if (!otherSeated.isEmpty()) {
            LivingEntity partner = otherSeated.get(0);
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
        if (world.getTime() > expiry) {
            RESERVED_SEATS.remove(pos);
            return false;
        }
        return true;
    }

    public static boolean isSeatOccupied(World world, BlockPos pos) {
        Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.2, pos.getZ() + 1.0);
        List<SeatEntity> seats = world.getEntitiesByClass(SeatEntity.class, box,
                s -> s.hasPassengers());
        return !seats.isEmpty();
    }

    public static LivingEntity getSeatedEntity(World world, BlockPos pos) {
        Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.2, pos.getZ() + 1.0);
        List<SeatEntity> seats = world.getEntitiesByClass(SeatEntity.class, box,
                s -> s.hasPassengers());
        if (!seats.isEmpty()) {
            Entity passenger = seats.get(0).getFirstPassenger();
            if (passenger instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }
}