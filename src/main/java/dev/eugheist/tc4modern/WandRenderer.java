package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Port of ItemWandRenderer + ModelWand. First person replays the 1.7.10 ItemRenderer chain (including the
 * EnumAction.bow pull branch, whose Integer.MAX_VALUE float arithmetic is reproduced), Forge's -0.5 render-helper
 * offset and the renderer's EQUIPPED_FIRST_PERSON branch with the use tilt and WAVE/CHARGE wobble. As in 1.7, the
 * arm is not drawn while an item is held. Other contexts map RenderItem/RenderPlayer transforms onto the modern
 * item display pipeline (item model: builtin/entity with identity transforms).
 */
public final class WandRenderer {
    private WandRenderer() {}
    private static ModelPart part(int u, int v, float x, float y, float z, int w, int h, int d, float px, float py, float pz) {
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("p", CubeListBuilder.create().texOffs(u, v).mirror().addBox(x, y, z, w, h, d), PartPose.offset(px, py, pz));
        // ModelWand boxes are created while the model is still 32x32; the later setTextureSize(64, 32) does not affect them.
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }
    private static ModelPart cap, capBottom, rod, focus;
    private static void init() {
        if (cap != null) return;
        cap = part(0, 0, -1, -1, -1, 2, 2, 2, 0, 0, 0);
        capBottom = part(0, 0, -1, -1, -1, 2, 2, 2, 0, 20, 0);
        rod = part(0, 8, -1, -1, -1, 2, 18, 2, 0, 2, 0);
        focus = part(0, 0, -3, -6, -3, 6, 6, 6, 0, 0, 0);
    }
    static ResourceLocation tex(String path) { return AlchemyContent.id("textures/models/" + path + ".png"); }
    static String rodOf(ItemStack s) { return "wand_rod_" + WandParts.rod(s).baseTag(); }
    static String capOf(ItemStack s) { return "wand_cap_" + WandParts.cap(s).tag(); }

