package dev.birinder.ac.entity;

import dev.birinder.ac.AncientCraft;
import dev.birinder.ac.entity.custom.GamblerEntity;
import dev.birinder.ac.entity.custom.SeatEntity;
import dev.birinder.ac.entity.projectile.SlingshotProjectileEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModEntities {

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> type) {
        net.minecraft.util.Identifier id = AncientCraft.id(name);
        if (Registries.ENTITY_TYPE.containsId(id)) {
            @SuppressWarnings("unchecked")
            EntityType<T> existing = (EntityType<T>) Registries.ENTITY_TYPE.get(id);
            return existing;
        }
        return Registry.register(Registries.ENTITY_TYPE, id, type.build(name));
    }

    public static final EntityType<SlingshotProjectileEntity> SLINGSHOT_PROJECTILE = register(
            "slingshot_projectile",
            EntityType.Builder.<SlingshotProjectileEntity>create(SlingshotProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25F, 0.25F)
                    .maxTrackingRange(4)
                    .trackingTickInterval(10));

    public static final EntityType<GamblerEntity> GAMBLER = register(
            "gambler",
            EntityType.Builder.create(GamblerEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6F, 1.95F)
                    .maxTrackingRange(8)
                    .trackingTickInterval(3));

    public static final EntityType<SeatEntity> SEAT = register(
            "seat",
            EntityType.Builder.<SeatEntity>create(SeatEntity::new, SpawnGroup.MISC)
                    .dimensions(0.01F, 0.01F)
                    .maxTrackingRange(4)
                    .trackingTickInterval(20));

    public static void registerModEntities() {
        AncientCraft.LOGGER.info("Registering Ancient Craft entities");
        FabricDefaultAttributeRegistry.register(GAMBLER, GamblerEntity.createGamblerAttributes());
    }
}
