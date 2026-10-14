package dev.eugheist.tc4modern;

import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Missing original BlockItems; secondary metadata forms must not replace the block's primary item mapping. */
final class CreativeBlocks {
    static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(TC4Modern.MODID);
    static {
        ITEMS.registerSimpleBlockItem("greatwood_leaves",TC4Modern.GREATWOOD_LEAVES);
        ITEMS.registerSimpleBlockItem("flux_goo",TC4Modern.FLUX_GOO);
        ITEMS.registerSimpleBlockItem("flux_gas",TC4Modern.FLUX_GAS);
        ITEMS.register("taint",()->new Variant(TC4Modern.TAINT.get(),TaintBlock.VARIANT,0,"taint"));
        ITEMS.register("taint_soil",()->new Variant(TC4Modern.TAINT.get(),TaintBlock.VARIANT,1,"taint_soil"));
        String[] names={"taint_fibres","taint_grass","taint_plant","taint_spore"};
        for(int i=0;i<names.length;i++){int value=i;String name=names[i];ITEMS.register(name,()->new Variant(TC4Modern.TAINT_FIBRES.get(),TaintFibresBlock.VARIANT,value,name));}
    }
    static final class Variant extends BlockItem {
        final IntegerProperty property;final int value;final String name;
        Variant(Block block,IntegerProperty property,int value,String name){super(block,new Item.Properties());this.property=property;this.value=value;this.name=name;}
        @Override public String getDescriptionId(){return "block.tc4modern."+name;}
        @Override protected BlockState getPlacementState(BlockPlaceContext context){var s=super.getPlacementState(context);return s==null?null:s.setValue(property,value);}
        @Override public void registerBlocks(java.util.Map<Block,Item> map,Item item){if(value==0)super.registerBlocks(map,item);}
    }
}
