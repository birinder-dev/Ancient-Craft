package dev.birinder.ac.client.mixin;

import dev.birinder.ac.client.util.SpyglassZoomUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void ac$spyglassCustomFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.isUsingItem() && client.player.getActiveItem().isOf(Items.SPYGLASS)) {
            double currentFov = cir.getReturnValueD();
            float zoomFactor = SpyglassZoomUtil.getZoomFactor();
            cir.setReturnValue(currentFov / (double) zoomFactor);
        }
    }
}
