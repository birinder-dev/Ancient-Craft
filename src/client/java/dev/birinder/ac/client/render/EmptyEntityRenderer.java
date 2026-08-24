package dev.birinder.ac.client.render;

import dev.birinder.ac.entity.custom.SeatEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class EmptyEntityRenderer extends EntityRenderer<SeatEntity> {

    public EmptyEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(SeatEntity entity) {
        return null;
    }
}
