package dev.birinder.ac.client.speech;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

import java.util.UUID;

public class SpeechAudioHelper {

    public static void playVoiceLine(LivingEntity entity, SoundEvent voiceSound) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || entity == null || voiceSound == null) {
            return;
        }

        UUID uuid = entity.getUuid();
        // Dynamic pitch modulation per villager: 0.85x (deep) to 1.15x (higher pitch)
        float pitch = 0.85F + (Math.abs(uuid.hashCode() % 100) / 330.0F);

        // Play 3D Spatial Directional Audio directly at NPC head position
        client.getSoundManager().play(
                new PositionedSoundInstance(
                        voiceSound,
                        SoundCategory.PLAYERS,
                        1.2F,
                        pitch,
                        Random.create(),
                        entity.getX(),
                        entity.getY() + entity.getStandingEyeHeight(),
                        entity.getZ()
                )
        );
    }
}
