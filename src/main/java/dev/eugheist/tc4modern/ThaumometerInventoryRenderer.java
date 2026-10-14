package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.*;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

/** Original ItemThaumometerRenderer INVENTORY chain and scanscreen, isolated to GUI context. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ThaumometerInventoryRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ObjParts MODEL=new ObjParts(AlchemyContent.id("models/scanner_original.obj"));
    ThaumometerInventoryRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        var key=new ModelResourceLocation(AlchemyContent.id("thaumometer"),"inventory");
        var original=event.getModels().get(key);if(original!=null)event.getModels().put(key,new GuiModel(original));
    }
    private static final class GuiModel extends BakedModelWrapper<BakedModel> {
        GuiModel(BakedModel original){super(original);}
        @Override public boolean isCustomRenderer(){return true;}
        @Override public BakedModel applyTransform(ItemDisplayContext context,PoseStack pose,boolean left) {
            return context==ItemDisplayContext.GUI?this:originalModel.applyTransform(context,pose,left);
        }
        @Override public java.util.List<BakedModel> getRenderPasses(ItemStack stack,boolean fabulous){return java.util.List.of(this);}
    }
    @SubscribeEvent public static void items(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions(){
            private ThaumometerInventoryRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new ThaumometerInventoryRenderer();return renderer;}
        },TC4Modern.THAUMOMETER.get());
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(context!=ItemDisplayContext.GUI)return;
        pose.pushPose();LegacyInventoryTransforms.helper(pose);
        pose.scale(.5f,.5f,.5f);
        pose.mulPose(Axis.XP.rotationDegrees(60));pose.mulPose(Axis.ZP.rotationDegrees(-30));pose.mulPose(Axis.YP.rotationDegrees(-248));
        MODEL.render("scanner",AlchemyContent.id("textures/models/scanner.png"),pose,buffers,light,overlay,0xFFFFFFFF);
        pose.translate(0,.11f,0);pose.mulPose(Axis.XP.rotationDegrees(90));pose.mulPose(Axis.ZP.rotationDegrees(90));pose.scale(2.5f,2.5f,2.5f);
        var player=Minecraft.getInstance().player;int ticks=player==null?0:player.tickCount;
        int screenLight=(int)(190+Math.sin(ticks)*10+10);
        var out=buffers.getBuffer(RenderType.entityTranslucent(AlchemyContent.id("textures/models/scanscreen.png")));var last=pose.last();
        for(float[] v:new float[][]{{-.5f,-.5f,0,0},{.5f,-.5f,1,0},{.5f,.5f,1,1},{-.5f,.5f,0,1}})
            out.addVertex(last,v[0],v[1],0).setColor(0xFFFFFFFF).setUv(v[2],v[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(screenLight).setNormal(last,0,0,1);
        pose.popPose();
    }
}
