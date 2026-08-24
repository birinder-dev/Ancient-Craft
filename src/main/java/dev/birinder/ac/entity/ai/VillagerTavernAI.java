package dev.birinder.ac.entity.ai;

import dev.birinder.ac.block.TavernBenchBlock;
import dev.birinder.ac.block.TavernTableBlock;
import dev.birinder.ac.entity.custom.SeatEntity;
import dev.birinder.ac.util.SitUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

public class VillagerTavernAI {

    public static final int RANGE = 12; // 12-block search radius
    private static long lastGlobalDialogueTick = 0;

    // Entity UUID -> State tracking
    private static final Map<UUID, VillagerState> STATES = new HashMap<>();

    private static final List<String> DIALOGUE_WEATHER = List.of(
            "Looks like rain's brewing over the hills...",
            "My joints ache today. Weather's turning foul for sure.",
            "A fine crisp breeze today. Good for brewing ale.",
            "Sun's harsh today. Dust gets into everything."
    );

    private static final List<String> DIALOGUE_TRADE = List.of(
            "Grain prices are highway robbery these days.",
            "The blacksmith charged me two emeralds for a simple door hinge!",
            "Merchant caravan from the dunes brought sour wine again.",
            "Can't even trade a bushel of wheat without taxes doubling."
    );

    private static final List<String> DIALOGUE_OCCUPATION = List.of(
            "Been tending fields since sunrise... my back is killing me.",
            "If the miller cheats me on wheat weight one more time...",
            "Tending the sheep all day with wolves lurking in the scrub...",
            "Another day, another harvest. At least the tavern is dry."
    );

    private static final List<String> DIALOGUE_TRAVELLERS = List.of(
            "Look at that wanderer strutting around with weapons drawn...",
            "These adventurers carry trouble wherever their boots step.",
            "Always rummaging through our barrels. What are they looking for?!",
            "They jump across our rooftops like mangy alley cats...",
            "Another stranger thinking they own the whole village."
    );

    public static void tick(VillagerEntity villager, ServerWorld world) {
        if (villager.isBaby() || world.isClient()) {
            return;
        }

        UUID id = villager.getUuid();
        VillagerState state = STATES.computeIfAbsent(id, k -> new VillagerState());

        // Panic or Night: stand up immediately and flee/sleep
        if (world.isNight() || villager.getBrain().hasActivity(Activity.PANIC) ||
                villager.getBrain().hasActivity(Activity.REST)) {
            if (villager.hasVehicle()) {
                villager.dismountVehicle();
            }
            state.targetSeatPos = null;
            state.sitTime = 0;
            state.pathingTicks = 0;
            return;
        }

        // Handle when already seated
        if (villager.hasVehicle() && villager.getVehicle() instanceof SeatEntity) {
            state.sitTime++;
            if (state.sitTime >= state.maxSitDuration) {
                villager.dismountVehicle(); // Stand up so others can sit
                state.sitTime = 0;
                state.cooldown = 200; // 10s cooldown before seeking a bench again
            } else {
                tickDialogue(villager, world);
            }
            return;
        }

        // Cooldown between seat searches
        if (state.cooldown > 0) {
            state.cooldown--;
            return;
        }

        // BED-STYLE INTENT CHECK:
        // Only villagers actively seeking and headed towards their specific targetSeatPos can sit!
        // Wandering villagers walking past will NEVER be pulled in.
        if (state.targetSeatPos != null) {
            state.pathingTicks++;

            // Timeout after 15 seconds (300 ticks) if pathfinding gets stuck or obstructed
            if (state.pathingTicks > 300) {
                state.targetSeatPos = null;
                state.pathingTicks = 0;
                state.cooldown = 100;
                return;
            }

            // Check if seat became occupied or reserved while walking
            if (SitUtil.isSeatOccupied(world, state.targetSeatPos) || SitUtil.isSeatReservedForPlayer(world, state.targetSeatPos)) {
                state.targetSeatPos = null;
                state.pathingTicks = 0;
                return;
            }

            BlockPos villagerPos = villager.getBlockPos();
            BlockPos targetPos = state.targetSeatPos;

            // Arrival verification: must be standing directly beside the targeted bench
            boolean isBeside = isBesideBench(villagerPos, targetPos, villager.getPos());

            if (isBeside) {
                BenchUnit unit = getBenchUnit(world, targetPos);
                if (unit.occupiedSeats < unit.maxAllowedOccupancy()) {
                    villager.getNavigation().stop();
                    // Mount the bench cleanly at exact sitting offset
                    if (SitUtil.sitEntity(world, targetPos, villager, SitUtil.BENCH_OFFSET)) {
                        state.sitTime = 0;
                        state.pathingTicks = 0;
                        state.maxSitDuration = 600 + villager.getRandom().nextInt(700); // 30-65 seconds
                        state.targetSeatPos = null;
                        return;
                    }
                }
            } else {
                // Actively navigate towards the targeted bench
                if (villager.getNavigation().isIdle() || world.getTime() % 20 == 0) {
                    villager.getNavigation().startMovingTo(
                            targetPos.getX() + 0.5,
                            targetPos.getY(),
                            targetPos.getZ() + 0.5,
                            0.95
                    );
                }
            }
            return;
        }

        // Periodic search: choose a target bench only when idle
        if (world.getTime() % 25 == 0) {
            state.targetSeatPos = findEligibleBenchSeat(villager, world);
            if (state.targetSeatPos != null) {
                state.pathingTicks = 0;
                villager.getNavigation().startMovingTo(
                        state.targetSeatPos.getX() + 0.5,
                        state.targetSeatPos.getY(),
                        state.targetSeatPos.getZ() + 0.5,
                        0.95
                );
            }
        }
    }

