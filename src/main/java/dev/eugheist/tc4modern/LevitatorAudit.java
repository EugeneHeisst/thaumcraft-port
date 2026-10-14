package dev.eugheist.tc4modern;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class LevitatorAudit {
 static final BlockPos POS=FocalAudit.BASE.offset(32,1,64);static int ticks;static volatile boolean prepared,lifted;static ItemEntity item;static double initial;
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_LIFTER FAIL "+s);System.out.println("TC4_LIFTER PASS "+s);}
 static void prepare(ServerPlayer p){var l=p.serverLevel();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.removeAllEffects();p.getInventory().clearContent();for(var cat:thaumcraft.api.research.ResearchCategories.researchCategories.values())for(var r:cat.research.values())DevCommands.grant(p.getPersistentData(),r.key,new HashSet<>());ThaumometerItem.sync(p,"");
  for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-3;y<=32;y++)l.setBlock(POS.offset(x,y,z),y==-3?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
  l.setBlock(POS,Levitator.BLOCK.get().defaultBlockState(),3);var t=(Levitator.LifterEntity)l.getBlockEntity(POS);Levitator.LifterEntity.tick(l,POS,l.getBlockState(POS),t);check(t.rangeAbove==10,"single levitator reaches10blocks");
  l.setBlock(POS.below(),Levitator.BLOCK.get().defaultBlockState(),3);l.setBlock(POS.below(2),Levitator.BLOCK.get().defaultBlockState(),3);t.requiresUpdate=true;Levitator.LifterEntity.tick(l,POS,l.getBlockState(POS),t);check(t.rangeAbove==30,"three-block stack reaches30blocks");
  l.setBlock(POS.above(8),Blocks.STONE.defaultBlockState(),3);t.requiresUpdate=true;Levitator.LifterEntity.tick(l,POS,l.getBlockState(POS),t);check(t.rangeAbove==7,"opaque ceiling truncates lift column");l.removeBlock(POS.above(8),false);
  l.setBlock(POS.below().east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);t.requiresUpdate=true;Levitator.LifterEntity.tick(l,POS,l.getBlockState(POS),t);check(t.rangeAbove==10,"powered lower levitator stops stack height contribution");l.removeBlock(POS.below().east(),false);
  t.requiresUpdate=true;item=new ItemEntity(l,POS.getX()+.5,POS.getY()+1.3,POS.getZ()+.5,new ItemStack(Items.DIAMOND));item.setDeltaMovement(Vec3.ZERO);item.fallDistance=9;l.addFreshEntity(item);Levitator.LifterEntity.tick(l,POS,l.getBlockState(POS),t);check(Math.abs(item.getDeltaMovement().y-.1f)<.0001&&item.fallDistance==0,"item accelerates by.1 and resets fall damage");item.setDeltaMovement(0,.34,0);Levitator.LifterEntity.tick(l,POS,l.getBlockState(POS),t);check(item.getDeltaMovement().y>.43,"original.35 check permits overshoot to.44");
  l.setBlock(POS.above().east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);item.setDeltaMovement(Vec3.ZERO);Levitator.LifterEntity.tick(l,POS,l.getBlockState(POS),t);check(item.getDeltaMovement().y==0&&t.gettingPower(),"power adjacent to block above disables lift");l.removeBlock(POS.above().east(),false);
  var cow=new net.minecraft.world.entity.animal.Cow(EntityType.COW,l);cow.moveTo(POS.getX()+.5,POS.getY()+2,POS.getZ()+.5);l.addFreshEntity(cow);Levitator.LifterEntity.tick(l,POS,l.getBlockState(POS),t);check(cow.getDeltaMovement().y>.09,"pushable living target lifted");cow.discard();
  item.setDeltaMovement(Vec3.ZERO);initial=item.getY();p.connection.teleport(POS.getX()+5,POS.getY()+4,POS.getZ()-6,40,25);p.getInventory().setItem(0,new ItemStack(Levitator.BLOCK.get()));p.getInventory().selected=8;
  var r=ArcaneRecipes.entries().stream().filter(e->e.key().equals("Levitator")).findFirst().orElseThrow();var grid=r.ingredients().stream().map(i->i.getItems()[0].copy()).toList();check(r.matches(grid,Set.of("LEVITATOR"))&&!r.matches(grid,Set.of()),"original levitator recipe with research gate");prepared=true;
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.lifterAuditOnly"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();if(ticks==80)server.execute(()->prepare(server.getPlayerList().getPlayer(id)));if(ticks==120){check(prepared,"server setup complete");var s=mc.level.getBlockState(POS);var quads=mc.getBlockRenderer().getBlockModel(s).getQuads(s,null,net.minecraft.util.RandomSource.create());check(quads.stream().noneMatch(q->q.getSprite().contents().name().getPath().equals("missingno")),"original levitator textures resolved");LiquidDeathAudit.shot(mc,"levitator-stack.png");}if(ticks==130)server.execute(()->{check(item.getY()>initial+5,"actual natural ticks lift dropped item up stack");lifted=true;});if(ticks==140){check(lifted,"natural lift complete");var inv=mc.player.getInventory();var book=new ThaumonomiconScreen(new ThaumonomiconMenu(0,inv),inv,net.minecraft.network.chat.Component.empty());book.auditOpen(thaumcraft.api.research.ResearchCategories.getResearch("LEVITATOR"),0);mc.setScreen(book);}if(ticks==150)LiquidDeathAudit.shot(mc,"levitator-book.png");if(ticks==160)mc.stop();}
}
