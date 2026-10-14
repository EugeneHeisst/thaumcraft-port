package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class OuterCrystal {
 public static final DeferredHolder<Block,CrystalBlock> BLOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("eldritch_crystal"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<Crystal>> TYPE=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("eldritch_crystal"));
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.BLOCK,h->h.register(BLOCK.getId(),new CrystalBlock()));e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(TYPE.getId(),BlockEntityType.Builder.of(Crystal::new,BLOCK.get()).build(null)));}
 public static final class CrystalBlock extends BaseEntityBlock {
  CrystalBlock(){super(Properties.of().strength(.7f,TC4SoundTypes.res(1)).lightLevel(s->7).sound(TC4SoundTypes.CRYSTAL).noOcclusion());registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING,Direction.UP));}
  @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new CrystalBlock());}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.FACING);}
  @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Crystal(p,s);}
  @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder b){return java.util.List.of(new net.minecraft.world.item.ItemStack(TC4Modern.BALANCED_SHARD.get()));}
 }
 public static final class Crystal extends BlockEntity {Crystal(BlockPos p,BlockState s){super(TYPE.get(),p,s);}}
}
