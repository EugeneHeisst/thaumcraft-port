package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import thaumcraft.common.lib.vis.NodeVisuals;

/** Modern submission of TileNodeEnergizedRenderer's original node and lightning ring. */
public final class EnergizedNodeRenderer implements BlockEntityRenderer<EnergizedNodeEntity> {
    public EnergizedNodeRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(EnergizedNodeEntity node,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        var center=Vec3.atCenterOf(node.getBlockPos());
        long now=System.nanoTime();
        var layers=NodeVisuals.layers(now,node.getBlockPos().getX(),mc.player.tickCount,center.distanceTo(mc.player.getEyePosition()),64,true,node.auraBase,node.nodeType,node.modifier);
        poses.pushPose();poses.translate(.5,.5,.5);
        AuraNodeRenderer.renderLayers(layers,partial,poses,buffers,center,true);
        // Original 4096 x 256 texture: sixteen horizontal frames, 40 ms per frame.
        double renderX=node.getBlockPos().getX()-mc.gameRenderer.getMainCamera().getPosition().x;
        int frame=(int)(((double)(now/40000000L)+renderX)%16);
        float u0=frame/16f,u1=(frame+1)/16f;
        float[][] corners={{-1,-1,u0,1},{-1,1,u1,1},{1,1,u1,0},{1,-1,u0,0}};
        var builder=buffers.getBuffer(NodeRenderTypes.energyRing());
        var facing=mc.getEntityRenderDispatcher().cameraOrientation();
        for(var corner:corners){
            var v=new Vector3f(corner[0]*.33f,corner[1]*.33f,0).rotate(facing);
            builder.addVertex(poses.last(),v.x,v.y,v.z).setColor(255,255,255,(int)(.9f*255)).setUv(corner[2],corner[3]);
        }
        poses.popPose();
    }
    @Override public boolean shouldRenderOffScreen(EnergizedNodeEntity node){return true;}
    @Override public int getViewDistance(){return 64;}
}
