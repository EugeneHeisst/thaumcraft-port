package dev.eugheist.tc4modern;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GuiAlchemyFurnace: original background, flame, cook bar, essentia column and glass overlay; no labels. */
public final class AlchemicalFurnaceScreen extends AbstractContainerScreen<AlchemicalFurnaceMenu> {
    private static final ResourceLocation BACKGROUND = AlchemyContent.id("textures/gui/gui_alchemyfurnace.png");
    public AlchemicalFurnaceScreen(AlchemicalFurnaceMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderBg(GuiGraphics gui, float partial, int mouseX, int mouseY) {
        int k = leftPos, l = topPos, i1;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        gui.blit(BACKGROUND, k, l, 0, 0, imageWidth, imageHeight);
        if (menu.burnTime() > 0) {
            i1 = menu.burnTime() * 20 / menu.itemBurnTime();
            gui.blit(BACKGROUND, k + 80, l + 26 + 20 - i1, 176, 20 - i1, 16, i1);
        }
        i1 = menu.cookTime() * 46 / menu.smeltTime();
        gui.blit(BACKGROUND, k + 106, l + 13 + 46 - i1, 216, 46 - i1, 9, i1);
        i1 = menu.vis() * 48 / 50;
        gui.blit(BACKGROUND, k + 61, l + 12 + 48 - i1, 200, 48 - i1, 8, i1);
        gui.blit(BACKGROUND, k + 60, l + 8, 232, 0, 10, 55);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    @Override protected void renderLabels(GuiGraphics gui, int x, int y) {}
    @Override public void render(GuiGraphics gui, int x, int y, float partial) { renderBackground(gui, x, y, partial); super.render(gui, x, y, partial); renderTooltip(gui, x, y); }
}
