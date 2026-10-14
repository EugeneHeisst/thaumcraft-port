package dev.eugheist.tc4modern;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
/** Original ModelBore, ModelBoreBase, ModelBoreEmit cube dimensions and texture coordinates. */
final class ArcaneBoreModels {
 static ModelPart ModelBoreBase(){var mesh=new MeshDefinition();var root=mesh.getRoot();
 root.addOrReplaceChild("Base1",CubeListBuilder.create().texOffs(64,24).mirror().addBox(-8,0,-8,16,2,16),PartPose.offset(0,0,0));
 root.addOrReplaceChild("Base2",CubeListBuilder.create().texOffs(64,24).mirror().addBox(-8,0,-8,16,2,16),PartPose.offset(0,14,0));
 root.addOrReplaceChild("PillarMid",CubeListBuilder.create().texOffs(84,42).mirror().addBox(-2.5f,0,-2.5f,5,12,5),PartPose.offset(0,2,0));
 root.addOrReplaceChild("Pillar2",CubeListBuilder.create().texOffs(64,42).mirror().addBox(-2,0,-2,4,12,4),PartPose.offset(-5,2,-5));
 root.addOrReplaceChild("Pillar3",CubeListBuilder.create().texOffs(64,42).mirror().addBox(-2,0,-2,4,12,4),PartPose.offset(-5,2,5));
 root.addOrReplaceChild("Pillar4",CubeListBuilder.create().texOffs(64,42).mirror().addBox(-2,0,-2,4,12,4),PartPose.offset(5,2,5));
 root.addOrReplaceChild("Pillar1",CubeListBuilder.create().texOffs(64,42).mirror().addBox(-2,0,-2,4,12,4),PartPose.offset(5,2,-5));
 root.addOrReplaceChild("Nozzle1",CubeListBuilder.create().texOffs(106,42).mirror().addBox(2.5f,-2,-2,5,4,4),PartPose.offset(0,8,0));
 root.addOrReplaceChild("Nozzle2",CubeListBuilder.create().texOffs(106,51).mirror().addBox(7,-2.5f,-2.5f,1,5,5),PartPose.offset(0,8,0));
 return LayerDefinition.create(mesh,128,64).bakeRoot();}
 static ModelPart ModelBore(){var mesh=new MeshDefinition();var root=mesh.getRoot();
 root.addOrReplaceChild("Base",CubeListBuilder.create().texOffs(0,32).mirror().addBox(-6,0,-6,12,2,12),PartPose.offset(0,0,0));
 root.addOrReplaceChild("Side1",CubeListBuilder.create().texOffs(0,0).mirror().addBox(-2,2,-5.5f,4,8,1),PartPose.offset(0,0,0));
 root.addOrReplaceChild("Side2",CubeListBuilder.create().texOffs(0,0).mirror().addBox(-2,2,4.5f,4,8,1),PartPose.offset(0,0,0));
 root.addOrReplaceChild("NozCrossbar",CubeListBuilder.create().texOffs(0,48).mirror().addBox(-1,-1,-6,2,2,12),PartPose.offset(0,8,0));
 root.addOrReplaceChild("NozFront",CubeListBuilder.create().texOffs(30,14).mirror().addBox(4,-2.5f,-2.5f,4,5,5),PartPose.offset(0,8,0));
 root.addOrReplaceChild("NozMid",CubeListBuilder.create().texOffs(0,14).mirror().addBox(-2,-4,-4,6,8,8),PartPose.offset(0,8,0));
 return LayerDefinition.create(mesh,128,64).bakeRoot();}
 static ModelPart ModelBoreEmit(){var mesh=new MeshDefinition();var root=mesh.getRoot();
 root.addOrReplaceChild("Knob",CubeListBuilder.create().texOffs(66,0).mirror().addBox(-2,12,-2,4,4,4),PartPose.offset(0,0,0));
 root.addOrReplaceChild("Cross1",CubeListBuilder.create().texOffs(56,16).mirror().addBox(-2,0,-2,4,1,4),PartPose.offset(0,8,0));
 root.addOrReplaceChild("Cross3",CubeListBuilder.create().texOffs(56,16).mirror().addBox(-2,0,-2,4,1,4),PartPose.offset(0,0,0));
 root.addOrReplaceChild("Cross2",CubeListBuilder.create().texOffs(56,24).mirror().addBox(-3,4,-3,6,1,6),PartPose.offset(0,0,0));
 root.addOrReplaceChild("Rod",CubeListBuilder.create().texOffs(56,0).mirror().addBox(-1,1,-1,2,11,2),PartPose.offset(0,0,0));
 return LayerDefinition.create(mesh,128,64).bakeRoot();}
}
