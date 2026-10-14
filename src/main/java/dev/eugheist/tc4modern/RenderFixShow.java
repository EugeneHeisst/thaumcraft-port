package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Hidden screenshot check for cut-out models (lamps, candles, doors, pedestals, mnemonic matrix, greatwood leaves) and the hovering workbench wand. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class RenderFixShow {
 static final BlockPos BASE=new BlockPos(760,121,-90);static int ticks;
 static BlockState block(String id){return BuiltInRegistries.BLOCK.get(AlchemyContent.id(id)).defaultBlockState();}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.renderFixShowOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;var s=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==100)s.execute(()->{var p=s.getPlayerList().getPlayer(id);var l=p.serverLevel();if(p.level()!=s.overworld())p.teleportTo(s.overworld(),BASE.getX(),BASE.getY(),BASE.getZ(),0,0);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();l.setDayTime(6000);
   l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(12)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
   for(int x=-8;x<=8;x++)for(int z=-4;z<=6;z++)for(int y=-1;y<5;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.GRASS_BLOCK:Blocks.AIR).defaultBlockState(),3);
   String[] row={"lamp_growth","lamp_fertility","arcane_lamp","candle_4","candle_14","arcane_pedestal","mnemonic_matrix","wand_pedestal","greatwood_leaves"};
   for(int i=0;i<row.length;i++){var st=block(row[i]);if(st.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT))st=st.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,true);l.setBlock(BASE.offset(-6+i*1+i/2*0,0,2),st,3);}
   l.setBlock(BASE.offset(3,0,2),WardedArcana.DOOR.get().defaultBlockState(),3);l.setBlock(BASE.offset(3,1,2),WardedArcana.DOOR.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),3);
   l.setBlock(BASE.offset(5,0,2),TC4Modern.ARCANE_WORKBENCH.get().defaultBlockState(),3);if(l.getBlockEntity(BASE.offset(5,0,2)) instanceof ArcaneWorkbenchEntity t)t.inventory.setItem(9,WandParts.make(WandParts.CAPS.get("gold"),WandParts.RODDEFS.get("greatwood")));
   GolemHarvestAudit.golem(p,GolemMaterial.IRON,BASE.offset(-2,0,4),-1);GolemHarvestAudit.golem(p,GolemMaterial.THAUMIUM,BASE.offset(0,0,4),-1);
   p.connection.teleport(BASE.getX()-.5,BASE.getY()+1.6,BASE.getZ()-2.5,0,22);});
  if(ticks==150){LiquidDeathAudit.shot(mc,"renderfix-row.png");for(var g:mc.level.getEntitiesOfClass(StrawGolem.class,new AABB(BASE).inflate(8)))System.out.println("TC4_RENDERFIX golem name "+g.material()+" -> "+g.getName().getString());}
  if(ticks==155)s.execute(()->s.getPlayerList().getPlayer(id).connection.teleport(BASE.getX()+5.5,BASE.getY()+1.5,BASE.getZ()+.6,0,35));
  if(ticks==175)LiquidDeathAudit.shot(mc,"renderfix-wand-a.png");
  if(ticks==215){LiquidDeathAudit.shot(mc,"renderfix-wand-b.png");System.out.println("TC4_RENDERFIX DONE");mc.stop();}
 }
}
