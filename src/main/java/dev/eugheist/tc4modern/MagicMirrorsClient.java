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
   // The star square (3/16..13/16) sits behind the frame face so the ring hides its corners, as on the original block.
   if(t.linked){pose.pushPose();pose.translate(0,0,-.05f);var random=new Random(31100L);var camera=mc.gameRenderer.getMainCamera().getPosition();
    // TileMirrorRenderer.drawPlane*: object-linear plane coordinates with an eye-linear q, i.e. per vertex
    // s = (w - cam)(1 + depth/dist) - cam on the two in-plane world axes, then the layer's time/scale/rotation texture matrix.
    var axis=d.getAxis();double camA=axis==Direction.Axis.X?camera.z:camera.x,camB=axis==Direction.Axis.Y?camera.z:camera.y;
    float[][] points={{-.3125f,-.3125f},{.3125f,-.3125f},{.3125f,.3125f},{-.3125f,.3125f}};var world=new double[4][3];
    for(int k=0;k<4;k++){var w=pose.last().pose().transformPosition(points[k][0],points[k][1],0,new org.joml.Vector3f());world[k][0]=camera.x+w.x;world[k][1]=camera.y+w.y;world[k][2]=camera.z+w.z;}
    double planeN=axis==Direction.Axis.X?world[0][0]:axis==Direction.Axis.Y?world[0][1]:world[0][2],camN=axis==Direction.Axis.X?camera.x:axis==Direction.Axis.Y?camera.y:camera.z;
    double dist=Math.max(.01,Math.abs(planeN-camN));double time=System.currentTimeMillis()%700000L/250000.0;
    for(int i=0;i<16;i++){float height=i==0?65:16-i,scale=i==0?.125f:i==1?.5f:.0625f,brightness=i==0?.1f:1f/(height+1),r=random.nextFloat()*.5f+.1f,g=random.nextFloat()*.5f+.4f,b=random.nextFloat()*.5f+.5f;if(i==0)r=g=b=1;var texture=AlchemyContent.id(i==0?"textures/misc/tunnel.png":"textures/misc/particlefield.png");var out=buffers.getBuffer(i==0?NodeRenderTypes.translucent(texture):NodeRenderTypes.additive(texture));double angle=Math.toRadians((i*i*4321+i*9)*2.0),cos=Math.cos(angle),sin=Math.sin(angle);
     double[][] uv=new double[4][2];
     for(int k=0;k<4;k++){double wa=axis==Direction.Axis.X?world[k][2]:world[k][0],wb=axis==Direction.Axis.Y?world[k][2]:world[k][1];
      double s=(wa-camA)*(1+height/dist)-camA,q=(wb-camB)*(1+height/dist)-camB;
      uv[k][0]=(cos*(s-.5)-sin*(q-.5)+.5)*scale;uv[k][1]=(sin*(s-.5)+cos*(q-.5)+.5)*scale+time;}
     // Repeating texture: drop whole tiles once per quad so large world coordinates keep float precision.
     double fu=Math.floor(uv[0][0]),fv=Math.floor(uv[0][1]);
     for(int k=0;k<4;k++)out.addVertex(pose.last().pose(),points[k][0],points[k][1],0).setColor(r*brightness,g*brightness,b*brightness,1).setUv((float)(uv[k][0]-fu),(float)(uv[k][1]-fv));
    }
    var seal=buffers.getBuffer(NodeRenderTypes.depthOnly());for(var p:points)seal.addVertex(pose.last().pose(),p[0],p[1],-.002f);
    pose.popPose();
   }
   var out=buffers.getBuffer(RenderType.entityTranslucent(AlchemyContent.id(t.linked?"textures/blocks/mirrorpanetrans.png":"textures/blocks/mirrorpane.png")));float[][] pts={{-.5f,-.5f,0,1},{.5f,-.5f,1,1},{.5f,.5f,1,0},{-.5f,.5f,0,0}};for(var p:pts)out.addVertex(pose.last().pose(),p[0],p[1],.001f).setColor(-1).setUv(p[2],p[3]).setOverlay(overlay).setLight(light).setNormal(pose.last(),0,0,1);pose.popPose();
  }
 }
}
