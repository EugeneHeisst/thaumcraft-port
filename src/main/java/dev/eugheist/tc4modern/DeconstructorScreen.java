package dev.eugheist.tc4modern;
import net.minecraft.client.gui.GuiGraphics;import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;import net.minecraft.world.entity.player.Inventory;import net.minecraft.network.chat.Component;
public final class DeconstructorScreen extends AbstractContainerScreen<DeconstructorMenu>{
 public DeconstructorScreen(DeconstructorMenu m,Inventory i,Component t){super(m,i,t);}
 @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){g.blit(AlchemyContent.id("textures/gui/gui_decontable.png"),leftPos,topPos,0,0,imageWidth,imageHeight);int n=menu.time.get()*46/40;if(n>0)g.blit(AlchemyContent.id("textures/gui/gui_decontable.png"),leftPos+93,topPos+61-n,176,46-n,9,n);var a=menu.aspect();if(a!=null)BasicsBookRecipes.aspect(g,a,0,leftPos+64,topPos+48);}
 @Override protected void renderLabels(GuiGraphics g,int x,int y){}
 @Override public void render(GuiGraphics g,int x,int y,float p){super.render(g,x,y,p);renderTooltip(g,x,y);if(menu.aspect()!=null&&x>=leftPos+64&&x<leftPos+80&&y>=topPos+48&&y<topPos+64)g.renderComponentTooltip(font,java.util.List.of(Component.literal(menu.aspect().getName()),Component.literal(menu.aspect().getLocalizedDescription())),x,y);}
 @Override public boolean mouseClicked(double x,double y,int b){if(menu.aspect()!=null&&x>=leftPos+64&&x<leftPos+80&&y>=topPos+48&&y<topPos+64){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("hhoff")),1f,.2f));return true;}return super.mouseClicked(x,y,b);}
}
