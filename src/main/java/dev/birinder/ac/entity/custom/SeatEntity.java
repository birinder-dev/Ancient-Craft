package dev.birinder.ac.entity.custom;

import dev.birinder.ac.block.TavernBenchBlock;
import dev.birinder.ac.entity.ModEntities;
import dev.birinder.ac.util.SitUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Dismounting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class SeatEntity extends Entity {

    private BlockPos seatSourcePos;

    public SeatEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    public SeatEntity(World world, BlockPos pos, double yOffset) {
        this(ModEntities.SEAT, world);
        this.seatSourcePos = pos.toImmutable();
        this.setPosition(pos.getX() + 0.5, pos.getY() + yOffset, pos.getZ() + 0.5);
    }

    public BlockPos getSeatSourcePos() {
        return this.seatSourcePos != null ? this.seatSourcePos : this.getBlockPos();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("SeatX") && nbt.contains("SeatY") && nbt.contains("SeatZ")) {
            this.seatSourcePos = new BlockPos(nbt.getInt("SeatX"), nbt.getInt("SeatY"), nbt.getInt("SeatZ"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.seatSourcePos != null) {
            nbt.putInt("SeatX", this.seatSourcePos.getX());
            nbt.putInt("SeatY", this.seatSourcePos.getY());
            nbt.putInt("SeatZ", this.seatSourcePos.getZ());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient()) {
            if (!this.hasPassengers()) {
                this.discard();
                return;
            }
            BlockPos pos = getSeatSourcePos();
            if (!SitUtil.isFurnitureBlock(this.getWorld(), pos)) {
                this.removeAllPassengers();
                this.discard();
            }
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        // Do NOT discard immediately here!
        // LivingEntity.dismountVehicle() invokes removePassenger() FIRST,
        // and then invokes updatePassengerForDismount() on the vehicle.
        // Discarding here would cause updatePassengerForDismount() to run on a removed entity.
        // Clean removal is safely handled in tick() when !hasPassengers().
    }

    @Override
    protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scale) {
        if (passenger instanceof PlayerEntity) {
            return Vec3d.ZERO;
        }
        // Vanilla PlayerEntity has a built-in vehicle riding offset (~0.35 - 0.4 blocks) in its model/dimensions.
        // Villagers have 0.0 vehicle offset and 12px (0.75b) legs.
        // Lowering non-players by 0.60 blocks places the villager's robe and torso firmly flush right on the 9.5px bench plank!
        return new Vec3d(0.0, -0.60, 0.0);
    }

    /**
     * Boat/Minecart style dismounting:
     * Displaces the passenger to the nearest full available floor block beside the bench/stool,
     * explicitly excluding the sitting block so the player or villager never lands inside the furniture.
     */
    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        World world = this.getWorld();
        BlockPos seatPos = getSeatSourcePos();
        BlockState seatState = world.getBlockState(seatPos);

        Direction facing = null;
        if (seatState.contains(TavernBenchBlock.FACING)) {
            facing = seatState.get(TavernBenchBlock.FACING);
        }

        List<BlockPos> horizontalCandidates = new ArrayList<>();

        if (facing != null) {
            Direction behind = facing.getOpposite();
            Direction right = facing.rotateYClockwise();
            Direction left = facing.rotateYCounterclockwise();

            // Ring 1 (Immediate neighbors, strictly excluding seatPos):
            horizontalCandidates.add(seatPos.offset(behind));                     // Behind bench (aisle / room floor)
            horizontalCandidates.add(seatPos.offset(right));                      // Right end
            horizontalCandidates.add(seatPos.offset(left));                       // Left end
            horizontalCandidates.add(seatPos.offset(behind).offset(right));        // Rear right diagonal
            horizontalCandidates.add(seatPos.offset(behind).offset(left));         // Rear left diagonal
            horizontalCandidates.add(seatPos.offset(facing));                     // Front (towards table/street)
            horizontalCandidates.add(seatPos.offset(facing).offset(right));        // Front right diagonal
            horizontalCandidates.add(seatPos.offset(facing).offset(left));         // Front left diagonal

            // Ring 2 (Extended search for multi-bench rows and tight tavern alleys):
            horizontalCandidates.add(seatPos.offset(behind, 2));
            horizontalCandidates.add(seatPos.offset(right, 2));
            horizontalCandidates.add(seatPos.offset(left, 2));
            horizontalCandidates.add(seatPos.offset(facing, 2));
            horizontalCandidates.add(seatPos.offset(behind, 2).offset(right));
            horizontalCandidates.add(seatPos.offset(behind, 2).offset(left));
            horizontalCandidates.add(seatPos.offset(facing, 2).offset(right));
            horizontalCandidates.add(seatPos.offset(facing, 2).offset(left));
        } else {
            // Stool / non-directional: prioritize opposite of player facing
            Direction entityFacing = passenger.getHorizontalFacing();
            Direction behind = entityFacing.getOpposite();
            Direction right = entityFacing.rotateYClockwise();
            Direction left = entityFacing.rotateYCounterclockwise();

            horizontalCandidates.add(seatPos.offset(behind));
            horizontalCandidates.add(seatPos.offset(right));
            horizontalCandidates.add(seatPos.offset(left));
            horizontalCandidates.add(seatPos.offset(entityFacing));
            horizontalCandidates.add(seatPos.offset(behind).offset(right));
            horizontalCandidates.add(seatPos.offset(behind).offset(left));
            horizontalCandidates.add(seatPos.offset(entityFacing).offset(right));
            horizontalCandidates.add(seatPos.offset(entityFacing).offset(left));

            horizontalCandidates.add(seatPos.offset(behind, 2));
            horizontalCandidates.add(seatPos.offset(right, 2));
            horizontalCandidates.add(seatPos.offset(left, 2));
            horizontalCandidates.add(seatPos.offset(entityFacing, 2));
        }

        // Search each horizontal candidate at multiple vertical elevations:
        // Level 0 (same elevation), Level -1 (step down onto ground/slab), Level +1 (raised platform)
        int[] yOffsets = {0, -1, 1};

        Vec3d safePos = null;
        for (BlockPos hPos : horizontalCandidates) {
            for (int dy : yOffsets) {
                BlockPos candidate = hPos.up(dy);
                if (isFullAvailableBlock(world, candidate, seatPos)) {
                    safePos = getDismountVec(world, candidate);
                    break;
                }
            }
            if (safePos != null) {
                break;
            }
        }

        // Absolute fallback: step 1 block behind (or north) on the floor, NEVER inside the seat block!
        if (safePos == null) {
            Direction fallbackDir = (facing != null) ? facing.getOpposite() : Direction.NORTH;
            BlockPos fallbackPos = seatPos.offset(fallbackDir);
            safePos = getDismountVec(world, fallbackPos);
        }

        return safePos;
    }

    private boolean isFullAvailableBlock(World world, BlockPos footPos, BlockPos seatPos) {
        // Strictly exclude the sitting block
        if (footPos.getX() == seatPos.getX() && footPos.getZ() == seatPos.getZ()) {
            return false;
        }

        if (SitUtil.isFurnitureBlock(world, footPos)) {
            return false;
        }

        BlockState footState = world.getBlockState(footPos);
        net.minecraft.util.shape.VoxelShape footShape = footState.getCollisionShape(world, footPos);
        // If foot level has high collision (blocks > 0.35m high), player cannot stand here
        if (!footShape.isEmpty() && footShape.getMax(Direction.Axis.Y) > 0.35) {
            return false;
        }

        if (SitUtil.isFurnitureBlock(world, footPos.up())) {
            return false;
        }

        BlockState headState = world.getBlockState(footPos.up());
        net.minecraft.util.shape.VoxelShape headShape = headState.getCollisionShape(world, footPos.up());
        if (!headShape.isEmpty()) {
            return false;
        }

        BlockPos groundPos = footPos.down();
        if (SitUtil.isFurnitureBlock(world, groundPos)) {
            return false;
        }

        BlockState groundState = world.getBlockState(groundPos);
        if (groundState.isAir()) {
            return false;
        }

        net.minecraft.util.shape.VoxelShape groundShape = groundState.getCollisionShape(world, groundPos);
        return !groundShape.isEmpty();
    }

    private Vec3d getDismountVec(World world, BlockPos footPos) {
        BlockState footState = world.getBlockState(footPos);
        net.minecraft.util.shape.VoxelShape footShape = footState.getCollisionShape(world, footPos);

        double floorY;
        if (!footShape.isEmpty()) {
            // Carpet, snow layer, etc. resting on the ground
            floorY = footPos.getY() + footShape.getMax(Direction.Axis.Y);
        } else {
            // Standing directly on the ground block (planks, cobble, slab)
            BlockPos groundPos = footPos.down();
            BlockState groundState = world.getBlockState(groundPos);
            net.minecraft.util.shape.VoxelShape groundShape = groundState.getCollisionShape(world, groundPos);
            if (!groundShape.isEmpty()) {
                floorY = groundPos.getY() + groundShape.getMax(Direction.Axis.Y);
            } else {
                floorY = footPos.getY();
            }
        }

        return new Vec3d(footPos.getX() + 0.5, floorY, footPos.getZ() + 0.5);
    }
}
