package dev.eugheist.tc4modern;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;
final class LeaderArmorModel extends HumanoidModel<LivingEntity> {
 LeaderArmorModel(boolean chest){super(root(chest));head.xScale=head.yScale=head.zScale=1.01f;}
 static ModelPart root(boolean chest){var mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0);var root=mesh.getRoot();
 var head=root.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.ZERO);
 var hat=root.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
 var body=root.addOrReplaceChild("body",CubeListBuilder.create(),PartPose.ZERO);
 var right_arm=root.addOrReplaceChild("right_arm",CubeListBuilder.create(),PartPose.offset(-5,2,0));
 var left_arm=root.addOrReplaceChild("left_arm",CubeListBuilder.create(),PartPose.offset(5,2,0));
 var right_leg=root.addOrReplaceChild("right_leg",CubeListBuilder.create(),PartPose.offset(-1.9f,12,0));
 var left_leg=root.addOrReplaceChild("left_leg",CubeListBuilder.create(),PartPose.offset(1.9f,12,0));
 head.addOrReplaceChild("Helmet",CubeListBuilder.create().texOffs(41,8).addBox(-4.5f, -9.0f, -4.5f, 9, 9, 9),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
 if(chest)body.addOrReplaceChild("CollarF",CubeListBuilder.create().texOffs(17,31).addBox(-4.5f, -1.5f, -3.0f, 9, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.2268928f, 0.0f, 0.0f));
 if(chest)body.addOrReplaceChild("CollarB",CubeListBuilder.create().texOffs(17,26).addBox(-4.5f, -1.5f, 7.0f, 9, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.2268928f, 0.0f, 0.0f));
 if(chest)body.addOrReplaceChild("CollarR",CubeListBuilder.create().texOffs(17,11).addBox(-5.5f, -1.5f, -3.0f, 1, 4, 11),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.2268928f, 0.0f, 0.0f));
 if(chest)body.addOrReplaceChild("CollarL",CubeListBuilder.create().texOffs(17,11).addBox(4.5f, -1.5f, -3.0f, 1, 4, 11),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.2268928f, 0.0f, 0.0f));
 if(chest)body.addOrReplaceChild("BeltR",CubeListBuilder.create().texOffs(76,44).addBox(-5.0f, 4.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0,0,0,0,0,0));
 body.addOrReplaceChild("Mbelt",CubeListBuilder.create().texOffs(56,55).addBox(-4.0f, 8.0f, -3.0f, 8, 4, 1),PartPose.offsetAndRotation(0,0,0,0,0,0));
 body.addOrReplaceChild("MbeltL",CubeListBuilder.create().texOffs(76,44).addBox(4.0f, 8.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0,0,0,0,0,0));
 body.addOrReplaceChild("MbeltR",CubeListBuilder.create().texOffs(76,44).addBox(-5.0f, 8.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0,0,0,0,0,0));
 if(chest)body.addOrReplaceChild("BeltL",CubeListBuilder.create().texOffs(76,44).addBox(4.0f, 4.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0,0,0,0,0,0));
 if(chest)body.addOrReplaceChild("CloakTL",CubeListBuilder.create().texOffs(0,43).addBox(2.5f, 1.0f, -1.0f, 2, 1, 3),PartPose.offsetAndRotation(0.0f, 0.0f, 3.0f,0.1396263f, 0.0f, 0.0f));
 if(chest)body.addOrReplaceChild("Cloak3",CubeListBuilder.create().texOffs(0,59).addBox(-4.5f, 17.0f, -3.7f, 9, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 3.0f,0.4465716f, 0.0f, 0.0f));
 if(chest)body.addOrReplaceChild("CloakTR",CubeListBuilder.create().texOffs(0,43).addBox(-4.5f, 1.0f, -1.0f, 2, 1, 3),PartPose.offsetAndRotation(0.0f, 0.0f, 3.0f,0.1396263f, 0.0f, 0.0f));
 if(chest)body.addOrReplaceChild("Cloak1",CubeListBuilder.create().texOffs(0,47).addBox(-4.5f, 2.0f, 1.0f, 9, 12, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 3.0f,0.1396263f, 0.0f, 0.0f));
 if(chest)body.addOrReplaceChild("Cloak2",CubeListBuilder.create().texOffs(0,59).addBox(-4.5f, 14.0f, -1.3f, 9, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 3.0f,0.3069452f, 0.0f, 0.0f));
 if(chest)body.addOrReplaceChild("Chestplate",CubeListBuilder.create().texOffs(56,45).addBox(-4.0f, 1.0f, -3.8f, 8, 7, 2),PartPose.offsetAndRotation(0,0,0,0,0,0));
 if(chest)body.addOrReplaceChild("ChestOrnament",CubeListBuilder.create().texOffs(76,53).addBox(-2.5f, 3.0f, -4.8f, 5, 5, 1),PartPose.offsetAndRotation(0,0,0,0,0,0));
 if(chest)body.addOrReplaceChild("ChestClothL",CubeListBuilder.create().texOffs(20,47).mirror().addBox(1.5f, 1.2f, -4.5f, 3, 9, 1),PartPose.offsetAndRotation(0,0,0,0.0663225f, 0.0f, 0.0f));
 if(chest)body.addOrReplaceChild("ChestClothR",CubeListBuilder.create().texOffs(20,47).addBox(-4.5f, 1.2f, -4.5f, 3, 9, 1),PartPose.offsetAndRotation(0,0,0,0.0663225f, 0.0f, 0.0f));
 if(chest)body.addOrReplaceChild("Backplate",CubeListBuilder.create().texOffs(36,45).addBox(-4.0f, 1.0f, 2.0f, 8, 11, 2),PartPose.offsetAndRotation(0,0,0,0,0,0));
 right_arm.addOrReplaceChild("GauntletR",CubeListBuilder.create().texOffs(100,26).addBox(-3.5f, 3.5f, -2.5f, 2, 6, 5),PartPose.offsetAndRotation(0,0,0,0,0,0));
 left_arm.addOrReplaceChild("GauntletL",CubeListBuilder.create().texOffs(114,26).addBox(1.5f, 3.5f, -2.5f, 2, 6, 5),PartPose.offsetAndRotation(0,0,0,0,0,0));
 left_arm.addOrReplaceChild("GauntletstrapL1",CubeListBuilder.create().texOffs(84,31).mirror().addBox(-1.5f, 3.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0,0,0,0,0,0));
 left_arm.addOrReplaceChild("GauntletstrapL2",CubeListBuilder.create().texOffs(84,31).mirror().addBox(-1.5f, 6.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0,0,0,0,0,0));
 right_arm.addOrReplaceChild("GauntletstrapR1",CubeListBuilder.create().texOffs(84,31).addBox(-1.5f, 3.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0,0,0,0,0,0));
 right_arm.addOrReplaceChild("GauntletstrapR2",CubeListBuilder.create().texOffs(84,31).addBox(-1.5f, 6.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0,0,0,0,0,0));
 right_arm.addOrReplaceChild("GauntletR2",CubeListBuilder.create().texOffs(102,37).addBox(-5.0f, 3.5f, -2.0f, 1, 5, 4),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.1675516f));
 left_arm.addOrReplaceChild("GauntletL2",CubeListBuilder.create().texOffs(102,37).addBox(4.0f, 3.5f, -2.0f, 1, 5, 4),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.1675516f));
 right_arm.addOrReplaceChild("ShoulderR",CubeListBuilder.create().texOffs(56,35).addBox(-3.5f, -2.5f, -2.5f, 5, 5, 5),PartPose.offsetAndRotation(0,0,0,0,0,0));
 right_arm.addOrReplaceChild("ShoulderR1",CubeListBuilder.create().texOffs(0,0).addBox(-4.3f, -1.5f, -3.0f, 3, 5, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.7853982f));
 right_arm.addOrReplaceChild("ShoulderR2",CubeListBuilder.create().texOffs(0,19).addBox(-3.3f, 3.5f, -2.5f, 1, 1, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.7853982f));
 right_arm.addOrReplaceChild("ShoulderR5",CubeListBuilder.create().texOffs(18,4).addBox(-2.3f, -1.5f, 3.0f, 1, 6, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.7853982f));
 right_arm.addOrReplaceChild("ShoulderR3",CubeListBuilder.create().texOffs(0,11).addBox(-2.3f, 3.5f, -3.0f, 1, 2, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.7853982f));
 right_arm.addOrReplaceChild("ShoulderR4",CubeListBuilder.create().texOffs(18,4).addBox(-2.3f, -1.5f, -4.0f, 1, 6, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.7853982f));
 left_arm.addOrReplaceChild("ShoulderL",CubeListBuilder.create().texOffs(56,35).addBox(-1.5f, -2.5f, -2.5f, 5, 5, 5),PartPose.offsetAndRotation(0,0,0,0,0,0));
 left_arm.addOrReplaceChild("ShoulderL1",CubeListBuilder.create().texOffs(0,0).addBox(1.3f, -1.5f, -3.0f, 3, 5, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.7853982f));
 left_arm.addOrReplaceChild("ShoulderL2",CubeListBuilder.create().texOffs(0,19).mirror().addBox(2.3f, 3.5f, -2.5f, 1, 1, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.7853982f));
 left_arm.addOrReplaceChild("ShoulderL3",CubeListBuilder.create().texOffs(0,11).addBox(1.3f, 3.5f, -3.0f, 1, 2, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.7853982f));
 left_arm.addOrReplaceChild("ShoulderL5",CubeListBuilder.create().texOffs(18,4).addBox(1.3f, -1.5f, 3.0f, 1, 6, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.7853982f));
 left_arm.addOrReplaceChild("ShoulderL4",CubeListBuilder.create().texOffs(18,4).addBox(1.3f, -1.5f, -4.0f, 1, 6, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.7853982f));
 if(chest)body.addOrReplaceChild("LegClothR",CubeListBuilder.create().texOffs(20,55).addBox(0.0f, 0.0f, 0.0f, 3, 8, 1),PartPose.offsetAndRotation(-4.5f, 10.4f, -3.9f,-0.0349066f, 0.0f, 0.0f));
 if(chest)body.addOrReplaceChild("LegClothL",CubeListBuilder.create().texOffs(20,55).mirror().addBox(0.0f, 0.0f, 0.0f, 3, 8, 1),PartPose.offsetAndRotation(1.5f, 10.4f, -3.9f,-0.0349066f, 0.0f, 0.0f));
 right_leg.addOrReplaceChild("BackpanelR1",CubeListBuilder.create().texOffs(0,25).addBox(-3.0f, -0.5f, 2.5f, 5, 7, 1),PartPose.offsetAndRotation(0,0,0,0.0698132f, 0.0f, 0.0f));
 right_leg.addOrReplaceChild("BackpanelR2",CubeListBuilder.create().texOffs(96,14).addBox(-3.0f, -0.5f, -2.5f, 5, 3, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.1396263f));
 right_leg.addOrReplaceChild("BackpanelR3",CubeListBuilder.create().texOffs(116,13).addBox(-3.0f, 2.5f, -2.5f, 1, 4, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.1396263f));
 right_leg.addOrReplaceChild("BackpanelR4",CubeListBuilder.create().texOffs(0,25).mirror().addBox(-3.0f, -0.5f, -3.5f, 5, 7, 1),PartPose.offsetAndRotation(0,0,0,-0.0349066f, 0.0f, 0.0f));
 left_leg.addOrReplaceChild("BackpanelL1",CubeListBuilder.create().texOffs(0,25).addBox(-2.0f, -0.5f, 2.5f, 5, 7, 1),PartPose.offsetAndRotation(0,0,0,0.0698132f, 0.0f, 0.0f));
 left_leg.addOrReplaceChild("BackpanelL4",CubeListBuilder.create().texOffs(0,25).addBox(-2.0f, -0.5f, -3.5f, 5, 7, 1),PartPose.offsetAndRotation(0,0,0,-0.0349066f, 0.0f, 0.0f));
 left_leg.addOrReplaceChild("BackpanelL2",CubeListBuilder.create().texOffs(96,14).addBox(-2.0f, -0.5f, -2.5f, 5, 3, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.1396263f));
 left_leg.addOrReplaceChild("BackpanelL3",CubeListBuilder.create().texOffs(116,13).addBox(2.0f, 2.5f, -2.5f, 1, 4, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.1396263f));
 return LayerDefinition.create(mesh,128,64).bakeRoot();}
 @Override public void setupAnim(LivingEntity entity,float walk,float amount,float age,float yaw,float pitch){super.setupAnim(entity,walk,amount,age,yaw,pitch);animate(walk,amount);}
 void animate(float walk,float amount){float a=(float)Math.cos(walk*.6662f)*1.4f*amount,b=(float)Math.cos(walk*.6662f+Math.PI)*1.4f*amount,c=Math.min(a,b);if(body.hasChild("LegClothR")){body.getChild("LegClothR").xRot=a-.1047198f;body.getChild("LegClothL").xRot=b-.1047198f;body.getChild("Cloak1").xRot=-c/2+.1396263f;body.getChild("Cloak2").xRot=-c/2+.3069452f;body.getChild("Cloak3").xRot=-c/2+.4465716f;}}
}
