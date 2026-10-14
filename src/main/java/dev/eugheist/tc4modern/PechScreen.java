package dev.eugheist.tc4modern;
import net.minecraft.client.gui.GuiGraphics;import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;import net.minecraft.network.chat.Component;import net.minecraft.world.entity.player.Inventory;
public final class PechScreen extends AbstractContainerScreen<PechMenu>{
 public PechScreen(PechMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=175;imageHeight=232;}
 @Override protected void renderBg(GuiGraphics gui,float partial,int mx,int my){gui.blit(AlchemyContent.id("textures/gui/gui_pech.png"),leftPos,topPos,0,0,imageWidth,imageHeight);if(menu.canTrade())gui.blit(AlchemyContent.id("textures/gui/gui_pech.png"),leftPos+67,topPos+24,176,0,25,25);}
 @Override protected void renderLabels(GuiGraphics gui,int mx,int my){}
 @Override public void render(GuiGraphics gui,int mx,int my,float partial){super.render(gui,mx,my,partial);renderTooltip(gui,mx,my);}
 @Override public boolean mouseClicked(double x,double y,int button){if(button==0&&x>=leftPos+67&&x<leftPos+92&&y>=topPos+24&&y<topPos+49&&menu.canTrade()){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("pech_dice")),.95f+minecraft.player.getRandom().nextFloat()*.1f,.5f));return true;}return super.mouseClicked(x,y,button);}
}
