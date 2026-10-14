package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
/** Modern submission of the original animated TileBellowsRenderer transforms. */
public final class BellowsModelRenderer {
    private final net.minecraft.client.model.geom.ModelPart planks=thaumcraft.client.renderers.models.BellowsGeometry.planks();
    private final net.minecraft.client.model.geom.ModelPart bag=thaumcraft.client.renderers.models.BellowsGeometry.bag();
    public void render(float inflation,int orientation,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        var vertices=buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/models/bellows.png")));
        float tscale=.125f+inflation*.875f;
        poses.pushPose();poses.translate(.5,-.5,.5);
        switch(orientation){
            case 0->{poses.translate(0,1,-1);poses.mulPose(Axis.XP.rotationDegrees(90));}
            case 1->{poses.translate(0,1,1);poses.mulPose(Axis.XP.rotationDegrees(270));}
            case 2->poses.mulPose(Axis.YP.rotationDegrees(180));
            case 4->poses.mulPose(Axis.YP.rotationDegrees(270));
            case 5->poses.mulPose(Axis.YP.rotationDegrees(90));
        }
        poses.translate(0,1,0);poses.pushPose();poses.scale(.5f,(inflation+.1f)/2,.5f);
        bag.render(poses,vertices,light,overlay);poses.popPose();poses.translate(0,-1,0);
        poses.pushPose();poses.translate(0,-tscale/2+.5f,0);planks.getChild("top").render(poses,vertices,light,overlay);poses.popPose();
        poses.pushPose();poses.translate(0,tscale/2-.5f,0);planks.getChild("bottom").render(poses,vertices,light,overlay);poses.popPose();
        planks.getChild("middle").render(poses,vertices,light,overlay);planks.getChild("nozzle").render(poses,vertices,light,overlay);
        poses.popPose();
    }
}
