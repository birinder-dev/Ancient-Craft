package dev.birinder.ac.item;

import dev.birinder.ac.AncientCraft;
import dev.birinder.ac.block.ModBlocks;
import dev.birinder.ac.entity.ModEntities;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModItems {

    public static final Item ANCIENT_TOOL = register(
            "ancient_tool",
            new AncientToolItem(new Item.Settings().maxDamage(250)));

    public static final Item PEBBLE = register(
            "pebble",
            new PebbleItem(new Item.Settings().maxCount(64)));

    public static final Item GROUND_PEBBLE = register(
            "ground_pebble",
            new BlockItem(ModBlocks.GROUND_PEBBLE, new Item.Settings()));

    public static final Item POISON_PEBBLE = register(
            "poison_pebble",
            new PebbleItem(new Item.Settings().maxCount(64)));

    // Tavern & Gambling Furniture
    public static final Item GAMBLING_TABLE = register(
            "gambling_table",
            new BlockItem(ModBlocks.GAMBLING_TABLE, new Item.Settings()));

    public static final Item GAMBLING_STOOL = register(
            "gambling_stool",
            new BlockItem(ModBlocks.GAMBLING_STOOL, new Item.Settings()));

    public static final Item TAVERN_STOOL = register(
            "tavern_stool",
            new BlockItem(ModBlocks.TAVERN_STOOL, new Item.Settings()));

    // Medieval Tavern Feast Table
    public static final Item TAVERN_TABLE = register(
            "tavern_table",
            new BlockItem(ModBlocks.TAVERN_TABLE, new Item.Settings()));

    // Thick KCD Medieval Bench
    public static final Item TAVERN_BENCH = register(
            "tavern_bench",
            new BlockItem(ModBlocks.TAVERN_BENCH, new Item.Settings()));

    // The Gambler Spawn Egg
    public static final Item GAMBLER_SPAWN_EGG = register(
            "gambler_spawn_egg",
            new SpawnEggItem(ModEntities.GAMBLER, 0x1E4D2B, 0xD4AF37, new Item.Settings()));

    private static Item register(String name, Item item) {
        net.minecraft.util.Identifier id = AncientCraft.id(name);
        if (Registries.ITEM.containsId(id)) {
            return Registries.ITEM.get(id);
        }
        return Registry.register(Registries.ITEM, id, item);
    }

    public static void registerModItems() {
        AncientCraft.LOGGER.info("Registering Ancient Craft items");

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
                .register(entries -> {
                    entries.add(ANCIENT_TOOL);
                    entries.add(PEBBLE);
                    entries.add(GROUND_PEBBLE);
                    entries.add(POISON_PEBBLE);
                });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
                .register(entries -> {
                    entries.add(GAMBLING_TABLE);
                    entries.add(GAMBLING_STOOL);
                    entries.add(TAVERN_STOOL);
                    entries.add(TAVERN_TABLE);
                    entries.add(TAVERN_BENCH);
                });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS)
                .register(entries -> {
                    entries.add(GAMBLER_SPAWN_EGG);
                });
    }
}
