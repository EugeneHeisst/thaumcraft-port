package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Visible audit: sorting golem core moves home-chest items into marked chests that already hold them. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class GolemSortingAudit {
 static final BlockPos BASE=new BlockPos(500,121,-90),STONE=BASE.offset(5,0,4),DIRT=BASE.offset(-5,0,4),FAR=BASE.offset(0,0,-30),PLAIN=BASE.offset(0,0,7);static int ticks;static StrawGolem golem;
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_SORT FAIL "+s);System.out.println("TC4_SORT PASS "+s);}
 static ChestBlockEntity chest(ServerPlayer p,BlockPos at){return (ChestBlockEntity)p.serverLevel().getBlockEntity(at);}
 static int count(ServerPlayer p,BlockPos at,Item item){var c=chest(p,at);int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
 static void mark(StrawGolem g,BlockPos pos){var m=new CompoundTag();m.putLong("pos",pos.asLong());m.putByte("side",(byte)Direction.UP.get3DDataValue());m.putByte("color",(byte)-1);g.markers.add(m);}
 static void setup(ServerPlayer p){
  var l=p.serverLevel();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();l.setDayTime(6000);
  l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(40)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
  for(int x=-10;x<=10;x++)for(int z=-33;z<=10;z++)for(int y=-1;y<5;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),3);
  for(var at:java.util.List.of(BASE,STONE,DIRT,FAR,PLAIN))l.setBlock(at,Blocks.CHEST.defaultBlockState(),3);
  var home=chest(p,BASE);home.setItem(0,new ItemStack(Items.COBBLESTONE,40));home.setItem(1,new ItemStack(Items.DIRT,20));home.setItem(2,new ItemStack(Items.DIAMOND,3));home.setItem(3,new ItemStack(Items.OAK_LOG,5));
  chest(p,STONE).setItem(0,new ItemStack(Items.COBBLESTONE,1));chest(p,DIRT).setItem(0,new ItemStack(Items.DIRT,1));chest(p,FAR).setItem(0,new ItemStack(Items.OAK_LOG,1));
  golem=GolemHarvestAudit.golem(p,GolemMaterial.IRON,BASE.above(),10);golem.moveTo(BASE.getX()+1.5,BASE.getY(),BASE.getZ()+.5);for(var at:java.util.List.of(STONE,DIRT,FAR,PLAIN))mark(golem,at);
  var copy=new StrawGolem(Golemancy.ENTITY.get(),l);copy.restoreItem(golem.asItem());check(Golemancy.coreId(new ItemStack(Golemancy.SORTING.get()))==10&&copy.core()==10,"sorting core is core 10 and survives the bell item");
  p.connection.teleport(BASE.getX()+.5,BASE.getY()+7,BASE.getZ()-5.5,0,50);
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.golemSortingAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==20)server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(BASE.getX()+.5,BASE.getY()+7,BASE.getZ()-5.5,0,50);});
  if(ticks==100)server.execute(()->setup(server.getPlayerList().getPlayer(id)));
  if(ticks==120)LiquidDeathAudit.shot(mc,"golem-sorting-working.png");
  if(ticks>60&&ticks%100==0)server.execute(()->{var p=server.getPlayerList().getPlayer(id);System.out.println("TC4_SORT progress t="+ticks+" stone="+count(p,STONE,Items.COBBLESTONE)+" dirt="+count(p,DIRT,Items.DIRT)+" carried="+golem.carried()+" taken="+GolemSortingGoal.taken+" placed="+GolemSortingGoal.placed);});
  if(ticks==500)server.execute(()->{var p=server.getPlayerList().getPlayer(id);
   check(count(p,STONE,Items.COBBLESTONE)==41&&count(p,BASE,Items.COBBLESTONE)==0,"all 40 cobblestone sorted into the chest that already held cobblestone");
   check(count(p,DIRT,Items.DIRT)==21&&count(p,BASE,Items.DIRT)==0,"all 20 dirt sorted into the dirt chest");
   check(count(p,BASE,Items.DIAMOND)==3,"diamonds with no matching chest stay home");
   check(count(p,BASE,Items.OAK_LOG)==5&&count(p,FAR,Items.OAK_LOG)==1,"log chest beyond range 16 is ignored");
   check(count(p,PLAIN,Items.COBBLESTONE)==0&&count(p,PLAIN,Items.DIRT)==0,"empty marked chest receives nothing");
   check(golem.carried().isEmpty(),"golem idle with empty hands");System.out.println("TC4_SORT ALL PASS");});
  if(ticks==800)mc.stop();
 }
}
