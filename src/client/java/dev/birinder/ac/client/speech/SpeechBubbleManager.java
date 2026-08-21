package dev.birinder.ac.client.speech;

import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpeechBubbleManager {

    private static final Map<Integer, SpeechBubble> activeBubbles = new ConcurrentHashMap<>();
    public static final int DEFAULT_DURATION_TICKS = 75; // 3.75 seconds

    public static boolean say(LivingEntity entity, String message, SoundEvent voiceSound) {
        if (entity == null || message == null || message.trim().isEmpty()) {
            return false;
        }

        // DEBOUNCE / STATE GUARD: Let the current line finish completely!
        SpeechBubble existing = activeBubbles.get(entity.getId());
        if (existing != null && !existing.isExpired()) {
            return false;
        }

        SpeechBubble bubble = new SpeechBubble(entity.getId(), message.trim(), DEFAULT_DURATION_TICKS);
        activeBubbles.put(entity.getId(), bubble);

        // Play the studio voice pack with 3D Spatial Audio & UUID Pitch
        if (voiceSound != null) {
            SpeechAudioHelper.playVoiceLine(entity, voiceSound);
        }
        return true;
    }

    public static SpeechBubble getBubble(int entityId) {
        return activeBubbles.get(entityId);
    }

    public static void tick() {
        Iterator<Map.Entry<Integer, SpeechBubble>> it = activeBubbles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, SpeechBubble> entry = it.next();
            SpeechBubble bubble = entry.getValue();
            bubble.tick();
            if (bubble.isExpired()) {
                it.remove();
            }
        }
    }

    public static void clear() {
        activeBubbles.clear();
    }
}