    /**
     * Bed-style proximity check:
     * Checks if the villager is standing right beside the bench or adjacent to it.
     */
    private static boolean isBesideBench(BlockPos villagerPos, BlockPos benchPos, net.minecraft.util.math.Vec3d precisePos) {
        int dx = Math.abs(villagerPos.getX() - benchPos.getX());
        int dy = Math.abs(villagerPos.getY() - benchPos.getY());
        int dz = Math.abs(villagerPos.getZ() - benchPos.getZ());

        // Standing on or immediately adjacent horizontally (Manhattan distance <= 1 on X/Z and <= 1 on Y)
        if (dx <= 1 && dz <= 1 && dy <= 1) {
            return true;
        }

        // Or within 2.0 blocks precise Euclidean distance
        double distSq = precisePos.squaredDistanceTo(benchPos.getX() + 0.5, benchPos.getY() + 0.5, benchPos.getZ() + 0.5);
        return distSq <= 4.41; // 2.1 blocks
    }

    private static void tickDialogue(VillagerEntity villager, ServerWorld world) {
        long currentTick = world.getTime();
        if (currentTick - lastGlobalDialogueTick < 100) {
            return; // 5-second interval
        }

        Box playerSearchBox = new Box(villager.getBlockPos()).expand(15.0);
        List<PlayerEntity> nearbyPlayers = world.getEntitiesByClass(PlayerEntity.class, playerSearchBox, p -> !p.isSpectator());
        if (nearbyPlayers.isEmpty()) {
            return;
        }

        List<VillagerEntity> seatedPool = world.getEntitiesByClass(
                VillagerEntity.class,
                playerSearchBox,
                v -> v.hasVehicle() && v.getVehicle() instanceof SeatEntity
        );

        if (seatedPool.isEmpty()) {
            return;
        }

        VillagerEntity speaker = seatedPool.get(villager.getRandom().nextInt(seatedPool.size()));
        if (speaker == villager) {
            String line = switch (villager.getRandom().nextInt(4)) {
                case 0 -> DIALOGUE_WEATHER.get(villager.getRandom().nextInt(DIALOGUE_WEATHER.size()));
                case 1 -> DIALOGUE_TRADE.get(villager.getRandom().nextInt(DIALOGUE_TRADE.size()));
                case 2 -> DIALOGUE_OCCUPATION.get(villager.getRandom().nextInt(DIALOGUE_OCCUPATION.size()));
                default -> DIALOGUE_TRAVELLERS.get(villager.getRandom().nextInt(DIALOGUE_TRAVELLERS.size()));
            };

            Text chatMessage = Text.literal("Villager: ").formatted(Formatting.GOLD)
                    .append(Text.literal("\"" + line + "\"").formatted(Formatting.ITALIC, Formatting.WHITE));

            for (PlayerEntity player : nearbyPlayers) {
                player.sendMessage(chatMessage, false);
            }

            lastGlobalDialogueTick = currentTick;
        }
    }

