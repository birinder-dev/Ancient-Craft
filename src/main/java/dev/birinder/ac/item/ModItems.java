package dev.birinder.ac.item;

import dev.birinder.ac.AncientCraft;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModItems {

    public static final Item ANCIENT_TOOL = register(
            "ancient_tool",
            new AncientToolItem(new Item.Settings().maxCount(1))
    );

    private static Item register(String name, Item item) {
        return Registry.register(
                Registries.ITEM,
                AncientCraft.id(name),
                item
        );
    }

    public static void registerModItems() {
        AncientCraft.LOGGER.info("Registering Ancient Craft items");

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
                .register(entries -> entries.add(ANCIENT_TOOL));
    }
}