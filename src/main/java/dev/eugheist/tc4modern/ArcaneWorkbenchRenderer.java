package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;

/** TileArcaneWorkbenchRenderer wand branch, plus a gentle hover the user asked for. The table model comes from the block model. */
public final class ArcaneWorkbenchRenderer implements BlockEntityRenderer<ArcaneWorkbenchEntity> {
 public ArcaneWorkbenchRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(ArcaneWorkbenchEntity table,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  var wand=table.inventory.getItem(9);if(!WandData.isWand(wand))return;
  pose.pushPose();
  /* requested addition (not in TC4): the wand hovers a little above the table, bobbing and swaying; phase varies per table */
  float t=table.getLevel()==null?0:(table.getLevel().getGameTime()%24000L)+partial+(table.getBlockPos().asLong()%97);
  pose.translate(.5,1.0625+.07+.03f*net.minecraft.util.Mth.sin(t*.08f),.5);pose.mulPose(Axis.YP.rotationDegrees(4*net.minecraft.util.Mth.sin(t*.031f)));pose.mulPose(Axis.XP.rotationDegrees(3*net.minecraft.util.Mth.sin(t*.047f)));pose.translate(-.5,0,-.5);
  pose.translate(.65,0,.25);pose.mulPose(Axis.XP.rotationDegrees(90));pose.mulPose(Axis.ZP.rotationDegrees(20+2.5f*net.minecraft.util.Mth.sin(t*.055f)));
  // RenderItem's ENTITY helper scale, then ItemWandRenderer's ENTITY translation and model flip.
  pose.scale(.5f,.5f,.5f);
  if(WandParts.isStaff(wand)){pose.translate(0,2,0);pose.scale(.9f,.9f,.9f);}else pose.translate(0,1,0);
  pose.mulPose(Axis.XP.rotationDegrees(180));WandRenderer.renderModel(wand,pose,buffers,light,overlay);pose.popPose();
 }
}
