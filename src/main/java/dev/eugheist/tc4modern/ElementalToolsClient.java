package dev.eugheist.tc4modern;
import java.util.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.particle.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
/** Original RenderEventHandler 17-cube scan, node strips and FXBubble. */
public final class ElementalToolsClient {
 static final Map<BlockPos,Integer> scanned=new LinkedHashMap<>();static long until;static int rendered;
 static void startScan(BlockPos pos){var mc=Minecraft.getInstance();if(mc.level==null)return;scanned.clear();until=System.currentTimeMillis()+5000;for(int x=-8;x<=8;x++)for(int y=-8;y<=8;y++)for(int z=-8;z<=8;z++){var at=pos.offset(x,y,z);var s=mc.level.getBlockState(at);int value=-1;if(s.getFluidState().is(FluidTags.LAVA))value=-10;else if(s.getFluidState().is(FluidTags.WATER))value=-5;else if(isOre(s)){var tags=DeconstructorEntity.tags(new ItemStack(s.getBlock()),mc.level);value=tags.visSize();}if(value>=0||value==-5||value==-10)scanned.put(at,value);}origin=pos.immutable();}
 static boolean isOre(net.minecraft.world.level.block.state.BlockState state){var tags=net.neoforged.neoforge.common.Tags.Blocks.ORES;return state.is(tags)||state.getTags().anyMatch(t->t.location().getNamespace().equals("c")&&t.location().getPath().startsWith("ores/"));}
 static BlockPos origin=BlockPos.ZERO;
 @EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)static final class Mod {
  @SubscribeEvent static void renderer(EntityRenderersEvent.RegisterRenderers e){/* The vanilla pickup particle renders ItemEntity.copy(): a plain ItemEntity that still carries this type, so the renderer must accept any ItemEntity. */
  @SuppressWarnings({"unchecked","rawtypes"})net.minecraft.world.entity.EntityType<ItemEntity> type=(net.minecraft.world.entity.EntityType)ElementalTools.FOLLOWING.get();e.registerEntityRenderer(type,ItemEntityRenderer::new);}
 }
 @EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)static final class Events {
  @SubscribeEvent static void render(RenderLevelStageEvent e){if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||System.currentTimeMillis()>=until)return;var mc=Minecraft.getInstance();var pose=new PoseStack();pose.mulPose(e.getModelViewMatrix());var eye=e.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();long now=System.currentTimeMillis(),dif=until-now;float base=1;if(dif>4750)base=1-(dif-4750)/5f;if(dif<1500)base=dif/1500f;for(var entry:scanned.entrySet()){var pos=entry.getKey();float alpha=base*(1-(float)pos.distSqr(origin)/64);if(alpha<=0)continue;rendered++;pose.pushPose();pose.translate(pos.getX()+.5-eye.x,pos.getY()+.5-eye.y,pos.getZ()+.5-eye.z);int value=entry.getValue();if(value==-5||value==-10){var out=buffers.getBuffer(NodeRenderTypes.additiveThrough(AlchemyContent.id("textures/blocks/wardedglass.png")));int rgb=value==-5?3986684:16734721;for(var dir:Direction.values()){pose.pushPose();pose.mulPose(dir.getRotation());pose.translate(0,.5,0);pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));quad(pose,out,.5f,0,1,0,1,color(alpha,rgb));pose.popPose();}}else{pose.mulPose(e.getCamera().rotation());int frame=(int)(now/50%32);var out=buffers.getBuffer(NodeRenderTypes.additiveThrough(AlchemyContent.id("textures/misc/nodes.png")));quad(pose,out,.2f*value/7,frame/32f,(frame+1)/32f,0,1/32f,color(alpha,0xaaaa11));quad(pose,out,.5f*value/7,frame/32f,(frame+1)/32f,0,1/32f,color(alpha,0xaa1122));}pose.popPose();}buffers.endBatch();}
 }
 static int color(float a,int rgb){return (Math.min(255,Math.max(0,(int)(a*255)))<<24)|rgb;}
 static void quad(PoseStack p,VertexConsumer out,float size,float u0,float u1,float v0,float v1,int color){float[][] vs={{-size,-size,u1,v1},{-size,size,u1,v0},{size,size,u0,v0},{size,-size,u0,v1}};for(var v:vs)out.addVertex(p.last().pose(),v[0],v[1],0).setUv(v[2],v[3]).setColor(color);}
 static void blockBubble(BlockPos p){var mc=Minecraft.getInstance();if(mc.level==null)return;var r=mc.level.random;int count=switch(mc.options.particles().get()){case ALL->2;case DECREASED->1;default->0;};for(int i=0;i<count;i++){mc.particleEngine.add(new Bubble(mc.level,p.getX(),p.getY()+r.nextFloat(),p.getZ()+r.nextFloat()));mc.particleEngine.add(new Bubble(mc.level,p.getX()+1,p.getY()+r.nextFloat(),p.getZ()+r.nextFloat()));mc.particleEngine.add(new Bubble(mc.level,p.getX()+r.nextFloat(),p.getY()+r.nextFloat(),p.getZ()));mc.particleEngine.add(new Bubble(mc.level,p.getX()+r.nextFloat(),p.getY()+r.nextFloat(),p.getZ()+1));}}
 static void bubble(ItemEntity item){var mc=Minecraft.getInstance();if(mc.level==null)return;var r=mc.level.random;mc.particleEngine.add(new Bubble(mc.level,item.getX()+(r.nextFloat()-r.nextFloat())*.125,item.getY()+(r.nextFloat()-r.nextFloat())*.125,item.getZ()+(r.nextFloat()-r.nextFloat())*.125));}
 static final class Bubble extends Particle {
  static final ParticleRenderType TYPE=new ParticleRenderType(){public BufferBuilder begin(Tesselator t,net.minecraft.client.renderer.texture.TextureManager textures){RenderSystem.setShader(GameRenderer::getParticleShader);RenderSystem.setShaderTexture(0,AlchemyContent.id("textures/misc/particles.png"));RenderSystem.enableBlend();RenderSystem.blendFunc(770,771);RenderSystem.depthMask(false);return t.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);}};
  final float size;int frame=16,remaining;
  Bubble(net.minecraft.client.multiplayer.ClientLevel l,double x,double y,double z){super(l,x,y,z);setSize(.02f,.02f);hasPhysics=false;size=(random.nextFloat()*.5f+.5f)*2*(random.nextFloat()*.3f+.2f);xd=(random.nextFloat()*2-1)*.02;yd=random.nextFloat()*.02;zd=(random.nextFloat()*2-1)*.02;remaining=(int)(3+8/(Math.random()*.8+.2));rCol=.33f;gCol=.33f;bCol=1;}
  @Override public void tick(){xo=x;yo=y;zo=z;yd+=.002;xd+=(random.nextFloat()-random.nextFloat())*.01;zd+=(random.nextFloat()-random.nextFloat())*.01;setPos(x+xd,y+yd,z+zd);xd*=.85f;yd*=.85f;zd*=.85f;if(remaining--<=0)remove();else if(remaining<=2)frame++;}
  @Override public ParticleRenderType getRenderType(){return TYPE;}
  @Override public void render(VertexConsumer out,Camera camera,float partial){var eye=camera.getPosition();float px=(float)(xo+(x-xo)*partial-eye.x),py=(float)(yo+(y-yo)*partial-eye.y),pz=(float)(zo+(z-zo)*partial-eye.z);float u=frame%16/16f,v=frame/16/16f;float[] xs={1,1,-1,-1},ys={-1,1,1,-1};for(int i=0;i<4;i++){var q=new org.joml.Vector3f(xs[i]*size*.1f,ys[i]*size*.1f,0).rotate(camera.rotation());out.addVertex(px+q.x,py+q.y,pz+q.z).setUv(i<2?u+.0624375f:u,i==0||i==3?v+.0624375f:v).setColor(rCol,gCol,bCol,1).setLight(LightTexture.FULL_BRIGHT);}}
 }
}
