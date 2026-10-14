package dev.eugheist.tc4modern;
import com.mojang.serialization.MapCodec;import net.minecraft.core.*;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.entity.*;import net.minecraft.world.level.block.state.*;import net.minecraft.world.phys.shapes.*;
public final class NodeJarBlock extends BaseEntityBlock {
 public static final MapCodec<NodeJarBlock> CODEC=simpleCodec(NodeJarBlock::new);public NodeJarBlock(Properties p){super(p);}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}@Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new NodeJarEntity(p,s);}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(3,0,3,13,12,13);}
 @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,net.minecraft.world.entity.LivingEntity e,net.minecraft.world.item.ItemStack item){if(l.getBlockEntity(p) instanceof NodeJarEntity jar){jar.snapshot=NoteData.tag(item);jar.changed();}}
 @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder b){var tile=b.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);return tile instanceof NodeJarEntity jar?java.util.List.of(jar.item()):java.util.List.of(new net.minecraft.world.item.ItemStack(this));}
}
