package dev.birinder.ac.client.render;

import dev.birinder.ac.entity.custom.TavernVillagerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.VillagerClothingFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.util.Identifier;

public class TavernVillagerEntityRenderer extends MobEntityRenderer<TavernVillagerEntity, VillagerResemblingModel<TavernVillagerEntity>> {

    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/villager/villager.png");

    public TavernVillagerEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new VillagerResemblingModel<>(context.getPart(EntityModelLayers.VILLAGER)), 0.5f);
        this.addFeature(new VillagerClothingFeatureRenderer<>(this, context.getResourceManager(), "villager"));
    }

    @Override
    public Identifier getTexture(TavernVillagerEntity entity) {
        return TEXTURE;
    }
}
