package dev.eugheist.tc4modern;
import net.minecraft.core.*;import net.minecraft.nbt.*;import net.minecraft.world.level.block.entity.*;import net.minecraft.world.level.block.state.*;
/** A sealed snapshot: unlike a free node it cannot recharge or supply vis. */
public final class NodeJarEntity extends BlockEntity {
 public CompoundTag snapshot=new CompoundTag();
 public NodeJarEntity(BlockPos pos,BlockState state){super(BasicsContent.NODE_JAR_ENTITY.get(),pos,state);}
 public void capture(AuraNodeEntity node){snapshot=node.saveWithoutMetadata(level.registryAccess());var base=new CompoundTag();node.reservoir.aspects.writeToNBT(base);snapshot.put("AspectsBase",base.getList("Aspects",10));snapshot.putLong("lastActive",0);int mod=node.reservoir.modifier==null?-1:node.reservoir.modifier.ordinal();if(level.random.nextFloat()<.75f)mod=switch(mod){case 0->-1;case -1->1;case 1->2;default->mod;};snapshot.putByte("modifier",(byte)mod);changed();}
 public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
 public net.minecraft.world.item.ItemStack item(){var item=new net.minecraft.world.item.ItemStack(BasicsContent.NODE_JAR.get());item.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(snapshot));return item;}
 public void release(){var data=snapshot.copy();level.setBlock(worldPosition,TC4Modern.AURA_NODE.get().defaultBlockState(),3);if(level.getBlockEntity(worldPosition) instanceof AuraNodeEntity node){node.loadWithComponents(data,level.registryAccess());node.changed();}level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.GLASS_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,1,1);}
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);tag.put("Node",snapshot.copy());}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);snapshot=tag.getCompound("Node").copy();}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
 @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
