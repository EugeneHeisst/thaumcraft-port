package dev.eugheist.tc4modern;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Platform adapter for original ItemInkwell.onItemUseFirst: joins two tables and inserts the tools. */
public class ScribingToolsItem extends Item implements thaumcraft.api.IScribeTools {
    public ScribingToolsItem(Properties properties) { super(properties); }
    /** Original IScribeTools check: any ink-holding scribe tool (Thaumic Tinkerer infused inkwell too), with uses left. */
    public static boolean isScribe(ItemStack stack) { return stack.getItem() instanceof thaumcraft.api.IScribeTools; }
    public static boolean hasInk(ItemStack stack) { return isScribe(stack) && stack.getDamageValue() < stack.getMaxDamage(); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var level=context.getLevel();var pos=context.getClickedPos();var player=context.getPlayer();
        if(player==null || !level.getBlockState(pos).is(TC4Modern.TABLE.get())) return InteractionResult.PASS;
        for(Direction direction:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}) {
            var adjacent=pos.relative(direction);
            if(!level.getBlockState(adjacent).is(TC4Modern.TABLE.get())) continue;
            if(!level.isClientSide) {
                var root=TC4Modern.RESEARCH_TABLE.get().defaultBlockState().setValue(ResearchTableBlock.FACING,direction);
                level.setBlock(pos,root,2);
                level.setBlock(adjacent,root.setValue(ResearchTableBlock.SECOND,true),3);
                if(level.getBlockEntity(pos) instanceof ResearchTableEntity table) table.inventory.setItem(0,stack.copyWithCount(1));
                if(!player.getAbilities().instabuild) stack.shrink(1);
                level.sendBlockUpdated(pos,root,root,3);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
