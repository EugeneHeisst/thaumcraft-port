package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** RenderMindSpider: viewer-bound, tiny translucent body and additive eyes. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class MindSpiderRenderer extends MobRenderer<MindSpider, SpiderModel<MindSpider>> {
    private static final ResourceLocation TEXTURE = texture("taint_spider.png");
    static ShaderInstance bodyShader;
    @SubscribeEvent static void shaders(RegisterShadersEvent event) throws java.io.IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
            ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "mind_spider"), DefaultVertexFormat.NEW_ENTITY),
            shader -> bodyShader = shader);
    }
    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "textures/models/" + name);
    }
    @SubscribeEvent static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WarpEntities.MIND_SPIDER.get(), MindSpiderRenderer::new);
    }
    public MindSpiderRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 0);
        addLayer(new RenderLayer<MindSpider, SpiderModel<MindSpider>>(this) {
            @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, MindSpider spider,
                float swing, float amount, float partial, float age, float yaw, float pitch) {
                getParentModel().renderToBuffer(pose, buffers.getBuffer(spider.isInvisible() ? Body.INVISIBLE_EYES : Body.EYES),
                    15728640, OverlayTexture.NO_OVERLAY);
            }
        });
    }
    private static boolean visibleToViewer(MindSpider spider) {
        var player = Minecraft.getInstance().player;
        return spider.viewer().isEmpty() || player != null && spider.viewer().equals(player.getGameProfile().getName());
    }
    @Override public boolean shouldRender(MindSpider spider, Frustum frustum, double x, double y, double z) {
        return visibleToViewer(spider) && super.shouldRender(spider, frustum, x, y, z);
    }
    @Override public void render(MindSpider spider, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!visibleToViewer(spider)) return;
        float opacity = Math.min(.1f, spider.tickCount / 100f);
        super.render(spider, yaw, partialTick, pose,
            type -> type == Body.TYPE ? new AlphaConsumer(buffers.getBuffer(type), opacity) : buffers.getBuffer(type), light);
    }
    @Override protected RenderType getRenderType(MindSpider spider, boolean bodyVisible, boolean translucent, boolean glowing) {
        return Body.TYPE;
    }
    @Override protected void scale(MindSpider spider, PoseStack pose, float partialTick) { pose.scale(.3f, .3f, .3f); }
    @Override protected float getFlipDegrees(MindSpider spider) { return 180; }
    @Override public ResourceLocation getTextureLocation(MindSpider spider) { return TEXTURE; }

    /** Body blending must not write depth; the eye layer retains its own state. */
    private abstract static class Body extends RenderType {
        private Body() { super("mind_spider", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, () -> {}, () -> {}); }
        static final RenderType TYPE = create("tc4_mind_spider_body", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, false, true, CompositeState.builder()
                .setShaderState(new ShaderStateShard(() -> bodyShader))
                .setTextureState(new TextureStateShard(TEXTURE, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false));
        static final RenderType EYES = eyes(false), INVISIBLE_EYES = eyes(true);
        private static RenderType eyes(boolean invisible) {
            return create("tc4_mind_spider_eyes_" + invisible, DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 1536, false, true, CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(texture("taint_spider_eyes.png"), false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setWriteMaskState(invisible ? COLOR_WRITE : COLOR_DEPTH_WRITE).createCompositeState(false));
        }
    }
    record AlphaConsumer(VertexConsumer delegate, float opacity) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { delegate.setColor(r, g, b, (int) (a * opacity)); return this; }
        @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
    }
}
