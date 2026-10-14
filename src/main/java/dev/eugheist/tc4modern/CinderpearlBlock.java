package dev.eugheist.tc4modern;
public final class CinderpearlBlock extends SilverwoodContent.PlantBlock {
 public CinderpearlBlock(Properties p){super(p);}
 @Override protected boolean mayPlaceOn(net.minecraft.world.level.block.state.BlockState s,net.minecraft.world.level.BlockGetter l,net.minecraft.core.BlockPos p){return super.mayPlaceOn(s,l,p)||s.is(net.minecraft.tags.BlockTags.SAND);}
 @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(net.minecraft.world.level.block.state.BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder b){return java.util.List.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLAZE_POWDER));}
}
