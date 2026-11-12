package thaumcraft.common.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;

/** Original ItemResource metadata 1 placement, adapted to modern placement context. */
public final class ItemNitor extends BlockItem {
    public ItemNitor(Block block,Item.Properties properties){super(block,properties);}
    @Override public InteractionResult place(BlockPlaceContext context){
        int before=context.getItemInHand().getCount();
        InteractionResult result=super.place(context);
        if(result.consumesAction() && !context.getLevel().isClientSide
                && context.getPlayer()!=null && context.getPlayer().getAbilities().instabuild
                && context.getItemInHand().getCount()==before)context.getItemInHand().shrink(1);
        return result;
    }
}
