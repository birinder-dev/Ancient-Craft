package dev.birinder.ac.util;

import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SitUtil {

    public static boolean sitPlayer(World world, BlockPos pos, PlayerEntity player, double yOffset) {
        if (!world.isClient() && !player.hasVehicle()) {
            ArmorStandEntity seat = new ArmorStandEntity(world, pos.getX() + 0.5, pos.getY() + yOffset,
                    pos.getZ() + 0.5) {
                @Override
                public void tick() {
                    super.tick();
                    if (!this.getWorld().isClient()) {
                        if (!this.hasPassengers()) {
                            this.discard();
                        }
                    }
                }
            };
            seat.setInvisible(true);
            seat.setNoGravity(true);

            world.spawnEntity(seat);
            player.startRiding(seat, true);
            return true;
        }
        return false;
    }
}