package dev.eugheist.tc4modern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class InventoryScanning {
    @SubscribeEvent public static void recipes(net.neoforged.neoforge.client.event.RecipesUpdatedEvent event){RecipeAspects.clear();}
    private static int menu=-1,slot=-1,ticks;
    private static ItemStack previous=ItemStack.EMPTY;
    private static void cancel(){if(menu!=-1&&Minecraft.getInstance().getConnection()!=null)PacketDistributor.sendToServer(new InventoryScanPayload(menu,-1,false));menu=-1;slot=-1;ticks=0;previous=ItemStack.EMPTY;}
    private static ItemStack creativeStack(AbstractContainerScreen<?> screen,ItemStack stack){
        return screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen?stack.copy():ItemStack.EMPTY;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(mc.player==null || !(mc.screen instanceof AbstractContainerScreen<?> screen) || !mc.player.containerMenu.getCarried().is(TC4Modern.THAUMOMETER.get())){cancel();return;}
        var hovered=screen.getSlotUnderMouse();
        if(hovered==null || !hovered.hasItem()){cancel();return;}
        int currentMenu=mc.player.containerMenu.containerId;
        if(menu!=currentMenu || slot!=hovered.index || !ItemStack.isSameItemSameComponents(previous,hovered.getItem())){
            menu=currentMenu;slot=hovered.index;ticks=0;previous=hovered.getItem().copy();
            PacketDistributor.sendToServer(new InventoryScanPayload(menu,slot,false,creativeStack(screen,previous)));
        }
        if(++ticks==ThaumometerItem.SCAN_TICKS+2)PacketDistributor.sendToServer(new InventoryScanPayload(menu,slot,true,creativeStack(screen,previous)));
    }
    @SubscribeEvent public static void render(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event){
        var mc=Minecraft.getInstance();
        if(mc.player==null || !(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !mc.player.containerMenu.getCarried().is(TC4Modern.THAUMOMETER.get()))return;
        var hovered=screen.getSlotUnderMouse();
        if(hovered==null || !hovered.hasItem())return;
        var gui=event.getGuiGraphics();int x=event.getMouseX()+16,y=event.getMouseY()+20;
        var target=ScanTarget.item(hovered.getItem(),mc.level);
        gui.pose().pushPose();gui.pose().translate(0,0,400);
        int statusY=y+20;
        if(ResearchKnowledge.isScanned(ClientKnowledge.knowledge,target.knowledgeKey())){
            int columns=Math.max(1,Math.min(8,(screen.width-16)/28));
            int count=target.aspects().size(),rows=Math.max(1,(count+columns-1)/columns);
            int startX=Math.max(8,Math.min(x,screen.width-Math.min(count,columns)*28-8));
            int startY=Math.max(8,Math.min(y,screen.height-rows*20-20));
            gui.fill(startX-2,startY-2,startX+Math.min(count,columns)*28,startY+rows*20-2,0xCC241C35);
            statusY=startY+rows*20;int index=0;
            for(var entry:target.aspects().entrySet()){
                x=startX+(index%columns)*28;y=startY+(index/columns)*20;index++;
                var aspect=thaumcraft.api.aspects.Aspect.getAspect(entry.getKey());int color=aspect.getColor();
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1);
                gui.blit(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/aspects/"+entry.getKey()+".png"),x,y,0,0,16,16,16,16);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
                gui.drawString(mc.font,Integer.toString(entry.getValue()),x+17,y+5,0xFFFFFF);
            }
        }else{
            gui.fill(x,y,x+42,y+5,0xCC241C35);
            gui.fill(x+1,y+1,x+1+Math.min(ticks,20)*2,y+4,0xFFB597E8);
        }
        if(!ClientKnowledge.status.isEmpty() && System.nanoTime()<ClientKnowledge.messageUntil){
            var status=ClientKnowledge.status.startsWith("missing:")?net.minecraft.network.chat.Component.translatable("hud.tc4modern.missing",ClientKnowledge.status.substring(8)):net.minecraft.network.chat.Component.translatable("hud.tc4modern."+ClientKnowledge.status);
            int statusX=Math.max(8,Math.min(event.getMouseX()+16,screen.width-mc.font.width(status)-8));
            gui.drawString(mc.font,status,statusX,Math.min(statusY,screen.height-12),0xEADCB4);
        }
        gui.pose().popPose();
    }
}
