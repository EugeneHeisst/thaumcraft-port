package thaumcraft.client.renderers.models.entities;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import thaumcraft.common.entities.monster.EntityTaintSpore;
/** Original two translucent cubes and hurt-sensitive oscillation, adapted to baked ModelPart. */
public final class ModelTaintSpore extends EntityModel<EntityTaintSpore> {
    private final ModelPart cube;
    public ModelTaintSpore(ModelPart root){super(net.minecraft.client.renderer.RenderType::entityTranslucent);cube=root.getChild("cube");}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cube",CubeListBuilder.create().texOffs(0,0).addBox(-6,2,-6,12,12,12).texOffs(0,0).addBox(-8,0,-8,16,16,16),PartPose.offset(0,24,0));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(EntityTaintSpore entity,float limbSwing,float limbSwingAmount,float age,float yaw,float pitch){
        float intensity=entity.hurtTime>0?.04f:.02f;
        cube.xRot=intensity*net.minecraft.util.Mth.sin(age*.05f);
        cube.zRot=intensity*net.minecraft.util.Mth.sin(age*.1f);
    }
    @Override public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer vertices,int light,int overlay,int color){cube.render(pose,vertices,light,overlay,color);}
}
