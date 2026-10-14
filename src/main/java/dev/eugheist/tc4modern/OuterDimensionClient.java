package dev.eugheist.tc4modern;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class OuterDimensionClient {
    @SubscribeEvent static void effects(net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event){event.register(AlchemyContent.id("outer_lands"),new DimensionSpecialEffects(1,false,DimensionSpecialEffects.SkyType.NONE,false,true){
        @Override public Vec3 getBrightnessDependentFogColor(Vec3 color,float sun){return new Vec3(160/255.0*.15,128/255.0*.15,160/255.0*.15);}
        @Override public boolean isFoggyAt(int x,int z){return true;}
    });}
}
