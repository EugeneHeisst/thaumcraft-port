package dev.eugheist.tc4modern;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
/** Original BlockLoot urn/crate shapes and rarity-specific generated drops. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class OuterLoot {
    public static final List<DeferredHolder<Block,Block>> URNS=blocks("urn"),CRATES=blocks("crate");
    static List<DeferredHolder<Block,Block>> blocks(String type){return java.util.stream.Stream.of("common","uncommon","rare").map(r->DeferredHolder.<Block,Block>create(Registries.BLOCK,AlchemyContent.id("loot_"+type+"_"+r))).toList();}
    @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.BLOCK,h->{for(var group:List.of(URNS,CRATES))for(int i=0;i<3;i++)h.register(group.get(i).getId(),new Container(group==URNS,i));});e.register(Registries.ITEM,h->{for(var group:List.of(URNS,CRATES))for(int i=0;i<3;i++)h.register(group.get(i).getId(),new BlockItem(group.get(i).get(),new Item.Properties().rarity(Rarity.values()[i])){
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){super.appendHoverText(stack,context,lines,flag);lines.add(net.minecraft.network.chat.Component.literal(switch(stack.getRarity()){case UNCOMMON->"Uncommon";case RARE->"Rare";default->"Common";}));}
    });});}
    static final class Container extends Block {
        final boolean urn;final int rarity;
        Container(boolean urn,int rarity){super(Properties.of().strength(.15f,0).noOcclusion().sound(urn?TC4SoundTypes.custom(1,.7f,"urnbreak"):SoundType.WOOD));this.urn=urn;this.rarity=rarity;}
        @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return urn?Block.box(2,1,2,14,13,14):Block.box(1,0,1,15,14,15);}
        @Override protected List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){var level=builder.getLevel();var drops=new ArrayList<ItemStack>();int count=1+rarity+level.random.nextInt(3);for(int i=0;i<count;i++)drops.add(LootBags.generate(rarity,level));return drops;}
    }
}
