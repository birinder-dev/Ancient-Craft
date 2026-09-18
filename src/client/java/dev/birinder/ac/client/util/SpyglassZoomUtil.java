package dev.birinder.ac.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;

public class SpyglassZoomUtil {

    // Strictly 3 discrete zoom steps: 0.5x (wide), 1.0x (normal), 2.0x (deep zoom)
    private static final float[] ZOOM_STEPS = { 0.5F, 1.0F, 2.0F };
    private static int currentStepIndex = 1; // Default is 1.0x

    public static void onScroll(double verticalScroll) {
        if (verticalScroll > 0) {
            // Scroll Up -> Zoom In
            currentStepIndex = Math.min(ZOOM_STEPS.length - 1, currentStepIndex + 1);
        } else if (verticalScroll < 0) {
            // Scroll Down -> Zoom Out
            currentStepIndex = Math.max(0, currentStepIndex - 1);
        }
    }

    public static float getZoomFactor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !client.player.isUsingItem() || !client.player.getActiveItem().isOf(Items.SPYGLASS)) {
            currentStepIndex = 1; // Reset to 1.0x when not scoping
        }
        return ZOOM_STEPS[currentStepIndex];
    }
}

