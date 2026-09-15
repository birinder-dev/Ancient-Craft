package dev.birinder.ac.client.mixin;

import dev.birinder.ac.client.gambling.GamblingClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders the fanned playing cards in first-person 3D handheld space,
 * while suppressing normal held weapons/tools during Gambling Mode.
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private void renderArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Arm arm) {
    }

    private static final Identifier CARD_TEXTURE = Identifier.of("ancient_craft", "textures/item/tavern_card.png");

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"), cancellable = true)
    private void ancientCraft$renderHandheldCards(
            float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
            ClientPlayerEntity player, int light, CallbackInfo ci) {
        if (GamblingClientState.isActive()) {
            ancientCraft$render3DCardHand(matrices, vertexConsumers, light, tickDelta, player);
            ci.cancel();
        }
    }

    private void ancientCraft$render3DCardHand(
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            float tickDelta, ClientPlayerEntity player) {

        int count = GamblingClientState.getCardCount();
        if (count <= 0) {
            return;
        }

        // Camera pitch compensation
        float pitch = player.getPitch(tickDelta);
        float pitchFactor = MathHelper.clamp(pitch / 90.0F, -1.0F, 1.0F);

        // 1. RENDER PLAYER ARMS:
        // Keep the exact arm pose the user praised ("the arms are cool")
        if (!player.isInvisible()) {
            matrices.push();
            matrices.translate(0.0F, -0.32F + pitchFactor * -0.06F, -0.46F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(64.0F));
            matrices.translate(0.0F, -0.08F, 0.16F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
            matrices.scale(0.85F, 0.85F, 0.85F);
            this.renderArm(matrices, vertexConsumers, light, Arm.RIGHT);
            this.renderArm(matrices, vertexConsumers, light, Arm.LEFT);
            matrices.pop();
        }

        // 2. RENDER THE CARD DECK:
        // Position cards held right at the hands in lower center view.
        // Base translation: chest height (Y = -0.36), slightly in front (Z = -0.48).
        // A gentle 14° backward tilt points the cards directly into the player's eyes!
        matrices.push();
        matrices.translate(0.0F, -0.36F + pitchFactor * -0.05F, -0.48F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(14.0F));

        // 3D Card physical dimensions: prominent and readable
        float cardW = 0.26F; // Width in meters (~26 cm)
        float cardH = 0.38F; // Height in meters (~38 cm)

        // Dynamic spacing and fan angle based on total cards
        float angleStep;
        float cardSpacing;

        if (count == 1) {
            angleStep = 0.0F;
            cardSpacing = 0.0F;
        } else if (count == 2) {
            angleStep = 8.0F;
            cardSpacing = 0.085F;
        } else if (count <= 5) {
            angleStep = 6.0F;
            cardSpacing = 0.070F;
        } else {
            angleStep = Math.max(3.2F, 24.0F / count);
            cardSpacing = Math.max(0.038F, 0.065F - (count - 5) * 0.005F);
        }

        int selectedSlot = player.getInventory().selectedSlot;

        // Ensure comfortable ambient lighting even in dimly lit taverns
        int blockLight = Math.max(12, light & 0xFFFF);
        int skyLight = (light >> 16) & 0xFFFF;
        int cardLight = (skyLight << 16) | blockLight;

        for (int i = 0; i < count; i++) {
            // Symmetrical offset t from center (e.g. count=2: -0.5, +0.5; count=5:
            // -2,-1,0,1,2)
            float t = (float) (i - (count - 1) / 2.0);

            float cardX = t * cardSpacing;
            // Fan divergence: Left cards (t < 0) tilt left (+Z rot), Right cards (t > 0)
            // tilt right (-Z rot)
            float angle = -t * angleStep;

            // Circular arc: center card is highest, outer cards curve down slightly
            float arcY = -(t * t) * 0.004F;
            // Layer depth: successive cards overlap cleanly from left to right to prevent
            // Z-fighting
            float arcZ = -i * 0.002F;

            float cardPivotY = arcY;

            // Hotbar selection feedback: selected card lifts up and steps forward towards
            // the player!
            boolean isSelected = (i == selectedSlot);
            if (isSelected) {
                cardPivotY += 0.055F; // Lifts up
                arcZ += 0.020F; // Steps forward towards player
            }

            matrices.push();

            // 1. Move to the bottom-center pivot point of this card
            matrices.translate(cardX, cardPivotY, arcZ);

            // 2. Rotate around Z axis (fan divergence: bottoms overlap at hands, tops
            // spread out)
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));

            // 3. Render the textured double-sided 3D card quad
            ancientCraft$renderCardQuad(matrices, vertexConsumers, cardLight, cardW, cardH, isSelected);

            matrices.pop();
        }

        matrices.pop();
    }

    private void ancientCraft$renderCardQuad(
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            float cardW, float cardH, boolean isSelected) {

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(CARD_TEXTURE));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f pos = entry.getPositionMatrix();

        float halfW = cardW / 2.0F;

        // If selected, tint slightly brighter/golden
        int r = isSelected ? 255 : 240;
        int g = isSelected ? 245 : 240;
        int b = isSelected ? 210 : 240;

        // Front Face (facing directly into the player's eyes):
        // Winding: Bottom-Left -> Bottom-Right -> Top-Right -> Top-Left
        // (Counter-Clockwise)
        buffer.vertex(pos, -halfW, 0.0F, 0.0F).color(r, g, b, 255).texture(0.0F, 1.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0.0F, 0.0F, 1.0F);
        buffer.vertex(pos, halfW, 0.0F, 0.0F).color(r, g, b, 255).texture(1.0F, 1.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0.0F, 0.0F, 1.0F);
        buffer.vertex(pos, halfW, cardH, 0.0F).color(r, g, b, 255).texture(1.0F, 0.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0.0F, 0.0F, 1.0F);
        buffer.vertex(pos, -halfW, cardH, 0.0F).color(r, g, b, 255).texture(0.0F, 0.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0.0F, 0.0F, 1.0F);

        // Back Face (facing away into the world):
        buffer.vertex(pos, halfW, 0.0F, -0.001F).color(r, g, b, 255).texture(1.0F, 1.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0.0F, 0.0F, -1.0F);
        buffer.vertex(pos, -halfW, 0.0F, -0.001F).color(r, g, b, 255).texture(0.0F, 1.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0.0F, 0.0F, -1.0F);
        buffer.vertex(pos, -halfW, cardH, -0.001F).color(r, g, b, 255).texture(0.0F, 0.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0.0F, 0.0F, -1.0F);
        buffer.vertex(pos, halfW, cardH, -0.001F).color(r, g, b, 255).texture(1.0F, 0.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0.0F, 0.0F, -1.0F);
    }
}
