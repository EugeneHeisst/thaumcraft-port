package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.particle.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
/** Original PacketFXShield directions and FXShieldRunes hemisphere, attached to the moving target. */
final class RunicShieldClient {
    static final ObjParts MODEL=new ObjParts(AlchemyContent.id("textures/models/hemis.obj"));
    static int received;
    static void receive(RunicRingShield.Impact packet){var mc=Minecraft.getInstance();if(mc.level==null)return;var source=mc.level.getEntity(packet.source());if(source==null)return;received++;
        if(packet.target()>=0){var target=mc.level.getEntity(packet.target());float yaw=0,pitch=90;if(target!=null){double dx=source.getX()-target.getX(),dz=source.getZ()-target.getZ(),dy=source.getBoundingBox().getCenter().y-target.getBoundingBox().getCenter().y;yaw=(float)(Math.atan2(dz,dx)*180/Math.PI)-90;pitch=(float)(-Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))*180/Math.PI);}add(source,yaw,pitch);}
        else {if(packet.target()!=-2)add(source,0,90);if(packet.target()!=-3)add(source,0,270);}
    }
    static void add(Entity source,float yaw,float pitch){Minecraft.getInstance().particleEngine.add(new Shield(source,yaw,pitch));}
    static final class Shield extends Particle {
        final Entity target;final float yaw,pitch;
        Shield(Entity target,float yaw,float pitch){super(Minecraft.getInstance().level,target.getX(),target.getBoundingBox().getCenter().y,target.getZ());this.target=target;this.yaw=yaw;this.pitch=pitch;lifetime=8+random.nextInt(4);hasPhysics=false;xd=yd=zd=0;}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
        @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime)remove();x=target.getX();y=target.getBoundingBox().getCenter().y;z=target.getZ();}
        @Override public void render(VertexConsumer unused,Camera camera,float partial){float fade=(age+partial)/lifetime;var pose=new PoseStack();var eye=camera.getPosition();pose.translate(Mth.lerp(partial,xo,x)-eye.x,Mth.lerp(partial,yo,y)-eye.y,Mth.lerp(partial,zo,z)-eye.z);pose.mulPose(Axis.YP.rotationDegrees(180-yaw));pose.mulPose(Axis.XP.rotationDegrees(-pitch));float size=.4f*target.getBbHeight();pose.scale(size,size,size);int frame=Math.min(15,(int)(14*fade)+1);var type=NodeRenderTypes.additive(AlchemyContent.id("textures/models/hemis"+frame+".png"));var buffers=Minecraft.getInstance().renderBuffers().bufferSource();MODEL.render("GeoSphere001",type,pose,buffers,220,0,(int)(Math.min(1,(1-fade)*3)*255)<<24|0xffffff);buffers.endBatch(type);}
    }
}
