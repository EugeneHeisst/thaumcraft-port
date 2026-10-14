package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class WardedArcanaClient {
 static int rendered;
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(WardedArcana.ENTITY.get(),Renderer::new);}
 @SubscribeEvent static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){e.registerBlock(new net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions(){@Override public boolean addHitEffects(net.minecraft.world.level.block.state.BlockState s,net.minecraft.world.level.Level l,net.minecraft.world.phys.HitResult target,net.minecraft.client.particle.ParticleEngine manager){if(target instanceof net.minecraft.world.phys.BlockHitResult hit&&l instanceof net.minecraft.client.multiplayer.ClientLevel client)manager.add(new WardingClient.WardParticle(client,hit));return true;}},WardedArcana.GLASS.get());}
 static final class Renderer implements BlockEntityRenderer<WardedArcana.Owned>{Renderer(BlockEntityRendererProvider.Context c){}public void render(WardedArcana.Owned t,float partial,PoseStack p,MultiBufferSource b,int light,int overlay){if(!t.getBlockState().is(WardedArcana.GLASS.get()))return;rendered++;var l=t.getLevel();var pos=t.getBlockPos();WardingClient.glass(p,b,pos,at->l.getBlockState(at).is(WardedArcana.GLASS.get()),false,0xffffffff,at->l.getBlockState(at).is(WardedArcana.GLASS.get()),NodeRenderTypes.translucent(TextureAtlas.LOCATION_BLOCKS));}}
}
