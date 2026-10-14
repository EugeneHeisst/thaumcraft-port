package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Isolated native biome restoration, support, growth and original sound/renderer checks. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class EtherealBloomAudit {
 static final BlockPos BASE=FocalAudit.BASE.offset(8,0,8);static int ticks;static volatile boolean passed,naturalPassed;static int initial;
 static void check(boolean ok,String text){if(!ok)throw new IllegalStateException("TC4_BLOOM FAIL "+text);System.out.println("TC4_BLOOM PASS "+text);}
 static void prepare(ServerPlayer player){
  var level=player.serverLevel();player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.getInventory().clearContent();
  for(var cat:thaumcraft.api.research.ResearchCategories.researchCategories.values())for(var r:cat.research.values())DevCommands.grant(player.getPersistentData(),r.key,new java.util.HashSet<>());ThaumometerItem.sync(player,"");AlchemyStarterAudit.run(player);
  var registry=level.registryAccess().registryOrThrow(Registries.BIOME);var taint=registry.getHolderOrThrow(TaintBiomes.TAINTED_LAND);var forest=registry.getHolderOrThrow(MagicalForestContent.BIOME);var desert=registry.getHolderOrThrow(Biomes.DESERT);
  var expected=level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(BASE.getX()),QuartPos.fromBlock(BASE.getY()),QuartPos.fromBlock(BASE.getZ()),level.getChunkSource().randomState().sampler());
  TaintBiomes.setColumn(level,BASE,taint);check(EtherealBloom.cleanse(level,BASE,0,0),"tainted cell restored");check(level.getNoiseBiome(QuartPos.fromBlock(BASE.getX()),QuartPos.fromBlock(BASE.getY()),QuartPos.fromBlock(BASE.getZ())).equals(expected),"restores generator biome rather than arbitrary plains");
  TaintBiomes.setColumn(level,BASE,forest);check(EtherealBloom.cleanse(level,BASE,0,0),"original also removes mutated magical forest");
  TaintBiomes.setColumn(level,BASE,desert);check(!EtherealBloom.cleanse(level,BASE,0,0),"ordinary biomes unchanged");
  TaintBiomes.setColumn(level,BASE.offset(7,0,7),taint);check(!EtherealBloom.cleanse(level,BASE,7,7),"corner outside radius nine excluded");
  TaintBiomes.setColumn(level,BASE.offset(6,0,6),taint);check(EtherealBloom.cleanse(level,BASE,6,6),"inner diagonal included");
  level.setBlock(BASE.below(),Blocks.STONE.defaultBlockState(),3);level.setBlock(BASE,EtherealBloom.BLOCK.get().defaultBlockState(),3);check(level.getBlockState(BASE).getLightEmission()==15,"bloom emits original light fifteen");
  level.setBlock(BASE.below(),Blocks.AIR.defaultBlockState(),3);check(level.isEmptyBlock(BASE),"unsupported bloom drops and disappears");level.setBlock(BASE.below(),Blocks.STONE.defaultBlockState(),3);
  for(int x=-8;x<=8;x+=4)for(int z=-8;z<=8;z+=4)TaintBiomes.setColumn(level,BASE.offset(x,0,z),taint);
  for(var drop:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(BASE).inflate(5)))drop.discard();
  initial=count(level);level.setBlock(BASE,EtherealBloom.BLOCK.get().defaultBlockState(),3);check(level.getBlockEntity(BASE) instanceof EtherealBloom.Entity,"native bloom block entity placed");
  player.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(EtherealBloom.ITEM.get()));player.getInventory().selected=8;player.connection.teleport(BASE.getX()+2,BASE.getY()+1.6,BASE.getZ()-3,30,13);passed=true;
 }
 static int count(net.minecraft.server.level.ServerLevel level){int n=0;for(int x=-8;x<=8;x+=4)for(int z=-8;z<=8;z+=4)if(level.getNoiseBiome(QuartPos.fromBlock(BASE.getX()+x),QuartPos.fromBlock(BASE.getY()),QuartPos.fromBlock(BASE.getZ()+z)).is(TaintBiomes.TAINTED_LAND))n++;return n;}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.bloomAuditOnly"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();if(ticks==80)server.execute(()->prepare(server.getPlayerList().getPlayer(id)));
  if(ticks==100){check(passed,"server mechanics completed");LiquidDeathAudit.shot(mc,"bloom-growing-20.png");}if(ticks==130)LiquidDeathAudit.shot(mc,"bloom-growing-50.png");if(ticks==185){LiquidDeathAudit.shot(mc,"bloom-grown.png");check(mc.level.getBlockEntity(BASE) instanceof EtherealBloom.Entity b&&b.growthCounter>=100,"client staged growth reaches full size");server.execute(()->{check(count(server.overworld())<initial,"natural twenty-tick cleansing removes tainted cells");naturalPassed=true;});}if(ticks==210){check(naturalPassed,"natural server ticks completed");mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));}if(ticks==220)LiquidDeathAudit.shot(mc,"bloom-inventory.png");if(ticks==240){check(AlchemyStarterAudit.passed,"eighteen original gameplay researches and crucible recipes verified");mc.stop();}}
}
