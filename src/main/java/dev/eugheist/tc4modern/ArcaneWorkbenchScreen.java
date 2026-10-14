package dev.eugheist.tc4modern;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
/** Original workbench background and dimensions. */
public final class ArcaneWorkbenchScreen extends AbstractContainerScreen<ArcaneWorkbenchMenu> {
    private static final ResourceLocation BACKGROUND=ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/gui/gui_arcaneworkbench.png");
    public ArcaneWorkbenchScreen(ArcaneWorkbenchMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=190;imageHeight=234;}
    @Override protected void renderBg(GuiGraphics gui,float partial,int x,int y){gui.blit(BACKGROUND,leftPos,topPos,0,0,imageWidth,imageHeight,256,256);
        var data=new net.minecraft.nbt.CompoundTag();data.put(ResearchKnowledge.KEY,ClientKnowledge.knowledge.copy());
        var recipe=ArcaneRecipes.find(menu.grid.getItems(),ResearchProgress.completed(data));if(recipe==null)return;
        var wand=menu.table.getItem(9);var primals=thaumcraft.api.aspects.Aspect.getPrimalAspects();
        for(int i=0;i<primals.size();i++){
            var aspect=primals.get(i);int quantity=recipe.cost().getAmount(aspect);if(quantity<=0)continue;
            float price=quantity*(WandData.isWand(wand)?WandData.craftingModifier(wand,minecraft.player,aspect):1);
            var location=thaumcraft.common.lib.vis.ArcaneCostVisuals.LOCATIONS[i];int bx=leftPos+location[0]-8,by=topPos+location[1]-8;
            int color=aspect.getColor();float alpha=thaumcraft.common.lib.vis.ArcaneCostVisuals.alpha(minecraft.player.tickCount,i,WandData.isWand(wand) && WandData.get(wand,aspect)>=(int)(price*100));
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha);
            gui.blit(aspect.getImage(),bx,by,0,0,16,16,16,16);com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
            gui.drawString(font,new java.text.DecimalFormat("0.##").format(price),bx+9,by+9,0xFFFFFF);
        }
        if(!WandData.consumeCrafting(wand,recipe.cost(),false,minecraft.player)){
            gui.renderItem(recipe.output(),leftPos+160,topPos+64);gui.fill(leftPos+160,topPos+64,leftPos+176,topPos+80,0x99505050);
            gui.drawCenteredString(font,Component.translatable("gui.tc4modern.insufficient_vis"),leftPos+168,topPos+46,0xEE6E6E);
        }}
    @Override protected void renderLabels(GuiGraphics gui,int x,int y){}
    @Override public void render(GuiGraphics gui,int x,int y,float partial){renderBackground(gui,x,y,partial);super.render(gui,x,y,partial);renderTooltip(gui,x,y);}
}
