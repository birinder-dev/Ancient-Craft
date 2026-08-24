package dev.birinder.ac.mixin;

import dev.birinder.ac.entity.ai.VillagerTavernAI;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity {

    protected VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    // 1. YOUR EXISTING AI TICK LOOP HOOK
    @Inject(method = "mobTick", at = @At("TAIL"))
    private void ancientCraft$tickTavern(CallbackInfo ci) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            VillagerTavernAI.tick((VillagerEntity) (Object) this, serverWorld);
        }
    }

    // 2. FIXED CLEANUP VIA METHOD OVERRIDING
    // Instead of forcing an injection target up the superclass chain, 
    // we simply override the native stopRiding method.
    @Override
    public void stopRiding() {
        // Run our custom cleanup logic on the server-side before the entity is separated
        if (!this.getWorld().isClient()) {
            if (this.getVehicle() instanceof ArmorStandEntity seat) {
                if (seat.isInvisible()) {
                    seat.discard(); // Deletes the invisible armor stand from the world
                }
            }
        }
        
        // CRITICAL: Call super.stopRiding() so Minecraft's native dismounting execution still happens!
        super.stopRiding();
    }
}

