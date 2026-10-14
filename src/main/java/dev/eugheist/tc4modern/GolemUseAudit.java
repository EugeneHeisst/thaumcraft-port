package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Visible audit: use golem core (AIUseItem) planting from a home chest, flipping a lever bare-handed and left-clicking a torch away. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class GolemUseAudit {
 static final BlockPos BASE=new BlockPos(540,121,-90),LEVER=BASE.offset(-6,0,6),TORCH=BASE.offset(6,0,6);static int ticks;static StrawGolem planter,flipper,breaker;static int leverOn,leverOff;
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_USE FAIL "+s);System.out.println("TC4_USE PASS "+s);}
 static void mark(StrawGolem g,BlockPos pos,Direction side){var m=new CompoundTag();m.putLong("pos",pos.asLong());m.putByte("side",(byte)side.get3DDataValue());m.putByte("color",(byte)-1);g.markers.add(m);}
 static int saplings(ServerPlayer p){int n=0;for(int i=-1;i<=1;i++)if(p.level().getBlockState(BASE.offset(i*2,0,4)).is(Blocks.OAK_SAPLING))n++;return n;}
 static void setup(ServerPlayer p){
  var l=p.serverLevel();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();l.setDayTime(6000);
  l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(30)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
  for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)for(int y=-1;y<5;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?(z==4&&Math.abs(x)<=2?Blocks.GRASS_BLOCK:Blocks.SMOOTH_STONE):Blocks.AIR).defaultBlockState(),3);
  l.setBlock(BASE,Blocks.CHEST.defaultBlockState(),3);((ChestBlockEntity)l.getBlockEntity(BASE)).setItem(0,new ItemStack(Items.OAK_SAPLING,8));((ChestBlockEntity)l.getBlockEntity(BASE)).setItem(1,new ItemStack(Items.COBBLESTONE,8));
  planter=GolemHarvestAudit.golem(p,GolemMaterial.IRON,BASE.above(),8);planter.moveTo(BASE.getX()+1.5,BASE.getY(),BASE.getZ()+.5);planter.filters.setItem(0,new ItemStack(Items.OAK_SAPLING));
  for(int i=-1;i<=1;i++)mark(planter,BASE.offset(i*2,0,4),Direction.UP);
  planter.getEntityData().set(StrawGolem.USE_FLAGS,1);
  l.setBlock(LEVER,Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR),3);
  flipper=GolemHarvestAudit.golem(p,GolemMaterial.IRON,LEVER.offset(2,0,0),8);mark(flipper,LEVER,Direction.UP);
  l.setBlock(TORCH,Blocks.TORCH.defaultBlockState(),3);breaker=GolemHarvestAudit.golem(p,GolemMaterial.IRON,TORCH.offset(-2,0,0),8);mark(breaker,TORCH,Direction.UP);breaker.getEntityData().set(StrawGolem.USE_FLAGS,2);
  var menu=new GolemMenu(0,p.getInventory(),planter);check(menu.clickMenuButton(p,52)&&planter.useFlags()==5&&menu.clickMenuButton(p,52)&&planter.useFlags()==1,"menu button 52 toggles sneaking");menu.removed(p);check(!planter.paused,"closing the menu resumes the golem");
  var copy=new StrawGolem(Golemancy.ENTITY.get(),l);copy.restoreItem(planter.asItem());check(copy.core()==8&&copy.useFlags()==1,"core 8 and toggles survive the bell item");
  p.connection.teleport(BASE.getX()+.5,BASE.getY()+6,BASE.getZ()-4.5,0,45);
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.golemUseAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==40)server.execute(()->setup(server.getPlayerList().getPlayer(id)));
  if(ticks>60)server.execute(()->{if(server.overworld().getBlockState(LEVER).getValue(BlockStateProperties.POWERED))leverOn++;else leverOff++;});
  if(ticks>40&&ticks%50==0)server.execute(()->{var p=server.getPlayerList().getPlayer(id);System.out.println("TC4_USE progress t="+ticks+" saplings="+saplings(p)+" carried="+planter.carried()+" lever="+p.level().getBlockState(LEVER).getValue(BlockStateProperties.POWERED)+" torch="+p.level().getBlockState(TORCH)+" clicks="+GolemUseGoal.clicks+" taken="+GolemUseGoal.taken);});
  if(ticks==200)LiquidDeathAudit.shot(mc,"golem-use-working.png");
  if(ticks==500)server.execute(()->{var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();
   check(saplings(p)==3,"planter golem fetched saplings and planted all three marked empty spaces");
   check(planter.carried().isEmpty(),"idle planter walked home and returned the leftover saplings returned="+GolemUseGoal.returned);int home=0;var c=(ChestBlockEntity)l.getBlockEntity(BASE);for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(Items.OAK_SAPLING))home+=c.getItem(i).getCount();check(home==5,"5 saplings left after three plantings home="+home+" carried="+planter.carried().getCount());
   check(c.getItem(1).is(Items.COBBLESTONE)&&c.getItem(1).getCount()==8,"unfiltered cobblestone untouched");
   check(leverOn>0&&leverOff>0&&flipper.carried().isEmpty(),"homeless golem keeps flipping the lever bare-handed on="+leverOn+" off="+leverOff);
   check(l.getBlockState(TORCH).isAir()&&!l.getEntitiesOfClass(ItemEntity.class,new AABB(TORCH).inflate(3),e->e.getItem().is(Items.TORCH)).isEmpty(),"left-click golem broke the torch and it dropped");
   System.out.println("TC4_USE ALL PASS");});
  if(ticks==505)server.execute(()->server.getPlayerList().getPlayer(id).connection.teleport(BASE.getX()+1.5,BASE.getY(),BASE.getZ()-1.5,0,10));
  if(ticks==515)server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.openMenu(planter,b->b.writeVarInt(planter.getId()));});
  if(ticks==535)LiquidDeathAudit.shot(mc,"golem-use-gui.png");
  if(ticks==540)mc.stop();
 }
}
