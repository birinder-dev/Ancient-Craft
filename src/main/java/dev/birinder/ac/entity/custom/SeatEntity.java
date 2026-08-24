package dev.birinder.ac.entity.custom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SeatEntity extends Entity {

    public SeatEntity(EntityType<? extends SeatEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setInvisible(true);
        this.setNoGravity(true); // CRITICAL: Prevents seat from falling through the floor!
    }

    public SeatEntity(EntityType<? extends SeatEntity> type, World world, BlockPos pos, double yOffset, float yaw) {
        this(type, world);
        this.setPosition(pos.getX() + 0.5, pos.getY() + yOffset, pos.getZ() + 0.5);
        this.setVelocity(Vec3d.ZERO);
        this.setYaw(yaw);
        this.setBodyYaw(yaw);
        this.setHeadYaw(yaw);
    }

    @Override
    public void tick() {
        super.tick();
        this.setVelocity(Vec3d.ZERO);
        if (!this.getWorld().isClient()) {
            // Clean up when passenger stands up (with grace period on initial spawn)
            if (this.age > 30 && !this.hasPassengers()) {
                this.discard();
            }
        }
    }

    @Override
    protected Vec3d getPassengerAttachmentPos(Entity passenger, net.minecraft.entity.EntityDimensions dimensions, float scaleFactor) {
        return new Vec3d(0.0, 0.0, 0.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}
