package dev.birinder.ac.block.entity;

import dev.birinder.ac.AncientCraft;
import dev.birinder.ac.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static final BlockEntityType<GamblingTableBlockEntity> GAMBLING_TABLE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            AncientCraft.id("gambling_table_block_entity"),
            BlockEntityType.Builder.create(GamblingTableBlockEntity::new, ModBlocks.GAMBLING_TABLE).build(null)
    );

    public static void registerBlockEntities() {
        AncientCraft.LOGGER.info("Registering Ancient Craft Block Entities");
    }
}
