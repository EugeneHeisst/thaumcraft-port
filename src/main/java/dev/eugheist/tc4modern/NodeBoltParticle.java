package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.*;
import thaumcraft.client.fx.WRVector3;
import thaumcraft.client.fx.bolt.FXLightningBoltCommon;
/** FXLightningBolt: the two passes (p_large glow, p_small core) of the original additive bolt types 0-4. */
public final class NodeBoltParticle extends Particle {
 private final FXLightningBoltCommon bolt;
 private final int pass,type;private final float width;
 private static final ParticleRenderType[] TYPES={type("p_large",false),type("p_small",false),type("p_large",true),type("p_small",true)};
 private static ParticleRenderType type(String texture,boolean translucent){return new ParticleRenderType(){
  public BufferBuilder begin(Tesselator tessellator,net.minecraft.client.renderer.texture.TextureManager textures){
   RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);
   RenderSystem.setShaderTexture(0,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/misc/"+texture+".png"));
   RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.blendFunc(770,translucent?771:1);RenderSystem.depthMask(false);
   return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);
  }
 };}
 public NodeBoltParticle(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z,double tx,double ty,double tz,long seed,int pass){
  this(level,x,y,z,tx,ty,tz,seed,pass,10,4f,5,0,.03f);
 }
 public NodeBoltParticle(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z,double tx,double ty,double tz,long seed,int pass,int duration,float multi,int speed,int type,float width){
  super(level,x,y,z);this.pass=pass;this.type=type;this.width=width;hasPhysics=false;
  bolt=new FXLightningBoltCommon(level,x,y,z,tx,ty,tz,seed,duration,multi,speed);bolt.type=type;bolt.defaultFractal();bolt.finalizeBolt();
  setBoundingBox(new net.minecraft.world.phys.AABB(x,y,z,tx,ty,tz).inflate(2));
 }
 @Override public void tick(){bolt.onUpdate();if(bolt.particleAge>=bolt.particleMaxAge)remove();}
 @Override public ParticleRenderType getRenderType(){return TYPES[pass+(type>=5?2:0)];}
 @Override public void render(VertexConsumer vertices,net.minecraft.client.Camera camera,float partial){
  var eye=camera.getPosition();if(eye.distanceToSqr(x,y,z)>10000)return;
  var look=new org.joml.Vector3f(0,0,1).rotate(camera.rotation());var view=new WRVector3(look.x,look.y,look.z);
  float age=bolt.particleAge>=0?(float)bolt.particleAge/bolt.particleMaxAge:0;
  // Original renderBolt overwrites the type6 mainalpha=.2 with these pass-specific values.
  float alpha=pass==0?(1-age)*.4f:1-age*.5f;
  int delay=(int)(bolt.length*3);int length=(int)((bolt.particleAge+partial+delay)/delay*bolt.numsegments0);
  for(var segment:bolt.segments){
   if(segment.segmentno>length)continue;
   float width=this.width*(new WRVector3(eye.x-segment.startpoint.point.x,eye.y-segment.startpoint.point.y,eye.z-segment.startpoint.point.z).length()/5+1)*(1+segment.light)*.5f;
   var d1=WRVector3.crossProduct(view,segment.prevdiff).scale(width/segment.sinprev);
   var d2=WRVector3.crossProduct(view,segment.nextdiff).scale(width/segment.sinnext);
   var start=segment.startpoint.point;var end=segment.endpoint.point;float a=alpha*segment.light;
   vertex(vertices,eye,end,d2,-1,.5f,0,a);vertex(vertices,eye,start,d1,-1,.5f,0,a);vertex(vertices,eye,start,d1,1,.5f,1,a);vertex(vertices,eye,end,d2,1,.5f,1,a);
   if(segment.next==null){var cap=end.copy().add(segment.diff.copy().normalize().scale(width));
    vertex(vertices,eye,cap,d2,-1,0,0,a);vertex(vertices,eye,end,d2,-1,.5f,0,a);vertex(vertices,eye,end,d2,1,.5f,1,a);vertex(vertices,eye,cap,d2,1,0,1,a);}
   if(segment.prev==null){var cap=start.copy().sub(segment.diff.copy().normalize().scale(width));
    vertex(vertices,eye,start,d1,-1,.5f,0,a);vertex(vertices,eye,cap,d1,-1,0,0,a);vertex(vertices,eye,cap,d1,1,0,1,a);vertex(vertices,eye,start,d1,1,.5f,1,a);}
  }
 }
 private void vertex(VertexConsumer v,net.minecraft.world.phys.Vec3 eye,WRVector3 p,WRVector3 d,int sign,float u,float t,float alpha){
  v.addVertex((float)(p.x+sign*d.x-eye.x),(float)(p.y+sign*d.y-eye.y),(float)(p.z+sign*d.z-eye.z)).setUv(u,t).setColor(COLORS[pass][type][0],COLORS[pass][type][1],COLORS[pass][type][2],alpha).setLight(0xF000F0);
 }
 /** Per pass and type: glow (p_large) and core (p_small) colours. */
 private static final float[][][] COLORS={
  {{.6f,.3f,.6f},{.6f,.6f,.1f},{.1f,.1f,.6f},{.1f,1f,.1f},{.6f,.1f,.1f},{.6f,.2f,.6f},{.75f,1f,1f}},
  {{1f,.6f,1f},{1f,1f,.1f},{.1f,.1f,1f},{.1f,.6f,.1f},{1f,.1f,.1f},{0,0,0},{.75f,1f,1f}}};
 /** new FXLightningBolt(..., seed, duration, multi, speed) with defaultFractal, setType and setWidth, then finalizeBolt. */
 public static void spawn(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z,double tx,double ty,double tz,int duration,float multi,int speed,int type,float width){
  long seed=level.random.nextLong();for(int pass=0;pass<2;pass++)net.minecraft.client.Minecraft.getInstance().particleEngine.add(new NodeBoltParticle(level,x,y,z,tx,ty,tz,seed,pass,duration,multi,speed,type,width));
 }
 public static void spawn(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z,double tx,double ty,double tz){
  long seed=level.random.nextLong();for(int pass=0;pass<2;pass++)net.minecraft.client.Minecraft.getInstance().particleEngine.add(new NodeBoltParticle(level,x,y,z,tx,ty,tz,seed,pass));
 }
}
