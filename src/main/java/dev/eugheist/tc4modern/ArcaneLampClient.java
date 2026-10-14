package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** TileArcaneLampRenderer + ModelBore.renderNozzle, original cubes/UVs/orientation. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class ArcaneLampClient {
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(ArcaneLamps.ENTITY.get(),Renderer::new);}
 /** TileArcaneLampRenderer: ModelBoreBase.renderNozzle (Bore.png 128x64 UVs) pointing at the lamp's facing, and a second nozzle into an adjacent arcane bore base. */
 static final class Renderer implements BlockEntityRenderer<ArcaneLamps.LampEntity>{
  final ModelPart nozzle;
  Renderer(BlockEntityRendererProvider.Context c){var mesh=new MeshDefinition();var root=mesh.getRoot();
   root.addOrReplaceChild("nozzle1",CubeListBuilder.create().texOffs(106,42).mirror().addBox(2.5f,-2,-2,5,4,4),PartPose.offset(0,8,0));
   root.addOrReplaceChild("nozzle2",CubeListBuilder.create().texOffs(106,51).mirror().addBox(7,-2.5f,-2.5f,1,5,5),PartPose.offset(0,8,0));
   nozzle=LayerDefinition.create(mesh,128,64).bakeRoot();}
  static void orient(PoseStack p,net.minecraft.core.Direction d){switch(d){case DOWN->{p.translate(-.5,.5,0);p.mulPose(Axis.ZP.rotationDegrees(-90));}case UP->{p.translate(.5,.5,0);p.mulPose(Axis.ZP.rotationDegrees(90));}case NORTH->p.mulPose(Axis.YP.rotationDegrees(90));case SOUTH->p.mulPose(Axis.YP.rotationDegrees(270));case WEST->p.mulPose(Axis.YP.rotationDegrees(180));default->{}}}
  public void render(ArcaneLamps.LampEntity t,float partial,PoseStack p,MultiBufferSource b,int light,int overlay){
   var dir=t.facing();var buffer=b.getBuffer(RenderType.entityCutout(AlchemyContent.id("textures/models/bore.png")));
   if(t.getLevel()!=null&&t.getLevel().getBlockEntity(t.getBlockPos().relative(dir)) instanceof ArcaneBore.BaseEntity){p.pushPose();p.translate(.5+dir.getStepX(),dir.getStepY(),.5+dir.getStepZ());orient(p,dir.getOpposite());nozzle.render(p,buffer,light,overlay);p.popPose();}
   p.pushPose();p.translate(.5,0,.5);orient(p,dir);nozzle.render(p,buffer,light,overlay);p.popPose();}
 }
}
