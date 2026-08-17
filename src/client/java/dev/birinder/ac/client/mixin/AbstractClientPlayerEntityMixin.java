package dev.birinder.ac.client.mixin;

import dev.birinder.ac.client.util.SpyglassZoomUtil;
import dev.birinder.ac.item.ModItems;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {

    @Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true)
    private void ancientCraft$slingshotFovMultiplier(CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;

        // 1. Check if player is holding and using the Slingshot
        if (player.isUsingItem() && player.getActiveItem().isOf(ModItems.ANCIENT_TOOL)) {

            // 2. getItemUseTime() returns how many ticks right-click has been held
            int i = player.getItemUseTime();

            // 3. Convert ticks to 0.0 -> 1.0 progress (20 ticks = 1 sec full charge)
            float f = (float) i / 20.0F;

            if (f > 1.0F) {
                f = 1.0F; // Lock progress at 100% when fully charged
            } else {
                f = f * f; // Quadratic curve (Exact same formula as Vanilla Bow)
            }

            // 4. Multiply current FOV by (1.0F - f * 0.15F) - Exact Vanilla Bow intensity!
            float currentFov = cir.getReturnValueF();
            cir.setReturnValue(currentFov * (1.0F - f * 0.15F));
        }
    }
}
