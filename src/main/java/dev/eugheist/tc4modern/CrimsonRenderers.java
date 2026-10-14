package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** RenderCultist, RenderElectricOrb, TileEldritchCapRenderer, TileEldritchObeliskRenderer and TileBannerRenderer. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CrimsonRenderers {
    private CrimsonRenderers() {}
    static final ModelLayerLocation CULTIST_LAYER = new ModelLayerLocation(AlchemyContent.id("cultist"), "main");
    static final ModelLayerLocation BANNER_LAYER = new ModelLayerLocation(AlchemyContent.id("crimson_banner"), "main");
    private static final ObjParts CAP_MODEL = new ObjParts(AlchemyContent.id("models/obelisk_cap.obj"));
    private static ResourceLocation tex(String path) { return AlchemyContent.id("textures/" + path + ".png"); }

    @SubscribeEvent static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // ModelBiped on the 64x32 cultist skin.
        event.registerLayerDefinition(CULTIST_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 32));
        event.registerLayerDefinition(BANNER_LAYER, CrimsonRenderers::bannerLayer);
    }

    @SubscribeEvent static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CrimsonContent.KNIGHT.get(), CultistRenderer::new);
        event.registerEntityRenderer(CrimsonContent.CLERIC.get(), CultistRenderer::new);
        event.registerEntityRenderer(CrimsonContent.GOLEM_ORB.get(), OrbRenderer::new);
        event.registerBlockEntityRenderer(CrimsonContent.ALTAR_ENTITY.get(), c -> new CapRenderer<>(tex("models/obelisk_cap_altar")));
        event.registerBlockEntityRenderer(CrimsonContent.CAP_ENTITY.get(), c -> new CapRenderer<>(tex("models/obelisk_cap")));
        event.registerBlockEntityRenderer(CrimsonContent.OBELISK_ENTITY.get(), c -> new ObeliskRenderer());
        event.registerBlockEntityRenderer(CrimsonContent.BANNER_ENTITY.get(), BannerRenderer::new);
    }

    @SubscribeEvent static void items(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()) {
                    private BannerRenderer banner;
                    @Override public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                        if (banner == null) banner = new BannerRenderer(Minecraft.getInstance().getEntityModels());
                        pose.pushPose();
                        pose.translate(0, -.5f, 0);
                        var tag=NoteData.tag(stack);banner.draw(0,false,0,pose,buffers,light,overlay,tag.contains("color")?tag.getByte("color"):-1,thaumcraft.api.aspects.Aspect.getAspect(tag.getString("aspect")));
                        pose.popPose();
                    }
                };
                return renderer;
            }
        }, CrimsonContent.BANNER.get().asItem());
    }

    /** RenderCultist: biped on cultist.png; ritualist clerics hover and trail a dark wispy line to the altar. */
    public static final class CultistRenderer extends HumanoidMobRenderer<CultistEntity, HumanoidModel<CultistEntity>> {
        private static final ResourceLocation SKIN = tex("models/cultist");
        private static final ResourceLocation WISPY = tex("misc/wispy");
        static long lineRenders;static float lastLineQuality;
        public CultistRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(CULTIST_LAYER)), .5f);
            addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        }
        @Override public ResourceLocation getTextureLocation(CultistEntity entity) { return SKIN; }

        @Override public void render(CultistEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            boolean ritual = entity instanceof CultistEntity.Cleric cleric && cleric.isRitualist();
            float bob = 0;
            pose.pushPose();
            if (ritual) {
                int seed = new Random(entity.getId()).nextInt(1000);
                bob = Mth.sin((entity.tickCount + partial + seed) / 9f) * .1f + .21f;
                pose.translate(0, bob, 0);
            }
            super.render(entity, yaw, partial, pose, buffers, light);
            pose.popPose();
            if (ritual) floatyLine(entity, partial, bob, pose, buffers);
        }

        /** drawFloatyLine with colour 0x110011, speed -0.03, width 0.25 and golemLinkQuality 16. */
        private static void floatyLine(CultistEntity entity, float partial, float bob, PoseStack pose, MultiBufferSource buffers) {
            var home = entity.getRestrictCenter();
            var origin = entity.getPosition(partial);
            double sx = origin.x, sy = origin.y + entity.getEyeHeight() * 1.2 + bob, sz = origin.z;
            double ex = home.getX() + .5, ey = home.getY() + 1.5, ez = home.getZ() + .5;
            float time = System.nanoTime() / 30000000L, speed = -.03f, width = .25f;
            float growth = Math.min(entity.tickCount, 10) / 10f;
            double cx = sx - ex, cy = sy - ey, cz = sz - ez;
            float dist = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
            float quality=WarpClientConfig.golemLinkQuality()/2f;
            float length = Math.round(dist) * quality;
            if (length <= 0) return;
            lastLineQuality=quality;lineRenders++;
            pose.pushPose();
            pose.translate(ex - origin.x, ey - origin.y, ez - origin.z);
            var out = buffers.getBuffer(Types.line(WISPY));
            var m = pose.last().pose();
            int r = 0x11, g = 0, b = 0x11, a = (int) (.8f * 255);
            for (int strip = 0; strip < 2; strip++) {
                float[] prev = null;
                for (int i = 0; i <= length * growth; i++) {
                    float f2 = i / length, f3 = 1 - Math.abs(i - length / 2) / (length / 2);
                    float phase = dist * (1 - f2) * quality - time % 32767f / 5;
                    float dx = (float) (cx + Mth.sin((float) ((sz % 16 + phase) / 4)) * .5f * f3);
                    float dy = (float) (cy + Mth.sin((float) ((sx % 16 + phase) / 3)) * .5f * f3);
                    float dz = (float) (cz + Mth.sin((float) ((sy % 16 + phase) / 2)) * .5f * f3);
                    float u = (1 - f2) * dist - time * speed;
                    float px = dx * f2, py = dy * f2, pz = dz * f2;
                    float[] cur = strip == 0 ? new float[]{px, py - width, pz, px, py + width, pz, u} : new float[]{px - width, py, pz, px + width, py, pz, u};
                    if (prev != null) {
                        out.addVertex(m, prev[0], prev[1], prev[2]).setUv(prev[6], 1).setColor(r, g, b, a);
                        out.addVertex(m, prev[3], prev[4], prev[5]).setUv(prev[6], 0).setColor(r, g, b, a);
                        out.addVertex(m, cur[3], cur[4], cur[5]).setUv(cur[6], 0).setColor(r, g, b, a);
                        out.addVertex(m, cur[0], cur[1], cur[2]).setUv(cur[6], 1).setColor(r, g, b, a);
                    }
                    prev = cur;
                }
            }
            pose.popPose();
        }
    }

    /** RenderElectricOrb: one animated particles.png frame, additive, pulsing, always facing the camera. */
    public static final class OrbRenderer<T extends net.minecraft.world.entity.Entity> extends EntityRenderer<T> {
        private static final ResourceLocation PARTICLES = tex("misc/particles");
        public OrbRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }
        @Override public ResourceLocation getTextureLocation(T orb) { return PARTICLES; }
        @Override public void render(T orb, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            float u0 = (1 + orb.tickCount % 6) / 8f, u1 = u0 + .125f;
            float v0 = orb instanceof GolemOrb golem && golem.red ? .75f : .875f, v1 = v0 + .125f;
            pose.pushPose();
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            float scale = 1 + Mth.sin(orb.tickCount / 5f) * .2f + .2f;
            pose.scale(scale, scale, scale);
            var out = buffers.getBuffer(NodeRenderTypes.additive(PARTICLES));
            var m = pose.last().pose();
            int a = (int) (.8f * 255);
            out.addVertex(m, -.5f, -.5f, 0).setUv(u0, v1).setColor(255, 255, 255, a);
            out.addVertex(m, .5f, -.5f, 0).setUv(u1, v1).setColor(255, 255, 255, a);
            out.addVertex(m, .5f, .5f, 0).setUv(u1, v0).setColor(255, 255, 255, a);
            out.addVertex(m, -.5f, .5f, 0).setUv(u0, v0).setColor(255, 255, 255, a);
            pose.popPose();
        }
    }

    /** TileEldritchCapRenderer: the obelisk_cap.obj "Cap" standing on the block (altar texture for the altar). */
    public static final class CapRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
        private final ResourceLocation texture;
        CapRenderer(ResourceLocation texture) { this.texture = texture; }
        @Override public void render(T tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            pose.pushPose();
            pose.translate(.5f, 0, .5f);
            pose.mulPose(Axis.XN.rotationDegrees(90));
            CAP_MODEL.render("Cap", texture, pose, buffers, light, overlay, 0xFFFFFFFF);
            pose.popPose();
            if(tile instanceof CrimsonAltarEntity altar&&altar.getEyes()>0){pose.pushPose();pose.translate(.5,0,.5);
                for(int a=0;a<altar.getEyes();a++){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(a*90));pose.translate(.46,.2,0);pose.mulPose(Axis.YP.rotationDegrees(90));pose.mulPose(Axis.XN.rotationDegrees(18));pose.scale(.5f,.5f,.5f);
                    Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(WarpEntities.ELDRITCH_EYE.get()),ItemDisplayContext.FIXED,light,overlay,pose,buffers,tile.getLevel(),0);pose.popPose();}pose.popPose();}
        }
    }

    /**
     * TileEldritchObeliskRenderer: three blocks of starfield (the end-portal technique with tunnel/particlefield; a flat
     * particlefield32 beyond sqrt(512) blocks) behind carved obelisk_side walls, capped top and bottom, bobbing gently.
     */
    public static final class ObeliskRenderer implements BlockEntityRenderer<EldritchBlock.ObeliskEntity> {
        private static final ResourceLocation SIDE = tex("models/obelisk_side"), CAP = tex("models/obelisk_cap"), FAR = tex("misc/particlefield32");
        @Override public void render(EldritchBlock.ObeliskEntity tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            var mc = Minecraft.getInstance();
            var viewer = mc.getCameraEntity();
            if (viewer == null) return;
            var pos = tile.getBlockPos();
            boolean near = viewer.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) < 512;
            float bob = Mth.sin((viewer.tickCount + partial) / 10f) * .1f + .1f;
            float y = 1 + bob;
            int height = 3;
            var m = pose.last().pose();
            if (near) {
                var portal = buffers.getBuffer(Types.PORTAL);
                quad(portal, m, 0, y, .99f, 0, y + height, .99f, 1, y + height, .99f, 1, y, .99f);
                quad(portal, m, 0, y, .01f, 1, y, .01f, 1, y + height, .01f, 0, y + height, .01f);
                quad(portal, m, .99f, y, 0, .99f, y + height, 0, .99f, y + height, 1, .99f, y, 1);
                quad(portal, m, .01f, y, 0, .01f, y, 1, .01f, y + height, 1, .01f, y + height, 0);
            } else {
                var flat = buffers.getBuffer(RenderType.entityCutoutNoCull(FAR));
                float[][] planes = {{0, .99f, 1, .99f}, {0, .01f, 1, .01f}, {.99f, 0, .99f, 1}, {.01f, 0, .01f, 1}};
                for (float[] p : planes) {
                    flatVertex(flat, pose, p[0], y + height, p[1], 1, 1);
                    flatVertex(flat, pose, p[0], y, p[1], 1, 0);
                    flatVertex(flat, pose, p[2], y, p[3], 0, 0);
                    flatVertex(flat, pose, p[2], y + height, p[3], 0, 1);
                }
            }
            // The walls and caps take the light five blocks up, above the obelisk body.
            int wallLight = tile.getLevel() == null ? light : LevelRenderer.getLightColor(tile.getLevel(), pos.above(5));
            pose.pushPose();
            pose.translate(.5f, y, .5f);
            var sides = buffers.getBuffer(RenderType.entityTranslucent(SIDE));
            for (int a = 0; a < 4; a++) {
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(a * 90));
                pose.translate(0, 0, -.5f);
                var last = pose.last();
                sideVertex(sides, last, -.5f, height, 0, 1, wallLight);
                sideVertex(sides, last, .5f, height, 1, 1, wallLight);
                sideVertex(sides, last, .5f, 0, 1, 0, wallLight);
                sideVertex(sides, last, -.5f, 0, 0, 0, wallLight);
                pose.popPose();
            }
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(90));
            CAP_MODEL.render("Cap", CAP, pose, buffers, wallLight, overlay, 0xFFFFFFFF);
            pose.popPose();
            pose.translate(0, height, 0);
            pose.mulPose(Axis.XN.rotationDegrees(90));
            CAP_MODEL.render("Cap", CAP, pose, buffers, wallLight, overlay, 0xFFFFFFFF);
            pose.popPose();
        }
        private static void quad(VertexConsumer out, org.joml.Matrix4f m, float... v) {
            for (int i = 0; i < 12; i += 3) out.addVertex(m, v[i], v[i + 1], v[i + 2]);
        }
        private static void flatVertex(VertexConsumer out, PoseStack pose, float x, float y, float z, float u, float v) {
            out.addVertex(pose.last(), x, y, z).setColor(.5f, .5f, .5f, 1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.pack(11, 0)).setNormal(pose.last(), 0, 1, 0);
        }
        private static void sideVertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float u, float v, int light) {
            out.addVertex(pose, x, y, 0).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
        }
        @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(EldritchBlock.ObeliskEntity tile) { return tile.getRenderBoundingBox(); }
        @Override public int getViewDistance() { return 96; }
    }

    /** ModelBanner, 128x64. */
    private static LayerDefinition bannerLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("b1", CubeListBuilder.create().texOffs(0, 29).mirror().addBox(-5, -7.5f, -1.5f, 2, 3, 3), PartPose.ZERO);
        root.addOrReplaceChild("b2", CubeListBuilder.create().texOffs(0, 29).mirror().addBox(3, -7.5f, -1.5f, 2, 3, 3), PartPose.ZERO);
        root.addOrReplaceChild("beam", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(-7, -7, -1, 14, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("banner", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-7, 0, -.5f, 14, 28, 1), PartPose.offset(0, -5, 0));
        root.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(62, 0).mirror().addBox(0, 0, -1, 2, 31, 2), PartPose.offset(-1, -7, -2));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** TileBannerRenderer for the cult banner (colour -1, no aspect). */
    public static final class BannerRenderer implements BlockEntityRenderer<CrimsonBanner.Entity> {
        private static final ResourceLocation TEXTURE = tex("models/banner_cultist");
        private final ModelPart b1, b2, beam, banner, pole;
        BannerRenderer(BlockEntityRendererProvider.Context context) { this(context.getModelSet()); }
        BannerRenderer(net.minecraft.client.model.geom.EntityModelSet models) {
            var root = models.bakeLayer(BANNER_LAYER);
            b1 = root.getChild("b1"); b2 = root.getChild("b2"); beam = root.getChild("beam"); banner = root.getChild("banner"); pole = root.getChild("pole");
        }
        @Override public void render(CrimsonBanner.Entity tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            var pos = tile.getBlockPos();
            long time = tile.getLevel() == null ? 0 : tile.getLevel().getGameTime();
            float sway = pos.getX() * 7 + pos.getY() * 9 + pos.getZ() * 13 + time + partial;
            draw(tile.facing,tile.wall,sway,pose,buffers,light,overlay,tile.color,tile.aspect);
        }
        static final int[] COLORS={0xF0F0F0,15435844,12801229,6719955,14602026,4312372,14188952,0x434343,0xA0A0A0,2651799,8073150,2437522,5320730,3887386,11743532,0x1E1B1B};
        void draw(int facing, boolean wall, float sway, PoseStack pose, MultiBufferSource buffers, int light, int overlay,int color,thaumcraft.api.aspects.Aspect aspect) {
            pose.pushPose();
            pose.translate(.5f, 1.5f, .5f);
            pose.mulPose(Axis.XP.rotationDegrees(180));
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.mulPose(Axis.YP.rotationDegrees(facing * 360 / 16f));
            var out = buffers.getBuffer(RenderType.entityCutoutNoCull(color<0&&aspect==null?TEXTURE:tex("models/banner_blank")));
            if (!wall) pole.render(pose, out, light, overlay);
            else pose.translate(0, 0, -.4125f);
            beam.render(pose, out, light, overlay);
            int tint=color>=0&&color<16?0xff000000|COLORS[color]:-1;
            b1.render(pose,out,light,overlay,tint);
            b2.render(pose,out,light,overlay,tint);
            banner.xRot = (.005f + .005f * Mth.cos(sway * Mth.PI * .02f)) * Mth.PI;
            banner.render(pose,out,light,overlay,tint);
            if(aspect!=null){pose.pushPose();pose.translate(0,0,.05001);pose.scale(.0375f,.0375f,.0375f);pose.mulPose(Axis.YP.rotationDegrees(180));pose.mulPose(Axis.XP.rotation(-banner.xRot*2));var tag=buffers.getBuffer(RenderType.entityTranslucent(aspect.getImage()));int tagColor=0xbf000000|aspect.getColor();float[][] points={{-8,4,0,0},{-8,20,0,1},{8,20,1,1},{8,4,1,0}};for(var p:points)tag.addVertex(pose.last().pose(),p[0],p[1],0).setColor(tagColor).setUv(p[2],p[3]).setOverlay(overlay).setLight(light).setNormal(pose.last(),0,0,-1);pose.popPose();}
            pose.popPose();
        }
        @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(CrimsonBanner.Entity tile) { return tile.getRenderBoundingBox(); }
    }

    abstract static class Types extends RenderType {
        private Types() { super("tc4_crimson", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {}); }
        /** RenderEndPortal technique with TC4's tunnel (layer 0) and particlefield (other layers), visible from both sides. */
        static final RenderType PORTAL = create("tc4_obelisk_field", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 1536, false, false,
            CompositeState.builder().setShaderState(RENDERTYPE_END_PORTAL_SHADER)
                .setTextureState(MultiTextureStateShard.builder().add(tex("misc/tunnel"), false, false).add(tex("misc/particlefield"), false, false).build())
                .setCullState(NO_CULL).createCompositeState(false));
        private static final java.util.Map<ResourceLocation, RenderType> LINES = new java.util.HashMap<>();
        static RenderType line(ResourceLocation texture) {
            return LINES.computeIfAbsent(texture, t -> create("tc4_floaty_line", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1024, false, true,
                CompositeState.builder().setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader)).setTextureState(new TextureStateShard(t, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
        }
    }
}
