package dev.eugheist.tc4modern;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Orientations share a scan; original metadata content variants keep separate aspect lists. */
final class BlockAspectBridge {
    static int variant(BlockState state) {
        if(state.getBlock() instanceof TaintBlock)return state.getValue(TaintBlock.VARIANT);
        if(state.getBlock() instanceof TaintFibresBlock)return state.getValue(TaintFibresBlock.VARIANT);
        return 0;
    }
    /** Item form of a placed block; powder snow (and any block whose item is a bucket) is not its bucket. */
    static ItemStack stack(BlockState state) {
        var stack=new ItemStack(state.getBlock());
        return bucketForm(stack) ? ItemStack.EMPTY : stack;
    }
    static boolean bucketForm(ItemStack stack){return stack.getItem() instanceof net.minecraft.world.item.DispensibleContainerItem;}
    static String key(BlockState state) {
        String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        int variant=variant(state);
        return variant==0 ? id : id+"#variant="+variant;
    }
    static Map<String,Integer> get(BlockState state,Level level) {
        String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String key=key(state);
        var tags=AspectTable.get(key);
        if(tags.isEmpty())tags=AspectTable.get(id);
        if(tags.isEmpty())tags=RecipeAspects.block(id,stack(state),level);
        if(tags.isEmpty())tags=ModernAspectCatalog.get("blocks",key);
        if(tags.isEmpty())tags=ModernAspectCatalog.get("blocks",id);
        return tags;
    }
}
