package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/** FXWisp constructors used by primal orbs: shrink, target attraction, normal/additive layers and block escape. */
final class PrimalWisp extends Particle {
    private final Entity target;
    private final float initialSize,gravityValue;
    private final boolean clip,dark;
    private static final ParticleRenderType NORMAL=new ParticleRenderType(){
        @Override public BufferBuilder begin(Tesselator t,net.minecraft.client.renderer.texture.TextureManager textures){com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();com.mojang.blaze3d.systems.RenderSystem.depthFunc(515);
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);RenderSystem.setShaderTexture(0,FociFx.PARTICLES);
            RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.depthMask(false);
            return t.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);
        }
    };
    PrimalWisp(ClientLevel level,double x,double y,double z,float size,int type,Entity target,float gravityValue,boolean clip){
        super(level,x,y,z);this.target=target;this.gravityValue=gravityValue;this.clip=clip;dark=type==5;
        xd=yd=zd=0;initialSize=(random.nextFloat()*.5f+.5f)*2*size;lifetime=(int)(36/(Math.random()*.3+.7));hasPhysics=!clip;setSize(.1f,.1f);
        switch(type){
            case 0->setColor(.75f+level.random.nextFloat()*.25f,.25f+level.random.nextFloat()*.25f,.75f+level.random.nextFloat()*.25f);
            case 1->setColor(.5f+level.random.nextFloat()*.3f,.5f+level.random.nextFloat()*.3f,.2f);
            case 2->setColor(.2f,.2f,.7f+level.random.nextFloat()*.3f);
            case 3->setColor(.2f,.7f+level.random.nextFloat()*.3f,.2f);
            case 4->setColor(.7f+level.random.nextFloat()*.3f,.2f,.2f);
            case 5->setColor(level.random.nextFloat()*.1f,level.random.nextFloat()*.1f,level.random.nextFloat()*.1f);
        }
        var viewer=Minecraft.getInstance().getCameraEntity();int range=Minecraft.useFancyGraphics()?50:25;
        if(viewer!=null&&viewer.distanceToSqr(x,y,z)>range*range)lifetime=0;
    }
    void towards(double tx,double ty,double tz){if(lifetime>0){xd=(tx-x)/lifetime;yd=(ty-y)/lifetime;zd=(tz-z)/lifetime;}}
    @Override public ParticleRenderType getRenderType(){return dark?NORMAL:FociFx.ADDITIVE;}
    @Override public void tick(){
        xo=x;yo=y;zo=z;if(age++>=lifetime)remove();yd-=.04*gravityValue;
        if(!clip)escape();move(xd,yd,zd);
        if(target!=null){
            xd*=.985;yd*=.985;zd*=.985;
            double dx=target.getX()-x,dy=target.getY()+target.getBbHeight()/2f-y,dz=target.getZ()-z,d=Math.sqrt(dx*dx+dy*dy+dz*dz);
            xd=Mth.clamp((float)(xd+dx/d*.2),-.2f,.2f);yd=Mth.clamp((float)(yd+dy/d*.2),-.2f,.2f);zd=Mth.clamp((float)(zd+dz/d*.2),-.2f,.2f);
        }else{xd*=.98f;yd*=.98f;zd*=.98f;if(onGround){xd*=.7f;zd*=.7f;}}
    }
    private void escape(){
        var p=BlockPos.containing(x,y,z);var s=level.getBlockState(p);
        if(s.isAir()||!s.isSolidRender(level,p)||level.containsAnyLiquid(getBoundingBox()))return;
        double fx=x-p.getX(),fy=y-p.getY(),fz=z-p.getZ(),best=9999;Direction direction=null;
        // Original tie order: west, east, down, up, north, south.
        for(var d:new Direction[]{Direction.WEST,Direction.EAST,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH}){
            var at=p.relative(d);if(level.getBlockState(at).isSolidRender(level,at))continue;
            double distance=switch(d){case WEST->fx;case EAST->1-fx;case DOWN->fy;case UP->1-fy;case NORTH->fz;case SOUTH->1-fz;};
            if(distance<best){best=distance;direction=d;}
        }
        float speed=random.nextFloat()*.05f+.025f,jitter=(random.nextFloat()-random.nextFloat())*.1f;
        if(direction!=null){xd=yd=zd=jitter;switch(direction.getAxis()){case X->xd=speed*direction.getStepX();case Y->yd=speed*direction.getStepY();case Z->zd=speed*direction.getStepZ();}}
    }
    @Override public void render(VertexConsumer out,Camera camera,float partial){
        if(lifetime<=0)return;float half=.5f*initialSize*(lifetime-age)/(float)lifetime;
        var eye=camera.getPosition();float cx=(float)(Mth.lerp(partial,xo,x)-eye.x),cy=(float)(Mth.lerp(partial,yo,y)-eye.y),cz=(float)(Mth.lerp(partial,zo,z)-eye.z);
        float[][] corners={{-1,-1,.125f,1},{-1,1,.125f,.875f},{1,1,0,.875f},{1,-1,0,1}};
        for(var c:corners){var v=new Vector3f(c[0]*half,c[1]*half,0).rotate(camera.rotation());out.addVertex(cx+v.x,cy+v.y,cz+v.z).setUv(c[2],c[3]).setColor(rCol,gCol,bCol,.375f).setLight(240);}
    }
}
