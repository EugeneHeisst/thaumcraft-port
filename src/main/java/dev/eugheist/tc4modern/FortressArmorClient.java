package dev.eugheist.tc4modern;
import java.util.*;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import com.mojang.blaze3d.vertex.*;
/** Original ModelFortressArmor: all cubes, set decorations, masks and goggles. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class FortressArmorClient {
 static int rendered;
 static final Map<LivingEntity,int[]> CACHE=new WeakHashMap<>();
 @SubscribeEvent static void extension(RegisterClientExtensionsEvent event){event.registerItem(new IClientItemExtensions(){final Model chest=new Model(true),other=new Model(false);public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){var model=slot==EquipmentSlot.CHEST?chest:other;model.entity=entity;model.update();return model;}},FortressArmor.item("helmet"),FortressArmor.item("chestplate"),FortressArmor.item("leggings"));}
 static final class Model extends HumanoidModel<LivingEntity> {
  LivingEntity entity;final Map<String,ModelPart> parts=new HashMap<>();
  Model(boolean chest){super(root(chest));collect(head);collect(body);collect(rightArm);collect(leftArm);collect(rightLeg);collect(leftLeg);}
  void collect(ModelPart parent){for(String name:NAMES)if(parent.hasChild(name))parts.put(name,parent.getChild(name));}
  void update(){if(entity==null)return;int[] cached=CACHE.computeIfAbsent(entity,e->new int[]{0,-1,0,-1});if(cached[3]<0||entity.tickCount-cached[3]>=20){cached[0]=0;for(var slot:List.of(EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD))if(entity.getItemBySlot(slot).getItem() instanceof FortressArmor.Piece)cached[0]++;var helmet=entity.getItemBySlot(EquipmentSlot.HEAD);if(helmet.getItem() instanceof FortressArmor.Piece){cached[1]=FortressArmor.mask(entity);cached[2]=NoteData.tag(helmet).contains("goggles")?1:0;}cached[3]=entity.tickCount;}int set=cached[0],mask=cached[1];for(int i=0;i<3;i++)parts.get("Mask"+i).visible=mask==i;parts.get("Goggles").visible=cached[2]==1;
   if(parts.containsKey("Scroll"))parts.get("Scroll").visible=set>=3;
   if(parts.containsKey("OrnamentL"))parts.get("OrnamentL").visible=set>=3;
   if(parts.containsKey("OrnamentL2"))parts.get("OrnamentL2").visible=set>=3;
   if(parts.containsKey("OrnamentR"))parts.get("OrnamentR").visible=set>=3;
   if(parts.containsKey("OrnamentR2"))parts.get("OrnamentR2").visible=set>=3;
   if(parts.containsKey("Gemornament"))parts.get("Gemornament").visible=set>=3;
   if(parts.containsKey("Gem"))parts.get("Gem").visible=set>=3;
   if(parts.containsKey("ShoulderplateL2"))parts.get("ShoulderplateL2").visible=set>=3;
   if(parts.containsKey("ShoulderplateL3"))parts.get("ShoulderplateL3").visible=set>=3;
   if(parts.containsKey("ShoulderplateR2"))parts.get("ShoulderplateR2").visible=set>=3;
   if(parts.containsKey("ShoulderplateR3"))parts.get("ShoulderplateR3").visible=set>=3;
   if(parts.containsKey("SidepanelL3"))parts.get("SidepanelL3").visible=set>=3;
   if(parts.containsKey("SidepanelR3"))parts.get("SidepanelR3").visible=set>=3;
   if(parts.containsKey("Book"))parts.get("Book").visible=set>=2;
   if(parts.containsKey("flapL"))parts.get("flapL").visible=set>=2;
   if(parts.containsKey("flapR"))parts.get("flapR").visible=set>=2;
   if(parts.containsKey("ShoulderplateLtop"))parts.get("ShoulderplateLtop").visible=set>=2;
   if(parts.containsKey("ShoulderplateL1"))parts.get("ShoulderplateL1").visible=set>=2;
   if(parts.containsKey("ShoulderplateRtop"))parts.get("ShoulderplateRtop").visible=set>=2;
   if(parts.containsKey("ShoulderplateR1"))parts.get("ShoulderplateR1").visible=set>=2;
   if(parts.containsKey("SidepanelL2"))parts.get("SidepanelL2").visible=set>=2;
   if(parts.containsKey("SidepanelR2"))parts.get("SidepanelR2").visible=set>=2;
  }
  @Override public void renderToBuffer(PoseStack pose,VertexConsumer vertices,int light,int overlay,int color){rendered++;update();head.xScale=head.yScale=head.zScale=entity!=null&&entity.isBaby()?1:1.01f;super.renderToBuffer(pose,vertices,light,overlay,color);}
 }
 static ModelPart root(boolean chest){var mesh=new MeshDefinition();var root=mesh.getRoot();var head=root.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.ZERO);root.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);var body=root.addOrReplaceChild("body",CubeListBuilder.create(),PartPose.ZERO);var right_arm=root.addOrReplaceChild("right_arm",CubeListBuilder.create(),PartPose.offset(-5,2,0));var left_arm=root.addOrReplaceChild("left_arm",CubeListBuilder.create(),PartPose.offset(5,2,0));var right_leg=root.addOrReplaceChild("right_leg",CubeListBuilder.create(),PartPose.offset(-1.9f,12,0));var left_leg=root.addOrReplaceChild("left_leg",CubeListBuilder.create(),PartPose.offset(1.9f,12,0));
  head.addOrReplaceChild("Goggles",CubeListBuilder.create().texOffs(100,18).addBox(-4.5f, -5.0f, -4.25f, 9, 5, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  head.addOrReplaceChild("OrnamentL",CubeListBuilder.create().texOffs(78,8).mirror().addBox(1.5f, -9.0f, -6.5f, 2, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.1396263f, 0.0f, 0.0f));
  head.addOrReplaceChild("OrnamentL2",CubeListBuilder.create().texOffs(78,8).mirror().addBox(3.5f, -10.0f, -6.5f, 1, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.1396263f, 0.0f, 0.0f));
  head.addOrReplaceChild("OrnamentR",CubeListBuilder.create().texOffs(78,8).addBox(-3.5f, -9.0f, -6.5f, 2, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.1396263f, 0.0f, 0.0f));
  head.addOrReplaceChild("OrnamentR2",CubeListBuilder.create().texOffs(78,8).addBox(-4.5f, -10.0f, -6.5f, 1, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.1396263f, 0.0f, 0.0f));
  head.addOrReplaceChild("Helmet",CubeListBuilder.create().texOffs(41,8).addBox(-4.5f, -9.0f, -4.5f, 9, 4, 9),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  head.addOrReplaceChild("HelmetR",CubeListBuilder.create().texOffs(21,13).addBox(-6.5f, -3.0f, -4.5f, 1, 5, 9),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.5235988f));
  head.addOrReplaceChild("HelmetL",CubeListBuilder.create().texOffs(21,13).mirror().addBox(5.5f, -3.0f, -4.5f, 1, 5, 9),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.5235988f));
  head.addOrReplaceChild("HelmetB",CubeListBuilder.create().texOffs(41,21).addBox(-4.5f, -3.0f, 5.5f, 9, 5, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.5235988f, 0.0f, 0.0f));
  head.addOrReplaceChild("capsthingy",CubeListBuilder.create().texOffs(21,0).addBox(-4.5f, -6.0f, -6.5f, 9, 1, 2),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  head.addOrReplaceChild("flapR",CubeListBuilder.create().texOffs(59,10).addBox(-10.0f, -2.0f, -1.0f, 3, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, -0.5235988f, 0.5235988f));
  head.addOrReplaceChild("flapL",CubeListBuilder.create().texOffs(59,10).mirror().addBox(7.0f, -2.0f, -1.0f, 3, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.5235988f, -0.5235988f));
  head.addOrReplaceChild("Gemornament",CubeListBuilder.create().texOffs(68,11).addBox(-1.5f, -9.0f, -7.0f, 3, 3, 2),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.1396263f, 0.0f, 0.0f));
  head.addOrReplaceChild("Gem",CubeListBuilder.create().texOffs(72,8).addBox(-1.0f, -8.5f, -7.5f, 2, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.1396263f, 0.0f, 0.0f));
  if(chest)body.addOrReplaceChild("BeltR",CubeListBuilder.create().texOffs(76,44).addBox(-5.0f, 4.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  if(!chest)body.addOrReplaceChild("Mbelt",CubeListBuilder.create().texOffs(56,55).addBox(-4.0f, 8.0f, -3.0f, 8, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  if(!chest)body.addOrReplaceChild("MbeltL",CubeListBuilder.create().texOffs(76,44).addBox(4.0f, 8.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  if(!chest)body.addOrReplaceChild("MbeltR",CubeListBuilder.create().texOffs(76,44).addBox(-5.0f, 8.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  if(chest)body.addOrReplaceChild("BeltL",CubeListBuilder.create().texOffs(76,44).addBox(4.0f, 4.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  if(chest)body.addOrReplaceChild("Chestplate",CubeListBuilder.create().texOffs(56,45).addBox(-4.0f, 1.0f, -4.0f, 8, 7, 2),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  if(chest)body.addOrReplaceChild("Scroll",CubeListBuilder.create().texOffs(34,27).addBox(-2.0f, 9.5f, 4.0f, 8, 3, 3),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.1919862f));
  if(chest)body.addOrReplaceChild("Backplate",CubeListBuilder.create().texOffs(36,45).addBox(-4.0f, 1.0f, 2.0f, 8, 11, 2),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  if(chest)body.addOrReplaceChild("Book",CubeListBuilder.create().texOffs(100,8).addBox(1.0f, -0.3f, 4.0f, 5, 7, 2),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.7679449f));
  right_arm.addOrReplaceChild("ShoulderR",CubeListBuilder.create().texOffs(56,35).addBox(-3.5f, -2.5f, -2.5f, 5, 5, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  right_arm.addOrReplaceChild("GauntletR",CubeListBuilder.create().texOffs(100,26).addBox(-3.5f, 3.5f, -2.5f, 2, 6, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  right_arm.addOrReplaceChild("GauntletstrapR1",CubeListBuilder.create().texOffs(84,31).addBox(-1.5f, 3.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  right_arm.addOrReplaceChild("GauntletstrapR2",CubeListBuilder.create().texOffs(84,31).addBox(-1.5f, 6.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  right_arm.addOrReplaceChild("ShoulderplateRtop",CubeListBuilder.create().texOffs(110,37).addBox(-5.5f, -2.5f, -3.5f, 2, 1, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.4363323f));
  right_arm.addOrReplaceChild("ShoulderplateR1",CubeListBuilder.create().texOffs(110,45).addBox(-4.5f, -1.5f, -3.5f, 1, 4, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.4363323f));
  right_arm.addOrReplaceChild("ShoulderplateR2",CubeListBuilder.create().texOffs(94,45).addBox(-3.5f, 1.5f, -3.5f, 1, 3, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.4363323f));
  right_arm.addOrReplaceChild("ShoulderplateR3",CubeListBuilder.create().texOffs(94,45).addBox(-2.5f, 3.5f, -3.5f, 1, 3, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.4363323f));
  left_arm.addOrReplaceChild("ShoulderL",CubeListBuilder.create().texOffs(56,35).mirror().addBox(-1.5f, -2.5f, -2.5f, 5, 5, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  left_arm.addOrReplaceChild("GauntletL",CubeListBuilder.create().texOffs(114,26).addBox(1.5f, 3.5f, -2.5f, 2, 6, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  left_arm.addOrReplaceChild("Gauntletstrapl1",CubeListBuilder.create().texOffs(84,31).mirror().addBox(-1.5f, 3.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  left_arm.addOrReplaceChild("GauntletstrapL2",CubeListBuilder.create().texOffs(84,31).mirror().addBox(-1.5f, 6.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
  left_arm.addOrReplaceChild("ShoulderplateLtop",CubeListBuilder.create().texOffs(110,37).mirror().addBox(3.5f, -2.5f, -3.5f, 2, 1, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.4363323f));
  left_arm.addOrReplaceChild("ShoulderplateL1",CubeListBuilder.create().texOffs(110,45).mirror().addBox(3.5f, -1.5f, -3.5f, 1, 4, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.4363323f));
  left_arm.addOrReplaceChild("ShoulderplateL2",CubeListBuilder.create().texOffs(94,45).mirror().addBox(2.5f, 1.5f, -3.5f, 1, 3, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.4363323f));
  left_arm.addOrReplaceChild("ShoulderplateL3",CubeListBuilder.create().texOffs(94,45).mirror().addBox(1.5f, 3.5f, -3.5f, 1, 3, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.4363323f));
  right_leg.addOrReplaceChild("LegpanelR1",CubeListBuilder.create().texOffs(0,51).addBox(-1.0f, 0.5f, -3.5f, 3, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
  right_leg.addOrReplaceChild("LegpanelR2",CubeListBuilder.create().texOffs(8,51).addBox(-1.0f, 3.5f, -2.5f, 3, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
  right_leg.addOrReplaceChild("LegpanelR3",CubeListBuilder.create().texOffs(0,56).addBox(-1.0f, 6.5f, -1.5f, 3, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
  right_leg.addOrReplaceChild("LegpanelR4",CubeListBuilder.create().texOffs(0,43).addBox(-3.0f, 0.5f, -3.5f, 2, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
  right_leg.addOrReplaceChild("LegpanelR5",CubeListBuilder.create().texOffs(0,47).addBox(-3.0f, 2.5f, -2.5f, 2, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
  right_leg.addOrReplaceChild("LegpanelR6",CubeListBuilder.create().texOffs(6,43).addBox(-3.0f, 4.5f, -1.5f, 2, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
  right_leg.addOrReplaceChild("SidepanelR1",CubeListBuilder.create().texOffs(0,22).addBox(-2.5f, 0.5f, -2.5f, 1, 4, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.4363323f));
  right_leg.addOrReplaceChild("SidepanelR2",CubeListBuilder.create().texOffs(0,31).addBox(-1.5f, 3.5f, -2.5f, 1, 3, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.4363323f));
  right_leg.addOrReplaceChild("SidepanelR3",CubeListBuilder.create().texOffs(12,31).addBox(-0.5f, 5.5f, -2.5f, 1, 3, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.4363323f));
  right_leg.addOrReplaceChild("BackpanelR1",CubeListBuilder.create().texOffs(0,18).addBox(-3.0f, 0.5f, 2.5f, 5, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.4363323f, 0.0f, 0.0f));
  right_leg.addOrReplaceChild("BackpanelR2",CubeListBuilder.create().texOffs(0,18).addBox(-3.0f, 2.5f, 1.5f, 5, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.4363323f, 0.0f, 0.0f));
  right_leg.addOrReplaceChild("BackpanelR3",CubeListBuilder.create().texOffs(0,18).addBox(-3.0f, 4.5f, 0.5f, 5, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.4363323f, 0.0f, 0.0f));
  left_leg.addOrReplaceChild("BackpanelL3",CubeListBuilder.create().texOffs(0,18).mirror().addBox(-2.0f, 4.5f, 0.5f, 5, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.4363323f, 0.0f, 0.0f));
  left_leg.addOrReplaceChild("LegpanelL1",CubeListBuilder.create().texOffs(0,51).mirror().addBox(-2.0f, 0.5f, -3.5f, 3, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
  left_leg.addOrReplaceChild("LegpanelL2",CubeListBuilder.create().texOffs(8,51).mirror().addBox(-2.0f, 3.5f, -2.5f, 3, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
  left_leg.addOrReplaceChild("LegpanelL3",CubeListBuilder.create().texOffs(0,56).mirror().addBox(-2.0f, 6.5f, -1.5f, 3, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
  left_leg.addOrReplaceChild("LegpanelL4",CubeListBuilder.create().texOffs(0,43).mirror().addBox(1.0f, 0.5f, -3.5f, 2, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
  left_leg.addOrReplaceChild("LegpanelL5",CubeListBuilder.create().texOffs(0,47).mirror().addBox(1.0f, 2.5f, -2.5f, 2, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
  left_leg.addOrReplaceChild("LegpanelL6",CubeListBuilder.create().texOffs(6,43).mirror().addBox(1.0f, 4.5f, -1.5f, 2, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
  left_leg.addOrReplaceChild("SidepanelL1",CubeListBuilder.create().texOffs(0,22).mirror().addBox(1.5f, 0.5f, -2.5f, 1, 4, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.4363323f));
  left_leg.addOrReplaceChild("SidepanelL2",CubeListBuilder.create().texOffs(0,31).mirror().addBox(0.5f, 3.5f, -2.5f, 1, 3, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.4363323f));
  left_leg.addOrReplaceChild("SidepanelL3",CubeListBuilder.create().texOffs(12,31).mirror().addBox(-0.5f, 5.5f, -2.5f, 1, 3, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.4363323f));
  left_leg.addOrReplaceChild("BackpanelL1",CubeListBuilder.create().texOffs(0,18).mirror().addBox(-2.0f, 0.5f, 2.5f, 5, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.4363323f, 0.0f, 0.0f));
  left_leg.addOrReplaceChild("BackpanelL2",CubeListBuilder.create().texOffs(0,18).mirror().addBox(-2.0f, 2.5f, 1.5f, 5, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.4363323f, 0.0f, 0.0f));
  head.addOrReplaceChild("Mask0",CubeListBuilder.create().texOffs(52,2).addBox(-4.5f,-5,-4.6f,9,5,1),PartPose.ZERO);
  head.addOrReplaceChild("Mask1",CubeListBuilder.create().texOffs(76,2).addBox(-4.5f,-5,-4.6f,9,5,1),PartPose.ZERO);
  head.addOrReplaceChild("Mask2",CubeListBuilder.create().texOffs(100,2).addBox(-4.5f,-5,-4.6f,9,5,1),PartPose.ZERO);
 return LayerDefinition.create(mesh,128,64).bakeRoot();}
 static final List<String> NAMES=List.of("Goggles","OrnamentL","OrnamentL2","OrnamentR","OrnamentR2","Helmet","HelmetR","HelmetL","HelmetB","capsthingy","flapR","flapL","Gemornament","Gem","BeltR","Mbelt","MbeltL","MbeltR","BeltL","Chestplate","Scroll","Backplate","Book","ShoulderR","GauntletR","GauntletstrapR1","GauntletstrapR2","ShoulderplateRtop","ShoulderplateR1","ShoulderplateR2","ShoulderplateR3","ShoulderL","GauntletL","Gauntletstrapl1","GauntletstrapL2","ShoulderplateLtop","ShoulderplateL1","ShoulderplateL2","ShoulderplateL3","LegpanelR1","LegpanelR2","LegpanelR3","LegpanelR4","LegpanelR5","LegpanelR6","SidepanelR1","SidepanelR2","SidepanelR3","BackpanelR1","BackpanelR2","BackpanelR3","BackpanelL3","LegpanelL1","LegpanelL2","LegpanelL3","LegpanelL4","LegpanelL5","LegpanelL6","SidepanelL1","SidepanelL2","SidepanelL3","BackpanelL1","BackpanelL2","Mask0","Mask1","Mask2");
}
