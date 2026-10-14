package dev.eugheist.tc4modern;
public final class BellowsItem extends net.minecraft.world.item.BlockItem {
    public BellowsItem(net.minecraft.world.level.block.Block block,Properties properties){super(block,properties);}
    @Override protected boolean placeBlock(net.minecraft.world.item.context.BlockPlaceContext context,net.minecraft.world.level.block.state.BlockState state){
        boolean placed=super.placeBlock(context,state);
        if(placed&&context.getLevel().getBlockEntity(context.getClickedPos()) instanceof BellowsEntity tile)tile.placed(context.getClickedFace());
        return placed;
    }
}
