package dev.eugheist.tc4modern;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Original renderAspectsInGui: Shift reveals scanned item tags, with backgrounds and unknown-aspect masking. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class ItemAspectOverlay {
    record Tag(Aspect aspect,int amount,boolean known){}
    static int rendered;static List<Tag> last=List.of();
    static boolean show(boolean shift){return shift!=WarpClientConfig.displayAspects();}
    static List<Tag> tags(ItemStack stack,net.minecraft.world.level.Level level,CompoundTag knowledge){
        var target=ScanTarget.item(stack,level);
        if(!ResearchKnowledge.isScanned(knowledge,target.knowledgeKey()))return List.of();
        var all=new AspectList();target.aspects().forEach((key,count)->{var aspect=Aspect.getAspect(key);if(aspect!=null)all.add(aspect,count);});
        var data=new CompoundTag();data.put(ResearchKnowledge.KEY,knowledge);var player=ResearchKnowledge.load(data);
        return java.util.Arrays.stream(all.getAspectsSortedAmount()).map(aspect->new Tag(aspect,all.getAmount(aspect),player.hasDiscoveredAspect("current",aspect))).toList();
    }
    @SubscribeEvent static void render(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event){
        var mc=Minecraft.getInstance();last=List.of();
        if(mc.player==null||mc.level==null||!(event.getScreen() instanceof AbstractContainerScreen<?> screen)||!show(Screen.hasShiftDown())||mc.player.containerMenu.getCarried().is(TC4Modern.THAUMOMETER.get()))return;
        var slot=screen.getSlotUnderMouse();if(slot==null||!slot.hasItem())return;
        var tags=tags(slot.getItem(),mc.level,ClientKnowledge.knowledge);if(tags.isEmpty())return;
        var gui=event.getGuiGraphics();int columns=Math.max(1,Math.min(tags.size(),(screen.width-8)/18)),rows=(tags.size()+columns-1)/columns;
        int x=Math.max(4,Math.min(event.getMouseX()+9,screen.width-columns*18-4)),y=Math.max(4,Math.min(event.getMouseY()-34,screen.height-rows*20-4));
        gui.pose().pushPose();gui.pose().translate(0,0,500);com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        for(int i=0;i<tags.size();i++){
            var tag=tags.get(i);int bx=x+i%columns*18,by=y+i/columns*20;
            gui.blit(AlchemyContent.id("textures/aspects/_back.png"),bx-2,by-2,20,20,0,0,16,16,16,16);
            if(tag.known)RecipeBookBridge.drawTag(gui,tag.aspect,tag.amount,bx,by);
            else gui.blit(AlchemyContent.id("textures/aspects/_unknown.png"),bx,by,0,0,16,16,16,16);
        }
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);gui.pose().popPose();last=tags;rendered++;
    }
}
