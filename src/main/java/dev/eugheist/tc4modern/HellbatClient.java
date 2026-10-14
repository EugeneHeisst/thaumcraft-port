package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** RenderFireBat and ModelFireBat (the 1.7 bat geometry on a 64x64 texture), always fully lit. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class HellbatClient {
    private HellbatClient() {}
    static final ModelLayerLocation LAYER = new ModelLayerLocation(AlchemyContent.id("firebat"), "main");
    @SubscribeEvent static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) { event.registerLayerDefinition(LAYER, Model::layer); }
    @SubscribeEvent static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(Hellbat.FIREBAT.get(), Renderer::new); }

    public static final class Model extends EntityModel<Hellbat.FireBat> {
        private final ModelPart head, body, rightWing, leftWing, outerRightWing, outerLeftWing;
        Model(ModelPart root) {
            head = root.getChild("head");
            body = root.getChild("body");
            rightWing = body.getChild("right_wing");
            leftWing = body.getChild("left_wing");
            outerRightWing = rightWing.getChild("outer_right_wing");
            outerLeftWing = leftWing.getChild("outer_left_wing");
        }
        static LayerDefinition layer() {
            var mesh = new MeshDefinition();
            var root = mesh.getRoot();
            var head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -3, 6, 6, 6), PartPose.ZERO);
            head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(24, 0).addBox(-4, -6, -2, 3, 4, 1), PartPose.ZERO);
            head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(1, -6, -2, 3, 4, 1), PartPose.ZERO);
            var body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-3, 4, -3, 6, 12, 6).texOffs(0, 34).addBox(-5, 16, 0, 10, 6, 1), PartPose.ZERO);
            var right = body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(42, 0).addBox(-12, 1, 1.5f, 10, 16, 1), PartPose.ZERO);
            right.addOrReplaceChild("outer_right_wing", CubeListBuilder.create().texOffs(24, 16).addBox(-8, 1, 0, 8, 12, 1), PartPose.offset(-12, 1, 1.5f));
            var left = body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(42, 0).mirror().addBox(2, 1, 1.5f, 10, 16, 1), PartPose.ZERO);
            left.addOrReplaceChild("outer_left_wing", CubeListBuilder.create().texOffs(24, 16).mirror().addBox(0, 1, 0, 8, 12, 1), PartPose.offset(12, 1, 1.5f));
            return LayerDefinition.create(mesh, 64, 64);
        }
        @Override public void setupAnim(Hellbat.FireBat bat, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
            if (bat.isHanging()) {
                head.xRot = headPitch / 57.295776f;
                head.yRot = (float) Math.PI - headYaw / 57.295776f;
                head.zRot = (float) Math.PI;
                head.setPos(0, -2, 0);
                rightWing.setPos(-3, 0, 3);
                leftWing.setPos(3, 0, 3);
                body.xRot = (float) Math.PI;
                rightWing.xRot = -.15707964f;
                rightWing.yRot = -1.2566371f;
                outerRightWing.yRot = -1.7278761f;
                leftWing.xRot = rightWing.xRot;
                leftWing.yRot = -rightWing.yRot;
                outerLeftWing.yRot = -outerRightWing.yRot;
            } else {
                head.xRot = headPitch / 57.295776f;
                head.yRot = headYaw / 57.295776f;
                head.zRot = 0;
                head.setPos(0, 0, 0);
                rightWing.setPos(0, 0, 0);
                leftWing.setPos(0, 0, 0);
                body.xRot = .7853982f + Mth.cos(age * .1f) * .15f;
                body.yRot = 0;
                rightWing.yRot = Mth.cos(age * 1.3f) * (float) Math.PI * .25f;
                leftWing.yRot = -rightWing.yRot;
                outerRightWing.yRot = rightWing.yRot * .5f;
                outerLeftWing.yRot = -rightWing.yRot * .5f;
            }
        }
        @Override public void renderToBuffer(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer out, int light, int overlay, int color) {
            head.render(pose, out, light, overlay, color);
            body.render(pose, out, light, overlay, color);
        }
    }

    public static final class Renderer extends MobRenderer<Hellbat.FireBat, Model> {
        private static final ResourceLocation FIRE = AlchemyContent.id("textures/models/firebat.png"), VAMPIRE = AlchemyContent.id("textures/models/vampirebat.png");
        Renderer(EntityRendererProvider.Context context) { super(context, new Model(context.bakeLayer(LAYER)), .25f); }
        @Override public ResourceLocation getTextureLocation(Hellbat.FireBat bat) { return bat.isVampire() ? VAMPIRE : FIRE; }
        @Override protected void scale(Hellbat.FireBat bat, PoseStack pose, float partial) {
            float s = bat.isDevil() || bat.isVampire() ? .6f : .35f;
            pose.scale(s, s, s);
        }
        @Override protected void setupRotations(Hellbat.FireBat bat, PoseStack pose, float age, float yaw, float partial, float scale) {
            if (!bat.isHanging()) pose.translate(0, Mth.cos(age * .3f) * .1f, 0);
            else pose.translate(0, -.1f, 0);
            super.setupRotations(bat, pose, age, yaw, partial, scale);
        }
        /** getBrightnessForRender 0xF000F0. */
        @Override protected int getBlockLightLevel(Hellbat.FireBat bat, BlockPos pos) { return 15; }
        @Override protected int getSkyLightLevel(Hellbat.FireBat bat, BlockPos pos) { return 15; }
    }
}
