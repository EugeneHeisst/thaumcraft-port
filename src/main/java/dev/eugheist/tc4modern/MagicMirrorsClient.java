package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
/** Original sixteen star/particle-field planes, fixed random seed and moving camera projection. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class MagicMirrorsClient {
 @SubscribeEvent static void menus(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(HandMirror.MENU.get(),Screen::new);}
 static final class Screen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<HandMirror.Menu> {
  Screen(HandMirror.Menu m,net.minecraft.world.entity.player.Inventory i,net.minecraft.network.chat.Component title){super(m,i,title);}
  @Override protected void renderBg(net.minecraft.client.gui.GuiGraphics g,float partial,int x,int y){g.blit(AlchemyContent.id("textures/gui/guihandmirror.png"),leftPos,topPos,0,0,imageWidth,imageHeight);}
  @Override protected void renderLabels(net.minecraft.client.gui.GuiGraphics g,int x,int y){}
  @Override public void render(net.minecraft.client.gui.GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
 }
 @SubscribeEvent static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{for(var holder:java.util.List.of(MagicMirrors.ITEM,MagicMirrors.ESSENTIA))net.minecraft.client.renderer.item.ItemProperties.register(holder.get().asItem(),AlchemyContent.id("linked"),(stack,level,entity,seed)->NoteData.tag(stack).contains("linkX")?1:0);});}
 @SubscribeEvent static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e){for(String name:java.util.List.of("mirror_frame","mirror_essentia_frame"))e.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(AlchemyContent.id("item/"+name)));}
 static int rendered;
 @SubscribeEvent static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(MagicMirrors.ENTITY.get(),Renderer::new);}
 static void particles(Level l,BlockPos p,Direction d){int count=Minecraft.getInstance().options.particles().get()==net.minecraft.client.ParticleStatus.MINIMAL?0:Minecraft.getInstance().options.particles().get()==net.minecraft.client.ParticleStatus.DECREASED?1:2;for(int i=0;i<count;i++){double x=p.getX()+.33+l.random.nextFloat()*.33-d.getStepX()*.5,y=p.getY()+.33+l.random.nextFloat()*.33-d.getStepY()*.5,z=p.getZ()+.33+l.random.nextFloat()*.33-d.getStepZ()*.5;var particle=Minecraft.getInstance().particleEngine.createParticle(net.minecraft.core.particles.ColorParticleOption.create(net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT,0xff000000),x,y,z,0,0,0);if(particle!=null){particle.setParticleSpeed(d.getStepX()*.05,d.getStepY()*.05,d.getStepZ()*.05);particle.scale(.5f);particle.setColor(0,0,0);}}}
 static final class Renderer implements BlockEntityRenderer<MagicMirrors.MirrorEntity> {
  Renderer(BlockEntityRendererProvider.Context c){}
  public void render(MagicMirrors.MirrorEntity t,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){rendered++;var mc=Minecraft.getInstance();pose.pushPose();var d=t.facing();pose.translate(.5-d.getStepX()*.5,.5-d.getStepY()*.5,.5-d.getStepZ()*.5);switch(d){case NORTH->pose.mulPose(Axis.YP.rotationDegrees(180));case EAST->pose.mulPose(Axis.YP.rotationDegrees(90));case WEST->pose.mulPose(Axis.YP.rotationDegrees(-90));case UP->pose.mulPose(Axis.XP.rotationDegrees(-90));case DOWN->pose.mulPose(Axis.XP.rotationDegrees(90));default->{}}
   // The generated item mesh supplies the original alpha-cut frame and its 1/16 extrusion.
   pose.pushPose();pose.translate(0,0,.03125);mc.getItemRenderer().render(new ItemStack(t.getBlockState().getBlock()),ItemDisplayContext.NONE,false,pose,buffers,light,overlay,mc.getModelManager().getModel(net.minecraft.client.resources.model.ModelResourceLocation.standalone(AlchemyContent.id(t.essentia()?"item/mirror_essentia_frame":"item/mirror_frame"))));pose.popPose();
   float jitter=t.instability>0?t.getLevel().random.nextFloat()*t.instability/10000f:0;pose.translate(0,0,.07+jitter);
   if(t.linked){var random=new Random(31100L);var camera=mc.gameRenderer.getMainCamera().getPosition();var relative=camera.subtract(t.getBlockPos().getX()+.5,t.getBlockPos().getY()+.5,t.getBlockPos().getZ()+.5);double depth=Math.max(.01,Math.abs(relative.x*d.getStepX()+relative.y*d.getStepY()+relative.z*d.getStepZ()));double viewU=d.getAxis()==Direction.Axis.X?relative.z:relative.x,viewV=d.getAxis()==Direction.Axis.Y?relative.z:relative.y;
    for(int i=0;i<16;i++){float height=i==0?65:16-i,scale=i==0?.125f:i==1?.5f:.0625f,brightness=i==0?.1f:1f/(height+1),r=random.nextFloat()*.5f+.1f,g=random.nextFloat()*.5f+.4f,b=random.nextFloat()*.5f+.5f;if(i==0)r=g=b=1;var texture=AlchemyContent.id(i==0?"textures/misc/tunnel.png":"textures/misc/particlefield.png");var out=buffers.getBuffer(i==0?NodeRenderTypes.translucent(texture):NodeRenderTypes.additive(texture));float angle=(float)Math.toRadians((i*i*4321+i*9)*2f);float[][] points={{-.3125f,-.3125f},{.3125f,-.3125f},{.3125f,.3125f},{-.3125f,.3125f}};
     for(var p:points){double x=(p[0]+viewU*height/depth)*scale,y=(p[1]+viewV*height/depth)*scale;float u=(float)(x*Math.cos(angle)-y*Math.sin(angle)+.5),v=(float)(x*Math.sin(angle)+y*Math.cos(angle)+.5+System.currentTimeMillis()%700000L/250000.0);out.addVertex(pose.last().pose(),p[0],p[1],0).setColor(r*brightness,g*brightness,b*brightness,1).setUv(u,v).setOverlay(overlay).setLight(0x00b400b4).setNormal(pose.last(),0,0,1);}
    }
   }
   var out=buffers.getBuffer(RenderType.entityTranslucent(AlchemyContent.id(t.linked?"textures/blocks/mirrorpanetrans.png":"textures/blocks/mirrorpane.png")));float[][] pts={{-.5f,-.5f,0,1},{.5f,-.5f,1,1},{.5f,.5f,1,0},{-.5f,.5f,0,0}};for(var p:pts)out.addVertex(pose.last().pose(),p[0],p[1],.001f).setColor(-1).setUv(p[2],p[3]).setOverlay(overlay).setLight(light).setNormal(pose.last(),0,0,1);pose.popPose();
  }
 }
}
