package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Book-only models do not introduce unfinished devices into the gameplay registry. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class BookDisplayModels {
    static ModelResourceLocation id(String name){return ModelResourceLocation.standalone(ResearchBookText.texture("book/"+name));}
    @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event){for(var name:java.util.List.of("arcane_workbench","lava","salts","spa","jar_void","tallowblock","alchemyblock","brainbox","centrifuge","crystalizer","tube_valve","tube_filter","tube_buffer","tube_restrict","tube_oneway"))event.register(id(name));event.register(id("deconstructor"));event.register(id("node_jar"));for(int i=0;i<7;i++)event.register(id("cluster_"+i));}
    static void draw(GuiGraphics gui,String name,int x,int y){
        if(name.equals("aura_node")){node(gui,x+8,y+8,12);return;}
        // BlockTableRenderer.renderInventoryBlock draws the whole TileDeconstructionTable, thaumometer included: use the real item (its BEWLR).
        if(name.equals("deconstructor")){gui.renderItem(new ItemStack(BasicsContent.DECONSTRUCTOR.get()),x,y);return;}
        var mc=Minecraft.getInstance();var model=mc.getModelManager().getModel(id(name));
        gui.pose().pushPose();gui.pose().translate(x+8,y+8,150);gui.pose().scale(16,-16,16);
        com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        mc.getItemRenderer().render(new ItemStack(Items.STONE),ItemDisplayContext.GUI,false,gui.pose(),gui.bufferSource(),15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,model);
        gui.flush();gui.pose().popPose();if(name.equals("node_jar"))node(gui,x+8,y+8,7);
    }
    private static void node(GuiGraphics gui,int x,int y,int size){
        var mc=Minecraft.getInstance();var aspects=new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.AIR,40).add(thaumcraft.api.aspects.Aspect.FIRE,40).add(thaumcraft.api.aspects.Aspect.WATER,40).add(thaumcraft.api.aspects.Aspect.EARTH,40);
        var layers=thaumcraft.common.lib.vis.NodeVisuals.layers(System.nanoTime(),0,mc.player==null?0:mc.player.tickCount,0,64,true,aspects,thaumcraft.api.nodes.NodeType.NORMAL,null);
        for(var layer:layers){int color=layer.color();float diameter=size*layer.scale()*2;
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.blendFunc(770,layer.additive()?1:771);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,Math.min(.45f,layer.alpha()*.35f));
            gui.pose().pushPose();gui.pose().translate(x,y,180);gui.pose().mulPose(new org.joml.Quaternionf().rotationZ(layer.angle()));gui.pose().scale(diameter/64,diameter/64,1);
            gui.blit(ResearchBookText.texture("textures/misc/nodes.png"),-32,-32,layer.frame()*64,layer.strip()*64,64,64,2048,2048);gui.pose().popPose();
        }
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
    }
}
