package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
/** Client bindings for the alchemy, silverwood and essentia slice. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class AlchemyClient {
    private AlchemyClient() {}
    /** BlockMagicalLeaves.colorMultiplier/getRenderColor: silverwood is a constant 0x8899AA. */
    @SubscribeEvent static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> tint == 0 ? 0x8899AA : 0xFFFFFF, SilverwoodContent.LEAVES.get());
    }
    /** Leaves tint, and ItemEssence/ItemResource label overlays tinted with their first aspect colour. */
    @SubscribeEvent static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0 ? 0xFF8899AA : 0xFFFFFFFF, SilverwoodContent.LEAVES.get());
        event.register((stack, tint) -> {
            if (tint == 0) return 0xFFFFFFFF;
            var aspects = EssentiaItems.getAspects(stack);
            return aspects == null ? 0xFFFFFFFF : 0xFF000000 | aspects.getAspects()[0].getColor();
        }, EssentiaDevices.ESSENTIA_PHIAL.get(), EssentiaDevices.LABEL.get());
    }
    @SubscribeEvent static void screens(RegisterMenuScreensEvent event) { event.register(EssentiaDevices.FURNACE_MENU.get(), AlchemicalFurnaceScreen::new); }
    @SubscribeEvent static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AlchemyMachines.ENTITY.get(), AlchemyMachineRenderer::new);
        event.registerBlockEntityRenderer(EssentiaDevices.ALEMBIC_ENTITY.get(), EssentiaRenderers.AlembicRenderer::new);
        event.registerBlockEntityRenderer(EssentiaDevices.JAR_ENTITY.get(), EssentiaRenderers.JarRenderer::new);
        event.registerBlockEntityRenderer(EssentiaTubes.TUBE_ENTITY.get(), EssentiaRenderers.TubeRenderer::new);
        event.registerBlockEntityRenderer(EssentiaTubes.VARIANT_ENTITY.get(), EssentiaRenderers.TubeRenderer::new);
        event.registerBlockEntityRenderer(EssentiaTubes.BUFFER_ENTITY.get(), EssentiaRenderers.BufferRenderer::new);
        event.registerBlockEntityRenderer(InfusionContent.MATRIX_ENTITY.get(), InfusionRenderers.MatrixRenderer::new);
        event.registerBlockEntityRenderer(InfusionContent.PILLAR_ENTITY.get(), InfusionRenderers.PillarRenderer::new);
        event.registerBlockEntityRenderer(InfusionContent.PEDESTAL_ENTITY.get(), InfusionRenderers.PedestalRenderer::new);
    }
    @SubscribeEvent static void items(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions(){
            private final AlchemyMachineRenderer machineRenderer=new AlchemyMachineRenderer(null);
            private final BlockEntityWithoutLevelRenderer renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
                @Override public void renderByItem(ItemStack stack,ItemDisplayContext ctx,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                    var block=((net.minecraft.world.item.BlockItem)stack.getItem()).getBlock();
                    var t=new AlchemyMachineEntity(net.minecraft.core.BlockPos.ZERO,block.defaultBlockState());
                    machineRenderer.render(t,0,pose,buffers,light,overlay);
                }
            };
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}
        },AlchemyMachines.CENTRIFUGE.get().asItem(),AlchemyMachines.CRYSTALIZER.get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()) {
                @Override public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                    // BlockMetalDeviceRenderer metadata 1 draws the alembic from the icon centre upwards.
                    pose.pushPose();
                    pose.translate(0, .5f, 0);
                    EssentiaRenderers.renderAlembic(null, pose, buffers, light, overlay);
                    pose.popPose();
                }
            };
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        }, EssentiaDevices.ALEMBIC.get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()) {
                private InfusionRenderers.MatrixRenderer matrix;
                /** BlockStoneDeviceRenderer.renderInventoryBlock metadata 2: the inactive matrix. */
                @Override public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                    if (matrix == null) matrix = new InfusionRenderers.MatrixRenderer(null);
                    matrix.render(new InfusionContent.MatrixEntity(net.minecraft.core.BlockPos.ZERO, InfusionContent.MATRIX.get().defaultBlockState()), 0, pose, buffers, light, overlay);
                }
            };
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        }, InfusionContent.MATRIX.get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()) {
                /** ItemJarFilledRenderer: the jar block plus its liquid and label. */
                @Override public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(((net.minecraft.world.item.BlockItem)stack.getItem()).getBlock().defaultBlockState(), pose, buffers, light, overlay, net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);
                    var data = stack.get(DataComponents.CUSTOM_DATA);
                    if (data == null) return;
                    var tag = data.copyTag();
                    var aspects = new AspectList(); aspects.readFromNBT(tag);
                    Aspect aspect = aspects.size() > 0 ? aspects.getAspects()[0] : null;
                    Aspect filter = tag.contains("AspectFilter") ? Aspect.getAspect(tag.getString("AspectFilter")) : null;
                    EssentiaRenderers.renderJarContents(aspect, aspect == null ? 0 : aspects.getAmount(aspect), 64, filter, 2, 0, pose, buffers, light);
                }
            };
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        }, EssentiaDevices.JAR.get().asItem(), AlchemyMachines.VOID_JAR.get().asItem());
    }
}
