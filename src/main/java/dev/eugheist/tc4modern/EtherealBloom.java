package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

/** BlockCustomPlant metadata4 / TileEtherealBloom. Counters intentionally reset on chunk reload. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class EtherealBloom {
 public static final DeferredHolder<Block,BloomBlock> BLOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("ethereal_bloom"));
 public static final DeferredHolder<Item,Item> ITEM=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("ethereal_bloom"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<Entity>> ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("ethereal_bloom"));
 @SubscribeEvent static void register(RegisterEvent event){
  event.register(Registries.BLOCK,h->h.register(BLOCK.getId(),new BloomBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.GRASS).lightLevel(s->15))));
  event.register(Registries.ITEM,h->h.register(ITEM.getId(),new BlockItem(BLOCK.get(),new Item.Properties())));
  event.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ENTITY.getId(),BlockEntityType.Builder.of(Entity::new,BLOCK.get()).build(null)));
 }
 public static boolean cleansable(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome){return biome.is(TaintBiomes.TAINTED_LAND)||biome.is(MagicalForestContent.BIOME)||biome.unwrapKey().map(k->k.location().equals(AlchemyContent.id("eerie"))).orElse(false);}
 /** The original samples a triangular [-7,7] offset, then tests the centres against radius nine. */
 public static boolean cleanse(ServerLevel level,BlockPos origin,int x,int z){
  var pos=origin.offset(x,0,z);if(origin.distToCenterSqr(pos.getX()+.5,origin.getY(),pos.getZ()+.5)>81||!cleansable(level.getNoiseBiome(QuartPos.fromBlock(pos.getX()),QuartPos.fromBlock(pos.getY()),QuartPos.fromBlock(pos.getZ()))))return false;
  var biome=level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(pos.getX()),QuartPos.fromBlock(pos.getY()),QuartPos.fromBlock(pos.getZ()),level.getChunkSource().randomState().sampler());
  if(biome.is(TaintBiomes.TAINTED_LAND))biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
  TaintBiomes.setColumn(level,pos,biome);return true;
 }
 public static final class BloomBlock extends BaseEntityBlock {
  public static final MapCodec<BloomBlock> CODEC=simpleCodec(BloomBlock::new);
  BloomBlock(Properties p){super(p);}
  @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
  @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
  @Override protected boolean canSurvive(BlockState s,LevelReader level,BlockPos pos){return level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP);}
  @Override protected BlockState updateShape(BlockState s,Direction d,BlockState other,LevelAccessor level,BlockPos pos,BlockPos from){return d==Direction.DOWN&&!canSurvive(s,level,pos)?Blocks.AIR.defaultBlockState():super.updateShape(s,d,other,level,pos,from);}
  @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext c){return box(1.6,0,1.6,14.4,12.8,14.4);}
  @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState s){return new Entity(pos,s);}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState s,BlockEntityType<T> type){return createTickerHelper(type,ENTITY.get(),Entity::tick);}
  @Override public int getFireSpreadSpeed(BlockState s,BlockGetter l,BlockPos p,Direction d){return 60;}
  @Override public int getFlammability(BlockState s,BlockGetter l,BlockPos p,Direction d){return 100;}
 }
 public static final class Entity extends BlockEntity {
  public int counter,growthCounter;
  Entity(BlockPos pos,BlockState state){super(ENTITY.get(),pos,state);}
  public static void tick(Level level,BlockPos pos,BlockState state,Entity tile){
   if(tile.counter==0)tile.counter=level.random.nextInt(100);tile.counter++;
   if(level instanceof ServerLevel server&&tile.counter%20==0)cleanse(server,pos,level.random.nextInt(8)-level.random.nextInt(8),level.random.nextInt(8)-level.random.nextInt(8));
   if(level.isClientSide&&tile.growthCounter==0)level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,TC4Modern.ROOTS.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,.6f,false);
   tile.growthCounter++;
  }
 }
}
