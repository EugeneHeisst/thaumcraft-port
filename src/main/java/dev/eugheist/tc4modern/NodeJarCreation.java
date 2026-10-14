package dev.eugheist.tc4modern;
import net.minecraft.core.*;import net.minecraft.tags.BlockTags;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.ItemStack;
/** Original WandManager 3x4x3 glass enclosure and wooden slab lid. */
public final class NodeJarCreation {
 public static boolean fits(Level level,BlockPos node){for(int y=-1;y<=2;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var p=node.offset(x,y,z);if(!level.hasChunkAt(p))return false;if(x==0&&y==0&&z==0){if(!level.getBlockState(p).is(TC4Modern.AURA_NODE.get())||!(level.getBlockEntity(p) instanceof AuraNodeEntity))return false;continue;}var state=level.getBlockState(p);if(y==2?!state.is(BlockTags.WOODEN_SLABS):!state.is(Blocks.GLASS))return false;}return true;}
 public static boolean create(Level level,BlockPos clicked,Player player,ItemStack wand){if(!ResearchProgress.completed(player.getPersistentData()).contains("NODEJAR"))return false;var cost=new thaumcraft.api.aspects.AspectList();for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects())cost.add(a,70);
  for(int y=-2;y<=1;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var pos=clicked.offset(x,y,z);if(!fits(level,pos)||!WandData.consumeCrafting(wand,cost,false,player))continue;for(int yy=-1;yy<=2;yy++)for(int xx=-1;xx<=1;xx++)for(int zz=-1;zz<=1;zz++)if(!level.mayInteract(player,pos.offset(xx,yy,zz))||!player.mayUseItemAt(pos.offset(xx,yy,zz),Direction.UP,wand))return false;
   if(level.isClientSide)return true;if(!WandData.consumeCrafting(wand,cost,true,player))return false;var node=(AuraNodeEntity)level.getBlockEntity(pos);level.setBlock(pos,BasicsContent.NODE_JAR.get().defaultBlockState(),2);var jar=(NodeJarEntity)level.getBlockEntity(pos);jar.capture(node);for(int yy=-1;yy<=2;yy++)for(int xx=-1;xx<=1;xx++)for(int zz=-1;zz<=1;zz++)if(xx!=0||yy!=0||zz!=0)level.removeBlock(pos.offset(xx,yy,zz),false);jar.changed();level.playSound(null,pos,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("wand")),net.minecraft.sounds.SoundSource.BLOCKS,1,1);return true;
  }return false;
 }
}
