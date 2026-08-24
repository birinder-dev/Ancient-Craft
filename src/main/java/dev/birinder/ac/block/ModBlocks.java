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

    public static final Block GAMBLING_TABLE = register(
            "gambling_table",
            new GamblingTableBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque().strength(2.0F)));

    public static final Block GAMBLING_STOOL = register(
            "gambling_stool",
            new GamblingStoolBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque().strength(1.5F)));

    public static final Block TAVERN_STOOL = register(
            "tavern_stool",
            new TavernStoolBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque().strength(1.5F)));

    // Medieval Tavern Feast Table (Omni-directional boundary joining, zero leg rest clutter)
    public static final Block TAVERN_TABLE = register(
            "tavern_table",
            new TavernTableBlock(AbstractBlock.Settings.copy(Blocks.SPRUCE_PLANKS).nonOpaque().strength(2.0F)));

    // Thick KCD Medieval Bench
    public static final Block TAVERN_BENCH = register(
            "tavern_bench",
            new TavernBenchBlock(AbstractBlock.Settings.copy(Blocks.SPRUCE_PLANKS).nonOpaque().strength(1.5F)));

    private static Block register(String name, Block block) {
        net.minecraft.util.Identifier id = AncientCraft.id(name);
        if (Registries.BLOCK.containsId(id)) {
            return Registries.BLOCK.get(id);
        }
        return Registry.register(
                Registries.BLOCK,
                id,
                block);
    }

    public static void registerModBlocks() {
        AncientCraft.LOGGER.info("Registering Ancient Craft blocks");
    }
}