    /** ModelWand.render: rods, staff geometry, sceptre caps, focus depth and original rune layers. */
    static void renderModel(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        init();
        var player = Minecraft.getInstance().player;
        int ticks = player == null ? 0 : player.tickCount;
        pose.pushPose();
        boolean staff=WandParts.isStaff(stack),sceptre=WandParts.isSceptre(stack);
        if(staff)pose.translate(0,.2,0);
        int rodLight = light;
        if (WandParts.rod(stack).glowing()) { int j = (int) (200f + Mth.sin(ticks) * 5f + 5f); rodLight = j % 65536 | (j / 65536) << 16 | (light & 0xFFFF0000); }
        pose.pushPose();
        if(staff){pose.translate(0,-.1,0);pose.scale(1.2f,2,1.2f);}
        rod.render(pose, buffers.getBuffer(RenderType.entityTranslucent(tex(rodOf(stack)))), rodLight, overlay);
        pose.popPose();
        pose.pushPose();
        pose.scale(staff?1.3f:1.2f,staff?1.1f:1,staff?1.3f:1.2f);
        var capBuffer = buffers.getBuffer(RenderType.entityTranslucent(tex(capOf(stack))));
        if(sceptre) {
            pose.pushPose();pose.scale(1.3f,1.3f,1.3f);cap.render(pose,capBuffer,light,overlay);pose.popPose();
            pose.pushPose();pose.translate(0,.3,0);pose.scale(1,.66f,1);cap.render(pose,capBuffer,light,overlay);pose.popPose();
        } else cap.render(pose, capBuffer, light, overlay);
        if(staff) {
            pose.translate(0,.225,0);pose.pushPose();pose.scale(1,.66f,1);cap.render(pose,capBuffer,light,overlay);pose.popPose();pose.translate(0,.65,0);
        }
        capBottom.render(pose, capBuffer, light, overlay);
        pose.popPose();
        var focusItem = FociContent.getFocusItem(stack);
        if (focusItem.getItem() instanceof FociContent.Focus f) {
            String depth=f.getDepthTexture(focusItem);
            if(depth!=null) {
                pose.pushPose();pose.translate(0,staff?-.15:-.09,0);pose.scale(staff?.165f:.16f,staff?.1765f:.16f,staff?.165f:.16f);
                depthCube(pose,buffers,AlchemyContent.id("textures/items/"+depth+".png"),light,overlay);
                pose.popPose();
            }
            pose.pushPose();
            if(staff){pose.translate(0,-.0475,0);pose.scale(.525f,.5525f,.525f);}else pose.scale(.5f,.5f,.5f);
            int c = f.getFocusColor(focusItem);
            int argb = (int) ((depth==null?.95f:.6f) * 255) << 24 | c & 0xFFFFFF;
            int j = (int) (195f + Mth.sin(ticks / 3f) * 10f + 10f);
            focus.render(pose, buffers.getBuffer(RenderType.entityTranslucent(tex("wand"))), j % 65536 | (j / 65536) << 16, overlay, argb);
            pose.popPose();
        }
        if(sceptre)for(int rot=0;rot<10;rot++){
            pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(36*rot+ticks));rune(pose,buffers,.16,-.01,-.125,rot,ticks);pose.popPose();
        }
        if(WandParts.rod(stack).runes()){
            pose.pushPose();for(int rot=0;rot<4;rot++){
                pose.mulPose(Axis.YP.rotationDegrees(90));for(int a=0;a<14;a++)rune(pose,buffers,.36+a*.14,-.01,-.08,(a+rot*3)%16,ticks);
            }pose.popPose();
        }
        pose.popPose();
    }
    /** ModelWand.drawRune, script.png's sixteen columns and original pulsing golden colour. */
    static void rune(PoseStack pose,MultiBufferSource buffers,double x,double y,double z,int rune,int ticks){
        float r=Mth.sin((ticks+rune*5)/5f)*.1f+.88f,g=Mth.sin((ticks+rune*5)/7f)*.1f+.63f,alpha=Mth.sin((ticks+rune*5)/10f)*.2f;
        pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees(90));pose.translate(x,y,z);
        float half=.06f+alpha/40f,u=rune*.0625f;var out=buffers.getBuffer(NodeRenderTypes.additive(AlchemyContent.id("textures/misc/script.png")));var m=pose.last().pose();
        out.addVertex(m,-half,half,0).setUv(u+.0625f,1).setColor(r,g,.2f,alpha+.6f);
        out.addVertex(m,half,half,0).setUv(u+.0625f,0).setColor(r,g,.2f,alpha+.6f);
        out.addVertex(m,half,-half,0).setUv(u,0).setColor(r,g,.2f,alpha+.6f);
        out.addVertex(m,-half,-half,0).setUv(u,1).setColor(r,g,.2f,alpha+.6f);pose.popPose();
    }

    /** ModelWand depth layer via BlockRenderer.drawFaces, a centered unit cube. */
    static void depthCube(PoseStack pose,MultiBufferSource buffers,ResourceLocation texture,int light,int overlay) {
        var out=buffers.getBuffer(RenderType.entityTranslucent(texture));
        for(var direction:net.minecraft.core.Direction.values()) {
            pose.pushPose();pose.mulPose(direction.getRotation());
            var last=pose.last();float[][] corners={{-.5f,.5f,-.5f,0,0},{-.5f,.5f,.5f,0,1},{.5f,.5f,.5f,1,1},{.5f,.5f,-.5f,1,0}};
            for(var p:corners)out.addVertex(last,p[0],p[1],p[2]).setColor(-1).setUv(p[3],p[4]).setOverlay(overlay).setLight(light).setNormal(last,0,1,0);
            pose.popPose();
        }
    }

    static boolean using(Player p, ItemStack stack) { return p != null && p.isUsingItem() && p.getUseItem().is(stack.getItem()) && (p.getUseItem() == stack || p.getItemInHand(p.getUsedItemHand()) == p.getUseItem()); }
    /** ItemWandRenderer use branch: tilt with func_71057_bx (ticks used) and WAVE/CHARGE wobble. */
    static void useAnimation(PoseStack pose, Player p, ItemStack stack, float pt, boolean firstPerson) {
        if (!using(p, stack)) return;
        float used = p.getTicksUsingItem() + pt;
        float t = Math.min(used, 3f);
        pose.translate(0, 1, 0);
        if (!firstPerson) pose.mulPose(Axis.ZP.rotationDegrees(33));
        else { pose.mulPose(Axis.XP.rotationDegrees(10)); pose.mulPose(Axis.ZP.rotationDegrees(10)); }
        pose.mulPose(Axis.XN.rotationDegrees(60f * (t / 3f)));
        var focusItem = FociContent.getFocusItem(stack);
        var f = focusItem.getItem() instanceof FociContent.Focus ff ? ff : null;
        boolean node = f == null || !FociContent.usingFocus(p);
        if (node || f.getAnimation(focusItem) == FociContent.Animation.WAVE) {
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(used / 10f) * 10f));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(used / 15f) * 10f));
        } else {
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(used / .8f)));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(used / .7f)));
        }
        pose.translate(0, -1, 0);
    }

    // ---------------- first person ----------------
    @EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
    static final class FirstPerson {
        @SubscribeEvent static void render(RenderHandEvent event) {
            var stack = event.getItemStack();
            if (!WandData.isWand(stack)) return;
            event.setCanceled(true);
            var mc = Minecraft.getInstance();
            var p = mc.player;
            boolean right = (event.getHand() == InteractionHand.MAIN_HAND) == (p.getMainArm() == HumanoidArm.RIGHT);
            var pose = event.getPoseStack();
            float swing = event.getSwingProgress(), lowered = event.getEquipProgress(), pt = event.getPartialTick();
            pose.pushPose();
            if (!right) pose.scale(-1, 1, 1);
            float f13 = .8f;
            float f6 = Mth.sin(swing * Mth.PI), f7 = Mth.sin(Mth.sqrt(swing) * Mth.PI);
            pose.translate(-f7 * .4f, Mth.sin(Mth.sqrt(swing) * Mth.PI * 2) * .2f, -f6 * .2f);
            pose.translate(.7f * f13, -.65f * f13 - lowered * .6f, -.9f * f13);
            pose.mulPose(Axis.YP.rotationDegrees(45));
            f6 = Mth.sin(swing * swing * Mth.PI);
            pose.mulPose(Axis.YP.rotationDegrees(-f6 * 20));
            pose.mulPose(Axis.ZP.rotationDegrees(-f7 * 20));
            pose.mulPose(Axis.XP.rotationDegrees(-f7 * 80));
            pose.scale(.4f, .4f, .4f);
            if (using(p, stack)) {
                // EnumAction.bow branch with getMaxItemUseDuration == Integer.MAX_VALUE, evaluated in float like 1.7.
                pose.mulPose(Axis.ZP.rotationDegrees(-18));
                pose.mulPose(Axis.YP.rotationDegrees(-12));
                pose.mulPose(Axis.XP.rotationDegrees(-8));
                pose.translate(-.9f, .2f, 0);
                int count = Integer.MAX_VALUE - p.getTicksUsingItem();
                float f10 = (float) Integer.MAX_VALUE - ((float) count - pt + 1.0f);
                float f11 = f10 / 20f;
                f11 = (f11 * f11 + f11 * 2f) / 3f;
                if (f11 > 1f) f11 = 1f;
                if (f11 > .1f) pose.translate(0, Mth.sin((f10 - .1f) * 1.3f) * .01f * (f11 - .1f), 0);
                pose.translate(0, 0, f11 * .1f);
                pose.mulPose(Axis.ZP.rotationDegrees(-335));
                pose.mulPose(Axis.YP.rotationDegrees(-50));
                pose.translate(0, .5f, 0);
                pose.scale(1, 1, 1f + f11 * .2f);
                pose.translate(0, -.5f, 0);
                pose.mulPose(Axis.YP.rotationDegrees(50));
                pose.mulPose(Axis.ZP.rotationDegrees(335));
            }
            pose.translate(-.5f, -.5f, -.5f);
            // ItemWandRenderer EQUIPPED_FIRST_PERSON
            if(WandParts.isStaff(stack))pose.translate(0,.5,0);
            pose.translate(.5f, 1.5f, .5f);
            pose.scale(1f, 1.1f, 1f);
            pose.mulPose(Axis.XP.rotationDegrees(180));
            useAnimation(pose, p, stack, pt, true);
            if (!right) pose.scale(-1, 1, 1);
            renderModel(stack, pose, event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    // ---------------- other contexts ----------------
    static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        ItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()); }
        @Override public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            var p = Minecraft.getInstance().player;
            float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            pose.pushPose();
            switch (ctx) {
                case GUI -> {
                    LegacyInventoryTransforms.helper(pose);
                    if(WandParts.isStaff(stack)){pose.translate(0,.5,0);pose.scale(.8f,.8f,.8f);}
                    pose.mulPose(Axis.ZP.rotationDegrees(66));
                    pose.translate(0, .6f, 0);
                    if(WandParts.isStaff(stack))pose.translate(-.7,.6,0);
                    pose.mulPose(Axis.XP.rotationDegrees(180));
                }
                case GROUND, FIXED -> {
                    pose.translate(.5f, .25f, .5f);
                    pose.scale(.5f, .5f, .5f);
                    if(WandParts.isStaff(stack)){pose.translate(0,2,0);pose.scale(.9f,.9f,.9f);}else pose.translate(0,1,0);
                    pose.mulPose(Axis.XP.rotationDegrees(180));
                }
                case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                    boolean left = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                    // Undo ItemInHandLayer's arm->item transform (rotX -90, rotY 180, translate(+-1/16, .125, -.625)).
                    pose.translate(.5f, .5f, .5f);
                    pose.translate(left ? 1 / 16f : -1 / 16f, -.125f, .625f);
                    pose.mulPose(Axis.YP.rotationDegrees(-180));
                    pose.mulPose(Axis.XP.rotationDegrees(90));
                    if (left) pose.scale(-1, 1, 1);
                    // RenderPlayer.renderEquippedItems, isFull3D branch, then Forge -0.5 and EQUIPPED branch.
                    pose.translate(-1 / 16f, 7 / 16f, 1 / 16f);
                    pose.translate(0, .1875f, 0);
                    pose.scale(.625f, -.625f, .625f);
                    pose.mulPose(Axis.XP.rotationDegrees(-100));
                    pose.mulPose(Axis.YP.rotationDegrees(45));
                    pose.translate(-.5f, -.5f, -.5f);
                    if(WandParts.isStaff(stack))pose.translate(0,.5,0);
                    pose.translate(.5f, 1.5f, .5f);
                    pose.mulPose(Axis.XP.rotationDegrees(180));
                    var holder = p;
                    useAnimation(pose, holder, stack, pt, false);
                    if (left) pose.scale(-1, 1, 1);
                }
                default -> { pose.translate(.5f, 1.5f, .5f); pose.mulPose(Axis.XP.rotationDegrees(180)); }
            }
            renderModel(stack, pose, buffers, light, overlay);
            pose.popPose();
        }
    }
    @EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    static final class Registration {
        @SubscribeEvent static void items(RegisterClientExtensionsEvent event) {
            event.registerItem(new IClientItemExtensions() {
                private ItemRenderer renderer;
                @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { if (renderer == null) renderer = new ItemRenderer(); return renderer; }
            }, TC4Modern.BASIC_WAND.get(), TC4Modern.GOLD_GREATWOOD_WAND.get(), WandParts.WAND.get());
        }
    }
}
