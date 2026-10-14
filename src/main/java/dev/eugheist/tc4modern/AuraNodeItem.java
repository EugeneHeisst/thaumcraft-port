package dev.eugheist.tc4modern;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Creative node placement; an optional Aspects component can select its reservoir. */
public final class AuraNodeItem extends BlockItem {
    public AuraNodeItem(Block block, Properties properties) { super(block,properties); }
    static AspectList aspects(net.minecraft.world.item.ItemStack stack) {
        var aspects=new AspectList();aspects.readFromNBT(NoteData.tag(stack));
        if(aspects.size()==0) for(var primal:new Aspect[]{Aspect.AIR,Aspect.FIRE,Aspect.EARTH,Aspect.WATER}) aspects.add(primal,40);
        return aspects;
    }
    @Override protected boolean placeBlock(BlockPlaceContext context,BlockState state) {
        if(!super.placeBlock(context,state)) return false;
        if(!context.getLevel().isClientSide && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof AuraNodeEntity node) {
            var selected=new AspectList();selected.readFromNBT(NoteData.tag(context.getItemInHand()));
            if(selected.size()>0)node.reservoir.setAspects(selected);
            else AuraNodeFeature.createRandomNodeAt((net.minecraft.server.level.ServerLevel)context.getLevel(),context.getClickedPos(),context.getLevel().random,false,false,false);
            node.changed();
        }
        return true;
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag) {
        var aspects=new AspectList();aspects.readFromNBT(NoteData.tag(stack));
        for(var aspect:aspects.getAspectsSorted()) lines.add(net.minecraft.network.chat.Component.literal(aspect.getName()+" × "+aspects.getAmount(aspect)));
    }
}
