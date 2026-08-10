package dev.birinder.ac.block;

import dev.birinder.ac.AncientCraft;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModBlocks {

    public static final Block GROUND_PEBBLE = register(
            "ground_pebble",
            new PebbleBlock(AbstractBlock.Settings.copy(Blocks.STONE).nonOpaque().breakInstantly()));

    private static Block register(String name, Block block) {
        return Registry.register(
                Registries.BLOCK,
                AncientCraft.id(name),
                block);
    }

    public static void registerModBlocks() {
        AncientCraft.LOGGER.info("Registering Ancient Craft blocks");
    }
}
