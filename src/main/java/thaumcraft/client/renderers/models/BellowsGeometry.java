package thaumcraft.client.renderers.models;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
/** Original ModelBellows geometry, preserving UV dimensions at original addBox time. */
public final class BellowsGeometry {
    private BellowsGeometry(){}
    public static ModelPart planks(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("bottom",CubeListBuilder.create().texOffs(0,0).addBox(-6,0,-6,12,2,12),PartPose.offset(0,22,0));
        root.addOrReplaceChild("middle",CubeListBuilder.create().texOffs(0,0).addBox(-6,-1,-6,12,2,12),PartPose.offset(0,16,0));
        root.addOrReplaceChild("top",CubeListBuilder.create().texOffs(0,0).addBox(-6,0,-6,12,2,12),PartPose.offset(0,8,0));
        root.addOrReplaceChild("nozzle",CubeListBuilder.create().texOffs(0,36).addBox(-2,-2,0,4,4,2),PartPose.offset(0,16,6));
        return LayerDefinition.create(mesh,128,64).bakeRoot();
    }
    public static ModelPart bag(){
        var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("bag",CubeListBuilder.create().texOffs(48,0).addBox(-10,-12.03333f,-10,20,24,20),PartPose.offset(0,.5f,0));
        return LayerDefinition.create(mesh,128,64).bakeRoot().getChild("bag");
    }
}
