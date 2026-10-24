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
 static int rendered;
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(OuterContent.VOID_ENTITY.get(),c->new OuterVoidClient());}
 static int faces(OuterContent.VoidEntity tile){var level=tile.getLevel();if(level==null)return 0;long now=level.getGameTime();
  if(tile.faces<0||now-tile.facesChecked>=10||now<tile.facesChecked){var pos=tile.getBlockPos();int faces=0;for(var direction:Direction.values()){var neighbour=level.getBlockState(pos.relative(direction));if(!neighbour.canOcclude()&&!neighbour.is(OuterContent.VOID.get()))faces|=1<<direction.ordinal();}tile.faces=faces;tile.facesChecked=now;}
  return tile.faces;}
 /** Buried void is rejected here, before the dispatcher computes light and pushes a pose for it. */
 @Override public boolean shouldRender(OuterContent.VoidEntity tile,net.minecraft.world.phys.Vec3 camera){return faces(tile)!=0&&BlockEntityRenderer.super.shouldRender(tile,camera);}
 @Override public void render(OuterContent.VoidEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){var level=tile.getLevel();if(level==null)return;var pos=tile.getBlockPos();var viewer=Minecraft.getInstance().getCameraEntity();if(viewer==null)return;
  /* Issue #13: the Outer Lands are walled by solid masses of void. The original drew every face not touching an opaque block, including the
     hidden ones between two void blocks, and looked up six neighbours each frame; skip void-void faces and refresh the mask (faces) twice a second. */
  if(faces(tile)==0)return;rendered++;boolean near=viewer.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<512;var camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
  for(var direction:Direction.values()){if((tile.faces&1<<direction.ordinal())==0)continue;
   /* a plane seen from behind is hidden by the opposite exposed plane or by the neighbouring void */
   double side=direction.getAxis().choose(camera.x-pos.getX(),camera.y-pos.getY(),camera.z-pos.getZ());if(direction.getAxisDirection()==Direction.AxisDirection.POSITIVE?side<=1:side>=0)continue;
   float[][] vertices=switch(direction){case UP->new float[][]{{0,1,0},{0,1,1},{1,1,1},{1,1,0}};case DOWN->new float[][]{{0,0,1},{0,0,0},{1,0,0},{1,0,1}};case NORTH->new float[][]{{1,1,0},{1,0,0},{0,0,0},{0,1,0}};case SOUTH->new float[][]{{0,1,1},{0,0,1},{1,0,1},{1,1,1}};case WEST->new float[][]{{0,1,0},{0,0,0},{0,0,1},{0,1,1}};case EAST->new float[][]{{1,1,1},{1,0,1},{1,0,0},{1,1,0}};};
   var out=buffers.getBuffer(near?CrimsonRenderers.Types.PORTAL:RenderType.entityCutoutNoCull(AlchemyContent.id("textures/misc/particlefield32.png")));float[][] uv={{1,1},{1,0},{0,0},{0,1}};
   for(int i=0;i<4;i++){var v=vertices[i];out.addVertex(pose.last().pose(),v[0],v[1],v[2]);if(!near)out.setColor(-1).setUv(uv[i][0],uv[i][1]).setOverlay(overlay).setLight(180).setNormal(pose.last(),direction.getStepX(),direction.getStepY(),direction.getStepZ());}
  }
 }
}
