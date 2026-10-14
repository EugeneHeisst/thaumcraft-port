package dev.eugheist.tc4modern;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import com.mojang.blaze3d.vertex.*;
public final class EldritchCrabModel extends EntityModel<EldritchCrab> {
 private final ModelPart root; private final ModelPart TailHelm; private final ModelPart TailBare; private final ModelPart RFLeg1; private final ModelPart RClaw1; private final ModelPart Head1; private final ModelPart RClaw0; private final ModelPart RClaw2; private final ModelPart LClaw2; private final ModelPart LClaw1; private final ModelPart RArm; private final ModelPart Torso; private final ModelPart RRLeg1; private final ModelPart Head0; private final ModelPart LRLeg1; private final ModelPart LFLeg1; private final ModelPart RRLeg0; private final ModelPart RFLeg0; private final ModelPart LFLeg0; private final ModelPart LRLeg0; private final ModelPart LClaw0; private final ModelPart LArm;
 public EldritchCrabModel(ModelPart root){this.root=root;TailHelm=root.getChild("TailHelm");TailBare=root.getChild("TailBare");RFLeg1=root.getChild("RFLeg1");RClaw1=root.getChild("RClaw1");Head1=root.getChild("Head1");RClaw0=root.getChild("RClaw0");RClaw2=root.getChild("RClaw2");LClaw2=root.getChild("LClaw2");LClaw1=root.getChild("LClaw1");RArm=root.getChild("RArm");Torso=root.getChild("Torso");RRLeg1=root.getChild("RRLeg1");Head0=root.getChild("Head0");LRLeg1=root.getChild("LRLeg1");LFLeg1=root.getChild("LFLeg1");RRLeg0=root.getChild("RRLeg0");RFLeg0=root.getChild("RFLeg0");LFLeg0=root.getChild("LFLeg0");LRLeg0=root.getChild("LRLeg0");LClaw0=root.getChild("LClaw0");LArm=root.getChild("LArm");}
 public static LayerDefinition layer(){var mesh=new MeshDefinition();var root=mesh.getRoot();
 root.addOrReplaceChild("TailHelm",CubeListBuilder.create().texOffs(0,0).addBox(-4.5f,-4.5f,-0.4f,9f,9f,9f),PartPose.offsetAndRotation(0.0f,18.0f,0.0f,0.1047198f,0.0f,0.0f));
 root.addOrReplaceChild("TailBare",CubeListBuilder.create().texOffs(64,0).addBox(-4.0f,-4.0f,-0.4f,8f,8f,8f),PartPose.offsetAndRotation(0.0f,18.0f,0.0f,0.1047198f,0.0f,0.0f));
 root.addOrReplaceChild("RClaw1",CubeListBuilder.create().texOffs(0,47).addBox(-2.0f,-1.0f,-5.066667f,4f,3f,5f),PartPose.offsetAndRotation(-6.0f,15.5f,-10.0f,0,0,0));
 root.addOrReplaceChild("Head1",CubeListBuilder.create().texOffs(0,38).addBox(-2.0f,-1.5f,-9.066667f,4f,4f,1f),PartPose.offsetAndRotation(0.0f,18.0f,0.0f,0,0,0));
 root.addOrReplaceChild("RClaw0",CubeListBuilder.create().texOffs(0,55).addBox(-2.0f,-2.5f,-3.066667f,4f,5f,3f),PartPose.offsetAndRotation(-6.0f,17.0f,-7.0f,0,0,0));
 root.addOrReplaceChild("RClaw2",CubeListBuilder.create().texOffs(14,54).addBox(-1.5f,-1.0f,-4.066667f,3f,2f,5f),PartPose.offsetAndRotation(-6.0f,18.5f,-10.0f,0.3141593f,0.0f,0.0f));
 root.addOrReplaceChild("RArm",CubeListBuilder.create().texOffs(44,4).addBox(-1.0f,-1.0f,-5.066667f,2f,2f,6f),PartPose.offsetAndRotation(-3.0f,17.0f,-4.0f,0.0f,0.7504916f,0.0f));
 root.addOrReplaceChild("LClaw2",CubeListBuilder.create().texOffs(14,54).addBox(-1.5f,-1.0f,-4.066667f,3f,2f,5f),PartPose.offsetAndRotation(6.0f,18.5f,-10.0f,0.3141593f,0.0f,0.0f));
 root.addOrReplaceChild("LClaw1",CubeListBuilder.create().texOffs(0,47).addBox(-2.0f,-1.0f,-5.066667f,4f,3f,5f),PartPose.offsetAndRotation(6.0f,15.5f,-10.0f,0,0,0));
 root.addOrReplaceChild("LClaw0",CubeListBuilder.create().texOffs(0,55).addBox(-2.0f,-2.5f,-3.066667f,4f,5f,3f),PartPose.offsetAndRotation(6.0f,17.0f,-7.0f,0,0,0));
 root.addOrReplaceChild("LArm",CubeListBuilder.create().texOffs(44,4).addBox(-1.0f,-1.0f,-4.066667f,2f,2f,6f),PartPose.offsetAndRotation(4.0f,17.0f,-5.0f,0.0f,-0.7504916f,0.0f));
 root.addOrReplaceChild("Torso",CubeListBuilder.create().texOffs(0,18).addBox(-3.5f,-3.5f,-6.066667f,7f,7f,6f),PartPose.offsetAndRotation(0.0f,18.0f,0.0f,0.0523599f,0.0f,0.0f));
 root.addOrReplaceChild("Head0",CubeListBuilder.create().texOffs(0,31).addBox(-2.5f,-2.0f,-8.066667f,5f,5f,2f),PartPose.offsetAndRotation(0.0f,18.0f,0.0f,0,0,0));
 root.addOrReplaceChild("RRLeg1",CubeListBuilder.create().texOffs(36,4).addBox(-4.5f,1.0f,-0.9f,2f,5f,2f),PartPose.offsetAndRotation(-4.0f,20.0f,-1.5f,0,0,0));
 root.addOrReplaceChild("RFLeg1",CubeListBuilder.create().texOffs(36,4).addBox(-5.0f,1.0f,-1.066667f,2f,5f,2f),PartPose.offsetAndRotation(-4.0f,20.0f,-3.5f,0,0,0));
 root.addOrReplaceChild("LRLeg1",CubeListBuilder.create().texOffs(36,4).addBox(2.5f,1.0f,-0.9f,2f,5f,2f),PartPose.offsetAndRotation(4.0f,20.0f,-1.5f,0,0,0));
 root.addOrReplaceChild("LFLeg1",CubeListBuilder.create().texOffs(36,4).addBox(3.0f,1.0f,-1.066667f,2f,5f,2f),PartPose.offsetAndRotation(4.0f,20.0f,-3.5f,0,0,0));
 root.addOrReplaceChild("RRLeg0",CubeListBuilder.create().texOffs(36,0).addBox(-4.5f,-1.0f,-0.9f,6f,2f,2f),PartPose.offsetAndRotation(-4.0f,20.0f,-1.5f,0,0,0));
 root.addOrReplaceChild("RFLeg0",CubeListBuilder.create().texOffs(36,0).addBox(-5.0f,-1.0f,-1.066667f,6f,2f,2f),PartPose.offsetAndRotation(-4.0f,20.0f,-3.5f,0,0,0));
 root.addOrReplaceChild("LFLeg0",CubeListBuilder.create().texOffs(36,0).addBox(-1.0f,-1.0f,-1.066667f,6f,2f,2f),PartPose.offsetAndRotation(4.0f,20.0f,-3.5f,0,0,0));
 root.addOrReplaceChild("LRLeg0",CubeListBuilder.create().texOffs(36,0).addBox(-1.5f,-1.0f,-0.9f,6f,2f,2f),PartPose.offsetAndRotation(4.0f,20.0f,-1.5f,0.0f,-0.2094395f,-0.4363323f));
 return LayerDefinition.create(mesh,128,64);}
 @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,int color){root.render(pose,out,light,overlay,color);}
 @Override public void setupAnim(EldritchCrab entity,float par1,float par2,float par3,float par4,float par5){TailHelm.visible=entity.hasHelm();TailBare.visible=!entity.hasHelm();
        rotate(RRLeg1, 0.0f, 0.2094395f, 0.4363323f);
        rotate(RFLeg1, 0.0f, -0.2094395f, 0.4363323f);
        rotate(LRLeg1, 0.0f, -0.2094395f, -0.4363323f);
        rotate(LFLeg1, 0.0f, 0.2094395f, -0.4363323f);
        rotate(RRLeg0, 0.0f, 0.2094395f, 0.4363323f);
        rotate(RFLeg0, 0.0f, -0.2094395f, 0.4363323f);
        rotate(LFLeg0, 0.0f, 0.2094395f, -0.4363323f);
        rotate(LRLeg0, 0.0f, -0.2094395f, -0.4363323f);
        float f9 = -(net.minecraft.util.Mth.cos((float)(par1 * 0.6662f * 2.0f + 0.0f)) * 0.4f) * par2;
        float f10 = -(net.minecraft.util.Mth.cos((float)(par1 * 0.6662f * 2.0f + (float)Math.PI)) * 0.4f) * par2;
        RRLeg1.yRot += f9;
        RRLeg0.yRot += f9;
        LRLeg1.yRot += -f9;
        LRLeg0.yRot += -f9;
        RFLeg1.yRot += f10;
        RFLeg0.yRot += f10;
        LFLeg1.yRot += -f10;
        LFLeg0.yRot += -f10;
        RRLeg1.zRot += f9;
        RRLeg0.zRot += f9;
        LRLeg1.zRot += -f9;
        LRLeg0.zRot += -f9;
        RFLeg1.zRot += f10;
        RFLeg0.zRot += f10;
        LFLeg1.zRot += -f10;
        LFLeg0.zRot += -f10;
        TailBare.yRot = TailHelm.yRot = net.minecraft.util.Mth.cos((float)(par1 * 0.6662f)) * 2.0f * par2 * 0.125f;
        TailBare.zRot = TailHelm.zRot = net.minecraft.util.Mth.cos((float)(par1 * 0.6662f)) * par2 * 0.125f;
        RClaw2.xRot = 0.3141593f - net.minecraft.util.Mth.sin((float)((float)entity.tickCount / 4.0f)) * 0.25f;
        LClaw2.xRot = 0.3141593f + net.minecraft.util.Mth.sin((float)((float)entity.tickCount / 4.1f)) * 0.25f;
        RClaw1.xRot = net.minecraft.util.Mth.sin((float)((float)entity.tickCount / 4.0f)) * 0.125f;
        LClaw1.xRot = -net.minecraft.util.Mth.sin((float)((float)entity.tickCount / 4.1f)) * 0.125f;
}
 private static void rotate(ModelPart p,float x,float y,float z){p.xRot=x;p.yRot=y;p.zRot=z;}
}
