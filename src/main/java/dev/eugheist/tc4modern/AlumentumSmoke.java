package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.*;

/** Particle-engine submission for original FXWisp type 5 (translucent smoke, not an item mesh). */
public final class AlumentumSmoke extends Particle {
    private final thaumcraft.common.tiles.NitorWisp state;
    private static final ParticleRenderType TYPE=new ParticleRenderType(){
        public BufferBuilder begin(Tesselator tessellator,net.minecraft.client.renderer.texture.TextureManager textures){
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/misc/particles.png"));
            RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.depthMask(false);
            return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);
        }
    };
    public AlumentumSmoke(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z){
        super(level,x,y,z);
        var rng=new java.util.random.RandomGenerator(){public long nextLong(){return random.nextLong();}public float nextFloat(){return random.nextFloat();}};
        state=new thaumcraft.common.tiles.NitorWisp(rng,.5,.5,.3f,5,.02f);
        state.x=state.previousX=x;state.y=state.previousY=y;state.z=state.previousZ=z;
        setSize(.1f,.1f);hasPhysics=false;
    }
    @Override public void tick(){
        if(!state.tick()){remove();return;}
        x=state.x;y=state.y;z=state.z;xo=state.previousX;yo=state.previousY;zo=state.previousZ;
        setPos(x,y,z);
    }
    @Override public ParticleRenderType getRenderType(){return TYPE;}
    private static boolean loggedRender;
    @Override public void render(VertexConsumer vertices,net.minecraft.client.Camera camera,float partial){
        if(Boolean.getBoolean("tc4modern.debugAlumentum")&&!loggedRender){loggedRender=true;org.slf4j.LoggerFactory.getLogger("tc4modern.Alumentum").info("AlumentumSmoke render pos=({}, {}, {}) eye={} box={}",x,y,z,camera.getPosition(),getBoundingBox());}
        var eye=camera.getPosition();
        float cx=(float)(xo+(x-xo)*partial-eye.x),cy=(float)(yo+(y-yo)*partial-eye.y),cz=(float)(zo+(z-zo)*partial-eye.z);
        float radius=state.radius();float[][] corners={{-1,-1,.125f,1},{-1,1,.125f,.875f},{1,1,0,.875f},{1,-1,0,1}};
        for(var c:corners){var o=new org.joml.Vector3f(-c[0]*radius,c[1]*radius,0).rotate(camera.rotation());
            vertices.addVertex(cx+o.x,cy+o.y,cz+o.z).setUv(c[2],c[3]).setColor(state.red,state.green,state.blue,.5f).setLight(240);
        }
    }
    private static boolean loggedTrail;
    public static void trail(thaumcraft.common.entities.projectile.EntityAlumentum entity){
        if(Boolean.getBoolean("tc4modern.debugAlumentum")&&!loggedTrail){loggedTrail=true;org.slf4j.LoggerFactory.getLogger("tc4modern.Alumentum").info("client trail {} pos={} velocity={} visible={}",entity.getId(),entity.position(),entity.getDeltaMovement(),NitorEntity.visible.test(entity.blockPosition()));}
        var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level==null||!NitorEntity.visible.test(entity.blockPosition()))return;
        var random=entity.level().random;
        for(int i=0;i<3;i++){
            mc.particleEngine.add(new AlumentumSmoke(mc.level,entity.getX()+(random.nextFloat()-random.nextFloat())*.3f,entity.getY()+(random.nextFloat()-random.nextFloat())*.3f,entity.getZ()+(random.nextFloat()-random.nextFloat())*.3f));
            mc.particleEngine.add(new AlumentumSmoke(mc.level,(entity.getX()+entity.xo)/2+(random.nextFloat()-random.nextFloat())*.3f,(entity.getY()+entity.yo)/2+(random.nextFloat()-random.nextFloat())*.3f,(entity.getZ()+entity.zo)/2+(random.nextFloat()-random.nextFloat())*.3f));
            float sx=(float)entity.getX()+(random.nextFloat()-random.nextFloat())*.1f;
            float sy=(float)entity.getY()+(random.nextFloat()-random.nextFloat())*.1f;
            float sz=(float)entity.getZ()+(random.nextFloat()-random.nextFloat())*.1f;
            int setting=NitorEntity.particleSetting.getAsInt(),count=setting==0?4:setting==1?2:0;
            if(random.nextInt(6)<count)mc.particleEngine.add(new AlumentumSpark(mc.level,sx,sy,sz));
        }
    }
}
