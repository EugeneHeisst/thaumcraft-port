package dev.eugheist.tc4modern;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** RenderEmber, RenderFrostShard and the KeyHandler "Change Wand Focus" key (F); the radial picker is FociRadial. */
public final class FociClient {
    private FociClient() {}
    public static final KeyMapping KEY_F = new KeyMapping("key.tc4modern.change_focus", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, "key.categories.misc");
    @EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    static final class ModEvents {
        @SubscribeEvent static void keys(RegisterKeyMappingsEvent e) { e.register(KEY_F); }
        @SubscribeEvent static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(FociContent.EMBER.get(), EmberRenderer::new);
            e.registerEntityRenderer(FociContent.FROST_SHARD.get(), FrostShardRenderer::new);
            e.registerEntityRenderer(FocusProjectiles.EXPLOSIVE_ORB.get(), ExplosiveOrbRenderer::new);
            e.registerEntityRenderer(FocusProjectiles.SHOCK_ORB.get(), CrimsonRenderers.OrbRenderer::new);
        }
    }
    static final ResourceLocation PARTICLES = AlchemyContent.id("textures/misc/particles.png");
    public static final class EmberRenderer extends EntityRenderer<FociEntities.Ember> {
        public EmberRenderer(EntityRendererProvider.Context c) { super(c); shadowRadius = 0; }
        @Override public ResourceLocation getTextureLocation(FociEntities.Ember e) { return PARTICLES; }
        @Override public void render(FociEntities.Ember e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            int p = (int) (8f * ((float) e.tickCount / e.duration));
            float f2 = (7 + p) / 16f, f3 = f2 + .0625f, f4 = .5625f, f5 = f4 + .0625f;
            float fc = (float) e.tickCount / e.duration, scale = .25f + fc;
            pose.pushPose();
            pose.scale(scale, scale, scale);
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            var out = buffers.getBuffer(NodeRenderTypes.additive(PARTICLES));
            var m = pose.last().pose();
            int c = (int) (.9f * 255) << 24 | 0xFFFFFF;
            out.addVertex(m, -.5f, -.5f, 0).setUv(f2, f5).setColor(c);
            out.addVertex(m, .5f, -.5f, 0).setUv(f3, f5).setColor(c);
            out.addVertex(m, .5f, .5f, 0).setUv(f3, f4).setColor(c);
            out.addVertex(m, -.5f, .5f, 0).setUv(f2, f4).setColor(c);
            pose.popPose();
        }
    }
    static final ResourceLocation PARTICLES2 = AlchemyContent.id("textures/misc/particles2.png");
    /** RenderExplosiveOrb: a four-frame particles2.png fireball, alpha blended at .8, scale .7, facing the camera. */
    public static final class ExplosiveOrbRenderer extends EntityRenderer<FocusProjectiles.ExplosiveOrb> {
        public ExplosiveOrbRenderer(EntityRendererProvider.Context c) { super(c); shadowRadius = 0; }
        @Override public ResourceLocation getTextureLocation(FocusProjectiles.ExplosiveOrb e) { return PARTICLES2; }
        @Override public void render(FocusProjectiles.ExplosiveOrb e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            float f2 = (e.tickCount % 4) / 16f, f3 = f2 + .0625f, f4 = .8125f, f5 = f4 + .0625f;
            pose.pushPose();
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            pose.scale(.7f, .7f, .7f);
            var out = buffers.getBuffer(RenderType.entityTranslucent(PARTICLES2));
            var last = pose.last();
            int c = (int) (.8f * 255) << 24 | 0xFFFFFF, l = 220 | 220 << 16;
            out.addVertex(last, -.5f, -.5f, 0).setColor(c).setUv(f2, f5).setOverlay(OverlayTexture.NO_OVERLAY).setLight(l).setNormal(last, 0, 1, 0);
            out.addVertex(last, .5f, -.5f, 0).setColor(c).setUv(f3, f5).setOverlay(OverlayTexture.NO_OVERLAY).setLight(l).setNormal(last, 0, 1, 0);
            out.addVertex(last, .5f, .5f, 0).setColor(c).setUv(f3, f4).setOverlay(OverlayTexture.NO_OVERLAY).setLight(l).setNormal(last, 0, 1, 0);
            out.addVertex(last, -.5f, .5f, 0).setColor(c).setUv(f2, f4).setOverlay(OverlayTexture.NO_OVERLAY).setLight(l).setNormal(last, 0, 1, 0);
            pose.popPose();
        }
    }
    static final ObjParts ORB = new ObjParts(AlchemyContent.id("models/orb.obj"));
    static final ResourceLocation FROST = AlchemyContent.id("textures/blocks/frostshard.png");
    public static final class FrostShardRenderer extends EntityRenderer<FociEntities.FrostShard> {
        public FrostShardRenderer(EntityRendererProvider.Context c) { super(c); }
        @Override public ResourceLocation getTextureLocation(FociEntities.FrostShard e) { return FROST; }
        @Override public void render(FociEntities.FrostShard e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            var rnd = new java.util.Random(e.getId());
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(e.yRotO + (e.getYRot() - e.yRotO) * partial));
            pose.mulPose(Axis.ZP.rotationDegrees(e.xRotO + (e.getXRot() - e.xRotO) * partial));
            float base = e.getDamage() * .1f;
            pose.scale(base + rnd.nextFloat() * .1f, base + rnd.nextFloat() * .1f, base + rnd.nextFloat() * .1f);
            ORB.render("orb", FROST, pose, buffers, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            pose.popPose();
        }
    }
}
