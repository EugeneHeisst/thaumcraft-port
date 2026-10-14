package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Original animated node atlas, submitted in item coordinates without world visibility gating. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class AuraNodeItemRenderer extends BlockEntityWithoutLevelRenderer {
    private AuraNodeItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @SubscribeEvent public static void register(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private AuraNodeItemRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new AuraNodeItemRenderer();return renderer;}
        },TC4Modern.AURA_NODE_ITEM.get());
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();int ticks=(int)(System.nanoTime()/50_000_000L%1_000_000);
        var layers=thaumcraft.common.lib.vis.NodeVisuals.layers(System.nanoTime(),0,ticks,0,64,true,AuraNodeItem.aspects(stack),thaumcraft.api.nodes.NodeType.NORMAL,null);
        pose.pushPose();pose.translate(.5,.5,.5);
        if(context!=ItemDisplayContext.GUI)pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        for(var layer:layers){
            pose.pushPose();/* Original item strips do not rotate. */
            var out=buffers.getBuffer(NodeRenderTypes.get(layer.additive(),false));
            float scale=layer.scale()*.48f,u=layer.frame()/32f,v=layer.strip()/32f;
            int rgb=layer.color(),alpha=Math.min(255,(int)(255*layer.alpha()*(layer.strip()==0?.25f:.5f)));
            for(float[] corner:new float[][]{{-1,-1,0,1},{1,-1,1,1},{1,1,1,0},{-1,1,0,0}})
                out.addVertex(pose.last(),corner[0]*scale,corner[1]*scale,0).setColor(rgb>>16&255,rgb>>8&255,rgb&255,alpha).setUv(u+corner[2]/32f,v+corner[3]/32f);
            pose.popPose();
        }
        pose.popPose();
    }
}
