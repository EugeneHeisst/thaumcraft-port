package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Visible demo: a bell bound to a working golem, its home and dyed markers, with a slow camera orbit (~30 s). */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class GolemBellShowcase {
 static final BlockPos BASE=new BlockPos(420,121,-90);static int ticks;static volatile boolean ready;static long homes,marks,links;
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_BELL_SHOW FAIL "+s);System.out.println("TC4_BELL_SHOW PASS "+s);}
 static void setup(ServerPlayer p){
  var l=p.serverLevel();p.removeAllEffects();p.setHealth(p.getMaxHealth());p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();l.setDayTime(13000);l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,l.getServer());
  l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(24)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
  for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)for(int y=-1;y<6;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?(Math.floorMod(x+z,2)==0?Blocks.POLISHED_ANDESITE:Blocks.STONE_BRICKS):Blocks.AIR).defaultBlockState(),3);
  l.setBlock(BASE,Blocks.CHEST.defaultBlockState(),3);
  BlockPos[] targets={BASE.offset(5,0,3),BASE.offset(-5,0,4),BASE.offset(4,0,-5),BASE.offset(-4,1,-4)};Direction[] sides={Direction.UP,Direction.EAST,Direction.NORTH,Direction.SOUTH};int[] dyes={-1,14,11,10};
  for(var t:targets){l.setBlock(t,Blocks.BARREL.defaultBlockState(),3);if(t.getY()>BASE.getY())l.setBlock(t.below(),Blocks.STONE_BRICKS.defaultBlockState(),3);}
  var g=GolemHarvestAudit.golem(p,GolemMaterial.THAUMIUM,BASE.above(),2);g.moveTo(BASE.getX()+1.5,BASE.getY(),BASE.getZ()+.5,0,0);
  for(int i=0;i<targets.length;i++){var m=new CompoundTag();m.putLong("pos",targets[i].asLong());m.putByte("side",(byte)sides[i].get3DDataValue());m.putByte("color",(byte)dyes[i]);g.markers.add(m);}
  var bell=new ItemStack(Golemancy.BELL.get());p.setItemInHand(InteractionHand.MAIN_HAND,bell);((Golemancy.Bell)bell.getItem()).interactLivingEntity(bell,p,g,InteractionHand.MAIN_HAND);p.containerMenu.broadcastChanges();
  for(int i=0;i<6;i++){var item=new ItemEntity(l,BASE.getX()-6.5+i*2.4,BASE.getY(),BASE.getZ()+7.5-(i%2)*14,new ItemStack(i%2==0?Items.COBBLESTONE:Items.OAK_LOG,3));item.setNoPickUpDelay();l.addFreshEntity(item);}
  ready=true;
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.golemBellShowOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==40)server.execute(()->setup(server.getPlayerList().getPlayer(id)));
  if(ticks>=60&&ticks<=660&&ticks%2==0){double a=Math.toRadians((ticks-60)*.6),r=8.5,x=BASE.getX()+.5+Math.sin(a)*r,z=BASE.getZ()+.5-Math.cos(a)*r;float yaw=(float)Math.toDegrees(Math.atan2(-(BASE.getX()+.5-x),BASE.getZ()+.5-z));server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.connection.teleport(x,BASE.getY()+4,z,yaw,28);});}
  if(ticks==100){check(ready&&GolemBellClient.selected(mc.player.getMainHandItem(),mc)!=null,"client bell holds the bound golem");homes=GolemBellClient.homes;marks=GolemBellClient.marks;links=GolemBellClient.links;}
  if(ticks==200||ticks==350||ticks==500||ticks==640)LiquidDeathAudit.shot(mc,"golem-bell-show-"+ticks+".png");
  if(ticks==660){check(GolemBellClient.homes>homes&&GolemBellClient.marks>marks&&GolemBellClient.links>links,"home marker, 4 dyed markers and flowing script rendered for 30 s homes="+(GolemBellClient.homes-homes)+" marks="+(GolemBellClient.marks-marks)+" links="+(GolemBellClient.links-links));System.out.println("TC4_BELL_SHOW ALL PASS");mc.stop();}
 }
}
