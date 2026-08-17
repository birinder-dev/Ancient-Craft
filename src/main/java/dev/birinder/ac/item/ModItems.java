package dev.birinder.ac.item;

import dev.birinder.ac.AncientCraft;
import dev.birinder.ac.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
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

        private static Item register(String name, Item item) {
                net.minecraft.util.Identifier id = AncientCraft.id(name);
                if (Registries.ITEM.containsId(id)) {
                        return Registries.ITEM.get(id);
                }
                return Registry.register(
                                Registries.ITEM,
                                id,
                                item);
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
        }
}
