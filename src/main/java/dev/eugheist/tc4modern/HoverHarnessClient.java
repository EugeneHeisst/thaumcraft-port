package dev.eugheist.tc4modern;
import java.util.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.*;
import net.neoforged.neoforge.network.PacketDistributor;
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class HoverHarnessClient {
 static int modelsRendered,ringsRendered,boltsSpawned;
 static final KeyMapping KEY=new KeyMapping("key.tc4modern.hover_harness",InputConstants.Type.KEYSYM,org.lwjgl.glfw.GLFW.GLFW_KEY_H,"key.categories.misc");
 static final Map<Player,Long> HUM=new WeakHashMap<>(),SHOCK=new WeakHashMap<>();
 static void hum(Player p){long time=System.currentTimeMillis();if(HUM.getOrDefault(p,0L)<time){HUM.put(p,time+1200);p.level().playLocalSound(p.getX(),p.getY(),p.getZ(),InfusionContent.MatrixEntity.sound("jacobs"),net.minecraft.sounds.SoundSource.PLAYERS,.05f,1+p.getRandom().nextFloat()*.05f,false);}}
 static void receive(boolean enabled){var p=Minecraft.getInstance().player;if(p==null)return;boolean previous=HoverHarness.active(p);HoverHarness.STATES.put(p,enabled);p.getPersistentData().putBoolean("TC4Hover",enabled);if(!p.getAbilities().instabuild)p.getAbilities().flying=enabled;if(previous!=enabled)p.level().playLocalSound(p.getX(),p.getY(),p.getZ(),InfusionContent.MatrixEntity.sound(enabled?"hhon":"hhoff"),net.minecraft.sounds.SoundSource.PLAYERS,.33f,1,false);}
 @SubscribeEvent static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();while(KEY.consumeClick())if(mc.player!=null&&mc.screen==null&&HoverHarness.chest(mc.player).is(HoverHarness.harness())){boolean enabled=!HoverHarness.active(mc.player);if(!enabled||HoverHarness.fuel(HoverHarness.jar(HoverHarness.chest(mc.player),mc.player.registryAccess()))>0){receive(enabled);PacketDistributor.sendToServer(new HoverHarness.Toggle(enabled));}}}
 @EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)static final class Registration {
  @SubscribeEvent static void keys(RegisterKeyMappingsEvent e){e.register(KEY);}
  @SubscribeEvent static void menus(RegisterMenuScreensEvent e){e.register(HoverHarness.MENU.get(),Screen::new);}
  @SubscribeEvent static void extension(RegisterClientExtensionsEvent e){e.registerItem(new IClientItemExtensions(){Model model;@Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){if(model==null)model=new Model();model.entity=entity;model.stack=stack;return model;}},HoverHarness.harness());}
 }
 static final class Screen extends AbstractContainerScreen<HoverHarness.Menu> {
  Screen(HoverHarness.Menu m,Inventory i,Component title){super(m,i,title);imageWidth=176;imageHeight=166;}
  @Override protected void renderBg(GuiGraphics gui,float partial,int x,int y){gui.blit(AlchemyContent.id("textures/gui/guihoverharness.png"),leftPos,topPos,0,0,imageWidth,imageHeight,256,256);if(!menu.offhand)gui.blit(AlchemyContent.id("textures/gui/guihoverharness.png"),leftPos+8+menu.held*18,topPos+142,240,0,16,16,256,256);}
  @Override protected void renderLabels(GuiGraphics gui,int x,int y){}
  @Override public void render(GuiGraphics gui,int x,int y,float partial){super.render(gui,x,y,partial);renderTooltip(gui,x,y);}
 }
 static final class Model extends HumanoidModel<LivingEntity> {
  static final ObjParts BACK=new ObjParts(AlchemyContent.id("textures/models/hoverharness.obj"));LivingEntity entity;ItemStack stack=ItemStack.EMPTY;
  static ModelPart root(){var mesh=new MeshDefinition();var root=mesh.getRoot();for(String part:List.of("head","hat","right_arm","left_arm","right_leg","left_leg"))root.addOrReplaceChild(part,CubeListBuilder.create(),PartPose.ZERO);root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,16).addBox(-4,0,-2,8,12,4,new CubeDeformation(.6f)),PartPose.ZERO);return LayerDefinition.create(mesh,64,32).bakeRoot();}
  Model(){super(root());}
  @Override public void renderToBuffer(PoseStack poses,VertexConsumer vertices,int light,int overlay,int color){modelsRendered++;super.renderToBuffer(poses,vertices,light,overlay,color);var buffers=Minecraft.getInstance().renderBuffers().bufferSource();poses.pushPose();poses.scale(.1f,.1f,.1f);poses.mulPose(Axis.XN.rotationDegrees(90));if(entity!=null&&entity.isShiftKeyDown())poses.mulPose(Axis.XP.rotationDegrees(28.64789f));poses.translate(0,.33,-3.7);var type=RenderType.entityCutoutNoCull(AlchemyContent.id("textures/models/hoverharness2.png"));BACK.renderAll(type,poses,buffers,LightTexture.FULL_BRIGHT,overlay,color);poses.popPose();
   if(entity==null||NoteData.tag(stack).getByte("hover")!=1)return;poses.pushPose();if(entity.isShiftKeyDown()){poses.mulPose(Axis.XP.rotationDegrees(28.64789f));poses.translate(0,.075,-.05);}poses.translate(0,.2,.55);ring(poses,buffers,2.5f,1);poses.mulPose(Axis.YP.rotationDegrees(180));poses.translate(0,0,.03);ring(poses,buffers,1.5f,.5f);poses.popPose();
   if(entity instanceof Player p){long time=System.currentTimeMillis();if(SHOCK.getOrDefault(p,0L)<time){SHOCK.put(p,time+50+p.getRandom().nextInt(50));double mod=p.isShiftKeyDown()?.075:0;float yaw=p.yBodyRot-90-p.getRandom().nextInt(180),pitch=-80+p.getRandom().nextInt(160);var direction=net.minecraft.world.phys.Vec3.directionFromRotation(pitch,yaw);var start=new net.minecraft.world.phys.Vec3(p.getX(),p.getY()-.45-mod,p.getZ());var hit=p.level().clip(new net.minecraft.world.level.ClipContext(start,start.add(direction.scale(6)),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));if(hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK){double angle=(p.yBodyRot+90)/180*Math.PI;var end=hit.getLocation();boltsSpawned++;NodeBoltParticle.spawn(Minecraft.getInstance().level,p.getX()-Math.cos(angle)*.5,start.y,p.getZ()-Math.sin(angle)*.5,end.x,end.y,end.z,1,2,3,6,.015f);}}}
  }
  static void ring(PoseStack pose,MultiBufferSource buffers,float size,float green){ringsRendered++;var mc=Minecraft.getInstance();var sprite=mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(AlchemyContent.id("items/lightningring"));var type=NodeRenderTypes.additive(TextureAtlas.LOCATION_BLOCKS);var out=buffers.getBuffer(type);float half=size/2;var matrix=pose.last();out.addVertex(matrix,-half,-half,0).setUv(sprite.getU0(),sprite.getV0()).setColor(1,green,1,1);out.addVertex(matrix,half,-half,0).setUv(sprite.getU1(),sprite.getV0()).setColor(1,green,1,1);out.addVertex(matrix,half,half,0).setUv(sprite.getU1(),sprite.getV1()).setColor(1,green,1,1);out.addVertex(matrix,-half,half,0).setUv(sprite.getU0(),sprite.getV1()).setColor(1,green,1,1);}
 }
}
