package dev.eugheist.tc4modern;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Baubles GuiPlayerExpanded, GuiBaublesButton, GuiEvents and KeyHandler (B). */
public final class BaublesScreen extends EffectRenderingInventoryScreen<BaublesMenu> {
    /** expanded_inventory.png from the user's own Baubles JAR, imported into the local resource pack. */
    static final ResourceLocation BACKGROUND = AlchemyContent.id("textures/gui/baubles_expanded_inventory.png");

    public BaublesScreen(BaublesMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("container.crafting"));
        titleLabelX = 106;
        titleLabelY = 16;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(new ToggleButton(this, false));
    }

    @Override protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
    }

    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
        super.render(gui, mouseX, mouseY, partial);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics gui, float partial, int mouseX, int mouseY) {
        gui.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // Original background already contains armour, amulet, ring and belt silhouettes.
        InventoryScreen.renderEntityInInventoryFollowsMouse(gui, leftPos + 26, topPos + 8, leftPos + 75, topPos + 78, 30, .0625f, mouseX, mouseY, minecraft.player);
    }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (Client.KEY.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }

    /** GuiBaublesButton: 10x10 at the top right of the player model, label under it while not hovered. */
    static final class ToggleButton extends Button {
        private final AbstractContainerScreen<?> screen;
        ToggleButton(AbstractContainerScreen<?> screen, boolean toBaubles) {
            super(screen.getGuiLeft() + 66, screen.getGuiTop() + 9, 10, 10,
                Component.translatable(toBaubles ? "button.tc4modern.baubles" : "button.tc4modern.normal"),
                button -> { if (toBaubles) PacketDistributor.sendToServer(BaublesNetwork.Open.INSTANCE); else Client.backToInventory(); }, DEFAULT_NARRATION);
            this.screen = screen;
        }
        @Override protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partial) {
            // The vanilla inventory slides when its recipe book opens; follow it like a re-initialised 1.7 button.
            setX(screen.getGuiLeft() + 66);
            setY(screen.getGuiTop() + 9);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            if (isHovered()) gui.blit(BACKGROUND, getX(), getY(), 200, 48, 10, 10);
            else {
                gui.blit(BACKGROUND, getX(), getY(), 210, 48, 10, 10);
                gui.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + 5, getY() + height, 0xFFFFFF);
            }
        }
    }

    @EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    static final class ClientSetup {
        @SubscribeEvent static void screens(RegisterMenuScreensEvent event) { event.register(BaublesMenu.TYPE.get(), BaublesScreen::new); }
        @SubscribeEvent static void keys(RegisterKeyMappingsEvent event) { event.register(Client.KEY); }
    }

    @EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
    static final class Client {
        static final KeyMapping KEY = new KeyMapping("key.tc4modern.baubles", InputConstants.Type.KEYSYM, InputConstants.KEY_B, KeyMapping.CATEGORY_INVENTORY);

        static void backToInventory() {
            var mc = Minecraft.getInstance();
            mc.player.closeContainer();
            mc.setScreen(new InventoryScreen(mc.player));
        }

        @SubscribeEvent static void tick(ClientTickEvent.Post event) {
            var mc = Minecraft.getInstance();
            while (KEY.consumeClick()) if (mc.player != null && mc.screen == null) PacketDistributor.sendToServer(BaublesNetwork.Open.INSTANCE);
        }

        @SubscribeEvent static void init(ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof InventoryScreen inventory) event.addListener(new ToggleButton(inventory, true));
        }
    }
}
