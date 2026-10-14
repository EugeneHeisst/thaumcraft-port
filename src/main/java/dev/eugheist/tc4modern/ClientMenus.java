package dev.eugheist.tc4modern;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientMenus {
    @SubscribeEvent public static void reloadObj(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event){
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->{ObjParts.invalidate();ResearchBookText.invalidate();});
    }
    @SubscribeEvent public static void armorExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event){/* ItemVoidRobeArmor.getArmorModel uses the same ModelRobe as the cultist robe */var robeModel=new CultistArmorExtension(false);event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){@Override public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(net.minecraft.world.entity.LivingEntity e,net.minecraft.world.item.ItemStack s,net.minecraft.world.entity.EquipmentSlot slot,net.minecraft.client.model.HumanoidModel<?> original){return robeModel.getHumanoidArmorModel(e,s,slot,original);}
            /* the dyed armour layer takes its tint from here (the stack is not in #minecraft:dyeable) */@Override public int getDefaultDyeColor(net.minecraft.world.item.ItemStack s){return 0xFF000000|thaumcraft.common.items.armor.ItemRobeArmor.color(s);}},VoidRobe.HELM.get(),VoidRobe.CHEST.get(),VoidRobe.LEGS.get());for(boolean plate:new boolean[]{true,false}){var extension=new CultistArmorExtension(plate);for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM){var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);if(id.getNamespace().equals(TC4Modern.MODID)&&id.getPath().startsWith(plate?"cultist_plate_":"cultist_robe_"))event.registerItem(extension,item);}}event.registerItem(new RobeClientExtension(),TC4Modern.ROBE_CHEST.get(),TC4Modern.ROBE_LEGS.get(),TC4Modern.ROBE_BOOTS.get());}
    @SubscribeEvent public static void bellowsExtension(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event){event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
        private final BellowsItemRenderer renderer=new BellowsItemRenderer();
        @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}
    },TC4Modern.BELLOWS_ITEM.get());}
    @SubscribeEvent public static void nodeDeviceItems(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event){event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
        private final NodeDeviceItemRenderer renderer=new NodeDeviceItemRenderer();
        @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}
    },TC4Modern.NODE_STABILIZER_ITEM.get(),TC4Modern.NODE_STABILIZER_ADV_ITEM.get(),TC4Modern.NODE_CONVERTER_ITEM.get());}
    @SubscribeEvent public static void relayItem(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event){event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
        private final VisRelayItemRenderer renderer=new VisRelayItemRenderer();
        @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}
    },TC4Modern.VIS_RELAY_ITEM.get());}
    @SubscribeEvent public static void shaders(net.neoforged.neoforge.client.event.RegisterShadersEvent event)throws java.io.IOException{
        event.registerShader(new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"node"),com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR),shader->NodeRenderTypes.shader=shader);
        event.registerShader(new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"vent"),com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE),shader->EssentiaVentParticle.shader=shader);
    }
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(PechContent.PECH.get(),PechRenderer::new);event.registerEntityRenderer(PechContent.BLAST.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);event.registerBlockEntityRenderer(BasicsContent.MANA_POD_ENTITY.get(),ManaPodRenderer::new);event.registerBlockEntityRenderer(BasicsContent.NODE_JAR_ENTITY.get(),NodeJarRenderer::new);event.registerBlockEntityRenderer(TC4Modern.VIS_RELAY_ENTITY.get(),VisRelayRenderer::new);event.registerBlockEntityRenderer(TC4Modern.ARCANE_ENTITY.get(),ArcaneWorkbenchRenderer::new);event.registerBlockEntityRenderer(TC4Modern.NODE_STABILIZER_ENTITY.get(),NodeStabilizerRenderer::new);event.registerBlockEntityRenderer(TC4Modern.NODE_CONVERTER_ENTITY.get(),NodeConverterRenderer::new);event.registerBlockEntityRenderer(TC4Modern.ENERGIZED_NODE_ENTITY.get(),EnergizedNodeRenderer::new);event.registerBlockEntityRenderer(TC4Modern.AURA_NODE_ENTITY.get(),AuraNodeRenderer::new);event.registerBlockEntityRenderer(TC4Modern.CRUCIBLE_ENTITY.get(),CrucibleRenderer::new);event.registerBlockEntityRenderer(TC4Modern.BELLOWS_ENTITY.get(),BellowsRenderer::new);event.registerBlockEntityRenderer(TC4Modern.NITOR_ENTITY.get(),NitorRenderer::new);event.registerEntityRenderer(TC4Modern.THAUMIC_SLIME.get(),ThaumicSlimeRenderer::new);event.registerEntityRenderer(TC4Modern.TAINT_SPIDER.get(),TaintSpiderRenderer::new);event.registerEntityRenderer(TC4Modern.TAINT_SPORE.get(),TaintSporeRenderer::new);event.registerEntityRenderer(TC4Modern.TAINT_SWARMER.get(),TaintSporeRenderer::new);event.registerEntityRenderer(TC4Modern.TAINT_SWARM.get(),TaintSwarmRenderer::new);event.registerEntityRenderer(TC4Modern.FALLING_TAINT.get(),TaintFallingRenderer::new);event.registerEntityRenderer(TC4Modern.ALUMENTUM_PROJECTILE.get(),AlumentumRenderer::new);event.registerEntityRenderer(TC4Modern.SPECIAL_ITEM.get(),net.minecraft.client.renderer.entity.ItemEntityRenderer::new);}
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) {
        event.register(PechRewards.POUCH_MENU.get(),RewardScreens.Pouch::new);
        event.register(PechContent.MENU.get(),PechScreen::new);
        event.register(BasicsContent.DECONSTRUCTOR_MENU.get(),DeconstructorScreen::new);
        event.register(TC4Modern.ARCANE_MENU.get(),ArcaneWorkbenchScreen::new);
        event.register(TC4Modern.RESEARCH_MENU.get(), ResearchTableScreen::new);
        event.register(TC4Modern.BOOK_MENU.get(), ThaumonomiconScreen::new);
    }
    @SubscribeEvent public static void blockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event){
        for(var block:net.minecraft.core.registries.BuiltInRegistries.BLOCK)if(block instanceof ThaumicCandleBlock candle)event.register((state,world,pos,tint)->tint==0?candle.color:0xFFFFFF,block);
        event.register((state,world,pos,tint)->tint==0?GreatwoodColors.foliage(world,pos):0xFFFFFF,TC4Modern.GREATWOOD_LEAVES.get());
        for(var ore:TC4Modern.SHARD_ORES)event.register((state,world,pos,tint)->tint==0?thaumcraft.common.lib.vis.ShardPalette.color(((ShardOreBlock)state.getBlock()).variant):0xFFFFFF,ore.get());
        for(var ore:TC4Modern.DEEPSLATE_SHARD_ORES)event.register((state,world,pos,tint)->tint==0?thaumcraft.common.lib.vis.ShardPalette.color(((ShardOreBlock)state.getBlock()).variant):0xFFFFFF,ore.get());
        event.register((state,world,pos,tint)->tint==0&&state.getValue(TaintBlock.VARIANT)==1?TaintColors.grass(world,pos):0xFFFFFF,TC4Modern.TAINT.get());
        // BlockTaintFibres.colorMultiplier: fibres, taint grass and spore stalks take the 3x3 averaged biome grass colour (purple in Tainted Land).
        event.register((state,world,pos,tint)->tint==0?TaintColors.grass(world,pos):0xFFFFFF,TC4Modern.TAINT_FIBRES.get());
    }
    @SubscribeEvent public static void candleColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event){for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM)if(item instanceof net.minecraft.world.item.BlockItem block&&block.getBlock() instanceof ThaumicCandleBlock candle)event.register((stack,tint)->tint==0?0xFF000000|candle.color:0xFFFFFFFF,item);}
    @SubscribeEvent public static void manaColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event){event.register((stack,tint)->{var a=ManaBeanItem.aspect(stack);if(a==null){var aa=thaumcraft.api.aspects.Aspect.aspects.values().toArray(thaumcraft.api.aspects.Aspect[]::new);a=aa[(int)(System.currentTimeMillis()/500%aa.length)];}return 0xFF000000|a.getColor();},BasicsContent.MANA_BEAN.get());event.register((stack,tint)->WispEssenceItem.color(stack),BasicsContent.WISP_ESSENCE.get());}
    @SubscribeEvent public static void colors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack,tint)->tint==0 ? 0xFF000000|thaumcraft.common.items.armor.ItemRobeArmor.color(stack) : 0xFFFFFFFF,TC4Modern.ROBE_CHEST.get(),TC4Modern.ROBE_LEGS.get(),TC4Modern.ROBE_BOOTS.get());event.register((stack,tint)->tint==0 ? 0xFF000000|thaumcraft.common.items.armor.ItemRobeArmor.color(stack) : 0xFFFFFFFF,VoidRobe.HELM.get(),VoidRobe.CHEST.get(),VoidRobe.LEGS.get());
        for(int i=0;i<TC4Modern.SHARDS.size();i++){
            int variant=i;event.register((stack,tint)->0xFF000000|thaumcraft.common.lib.vis.ShardPalette.color(variant),TC4Modern.SHARDS.get(i).get());
            event.register((stack,tint)->tint==0?0xFF000000|thaumcraft.common.lib.vis.ShardPalette.color(variant):0xFFFFFFFF,TC4Modern.SHARD_ORE_ITEMS.get(i).get());
        }
        event.register((stack,tint) -> {
            var note=NoteData.read(stack);return tint==1 && note!=null ? 0xFF000000|note.color : -1;
        },TC4Modern.RESEARCH_NOTES.get());
    }
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(()->{
            thaumcraft.common.entities.projectile.EntityAlumentum.clientTrail=AlumentumSmoke::trail;
            thaumcraft.common.entities.monster.EntityTaintSpore.swarmParticles=SporeSwarmParticle::update;
            thaumcraft.common.entities.monster.EntityTaintSpore.sploosh=SporeSplooshParticle::burst;
            thaumcraft.common.entities.monster.EntityTaintSwarm.particles=SporeSwarmParticle::updateSwarm;
            thaumcraft.common.entities.monster.EntityTaintSporeSwarmer.particles=SporeSwarmParticle::updateSwarmer;
            thaumcraft.common.entities.monster.EntityTaintSporeSwarmer.clearParticles=SporeSwarmParticle::clear;
            thaumcraft.common.entities.monster.EntityTaintSporeSwarmer.fragments=SporeSplooshParticle::burst;
            thaumcraft.common.entities.EntityFallingTaint.clientParticles=SporeSplooshParticle::land;
            VisRelayEntity.clientBeamTick=VisRelayBeamParticle::update;
            EssentiaTubeEntity.clientVent=EssentiaVentParticle::spawn;
            NitorEntity.particleSetting=()->net.minecraft.client.Minecraft.getInstance().options.particles().get().getId();
            NitorEntity.visible=pos->{var mc=net.minecraft.client.Minecraft.getInstance();return mc.player!=null && mc.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<(mc.options.graphicsMode().get()!=net.minecraft.client.GraphicsStatus.FAST?2500:625);};
        });
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(TC4Modern.RESEARCH_NOTES.get(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"complete"),(stack,level,entity,seed) -> {
            var note=NoteData.read(stack);return note!=null && note.complete ? 1:0;
        }));
    }
}
