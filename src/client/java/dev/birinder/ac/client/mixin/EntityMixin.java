package dev.birinder.ac.client.mixin;

import dev.birinder.ac.client.util.SpyglassTargetUtil;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void ac$spyglassOutlineColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (SpyglassTargetUtil.isCurrentTarget(self)) {
            cir.setReturnValue(SpyglassTargetUtil.getTargetColor(self));
        }
    }
}
