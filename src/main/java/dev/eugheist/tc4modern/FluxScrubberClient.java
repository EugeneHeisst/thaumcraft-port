package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Original TileFluxScrubberRenderer over unchanged obelisk_cap OBJ Cap/Tip groups. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
final class FluxScrubberClient {
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(FluxScrubber.ENTITY.get(),Renderer::new);}
 @SubscribeEvent static void items(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){final ItemRenderer renderer=new ItemRenderer();public BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}},FluxScrubber.BLOCK.get().asItem());}
 static final class Renderer implements BlockEntityRenderer<FluxScrubber.ScrubberEntity>{
  final ObjParts mesh=new ObjParts(AlchemyContent.id("models/obelisk_cap.obj"));static final net.minecraft.resources.ResourceLocation TEX=AlchemyContent.id("textures/models/fluxscrubber.png");Renderer(BlockEntityRendererProvider.Context c){}
  public void render(FluxScrubber.ScrubberEntity tile,float partial,PoseStack p,MultiBufferSource b,int light,int overlay){p.pushPose();p.translate(.5,.5,.5);switch(tile.facing()){case DOWN->p.mulPose(Axis.XP.rotationDegrees(-90));case UP->p.mulPose(Axis.XP.rotationDegrees(90));case SOUTH->p.mulPose(Axis.YP.rotationDegrees(180));case WEST->p.mulPose(Axis.YP.rotationDegrees(90));case EAST->p.mulPose(Axis.YP.rotationDegrees(-90));default->{}}p.translate(0,0,-.5);mesh.render("Cap",TEX,p,b,light,overlay,0xffffffff);var player=Minecraft.getInstance().player;float q=(player==null?0:player.tickCount)+partial+tile.count;float bob=(float)Math.sin(q/8)*.075f+.075f;p.translate(0,0,-bob);mesh.render("Tip",TEX,p,b,light,overlay,0xffffffff);p.popPose();}
 }
 static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
  final Renderer renderer=new Renderer(null);FluxScrubber.ScrubberEntity tile;ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
  @Override public void renderByItem(net.minecraft.world.item.ItemStack s,net.minecraft.world.item.ItemDisplayContext c,PoseStack p,MultiBufferSource b,int light,int overlay){if(tile==null)tile=new FluxScrubber.ScrubberEntity(net.minecraft.core.BlockPos.ZERO,FluxScrubber.BLOCK.get().defaultBlockState());renderer.render(tile,0,p,b,light,overlay);}
 }
}
