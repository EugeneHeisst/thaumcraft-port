package dev.eugheist.tc4modern;
import net.minecraft.core.*;import net.minecraft.nbt.*;import net.minecraft.world.level.block.entity.*;import net.minecraft.world.level.block.state.*;import thaumcraft.api.aspects.*;
/** Original TileManaPod growth and weighted adjacent-aspect breeding. */
public final class ManaPodEntity extends BlockEntity {
 public Aspect aspect;
 public ManaPodEntity(BlockPos p,BlockState s){super(BasicsContent.MANA_POD_ENTITY.get(),p,s);}
 public void grow(){int age=getBlockState().getValue(ManaPodBlock.AGE);if(age<7)level.setBlock(worldPosition,getBlockState().setValue(ManaPodBlock.AGE,++age),3);if(age<=2)return;if(age==3){var nearby=new AspectList();if(aspect!=null)nearby.add(aspect,1);for(var d:Direction.Plane.HORIZONTAL)if(level.getBlockEntity(worldPosition.relative(d)) instanceof ManaPodEntity pod&&pod.aspect!=null)nearby.add(pod.aspect,1);if(nearby.size()>1){var out=new java.util.ArrayList<Aspect>();var aa=nearby.getAspects();for(int i=0;i<aa.length;i++){out.add(aa[i]);for(int j=0;j<aa.length;j++){if(i==j)continue;var combo=thaumcraft.common.lib.research.ResearchManager.getCombinationResult(aa[i],aa[j]);if(combo!=null){out.add(combo);out.add(combo);}}}aspect=out.get(level.random.nextInt(out.size()));}if(nearby.size()>0&&aspect==null)aspect=nearby.getAspectsSortedAmount()[0];}if(aspect==null){var primals=Aspect.getPrimalAspects();aspect=level.random.nextInt(8)==0?Aspect.PLANT:primals.get(level.random.nextInt(primals.size()));}changed();}
 public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
 @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);if(aspect!=null)t.putString("aspect",aspect.getTag());}
 @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);aspect=Aspect.getAspect(t.getString("aspect"));}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}@Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
