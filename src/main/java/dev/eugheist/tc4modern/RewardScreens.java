package dev.eugheist.tc4modern;
import net.minecraft.client.gui.GuiGraphics;import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;import net.minecraft.world.entity.player.Inventory;import net.minecraft.network.chat.Component;
public final class RewardScreens {
 public static final class Pouch extends AbstractContainerScreen<FocusPouchMenu>{public Pouch(FocusPouchMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageHeight=232;inventoryLabelY=139;}@Override protected void renderBg(GuiGraphics gui,float partial,int mx,int my){gui.blit(AlchemyContent.id("textures/gui/gui_focuspouch.png"),leftPos,topPos,0,0,imageWidth,imageHeight);}@Override public void render(GuiGraphics gui,int mx,int my,float partial){super.render(gui,mx,my,partial);renderTooltip(gui,mx,my);}}
}
