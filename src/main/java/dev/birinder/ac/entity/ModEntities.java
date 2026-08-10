package dev.birinder.ac.entity;

import dev.birinder.ac.AncientCraft;
import dev.birinder.ac.entity.projectile.SlingshotProjectileEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModEntities {

    public static final EntityType<SlingshotProjectileEntity> SLINGSHOT_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            AncientCraft.id("slingshot_projectile"),
            EntityType.Builder.<SlingshotProjectileEntity>create(SlingshotProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25F, 0.25F)
                    .maxTrackingRange(4)
                    .trackingTickInterval(10)
                    .build("slingshot_projectile")
    );

    public static void registerModEntities() {
        AncientCraft.LOGGER.info("Registering Ancient Craft entities");
    }
}
