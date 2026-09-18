package dev.birinder.ac.client.mixin;

import dev.birinder.ac.client.gambling.GamblingClientState;
import dev.birinder.ac.client.util.SpyglassTargetUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    public ClientPlayerEntity player;

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void ac$spyglassOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (this.player != null && this.player.isUsingItem() && this.player.getActiveItem().isOf(Items.SPYGLASS)) {
            if (SpyglassTargetUtil.isCurrentTarget(entity)) {
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * Lock weapon/tool attacks while in Gambling Mode to protect weapon durability
     * and prevent accidental swinging.
     */
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void ancientCraft$lockAttackInGambling(CallbackInfoReturnable<Boolean> cir) {
        if (GamblingClientState.isActive()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Lock item usage (eating food, drinking potions, placing blocks, throwing pearls)
     * while in Gambling Mode.
     */
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void ancientCraft$lockItemUseInGambling(CallbackInfo ci) {
        if (GamblingClientState.isActive()) {
            ci.cancel();
        }
    }

    /**
     * Lock block breaking while in Gambling Mode.
     */
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void ancientCraft$lockBlockBreakingInGambling(boolean breaking, CallbackInfo ci) {
        if (breaking && GamblingClientState.isActive()) {
            ci.cancel();
        }
    }
}

