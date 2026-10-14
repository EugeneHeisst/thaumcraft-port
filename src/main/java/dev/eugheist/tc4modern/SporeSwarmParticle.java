package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.*;
import thaumcraft.common.entities.monster.EntityTaintSpore;
/** Modern particle submission and target binding for original FXSwarm. */
public final class SporeSwarmParticle extends Particle {
    private final net.minecraft.world.entity.LivingEntity target;
    private final float speed,turn;
    private final thaumcraft.client.fx.particles.SwarmMotion motion=new thaumcraft.client.fx.particles.SwarmMotion();
    private final thaumcraft.client.fx.particles.SwarmEscape escape=new thaumcraft.client.fx.particles.SwarmEscape();
    private final float scale;
    private static final java.util.WeakHashMap<net.minecraft.world.entity.LivingEntity,java.util.ArrayList<SporeSwarmParticle>> SWARMS=new java.util.WeakHashMap<>();
    private static final ParticleRenderType TYPE=new ParticleRenderType(){
        public BufferBuilder begin(Tesselator tessellator,net.minecraft.client.renderer.texture.TextureManager textures){
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/misc/particles.png"));
            RenderSystem.enableBlend();RenderSystem.blendFunc(770,771);RenderSystem.depthMask(false);
            return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);
        }
    };
    private final java.util.random.RandomGenerator rng=new java.util.random.RandomGenerator(){public long nextLong(){return random.nextLong();}public int nextInt(int bound){return random.nextInt(bound);}public float nextFloat(){return random.nextFloat();}public double nextGaussian(){return random.nextGaussian();}public boolean nextBoolean(){return random.nextBoolean();}};
    public SporeSwarmParticle(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z,net.minecraft.world.entity.LivingEntity target,float red,float green,float blue,float speed,float turn,float gravity){
        super(level,x,y,z);this.target=target;this.speed=speed;this.turn=turn;rCol=red;gCol=green;bCol=blue;
        scale=random.nextFloat()*.5f+1;
        xd=(random.nextFloat()-random.nextFloat())*.2f;yd=(random.nextFloat()-random.nextFloat())*.2f;zd=(random.nextFloat()-random.nextFloat())*.2f;
        this.gravity=gravity;hasPhysics=true;
        var mc=net.minecraft.client.Minecraft.getInstance();int visible=mc.options.graphicsMode().get()!=net.minecraft.client.GraphicsStatus.FAST?64:32;
        if(mc.player!=null&&mc.player.distanceToSqr(x,y,z)>visible*visible)remove();
    }
    public static void update(EntityTaintSpore target){update(target,target.getSporeSize()/3,.1f,10,0);}
    public static void updateSwarmer(thaumcraft.common.entities.monster.EntityTaintSporeSwarmer target){update(target,(500-target.spawnCounter)/25,.1f,10,0);}
    public static void updateSwarm(thaumcraft.common.entities.monster.EntityTaintSwarm target){
        int setting=net.minecraft.client.Minecraft.getInstance().options.particles().get().getId();
        update(target,Math.max(setting==2?0:setting==1?25:50,10),.22f,15,.08f);
    }
    public static void clear(net.minecraft.world.entity.LivingEntity target){var list=SWARMS.remove(target);if(list!=null)for(var particle:list)particle.remove();}
    private static void update(net.minecraft.world.entity.LivingEntity target,int desired,float speed,float turn,float gravity){
        var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level==null)return;
        var swarm=SWARMS.computeIfAbsent(target,key->new java.util.ArrayList<>());
        for(int i=0;i<swarm.size();i++)if(!swarm.get(i).isAlive()){swarm.remove(i);break;}
        if(swarm.size()<desired){
            var r=mc.level.random;
            var particle=new SporeSwarmParticle(mc.level,target.getX()+(r.nextFloat()-r.nextFloat())*2,target.getY()+(r.nextFloat()-r.nextFloat())*2,target.getZ()+(r.nextFloat()-r.nextFloat())*2,target,.8f+r.nextFloat()*.2f,r.nextFloat()*.4f,1-r.nextFloat()*.2f,speed,turn,gravity);
            swarm.add(particle);mc.particleEngine.add(particle);
        }
    }
    @Override public void tick(){
        xo=x;yo=y;zo=z;age++;
        boolean missing=target.isRemoved()||target.deathTime>0;
        if(missing)SWARMS.remove(target);
        motion.vx=xd;motion.vy=yd;motion.vz=zd;
        if(missing){motion.targetMissing(gravity);yd=motion.vy;if(!motion.alive)remove();}else yd+=gravity;
        escape.vx=xd;escape.vy=yd;escape.vz=zd;
        escape.pushOutOfBlocks(x,y,z,new thaumcraft.client.fx.particles.SwarmEscape.World(){
            public boolean fibres(int bx,int by,int bz){return level.getBlockState(new net.minecraft.core.BlockPos(bx,by,bz)).is(TC4Modern.TAINT_FIBRES.get());}
            public boolean air(int bx,int by,int bz){return level.isEmptyBlock(new net.minecraft.core.BlockPos(bx,by,bz));}
            public boolean liquidInBounds(){
                var box=getBoundingBox();
                int minX=net.minecraft.util.Mth.floor(box.minX),minY=net.minecraft.util.Mth.floor(box.minY),minZ=net.minecraft.util.Mth.floor(box.minZ);
                if(box.minX<0)minX--;if(box.minY<0)minY--;if(box.minZ<0)minZ--;
                for(int bx=minX;bx<net.minecraft.util.Mth.floor(box.maxX+1);bx++)
                    for(int by=minY;by<net.minecraft.util.Mth.floor(box.maxY+1);by++)
                        for(int bz=minZ;bz<net.minecraft.util.Mth.floor(box.maxZ+1);bz++)
                            if(level.getBlockState(new net.minecraft.core.BlockPos(bx,by,bz)).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)return true;
                return false;
            }
            public boolean opaqueNormalCube(int bx,int by,int bz){
                var pos=new net.minecraft.core.BlockPos(bx,by,bz);
                if(bx< -30000000||bz< -30000000||bx>=30000000||bz>=30000000||!level.hasChunkAt(pos))return true;
                return level.getBlockState(pos).isSolidRender(level,pos);
            }
        },rng);
        xd=escape.vx;yd=escape.vy;zd=escape.vz;
        move(xd,yd,zd);xd*=.985;yd*=.985;zd*=.985;
        if(!missing){
            double dx=target.getX()-x,dz=target.getZ()-z;
            double dy=(target.getBoundingBox().minY+target.getBoundingBox().maxY)/2-(getBoundingBox().minY+getBoundingBox().maxY)/2;
            double distance=(target.getX()-x)*(target.getX()-x)+(target.getY()-y)*(target.getY()-y)+dz*dz;
            motion.steer(dx,dy,dz,distance,target.getBbWidth(),target.hurtTime>0,turn,speed,rng);
            xd=motion.vx;yd=motion.vy;zd=motion.vz;
        }
        thaumcraft.client.fx.particles.SwarmBuzz.tick(rng,
            ()->level.getNearestPlayer(x,y,z,8,false)!=null,
            pitch->level.playLocalSound(x,y,z,TC4Modern.FLY.get(),net.minecraft.sounds.SoundSource.NEUTRAL,.03f,(float)pitch,false));
    }
    @Override public ParticleRenderType getRenderType(){return TYPE;}
    @Override public void render(VertexConsumer vertices,net.minecraft.client.Camera camera,float partial){
        var eye=camera.getPosition();float cx=(float)(xo+(x-xo)*partial-eye.x),cy=(float)(yo+(y-yo)*partial-eye.y),cz=(float)(zo+(z-zo)*partial-eye.z);
        float u=thaumcraft.client.fx.particles.SwarmMotion.sprite(age)/16f,radius=thaumcraft.client.fx.particles.SwarmMotion.radius(age,scale);
        float green=target.hurtTime<=0?gCol:gCol/2,blue=target.hurtTime<=0?bCol:bCol/2;
        float[][] corners={{-1,-1,u+.0624375f,.3124375f},{-1,1,u+.0624375f,.25f},{1,1,u,.25f},{1,-1,u,.3124375f}};
        for(var c:corners){var offset=new org.joml.Vector3f(-c[0]*radius,c[1]*radius,0).rotate(camera.rotation());vertices.addVertex(cx+offset.x,cy+offset.y,cz+offset.z).setUv(c[2],c[3]).setColor(rCol,green,blue,motion.alpha()).setLight(240);}
    }
}
