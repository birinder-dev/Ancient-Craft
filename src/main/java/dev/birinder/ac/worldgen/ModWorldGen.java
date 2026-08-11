package dev.birinder.ac.worldgen;

import dev.birinder.ac.AncientCraft;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

public class ModWorldGen {

    public static final RegistryKey<PlacedFeature> GROUND_PEBBLE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, AncientCraft.id("ground_pebble"));

    public static void generateWorldGen() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.TOP_LAYER_MODIFICATION,
                GROUND_PEBBLE_PLACED_KEY
        );
    }
}
