package dev.birinder.ac.client.render;

import dev.birinder.ac.AncientCraft;
import dev.birinder.ac.client.AncientCraftClient;
import dev.birinder.ac.client.render.model.GuardEntityModel;
import dev.birinder.ac.entity.custom.GuardEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class GuardEntityRenderer extends BipedEntityRenderer<GuardEntity, GuardEntityModel> {

    private static final Identifier TEXTURE = AncientCraft.id("textures/entity/guard/guard.png");

    public GuardEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new GuardEntityModel(context.getPart(AncientCraftClient.GUARD_MODEL_LAYER)), 0.5F);
    }

    @Override
    public Identifier getTexture(GuardEntity entity) {
        return TEXTURE;
    }
}
