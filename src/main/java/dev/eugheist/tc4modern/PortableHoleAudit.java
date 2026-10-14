package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;

/** Dedicated checks of the original portable hole blacklist, its configured names and the Outer Lands refusal, with real focus use. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class PortableHoleAudit {
 static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_PORTABLE_HOLE FAIL "+text);System.out.println("TC4_PORTABLE_HOLE PASS "+text);}
 static ItemStack wand(){var wand=WandParts.make(WandParts.CAPS.get("gold"),WandParts.RODDEFS.get("greatwood"));for(var aspect:Aspect.getPrimalAspects())WandData.add(wand,aspect,100,true);FociContent.setFocus(wand,new ItemStack(FociWorld.PORTABLE_HOLE.get()));return wand;}
 static int vis(ItemStack wand){int sum=0;for(var aspect:Aspect.getPrimalAspects())sum+=WandData.get(wand,aspect);return sum;}
 /** A fake player facing north at a three-block stone wall, the middle block replaced by the probe. */
 static boolean use(ServerLevel l,BlockPos at,Block probe,ItemStack wand){
  for(int z=0;z<3;z++)l.setBlock(at.north(z),Blocks.STONE.defaultBlockState(),3);l.setBlock(at.north(1),probe.defaultBlockState(),3);l.setBlock(at.north(3),Blocks.AIR.defaultBlockState(),3);for(int z=1;z<=3;z++)l.setBlock(at.south(z),Blocks.AIR.defaultBlockState(),3);l.setBlock(at.south(2).above(),Blocks.AIR.defaultBlockState(),3);
  var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("tc4-hole-audit".getBytes()),"tc4-hole-audit"));p.moveTo(at.getX()+.5,at.getY()-1.62+.5,at.getZ()+2.5,180,0);p.setYRot(180);p.yRotO=180;p.setXRot(0);p.xRotO=0;p.setYHeadRot(180);p.setItemInHand(InteractionHand.MAIN_HAND,wand);
  return ((FociWorld.PortableHoleFocus)FociWorld.PORTABLE_HOLE.get()).onFocusRightClick(wand,l,p);
 }
 @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent e){if(!Boolean.getBoolean("tc4modern.portableHoleAudit"))return;var server=e.getServer();try{
  check(WarpConfig.SPEC.isLoaded()&&PortableHoleConfig.NAMES.get().equals("iron_door, minecraft:obsidian"),"actual TOML loads the configured comma-delimited names");
  var l=server.overworld();var at=new BlockPos(0,150,0);
  for(var block:java.util.List.of(Blocks.RED_BED,Blocks.PISTON,Blocks.STICKY_PISTON,Blocks.PISTON_HEAD,Blocks.MOVING_PISTON,Blocks.OAK_DOOR,Blocks.SPRUCE_DOOR,Blocks.IRON_DOOR,CrimsonContent.ALTAR.get(),OuterContent.GLYPHED.get(),OuterContent.GLOWING.get(),OuterDevices.TRAP.get(),OuterDevices.VENT.get(),OuterGate.FRAME.get(),OuterGate.LOCK.get(),OuterPortal.BLOCK.get(),TC4Modern.INFERNAL_FURNACE.get()))
   check(PortableHoleConfig.blocked(block.defaultBlockState()),"original blacklist covers "+net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block));
  check(PortableHoleConfig.blocked(Blocks.OBSIDIAN.defaultBlockState())&&!PortableHoleConfig.blocked(Blocks.STONE.defaultBlockState())&&!PortableHoleConfig.blocked(Blocks.DIRT.defaultBlockState()),"configured obsidian joins the list while ordinary stone and dirt stay holeable");
  check(OuterContent.GLYPHED.get().defaultBlockState().getDestroySpeed(l,at)==2&&OuterContent.GLYPHED.get().getExplosionResistance()==30&&OuterDevices.TRAP.get().defaultBlockState().getDestroySpeed(l,at)==15&&OuterDevices.VENT.get().getExplosionResistance()==30,"BlockEldritch metadata 4/5 and 9/10 use original hardness 2/15 and resistance 30");
  var miner=net.neoforged.neoforge.common.util.FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("tc4-mining-audit".getBytes()),"tc4-mining-audit"));
  miner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);miner.setOnGround(true);miner.removeAllEffects();miner.moveTo(8.5,150,0.5);
  var miningAt=at.east(8);
  for(var block:java.util.List.of(OuterDevices.VENT.get(),OuterDevices.TRAP.get()))for(var facing:Direction.values()){
   var state=block.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,facing);l.setBlock(miningAt,state,3);
   miner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);float hand=state.getDestroyProgress(miner,l,miningAt);
   miner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));float iron=state.getDestroyProgress(miner,l,miningAt);
   miner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(net.minecraft.world.item.Items.NETHERITE_PICKAXE));float netherite=state.getDestroyProgress(miner,l,miningAt);
   check(Math.abs(hand-1f/450)<.00001&&Math.abs(iron-1f/75)<.00001&&Math.abs(netherite-1f/50)<.00001,"actual mining progress hand/iron/netherite 450/75/50 ticks: "+net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block)+" "+facing);
   check(miner.gameMode.destroyBlock(miningAt)&&l.getBlockState(miningAt).isAir(),"survival pickaxe destroys device "+facing);
  }
  var wand=wand();int before=vis(wand);boolean used=use(l,at,Blocks.STONE,wand);check(used&&l.getBlockEntity(at) instanceof FociWorld.HoleEntity hole&&hole.count==4&&hole.old.is(Blocks.STONE)&&vis(wand)<before,"real focus opens a hole sized for all three stone blocks and spends vis");
  wand=wand();before=vis(wand);use(l,at,Blocks.OBSIDIAN,wand);check(l.getBlockEntity(at) instanceof FociWorld.HoleEntity hole2&&hole2.count==2&&l.getBlockState(at.north()).is(Blocks.OBSIDIAN)&&!FociWorld.createHole(l,at.north(),Direction.SOUTH.get3DDataValue(),(byte)1,(short)120)&&l.getBlockState(at.north()).is(Blocks.OBSIDIAN),"configured blacklisted block stops the tunnel depth and cannot itself become a hole");
  var door=at.east(4);l.setBlock(door,Blocks.STONE.defaultBlockState(),3);l.setBlock(door.above(),Blocks.STONE.defaultBlockState(),3);l.setBlock(door.above(),Blocks.OAK_DOOR.defaultBlockState(),3);check(!FociWorld.createHole(l,door.above(),0,(byte)1,(short)120)&&l.getBlockState(door.above()).is(Blocks.OAK_DOOR),"wooden doors stay intact like the original fixed list");
  var outer=server.getLevel(OuterDimension.KEY);check(outer!=null,"Outer Lands dimension loaded");var outerAt=new BlockPos(8,120,8);wand=wand();before=vis(wand);check(use(outer,outerAt,Blocks.STONE,wand)&&outer.getBlockState(outerAt).is(Blocks.STONE)&&vis(wand)==before,"Outer Lands refuses the portable hole without spending vis like the original");
  System.out.println("TC4_PORTABLE_HOLE ALL PASS original blacklist, configured names, eldritch hardness and Outer Lands refusal");
 }finally{server.halt(false);}}
}
