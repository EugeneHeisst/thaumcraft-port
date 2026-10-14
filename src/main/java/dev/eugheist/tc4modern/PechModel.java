package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;import net.minecraft.client.model.*;import net.minecraft.client.model.geom.*;import net.minecraft.client.model.geom.builders.*;import net.minecraft.util.Mth;import net.minecraft.world.entity.HumanoidArm;
/** Original ModelPech's geometry/UVs and walk/jaw/pack/arm animations. */
public final class PechModel extends EntityModel<PechEntity> implements ArmedModel {
    private final ModelPart root,Body,RightLeg,LeftLeg,Head,Jowls,LowerPack,UpperPack,RightArm,LeftArm;
    public PechModel(){root=layer().bakeRoot();        Body=root.getChild("Body");
        RightLeg=root.getChild("RightLeg");
        LeftLeg=root.getChild("LeftLeg");
        Head=root.getChild("Head");
        Jowls=root.getChild("Jowls");
        LowerPack=root.getChild("LowerPack");
        UpperPack=root.getChild("UpperPack");
        RightArm=root.getChild("RightArm");
        LeftArm=root.getChild("LeftArm");}
    private static LayerDefinition layer(){var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("Body",CubeListBuilder.create().texOffs(34,12).mirror(true).addBox(-3.0f, 0.0f, 0.0f, 6, 10, 6),PartPose.offsetAndRotation(0.0f, 9.0f, -3.0f,0.3129957f, 0.0f, 0.0f));
        root.addOrReplaceChild("RightLeg",CubeListBuilder.create().texOffs(35,1).mirror(false).addBox(-2.9f, 0.0f, 0.0f, 3, 6, 3),PartPose.offsetAndRotation(0.0f, 18.0f, 0.0f,0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("LeftLeg",CubeListBuilder.create().texOffs(35,1).mirror(true).addBox(-0.1f, 0.0f, 0.0f, 3, 6, 3),PartPose.offsetAndRotation(0.0f, 18.0f, 0.0f,0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("Head",CubeListBuilder.create().texOffs(2,11).mirror(true).addBox(-3.5f, -5.0f, -5.0f, 7, 5, 5),PartPose.offsetAndRotation(0.0f, 8.0f, 0.0f,0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("Jowls",CubeListBuilder.create().texOffs(1,21).mirror(true).addBox(-4.0f, -1.0f, -6.0f, 8, 3, 5),PartPose.offsetAndRotation(0.0f, 8.0f, 0.0f,0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("LowerPack",CubeListBuilder.create().texOffs(0,0).mirror(true).addBox(-5.0f, 0.0f, 0.0f, 10, 5, 5),PartPose.offsetAndRotation(0.0f, 10.0f, 3.5f,0.3013602f, 0.0f, 0.0f));
        root.addOrReplaceChild("UpperPack",CubeListBuilder.create().texOffs(64,1).mirror(true).addBox(-7.5f, -14.0f, 0.0f, 15, 14, 11),PartPose.offsetAndRotation(0.0f, 10.0f, 3.0f,0.4537856f, 0.0f, 0.0f));
        root.addOrReplaceChild("RightArm",CubeListBuilder.create().texOffs(52,2).mirror(false).addBox(-2.0f, 0.0f, -1.0f, 2, 6, 2),PartPose.offsetAndRotation(-3.0f, 10.0f, -1.0f,0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("LeftArm",CubeListBuilder.create().texOffs(52,2).mirror(true).addBox(0.0f, 0.0f, -1.0f, 2, 6, 2),PartPose.offsetAndRotation(3.0f, 10.0f, -1.0f,0.0f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh,128,64);
    }
    @Override public void setupAnim(PechEntity entity,float walk,float amount,float age,float yaw,float pitch){
        root.getAllParts().forEach(ModelPart::resetPose);
        Head.yRot=yaw*Mth.DEG_TO_RAD;Head.xRot=pitch*Mth.DEG_TO_RAD;
        Jowls.yRot=Head.yRot;
        Jowls.xRot=Head.xRot+(.2617994f+Mth.cos(walk*.6662f)*amount*.25f)+.34906587f*Math.abs(Mth.sin(entity.mumble));
        RightArm.xRot=Mth.cos(walk*.6662f+Mth.PI)*2*amount*.5f;LeftArm.xRot=Mth.cos(walk*.6662f)*2*amount*.5f;
        RightLeg.xRot=Mth.cos(walk*.6662f)*1.4f*amount;LeftLeg.xRot=Mth.cos(walk*.6662f+Mth.PI)*1.4f*amount;
        LowerPack.yRot=Mth.cos(walk*.6662f)*2*amount*.125f;LowerPack.zRot=Mth.cos(walk*.6662f)*2*amount*.125f;
        if(riding){RightArm.xRot-=.62831855f;LeftArm.xRot-=.62831855f;RightLeg.xRot=LeftLeg.xRot=-1.2566371f;RightLeg.yRot=.31415927f;LeftLeg.yRot=-.31415927f;}
        if(attackTime>0){float f=1-attackTime;f*=f;f*=f;f=1-f;float lift=Mth.sin(f*Mth.PI),head=Mth.sin(attackTime*Mth.PI)*-(Head.xRot-.7f)*.75f;RightArm.xRot-=lift*1.2f+head;RightArm.zRot=Mth.sin(attackTime*Mth.PI)*-.4f;}
        if(entity.isCrouching()){RightArm.xRot+=.4f;LeftArm.xRot+=.4f;}
        RightArm.zRot+=Mth.cos(age*.09f)*.05f+.05f;LeftArm.zRot-=Mth.cos(age*.09f)*.05f+.05f;
        RightArm.xRot+=Mth.sin(age*.067f)*.05f;LeftArm.xRot-=Mth.sin(age*.067f)*.05f;
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,int color){root.render(pose,buffer,light,overlay,color);}
    @Override public void translateToHand(HumanoidArm arm,PoseStack pose){(arm==HumanoidArm.RIGHT?RightArm:LeftArm).translateAndRotate(pose);}
}
