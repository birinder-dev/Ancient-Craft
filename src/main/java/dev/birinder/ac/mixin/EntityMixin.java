package dev.birinder.ac.mixin;

import dev.birinder.ac.entity.custom.SeatEntity;
import dev.birinder.ac.util.SitUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public abstract World getWorld();

    @Shadow
    public abstract boolean hasPassengers();


    /**
     * Cleans up any seats upon passenger dismount.
     * SeatEntity handles its own dismount position natively on both client and server
     * like boat/minecart. This ensures instant seat vacancy tracking.
     */
    @Inject(method = "removePassenger", at = @At("TAIL"))
    private void ancientCraft$onRemovePassenger(Entity passenger, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof SeatEntity seat) {
            SitUtil.vacateSeat(seat.getSeatSourcePos());
        } else if (self instanceof ArmorStandEntity stand && stand.getCommandTags().contains("tavern_seat")) {
            // Backward compatibility cleanup for any legacy armor stand seats
            BlockPos seatPos = stand.getBlockPos();
            SitUtil.vacateSeat(seatPos);
            if (!this.hasPassengers() && !this.getWorld().isClient()) {
                stand.discard();
            }
        }
    }
}
