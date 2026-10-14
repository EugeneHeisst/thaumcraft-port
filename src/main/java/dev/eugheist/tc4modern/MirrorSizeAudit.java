package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class MirrorSizeAudit {
 static int ticks;static final BlockPos BASE=new BlockPos(140,121,-140);static MagicMirrors.MirrorEntity source,remote;static volatile boolean prepared,done;static ThaumatoriumEntity thaumatorium;
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_MIRROR_SIZE FAIL "+s);System.out.println("TC4_MIRROR_SIZE PASS "+s);}
 static void prepare(ServerPlayer p){var l=p.serverLevel();p.removeAllEffects();p.setHealth(p.getMaxHealth());p.closeContainer();p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);l.setDayTime(6000);l.setWeatherParameters(10000,0,false,false);
 for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=-1;y<=4;y++)l.setBlock(BASE.offset(x,y,z),y==-1?Blocks.GLOWSTONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
 l.getEntitiesOfClass(ItemEntity.class,new AABB(BASE).inflate(20)).forEach(ItemEntity::discard);
 source=MagicMirrorsAudit.put(l,BASE,false,Direction.UP);remote=MagicMirrorsAudit.put(l,BASE.offset(4,0,0),false,Direction.UP);MagicMirrorsAudit.pair(source,remote);
 var handler=l.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,BASE,Direction.UP);check(handler!=null&&handler.insertItem(0,new ItemStack(Items.EMERALD,3),false).isEmpty(),"registered inventory capability accepts automation insertion");
 var dropped=new ItemEntity(l,BASE.getX()+.5,BASE.getY()+1.5,BASE.getZ()+.5,new ItemStack(Items.DIAMOND,4));dropped.setDeltaMovement(0,0,0);dropped.setPickUpDelay(40);l.addFreshEntity(dropped);
 var machine=BASE.offset(6,0,3);l.setBlock(machine,Thaumatorium.BODY.get().defaultBlockState(),2);thaumatorium=(ThaumatoriumEntity)l.getBlockEntity(machine);thaumatorium.setItem(0,new ItemStack(Items.COAL,3));
 p.connection.teleport(BASE.getX()+2.5,BASE.getY()+1,BASE.getZ()+4.5,180,30);prepared=true;}
 static void finish(ServerPlayer p){var l=p.serverLevel();int count=l.getEntitiesOfClass(ItemEntity.class,new AABB(remote.getBlockPos()).inflate(2)).stream().filter(i->i.getItem().is(Items.DIAMOND)).mapToInt(i->i.getItem().getCount()).sum();System.out.println("TC4_MIRROR_SIZE transfer dest="+count+" sourceQueue="+source.output+" remoteQueue="+remote.output);int emeralds=l.getEntitiesOfClass(ItemEntity.class,new AABB(remote.getBlockPos()).inflate(2)).stream().filter(i->i.getItem().is(Items.EMERALD)).mapToInt(i->i.getItem().getCount()).sum();check(emeralds==3,"automated input arrives as three emeralds");check(count==4,"naturally dropped stack reaches destination and remains there");done=true;}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.mirrorSizeAuditOnly"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null)return;ticks++;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();if(ticks==40)server.execute(()->prepare(server.getPlayerList().getPlayer(id)));
 if(ticks==100){check(prepared,"fixture ready");LiquidDeathAudit.shot(mc,"mirror-real-drop.png");}
 String[] names={"mnemonic_matrix","alchemical_construct","node_jar"};for(int i=0;i<names.length;i++){if(ticks==120+i*35){var stack=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id(names[i])));mc.player.getInventory().setItem(0,stack);mc.player.getInventory().selected=0;var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);check(Math.abs(model.getTransforms().firstPersonRightHand.scale.x()-.4f)<.001&&Math.abs(model.getTransforms().thirdPersonRightHand.scale.x()-.375f)<.001,"standard hand transforms "+names[i]);}if(ticks==135+i*35)LiquidDeathAudit.shot(mc,names[i]+"-hand-fixed.png");}
 if(ticks==220){mc.level.removeBlockEntity(BASE.offset(6,0,3));server.execute(()->Thaumatorium.open(server.getPlayerList().getPlayer(id),thaumatorium));}
 if(ticks==240){check(mc.screen instanceof ThaumatoriumClient.Screen&&mc.player.containerMenu.getSlot(0).getItem().getCount()==3,"thaumatorium opens and receives slot contents with client block entity absent");check(((ThaumatoriumMenu)mc.player.containerMenu).tile!=null,"snapshot supplies missing client tile");LiquidDeathAudit.shot(mc,"thaumatorium-missing-client-tile-fixed.png");}
 if(ticks==260){mc.player.closeContainer();mc.setScreen(null);server.execute(()->server.getPlayerList().getPlayer(id).connection.teleport(BASE.getX()+.5,BASE.getY()+.5,BASE.getZ()+1.5,180,60));}
 if(ticks==275)LiquidDeathAudit.shot(mc,"mirror-linked-close-fixed.png");
 if(ticks==280)server.execute(()->finish(server.getPlayerList().getPlayer(id)));if(ticks==300){check(done,"server transfer completed");System.out.println("TC4_MIRROR_SIZE ALL PASS");mc.stop();}}
}
