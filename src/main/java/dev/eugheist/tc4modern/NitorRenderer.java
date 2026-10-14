package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Vector3f;

/** Original FXWisp fullbright/additive quad, using its exact original particle-sheet region. */
public final class NitorRenderer implements BlockEntityRenderer<NitorEntity> {
    private boolean logged;
    public NitorRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(NitorEntity tile,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        if(Boolean.getBoolean("tc4modern.debugNitor") && !logged){logged=true;org.slf4j.LoggerFactory.getLogger("tc4modern.Nitor").info("render {} wisps={}",tile.getBlockPos(),tile.wisps.size());}
        var facing=Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        var vertex=buffers.getBuffer(NodeRenderTypes.wisp());
        for(var wisp:tile.wisps){
            float cx=(float)(wisp.previousX+(wisp.x-wisp.previousX)*partial);
            float cy=(float)(wisp.previousY+(wisp.y-wisp.previousY)*partial);
            float cz=(float)(wisp.previousZ+(wisp.z-wisp.previousZ)*partial);
            float radius=wisp.radius();
            float[][] corners={{-1,-1,.125f,1},{-1,1,.125f,.875f},{1,1,0,.875f},{1,-1,0,1}};
            for(var corner:corners){
                var offset=new Vector3f(corner[0]*radius,corner[1]*radius,0).rotate(facing);
                vertex.addVertex(poses.last(),cx+offset.x,cy+offset.y,cz+offset.z)
                        .setColor((int)(wisp.red*255),(int)(wisp.green*255),(int)(wisp.blue*255),128)
                        .setUv(corner[2],corner[3]);
            }
        }
    }
}
