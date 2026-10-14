package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/** ForgeHooksClient.renderInventoryItem's INVENTORY_BLOCK helper in modern 16-pixel item coordinates. */
final class LegacyInventoryTransforms {
    static void helper(PoseStack pose) {
        pose.translate(.5f,.5f,.5f);
        // GuiGraphics already flips Y; old Forge flips Z after its ten-pixel scale.
        pose.scale(.625f,-.625f,-.625f);
        pose.mulPose(Axis.XP.rotationDegrees(210));
        pose.mulPose(Axis.YP.rotationDegrees(45));
        pose.mulPose(Axis.YP.rotationDegrees(-90));
    }
}
