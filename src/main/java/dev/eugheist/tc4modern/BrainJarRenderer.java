package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Original ModelBrain and ModelJar brine, using TileJarRenderer transforms. */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BrainJarRenderer implements BlockEntityRenderer<BrainJarEntity> {
    private final ModelPart brain;
    private final ModelPart brine;
    private final ModelPart shell;
    public BrainJarRenderer(BlockEntityRendererProvider.Context context) {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("Shape1", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, 12, 10, 16), PartPose.offset(-6, 8, -8));
        root.addOrReplaceChild("Shape2", CubeListBuilder.create().texOffs(64, 0).addBox(0, 0, 0, 8, 3, 7), PartPose.offset(-4, 18, 0));
        root.addOrReplaceChild("Shape3", CubeListBuilder.create().texOffs(0, 32).addBox(0, 0, 0, 2, 6, 2), PartPose.offsetAndRotation(-1, 18, -2, .4089647f, 0, 0));
        brain = LayerDefinition.create(mesh, 128, 64).bakeRoot();
        var liquid = new MeshDefinition();
        liquid.getRoot().addOrReplaceChild("Brine", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -11, -4, 8, 10, 8), PartPose.ZERO);
        brine = LayerDefinition.create(liquid, 64, 32).bakeRoot();
        var glass = new MeshDefinition();
        glass.getRoot().addOrReplaceChild("Core", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -12, -5, 10, 12, 10), PartPose.ZERO);
        glass.getRoot().addOrReplaceChild("Lid", CubeListBuilder.create().texOffs(0, 24).addBox(-3, 0, -3, 6, 2, 6), PartPose.offset(0, -14, 0));
        shell = LayerDefinition.create(glass, 64, 32).bakeRoot();
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BrainJarContent.ENTITY.get(), BrainJarRenderer::new);
    }
    @SubscribeEvent public static void items(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private final net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer = new net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()) {
                private BrainJarRenderer model;
                @Override public void renderByItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext context,
                        PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                    if (model == null) model = new BrainJarRenderer(null);
                    model.renderContents(null, 0, pose, buffers, light, overlay, true);
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(EssentiaDevices.JAR.get().defaultBlockState(), pose, buffers, light, overlay, net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);
                }
            };
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        }, BrainJarContent.ITEM.get());
    }
    @Override public void render(BrainJarEntity jar, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderContents(jar, partial, pose, buffers, light, overlay, false);
    }
    private void renderContents(BrainJarEntity jar, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay, boolean item) {
        var player = Minecraft.getInstance().player;
        float bob = player == null ? .03f : (float)Math.sin(player.tickCount / 14f) * .03f + .03f;
        pose.pushPose();
        if (item) {
            pose.translate(.5, .5, .5);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.translate(-.5, -.5, -.5);
        }
        pose.translate(.5, .01, .5);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.pushPose();
        pose.translate(0, -.8f + bob, 0);
        float rotation = jar == null ? 0 : jar.previousRotation + BrainJarEntity.wrapRotation(jar.rotation - jar.previousRotation) * partial;
        pose.mulPose(Axis.YP.rotationDegrees(rotation * 180f / 3.141593f - 90));
        pose.scale(.4f, .4f, .4f);
        brain.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(AlchemyContent.id("textures/models/brain2.png"))), light, overlay);
        pose.popPose();
        brine.render(pose, buffers.getBuffer(RenderType.entityTranslucent(AlchemyContent.id("textures/models/jarbrine.png"))), light, overlay);
        shell.render(pose, buffers.getBuffer(RenderType.entityTranslucent(AlchemyContent.id("textures/models/jar.png"))), light, overlay);
        pose.popPose();
    }
}
