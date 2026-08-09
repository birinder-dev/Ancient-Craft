package dev.birinder.ac.client.mixin;

import dev.birinder.ac.item.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(
            method = "getFov",
            at = @At("RETURN"),
            cancellable = true
    )
    private void ancientCraft$scannerZoom(
            Camera camera,
            float tickDelta,
            boolean changingFov,
            CallbackInfoReturnable<Double> cir
    ) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            return;
        }

        ItemStack stack = client.player.getActiveItem();

        if (!stack.isOf(ModItems.ANCIENT_TOOL)) {
            return;
        }

        if (!client.player.isUsingItem()) {
            return;
        }

        double currentFov = cir.getReturnValue();

        cir.setReturnValue(currentFov * 0.65);
    }
}