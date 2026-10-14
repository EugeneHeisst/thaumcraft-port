package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.particle.*;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
/** Original FXWispEG and PacketFXSonic/FXSonic. */
final class GuardianClient {
 static int wisps,sonics,sonicFrames,orbBursts;
 static void orbBurst(Entity e){var mc=Minecraft.getInstance();var r=mc.level.random;for(int i=0;i<30;i++){float fx=(r.nextFloat()-r.nextFloat())*.3f,fy=(r.nextFloat()-r.nextFloat())*.3f,fz=(r.nextFloat()-r.nextFloat())*.3f;var particle=new PrimalWisp(mc.level,e.getX()+fx,e.getY()+fy,e.getZ()+fz,.3f,5,null,.02f,false);particle.towards(e.getX()+fx*8,e.getY()+fy*8,e.getZ()+fz*8);mc.particleEngine.add(particle);}orbBursts++;}
 static void wisp(EldritchGuardian g){var r=g.getRandom();Minecraft.getInstance().particleEngine.add(new Wisp(g,g.getX()+(r.nextFloat()-r.nextFloat())*.2,g.getY()+.22*g.getBbHeight(),g.getZ()+(r.nextFloat()-r.nextFloat())*.2));wisps++;}
 static void sonic(int id){var mc=Minecraft.getInstance();if(mc.level==null)return;var e=mc.level.getEntity(id);if(e!=null){mc.particleEngine.add(new Sonic(e));sonics++;}}
 static final class Sonic extends Particle {
  final Entity target;final float yaw,pitch;
  Sonic(Entity e){super(Minecraft.getInstance().level,e.getX(),e.getEyeY(),e.getZ());target=e;yaw=e.getYHeadRot();pitch=e.getXRot();lifetime=10+random.nextInt(5);hasPhysics=false;xd=yd=zd=0;}
  @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
  @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime)remove();x=target.getX();y=target.getEyeY();z=target.getZ();}
  @Override public void render(VertexConsumer unused,Camera camera,float partial){sonicFrames++;float fade=(age+partial)/lifetime;var pose=new PoseStack();var eye=camera.getPosition();pose.translate(Mth.lerp(partial,xo,x)-eye.x,Mth.lerp(partial,yo,y)-eye.y,Mth.lerp(partial,zo,z)-eye.z);pose.mulPose(Axis.YP.rotationDegrees(-yaw));pose.mulPose(Axis.XP.rotationDegrees(pitch));pose.translate(0,0,2*target.getBbHeight()+target.getBbWidth()/2);float h=target.getBbHeight();pose.scale(.25f*h,.25f*h,-h);int frame=Math.min(15,(int)(14*fade)+1);var type=NodeRenderTypes.additive(AlchemyContent.id("textures/models/ripple"+frame+".png"));var buffers=Minecraft.getInstance().renderBuffers().bufferSource();RunicShieldClient.MODEL.renderAll(type,pose,buffers,220,0,0xff808080);buffers.endBatch(type);}
 }
 static final class Wisp extends Particle {
  final Entity target;final float size;
  Wisp(Entity target,double x,double y,double z){super(Minecraft.getInstance().level,x,y,z);this.target=target;xd=random.nextGaussian()*.03;yd=-.05;zd=random.nextGaussian()*.03;size=(random.nextFloat()*.5f+.5f)*.8f;lifetime=(int)(40/(Math.random()*.3+.7));hasPhysics=true;setSize(.01f,.01f);rCol=random.nextFloat()*.05f;gCol=random.nextFloat()*.05f;bCol=random.nextFloat()*.05f;var camera=Minecraft.getInstance().getCameraEntity();int distance=Minecraft.useFancyGraphics()?50:25;if(camera!=null&&camera.distanceToSqr(x,y,z)>distance*distance)lifetime=0;}
  @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
  @Override public void tick(){xo=x;yo=y;zo=z;if(!onGround)setPos(x+target.getDeltaMovement().x,y,z+target.getDeltaMovement().z);pushOut();move(xd,yd,zd);xd*=.98f;yd*=.98f;zd*=.98f;if(onGround){xd*=.85f;zd*=.85f;}if(age++>=lifetime)remove();}
  void pushOut(){var p=BlockPos.containing(x,y,z);if(level.isEmptyBlock(p)||!level.getBlockState(p).isRedstoneConductor(level,p)||level.containsAnyLiquid(getBoundingBox()))return;Direction selected=null;double nearest=9999;for(var d:new Direction[]{Direction.WEST,Direction.EAST,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH}){var n=p.relative(d);if(level.getBlockState(n).isRedstoneConductor(level,n))continue;double f=switch(d.getAxis()){case X->x-p.getX();case Y->y-p.getY();case Z->z-p.getZ();};double dist=d.getAxisDirection()==Direction.AxisDirection.NEGATIVE?f:1-f;if(dist<nearest){nearest=dist;selected=d;}}float speed=random.nextFloat()*.05f+.025f,other=(random.nextFloat()-random.nextFloat())*.1f;if(selected!=null){xd=yd=zd=other;switch(selected.getAxis()){case X->xd=selected.getStepX()*speed;case Y->yd=selected.getStepY()*speed;case Z->zd=selected.getStepZ()*speed;}}}
  @Override public void render(VertexConsumer unused,Camera camera,float partial){float ageScale=1-age/(float)Math.max(1,lifetime);var eye=camera.getPosition();float base=(float)(1-Math.min(1024,eye.distanceToSqr(x,y,z))/1024);int alpha=(int)(255*.15f*ageScale*base*base),color=alpha<<24|(int)(rCol*255)<<16|(int)(gCol*255)<<8|(int)(bCol*255);float u=(age%13)/16f,v=3/16f;var type=NodeRenderTypes.translucent(AlchemyContent.id("textures/misc/particles.png"));var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var out=buffers.getBuffer(type);float xx=(float)(Mth.lerp(partial,xo,x)-eye.x),yy=(float)(Mth.lerp(partial,yo,y)-eye.y),zz=(float)(Mth.lerp(partial,zo,z)-eye.z);float[][] corners={{-1,-1,u+.0624375f,v+.0624375f},{-1,1,u+.0624375f,v},{1,1,u,v},{1,-1,u,v+.0624375f}};for(var q:corners){var vec=new Vector3f(q[0]*size*.5f,q[1]*size*.5f,0).rotate(camera.rotation());out.addVertex(xx+vec.x,yy+vec.y,zz+vec.z).setUv(q[2],q[3]).setColor(color);}buffers.endBatch(type);}
 }
}
