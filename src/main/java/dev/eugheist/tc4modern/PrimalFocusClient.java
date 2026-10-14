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

/** RenderPrimalOrb: twelve seeded, primal-colored ray fans and a half-size additive core. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class PrimalFocusClient extends EntityRenderer<PrimalFocus.Orb> {
    private static final int[] COLORS = {0xFFFF7E,16727041,37119,40960,0xEECCFF,0x555577};
    private static final ResourceLocation PARTICLES = ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "textures/misc/particles.png");
    @SubscribeEvent static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PrimalFocus.ORB.get(), PrimalFocusClient::new);
    }
    public PrimalFocusClient(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }
    @Override public ResourceLocation getTextureLocation(PrimalFocus.Orb orb) { return PARTICLES; }
    @Override public void render(PrimalFocus.Orb orb, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
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
                    rays.addVertex(pose.last().pose(), p[0], p[1], p[2]).setColor(COLORS[i / 2] & 0xFFFFFF);
                }
            }
        }
        pose.popPose();
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float u = (orb.tickCount % 13) / 16f, right = u + .0624375f;
        float v = .125f, bottom = v + .0624375f;
        var core = buffers.getBuffer(Types.CORE);
        var matrix = pose.last().pose();
        core.addVertex(matrix, -.25f, -.25f, 0).setUv(u, bottom).setColor(0xCCFFFFFF);
        core.addVertex(matrix, .25f, -.25f, 0).setUv(right, bottom).setColor(0xCCFFFFFF);
        core.addVertex(matrix, .25f, .25f, 0).setUv(right, v).setColor(0xCCFFFFFF);
        core.addVertex(matrix, -.25f, .25f, 0).setUv(u, v).setColor(0xCCFFFFFF);
        pose.popPose();
    }
    static void trail(PrimalFocus.Orb orb) {
        var level=(net.minecraft.client.multiplayer.ClientLevel)orb.level();var r=level.random;var engine=net.minecraft.client.Minecraft.getInstance().particleEngine;
        // Original passes offsets as WORLD coordinates to the target constructor; keep this origin-centered quirk.
        for(int a=0;a<6;a++)engine.add(new PrimalWisp(level,(r.nextFloat()-r.nextFloat())*.2f,(r.nextFloat()-r.nextFloat())*.2f,(r.nextFloat()-r.nextFloat())*.2f,.4f,a,orb,0,false));
        engine.add(new PrimalWisp(level,orb.getX()+(r.nextFloat()-r.nextFloat())*.2f,orb.getY()+(r.nextFloat()-r.nextFloat())*.2f,orb.getZ()+(r.nextFloat()-r.nextFloat())*.2f,.1f,r.nextInt(6),null,0,true));
    }
    static void burst(PrimalFocus.Orb orb) {
        var level=(net.minecraft.client.multiplayer.ClientLevel)orb.level();var r=level.random;var engine=net.minecraft.client.Minecraft.getInstance().particleEngine;
        for(int a=0;a<6;a++)for(int b=0;b<6;b++){
            float x=(r.nextFloat()-r.nextFloat())*.5f,y=(r.nextFloat()-r.nextFloat())*.5f,z=(r.nextFloat()-r.nextFloat())*.5f;
            var p=new PrimalWisp(level,orb.getX()+x,orb.getY()+y,orb.getZ()+z,.4f,b,null,.05f,false);
            p.towards(orb.getX()+x*10,orb.getY()+y*10,orb.getZ()+z*10);engine.add(p);
        }
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
                .setShaderState(new ShaderStateShard(() -> EldritchOrbRenderer.coreShader))
                .setTextureState(new TextureStateShard(PARTICLES, false, false))
                .setTransparencyState(ADD).setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
    }
}
