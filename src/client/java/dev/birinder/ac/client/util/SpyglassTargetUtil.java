package dev.birinder.ac.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;

public class SpyglassTargetUtil {

    private static LivingEntity currentTarget = null;
    private static long lastUpdateTime = 0;

    public static LivingEntity getTargetedLivingEntity(MinecraftClient client, float maxDistance) {
        if (client.player == null || client.world == null) {
            currentTarget = null;
            return null;
        }

        if (!client.player.isUsingItem() || !client.player.getActiveItem().isOf(Items.SPYGLASS)) {
            currentTarget = null;
            return null;
        }

        long now = System.currentTimeMillis();
        if (now - lastUpdateTime < 15 && currentTarget != null && currentTarget.isAlive()) {
            return currentTarget;
        }
        lastUpdateTime = now;

        Entity camera = client.getCameraEntity();
        if (camera == null) {
            camera = client.player;
        }

        Vec3d cameraPos = camera.getCameraPosVec(1.0F);
        Vec3d rotationVec = camera.getRotationVec(1.0F);
        Vec3d endPos = cameraPos.add(rotationVec.x * maxDistance, rotationVec.y * maxDistance, rotationVec.z * maxDistance);

        Box box = camera.getBoundingBox().stretch(rotationVec.multiply(maxDistance)).expand(1.0D, 1.0D, 1.0D);

        EntityHitResult entityHitResult = ProjectileUtil.raycast(
                camera,
                cameraPos,
                endPos,
                box,
                entity -> !entity.isSpectator() && entity.canHit() && entity instanceof LivingEntity,
                maxDistance * maxDistance
        );

        if (entityHitResult != null && entityHitResult.getEntity() instanceof LivingEntity living) {
            currentTarget = living;
            return living;
        }

        currentTarget = null;
        return null;
    }

    public static boolean isCurrentTarget(Entity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !client.player.isUsingItem() || !client.player.getActiveItem().isOf(Items.SPYGLASS)) {
            return false;
        }
        return currentTarget != null && currentTarget == entity;
    }

    public static int getTargetColor(Entity entity) {
        // Tamed Animals (Dogs, Cats, Horses, Parrots) -> Pink!
        if (entity instanceof TameableEntity tameable && tameable.isTamed()) {
            return 0xFF69B4; // Vibrant Pink (Tamed Pet)
        }
        if (entity instanceof AbstractHorseEntity horse && horse.isTame()) {
            return 0xFF69B4; // Vibrant Pink (Tamed Mount)
        }

        // Red = Aggressive (Monster or Angerable mob that is currently angry)
        if (entity instanceof Angerable angerable && angerable.hasAngerTime()) {
            return 0xFF2222; // Vibrant Red
        }
        if (entity instanceof Monster) {
            return 0xFF2222; // Vibrant Red
        }

        // White = Neutral (Passive until attacked: Wolf, Bee, Iron Golem, Spider in daytime, Piglin, Enderman)
        if (entity instanceof Angerable || entity instanceof SpiderEntity) {
            return 0xFFFFFF; // Pure White
        }

        // Green = Passive (Pigs, Cows, Sheep, Chickens, Horses, Villagers)
        return 0x33FF44; // Vibrant Green
    }
}
