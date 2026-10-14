package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class HungryChestClient {
 @SubscribeEvent static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(HungryChest.TILE.get(),Renderer::new);}
 static final class Renderer implements BlockEntityRenderer<HungryChest.Tile>{
  final ModelPart lid,knob,base;
  Renderer(BlockEntityRendererProvider.Context c){var mesh=new MeshDefinition();var r=mesh.getRoot();r.addOrReplaceChild("lid",CubeListBuilder.create().texOffs(0,0).addBox(0,-5,-14,14,5,14),PartPose.offset(1,7,15));r.addOrReplaceChild("knob",CubeListBuilder.create().texOffs(0,0).addBox(-1,-2,-15,2,4,1),PartPose.offset(8,7,15));r.addOrReplaceChild("base",CubeListBuilder.create().texOffs(0,19).addBox(0,0,0,14,10,14),PartPose.offset(1,6,1));var root=LayerDefinition.create(mesh,64,64).bakeRoot();lid=root.getChild("lid");knob=root.getChild("knob");base=root.getChild("base");}
  public void render(HungryChest.Tile t,float partial,PoseStack p,MultiBufferSource buffers,int light,int overlay){p.pushPose();p.translate(.5,1,.5);p.mulPose(Axis.YP.rotationDegrees(-t.getBlockState().getValue(HungryChest.FACING).toYRot()));p.scale(1,-1,-1);p.translate(-.5,0,-.5);float open=1-t.getOpenNess(partial);lid.xRot=knob.xRot=-(1-open*open*open)*(float)Math.PI/2;var out=buffers.getBuffer(RenderType.entityCutout(AlchemyContent.id("textures/models/chesthungry.png")));lid.render(p,out,light,overlay);knob.render(p,out,light,overlay);base.render(p,out,light,overlay);p.popPose();}
 }
}
