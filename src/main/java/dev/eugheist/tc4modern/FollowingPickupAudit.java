package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Visible regression for the 0.17.77 crash: picking up an elemental-axe log plays the vanilla pickup particle on a following_item copy. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class FollowingPickupAudit {
 static final BlockPos BASE=new BlockPos(440,121,-90);static int ticks;static volatile int followed;
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_PICKUP FAIL "+s);System.out.println("TC4_PICKUP PASS "+s);}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.followingPickupAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==40)server.execute(()->{var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();l.setDayTime(6000);
   l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(16)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
   for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=-1;y<8;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
   for(int y=0;y<4;y++)l.setBlock(BASE.offset(0,y,3),Blocks.SPRUCE_LOG.defaultBlockState(),3);
   p.connection.teleport(BASE.getX()+.5,BASE.getY(),BASE.getZ()+.5,0,20);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuiltInRegistries.ITEM.get(AlchemyContent.id("elemental_axe"))));});
  if(ticks>=60&&ticks<=120&&ticks%10==0)server.execute(()->{var p=server.getPlayerList().getPlayer(id);for(int y=0;y<4;y++){var at=BASE.offset(0,y,3);if(!p.level().isEmptyBlock(at)){p.gameMode.destroyBlock(at);break;}}followed+=p.level().getEntitiesOfClass(ElementalTools.FollowingItem.class,new AABB(BASE).inflate(8)).size();});
  if(ticks==130)LiquidDeathAudit.shot(mc,"following-pickup.png");
  if(ticks==200){int logs=mc.player.getInventory().countItem(Items.SPRUCE_LOG);check(followed>0,"elemental axe spawned following_item drops seen="+followed);check(logs>=4,"all following logs picked up with the vanilla pickup animation, no crash logs="+logs);System.out.println("TC4_PICKUP ALL PASS");mc.stop();}
 }
}
