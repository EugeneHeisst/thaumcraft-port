package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** Original RenderEldritchOrb ray fans and thirteen-frame particle core. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class EldritchOrbRenderer extends EntityRenderer<EldritchOrb> {
    private static final ResourceLocation PARTICLES = ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "textures/misc/particles.png");
    static ShaderInstance coreShader;
    @SubscribeEvent static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WarpEntities.ELDRITCH_ORB.get(), EldritchOrbRenderer::new);
    }
    @SubscribeEvent static void shaders(RegisterShadersEvent event) throws java.io.IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
            ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "eldritch_orb"), DefaultVertexFormat.POSITION_TEX_COLOR),
            shader -> coreShader = shader);
    }
    public EldritchOrbRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }
    @Override public ResourceLocation getTextureLocation(EldritchOrb orb) { return PARTICLES; }
    @Override public void render(EldritchOrb orb, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        Random random = new Random(orb.getId());
        pose.pushPose();
        var rays = buffers.getBuffer(Types.RAYS);
        for (int i = 0; i < 12; i++) {
            pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360));
            pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360));
            pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360));
            pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360));
            pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360));
            pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360 + orb.tickCount / 80f * 360));
            float growth = Math.min(orb.tickCount, 10) / 300f;
            float length = (random.nextFloat() * 20 + 5) * growth;
            float width = (random.nextFloat() * 2 + 1) * growth;
            float[][] corners = {{-.866f * width, length, -.5f * width}, {.866f * width, length, -.5f * width}, {0, length, width}};
            for (int triangle = 0; triangle < 3; triangle++) {
                rays.addVertex(pose.last().pose(), 0, 0, 0).setColor(0xFFFFFFFF);
                for (int corner = 0; corner < 2; corner++) {
                    float[] p = corners[(triangle + corner) % 3];
                    rays.addVertex(pose.last().pose(), p[0], p[1], p[2]).setColor(0x00EECCFF);
                }
            }
        }
        pose.popPose();
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float u = (orb.tickCount % 13) / 16f, right = u + .0624375f;
        float v = .1875f, bottom = v + .0624375f;
        var core = buffers.getBuffer(Types.CORE);
        var matrix = pose.last().pose();
        core.addVertex(matrix, -.5f, -.5f, 0).setUv(u, bottom).setColor(-1);
        core.addVertex(matrix, .5f, -.5f, 0).setUv(right, bottom).setColor(-1);
        core.addVertex(matrix, .5f, .5f, 0).setUv(right, v).setColor(-1);
        core.addVertex(matrix, -.5f, .5f, 0).setUv(u, v).setColor(-1);
        pose.popPose();
    }
    private abstract static class Types extends RenderType {
        private Types() { super("eldritch_orb", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 256, false, false, () -> {}, () -> {}); }
        private static final TransparencyStateShard ADD = new TransparencyStateShard("eldritch_orb_add",
            () -> { RenderSystem.enableBlend(); RenderSystem.blendFunc(770, 1); },
            () -> { RenderSystem.disableBlend(); RenderSystem.defaultBlendFunc(); });
        static final RenderType RAYS = create("tc4_eldritch_orb_rays", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES, 1536, false, false, CompositeState.builder()
                .setShaderState(POSITION_COLOR_SHADER).setTransparencyState(ADD)
                .setCullState(CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        static final RenderType CORE = create("tc4_eldritch_orb_core", DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
                .setShaderState(new ShaderStateShard(() -> coreShader))
                .setTextureState(new TextureStateShard(PARTICLES, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
    }
}
