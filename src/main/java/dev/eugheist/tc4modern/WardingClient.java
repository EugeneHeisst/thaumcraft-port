package dev.eugheist.tc4modern;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.*;
import net.minecraft.util.*;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.*;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
import java.util.function.Predicate;

/** REHWandHandler architect overlay, TileWardedRenderer and FXBlockWard. */
public final class WardingClient {
    private WardingClient() {}
    static int architectRendered;
    public static final KeyMapping KEY_G = new KeyMapping("key.tc4modern.misc_toggle", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_G, "key.categories.misc");
    private static final int[] CONNECTED = new int[]{0, 0, 6, 6, 0, 0, 6, 6, 3, 3, 19, 15, 3, 3, 19, 15, 1, 1, 18, 18, 1, 1, 13, 13, 2, 2, 23, 31, 2, 2, 27, 14, 0, 0, 6, 6, 0, 0, 6, 6, 3, 3, 19, 15, 3, 3, 19, 15, 1, 1, 18, 18, 1, 1, 13, 13, 2, 2, 23, 31, 2, 2, 27, 14, 4, 4, 5, 5, 4, 4, 5, 5, 17, 17, 22, 26, 17, 17, 22, 26, 16, 16, 20, 20, 16, 16, 28, 28, 21, 21, 46, 42, 21, 21, 43, 38, 4, 4, 5, 5, 4, 4, 5, 5, 9, 9, 30, 12, 9, 9, 30, 12, 16, 16, 20, 20, 16, 16, 28, 28, 25, 25, 45, 37, 25, 25, 40, 32, 0, 0, 6, 6, 0, 0, 6, 6, 3, 3, 19, 15, 3, 3, 19, 15, 1, 1, 18, 18, 1, 1, 13, 13, 2, 2, 23, 31, 2, 2, 27, 14, 0, 0, 6, 6, 0, 0, 6, 6, 3, 3, 19, 15, 3, 3, 19, 15, 1, 1, 18, 18, 1, 1, 13, 13, 2, 2, 23, 31, 2, 2, 27, 14, 4, 4, 5, 5, 4, 4, 5, 5, 17, 17, 22, 26, 17, 17, 22, 26, 7, 7, 24, 24, 7, 7, 10, 10, 29, 29, 44, 41, 29, 29, 39, 33, 4, 4, 5, 5, 4, 4, 5, 5, 9, 9, 30, 12, 9, 9, 30, 12, 7, 7, 24, 24, 7, 7, 10, 10, 8, 8, 36, 35, 8, 8, 34, 11};
    @EventBusSubscriber(modid=TC4Modern.MODID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
    static final class ModEvents {
        @SubscribeEvent static void keys(RegisterKeyMappingsEvent e) { e.register(KEY_G); }
        @SubscribeEvent static void renderers(EntityRenderersEvent.RegisterRenderers e) { e.registerBlockEntityRenderer(Warding.WARDED_ENTITY.get(), Renderer::new); }
        @SubscribeEvent static void extensions(RegisterClientExtensionsEvent e) {
            e.registerBlock(new IClientBlockExtensions() {
                @Override public boolean addHitEffects(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, HitResult target, net.minecraft.client.particle.ParticleEngine manager) {
                    if (target instanceof BlockHitResult hit && level instanceof net.minecraft.client.multiplayer.ClientLevel client)
                        manager.add(new WardParticle(client, hit));
                    return true;
                }
                @Override public boolean addDestroyEffects(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.client.particle.ParticleEngine manager) { return true; }
            }, Warding.WARDED.get());
        }
    }
    @EventBusSubscriber(modid=TC4Modern.MODID, value=Dist.CLIENT)
    static final class Events {
        @SubscribeEvent static void tick(ClientTickEvent.Post e) {
            var mc=Minecraft.getInstance();
            while (KEY_G.consumeClick()) if (mc.screen==null && mc.player!=null && (WandData.isWand(mc.player.getMainHandItem())||mc.player.getMainHandItem().getItem() instanceof ElementalShovel)) PacketDistributor.sendToServer(new Warding.MiscToggle());
        }
        @SubscribeEvent static void overlay(RenderHighlightEvent.Block e) {
            var mc=Minecraft.getInstance(); if(mc.player==null) return;
            var wand=mc.player.getMainHandItem(); var focus=FociContent.getFocus(wand);
            Warding.Architect architect;if(wand.getItem() instanceof Warding.Architect a)architect=a;else if(focus instanceof Warding.Architect a&&focus.isUpgradedWith(FociContent.getFocusItem(wand),FociContent.ARCHITECT))architect=a;else return;
            var hit=e.getTarget(); int side=hit.getDirection().get3DDataValue(); var origin=hit.getBlockPos();
            var blocks=new HashSet<>(architect.architectBlocks(wand,mc.level,origin,side,mc.player)); if(blocks.isEmpty()) return;architectRendered++;
            var pose=e.getPoseStack(); var eye=e.getCamera().getPosition(); var buffers=e.getMultiBufferSource();
            for(var pos:blocks) {
                pose.pushPose(); pose.translate(pos.getX()-eye.x,pos.getY()-eye.y,pos.getZ()-eye.z);
                glass(pose,buffers,pos,blocks::contains,true,color(mc.player.tickCount,pos,.3f,.3f,.8f,.2f),blocks::contains);
                pose.popPose();
            }
            pose.pushPose(); pose.translate(origin.getX()+.5-eye.x,origin.getY()+.5-eye.y,origin.getZ()+.5-eye.z);
            int c=color(mc.player.tickCount,origin,.3f,.3f,.8f,1);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            if(architect.showAxis(wand,mc.level,mc.player,side,Warding.Axis.X)) arrows(pose,buffers,c,0);
            if(architect.showAxis(wand,mc.level,mc.player,side,Warding.Axis.Z)) arrows(pose,buffers,c,1);
            if(architect.showAxis(wand,mc.level,mc.player,side,Warding.Axis.Y)) arrows(pose,buffers,c,2);
            pose.popPose(); e.setCanceled(true);
        }
    }
    private static void arrows(PoseStack pose,MultiBufferSource buffers,int color,int axis) {
        pose.pushPose();
        if(axis==1) pose.mulPose(Axis.ZP.rotationDegrees(90));
        if(axis==2) pose.mulPose(Axis.YP.rotationDegrees(90));
        var out=buffers.getBuffer(NodeRenderTypes.additiveThrough(AlchemyContent.id("textures/misc/architect_arrows.png")));
        for(int i=0;i<2;i++) { quad(pose,out,new float[][]{{-.5f,-.5f,0},{.5f,-.5f,0},{.5f,.5f,0},{-.5f,.5f,0}},0,1,0,1,color);pose.mulPose(Axis.XP.rotationDegrees(90)); }
        pose.popPose();
    }
    private static int color(int ticks,BlockPos p,float r,float g,float b,float alpha) {
        int rr=(int)((Mth.sin(ticks/2f+p.getX())*.2f+r)*255), gg=(int)((Mth.sin(ticks/3f+p.getY())*.2f+g)*255), bb=(int)((Mth.sin(ticks/4f+p.getZ())*.2f+b)*255);
        return (int)(alpha*255)<<24 | Mth.clamp(rr,0,255)<<16 | Mth.clamp(gg,0,255)<<8 | Mth.clamp(bb,0,255);
    }
    static int texture(BlockPos pos,int side,Predicate<BlockPos> connected) {
        int mask=0,i=0;
        for(int row=-1;row<=1;row++) for(int col=-1;col<=1;col++) {
            if(row==0&&col==0) continue;
            BlockPos at=side<2?pos.offset(col,0,row):side<4?pos.offset(col*(side==2?-1:1),-row,0):pos.offset(0,-row,col*(side==5?-1:1));
            if(connected.test(at)) mask|=1<<i; i++;
        }
        return CONNECTED[mask]+1;
    }
    private static final float L=-.001f,H=1.001f;
    private static final float[][][] FACES={
        {{L,L,H},{H,L,H},{H,L,L},{L,L,L}},{{L,H,L},{H,H,L},{H,H,H},{L,H,H}},
        {{H,L,L},{L,L,L},{L,H,L},{H,H,L}},{{L,L,H},{H,L,H},{H,H,H},{L,H,H}},
        {{L,L,L},{L,L,H},{L,H,H},{L,H,L}},{{H,L,H},{H,L,L},{H,H,L},{H,H,H}}
    };
    static void glass(PoseStack pose,MultiBufferSource buffers,BlockPos pos,Predicate<BlockPos> connected,boolean through,int color,Predicate<BlockPos> hidden) {
        glass(pose,buffers,pos,connected,through,color,hidden,through?NodeRenderTypes.additiveThrough(TextureAtlas.LOCATION_BLOCKS):NodeRenderTypes.additive(TextureAtlas.LOCATION_BLOCKS));
    }
    static void glass(PoseStack pose,MultiBufferSource buffers,BlockPos pos,Predicate<BlockPos> connected,boolean through,int color,Predicate<BlockPos> hidden,RenderType type) {
        var mc=Minecraft.getInstance();var out=buffers.getBuffer(type);
        for(var d:Direction.values()) if(!hidden.test(pos.relative(d))) {
            var sprite=mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(AlchemyContent.id("blocks/warded_glass_"+texture(pos,d.get3DDataValue(),connected)));
            quad(pose,out,FACES[d.get3DDataValue()],sprite.getU0(),sprite.getU1(),sprite.getV0(),sprite.getV1(),color);
        }
    }
    private static void quad(PoseStack pose,VertexConsumer out,float[][] p,float u0,float u1,float v0,float v1,int color) {
        for(int i=0;i<4;i++) out.addVertex(pose.last().pose(),p[i][0],p[i][1],p[i][2]).setUv(i==0||i==3?u0:u1,i<2?v1:v0).setColor(color);
    }
    public static final class Renderer implements BlockEntityRenderer<Warding.WardedEntity> {
        private final net.minecraft.client.renderer.block.BlockRenderDispatcher renderer;
        Renderer(BlockEntityRendererProvider.Context c) { renderer=c.getBlockRenderDispatcher(); }
        @Override public void render(Warding.WardedEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
            var level=tile.getLevel();if(level==null) return;var pos=tile.getBlockPos();var state=tile.state;
            var model=renderer.getBlockModel(state); var random=RandomSource.create();
            for(var layer:model.getRenderTypes(state,random,net.neoforged.neoforge.client.model.data.ModelData.EMPTY))
                renderer.getModelRenderer().tesselateBlock(level,model,state,pos,pose,buffers.getBuffer(net.neoforged.neoforge.client.RenderTypeHelper.getMovingBlockRenderType(layer)),false,random,state.getSeed(pos),overlay,net.neoforged.neoforge.client.model.data.ModelData.EMPTY,layer);
            var player=Minecraft.getInstance().player;
            if(player!=null && FociContent.getFocus(player.getMainHandItem()) instanceof Warding.WardingFocus) {
                boolean own=tile.owner==player.getName().getString().hashCode();
                glass(pose,buffers,pos,at->level.getBlockEntity(at) instanceof Warding.WardedEntity w&&w.owner==tile.owner,false,
                    color(player.tickCount,pos,.8f,own?.7f:.28f,.28f,own?.5f:.25f),at->level.getBlockState(at).is(Warding.WARDED.get()));
            }
        }
    }
    /** Original lifetime, Gaussian size, hit jitter, face rotation and sixteen hemis frames. */
    static final class WardParticle extends net.minecraft.client.particle.Particle {
        private final Direction side;private final float size,rotation,sx,sy,sz;
        WardParticle(net.minecraft.client.multiplayer.ClientLevel level,BlockHitResult hit) {
            super(level,hit.getBlockPos().getX()+.5,hit.getBlockPos().getY()+.5,hit.getBlockPos().getZ()+.5);
            side=hit.getDirection();lifetime=12+random.nextInt(5);hasPhysics=false;xd=yd=zd=0;
            size=(float)(1.4+random.nextGaussian()*.3f);rotation=random.nextInt(360);
            var h=hit.getLocation().subtract(hit.getBlockPos().getX(),hit.getBlockPos().getY(),hit.getBlockPos().getZ());
            sx=side.getStepX()!=0?0:Mth.clamp((float)h.x-.6f+random.nextFloat()*.2f,-.4f,.4f);
            sy=side.getStepY()!=0?0:Mth.clamp((float)h.y-.6f+random.nextFloat()*.2f,-.4f,.4f);
            sz=side.getStepZ()!=0?0:Mth.clamp((float)h.z-.6f+random.nextFloat()*.2f,-.4f,.4f);
        }
        @Override public net.minecraft.client.particle.ParticleRenderType getRenderType() { return net.minecraft.client.particle.ParticleRenderType.CUSTOM; }
        @Override public void tick() { float threshold=lifetime/5f;alpha=age<=threshold?age/threshold:(lifetime-age)/(float)lifetime;if(age++>=lifetime)remove(); }
        @Override public void render(VertexConsumer unused,Camera camera,float partial) {
            var pose=new PoseStack();var eye=camera.getPosition();pose.translate(x+sx-eye.x,y+sy-eye.y,z+sz-eye.z);
            if(side.getStepX()!=0||side.getStepY()!=0)pose.mulPose(new org.joml.Quaternionf().rotationAxis((float)Math.PI/2,side.getStepY(),-side.getStepX(),side.getStepZ()));
            pose.mulPose(Axis.ZP.rotationDegrees(rotation));
            pose.translate(0,0,side.getStepZ()>0?.505f:-.505f);
            if(side.getStepZ()>0)pose.mulPose(Axis.YN.rotationDegrees(180));
            pose.scale(size,size,size);
            int frame=Math.min(15,(int)(15*(age+partial)/lifetime));
            var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var type=NodeRenderTypes.additive(AlchemyContent.id("textures/models/hemis"+frame+".png"));
            quad(pose,buffers.getBuffer(type),new float[][]{{-.5f,.5f,0},{.5f,.5f,0},{.5f,-.5f,0},{-.5f,-.5f,0}},0,1,0,1,(int)(alpha*.5f*255)<<24|0xFFFFFF);buffers.endBatch(type);
        }
    }
}
