package dev.birinder.ac.client.render;

import dev.birinder.ac.entity.custom.GamblerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.util.Identifier;

public class GamblerEntityRenderer extends MobEntityRenderer<GamblerEntity, VillagerResemblingModel<GamblerEntity>> {

    private static final Identifier VILLAGER_TEXTURE = Identifier.ofVanilla("textures/entity/villager/villager.png");

    public GamblerEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new VillagerResemblingModel<>(context.getPart(EntityModelLayers.VILLAGER)), 0.5F);
    }

    @Override
    public Identifier getTexture(GamblerEntity entity) {
        return VILLAGER_TEXTURE;
    }
}
