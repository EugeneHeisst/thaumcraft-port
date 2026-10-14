package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.aspects.*;
/** Original TileFluxScrubber: shuffled16-block sphere, sixteen checks/tick, five aer per flux unit. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class FluxScrubber {
 public static final DeferredHolder<Block,ScrubberBlock> BLOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("flux_scrubber"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ScrubberEntity>> ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("flux_scrubber"));
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.BLOCK,h->h.register(BLOCK.getId(),new ScrubberBlock(Block.Properties.of().strength(3,TC4SoundTypes.res(25)).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion())));e.register(Registries.ITEM,h->h.register(BLOCK.getId(),new BlockItem(BLOCK.get(),new Item.Properties())));e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ENTITY.getId(),BlockEntityType.Builder.of(ScrubberEntity::new,BLOCK.get()).build(null)));}
 public static final class ScrubberBlock extends BaseEntityBlock {
  static final MapCodec<ScrubberBlock> CODEC=simpleCodec(ScrubberBlock::new);ScrubberBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING,Direction.DOWN));}
  @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.FACING);}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.FACING,c.getClickedFace().getOpposite());}
  @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ScrubberEntity(p,s);}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return createTickerHelper(t,ENTITY.get(),ScrubberEntity::tick);}
 }
 public static final class ScrubberEntity extends BlockEntity implements IEssentiaTransport {
  int essentia,charges,power,count;final ArrayList<BlockPos> checklist=new ArrayList<>();int cursor;
  ScrubberEntity(BlockPos p,BlockState s){super(ENTITY.get(),p,s);}
  Direction facing(){return getBlockState().getValue(BlockStateProperties.FACING);}
  static void tick(Level l,BlockPos p,BlockState s,ScrubberEntity t){if(t.count==0)t.count=l.random.nextInt(1000);if(l.isClientSide)return;
   if(t.charges>=4){t.charges-=4;if(l.random.nextInt(4)==0){t.essentia=Math.min(4,t.essentia+1);t.setChanged();}}
   if(t.power<5)t.power+=VisNetwork.drain((ServerLevel)l,p,Aspect.AIR,10,(node,color)->{});
   if(t.power>=5)t.checkFlux();
  }
  void checkFlux(){
   if(cursor>=checklist.size()){checklist.clear();cursor=0;for(int x=-16;x<=16;x++)for(int y=-16;y<=16;y++)for(int z=-16;z<=16;z++)checklist.add(worldPosition.offset(x,y,z));Collections.shuffle(checklist,new java.util.Random(){@Override public int nextInt(int bound){return level.random.nextInt(bound);}});}
   for(int i=0;i<16&&cursor<checklist.size();i++){
    var p=checklist.get(cursor++);var s=level.getBlockState(p);
    if(!(s.getBlock() instanceof FluxBlock)||worldPosition.distSqr(p)>=256)continue;
    power-=5;int amount=s.getValue(FluxBlock.AMOUNT);level.setBlock(p,amount>1?s.setValue(FluxBlock.AMOUNT,amount-1):Blocks.AIR.defaultBlockState(),3);
    FociWorld.sparkle((ServerLevel)level,p,0xdd00ff,7);charges++;setChanged();return;
   }
  }
  @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putInt("facing",facing().get3DDataValue());tag.putInt("charges",charges);tag.putInt("power",power);tag.putInt("essentia",essentia);}
  @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);charges=tag.getInt("charges");power=tag.getInt("power");essentia=tag.getInt("essentia");}
  @Override public boolean isConnectable(Direction face){return face==facing();}@Override public boolean canOutputTo(Direction face){return face==facing();}@Override public boolean canInputFrom(Direction face){return false;}
  @Override public void setSuction(Aspect a,int n){}@Override public Aspect getSuctionType(Direction face){return null;}@Override public int getSuctionAmount(Direction face){return 0;}@Override public int getMinimumSuction(){return 0;}@Override public boolean renderExtendedTube(){return false;}
  @Override public Aspect getEssentiaType(Direction face){return Aspect.MAGIC;}@Override public int getEssentiaAmount(Direction face){return essentia;}
  // The original ignores both the requested aspect and face; connecting tubes enforce the output side.
  @Override public int takeEssentia(Aspect a,int n,Direction face){int taken=Math.min(essentia,n);essentia-=taken;setChanged();return taken;}
  @Override public int addEssentia(Aspect a,int n,Direction face){return 0;}
 }
}
