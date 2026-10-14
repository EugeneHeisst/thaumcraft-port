package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** The original six exposed planes, using the same tunnel/particle-field technique as obelisks. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
final class OuterVoidClient implements BlockEntityRenderer<OuterContent.VoidEntity> {
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(OuterContent.VOID_ENTITY.get(),c->new OuterVoidClient());}
 @Override public void render(OuterContent.VoidEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){var level=tile.getLevel();if(level==null)return;var pos=tile.getBlockPos();var viewer=Minecraft.getInstance().getCameraEntity();if(viewer==null)return;boolean near=viewer.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<512;
  for(var direction:Direction.values()){if(level.getBlockState(pos.relative(direction)).canOcclude())continue;
   float[][] vertices=switch(direction){case UP->new float[][]{{0,1,0},{0,1,1},{1,1,1},{1,1,0}};case DOWN->new float[][]{{0,0,1},{0,0,0},{1,0,0},{1,0,1}};case NORTH->new float[][]{{1,1,0},{1,0,0},{0,0,0},{0,1,0}};case SOUTH->new float[][]{{0,1,1},{0,0,1},{1,0,1},{1,1,1}};case WEST->new float[][]{{0,1,0},{0,0,0},{0,0,1},{0,1,1}};case EAST->new float[][]{{1,1,1},{1,0,1},{1,0,0},{1,1,0}};};
   var out=buffers.getBuffer(near?CrimsonRenderers.Types.PORTAL:RenderType.entityCutoutNoCull(AlchemyContent.id("textures/misc/particlefield32.png")));float[][] uv={{1,1},{1,0},{0,0},{0,1}};
   for(int i=0;i<4;i++){var v=vertices[i];out.addVertex(pose.last().pose(),v[0],v[1],v[2]);if(!near)out.setColor(-1).setUv(uv[i][0],uv[i][1]).setOverlay(overlay).setLight(180).setNormal(pose.last(),direction.getStepX(),direction.getStepY(),direction.getStepZ());}
  }
 }
}
