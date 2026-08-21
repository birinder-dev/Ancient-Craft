package dev.birinder.ac.client.mixin;

import dev.birinder.ac.client.gui.SpyglassHudOverlay;
import dev.birinder.ac.client.speech.SpeechBubble;
import dev.birinder.ac.client.speech.SpeechBubbleManager;
import dev.birinder.ac.client.speech.SpeechBubbleRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> {

    protected LivingEntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("RETURN"))
    private void ancientCraft$renderLivingSpeechBubble(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (SpyglassHudOverlay.isRenderingPortrait) {
            return;
        }

        SpeechBubble bubble = SpeechBubbleManager.getBubble(entity.getId());
        if (bubble != null) {
            SpeechBubbleRenderer.renderSpeechBubble(
                    entity,
                    bubble,
                    matrices,
                    vertexConsumers,
                    this.dispatcher,
                    this.getTextRenderer(),
                    light
            );
        }
    }
}
