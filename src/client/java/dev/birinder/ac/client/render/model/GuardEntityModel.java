package dev.birinder.ac.client.render.model;

import dev.birinder.ac.entity.custom.GuardEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;

public class GuardEntityModel extends BipedEntityModel<GuardEntity> {

        public GuardEntityModel(ModelPart root) {
                super(root);
        }

        public static TexturedModelData getTexturedModelData() {
                ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
                ModelPartData root = modelData.getRoot();

                // Clear the hat (head armor) to fix glitching
                root.addChild(EntityModelPartNames.HAT, ModelPartBuilder.create(), ModelTransform.NONE);

                // Right Arm Pauldron (New, clean segmented pauldron)
                ModelPartData rightArm = root.getChild(EntityModelPartNames.RIGHT_ARM);
                rightArm.addChild("right_pauldron_top",
                                ModelPartBuilder.create()
                                                .uv(48, 0)
                                                .cuboid(-4.0F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new Dilation(0.3F)),
                                ModelTransform.NONE);
                rightArm.addChild("right_pauldron_bottom",
                                ModelPartBuilder.create()
                                                .uv(48, 10)
                                                .cuboid(-3.5F, 2.5F, -2.0F, 4.0F, 4.0F, 4.0F, new Dilation(0.1F)),
                                ModelTransform.NONE);

                // Left Arm Pauldron (New, clean segmented pauldron)
                ModelPartData leftArm = root.getChild(EntityModelPartNames.LEFT_ARM);
                leftArm.addChild("left_pauldron_top",
                                ModelPartBuilder.create()
                                                .uv(48, 0)
                                                .cuboid(-1.0F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new Dilation(0.3F)),
                                ModelTransform.NONE);
                leftArm.addChild("left_pauldron_bottom",
                                ModelPartBuilder.create()
                                                .uv(48, 10)
                                                .cuboid(-0.5F, 2.5F, -2.0F, 4.0F, 4.0F, 4.0F, new Dilation(0.1F)),
                                ModelTransform.NONE);

                // Padded Gambeson Fauld / Skirt (Torso lower extension)
                ModelPartData body = root.getChild(EntityModelPartNames.BODY);
                body.addChild("gambeson_skirt",
                                ModelPartBuilder.create()
                                                .uv(16, 36)
                                                .cuboid(-4.25F, 9.0F, -2.25F, 8.5F, 4.5F, 4.5F, new Dilation(0.15F)),
                                ModelTransform.NONE);

                return TexturedModelData.of(modelData, 64, 64);
        }
}