package thaumcraft.client.renderers.models.gear;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
/** Original ModelRobe cuboids, UVs, attachment branches and cloth motion, adapted to ModelPart. */
public final class ModelRobe extends HumanoidModel<LivingEntity> {
    public ModelRobe(ModelPart root) { super(root); }
    public static LayerDefinition layer(boolean inner) {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE,0);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.offset(0.0f,0.0f,0.0f));
        root.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.offset(0.0f,0.0f,0.0f));
        root.addOrReplaceChild("body",CubeListBuilder.create(),PartPose.offset(0.0f,0.0f,0.0f));
        root.addOrReplaceChild("right_arm",CubeListBuilder.create(),PartPose.offset(-5.0f,2.0f,0.0f));
        root.addOrReplaceChild("left_arm",CubeListBuilder.create(),PartPose.offset(5.0f,2.0f,0.0f));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create(),PartPose.offset(-1.9f,12.0f,0.0f));
        root.addOrReplaceChild("left_leg",CubeListBuilder.create(),PartPose.offset(1.9f,12.0f,0.0f));
        root.getChild("head").addOrReplaceChild("Hood1", CubeListBuilder.create().texOffs(16,7).addBox(-4.5f,-9.0f,-4.6f,9.0f,9.0f,9.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        root.getChild("head").addOrReplaceChild("Hood2", CubeListBuilder.create().texOffs(52,13).addBox(-4.0f,-9.7f,2.0f,8.0f,9.0f,3.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,-0.2268928f,0.0f,0.0f));
        root.getChild("head").addOrReplaceChild("Hood3", CubeListBuilder.create().texOffs(52,14).addBox(-3.5f,-10.0f,3.5f,7.0f,8.0f,3.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,-0.3490659f,0.0f,0.0f));
        root.getChild("head").addOrReplaceChild("Hood4", CubeListBuilder.create().texOffs(53,15).addBox(-3.0f,-10.7f,3.5f,6.0f,7.0f,3.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,-0.5759587f,0.0f,0.0f));
        root.getChild("body").addOrReplaceChild("Mbelt", CubeListBuilder.create().texOffs(16,55).addBox(-4.0f,7.0f,-3.0f,8.0f,5.0f,1.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        root.getChild("body").addOrReplaceChild("MbeltB", CubeListBuilder.create().texOffs(16,55).addBox(-4.0f,7.0f,-4.0f,8.0f,5.0f,1.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,3.141593f,0.0f));
        root.getChild("body").addOrReplaceChild("MbeltL", CubeListBuilder.create().texOffs(16,36).addBox(4.0f,8.0f,-3.0f,1.0f,3.0f,6.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        root.getChild("body").addOrReplaceChild("MbeltR", CubeListBuilder.create().texOffs(16,36).addBox(-5.0f,8.0f,-3.0f,1.0f,3.0f,6.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        if (inner) root.getChild("left_leg").addOrReplaceChild("Focipouch", CubeListBuilder.create().texOffs(100,20).addBox(3.5f,0.5f,-2.5f,3.0f,6.0f,5.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,-0.122173f));
        if (inner) root.getChild("body").addOrReplaceChild("FrontclothR1", CubeListBuilder.create().texOffs(108,38).addBox(0.0f,0.0f,0.0f,3.0f,8.0f,1.0f), PartPose.offsetAndRotation(-3.0f,11.0f,-2.9f,-0.1047198f,0.0f,0.0f));
        if (inner) root.getChild("body").addOrReplaceChild("FrontclothR2", CubeListBuilder.create().texOffs(108,47).addBox(0.0f,7.5f,1.7f,3.0f,3.0f,1.0f), PartPose.offsetAndRotation(-3.0f,11.0f,-2.9f,-0.3316126f,0.0f,0.0f));
        if (inner) root.getChild("body").addOrReplaceChild("FrontclothL1", CubeListBuilder.create().texOffs(108,38).mirror().addBox(0.0f,0.0f,0.0f,3.0f,8.0f,1.0f), PartPose.offsetAndRotation(0.0f,11.0f,-2.9f,-0.1047198f,0.0f,0.0f));
        if (inner) root.getChild("body").addOrReplaceChild("FrontclothL2", CubeListBuilder.create().texOffs(108,47).mirror().addBox(0.0f,7.5f,1.7f,3.0f,3.0f,1.0f), PartPose.offsetAndRotation(0.0f,11.0f,-2.9f,-0.3316126f,0.0f,0.0f));
        if (inner) root.getChild("body").addOrReplaceChild("ClothBackR1", CubeListBuilder.create().texOffs(118,16).mirror().addBox(0.0f,0.0f,0.0f,4.0f,8.0f,1.0f), PartPose.offsetAndRotation(-4.0f,11.5f,2.9f,0.1047198f,0.0f,0.0f));
        if (inner) root.getChild("body").addOrReplaceChild("ClothBackR2", CubeListBuilder.create().texOffs(123,9).addBox(0.0f,7.8f,-0.9f,1.0f,2.0f,1.0f), PartPose.offsetAndRotation(-4.0f,11.5f,2.9f,0.2268928f,0.0f,0.0f));
        if (inner) root.getChild("body").addOrReplaceChild("ClothBackR3", CubeListBuilder.create().texOffs(120,12).mirror().addBox(1.0f,7.8f,-0.9f,3.0f,3.0f,1.0f), PartPose.offsetAndRotation(-4.0f,11.5f,2.9f,0.2268928f,0.0f,0.0f));
        if (inner) root.getChild("body").addOrReplaceChild("ClothBackL1", CubeListBuilder.create().texOffs(118,16).addBox(0.0f,0.0f,0.0f,4.0f,8.0f,1.0f), PartPose.offsetAndRotation(0.0f,11.5f,2.9f,0.1047198f,0.0f,0.0f));
        if (inner) root.getChild("body").addOrReplaceChild("ClothBackL2", CubeListBuilder.create().texOffs(123,9).mirror().addBox(3.0f,7.8f,-0.9f,1.0f,2.0f,1.0f), PartPose.offsetAndRotation(0.0f,11.5f,2.9f,0.2268928f,0.0f,0.0f));
        if (inner) root.getChild("body").addOrReplaceChild("ClothBackL3", CubeListBuilder.create().texOffs(120,12).addBox(0.0f,7.8f,-0.9f,3.0f,3.0f,1.0f), PartPose.offsetAndRotation(0.0f,11.5f,2.9f,0.2268928f,0.0f,0.0f));
        if (!inner) root.getChild("body").addOrReplaceChild("Chestplate", CubeListBuilder.create().texOffs(16,25).addBox(-4.0f,1.0f,-3.0f,8.0f,6.0f,1.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        if (!inner) root.getChild("body").addOrReplaceChild("Chestthing", CubeListBuilder.create().texOffs(56,50).addBox(-2.5f,1.0f,-4.0f,5.0f,7.0f,1.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        if (!inner) root.getChild("body").addOrReplaceChild("Scroll", CubeListBuilder.create().texOffs(78,25).addBox(-2.0f,9.5f,4.0f,8.0f,3.0f,3.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.1919862f));
        if (!inner) root.getChild("body").addOrReplaceChild("Backplate", CubeListBuilder.create().texOffs(36,45).addBox(-4.0f,1.0f,1.9f,8.0f,11.0f,2.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        if (!inner) root.getChild("body").addOrReplaceChild("Book", CubeListBuilder.create().texOffs(81,16).addBox(1.0f,0.0f,4.0f,5.0f,7.0f,2.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.7679449f));
        if (!inner) root.getChild("body").addOrReplaceChild("ClothchestL", CubeListBuilder.create().texOffs(108,38).mirror().addBox(2.1f,0.5f,-3.5f,2.0f,8.0f,1.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        if (!inner) root.getChild("body").addOrReplaceChild("ClothchestR", CubeListBuilder.create().texOffs(108,38).addBox(-4.1f,0.5f,-3.5f,2.0f,8.0f,1.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        root.getChild("right_arm").addOrReplaceChild("ShoulderR", CubeListBuilder.create().texOffs(16,45).mirror().addBox(-3.5f,-2.5f,-2.5f,5.0f,5.0f,5.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        root.getChild("right_arm").addOrReplaceChild("RArm1", CubeListBuilder.create().texOffs(88,39).addBox(-3.5f,2.5f,-2.5f,5.0f,7.0f,5.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        root.getChild("right_arm").addOrReplaceChild("RArm2", CubeListBuilder.create().texOffs(76,32).addBox(-3.0f,5.5f,2.5f,4.0f,4.0f,2.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        root.getChild("right_arm").addOrReplaceChild("RArm3", CubeListBuilder.create().texOffs(88,32).addBox(-2.5f,3.5f,2.5f,3.0f,2.0f,1.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        root.getChild("right_arm").addOrReplaceChild("ShoulderplateTopR", CubeListBuilder.create().texOffs(56,25).addBox(-5.5f,-2.5f,-3.5f,2.0f,1.0f,7.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.4363323f));
        root.getChild("right_arm").addOrReplaceChild("ShoulderplateR1", CubeListBuilder.create().texOffs(56,33).addBox(-4.5f,-1.5f,-3.5f,1.0f,4.0f,7.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.4363323f));
        root.getChild("right_arm").addOrReplaceChild("ShoulderplateR2", CubeListBuilder.create().texOffs(40,33).addBox(-3.5f,1.5f,-3.5f,1.0f,3.0f,7.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.4363323f));
        root.getChild("right_arm").addOrReplaceChild("ShoulderplateR3", CubeListBuilder.create().texOffs(40,33).addBox(-2.5f,3.5f,-3.5f,1.0f,3.0f,7.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.4363323f));
        root.getChild("left_arm").addOrReplaceChild("ShoulderL", CubeListBuilder.create().texOffs(16,45).mirror().addBox(-1.5f,-2.5f,-2.5f,5.0f,5.0f,5.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        root.getChild("left_arm").addOrReplaceChild("LArm1", CubeListBuilder.create().texOffs(88,39).mirror().addBox(-1.5f,2.5f,-2.5f,5.0f,7.0f,5.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        root.getChild("left_arm").addOrReplaceChild("LArm2", CubeListBuilder.create().texOffs(76,32).addBox(-1.0f,5.5f,2.5f,4.0f,4.0f,2.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        root.getChild("left_arm").addOrReplaceChild("LArm3", CubeListBuilder.create().texOffs(88,32).addBox(-0.5f,3.5f,2.5f,3.0f,2.0f,1.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f));
        root.getChild("left_arm").addOrReplaceChild("ShoulderplateTopL", CubeListBuilder.create().texOffs(56,25).addBox(3.5f,-2.5f,-3.5f,2.0f,1.0f,7.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,-0.4363323f));
        root.getChild("left_arm").addOrReplaceChild("ShoulderplateL1", CubeListBuilder.create().texOffs(56,33).addBox(3.5f,-1.5f,-3.5f,1.0f,4.0f,7.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,-0.4363323f));
        root.getChild("left_arm").addOrReplaceChild("ShoulderplateL2", CubeListBuilder.create().texOffs(40,33).addBox(2.5f,1.5f,-3.5f,1.0f,3.0f,7.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,-0.4363323f));
        root.getChild("left_arm").addOrReplaceChild("ShoulderplateL3", CubeListBuilder.create().texOffs(40,33).addBox(1.5f,3.5f,-3.5f,1.0f,3.0f,7.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,-0.4363323f));
        root.getChild("right_leg").addOrReplaceChild("LegpanelR4", CubeListBuilder.create().texOffs(76,38).addBox(-3.0f,0.5f,-3.5f,2.0f,3.0f,1.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,-0.4363323f,0.0f,0.0f));
        root.getChild("right_leg").addOrReplaceChild("LegpanelR5", CubeListBuilder.create().texOffs(76,42).addBox(-3.0f,2.5f,-2.5f,2.0f,3.0f,1.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,-0.4363323f,0.0f,0.0f));
        root.getChild("right_leg").addOrReplaceChild("LegpanelR6", CubeListBuilder.create().texOffs(82,38).addBox(-3.0f,4.5f,-1.5f,2.0f,3.0f,1.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,-0.4363323f,0.0f,0.0f));
        root.getChild("right_leg").addOrReplaceChild("SidepanelR1", CubeListBuilder.create().texOffs(116,25).mirror().addBox(-2.5f,0.5f,-2.5f,1.0f,4.0f,5.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.4363323f));
        root.getChild("right_leg").addOrReplaceChild("SideclothR1", CubeListBuilder.create().texOffs(116,42).addBox(-2.5f,0.5f,-2.5f,1.0f,5.0f,5.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.122173f));
        root.getChild("right_leg").addOrReplaceChild("SideclothR2", CubeListBuilder.create().texOffs(116,34).addBox(-1.5f,5.5f,-2.5f,1.0f,3.0f,5.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.296706f));
        root.getChild("right_leg").addOrReplaceChild("SideclothR3", CubeListBuilder.create().texOffs(116,1).addBox(0.4f,8.4f,-2.5f,1.0f,3.0f,5.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,0.5235988f));
        root.getChild("left_leg").addOrReplaceChild("LegpanelL4", CubeListBuilder.create().texOffs(76,38).mirror().addBox(1.0f,0.5f,-3.5f,2.0f,3.0f,1.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,-0.4363323f,0.0f,0.0f));
        root.getChild("left_leg").addOrReplaceChild("LegpanelL5", CubeListBuilder.create().texOffs(76,42).mirror().addBox(1.0f,2.5f,-2.5f,2.0f,3.0f,1.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,-0.4363323f,0.0f,0.0f));
        root.getChild("left_leg").addOrReplaceChild("LegpanelL6", CubeListBuilder.create().texOffs(82,38).mirror().addBox(1.0f,4.5f,-1.5f,2.0f,3.0f,1.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,-0.4363323f,0.0f,0.0f));
        root.getChild("left_leg").addOrReplaceChild("SidepanelL1", CubeListBuilder.create().texOffs(116,25).addBox(1.5f,0.5f,-2.5f,1.0f,4.0f,5.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,-0.4363323f));
        root.getChild("left_leg").addOrReplaceChild("SideclothL1", CubeListBuilder.create().texOffs(116,42).addBox(1.5f,0.5f,-2.5f,1.0f,5.0f,5.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,-0.122173f));
        root.getChild("left_leg").addOrReplaceChild("SideclothL2", CubeListBuilder.create().texOffs(116,34).addBox(0.5f,5.5f,-2.5f,1.0f,3.0f,5.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,-0.296706f));
        root.getChild("left_leg").addOrReplaceChild("SideclothL3", CubeListBuilder.create().texOffs(116,1).addBox(-1.4f,8.4f,-2.5f,1.0f,3.0f,5.0f), PartPose.offsetAndRotation(0.0f,0.0f,0.0f,0.0f,0.0f,-0.5235988f));
        return LayerDefinition.create(mesh,128,64);
    }
    private void cloth(float c) {
        if (!body.hasChild("FrontclothR1")) return;
        for(String side:new String[]{"R","L"}) {
            body.getChild("Frontcloth"+side+"1").xRot=c-.1047198f;
            body.getChild("Frontcloth"+side+"2").xRot=c-.3316126f;
            body.getChild("ClothBack"+side+"1").xRot=-c+.1047198f;
            body.getChild("ClothBack"+side+"2").xRot=-c+.2268928f;
            body.getChild("ClothBack"+side+"3").xRot=-c+.2268928f;
        }
    }
    @Override public void setupAnim(LivingEntity e,float walk,float amount,float age,float yaw,float pitch) {
        super.setupAnim(e,walk,amount,age,yaw,pitch);
        float a=net.minecraft.util.Mth.cos(walk*.6662f)*1.4f*amount;
        float b=net.minecraft.util.Mth.cos(walk*.6662f+(float)Math.PI)*1.4f*amount;
        cloth(Math.min(a,b));
    }
    public void animateArmor(LivingEntity e,float walk,float amount,float age) {
        float a=net.minecraft.util.Mth.cos(walk*.6662f)*1.4f*amount;
        float b=net.minecraft.util.Mth.cos(walk*.6662f+(float)Math.PI)*1.4f*amount;
        cloth(Math.min(a,b));
        if(e instanceof net.minecraft.world.entity.monster.Zombie || e instanceof net.minecraft.world.entity.monster.AbstractSkeleton){
            float f6=net.minecraft.util.Mth.sin(attackTime*(float)Math.PI);
            float f7=net.minecraft.util.Mth.sin((1-(1-attackTime)*(1-attackTime))*(float)Math.PI);
            rightArm.yRot=-(.1f-f6*.6f);leftArm.yRot=.1f-f6*.6f;
            rightArm.xRot=leftArm.xRot=-1.5707964f-f6*1.2f+f7*.4f;
            rightArm.zRot=net.minecraft.util.Mth.cos(age*.09f)*.05f+.05f;leftArm.zRot=-rightArm.zRot;
            rightArm.xRot+=net.minecraft.util.Mth.sin(age*.067f)*.05f;
            leftArm.xRot-=net.minecraft.util.Mth.sin(age*.067f)*.05f;
        }
    }
    @Override public void renderToBuffer(PoseStack poses,VertexConsumer buffer,int light,int overlay,int color) {
        // Armor hook supplies original cloth motion after copying the modern biped pose.
        if(young){super.renderToBuffer(poses,buffer,light,overlay,color);return;}
        poses.pushPose();poses.scale(1.01f,1.01f,1.01f);head.render(poses,buffer,light,overlay,color);poses.popPose();
        body.render(poses,buffer,light,overlay,color);rightArm.render(poses,buffer,light,overlay,color);leftArm.render(poses,buffer,light,overlay,color);rightLeg.render(poses,buffer,light,overlay,color);leftLeg.render(poses,buffer,light,overlay,color);hat.render(poses,buffer,light,overlay,color);
    }
}
