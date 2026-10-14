package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

/** Modern quad bridge for original TileCrucibleRenderer water height and concentration color. */
public final class CrucibleRenderer implements BlockEntityRenderer<CrucibleEntity> {
    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(CrucibleEntity tile,float partial,PoseStack poses,
            MultiBufferSource buffers,int light,int overlay) {
        var facing=Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        var particles=buffers.getBuffer(NodeRenderTypes.wisp());
        for(var bubble:tile.bubbles){
            float cx=(float)(bubble.previousX+(bubble.x-bubble.previousX)*partial);
            float cy=(float)(bubble.previousY+(bubble.y-bubble.previousY)*partial);
            float cz=(float)(bubble.previousZ+(bubble.z-bubble.previousZ)*partial);
            float u=(bubble.particle%16)/16f,v=(bubble.particle/16)/16f,radius=.1f*bubble.scale;
            float[][] corners={{-1,-1,u+.0624375f,v+.0624375f},{-1,1,u+.0624375f,v},{1,1,u,v},{1,-1,u,v+.0624375f}};
            for(var c:corners){
                var offset=new org.joml.Vector3f(c[0]*radius,c[1]*radius,0).rotate(facing);
                particles.addVertex(poses.last(),cx+offset.x,cy+offset.y,cz+offset.z)
                        .setColor((int)(bubble.red*255),(int)(bubble.green*255),(int)(bubble.blue*255),255).setUv(c[2],c[3]);
            }
        }
        if(tile.crucible.water<=0)return;
        var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"blocks/crucible_water"));
        var color=tile.crucible.fluidColor();
        int r=Math.clamp((int)(color[0]*255),0,255),g=Math.clamp((int)(color[1]*255),0,255),b=Math.clamp((int)(color[2]*255),0,255);
        float y=tile.crucible.fluidHeight();
        var vertex=buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        // Original translated/rotated unit quad, above the inner floor.
        float[][] corners={{0,y,1,sprite.getU0(),sprite.getV0()},
                {1,y,1,sprite.getU1(),sprite.getV0()},
                {1,y,0,sprite.getU1(),sprite.getV1()},
                {0,y,0,sprite.getU0(),sprite.getV1()}};
        for(var c:corners)vertex.addVertex(poses.last(),c[0],c[1],c[2])
                .setColor(r,g,b,255).setUv(c[3],c[4]).setOverlay(overlay)
                .setLight(light).setNormal(poses.last(),0,1,0);
    }
}