    private static BlockPos findEligibleBenchSeat(VillagerEntity villager, World world) {
        BlockPos origin = villager.getBlockPos();
        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterate(origin.add(-RANGE, -3, -RANGE), origin.add(RANGE, 3, RANGE))) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof TavernBenchBlock) {
                if (!SitUtil.isSeatOccupied(world, pos) && !SitUtil.isSeatReservedForPlayer(world, pos)) {
                    BenchUnit unit = getBenchUnit(world, pos);
                    if (unit.occupiedSeats < unit.maxAllowedOccupancy()) {
                        double distSq = origin.getSquaredDistance(pos);
                        if (distSq < nearestDistSq) {
                            nearestDistSq = distSq;
                            nearest = pos.toImmutable();
                        }
                    }
                }
            }
        }
        return nearest;
    }

    private static boolean isAdjacentToTavernTable(World world, BlockPos benchPos) {
        return world.getBlockState(benchPos.north()).getBlock() instanceof TavernTableBlock
                || world.getBlockState(benchPos.south()).getBlock() instanceof TavernTableBlock
                || world.getBlockState(benchPos.east()).getBlock() instanceof TavernTableBlock
                || world.getBlockState(benchPos.west()).getBlock() instanceof TavernTableBlock;
    }

    private static BenchUnit getBenchUnit(World world, BlockPos startPos) {
        BlockState startState = world.getBlockState(startPos);
        if (!(startState.getBlock() instanceof TavernBenchBlock)) {
            return new BenchUnit(1, 0);
        }

        Direction facing = startState.get(TavernBenchBlock.FACING);
        Direction leftDir = facing.rotateYCounterclockwise();
        Direction rightDir = facing.rotateYClockwise();

        Set<BlockPos> unitPositions = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        unitPositions.add(startPos);
        queue.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction checkDir : new Direction[]{leftDir, rightDir}) {
                BlockPos neighbor = current.offset(checkDir);
                if (!unitPositions.contains(neighbor)) {
                    BlockState nState = world.getBlockState(neighbor);
                    if (nState.getBlock() instanceof TavernBenchBlock && nState.get(TavernBenchBlock.FACING).getAxis() == facing.getAxis()) {
                        unitPositions.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        int totalSeats = unitPositions.size();
        int occupiedSeats = 0;
        for (BlockPos p : unitPositions) {
            if (SitUtil.isSeatOccupied(world, p) || SitUtil.isSeatReservedForPlayer(world, p)) {
                occupiedSeats++;
            }
        }

        return new BenchUnit(totalSeats, occupiedSeats);
    }

    private static class VillagerState {
        BlockPos targetSeatPos = null;
        int sitTime = 0;
        int maxSitDuration = 600;
        int cooldown = 0;
        int pathingTicks = 0;
    }

    private record BenchUnit(int totalSeats, int occupiedSeats) {
        public int maxAllowedOccupancy() {
            if (totalSeats <= 1) {
                return 1;
            }
            return Math.max(1, (int) Math.floor(totalSeats * 0.70));
        }
    }
}
