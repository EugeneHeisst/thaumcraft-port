package dev.eugheist.tc4modern;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
/** GuiSpa: original tank gauge, salt slot, mix toggle and tooltips. */
public final class SpaScreen extends AbstractContainerScreen<SpaMenu> {
 static final net.minecraft.resources.ResourceLocation TEXTURE=AlchemyContent.id("textures/gui/gui_spa.png");
 public SpaScreen(SpaMenu menu,Inventory inv,Component title){super(menu,inv,title);}
 @Override protected void renderLabels(GuiGraphics gui,int x,int y){}
 @Override protected void renderBg(GuiGraphics gui,float partial,int mouseX,int mouseY){gui.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);gui.blit(TEXTURE,leftPos+89,topPos+35,208,menu.mix()?16:32,8,8);
  if(menu.amount()>0){var ext=net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(menu.fluid());var texture=menu.fluid()==net.minecraft.world.level.material.Fluids.WATER?AlchemyContent.id("blocks/crucible_water"):ext.getStillTexture();if(texture!=null){var sprite=minecraft.getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(texture);for(int i=0;i<6;i++)gui.blit(leftPos+107,topPos+15+i*8,0,8,8,sprite);int cover=(int)(48-48*menu.amount()/5000f);gui.blit(TEXTURE,leftPos+107,topPos+15,107,15,10,cover);}}
  gui.blit(TEXTURE,leftPos+106,topPos+11,232,0,10,55);
 }
 @Override public void render(GuiGraphics gui,int x,int y,float partial){super.render(gui,x,y,partial);renderTooltip(gui,x,y);if(x>=leftPos+104&&x<leftPos+114&&y>=topPos+10&&y<topPos+65&&menu.amount()>0)gui.renderComponentTooltip(font,java.util.List.of(menu.fluid().getFluidType().getDescription(),Component.literal(menu.amount()+" mb")),x,y);if(x>=leftPos+88&&x<leftPos+98&&y>=topPos+34&&y<topPos+44)gui.renderTooltip(font,Component.translatable("text.spa.mix."+menu.mix()),x,y);}
 @Override public boolean mouseClicked(double x,double y,int button){boolean handled=super.mouseClicked(x,y,button);if(x>=leftPos+89&&x<leftPos+97&&y>=topPos+35&&y<topPos+43){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);minecraft.level.playLocalSound(minecraft.player.getX(),minecraft.player.getY(),minecraft.player.getZ(),FociContent.sound("cameraclack"),net.minecraft.sounds.SoundSource.PLAYERS,.4f,1,false);return true;}return handled;}
}
