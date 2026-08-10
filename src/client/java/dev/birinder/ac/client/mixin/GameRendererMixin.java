package dev.birinder.ac.client.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    // Cleaned up: Zoom logic moved to AbstractClientPlayerEntityMixin for native
    // Minecraft FOV smoothing!
}
