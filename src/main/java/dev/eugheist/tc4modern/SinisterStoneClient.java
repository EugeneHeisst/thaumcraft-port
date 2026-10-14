package dev.eugheist.tc4modern;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class SinisterStoneClient {
 record Node(ResourceKey<Level> dimension,BlockPos pos){}
 static final Map<Node,Long> NODES=new HashMap<>();static boolean active;
 @SubscribeEvent static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{
  SinisterStone.nodeSeen=(l,p)->NODES.put(new Node(l.dimension(),p.immutable()),System.currentTimeMillis());SinisterStone.update=SinisterStoneClient::update;
  net.minecraft.client.renderer.item.ItemProperties.register(SinisterStone.ITEM.get(),AlchemyContent.id("active"),(s,l,entity,seed)->active?1:0);
 });}
 static boolean withinCone(Entity viewer,BlockPos pos){var relative=pos.getCenter().subtract(viewer.getEyePosition());double dot=relative.dot(viewer.getLookAngle());return dot/relative.length()>Math.cos(.66f/2)&&dot<256;}
 static void update(Entity e){var expired=new ArrayList<Node>();active=false;long now=System.currentTimeMillis();for(var entry:NODES.entrySet()){var n=entry.getKey();if(entry.getValue()<now-10000)expired.add(n);if(n.dimension().equals(e.level().dimension())&&withinCone(e,n.pos())){active=true;break;}}for(var n:expired)NODES.remove(n);}
}
