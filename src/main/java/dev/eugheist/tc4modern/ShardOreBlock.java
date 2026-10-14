package dev.eugheist.tc4modern;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
/** Platform loot adapter for original infused stone, split from metadata into six blocks. */
public final class ShardOreBlock extends DropExperienceBlock {
    public final int variant;
    public ShardOreBlock(Properties properties,int variant){super(net.minecraft.util.valueproviders.UniformInt.of(0,3),properties);this.variant=variant;}
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder params){
        ItemStack tool=params.getOptionalParameter(LootContextParams.TOOL);
        if(tool==null)tool=ItemStack.EMPTY;
        var enchantments=params.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        if(EnchantmentHelper.getItemEnchantmentLevel(enchantments.getHolderOrThrow(Enchantments.SILK_TOUCH),tool)>0)return List.of(new ItemStack(TC4Modern.SHARD_ORE_ITEMS.get(variant).get()));
        int fortune=EnchantmentHelper.getItemEnchantmentLevel(enchantments.getHolderOrThrow(Enchantments.FORTUNE),tool);
        var source=params.getLevel().random;
        var random=new java.util.Random(){@Override public int nextInt(int bound){return source.nextInt(bound);}};
        return List.of(new ItemStack(TC4Modern.SHARDS.get(variant).get(),thaumcraft.common.lib.vis.ShardPalette.drops(random,fortune)));
    }
}
