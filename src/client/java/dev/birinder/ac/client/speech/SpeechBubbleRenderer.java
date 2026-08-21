package dev.birinder.ac.client.speech;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

import java.util.List;

public class SpeechBubbleRenderer {

    public static void renderSpeechBubble(
            Entity entity,
            SpeechBubble bubble,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            EntityRenderDispatcher dispatcher,
            TextRenderer textRenderer,
            int light
    ) {
        if (bubble == null || bubble.isExpired() || textRenderer == null) {
            return;
        }

        List<String> lines = bubble.getLines();
        if (lines.isEmpty()) {
            return;
        }

        matrices.push();

        // 1. Position directly above entity head
        float heightOffset = entity.getHeight() + 0.55F;
        matrices.translate(0.0D, heightOffset, 0.0D);

        // 2. Camera Billboarding (face player camera)
        matrices.multiply(dispatcher.getRotation());
        // Standard vanilla nametag matrix scale: +X, -Y, +Z
        matrices.scale(0.025F, -0.025F, 0.025F);

        int lineHeight = textRenderer.fontHeight + 2;
        int totalHeight = lines.size() * lineHeight;
        int startY = -totalHeight;

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // 3. Render pure crisp text with shadow and 100% transparent background (no black box)
        int currentY = startY;
        for (String line : lines) {
            Text text = Text.literal(line);
            int lineW = textRenderer.getWidth(line);
            float lineX = -lineW / 2.0F;

            textRenderer.draw(
                    text,
                    lineX,
                    (float) currentY,
                    0xFFFFFFFF,
                    true, // Crisp shadow
                    matrix,
                    vertexConsumers,
                    TextRenderer.TextLayerType.NORMAL,
                    0, // 100% transparent background
                    light
            );

            currentY += lineHeight;
        }

        matrices.pop();
    }
}
