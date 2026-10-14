package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Real client right click with the shovel of the earthmover: all 9 placed blocks must exist on both sides (no ghost blocks). */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class ShovelGhostAudit {
 static int ticks;static volatile boolean ready;static final BlockPos C=InfusionAudit.POS.offset(-20,0,-20);
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_SHOVEL FAIL "+s);System.out.println("TC4_SHOVEL PASS "+s);}
 static void prepare(ServerPlayer p){var l=p.serverLevel();p.setGameMode(GameType.SURVIVAL);for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-1;y<=4;y++)l.setBlock(C.offset(x,y,z),y==-1?Blocks.DIRT.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
  p.getInventory().clearContent();p.getInventory().selected=0;p.getInventory().setItem(0,new ItemStack(ElementalTools.item("shovel")));p.getInventory().setItem(2,new ItemStack(Items.DIRT,64));p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(C.getX()+.5,C.getY()+2,C.getZ()-2.5,0,50);ready=true;}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.shovelGhostAuditOnly"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==80)server.execute(()->prepare(server.getPlayerList().getPlayer(id)));
  if(ticks==140){check(ready&&mc.player.getMainHandItem().getItem()==ElementalTools.item("shovel"),"client holds the shovel");var at=C.below();mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at).add(0,.5,0),Direction.UP,at,false));}
  if(ticks==200){int server9=0,client9=0;for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var at=C.offset(x,0,z);if(server.overworld().getBlockState(at).is(Blocks.DIRT))server9++;if(mc.level.getBlockState(at).is(Blocks.DIRT))client9++;}
   LiquidDeathAudit.shot(mc,"shovel-3x3.png");mc.stop();check(server9==9,"server placed 3x3 dirt ("+server9+")");check(client9==9,"client sees all 3x3 dirt ("+client9+")");}}
}
