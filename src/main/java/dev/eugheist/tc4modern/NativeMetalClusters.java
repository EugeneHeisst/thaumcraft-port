package dev.eugheist.tc4modern;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
/** Original ItemNugget metadata18/19/20, activated by ore-bearing addon integrations. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class NativeMetalClusters {
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ITEM,h->{for(String metal:java.util.List.of("tin","silver","lead"))h.register(AlchemyContent.id(metal+"_cluster"),new Item(new Item.Properties()));});}
 @SubscribeEvent static void setup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e){e.enqueueWork(()->{for(String metal:java.util.List.of("tin","silver","lead")){var aspects=new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.ORDER,1).add(thaumcraft.api.aspects.Aspect.METAL,5).add(thaumcraft.api.aspects.Aspect.EARTH,1);aspects.add(metal.equals("tin")?thaumcraft.api.aspects.Aspect.CRYSTAL:metal.equals("silver")?thaumcraft.api.aspects.Aspect.GREED:thaumcraft.api.aspects.Aspect.ORDER,2);dev.eugheist.tc4modern.api.TC4Addons.registerObjectTag(AlchemyContent.id(metal+"_cluster"),aspects);}});}
}

