package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Dedicated checks of original allow_warded_stone=false: cosmetic hardness, ordinary breaking and drops, wand removal by anyone, no warding research. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class WardedConfigAudit {
 static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_WARDED_CONFIG FAIL "+text);System.out.println("TC4_WARDED_CONFIG PASS "+text);}
 static int drops(net.minecraft.server.level.ServerLevel l,BlockPos at,net.minecraft.world.item.Item item){int n=0;for(var e:l.getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(2)))if(e.getItem().is(item)){n+=e.getItem().getCount();e.discard();}return n;}
 @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent e){if(!Boolean.getBoolean("tc4modern.wardedConfigAudit"))return;var server=e.getServer();try{
  check(WarpConfig.SPEC.isLoaded()&&!WarpConfig.wardedStone(),"actual TOML disables allow_warded_stone");
  thaumcraft.common.config.ConfigResearchData.init();var api=thaumcraft.api.research.ResearchCategories.class;
  check(thaumcraft.api.research.ResearchCategories.getResearch("WARDEDARCANA")==null&&thaumcraft.api.research.ResearchCategories.getResearch("FOCUSWARDING")==null&&thaumcraft.api.research.ResearchCategories.getResearch("FOCUSEXCAVATION")!=null,"WARDEDARCANA and FOCUSWARDING are not registered, other research remains");
  /* the same TOML also turns off allow_mirrors */
  check(WarpConfig.SPEC.isLoaded()&&!WarpConfig.allowMirrors(),"actual TOML disables allow_mirrors");
  check(java.util.stream.Stream.of("MIRROR","MIRRORHAND","MIRRORESSENTIA").allMatch(k->thaumcraft.api.research.ResearchCategories.getResearch(k)==null)&&thaumcraft.api.research.ResearchCategories.getResearch("INFUSION")!=null,"mirror research is not registered");
  check(InfusionItemRecipes.all().stream().noneMatch(r->r.key().startsWith("Mirror"))&&InfusionItemRecipes.all().stream().anyMatch(r->r.key().equals("ElementalAxe")),"mirror infusion recipes are not added, other infusion recipes remain");
  var l=server.overworld();var at=new BlockPos(0,180,0);for(int x=-1;x<=6;x++)for(int z=-1;z<=1;z++){l.setBlock(at.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);for(int y=0;y<3;y++)l.setBlock(at.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);}
  var owner=net.neoforged.neoforge.common.util.FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("tc4-ward-owner".getBytes()),"WardOwner"));
  var other=net.neoforged.neoforge.common.util.FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("tc4-ward-other".getBytes()),"Stranger"));other.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
  var glass=at;var plate=at.east(2);var door=at.east(4);
  l.setBlock(glass,WardedArcana.GLASS.get().defaultBlockState(),3);WardedArcana.assign(l,glass,owner);l.setBlock(plate,WardedArcana.PLATE.get().defaultBlockState(),3);WardedArcana.assign(l,plate,owner);
  l.setBlock(door,WardedArcana.DOOR.get().defaultBlockState(),3);l.setBlock(door.above(),WardedArcana.DOOR.get().defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),3);WardedArcana.assign(l,door,owner);WardedArcana.assign(l,door.above(),owner);
  check(l.getBlockState(glass).getDestroySpeed(l,glass)==5&&l.getBlockState(plate).getDestroySpeed(l,plate)==2&&l.getBlockState(door).getDestroySpeed(l,door)==15,"cosmetic hardness follows the original 5 / 2 / 15");
  check(l.getBlockState(glass).getDestroyProgress(other,l,glass)>0,"a stranger makes real mining progress on warded glass");
  check(other.gameMode.destroyBlock(glass)&&l.getBlockState(glass).isAir()&&drops(l,glass,WardedArcana.GLASS.get().asItem())==1,"a stranger breaks warded glass and it drops itself");
  check(other.gameMode.destroyBlock(plate)&&l.getBlockState(plate).isAir()&&drops(l,plate,WardedArcana.PLATE.get().asItem())==1,"a stranger breaks the arcane pressure plate and it drops itself");
  boolean broke=other.gameMode.destroyBlock(door.above());
  check(broke&&l.getBlockState(door).isAir()&&l.getBlockState(door.above()).isAir()&&drops(l,door,WardedArcana.DOOR.get().asItem())==1,"breaking the upper door half removes both halves and drops one arcane door");
  l.setBlock(glass,WardedArcana.GLASS.get().defaultBlockState(),3);WardedArcana.assign(l,glass,owner);
  var wand=WandParts.make(WandParts.CAPS.get("iron"),WandParts.RODDEFS.get("wood"));other.setItemInHand(InteractionHand.MAIN_HAND,wand);
  var ctx=new net.minecraft.world.item.context.UseOnContext(other,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(glass),Direction.UP,glass,false));
  check(WardedArcana.removeWithWand(wand,ctx)&&l.getBlockState(glass).isAir()&&drops(l,glass,WardedArcana.GLASS.get().asItem())==1,"any player removes cosmetic warded glass with a wand, as the original !wardedStone check allows");
  for(var clicked:new BlockPos[]{door,door.above()}){l.setBlock(door,WardedArcana.DOOR.get().defaultBlockState(),3);l.setBlock(door.above(),WardedArcana.DOOR.get().defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),3);WardedArcana.assign(l,door,owner);WardedArcana.assign(l,door.above(),owner);
   var doorCtx=new net.minecraft.world.item.context.UseOnContext(other,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(clicked),Direction.SOUTH,clicked,false));
   check(WardedArcana.removeWithWand(wand,doorCtx)&&l.getBlockState(door).isAir()&&l.getBlockState(door.above()).isAir()&&drops(l,door,WardedArcana.DOOR.get().asItem())==1,"wand removal of the "+(clicked.equals(door)?"lower":"upper")+" door half by a stranger yields exactly one door");}
  System.out.println("TC4_WARDED_CONFIG ALL PASS cosmetic warded blocks, ordinary drops, wand removal, gated research and no mirrors");
 }finally{server.halt(false);}}
}
