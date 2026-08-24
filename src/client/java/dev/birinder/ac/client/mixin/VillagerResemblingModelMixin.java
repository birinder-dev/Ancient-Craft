package dev.birinder.ac.client.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerResemblingModel.class)
public abstract class VillagerResemblingModelMixin<T extends Entity> extends SinglePartEntityModel<T> {

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void ancientCraft$applySittingPose(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        try {
            ModelPart root = this.getPart();
            
            // Find the coat/skirt part
            ModelPart robe = null;
            if (root.hasChild("body") && root.getChild("body").hasChild("jacket")) {
                robe = root.getChild("body").getChild("jacket");
            } else if (root.hasChild("jacket")) {
                robe = root.getChild("jacket");
            } else if (root.hasChild("robe")) {
                robe = root.getChild("robe");
            }

            if (this.riding) {
                // 1. Bent legs (sitting forward ~81 degrees matching player's BipedEntityModel)
                if (root.hasChild("right_leg") && root.hasChild("left_leg")) {
                    ModelPart rightLeg = root.getChild("right_leg");
                    ModelPart leftLeg = root.getChild("left_leg");

                    rightLeg.pitch = -1.4137167F; // ~81 degrees forward
                    rightLeg.yaw = 0.31415927F;   // 18 degrees outward
                    rightLeg.roll = 0.07853982F;

                    leftLeg.pitch = -1.4137167F;  // ~81 degrees forward
                    leftLeg.yaw = -0.31415927F;  // 18 degrees outward
                    leftLeg.roll = -0.07853982F;
                }

                // 2. Hide the coat skirt completely when sitting as requested!
                if (robe != null) {
                    robe.visible = false;
                }
            } else {
                // Make sure to reset visibility when they stand up!
                if (robe != null) {
                    robe.visible = true;
                }
            }
        } catch (Exception ignored) {
        }
    }
}
