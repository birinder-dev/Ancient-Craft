package dev.birinder.ac.client.mixin;

import dev.birinder.ac.client.gambling.GamblingClientState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into InGameHud to suppress regular hotbar item icons (swords, tools, etc.)
 * while in Gambling Mode, so the hotbar slots act as clean card number selectors.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderHotbarItem", at = @At("HEAD"), cancellable = true)
    private void ancientCraft$hideHotbarItemsInGamblingState(
            DrawContext context, int x, int y, RenderTickCounter tickCounter,
            PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        if (GamblingClientState.isActive()) {
            ci.cancel();
        }
    }
}
