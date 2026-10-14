package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import thaumcraft.api.aspects.*;
/** WandManager.createOculus: four eyes, sinister node and 100 vis of each primal. */
final class OuterActivation {
 static boolean use(UseOnContext context,ItemStack wand){var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();if(player==null||!ResearchProgress.completed(player.getPersistentData()).contains("OCULUS")||!(level.getBlockEntity(pos) instanceof CrimsonAltarEntity altar)||altar.getEyes()!=4||altar.isOpen()||!(level.getBlockEntity(pos.above()) instanceof AuraNodeEntity node)||node.nodeType!=thaumcraft.api.nodes.NodeType.DARK)return false;
  if(level.isClientSide)return true;var server=(ServerLevel)level;ensureMaze(server,pos);var cell=OuterMazeData.get(server).cell(pos.getX()>>4,pos.getZ()>>4);if(cell==null||cell.feature!=1)return false;var cost=new AspectList();for(var a:Aspect.getPrimalAspects())cost.add(a,100);if(!WandData.consumeCrafting(wand,cost,true,player))return false;altar.openPortal();level.setBlockAndUpdate(pos.above(),OuterPortal.BLOCK.get().defaultBlockState());level.playSound(null,pos,FociContent.sound("wand"),net.minecraft.sounds.SoundSource.BLOCKS,1,1);return true;
 }
 static void ensureMaze(ServerLevel level,BlockPos pos){var data=OuterMazeData.get(level);int cx=pos.getX()>>4,cz=pos.getZ()>>4;if(data.cell(cx,cz)!=null)return;int width=15+level.random.nextInt(8)*2,height=15+level.random.nextInt(8)*2;if(!data.create(cx,cz,width,height,level.random.nextLong()))return;
  // A second obelisk may allocate a labyrinth where the empty dimension was already explored.
  // Populate those loaded chunks now; fresh chunks use the normal decoration pipeline.
  var outer=level.getServer().getLevel(OuterDimension.KEY);if(outer==null)return;for(int x=cx-width;x<=cx+width;x++)for(int z=cz-height;z<=cz+height;z++){var cell=data.cell(x,z);if(cell!=null&&outer.getChunkSource().getChunkNow(x,z)!=null&&outer.isEmptyBlock(new BlockPos(x*16+8,51,z*16+8)))OuterChunkGenerator.generate(outer,x,z,cell);}
}
}
