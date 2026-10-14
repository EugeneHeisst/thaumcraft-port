package dev.eugheist.tc4modern;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
/** Original server 2000-tick warp-event gate and WarpEvents.spawnMist. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class WarpRuntime {
 static int checks,events;
 @SubscribeEvent static void tick(PlayerTickEvent.Post event){if(event.getEntity() instanceof ServerPlayer p&&WarpEventRoll.due(p.tickCount,WarpConfig.wussMode(),p.hasEffect(WarpEffects.WARP_WARD)))check(p);}
 static WarpEventRoll.Result check(ServerPlayer p){checks++;var state=PlayerWarp.load(p);int perm=state.get(WarpState.Kind.PERMANENT),sticky=state.get(WarpState.Kind.STICKY);var slots=BaublesInventory.of(p);var gear=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();for(int i=0;i<slots.getContainerSize();i++)gear.add(slots.getItem(i));var roll=WarpEventRoll.check(p,WarpGear.total(p,gear),p.level().random::nextInt);if(roll.triggered()){events++;PacketDistributor.sendToPlayer(p,new WarpMiscPayload(0));if(!WarpEventApplication.apply(p,WarpEventPlan.select(roll.effect(),roll.strength())))throw new IllegalStateException("Unhandled original warp event "+roll.effect());WarpEventApplication.unlockResearch(p,perm,sticky);}PacketDistributor.sendToPlayer(p,new WarpSyncPayload(PlayerWarp.load(p).get(WarpState.Kind.TEMPORARY),WarpState.Kind.TEMPORARY.ordinal()));return roll;}
 static int mist(ServerPlayer p,int count){PacketDistributor.sendToPlayer(p,new WarpMiscPayload(1));var l=p.serverLevel();int spawned=0;for(int i=0;i<Math.min(8,count);i++){var g=new EldritchGuardian(WarpEntities.ELDRITCH_GUARDIAN.get(),l);for(int a=0;a<50;a++){int x=Mth.floor(p.getX())+Mth.nextInt(l.random,7,24)*Mth.nextInt(l.random,-1,1),y=Mth.floor(p.getY())+Mth.nextInt(l.random,7,24)*Mth.nextInt(l.random,-1,1),z=Mth.floor(p.getZ())+Mth.nextInt(l.random,7,24)*Mth.nextInt(l.random,-1,1);var floor=new BlockPos(x,y-1,z);if(!WarpSpiderSpawns.supportsSpider(l.getBlockState(floor),l,floor))continue;g.setPos(x,y,z);if(!l.isUnobstructed(g)||!l.noCollision(g)||l.containsAnyLiquid(g.getBoundingBox()))continue;g.setTarget(p);if(l.addFreshEntity(g))spawned++;break;}}return spawned;}
}
