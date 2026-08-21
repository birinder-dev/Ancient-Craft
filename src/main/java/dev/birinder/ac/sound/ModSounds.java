package dev.birinder.ac.sound;

import dev.birinder.ac.AncientCraft;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    public static final SoundEvent VOICE_HURT_1 = register("voice.v0.hurt_1");
    public static final SoundEvent VOICE_HURT_2 = register("voice.v0.hurt_2");
    public static final SoundEvent VOICE_HURT_3 = register("voice.v0.hurt_3");

    public static final SoundEvent VOICE_THREAT_1 = register("voice.v0.threat_1");
    public static final SoundEvent VOICE_THREAT_2 = register("voice.v0.threat_2");
    public static final SoundEvent VOICE_THREAT_3 = register("voice.v0.threat_3");

    public static final SoundEvent VOICE_GREET_1 = register("voice.v0.greet_1");
    public static final SoundEvent VOICE_GREET_2 = register("voice.v0.greet_2");

    public static final SoundEvent[] HURT_VOICES = { VOICE_HURT_1, VOICE_HURT_2, VOICE_HURT_3 };
    public static final SoundEvent[] THREAT_VOICES = { VOICE_THREAT_1, VOICE_THREAT_2, VOICE_THREAT_3 };
    public static final SoundEvent[] GREET_VOICES = { VOICE_GREET_1, VOICE_GREET_2 };

    private static SoundEvent register(String name) {
        Identifier id = Identifier.of(AncientCraft.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void initialize() {
        // Static init
    }
}
