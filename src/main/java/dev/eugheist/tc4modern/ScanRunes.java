package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
/** Modern submission of the original FXBlockRunes scan particle and the thaumometer's client scan feedback. */
public final class ScanRunes extends Particle {
    private static final ParticleRenderType TYPE=new ParticleRenderType(){
        public BufferBuilder begin(Tesselator tessellator,net.minecraft.client.renderer.texture.TextureManager textures){com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();com.mojang.blaze3d.systems.RenderSystem.depthFunc(515);
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0,ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/misc/particles.png"));
            // ParticleEngine layer 0: additive, no depth writes.
            RenderSystem.enableBlend();RenderSystem.blendFunc(770,1);RenderSystem.depthMask(false);
            return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);
        }
    };
    private static final SoundEvent CAMERA_TICKS=SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"cameraticks"));
    private final float rotation,ofx,ofy,scale;
    private final int runeIndex;
    private float alphaNow;
    public ScanRunes(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z,float r,float g,float b,int m,float gravity){
        super(level,x,y,z);
        if(r==0)r=1;
        rotation=random.nextInt(4)*90;
        rCol=r;gCol=g;bCol=b;
        this.gravity=gravity;xd=yd=zd=0;
        lifetime=3*m;
        setSize(.01f,.01f);hasPhysics=false;
        runeIndex=(int)(Math.random()*16+224);
        ofx=random.nextFloat()*.2f;
        ofy=-.3f+random.nextFloat()*.6f;
        scale=(float)(1+random.nextGaussian()*.1f);
        alphaNow=0;
    }
    @Override public void tick(){
        xo=x;yo=y;zo=z;
        float threshold=lifetime/5f;
        alphaNow=age<=threshold?age/threshold:(float)(lifetime-age)/lifetime;
        if(age++>=lifetime)remove();
        yd-=.04*gravity;
        x+=xd;y+=yd;z+=zd;
    }
    @Override public ParticleRenderType getRenderType(){return TYPE;}
    @Override public void render(VertexConsumer vertices,net.minecraft.client.Camera camera,float partial){
        var eye=camera.getPosition();
        var pose=new PoseStack();
        pose.translate((float)(xo+(x-xo)*partial-eye.x),(float)(yo+(y-yo)*partial-eye.y),(float)(zo+(z-zo)*partial-eye.z));
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.mulPose(Axis.ZP.rotationDegrees(90));
        pose.translate(ofx,ofy,-.51f);
        var m=pose.last().pose();
        float u0=(runeIndex%16)/16f,u1=u0+.0624375f,v0=.375f,v1=v0+.0624375f,s=.3f*scale,a=alphaNow/2;
        vertices.addVertex(m,-.5f*s,.5f*s,0).setUv(u1,v1).setColor(rCol,gCol,bCol,a).setLight(240);
        vertices.addVertex(m,.5f*s,.5f*s,0).setUv(u1,v0).setColor(rCol,gCol,bCol,a).setLight(240);
        vertices.addVertex(m,.5f*s,-.5f*s,0).setUv(u0,v0).setColor(rCol,gCol,bCol,a).setLight(240);
        vertices.addVertex(m,-.5f*s,-.5f*s,0).setUv(u0,v1).setColor(rCol,gCol,bCol,a).setLight(240);
    }
    /** ClientProxy.blockRunes(world, x, y, z, r, g, b, dur, grav). */
    public static void blockRunes(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z,float r,float g,float b,int duration,float gravity){
        Minecraft.getInstance().particleEngine.add(new ScanRunes(level,x+.5,y+.5,z+.5,r,g,b,duration,gravity));
    }
    /** ClientProxy.blockRunes: the effect is centred half a block from the passed corner. */
    private static void blockRunes(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z,int duration){
        var rng=level.random;
        Minecraft.getInstance().particleEngine.add(new ScanRunes(level,x+.5,y+.5,z+.5,.3f+rng.nextFloat()*.7f,0,.3f+rng.nextFloat()*.7f,duration,.03f));
    }
    /**
     * Client half of ItemThaumometer.doScan/onUsingTick for a valid (not yet studied) target:
     * runes every tick and camera ticks on even remaining counts.
     */
    public static void scanTick(Player player,ScanTarget target,int remaining){
        if(!(player.level() instanceof net.minecraft.client.multiplayer.ClientLevel level)||player!=Minecraft.getInstance().player)return;
        if(ResearchKnowledge.isScanned(ClientKnowledge.knowledge,target.knowledgeKey()))return;
        String id=target.identity();
        if(id.startsWith("entity:")){
            var entity=level.getEntity(Integer.parseInt(id.substring(7)));
            if(entity!=null)blockRunes(level,entity.getX()-.5,entity.getY()+entity.getEyeHeight()/2,entity.getZ()-.5,(int)(entity.getBbHeight()*15));
        }else if(id.startsWith("item:")){
            if(Minecraft.getInstance().hitResult instanceof net.minecraft.world.phys.EntityHitResult hit){var e=hit.getEntity();blockRunes(level,e.getX()-.5,e.getY()+e.getEyeHeight()/2,e.getZ()-.5,(int)(e.getBbHeight()*15));}
        }else{
            BlockPos pos=id.startsWith("block:")?ScanTarget.blockPos(id):Minecraft.getInstance().hitResult instanceof net.minecraft.world.phys.BlockHitResult hit?hit.getBlockPos():null;
            if(pos!=null)blockRunes(level,pos.getX(),pos.getY()+.25,pos.getZ(),15);
        }
        if(remaining%2==0)level.playLocalSound(player.getX(),player.getY(),player.getZ(),CAMERA_TICKS,SoundSource.PLAYERS,.2f,.45f+level.random.nextFloat()*.1f,false);
    }
}
