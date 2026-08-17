package dev.birinder.ac.client.gui;

import dev.birinder.ac.client.util.SpyglassTargetUtil;
import dev.birinder.ac.client.util.SpyglassZoomUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class SpyglassHudOverlay implements HudRenderCallback {

    public static boolean isRenderingPortrait = false;

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        // Only render HUD card when actively scoping through Spyglass
        if (!client.player.isUsingItem() || !client.player.getActiveItem().isOf(Items.SPYGLASS)) {
            return;
        }

        LivingEntity target = SpyglassTargetUtil.getTargetedLivingEntity(client, 64.0F);
        if (target == null) {
            return;
        }

        int screenWidth = drawContext.getScaledWindowWidth();
        int cardWidth = 195;
        int cardX = (screenWidth - cardWidth) / 2;
        int cardY = 4; // Shifted up near top border

        // Portrait Box dimensions on the left (Ultra-compact 34x34 face frame)
        int portraitBoxSize = 34;
        int portraitX = cardX + 4;
        int portraitY = cardY + 4;

        List<Text> lines = new ArrayList<>();

        // 1. Entity Name & Classification
        String nameStr = target.getDisplayName().getString();
        Formatting nameColor = Formatting.YELLOW;
        if (target instanceof PlayerEntity) {
            nameColor = Formatting.AQUA;
        } else if (target instanceof AbstractHorseEntity) {
            nameColor = Formatting.GOLD;
        } else if (!target.getType().isSaveable()) {
            nameColor = Formatting.WHITE;
        }
        lines.add(Text.literal("◆ " + nameStr).formatted(nameColor, Formatting.BOLD));

        // 2. Health & Effective Health (EHP) with Armor Calculation
        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        int armor = target.getArmor();
        float damageReduction = Math.min(0.80F, armor * 0.04F);
        float effectiveHealth = damageReduction < 1.0F ? health / (1.0F - damageReduction) : health;

        String hpStr = String.format("HP: %.1f/%.1f", health, maxHealth);
        Formatting hpColor = (health / maxHealth > 0.5F) ? Formatting.GREEN : (health / maxHealth > 0.25F ? Formatting.YELLOW : Formatting.RED);

        if (armor > 0) {
            lines.add(Text.literal("♥ " + hpStr + String.format(" (EHP: %.0f)", effectiveHealth)).formatted(hpColor));
            lines.add(Text.literal(String.format("🛡 Armor: %d (%.0f%%)", armor, damageReduction * 100.0F)).formatted(Formatting.AQUA));
        } else {
            lines.add(Text.literal("♥ " + hpStr).formatted(hpColor));
        }

        // 3. Mount Stats (Combined on one compact line with distinct colors and 'blocks'!)
        if (target instanceof AbstractHorseEntity horse) {
            EntityAttributeInstance speedAttr = horse.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            double speed = speedAttr != null ? speedAttr.getValue() * 43.17D : 0.0D;
            double jumpStrength = horse.getAttributeValue(EntityAttributes.GENERIC_JUMP_STRENGTH);
            double jumpHeight = -0.18D + 3.0D * jumpStrength + 4.0D * jumpStrength * jumpStrength;

            Text mountText = Text.literal("🐎 ")
                    .formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal(String.format("%.2f m/s  ", speed)).formatted(Formatting.LIGHT_PURPLE))
                    .append(Text.literal(String.format("▲ %.2f blocks", jumpHeight)).formatted(Formatting.GOLD));

            lines.add(mountText);
        }

        // 4. Taming Status (Pink for tamed!)
        if (target instanceof TameableEntity tameable) {
            if (tameable.isTamed()) {
                LivingEntity owner = tameable.getOwner();
                String ownerName = owner != null ? owner.getName().getString() : "Tamed";
                lines.add(Text.literal("★ Tamed: " + ownerName).formatted(Formatting.LIGHT_PURPLE));
            } else {
                lines.add(Text.literal("☆ Wild").formatted(Formatting.GRAY));
            }
        }

        // 5. Active Potion Effects
        for (StatusEffectInstance effect : target.getStatusEffects()) {
            String effectName = effect.getEffectType().value().getName().getString();
            int durationSecs = effect.getDuration() / 20;
            lines.add(Text.literal(String.format("✦ %s (%ds)", effectName, durationSecs)).formatted(Formatting.DARK_AQUA));
        }

        // 6. Current Zoom Level
        lines.add(Text.literal(String.format("🔍 Zoom: %.1fx", SpyglassZoomUtil.getZoomFactor())).formatted(Formatting.GRAY));

        // Calculate card height dynamically (Comfortable 9px spacing to prevent text congestion)
        int lineHeight = 9;
        int textSectionHeight = lines.size() * lineHeight;
        int cardHeight = Math.max(portraitBoxSize + 7, textSectionHeight + 7);

        // Render Semi-Transparent Dark Card Background & Gold Border
        drawContext.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, 0xDD101015);
        drawContext.drawBorder(cardX, cardY, cardWidth, cardHeight, 0xFFDAA520);

        // Render 3D Portrait Frame
        drawContext.fill(portraitX, portraitY, portraitX + portraitBoxSize, portraitY + portraitBoxSize, 0xBB1A1A22);
        drawContext.drawBorder(portraitX, portraitY, portraitBoxSize, portraitBoxSize, 0xFF888888);

        // Render Pure Matrix Eye-Anchored 3D Entity (Zero world mob stutter & universal face centering!)
        try {
            isRenderingPortrait = true;
            drawContext.enableScissor(portraitX + 1, portraitY + 1, portraitX + portraitBoxSize - 1, portraitY + portraitBoxSize - 1);

            float entityWidth = target.getWidth();
            float entityHeight = target.getHeight();
            float eyeHeight = target.getStandingEyeHeight();
            float maxDimension = Math.max(entityWidth, entityHeight);

            // Large wide mobs (Ghasts, Dragons, Elder Guardians, Giant Slimes) fit full body;
            // standard/tall biped mobs (Endermen, Players, Horses) zoom cleanly into face & eyes
            boolean isHugeMob = entityWidth > 1.8F || maxDimension > 3.5F;
            int renderSize;
            if (isHugeMob) {
                renderSize = Math.max(3, (int) (20.0F / maxDimension));
            } else {
                float headSpan = Math.max(0.55F, Math.max(entityWidth * 0.7F, Math.min(entityHeight * 0.35F, 1.2F)));
                renderSize = Math.max(8, Math.min(22, (int) (15.0F / headSpan)));
            }

            // Mathematically anchors the eyes/face dead-center in the portrait box
            int centerY = portraitY + portraitBoxSize / 2;
            int entityBottomY = centerY + (int) (eyeHeight * renderSize);

            // Small creatures & fish tilt slightly downwards from above (-16F pitch)
            float pitchAngle = (entityHeight < 0.6F || target instanceof net.minecraft.entity.passive.FishEntity) ? -16.0F : 0.0F;

            // Matrix-only rotation (+25F yaw makes them face right!)
            InventoryScreen.drawEntity(
                    drawContext,
                    portraitX + 2, portraitY + 2,
                    portraitX + portraitBoxSize - 2, entityBottomY,
                    renderSize,
                    0.0625F,
                    25.0F, pitchAngle,
                    target
            );

            drawContext.disableScissor();
        } catch (Throwable t) {
            drawContext.disableScissor();
            drawContext.drawText(client.textRenderer, Text.literal("?").formatted(Formatting.GOLD, Formatting.BOLD), portraitX + 14, portraitY + 12, 0xFFDAA520, false);
        } finally {
            isRenderingPortrait = false;
        }

        // Health Bar Visualization (Full Card Width at Bottom)
        int barX = cardX + 5;
        int barY = cardY + cardHeight - 4;
        int barWidth = cardWidth - 10;
        int healthFill = (int) (barWidth * Math.min(1.0F, Math.max(0.0F, health / maxHealth)));

        drawContext.fill(barX, barY, barX + barWidth, barY + 2, 0xFF441111);
        drawContext.fill(barX, barY, barX + healthFill, barY + 2, 0xFF00FF55);

        // If mob has armor, draw cyan armor segment overlay on top of health bar
        if (armor > 0) {
            int armorFill = (int) (barWidth * Math.min(1.0F, armor / 20.0F));
            drawContext.fill(barX, barY - 1, barX + armorFill, barY, 0xFF00DDFF);
        }

        // Render text lines to the right of the 3D portrait
        int textX = portraitX + portraitBoxSize + 5;
        int textY = cardY + 3;
        for (Text line : lines) {
            drawContext.drawText(client.textRenderer, line, textX, textY, 0xFFFFFFFF, false);
            textY += lineHeight;
        }
    }
}
