package thaumcraft.client.renderers.models.entities;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
/** Original Minecraft 1.7.10 ModelSpider used by RenderTaintSpider: left legs are not UV mirrored. */
public final class TaintSpiderGeometry {
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(32,4).addBox(-4,-4,-8,8,8,8),PartPose.offset(0,15,-3));
        root.addOrReplaceChild("body0",CubeListBuilder.create().texOffs(0,0).addBox(-3,-3,-3,6,6,6),PartPose.offset(0,15,0));
        root.addOrReplaceChild("body1",CubeListBuilder.create().texOffs(0,12).addBox(-5,-4,-6,10,8,12),PartPose.offset(0,15,9));
        String[] names={"hind","middle_hind","middle_front","front"};
        for(int i=0;i<4;i++){
            root.addOrReplaceChild("right_"+names[i]+"_leg",CubeListBuilder.create().texOffs(18,0).addBox(-15,-1,-1,16,2,2),PartPose.offset(-4,15,2-i));
            root.addOrReplaceChild("left_"+names[i]+"_leg",CubeListBuilder.create().texOffs(18,0).addBox(-1,-1,-1,16,2,2),PartPose.offset(4,15,2-i));
        }
        return LayerDefinition.create(mesh,64,32);
    }
    private TaintSpiderGeometry(){}
}
