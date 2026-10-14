package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** TileMagicWorkbenchChargerRenderer over the unchanged original vis_relay OBJ. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class VisChargeRelayClient {
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(VisChargeRelay.ENTITY.get(),Renderer::new);}
 @SubscribeEvent static void items(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){final ItemRenderer renderer=new ItemRenderer();public BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}},VisChargeRelay.ITEM.get());}
 static final class Renderer implements BlockEntityRenderer<VisChargeRelay.Entity>{
  final ObjParts mesh=new ObjParts(AlchemyContent.id("models/vis_relay.obj"));
  static final net.minecraft.resources.ResourceLocation TEXTURE=AlchemyContent.id("textures/models/vis_relay.png");
  Renderer(BlockEntityRendererProvider.Context c){}
  public void render(VisChargeRelay.Entity tile,float partial,PoseStack p,MultiBufferSource b,int light,int overlay){
   p.pushPose();p.translate(.5,.5,.5);p.mulPose(Axis.XP.rotationDegrees(-90));p.mulPose(Axis.XP.rotationDegrees(180));p.mulPose(Axis.ZP.rotationDegrees(45));
   mesh.render("RingFloat",TEXTURE,p,b,light,overlay,0xFFFFFFFF);
   p.pushPose();p.mulPose(Axis.XP.rotationDegrees(180));p.translate(0,0,.5);
   for(int a=0;a<4;a++){mesh.render("Support",TEXTURE,p,b,light,overlay,0xFFFFFFFF);p.mulPose(Axis.ZP.rotationDegrees(90));}p.popPose();
   int color=0xFFFFFFFF;if(tile.relay.color>=0&&tile.relay.color<6){int rgb=thaumcraft.common.tiles.VisRelayState.COLORS[tile.relay.color];color=0xFF000000|Math.min(255,(rgb>>16&255)*255/200)<<16|Math.min(255,(rgb>>8&255)*255/200)<<8|Math.min(255,(rgb&255)*255/200);}
   var player=Minecraft.getInstance().player;float ticks=(player==null?0:player.tickCount)+partial;
   int brightness=(tile.parent()!=null?50:0)+(int)(150*((float)Math.sin(ticks/2)*.05f+.95f));mesh.render("Crystal",TEXTURE,p,b,brightness,overlay,color);p.popPose();
  }
 }
 static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
  final Renderer renderer=new Renderer(null);VisChargeRelay.Entity tile;
  ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
  @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext c,PoseStack p,MultiBufferSource b,int light,int overlay){if(tile==null)tile=new VisChargeRelay.Entity(net.minecraft.core.BlockPos.ZERO,VisChargeRelay.BLOCK.get().defaultBlockState());renderer.render(tile,0,p,b,light,overlay);}
 }
}
