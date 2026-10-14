package dev.eugheist.tc4modern;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class ArtificeDeviceClient {
 @SubscribeEvent static void blockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block e){e.register((s,l,p,t)->t==0?0xffffcc:0xdd11ff,Levitator.BLOCK.get());}
 @SubscribeEvent static void itemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e){e.register((s,t)->t==0?0xffffcc:0xdd11ff,Levitator.BLOCK.get());}
 @SubscribeEvent static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->{
  Levitator.shift=()->net.minecraft.client.Minecraft.getInstance().options.keyShift.isDown();
  Levitator.sparkle=(l,p)->{var r=l.random;FociFx.sparkle(p.getX()+.2+r.nextFloat()*.6,p.getY()+1,p.getZ()+.2+r.nextFloat()*.6,1,3,-.3f);};
  ArtificeDevices.travelSparkles=(l,p)->FociFx.blockSparkle(p,32768,5);
  ArtificeDevices.wardRunes=(l,p)->{for(var e:l.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(p).inflate(1))){if(e instanceof net.minecraft.world.entity.player.Player)continue;var r=l.random;ScanRunes.blockRunes((net.minecraft.client.multiplayer.ClientLevel)l,p.getX(),p.getY()+.6+r.nextFloat()*Math.max(.8,e.getEyeHeight()),p.getZ(),.6f+r.nextFloat()*.4f,0,.3f+r.nextFloat()*.7f,20,0);break;}};
 });}
}
