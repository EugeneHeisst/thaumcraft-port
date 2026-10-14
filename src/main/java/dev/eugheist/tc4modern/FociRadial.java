package dev.eugheist.tc4modern;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * KeyHandler (F) and REHWandHandler.handleFociRadial/renderFocusRadialHUD: holding F with a wand frees the cursor and
 * lays every focus from the inventory, focus pouches and the worn pouch around the screen centre. Hovering grows a
 * focus; releasing F over it, or clicking it, sends the change. Sneak+F removes the current focus.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class FociRadial {
    private FociRadial() {}
    private static final ResourceLocation RADIAL = AlchemyContent.id("textures/misc/radial.png"), RADIAL2 = AlchemyContent.id("textures/misc/radial2.png");
    static boolean radialActive, radialLock;
    private static boolean keyPressedF, lastState;
    private static float radialHudScale;
    private static long lastTime;
    private static final TreeMap<String, Integer> FOCI = new TreeMap<>();
    private static final Map<String, ItemStack> ITEMS = new HashMap<>();
    private static final Map<String, Boolean> HOVER = new HashMap<>();
    private static final Map<String, Float> SCALE = new HashMap<>();

    /** KeyHandler.playerTick START. */
    @SubscribeEvent static void tick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        while (FociClient.KEY_F.consumeClick()) {}
        if (player == null) return;
        if (FociClient.KEY_F.isDown()) {
            if (mc.screen != null) return;
            if (!keyPressedF) radialLock = false;
            if (!radialLock && WandData.isWand(player.getMainHandItem()) && !WandParts.isSceptre(player.getMainHandItem())) {
                if (player.isShiftKeyDown()) {
                    if (!keyPressedF) PacketDistributor.sendToServer(new FociContent.FocusChange("REMOVE"));
                } else radialActive = true;
            }
            if(!keyPressedF&&player.getMainHandItem().is(Golemancy.BELL.get()))PacketDistributor.sendToServer(new Golemancy.Reset());
            keyPressedF = true;
        } else {
            radialActive = false;
            keyPressedF = false;
        }
    }

    /** Collects foci like the radial did: worn pouches first, then the 36 inventory slots and their pouches. */
    private static void gather(net.minecraft.world.entity.player.Player player) {
        FOCI.clear(); ITEMS.clear(); HOVER.clear(); SCALE.clear();
        int pouches = 0;
        var baubles = BaublesInventory.of(player);
        for (int a = 0; a < baubles.getContainerSize(); a++) {
            var item = baubles.getItem(a);
            if (!(item.getItem() instanceof FocusPouchItem)) continue;
            pouches++;
            var contents = FocusPouchItem.contents(item);
            for (int q = 0; q < contents.size(); q++) add(contents.get(q), q + pouches * 1000);
        }
        var inventory = player.getInventory();
        for (int a = 0; a < 36; a++) {
            var item = inventory.items.get(a);
            add(item, a);
            if (!(item.getItem() instanceof FocusPouchItem)) continue;
            pouches++;
            var contents = FocusPouchItem.contents(item);
            for (int q = 0; q < contents.size(); q++) add(contents.get(q), q + pouches * 1000);
        }
    }
    private static void add(ItemStack item, int slot) {
        if (!(item.getItem() instanceof FociContent.Focus focus)) return;
        String key = focus.getSortingHelper(item);
        FOCI.put(key, slot);
        ITEMS.put(key, item.copy());
        SCALE.put(key, 1f);
        HOVER.put(key, false);
    }

    /** handleFociRadial, drawn after the vanilla HUD like the original TEXT overlay. */
    @SubscribeEvent static void render(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !radialActive && radialHudScale <= 0) return;
        long time = System.nanoTime() / 1000000L;
        if (radialActive) {
            if (mc.screen != null) {
                radialActive = false;
                radialLock = true;
                mc.mouseHandler.grabMouse();
                return;
            }
            if (radialHudScale == 0) {
                gather(mc.player);
                if (!FOCI.isEmpty() && mc.mouseHandler.isMouseGrabbed()) mc.mouseHandler.releaseMouse();
            }
        } else if (mc.screen == null && lastState) {
            if (mc.isWindowActive() && !mc.mouseHandler.isMouseGrabbed()) mc.mouseHandler.grabMouse();
            lastState = false;
        }
        draw(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(true));
        if (time > lastTime) {
            for (var key : HOVER.keySet()) {
                if (HOVER.get(key)) {
                    if (!radialActive && !radialLock) {
                        PacketDistributor.sendToServer(new FociContent.FocusChange(key));
                        radialLock = true;
                    }
                    if (SCALE.get(key) < 1.3f) SCALE.put(key, SCALE.get(key) + .025f);
                } else if (SCALE.get(key) > 1) SCALE.put(key, SCALE.get(key) - .025f);
            }
            if (!radialActive) radialHudScale -= .05f;
            else if (radialHudScale < 1) radialHudScale += .05f;
            if (radialHudScale > 1) radialHudScale = 1;
            if (radialHudScale < 0) { radialHudScale = 0; radialLock = false; }
            lastTime = time + 5;
            lastState = radialActive;
        }
    }

    /** renderFocusRadialHUD. */
    private static void draw(GuiGraphics gui, float partial) {
        var mc = Minecraft.getInstance();
        var wand = mc.player.getMainHandItem();
        if (!WandData.isWand(wand) || ITEMS.isEmpty()) return;
        var window = mc.getWindow();
        double sw = gui.guiWidth(), sh = gui.guiHeight();
        int mouseX = (int) (mc.mouseHandler.xpos() * sw / window.getScreenWidth());
        int mouseY = (int) (mc.mouseHandler.ypos() * sh / window.getScreenHeight());
        boolean click = GLFW.glfwGetMouseButton(window.getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        var pose = gui.pose();
        pose.pushPose();
        pose.translate(sw / 2, sh / 2, 0);
        ItemStack tooltip = ItemStack.EMPTY;
        float width = 16 + ITEMS.size() * 2.5f;
        float spin = partial + mc.player.tickCount % 720 / 2f;
        quad(gui, RADIAL, spin, width * 2.75f * radialHudScale);
        quad(gui, RADIAL2, -spin, width * 2.55f * radialHudScale);
        var current = FociContent.getFocusItem(wand);
        if (!current.isEmpty()) {
            gui.renderItem(new ItemStack(current.getItem()), -8, -8);
            int mx = (int) (mouseX - sw / 2), my = (int) (mouseY - sh / 2);
            if (mx >= -10 && mx <= 10 && my >= -10 && my <= 10) tooltip = current;
        }
        pose.scale(radialHudScale, radialHudScale, radialHudScale);
        float rot = -90 * radialHudScale, slice = 360f / ITEMS.size();
        String key = FOCI.firstKey();
        for (int a = 0; a < ITEMS.size() && key != null; a++) {
            double xx = Mth.cos(rot * Mth.DEG_TO_RAD) * width, yy = Mth.sin(rot * Mth.DEG_TO_RAD) * width;
            rot += slice;
            pose.pushPose();
            pose.translate(xx, yy, 100);
            float s = SCALE.get(key);
            pose.scale(s, s, s);
            gui.renderItem(new ItemStack(ITEMS.get(key).getItem()), -8, -8);
            pose.popPose();
            if (!radialLock && radialActive) {
                int mx = (int) (mouseX - sw / 2 - xx), my = (int) (mouseY - sh / 2 - yy);
                if (mx >= -10 && mx <= 10 && my >= -10 && my <= 10) {
                    HOVER.put(key, true);
                    tooltip = ITEMS.get(key);
                    if (click) {
                        radialActive = false;
                        radialLock = true;
                        PacketDistributor.sendToServer(new FociContent.FocusChange(key));
                        break;
                    }
                } else HOVER.put(key, false);
            }
            key = FOCI.higherKey(key);
        }
        pose.popPose();
        if (!tooltip.isEmpty())
            gui.renderTooltip(mc.font, net.minecraft.client.gui.screens.Screen.getTooltipFromItem(mc, tooltip), java.util.Optional.empty(), (int) (sw / 2) - 4, (int) (sh / 2) + 20);
    }

    /** renderQuadCenteredFromTexture(size, .5, .5, .5, 200, 771, .5) under a spin about the screen normal. */
    private static void quad(GuiGraphics gui, ResourceLocation texture, float degrees, float size) {
        var pose = gui.pose();
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        var m = pose.last().pose();
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float h = size / 2;
        buffer.addVertex(m, -h, h, 0).setUv(0, 1).setColor(.5f, .5f, .5f, .5f);
        buffer.addVertex(m, h, h, 0).setUv(1, 1).setColor(.5f, .5f, .5f, .5f);
        buffer.addVertex(m, h, -h, 0).setUv(1, 0).setColor(.5f, .5f, .5f, .5f);
        buffer.addVertex(m, -h, -h, 0).setUv(0, 0).setColor(.5f, .5f, .5f, .5f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        pose.popPose();
    }
}
