package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.*;
/** Original FXBreaking slimeball fragments, including shrinking radius and alpha. */
public final class SporeSplooshParticle extends Particle {
    private final float scale,u,v;
    private static final ParticleRenderType TYPE=new ParticleRenderType(){
        public BufferBuilder begin(Tesselator tessellator,net.minecraft.client.renderer.texture.TextureManager textures){
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/misc/original_slimeball.png"));
            RenderSystem.enableBlend();RenderSystem.blendFunc(770,771);RenderSystem.depthMask(false);
            return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);
        }
    };
    public SporeSplooshParticle(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z){this(level,x,y,z,false);}
    private SporeSplooshParticle(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z,boolean landing){
        this(level,x,y,z,landing,false);
    }
    private SporeSplooshParticle(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z,boolean landing,boolean infested){
        super(level,x,y,z);
        u=random.nextFloat()*3/4;v=random.nextFloat()*3/4;
        float baseScale=(random.nextFloat()*.5f+.5f)*2;
        random.nextFloat(); // Original EntityFX default lifetime is subsequently overridden.
        double vx=(float)(Math.random()*2-1)*.4f,vy=(float)(Math.random()*2-1)*.4f,vz=(float)(Math.random()*2-1)*.4f;
        float magnitude=(float)(Math.random()+Math.random()+1)*.15f;
        float length=net.minecraft.util.Mth.sqrt((float)(vx*vx+vy*vy+vz*vz));
        xd=vx/length*magnitude*(double).4f;yd=vy/length*magnitude*(double).4f+(double).1f;zd=vz/length*magnitude*(double).4f;
        boolean bright=!landing&&level.random.nextBoolean();float multiplier=infested?.4f:landing?.4f:bright?.4f:.6f;
        rCol=infested?.7f:landing?.1f:bright?.6f:.3f;gCol=0;bCol=infested?1:landing?.1f:.3f;scale=baseScale/2*multiplier;
        setSize(.2f*multiplier,.2f*multiplier);gravity=1;hasPhysics=true;
        lifetime=(int)(66f/(level.random.nextFloat()*.9f+.1f));
    }
    public static void infested(net.minecraft.world.entity.Entity entity){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level!=null)mc.particleEngine.add(new SporeSplooshParticle(mc.level,entity.getX(),entity.getBoundingBox().getCenter().y,entity.getZ(),false,true));}
    public static void land(thaumcraft.common.entities.EntityFallingTaint entity){
        var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level==null)return;
        var r=mc.level.random;float angle=r.nextFloat()*(float)Math.PI*2,radius=r.nextFloat()*.5f+.5f;
        mc.particleEngine.add(new SporeSplooshParticle(mc.level,entity.getX()+net.minecraft.util.Mth.sin(angle)*radius,(entity.getBoundingBox().minY+entity.getBoundingBox().maxY)/2,entity.getZ()+net.minecraft.util.Mth.cos(angle)*radius,true));
    }
    public static void burst(thaumcraft.common.entities.monster.EntityTaintSpore entity){burst(entity,50);}
    public static void burst(net.minecraft.world.entity.Entity entity,int count){
        var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level==null)return;
        for(int i=0;i<count;i++){
            var r=mc.level.random;float angle=r.nextFloat()*(float)Math.PI*2,radius=r.nextFloat()*.5f+.5f;
            mc.particleEngine.add(new SporeSplooshParticle(mc.level,entity.getX()+net.minecraft.util.Mth.sin(angle)*radius,entity.getY()+r.nextFloat()*entity.getBbHeight(),entity.getZ()+net.minecraft.util.Mth.cos(angle)*radius));
        }
    }
    @Override public void tick(){
        xo=x;yo=y;zo=z;if(age++>=lifetime)remove();yd-=.04*(double)gravity;
        move(xd,yd,zd);xd*=(double).98f;yd*=(double).98f;zd*=(double).98f;
        if(onGround){xd*=(double).7f;zd*=(double).7f;}
    }
    @Override public ParticleRenderType getRenderType(){return TYPE;}
    @Override public void render(VertexConsumer vertices,net.minecraft.client.Camera camera,float partial){
        var eye=camera.getPosition();float cx=(float)(xo+(x-xo)*partial-eye.x),cy=(float)(yo+(y-yo)*partial-eye.y),cz=(float)(zo+(z-zo)*partial-eye.z),fade=1-age/(float)lifetime,radius=.1f*scale*fade;
        float[][] corners={{-1,-1,u,v+.25f},{-1,1,u,v},{1,1,u+.25f,v},{1,-1,u+.25f,v+.25f}};
        for(var c:corners){var o=new org.joml.Vector3f(-c[0]*radius,c[1]*radius,0).rotate(camera.rotation());vertices.addVertex(cx+o.x,cy+o.y,cz+o.z).setUv(c[2],c[3]).setColor(rCol,gCol,bCol,fade).setLight(getLightColor(partial));}
    }
}
