package dev.eugheist.tc4modern;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** GuiFocalManipulator and TileFocalManipulatorRenderer. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FocalManipulatorClient {
    private FocalManipulatorClient() {}
    @SubscribeEvent static void screens(RegisterMenuScreensEvent event) { event.register(FocalManipulator.MENU.get(), Screen::new); }
    @SubscribeEvent static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerBlockEntityRenderer(FocalManipulator.ENTITY.get(), Renderer::new); }
    /** BlockStoneDeviceRenderer metadata 13: the inventory block is the table model alone. */
    @SubscribeEvent static void items(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()) {
                    @Override public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(FocalManipulator.BLOCK.get().defaultBlockState(), pose, buffers, light, overlay,
                            net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);
                    }
                };
                return renderer;
            }
        }, FocalManipulator.BLOCK.get().asItem());
    }

    /** The table itself is the block model (ModelArcaneWorkbench with wandtable.png); the focus floats and turns above it. */
    public static final class Renderer implements BlockEntityRenderer<FocalManipulator.TableEntity> {
        public Renderer(BlockEntityRendererProvider.Context context) {}
        @Override public void render(FocalManipulator.TableEntity table, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            var held = table.getItem(0);
            if (table.getLevel() == null || !(held.getItem() instanceof FociContent.Focus)) return;
            var viewer = Minecraft.getInstance().getCameraEntity();
            float ticks = (viewer == null ? 0 : viewer.tickCount) + partial;
            // EntityItem hoverStart = sin(t/14)*.2+.2 rendered in-frame: RenderItem bob and spin from hoverStart, flat item at half size.
            float hover = (float) Math.sin(ticks / 14) * .2f + .2f;
            pose.pushPose();
            pose.translate(.5f, 1f, .5f);
            pose.mulPose(Axis.YP.rotationDegrees(ticks % 360));
            pose.translate(0, Math.sin(hover) * .1f + .1f, 0);
            pose.mulPose(Axis.YP.rotationDegrees((float) Math.toDegrees(hover)));
            pose.scale(.5f, .5f, .5f);
            Minecraft.getInstance().getItemRenderer().renderStatic(held.copyWithCount(1), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, table.getLevel(), 0);
            pose.popPose();
        }
        @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(FocalManipulator.TableEntity table) { return new net.minecraft.world.phys.AABB(table.getBlockPos()).expandTowards(0, 1, 0); }
    }

    public static final class Screen extends AbstractContainerScreen<FocalManipulator.Menu> {
        private static final ResourceLocation TEXTURE = AlchemyContent.id("textures/gui/gui_wandtable.png");
        private static final ResourceLocation PARTICLES = AlchemyContent.id("textures/misc/particles.png");
        private final FocalManipulator.TableEntity table;
        private int selected = -1, rank;
        private long time, nextSparkle;
        private final DecimalFormat formatter = new DecimalFormat("#######.#");
        private final List<FociContent.UpgradeType> possibleUpgrades = new ArrayList<>(), upgrades = new ArrayList<>();
        private AspectList aspects = new AspectList();
        private final HashMap<Long, Sparkle> sparkles = new HashMap<>();
        private static final class Sparkle {
            float x, y, mx, my, r, g, b; long nextframe; int frame;
            Sparkle(float x, float y, float mx, float my, float r, float g, float b) { this.x = x; this.y = y; this.mx = mx; this.my = my; this.r = r; this.g = g; this.b = b; nextframe = System.currentTimeMillis() + 50; }
        }

        public Screen(FocalManipulator.Menu menu, Inventory inv, Component title) {
            super(menu, inv, title);
            table = menu.table;
            imageWidth = 192;
            imageHeight = 233;
        }
        @Override protected void init() {
            super.init();
            if (table != null) { gatherInfo(); if(table.size > 0) selected = table.upgrade; }
        }
        private int xpLevel() { return minecraft.player.experienceLevel; }
        private final java.util.Random random = new java.util.Random();

        @Override public void render(GuiGraphics gui, int mx, int my, float partial) {
            super.render(gui, mx, my, partial);
            renderTooltip(gui, mx, my);
            if (table == null) return;
            int baseX = leftPos, baseY = topPos;
            if (rank > 0) for (int a = 0; a < possibleUpgrades.size(); ++a) {
                int px = mx - (baseX + 48 + a * 16), py = my - (baseY + 104);
                if (px >= 0 && py >= 0 && px < 16 && py < 16) fixedTooltip(gui, possibleUpgrades.get(a));
            }
            if (selected >= 0) {
                if (in(mx, my, baseX + 48, baseY + 48, 36, 36)) gui.renderTooltip(font, Component.translatable("wandtable.text1"), mx, my);
                if (in(mx, my, baseX + 108, baseY + 58, 36, 16)) gui.renderTooltip(font, Component.translatable("wandtable.text2"), mx, my);
                if (table.size == 0 && rank * FocalManipulator.XP_MULT <= xpLevel() && in(mx, my, baseX + 48, baseY + 88, 96, 8))
                    gui.renderTooltip(font, Component.translatable("wandtable.text3"), mx, my);
            }
            for (int a = 0; a < upgrades.size(); ++a)
                if (in(mx, my, baseX + 56 + a * 16, baseY + 32, 16, 16)) fixedTooltip(gui, upgrades.get(a));
        }
        private static boolean in(int mx, int my, int x, int y, int w, int h) { return mx >= x && my >= y && mx < x + w && my < y + h; }
        /** drawHoveringTextFixed: name and wrapped description pinned beside the right edge of the GUI. */
        private void fixedTooltip(GuiGraphics gui, FociContent.UpgradeType u) {
            int width = Math.max(40, this.width - (leftPos + imageWidth - 16));
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.addAll(font.split(Component.translatable(u.nameKey()).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.UNDERLINE), width));
            lines.addAll(font.split(Component.translatable(u.textKey()), width));
            gui.renderTooltip(font, lines, leftPos + imageWidth - 36, topPos + 24);
        }

        @Override protected void renderBg(GuiGraphics gui, float partial, int mx, int my) {
            time = System.currentTimeMillis();
            int k = leftPos, l = topPos;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            gui.blit(TEXTURE, k, l, 0, 0, imageWidth, imageHeight);
            if (table == null) return;
            if (table.getItem(0).isEmpty() || table.rank < 0 || table.reset) {
                rank = 0; selected = -1;
                possibleUpgrades.clear(); upgrades.clear();
                aspects = new AspectList();
                table.reset = false;
                table.rank = 0;
            }
            if (rank > 0) for (int a = 0; a < possibleUpgrades.size(); ++a)
                if (selected == possibleUpgrades.get(a).id()) gui.blit(TEXTURE, k + 48 + a * 16, l + 104, 200, 0, 16, 16);
            if (rank > 0 && selected >= 0 && !table.getItem(0).isEmpty()) {
                int xp = rank * FocalManipulator.XP_MULT;
                if (table.size == 0 && xp <= xpLevel()) gui.blit(TEXTURE, k + 48, l + 88, 8, 240, 96, 8);
                gui.blit(TEXTURE, k + 108, l + 59, 200, 16, 16, 16);
                int start = 0;
                if (table.aspects.size() > 0) for (Aspect aspect : table.aspects.getAspectsSorted()) {
                    if (aspect == null || table.aspects.getAmount(aspect) == 0) continue;
                    int size = (int) (table.aspects.getAmount(aspect) / (float) table.size * 96);
                    int c = aspect.getColor();
                    float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, b = (c & 255) / 255f;
                    RenderSystem.setShaderColor(r, g, b, .9f);
                    gui.blit(TEXTURE, k + 48 + start, l + 88, 112 + start, 240, size, 8);
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    start += size;
                    if (random.nextInt(66) != 0) continue;
                    float x = 48 + start, y = 92;
                    sparkles.put(time, new Sparkle(x, y, (46 + rank * 16 - x) / 9f, (38 - y) / 9f, r, g, b));
                }
                gui.drawString(font, "" + xp, k + 125, l + 64, xp > xpLevel() ? 16151160 : 10092429);
                AspectList al = table.size > 0 ? table.aspects : aspects;
                int q = 0;
                for (Aspect a2 : al.getAspectsSorted()) {
                    if (a2 == null) continue;
                    gui.pose().pushPose();
                    gui.pose().translate(k + 49, l + 68 - al.size() * 2.5, 0);
                    gui.pose().scale(.5f, .5f, .5f);
                    gui.drawString(font, a2.getName(), 0, q * 10, a2.getColor());
                    gui.drawString(font, formatter.format(al.getAmount(a2) / 100f), 48, q * 10, a2.getColor());
                    gui.pose().popPose();
                    ++q;
                }
            }
            if (rank > 0) {
                if (nextSparkle < time) {
                    nextSparkle = time + (table.size > 0 ? 10 : 500) + random.nextInt(200);
                    sparkles.put(time, new Sparkle(42 + rank * 16 + random.nextInt(12), 34 + random.nextInt(12), 0, 0,
                        .5f + random.nextFloat() * .4f, 1 - random.nextFloat() * .4f, 1 - random.nextFloat() * .4f));
                }
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                for (int a = 0; a < possibleUpgrades.size(); ++a) gui.blit(possibleUpgrades.get(a).icon(), k + 48 + a * 16, l + 104, 0, 0, 16, 16, 16, 16);
            } else if (rank == 0 && !table.getItem(0).isEmpty()) {
                try { gatherInfo(); } catch (Exception ignored) {}
            }
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            for (int a = 0; a < upgrades.size(); ++a) gui.blit(upgrades.get(a).icon(), k + 56 + a * 16, l + 32, 0, 0, 16, 16, 16, 16);
            RenderSystem.disableBlend();
        }

        private void gatherInfo() {
            possibleUpgrades.clear(); upgrades.clear();
            aspects = new AspectList();
            var stack = table.getItem(0);
            if (!(stack.getItem() instanceof FociContent.Focus focus)) return;
            short[] s = focus.getAppliedUpgrades(stack);
            rank = 1;
            int fu = 0;
            while (rank <= 5 && s[rank - 1] != -1) {
                var type = FociContent.upgradeType(s[rank - 1]);
                if (type != null) upgrades.add(type);
                ++fu; ++rank;
            }
            if (fu == 5) rank = -1;
            else {
                int[] ut = focus.getPossibleUpgradesByRank(stack, rank);
                if (ut == null) return;
                for (int id : ut) if (FociContent.upgradeType(id) != null && focus.canApplyUpgrade(stack, minecraft.player, id, rank)) possibleUpgrades.add(FociContent.upgradeType(id));
            }
            if (table.size > 0) selected = table.upgrade;
        }

        /** drawGuiContainerForegroundLayer: the sparkles, eight 50 ms frames from particles.png row 7, additive. */
        @Override protected void renderLabels(GuiGraphics gui, int mx, int my) {
            for (Long key : sparkles.keySet().toArray(new Long[0])) {
                var s = sparkles.get(key);
                drawSparkle(gui, s.x, s.y, s.frame, s.r, s.g, s.b);
                if (s.nextframe < time) { ++s.frame; s.nextframe = time + 50; s.x += s.mx; s.y += s.my; }
                if (s.frame == 9) sparkles.remove(key);
            }
        }
        private void drawSparkle(GuiGraphics gui, float x, float y, int frame, float r, float g, float b) {
            gui.pose().pushPose();
            gui.pose().translate(x, y, 200);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(770, 1);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, PARTICLES);
            float u0 = frame / 16f, u1 = u0 + .0624375f, v0 = .4375f, v1 = v0 + .0624375f;
            var m = gui.pose().last().pose();
            var buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            buf.addVertex(m, -4, 4, 0).setUv(u1, v1).setColor(r, g, b, .9f);
            buf.addVertex(m, 4, 4, 0).setUv(u1, v0).setColor(r, g, b, .9f);
            buf.addVertex(m, 4, -4, 0).setUv(u0, v0).setColor(r, g, b, .9f);
            buf.addVertex(m, -4, -4, 0).setUv(u0, v1).setColor(r, g, b, .9f);
            BufferUploader.drawWithShader(buf.buildOrThrow());
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            gui.pose().popPose();
        }

        @Override public boolean mouseClicked(double mxd, double myd, int button) {
            if (table != null) {
                int mx = (int) mxd, my = (int) myd, gx = leftPos, gy = topPos;
                if (table.size == 0 && selected >= 0 && rank * FocalManipulator.XP_MULT <= xpLevel() && in(mx, my, gx + 48, gy + 88, 96, 8)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selected);
                    click();
                    return true;
                }
                if (table.size == 0) for (var u : possibleUpgrades) {
                    int a = possibleUpgrades.indexOf(u);
                    if (!in(mx, my, gx + 48 + a * 16, gy + 104, 16, 16)) continue;
                    aspects = new AspectList();
                    if (selected == u.id()) selected = -1;
                    else { selected = u.id(); aspects = FocalManipulator.cost(selected, rank); }
                    click();
                    return true;
                }
            }
            return super.mouseClicked(mxd, myd, button);
        }
        private void click() {
            var p = minecraft.player;
            p.level().playLocalSound(p.getX(), p.getY(), p.getZ(), FociContent.sound("cameraclack"), net.minecraft.sounds.SoundSource.PLAYERS, .4f, 1f, false);
        }
    }
}
